package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tertiaryinfotech.hrportal.data.BankDirection
import com.tertiaryinfotech.hrportal.data.CpfSubmissions
import com.tertiaryinfotech.hrportal.data.MainTabs
import com.tertiaryinfotech.hrportal.push.Push
import com.tertiaryinfotech.hrportal.push.routeForLink
import com.tertiaryinfotech.hrportal.ui.AuthViewModel
import com.tertiaryinfotech.hrportal.ui.ViewAs
import com.tertiaryinfotech.hrportal.ui.components.BrandBarTitle
import com.tertiaryinfotech.hrportal.ui.components.NotificationBell
import com.tertiaryinfotech.hrportal.ui.theme.Brand

/**
 * The bottom tabs, matching the iOS `MainTabView` 1:1: Home / Calendar / Team / SOP / More for admins (no Leave tab
 * since 25 Nov 2026), Home / Leave / Calendar / SOP / More for staff and interns (the full team list is admin-only;
 * `/api/mobile/employees` answers 403 otherwise), Accountants get Home / Income / Expenses / More, the Trainer Admin
 * view (More → View As) Home / Trainers / SOP / More ([MainTabs.routes]).
 */
private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Filled.Home),
    LEAVE("leave", "Leave", Icons.AutoMirrored.Filled.EventNote),
    CALENDAR("calendar", "Calendar", Icons.Filled.CalendarMonth),
    TEAM("team", "Team", Icons.Filled.Groups),
    // Standard Operating Procedures: staff and interns in Team's place (22 Nov 2026); admins after Team and the
    // Trainer Admin view after Trainers (25 Nov 2026).
    SOP("sops", "SOP", Icons.AutoMirrored.Filled.MenuBook),
    // Accountants: the month's bank income and expenses (the same screens as Home → Finance → Income / Expenses).
    INCOME("finance_income", "Income", Icons.AutoMirrored.Filled.TrendingUp),
    EXPENSES("finance_expenses", "Expenses", Icons.Filled.CreditCard),
    // Trainer Admin view only: the trainer directory as a tab (the same "trainers" route More opens).
    TRAINERS("trainers", "Trainers", Icons.Filled.School),
    MORE("more", "More", Icons.Filled.Pending),
}

/** Form screens show a plain title. Every other screen shows the app lockup ("TIA HRMS"), as on iOS. */
private val FORM_TITLES = mapOf(
    "leave_request" to "Apply for leave",
    "expense_new" to "New claim",
    "time_off_request" to "Request time off",
    "calendar_new" to "Add event",
    "payslip_pdf" to "Payslip",
    "cpf_pdf" to "CPF statement",
    "notifications" to "Notifications",
)

/**
 * The signed-in app shell: the navy brand bar on top and the tab bar on every screen. The top bar
 * shows a back chevron on anything below a tab root.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(auth: AuthViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    // A nested screen stays under whichever tab it was opened from, so the Clock screen highlights
    // Home when reached from Home's quick action and More when reached from the More menu.
    val trainerView = auth.isTrainerView
    val shown = auth.shown
    // Accountants have no leave, MC or time off (Alfred, 22 Nov 2026): no Leave tab.
    val tabs = tabsFor(trainerView, admin = shown.admin, accountant = shown.accountant)
    val rootTab = tabs.firstOrNull { it.route == route }
    var activeTabName by rememberSaveable { mutableStateOf(Tab.HOME.name) }
    LaunchedEffect(rootTab) { rootTab?.let { activeTabName = it.name } }
    val activeTab = rootTab ?: Tab.valueOf(activeTabName)

    // Switching View As: Trainer Admin opens the Trainers tab, any other view goes back to Home.
    val viewKey = "${ViewAs.mode.key}:$trainerView"
    var shownView by rememberSaveable { mutableStateOf(viewKey) }
    LaunchedEffect(viewKey) {
        if (viewKey == shownView) return@LaunchedEffect
        shownView = viewKey
        val target = if (trainerView) Tab.TRAINERS else Tab.HOME
        activeTabName = target.name
        switchTab(nav, target.route)
    }

    LaunchedEffect(Unit) {
        auth.refreshUnreadCount()
        auth.refreshCapabilities()
    }

    // Push alerts: ask once for the Android 13+ notification permission after sign-in.
    val context = LocalContext.current
    val notifyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !Push.canNotify(context)) {
            notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Back in the foreground (often from a push alert): refresh the bell's unread count.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) auth.refreshUnreadCount()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // A tapped push alert opens its screen (approvals for admins, Clock for interns, …).
    val pendingLink = Push.pendingLink
    LaunchedEffect(pendingLink) {
        val link = pendingLink ?: return@LaunchedEffect
        Push.pendingLink = null
        val linked = routeForLink(link, isApprover = isApprover(auth), mustClockIn = auth.user?.mustClockIn == true) ?: "notifications"
        // The Team tab is admin-only: anyone else lands on the SOPs in its place (iOS `.tab(3)`).
        val target = if (linked == Tab.TEAM.route && !shown.admin) {
            if (shown.accountant) Tab.EXPENSES.route else Tab.SOP.route
        } else linked
        val tab = tabs.firstOrNull { it.route == target }
        if (tab != null) { activeTabName = tab.name; switchTab(nav, target) } else nav.navigate(target)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val formTitle = FORM_TITLES.entries.firstOrNull { route?.startsWith(it.key) == true }?.value
                    if (formTitle != null) {
                        Text(formTitle, color = Brand.OnBar, fontWeight = FontWeight.SemiBold)
                    } else {
                        BrandBarTitle()
                    }
                },
                navigationIcon = {
                    if (rootTab == null) {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Back", tint = Brand.BarAccent)
                        }
                    }
                },
                actions = {
                    when (route) {
                        // The SOP tab carries the bell too, as on iOS (`brandBar(bell: true)`).
                        Tab.HOME.route, Tab.SOP.route -> NotificationBell(auth.unreadNotifications) { nav.navigate("notifications") }
                        Tab.LEAVE.route -> if (!shown.admin) AddAction("Apply for leave") { nav.navigate("leave_request/AL") }
                        "expenses" -> AddAction("New claim") { nav.navigate("expense_new") }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Brand.Bar),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Brand.Bar) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = activeTab == tab,
                        onClick = {
                            activeTabName = tab.name
                            if (tab == activeTab) popToRoot(nav, tab.route) else switchTab(nav, tab.route)
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, maxLines = 1, softWrap = false) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Brand.BarAccent,
                            selectedTextColor = Brand.BarAccent,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = Brand.OnBar.copy(alpha = 0.6f),
                            unselectedTextColor = Brand.OnBar.copy(alpha = 0.6f),
                        ),
                    )
                }
            }
        },
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Tab.HOME.route,
            // imePadding shrinks the content above the keyboard so a focused form field scrolls into view.
            modifier = Modifier.padding(inner).consumeWindowInsets(inner).imePadding(),
        ) {
            composable(Tab.HOME.route) { DashboardScreen(auth, nav) }
            // Admins do not apply for leave or MC: a /leave link opens leave management (everyone's leave and MC).
            composable(Tab.LEAVE.route) { if (shown.admin) LeaveSummaryScreen(nav) else LeaveScreen(nav) }
            // Accountants and the Trainer Admin view have no Calendar tab: a link to it pushes this screen instead.
            composable(Tab.CALENDAR.route) { TeamCalendarScreen(admin = shown.admin) }
            composable(Tab.TEAM.route) { TeamScreen(nav) }
            // Standard Operating Procedures: the SOP tab, More → Documents → SOP, and one SOP.
            composable(Tab.SOP.route) { SopListScreen(nav) }
            composable("sop_list") { SopListScreen(nav) }
            composable("sop/{slug}") { e -> SopDetailScreen(e.arguments?.getString("slug").orEmpty()) }
            composable(Tab.MORE.route) { MoreScreen(auth, nav) }
            // Trainer Admin directory (TRAINER_ADMIN only) — More → Manage, or the Trainers tab of the Trainer Admin view.
            composable(Tab.TRAINERS.route) { TrainersScreen(nav) }
            // Trainer contact list — every signed-in user, opened from More.
            composable("trainer_contacts") { TrainerContactsScreen(nav) }
            composable("trainer_contact/{id}") { e ->
                TrainerContactScreen(e.arguments?.getString("id").orEmpty())
            }
            composable("trainer_profile/{id}") { e ->
                TrainerProfileScreen(e.arguments?.getString("id").orEmpty())
            }

            composable("team_records/{id}") { e ->
                EmployeeRecordsScreen(e.arguments?.getString("id").orEmpty(), nav)
            }
            composable("team_records_clock/{id}") { e ->
                RecordsClockScreen(e.arguments?.getString("id").orEmpty())
            }
            composable("team_records_leave/{id}?kind={kind}") { e ->
                val kind = RecordsLeaveKind.entries.firstOrNull { it.name == e.arguments?.getString("kind") }
                RecordsLeaveScreen(e.arguments?.getString("id").orEmpty(), kind ?: RecordsLeaveKind.LEAVE)
            }
            composable("team_group/{group}") { e -> TeamScreen(nav, initialGroup = e.arguments?.getString("group") ?: "staff") }
            composable("expenses") { ExpensesScreen(nav) }
            composable("expense_new") { NewClaimScreen(nav) }
            // CPF EZPay statements list everyone's NRIC and wages: admin and accountant views only (server 403 otherwise).
            composable("payslips") { PayslipsScreen(nav, showCpf = CpfSubmissions.canView(shown)) }
            composable("cpf_submissions") { CpfSubmissionsScreen(nav) }
            composable("cpf_pdf") { CpfPdfScreen() }
            composable("profile") { ProfileScreen(nav) }
            composable("leave_request/{type}") { e ->
                ApplyLeaveScreen(nav, e.arguments?.getString("type"), isIntern = auth.user?.isIntern == true)
            }
            composable("calendar_list") { CalendarScreen(nav) }
            composable("calendar_day/{date}") { e ->
                CalendarDayScreen(nav, e.arguments?.getString("date") ?: "")
            }
            composable("calendar_new/{date}") { e ->
                AddCalendarEventScreen(nav, e.arguments?.getString("date")?.takeIf { it != "none" })
            }
            // An approver previewing another role (View As) must not create a real punch.
            composable("timesheet") { TimesheetScreen(nav, preview = auth.isRolePreview) }
            composable("timesheet_week") { WeeklyTimesheetScreen(nav) }
            // Interns and accountants who clock in: their own days and hours by month (iOS MyTimesheetView).
            composable("my_timesheet") { MyTimesheetScreen(nav) }
            composable("time_off") { TimeOffScreen(nav, auth) }
            composable("time_off_request") { RequestTimeOffScreen(nav) }
            composable("approvals") { ApprovalsScreen(nav) }
            // One admin Timesheet (interns' days + hours, missing punches, Off In Lieu). The old
            // "intern_attendance" route is kept as an alias for links that still point at it.
            composable("timesheet_admin") { AdminTimesheetScreen(nav) }
            composable("intern_attendance") { AdminTimesheetScreen(nav) }
            composable("intern_attendance_detail/{id}?name={name}&code={code}&month={month}") { e ->
                val a = e.arguments
                InternAttendanceDetailScreen(
                    employeeId = a?.getString("id").orEmpty(),
                    name = a?.getString("name").orEmpty(),
                    code = a?.getString("code").orEmpty(),
                    initialMonth = a?.getString("month") ?: AttendanceMonth.current(),
                )
            }
            composable("mc_leave_summary") { LeaveSummaryScreen(nav) }
            composable("mc_leave_summary_detail/{id}?month={month}") { e ->
                val a = e.arguments
                LeaveSummaryDetailScreen(
                    employeeId = a?.getString("id").orEmpty(),
                    month = a?.getString("month") ?: AttendanceMonth.current(),
                )
            }
            // The accountant Home's Finance card rows (admin / accountant; the server refuses anyone else).
            composable("finance_income") { BankTransactionsScreen(BankDirection.CREDIT) }
            composable("finance_expenses") { BankTransactionsScreen(BankDirection.DEBIT) }
            composable("finance_statements") { BankStatementsScreen() }
            composable("finance_reconciliation") { ReconciliationScreen() }
            composable("notifications") { NotificationsScreen(auth, nav) }
            composable("payslip_pdf") { PayslipPdfScreen(nav, showCpf = CpfSubmissions.canView(shown)) }
        }
    }
}

@Composable
private fun AddAction(description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Filled.AddCircle, contentDescription = description, tint = Brand.BarAccent)
    }
}


/** The bottom tabs for the current view; [admin] / [accountant] are what the screens show (View As aware). */
private fun tabsFor(trainerView: Boolean, admin: Boolean, accountant: Boolean): List<Tab> =
    MainTabs.routes(trainerView, admin, accountant).map { r -> Tab.entries.first { it.route == r } }

private fun switchTab(nav: NavHostController, route: String) {
    nav.navigate(route) {
        popUpTo(nav.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Tapping the tab you're already on returns to its root, like the iOS tab bar. */
private fun popToRoot(nav: NavHostController, route: String) {
    if (!nav.popBackStack(route, inclusive = false)) switchTab(nav, route)
}
