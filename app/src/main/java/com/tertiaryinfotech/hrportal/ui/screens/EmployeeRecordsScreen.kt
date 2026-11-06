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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import com.tertiaryinfotech.hrportal.data.EmployeeRecords
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.LeaveRequest
import com.tertiaryinfotech.hrportal.ui.components.RefreshableColumn
import com.tertiaryinfotech.hrportal.ui.components.AsyncContent
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.LoadState
import com.tertiaryinfotech.hrportal.ui.components.StatTile
import com.tertiaryinfotech.hrportal.ui.components.StatusPill
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import java.util.Locale

/** How many rows each section previews before "See all". */
private const val PREVIEW_COUNT = 3

/** Which history list [RecordsLeaveScreen] shows. */
enum class RecordsLeaveKind(val title: String, val empty: String) {
    MC("MC history", "No medical leave on record."),
    LEAVE("Leave history", "No leave on record."),
}

/** Routes start with "team" so the Team tab's top bar + bottom nav stay in place. */
object RecordsRoutes {
    fun records(id: String) = "team_records/${Uri.encode(id)}"
    fun clock(id: String) = "team_records_clock/${Uri.encode(id)}"
    fun leave(id: String, kind: RecordsLeaveKind) = "team_records_leave/${Uri.encode(id)}?kind=${kind.name}"
}

/** Link / accent text that reads on both the dark gradient and the light theme. */
private val accent: Color get() = if (Brand.isDark) Brand.Sky else Brand.Premier

/** Approved days only — pending/rejected requests weren't taken. */
private fun approvedDays(rows: List<LeaveRequest>): String =
    Fmt.num(rows.filter { it.status.uppercase() == "APPROVED" }.sumOf { it.days })

/** Fetches one person's records and renders [content] once loaded. */
@Composable
private fun RecordsLoader(employeeId: String, content: @Composable (EmployeeRecords, suspend () -> Unit, CoroutineScope) -> Unit) {
    var state by remember(employeeId) { mutableStateOf<LoadState<EmployeeRecords>>(LoadState.Idle) }
    val scope = rememberCoroutineScope()
    val load: suspend () -> Unit = {
        if (state !is LoadState.Loaded) state = LoadState.Loading
        state = try {
            LoadState.Loaded(HrmsApi.employeeRecords(employeeId))
        } catch (e: Exception) {
            LoadState.Failed(e.message ?: "Could not load records.")
        }
    }
    AsyncContent(state = state, load = load) { data -> content(data, load, scope) }
}

/**
 * Approver drill-down from the Team tab: one person's clock-in history, MC history and leave
 * history. The three tiles up top open each full history. Reading another employee's records is
 * enforced server-side (403 unless approver or self). Mirrors iOS `EmployeeRecordsView`.
 */
@Composable
fun EmployeeRecordsScreen(employeeId: String, nav: NavController) {
    RecordsLoader(employeeId) { data, load, scope ->
        val e = data.employee
        RefreshableColumn(scope, load, Arrangement.spacedBy(14.dp)) {
            Card {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(e.name, color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    val role = listOfNotNull(e.position, e.department).joinToString(" · ")
                    if (role.isNotEmpty()) Text(role, color = Brand.TextSecondary, fontSize = 14.sp)
                    Text(
                        listOfNotNull(
                            e.employeeCode.ifEmpty { null },
                            e.employmentType?.replace("_", " ")?.lowercase()
                                ?.split(" ")?.joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } },
                        ).joinToString(" · "),
                        color = accent, fontSize = 12.sp,
                    )
                }
            }

            // Each tile opens its full history.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RecordsTile("${data.attendance.daysWorked}", "Days clocked", Icons.Filled.AccessTime, Brand.Mint,
                    Modifier.weight(1f)) { nav.navigate(RecordsRoutes.clock(employeeId)) }
                RecordsTile(approvedDays(data.medical), "MC days", Icons.Filled.MedicalServices, Brand.Red,
                    Modifier.weight(1f)) { nav.navigate(RecordsRoutes.leave(employeeId, RecordsLeaveKind.MC)) }
                RecordsTile(approvedDays(data.otherLeave), "Leave days", Icons.Filled.Flight, accent,
                    Modifier.weight(1f)) { nav.navigate(RecordsRoutes.leave(employeeId, RecordsLeaveKind.LEAVE)) }
            }

            // Clock-in history
            SectionTitle("Clock-in history", Icons.Outlined.AccessTime)
            Card(modifier = Modifier.clickable { nav.navigate(RecordsRoutes.clock(employeeId)) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (data.attendance.days.isEmpty()) {
                        Text("No clock-ins on record.", color = Brand.TextSecondary, fontSize = 14.sp)
                    } else {
                        data.attendance.days.take(PREVIEW_COUNT).forEach { PunchLine(it) }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Brand.Border))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(String.format(Locale.US, "%.1f h total", data.attendance.totalHours),
                            color = Brand.Mint, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                            modifier = Modifier.weight(1f))
                        Text("See all clock in/out", color = accent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                            tint = accent, modifier = Modifier.size(18.dp))
                    }
                }
            }

            LeaveSection(RecordsLeaveKind.MC, Icons.Outlined.MedicalServices, data.medical) {
                nav.navigate(RecordsRoutes.leave(employeeId, RecordsLeaveKind.MC))
            }
            LeaveSection(RecordsLeaveKind.LEAVE, Icons.Outlined.CalendarMonth, data.otherLeave) {
                nav.navigate(RecordsRoutes.leave(employeeId, RecordsLeaveKind.LEAVE))
            }
        }
    }
}

/** A tappable stat tile: icon, value and label stacked, like the iOS records tiles. */
@Composable
private fun RecordsTile(
    value: String, label: String, icon: ImageVector, tint: Color, modifier: Modifier, onClick: () -> Unit,
) {
    Card(modifier = modifier.clickable(onClick = onClick)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Text(value, color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(label, color = Brand.TextSecondary, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SectionTitle(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Brand.TextSecondary, modifier = Modifier.size(20.dp))
        Text(title, color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun LeaveSection(kind: RecordsLeaveKind, icon: ImageVector, rows: List<LeaveRequest>, onSeeAll: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { SectionTitle(kind.title, icon) }
            if (rows.size > PREVIEW_COUNT) {
                Text("See all ${rows.size}", color = accent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                    modifier = Modifier.clickable(onClick = onSeeAll).padding(6.dp))
            }
        }
        if (rows.isEmpty()) {
            Card { Text(kind.empty, color = Brand.TextSecondary, fontSize = 14.sp) }
        } else {
            rows.take(PREVIEW_COUNT).forEach { LeaveRequestRow(it) }
        }
    }
}

/** Every clock in / out on record for one person, grouped by month with each month's hours. */
@Composable
fun RecordsClockScreen(employeeId: String) {
    RecordsLoader(employeeId) { data, _, _ ->
        val a = data.attendance
        // `days` arrives newest first, so grouping preserves that order.
        val months = a.days.groupBy { it.date.take(7) }
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("${data.employee.name} · Clock in/out history", color = Brand.TextSecondary, fontSize = 14.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    value = String.format(Locale.US, "%.1f h", a.totalHours), label = "Total hours",
                    icon = Icons.Filled.Functions, tint = Brand.Mint,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = "${a.daysWorked}", label = "Days worked", icon = Icons.Outlined.EventAvailable,
                    tint = Brand.Blue, modifier = Modifier.weight(1f),
                )
            }
            if (a.days.isEmpty()) EmptyHint(Icons.Outlined.AccessTime, "No clock-ins on record.")
            months.forEach { (month, days) ->
                Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(AttendanceMonth.label(month), color = Brand.TextPrimary, fontWeight = FontWeight.Bold,
                        fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text(
                        String.format(Locale.US, "%.2f h · %d day%s", days.sumOf { it.hours ?: 0.0 },
                            days.size, if (days.size == 1) "" else "s"),
                        color = Brand.Mint, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                    )
                }
                days.forEach { d -> Card { PunchLine(d) } }
            }
        }
    }
}

/** The full list of one person's MC or leave requests. */
@Composable
fun RecordsLeaveScreen(employeeId: String, kind: RecordsLeaveKind) {
    RecordsLoader(employeeId) { data, _, _ ->
        val rows = if (kind == RecordsLeaveKind.MC) data.medical else data.otherLeave
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("${data.employee.name} · ${kind.title}", color = Brand.TextSecondary, fontSize = 14.sp)
            if (rows.isEmpty()) EmptyHint(Icons.Outlined.Inbox, kind.empty)
            rows.forEach { LeaveRequestRow(it) }
        }
    }
}

/** A leave request card — type, dates, reason, status and days. */
@Composable
private fun LeaveRequestRow(r: LeaveRequest) {
    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(r.leaveType, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("${Fmt.date(r.startDate)} → ${Fmt.date(r.endDate)}", color = Brand.TextSecondary, fontSize = 12.sp)
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
