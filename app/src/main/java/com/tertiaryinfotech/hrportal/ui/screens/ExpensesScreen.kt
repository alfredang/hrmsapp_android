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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.hrportal.data.ExpenseCategory
import com.tertiaryinfotech.hrportal.data.ExpenseClaim
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.Net
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.StatusPill
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.ui.theme.Spacing
import com.tertiaryinfotech.hrportal.util.Fmt
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val STATUS_FILTERS = listOf("All", "Pending", "Approved", "Rejected", "Cancelled", "Paid")

/** Expense claims tab — personal claims from /api/mobile/expenses, with client-side search/date-
 *  range/status filtering (the mobile API takes no query params, so all three run locally over
 *  the already-fetched claim list), plus submitting a new one (mirrors the web's
 *  `expense-submit-form.tsx` → `POST /api/expenses`). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen() {
    var showAdd by remember { mutableStateOf(false) }
    var categories by remember { mutableStateOf<List<ExpenseCategory>>(emptyList()) }
    var reloadKey by remember { mutableStateOf(0) }

    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") }
    var dateFromMillis by remember { mutableStateOf<Long?>(null) }
    var dateToMillis by remember { mutableStateOf<Long?>(null) }
    var showDateFrom by remember { mutableStateOf(false) }
    var showDateTo by remember { mutableStateOf(false) }
    val dayFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    Column(modifier = Modifier.fillMaxSize()) {
        key(reloadKey) {
            AsyncListScreen(fetch = {
                val data = HrmsApi.expenses()
                categories = data.categories
                data
            }) { data ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Expense Claims", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("Submit and manage expenses", color = Brand.TextSecondary, fontSize = 14.sp)
                    }
                }
                item { Spacer18() }
                item {
                    PremierButton(title = "New Expense", icon = Icons.Filled.AddCircle) {
                        categories = data.categories
                        showAdd = true
                    }
                }
                item { Spacer14() }
                item {
                    TotalExpensesCard(
                        total = data.claims.sumOf { it.amount },
                        count = data.claims.size,
                    )
                }
                item { Spacer14() }
                item {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search expenses…", color = Brand.TextMuted) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Brand.TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Brand.Border,
                            unfocusedContainerColor = Brand.Border,
                            focusedTextColor = Brand.TextPrimary,
                            unfocusedTextColor = Brand.TextPrimary,
                            focusedIndicatorColor = Brand.Primary,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                    )
                }
                item { Spacer12() }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DateFilterField(
                            millis = dateFromMillis, modifier = Modifier.weight(1f),
                            onClick = { showDateFrom = true },
                        )
                        Text("to", color = Brand.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp))
                        DateFilterField(
                            millis = dateToMillis, modifier = Modifier.weight(1f),
                            onClick = { showDateTo = true },
                        )
                    }
                }
                if (showDateFrom) {
                    item {
                        DateField(
                            initial = dateFromMillis ?: System.currentTimeMillis(),
                            onPick = { dateFromMillis = it; showDateFrom = false },
                            onCancel = { showDateFrom = false },
                        )
                    }
                }
                if (showDateTo) {
                    item {
                        DateField(
                            initial = dateToMillis ?: System.currentTimeMillis(),
                            onPick = { dateToMillis = it; showDateTo = false },
                            onCancel = { showDateTo = false },
                        )
                    }
                }
                item { Spacer12() }
                item {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        FieldBox(modifier = Modifier.menuAnchor()) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(statusFilter, color = Brand.TextPrimary, modifier = Modifier.weight(1f))
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Brand.TextPrimary)
                            }
                        }
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            STATUS_FILTERS.forEach { s ->
                                DropdownMenuItem(text = { Text(s) }, onClick = { statusFilter = s; expanded = false })
                            }
                        }
                    }
                }
                item { Spacer18() }

                val filtered = data.claims.filter { c ->
                    val matchesSearch = query.isBlank() ||
                        c.description.contains(query, ignoreCase = true) ||
                        (c.category?.contains(query, ignoreCase = true) == true)
                    val matchesStatus = statusFilter == "All" || c.status.equals(statusFilter, ignoreCase = true)
                    val claimDay = Fmt.parse(c.expenseDate)?.let { dayFmt.format(it) }
                    val matchesFrom = dateFromMillis == null ||
                        (claimDay != null && claimDay >= dayFmt.format(Date(dateFromMillis!!)))
                    val matchesTo = dateToMillis == null ||
                        (claimDay != null && claimDay <= dayFmt.format(Date(dateToMillis!!)))
                    matchesSearch && matchesStatus && matchesFrom && matchesTo
                }

                if (filtered.isEmpty()) {
                    item { EmptyHint(Icons.Filled.CreditCard, "No expense claims match your filters.") }
                } else {
                    items(filtered) { c ->
                        ClaimRow(c, onAcknowledged = { reloadKey++ })
                        Spacer12()
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddExpenseSheet(
            categories = categories,
            onDismiss = { showAdd = false },
            onSubmitted = { showAdd = false; reloadKey++ },
        )
    }
}

/** Mirrors the reference's KPI-style summary card: label / big value / claim count, with a muted
 *  dark icon tile (same treatment as the dashboard's Recent Activity tiles). */
@Composable
private fun TotalExpensesCard(total: Double, count: Int) {
    Card {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Total Expenses", color = Brand.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(Fmt.money(total), color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.padding(top = 2.dp))
                Text("$count claim${if (count == 1) "" else "s"}", color = Brand.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Brand.BorderLight.copy(alpha = 0.35f)).padding(10.dp)) {
                Icon(Icons.Filled.AttachMoney, contentDescription = null, tint = Brand.TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** A date-range endpoint field — shows "DD/MM/YYYY" placeholder until picked. */
@Composable
private fun DateFilterField(millis: Long?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val df = remember { SimpleDateFormat("dd/MM/yyyy", Locale.US) }
    FieldBox(modifier = modifier.clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                millis?.let { df.format(Date(it)) } ?: "DD/MM/YYYY",
                color = if (millis != null) Brand.TextPrimary else Brand.TextMuted,
                fontSize = 13.sp, modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = Brand.TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ClaimRow(c: ExpenseClaim, onAcknowledged: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirming by remember(c.id) { mutableStateOf(false) }
    var acknowledging by remember(c.id) { mutableStateOf(false) }

    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(c.description, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, modifier = Modifier.weight(1f))
            StatusPill(c.status)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(c.category ?: "—", color = Brand.TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
            if (!c.receiptUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, Brand.Blue, RoundedCornerShape(6.dp))
                        .clickable {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Net.url(c.receiptUrl!!))))
                            } catch (_: ActivityNotFoundException) { /* no app to view it — ignore */ }
                        }
                        .padding(5.dp),
                ) {
                    Icon(Icons.Filled.Visibility, contentDescription = "View receipt", tint = Brand.Blue, modifier = Modifier.size(14.dp))
                }
            }
        }
        if (!c.rejectionReason.isNullOrEmpty()) {
            Text(c.rejectionReason, color = Brand.Red.copy(alpha = 0.9f), fontSize = 11.sp, maxLines = 2, modifier = Modifier.padding(top = 4.dp))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(Fmt.money(c.amount, c.currency), color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = Brand.TextMuted, modifier = Modifier.size(12.dp))
            Text(Fmt.date(c.expenseDate), color = Brand.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
        }

        // Staff self-acknowledge that a reimbursement was received — mirrors the web's
        // `expense-list.tsx` two-step confirm, calling the same POST /api/expenses/{id}/acknowledge
        // route (no admin/finance role required — only the claim's own employee).
        if (c.status.equals("APPROVED", ignoreCase = true)) {
            if (confirming) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Confirm payment received?", color = Brand.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PremierButton(
                            title = "Yes, Received", fullWidth = false, loading = acknowledging,
                            onClick = {
                                acknowledging = true
                                scope.launch {
                                    try {
                                        HrmsApi.acknowledgeExpense(c.id)
                                        onAcknowledged()
                                    } catch (_: Exception) {
                                        acknowledging = false
                                        confirming = false
                                    }
                                }
                            },
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Brand.Corner.dp))
                                .border(1.dp, Brand.Border, RoundedCornerShape(Brand.Corner.dp))
                                .clickable(enabled = !acknowledging) { confirming = false }
                                .padding(horizontal = Spacing.lg, vertical = 10.dp),
                        ) {
                            Text("No", color = Brand.TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(Brand.Corner.dp))
                        .border(1.dp, Brand.Green, RoundedCornerShape(Brand.Corner.dp))
                        .clickable { confirming = true }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Brand.Green, modifier = Modifier.size(16.dp))
                        Text("Payment Received", color = Brand.Green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
