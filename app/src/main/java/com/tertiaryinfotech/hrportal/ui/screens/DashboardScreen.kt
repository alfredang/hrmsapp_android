package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.AttendancePunch
import com.tertiaryinfotech.hrportal.data.DashboardSummary
import com.tertiaryinfotech.hrportal.data.Directory
import com.tertiaryinfotech.hrportal.data.DirectoryRequest
import com.tertiaryinfotech.hrportal.data.FinanceKpis
import com.tertiaryinfotech.hrportal.data.HomeTiles
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.PendingApprovals
import com.tertiaryinfotech.hrportal.data.ViewAsRules
import com.tertiaryinfotech.hrportal.ui.AuthViewModel
import com.tertiaryinfotech.hrportal.ui.ViewAs
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.Kpi
import com.tertiaryinfotech.hrportal.ui.components.KpiStrip
import com.tertiaryinfotech.hrportal.ui.components.StatTile
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Home tab (iOS `DashboardView`). Each role has its own Home (Alfred, 22 Nov 2026), in the order that role
 * uses it most: people who must clock in see the punch first; staff see their leave and MC balances with the
 * two apply buttons; admins the workforce overview and what is waiting; accountants the Finance card only.
 * View As previews another role's Home (the summary is fetched with `?view=`).
 */
@Composable
fun DashboardScreen(auth: AuthViewModel, nav: NavController) {
    if (auth.isTrainerView) {
        TrainerAdminHome(auth, nav)
        return
    }
    val mode = ViewAs.mode
    val shown = auth.shown
    val view = ViewAsRules.summaryView(mode, auth.isApprover, auth.canViewAs)
    AsyncListScreen(key = mode, fetch = {
        coroutineScope {
            val s = HrmsApi.summary(view)
            val adminHome = s.isAdmin && shown.admin
            val counts = if (adminHome) async { activeCountsOrNull() } else null
            val pending = if (adminHome) async { pendingOrNull() } else null
            HomeData(s, counts?.await(), pending?.await())
        }
    }) { home ->
        val s = home.summary
        val admin = s.isAdmin && shown.admin
        val accountant = shown.accountant && !admin
        item { Greeting(s, auth) { nav.navigate("profile") } }
        if (auth.availableViews.size > 1) {
            item { Spacer12() }
            item { ViewAsRow(auth) }
        }
        item { Spacer18() }

        if (shown.mustClockIn && !admin) {
            item { HomeClockCard(preview = shown.preview) { nav.navigate("timesheet") } }
            item { Spacer18() }
        }

        if (admin) {
            // The web dashboard's three KPI tiles: active staff, active interns, pending approvals.
            val waiting = home.pending?.total ?: (s.pendingLeaves + s.pendingClaims)
            item { SectionTitle("Overview") }
            item { Spacer12() }
            item {
                KpiStrip(buildList {
                    home.counts?.let { (staff, interns) ->
                        add(Kpi("$staff", "Full-time staff", Brand.TextPrimary) { nav.navigate("team_group/staff") })
                        add(Kpi("$interns", "Interns", Brand.TextPrimary) { nav.navigate("team_group/interns") })
                    }
                    add(Kpi("$waiting", "Pending approvals", if (waiting > 0) Brand.Amber else Brand.TextPrimary) { nav.navigate("approvals") })
                })
            }
            item { Spacer18() }
            home.pending?.let { p ->
                item { SectionTitle("Waiting for approval") }
                item { Spacer12() }
                item { PendingCard(p, nav) }
                item { Spacer18() }
            }
        }

        // Accountants: claims waiting, this month's bank income and expenses, statements and reconciliation.
        s.financeKpis?.let { f ->
            item { SectionTitle("Finance") }
            item { Spacer12() }
            item { FinanceCard(f, canApprove = s.isAdmin, nav = nav) }
            item { Spacer18() }
        }

        // Everyone but admins and accountants: leave and MC left, and the two forms one tap away.
        if (!admin && !accountant) {
            item { BalanceCard(s, intern = shown.intern, nav = nav) }
            item { Spacer18() }
        }

        // Trainer counts: Trainer Admins who are not approvers (admins see them in the Trainer Admin view).
        if (!admin && !shown.preview) {
            s.trainerKpis?.let { t ->
                item { SectionTitle("Trainer Admin") }
                item { Spacer12() }
                // Search every trainer by name, skill, certification or course (Alfred, 22 Nov 2026).
                item {
                    TrainerHomeSearch(
                        onOpen = { ViewAs.pendingDirectory = it; nav.navigate("trainers") },
                        onProfile = { nav.navigate("trainer_profile/$it") },
                    )
                }
                item { Spacer12() }
                item {
                    KpiStrip(ViewAsRules.trainerTiles(t).map { (tile, filter) ->
                        Kpi(tile.value, tile.label, Brand.TextPrimary) {
                            ViewAs.pendingDirectory = DirectoryRequest(filter)
                            nav.navigate("trainers")
                        }
                    })
                }
                item { Spacer18() }
            }
        }

        // Accountants: finance only, no quick actions (Alfred, 22 Nov 2026).
        if (!accountant) {
            item { SectionTitle("Quick actions") }
            item { Spacer12() }
            quickActions(admin, shown.intern, nav).chunked(2).forEach { pair ->
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { a -> QuickAction(a, Modifier.weight(1f)) }
                        if (pair.size == 1) Box(Modifier.weight(1f))
                    }
                }
                item { Spacer12() }
            }
            item { Spacer12() }
        }

        if (!admin && !accountant) {
            // Staff / interns: the server's own leave tiles (the decision-email summary), shown as given.
            HomeTiles.leave(s, shown.intern)?.let { leaveTiles ->
                item { SectionTitle("My leave this year") }
                item { Spacer12() }
                leaveTiles.chunked(2).forEach { pair ->
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            pair.forEach { t ->
                                val (icon, tint) = leaveTileStyle(leaveTiles.indexOf(t))
                                StatTile(t.value, t.label, Modifier.weight(1f), icon, tint, onClick = { openTab(nav, "leave") }, hint = t.hint)
                            }
                            if (pair.size == 1) Box(Modifier.weight(1f))
                        }
                    }
                    item { Spacer12() }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(fmt(s.otRemaining), "OT leave", Modifier.weight(1f), Icons.Filled.AccessTime, Brand.Mint)
                    StatTile(Fmt.money(s.expenseYtd), "Expenses YTD", Modifier.weight(1f), Icons.Filled.CreditCard, Brand.Green)
                }
            }
        }
    }
}

private data class HomeData(
    val summary: DashboardSummary,
    /** Approvers: ACTIVE staff and interns (the Team list, as the web's workforce snapshot), or null. */
    val counts: Pair<Int, Int>?,
    /** Approvers: what is waiting, by type; null when the approvals feed failed. */
    val pending: PendingApprovals?,
)

/** Active staff and interns from the Team list (active people only), split on the server's `intern` flag. */
private suspend fun activeCountsOrNull(): Pair<Int, Int>? = try {
    Directory.split(HrmsApi.employees().employees)?.let { it.staff.size to it.interns.size }
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}

/**
 * Approvers: leave, MC, time off, claims and No Pay Leave paybacks waiting (→ Approvals); no missed punches (23 Nov 2026).
 * Feeds are best-effort: a failed one counts as 0; a failed approvals feed leaves the section out.
 */
private suspend fun pendingOrNull(): PendingApprovals? = coroutineScope {
    val approvals = async { runCatching { HrmsApi.approvals() }.getOrNull() }
    val timeOff = async { runCatching { HrmsApi.timeOff() }.getOrNull() }
    approvals.await()?.let { PendingApprovals.from(it, timeOff.await().orEmpty()) }
}

private data class Action(val id: String, val title: String, val icon: ImageVector, val tint: Color, val open: () -> Unit)

/** Quick actions follow the role, as on the web dashboard (`QuickActions`); clock in is never one. */
private fun quickActions(admin: Boolean, intern: Boolean, nav: NavController): List<Action> = when {
    admin -> listOf(
        Action("approvals", "Approvals", Icons.Filled.CheckCircle, Brand.Green) { nav.navigate("approvals") },
        Action("leavemc", "Leave & MC", Icons.Filled.MedicalServices, Brand.Pink) { nav.navigate("mc_leave_summary") },
        Action("timesheet", "Timesheet", Icons.Filled.TableChart, Brand.Mint) { nav.navigate("timesheet_admin") },
        Action("calendar", "Calendar", Icons.Filled.CalendarMonth, Brand.Accent) { openTab(nav, "calendar") },
    )
    intern -> listOf(
        Action("mytimesheet", "My timesheet", Icons.Filled.TableChart, Brand.Mint) { nav.navigate("my_timesheet") },
        Action("timeoff", "Request time off", Icons.Filled.HourglassBottom, Brand.Orange) { nav.navigate("time_off") },
        Action("claim", "Snap a claim", Icons.Filled.PhotoCamera, Brand.Green) { nav.navigate("expense_new") },
        Action("calendar", "Calendar", Icons.Filled.CalendarMonth, Brand.Accent) { openTab(nav, "calendar") },
    )
    else -> listOf(
        Action("timeoff", "Request time off", Icons.Filled.HourglassBottom, Brand.Orange) { nav.navigate("time_off") },
        Action("claim", "Snap a claim", Icons.Filled.PhotoCamera, Brand.Green) { nav.navigate("expense_new") },
        Action("offinlieu", "Off In Lieu", Icons.Filled.EventAvailable, Brand.Mint) { nav.navigate("timesheet_week") },
        Action("payslips", "Payslips", Icons.Filled.Description, Brand.Accent) { nav.navigate("payslips") },
    )
}

@Composable
private fun QuickAction(a: Action, modifier: Modifier) {
    Card(modifier = modifier.semantics { testTag = "home.action.${a.id}" }, onClick = a.open) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(a.icon, contentDescription = null, tint = a.tint, modifier = Modifier.size(20.dp))
            Text(a.title, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
        }
    }
}

/** Icon and colour per leave tile, by position (web `LEAVE_TILE_STYLE`: paid taken, AL balance, MC, NPL). */
private fun leaveTileStyle(i: Int): Pair<ImageVector, Color> = when (i % 4) {
    0 -> Icons.Filled.EventAvailable to Brand.Green
    1 -> Icons.Filled.WbSunny to Brand.Accent
    2 -> Icons.Filled.MedicalServices to Brand.Red
    else -> Icons.Filled.EventBusy to Brand.Amber
}

/** Switch to a bottom tab from Home, the way the tab bar does. */
private fun openTab(nav: NavController, route: String) {
    nav.navigate(route) {
        popUpTo(nav.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Welcome card; a tap opens My Profile. The name spans the full width on one line (shrinking for a long name);
 * the chevron sits beside "Welcome back" so it takes no width from the name.
 */
@Composable
private fun Greeting(s: DashboardSummary, auth: AuthViewModel, onOpenProfile: () -> Unit) {
    Card(modifier = Modifier.semantics { testTag = "home.profile" }, onClick = onOpenProfile) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Welcome back", color = Brand.TextPrimary.copy(alpha = 0.75f), fontSize = 15.sp, modifier = Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                    tint = Brand.TextPrimary.copy(alpha = 0.4f), modifier = Modifier.size(20.dp),
                )
            }
            FitText(s.name ?: auth.user?.displayName ?: "Employee", maxSize = 28f, minSize = 14f)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                s.role?.takeIf { it.isNotBlank() }?.let { role ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Brand.SurfaceStrong)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            role.lowercase().replaceFirstChar { it.uppercase() },
                            color = Brand.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = Brand.Accent, modifier = Modifier.size(16.dp))
                Text("My Profile", color = Brand.Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Bold text on one line across the available width, shrinking from [maxSize] towards [minSize] to fit. */
@Composable
private fun FitText(text: String, maxSize: Float, minSize: Float) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val widthPx = with(density) { maxWidth.toPx() }
        var size = maxSize
        while (size > minSize) {
            val w = measurer.measure(text, TextStyle(fontSize = size.sp, fontWeight = FontWeight.Bold), maxLines = 1, softWrap = false).size.width
            if (w <= widthPx) break
            size -= 1f
        }
        Text(
            text, color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = size.sp,
            lineHeight = (size + 6).sp, maxLines = 1, softWrap = false,
            modifier = Modifier.semantics { testTag = "home.name" },
        )
    }
}

/** Staff, interns: annual leave and MC left, with Apply for leave / Apply for MC preset to that type. */
@Composable
private fun BalanceCard(s: DashboardSummary, intern: Boolean, nav: NavController) {
    Card(padding = 16) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Balance(fmt(s.alAvailable), "Annual leave left", Icons.Filled.WbSunny, Brand.Accent, Modifier.weight(1f), "al")
                if (intern) Balance("No pay", "Sick leave (MC)", Icons.Filled.MedicalServices, Brand.Pink, Modifier.weight(1f), "mc")
                else Balance(fmt(s.mcAvailable), "MC left", Icons.Filled.MedicalServices, Brand.Pink, Modifier.weight(1f), "mc")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ApplyButton("Apply for leave", Icons.AutoMirrored.Filled.EventNote, filled = true, Modifier.weight(1f).semantics { testTag = "home.apply.AL" }) {
                    nav.navigate("leave_request/AL")
                }
                ApplyButton("Apply for MC", Icons.Filled.MedicalServices, filled = false, Modifier.weight(1f).semantics { testTag = "home.apply.MC" }) {
                    nav.navigate("leave_request/MC")
                }
            }
        }
    }
}

@Composable
private fun Balance(value: String, label: String, icon: ImageVector, tint: Color, modifier: Modifier, id: String) {
    Column(modifier.semantics { testTag = "home.balance.$id" }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Text(label, color = Brand.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 5.dp))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 34.sp, maxLines = 1)
            if (value != "—" && value != "No pay") {
                Text("days", color = Brand.TextSecondary, fontSize = 15.sp, modifier = Modifier.padding(start = 4.dp, bottom = 5.dp))
            }
        }
    }
}

@Composable
private fun ApplyButton(title: String, icon: ImageVector, filled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(if (filled) Brand.Primary else Brand.Field)
            .border(1.dp, if (filled) Color.Transparent else Brand.Accent.copy(alpha = 0.5f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (filled) Color.White else Brand.Accent, modifier = Modifier.size(18.dp))
        Text(
            title, color = if (filled) Color.White else Brand.Accent, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
            maxLines = 1, modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** Approvers: what is waiting by type; each row opens Approvals. */
@Composable
private fun PendingCard(p: PendingApprovals, nav: NavController) {
    val rows = listOf(
        PendingRow("leave", "Leave", Icons.AutoMirrored.Filled.EventNote, Brand.Accent, p.leave, "approvals"),
        PendingRow("mc", "MC", Icons.Filled.MedicalServices, Brand.Pink, p.mc, "approvals"),
        PendingRow("timeoff", "Time off", Icons.Filled.HourglassBottom, Brand.Orange, p.timeOff, "approvals"),
        PendingRow("claims", "Expense claims", Icons.Filled.CreditCard, Brand.Green, p.claims, "approvals"),
        PendingRow("payback", "Payback", Icons.Filled.Replay, Brand.Purple, p.paybacks, "approvals"),
    )
    Card(padding = 0) {
        rows.forEachIndexed { i, r ->
            if (i > 0) Divider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { nav.navigate(r.route) }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .semantics { testTag = "home.pending.${r.id}" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(r.icon, contentDescription = null, tint = r.tint, modifier = Modifier.width(24.dp))
                Text(r.label, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    modifier = Modifier.padding(start = 12.dp).weight(1f))
                Text("${r.count}", color = if (r.count > 0) Brand.Amber else Brand.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                    tint = Brand.TextPrimary.copy(alpha = 0.4f), modifier = Modifier.padding(start = 6.dp).size(18.dp))
            }
        }
    }
}

private data class PendingRow(val id: String, val label: String, val icon: ImageVector, val tint: Color, val count: Int, val route: String)

/**
 * Accountants: pending claims, income / expenses this month, bank statements, bank reconciliation. Every row opens
 * its detail (Alfred, 22 Nov 2026); pending claims open Approvals for approvers only (the server refuses anyone else).
 */
@Composable
private fun FinanceCard(f: FinanceKpis, canApprove: Boolean, nav: NavController) {
    val tiles = HomeTiles.finance(f)
    Card(padding = 0) {
        tiles.forEachIndexed { i, t ->
            if (i > 0) Divider()
            val (icon, tint) = when (t.id) {
                "pending" -> Icons.AutoMirrored.Filled.ReceiptLong to (if (f.pendingClaims > 0) Brand.Amber else Brand.Accent)
                "income" -> Icons.AutoMirrored.Filled.TrendingUp to Brand.Green
                "expenses" -> Icons.AutoMirrored.Filled.TrendingDown to Brand.Red
                "statements" -> Icons.Filled.Description to Brand.Accent
                else -> Icons.Filled.Balance to when {
                    f.reconciliation == null -> Brand.TextSecondary
                    f.reconciliation.toReview > 0 -> Brand.Amber
                    else -> Brand.Green
                }
            }
            val route = financeRoute(t.id, canApprove)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (route != null) Modifier.clickable { nav.navigate(route) } else Modifier)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .semantics { testTag = "home.finance.${t.id}" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.width(24.dp))
                Column(Modifier.padding(start = 12.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(t.label, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
                    t.hint?.let { Text(it, color = Brand.TextSecondary, fontSize = 12.sp, maxLines = 2) }
                }
                Text(t.value, color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp))
                if (route != null) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                        tint = Brand.TextPrimary.copy(alpha = 0.4f), modifier = Modifier.padding(start = 4.dp).size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().padding(start = 50.dp).height(1.dp).background(Brand.TextPrimary.copy(alpha = 0.08f)))
}

/**
 * Home, for people who must clock in (interns, accountants without an approver role): today's punch and a
 * one-tap Clock in / Clock out, right under the greeting. History lives on the Clock screen. While an approver
 * previews the role ([preview]) the button shows but cannot punch.
 */
@Composable
private fun HomeClockCard(preview: Boolean, onHistory: () -> Unit) {
    var today by remember { mutableStateOf<AttendancePunch?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var punching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        runCatching { HrmsApi.attendance() }.getOrNull()?.let { today = it.today }
        loaded = true
    }
    LaunchedEffect(Unit) { load() }

    val inAt = Fmt.parse(today?.clockIn)
    val outAt = Fmt.parse(today?.clockOut)
    val working = inAt != null && outAt == null
    val done = inAt != null && outAt != null

    Card(padding = 16, modifier = Modifier.semantics { testTag = "home.clock" }) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when { working -> Icons.Filled.Timer; done -> Icons.Filled.CheckCircle; else -> Icons.Filled.WbTwilight },
                    contentDescription = null,
                    tint = when { working -> Brand.Green; done -> Brand.Accent; else -> Brand.Amber },
                    modifier = Modifier.size(28.dp),
                )
                Column(Modifier.padding(start = 12.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Today", color = Brand.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    when {
                        working -> {
                            var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                            LaunchedEffect(inAt) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
                            val since = inAt!!.time
                            Text(
                                "Clocked in ${time(since)}" + (elapsed(since, now)?.let { " · $it" } ?: ""),
                                color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                            )
                        }
                        done -> {
                            val hours = (outAt!!.time - inAt!!.time) / 3_600_000.0
                            Text(
                                "${time(inAt.time)} – ${time(outAt.time)} · " + String.format(Locale.US, "%.1f h", hours),
                                color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                            )
                        }
                        else -> Text(if (loaded) "Not clocked in yet" else "Loading…", color = Brand.TextPrimary,
                            fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
                Text("History", color = Brand.Accent, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    modifier = Modifier.clickable(onClick = onHistory).padding(8.dp))
            }
            if (!done) {
                val enabled = loaded && !punching && !preview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .alpha(if (preview) 0.45f else 1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (working) Brand.Red else Brand.Primary)
                        .then(if (enabled) Modifier.clickable {
                            scope.launch {
                                error = null
                                punching = true
                                try {
                                    if (working) HrmsApi.clockOut() else HrmsApi.clockIn()
                                    load()
                                } catch (e: Exception) {
                                    error = e.message ?: "Could not record your punch."
                                } finally {
                                    punching = false
                                }
                            }
                        } else Modifier)
                        .semantics { testTag = "home.clock.punch" },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (punching) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    else Icon(if (working) Icons.AutoMirrored.Filled.Logout else Icons.AutoMirrored.Filled.Login,
                        contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(if (working) "Clock out" else "Clock in", color = Color.White, fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }
            error?.let { Text(it, color = Brand.Red, fontSize = 13.sp) }
        }
    }
}

private val clockFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
private fun time(ms: Long): String = clockFmt.format(java.util.Date(ms))

/** Time since clock-in; null past 24 h (an open punch from an earlier day is not "working now"). */
private fun elapsed(from: Long, now: Long): String? {
    val m = ((now - from) / 60_000).coerceAtLeast(0)
    if (m >= 24 * 60) return null
    return "${m / 60}h ${String.format(Locale.US, "%02d", m % 60)}m"
}

/**
 * Home in the Trainer Admin view (web view mode "trainer"; iOS `trainerAdminHome`): the title card with the name, the
 * View As switch right under it (Alfred, 22 Nov 2026, as on the other Homes), the trainer search, the three trainer
 * counts (each opening the directory on its filter) and a link to the directory.
 */
@Composable
private fun TrainerAdminHome(auth: AuthViewModel, nav: NavController) {
    AsyncListScreen(fetch = { HrmsApi.summary() }) { s ->
        item {
            Card(modifier = Modifier.semantics { testTag = "home.trainerAdmin.title" }) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Trainer Admin", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp)
                    val name = s.name ?: auth.user?.displayName
                    if (!name.isNullOrBlank()) Text(name, color = Brand.TextSecondary, fontSize = 15.sp)
                }
            }
        }
        if (auth.availableViews.size > 1) {
            item { Spacer18() }
            item { ViewAsRow(auth) }
        }
        item { Spacer18() }
        // Search every trainer by name, skill, certification or course (Alfred, 22 Nov 2026).
        item {
            TrainerHomeSearch(
                onOpen = { ViewAs.pendingDirectory = it; openTab(nav, "trainers") },
                onProfile = { nav.navigate("trainer_profile/$it") },
            )
        }
        item { Spacer18() }
        s.trainerKpis?.let { t ->
            item {
                KpiStrip(ViewAsRules.trainerTiles(t).map { (tile, filter) ->
                    Kpi(tile.value, tile.label, Brand.TextPrimary) {
                        ViewAs.pendingDirectory = DirectoryRequest(filter)
                        openTab(nav, "trainers")
                    }
                })
            }
            item { Spacer18() }
        }
        item {
            Card(onClick = { openTab(nav, "trainers") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.School, contentDescription = null, tint = Brand.Accent, modifier = Modifier.size(22.dp))
                    Text(
                        "Open trainer directory", color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        modifier = Modifier.padding(start = 12.dp).weight(1f),
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                        tint = Brand.TextPrimary.copy(alpha = 0.4f), modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** View As on Home (approvers and Trainer Admins): the menu, and a note while previewing another role. */
@Composable
private fun ViewAsRow(auth: AuthViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ViewAsMenu(auth, tag = "home.viewAs")
        if (auth.isRolePreview) {
            Text(
                "Previewing the ${ViewAs.mode.title(true)} screens. Forms still use your own account; clocking in is off.",
                color = Brand.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
    }
}

private fun fmt(v: Double?): String = if (v == null) "—" else Fmt.num(v)
