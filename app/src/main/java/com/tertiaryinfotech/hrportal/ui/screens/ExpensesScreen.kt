package com.tertiaryinfotech.hrportal.ui.screens

import com.tertiaryinfotech.hrportal.ui.components.StatTile
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
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

/** Expense claims (iOS `ExpensesView`). The optional filter panel is an Android extra: the mobile API
 *  takes no query params, so search, date range and status all run over the fetched list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(nav: NavController) {
    var reloadKey by remember { mutableStateOf(0) }
    var showFilters by remember { mutableStateOf(false) }

    // NewClaimScreen sets this before popping back, which forces a refetch.
    val savedStateHandle = nav.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getStateFlow("claim_submitted", false)?.collect { submitted ->
            if (submitted) {
                reloadKey++
                savedStateHandle.set("claim_submitted", false)
            }
        }
    }

    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") }
    var dateFromMillis by remember { mutableStateOf<Long?>(null) }
    var dateToMillis by remember { mutableStateOf<Long?>(null) }
    var showDateFrom by remember { mutableStateOf(false) }
    var showDateTo by remember { mutableStateOf(false) }
    val dayFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    Column(modifier = Modifier.fillMaxSize()) {
        key(reloadKey) {
            AsyncListScreen(fetch = { HrmsApi.expenses() }) { data ->
                item {
                    PremierButton(title = "New claim \u2014 snap receipt", icon = Icons.Filled.PhotoCamera) {
                        nav.navigate("expense_new")
                    }
                }
                item { Spacer14() }
                item {
                    StatTile(
                        value = Fmt.money(data.approvedTotal), label = "Approved total",
                        icon = Icons.Filled.Verified, tint = Brand.Green,
                    )
                }
                item { Spacer14() }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { SectionTitle("Claims") }
                        if (data.claims.isNotEmpty()) {
                            Row(
                                modifier = Modifier.clickable { showFilters = !showFilters }.padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(Icons.Filled.FilterList, contentDescription = null, tint = Brand.Accent, modifier = Modifier.size(16.dp))
                                Text(if (showFilters) "Hide filters" else "Filter", color = Brand.Accent, fontSize = 13.sp)
                            }
                        }
                    }
                }
                item { Spacer12() }
                if (showFilters) {
                item {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search expenses…", color = Brand.TextMuted) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Brand.TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Brand.SmallCorner.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Brand.Field,
                            unfocusedContainerColor = Brand.Field,
                            focusedTextColor = Brand.TextPrimary,
                            unfocusedTextColor = Brand.TextPrimary,
                            focusedIndicatorColor = Brand.Accent,
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
                } // showFilters

                val filtered = data.claims.filter { c ->
                    // Filters only apply while the panel is open, so closing it shows everything again.
                    if (!showFilters) return@filter true
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
                    item {
                        EmptyHint(
                            Icons.Filled.CreditCard,
                            if (data.claims.isEmpty()) "No expense claims yet." else "No expense claims match your filters.",
                        )
                    }
                } else {
                    items(filtered) { c ->
                        ClaimRow(c, onAcknowledged = { reloadKey++ })
                        Spacer12()
                    }
                }
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
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(c.description, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
                Text(
                    listOfNotNull(c.category, c.expenseDate?.let { Fmt.date(it) }).joinToString(" \u00b7 "),
                    color = Brand.TextSecondary, fontSize = 12.sp,
                )
                if (!c.rejectionReason.isNullOrEmpty()) {
                    Text(c.rejectionReason, color = Brand.Red.copy(alpha = 0.9f), fontSize = 11.sp, maxLines = 2)
                }
                if (!c.receiptUrl.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.clickable {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Net.url(c.receiptUrl))))
                            } catch (_: ActivityNotFoundException) { /* no app to view it */ }
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Filled.Visibility, contentDescription = null, tint = Brand.Accent, modifier = Modifier.size(14.dp))
                        Text("View receipt", color = Brand.Accent, fontSize = 12.sp)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(Fmt.money(c.amount, c.currency), color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                StatusPill(c.status)
            }
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
