package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.hrportal.data.ExpenseCategory
import com.tertiaryinfotech.hrportal.data.ExpenseClaim
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.StatTile
import com.tertiaryinfotech.hrportal.ui.components.StatusPill
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.ui.theme.IconTint
import com.tertiaryinfotech.hrportal.util.Fmt

/** Expense claims tab — personal claims from /api/mobile/expenses, plus submitting a new one
 *  (mirrors the web's `expense-submit-form.tsx` → `POST /api/expenses`). */
@Composable
fun ExpensesScreen() {
    var showAdd by remember { mutableStateOf(false) }
    var categories by remember { mutableStateOf<List<ExpenseCategory>>(emptyList()) }
    var reloadKey by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        key(reloadKey) {
            AsyncListScreen(fetch = {
                val data = HrmsApi.expenses()
                categories = data.categories
                data
            }) { data ->
                item {
                    PremierButton(title = "New Expense", icon = Icons.Filled.AddCircle) {
                        categories = data.categories
                        showAdd = true
                    }
                }
                item { Spacer14() }
                item {
                    StatTile(value = Fmt.money(data.approvedTotal), label = "Approved Total",
                        icon = Icons.Filled.Verified, tint = IconTint.Green.icon, iconBg = IconTint.Green.bg)
                }
                item { Spacer14() }
                item { SectionTitle("Claims") }
                item { Spacer12() }
                if (data.claims.isEmpty()) {
                    item { EmptyHint(Icons.Filled.CreditCard, "No expense claims yet.") }
                } else {
                    items(data.claims) { c ->
                        ClaimRow(c)
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

@Composable
private fun ClaimRow(c: ExpenseClaim) {
    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(c.description, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
                Text(
                    listOfNotNull(c.category, Fmt.date(c.expenseDate)).joinToString(" · "),
                    color = Brand.TextSecondary, fontSize = 12.sp,
                )
                if (!c.rejectionReason.isNullOrEmpty()) {
                    Text(c.rejectionReason, color = Brand.Red.copy(alpha = 0.9f), fontSize = 11.sp, maxLines = 2)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Fmt.money(c.amount, c.currency), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                StatusPill(c.status)
            }
        }
    }
}
