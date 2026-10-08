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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.AttendanceHistory
import com.tertiaryinfotech.hrportal.data.AttendanceSummary
import com.tertiaryinfotech.hrportal.data.AttendanceSummaryRow
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * `yyyy-MM` month keys for the attendance screens — always Singapore time so a month boundary
 * matches the server's punch dates. Mirrors iOS `AttendanceMonth`.
 */
object AttendanceMonth {
    private val sgt: TimeZone = TimeZone.getTimeZone("Asia/Singapore")
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    fun current(): String {
        val c = Calendar.getInstance(sgt)
        return String.format(Locale.US, "%04d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
    }

    fun shift(month: String, n: Int): String {
        val parts = month.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 2) return month
        val total = parts[0] * 12 + (parts[1] - 1) + n
        return String.format(Locale.US, "%04d-%02d", total / 12, total % 12 + 1)
    }

    private fun ymd(s: String) = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = utc }.parse(s)

    /** "October 2026" */
    fun label(month: String): String {
        val d = runCatching { ymd("$month-01") }.getOrNull() ?: return month
        return SimpleDateFormat("LLLL yyyy", Locale.getDefault()).apply { timeZone = utc }.format(d)
    }

    /** "Wed, 8 Oct 2026" for a plain `yyyy-MM-dd`. */
    fun dayLabel(date: String): String {
        val d = runCatching { ymd(date) }.getOrNull() ?: return date
        return SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).apply { timeZone = utc }.format(d)
    }

    /** Clock time in Singapore time, e.g. "9:21 AM". */
    fun time(iso: String?): String {
        val d = Fmt.parse(iso) ?: return "—"
        return SimpleDateFormat("h:mm a", Locale.getDefault()).apply { timeZone = sgt }.format(d)
    }
}

/** Previous / next month switcher shared by the attendance screens. */
@Composable
fun MonthSwitcher(month: String, onChange: (String) -> Unit) {
    val atCurrent = month >= AttendanceMonth.current()
    Row(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Brand.Surface).padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month",
            tint = Brand.TextSecondary,
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                .clickable { onChange(AttendanceMonth.shift(month, -1)) }.padding(4.dp),
        )
        Text(
            AttendanceMonth.label(month), color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 120.dp),
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month",
            tint = Brand.TextSecondary,
            modifier = Modifier.size(32.dp).alpha(if (atCurrent) 0.3f else 1f).clip(RoundedCornerShape(8.dp))
                .then(if (atCurrent) Modifier else Modifier.clickable { onChange(AttendanceMonth.shift(month, 1)) })
                .padding(4.dp),
        )
    }
}

/**
 * Daily check-in / check-out history for one month with the month's total hours — the same
 * content as the web app's "Daily history" and iOS `AttendanceHistorySection`. Without an
 * [employeeId] it shows the signed-in user's own punches. Bump [reloadToken] to refetch.
 */
@Composable
fun AttendanceHistorySection(
    month: String,
    onMonthChange: (String) -> Unit,
    employeeId: String? = null,
    title: String = "Daily history",
    reloadToken: Int = 0,
) {
    var data by remember { mutableStateOf<AttendanceHistory?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(month, employeeId, reloadToken) {
        loading = true
        error = null
        try {
            data = HrmsApi.attendanceHistory(month, employeeId)
        } catch (e: Exception) {
            error = e.message ?: "Could not load attendance history."
        }
        loading = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                modifier = Modifier.weight(1f))
            MonthSwitcher(month, onMonthChange)
        }
        val d = data.takeIf { !loading }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                value = d?.let { String.format(Locale.US, "%.1f h", it.totalHours) } ?: "—",
                label = "Total hours", icon = Icons.Filled.Functions, tint = Brand.Mint,
                iconBg = Color.White.copy(alpha = 0.12f), modifier = Modifier.weight(1f),
            )
            StatTile(
                value = d?.daysWorked?.toString() ?: "—", label = "Days worked",
                icon = Icons.Outlined.EventAvailable, tint = Brand.Blue,
                iconBg = Color.White.copy(alpha = 0.12f), modifier = Modifier.weight(1f),
            )
        }
        when {
            loading -> Box(modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Brand.TextSecondary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }
            error != null -> StatusBanner(isError = true, text = error!!)
            d != null && d.days.isNotEmpty() -> {
                d.days.forEach { day ->
                    Card {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(AttendanceMonth.dayLabel(day.date), color = Brand.TextPrimary,
                                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                val out = when {
                                    day.clockOut != null -> AttendanceMonth.time(day.clockOut)
                                    day.clockIn != null -> "…"
                                    else -> "—"
                                }
                                Text("In ${AttendanceMonth.time(day.clockIn)}  ·  Out $out",
                                    color = Brand.TextSecondary, fontSize = 12.sp)
                            }
                            when {
                                day.hours != null -> Text(String.format(Locale.US, "%.2f h", day.hours),
                                    color = Brand.Mint, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                day.clockIn != null -> StatusPill("WORKING")
                            }
                        }
                    }
                }
                Card {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Total", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            modifier = Modifier.weight(1f))
                        Text(String.format(Locale.US, "%.2f h", d.totalHours), color = Brand.Mint,
                            fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
            else -> EmptyHint(Icons.Outlined.EventAvailable, "No check-ins in ${AttendanceMonth.label(month)}.")
        }
    }
}

/**
 * Admin: each intern's days worked and total hours for a month, drilling into that intern's daily
 * check-in / check-out history. Mirrors the web's Intern Attendance page (`/attendance/overview`)
 * and iOS `InternAttendanceView`.
 */
@Composable
fun InternAttendanceScreen(nav: NavController) {
    var month by rememberSaveable { mutableStateOf(AttendanceMonth.current()) }
    var state by remember { mutableStateOf<LoadState<AttendanceSummary>>(LoadState.Idle) }

    suspend fun load() {
        if (state !is LoadState.Loaded) state = LoadState.Loading
        state = try {
            LoadState.Loaded(HrmsApi.attendanceSummary(month))
        } catch (e: Exception) {
            LoadState.Failed(e.message ?: "Could not load.")
        }
    }

    LaunchedEffect(month) { if (state !is LoadState.Idle) load() }

    AsyncContent(state = state, load = { load() }) { data ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Intern Attendance", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    modifier = Modifier.weight(1f))
                MonthSwitcher(month) { month = it }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    value = "${data.employees.size}", label = "People tracked", icon = Icons.Outlined.Groups,
                    tint = Brand.Blue, iconBg = Color.White.copy(alpha = 0.12f), modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = String.format(Locale.US, "%.1f h", data.totalHours), label = "Total hours",
                    icon = Icons.Filled.Functions, tint = Brand.Mint, iconBg = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f),
                )
            }
            if (data.employees.isEmpty()) {
                EmptyHint(Icons.Outlined.Groups, "No interns or check-ins for ${AttendanceMonth.label(month)}.")
            }
            data.employees.forEach { r ->
                InternRow(r) {
                    val enc = { s: String -> Uri.encode(s) }
                    nav.navigate("intern_attendance_detail/${enc(r.id)}?name=${enc(r.name)}&code=${enc(r.employeeCode)}&month=$month")
                }
            }
        }
    }
}

@Composable
private fun InternRow(r: AttendanceSummaryRow, onClick: () -> Unit) {
    Card(modifier = Modifier.clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(r.name, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    "${r.employeeCode}${if (r.isIntern) "" else " · non-intern"} · ${r.daysWorked} day${if (r.daysWorked == 1) "" else "s"}",
                    color = Brand.TextSecondary, fontSize = 12.sp,
                )
                Text("Last check-in: ${r.lastPunchDate?.let(AttendanceMonth::dayLabel) ?: "—"}",
                    color = Brand.TextMuted, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(String.format(Locale.US, "%.2f h", r.totalHours), color = Brand.Mint,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (r.clockedInNow) StatusPill("WORKING")
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Brand.TextMuted)
        }
    }
}

/** One intern's daily history (admin drill-down). */
@Composable
fun InternAttendanceDetailScreen(employeeId: String, name: String, code: String, initialMonth: String) {
    var month by rememberSaveable { mutableStateOf(initialMonth) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("$name · $code", color = Brand.TextSecondary, fontSize = 14.sp)
        AttendanceHistorySection(month = month, onMonthChange = { month = it }, employeeId = employeeId)
    }
}
