package com.tertiaryinfotech.hrportal.push

/**
 * Best-effort mapping from a notification's web `link` (and type) to an in-app route — used by the
 * Notifications list and by a tapped push alert. Approval requests open Approvals for admins.
 */
fun routeForLink(link: String?, type: String? = null, isApprover: Boolean = false): String? {
    if (isApprover && (type?.endsWith("_SUBMITTED") == true || link?.startsWith("/approvals") == true)) return "approvals"
    return when {
        link == null -> null
        link.startsWith("/approvals") -> "notifications"
        link.startsWith("/leave") -> "leave"
        link.startsWith("/expenses") -> "expenses"
        link.startsWith("/calendar") -> "calendar"
        // Missing-punch submissions point admins at the merged Timesheet page.
        link.startsWith("/timesheet/overview") || link.startsWith("/attendance/overview") -> if (isApprover) "timesheet_admin" else "timesheet"
        link.startsWith("/timesheet") -> "timesheet_week"
        link.startsWith("/attendance") -> "timesheet"
        link.startsWith("/time-off") -> "time_off"
        else -> null
    }
}
