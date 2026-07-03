package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.CalendarEvent
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.BrandScaffold
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import java.text.SimpleDateFormat
import java.util.Locale

/** Calendar — public holidays, your events, and approved leave, grouped by month. Reached from
 *  the drawer, so it keeps its own back-button top bar. */
@Composable
fun CalendarScreen(nav: NavController) {
    BrandScaffold(title = "Calendar", onBack = { nav.popBackStack() }) { inner ->
        Column(modifier = Modifier.padding(inner).fillMaxSize()) {
            AsyncListScreen(fetch = { HrmsApi.calendar() }) { data ->
                if (data.events.isEmpty()) {
                    item { EmptyHint(Icons.Filled.CalendarMonth, "No upcoming events.") }
                }
                grouped(data.events).forEach { (month, events) ->
                    item {
                        Text(month, color = Color.White,
                            fontWeight = FontWeight.Bold, fontSize = 17.sp,
                            modifier = Modifier.padding(bottom = 10.dp, top = 8.dp))
                    }
                    items(events) { e ->
                        EventRow(e)
                        Spacer12()
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRow(e: CalendarEvent) {
    val tint = tintFor(e.type)
    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.width(46.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(dayNum(e.startDate), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(weekday(e.startDate), color = Brand.TextSecondary, fontSize = 11.sp)
            }
            Box(modifier = Modifier.padding(horizontal = 12.dp).width(3.dp).height(36.dp)
                .clip(CircleShape).background(tint))
            Column(modifier = Modifier.weight(1f)) {
                Text(e.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(label(e.type), color = tint, fontSize = 11.sp)
            }
        }
    }
}

/** Mirrors the web calendar's literal event-type colors (`calendar/page.tsx:73-80`). */
private fun tintFor(type: String): Color = when (type) {
    "HOLIDAY" -> Brand.Red
    "MEETING" -> Brand.Blue
    "TRAINING" -> Brand.Purple
    "COMPANY_EVENT" -> Brand.Green
    "LEAVE" -> Brand.Amber
    else -> Brand.Blue
}

private fun label(type: String): String =
    type.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }

private fun grouped(events: List<CalendarEvent>): List<Pair<String, List<CalendarEvent>>> {
    val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    return events
        .groupBy { e -> Fmt.parse(e.startDate)?.let { monthFmt.format(it) } ?: "Other" }
        .toList()
        .sortedBy { it.second.firstOrNull()?.startDate ?: "" }
}

private fun dayNum(iso: String?): String {
    val d = Fmt.parse(iso) ?: return "•"
    return SimpleDateFormat("d", Locale.getDefault()).format(d)
}

private fun weekday(iso: String?): String {
    val d = Fmt.parse(iso) ?: return ""
    return SimpleDateFormat("EEE", Locale.getDefault()).format(d)
}
