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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.EmployeeProfile
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.WorkWeek
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.InitialsAvatar
import com.tertiaryinfotech.hrportal.ui.theme.Appearance
import com.tertiaryinfotech.hrportal.ui.theme.AppearanceMode
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt

/** My profile (iOS `ProfileView`): the employee record, self-edit and change password. */
@Composable
fun ProfileScreen(nav: NavController) {
    var showEdit by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    var loaded by remember { mutableStateOf<EmployeeProfile?>(null) }

    key(reloadKey) {
        AsyncListScreen(fetch = { HrmsApi.profile() }) { data ->
            val e = data.employee
            loaded = e
            if (e == null) {
                item { EmptyHint(Icons.Filled.PersonOff, "No profile linked to this account.") }
            } else {
                item { AvatarHeader(e) }
                item { Spacer18() }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionButton("Edit profile", Icons.Filled.Edit, Modifier.weight(1f)) { showEdit = true }
                        ActionButton("Password", Icons.Filled.Lock, Modifier.weight(1f)) { showPassword = true }
                    }
                }
                item { Spacer18() }
                item {
                    Card {
                        Detail("Employee ID", e.employeeId)
                        Detail("Email", e.email)
                        Detail("Phone", e.phone ?: "\u2014")
                        Detail("Position", e.position ?: "\u2014")
                        Detail("Department", e.department ?: "\u2014")
                        Detail("Employment", cap(e.employmentType), last = true)
                    }
                }
                if (WorkScheduleRows.shown(e)) {
                    item { Spacer18() }
                    item { WorkScheduleCard(e) }
                }
                item { Spacer18() }
                item {
                    Card {
                        Detail("Nationality", e.nationality)
                        Detail("Gender", cap(e.gender))
                        Detail("Date of birth", Fmt.date(e.dateOfBirth))
                        if (!e.educationLevel.isNullOrBlank()) Detail("Education", cap(e.educationLevel))
                        Detail("Joined", Fmt.date(e.startDate))
                        if (!e.endDate.isNullOrBlank()) Detail("End date", Fmt.date(e.endDate))
                        Detail("Status", cap(e.status), last = true)
                    }
                }
            }
            item { Spacer18() }
            item { AppearanceCard() }
        }
    }

    if (showEdit) {
        loaded?.let { e ->
            EditProfileSheet(
                employee = e,
                onDismiss = { showEdit = false },
                onSaved = { showEdit = false; reloadKey++ },
            )
        }
    }

    if (showPassword) {
        ChangePasswordSheet(onDismiss = { showPassword = false }, onSaved = { showPassword = false })
    }
}

@Composable
private fun AvatarHeader(e: EmployeeProfile) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.shadow(10.dp, CircleShape, spotColor = Brand.Premier, ambientColor = Brand.Premier),
        ) {
            InitialsAvatar(initials(e.name), 88, avatarUrl = e.avatarUrl)
        }
        Text(e.name, color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        if (e.roles.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Brand.SurfaceStrong)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(e.roles.joinToString(" \u00b7 "), color = Brand.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Rows of the Work Schedule card (web `work-schedule-card.tsx`). */
internal object WorkScheduleRows {
    /** Hidden on an older server that sends none of the schedule fields. */
    fun shown(e: EmployeeProfile): Boolean =
        e.officeHours != null || e.workdays != null || e.workingDays != null || e.offDays != null

    fun of(e: EmployeeProfile): List<Pair<String, String>> {
        val (working, off) = WorkWeek.of(e)
        return listOf(
            "Title" to (e.position?.takeIf { it.isNotBlank() } ?: "\u2014"),
            "Department" to (e.department?.takeIf { it.isNotBlank() } ?: "\u2014"),
            "Working hours" to (e.officeHours?.takeIf { it.isNotBlank() } ?: "9am to 6pm"),
            "Working days" to working.joinToString(", ").ifEmpty { "None" },
            "Off days" to off.joinToString(", ").ifEmpty { "None" },
        )
    }
}

/** My Profile: title, department, working hours, working days and off days at a glance (web, 20 Nov 2026). */
@Composable
private fun WorkScheduleCard(e: EmployeeProfile) {
    val rows = WorkScheduleRows.of(e)
    Card {
        Text("Work Schedule", color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Box(Modifier.padding(top = 4.dp))
        rows.forEachIndexed { i, (label, value) -> Detail(label, value, last = i == rows.lastIndex) }
    }
}

/** Light / Dark / System. Saved on this device; the whole app repaints at once. */
@Composable
private fun AppearanceCard() {
    val options = listOf(
        AppearanceMode.LIGHT to Icons.Filled.LightMode,
        AppearanceMode.DARK to Icons.Filled.DarkMode,
        AppearanceMode.SYSTEM to Icons.Filled.PhoneAndroid,
    )
    Card {
        Text("Appearance", color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Text(
            "System follows your device's light or dark setting. Your choice is saved on this device.",
            color = Brand.TextSecondary, fontSize = 13.sp, lineHeight = 18.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        Row(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (mode, icon) ->
                val on = Appearance.mode == mode
                val shape = RoundedCornerShape(Brand.SmallCorner.dp)
                val content = if (on) Color.White else Brand.TextPrimary
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(shape)
                        .background(if (on) Brand.Primary else Brand.Field)
                        .border(1.dp, if (on) Brand.Primary else Brand.TextPrimary.copy(alpha = 0.18f), shape)
                        .selectable(selected = on, role = Role.RadioButton) { Appearance.select(mode) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
                    Text(
                        mode.label, color = content, fontSize = 14.sp, maxLines = 1,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(title: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Brand.SmallCorner.dp)
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(Brand.Field)
            .border(1.dp, Brand.TextPrimary.copy(alpha = 0.18f), shape)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Brand.TextPrimary, modifier = Modifier.size(16.dp))
        Text(title, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun Detail(label: String, value: String, last: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = Brand.TextSecondary, fontSize = 15.sp)
        Text(
            value, color = Brand.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 15.sp,
            textAlign = TextAlign.End, modifier = Modifier.weight(1f),
        )
    }
    if (!last) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Brand.TextPrimary.copy(alpha = 0.08f)))
}

/** "FULL_TIME" to "Full Time". */
private fun cap(s: String): String =
    s.split('_').joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercase() } }

private fun initials(name: String): String =
    name.split(" ").filter { it.isNotBlank() }.take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
