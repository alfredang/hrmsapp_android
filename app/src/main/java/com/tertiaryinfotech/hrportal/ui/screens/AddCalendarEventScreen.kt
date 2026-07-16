package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.ApiException
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.ui.components.BrandScaffold
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.StatusBanner
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.ui.theme.Spacing
import com.tertiaryinfotech.hrportal.util.Fmt
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class EventTypeOption(val code: String, val label: String)

/** HOLIDAY is deliberately excluded — the web's `POST /api/calendar` accepts it from any
 *  authenticated user with no role check (confirmed against tertiary-hrms), but letting a
 *  staff/intern account create a company-wide public holiday doesn't make sense, so this app
 *  narrows the picker to the three event types an individual would actually create. */
private val EVENT_TYPES = listOf(
    EventTypeOption("MEETING", "Meeting"),
    EventTypeOption("TRAINING", "Training"),
    EventTypeOption("COMPANY_EVENT", "Company Event"),
)

/** New calendar event form — posts to the existing (non-mobile-namespaced) `/api/calendar` route,
 *  same as the web's own `/calendar/new` page; that route only checks for a signed-in session
 *  (the same NextAuth cookie this app already carries), so no backend change was needed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCalendarEventScreen(nav: NavController, initialDateIso: String?) {
    var typeCode by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startMillis by remember { mutableStateOf(initialDateIso?.let { Fmt.parse(it)?.time }) }
    var endMillis by remember { mutableStateOf<Long?>(null) }
    var allDay by remember { mutableStateOf(true) }
    var showStart by remember { mutableStateOf(false) }
    var showEnd by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isoFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val displayFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.US) }
    val canSubmit = typeCode != null && title.isNotBlank() && startMillis != null && !submitting

    BrandScaffold(title = "Back to Calendar", onBack = { nav.popBackStack() }) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Add Event", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Text("Create a new calendar event", color = Brand.TextSecondary, fontSize = 14.sp)
            }

            error?.let { StatusBanner(isError = true, text = it) }

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
                    Icon(Icons.Filled.Event, contentDescription = null, tint = Brand.Primary, modifier = Modifier.size(18.dp))
                    Text("New Calendar Event", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Event Type *")
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        FieldBox(modifier = Modifier.menuAnchor()) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    EVENT_TYPES.firstOrNull { it.code == typeCode }?.label ?: "Select event type",
                                    color = if (typeCode != null) Brand.TextPrimary else Brand.TextMuted,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Brand.TextPrimary)
                            }
                        }
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            EVENT_TYPES.forEach { opt ->
                                DropdownMenuItem(text = { Text(opt.label) }, onClick = { typeCode = opt.code; expanded = false })
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Title *")
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Enter event title", color = Brand.TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Description (optional)")
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Enter event description...", color = Brand.TextMuted) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        colors = fieldColors(),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Start Date *")
                    FieldBox(modifier = Modifier.clickable { showStart = true }) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                startMillis?.let { displayFmt.format(Date(it)) } ?: "DD/MM/YYYY",
                                color = if (startMillis != null) Brand.TextPrimary else Brand.TextMuted,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = Brand.TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                if (showStart) {
                    DateField(
                        initial = startMillis ?: System.currentTimeMillis(),
                        onPick = {
                            startMillis = it
                            if (endMillis != null && endMillis!! < it) endMillis = it
                            showStart = false
                        },
                        onCancel = { showStart = false },
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("End Date")
                    FieldBox(modifier = Modifier.clickable { showEnd = true }) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                endMillis?.let { displayFmt.format(Date(it)) } ?: "DD/MM/YYYY",
                                color = if (endMillis != null) Brand.TextPrimary else Brand.TextMuted,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = Brand.TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text("Defaults to start date if not set.", color = Brand.TextMuted, fontSize = 11.sp)
                }
                if (showEnd) {
                    DateField(
                        initial = endMillis ?: startMillis ?: System.currentTimeMillis(),
                        minMillis = startMillis,
                        onPick = { endMillis = it; showEnd = false },
                        onCancel = { showEnd = false },
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { allDay = !allDay },
                ) {
                    Checkbox(checked = allDay, onCheckedChange = { allDay = it })
                    Text("All day event", color = Brand.TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.End),
                ) {
                    OutlinedButton(
                        onClick = { nav.popBackStack() },
                        enabled = !submitting,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, Brand.BorderLight),
                    ) { Text("Cancel", color = Brand.TextSecondary) }
                    PremierButton(
                        fullWidth = false,
                        title = "Create Event",
                        icon = Icons.Filled.Send,
                        loading = submitting,
                        enabled = canSubmit,
                    ) {
                        val type = typeCode ?: return@PremierButton
                        val start = startMillis ?: return@PremierButton
                        error = null; submitting = true
                        scope.launch {
                            try {
                                HrmsApi.createCalendarEvent(
                                    title = title.trim(),
                                    description = description.trim().ifBlank { null },
                                    startDate = isoFmt.format(Date(start)),
                                    endDate = isoFmt.format(Date(endMillis ?: start)),
                                    allDay = allDay,
                                    type = type,
                                )
                                submitting = false; done = true
                            } catch (ex: Exception) {
                                submitting = false
                                error = (ex as? ApiException)?.message ?: "Could not create the event."
                            }
                        }
                    }
                }
            }
        }
    }

    if (done) {
        AlertDialog(
            onDismissRequest = { nav.popBackStack() },
            confirmButton = { TextButton(onClick = { nav.popBackStack() }) { Text("Done") } },
            title = { Text("Event created") },
            text = { Text("Your calendar event was created.") },
        )
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Brand.Border,
    unfocusedContainerColor = Brand.Border,
    focusedTextColor = Brand.TextPrimary,
    unfocusedTextColor = Brand.TextPrimary,
    focusedIndicatorColor = Brand.Primary,
    unfocusedIndicatorColor = Color.Transparent,
)
