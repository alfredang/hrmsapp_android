package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.LeaveBalance
import com.tertiaryinfotech.hrportal.data.LeaveRequest
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.StatusPill
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt

/** Leave tab (iOS `LeaveView`): every balance and every request, whatever the leave type, so No Pay
 *  and Compassionate Leave show up here after applying. */
@Composable
fun LeaveScreen(nav: NavController) {
    var reloadKey by remember { mutableStateOf(0) }

    // ApplyLeaveScreen stashes a flag on this entry's saved state before popping back, which forces
    // a refetch here.
    val savedStateHandle = nav.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getStateFlow("leave_applied", false)?.collect { applied ->
            if (applied) {
                reloadKey++
                savedStateHandle.set("leave_applied", false)
            }
        }
    }

    key(reloadKey) {
        AsyncListScreen(fetch = { HrmsApi.leave() }) { data ->
            item {
                PremierButton(title = "Apply for Leave", icon = Icons.Filled.AddCircle) {
                    nav.navigate("leave_request/AL")
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
                item { Spacer(6) }
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

@Composable
private fun Spacer(height: Int) = androidx.compose.foundation.layout.Spacer(Modifier.padding(top = height.dp))

@Composable
private fun BalanceCard(b: LeaveBalance) {
    // No Pay Leave: the server sends the prorated cap as proRated and what's left as available.
    if (b.code == "NPL" && b.proRated != null) {
        NplBalanceCard(b, cap = b.proRated)
        return
    }
    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(b.name, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(if (b.paid) "Paid" else "Unpaid", color = Brand.TextPrimary.copy(alpha = 0.6f), fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    Fmt.days(b.available),
                    color = if (b.available < 0) Brand.Red else Brand.Accent,
                    fontWeight = FontWeight.Bold, fontSize = 20.sp,
                )
                Text("available", color = Brand.TextPrimary.copy(alpha = 0.6f), fontSize = 11.sp)
            }
        }
        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MiniStat("Used", b.used)
            MiniStat("Pending", b.pending)
            if (b.carriedOver > 0) MiniStat("Carried", b.carriedOver)
        }
    }
}

/** No Pay Leave: days left against the cap (21 a year, prorated by full months served). */
@Composable
private fun NplBalanceCard(b: LeaveBalance, cap: Double) {
    val remaining = b.available
    val usedShare = if (cap > 0) ((cap - remaining) / cap).coerceIn(0.0, 1.0).toFloat() else 1f
    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(b.name, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("Unpaid \u00b7 capped at ${Fmt.num(cap)} days to date", color = Brand.TextPrimary.copy(alpha = 0.6f), fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${Fmt.num(remaining)} of ${Fmt.num(cap)}",
                    color = if (remaining <= 0) Brand.Red else Brand.Accent,
                    fontWeight = FontWeight.Bold, fontSize = 20.sp,
                )
                Text("days left", color = Brand.TextPrimary.copy(alpha = 0.6f), fontSize = 11.sp)
            }
        }
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Brand.TextPrimary.copy(alpha = 0.12f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(usedShare)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(if (remaining <= 0) Brand.Red else Brand.Amber),
            )
        }
        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MiniStat("Cap", cap)
            MiniStat("Used", b.used)
            MiniStat("Pending", b.pending)
            MiniStat("Left", remaining)
        }
        Text(
            "No Pay Leave is capped at 21 days a year, prorated by the full months you have served.",
            color = Brand.TextPrimary.copy(alpha = 0.55f), fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun MiniStat(label: String, v: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = Brand.TextPrimary.copy(alpha = 0.55f), fontSize = 11.sp)
        Text(Fmt.num(v), color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun RequestRow(r: LeaveRequest) {
    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(r.leaveType, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("${Fmt.date(r.startDate)} → ${Fmt.date(r.endDate)}", color = Brand.TextSecondary, fontSize = 12.sp)
                if (!r.reason.isNullOrEmpty()) {
                    Text(r.reason, color = Brand.TextPrimary.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 2)
                }
                if (!r.rejectionReason.isNullOrEmpty()) {
                    Text(r.rejectionReason, color = Brand.Red.copy(alpha = 0.9f), fontSize = 11.sp, maxLines = 2)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill(r.status)
                Text(Fmt.days(r.days), color = Brand.TextSecondary, fontSize = 11.sp)
            }
        }
    }
}
