package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.AppNotification
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.isVisibleNotification
import com.tertiaryinfotech.hrportal.push.routeForLink
import com.tertiaryinfotech.hrportal.ui.AuthViewModel
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.DecisionButtons
import com.tertiaryinfotech.hrportal.ui.components.NoteDialog
import com.tertiaryinfotech.hrportal.ui.components.StatusBanner
import androidx.compose.material.icons.filled.Replay
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Notifications — every alert addressed to this user (missed clock-in/out, timesheet reminders,
 * decisions, and for admins new approval requests). Tapping an item marks it read and, where a matching in-app
 * destination exists, navigates there — mirroring the web's `link`-driven click-through.
 * For an approver, a request notice (leave, MC, time off, claim, No Pay Leave payback, missed punch)
 * carries Approve / Reject itself (Alfred, 23 Nov 2026; iOS `NotificationsView`).
 */
@Composable
fun NotificationsScreen(auth: AuthViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    var reloadKey by remember { mutableStateOf(0) }
    var actingId by remember { mutableStateOf<String?>(null) }
    var rejecting by remember { mutableStateOf<AppNotification?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val canDecide = isApprover(auth) && !auth.isRolePreview

    fun decide(n: AppNotification, approve: Boolean, reason: String?) {
        val a = n.requestAction ?: return
        scope.launch {
            actingId = n.id
            error = null
            try {
                HrmsApi.decide(a, approve, reason)
                runCatching { HrmsApi.markNotificationRead(n.id) }
                auth.refreshUnreadCount()
                reloadKey++   // the server drops a decided request's notice
            } catch (e: Exception) {
                error = e.message ?: "Could not update the request."
            } finally {
                actingId = null
            }
        }
    }

    run {
        Column {
            key(reloadKey) {
                AsyncListScreen(fetch = { HrmsApi.notifications().filter { isVisibleNotification(it.type) } }) { list ->
                    if (list.isEmpty()) {
                        item { EmptyHint(Icons.Filled.NotificationsNone, "No notifications yet.") }
                    } else {
                        error?.let { item { StatusBanner(isError = true, text = it); Spacer12() } }
                        items(list) { n ->
                            val decidable = canDecide && n.requestAction != null
                            NotificationRow(
                                n,
                                actions = if (!decidable) null else { {
                                    DecisionButtons(
                                        busy = actingId == n.id,
                                        onApprove = { if (actingId == null) decide(n, true, null) },
                                        onReject = { if (actingId == null) rejecting = n },
                                    )
                                } },
                            ) {
                                scope.launch {
                                    if (!n.read) {
                                        try {
                                            HrmsApi.markNotificationRead(n.id)
                                            auth.refreshUnreadCount()
                                            reloadKey++
                                        } catch (_: Exception) { /* keep showing as unread */ }
                                    }
                                    routeForLink(n.link, n.type, isApprover(auth), auth.user?.mustClockIn == true)?.let { nav.navigate(it) }
                                }
                            }
                            Spacer12()
                        }
                    }
                }
            }
        }
    }

    rejecting?.let { n ->
        NoteDialog(
            title = "Reject request",
            message = n.message,
            confirmLabel = "Reject",
            confirmColor = Brand.Red,
            onConfirm = { reason ->
                rejecting = null
                decide(n, false, reason)
            },
            onDismiss = { rejecting = null },
        )
    }
}

@Composable
private fun NotificationRow(n: AppNotification, actions: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    val (icon, tint) = iconFor(n.type)
    Card {
      Column {
        Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.clip(CircleShape).background(tint.copy(alpha = 0.15f)).padding(8.dp),
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        n.title, color = if (n.read) Brand.TextSecondary else Brand.TextPrimary,
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f),
                    )
                    Text(timeAgo(n.createdAt), color = Brand.TextMuted, fontSize = 10.sp)
                }
                Text(n.message, color = Brand.TextSecondary, fontSize = 12.sp, maxLines = 2, modifier = Modifier.padding(top = 2.dp))
            }
            if (!n.read) {
                Box(
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp).size(8.dp).clip(CircleShape).background(Brand.Blue),
                )
            }
        }
        if (actions != null) {
            Box(modifier = Modifier.padding(start = 46.dp, top = 12.dp)) { actions() }
        }
      }
    }
}

private fun iconFor(type: String): Pair<ImageVector, Color> = when (type) {
    "LEAVE_APPROVED" -> Icons.Filled.EventAvailable to Brand.Green
    "LEAVE_REJECTED" -> Icons.Filled.EventBusy to Brand.Red
    "OT_APPROVED" -> Icons.Filled.Work to Brand.Emerald
    "OT_REJECTED" -> Icons.Filled.Work to Brand.Red
    "ATTENDANCE_MISSED", "TIMESHEET_REMINDER" -> Icons.Filled.Schedule to Brand.Amber
    "LEAVE_SUBMITTED", "TIME_OFF_SUBMITTED", "EXPENSE_SUBMITTED", "ATTENDANCE_CORRECTION_SUBMITTED" ->
        Icons.Filled.PendingActions to Brand.Accent
    "PAYBACK_SUBMITTED" -> Icons.Filled.Replay to Brand.Accent
    "PAYBACK_APPROVED" -> Icons.Filled.EventAvailable to Brand.Green
    "PAYBACK_REJECTED" -> Icons.Filled.EventBusy to Brand.Red
    "TIME_OFF_APPROVED", "EXPENSE_APPROVED", "ATTENDANCE_CORRECTION_APPROVED" -> Icons.Filled.EventAvailable to Brand.Green
    "TIME_OFF_REJECTED", "EXPENSE_REJECTED", "ATTENDANCE_CORRECTION_REJECTED" -> Icons.Filled.EventBusy to Brand.Red
    else -> Icons.Filled.Info to Brand.TextSecondary
}

private fun timeAgo(iso: String?): String {
    val d = Fmt.parse(iso) ?: return ""
    val diffMs = Date().time - d.time
    val mins = diffMs / 60000
    if (mins < 1) return "now"
    if (mins < 60) return "${mins}m"
    val hrs = mins / 60
    if (hrs < 24) return "${hrs}h"
    return "${hrs / 24}d"
}
