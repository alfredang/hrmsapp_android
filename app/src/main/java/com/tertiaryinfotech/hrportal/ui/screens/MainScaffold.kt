package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tertiaryinfotech.hrportal.ui.AuthViewModel
import com.tertiaryinfotech.hrportal.ui.components.InitialsAvatar
import com.tertiaryinfotech.hrportal.ui.components.TopBarActions
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import kotlinx.coroutines.launch

/**
 * The signed-in app shell. Five bottom tabs mirror the web's primary staff/intern nav
 * (Home/Leave/Expenses/Payroll/Profile); a hamburger-triggered drawer mirrors the website's full
 * sidebar (Dashboard, My Profile, Leave, Expense Claims, Payroll, Calendar, Timesheet, Woods
 * Square Access) for everything else. Accounting/admin-authoring stay excluded (CLAUDE.md).
 */
private enum class Tab(val route: String, val label: String, val icon: ImageVector, val title: String) {
    HOME("home", "Home", Icons.Filled.Dashboard, "Dashboard"),
    LEAVE("leave", "Leave", Icons.Filled.Schedule, "Leave"),
    EXPENSES("expenses", "Expenses", Icons.Filled.Receipt, "Expense Claims"),
    PAYROLL("payslips", "Payroll", Icons.Filled.CreditCard, "Payslips"),
    PROFILE("profile", "Profile", Icons.Filled.AccountCircle, "My Profile"),
}

private data class DrawerItem(val route: String, val label: String, val icon: ImageVector)

private val DRAWER_ITEMS = listOf(
    DrawerItem("home", "Dashboard", Icons.Filled.Dashboard),
    DrawerItem("profile", "My Profile", Icons.Filled.AccountCircle),
    DrawerItem("leave", "Leave", Icons.Filled.Schedule),
    DrawerItem("expenses", "Expense Claims", Icons.Filled.Receipt),
    DrawerItem("payslips", "Payroll", Icons.Filled.CreditCard),
    DrawerItem("calendar", "Calendar", Icons.Filled.CalendarMonth),
    DrawerItem("timesheet", "Timesheet", Icons.Filled.AccessTime),
    DrawerItem("woods_square", "Woods Square Access", Icons.Filled.MeetingRoom),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(auth: AuthViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val currentTab = Tab.entries.firstOrNull { currentRoute?.startsWith(it.route) == true }
    // Profile renders its own back-button top bar (edit action, etc.) — the shared one is
    // skipped there to avoid a double top bar, even though it's also a bottom tab.
    val showSharedTopBar = currentTab != null && currentTab != Tab.PROFILE

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
                        title = { Text(currentTab!!.title, color = Color.White, fontWeight = FontWeight.SemiBold) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
                            }
                        },
                        actions = {
                            TopBarActions(
                                initials = auth.user?.initials ?: "?",
                                unreadCount = auth.unreadNotifications,
                                onBellClick = { nav.navigate("notifications") },
                                onProfileClick = { switchTab(nav, "profile") },
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Brand.Background, titleContentColor = Color.White),
                    )
                }
            },
            bottomBar = {
                if (currentTab != null) {
                    NavigationBar(containerColor = Brand.Background) {
                        Tab.entries.forEach { tab ->
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
                composable(Tab.LEAVE.route) { LeaveScreen() }
                composable(Tab.EXPENSES.route) { ExpensesScreen() }
                composable(Tab.PAYROLL.route) { PayslipsScreen(nav) }
                composable(Tab.PROFILE.route) { ProfileScreen(nav, auth) }

                // Drawer-only destinations (own back-button top bar via BrandScaffold)
                composable("calendar") { CalendarScreen(nav) }
                composable("timesheet") { TimesheetScreen(nav) }
                composable("woods_square") { WoodsSquareScreen(nav) }
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
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Brand.Primary), contentAlignment = Alignment.Center) {
                    Text("TI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Text(
                    "Tertiary Infotech Acadmey", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            DRAWER_ITEMS.forEach { item ->
                val selected = currentRoute?.startsWith(item.route) == true
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
                        item.label, color = if (selected) Brand.Primary else Color.White, fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InitialsAvatar(auth.user?.initials ?: "?", 36)
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(auth.user?.displayName ?: "", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
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
