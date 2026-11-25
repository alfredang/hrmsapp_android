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
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
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
import com.tertiaryinfotech.hrportal.data.PayslipFigures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
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

/**
 * Payslips (iOS `PayslipsView`): personal payslips from /api/mobile/payslips, each with its pay breakdown
 * ([PayBreakdown]). Admins and accountants ([showCpf],
 * `CpfSubmissions.canView`) also get a CPF Submissions link at the top.
 */
@Composable
fun PayslipsScreen(nav: NavController, showCpf: Boolean = false) {
    AsyncListScreen(fetch = { HrmsApi.payslips() }) { data ->
        if (showCpf) {
            item { CpfSubmissionsLink { nav.navigate("cpf_submissions") } }
            item { Spacer12() }
        }
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

/** The CPF Submissions row at the top of Payslips (admins and accountants only). */
@Composable
private fun CpfSubmissionsLink(onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.semantics { testTag = "payslips.cpfSubmissions" }) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.FolderShared, contentDescription = null, tint = Brand.Accent, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("CPF Submissions", color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("The company's monthly CPF EZPay statements", color = Brand.TextSecondary, fontSize = 12.sp)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                tint = Brand.TextPrimary.copy(alpha = 0.4f), modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun PayslipRow(p: Payslip, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.semantics { testTag = "payslip.row.${p.id}" }) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(period(p), color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("Paid ${Fmt.date(p.paymentDate)}", color = Brand.TextSecondary, fontSize = 12.sp)
            }
            StatusPill(p.status)
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                tint = Brand.TextPrimary.copy(alpha = 0.4f), modifier = Modifier.padding(start = 6.dp).size(20.dp),
            )
        }
        Hairline(Modifier.padding(vertical = 10.dp))
        PayBreakdown(p)
    }
}

/**
 * The pay figures of one payslip ([PayslipFigures]): gross pay, payback add-back, No Pay Leave (interns: MC / No Pay
 * Leave) deduction, the CPF breakdown or "CPF: None" for interns, and the take-home pay, emphasised. Used on the
 * Payslips list and above the PDF.
 */
@Composable
fun PayBreakdown(p: Payslip, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PayslipFigures.lines(p).forEach { line ->
            if (line.kind == PayslipFigures.Kind.NET) Hairline(Modifier.padding(vertical = 2.dp))
            PayLineRow(line)
        }
    }
}

@Composable
private fun PayLineRow(line: PayslipFigures.Line) {
    val net = line.kind == PayslipFigures.Kind.NET
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                line.label,
                color = when (line.kind) {
                    PayslipFigures.Kind.NOTE -> Brand.TextSecondary
                    PayslipFigures.Kind.INFO -> Brand.TextSecondary
                    else -> Brand.TextPrimary
                },
                fontWeight = if (net) FontWeight.Bold else FontWeight.Normal,
                fontSize = if (net) 15.sp else 13.sp,
            )
            line.note?.let { Text(it, color = Brand.TextMuted, fontSize = 11.sp) }
        }
        val amount = line.amount ?: return@Row
        val (text, color) = when (line.kind) {
            PayslipFigures.Kind.ADD -> "+ ${Fmt.money(amount)}" to Brand.Green
            PayslipFigures.Kind.DEDUCT -> "\u2212 ${Fmt.money(amount)}" to Brand.Red
            PayslipFigures.Kind.INFO -> Fmt.money(amount) to Brand.TextSecondary
            PayslipFigures.Kind.NET -> Fmt.money(amount) to Brand.Green
            else -> Fmt.money(amount) to Brand.TextPrimary
        }
        Text(
            text, color = color, maxLines = 1,
            fontWeight = if (net) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = if (net) 17.sp else 13.sp,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Brand.TextPrimary.copy(alpha = 0.08f)))
}

private fun period(p: Payslip): String {
    val d = Fmt.parse(p.payPeriodStart) ?: return "Payslip"
    return SimpleDateFormat("MMMM yyyy", Locale.US).format(d)
}
