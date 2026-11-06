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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LightMode
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.EmployeeProfile
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.InitialsAvatar
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.ui.theme.ThemePrefs
import com.tertiaryinfotech.hrportal.util.Fmt

/** My profile (iOS `ProfileView`): the employee record, self-edit and change password. The Dark /
 *  Light theme card is an Android extra. */
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
                item { Spacer18() }
                item { ThemeCard() }
            }
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
                    .background(if (Brand.isDark) Color.White.copy(alpha = 0.16f) else Brand.SurfaceStrong)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(e.roles.joinToString(" \u00b7 "), color = Brand.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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

/** Dark/Light appearance card. Flips every reactive [Brand] token app-wide and persists the choice
 *  via [ThemePrefs]. */
@Composable
private fun ThemeCard() {
    val context = LocalContext.current
    Card {
        Text("Theme", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(
            "Choose your appearance preference. Dark mode is the default.",
            color = Brand.TextSecondary, fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ThemeOption(
                icon = Icons.Filled.DarkMode, label = "Dark", selected = Brand.isDark,
                modifier = Modifier.weight(1f),
            ) { Brand.applyTheme(true); ThemePrefs.setDark(context, true) }
            ThemeOption(
                icon = Icons.Filled.LightMode, label = "Light", selected = !Brand.isDark,
                modifier = Modifier.weight(1f),
            ) { Brand.applyTheme(false); ThemePrefs.setDark(context, false) }
        }
    }
}

@Composable
private fun ThemeOption(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Brand.Corner.dp))
            .let {
                if (selected) it.background(Brand.AccentGradient)
                else it.border(1.dp, Brand.BorderLight, RoundedCornerShape(Brand.Corner.dp))
            }
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Color.White else Brand.TextSecondary, modifier = Modifier.size(18.dp))
        Text(
            label, color = if (selected) Color.White else Brand.TextSecondary,
            fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** "FULL_TIME" to "Full Time". */
private fun cap(s: String): String =
    s.split('_').joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercase() } }

private fun initials(name: String): String =
    name.split(" ").filter { it.isNotBlank() }.take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
