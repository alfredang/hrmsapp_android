package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.ApiException
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.WoodsSquareInvite
import com.tertiaryinfotech.hrportal.data.WoodsSquareRequest
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.BrandScaffold
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.StatusBanner
import com.tertiaryinfotech.hrportal.ui.components.StatusPill
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.ui.theme.GradientScreen
import com.tertiaryinfotech.hrportal.util.Fmt
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Woods Square Access — the signed-in employee's building-access invites (sent by HR) and their
 * own access requests. Ported from the web's `woods-square-access-card.tsx` /
 * `woods-square-access` page: view invites/requests, request new access, cancel a pending one.
 * Reached from the drawer, so it keeps its own back-button top bar.
 */
@Composable
fun WoodsSquareScreen(nav: NavController) {
    var showRequest by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    BrandScaffold(title = "Woods Square Access", onBack = { nav.popBackStack() }) { inner ->
        Column(modifier = Modifier.padding(inner).fillMaxSize()) {
            key(reloadKey) {
                AsyncListScreen(fetch = { HrmsApi.woodsSquare() }) { data ->
                    item {
                        PremierButton(title = "Request Access", icon = Icons.Filled.AddCircle) { showRequest = true }
                    }
                    item { Spacer18() }
                    item { SectionTitle("Invites") }
                    item { Spacer12() }
                    if (data.invites.isEmpty()) {
                        item { EmptyHint(Icons.Filled.MeetingRoom, "No active invites.") }
                    } else {
                        items(data.invites) { i -> InviteRow(i); Spacer12() }
                    }
                    item { Spacer18() }
                    item { SectionTitle("My requests") }
                    item { Spacer12() }
                    if (data.requests.isEmpty()) {
                        item { EmptyHint(Icons.Filled.MeetingRoom, "No access requests yet.") }
                    } else {
                        items(data.requests) { r ->
                            RequestRow(r, onCancel = {
                                scope.launch {
                                    try {
                                        HrmsApi.cancelWoodsSquareRequest(r.id)
                                        reloadKey++
                                    } catch (_: Exception) { /* surfaced via list staying unchanged */ }
                                }
                            })
                            Spacer12()
                        }
                    }
                }
            }
        }
    }
    if (showRequest) {
        RequestAccessSheet(onDismiss = { showRequest = false }, onSubmitted = { showRequest = false; reloadKey++ })
    }
}

@Composable
private fun InviteRow(i: WoodsSquareInvite) {
    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${Fmt.date(i.fromDate)} → ${Fmt.date(i.toDate)}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Sent ${Fmt.date(i.createdAt)}", color = Brand.TextSecondary, fontSize = 11.sp)
            }
            StatusPill(i.status)
        }
    }
}

@Composable
private fun RequestRow(r: WoodsSquareRequest, onCancel: () -> Unit) {
    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (r.fromDate != null) "${Fmt.date(r.fromDate)} → ${Fmt.date(r.toDate)}" else "Any date",
                    color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                )
                if (!r.note.isNullOrEmpty()) {
                    Text(r.note, color = Brand.TextSecondary, fontSize = 12.sp, maxLines = 2)
                }
                Text("Requested ${Fmt.date(r.createdAt)}", color = Brand.TextMuted, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatusPill(r.status)
                if (r.status.equals("PENDING", ignoreCase = true)) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Cancel, contentDescription = "Cancel request", tint = Brand.Red)
                    }
                }
            }
        }
    }
}

/** Request-access form — `POST /api/woods-square/access-requests`, `{fromDate?, toDate?, note?}`. */
@Composable
private fun RequestAccessSheet(onDismiss: () -> Unit, onSubmitted: () -> Unit) {
    var fromMillis by remember { mutableStateOf<Long?>(null) }
    var toMillis by remember { mutableStateOf<Long?>(null) }
    var showFrom by remember { mutableStateOf(false) }
    var showTo by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val df = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        GradientScreen {
            BrandScaffold(title = "Request access", onBack = onDismiss) { inner ->
                Column(
                    modifier = Modifier
                        .padding(inner)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    error?.let { StatusBanner(isError = true, text = it) }

                    Text(
                        "Dates are optional (up to a 7-day window). Leave blank to request general access.",
                        color = Brand.TextSecondary, fontSize = 12.sp,
                    )

                    FieldLabel("From (optional)")
                    FieldBox(modifier = Modifier.clickable { showFrom = true }) {
                        Text(fromMillis?.let { Fmt.date(df.format(Date(it))) } ?: "Select a date", color = if (fromMillis != null) Color.White else Brand.TextMuted)
                    }
                    if (showFrom) {
                        DateField(initial = fromMillis ?: System.currentTimeMillis(), onPick = { fromMillis = it; showFrom = false }, onCancel = { showFrom = false })
                    }

                    FieldLabel("To (optional)")
                    FieldBox(modifier = Modifier.clickable { showTo = true }) {
                        Text(toMillis?.let { Fmt.date(df.format(Date(it))) } ?: "Select a date", color = if (toMillis != null) Color.White else Brand.TextMuted)
                    }
                    if (showTo) {
                        DateField(initial = toMillis ?: fromMillis ?: System.currentTimeMillis(), minMillis = fromMillis, onPick = { toMillis = it; showTo = false }, onCancel = { showTo = false })
                    }

                    FieldLabel("Note (optional)")
                    TextField(
                        value = note,
                        onValueChange = { if (it.length <= 300) note = it },
                        placeholder = { Text("e.g. Meeting with client", color = Brand.TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Brand.Border,
                            unfocusedContainerColor = Brand.Border,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Brand.Primary,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )

                    PremierButton(title = "Submit request", icon = Icons.Filled.Send, loading = submitting) {
                        if ((fromMillis == null) != (toMillis == null)) {
                            error = "Choose both dates, or leave both blank."
                            return@PremierButton
                        }
                        error = null; submitting = true
                        scope.launch {
                            try {
                                HrmsApi.requestWoodsSquareAccess(
                                    fromMillis?.let { df.format(Date(it)) },
                                    toMillis?.let { df.format(Date(it)) },
                                    note.ifBlank { null },
                                )
                                submitting = false
                                onSubmitted()
                            } catch (ex: Exception) {
                                submitting = false
                                error = (ex as? ApiException)?.message ?: "Could not submit your access request."
                            }
                        }
                    }
                }
            }
        }
    }
}
