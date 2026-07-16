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
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.StatusPill
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.ui.theme.ThemePrefs
import com.tertiaryinfotech.hrportal.util.Fmt

/** My profile — full employee record from /api/mobile/profile, self-editable (mirrors
 *  `employee-detail-editable.tsx`) plus a password-change card (mirrors `password-change-card.tsx`)
 *  and a Dark/Light theme card. A real destination sharing the app's hamburger top bar (not its
 *  own back-button scaffold), matching the web's own page-not-modal presentation. */
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
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("My Profile", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("Your personal information", color = Brand.TextSecondary, fontSize = 14.sp)
                    }
                }
                item { Spacer18() }
                item { AvatarHeader(e) }
                item { Spacer12() }
                item {
                    PremierButton(title = "Edit Employee", icon = Icons.Filled.Edit, fullWidth = false) { showEdit = true }
                }
                item { Spacer18() }
                item {
                    Card {
                        CardHeading(Icons.Filled.Person, "Personal Information")
                        IconDetail(Icons.Filled.Email, "Email", e.email)
                        IconDetail(Icons.Filled.Phone, "Phone", e.phone ?: "—")
                        IconDetail(Icons.Filled.CalendarToday, "Date of Birth", Fmt.date(e.dateOfBirth))
                        PlainDetail("Gender", cap(e.gender))
                        PillDetail("Nationality", e.nationality, Brand.Green)
                        if (!e.educationLevel.isNullOrBlank()) {
                            PillDetail("Highest Education Level", cap(e.educationLevel), Brand.Blue)
                        }
                    }
                }
                item { Spacer18() }
                item {
                    Card {
                        CardHeading(Icons.Filled.Work, "Employment Details")
                        IconDetail(Icons.Filled.Work, "Job Function", e.department ?: "—")
                        if (e.roles.isNotEmpty()) {
                            RolesDetail("User Type", e.roles)
                        }
                        IconDetail(Icons.Filled.CalendarToday, "Start Date", Fmt.date(e.startDate))
                        if (!e.endDate.isNullOrBlank()) {
                            IconDetail(Icons.Filled.CalendarToday, "End Date", Fmt.date(e.endDate))
                        }
                        IconDetail(Icons.Filled.Work, "Employment", cap(e.employmentType))
                        StatusDetail("Status", cap(e.status), last = true)
                    }
                }
                item { Spacer18() }
                item { ThemeCard() }
                item { Spacer18() }
                item {
                    PremierButton(title = "Change password", icon = Icons.Filled.Lock) { showPassword = true }
                }
                item { Spacer18() }
                item {
                    Text(
                        "Powered by Tertiary Infotech Academy Pte Ltd",
                        color = Brand.TextFaint, fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
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
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box {
            InitialsAvatar(initials(e.name), 88, avatarUrl = e.avatarUrl)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Brand.Background)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Brand.Green),
            )
        }
        Text(e.name, color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(top = 6.dp))
        if (!e.position.isNullOrBlank()) {
            Text(e.position, color = Brand.TextSecondary, fontSize = 13.sp)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 2.dp),
        ) {
            StatusPill(cap(e.status))
            Text("ID: ${e.employeeId}", color = Brand.TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CardHeading(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Brand.Primary, modifier = Modifier.size(20.dp))
        Text(text, color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(start = 8.dp))
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Brand.Border).padding(bottom = 4.dp))
}

@Composable
private fun IconDetail(icon: ImageVector, label: String, value: String, last: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(label, color = Brand.TextSecondary, fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Icon(icon, contentDescription = null, tint = Brand.TextSecondary, modifier = Modifier.size(15.dp))
            Text(
                value, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
    if (!last) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Brand.Border))
}

@Composable
private fun PlainDetail(label: String, value: String, last: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(label, color = Brand.TextSecondary, fontSize = 12.sp)
        Text(value, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
    }
    if (!last) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Brand.Border))
}

@Composable
private fun PillDetail(label: String, value: String, color: Color, last: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(label, color = Brand.TextSecondary, fontSize = 12.sp)
        OutlinePill(value.uppercase(), color, modifier = Modifier.padding(top = 6.dp))
    }
    if (!last) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Brand.Border))
}

@Composable
private fun RolesDetail(label: String, roles: List<String>, last: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(label, color = Brand.TextSecondary, fontSize = 12.sp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            roles.forEach { role -> OutlinePill(cap(role), roleColor(role)) }
        }
    }
    if (!last) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Brand.Border))
}

@Composable
private fun StatusDetail(label: String, value: String, last: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Brand.TextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        StatusPill(value)
    }
    if (!last) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Brand.Border))
}

@Composable
private fun OutlinePill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, color, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun roleColor(role: String): Color = when (role.uppercase()) {
    "ACCOUNTANT" -> Brand.Green
    "ADMIN" -> Brand.Purple
    "INTERN" -> Brand.Amber
    "STAFF" -> Brand.Blue
    else -> Brand.TextSecondary
}

/** Dark/Light appearance card — mirrors the web's `theme-toggle.tsx` settings card. Flips every
 *  reactive [Brand] token app-wide and persists the choice via [ThemePrefs]. */
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
                if (selected) it.background(Brand.Primary)
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

private fun cap(s: String): String = s.lowercase().replaceFirstChar { it.uppercase() }

private fun initials(name: String): String =
    name.split(" ").filter { it.isNotBlank() }.take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
