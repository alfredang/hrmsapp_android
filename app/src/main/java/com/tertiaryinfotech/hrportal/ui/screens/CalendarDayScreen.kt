package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.CalendarEvent
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import java.text.SimpleDateFormat
import java.util.Locale

/** Day detail — reached by tapping a date in the Calendar grid. Splits that day's events (already
 *  fetched by /api/mobile/calendar, same source as the grid/list views) into "Events" (everything
 *  but LEAVE) and "My Leaves" (LEAVE-type entries, i.e. this employee's own approved leave). */
@Composable
fun CalendarDayScreen(nav: NavController, dateIso: String) {
    val headingFmt = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()) }
    val displayDate = remember(dateIso) { Fmt.parse(dateIso) }

    Column(modifier = Modifier.fillMaxSize()) {
        AsyncListScreen(fetch = { fetchCalendarData() }) { data ->
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { nav.popBackStack() },
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Brand.TextSecondary, modifier = Modifier.size(18.dp))
                    Text("Back to Calendar", color = Brand.TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }
            item { Spacer18() }

            val dayEvents = data.filter { e -> fallsOn(e, dateIso) }
            val nonLeave = dayEvents.filter { it.type != "LEAVE" }
            val leave = dayEvents.filter { it.type == "LEAVE" }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        displayDate?.let { headingFmt.format(it) } ?: dateIso,
                        color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                    )
                    Text(
                        if (dayEvents.isEmpty()) "Nothing scheduled" else "${dayEvents.size} item${if (dayEvents.size == 1) "" else "s"} scheduled",
                        color = Brand.TextSecondary, fontSize = 14.sp,
                    )
                }
            }
            item { Spacer18() }
            item {
                PremierButton(title = "Add Event", icon = Icons.Filled.AddCircle) {
                    nav.navigate("calendar_new/$dateIso")
                }
            }
            item { Spacer18() }
            item {
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Brand.Primary, modifier = Modifier.size(18.dp))
                        Text("Events", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                    if (nonLeave.isEmpty()) {
                        Text(
                            "No events scheduled", color = Brand.TextMuted, fontSize = 13.sp,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        )
                    } else {
                        Column(modifier = Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            nonLeave.forEach { e -> DayEventRow(e) }
                        }
                    }
                }
            }
            item { Spacer18() }
            item {
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Group, contentDescription = null, tint = Brand.Amber, modifier = Modifier.size(18.dp))
                        Text("My Leaves", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                    if (leave.isEmpty()) {
                        Text(
                            "You have no leave on this day", color = Brand.TextMuted, fontSize = 13.sp,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        )
                    } else {
                        Column(modifier = Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            leave.forEach { e -> DayEventRow(e) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayEventRow(e: CalendarEvent) {
    val tint = tintFor(e.type)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(3.dp).height(32.dp).clip(CircleShape).background(tint))
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(e.title, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(label(e.type), color = tint, fontSize = 11.sp)
        }
    }
}

/** Whether [dateIso] ("yyyy-MM-dd") falls within [e]'s start/end range, inclusive. */
private fun fallsOn(e: CalendarEvent, dateIso: String): Boolean {
    val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val start = Fmt.parse(e.startDate) ?: return false
    val end = Fmt.parse(e.endDate) ?: start
    val startKey = dayFmt.format(start)
    val endKey = dayFmt.format(end)
    return dateIso in startKey..endKey
}
