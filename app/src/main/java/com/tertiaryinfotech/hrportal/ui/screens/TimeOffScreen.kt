package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.TimeOffRequest
import com.tertiaryinfotech.hrportal.ui.components.AsyncContent
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.LoadState
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.StatusBanner
import com.tertiaryinfotech.hrportal.ui.components.StatusPill
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Time Off — the user's hourly time-off requests (mostly for interns: exams, emergencies), ported
 * from the web's `/time-off` page. Lists every request with its date, time window, hours, reason
 * and status; PENDING rows can be cancelled (owner-only, enforced server-side too). "Request Time
 * Off" navigates to [RequestTimeOffScreen], which stashes a `time_off_submitted` flag on this
 * screen's saved-state handle before popping back — same return-a-result pattern as LeaveScreen's
 * `leave_applied` — which we turn into an imperative reload (AsyncContent + LoadState, like
 * TimesheetScreen, since cancel also needs an in-place refetch).
 */
@Composable
fun TimeOffScreen(nav: NavController) {
    var state by remember { mutableStateOf<LoadState<List<TimeOffRequest>>>(LoadState.Idle) }
    var error by remember { mutableStateOf<String?>(null) }
    var cancelTarget by remember { mutableStateOf<TimeOffRequest?>(null) }
    var cancelling by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        if (state !is LoadState.Loaded) state = LoadState.Loading
        state = try {
            LoadState.Loaded(HrmsApi.timeOff())
        } catch (e: Exception) {
            LoadState.Failed(e.message ?: "Could not load.")
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
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PremierButton(title = "Request Time Off", icon = Icons.Filled.AddCircle) {
                    nav.navigate("time_off_request")
                }
                error?.let { StatusBanner(isError = true, text = it) }
                Spacer12()
                SectionTitle("My requests")
                if (requests.isEmpty()) {
                    EmptyHint(Icons.Outlined.MoreTime, "No time-off requests yet.")
                } else {
                    requests.forEach { r ->
                        TimeOffRow(r, onCancel = { cancelTarget = r })
                    }
                }
            }
        }
    }

    cancelTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!cancelling) cancelTarget = null },
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

@Composable
private fun TimeOffRow(r: TimeOffRequest, onCancel: () -> Unit) {
    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(Fmt.date(r.date), color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    "${timeOff12h(r.startTime)} – ${timeOff12h(r.endTime)} · ${Fmt.num(r.hours)} h",
                    color = Brand.TextSecondary, fontSize = 12.sp,
                )
                Text(timeOffReasonLabel(r), color = Brand.TextMuted, fontSize = 11.sp, maxLines = 2)
                if (!r.rejectionReason.isNullOrBlank()) {
                    Text("Rejected: ${r.rejectionReason}", color = Brand.Red, fontSize = 11.sp)
                }
                if (!r.approvalComment.isNullOrBlank()) {
                    Text("Comment: ${r.approvalComment}", color = Brand.TextSecondary, fontSize = 11.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill(r.status)
                if (r.status.equals("PENDING", ignoreCase = true)) {
                    TextButton(onClick = onCancel) { Text("Cancel", color = Brand.Red, fontSize = 13.sp) }
                }
            }
        }
    }
}

/** "Exams" / "Emergency" / "Others — <detail>", title-cased from the server's enum. */
internal fun timeOffReasonLabel(r: TimeOffRequest): String {
    val base = r.reason.lowercase(Locale.US).replaceFirstChar { it.uppercase() }
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
