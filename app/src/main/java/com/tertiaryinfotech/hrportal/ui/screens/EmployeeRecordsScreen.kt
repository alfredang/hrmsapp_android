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
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Restore
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import com.tertiaryinfotech.hrportal.data.EmployeeRecords
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.LeaveRequest
import com.tertiaryinfotech.hrportal.data.RecordsBalance
import com.tertiaryinfotech.hrportal.data.RecordsPayback
import com.tertiaryinfotech.hrportal.data.ExpenseClaim
import com.tertiaryinfotech.hrportal.ui.components.RecordHeaders
import com.tertiaryinfotech.hrportal.ui.components.RefreshableColumn
import com.tertiaryinfotech.hrportal.ui.components.AsyncContent
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.LoadState
import com.tertiaryinfotech.hrportal.ui.components.StatTile
import com.tertiaryinfotech.hrportal.ui.components.StatusPill
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import com.tertiaryinfotech.hrportal.ui.components.RecordDetailSheet
import com.tertiaryinfotech.hrportal.ui.components.RecordRow
import com.tertiaryinfotech.hrportal.ui.components.RecordTable
import java.util.Locale

/** How many rows each section previews before "See all". */
private const val PREVIEW_COUNT = 3

/** Which history list [RecordsLeaveScreen] shows. */
enum class RecordsLeaveKind(val title: String, val empty: String) {
    MC("MC history", "No medical leave on record."),
    LEAVE("Leave history", "No leave on record."),
    NPL("No Pay Leave history", "No No Pay Leave on record."),
    PAYBACK("Payback history", "No paybacks on record."),
    CLAIMS("Expense claim history", "No expense claims on record."),
}

private val CLAIM_HEADERS = RecordHeaders(type = "Category", dates = "Expense date", amount = "Amount", reason = "Description")
private val PAYBACK_HEADERS = RecordHeaders(type = "No Pay Leave", dates = "Worked on", amount = "Days", reason = "Note")

private fun recordsPaybackRow(p: RecordsPayback) = RecordRow(
    id = p.id,
    status = p.status,
    type = Fmt.date(p.nplDate),
    dates = Fmt.date(p.workDate),
    amount = if (p.days == 0.5) "Half" else Fmt.num(p.days),
    applied = p.createdAt?.let { Fmt.date(it) },
    reason = p.note,
    details = buildList {
        if (!p.approver.isNullOrBlank()) add("Approver" to p.approver)
        if (!p.rejectionReason.isNullOrBlank()) add("Rejected" to p.rejectionReason)
    },
)

private fun recordsClaimRow(c: ExpenseClaim) = RecordRow(
    id = c.id,
    status = c.status,
    type = c.category ?: "Claim",
    dates = Fmt.date(c.expenseDate),
    amount = Fmt.money(c.amount),
    applied = c.createdAt?.let { Fmt.date(it) },
    reason = c.description,
    details = buildList {
        if (!c.approver.isNullOrBlank()) add("Approver" to c.approver)
        if (!c.rejectionReason.isNullOrBlank()) add("Rejected" to c.rejectionReason)
        if (!c.receiptUrl.isNullOrBlank()) add("Receipt" to "Attached")
    },
)

/** The one-line rows and headers for a history list. */
private fun historyRows(data: EmployeeRecords, kind: RecordsLeaveKind): Pair<List<RecordRow>, RecordHeaders> = when (kind) {
    RecordsLeaveKind.MC -> data.medical.map(::leaveRequestRow) to RecordHeaders()
    RecordsLeaveKind.NPL -> data.noPayLeave.map(::leaveRequestRow) to RecordHeaders()
    RecordsLeaveKind.LEAVE -> data.otherLeave.map(::leaveRequestRow) to RecordHeaders()
    RecordsLeaveKind.PAYBACK -> data.paybacks.orEmpty().map(::recordsPaybackRow) to PAYBACK_HEADERS
    RecordsLeaveKind.CLAIMS -> data.claims.orEmpty().map(::recordsClaimRow) to CLAIM_HEADERS
}

/** Routes start with "team" so the Team tab's top bar + bottom nav stay in place. */
object RecordsRoutes {
    fun records(id: String) = "team_records/${Uri.encode(id)}"
    fun clock(id: String) = "team_records_clock/${Uri.encode(id)}"
    fun leave(id: String, kind: RecordsLeaveKind) = "team_records_leave/${Uri.encode(id)}?kind=${kind.name}"
}

/** Link / accent text. */
private val accent: Color get() = Brand.Accent

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

            // Each tile opens its full history. Leave / MC / No Pay Leave: taken and balance
            // (Alfred, 25 Nov 2026); an older server without balances keeps the old tiles.
            val balances = data.balances.orEmpty()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (balances.isNotEmpty()) {
                    balances.forEach { b ->
                        val kind = when {
                            b.isMedical -> RecordsLeaveKind.MC
                            b.isNoPay -> RecordsLeaveKind.NPL
                            else -> RecordsLeaveKind.LEAVE
                        }
                        BalanceTile(b, Modifier.weight(1f)) { nav.navigate(RecordsRoutes.leave(employeeId, kind)) }
                    }
                } else {
                    if (data.showsClock) {
                        RecordsTile("${data.attendance.daysWorked}", "Days clocked", Icons.Filled.AccessTime, Brand.Mint,
                            Modifier.weight(1f)) { nav.navigate(RecordsRoutes.clock(employeeId)) }
                    }
                    RecordsTile(approvedDays(data.medical), "MC days", Icons.Filled.MedicalServices, Brand.Red,
                        Modifier.weight(1f)) { nav.navigate(RecordsRoutes.leave(employeeId, RecordsLeaveKind.MC)) }
                    RecordsTile(approvedDays(data.otherLeave), "Leave days", Icons.Filled.Flight, accent,
                        Modifier.weight(1f)) { nav.navigate(RecordsRoutes.leave(employeeId, RecordsLeaveKind.LEAVE)) }
                }
            }

            // Clock-in history: interns and others who clock in only (staff do not).
            if (data.showsClock) {
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
            }

            LeaveSection(RecordsLeaveKind.MC, Icons.Outlined.MedicalServices, data.medical) {
                nav.navigate(RecordsRoutes.leave(employeeId, RecordsLeaveKind.MC))
            }
            LeaveSection(RecordsLeaveKind.LEAVE, Icons.Outlined.CalendarMonth, data.otherLeave) {
                nav.navigate(RecordsRoutes.leave(employeeId, RecordsLeaveKind.LEAVE))
            }
            // Payback and expense claim history (Alfred, 25 Nov 2026); hidden on an older server.
            if (data.paybacks != null) {
                HistorySection(RecordsLeaveKind.PAYBACK, Icons.Outlined.Restore, data) {
                    nav.navigate(RecordsRoutes.leave(employeeId, RecordsLeaveKind.PAYBACK))
                }
            }
            if (data.claims != null) {
                HistorySection(RecordsLeaveKind.CLAIMS, Icons.Outlined.CreditCard, data) {
                    nav.navigate(RecordsRoutes.leave(employeeId, RecordsLeaveKind.CLAIMS))
                }
            }
        }
    }
}

/** One balance KPI: the leave name, days taken, and the balance out of the allowance. */
@Composable
private fun BalanceTile(b: RecordsBalance, modifier: Modifier, onClick: () -> Unit) {
    val (icon, tint, label) = when {
        b.isMedical -> Triple(Icons.Filled.MedicalServices, Brand.Red, "MC")
        b.isNoPay -> Triple(Icons.Filled.Payments, Brand.Amber, "No Pay Leave")
        else -> Triple(Icons.Filled.Flight, accent, "Leave")
    }
    Card(modifier = modifier
        .clickable(onClick = onClick)
        .semantics(mergeDescendants = true) { contentDescription = "${b.name}: ${b.takenText}, ${b.balanceText}" }
        .testTag("records.balance.${b.code}")) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                Text(label, color = tint, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1,
                    modifier = Modifier.padding(start = 4.dp))
            }
            Text(RecordsBalance.days(b.taken), color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("taken", color = Brand.TextSecondary, fontSize = 12.sp)
            Box(Modifier.fillMaxWidth().height(1.dp).background(Brand.Border))
            Text("${RecordsBalance.days(b.available)}/${RecordsBalance.days(b.allowance)}",
                color = Brand.Mint, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text("balance", color = Brand.TextSecondary, fontSize = 12.sp)
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
            LeaveRecords(rows.take(PREVIEW_COUNT))
        }
    }
}

/** Payback or expense claim history: up to three one-line records, then "See all". */
@Composable
private fun HistorySection(kind: RecordsLeaveKind, icon: ImageVector, data: EmployeeRecords, onSeeAll: () -> Unit) {
    val (rows, headers) = historyRows(data, kind)
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
            HistoryRecords(rows.take(PREVIEW_COUNT), headers)
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
        val (rows, headers) = historyRows(data, kind)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("${data.employee.name} · ${kind.title}", color = Brand.TextSecondary, fontSize = 14.sp)
            if (rows.isEmpty()) EmptyHint(Icons.Outlined.Inbox, kind.empty) else HistoryRecords(rows, headers)
        }
    }
}

/** One person's leave / MC requests as one-line records; a tap shows the full reason. */
@Composable
private fun LeaveRecords(rows: List<LeaveRequest>) = HistoryRecords(rows.map(::leaveRequestRow), RecordHeaders())

/** One-line records (leave, paybacks, claims); a tap shows the full detail. */
@Composable
private fun HistoryRecords(rows: List<RecordRow>, headers: RecordHeaders) {
    var opened by remember { mutableStateOf<RecordRow?>(null) }
    RecordTable(rows, showName = false, headers = headers, onOpen = { opened = it })
    opened?.let { RecordDetailSheet(it, onDismiss = { opened = null }) }
}
