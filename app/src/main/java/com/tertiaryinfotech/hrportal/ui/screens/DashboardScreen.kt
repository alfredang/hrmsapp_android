package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.hrportal.data.DashboardSummary
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.ui.AuthViewModel
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.StatTile
import com.tertiaryinfotech.hrportal.ui.components.BrandScaffold
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt

/** Home tab — a snapshot pulled from /api/mobile/summary. */
@Composable
fun DashboardScreen(auth: AuthViewModel) {
    BrandScaffold(title = "Dashboard") { inner ->
        Column(modifier = Modifier.padding(inner)) {
            AsyncListScreen(fetch = { HrmsApi.summary() }) { s ->
                item { Greeting(s, auth) }
                item { Spacer18() }

                if (s.isAdmin) {
                    item { SectionTitle("Approvals queue") }
                    item { Spacer12() }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatTile(value = "${s.pendingLeaves}", label = "Pending leave",
                                icon = Icons.Filled.EventBusy, tint = Brand.Yellow, modifier = Modifier.weight(1f))
                            StatTile(value = "${s.pendingClaims}", label = "Pending claims",
                                icon = Icons.Filled.PendingActions, tint = Brand.Orange, modifier = Modifier.weight(1f))
                        }
                    }
                    item { Spacer18() }
                }

                item { SectionTitle("My balances") }
                item { Spacer12() }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(value = fmt(s.alAvailable), label = "Annual leave",
                            icon = Icons.Filled.WbSunny, tint = Brand.Sky, modifier = Modifier.weight(1f))
                        StatTile(value = fmt(s.mcAvailable), label = "Medical leave",
                            icon = Icons.Filled.LocalHospital, tint = Brand.Pink, modifier = Modifier.weight(1f))
                    }
                }
                item { Spacer12() }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(value = fmt(s.otRemaining), label = "OT leave",
                            icon = Icons.Filled.AccessTime, tint = Brand.Mint, modifier = Modifier.weight(1f))
                        StatTile(value = Fmt.money(s.expenseYtd), label = "Expenses YTD",
                            icon = Icons.Filled.CreditCard, tint = Brand.Green, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun Greeting(s: DashboardSummary, auth: AuthViewModel) {
    Card {
        Text("Welcome back", color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp)
        Text(
            s.name ?: auth.user?.displayName ?: "Employee",
            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp,
        )
        s.role?.let { role ->
            Text(
                role.replaceFirstChar { it.uppercase() },
                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

private fun fmt(v: Double?): String = if (v == null) "—" else Fmt.num(v)
