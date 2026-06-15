package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.LeaveType
import com.tertiaryinfotech.hrportal.ui.components.BrandScaffold
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.StatusBanner
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.ui.theme.GradientScreen
import com.tertiaryinfotech.hrportal.util.Fmt
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Apply-for-leave form. Posts to the existing /api/leave endpoint; the server computes
 * working days, proration, and balance deductions. Presented as a full-screen dialog.
 * Mirrors iOS ApplyLeaveView.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyLeaveSheet(
    types: List<LeaveType>,
    onDismiss: () -> Unit,
    onApplied: () -> Unit,
) {
    var typeId by remember {
        mutableStateOf(types.firstOrNull { it.code == "AL" }?.id ?: types.firstOrNull()?.id ?: "")
    }
    var startMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var endMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var dayType by remember { mutableStateOf("FULL_DAY") }
    var reason by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val df = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val isSingleDay = df.format(Date(startMillis)) == df.format(Date(endMillis))
    val selectedType = types.firstOrNull { it.id == typeId }
    val isMedical = selectedType?.let {
        it.code == "MC" || it.code == "SL" || it.name.contains("medical", ignoreCase = true)
    } ?: false

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        GradientScreen {
            BrandScaffold(
                title = "Apply for leave",
                onBack = onDismiss,
            ) { inner ->
                Column(
                    modifier = Modifier
                        .padding(inner)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    error?.let { StatusBanner(isError = true, text = it) }

                    // Leave type dropdown
                    FieldLabel("Leave type")
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        FieldBox(modifier = Modifier.menuAnchor()) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    selectedType?.name ?: "Select…",
                                    color = Color.White, modifier = Modifier.weight(1f),
                                )
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White)
                            }
                        }
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            types.forEach { t ->
                                DropdownMenuItem(text = { Text(t.name) }, onClick = { typeId = t.id; expanded = false })
                            }
                        }
                    }

                    if (isMedical) {
                        Text(
                            "For medical leave, please email your medical certificate to HR or attach it on the web portal.",
                            color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(12.dp),
                        )
                    }

                    // Dates
                    var showStart by remember { mutableStateOf(false) }
                    var showEnd by remember { mutableStateOf(false) }
                    FieldLabel("From")
                    FieldBox(modifier = Modifier.clickable { showStart = true }) {
                        Text(Fmt.date(df.format(Date(startMillis))), color = Color.White)
                    }
                    FieldLabel("To")
                    FieldBox(modifier = Modifier.clickable { showEnd = true }) {
                        Text(Fmt.date(df.format(Date(endMillis))), color = Color.White)
                    }

                    if (showStart) {
                        DateField(initial = startMillis, onPick = {
                            startMillis = it
                            if (endMillis < it) endMillis = it
                            showStart = false
                        }, onCancel = { showStart = false })
                    }
                    if (showEnd) {
                        DateField(initial = endMillis, minMillis = startMillis, onPick = {
                            endMillis = it; showEnd = false
                        }, onCancel = { showEnd = false })
                    }

                    if (isSingleDay) {
                        FieldLabel("Duration")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val opts = listOf("FULL_DAY" to "Full day", "AM_HALF" to "Morning", "PM_HALF" to "Afternoon")
                            opts.forEachIndexed { i, (value, label) ->
                                SegmentedButton(
                                    selected = dayType == value,
                                    onClick = { dayType = value },
                                    shape = SegmentedButtonDefaults.itemShape(i, opts.size),
                                ) { Text(label) }
                            }
                        }
                    }

                    FieldLabel("Reason (optional)")
                    TextField(
                        value = reason,
                        onValueChange = { reason = it },
                        placeholder = { Text("e.g. Family matters", color = Color.White.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.08f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Brand.Sky,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )

                    PremierButton(
                        title = "Submit request", icon = Icons.Filled.Send,
                        loading = submitting, enabled = typeId.isNotEmpty(),
                    ) {
                        error = null; submitting = true
                        val s = df.format(Date(startMillis))
                        val e = df.format(Date(endMillis))
                        val dt = if (isSingleDay) dayType else "FULL_DAY"
                        scope.launch {
                            try {
                                HrmsApi.applyLeave(typeId, s, e, dt, reason)
                                submitting = false; done = true
                            } catch (ex: Exception) {
                                submitting = false
                                error = ex.message ?: "Could not submit your leave request."
                            }
                        }
                    }
                }
            }
        }
    }

    if (done) {
        AlertDialog(
            onDismissRequest = onApplied,
            confirmButton = { TextButton(onClick = onApplied) { Text("Done") } },
            title = { Text("Request submitted") },
            text = { Text("Your leave request was submitted for approval.") },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(initial: Long, minMillis: Long? = null, onPick: (Long) -> Unit, onCancel: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial)
    DatePickerDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(onClick = {
                val picked = state.selectedDateMillis ?: initial
                onPick(if (minMillis != null && picked < minMillis) minMillis else picked)
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
}

@Composable
private fun FieldBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(14.dp),
    ) { content() }
}
