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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.ApiException
import com.tertiaryinfotech.hrportal.data.AuthException
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.TimesheetDay
import com.tertiaryinfotech.hrportal.data.TimesheetResponse
import com.tertiaryinfotech.hrportal.ui.components.AsyncContent
import com.tertiaryinfotech.hrportal.ui.components.BrandScaffold
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.LoadState
import com.tertiaryinfotech.hrportal.ui.components.StatusPill
import com.tertiaryinfotech.hrportal.ui.components.alphaIf
import com.tertiaryinfotech.hrportal.ui.components.clickableIf
import com.tertiaryinfotech.hrportal.ui.theme.Brand
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
 * screen (like the web one) only concerns weekend/PH OT. No clock-in/out here (product decision).
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

    Column(modifier = Modifier.fillMaxSize()) {
        AsyncContent(state = state, load = { load() }) { data ->
                val nonWorkDays = data.days.filter { it.isNonWorkDay }
                val submittableDays = nonWorkDays.filter { it.isSubmittable }
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Weekly Timesheet", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text(
                            "If you work on a weekend or public holiday, log your hours here. Off In Lieu days are credited after admin approval.",
                            color = Brand.TextSecondary, fontSize = 13.sp,
                        )
                    }
                    Spacer12()
                    WeekNavHeader(
                        weekStart = weekStart,
                        isCurrentWeek = weekStart == currentWeek,
                        onPrev = { weekStart = addWeeks(weekStart, -1) },
                        onNext = { val n = addWeeks(weekStart, 1); if (n <= currentWeek) weekStart = n },
                        onJumpToday = { weekStart = currentWeek },
                    )
                    Spacer12()

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
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week", tint = Brand.TextSecondary)
            }
            Text(formatWeekRange(weekStart), color = Brand.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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

@Composable
private fun DayCard(day: TimesheetDay, hours: Double, onHoursChange: (Double) -> Unit) {
    val editable = day.isSubmittable && day.status != "APPROVED"
    val tagColor = if (day.isPublicHoliday) Brand.Amber else Brand.Emerald
    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Text(day.dayName, color = tagColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(formatDayLabel(day.date), color = Brand.TextPrimary, fontSize = 14.sp)
            }
            TagPill(if (day.isPublicHoliday) day.phName ?: "Public Holiday" else "Weekend", tagColor)
        }

        FieldRow("HOURS") {
            if (editable) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HourChip("—", 0.0, hours, onHoursChange)
                    HourChip("4h", 4.0, hours, onHoursChange)
                    HourChip("8h", 8.0, hours, onHoursChange)
                }
            } else {
                Text(
                    if (hours == 0.0) "—" else "${hours.toInt()}h",
                    color = if (hours == 0.0) Brand.TextMuted else Brand.TextPrimary,
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                )
            }
        }

        val earned = otForHours(hours)
        FieldRow("OFF IN LIEU") {
            Text(
                if (earned > 0) "+${if (earned % 1.0 == 0.0) earned.toInt().toString() else earned.toString()} d" else "—",
                color = if (earned > 0) Brand.Emerald else Brand.TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            )
        }

        FieldRow("STATUS") {
            if (day.status != null) StatusPill(day.status) else Text("—", color = Brand.TextMuted, fontSize = 13.sp)
        }

        if (!day.adminComment.isNullOrEmpty()) {
            Text(day.adminComment, color = Brand.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun FieldRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Brand.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        content()
    }
}

@Composable
private fun TagPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, color, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
