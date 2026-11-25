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
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.EventAvailable
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
import com.tertiaryinfotech.hrportal.data.MoreMenu
import com.tertiaryinfotech.hrportal.data.ViewAsMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import com.tertiaryinfotech.hrportal.ui.ViewAs
import com.tertiaryinfotech.hrportal.ui.AuthViewModel
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.InitialsAvatar
import com.tertiaryinfotech.hrportal.ui.theme.Brand

/**
 * More tab = the web sidebar (iOS `MoreView`, Alfred 22 Nov 2026): View As, then My Stuff, People, Processes,
 * Finance, Accounting, Documents and System for the current view ([MoreMenu]), then sign out and the legal links.
 */
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

        // View As: approvers get the menu (Admin / Staff / Intern / Accountant / Trainer Admin); Trainer Admin
        // holders who are not approvers keep the two-way control (My view / Trainer Admin).
        if (auth.availableViews.size > 1) {
            GroupLabel("View As")
            if (auth.isApprover) {
                ViewAsMenu(auth, tag = "more.viewAs")
            } else {
                SegmentedPicker(
                    options = auth.availableViews.map { it.name to it.label },
                    selected = (if (auth.isTrainerView) ViewAsMode.TRAINER else ViewAsMode.MINE).name,
                    onSelect = { ViewAs.select(ViewAsMode.valueOf(it)) },
                )
            }
        }

        // The web sidebar, section by section, for the current view (data/MoreMenu.kt).
        val context = LocalContext.current
        MoreMenu.sections(auth.shown, auth.isTrainerView, auth.canManageTrainers).forEach { section ->
            GroupLabel(section.title)
            Card(padding = 6) {
                section.rows.forEachIndexed { i, row ->
                    if (i > 0) MenuDivider()
                    val (icon, tint) = rowIcon(row.icon)
                    val tag = when (row.target) {
                        is MoreMenu.Target.Apply -> "more.apply.${row.id}"
                        else -> "more.row.${row.id}"
                    }
                    MenuLink(row.title, icon, tint, tag, external = row.target is MoreMenu.Target.Web) {
                        when (val t = row.target) {
                            is MoreMenu.Target.Route ->
                                if (t.route == "trainers" && auth.isTrainerView) {
                                    nav.navigate("trainers") {
                                        popUpTo(nav.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                } else nav.navigate(t.route)
                            is MoreMenu.Target.Apply -> nav.navigate("leave_request/${t.code}")
                            is MoreMenu.Target.Web -> openWeb(context, MoreMenu.url(t.path))
                        }
                    }
                }
            }
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

/** Approver roles (mirrors the web's `hasAdminAccess`) see the admin Timesheet and the leave & MC roll-up. */
fun isApprover(auth: AuthViewModel): Boolean = auth.isApprover

/** Icon and tint of a More row, by its [MoreMenu.Row.icon] key. */
private fun rowIcon(key: String): Pair<ImageVector, Color> = when (key) {
    "profile" -> Icons.Filled.AccountCircle to Brand.Azure
    "calendar" -> Icons.Filled.CalendarMonth to Brand.Accent
    "staff" -> Icons.Filled.Groups to Brand.Accent
    "interns" -> Icons.Filled.PersonAdd to Brand.Accent
    "trainers" -> Icons.Filled.ContactPage to Brand.Gold600
    "trainercontacts" -> Icons.Filled.School to Brand.Gold600
    "approvals" -> Icons.Filled.CheckCircle to Brand.Green
    "leave", "annual" -> Icons.AutoMirrored.Filled.EventNote to Brand.Accent
    "medical" -> Icons.Filled.MedicalServices to Brand.Pink
    "timeoff" -> Icons.Filled.HourglassBottom to Brand.Orange
    "timesheet_admin", "mytimesheet" -> Icons.Filled.TableChart to Brand.Mint
    "clock" -> Icons.Filled.AccessTimeFilled to Brand.Amber
    "offinlieu" -> Icons.Filled.EventAvailable to Brand.Mint
    "payslips" -> Icons.Filled.Description to Brand.Green
    "cpf" -> Icons.Filled.FolderShared to Brand.Accent
    "claims" -> Icons.Filled.CreditCard to Brand.Orange
    "expensetracking" -> Icons.AutoMirrored.Filled.TrendingDown to Brand.Red
    "incometracking" -> Icons.AutoMirrored.Filled.TrendingUp to Brand.Green
    "reconciliation" -> Icons.Filled.Balance to Brand.Gold600
    "statements" -> Icons.Filled.Folder to Brand.Accent
    "sop" -> Icons.AutoMirrored.Filled.MenuBook to Brand.Accent
    "settings" -> Icons.Filled.Settings to Brand.TextSecondary
    else -> Icons.Filled.Description to Brand.Accent
}

/** A web row opens the page in the browser (the HRMS web app keeps its own session there). */
private fun openWeb(context: android.content.Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) { /* no browser installed */ }
}

@Composable
private fun MenuLink(title: String, icon: ImageVector, tint: Color, tag: String, external: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Brand.SmallCorner.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 15.dp)
            .semantics { testTag = tag },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(title, color = Brand.TextPrimary, fontSize = 16.sp, modifier = Modifier.padding(start = 14.dp).weight(1f))
        Icon(
            if (external) Icons.AutoMirrored.Filled.OpenInNew else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = if (external) "Opens the web page" else null,
            tint = Brand.TextPrimary.copy(alpha = 0.4f), modifier = Modifier.size(if (external) 16.dp else 20.dp),
        )
    }
}

/** Small uppercase caption above a menu group (iOS grouped-list header). */
@Composable
private fun GroupLabel(text: String) {
    Text(
        text.uppercase(), color = Brand.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().padding(start = 6.dp, top = 6.dp),
    )
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
