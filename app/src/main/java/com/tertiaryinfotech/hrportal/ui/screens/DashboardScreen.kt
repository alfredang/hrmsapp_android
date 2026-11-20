package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.ActiveCounts
import com.tertiaryinfotech.hrportal.data.AnalyticsCalc
import com.tertiaryinfotech.hrportal.data.DashboardSummary
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.ui.AuthViewModel
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.Kpi
import com.tertiaryinfotech.hrportal.ui.components.KpiStrip
import com.tertiaryinfotech.hrportal.ui.components.StatTile
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Home tab: a snapshot from /api/mobile/summary (iOS `DashboardView`). */
@Composable
fun DashboardScreen(auth: AuthViewModel, nav: NavController) {
    // Approvers also load the active staff and intern counts (from Analytics) for the three tiles.
    // That call is optional: when it fails or an older server has no counts, the tiles fall back.
    val approver = isApprover(auth)
    AsyncListScreen(fetch = {
        coroutineScope {
            val counts = if (approver) async { activeCountsOrNull() } else null
            HomeData(HrmsApi.summary(), counts?.await())
        }
    }) { home ->
        val s = home.summary
        item { Greeting(s, auth) }
        item { Spacer18() }

        // ADMIN/HR/MANAGER only, enforced server-side.
        if (s.isAdmin) {
            val counts = home.counts
            val pending = s.pendingLeaves + s.pendingClaims
            if (counts != null) {
                // Web /dashboard (20 Nov 2026): full-time staff, interns, pending approvals.
                item { SectionTitle("Overview") }
                item { Spacer12() }
                item {
                    KpiStrip(
                        listOf(
                            Kpi("${counts.staff}", "Full-time staff", Brand.TextPrimary) { openTab(nav, "team") },
                            Kpi("${counts.interns}", "Interns", Brand.TextPrimary) { openTab(nav, "team") },
                            Kpi("$pending", "Pending approvals", if (pending > 0) Brand.Amber else Brand.TextPrimary) { nav.navigate("approvals") },
                        ),
                    )
                }
            } else {
                item { SectionTitle("Approvals queue") }
                item { Spacer12() }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(
                            value = "${s.pendingLeaves}", label = "Pending leave",
                            icon = Icons.AutoMirrored.Filled.EventNote, tint = Brand.Amber,
                            modifier = Modifier.weight(1f), onClick = { nav.navigate("approvals") },
                        )
                        StatTile(
                            value = "${s.pendingClaims}", label = "Pending claims",
                            icon = Icons.Filled.PendingActions, tint = Brand.Orange,
                            modifier = Modifier.weight(1f), onClick = { nav.navigate("approvals") },
                        )
                    }
                }
            }
            item { Spacer18() }
        }

        item { SectionTitle("Quick actions") }
        item { Spacer12() }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction("Clock in / out", Icons.Filled.AccessTimeFilled, Brand.Amber, Modifier.weight(1f)) {
                    nav.navigate("timesheet")
                }
                QuickAction("Snap a claim", Icons.Filled.PhotoCamera, Brand.Orange, Modifier.weight(1f)) {
                    nav.navigate("expense_new")
                }
            }
        }
        item { Spacer18() }

        item { SectionTitle("My balances") }
        item { Spacer12() }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(fmt(s.alAvailable), "Annual leave", Modifier.weight(1f), Icons.Filled.WbSunny, Brand.Accent)
                StatTile(fmt(s.mcAvailable), "Medical leave", Modifier.weight(1f), Icons.Filled.MedicalServices, Brand.Pink)
            }
        }
        item { Spacer12() }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(fmt(s.otRemaining), "Off In Lieu", Modifier.weight(1f), Icons.Filled.AccessTime, Brand.Mint)
                StatTile(Fmt.money(s.expenseYtd), "Expenses YTD", Modifier.weight(1f), Icons.Filled.CreditCard, Brand.Green)
            }
        }
    }
}

private data class HomeData(val summary: DashboardSummary, val counts: ActiveCounts?)

private suspend fun activeCountsOrNull(): ActiveCounts? = try {
    AnalyticsCalc.activeCounts(HrmsApi.analytics())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}

/** Switch to a bottom tab from Home, the way the tab bar does. */
private fun openTab(nav: NavController, route: String) {
    nav.navigate(route) {
        popUpTo(nav.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun Greeting(s: DashboardSummary, auth: AuthViewModel) {
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Welcome back", color = Brand.TextPrimary.copy(alpha = 0.75f), fontSize = 15.sp)
            Text(
                s.name ?: auth.user?.displayName ?: "Employee",
                color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp,
            )
            s.role?.takeIf { it.isNotBlank() }?.let { role ->
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Brand.SurfaceStrong)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        role.lowercase().replaceFirstChar { it.uppercase() },
                        color = Brand.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAction(title: String, icon: ImageVector, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier, onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Text(title, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

private fun fmt(v: Double?): String = if (v == null) "—" else Fmt.num(v)
