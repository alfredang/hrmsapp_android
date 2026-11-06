package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.Payslip
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.StatusPill
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import java.text.SimpleDateFormat
import java.util.Locale

/** Holds the payslip tapped on the list so the PDF screen can download/render it. */
object PayslipSelection {
    var current: Payslip? = null
}

/** Payslips (iOS `PayslipsView`): personal payslips from /api/mobile/payslips. */
@Composable
fun PayslipsScreen(nav: NavController) {
    AsyncListScreen(fetch = { HrmsApi.payslips() }) { data ->
        if (data.payslips.isEmpty()) {
            item { EmptyHint(Icons.Filled.Description, "No payslips available yet.") }
        } else {
            items(data.payslips) { p ->
                PayslipRow(p) {
                    PayslipSelection.current = p
                    nav.navigate("payslip_pdf")
                }
                Spacer12()
            }
        }
    }
}

@Composable
private fun PayslipRow(p: Payslip, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(period(p), color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("Paid ${Fmt.date(p.paymentDate)}", color = Brand.TextSecondary, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(Fmt.money(p.netSalary), color = Brand.Green, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                StatusPill(p.status)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                tint = Brand.TextPrimary.copy(alpha = 0.4f), modifier = Modifier.padding(start = 6.dp).size(20.dp),
            )
        }
    }
}

private fun period(p: Payslip): String {
    val d = Fmt.parse(p.payPeriodStart) ?: return "Payslip"
    return SimpleDateFormat("MMMM yyyy", Locale.US).format(d)
}
