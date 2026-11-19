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
        return AnalyticsData(
            months, finance, FinanceRange("2026-04-30", "2026-11-16"), leave, people,
            months.mapIndexed { i, m -> HeadcountRow(m.key, staff[i], interns[i]) },
        )
    }
}
