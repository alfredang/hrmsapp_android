package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.tertiaryinfotech.hrportal.data.Net
import com.tertiaryinfotech.hrportal.ui.AuthViewModel
import com.tertiaryinfotech.hrportal.ui.components.InitialsAvatar
import com.tertiaryinfotech.hrportal.ui.components.TopBarActions
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import kotlinx.coroutines.launch

/**
 * The signed-in app shell. Only Home/Leave show as bottom tabs (per product decision); a
 * hamburger-triggered drawer mirrors the website's full sidebar (Dashboard, My Profile, Leave,
 * Expense Claims, Payroll, Calendar, Timesheet, Woods Square Access) for everything else —
 * Expenses/Payroll/Profile stay reachable from there and still share this same top bar chrome
 * (title/hamburger/bell/avatar) when opened, they just aren't bottom-nav destinations anymore.
 * Accounting/admin-authoring stay excluded (CLAUDE.md).
 */
private enum class Tab(val route: String, val label: String, val icon: ImageVector, val title: String, val showInBottomNav: Boolean = true) {
    HOME("home", "Home", Icons.Filled.Dashboard, "Dashboard"),
    LEAVE("leave", "Leave", Icons.Filled.Schedule, "Leave"),
    EXPENSES("expenses", "Expenses", Icons.Filled.Receipt, "Expense Claims", showInBottomNav = false),
    PAYROLL("payslips", "Payroll", Icons.Filled.CreditCard, "Payslips", showInBottomNav = false),
    PROFILE("profile", "Profile", Icons.Filled.AccountCircle, "My Profile", showInBottomNav = false),
}

private data class DrawerItem(val route: String, val label: String, val icon: ImageVector)

// Outlined variants throughout — Filled icons render at inconsistent visual weights across
// different glyphs (some solid, some already stroke-like), which reads as a mismatched icon set;
// Outlined keeps every row the same thin-stroke style, matching the web sidebar.
private val DRAWER_ITEMS = listOf(
    DrawerItem("home", "Dashboard", Icons.Outlined.Dashboard),
    DrawerItem("profile", "My Profile", Icons.Outlined.AccountCircle),
    DrawerItem("leave", "Leave", Icons.Outlined.Schedule),
    DrawerItem("expenses", "Expense Claims", Icons.Outlined.Receipt),
    DrawerItem("payslips", "Payroll", Icons.Outlined.CreditCard),
    DrawerItem("calendar", "Calendar", Icons.Outlined.CalendarMonth),
    DrawerItem("timesheet_week", "Timesheet", Icons.Outlined.EventNote),
    DrawerItem("time_off", "Time Off", Icons.Outlined.MoreTime),
    // The clock in/out punch screen keeps the pre-existing "timesheet" route (it stores punches,
    // not the weekly OT grid) — only its label changed when the real weekly Timesheet arrived.
    DrawerItem("timesheet", "Clock In / Out", Icons.Outlined.AccessTime),
)

/** Drawer-only destinations (not a bottom [Tab]) that still share the hamburger top bar rather
 *  than rendering their own back-arrow header — same chrome as the tab screens, just no bottom
 *  nav highlight since they aren't bottom-tab destinations. */
private val DRAWER_ONLY_TOP_BAR_TITLES = mapOf(
    "calendar" to "Calendar",
    // Order matters: title resolution is startsWith-based and "timesheet_week" / "time_off_request"
    // both start with a sibling route's prefix, so the longer keys must come first.
    "timesheet_week" to "Timesheet",
    "timesheet" to "Clock In / Out",
    "time_off" to "Time Off",
    "approvals" to "Approvals",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(auth: AuthViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val currentTab = Tab.entries.firstOrNull { currentRoute?.startsWith(it.route) == true }
    val drawerOnlyTitle = DRAWER_ONLY_TOP_BAR_TITLES.entries.firstOrNull { currentRoute?.startsWith(it.key) == true }?.value
    val topBarTitle = currentTab?.title ?: drawerOnlyTitle
    val showSharedTopBar = topBarTitle != null

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { auth.refreshUnreadCount() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                auth = auth,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    switchTab(nav, route)
                },
                onSignOut = { scope.launch { drawerState.close() }; auth.signOut() },
            )
        },
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (showSharedTopBar) {
                    TopAppBar(
                        title = { Text(topBarTitle!!, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Brand.TextPrimary)
                            }
                        },
                        actions = {
                            if (currentTab == Tab.HOME) {
                                RoleViewSwitcher(assignedRoles = auth.user?.roles ?: emptyList(), fallbackRole = auth.user?.role)
                            }
                            TopBarActions(
                                initials = auth.user?.initials ?: "?",
                                displayName = auth.user?.displayName ?: "",
                                email = auth.user?.email ?: "",
                                avatarUrl = null,
                                unreadCount = auth.unreadNotifications,
                                onBellClick = { nav.navigate("notifications") },
                                onProfileClick = { switchTab(nav, "profile") },
                                onSignOut = { auth.signOut() },
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Brand.Background, titleContentColor = Brand.TextPrimary),
                    )
                }
            },
            bottomBar = {
                if (currentTab != null) {
                    NavigationBar(containerColor = Brand.Background) {
                        Tab.entries.filter { it.showInBottomNav }.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { switchTab(nav, tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Brand.Primary,
                                    selectedTextColor = Brand.Primary,
                                    indicatorColor = Color.Transparent,
                                    unselectedIconColor = Brand.TextSecondary,
                                    unselectedTextColor = Brand.TextSecondary,
                                ),
                            )
                        }
                    }
                }
            },
        ) { inner ->
            NavHost(
                navController = nav,
                startDestination = Tab.HOME.route,
                modifier = Modifier.padding(inner),
            ) {
                composable(Tab.HOME.route) { DashboardScreen(auth, nav) }
                composable(Tab.LEAVE.route) { LeaveScreen(nav) }
                composable(Tab.EXPENSES.route) { ExpensesScreen() }
                composable(Tab.PAYROLL.route) { PayslipsScreen(nav) }
                composable(Tab.PROFILE.route) { ProfileScreen(nav) }

                // Route starts with "leave" so the Tab-matching logic above still resolves this
                // to Tab.LEAVE — same shared hamburger top bar + bottom nav as the Leave tab
                // itself, matching the web's own "Request Leave" page (not a modal).
                composable("leave_request/{type}") { backStackEntry ->
                    ApplyLeaveScreen(nav, backStackEntry.arguments?.getString("type"))
                }

                // Drawer-only destinations (own back-button top bar via BrandScaffold)
                composable("calendar") { CalendarScreen(nav) }
                composable("calendar_day/{date}") { backStackEntry ->
                    CalendarDayScreen(nav, backStackEntry.arguments?.getString("date") ?: "")
                }
                composable("calendar_new/{date}") { backStackEntry ->
                    AddCalendarEventScreen(nav, backStackEntry.arguments?.getString("date")?.takeIf { it != "none" })
                }
                composable("timesheet") { TimesheetScreen(nav) }
                composable("timesheet_week") { WeeklyTimesheetScreen(nav) }
                composable("time_off") { TimeOffScreen(nav) }
                // Form destination (not in the drawer) — its route starts with "time_off" so the
                // startsWith matching above keeps the shared "Time Off" top bar, same trick as
                // "leave_request" under the Leave tab.
                composable("time_off_request") { RequestTimeOffScreen(nav) }
                composable("approvals") { ApprovalsScreen(nav) }
                composable("notifications") { NotificationsScreen(auth, nav) }
                composable("payslip_pdf") { PayslipPdfScreen(nav) }
            }
        }
    }
}

private fun switchTab(nav: NavHostController, route: String) {
    nav.navigate(route) {
        popUpTo(nav.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AppDrawerContent(auth: AuthViewModel, currentRoute: String?, onNavigate: (String) -> Unit, onSignOut: () -> Unit) {
    ModalDrawerSheet(drawerContainerColor = Brand.Surface) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Same real-logo-or-initials-fallback logic as BrandHeader on the login screen —
                // the drawer should show the actual company logo, not a hardcoded "TI" mark.
                val logoUrl = auth.branding?.logo?.takeIf { it.isNotBlank() }?.let { Net.url(it) }
                var logoFailed by remember(logoUrl) { mutableStateOf(false) }
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Brand.Primary), contentAlignment = Alignment.Center) {
                    if (logoUrl != null && !logoFailed) {
                        AsyncImage(
                            model = logoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            onError = { logoFailed = true },
                            modifier = Modifier.size(36.dp).clip(CircleShape),
                        )
                    } else {
                        Text(auth.branding?.initials ?: "TI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                Text(
                    "Tertiary Infotech Acadmey", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            // Longest-prefix match, not plain startsWith — "timesheet_week" starts with the clock
            // screen's "timesheet" route, and only the more specific row should highlight.
            val selectedRoute = DRAWER_ITEMS
                .filter { currentRoute?.startsWith(it.route) == true }
                .maxByOrNull { it.route.length }?.route
            DRAWER_ITEMS.forEach { item ->
                val selected = item.route == selectedRoute
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(item.route) }
                        .background(if (selected) Brand.Primary.copy(alpha = 0.15f) else Color.Transparent)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(item.icon, contentDescription = null, tint = if (selected) Brand.Primary else Brand.TextSecondary, modifier = Modifier.size(20.dp))
                    Text(
                        item.label, color = if (selected) Brand.Primary else Brand.TextPrimary, fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InitialsAvatar(auth.user?.initials ?: "?", 36)
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(auth.user?.displayName ?: "", color = Brand.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Text(auth.user?.email ?: "", color = Brand.TextSecondary, fontSize = 11.sp, maxLines = 1)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onSignOut).padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Brand.Red, modifier = Modifier.size(20.dp))
                Text("Sign out", color = Brand.Red, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}

private data class ViewRoleOption(val code: String, val label: String, val icon: ImageVector)

/** Staff/Intern only — Admin and Accountant are web-only roles (CLAUDE.md excludes accounting and
 *  admin-authoring flows from this app), so they're never offered here even if assigned. */
private val VIEW_ROLE_OPTIONS = listOf(
    ViewRoleOption("STAFF", "Staff", Icons.Filled.Person),
    ViewRoleOption("INTERN", "Intern", Icons.Filled.School),
)

/**
 * Mirrors the web dashboard's "Switch Role View" control (`view-toggle.tsx`) — an eye icon, the
 * active role's icon, its label, and a chevron, sitting in the top bar next to the bell/avatar
 * (same placement as the web's header row), shown only on the Home tab. Restricted to Staff/Intern
 * per product decision; this app has no separate admin/staff dashboard content to swap (native app
 * is intentionally staff-scoped — CLAUDE.md), so picking a role here only changes the displayed
 * label.
 */
@Composable
private fun RoleViewSwitcher(assignedRoles: List<String>, fallbackRole: String?) {
    val available = VIEW_ROLE_OPTIONS.filter { opt -> assignedRoles.any { it.equals(opt.code, ignoreCase = true) } }
        .ifEmpty { VIEW_ROLE_OPTIONS.filter { it.code.equals(fallbackRole, ignoreCase = true) } }
    if (available.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf(available.first().code) }
    val current = available.firstOrNull { it.code == selected } ?: available.first()

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .border(1.dp, Brand.BorderLight, RoundedCornerShape(50))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(Icons.Filled.Visibility, contentDescription = null, tint = Brand.TextSecondary, modifier = Modifier.size(14.dp))
            Icon(current.icon, contentDescription = null, tint = Brand.Primary, modifier = Modifier.size(14.dp))
            Text(current.label, color = Brand.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Brand.TextSecondary, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(
                "SWITCH ROLE VIEW", color = Brand.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
            available.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.label) },
                    leadingIcon = { Icon(opt.icon, contentDescription = null) },
                    trailingIcon = {
                        if (opt.code == current.code) Icon(Icons.Filled.Check, contentDescription = null, tint = Brand.Primary)
                    },
                    onClick = { selected = opt.code; expanded = false },
                )
            }
        }
    }
}
