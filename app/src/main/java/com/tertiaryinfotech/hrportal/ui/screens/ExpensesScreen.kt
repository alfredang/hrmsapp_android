package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.ExpenseClaim
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.BrandScaffold
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.StatTile
import com.tertiaryinfotech.hrportal.ui.components.StatusPill
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt

/** Expense claims — personal claims from /api/mobile/expenses. */
@Composable
fun ExpensesScreen(nav: NavController) {
    BrandScaffold(title = "Expenses", onBack = { nav.popBackStack() }) { inner ->
        Column(modifier = Modifier.padding(inner)) {
            AsyncListScreen(fetch = { HrmsApi.expenses() }) { data ->
                item {
                    StatTile(value = Fmt.money(data.approvedTotal), label = "Approved total",
                        icon = Icons.Filled.Verified, tint = Brand.Green)
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
}

@Composable
private fun ClaimRow(c: ExpenseClaim) {
    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(c.description, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
                Text(
                    listOfNotNull(c.category, Fmt.date(c.expenseDate)).joinToString(" · "),
                    color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp,
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
