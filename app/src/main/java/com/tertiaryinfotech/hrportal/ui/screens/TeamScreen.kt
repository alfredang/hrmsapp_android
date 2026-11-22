package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.Directory
import com.tertiaryinfotech.hrportal.data.Employee
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.FilterTabs
import com.tertiaryinfotech.hrportal.ui.components.InitialsAvatar
import com.tertiaryinfotech.hrportal.ui.theme.Brand

/**
 * Team tab — the company directory, searchable by name, position, department, email and employment type. Approvers see it as two lists,
 * Staff and Interns (when the server sends `intern`), and tap a person for their clock-in, MC and
 * leave history ([EmployeeRecordsScreen]).
 *
 * A bottom-tab destination, so it renders bare: MainScaffold supplies the shared top bar and
 * bottom nav (a BrandScaffold here would stack a second, back-arrow app bar on top).
 */
@Composable
fun TeamScreen(nav: NavController, initialGroup: String = "staff") {
    var query by remember { mutableStateOf("") }
    // Approvers: Staff or Interns (the web has Staff Management and Intern Management).
    var group by rememberSaveable { mutableStateOf(initialGroup) }

    Column {
        AsyncListScreen(fetch = { HrmsApi.employees() }) { data ->
            item {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Name, department, email or role", color = Brand.TextMuted) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Brand.TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(Brand.SmallCorner.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Brand.Field,
                        unfocusedContainerColor = Brand.Field,
                        focusedTextColor = Brand.TextPrimary,
                        unfocusedTextColor = Brand.TextPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
            }

            val matches = filterEmployees(data.employees, query)
            // Null unless the server sent the intern flag (approvers on a current server).
            val groups = Directory.split(matches)
            val showTabs = Directory.split(data.employees) != null
            if (showTabs) {
                item {
                    FilterTabs(
                        options = listOf("staff" to "Staff ${groups?.staff?.size ?: 0}", "interns" to "Interns ${groups?.interns?.size ?: 0}"),
                        selected = group,
                        onSelect = { group = it },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }
            val filtered = when {
                !showTabs -> matches
                group == "interns" -> groups?.interns.orEmpty()
                else -> groups?.staff.orEmpty()
            }
            if (filtered.isEmpty()) {
                item { EmptyHint(Icons.Filled.Group, if (!showTabs) "No colleagues found." else if (group == "interns") "No interns found." else "No staff found.") }
            } else {
                items(filtered) { e ->
                    EmployeeRow(e, data.isAdmin) { nav.navigate(RecordsRoutes.records(e.id)) }
                    Spacer12()
                }
            }
        }
    }
}

private fun filterEmployees(list: List<Employee>, query: String): List<Employee> =
    if (query.isBlank()) list else list.filter { it.matches(query) }

/** Full name (wraps), department with the employment pill, and the email for approvers (Alfred, 22 Nov 2026). */
@Composable
private fun EmployeeRow(e: Employee, isAdmin: Boolean, onOpen: () -> Unit) {
    Card(onClick = if (isAdmin) onOpen else null) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(initials(e.name), 46, avatarUrl = e.avatarUrl)
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(e.name, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        e.department?.takeIf { it.isNotBlank() } ?: "No department",
                        color = Brand.TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    e.employmentLabel?.let { type ->
                        // Part time in gold with navy text (a fixed fill, so navy in both appearances).
                        val part = e.isPartTime
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (part) Brand.Gold.copy(alpha = 0.55f) else Brand.SurfaceStrong)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                .semantics { testTag = "team.type.${e.employeeId.ifEmpty { e.id }}" },
                        ) {
                            Text(type, color = if (part) Brand.Navy else Brand.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
                if (isAdmin && !e.email.isNullOrEmpty()) {
                    Text(e.email, color = Brand.Accent, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (isAdmin) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Brand.TextMuted)
            }
        }
    }
}

private fun initials(name: String): String =
    name.split(" ").filter { it.isNotBlank() }.take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
