package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tertiaryinfotech.hrportal.ui.AuthViewModel
import com.tertiaryinfotech.hrportal.ui.components.BrandBarTitle
import com.tertiaryinfotech.hrportal.ui.components.NotificationBell
import com.tertiaryinfotech.hrportal.ui.theme.Brand

/** The five bottom tabs, matching the iOS `MainTabView` 1:1. */
private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Filled.Home),
    LEAVE("leave", "Leave", Icons.AutoMirrored.Filled.EventNote),
    CALENDAR("calendar", "Calendar", Icons.Filled.CalendarMonth),
    TEAM("team", "Team", Icons.Filled.Groups),
    /** Trainer Admins only (`summary.canManageTrainers`). */
    TRAINERS("trainers", "Trainers", Icons.Filled.School),
    MORE("more", "More", Icons.Filled.Pending),
}

/** Form screens show a plain title. Every other screen shows the company lockup, as on iOS. */
private val FORM_TITLES = mapOf(
    "leave_request" to "Apply for leave",
    "expense_new" to "New claim",
    "time_off_request" to "Request time off",
    "calendar_new" to "Add event",
    "payslip_pdf" to "Payslip",
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
    val rootTab = Tab.entries.firstOrNull { it.route == route }
    var activeTabName by rememberSaveable { mutableStateOf(Tab.HOME.name) }
    LaunchedEffect(rootTab) { rootTab?.let { activeTabName = it.name } }
    val activeTab = rootTab ?: Tab.valueOf(activeTabName)

    LaunchedEffect(Unit) {
        auth.refreshUnreadCount()
        auth.refreshCapabilities()
    }
    val tabs = Tab.entries.filter { it != Tab.TRAINERS || auth.canManageTrainers }

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
                        Tab.HOME.route -> NotificationBell(auth.unreadNotifications) { nav.navigate("notifications") }
                        Tab.LEAVE.route -> AddAction("Apply for leave") { nav.navigate("leave_request/AL") }
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
            composable(Tab.LEAVE.route) { LeaveScreen(nav) }
            composable(Tab.CALENDAR.route) { TeamCalendarScreen() }
            composable(Tab.TEAM.route) { TeamScreen(nav) }
            composable(Tab.MORE.route) { MoreScreen(auth, nav) }
            composable(Tab.TRAINERS.route) { TrainersScreen(nav) }
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
            composable("expenses") { ExpensesScreen(nav) }
            composable("expense_new") { NewClaimScreen(nav) }
            composable("payslips") { PayslipsScreen(nav) }
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
            composable("timesheet") { TimesheetScreen(nav) }
            composable("timesheet_week") { WeeklyTimesheetScreen(nav) }
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
            composable("notifications") { NotificationsScreen(auth, nav) }
            composable("payslip_pdf") { PayslipPdfScreen(nav) }
        }
    }
}

@Composable
private fun AddAction(description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Filled.AddCircle, contentDescription = description, tint = Brand.BarAccent)
    }
}

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
