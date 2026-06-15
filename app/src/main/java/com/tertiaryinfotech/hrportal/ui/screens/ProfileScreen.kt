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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.BrandScaffold
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.InitialsAvatar
import com.tertiaryinfotech.hrportal.util.Fmt

/** My profile — full employee record from /api/mobile/profile. */
@Composable
fun ProfileScreen(nav: NavController) {
    BrandScaffold(title = "My profile", onBack = { nav.popBackStack() }) { inner ->
        Column(modifier = Modifier.padding(inner)) {
            AsyncListScreen(fetch = { HrmsApi.profile() }) { data ->
                val e = data.employee
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
                            Detail("Gender", cap(e.gender))
                            Detail("Date of birth", Fmt.date(e.dateOfBirth))
                            Detail("Joined", Fmt.date(e.startDate))
                            Detail("Status", cap(e.status), last = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarHeader(e: EmployeeProfile) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InitialsAvatar(initials(e.name), 88)
        Text(e.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        if (e.roles.isNotEmpty()) {
            Text(
                e.roles.joinToString(" · "),
                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun Detail(label: String, value: String, last: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp, textAlign = TextAlign.End)
    }
    if (!last) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
    }
}

private fun cap(s: String): String = s.lowercase().replaceFirstChar { it.uppercase() }

private fun initials(name: String): String =
    name.split(" ").filter { it.isNotBlank() }.take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
