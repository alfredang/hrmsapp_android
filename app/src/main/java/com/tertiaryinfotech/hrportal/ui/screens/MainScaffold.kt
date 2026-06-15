package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tertiaryinfotech.hrportal.ui.AuthViewModel
import com.tertiaryinfotech.hrportal.ui.theme.Brand

/**
 * The signed-in app shell. Four tabs cover the HR modules (Accounting is intentionally
 * excluded). Each tab is its own nested navigation graph on the Premier Blue surface.
 * Mirrors iOS MainTabView.
 */
private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Filled.Home),
    LEAVE("leave", "Leave", Icons.Filled.CalendarMonth),
    TEAM("team", "Team", Icons.Filled.Groups),
    MORE("more", "More", Icons.Filled.MoreHoriz),
}

@Composable
fun MainScaffold(auth: AuthViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0A1A38)) {
                Tab.entries.forEach { tab ->
                    val selected = currentRoute?.startsWith(tab.route) == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Brand.Sky,
                            selectedTextColor = Brand.Sky,
                            indicatorColor = Color.White.copy(alpha = 0.10f),
                            unselectedIconColor = Color.White.copy(alpha = 0.6f),
                            unselectedTextColor = Color.White.copy(alpha = 0.6f),
                        ),
                    )
                }
            }
        },
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Tab.HOME.route,
            modifier = Modifier.padding(inner),
        ) {
            composable(Tab.HOME.route) { DashboardScreen(auth) }
            composable(Tab.LEAVE.route) { LeaveScreen() }
            composable(Tab.TEAM.route) { TeamScreen() }
            composable(Tab.MORE.route) { MoreScreen(auth, nav) }

            // "More" sub-destinations
            composable("payslips") { PayslipsScreen(nav) }
            composable("payslip_pdf") { PayslipPdfScreen(nav) }
            composable("expenses") { ExpensesScreen(nav) }
            composable("calendar") { CalendarScreen(nav) }
            composable("timesheet") { TimesheetScreen(nav) }
            composable("profile") { ProfileScreen(nav) }
        }
    }
}
