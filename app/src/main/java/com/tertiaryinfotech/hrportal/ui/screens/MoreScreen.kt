package com.tertiaryinfotech.hrportal.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.ui.AuthViewModel
import com.tertiaryinfotech.hrportal.ui.components.BrandScaffold
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.InitialsAvatar
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.ui.theme.IconTint

/** More tab — entry points to the remaining modules + sign out. Mirrors iOS MoreView. */
@Composable
fun MoreScreen(auth: AuthViewModel, nav: NavController) {
    BrandScaffold(title = "More") { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
            Card {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    InitialsAvatar(auth.user?.initials ?: "?", 52)
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text(auth.user?.displayName ?: "Employee", color = Color.White,
                            fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(auth.user?.email ?: "", color = Brand.TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            // Module list
            Card(padding = 6) {
                MenuLink("Payslips", Icons.Filled.Description, IconTint.Green) { nav.navigate("payslips") }
                MenuDivider()
                MenuLink("Expense claims", Icons.Filled.CreditCard, IconTint.Purple) { nav.navigate("expenses") }
                MenuDivider()
                MenuLink("Calendar", Icons.Filled.CalendarMonth, IconTint.Blue) { nav.navigate("calendar") }
                MenuDivider()
                MenuLink("Timesheet", Icons.Filled.Schedule, IconTint.Emerald) { nav.navigate("timesheet") }
                MenuDivider()
                MenuLink("My profile", Icons.Filled.AccountCircle, IconTint.Amber) { nav.navigate("profile") }
                MenuDivider()
                MenuLink("Woods Square Access", Icons.Filled.MeetingRoom, IconTint.Blue) { nav.navigate("woods_square") }
            }

            SignOutButton(auth)

            Text("Tertiary HRMS · v1.0", color = Brand.TextMuted, fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun MenuLink(title: String, icon: ImageVector, tint: IconTint.Tint, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(tint.bg).padding(8.dp),
        ) {
            Icon(icon, contentDescription = null, tint = tint.icon, modifier = Modifier.size(20.dp))
        }
        Text(title, color = Color.White, modifier = Modifier.padding(start = 14.dp).weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            tint = Brand.TextMuted)
    }
}

@Composable
private fun MenuDivider() {
    Box(modifier = Modifier.fillMaxWidth().padding(start = 52.dp).height(1.dp).background(Brand.Border))
}

@Composable
private fun SignOutButton(auth: AuthViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Brand.ControlHeight.dp)
            .clip(RoundedCornerShape(Brand.Corner.dp))
            .background(Brand.Surface)
            .border(1.dp, Brand.Border, RoundedCornerShape(Brand.Corner.dp))
            .clickable { auth.signOut() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Brand.Red)
        Text("Sign out", color = Brand.Red, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
    }
}
