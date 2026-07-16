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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.ApiException
import com.tertiaryinfotech.hrportal.data.EmployeeProfile
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

private enum class PassStatus(val label: String) {
    UPCOMING("Upcoming"), ACTIVATED("Activated"), EXPIRED("Expired"), GENERAL("General access")
}

private val dayKeyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

private fun passStatusOf(fromIso: String?, toIso: String?): PassStatus {
    val from = Fmt.parse(fromIso) ?: return PassStatus.GENERAL
    val to = Fmt.parse(toIso) ?: return PassStatus.GENERAL
    val todayKey = dayKeyFmt.format(Date())
    val fromKey = dayKeyFmt.format(from)
    val toKey = dayKeyFmt.format(to)
    return when {
        todayKey < fromKey -> PassStatus.UPCOMING
        todayKey > toKey -> PassStatus.EXPIRED
        else -> PassStatus.ACTIVATED
    }
}

/** Same null/parse-failure check [passStatusOf] uses, so a card never disagrees with itself about
 *  whether it has a real date range (some invites' fromDate/toDate genuinely aren't parseable
 *  dates — "general access" invites with no specific window). */
private fun hasDateRange(fromIso: String?, toIso: String?): Boolean =
    Fmt.parse(fromIso) != null && Fmt.parse(toIso) != null

private fun daysBetween(fromIso: String, toIso: String): Int {
    val from = Fmt.parse(fromIso) ?: return 0
    val to = Fmt.parse(toIso) ?: return 0
    return ((to.time - from.time) / 86_400_000L).toInt()
}

private data class WoodsSquarePageData(
    val invites: List<WoodsSquareInvite>,
    val requests: List<WoodsSquareRequest>,
    val employee: EmployeeProfile?,
)

private suspend fun fetchWoodsSquareData(): WoodsSquarePageData {
    val ws = HrmsApi.woodsSquare()
    val employee = try { HrmsApi.profile().employee } catch (_: Exception) { null }
    return WoodsSquarePageData(ws.invites, ws.requests, employee)
}

/**
 * Woods Square Access — the signed-in employee's building-access invites (sent by HR), rendered as
 * a digital-pass "mobile credential" (mirrors the web's `woods-square-access-card.tsx` hero card:
 * Activated/Upcoming/Expired computed client-side from each invite's date range, a countdown, and
 * a valid-range progress bar), plus their own access requests. A real destination sharing the
 * app's hamburger top bar (not its own back-button scaffold).
 */
@Composable
fun WoodsSquareScreen(nav: NavController) {
    var showRequest by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    var activeTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        key(reloadKey) {
            AsyncListScreen(fetch = { fetchWoodsSquareData() }) { data ->
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text("Woods Square Access", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                            }
                            PremierButton(title = "Request access", icon = Icons.Filled.Send, fullWidth = false) { showRequest = true }
                        }
                    }
                    item { Spacer12() }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("MOBILE CREDENTIAL", color = Brand.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Your Woods Square building pass and entry PIN, in one place.",
                                color = Brand.TextSecondary, fontSize = 13.sp,
                            )
                        }
                    }
                    item { Spacer18() }

                    val statuses = data.invites.associateWith { passStatusOf(it.fromDate, it.toDate) }
                    val hero = data.invites.firstOrNull { statuses[it] == PassStatus.ACTIVATED }
                        ?: data.invites.filter { statuses[it] == PassStatus.UPCOMING }.minByOrNull { it.fromDate ?: "" }
                        ?: data.invites.firstOrNull()
                    val others = data.invites.filter { it.id != hero?.id }
                        .sortedByDescending { it.fromDate ?: "" }

                    if (hero != null) {
                        item {
                            HeroPassCard(
                                invite = hero, status = statuses[hero] ?: PassStatus.GENERAL,
                                employeeName = data.employee?.name ?: "—",
                                employeeRole = data.employee?.role?.let { cap(it) } ?: "Staff",
                                employeeId = data.employee?.employeeId ?: "—",
                            )
                        }
                        item { Spacer12() }
                        item { PinInfoBanner(data.employee?.employeeId ?: "—") }
                        item { Spacer18() }
                    } else {
                        item { EmptyHint(Icons.Filled.MeetingRoom, "No active building-access pass yet.") }
                        item { Spacer18() }
                    }

                    item {
                        TabRow(
                            selectedTabIndex = activeTab,
                            containerColor = Color.Transparent,
                            contentColor = Brand.Primary,
                        ) {
                            Tab(
                                selected = activeTab == 0,
                                onClick = { activeTab = 0 },
                                text = { Text("Other passes ${others.size}") },
                            )
                            Tab(
                                selected = activeTab == 1,
                                onClick = { activeTab = 1 },
                                text = { Text("My requests ${data.requests.size}") },
                            )
                        }
                    }
                    item { Spacer18() }

                    if (activeTab == 0) {
                        if (others.isEmpty()) {
                            item { EmptyHint(Icons.Filled.MeetingRoom, "No other passes.") }
                        } else {
                            items(others) { i -> OtherPassRow(i, statuses[i] ?: PassStatus.GENERAL); Spacer12() }
                        }
                    } else {
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

private fun cap(s: String): String = s.lowercase().replaceFirstChar { it.uppercase() }

/** A literal "physical ID card" look — deliberately hardcoded white/dark-text regardless of the
 *  app theme (like the payslip PDF page), since it represents a physical/digital badge object. */
@Composable
private fun HeroPassCard(invite: WoodsSquareInvite, status: PassStatus, employeeName: String, employeeRole: String, employeeId: String) {
    val statusColor = when (status) {
        PassStatus.ACTIVATED -> Color(0xFF16A34A)
        PassStatus.UPCOMING -> Color(0xFFD97706)
        PassStatus.EXPIRED -> Color(0xFFDC2626)
        PassStatus.GENERAL -> Color(0xFF64748B)
    }
    val hasRange = hasDateRange(invite.fromDate, invite.toDate)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp)),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(statusColor))
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Badge, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text("woods square", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("MOBILE ACCESS", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Filled.Info, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFDCFCE7)),
                    contentAlignment = Alignment.Center,
                ) { Text(initials(employeeName), color = Color(0xFF166534), fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(employeeName, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("$employeeRole · ID $employeeId", color = Color(0xFF64748B), fontSize = 12.sp)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                    Text(status.label, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                if (status == PassStatus.ACTIVATED && hasRange) {
                    val left = daysBetween(dayKeyFmt.format(Date()), invite.toDate!!)
                    Text("$left day${if (left == 1) "" else "s"} left", color = Color(0xFF64748B), fontSize = 12.sp)
                }
            }

            if (hasRange) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("VALID", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${Fmt.date(invite.fromDate)} – ${Fmt.date(invite.toDate)}",
                        color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    )
                    val totalDays = daysBetween(invite.fromDate!!, invite.toDate!!).coerceAtLeast(1)
                    val elapsed = daysBetween(invite.fromDate, dayKeyFmt.format(Date())).coerceIn(0, totalDays)
                    val progress = elapsed.toFloat() / totalDays.toFloat()
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(Color(0xFFE2E8F0))) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(statusColor),
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.VpnKey, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                Text(
                    "Entry via PIN — sent to your email", color = Color(0xFF64748B), fontSize = 12.sp,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun PinInfoBanner(employeeId: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Brand.Corner.dp))
            .background(Brand.Primary.copy(alpha = 0.12f))
            .border(1.dp, Brand.Primary.copy(alpha = 0.35f), RoundedCornerShape(Brand.Corner.dp))
            .padding(14.dp),
    ) {
        Icon(Icons.Filled.Email, contentDescription = null, tint = Brand.Primary, modifier = Modifier.size(18.dp))
        Text(
            buildString {
                append("Your entry PIN is emailed to you for each pass — check your inbox and spam. ")
                append("Staff ID $employeeId. Lost it? Ask an admin to resend.")
            },
            color = Brand.TextSecondary, fontSize = 12.sp,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

private fun initials(name: String): String =
    name.split(" ").filter { it.isNotBlank() }.take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase()

@Composable
private fun OtherPassRow(i: WoodsSquareInvite, status: PassStatus) {
    val hasRange = hasDateRange(i.fromDate, i.toDate)
    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Brand.Border),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.VpnKey, contentDescription = null, tint = Brand.TextSecondary, modifier = Modifier.size(16.dp)) }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                if (hasRange) {
                    Text("${Fmt.date(i.fromDate)} – ${Fmt.date(i.toDate)}", color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    val days = daysBetween(i.fromDate!!, i.toDate!!) + 1
                    val detail = when (status) {
                        PassStatus.UPCOMING -> "$days days · Starts in ${daysBetween(dayKeyFmt.format(Date()), i.fromDate)} days"
                        PassStatus.EXPIRED -> "$days days · ended ${Fmt.date(i.toDate)}"
                        PassStatus.ACTIVATED -> "$days days · Active now"
                        PassStatus.GENERAL -> "$days days"
                    }
                    Text(detail, color = Brand.TextSecondary, fontSize = 11.sp)
                } else {
                    Text("General access", color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Sent ${Fmt.date(i.createdAt)}", color = Brand.TextSecondary, fontSize = 11.sp)
                }
            }
            StatusPill(status.label)
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
                    color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
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

/** Request-access form — `POST /api/woods-square/access-requests`, `{fromDate?, toDate?, note?}`.
 *  Quick-pick chips (Today/3 days/5 days/1 week) fill the same From/To state the manual date
 *  fields use, so either path works. */
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

    fun pickQuickRange(days: Int) {
        val today = System.currentTimeMillis()
        fromMillis = today
        toMillis = today + (days - 1) * 86_400_000L
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        GradientScreen {
            BrandScaffold(
                title = "Request Woods Square Access",
                actions = { IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close", tint = Brand.TextPrimary) } },
                onBack = null,
            ) { inner ->
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
                        "Ask an admin to send you a building-access invite. Dates and a note are optional — leave them blank and the admin will decide.",
                        color = Brand.TextSecondary, fontSize = 13.sp,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Brand.Corner.dp))
                            .background(Brand.Primary.copy(alpha = 0.12f))
                            .border(1.dp, Brand.Primary.copy(alpha = 0.35f), RoundedCornerShape(Brand.Corner.dp))
                            .padding(14.dp),
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = Brand.Primary, modifier = Modifier.size(18.dp))
                        Text(
                            buildString {
                                append("Each access window can be up to 1 week (7 days) — for a longer stay, send another request.")
                            },
                            color = Brand.TextSecondary, fontSize = 12.sp,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }

                    FieldLabel("ACCESS WINDOW (OPTIONAL)")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickPickChip("Today") { pickQuickRange(1) }
                        QuickPickChip("3 days") { pickQuickRange(3) }
                        QuickPickChip("5 days") { pickQuickRange(5) }
                        QuickPickChip("1 week") { pickQuickRange(7) }
                    }

                    FieldBox(modifier = Modifier.clickable { showFrom = true }) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (fromMillis != null && toMillis != null) {
                                    "${Fmt.date(df.format(Date(fromMillis!!)))} – ${Fmt.date(df.format(Date(toMillis!!)))}"
                                } else "Pick a date range",
                                color = if (fromMillis != null) Brand.TextPrimary else Brand.TextMuted,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (showFrom) {
                        DateField(
                            initial = fromMillis ?: System.currentTimeMillis(),
                            onPick = { fromMillis = it; if (toMillis == null || toMillis!! < it) toMillis = it; showFrom = false; showTo = true },
                            onCancel = { showFrom = false },
                        )
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
                            focusedTextColor = Brand.TextPrimary,
                            unfocusedTextColor = Brand.TextPrimary,
                            focusedIndicatorColor = Brand.Primary,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = onDismiss,
                            enabled = !submitting,
                            shape = MaterialTheme.shapes.small,
                        ) { Text("Cancel", color = Brand.TextSecondary) }
                        PremierButton(fullWidth = false, title = "Send request", icon = Icons.Filled.Send, loading = submitting) {
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
}

@Composable
private fun QuickPickChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Brand.Border)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, color = Brand.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
