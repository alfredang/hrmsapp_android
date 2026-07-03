package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.hrportal.data.ApiException
import com.tertiaryinfotech.hrportal.data.AttendancePunch
import com.tertiaryinfotech.hrportal.data.AttendanceResponse
import com.tertiaryinfotech.hrportal.data.AuthException
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.TimesheetDay
import com.tertiaryinfotech.hrportal.data.TimesheetResponse
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.ui.components.AsyncContent
import com.tertiaryinfotech.hrportal.ui.components.BrandScaffold
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.LoadState
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.StatusPill
import com.tertiaryinfotech.hrportal.ui.components.alphaIf
import com.tertiaryinfotech.hrportal.ui.components.clickableIf
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Weekly timesheet — logs weekend/public-holiday hours for Off-In-Lieu credit. Ported 1:1 from
 * the web's `WeeklyTimesheet` component (`src/components/timesheet/weekly-timesheet.tsx`):
 * week navigation, an Off/4h/8h chip selector per submittable non-work day, a submit-for-approval
 * confirmation, and status badges once submitted. Regular workdays aren't tracked here — this
 * screen (like the web one) only concerns weekend/PH OT.
 */
@Composable
fun TimesheetScreen(nav: NavController) {
    val currentWeek = remember { currentMonday() }
    var weekStart by remember { mutableStateOf(currentWeek) }
    var state by remember { mutableStateOf<LoadState<TimesheetResponse>>(LoadState.Idle) }
    var draft by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var submitting by remember { mutableStateOf(false) }
    var savedMsg by remember { mutableStateOf<String?>(null) }
    var showConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        state = LoadState.Loading
        state = try {
            val data = HrmsApi.timesheet(weekStart)
            draft = data.days.filter { it.isNonWorkDay }.associate { it.date to it.hours }
            savedMsg = null
            LoadState.Loaded(data)
        } catch (e: AuthException) {
            LoadState.Failed(e.message ?: "Could not load.")
        } catch (e: ApiException) {
            LoadState.Failed(e.message ?: "Could not load.")
        } catch (e: Exception) {
            LoadState.Failed("Could not load.")
        }
    }

    LaunchedEffect(weekStart) { load() }

    BrandScaffold(title = "Timesheet", onBack = { nav.popBackStack() }) { inner ->
    Column(modifier = Modifier.padding(inner).fillMaxSize()) {
        ClockInOutCard()
        SectionDivider()
        WeekNavHeader(
                weekStart = weekStart,
                isCurrentWeek = weekStart == currentWeek,
                onPrev = { weekStart = addWeeks(weekStart, -1) },
                onNext = { val n = addWeeks(weekStart, 1); if (n <= currentWeek) weekStart = n },
                onJumpToday = { weekStart = currentWeek },
            )
            AsyncContent(state = state, load = { load() }) { data ->
                val nonWorkDays = data.days.filter { it.isNonWorkDay }
                val submittableDays = nonWorkDays.filter { it.isSubmittable }
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (nonWorkDays.isEmpty()) {
                        Text(
                            "No weekends or public holidays this week.",
                            color = Brand.TextSecondary, fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    } else {
                        nonWorkDays.forEach { day ->
                            DayCard(
                                day = day,
                                hours = draft[day.date] ?: day.hours,
                                onHoursChange = { h -> draft = draft + (day.date to h) },
                            )
                        }
                    }

                    if (submittableDays.isNotEmpty()) {
                        val otPreview = submittableDays.sumOf { otForHours(draft[it.date] ?: 0.0) }
                        Spacer12()
                        if (otPreview > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.WbSunny, contentDescription = null, tint = Brand.Emerald, modifier = Modifier.size(16.dp))
                                Text(
                                    "If approved: +$otPreview Off In Lieu day${if (otPreview != 1.0) "s" else ""}",
                                    color = Brand.Emerald, fontSize = 12.sp,
                                )
                            }
                        }
                        savedMsg?.let {
                            Text(it, color = if (it.contains("Failed") || it.contains("Could not")) Brand.Red else Brand.Emerald, fontSize = 12.sp)
                        }
                        val canSubmit = !submitting && submittableDays.any { (draft[it.date] ?: 0.0) > 0.0 }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Brand.ControlHeight.dp)
                                .clip(RoundedCornerShape(Brand.Corner.dp))
                                .background(Brand.Primary)
                                .alphaIf(!canSubmit, 0.55f)
                                .clickableIf(canSubmit) { showConfirm = true },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (submitting) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Filled.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Text("Submit for Approval", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }

                    Text(
                        "Log hours worked on weekends and public holidays. Submit by 11:30 PM SGT. Off In Lieu days are credited after admin approval.",
                        color = Brand.TextMuted, fontSize = 11.sp,
                    )
                }
            }
        }
    }

    if (showConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Submit for approval?") },
            text = { Text("Your hours will be sent to admin for review. Off In Lieu days will be credited once approved.") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    submitting = true
                    scope.launch {
                        try {
                            val entries = state.let { it as? LoadState.Loaded<TimesheetResponse> }?.value
                                ?.days?.filter { it.isNonWorkDay && it.isSubmittable }
                                ?.mapNotNull { d -> (draft[d.date] ?: 0.0).takeIf { it > 0.0 }?.let { d.date to it } }
                                ?: emptyList()
                            if (entries.isEmpty()) {
                                savedMsg = "No hours to submit."
                            } else {
                                HrmsApi.submitTimesheet(weekStart, entries)
                                savedMsg = "Submitted for admin approval."
                                load()
                            }
                        } catch (e: Exception) {
                            savedMsg = (e as? ApiException)?.message ?: "Failed to save."
                        } finally {
                            submitting = false
                        }
                    }
                }) { Text("Submit") }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Go Back") } },
        )
    }
}

@Composable
private fun WeekNavHeader(weekStart: String, isCurrentWeek: Boolean, onPrev: () -> Unit, onNext: () -> Unit, onJumpToday: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week", tint = Brand.TextSecondary)
            }
            Text(formatWeekRange(weekStart), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            IconButton(onClick = onNext, enabled = !isCurrentWeek) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week",
                    tint = if (isCurrentWeek) Brand.TextMuted else Brand.TextSecondary,
                )
            }
        }
        if (!isCurrentWeek) {
            Text("Jump to current week", color = Brand.Primary, fontSize = 12.sp, modifier = Modifier.clickable(onClick = onJumpToday))
        }
    }
}

/** Clock-in/out card — mirrors `AttendancePunch`/`/api/mobile/attendance` (Prisma model +
 *  routes added directly to tertiary-hrms this session). One punch per employee per SGT day;
 *  ticks a live elapsed-time display while clocked in but not yet out. */
@Composable
private fun ClockInOutCard() {
    var state by remember { mutableStateOf<LoadState<AttendanceResponse>>(LoadState.Idle) }
    var acting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        state = LoadState.Loading
        state = try {
            LoadState.Loaded(HrmsApi.attendance())
        } catch (e: Exception) {
            LoadState.Failed((e as? ApiException)?.message ?: "Could not load.")
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        when (val s = state) {
            is LoadState.Idle, is LoadState.Loading -> {
                Card {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(color = Brand.Primary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    }
                }
            }
            is LoadState.Failed -> {
                Card {
                    Text(s.message, color = Brand.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                    PremierButton(title = "Retry", icon = Icons.Filled.AccessTime) { scope.launch { load() } }
                }
            }
            is LoadState.Loaded -> {
            val data = s.value
            val today = data.today
            val clockInAt = Fmt.parse(today?.clockIn)?.time
            val clockOutAt = Fmt.parse(today?.clockOut)?.time
            var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }

            LaunchedEffect(clockInAt, clockOutAt) {
                if (clockInAt != null && clockOutAt == null) {
                    while (true) {
                        nowMs = System.currentTimeMillis()
                        delay(1000)
                    }
                }
            }

            Card {
                Text("Today", color = Brand.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                when {
                    clockInAt == null -> {
                        Text(
                            "You haven't clocked in yet.", color = Color.White,
                            fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                        )
                    }
                    clockOutAt == null -> {
                        Text(
                            formatElapsed(nowMs - clockInAt), color = Brand.Emerald,
                            fontSize = 28.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            "Clocked in at ${formatTime(clockInAt)}", color = Brand.TextSecondary,
                            fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                    else -> {
                        Text(
                            formatElapsed(clockOutAt - clockInAt), color = Color.White,
                            fontSize = 22.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            "Worked today · ${formatTime(clockInAt)} – ${formatTime(clockOutAt)}",
                            color = Brand.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                }
                error?.let { Text(it, color = Brand.Red, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)) }
                PremierButton(
                    title = when {
                        clockOutAt != null -> "Clocked Out"
                        clockInAt == null -> "Clock In"
                        else -> "Clock Out"
                    },
                    icon = Icons.Filled.AccessTime,
                    loading = acting,
                    enabled = clockOutAt == null,
                ) {
                    scope.launch {
                        acting = true; error = null
                        try {
                            if (clockInAt == null) HrmsApi.clockIn() else HrmsApi.clockOut()
                            load()
                        } catch (e: Exception) {
                            error = (e as? ApiException)?.message ?: "Something went wrong."
                        } finally {
                            acting = false
                        }
                    }
                }

                if (data.recent.isNotEmpty()) {
                    Spacer12()
                    data.recent.filter { it.id != today?.id }.take(6).forEach { p -> AttendanceRow(p) }
                }
            }
            }
        }
    }
}

@Composable
private fun AttendanceRow(p: AttendancePunch) {
    val inAt = Fmt.parse(p.clockIn)?.time
    val outAt = Fmt.parse(p.clockOut)?.time
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(Fmt.date(p.date, short = true), color = Brand.TextSecondary, fontSize = 11.sp)
        Text(
            if (inAt != null && outAt != null) "${formatTime(inAt)} – ${formatTime(outAt)} · ${formatElapsed(outAt - inAt)}"
            else if (inAt != null) "${formatTime(inAt)} – —" else "—",
            color = Color.White, fontSize = 11.sp,
        )
    }
}

@Composable
private fun SectionDivider() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Brand.Border))
}

private fun formatElapsed(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return String.format(Locale.US, "%d:%02d:%02d", h, m, s)
}

private fun formatTime(ms: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(java.util.Date(ms))

@Composable
private fun DayCard(day: TimesheetDay, hours: Double, onHoursChange: (Double) -> Unit) {
    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "${day.dayName} · ${formatDayLabel(day.date)}",
                        color = if (day.isPublicHoliday) Brand.Amber else Brand.Emerald,
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    )
                }
                Text(
                    if (day.isPublicHoliday) "PH: ${day.phName}" else "Weekend",
                    color = Brand.TextSecondary, fontSize = 11.sp,
                )
                if (day.status != null) {
                    Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(day.status)
                    }
                    if (!day.adminComment.isNullOrEmpty()) {
                        Text(day.adminComment, color = Brand.TextMuted, fontSize = 10.sp)
                    }
                } else if (day.isSubmittable) {
                    Text("Not submitted", color = Brand.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }

            if (day.isSubmittable && day.status != "APPROVED") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HourChip("—", 0.0, hours, onHoursChange)
                    HourChip("4h", 4.0, hours, onHoursChange)
                    HourChip("8h", 8.0, hours, onHoursChange)
                }
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (hours == 0.0) "—" else "${hours.toInt()}h",
                        color = if (hours == 0.0) Brand.TextMuted else Brand.Emerald,
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    )
                    val earned = otForHours(hours)
                    if (earned > 0) Text("+$earned d", color = Brand.Emerald, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun HourChip(label: String, value: Double, selected: Double, onSelect: (Double) -> Unit) {
    val isSelected = selected == value
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected && value > 0) Brand.Emerald else Brand.Border)
            .border(1.dp, if (isSelected) Color.Transparent else Brand.BorderLight, RoundedCornerShape(8.dp))
            .clickable { onSelect(value) }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(label, color = if (isSelected) Color.White else Brand.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

private fun otForHours(hours: Double): Double = when {
    hours >= 8 -> 1.0
    hours >= 4 -> 0.5
    else -> 0.0
}

// MARK: - UTC Monday-week helpers (mirrors weekly-timesheet.tsx's getMonday/addWeeks)

private fun utcCalendar(): Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

private fun calFrom(dateStr: String): Calendar {
    val (y, m, d) = dateStr.split("-").map { it.toInt() }
    val cal = utcCalendar()
    cal.clear()
    cal.set(y, m - 1, d)
    return cal
}

private fun isoKey(cal: Calendar): String =
    String.format(Locale.US, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))

private fun currentMonday(): String = mondayOf(isoKey(utcCalendar()))

private fun mondayOf(dateStr: String): String {
    val cal = calFrom(dateStr)
    val dow = cal.get(Calendar.DAY_OF_WEEK) // SUNDAY=1 .. SATURDAY=7
    val diff = if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow
    cal.add(Calendar.DAY_OF_MONTH, diff)
    return isoKey(cal)
}

private fun addWeeks(dateStr: String, n: Int): String {
    val cal = calFrom(dateStr)
    cal.add(Calendar.DAY_OF_MONTH, n * 7)
    return isoKey(cal)
}

private fun formatWeekRange(weekStart: String): String {
    val fmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
    val start = calFrom(weekStart)
    val end = calFrom(weekStart).apply { add(Calendar.DAY_OF_MONTH, 6) }
    return "${fmt.format(start.time)} – ${fmt.format(end.time)}"
}

private fun formatDayLabel(dateStr: String): String {
    val fmt = SimpleDateFormat("d MMM", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
    return fmt.format(calFrom(dateStr).time)
}
