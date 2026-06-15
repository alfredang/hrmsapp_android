package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.LeaveBalance
import com.tertiaryinfotech.hrportal.data.LeaveRequest
import com.tertiaryinfotech.hrportal.data.LeaveResponse
import com.tertiaryinfotech.hrportal.data.LeaveType
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.BrandScaffold
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.StatusPill
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt

/** Leave tab — balances, request history, and apply-for-leave. */
@Composable
fun LeaveScreen() {
    var showApply by remember { mutableStateOf(false) }
    var applyTypes by remember { mutableStateOf<List<LeaveType>>(emptyList()) }
    var reloadKey by remember { mutableStateOf(0) }

    BrandScaffold(
        title = "Leave",
        actions = {
            IconButton(onClick = { showApply = applyTypes.isNotEmpty() }) {
                Icon(Icons.Filled.AddCircle, contentDescription = "Apply for leave", tint = Brand.Sky)
            }
        },
    ) { inner ->
        Column(modifier = Modifier.padding(inner)) {
            // reloadKey forces AsyncListScreen to refetch after a successful apply.
            key(reloadKey) {
                AsyncListScreen(fetch = {
                    val data = HrmsApi.leave()
                    applyTypes = data.types
                    data
                }) { data ->
                    item {
                        PremierButton(title = "Apply for Leave", icon = Icons.Filled.AddCircle) {
                            applyTypes = data.types
                            showApply = true
                        }
                    }
                    item { Spacer18() }

                    if (data.balances.isNotEmpty()) {
                        item { SectionTitle("Balances") }
                        item { Spacer12() }
                        items(data.balances) { b ->
                            BalanceCard(b)
                            Spacer12()
                        }
                    }

                    item { SectionTitle("My requests") }
                    item { Spacer12() }
                    if (data.requests.isEmpty()) {
                        item { EmptyHint(Icons.Filled.CalendarToday, "No leave requests yet.") }
                    } else {
                        items(data.requests) { r ->
                            RequestRow(r)
                            Spacer12()
                        }
                    }
                }
            }
        }
    }

    if (showApply) {
        ApplyLeaveSheet(
            types = applyTypes,
            onDismiss = { showApply = false },
            onApplied = { showApply = false; reloadKey++ },
        )
    }
}

@Composable
private fun BalanceCard(b: LeaveBalance) {
    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(b.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(if (b.paid) "Paid" else "Unpaid", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Fmt.days(b.available),
                    color = if (b.available < 0) Brand.Red else Brand.Sky,
                    fontWeight = FontWeight.Bold, fontSize = 18.sp,
                )
                Text("available", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            }
        }
        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MiniStat("Used", b.used)
            MiniStat("Pending", b.pending)
            if (b.carriedOver > 0) MiniStat("Carried", b.carriedOver)
        }
    }
}

@Composable
private fun MiniStat(label: String, v: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
        Text(Fmt.num(v), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun RequestRow(r: LeaveRequest) {
    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(r.leaveType, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("${Fmt.date(r.startDate)} → ${Fmt.date(r.endDate)}",
                    color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                if (!r.reason.isNullOrEmpty()) {
                    Text(r.reason, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, maxLines = 2)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill(r.status)
                Text(Fmt.days(r.days), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }
}
