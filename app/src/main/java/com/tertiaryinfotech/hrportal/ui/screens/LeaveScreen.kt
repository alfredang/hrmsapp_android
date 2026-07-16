package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.draw.clip
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
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.StatusPill
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.ui.theme.StatusTint
import com.tertiaryinfotech.hrportal.util.Fmt
import androidx.navigation.NavController

private val LEAVE_TABS = listOf("AL" to "Annual Leave", "MC" to "Medical Leave")

/** Leave tab — Annual/Medical tabs (mirrors the web's `/leave/annual` + `/leave/medical` pages),
 *  balances, request history, and apply-for-leave. "Apply for Leave" navigates to
 *  [ApplyLeaveScreen] as a real destination (not a dialog) so it shares this app's normal
 *  hamburger-drawer top bar, matching the web's own page-not-modal presentation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveScreen(nav: NavController) {
    var selectedCode by remember { mutableStateOf("AL") }
    var reloadKey by remember { mutableStateOf(0) }

    // ApplyLeaveScreen is a real destination now (not a dialog), so it stashes a result flag on
    // this screen's own saved-state handle before popping back — the standard Navigation Compose
    // "return a result" pattern — which we turn into a `key()`-forced AsyncListScreen refetch.
    val savedStateHandle = nav.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getStateFlow("leave_applied", false)?.collect { applied ->
            if (applied) {
                reloadKey++
                savedStateHandle.set("leave_applied", false)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            LEAVE_TABS.forEachIndexed { i, (code, label) ->
                SegmentedButton(
                    selected = selectedCode == code,
                    onClick = { selectedCode = code },
                    shape = SegmentedButtonDefaults.itemShape(i, LEAVE_TABS.size),
                ) { Text(label) }
            }
        }
        key(reloadKey) {
        AsyncListScreen(fetch = { HrmsApi.leave() }) { data ->
            val balances = data.balances.filter { it.code == selectedCode }
            val requests = data.requests.filter { it.leaveCode == selectedCode }

            item {
                PremierButton(title = if (selectedCode == "MC") "Request MC" else "Apply for Leave", icon = Icons.Filled.AddCircle) {
                    nav.navigate("leave_request/$selectedCode")
                }
            }
            item { Spacer18() }

            if (balances.isNotEmpty()) {
                item { SectionTitle("Balances") }
                item { Spacer12() }
                items(balances) { b ->
                    BalanceCard(b)
                    Spacer12()
                }
            }

            item { SectionTitle("My requests") }
            item { Spacer12() }
            if (requests.isEmpty()) {
                item { EmptyHint(Icons.Filled.CalendarToday, "No leave requests yet.") }
            } else {
                items(requests) { r ->
                    RequestRow(r)
                    Spacer12()
                }
            }
        }
        } // end key(reloadKey)
    }
}

@Composable
private fun BalanceCard(b: LeaveBalance) {
    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(b.name, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                if (b.paid) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(StatusTint.Green.bg)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text("Paid Leave", color = StatusTint.Green.text, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("Unpaid", color = Brand.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Fmt.days(b.available),
                    color = if (b.available < 0) Brand.Red else Brand.Primary,
                    fontWeight = FontWeight.Bold, fontSize = 18.sp,
                )
                Text("available", color = Brand.TextSecondary, fontSize = 11.sp)
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
        Text(label, color = Brand.TextSecondary, fontSize = 11.sp)
        Text(Fmt.num(v), color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun RequestRow(r: LeaveRequest) {
    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(r.leaveType, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("${Fmt.date(r.startDate)} → ${Fmt.date(r.endDate)}",
                    color = Brand.TextSecondary, fontSize = 12.sp)
                if (!r.reason.isNullOrEmpty()) {
                    Text(r.reason, color = Brand.TextMuted, fontSize = 11.sp, maxLines = 2)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill(r.status)
                Text(Fmt.days(r.days), color = Brand.TextSecondary, fontSize = 11.sp)
            }
        }
    }
}
