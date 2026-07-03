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
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Work
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
import com.tertiaryinfotech.hrportal.data.STAFF_NOTIFICATION_TYPES
import com.tertiaryinfotech.hrportal.ui.AuthViewModel
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.BrandScaffold
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Notifications — staff/intern-relevant types only (mirrors `EMPLOYEE_TYPES` filtering in the
 * web's `notification-bell.tsx`). Tapping an item marks it read and, where a matching in-app
 * destination exists, navigates there — mirroring the web's `link`-driven click-through.
 */
@Composable
fun NotificationsScreen(auth: AuthViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    var reloadKey by remember { mutableStateOf(0) }

    BrandScaffold(title = "Notifications", onBack = { nav.popBackStack() }) { inner ->
        Column(modifier = Modifier.padding(inner)) {
            key(reloadKey) {
                AsyncListScreen(fetch = { HrmsApi.notifications().filter { it.type in STAFF_NOTIFICATION_TYPES } }) { list ->
                    if (list.isEmpty()) {
                        item { EmptyHint(Icons.Filled.NotificationsNone, "No notifications yet.") }
                    } else {
                        items(list) { n ->
                            NotificationRow(n) {
                                scope.launch {
                                    if (!n.read) {
                                        try {
                                            HrmsApi.markNotificationRead(n.id)
                                            auth.refreshUnreadCount()
                                            reloadKey++
                                        } catch (_: Exception) { /* keep showing as unread */ }
                                    }
                                    routeFor(n.link)?.let { nav.navigate(it) }
                                }
                            }
                            Spacer12()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: AppNotification, onClick: () -> Unit) {
    val (icon, tint) = iconFor(n.type)
    Card(modifier = Modifier.clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.clip(CircleShape).background(tint.copy(alpha = 0.15f)).padding(8.dp),
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        n.title, color = if (n.read) Brand.TextSecondary else Color.White,
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
    }
}

private fun iconFor(type: String): Pair<ImageVector, Color> = when (type) {
    "LEAVE_APPROVED" -> Icons.Filled.EventAvailable to Brand.Green
    "LEAVE_REJECTED" -> Icons.Filled.EventBusy to Brand.Red
    "OT_APPROVED" -> Icons.Filled.Work to Brand.Emerald
    "OT_REJECTED" -> Icons.Filled.Work to Brand.Red
    "WOODS_SQUARE_APPROVED" -> Icons.Filled.MeetingRoom to Brand.Emerald
    "WOODS_SQUARE_DECLINED" -> Icons.Filled.MeetingRoom to Brand.Red
    else -> Icons.Filled.Info to Brand.TextSecondary
}

/** Best-effort mapping from the web's notification `link` (a web route path) to an in-app route. */
private fun routeFor(link: String?): String? = when {
    link == null -> null
    link.startsWith("/leave") -> "leave"
    link.startsWith("/expenses") -> "expenses"
    link.startsWith("/calendar") -> "calendar"
    link.startsWith("/woods-square") -> "woods_square"
    link.startsWith("/timesheet") -> "timesheet"
    else -> null
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
