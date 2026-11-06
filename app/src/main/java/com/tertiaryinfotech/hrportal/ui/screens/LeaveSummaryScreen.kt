package com.tertiaryinfotech.hrportal.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.LeaveSummary
import com.tertiaryinfotech.hrportal.data.LeaveSummaryEmployee
import com.tertiaryinfotech.hrportal.ui.components.RefreshableColumn
import com.tertiaryinfotech.hrportal.ui.components.AsyncContent
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.LoadState
import com.tertiaryinfotech.hrportal.ui.components.StatTile
import com.tertiaryinfotech.hrportal.ui.components.StatusPill
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import java.util.Locale

/** "1.5" / "2" — trims a trailing ".0" from day/hour counts. */
private fun days(v: Double): String =
    if (v % 1.0 == 0.0) String.format(Locale.US, "%.0f", v) else String.format(Locale.US, "%.1f", v)


/**
 * Admin: the month's leave + MC roll-up of every active staff member and intern — totals up top,
 * one row per person, drilling into that person's requests and year balances. Same data as the
 * AI-agent report (`/api/agent/leave-summary`). Mirrors iOS `LeaveSummaryView`.
 */
@Composable
fun LeaveSummaryScreen(nav: NavController) {
    var month by rememberSaveable { mutableStateOf(AttendanceMonth.current()) }
    var interns by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<LoadState<LeaveSummary>>(LoadState.Idle) }

    suspend fun load() {
        if (state !is LoadState.Loaded) state = LoadState.Loading
        state = try {
            LoadState.Loaded(HrmsApi.leaveSummary(month))
        } catch (e: Exception) {
            LoadState.Failed(e.message ?: "Could not load.")
        }
    }

    LaunchedEffect(month) { if (state !is LoadState.Idle) load() }

    AsyncContent(state = state, load = { load() }) { data ->
        val t = data.totals
        val al = t.byLeaveType.firstOrNull { it.code == "AL" }
        val pending = t.byLeaveType.sumOf { it.pendingDays }
        val rows = data.employees.filter { it.isIntern == interns }

        RefreshableColumn(scope, { load() }) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Leave & MC", color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
                    modifier = Modifier.weight(1f))
                MonthSwitcher(month) { month = it }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    value = "${t.headcount.total}", label = "People",
                    icon = Icons.Outlined.Groups, tint = Brand.Accent,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = days(t.mc.approvedDays), label = "MC days · ${t.mc.employeesOnMc} on MC",
                    icon = Icons.Outlined.MedicalServices, tint = Brand.Pink,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    value = days(al?.approvedDays ?: 0.0), label = "AL days",
                    icon = Icons.Outlined.BeachAccess, tint = Brand.Orange,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = days(pending), label = "Pending days",
                    icon = Icons.Outlined.HourglassTop, tint = Brand.Amber,
                    modifier = Modifier.weight(1f),
                )
            }
            if (t.timeOff.approvedHours > 0 || t.timeOff.pendingHours > 0) {
                Text(
                    "Intern time-off: ${days(t.timeOff.approvedHours)} h approved" +
                        if (t.timeOff.pendingHours > 0) " · ${days(t.timeOff.pendingHours)} h pending" else "",
                    color = Brand.TextSecondary, fontSize = 12.sp,
                )
            }

            SegmentedPicker(
                options = listOf(
                    "staff" to "Staff (${t.headcount.staff})",
                    "interns" to "Interns (${t.headcount.interns})",
                ),
                selected = if (interns) "interns" else "staff",
                onSelect = { interns = it == "interns" },
            )

            if (rows.isEmpty()) {
                EmptyHint(Icons.Outlined.EventNote, "No ${if (interns) "interns" else "staff"} for ${AttendanceMonth.label(month)}.")
            }
            rows.forEach { e ->
                SummaryRow(e) {
                    nav.navigate("mc_leave_summary_detail/${Uri.encode(e.employeeId)}?month=$month")
                }
            }
            Text(
                "Working days within the month. Leave spanning two months is split between them.",
                color = Brand.TextMuted, fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun SummaryRow(e: LeaveSummaryEmployee, onClick: () -> Unit) {
    Card(modifier = Modifier.clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(e.name, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("${e.employeeId} · ${if (e.isIntern) "Intern" else "Staff"}",
                    color = Brand.TextSecondary, fontSize = 12.sp)
                val parts = buildList {
                    add("MC ${days(e.approved("MC"))}")
                    add("AL ${days(e.approved("AL"))}")
                    if (e.otherApproved > 0) add("Other ${days(e.otherApproved)}")
                    e.timeOff?.let { if (it.approvedHours > 0) add("Time-off ${days(it.approvedHours)} h") }
                }
                Text(parts.joinToString("  ·  "), color = Brand.TextMuted, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val mc = e.approved("MC")
                if (mc > 0) Text("${days(mc)} d MC", color = Brand.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (e.pendingDays > 0) StatusPill("PENDING")
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Brand.TextMuted)
        }
    }
}

/** One person's leave requests for the month plus their year balances (admin drill-down). */
@Composable
fun LeaveSummaryDetailScreen(employeeId: String, month: String) {
    var state by remember { mutableStateOf<LoadState<LeaveSummary>>(LoadState.Idle) }

    AsyncContent(state = state, load = {
        state = LoadState.Loading
        state = try {
            LoadState.Loaded(HrmsApi.leaveSummary(month))
        } catch (e: Exception) {
            LoadState.Failed(e.message ?: "Could not load.")
        }
    }) { data ->
        val e = data.employees.firstOrNull { it.employeeId == employeeId }
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (e == null) {
                EmptyHint(Icons.Outlined.Groups, "This person is not in the ${AttendanceMonth.label(month)} summary.")
                return@Column
            }
            Text(e.name, color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(
                listOfNotNull(e.employeeId, if (e.isIntern) "Intern" else "Staff", e.position, e.department)
                    .joinToString(" · "),
                color = Brand.TextSecondary, fontSize = 13.sp,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                fun available(code: String) = e.balances.firstOrNull { it.code == code }?.let { days(it.available) } ?: "\u2014"
                StatTile(available("AL"), "AL available", Modifier.weight(1f), Icons.Outlined.BeachAccess, Brand.Orange)
                StatTile(available("MC"), "MC available", Modifier.weight(1f), Icons.Outlined.MedicalServices, Brand.Pink)
            }

            Text("${AttendanceMonth.label(month)} leave", color = Brand.TextPrimary,
                fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
            if (e.requests.isEmpty() && e.timeOff == null) {
                EmptyHint(Icons.Outlined.EventNote, "No leave or time-off this month.")
            }
            e.requests.forEach { r ->
                Card {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(r.leaveTypeName.ifEmpty { r.leaveType }, color = Brand.TextPrimary,
                                fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            val range = if (r.startDate == r.endDate) AttendanceMonth.dayLabel(r.startDate)
                                else "${AttendanceMonth.dayLabel(r.startDate)} – ${AttendanceMonth.dayLabel(r.endDate)}"
                            Text(range, color = Brand.TextSecondary, fontSize = 12.sp)
                            val note = buildList {
                                if (r.daysInMonth != r.totalDays) add("${days(r.totalDays)} d in total")
                                if (r.dayType == "AM_HALF") add("AM half")
                                if (r.dayType == "PM_HALF") add("PM half")
                                if (r.hasDocument) add("Document attached")
                            }
                            if (note.isNotEmpty()) Text(note.joinToString(" · "), color = Brand.TextMuted, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${days(r.daysInMonth)} d", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            StatusPill(r.status)
                        }
                    }
                }
            }
            e.timeOff?.requests?.forEach { t ->
                Card {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Time off · ${reasonName(t.reason)}", color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("${AttendanceMonth.dayLabel(t.date)} · ${t.startTime}–${t.endTime}",
                                color = Brand.TextSecondary, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${days(t.hours)} h", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            StatusPill(t.status)
                        }
                    }
                }
            }

            if (e.balances.isNotEmpty()) {
                Text("${data.balancesYear} balances", color = Brand.TextPrimary,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                Card {
                    e.balances.forEachIndexed { i, b ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(b.name.ifEmpty { b.code }, color = Brand.TextPrimary, fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Used ${days(b.usedYtd)}" + (if (b.pending > 0) " · ${days(b.pending)} pending" else ""),
                                    color = Brand.TextMuted, fontSize = 11.sp,
                                )
                            }
                            Text("${days(b.available)} left", color = Brand.Mint, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        if (i < e.balances.lastIndex) {
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Brand.Border))
                        }
                    }
                }
            }
        }
    }
}
