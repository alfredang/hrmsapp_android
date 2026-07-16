package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.DashboardSummary
import com.tertiaryinfotech.hrportal.data.ExpenseClaim
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.ui.AuthViewModel
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.StatTile
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.ui.theme.IconTint
import com.tertiaryinfotech.hrportal.util.Fmt

/**
 * Home tab — mirrors the web dashboard (`(dashboard)/dashboard/page.tsx`): a welcome header, KPI
 * balance cards, Quick Actions, and Recent Activity (`quick-actions.tsx`, `recent-activity.tsx`).
 * Recent Activity is derived client-side from `/api/mobile/expenses` (already-deployed endpoint)
 * rather than a new backend field, since the web's own version is just the last 5 expense claims.
 */
private data class DashboardData(val summary: DashboardSummary, val recentClaims: List<ExpenseClaim>)

@Composable
fun DashboardScreen(auth: AuthViewModel, nav: NavController) {
    AsyncListScreen(fetch = { DashboardData(HrmsApi.summary(), HrmsApi.expenses().claims) }) { d ->
        val s = d.summary
        item { Greeting(s, auth) }
        item { Spacer18() }

        item { SectionTitle("My balances") }
        item { Spacer12() }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(value = fmt(s.alAvailable), label = "Annual Leave",
                    icon = Icons.Filled.WbSunny, tint = IconTint.Blue.icon, iconBg = IconTint.Blue.bg,
                    modifier = Modifier.weight(1f))
                StatTile(value = fmt(s.mcAvailable), label = "Medical Leave",
                    icon = Icons.Filled.LocalHospital, tint = IconTint.Red.icon, iconBg = IconTint.Red.bg,
                    modifier = Modifier.weight(1f))
            }
        }
        item { Spacer12() }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(value = Fmt.money(s.expenseYtd), label = "Expense Claims",
                    icon = Icons.Filled.CreditCard, tint = IconTint.Green.icon, iconBg = IconTint.Green.bg,
                    modifier = Modifier.weight(1f))
                StatTile(value = fmt(s.otRemaining), label = "Off In Lieu",
                    icon = Icons.Filled.AccessTime, tint = IconTint.Emerald.icon, iconBg = IconTint.Emerald.bg,
                    modifier = Modifier.weight(1f))
            }
        }
        item { Spacer18() }

        item { SectionTitle("Quick Actions") }
        item { Spacer12() }
        item {
            Column {
                QuickActionRow("Request Leave", "Submit time off request", Icons.Filled.Schedule, Brand.Amber) {
                    switchTab(nav, "leave")
                }
                Spacer12()
                QuickActionRow("Submit Expense", "Claim reimbursement", Icons.Filled.AttachMoney, Brand.Green) {
                    switchTab(nav, "expenses")
                }
                Spacer12()
                QuickActionRow("View Calendar", "Check upcoming events", Icons.Filled.CalendarMonth, Brand.Red) {
                    switchTab(nav, "calendar")
                }
            }
        }
        item { Spacer18() }

        item { RecentActivityCard(d.recentClaims, s.name ?: auth.user?.displayName ?: "Employee") }
        item { Spacer18() }
        item {
            Text(
                "Powered by Tertiary Infotech Academy Pte Ltd",
                color = Brand.TextFaint, fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

private fun switchTab(nav: NavController, route: String) {
    nav.navigate(route) {
        popUpTo(nav.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun Greeting(s: DashboardSummary, auth: AuthViewModel) {
    Column {
        Text(
            "Welcome Back, ${s.name ?: auth.user?.displayName ?: "Employee"}",
            color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 26.sp,
        )
        Text(
            "Here's what's happening in your organization",
            color = Brand.TextSecondary, fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Mirrors `quick-actions.tsx`'s staff action list: solid-color icon tile, title, subtitle, chevron. */
@Composable
private fun QuickActionRow(title: String, subtitle: String, icon: ImageVector, tileColor: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(tileColor).padding(10.dp)) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text(title, color = Brand.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(subtitle, color = Brand.TextSecondary, fontSize = 12.sp)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Brand.TextMuted)
        }
    }
}

/** Mirrors `recent-activity.tsx` exactly: `bg-gray-950` card, a `CardTitle` heading, then the last
 *  5 expense claims as {name} / {amount} - {category} / date ("10 Jun", no year) spaced with
 *  `space-y-4` (16dp gaps, no row dividers) and a translucent `bg-amber-900/30` rounded-square
 *  icon tile rather than a bright pastel circle. */
@Composable
private fun RecentActivityCard(claims: List<ExpenseClaim>, userName: String) {
    val recent = claims.take(5)
    Card(containerColor = Brand.Background, padding = 20) {
        Text("Recent Activity", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        if (recent.isEmpty()) {
            Text(
                "No recent activity", color = Brand.TextSecondary, fontSize = 13.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
            )
        } else {
            Spacer(Modifier.height(16.dp))
            recent.forEachIndexed { i, c ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brand.Amber.copy(alpha = 0.18f))
                            .padding(8.dp),
                    ) {
                        Icon(Icons.Filled.AttachMoney, contentDescription = null, tint = Brand.Amber, modifier = Modifier.size(16.dp))
                    }
                    Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(userName, color = Brand.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1)
                        Text(
                            "${Fmt.money(c.amount, c.currency)} - ${c.category ?: ""}",
                            color = Brand.TextSecondary, fontSize = 11.sp,
                        )
                    }
                    Text(Fmt.dayMonth(c.createdAt), color = Brand.TextMuted, fontSize = 10.sp)
                }
                if (i < recent.size - 1) Spacer(Modifier.height(16.dp))
            }
        }
    }
}

private fun fmt(v: Double?): String = if (v == null) "—" else Fmt.num(v)
