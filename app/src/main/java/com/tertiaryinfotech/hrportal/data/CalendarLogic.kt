package com.tertiaryinfotech.hrportal.data

import com.tertiaryinfotech.hrportal.util.Fmt
import java.util.Calendar
import java.util.Date

/**
 * 1.16 calendar parity with the web (Alfred, 20 Nov 2026). Pure logic, no Android types, so it is
 * unit-tested on the JVM. Sources: web `src/lib/calendar-categories.ts`, `src/lib/calendar-summary.ts`
 * and `src/components/calendar/calendar-view.tsx`.
 */
object CalendarCategories {
    const val HOLIDAY = "HOLIDAY"
    const val ANNUAL_LEAVE = "ANNUAL_LEAVE"
    const val MEDICAL_LEAVE = "MEDICAL_LEAVE"
    const val NO_PAY_LEAVE = "NO_PAY_LEAVE"
    const val OTHER_LEAVE = "OTHER_LEAVE"
    const val TIME_OFF = "TIME_OFF"
    const val BIRTHDAY = "BIRTHDAY"

    /** The web table, used when the server sends no `categories` (an older server). Legend order. */
    val FALLBACK: List<CalendarCategoryInfo> = listOf(
        CalendarCategoryInfo(HOLIDAY, "Public Holiday", "#ef4444", ""),
        CalendarCategoryInfo(ANNUAL_LEAVE, "Annual Leave", "#f59e0b", "🏖️"),
        CalendarCategoryInfo(MEDICAL_LEAVE, "Medical Leave", "#ec4899", "🤒"),
        CalendarCategoryInfo(NO_PAY_LEAVE, "No Pay Leave", "#64748b", "💸"),
        CalendarCategoryInfo(OTHER_LEAVE, "Other Leave", "#9a6b4f", "📋"),
        CalendarCategoryInfo(TIME_OFF, "Time Off", "#06b6d4", "⏰"),
        CalendarCategoryInfo(BIRTHDAY, "Birthday", "#d946ef", "🎂"),
        CalendarCategoryInfo("MEETING", "Meeting", "#3b82f6", ""),
        CalendarCategoryInfo("TRAINING", "Training", "#a855f7", ""),
        CalendarCategoryInfo("COMPANY_EVENT", "Company Event", "#22c55e", ""),
    )

    /** The server's legend when it sent a usable one, else [FALLBACK]. Bad colours fall back per row. */
    fun table(data: TeamCalendarResponse): List<CalendarCategoryInfo> {
        val server = data.categories?.filter { it.key.isNotBlank() }.orEmpty()
        if (server.isEmpty()) return FALLBACK
        val fallback = FALLBACK.associateBy { it.key }
        return server.map { c ->
            val f = fallback[c.key]
            c.copy(
                label = c.label.ifBlank { f?.label ?: c.key },
                color = if (parseHex(c.color) != null) c.color else f?.color ?: "#6b7280",
            )
        }
    }

    /** "#RRGGBB" to 0xRRGGBB, or null when it is not a 6-digit hex colour. */
    fun parseHex(color: String): Int? =
        if (Regex("^#[0-9a-fA-F]{6}$").matches(color)) color.substring(1).toInt(16) else null

    fun isLeave(category: String?): Boolean = category != null && category.endsWith("_LEAVE")
}

/** Web `PersonMonthSummary`. */
data class PersonMonthSummary(
    val annualLeaveDays: Double = 0.0,
    val medicalLeaveDays: Double = 0.0,
    val noPayLeaveDays: Double = 0.0,
    val otherLeaveDays: Double = 0.0,
    val timeOffHours: Double = 0.0,
    /** Approved full-day Off Days (interns). */
    val offDays: Int = 0,
)

/** Web `SummaryEvent`: a leave or time off placed on the calendar. */
data class SummaryEvent(
    val category: String,
    val start: Date,
    val end: Date,
    val allDay: Boolean,
    val personId: String?,
    /** Leave: the days charged for the whole request (0.5 for a half day). */
    val days: Double? = null,
    /** Time off: hours taken (0 for a full Off Day). */
    val hours: Double? = null,
)

/** 1:1 port of web `personMonthSummary` (src/lib/calendar-summary.ts), in the device's time zone. */
object CalendarSummary {
    /** "yyyy-MM-dd" in local time, the key the web uses for holidays. */
    fun dayKey(d: Date): String {
        val c = Calendar.getInstance().apply { time = d }
        return String.format(
            java.util.Locale.US, "%04d-%02d-%02d",
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
        )
    }

    private fun round2(n: Double): Double = Math.round(n * 100).toDouble() / 100

    /**
     * Totals for [personId] in the month ([month] is 0-based). A leave wholly inside the month counts
     * its charged days; one that crosses a month boundary is split by working days (Mon–Fri, public
     * holidays excluded), so the months together add up to the charged days.
     */
    fun personMonthSummary(
        events: List<SummaryEvent>,
        personId: String,
        year: Int,
        month: Int,
        holidayKeys: Set<String> = emptySet(),
    ): PersonMonthSummary {
        var al = 0.0; var mc = 0.0; var npl = 0.0; var other = 0.0; var hours = 0.0; var offDays = 0
        fun inMonth(c: Calendar) = c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month
        fun isWorking(c: Calendar): Boolean {
            val dow = c.get(Calendar.DAY_OF_WEEK)
            return dow != Calendar.SUNDAY && dow != Calendar.SATURDAY && dayKey(c.time) !in holidayKeys
        }

        for (e in events) {
            if (e.personId != personId) continue
            val start = midnight(e.start)
            val end = midnight(e.end)

            if (e.category == CalendarCategories.TIME_OFF) {
                if (!inMonth(start)) continue
                if (e.allDay) offDays += 1 else hours += e.hours ?: 0.0
                continue
            }
            if (!CalendarCategories.isLeave(e.category)) continue

            var working = 0; var workingIn = 0; var calendar = 0; var calendarIn = 0
            val d = start.clone() as Calendar
            var guard = 0
            while (!d.after(end) && guard < 800) {
                calendar += 1
                if (inMonth(d)) calendarIn += 1
                if (isWorking(d)) {
                    working += 1
                    if (inMonth(d)) workingIn += 1
                }
                d.add(Calendar.DAY_OF_MONTH, 1)
                guard++
            }
            if (calendarIn == 0) continue
            val charged = e.days ?: working.toDouble()
            // No working day in the range (e.g. weekend leave): fall back to calendar days.
            val share = when {
                calendarIn == calendar -> 1.0
                working > 0 -> workingIn.toDouble() / working
                else -> calendarIn.toDouble() / calendar
            }
            val days = charged * share
            when (e.category) {
                CalendarCategories.ANNUAL_LEAVE -> al += days
                CalendarCategories.MEDICAL_LEAVE -> mc += days
                CalendarCategories.NO_PAY_LEAVE -> npl += days
                else -> other += days
            }
        }
        return PersonMonthSummary(round2(al), round2(mc), round2(npl), round2(other), round2(hours), offDays)
    }

    /** The picked person's leave (with a category) and time off, as summary events. */
    fun events(data: TeamCalendarResponse): List<SummaryEvent> = buildList {
        for (e in data.entries) {
            val cat = e.category ?: continue
            val s = Fmt.parse(e.startDate) ?: continue
            add(SummaryEvent(cat, s, Fmt.parse(e.endDate) ?: s, allDay = true, personId = e.employeeId, days = e.days))
        }
        for (t in data.timeOff) {
            val d = Fmt.parse(t.date) ?: continue
            add(SummaryEvent(CalendarCategories.TIME_OFF, d, d, allDay = t.offDay, personId = t.employeeId, hours = t.hours))
        }
    }

    /** Holiday day keys ("yyyy-MM-dd", local) for the working-day split. */
    fun holidayKeys(data: TeamCalendarResponse): Set<String> =
        data.holidays.mapNotNull { Fmt.parse(it.startDate)?.let(::dayKey) }.toSet()

    /** The web's month-total pills: label, value, category key. Other Leave only when above 0. */
    fun rows(s: PersonMonthSummary): List<Triple<String, String, String>> {
        fun days(n: Double) = "${num(n)} ${if (n == 1.0) "day" else "days"}"
        return buildList {
            add(Triple("Annual Leave", days(s.annualLeaveDays), CalendarCategories.ANNUAL_LEAVE))
            add(Triple("Medical Leave", days(s.medicalLeaveDays), CalendarCategories.MEDICAL_LEAVE))
            add(Triple("No Pay Leave", days(s.noPayLeaveDays), CalendarCategories.NO_PAY_LEAVE))
            if (s.otherLeaveDays > 0) add(Triple("Other Leave", days(s.otherLeaveDays), CalendarCategories.OTHER_LEAVE))
            val extra = if (s.offDays > 0) " + ${s.offDays} off ${if (s.offDays == 1) "day" else "days"}" else ""
            add(Triple("Time Off", "${num(s.timeOffHours)} h$extra", CalendarCategories.TIME_OFF))
        }
    }

    /** JavaScript number formatting for these values: 2 → "2", 2.5 → "2.5", 1.33 → "1.33". */
    fun num(n: Double): String = if (n % 1.0 == 0.0) n.toLong().toString() else n.toString()

    private fun midnight(d: Date): Calendar = Calendar.getInstance().apply {
        time = d
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
}

/** Whose off days are shaded: one schedule, with the person whose full-day Off Days also count. */
data class OffDaySchedule(
    /** ISO weekdays worked (1 = Mon … 7 = Sun), never empty. */
    val workdays: List<Int>,
    /** The person whose approved Off Days shade a day; null = the viewer's own (`isSelf`). */
    val personId: String?,
)

/**
 * Off-day shading (web `calendar-view.tsx`): only when a single schedule applies — the staff / intern
 * viewer's own `ownWorkdays`, or the person an approver picked (none for Everyone). A day is shaded
 * when its ISO weekday is not a workday (Mon–Fri when the list is empty) or it has an approved
 * full-day Off Day for that person.
 */
object OffDays {
    val DEFAULT_WORKDAYS = listOf(1, 2, 3, 4, 5)

    /**
     * @param approver the viewer has the person filter ([CalendarPeople.build] is not null).
     * @param mineOnly "Only me" is on: the viewer's own schedule.
     * @param personId the person an approver picked, null for Everyone.
     */
    fun schedule(data: TeamCalendarResponse, approver: Boolean, mineOnly: Boolean, personId: String?): OffDaySchedule? {
        if (!approver || mineOnly) {
            val own = data.ownWorkdays ?: return null
            return OffDaySchedule(own.ifEmpty { DEFAULT_WORKDAYS }, null)
        }
        if (personId == null) return null
        // An older server sends no workdays at all: nothing to shade by.
        if (data.people?.any { it.workdays != null } != true && data.ownWorkdays == null) return null
        val workdays = data.people?.firstOrNull { it.id == personId }?.workdays.orEmpty()
        return OffDaySchedule(workdays.ifEmpty { DEFAULT_WORKDAYS }, personId)
    }

    /** ISO weekday of [d]: 1 = Mon … 7 = Sun (the web's `date.getDay() || 7`). */
    fun isoWeekday(d: Date): Int {
        val dow = Calendar.getInstance().apply { time = d }.get(Calendar.DAY_OF_WEEK)
        return if (dow == Calendar.SUNDAY) 7 else dow - 1
    }

    /** Whether [day] is shaded under [schedule], given the day's approved time off. */
    fun isOff(schedule: OffDaySchedule?, day: Date, timeOffOnDay: List<TeamTimeOff>): Boolean {
        if (schedule == null) return false
        if (isoWeekday(day) !in schedule.workdays) return true
        return timeOffOnDay.any { t ->
            t.offDay && if (schedule.personId == null) t.isSelf else t.employeeId == schedule.personId
        }
    }
}

/** The person / "Only me" filters for time off and birthdays (leave uses [CalendarPeople.filter]). */
object CalendarFilters {
    fun timeOff(list: List<TeamTimeOff>, mineOnly: Boolean, personId: String?): List<TeamTimeOff> = when {
        mineOnly -> list.filter { it.isSelf }
        personId != null -> list.filter { it.employeeId == personId }
        else -> list
    }

    /**
     * Birthdays (approvers only) follow the person filter; "Only me" keeps the viewer's own, found
     * from their own leave or time off (none when that is unknown).
     */
    fun birthdays(data: TeamCalendarResponse, mineOnly: Boolean, personId: String?): List<TeamBirthday> {
        val all = data.birthdays.orEmpty()
        return when {
            mineOnly -> {
                val self = data.entries.firstOrNull { it.isSelf }?.employeeId ?: data.timeOff.firstOrNull { it.isSelf }?.employeeId
                if (self == null) emptyList() else all.filter { it.employeeId == self }
            }
            personId != null -> all.filter { it.employeeId == personId }
            else -> all
        }
    }
}

/** Web `splitWorkWeek` (src/lib/attendance-policy.ts): working days and the rest of the week as off days. */
object WorkWeek {
    private val NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    fun split(workdays: List<Int>?): Pair<List<String>, List<String>> {
        val days = workdays?.takeIf { it.isNotEmpty() } ?: OffDays.DEFAULT_WORKDAYS
        val (working, off) = NAMES.withIndex().partition { (i, _) -> (i + 1) in days }
        return working.map { it.value } to off.map { it.value }
    }

    /** Working / off days for the profile card: the server's lists, else computed from `workdays`. */
    fun of(p: EmployeeProfile): Pair<List<String>, List<String>> {
        val computed = split(p.workdays)
        return (p.workingDays ?: computed.first) to (p.offDays ?: computed.second)
    }
}
