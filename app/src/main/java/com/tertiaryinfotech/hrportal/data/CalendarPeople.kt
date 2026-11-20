package com.tertiaryinfotech.hrportal.data

/**
 * One choice in the calendar's "Leave & time off for" list. [intern] is null when the server did not
 * say. [leaveCount] counts the person's leave and time off entries (web `calendar-view.tsx`, 1.16).
 */
data class CalendarPersonOption(val id: String, val name: String, val intern: Boolean?, val leaveCount: Int)

/** The people an approver can filter the team calendar by, grouped as the web calendar groups them. */
data class CalendarPeopleList(
    val staff: List<CalendarPersonOption>,
    val interns: List<CalendarPersonOption>,
    /** People whose group is unknown (an older server that sends neither `people` nor `group`). */
    val others: List<CalendarPersonOption>,
    /** Every leave and time-off entry, including those of people who have left (shown under Everyone only). */
    val totalEntries: Int,
) {
    val all: List<CalendarPersonOption> get() = staff + interns + others
    fun find(id: String?): CalendarPersonOption? = if (id == null) null else all.firstOrNull { it.id == id }
}

/**
 * Pure logic for the team calendar's person filter (no Android types: unit-tested on the JVM).
 * Same rules as the web `calendar-view.tsx`: every ACTIVE staff member and intern is listed, with
 * or without leave, with their number of leave entries; people who have left are not listed.
 */
object CalendarPeople {
    /**
     * Null means "show no filter" (the viewer is not an approver).
     *
     * `people` comes from the server for approvers. When it is absent (an older server), an approver
     * still gets a list built from the leave entries themselves, which then only holds people with
     * leave. An approver is recognised by `canSeeTypes` or by a `group` on any entry.
     */
    fun build(data: TeamCalendarResponse): CalendarPeopleList? {
        val counts = (data.entries.map { it.employeeId } + data.timeOff.map { it.employeeId })
            .filter { it.isNotEmpty() }.groupingBy { it }.eachCount()
        val options: List<CalendarPersonOption> = when {
            data.people != null -> data.people
                .filter { it.id.isNotEmpty() }
                .distinctBy { it.id }
                .map { CalendarPersonOption(it.id, it.name, it.intern, counts[it.id] ?: 0) }
            data.canSeeTypes || data.entries.any { it.group != null } -> data.entries
                .filter { it.employeeId.isNotEmpty() }
                .distinctBy { it.employeeId }
                .map { e ->
                    val intern = when (e.group) { "intern" -> true; "staff" -> false; else -> null }
                    CalendarPersonOption(e.employeeId, e.employeeName, intern, counts[e.employeeId] ?: 0)
                }
            else -> return null
        }
        val sorted = options.sortedBy { it.name.lowercase() }
        return CalendarPeopleList(
            staff = sorted.filter { it.intern == false },
            interns = sorted.filter { it.intern == true },
            others = sorted.filter { it.intern == null },
            totalEntries = data.entries.size + data.timeOff.size,
        )
    }

    /** Leave for one person, or everything when [personId] is null. Holidays are never filtered. */
    fun filter(entries: List<TeamLeaveEntry>, personId: String?): List<TeamLeaveEntry> =
        if (personId == null) entries else entries.filter { it.employeeId == personId }
}
