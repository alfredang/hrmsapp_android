package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.TimesheetDay
import com.tertiaryinfotech.hrportal.data.TimesheetResponse
import com.tertiaryinfotech.hrportal.ui.components.alphaIf
import com.tertiaryinfotech.hrportal.ui.components.BannerKind
import com.tertiaryinfotech.hrportal.ui.components.RefreshableColumn
import com.tertiaryinfotech.hrportal.ui.components.AsyncContent
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.LoadState
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.StatTile
import com.tertiaryinfotech.hrportal.ui.components.StatusBanner
import com.tertiaryinfotech.hrportal.ui.components.StatusPill
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private val HOUR_OPTIONS = listOf(0.0 to "Off", 4.0 to "Half", 8.0 to "Full")

/**
 * Weekly Timesheet — log hours worked on weekends and public holidays for Off-In-Lieu credit,
 * ported from the web's `/timesheet` `WeeklyTimesheet` (SCREEN_MAP.md). Week navigation
 * (prev/next, next capped at the current week, "Jump to current week"), one row per day Mon–Sun;
 * only non-work days get the Off/Half/Full hour selector (respecting per-day `isSubmittable`),
 * regular workdays render as non-editable rows. Live OT preview per day (8h → 1 day, 4h → 0.5)
 * plus a week total, and a confirm-then-submit "Submit for Approval" flow. Uses the data layer
 * that already existed unused: `HrmsApi.timesheet(weekStart)` / `HrmsApi.submitTimesheet(...)`.
 */
@Composable
fun WeeklyTimesheetScreen(nav: NavController) {
    val currentMonday = remember { mondayOfCurrentWeek() }
    var weekStart by remember { mutableStateOf(currentMonday) }
    var state by remember { mutableStateOf<LoadState<TimesheetResponse>>(LoadState.Idle) }
    // Per-date selected hours, seeded from the fetched week; only submittable days are editable.
    var selections by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var submitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Pair<Boolean, String>?>(null) } // isError to text
    var showConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        state = LoadState.Loading
        state = try {
            val data = HrmsApi.timesheet(weekStart)
            selections = data.days.associate { it.date to it.hours }
            LoadState.Loaded(data)
        } catch (e: Exception) {
            LoadState.Failed(e.message ?: "Could not load.")
        }
    }

    // Refetch whenever the shown week changes (also covers first appearance via AsyncContent).
    LaunchedEffect(weekStart) {
        if (state !is LoadState.Idle) {
            message = null
            load()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncContent(state = state, load = { load() }) { data ->
            val submittableDays = data.days.filter { it.isSubmittable }
            val weekOtTotal = data.days.sumOf { otPreview(selections[it.date] ?: 0.0) }
            // Off (0h) is sent only for a day that already had hours, which clears it. Otherwise
            // leaving a day on Off would raise an empty pending row.
            val pendingEntries = submittableDays
                .map { Triple(it.date, selections[it.date] ?: 0.0, it.hours) }
                .filter { (_, selected, saved) -> selected > 0.0 || saved > 0.0 }
                .map { (date, selected, _) -> date to selected }

            RefreshableColumn(scope, { load() }) {
                WeekNav(
                    weekStart = weekStart,
                    atCurrentWeek = weekStart == currentMonday,
                    onPrev = { weekStart = addWeeks(weekStart, -1) },
                    onNext = { weekStart = addWeeks(weekStart, 1) },
                    onJumpToCurrent = { weekStart = currentMonday },
                )

                if (data.isLocked) {
                    StatusBanner(BannerKind.Info, "This week is locked and can no longer be edited.")
                }

                StatTile(
                    value = Fmt.days(weekOtTotal),
                    label = "Off In Lieu this week",
                    icon = Icons.Filled.Functions,
                    tint = Brand.Mint,
                    )

                message?.let { (isError, text) -> StatusBanner(isError = isError, text = text) }

                if (data.days.none { it.isNonWorkDay }) {
                    EmptyHint(Icons.Outlined.EventAvailable, "No weekends or public holidays this week.")
                }
                data.days.forEach { day ->
                    DayRow(
                        day = day,
                        selectedHours = selections[day.date] ?: 0.0,
                        locked = data.isLocked,
                        onHoursChange = { h -> selections = selections + (day.date to h) },
                    )
                }

                PremierButton(
                    title = "Submit for Approval",
                    icon = Icons.Filled.Send,
                    loading = submitting,
                    enabled = !data.isLocked && submittableDays.isNotEmpty() && !submitting,
                ) {
                    if (pendingEntries.isEmpty()) {
                        message = true to "No hours to submit."
                    } else {
                        showConfirm = true
                    }
                }

                Text(
                    "Log hours worked on weekends and public holidays. Submit by 11:30 PM SGT. " +
                        "Off In Lieu days are credited after admin approval.",
                    color = Brand.TextMuted, fontSize = 11.sp,
                )
            }

            if (showConfirm) {
                AlertDialog(
                    onDismissRequest = { showConfirm = false },
                    title = { Text("Submit timesheet?") },
                    text = { Text("Submit this week's weekend and public-holiday hours for admin approval?") },
                    confirmButton = {
                        TextButton(onClick = {
                            showConfirm = false
                            submitting = true
                            message = null
                            scope.launch {
                                try {
                                    HrmsApi.submitTimesheet(weekStart, pendingEntries)
                                    message = false to "Your hours were submitted. Off In Lieu days are credited after admin approval."
                                    load()
                                } catch (e: Exception) {
                                    message = true to (e.message ?: "Could not submit your hours.")
                                } finally {
                                    submitting = false
                                }
                            }
                        }) { Text("Submit") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
                    },
                )
            }
        }
    }
}

@Composable
private fun WeekNav(
    weekStart: String,
    atCurrentWeek: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onJumpToCurrent: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            WeekButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous week", onPrev)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Week of", color = Brand.TextPrimary.copy(alpha = 0.6f), fontSize = 11.sp)
                Text(Fmt.date(weekStart), color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            WeekButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next week", onNext)
        }
        if (!atCurrentWeek) {
            TextButton(onClick = onJumpToCurrent) {
                Text("Jump to current week", color = Brand.Accent, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun WeekButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp).clip(CircleShape).background(Brand.Field),
    ) {
        Icon(icon, contentDescription = description, tint = Brand.Accent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayRow(day: TimesheetDay, selectedHours: Double, locked: Boolean, onHoursChange: (Double) -> Unit) {
    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${day.dayName} · ${Fmt.date(day.date)}",
                    color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                )
                if (day.isPublicHoliday && !day.phName.isNullOrBlank()) {
                    Text(day.phName, color = Brand.Amber, fontSize = 11.sp)
                }
            }
            day.status?.let { StatusPill(it) }
        }
        if (day.isNonWorkDay) {
            Box(modifier = Modifier.padding(top = 10.dp).alphaIf(!(day.isSubmittable && !locked), 0.5f)) {
                SegmentedPicker(
                    options = HOUR_OPTIONS.map { (hours, label) -> hours.toString() to label },
                    selected = selectedHours.toString(),
                    onSelect = { if (day.isSubmittable && !locked) onHoursChange(it.toDouble()) },
                )
            }
            val ot = otPreview(selectedHours)
            if (ot > 0) {
                Text(
                    "Earns ${Fmt.days(ot)} Off In Lieu",
                    color = Brand.Mint, fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.EventBusy, contentDescription = null, tint = Brand.TextMuted, modifier = Modifier.padding(0.dp))
                Text("Workday", color = Brand.TextMuted, fontSize = 12.sp)
            }
        }
        if (!day.adminComment.isNullOrBlank()) {
            Text(
                "Admin: ${day.adminComment}",
                color = Brand.TextSecondary, fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

// MARK: - Helpers

/** OT preview matching the web: 8h → 1 day, 4h → 0.5 day, anything else → 0. */
private fun otPreview(hours: Double): Double = when {
    hours >= 8.0 -> 1.0
    hours >= 4.0 -> 0.5
    else -> 0.0
}

private val WEEK_DF = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("Asia/Singapore")
}

/** The Monday of the current week in Asia/Singapore as "yyyy-MM-dd". (java.util.Calendar rather
 *  than java.time because minSdk is 24 and the project doesn't enable core-library desugaring.) */
private fun mondayOfCurrentWeek(): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Singapore"))
    // Walk back to Monday (Calendar.MONDAY = 2; Sunday belongs to the week that started 6 days ago).
    val dow = cal.get(Calendar.DAY_OF_WEEK)
    val back = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
    cal.add(Calendar.DAY_OF_MONTH, -back)
    return WEEK_DF.format(cal.time)
}

/** Shift a "yyyy-MM-dd" Monday by [weeks] whole weeks. */
private fun addWeeks(weekStart: String, weeks: Int): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Singapore"))
    cal.time = WEEK_DF.parse(weekStart) ?: return weekStart
    cal.add(Calendar.DAY_OF_MONTH, weeks * 7)
    return WEEK_DF.format(cal.time)
}

/** "7 Jul – 13 Jul 2026" style label for the Mon–Sun range starting at [weekStart]. */
private fun weekRangeLabel(weekStart: String): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Singapore"))
    cal.time = WEEK_DF.parse(weekStart) ?: return weekStart
    val startLabel = SimpleDateFormat("d MMM", Locale.getDefault()).format(cal.time)
    cal.add(Calendar.DAY_OF_MONTH, 6)
    val endLabel = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(cal.time)
    return "$startLabel – $endLabel"
}
