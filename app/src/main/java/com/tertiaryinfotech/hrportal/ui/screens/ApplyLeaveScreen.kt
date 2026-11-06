package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.ApiException
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.LeaveBalance
import com.tertiaryinfotech.hrportal.data.LeaveType
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.AttachmentSection
import com.tertiaryinfotech.hrportal.ui.components.PickedAttachment
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.StatusBanner
import com.tertiaryinfotech.hrportal.ui.components.encodeAttachment
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.ui.theme.Spacing
import com.tertiaryinfotech.hrportal.util.Fmt
import com.tertiaryinfotech.hrportal.util.WorkingDays
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** Apply-for-leave form (iOS `ApplyLeaveView`), a normal destination inside the app shell. */
@Composable
fun ApplyLeaveScreen(nav: NavController, initialTypeCode: String?) {
    AsyncListScreen(fetch = { HrmsApi.leave() }) { data ->
        item {
            ApplyLeaveForm(
                types = data.types,
                balances = data.balances,
                initialTypeCode = initialTypeCode,
                onApplied = {
                    nav.previousBackStackEntry?.savedStateHandle?.set("leave_applied", true)
                    nav.popBackStack()
                },
            )
        }
    }
}

/**
 * Layout follows iOS. The server computes working days and the deduction, so no day count is sent.
 * The annual-leave panel (advance leave, Off In Lieu, deficit preview) goes beyond the iOS form: it
 * ports the web's `leave-request-form.tsx` and is an estimate, since only the server knows the
 * prorated full-year entitlement.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplyLeaveForm(
    types: List<LeaveType>,
    balances: List<LeaveBalance>,
    initialTypeCode: String? = null,
    onApplied: () -> Unit,
) {
    var typeId by remember {
        mutableStateOf(
            types.firstOrNull { it.code == initialTypeCode }?.id
                ?: types.firstOrNull { it.code == "AL" }?.id
                ?: types.firstOrNull()?.id
                ?: "",
        )
    }
    var startMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var endMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var dayType by remember { mutableStateOf("FULL_DAY") }
    var halfDayPosition by remember { mutableStateOf<String?>(null) }
    var reason by remember { mutableStateOf("") }
    var attachment by remember { mutableStateOf<PickedAttachment?>(null) }
    var useAdvance by remember { mutableStateOf(false) }
    var useOt by remember { mutableStateOf(false) }
    var otDaysToUse by remember { mutableStateOf(0.0) }
    var holidays by remember { mutableStateOf<Set<String>>(emptySet()) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val df = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val isSingleDay = df.format(Date(startMillis)) == df.format(Date(endMillis))
    val isMultiDay = !isSingleDay
    val selectedType = types.firstOrNull { it.id == typeId }
    val isMC = selectedType?.code == "MC" || selectedType?.code == "SL"
    val isAL = selectedType?.code == "AL"
    val isOT = selectedType?.code == "AL_OT"

    // AL/OT context — derived from the same balances the rest of the Leave screen already fetched.
    val otBalance = balances.firstOrNull { it.code == "AL_OT" }?.available ?: 0.0
    val alBalance = balances.firstOrNull { it.code == "AL" }
    val alPersonalEntitlement = alBalance?.entitlement ?: 0.0
    val alFullAvailable = alPersonalEntitlement + (alBalance?.carriedOver ?: 0.0) -
        (alBalance?.used ?: 0.0) - (alBalance?.pending ?: 0.0)

    // Every year the range covers, so a Dec-Jan request also skips 1 Jan.
    val startYear = df.format(Date(startMillis)).take(4).toInt()
    val endYear = df.format(Date(endMillis)).take(4).toInt()
    LaunchedEffect(startYear, endYear) {
        val dates = mutableSetOf<String>()
        for (year in startYear..maxOf(startYear, endYear)) {
            try {
                dates += HrmsApi.publicHolidays(year).dates
            } catch (_: Exception) { /* preview only; the server still computes the real count */ }
        }
        holidays = dates
    }

    // Working-days computation — mirrors calculateWorkingDays() exactly.
    val wd = remember(startMillis, endMillis, holidays) {
        if (startMillis > endMillis) null else WorkingDays.calculate(Date(startMillis), Date(endMillis), holidays)
    }
    val daysNum: Double = when {
        wd == null -> 0.0
        isSingleDay -> if (wd.workingDays == 0) 0.0 else if (dayType == "FULL_DAY") 1.0 else 0.5
        else -> {
            val base = wd.workingDays - (if (halfDayPosition != null) 0.5 else 0.0)
            if (base > 0) base else 0.0
        }
    }
    val showBreakdown = wd != null && (
        (isSingleDay && wd.workingDays == 0) || (isMultiDay && wd.calendarDays != wd.workingDays)
    )

    // Reset the irrelevant day-type control when the date range shape changes, and when a
    // non-AL type is selected (mirrors the web's own reset effects).
    LaunchedEffect(isSingleDay) { if (isSingleDay) halfDayPosition = null else dayType = "FULL_DAY" }
    LaunchedEffect(isAL) { if (!isAL) { dayType = "FULL_DAY"; halfDayPosition = null } }

    // Same arithmetic as the web form: earned balance first, then advance if switched on, and
    // Off In Lieu covers the deficit before any advance so each day is only subtracted once.
    val availableNow = max(0.0, alBalance?.available ?: 0.0)
    val advanceAvailable = max(0.0, alFullAvailable - availableNow)
    val alCover = if (useAdvance) availableNow + advanceAvailable else availableNow
    val deficitBeforeOt = if (isAL) max(0.0, daysNum - alCover) else 0.0
    val nonEarnedDays = if (isAL) max(0.0, daysNum - availableNow) else 0.0
    val effectiveOtUsed = if (useOt) minOf(otDaysToUse, otBalance, daysNum) else 0.0
    val otForDeficit = min(effectiveOtUsed, deficitBeforeOt)
    val otForAdvance = effectiveOtUsed - otForDeficit
    val advanceBeforeOt = if (isAL && useAdvance) max(0.0, min(daysNum, alCover) - availableNow) else 0.0
    val advanceDays = max(0.0, advanceBeforeOt - otForAdvance)
    val deficitDays = if (isAL) max(0.0, deficitBeforeOt - otForDeficit) else 0.0
    val fullyWithinEntitlement = isAL && daysNum <= alCover
    val otSliderMax = min(otBalance, nonEarnedDays)

    LaunchedEffect(useOt, deficitBeforeOt, nonEarnedDays, otBalance) {
        if (!useOt) { otDaysToUse = 0.0; return@LaunchedEffect }
        val target = if (deficitBeforeOt > 0) deficitBeforeOt else nonEarnedDays
        otDaysToUse = min(target, otBalance)
    }

    val canSubmit = typeId.isNotEmpty() && daysNum >= 0.5 && !(isOT && daysNum > otBalance)

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        error?.let { StatusBanner(isError = true, text = it) }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Leave type")
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                FieldBox(modifier = Modifier.menuAnchor()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            selectedType?.let { leaveTypeLabel(it, otBalance, availableNow) } ?: "Select\u2026",
                            color = Brand.TextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = Brand.TextMuted, modifier = Modifier.size(18.dp))
                    }
                }
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    types.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(leaveTypeLabel(t, otBalance, availableNow)) },
                            onClick = { typeId = t.id; expanded = false },
                        )
                    }
                }
            }
            if (selectedType?.paid == false) {
                Text("This leave type is unpaid.", color = Brand.Amber, fontSize = 12.sp)
            }
            if (isOT) {
                Text(
                    "You have ${Fmt.num(otBalance)} Off In Lieu day(s) earned from weekend/public holiday work.",
                    color = Brand.Mint, fontSize = 12.sp,
                )
            }
        }

        if (isMC) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldLabel("Medical certificate")
                FieldBox {
                    AttachmentSection(
                        attachment = attachment,
                        onChange = { attachment = it },
                        onError = { error = it },
                        takeLabel = "Take photo of MC",
                        baseName = "medical-certificate",
                    )
                    if (attachment == null) {
                        Text(
                            "Your MC is filed to your Medical Certificates folder and attached to this request.",
                            color = Brand.TextPrimary.copy(alpha = 0.6f), fontSize = 11.sp,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }
            }
        }

        var showStart by remember { mutableStateOf(false) }
        var showEnd by remember { mutableStateOf(false) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("From")
            FieldBox(modifier = Modifier.clickable { showStart = true }) {
                Text(Fmt.date(df.format(Date(startMillis))), color = Brand.TextPrimary, fontSize = 16.sp)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("To")
            FieldBox(modifier = Modifier.clickable { showEnd = true }) {
                Text(Fmt.date(df.format(Date(endMillis))), color = Brand.TextPrimary, fontSize = 16.sp)
            }
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

        // Half days are annual-leave only: the server records every other type as full days.
        if (isAL && isSingleDay) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel("Duration")
                SegmentedPicker(
                    options = listOf("FULL_DAY" to "Full day", "AM_HALF" to "Morning half", "PM_HALF" to "Afternoon half"),
                    selected = dayType,
                    onSelect = { dayType = it },
                )
            }
        } else if (isAL && isMultiDay) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel("Include a half day?")
                SegmentedPicker(
                    options = listOf("none" to "No", "first" to "First day", "last" to "Last day"),
                    selected = halfDayPosition ?: "none",
                    onSelect = { halfDayPosition = it.takeIf { v -> v != "none" } },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Number of working days")
            FieldBox {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(Fmt.num(daysNum), color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                        Text(
                            if (daysNum == 1.0) "working day" else "working days",
                            color = Brand.TextSecondary, fontSize = 15.sp, modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Filled.EventAvailable, contentDescription = null, tint = Brand.Accent, modifier = Modifier.size(20.dp))
                    }
                    if (wd != null && wd.workingDays == 0) {
                        Text("This range falls entirely on weekends / public holidays.", color = Brand.Amber, fontSize = 12.sp)
                    } else if (showBreakdown && wd != null) {
                        val parts = buildList {
                            if (wd.weekendDays > 0) add("${wd.weekendDays} weekend${if (wd.weekendDays != 1) "s" else ""} skipped")
                            if (wd.holidayDays > 0) add("${wd.holidayDays} public holiday${if (wd.holidayDays != 1) "s" else ""} skipped")
                        }
                        Text(
                            "${wd.calendarDays} calendar day${if (wd.calendarDays != 1) "s" else ""} \u2192 " +
                                "${Fmt.num(daysNum)} working day${if (daysNum != 1.0) "s" else ""}" +
                                (if (parts.isNotEmpty()) " (${parts.joinToString(", ")})" else ""),
                            color = Brand.TextSecondary, fontSize = 12.sp,
                        )
                    }
                    Text(
                        "Weekends and Singapore public holidays are automatically excluded.",
                        color = Brand.TextPrimary.copy(alpha = 0.5f), fontSize = 11.sp,
                    )
                }
            }
        }

        if (isOT && daysNum > otBalance) {
            StatusBanner(isError = true, text = "Insufficient Off In Lieu balance. Available: ${Fmt.num(otBalance)} day(s).")
        }

        if (isAL && daysNum > 0) {
            AlBalancePanel(
                availableNow = availableNow, alPersonalEntitlement = alPersonalEntitlement, otBalance = otBalance,
                advanceAvailable = advanceAvailable, useAdvance = useAdvance, onUseAdvanceChange = { useAdvance = it },
                useOt = useOt, onUseOtChange = { useOt = it },
                otDaysToUse = otDaysToUse, onOtDaysChange = { otDaysToUse = it }, otSliderMax = otSliderMax,
                nonEarnedDays = nonEarnedDays, daysNum = daysNum, advanceDays = advanceDays,
                deficitDays = deficitDays, effectiveOtUsed = effectiveOtUsed,
                fullyWithinEntitlement = fullyWithinEntitlement,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Reason (optional)")
            FormTextField(reason, { reason = it }, "e.g. Family matters")
        }

        PremierButton(
            title = if (deficitDays > 0) "Submit with ${Fmt.num(deficitDays)}d deficit" else "Submit request",
            icon = Icons.AutoMirrored.Filled.Send,
            loading = submitting, enabled = canSubmit,
        ) {
            error = null; submitting = true
            val s = df.format(Date(startMillis))
            val e = df.format(Date(endMillis))
            val dt = if (isSingleDay && isAL) dayType else "FULL_DAY"
            val hp = if (isMultiDay && isAL) halfDayPosition else null
            val picked = if (isMC) attachment else null
            scope.launch {
                try {
                    var documentUrl: String? = null
                    var documentFileName: String? = null
                    if (picked != null) {
                        val encoded = encodeAttachment(context, picked, onTooLarge = { error = "That file is over the 5 MB limit." })
                            ?: throw ApiException(ApiException.Kind.NETWORK, error ?: "Could not read the file.")
                        val uploaded = HrmsApi.uploadFile(encoded.bytes, encoded.fileName, encoded.mimeType)
                        documentUrl = uploaded.url
                        documentFileName = uploaded.fileName
                    }
                    HrmsApi.applyLeave(
                        typeId, s, e, dt, hp, reason, documentUrl, documentFileName,
                        effectiveOtUsed, isAL && useAdvance,
                    )
                    submitting = false; done = true
                } catch (ex: Exception) {
                    submitting = false
                    error = (ex as? ApiException)?.message ?: "Could not submit your leave request."
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

private fun leaveTypeLabel(type: LeaveType, otBalance: Double, alAvailable: Double): String = when {
    type.code == "AL_OT" -> "${type.name} (${Fmt.num(otBalance)} days available)"
    type.code == "AL" -> "${type.name} (${Fmt.num(alAvailable)} days available)"
    type.defaultDays > 0 -> "${type.name} (${type.defaultDays} days/year)"
    else -> type.name
}

/** Balance summary, the advance and Off In Lieu switches, and the earned/advance/deficit breakdown. */
@Composable
private fun AlBalancePanel(
    availableNow: Double,
    alPersonalEntitlement: Double,
    otBalance: Double,
    advanceAvailable: Double,
    useAdvance: Boolean,
    onUseAdvanceChange: (Boolean) -> Unit,
    useOt: Boolean,
    onUseOtChange: (Boolean) -> Unit,
    otDaysToUse: Double,
    onOtDaysChange: (Double) -> Unit,
    otSliderMax: Double,
    nonEarnedDays: Double,
    daysNum: Double,
    advanceDays: Double,
    deficitDays: Double,
    effectiveOtUsed: Double,
    fullyWithinEntitlement: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        // Balance summary
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(Brand.Surface)
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text("LEAVE BALANCE SUMMARY", color = Brand.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryStat("Remaining Balance", Fmt.num(availableNow), if (availableNow >= daysNum) Brand.Green else Brand.Amber)
                SummaryStat("Full Entitlement", Fmt.num(alPersonalEntitlement), Brand.Accent)
                SummaryStat("Off In Lieu Available", Fmt.num(otBalance), Brand.Mint)
            }
        }

        // Advance toggle: borrow what's still to be earned this year.
        if (advanceAvailable > 0 && daysNum > availableNow) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(Brand.Surface)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Use advance annual leave", color = Brand.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "Borrow up to ${Fmt.num(advanceAvailable)} day(s) you'll earn by the end of this year." +
                            if (useAdvance) "" else " If off, days beyond your balance count as deficit.",
                        color = Brand.TextMuted, fontSize = 11.sp,
                    )
                }
                Switch(checked = useAdvance, onCheckedChange = onUseAdvanceChange, colors = SwitchDefaults.colors(checkedTrackColor = Brand.Orange))
            }
        }

        // OT offset toggle
        if (otBalance > 0 && nonEarnedDays > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(Brand.Surface)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Use Off In Lieu days for this request", color = Brand.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "${Fmt.num(otBalance)} Off In Lieu day(s) available",
                        color = Brand.TextMuted, fontSize = 11.sp,
                    )
                }
                Switch(checked = useOt, onCheckedChange = onUseOtChange, colors = SwitchDefaults.colors(checkedTrackColor = Brand.Green))
            }
        }

        // OT slider
        if (useOt && otBalance > 0 && nonEarnedDays > 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(Brand.Green.copy(alpha = 0.08f))
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Off In Lieu days to use", color = Brand.Green, fontSize = 11.sp)
                    Text("${Fmt.num(otDaysToUse)} day(s)", color = Brand.Green, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Slider(
                    value = otDaysToUse.toFloat(),
                    onValueChange = { onOtDaysChange((it * 2).toInt() / 2.0) },
                    valueRange = 0f..otSliderMax.toFloat().coerceAtLeast(0f),
                    steps = (if (otSliderMax > 0) (otSliderMax / 0.5).toInt() - 1 else 0).coerceAtLeast(0),
                    colors = SliderDefaults.colors(thumbColor = Brand.Green, activeTrackColor = Brand.Green),
                )
            }
        }

        // Breakdown
        if (daysNum > 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(Brand.Surface)
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                if (min(availableNow, daysNum) > 0) {
                    BreakdownRow("From your remaining balance:", "${Fmt.num(min(availableNow, daysNum))} day(s)", Brand.TextSecondary, Brand.TextPrimary)
                }
                if (advanceDays > 0) {
                    BreakdownRow("Advance (within entitlement):", "${Fmt.num(advanceDays)} day(s)", Brand.Amber, Brand.Amber)
                }
                if (effectiveOtUsed > 0) {
                    BreakdownRow("Off In Lieu days used:", "${Fmt.num(effectiveOtUsed)} day(s)", Brand.TextSecondary, Brand.Green)
                }
                if (deficitDays > 0) {
                    BreakdownRow("Deficit (beyond entitlement):", "${Fmt.num(deficitDays)} day(s)", Brand.Red, Brand.Red)
                }
                if (advanceDays > 0 && deficitDays == 0.0) {
                    BreakdownNote(
                        "${Fmt.num(advanceDays)} day(s) are within your ${Fmt.num(alPersonalEntitlement)}-day entitlement but not yet accrued. " +
                            "These will be earned over your remaining service months.",
                        Brand.Amber, Icons.Filled.Info,
                    )
                }
                if (deficitDays > 0) {
                    BreakdownNote(
                        "${Fmt.num(deficitDays)} day(s) exceed your full entitlement. These will be recorded as a deficit, offset by " +
                            "future Off In Lieu days earned from weekend/holiday work.",
                        Brand.Red, Icons.Filled.Warning,
                    )
                }
                if (fullyWithinEntitlement && advanceDays == 0.0 && deficitDays == 0.0) {
                    BreakdownNote(
                        if (effectiveOtUsed > 0) "Covered by earned AL + ${Fmt.num(effectiveOtUsed)} Off In Lieu day(s)."
                        else "Fully covered by your earned AL balance.",
                        Brand.Green, Icons.Filled.CheckCircle,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Brand.TextMuted, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun BreakdownRow(label: String, value: String, labelColor: Color, valueColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = labelColor, fontSize = 11.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.Medium, fontSize = 11.sp)
    }
}

@Composable
private fun BreakdownNote(text: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Text(text, color = color, fontSize = 10.sp)
    }
}
