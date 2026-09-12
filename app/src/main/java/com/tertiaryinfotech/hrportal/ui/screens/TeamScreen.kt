package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.hrportal.data.Employee
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.InitialsAvatar
import com.tertiaryinfotech.hrportal.ui.theme.Brand

/**
 * Team tab — the company directory with a name/role search.
 *
 * A bottom-tab destination, so it renders bare: MainScaffold supplies the shared top bar and
 * bottom nav (a BrandScaffold here would stack a second, back-arrow app bar on top).
 */
@Composable
fun TeamScreen() {
    var query by remember { mutableStateOf("") }

    Column {
        AsyncListScreen(fetch = { HrmsApi.employees() }) { data ->
            item {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search name or role", color = Brand.TextMuted) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Brand.TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Brand.Border,
                        unfocusedContainerColor = Brand.Border,
                        focusedTextColor = Brand.TextPrimary,
                        unfocusedTextColor = Brand.TextPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
            }

            val filtered = filterEmployees(data.employees, query)
            if (filtered.isEmpty()) {
                item { EmptyHint(Icons.Filled.Group, "No colleagues found.") }
            } else {
                items(filtered) { e ->
                    EmployeeRow(e, data.isAdmin)
                    Spacer12()
                }
            }
        }
    }
}

private fun filterEmployees(list: List<Employee>, query: String): List<Employee> {
    if (query.isBlank()) return list
    val q = query.lowercase()
    return list.filter {
        it.name.lowercase().contains(q) ||
            (it.position ?: "").lowercase().contains(q) ||
            (it.department ?: "").lowercase().contains(q)
    }
}

@Composable
private fun EmployeeRow(e: Employee, isAdmin: Boolean) {
    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(initials(e.name), 46, avatarUrl = e.avatarUrl)
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text(e.name, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    listOfNotNull(e.position, e.department).joinToString(" · "),
                    color = Brand.TextSecondary, fontSize = 12.sp,
                )
                if (isAdmin && !e.email.isNullOrEmpty()) {
                    Text(e.email, color = Brand.Primary, fontSize = 11.sp)
                }
            }
        }
    }
}

private fun initials(name: String): String =
    name.split(" ").filter { it.isNotBlank() }.take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
