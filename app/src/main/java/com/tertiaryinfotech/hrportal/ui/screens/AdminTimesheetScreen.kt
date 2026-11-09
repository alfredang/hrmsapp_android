package com.tertiaryinfotech.hrportal.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PendingActions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.AttendanceCorrection
import com.tertiaryinfotech.hrportal.data.AttendanceSummary
import com.tertiaryinfotech.hrportal.data.AttendanceSummaryRow
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.OilEntry
import com.tertiaryinfotech.hrportal.ui.components.AsyncContent
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.CollapsibleSection
import com.tertiaryinfotech.hrportal.ui.components.CompactLine
import com.tertiaryinfotech.hrportal.ui.components.DecisionButtons
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.FilterTabs
import com.tertiaryinfotech.hrportal.ui.components.LineDivider
import com.tertiaryinfotech.hrportal.ui.components.LoadState
import com.tertiaryinfotech.hrportal.ui.components.NoteDialog
import com.tertiaryinfotech.hrportal.ui.components.RecordDetailSheet
import com.tertiaryinfotech.hrportal.ui.components.RecordHeaders
import com.tertiaryinfotech.hrportal.ui.components.RecordRow
import com.tertiaryinfotech.hrportal.ui.components.RefreshableColumn
import com.tertiaryinfotech.hrportal.ui.components.Kpi
import com.tertiaryinfotech.hrportal.ui.components.KpiStrip
import com.tertiaryinfotech.hrportal.ui.components.StatusBanner
import com.tertiaryinfotech.hrportal.ui.components.StatusPill
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Admin Timesheet (approvers only) — the web's merged `/timesheet/overview` (464200b): only interns
 * have timesheets, so this is one screen for them.
 *
 *  - **Needs approval** (collapsible): pending missing-punch requests (`/api/attendance/corrections`)
 *    and weekend / public-holiday Off In Lieu entries (`/api/timesheet/admin`), each opening a sheet
 *    with Approve / Reject.
 *  - **Interns** (collapsible): days worked and total hours per intern for the month
 *    (`/api/mobile/attendance/summary`), with a month switcher; tap an intern for their daily
 *    clock-in / clock-out. An "Everyone" tab adds non-interns who clocked in.
 *
 * Replaces the separate Intern Attendance screen.
 */
@Composable
fun AdminTimesheetScreen(nav: NavController) {
    var month by rememberSaveable { mutableStateOf(AttendanceMonth.current()) }
    var who by rememberSaveable { mutableStateOf("INTERNS") }
    var state by remember { mutableStateOf<LoadState<AttendanceSummary>>(LoadState.Idle) }
    var corrections by remember { mutableStateOf<List<AttendanceCorrection>>(emptyList()) }
    var oil by remember { mutableStateOf<List<OilEntry>>(emptyList()) }
    var queueError by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var openCorrection by remember { mutableStateOf<AttendanceCorrection?>(null) }
    var openOil by remember { mutableStateOf<OilEntry?>(null) }
    var rejecting by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // id, isCorrection
    var actingId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun loadQueue() {
        // Each queue loads on its own so one failing endpoint doesn't hide the other.
        coroutineScope {
            val c = async { runCatching { HrmsApi.pendingCorrections() } }
            val o = async { runCatching { HrmsApi.oilEntries() } }
            val cr = c.await()
            val or = o.await()
            corrections = cr.getOrDefault(emptyList())
            oil = or.getOrDefault(emptyList()).filter { it.status.equals("PENDING", ignoreCase = true) }
            queueError = (cr.exceptionOrNull() ?: or.exceptionOrNull())?.let { it.message ?: "Could not load approvals." }
        }
    }

    suspend fun load() {
        if (state !is LoadState.Loaded) state = LoadState.Loading
        // Both together, and the screen shows only once the queue is in, so "Needs approval"
        // opens by itself when something is waiting.
        coroutineScope {
            val q = async { loadQueue() }
            val summary = try {
                LoadState.Loaded(HrmsApi.attendanceSummary(month))
            } catch (e: Exception) {
                LoadState.Failed(e.message ?: "Could not load.")
            }
            q.await()
            state = summary
        }
    }

    suspend fun decide(id: String, isCorrection: Boolean, approve: Boolean, note: String?) {
        actingId = id
        actionError = null
        try {
            if (isCorrection) HrmsApi.decideCorrection(id, approve, note) else HrmsApi.decideOil(id, approve, note)
            openCorrection = null
            openOil = null
            loadQueue()
            if (isCorrection && approve) state = runCatching { LoadState.Loaded(HrmsApi.attendanceSummary(month)) }.getOrDefault(state)
        } catch (e: Exception) {
            actionError = e.message ?: "Could not update the request."
        } finally {
            actingId = null
        }
    }

    LaunchedEffect(month) {
        if (state !is LoadState.Idle) {
            state = try {
                LoadState.Loaded(HrmsApi.attendanceSummary(month))
            } catch (e: Exception) {
                LoadState.Failed(e.message ?: "Could not load.")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncContent(state = state, load = { load() }) { data ->
            val interns = data.employees.filter { it.isIntern }
            val rows = if (who == "INTERNS") interns else data.employees
            val waiting = corrections.size + oil.size

            RefreshableColumn(scope, { load() }, Arrangement.spacedBy(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Timesheet", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                        modifier = Modifier.weight(1f))
                    MonthSwitcher(month) { month = it }
                }
                Text(
                    "Interns' clock-in days and hours. Tap an intern to see each day.",
                    color = Brand.TextSecondary, fontSize = 13.sp,
                )
                KpiStrip(
                    listOf(
                        Kpi("${interns.size}", "Interns"),
                        Kpi("${rows.sumOf { it.daysWorked }}", "Days worked", Brand.Teal),
                        Kpi(String.format(Locale.US, "%.1f h", rows.sumOf { it.totalHours }), "Total hours", Brand.Mint),
                        Kpi("${rows.count { it.clockedInNow }}", "Working now", Brand.Amber),
                    ),
                )
                actionError?.let { StatusBanner(isError = true, text = it) }

                CollapsibleSection(
                    "Needs approval", icon = Icons.Outlined.PendingActions, count = waiting, highlight = true,
                    initiallyExpanded = waiting > 0,
                ) {
                    queueError?.let { StatusBanner(isError = true, text = it) }
                    if (waiting == 0 && queueError == null) {
                        Card { Text("No missing punches or Off In Lieu entries waiting.", color = Brand.TextSecondary, fontSize = 14.sp) }
                    }
                    if (corrections.isNotEmpty()) {
                        SubHeader("Missing punches · ${corrections.size}")
                        Card(padding = 0) {
                            corrections.forEachIndexed { i, c ->
                                if (i > 0) LineDivider()
                                CompactLine(
                                    lead = c.employeeName,
                                    middle = "${kindLabel(c.kind)} · ${shortDay(c.date)}",
                                    trailing = requestedTimes(c),
                                    onClick = { openCorrection = c },
                                )
                            }
                        }
                    }
                    if (oil.isNotEmpty()) {
                        SubHeader("Off In Lieu · ${oil.size}")
                        Card(padding = 0) {
                            oil.forEachIndexed { i, e ->
                                if (i > 0) LineDivider()
                                CompactLine(
                                    lead = e.employeeName,
                                    middle = "${if (e.isPublicHoliday) "Public holiday" else e.dayName} · ${shortDay(e.date)}",
                                    trailing = "${Fmt.num(e.hours)} h",
                                    onClick = { openOil = e },
                                )
                            }
                        }
                    }
                }

                CollapsibleSection("Interns", icon = Icons.Outlined.Groups, count = interns.size) {
                    val others = data.employees.size - interns.size
                    if (others > 0) {
                        FilterTabs(
                            options = listOf("INTERNS" to "Interns · ${interns.size}", "ALL" to "Everyone · ${data.employees.size}"),
                            selected = who, onSelect = { who = it },
                        )
                    }
                    if (rows.isEmpty()) {
                        EmptyHint(Icons.Outlined.Groups, "No interns or check-ins for ${AttendanceMonth.label(month)}.")
                    } else {
                        Card(padding = 0) {
                            rows.forEachIndexed { i, r ->
                                if (i > 0) LineDivider()
                                InternLine(r) {
                                    val enc = { s: String -> Uri.encode(s) }
                                    nav.navigate("intern_attendance_detail/${enc(r.id)}?name=${enc(r.name)}&code=${enc(r.employeeCode)}&month=$month")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    openCorrection?.let { c ->
        RecordDetailSheet(correctionRow(c), onDismiss = { openCorrection = null }, headers = CORRECTION_HEADERS) {
            DecisionButtons(
                busy = actingId == c.id,
                onApprove = { scope.launch { decide(c.id, isCorrection = true, approve = true, note = null) } },
                onReject = { rejecting = c.id to true },
            )
        }
    }
    openOil?.let { e ->
        RecordDetailSheet(oilRow(e), onDismiss = { openOil = null }, headers = OIL_HEADERS) {
            DecisionButtons(
                busy = actingId == e.id,
                onApprove = { scope.launch { decide(e.id, isCorrection = false, approve = true, note = null) } },
                onReject = { rejecting = e.id to false },
            )
        }
    }
    rejecting?.let { (id, isCorrection) ->
        NoteDialog(
            title = if (isCorrection) "Reject missing punch" else "Reject Off In Lieu",
            message = "The employee is notified with your reason.",
            confirmLabel = "Reject",
            confirmColor = Brand.Red,
            onConfirm = { note -> rejecting = null; scope.launch { decide(id, isCorrection, approve = false, note = note) } },
            onDismiss = { rejecting = null },
        )
    }
}

@Composable
private fun SubHeader(text: String) {
    Text(text.uppercase(), color = Brand.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp))
}

@Composable
private fun InternLine(r: AttendanceSummaryRow, onClick: () -> Unit) {
    CompactLine(
        lead = r.name,
        badge = if (r.clockedInNow) ({ StatusPill("WORKING") }) else null,
        middle = buildString {
            append(r.employeeCode)
            if (!r.isIntern) append(" · staff")
            append(" · ${r.daysWorked} day${if (r.daysWorked == 1) "" else "s"}")
        },
        trailing = String.format(Locale.US, "%.1f h", r.totalHours),
        trailingColor = Brand.Mint,
        onClick = onClick,
    )
}

private val CORRECTION_HEADERS = RecordHeaders(type = "Missing", dates = "Day", amount = "Requested", applied = "Submitted")
private val OIL_HEADERS = RecordHeaders(type = "Day type", dates = "Day", amount = "Hours", reason = "Admin comment")

internal fun kindLabel(kind: String): String = when (kind.uppercase()) {
    "CLOCK_IN" -> "Clock-in"
    "CLOCK_OUT" -> "Clock-out"
    "BOTH" -> "Clock-in & out"
    else -> kind
}

/** "5 Nov" from `yyyy-MM-dd`. */
private fun shortDay(date: String): String = Fmt.dayMonth(date)

private fun requestedTimes(c: AttendanceCorrection): String = when (c.kind.uppercase()) {
    "CLOCK_IN" -> AttendanceMonth.time(c.requestedClockIn)
    "CLOCK_OUT" -> AttendanceMonth.time(c.requestedClockOut)
    else -> "${AttendanceMonth.time(c.requestedClockIn)}–${AttendanceMonth.time(c.requestedClockOut)}"
}

private fun correctionRow(c: AttendanceCorrection) = RecordRow(
    id = c.id,
    name = c.employeeName,
    status = c.status,
    type = "Missing ${kindLabel(c.kind).lowercase()}",
    dates = AttendanceMonth.dayLabel(c.date),
    amount = buildList {
        if (c.kind.uppercase() != "CLOCK_OUT") add("In ${AttendanceMonth.time(c.requestedClockIn)}")
        if (c.kind.uppercase() != "CLOCK_IN") add("Out ${AttendanceMonth.time(c.requestedClockOut)}")
    }.joinToString(" · "),
    applied = c.createdAt?.let { Fmt.date(it) },
    reason = c.reason,
    details = listOf(
        "On file now" to "In ${AttendanceMonth.time(c.currentClockIn)} · Out ${AttendanceMonth.time(c.currentClockOut)}",
        "Employee" to c.employeeCode,
    ),
)

/** Off In Lieu credited for weekend / PH hours, as the server does it: 8 h+ = 1 day, 4 h+ = half. */
internal fun oilDaysFor(hours: Double): Double = when {
    hours >= 8 -> 1.0
    hours >= 4 -> 0.5
    else -> 0.0
}

private fun oilRow(e: OilEntry) = RecordRow(
    id = e.id,
    name = e.employeeName,
    status = e.status,
    type = if (e.isPublicHoliday) "Public holiday" else "Weekend (${e.dayName})",
    dates = AttendanceMonth.dayLabel(e.date),
    amount = "${Fmt.num(e.hours)} h",
    reason = e.adminComment,
    details = listOf(
        "Off In Lieu" to "${Fmt.num(oilDaysFor(e.hours))} day(s) if approved",
        "Employee" to e.employeeCode,
    ),
)
