package com.tertiaryinfotech.hrportal.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.BuildConfig
import com.tertiaryinfotech.hrportal.ui.AuthViewModel
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.InitialsAvatar
import com.tertiaryinfotech.hrportal.ui.theme.Brand

/** More tab: entry points to the remaining modules plus sign out (iOS `MoreView`). */
@Composable
fun MoreScreen(auth: AuthViewModel, nav: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(auth.user?.initials ?: "?", 52)
                Column(modifier = Modifier.padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(auth.user?.displayName ?: "Employee", color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(auth.user?.email ?: "", color = Brand.TextSecondary, fontSize = 12.sp)
                }
            }
        }

        Card(padding = 6) {
            MenuLink("Payslips", Icons.Filled.Description, Brand.Green) { nav.navigate("payslips") }
            MenuDivider()
            MenuLink("Expense claims", Icons.Filled.CreditCard, Brand.Orange) { nav.navigate("expenses") }
            MenuDivider()
            MenuLink("Calendar", Icons.Filled.CalendarMonth, Brand.Accent) { nav.navigate("calendar_list") }
            MenuDivider()
            MenuLink("Clock In / Out", Icons.Filled.AccessTimeFilled, Brand.Amber) { nav.navigate("timesheet") }
            MenuDivider()
            if (isApprover(auth)) {
                MenuLink("Intern Attendance", Icons.Filled.Badge, Brand.Teal) { nav.navigate("intern_attendance") }
                MenuDivider()
                MenuLink("Leave & MC Summary", Icons.Filled.MedicalServices, Brand.Pink) { nav.navigate("mc_leave_summary") }
                MenuDivider()
            }
            MenuLink("Timesheet", Icons.Filled.TableChart, Brand.Mint) { nav.navigate("timesheet_week") }
            MenuDivider()
            MenuLink("Time Off", Icons.Filled.HourglassBottom, Brand.Purple) { nav.navigate("time_off") }
            MenuDivider()
            MenuLink("My profile", Icons.Filled.AccountCircle, Brand.Azure) { nav.navigate("profile") }
        }

        SignOutButton(auth)

        Text(
            "TIA HRMS · v${BuildConfig.VERSION_NAME}",
            color = Brand.TextPrimary.copy(alpha = 0.5f), fontSize = 11.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
        LegalLinks()
    }
}

/** The company privacy and terms pages, which every Play release must link from inside the app. */
@Composable
private fun LegalLinks() {
    val context = LocalContext.current
    fun open(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) { /* no browser installed */ }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Privacy", color = Brand.Accent, fontSize = 12.sp,
            modifier = Modifier.clickable { open("https://www.tertiaryinfotech.com/privacy") }.padding(vertical = 6.dp),
        )
        Text(
            "Terms", color = Brand.Accent, fontSize = 12.sp,
            modifier = Modifier.clickable { open("https://www.tertiaryinfotech.com/terms") }.padding(vertical = 6.dp),
        )
    }
}

/** Approver roles (mirrors the web's `hasAdminAccess`) see the intern attendance and leave & MC roll-ups. */
fun isApprover(auth: AuthViewModel): Boolean =
    auth.user?.primaryRole?.uppercase() in setOf("ADMIN", "HR", "MANAGER")

@Composable
private fun MenuLink(title: String, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Brand.SmallCorner.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(title, color = Brand.TextPrimary, fontSize = 16.sp, modifier = Modifier.padding(start = 14.dp).weight(1f))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            tint = Brand.TextPrimary.copy(alpha = 0.4f), modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun MenuDivider() {
    Box(modifier = Modifier.fillMaxWidth().padding(start = 52.dp).height(1.dp).background(Brand.TextPrimary.copy(alpha = 0.08f)))
}

@Composable
private fun SignOutButton(auth: AuthViewModel) {
    val shape = RoundedCornerShape(Brand.Corner.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Brand.ControlHeight.dp)
            .clip(shape)
            .background(Brand.Field)
            .border(1.dp, Brand.TextPrimary.copy(alpha = 0.2f), shape)
            .clickable { auth.signOut() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Brand.TextPrimary)
        Text("Sign out", color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.padding(start = 8.dp))
    }
}
