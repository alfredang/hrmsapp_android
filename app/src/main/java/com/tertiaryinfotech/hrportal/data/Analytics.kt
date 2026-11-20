package com.tertiaryinfotech.hrportal.data

import kotlinx.serialization.Serializable

/**
 * `GET /api/mobile/analytics` (approvers only): the data behind the web /dashboard/analytics page.
 * The last 12 calendar months, oldest first. The app filters and slices it on the device, exactly
 * as the web page does in the browser. Shapes mirror `AnalyticsData` in the web `src/lib/analytics.ts`.
 */
@Serializable
data class AnalyticsData(
    val months: List<AnalyticsMonth> = emptyList(),
    /** Null when the viewer may not see accounting data. */
    val finance: List<FinanceRow>? = null,
    val financeRange: FinanceRange? = null,
    val leave: List<LeaveDaysRow> = emptyList(),
    val people: List<AnalyticsPerson> = emptyList(),
    val headcount: List<HeadcountRow> = emptyList(),
    /**
     * One row per ACTIVE person (no name, no date of birth) for the workforce breakdown donuts.
     * Null or absent (an older server) hides that section.
     */
    val breakdown: List<BreakdownPerson>? = null,
)

/** Gender, age group and ethnic group of one active person, as text labels from the server. */
@Serializable
data class BreakdownPerson(
    val intern: Boolean = false,
    val gender: String = "",
    val ageGroup: String = "",
    val ethnicGroup: String = "",
)

@Serializable
data class AnalyticsMonth(val key: String, val label: String = "", val year: Int = 0)

@Serializable
data class FinanceRow(
    val month: String,
    val type: String = "Other",
    @Serializable(with = FlexNumberSerializer::class) val revenue: Double = 0.0,
    @Serializable(with = FlexNumberSerializer::class) val expenses: Double = 0.0,
)

@Serializable
data class FinanceRange(val from: String = "", val to: String = "")

/** Approved leave days for one person in one month. [category] is "LEAVE", "MC" or "NPL". */
@Serializable
data class LeaveDaysRow(
    val month: String,
    val personId: String,
    val category: String = "LEAVE",
    @Serializable(with = FlexNumberSerializer::class) val days: Double = 0.0,
)

@Serializable
data class AnalyticsPerson(
    val id: String,
    val name: String = "",
    val code: String = "",
    val intern: Boolean = false,
    val active: Boolean = true,
)

@Serializable
data class HeadcountRow(val month: String, val staff: Int = 0, val interns: Int = 0)

enum class AnalyticsWho { ALL, STAFF, INTERNS }

/** What the filter row selects. Defaults are the unfiltered 12-month view. */
data class AnalyticsFilter(
    val period: Int = 12,
    val who: AnalyticsWho = AnalyticsWho.ALL,
    val personId: String? = null,
    val leaveOff: Set<String> = emptySet(),
    val typeOff: Set<String> = emptySet(),
) {
    val isDefault: Boolean
        get() = period == 12 && who == AnalyticsWho.ALL && personId == null && leaveOff.isEmpty() && typeOff.isEmpty()
}

data class MonthValues(val key: String, val label: String, val title: String, val values: Map<String, Double>)
data class CategoryTotal(val type: String, val revenue: Double, val expenses: Double) {
    val isRevenue: Boolean get() = revenue >= expenses
    val amount: Double get() = if (isRevenue) revenue else expenses
}
data class PersonLeave(val person: AnalyticsPerson, val leave: Double, val mc: Double, val npl: Double) {
    val total: Double get() = leave + mc + npl
    fun of(category: String): Double = when (category) { "MC" -> mc; "NPL" -> npl; else -> leave }
}
data class HeadcountChange(val title: String, val staff: Int, val interns: Int, val total: Int, val change: Int?)

/** Everything the Analytics screen shows, computed from one filter so tiles and charts always agree. */
data class AnalyticsView(
    val rangeLabel: String,
    val hasFinance: Boolean,
    val revenue: Double,
    val expenses: Double,
    val financeByMonth: List<MonthValues>,
    val categories: List<CategoryTotal>,
    val leaveSeries: List<String>,
    val leaveByMonth: List<MonthValues>,
    val leaveTotals: Map<String, Double>,
    val byPerson: List<PersonLeave>,
    val selectablePeople: List<AnalyticsPerson>,
    val selectedPerson: AnalyticsPerson?,
    val headSeries: List<String>,
    val headByMonth: List<MonthValues>,
    val headNow: Int,
    val headDelta: Int,
    val headChanges: List<HeadcountChange>,
) {
    val net: Double get() = revenue - expenses
    val hasFinanceRows: Boolean get() = categories.isNotEmpty()
    val peopleOnMc: Int get() = byPerson.count { it.mc > 0 }
}

enum class BreakdownKind { GENDER, AGE, ETHNIC }

/** One donut slice. [order] is the label's position in its fixed order (the colour slot); -1 = "Not recorded". */
data class BreakdownSlice(val label: String, val count: Int, val percent: Int, val order: Int) {
    val notRecorded: Boolean get() = order < 0
}

data class BreakdownChart(val kind: BreakdownKind, val title: String, val slices: List<BreakdownSlice>) {
    val total: Int get() = slices.sumOf { it.count }
}

data class BreakdownView(
    val who: AnalyticsWho,
    /** "staff", "interns" or "people". */
    val noun: String,
    val shown: Int,
    val charts: List<BreakdownChart>,
    /** People in the shown group whose ethnic group is "Not recorded". */
    val ethnicMissing: Int,
)

/**
 * Workforce breakdown (web `src/lib/demographics.ts` + `workforce-breakdown.tsx`): counts of active
 * people by gender, age group and ethnic group. Pure, unit-tested on the JVM.
 */
object Demographics {
    const val NOT_RECORDED = "Not recorded"
    val GENDERS = listOf("Male", "Female", "Other")
    /** En dashes, exactly as the server sends them. */
    val AGE_GROUPS = listOf("Under 25", "25–34", "35–44", "45–54", "55 and over")
    val ETHNIC_GROUPS = listOf("Chinese", "Malay", "Indian", "Eurasian", "Others")

    /**
     * Count per label in the fixed [order], then any label the app does not know yet (so the total
     * still equals the number of people), then "Not recorded" last. Empty slices are dropped.
     * A blank value counts as "Not recorded".
     */
    fun countBy(people: List<BreakdownPerson>, order: List<String>, value: (BreakdownPerson) -> String): List<BreakdownSlice> {
        val counts = LinkedHashMap<String, Int>()
        for (p in people) {
            val label = value(p).trim().ifEmpty { NOT_RECORDED }
            counts[label] = (counts[label] ?: 0) + 1
        }
        val total = people.size
        val unknown = counts.keys.filter { it !in order && it != NOT_RECORDED }.sorted()
        val labels = order.filter { it != NOT_RECORDED } + unknown + NOT_RECORDED
        return labels.mapNotNull { label ->
            val n = counts[label] ?: return@mapNotNull null
            val slot = if (label == NOT_RECORDED) -1 else labels.indexOf(label)
            BreakdownSlice(label, n, percent(n, total), slot)
        }
    }

    /** Whole percent, rounded half up as the web's `Math.round`. */
    fun percent(n: Int, total: Int): Int = if (total <= 0) 0 else Math.round(n * 100.0 / total).toInt()

    fun view(people: List<BreakdownPerson>, who: AnalyticsWho): BreakdownView {
        val shown = people.filter { who == AnalyticsWho.ALL || (who == AnalyticsWho.INTERNS) == it.intern }
        val charts = listOf(
            BreakdownChart(BreakdownKind.GENDER, "Gender", countBy(shown, GENDERS) { it.gender }),
            BreakdownChart(BreakdownKind.AGE, "Age group", countBy(shown, AGE_GROUPS) { it.ageGroup }),
            BreakdownChart(BreakdownKind.ETHNIC, "Ethnic group", countBy(shown, ETHNIC_GROUPS) { it.ethnicGroup }),
        )
        val noun = when (who) { AnalyticsWho.STAFF -> "staff"; AnalyticsWho.INTERNS -> "interns"; AnalyticsWho.ALL -> "people" }
        val missing = shown.count { it.ethnicGroup.trim().ifEmpty { NOT_RECORDED } == NOT_RECORDED }
        return BreakdownView(who, noun, shown.size, charts, missing)
    }
}

/** Active staff and interns today, for the Home tiles. */
data class ActiveCounts(val staff: Int, val interns: Int)

/** Pure filtering and aggregation for the Analytics screen (no Android types: unit-tested on the JVM). */
object AnalyticsCalc {
    val LEAVE_CATEGORIES = listOf("LEAVE", "MC", "NPL")

    fun view(data: AnalyticsData, f: AnalyticsFilter): AnalyticsView {
        val months = data.months.takeLast(f.period.coerceIn(1, 12))
        val keys = months.map { it.key }.toSet()
        fun title(key: String) = data.months.firstOrNull { it.key == key }?.let { "${it.label} ${it.year}" } ?: key
        val range = if (months.isEmpty()) "" else "${title(months.first().key)} to ${title(months.last().key)}"

        // People
        val byId = data.people.associateBy { it.id }
        fun inWho(intern: Boolean) = f.who == AnalyticsWho.ALL || (f.who == AnalyticsWho.INTERNS) == intern
        val selectable = data.people.filter { inWho(it.intern) }
        // A person picked under "All" stops applying once the group no longer contains them.
        val person = f.personId?.let { id -> selectable.firstOrNull { it.id == id } }

        // Leave
        val leaveSeries = LEAVE_CATEGORIES.filter { it !in f.leaveOff }
        val leaveRows = data.leave.filter { r ->
            val p = byId[r.personId] ?: return@filter false
            if (r.month !in keys || r.category !in leaveSeries) return@filter false
            if (person != null) r.personId == person.id else inWho(p.intern)
        }
        val leaveByMonth = months.map { m ->
            val v = LEAVE_CATEGORIES.associateWith { c -> leaveRows.filter { it.month == m.key && it.category == c }.sumOf { it.days } }
            MonthValues(m.key, m.label, title(m.key), v)
        }
        val leaveTotals = LEAVE_CATEGORIES.associateWith { c -> leaveRows.filter { it.category == c }.sumOf { it.days } }
        val byPerson = leaveRows.groupBy { it.personId }.mapNotNull { (id, rows) ->
            val p = byId[id] ?: return@mapNotNull null
            fun sum(c: String) = rows.filter { it.category == c }.sumOf { it.days }
            PersonLeave(p, sum("LEAVE"), sum("MC"), sum("NPL"))
        }.sortedWith(compareByDescending<PersonLeave> { it.total }.thenBy { it.person.name })

        // Finance
        val financeRows = data.finance.orEmpty().filter { it.month in keys }
        val categories = financeRows.groupBy { it.type }
            .map { (type, rows) -> CategoryTotal(type, rows.sumOf { it.revenue }, rows.sumOf { it.expenses }) }
            .sortedByDescending { it.revenue + it.expenses }
        val financeOn = financeRows.filter { it.type !in f.typeOff }
        val financeByMonth = months.map { m ->
            val rows = financeOn.filter { it.month == m.key }
            MonthValues(m.key, m.label, title(m.key), mapOf("revenue" to rows.sumOf { it.revenue }, "expenses" to rows.sumOf { it.expenses }))
        }

        // Headcount (the person filter does not apply: it is a count of people)
        val headSeries = listOf("staff", "interns").filter {
            f.who == AnalyticsWho.ALL || (it == "staff") == (f.who == AnalyticsWho.STAFF)
        }
        val headByMonth = data.headcount.filter { it.month in keys }.map { h ->
            MonthValues(
                h.month, data.months.firstOrNull { it.key == h.month }?.label ?: h.month, title(h.month),
                mapOf(
                    "staff" to if (f.who == AnalyticsWho.INTERNS) 0.0 else h.staff.toDouble(),
                    "interns" to if (f.who == AnalyticsWho.STAFF) 0.0 else h.interns.toDouble(),
                ),
            )
        }
        fun total(m: MonthValues) = (m.values.values.sum()).toInt()
        val headNow = headByMonth.lastOrNull()?.let(::total) ?: 0
        val headFirst = headByMonth.firstOrNull()?.let(::total) ?: 0
        val changes = headByMonth.mapIndexed { i, m ->
            HeadcountChange(
                m.title, (m.values["staff"] ?: 0.0).toInt(), (m.values["interns"] ?: 0.0).toInt(), total(m),
                if (i == 0) null else total(m) - total(headByMonth[i - 1]),
            )
        }.reversed()

        return AnalyticsView(
            rangeLabel = range,
            hasFinance = data.finance != null,
            revenue = financeOn.sumOf { it.revenue },
            expenses = financeOn.sumOf { it.expenses },
            financeByMonth = financeByMonth,
            categories = categories,
            leaveSeries = leaveSeries,
            leaveByMonth = leaveByMonth,
            leaveTotals = leaveTotals,
            byPerson = byPerson,
            selectablePeople = selectable,
            selectedPerson = person,
            headSeries = headSeries,
            headByMonth = headByMonth,
            headNow = headNow,
            headDelta = headNow - headFirst,
            headChanges = changes,
        )
    }

    /**
     * Active full-time staff and interns for the Home tiles: from the breakdown (one row per active
     * person) when the server sends it, else the last month of the headcount series. Null when
     * neither is there, and the tiles are then not shown.
     */
    fun activeCounts(data: AnalyticsData): ActiveCounts? {
        data.breakdown?.let { b -> return ActiveCounts(b.count { !it.intern }, b.count { it.intern }) }
        return data.headcount.lastOrNull()?.let { ActiveCounts(it.staff, it.interns) }
    }

    /** Axis maximum whose midpoint is also a round number (minimum 4). Same rule as the web chart. */
    fun niceCeil(v: Double): Double {
        if (v <= 4) return 4.0
        if (v <= 10) return Math.ceil(v / 2) * 2
        val pow = Math.pow(10.0, Math.floor(Math.log10(v)))
        for (step in doubleArrayOf(1.0, 1.2, 1.6, 2.0, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0)) if (v <= step * pow) return step * pow
        return 10 * pow
    }

    /** "$1.2M", "$400k", "$0" for the money axis. */
    fun moneyAxis(v: Double): String = when {
        v >= 1_000_000 -> "$" + trim(v / 1_000_000, 1) + "M"
        v >= 1000 -> "$" + trim(v / 1000, 0) + "k"
        else -> "$" + trim(v, 0)
    }

    /** "$3,089,763" and "-$189,557": whole dollars, as on the web tiles. */
    fun money(v: Double): String {
        val whole = Math.round(Math.abs(v))
        val grouped = String.format(java.util.Locale.US, "%,d", whole)
        return (if (v < 0 && whole != 0L) "-$" else "$") + grouped
    }

    /** "2" / "1.5": trims a trailing ".0" from day counts. */
    fun days(v: Double): String = trim(v, 1)

    private fun trim(v: Double, decimals: Int): String {
        val s = String.format(java.util.Locale.US, "%.${decimals}f", v)
        return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
    }

    /** Sample data for debug builds only, so the screen can be checked without an approver login. */
    fun sample(): AnalyticsData {
        val labels = listOf("Dec", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov")
        val months = labels.mapIndexed { i, l -> AnalyticsMonth(if (i == 0) "2025-12" else "2026-%02d".format(i), l, if (i == 0) 2025 else 2026) }
        val people = listOf(
            AnalyticsPerson("p1", "ANG SEE SIANG", "E0014"), AnalyticsPerson("p2", "JYOTI CHOPRA", "E0020"),
            AnalyticsPerson("p3", "MARCUS WOON ZHI XUAN", "E0021"), AnalyticsPerson("p4", "AMANDA HALIM", "I0007", intern = true),
            AnalyticsPerson("p5", "FEMINA JASMIN", "I0008", intern = true), AnalyticsPerson("p6", "EVAN PANG", "I0005", intern = true, active = false),
        )
        val rev = listOf(0, 0, 0, 0, 12, 400, 440, 570, 520, 400, 620, 140)
        val trainer = listOf(0, 0, 0, 0, 3, 210, 180, 90, 300, 190, 150, 120)
        val vendor = listOf(0, 0, 0, 0, 1, 160, 120, 110, 240, 170, 90, 130)
        val payroll = listOf(0, 0, 0, 0, 0, 30, 32, 30, 34, 33, 31, 24)
        val finance = months.flatMapIndexed { i, m ->
            listOf(
                FinanceRow(m.key, "Income", revenue = rev[i] * 1000.0),
                FinanceRow(m.key, "Trainer Fee", expenses = trainer[i] * 1000.0),
                FinanceRow(m.key, "Vendor Payment", expenses = vendor[i] * 1000.0),
                FinanceRow(m.key, "Payroll", expenses = payroll[i] * 1000.0),
                FinanceRow(m.key, "Subscription", expenses = if (i > 4) 4800.0 else 0.0),
            ).filter { it.revenue > 0 || it.expenses > 0 }
        }
        val leave = listOf(
            LeaveDaysRow("2026-05", "p2", "LEAVE", 1.0), LeaveDaysRow("2026-06", "p1", "LEAVE", 2.5), LeaveDaysRow("2026-06", "p4", "MC", 2.0),
            LeaveDaysRow("2026-07", "p4", "LEAVE", 4.0), LeaveDaysRow("2026-07", "p5", "MC", 5.0), LeaveDaysRow("2026-07", "p1", "NPL", 2.0),
            LeaveDaysRow("2026-08", "p3", "MC", 2.0), LeaveDaysRow("2026-08", "p6", "LEAVE", 1.0), LeaveDaysRow("2026-09", "p2", "LEAVE", 5.0),
            LeaveDaysRow("2026-09", "p3", "MC", 4.0), LeaveDaysRow("2026-10", "p1", "MC", 6.0), LeaveDaysRow("2026-10", "p5", "MC", 4.0),
            LeaveDaysRow("2026-10", "p4", "LEAVE", 3.0), LeaveDaysRow("2026-11", "p2", "LEAVE", 3.0), LeaveDaysRow("2026-11", "p3", "LEAVE", 4.0),
            LeaveDaysRow("2026-11", "p1", "NPL", 1.0), LeaveDaysRow("2026-11", "p4", "MC", 1.0),
        )
        val staff = listOf(10, 10, 11, 11, 12, 12, 13, 13, 14, 14, 14, 14)
        val interns = listOf(0, 1, 1, 5, 5, 7, 9, 7, 6, 6, 9, 9)
        // 14 staff and 9 interns, matching the last headcount month. Ethnic group is only partly recorded.
        fun rows(intern: Boolean, n: Int, gender: List<String>, age: List<String>, ethnic: List<String>) =
            (0 until n).map { BreakdownPerson(intern, gender[it % gender.size], age[it % age.size], ethnic[it % ethnic.size]) }
        val breakdown = rows(
            false, 14, listOf("Female", "Male", "Female", "Male", "Female"),
            listOf("25–34", "35–44", "25–34", "45–54", "35–44", "55 and over", "25–34"),
            listOf("Chinese", "Chinese", "Malay", "Indian", "Chinese", "Not recorded", "Eurasian"),
        ) + rows(
            true, 9, listOf("Male", "Female", "Male"),
            listOf("Under 25", "Under 25", "Under 25", "25–34"),
            listOf("Not recorded"),
        )
        return AnalyticsData(
            months, finance, FinanceRange("2026-04-30", "2026-11-16"), leave, people,
            months.mapIndexed { i, m -> HeadcountRow(m.key, staff[i], interns[i]) },
            breakdown,
        )
    }
}
