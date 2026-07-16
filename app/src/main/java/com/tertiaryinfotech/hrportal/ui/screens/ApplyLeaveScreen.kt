package com.tertiaryinfotech.hrportal.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.StatusBanner
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.ui.theme.Spacing
import com.tertiaryinfotech.hrportal.util.Fmt
import com.tertiaryinfotech.hrportal.util.WorkingDays
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Apply-for-leave screen — a real nav destination (not a Dialog) so it shares the same
 * hamburger-drawer top bar and bottom nav as every other screen, mirroring the web's
 * `/leave/request` page being a normal page in the app shell rather than a modal. Its route
 * ("leave_request/...") deliberately starts with "leave" so `MainScaffold`'s existing
 * startsWith-based tab matching treats it as still being on the Leave tab for free — same shared
 * top bar, same bottom nav highlight — no changes needed there.
 */
@Composable
fun ApplyLeaveScreen(nav: NavController, initialTypeCode: String?) {
    AsyncListScreen(fetch = { HrmsApi.leave() }) { data ->
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Request Leave", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Text("Submit a new leave application", color = Brand.TextSecondary, fontSize = 14.sp)
            }
        }
        item { Spacer18() }
        item {
            ApplyLeaveForm(
                types = data.types,
                balances = data.balances,
                initialTypeCode = initialTypeCode,
                onCancel = { nav.popBackStack() },
                onApplied = {
                    nav.previousBackStackEntry?.savedStateHandle?.set("leave_applied", true)
                    nav.popBackStack()
                },
            )
        }
    }
}

/**
 * The actual form — posts to the existing /api/leave endpoint; the server computes balance
 * deductions once it receives our client-computed `days`/`otDaysUsed`. Ported field-for-field
 * from the web's `leave-request-form.tsx` — working-days preview (via `/api/public-holidays`),
 * the AL Off-In-Lieu offset panel, MC document upload, and deficit-aware submit gating all mirror
 * that component exactly (values sourced from the same `/api/mobile/leave` balance rows already
 * fetched by [ApplyLeaveScreen] above — no separate backend call needed for the AL/OT numbers).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplyLeaveForm(
    types: List<LeaveType>,
    balances: List<LeaveBalance>,
    initialTypeCode: String? = null,
    onCancel: () -> Unit,
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
    var documentUrl by remember { mutableStateOf<String?>(null) }
    var documentFileName by remember { mutableStateOf<String?>(null) }
    var uploadingDoc by remember { mutableStateOf(false) }
    var showAttachOptions by remember { mutableStateOf(false) }
    var cameraCaptureUri by remember { mutableStateOf<Uri?>(null) }
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
    val alEarned = alBalance?.proRated ?: 0.0
    val alFullAvailable = alPersonalEntitlement + (alBalance?.carriedOver ?: 0.0) -
        (alBalance?.used ?: 0.0) - (alBalance?.pending ?: 0.0)

    // Fetch public holidays whenever the start-date year changes.
    LaunchedEffect(df.format(Date(startMillis)).take(4)) {
        val year = df.format(Date(startMillis)).take(4).toIntOrNull() ?: return@LaunchedEffect
        try {
            holidays = HrmsApi.publicHolidays(year).dates.toSet()
        } catch (_: Exception) { /* preview just won't exclude PH; server still validates */ }
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

    val deficitBeforeOt = if (isAL) max(0.0, daysNum - alFullAvailable) else 0.0
    val nonEarnedDays = if (isAL) max(0.0, daysNum - alEarned) else 0.0
    val effectiveOtUsed = if (useOt) minOf(otDaysToUse, otBalance, daysNum) else 0.0
    val advanceDays = if (isAL) max(0.0, min(daysNum, alFullAvailable) - alEarned - effectiveOtUsed) else 0.0
    val deficitDays = if (isAL) max(0.0, deficitBeforeOt - effectiveOtUsed) else 0.0
    val fullyWithinEntitlement = isAL && daysNum <= alFullAvailable
    val otSliderMax = min(otBalance, nonEarnedDays)

    LaunchedEffect(useOt, deficitBeforeOt, nonEarnedDays, otBalance) {
        if (!useOt) { otDaysToUse = 0.0; return@LaunchedEffect }
        val target = if (deficitBeforeOt > 0) deficitBeforeOt else nonEarnedDays
        otDaysToUse = min(target, otBalance)
    }

    fun uploadDocument(uri: Uri, name: String, mime: String) {
        scope.launch {
            uploadingDoc = true
            error = null
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw ApiException(ApiException.Kind.NETWORK, "Could not read the file.")
                val result = HrmsApi.uploadFile(bytes, name, mime)
                documentUrl = result.url
                documentFileName = result.fileName
            } catch (e: Exception) {
                error = (e as? ApiException)?.message ?: "Could not upload the document."
            } finally {
                uploadingDoc = false
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val resolver = context.contentResolver
        uploadDocument(uri, queryFileName(resolver, uri) ?: "document", resolver.getType(uri) ?: "application/octet-stream")
    }

    // Camera capture: TakePicture writes the full-resolution photo to a FileProvider-shared temp
    // file so the MC document photo stays legible after upload.
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = cameraCaptureUri
        if (success && uri != null) uploadDocument(uri, "mc_${System.currentTimeMillis()}.jpg", "image/jpeg")
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createCaptureUri(context)
            cameraCaptureUri = uri
            cameraLauncher.launch(uri)
        } else {
            error = "Camera permission is needed to take a photo."
        }
    }
    fun startCameraCapture() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val uri = createCaptureUri(context)
            cameraCaptureUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val canSubmit = typeId.isNotEmpty() && daysNum >= 0.5 && !uploadingDoc &&
        !(isOT && daysNum > otBalance) && !(isMC && documentUrl == null)

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        error?.let { StatusBanner(isError = true, text = it) }

        // "Leave Application" card — mirrors the web's <Card className="bg-gray-950
        // border-gray-800"> wrapper: every field below sits on a visibly darker,
        // bordered surface distinct from the page background behind it.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(Brand.Surface)
                .border(1.dp, Brand.Border, MaterialTheme.shapes.medium)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = Brand.Primary, modifier = Modifier.size(18.dp))
                Text("Leave Application", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }

            // Leave type
                    FieldLabel("Leave type *")
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        FieldBox(modifier = Modifier.menuAnchor()) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    selectedType?.let { leaveTypeLabel(it, otBalance, alEarned) } ?: "Select…",
                                    color = Brand.TextPrimary, modifier = Modifier.weight(1f),
                                )
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Brand.TextPrimary)
                            }
                        }
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            types.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(leaveTypeLabel(t, otBalance, alEarned)) },
                                    onClick = { typeId = t.id; expanded = false },
                                )
                            }
                        }
                    }
                    if (isOT) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = Brand.Emerald, modifier = Modifier.size(14.dp))
                            Text(
                                "You have ${Fmt.num(otBalance)} Off In Lieu day(s) earned from weekend/public holiday work.",
                                color = Brand.Emerald, fontSize = 12.sp,
                            )
                        }
                    }

                    // Dates
                    var showStart by remember { mutableStateOf(false) }
                    var showEnd by remember { mutableStateOf(false) }
                    FieldLabel("From")
                    FieldBox(modifier = Modifier.clickable { showStart = true }) {
                        Text(Fmt.date(df.format(Date(startMillis))), color = Brand.TextPrimary)
                    }
                    FieldLabel("To")
                    FieldBox(modifier = Modifier.clickable { showEnd = true }) {
                        Text(Fmt.date(df.format(Date(endMillis))), color = Brand.TextPrimary)
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

                    // Day-type / half-day selector — AL only, mirrors renderDayTypeSelector().
                    if (isAL && isSingleDay) {
                        FieldLabel("Day Type *")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val opts = listOf("FULL_DAY" to "Full Day", "AM_HALF" to "AM Half", "PM_HALF" to "PM Half")
                            opts.forEachIndexed { i, (value, label) ->
                                SegmentedButton(
                                    selected = dayType == value,
                                    onClick = { dayType = value },
                                    shape = SegmentedButtonDefaults.itemShape(i, opts.size),
                                ) { Text(label) }
                            }
                        }
                    } else if (isAL && isMultiDay) {
                        FieldLabel("Include a half-day?")
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            ToggleChip(
                                label = if (halfDayPosition != null) "Yes" else "No — all full days",
                                selected = halfDayPosition != null,
                                onClick = { halfDayPosition = if (halfDayPosition != null) null else "first" },
                            )
                        }
                        if (halfDayPosition != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                listOf("first" to df.format(Date(startMillis)), "last" to df.format(Date(endMillis))).forEach { (pos, date) ->
                                    ToggleChip(
                                        label = "Half on $pos day (${Fmt.date(date)})",
                                        selected = halfDayPosition == pos,
                                        onClick = { halfDayPosition = pos },
                                    )
                                }
                            }
                        }
                    }

                    // Number of Working Days preview
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        FieldLabel("Number of Working Days")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .background(Brand.Border)
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        ) {
                            Text(
                                if (daysNum > 0) "${Fmt.num(daysNum)} " + (if (daysNum == 1.0) "working day" else "working days") else "—",
                                color = Brand.TextPrimary, fontSize = 14.sp,
                            )
                        }
                        if (showBreakdown && wd != null) { // wd non-null is implied by showBreakdown, kept for smart-cast
                            Text(
                                "${wd.calendarDays} calendar day${if (wd.calendarDays != 1) "s" else ""} → " +
                                    "${Fmt.num(daysNum)} working day${if (daysNum != 1.0) "s" else ""}" +
                                    (if (wd.weekendDays > 0) " (${wd.weekendDays} weekend${if (wd.weekendDays != 1) "s" else ""} skipped)" else "") +
                                    (if (wd.holidayDays > 0) ", ${wd.holidayDays} public holiday${if (wd.holidayDays != 1) "s" else ""} skipped" else ""),
                                color = Brand.Blue, fontSize = 11.sp,
                            )
                        }
                        if (wd != null && wd.workingDays == 0 && isSingleDay) {
                            Text("Selected date(s) fall on weekends or public holidays — no working days.", color = Brand.Amber, fontSize = 11.sp)
                        }
                        Text("Weekends and Singapore public holidays are automatically excluded.", color = Brand.TextMuted, fontSize = 11.sp)
                    }

                    if (isOT && daysNum > otBalance) {
                        StatusBanner(isError = true, text = "Insufficient Off In Lieu balance. Available: ${Fmt.num(otBalance)} day(s).")
                    }

                    // AL Off-In-Lieu offset panel
                    if (isAL && daysNum > 0) {
                        AlBalancePanel(
                            alEarned = alEarned, alPersonalEntitlement = alPersonalEntitlement, otBalance = otBalance,
                            useOt = useOt, onUseOtChange = { useOt = it },
                            otDaysToUse = otDaysToUse, onOtDaysChange = { otDaysToUse = it }, otSliderMax = otSliderMax,
                            nonEarnedDays = nonEarnedDays, daysNum = daysNum, advanceDays = advanceDays,
                            deficitDays = deficitDays, effectiveOtUsed = effectiveOtUsed,
                            fullyWithinEntitlement = fullyWithinEntitlement,
                        )
                    }

                    FieldLabel("Reason (optional)")
                    TextField(
                        value = reason,
                        onValueChange = { reason = it },
                        placeholder = { Text("Enter reason for leave...", color = Brand.TextMuted) },
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

                    // Medical Certificate upload — MC/SL only, required.
                    if (isMC) {
                        FieldLabel("Medical Certificate / Doctor's Evidence *")
                        if (documentFileName != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .background(Brand.Border)
                                    .padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.UploadFile, contentDescription = null, tint = Brand.TextSecondary)
                                Text(
                                    documentFileName!!, color = Brand.TextSecondary, fontSize = 13.sp, maxLines = 1,
                                    modifier = Modifier.padding(start = Spacing.sm).weight(1f),
                                )
                                IconButton(onClick = { documentUrl = null; documentFileName = null }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Brand.TextSecondary)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .background(Color.Transparent)
                                    .clickable(enabled = !uploadingDoc) { showAttachOptions = true }
                                    .padding(Spacing.lg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                    if (uploadingDoc) {
                                        CircularProgressIndicator(color = Brand.Primary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                        Text("Uploading…", color = Brand.TextMuted, fontSize = 13.sp)
                                    } else {
                                        Icon(Icons.Filled.UploadFile, contentDescription = null, tint = Brand.TextSecondary)
                                        Text("Upload MC document (Image or PDF, max 5MB)", color = Brand.TextMuted, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

            // Cancel + Submit side by side, right-aligned — mirrors the web's
            // <div className="flex justify-end gap-3"> footer row exactly.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.End),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !submitting,
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, Brand.BorderLight),
                ) { Text("Cancel", color = Brand.TextSecondary) }
                PremierButton(
                    fullWidth = false,
                    title = if (deficitDays > 0) "Submit with ${Fmt.num(deficitDays)}d deficit" else "Submit Request",
                    icon = Icons.Filled.Send,
                    loading = submitting, enabled = canSubmit,
                ) {
                    error = null; submitting = true
                    val s = df.format(Date(startMillis))
                    val e = df.format(Date(endMillis))
                    val dt = if (isSingleDay) dayType else "FULL_DAY"
                    val hp = if (isMultiDay) halfDayPosition else null
                    scope.launch {
                        try {
                            HrmsApi.applyLeave(typeId, s, e, daysNum, dt, hp, reason, documentUrl, documentFileName, effectiveOtUsed)
                            submitting = false; done = true
                        } catch (ex: Exception) {
                            submitting = false
                            error = (ex as? ApiException)?.message ?: "Could not submit your leave request."
                        }
                    }
                }
            }
        } // end "Leave Application" card
    }

    if (done) {
        AlertDialog(
            onDismissRequest = onApplied,
            confirmButton = { TextButton(onClick = onApplied) { Text("Done") } },
            title = { Text("Request submitted") },
            text = { Text("Your leave request was submitted for approval.") },
        )
    }

    if (showAttachOptions) {
        AttachOptionsDialog(
            onDismiss = { showAttachOptions = false },
            onTakePhoto = { showAttachOptions = false; startCameraCapture() },
            onChooseFile = { showAttachOptions = false; filePicker.launch("image/*,application/pdf") },
        )
    }
}

/**
 * A filled-when-selected / outlined-when-not pill button — mirrors the web's literal toggle
 * class logic (`renderDayTypeSelector()`): `bg-primary text-primary-foreground border-primary`
 * when active, `bg-gray-900 text-gray-400 border-gray-700` otherwise. Used for the half-day
 * Yes/No toggle and the first/last half-day position picks.
 */
@Composable
private fun ToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) Brand.Primary else Brand.Surface)
            .border(1.dp, if (selected) Brand.Primary else Brand.BorderLight, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        Text(
            label,
            color = if (selected) Color.White else Brand.TextSecondary,
            fontWeight = FontWeight.Medium, fontSize = 13.sp,
        )
    }
}

private fun leaveTypeLabel(type: LeaveType, otBalance: Double, alEarned: Double): String = when {
    type.code == "AL_OT" -> "${type.name} (${Fmt.num(otBalance)} days available)"
    type.code == "AL" -> "${type.name} (${Fmt.num(alEarned)} days available)"
    type.defaultDays > 0 -> "${type.name} (${type.defaultDays} days/year)"
    else -> type.name
}

/** Mirrors `renderAlBalancePanel()` — Leave Balance Summary grid, the Off-In-Lieu offset
 *  toggle + slider, and the color-coded earned/advance/OT-used/deficit breakdown. */
@Composable
private fun AlBalancePanel(
    alEarned: Double,
    alPersonalEntitlement: Double,
    otBalance: Double,
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
                SummaryStat("Earned to Date", Fmt.num(alEarned), if (alEarned >= daysNum) Brand.Green else Brand.Amber)
                SummaryStat("Full Entitlement", Fmt.num(alPersonalEntitlement), Brand.Blue)
                SummaryStat("Off In Lieu Available", Fmt.num(otBalance), Brand.Emerald)
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
                Switch(checked = useOt, onCheckedChange = onUseOtChange, colors = SwitchDefaults.colors(checkedTrackColor = Brand.Emerald))
            }
        }

        // OT slider
        if (useOt && otBalance > 0 && nonEarnedDays > 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(Brand.Emerald.copy(alpha = 0.08f))
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Off In Lieu days to use", color = Brand.Emerald, fontSize = 11.sp)
                    Text("${Fmt.num(otDaysToUse)} day(s)", color = Brand.Emerald, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Slider(
                    value = otDaysToUse.toFloat(),
                    onValueChange = { onOtDaysChange((it * 2).toInt() / 2.0) },
                    valueRange = 0f..otSliderMax.toFloat().coerceAtLeast(0f),
                    steps = (if (otSliderMax > 0) (otSliderMax / 0.5).toInt() - 1 else 0).coerceAtLeast(0),
                    colors = SliderDefaults.colors(thumbColor = Brand.Emerald, activeTrackColor = Brand.Emerald),
                )
            }
        }

        // Breakdown
        if (daysNum > 0) {
            val borderColor = if (deficitDays > 0) Brand.Red else if (advanceDays > 0) Brand.Amber else Brand.Green
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(Brand.Surface)
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                if (min(alEarned, daysNum) > 0) {
                    BreakdownRow("Already earned (accrued):", "${Fmt.num(min(alEarned, daysNum))} day(s)", Brand.TextSecondary, Brand.TextPrimary)
                }
                if (advanceDays > 0) {
                    BreakdownRow("Advance (within entitlement):", "${Fmt.num(advanceDays)} day(s)", Brand.Amber, Brand.Amber)
                }
                if (effectiveOtUsed > 0) {
                    BreakdownRow("Off In Lieu days used:", "${Fmt.num(effectiveOtUsed)} day(s)", Brand.TextSecondary, Brand.Emerald)
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
