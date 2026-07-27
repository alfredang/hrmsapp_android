package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
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
import com.tertiaryinfotech.hrportal.data.ApiException
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.StatusBanner
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TIME_OFF_REASONS = listOf("EXAMS" to "Exams", "EMERGENCY" to "Emergency", "OTHERS" to "Others")

/** All selectable times, in 15-minute increments across the day ("00:00" … "23:45") — matches the
 *  house preference for dropdown pickers (ExposedDropdownMenuBox, like the leave-type selector)
 *  over the Material time-dial dialog. */
private val TIME_OPTIONS: List<String> =
    (0 until 24 * 60 step 15).map { String.format(Locale.US, "%02d:%02d", it / 60, it % 60) }

/**
 * Request-time-off form — a real nav destination sharing the drawer top bar (route starts with
 * "time_off" so MainScaffold's startsWith matching keeps the shared "Time Off" chrome), structured
 * after [ApplyLeaveScreen]: FieldLabel/FieldBox/DateField, dropdown pickers, client-side
 * validation mirroring the server's (end > start, detail required for Others), StatusBanner
 * errors, and the saved-state-handle result flag on success.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestTimeOffScreen(nav: NavController) {
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var startTime by remember { mutableStateOf("14:00") }
    var endTime by remember { mutableStateOf("16:00") }
    var reason by remember { mutableStateOf("EXAMS") }
    var reasonDetail by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val df = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val isOthers = reason == "OTHERS"

    // Client-side mirror of the server's validation (the server still re-checks everything).
    val startMin = timeOffMinutes(startTime)
    val endMin = timeOffMinutes(endTime)
    val timesValid = startMin != null && endMin != null && endMin > startMin
    val hoursPreview = if (timesValid) (endMin!! - startMin!!) / 60.0 else null
    val canSubmit = timesValid && (!isOthers || reasonDetail.isNotBlank()) && !submitting

    val onSubmitted = {
        nav.previousBackStackEntry?.savedStateHandle?.set("time_off_submitted", true)
        nav.popBackStack()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Request Time Off", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text("Take a few hours off for exams or emergencies", color = Brand.TextSecondary, fontSize = 14.sp)
        }

        error?.let { StatusBanner(isError = true, text = it) }

        // Date
        var showDate by remember { mutableStateOf(false) }
        FieldLabel("Date *")
        FieldBox(modifier = Modifier.clickable { showDate = true }) {
            Text(Fmt.date(df.format(Date(dateMillis))), color = Brand.TextPrimary)
        }
        if (showDate) {
            DateField(initial = dateMillis, onPick = { dateMillis = it; showDate = false }, onCancel = { showDate = false })
        }

        // Start / end time — dropdowns of 15-minute increments, rendered 12-hour.
        FieldLabel("Start time *")
        TimeDropdown(selected = startTime, onSelect = { startTime = it })
        FieldLabel("End time *")
        TimeDropdown(selected = endTime, onSelect = { endTime = it })

        if (!timesValid) {
            Text(
                "End time must be later than start time.",
                color = Brand.Amber, fontSize = 11.sp,
            )
        } else if (hoursPreview != null) {
            Text(
                "${Fmt.num(hoursPreview)} hour${if (hoursPreview == 1.0) "" else "s"} of time off",
                color = Brand.Blue, fontSize = 11.sp,
            )
        }

        // Reason
        FieldLabel("Reason *")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            TIME_OFF_REASONS.forEachIndexed { i, (value, label) ->
                SegmentedButton(
                    selected = reason == value,
                    onClick = { reason = value },
                    shape = SegmentedButtonDefaults.itemShape(i, TIME_OFF_REASONS.size),
                ) { Text(label) }
            }
        }

        if (isOthers) {
            FieldLabel("Please specify *")
            TextField(
                value = reasonDetail,
                onValueChange = { reasonDetail = it },
                placeholder = { Text("Describe the reason...", color = Brand.TextMuted) },
                modifier = Modifier.fillMaxWidth().height(110.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Brand.Border,
                    unfocusedContainerColor = Brand.Border,
                    focusedTextColor = Brand.TextPrimary,
                    unfocusedTextColor = Brand.TextPrimary,
                    focusedIndicatorColor = Brand.Primary,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
        }

        PremierButton(
            title = "Submit Request",
            icon = Icons.Filled.Send,
            loading = submitting,
            enabled = canSubmit,
        ) {
            error = null
            submitting = true
            scope.launch {
                try {
                    HrmsApi.createTimeOff(
                        date = df.format(Date(dateMillis)),
                        startTime = startTime,
                        endTime = endTime,
                        reason = reason,
                        reasonDetail = reasonDetail.takeIf { isOthers && it.isNotBlank() },
                    )
                    submitting = false
                    done = true
                } catch (ex: Exception) {
                    submitting = false
                    error = (ex as? ApiException)?.message ?: "Could not submit your time-off request."
                }
            }
        }
    }

    if (done) {
        AlertDialog(
            onDismissRequest = { onSubmitted() },
            confirmButton = { TextButton(onClick = { onSubmitted() }) { Text("Done") } },
            title = { Text("Request submitted") },
            text = { Text("Your time-off request was submitted for approval.") },
        )
    }
}

/** A FieldBox-hosted dropdown of [TIME_OPTIONS] — same ExposedDropdownMenuBox pattern as the
 *  leave-type selector in ApplyLeaveScreen, showing 12-hour labels for the "HH:mm" values. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        FieldBox(modifier = Modifier.menuAnchor()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(timeOff12h(selected), color = Brand.TextPrimary, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Brand.TextPrimary)
            }
        }
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TIME_OPTIONS.forEach { t ->
                DropdownMenuItem(
                    text = { Text(timeOff12h(t)) },
                    onClick = { onSelect(t); expanded = false },
                )
            }
        }
    }
}

/** "HH:mm" → minutes since midnight, or null when malformed. */
private fun timeOffMinutes(hhmm: String): Int? {
    val parts = hhmm.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val m = parts.getOrNull(1)?.toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}
