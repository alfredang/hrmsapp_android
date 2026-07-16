package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.Payslip
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import java.text.SimpleDateFormat
import java.util.Locale

private val STATUS_FILTERS = listOf("All Status" to "All Status", "Draft" to "DRAFT", "Processed" to "GENERATED", "Paid" to "PAID")
private const val PAGE_SIZE = 10

/** Holds the payslip tapped on the list so the PDF screen can download/render it. */
object PayslipSelection {
    var current: Payslip? = null
}

/** Payroll — personal payslips from /api/mobile/payslips. A real destination sharing the app's
 *  hamburger top bar (not its own back-button scaffold, which was rendering a redundant second
 *  header underneath the shared one). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayslipsScreen(nav: NavController) {
    var statusFilter by remember { mutableStateOf("All Status") }
    var page by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxWidth()) {
        AsyncListScreen(fetch = { HrmsApi.payslips() }) { data ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Payroll", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Text("Manage salary payments", color = Brand.TextSecondary, fontSize = 14.sp)
                }
            }
            item { Spacer18() }
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
                        STATUS_FILTERS.forEach { (label, _) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { statusFilter = label; expanded = false; page = 0 })
                        }
                    }
                }
            }
            item { Spacer18() }

            val statusCode = STATUS_FILTERS.first { it.first == statusFilter }.second
            val filtered = if (statusCode == "All Status") data.payslips else data.payslips.filter { it.status.equals(statusCode, ignoreCase = true) }
            val pageCount = maxOf(1, (filtered.size + PAGE_SIZE - 1) / PAGE_SIZE)
            val clampedPage = page.coerceIn(0, pageCount - 1)
            val paged = filtered.drop(clampedPage * PAGE_SIZE).take(PAGE_SIZE)

            if (filtered.isEmpty()) {
                item { EmptyHint(Icons.Filled.Description, "No payslips match this filter.") }
            } else {
                items(paged) { p ->
                    PayslipRow(p) {
                        PayslipSelection.current = p
                        nav.navigate("payslip_pdf")
                    }
                    Spacer12()
                }
                item { Spacer12() }
                item {
                    Text(
                        "Showing ${clampedPage * PAGE_SIZE + 1}–${clampedPage * PAGE_SIZE + paged.size} of ${filtered.size}",
                        color = Brand.TextMuted, fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    )
                }
                item { Spacer12() }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = { if (clampedPage > 0) page = clampedPage - 1 },
                            enabled = clampedPage > 0,
                            border = BorderStroke(1.dp, Brand.BorderLight),
                        ) { Text("Previous", color = if (clampedPage > 0) Brand.TextPrimary else Brand.TextMuted) }
                        Text("Page ${clampedPage + 1} of $pageCount", color = Brand.TextSecondary, fontSize = 13.sp)
                        OutlinedButton(
                            onClick = { if (clampedPage < pageCount - 1) page = clampedPage + 1 },
                            enabled = clampedPage < pageCount - 1,
                            border = BorderStroke(1.dp, Brand.BorderLight),
                        ) { Text("Next", color = if (clampedPage < pageCount - 1) Brand.TextPrimary else Brand.TextMuted) }
                    }
                }
            }
            item { Spacer18() }
            item {
                Text(
                    "Powered by Tertiary Infotech Academy Pte Ltd",
                    color = Brand.TextFaint, fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PayslipRow(p: Payslip, onDownload: () -> Unit) {
    Card {
        Text(period(p), color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Gross", color = Brand.TextSecondary, fontSize = 12.sp)
                Text(Fmt.money(p.grossSalary), color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Net Pay", color = Brand.TextSecondary, fontSize = 12.sp)
                Text(Fmt.money(p.netSalary), color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Text(
            "CPF (EE): ${Fmt.money(p.cpfEmployee)}", color = Brand.TextMuted, fontSize = 12.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
        OutlinedButton(
            onClick = onDownload,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            border = BorderStroke(1.dp, Brand.BorderLight),
        ) {
            Icon(Icons.Filled.Download, contentDescription = null, tint = Brand.TextPrimary, modifier = Modifier.size(16.dp))
            Text("Download Payslip", color = Brand.TextPrimary, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

private fun period(p: Payslip): String {
    val d = Fmt.parse(p.payPeriodStart) ?: return "Payslip"
    return SimpleDateFormat("MM/yyyy", Locale.US).format(d)
}
