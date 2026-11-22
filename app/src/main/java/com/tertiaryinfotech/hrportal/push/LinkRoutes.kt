package com.tertiaryinfotech.hrportal.push

/**
 * Best-effort mapping from a notification's web `link` (and type) to an in-app route — used by the
 * Notifications list and by a tapped push alert. Approval requests open Approvals for admins.
 * [mustClockIn] (interns and accountants, `SessionUser.mustClockIn`): a "/timesheet" link opens their
 * own Timesheet (`my_timesheet`); "/attendance" always opens the clock screen.
 * A query (`?request=<id>`, which the server appends to approval-request links) or a `#fragment` is
 * dropped before matching, as iOS `PushRoute` does.
 */
fun routeForLink(rawLink: String?, type: String? = null, isApprover: Boolean = false, mustClockIn: Boolean = false): String? {
    val link = rawLink?.substringBefore('?')?.substringBefore('#')
    if (isApprover && (type?.endsWith("_SUBMITTED") == true || link?.startsWith("/approvals") == true)) return "approvals"
    return when {
        link == null -> null
        link.startsWith("/approvals") -> "notifications"
        link.startsWith("/leave") -> "leave"
        link.startsWith("/expenses") -> "expenses"
        link.startsWith("/calendar") -> "calendar"
        // Missing-punch submissions point admins at the merged Timesheet page.
        link.startsWith("/timesheet/overview") || link.startsWith("/attendance/overview") -> if (isApprover) "timesheet_admin" else "timesheet"
        link.startsWith("/timesheet") -> if (mustClockIn) "my_timesheet" else "timesheet_week"
        link.startsWith("/attendance") -> "timesheet"
        link.startsWith("/time-off") -> "time_off"
        else -> null
    }
}
