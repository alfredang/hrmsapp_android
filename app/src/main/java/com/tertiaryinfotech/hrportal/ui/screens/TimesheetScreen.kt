package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.TimesheetDay
import com.tertiaryinfotech.hrportal.data.TimesheetResponse
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.BrandScaffold
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.StatTile
import com.tertiaryinfotech.hrportal.ui.components.StatusPill
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import java.util.Locale

/** Weekly timesheet — the current week from the existing /api/timesheet endpoint. */
@Composable
fun TimesheetScreen(nav: NavController) {
    BrandScaffold(title = "Timesheet", onBack = { nav.popBackStack() }) { inner ->
        Column(modifier = Modifier.padding(inner)) {
            AsyncListScreen(fetch = { HrmsApi.timesheet() }) { data ->
                item {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Week of ${Fmt.date(data.weekStart)}", color = Color.White,
                            fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                        if (data.isLocked) {
                            Icon(Icons.Filled.Lock, contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(end = 4.dp))
                            Text("Locked", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }
                item { Spacer14() }
                item {
                    StatTile(
                        value = String.format(Locale.US, "%.1f h", total(data)),
                        label = "Total hours this week", icon = Icons.Filled.Functions, tint = Brand.Mint,
                    )
                }
                item { Spacer14() }
                items(data.days) { d ->
                    DayRow(d)
                    Spacer12()
                }
            }
        }
    }
}

@Composable
private fun DayRow(d: TimesheetDay) {
    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${d.dayName} · ${Fmt.date(d.date, short = true)}",
                    color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                if (d.isPublicHoliday && !d.phName.isNullOrEmpty()) {
                    Text(d.phName, color = Brand.Red.copy(alpha = 0.9f), fontSize = 11.sp)
                } else if (d.isWeekend) {
                    Text("Weekend", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(String.format(Locale.US, "%.1f h", d.hours),
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (d.otCredited > 0) {
                    Text("OT +${String.format(Locale.US, "%.1f", d.otCredited)}", color = Brand.Mint, fontSize = 11.sp)
                }
                d.status?.let { StatusPill(it) }
            }
        }
    }
}

private fun total(d: TimesheetResponse): Double = d.days.sumOf { it.hours }
