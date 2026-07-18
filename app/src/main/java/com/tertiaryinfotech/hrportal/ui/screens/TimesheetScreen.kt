package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.AttendancePunch
import com.tertiaryinfotech.hrportal.data.AttendanceResponse
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.ui.components.AsyncContent
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.LoadState
import com.tertiaryinfotech.hrportal.ui.components.StatTile
import com.tertiaryinfotech.hrportal.ui.components.StatusBanner
import com.tertiaryinfotech.hrportal.ui.components.StatusPill
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Timesheet — a simple **clock in / out** with a live elapsed timer and a last-7-days log, ported
 * from the native iOS `ClockView`/`TimesheetView` so the two platforms behave identically. Every
 * punch is stored centrally (AttendancePunch table) via `/api/mobile/attendance` +
 * `attendance/clock-{in,out}`; this screen shows today's state live plus the last 7 days. (Regular
 * work hours are derived from clock in/out; there is no separate weekly OT grid here.)
 */
@Composable
fun TimesheetScreen(nav: NavController) {
    var state by remember { mutableStateOf<LoadState<AttendanceResponse>>(LoadState.Idle) }
    var punching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        if (state !is LoadState.Loaded) state = LoadState.Loading
        state = try {
            LoadState.Loaded(HrmsApi.attendance())
        } catch (e: Exception) {
            LoadState.Failed(e.message ?: "Could not load.")
        }
    }

    suspend fun punch(clockIn: Boolean) {
        if (punching) return
        error = null
        punching = true
        try {
            if (clockIn) HrmsApi.clockIn() else HrmsApi.clockOut()
            load()
        } catch (e: Exception) {
            error = e.message ?: "Could not record your punch."
        } finally {
            punching = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncContent(state = state, load = { load() }) { data ->
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TodayCard(data, punching) { clockIn -> scope.launch { punch(clockIn) } }
                error?.let { StatusBanner(isError = true, text = it) }
                WeekSummary(data)
                RecentList(data)
            }
        }
    }
}

// MARK: - Today card (three phases: not-clocked-in / working / done)

private sealed interface Phase {
    data object NotClockedIn : Phase
    data class Working(val sinceMs: Long) : Phase
    data class Done(val inAt: String?, val outAt: String?, val hours: Double?) : Phase
}

private fun phaseOf(data: AttendanceResponse): Phase {
    val today = data.today ?: return Phase.NotClockedIn
    val inIso = today.clockIn ?: return Phase.NotClockedIn
    if (today.clockOut == null) {
        val since = Fmt.parse(inIso)?.time ?: return Phase.NotClockedIn
        return Phase.Working(since)
    }
    return Phase.Done(today.clockIn, today.clockOut, hoursOf(today))
}

@Composable
private fun TodayCard(data: AttendanceResponse, punching: Boolean, onPunch: (Boolean) -> Unit) {
    Card(padding = 24) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            when (val p = phaseOf(data)) {
                is Phase.NotClockedIn -> {
                    StatusHeader(
                        icon = Icons.Filled.WbSunny, tint = Brand.Amber,
                        title = "Not clocked in yet",
                        subtitle = "Tap below when you start work — your time is logged to HR automatically.",
                    )
                    PunchButton("Clock in", Icons.AutoMirrored.Filled.Login, Brand.Primary, punching) { onPunch(true) }
                }
                is Phase.Working -> {
                    LiveTimer(p.sinceMs)
                    PunchButton("Clock out", Icons.AutoMirrored.Filled.Logout, Color(0xFFF97316), punching) { onPunch(false) }
                }
                is Phase.Done -> {
                    StatusHeader(
                        icon = Icons.Filled.CheckCircle, tint = Brand.Green,
                        title = "Done for today",
                        subtitle = buildString {
                            append("${timeStr(p.inAt)} – ${timeStr(p.outAt)}")
                            p.hours?.let { append(String.format(Locale.US, " · %.1f h logged", it)) }
                        },
                    )
                    Text("See you tomorrow 👋", color = Brand.TextMuted, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun StatusHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(42.dp))
        Text(title, color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(subtitle, color = Brand.TextSecondary, fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/** Live H:MM:SS timer since clock-in — recomposes every second, like iOS's TimelineView. */
@Composable
private fun LiveTimer(sinceMs: Long) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sinceMs) {
        while (true) { now = System.currentTimeMillis(); delay(1000) }
    }
    val elapsed = ((now - sinceMs) / 1000).coerceAtLeast(0)
    val h = elapsed / 3600; val m = (elapsed % 3600) / 60; val s = elapsed % 60
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Working — clocked in at ${timeStrFromMs(sinceMs)}", color = Brand.TextSecondary, fontSize = 13.sp)
        Text(
            String.format(Locale.US, "%d:%02d:%02d", h, m, s),
            color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 44.sp,
        )
    }
}

@Composable
private fun PunchButton(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, loading: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(Brand.Corner.dp))
            .background(tint)
            .then(if (loading) Modifier else Modifier.clickable(onClick = onClick)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        } else {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                modifier = Modifier.padding(start = 10.dp))
        }
    }
}

// MARK: - Week summary + recent list

@Composable
private fun WeekSummary(data: AttendanceResponse) {
    val total = data.recent.mapNotNull { hoursOf(it) }.sum()
    val daysWorked = data.recent.count { it.clockIn != null }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(
            value = String.format(Locale.US, "%.1f h", total), label = "Logged (last 7 days)",
            icon = Icons.Filled.Functions, tint = Brand.Mint,
            iconBg = Color.White.copy(alpha = 0.12f), modifier = Modifier.weight(1f),
        )
        StatTile(
            value = "$daysWorked", label = "Days worked",
            icon = Icons.Outlined.EventAvailable, tint = Brand.Blue,
            iconBg = Color.White.copy(alpha = 0.12f), modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RecentList(data: AttendanceResponse) {
    Text("Recent days", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    if (data.recent.isEmpty()) {
        EmptyHint(Icons.Outlined.EventAvailable, "No punches yet. Your log appears here.")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            data.recent.forEach { p -> RecentRow(p) }
        }
    }
}

@Composable
private fun RecentRow(p: AttendancePunch) {
    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(Fmt.date(p.date ?: p.clockIn), color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    "${timeStr(p.clockIn)} – ${if (p.clockOut == null) "…" else timeStr(p.clockOut)}",
                    color = Brand.TextSecondary, fontSize = 12.sp,
                )
            }
            val hours = hoursOf(p)
            when {
                hours != null -> Text(String.format(Locale.US, "%.1f h", hours), color = Brand.Mint, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                p.clockIn != null -> StatusPill("WORKING")
            }
        }
    }
}

// MARK: - Helpers

/** Hours worked = clockOut − clockIn, in hours; null while still clocked in or no data. */
private fun hoursOf(p: AttendancePunch): Double? {
    val inMs = Fmt.parse(p.clockIn)?.time ?: return null
    val outMs = Fmt.parse(p.clockOut)?.time ?: return null
    if (outMs <= inMs) return null
    return (outMs - inMs) / 3_600_000.0
}

private val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
private fun timeStr(iso: String?): String {
    val d = Fmt.parse(iso) ?: return "—"
    return timeFmt.format(d)
}
private fun timeStrFromMs(ms: Long): String = timeFmt.format(java.util.Date(ms))
