package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.EmployeeProfile
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.ui.AuthViewModel
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.BrandScaffold
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.InitialsAvatar
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt

/** My profile — full employee record from /api/mobile/profile, self-editable (mirrors
 *  `employee-detail-editable.tsx`) plus a password-change card (mirrors `password-change-card.tsx`).
 *  Sign out lives here too, mirroring the web's account-menu placement under the profile avatar. */
@Composable
fun ProfileScreen(nav: NavController, auth: AuthViewModel) {
    var showEdit by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    var loaded by remember { mutableStateOf<EmployeeProfile?>(null) }

    BrandScaffold(
        title = "My profile",
        onBack = { nav.popBackStack() },
        actions = {
            if (loaded != null) {
                IconButton(onClick = { showEdit = true }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit profile", tint = Color.White)
                }
            }
        },
    ) { inner ->
        Column(modifier = Modifier.padding(inner)) {
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
                            Card {
                                Detail("Employee ID", e.employeeId)
                                Detail("Email", e.email)
                                Detail("Phone", e.phone ?: "—")
                                Detail("Position", e.position ?: "—")
                                Detail("Department", e.department ?: "—")
                                Detail("Employment", cap(e.employmentType), last = true)
                            }
                        }
                        item { Spacer18() }
                        item {
                            Card {
                                Detail("Nationality", e.nationality)
                                Detail("NRIC", e.nric ?: "—")
                                Detail("Gender", cap(e.gender))
                                Detail("Education", cap(e.educationLevel ?: "—"))
                                Detail("Date of birth", Fmt.date(e.dateOfBirth))
                                Detail("Address", e.address ?: "—")
                                Detail("Joined", Fmt.date(e.startDate))
                                Detail("Status", cap(e.status), last = true)
                            }
                        }
                        item { Spacer18() }
                        item {
                            PremierButton(title = "Change password", icon = Icons.Filled.Lock) { showPassword = true }
                        }
                        item { Spacer12() }
                        item { SignOutButton(auth) }
                    }
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InitialsAvatar(initials(e.name), 88, avatarUrl = e.avatarUrl)
        Text(e.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        if (e.roles.isNotEmpty()) {
            Text(
                e.roles.joinToString(" · "),
                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Brand.Primary)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun Detail(label: String, value: String, last: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Brand.TextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp, textAlign = TextAlign.End)
    }
    if (!last) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Brand.Border))
    }
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

private fun cap(s: String): String = s.lowercase().replaceFirstChar { it.uppercase() }

private fun initials(name: String): String =
    name.split(" ").filter { it.isNotBlank() }.take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
