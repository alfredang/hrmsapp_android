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
import com.tertiaryinfotech.hrportal.data.DashboardSummary
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.ui.AuthViewModel
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.StatTile
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt

/** Home tab: a snapshot from /api/mobile/summary (iOS `DashboardView`). */
@Composable
fun DashboardScreen(auth: AuthViewModel, nav: NavController) {
    AsyncListScreen(fetch = { HrmsApi.summary() }) { s ->
        item { Greeting(s, auth) }
        item { Spacer18() }

        // ADMIN/HR/MANAGER only, enforced server-side.
        if (s.isAdmin) {
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
                        .background(if (Brand.isDark) Color.White.copy(alpha = 0.16f) else Brand.SurfaceStrong)
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
