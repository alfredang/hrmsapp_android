package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.TimeOffRequest
import com.tertiaryinfotech.hrportal.ui.AuthViewModel
import com.tertiaryinfotech.hrportal.ui.components.DecisionButtons
import com.tertiaryinfotech.hrportal.ui.components.FilterTabs
import com.tertiaryinfotech.hrportal.ui.components.NoteDialog
import com.tertiaryinfotech.hrportal.ui.components.RecordDetailSheet
import com.tertiaryinfotech.hrportal.ui.components.RecordHeaders
import com.tertiaryinfotech.hrportal.ui.components.RecordRow
import com.tertiaryinfotech.hrportal.ui.components.RecordTable
import com.tertiaryinfotech.hrportal.ui.components.TintButton
import com.tertiaryinfotech.hrportal.ui.components.RefreshableColumn
import com.tertiaryinfotech.hrportal.ui.components.AsyncContent
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.LoadState
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.StatusBanner
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Time Off — hourly time-off requests (mostly interns: exams, emergencies), ported from the web's
 * `/time-off` page. `GET /api/time-off` returns the user's own requests, and **everyone's** for
 * approvers (MANAGER / HR / ADMIN), so approvers get the admin list here: Pending / All tabs and one
 * line per request (name, status, type, date and time, hours, applied, reason). Tapping a line opens
 * a sheet with the full reason plus Approve / Reject (approvers) or Cancel (your own pending one).
 * "Request Time Off" opens [RequestTimeOffScreen], which sets `time_off_submitted` on this entry's
 * saved state before popping back, and that triggers a reload.
 */
@Composable
fun TimeOffScreen(nav: NavController, auth: AuthViewModel) {
    val approver = isApprover(auth)
    val myId = auth.user?.employeeId
    var state by remember { mutableStateOf<LoadState<List<TimeOffRequest>>>(LoadState.Idle) }
    var tab by rememberSaveable { mutableStateOf(if (approver) "PENDING" else "ALL") }
    var error by remember { mutableStateOf<String?>(null) }
    var opened by remember { mutableStateOf<TimeOffRequest?>(null) }
    var cancelTarget by remember { mutableStateOf<TimeOffRequest?>(null) }
    var rejectTarget by remember { mutableStateOf<TimeOffRequest?>(null) }
    var cancelling by remember { mutableStateOf(false) }
    var actingId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        if (state !is LoadState.Loaded) state = LoadState.Loading
        state = try {
            LoadState.Loaded(HrmsApi.timeOff())
        } catch (e: Exception) {
            LoadState.Failed(e.message ?: "Could not load.")
        }
    }

    suspend fun decide(r: TimeOffRequest, approve: Boolean, note: String?) {
        actingId = r.id
        error = null
        try {
            HrmsApi.decideTimeOff(r.id, approve, note)
            opened = null
            load()
        } catch (e: Exception) {
            error = e.message ?: "Could not update the request."
        } finally {
            actingId = null
        }
    }

    // RequestTimeOffScreen sets this flag before popping back — reload so the new row appears.
    val savedStateHandle = nav.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getStateFlow("time_off_submitted", false)?.collect { submitted ->
            if (submitted) {
                savedStateHandle.set("time_off_submitted", false)
                load()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncContent(state = state, load = { load() }) { requests ->
            val pending = requests.filter { it.status.equals("PENDING", ignoreCase = true) }
            val shown = if (tab == "PENDING") pending else requests
            RefreshableColumn(scope, { load() }, Arrangement.spacedBy(14.dp)) {
                PremierButton(title = "Request Time Off", icon = Icons.Filled.AddCircle) {
                    nav.navigate("time_off_request")
                }
                error?.let { StatusBanner(isError = true, text = it) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (approver) "Time-off requests" else "My requests",
                        color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
                        modifier = Modifier.weight(1f),
                    )
                }
                FilterTabs(
                    options = listOf("PENDING" to "Pending · ${pending.size}", "ALL" to "All · ${requests.size}"),
                    selected = tab, onSelect = { tab = it },
                )
                if (shown.isEmpty()) {
                    EmptyHint(Icons.Outlined.MoreTime, if (tab == "PENDING") "Nothing waiting." else "No time-off requests yet.")
                } else {
                    RecordTable(
                        rows = shown.map { timeOffRow(it, showName = approver) },
                        showName = approver,
                        headers = TIME_OFF_HEADERS,
                        onOpen = { row -> opened = shown.firstOrNull { it.id == row.id } },
                    )
                    Text("Tap a request for the full reason.", color = Brand.TextMuted, fontSize = 12.sp)
                }
            }
        }
    }

    opened?.let { r ->
        val isPending = r.status.equals("PENDING", ignoreCase = true)
        val mine = myId != null && r.employeeId == myId
        RecordDetailSheet(timeOffRow(r, showName = approver), onDismiss = { opened = null }, headers = TIME_OFF_HEADERS) {
            when {
                isPending && approver && !mine -> DecisionButtons(
                    busy = actingId == r.id,
                    onApprove = { scope.launch { decide(r, true, null) } },
                    onReject = { rejectTarget = r },
                )
                isPending && (mine || !approver) -> TintButton(
                    "Cancel request", Icons.Filled.Cancel, Brand.Red, modifier = Modifier.fillMaxWidth(),
                ) { cancelTarget = r }
                else -> {}
            }
        }
    }

    rejectTarget?.let { r ->
        NoteDialog(
            title = "Reject time off",
            message = "Reject ${r.employee?.name ?: "this"} time off on ${Fmt.date(r.date)}?",
            confirmLabel = "Reject",
            confirmColor = Brand.Red,
            onConfirm = { note -> rejectTarget = null; scope.launch { decide(r, false, note) } },
            onDismiss = { rejectTarget = null },
        )
    }

    cancelTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!cancelling) cancelTarget = null },
            containerColor = Brand.Dialog,
            title = { Text("Cancel this request?") },
            text = {
                Text(
                    "Your ${timeOff12h(target.startTime)} – ${timeOff12h(target.endTime)} time-off request on " +
                        "${Fmt.date(target.date)} will be cancelled.",
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !cancelling,
                    onClick = {
                        cancelling = true
                        error = null
                        scope.launch {
                            try {
                                HrmsApi.cancelTimeOff(target.id)
                                opened = null
                                load()
                            } catch (e: Exception) {
                                error = e.message ?: "Could not cancel the request."
                            } finally {
                                cancelling = false
                                cancelTarget = null
                            }
                        }
                    },
                ) { Text(if (cancelling) "Cancelling…" else "Cancel request", color = Brand.Red) }
            },
            dismissButton = {
                TextButton(enabled = !cancelling, onClick = { cancelTarget = null }) { Text("Keep it") }
            },
        )
    }
}

private val TIME_OFF_HEADERS = RecordHeaders(dates = "Date & time", amount = "Hours")

private fun timeOffRow(r: TimeOffRequest, showName: Boolean) = RecordRow(
    id = r.id,
    name = if (showName) r.employee?.name ?: "—" else null,
    status = r.status,
    type = reasonName(r.reason),
    dates = "${Fmt.date(r.date)} · ${timeOff12h(r.startTime)}–${timeOff12h(r.endTime)}",
    amount = "${Fmt.num(r.hours)} h",
    applied = r.createdAt?.let { Fmt.date(it) },
    reason = r.reasonDetail?.takeIf { it.isNotBlank() } ?: timeOffReasonLabel(r),
    details = buildList {
        if (!r.rejectionReason.isNullOrBlank()) add("Rejected" to r.rejectionReason)
        if (!r.approvalComment.isNullOrBlank()) add("Comment" to r.approvalComment)
    },
)

/** "Exams" / "Emergency" / "Others — <detail>", title-cased from the server's enum. */
/** "UNPAID_LEAVE" to "Unpaid Leave". */
internal fun reasonName(reason: String): String =
    reason.split('_').joinToString(" ") { w -> w.lowercase(Locale.US).replaceFirstChar { it.uppercase() } }

internal fun timeOffReasonLabel(r: TimeOffRequest): String {
    val base = reasonName(r.reason)
    return if (r.reason.equals("OTHERS", ignoreCase = true) && !r.reasonDetail.isNullOrBlank()) {
        "$base — ${r.reasonDetail}"
    } else base
}

/** Renders a server "HH:mm" time as 12-hour, e.g. "14:00" → "2:00 PM". */
internal fun timeOff12h(hhmm: String): String {
    val parts = hhmm.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: return hhmm
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val ampm = if (h < 12) "AM" else "PM"
    val h12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    return String.format(Locale.US, "%d:%02d %s", h12, m, ampm)
}
