package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.CalendarEvent
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.BrandScaffold
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class CalendarViewMode { GRID, LIST }

/**
 * Merges `/api/mobile/calendar`'s events with the canonical Singapore public-holiday list from
 * `/api/public-holidays` (already used by the Apply Leave screen), replacing whatever the
 * `CalendarEvent` table has typed as `HOLIDAY` — that bucket is editable by anyone through the
 * web's admin "Add Event" screen, so it can (and did, in production) end up with test/garbage
 * entries alongside real holidays with no way to tell them apart. `/api/public-holidays` is backed
 * by a separate, curated `PublicHoliday` table + a hardcoded SG holiday list fallback, so it's the
 * one reliable "real holidays only" source. Fetches the years actually spanned by the event list
 * plus last/this/next year, so month-grid browsing near the current date always has holiday pills.
 */
suspend fun fetchCalendarData(): List<CalendarEvent> {
    val resp = HrmsApi.calendar()
    val nonHoliday = resp.events.filter { it.type != "HOLIDAY" }

    val thisYear = Calendar.getInstance().get(Calendar.YEAR)
    val eventYears = resp.events.mapNotNull { e -> Fmt.parse(e.startDate) }
        .map { d -> Calendar.getInstance().apply { time = d }.get(Calendar.YEAR) }
    val years = (eventYears + listOf(thisYear - 1, thisYear, thisYear + 1)).toSet()

    val holidayEvents = years.flatMap { year ->
        try {
            HrmsApi.publicHolidays(year).holidays.map { h ->
                CalendarEvent(id = "ph_${h.date}", title = h.name, startDate = h.date, endDate = h.date, allDay = true, type = "HOLIDAY")
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    return nonHoliday + holidayEvents
}

/** Calendar — public holidays, your events, and approved leave. A Month grid view (default,
 *  mirrors the reference design) and the original List view (grouped by month, "with all the
 *  events") are both available via a toggle; both read the same already-fetched event list. A
 *  real destination sharing the app's hamburger top bar (not its own back-button scaffold). */
@Composable
fun CalendarScreen(nav: NavController) {
    var viewMode by remember { mutableStateOf(CalendarViewMode.GRID) }
    var gridMonth by remember { mutableStateOf(Calendar.getInstance()) }

    Column(modifier = Modifier.fillMaxSize()) {
        AsyncListScreen(fetch = { fetchCalendarData() }) { data ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Calendar", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Text("Events, holidays & leave schedule", color = Brand.TextSecondary, fontSize = 14.sp)
                }
            }
                item { Spacer18() }
                item {
                    PremierButton(title = "Add Event", icon = Icons.Filled.AddCircle) {
                        nav.navigate("calendar_new/none")
                    }
                }
                item { Spacer14() }
                item { LegendCard() }
                item { Spacer14() }
                item {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = viewMode == CalendarViewMode.GRID,
                            onClick = { viewMode = CalendarViewMode.GRID },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CalendarViewMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Month", modifier = Modifier.padding(start = 6.dp))
                            }
                        }
                        SegmentedButton(
                            selected = viewMode == CalendarViewMode.LIST,
                            onClick = { viewMode = CalendarViewMode.LIST },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.ViewList, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("List", modifier = Modifier.padding(start = 6.dp))
                            }
                        }
                    }
                }
                item { Spacer18() }

                if (viewMode == CalendarViewMode.GRID) {
                    item {
                        MonthGrid(
                            month = gridMonth,
                            events = data,
                            onPrev = { gridMonth = (gridMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) } },
                            onNext = { gridMonth = (gridMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) } },
                            onToday = { gridMonth = Calendar.getInstance() },
                            onDayClick = { dateIso -> nav.navigate("calendar_day/$dateIso") },
                        )
                    }
                } else {
                    if (data.isEmpty()) {
                        item { EmptyHint(Icons.Filled.CalendarMonth, "No upcoming events.") }
                    }
                    grouped(data).forEach { (month, events) ->
                        item {
                            Text(month, color = Brand.TextPrimary,
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
private fun LegendCard() {
    Card {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LegendItem(Brand.Red, "Holiday")
            LegendItem(Brand.Blue, "Meeting")
            LegendItem(Brand.Purple, "Training")
        }
        Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            LegendItem(Brand.Green, "Company Event")
            LegendItem(Brand.Amber, "Leave")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, color = Brand.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

/** A month calendar grid — each day cell shows a colored pill (holiday name, or the event's
 *  title) for the first event that day, plus a "+N" indicator when more than one falls on the
 *  same date. Tapping a day opens [onDayClick] with its `yyyy-MM-dd` key. */
@Composable
private fun MonthGrid(
    month: Calendar,
    events: List<CalendarEvent>,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onDayClick: (String) -> Unit,
) {
    val monthFmt = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dayKeyFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val eventsByDay = remember(events) { eventsByDay(events) }

    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(monthFmt.format(month.time), color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBoxButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, onPrev)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brand.Border)
                        .clickable(onClick = onToday)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) { Text("Today", color = Brand.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                IconBoxButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, onNext)
            }
        }
        Spacer18()
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                Text(d, color = Brand.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer12()

        val today = Calendar.getInstance()
        val isCurrentMonth = today.get(Calendar.YEAR) == month.get(Calendar.YEAR) && today.get(Calendar.MONTH) == month.get(Calendar.MONTH)
        val firstOfMonth = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val leadingBlanks = firstOfMonth.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)
        val rows = (leadingBlanks + daysInMonth + 6) / 7

        var dayCounter = 1
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    Box(modifier = Modifier.weight(1f).heightIn(min = 64.dp).padding(2.dp), contentAlignment = Alignment.TopCenter) {
                        if (cellIndex >= leadingBlanks && dayCounter <= daysInMonth) {
                            val d = dayCounter
                            val cellDate = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, d) }
                            val dateKey = dayKeyFmt.format(cellDate.time)
                            val isToday = isCurrentMonth && d == today.get(Calendar.DAY_OF_MONTH)
                            val dayEvents = eventsByDay[dateKey].orEmpty()

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.fillMaxWidth().clickable { onDayClick(dateKey) },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .let { if (isToday) it.clip(CircleShape).background(Brand.Primary) else it },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "$d", color = if (isToday) Color.White else Brand.TextPrimary,
                                        fontSize = 14.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                                dayEvents.firstOrNull()?.let { e ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(tintFor(e.type))
                                            .padding(horizontal = 4.dp, vertical = 1.dp),
                                    ) {
                                        Text(
                                            e.title, color = Color.White, fontSize = 9.sp, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    if (dayEvents.size > 1) {
                                        Text("+${dayEvents.size - 1}", color = Brand.TextMuted, fontSize = 9.sp)
                                    }
                                }
                            }
                            dayCounter++
                        }
                    }
                }
            }
        }
    }
}

/** Expands each event across every day between its start/end date (inclusive) so multi-day
 *  events/leave show on every date they span, keyed by `yyyy-MM-dd`. */
private fun eventsByDay(events: List<CalendarEvent>): Map<String, List<CalendarEvent>> {
    val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val map = mutableMapOf<String, MutableList<CalendarEvent>>()
    events.forEach { e ->
        val start = Fmt.parse(e.startDate) ?: return@forEach
        val end = Fmt.parse(e.endDate) ?: start
        val cursor = Calendar.getInstance().apply { time = start }
        val endCal = Calendar.getInstance().apply { time = end }
        var guard = 0
        while (!cursor.after(endCal) && guard < 366) {
            map.getOrPut(dayFmt.format(cursor.time)) { mutableListOf() }.add(e)
            cursor.add(Calendar.DAY_OF_MONTH, 1)
            guard++
        }
    }
    return map
}

@Composable
private fun IconBoxButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Brand.Border).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = Brand.TextPrimary, modifier = Modifier.size(18.dp)) }
}

@Composable
private fun EventRow(e: CalendarEvent) {
    val tint = tintFor(e.type)
    Card {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.width(46.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(dayNum(e.startDate), color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(weekday(e.startDate), color = Brand.TextSecondary, fontSize = 11.sp)
            }
            Box(modifier = Modifier.padding(horizontal = 12.dp).width(3.dp).height(36.dp)
                .clip(CircleShape).background(tint))
            Column(modifier = Modifier.weight(1f)) {
                Text(e.title, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(label(e.type), color = tint, fontSize = 11.sp)
            }
        }
    }
}

/** Mirrors the web calendar's literal event-type colors (`calendar/page.tsx:73-80`). Not private —
 *  [CalendarDayScreen] reuses this same mapping. */
fun tintFor(type: String): Color = when (type) {
    "HOLIDAY" -> Brand.Red
    "MEETING" -> Brand.Blue
    "TRAINING" -> Brand.Purple
    "COMPANY_EVENT" -> Brand.Green
    "LEAVE" -> Brand.Amber
    else -> Brand.Blue
}

fun label(type: String): String =
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
