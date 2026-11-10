package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.TeamCalendarResponse
import com.tertiaryinfotech.hrportal.data.TeamLeaveEntry
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Team calendar in the Planner app's layout (Alfred, 11 Nov 2026): a Month / List toggle, the month
 * title with ‹ ›, a Monday-first edge-to-edge grid with thin separators (adjacent-month days greyed,
 * up to two chips per day + "+N", today a filled circle on a tinted cell), and the selected day's
 * agenda underneath. List mode shows upcoming leave and holidays grouped by day. Everyone / Only me
 * narrows it to your own leave.
 *
 * A colleague's leave *type* is only present when the server chose to disclose it (your own
 * leave, or an approver viewing). When it is null the card reads "On leave" — the masking is the
 * server's decision, never reconstructed here.
 */

/** Chip tints — fixed so the legend always matches the grid. */
private val HolidayTint = Color(0xFFE04059)
/** Light grey fill for a public-holiday cell (today's highlight still wins). */
private val HolidayCell = Color(0xFFE9ECF2)
private val ColleagueTint = Color(0xFFF2A633)
private val SelfTint = Brand.Premier
private val GridLine = Color(0xFFD5E1F7)

@Composable
fun TeamCalendarScreen() {
    val today = startOfDay(Date())
    // `monthAnchor` is always the first day of the shown month; the API is year-scoped.
    var monthAnchor by remember { mutableStateOf(startOfMonth(today)) }
    var selectedDay by remember { mutableStateOf(today) }
    var mineOnly by remember { mutableStateOf(false) }
    var listMode by remember { mutableStateOf(false) }

    val year = yearOf(if (listMode) today else monthAnchor)

    fun goMonth(delta: Int) {
        monthAnchor = shiftMonth(monthAnchor, delta)
        // Land on today when it is in the new month, else on the 1st.
        selectedDay = if (sameMonth(today, monthAnchor)) today else monthAnchor
    }

    AsyncListScreen(
        key = year to listMode,
        // List mode also loads next year so "upcoming" keeps going past 31 December.
        fetch = { if (listMode) mergeYears(HrmsApi.teamCalendar(year), runCatching { HrmsApi.teamCalendar(year + 1) }.getOrNull()) else HrmsApi.teamCalendar(year) },
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
    ) { data ->
        val byDay = indexEntries(data, mineOnly)
        val holidays = holidayMap(data)

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TwoWay("Month", "List", second = listMode) { listMode = it }
                TwoWay("Everyone", "Only me", second = mineOnly) { mineOnly = it }
            }
        }

        if (!listMode) {
            item {
                MonthTitle(
                    title = monthTitle(monthAnchor),
                    onPrev = { goMonth(-1) },
                    onNext = { goMonth(1) },
                    onToday = { monthAnchor = startOfMonth(today); selectedDay = today },
                )
                WeekdayRow()
                MonthGrid(monthAnchor, selectedDay, byDay, holidays) { day ->
                    selectedDay = day
                    if (!sameMonth(day, monthAnchor)) monthAnchor = startOfMonth(day)
                }
                Legend()
            }
            val k = dayKey(selectedDay)
            agenda(selectedDay, isToday(selectedDay), byDay[k].orEmpty(), holidays[k])
        } else {
            upcoming(today, byDay, holidays)
        }
    }
}

// MARK: - Header

/** Two-segment toggle (Month / List, Everyone / Only me). */
@Composable
private fun TwoWay(first: String, secondLabel: String, second: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Brand.Surface)
            .border(1.dp, Brand.Border, RoundedCornerShape(10.dp))
            .padding(3.dp),
    ) {
        SegmentButton(first, selected = !second, modifier = Modifier.weight(1f)) { onChange(false) }
        SegmentButton(secondLabel, selected = second, modifier = Modifier.weight(1f)) { onChange(true) }
    }
}

@Composable
private fun SegmentButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Brand.Premier else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Color.White else Brand.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun MonthTitle(title: String, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = Brand.TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(
            "Today", color = Brand.Premier, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onToday).padding(horizontal = 12.dp, vertical = 12.dp),
        )
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month", tint = Brand.Premier)
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month", tint = Brand.Premier)
        }
    }
}

@Composable
private fun WeekdayRow() {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
            Text(
                d, color = Brand.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
            )
        }
    }
}

// MARK: - Grid (edge to edge, thin separators)

@Composable
private fun MonthGrid(
    monthAnchor: Date,
    selectedDay: Date,
    byDay: Map<String, List<TeamLeaveEntry>>,
    holidays: Map<String, String>,
    onSelect: (Date) -> Unit,
) {
    // A plain Column of Rows: this grid sits inside the parent LazyColumn.
    Column(modifier = Modifier.fillMaxWidth().border(0.5.dp, GridLine)) {
        gridDays(monthAnchor).chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    val k = dayKey(day)
                    DayCell(
                        day = day,
                        inMonth = sameMonth(day, monthAnchor),
                        selected = sameDay(day, selectedDay),
                        entries = byDay[k].orEmpty(),
                        holiday = holidays[k],
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(day) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Date,
    inMonth: Boolean,
    selected: Boolean,
    entries: List<TeamLeaveEntry>,
    holiday: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val today = isToday(day)
    val chips = buildList {
        holiday?.let { add(it to HolidayTint) }
        entries.forEach { add((if (it.isSelf) "You" else firstName(it.employeeName)) to if (it.isSelf) SelfTint else ColleagueTint) }
    }
    Column(
        modifier = modifier
            .height(78.dp)
            .background(
                when {
                    today -> Brand.SurfaceStrong
                    selected -> Brand.Premier.copy(alpha = 0.08f)
                    holiday != null -> HolidayCell
                    else -> Brand.Surface
                },
            )
            .border(0.25.dp, GridLine)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 3.dp)
            .clearAndSetSemantics { contentDescription = dayAccessibilityText(day, entries, holiday) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (today) Brand.Premier else Color.Transparent)
                .let { if (selected && !today) it.border(1.5.dp, Brand.Premier, CircleShape) else it },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                dayOfMonth(day).toString(),
                color = when {
                    today -> Color.White
                    !inMonth -> Brand.TextFaint
                    selected -> Brand.Premier
                    else -> Brand.TextPrimary
                },
                fontSize = 12.sp,
                fontWeight = if (today || selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
        Column(modifier = Modifier.fillMaxWidth().alpha(if (inMonth) 1f else 0.45f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            chips.take(2).forEach { (text, tint) ->
                // A holiday's own name may wrap to two lines when it is the only chip.
                DayChip(text, tint, maxLines = if (tint == HolidayTint && chips.size == 1) 2 else 1)
            }
            if (chips.size > 2) {
                Text("+${chips.size - 2}", color = Brand.TextMuted, fontSize = 9.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun DayChip(text: String, tint: Color, maxLines: Int = 1) {
    Text(
        text,
        // Navy on amber; white on blue and red.
        color = if (tint == ColleagueTint) Brand.Navy else Color.White,
        fontSize = 9.sp,
        lineHeight = 11.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(tint)
            .padding(horizontal = 3.dp, vertical = 1.dp),
    )
}

@Composable
private fun Legend() {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendDot(SelfTint, "You")
        LegendDot(ColleagueTint, "Colleague")
        LegendDot(HolidayTint, "Holiday")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(label, color = Brand.TextSecondary, fontSize = 11.sp)
    }
}

// MARK: - Agenda (selected day) and List mode

private fun LazyListScope.agenda(day: Date, isToday: Boolean, entries: List<TeamLeaveEntry>, holiday: String?) {
    item { AgendaHeader(if (isToday) "Today · ${agendaDate(day)}" else agendaDate(day)) }
    holiday?.let { h -> item { HolidayCard(h) } }
    if (entries.isEmpty() && holiday == null) {
        item { Box(Modifier.padding(horizontal = 16.dp)) { EmptyHint(Icons.Outlined.CalendarMonth, "Nobody is on leave.") } }
    }
    entries.forEach { e -> item { LeaveCard(e) } }
}

private fun LazyListScope.upcoming(today: Date, byDay: Map<String, List<TeamLeaveEntry>>, holidays: Map<String, String>) {
    val days = (0 until 400).map { addDays(today, it) }
        .filter { d -> val k = dayKey(d); byDay[k].orEmpty().isNotEmpty() || holidays[k] != null }
        .take(60)
    if (days.isEmpty()) {
        item { Box(Modifier.padding(16.dp)) { EmptyHint(Icons.Outlined.CalendarMonth, "No upcoming leave or holidays.") } }
        return
    }
    days.forEach { d ->
        val k = dayKey(d)
        item { AgendaHeader(if (isToday(d)) "Today · ${agendaDate(d)}" else agendaDate(d)) }
        holidays[k]?.let { h -> item { HolidayCard(h) } }
        byDay[k].orEmpty().forEach { e -> item { LeaveCard(e) } }
    }
}

@Composable
private fun AgendaHeader(text: String) {
    Text(
        text, color = Brand.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun HolidayCard(name: String) {
    Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Card {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Flag, contentDescription = null, tint = HolidayTint, modifier = Modifier.size(20.dp))
                Column {
                    Text(name, color = Brand.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Public holiday", color = Brand.TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun LeaveCard(e: TeamLeaveEntry) {
    Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(4.dp).height(34.dp).clip(RoundedCornerShape(2.dp)).background(if (e.isSelf) SelfTint else ColleagueTint))
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        if (e.isSelf) "You" else e.employeeName,
                        color = if (e.isSelf) Brand.Premier else Brand.TextPrimary,
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(entryDetail(e), color = Brand.TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** "Annual Leave · 2 days · Engineering" — the type is omitted when the server masked it. */
private fun entryDetail(e: TeamLeaveEntry): String {
    val bits = mutableListOf(e.leaveType ?: "On leave")
    bits += if (e.halfDay) "Half day" else formatDays(e.days)
    e.department?.takeIf { it.isNotBlank() && !e.isSelf }?.let { bits += it }
    return bits.joinToString(" · ")
}

private fun formatDays(days: Double): String {
    val n = if (days % 1.0 == 0.0) days.toInt().toString() else days.toString()
    return if (days == 1.0) "$n day" else "$n days"
}

private fun mergeYears(a: TeamCalendarResponse, b: TeamCalendarResponse?): TeamCalendarResponse =
    if (b == null) a else a.copy(
        entries = (a.entries + b.entries).distinctBy { it.id },
        holidays = (a.holidays + b.holidays).distinctBy { it.id },
    )

// MARK: - Date helpers

private fun cal(): Calendar = Calendar.getInstance(Locale.US)

private fun startOfMonth(d: Date): Date = cal().apply {
    time = d
    set(Calendar.DAY_OF_MONTH, 1)
    clearTime()
}.time

private fun Calendar.clearTime() {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}

private fun shiftMonth(anchor: Date, delta: Int): Date = cal().apply {
    time = anchor
    add(Calendar.MONTH, delta)
    set(Calendar.DAY_OF_MONTH, 1)
    clearTime()
}.time

private fun yearOf(d: Date): Int = cal().apply { time = d }.get(Calendar.YEAR)

private fun dayOfMonth(d: Date): Int = cal().apply { time = d }.get(Calendar.DAY_OF_MONTH)

private fun monthTitle(d: Date): String = SimpleDateFormat("LLLL yyyy", Locale.getDefault()).format(d)

private fun fullDate(d: Date): String = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(d)

private fun isToday(d: Date): Boolean {
    val a = cal().apply { time = d }
    val b = cal()
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}

/** Stable per-day key for bucketing (year-month-day, calendar-local). */
private fun dayKey(d: Date): String {
    val c = cal().apply { time = d }
    return "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH)}-${c.get(Calendar.DAY_OF_MONTH)}"
}

/**
 * Explode each multi-day leave into the days it covers, so a request spanning a week shows on
 * every one of those days. The guard bounds a pathological range (a bad server date) to a year.
 */
private fun indexEntries(data: TeamCalendarResponse, mineOnly: Boolean): Map<String, List<TeamLeaveEntry>> {
    val map = mutableMapOf<String, MutableList<TeamLeaveEntry>>()
    val source = if (mineOnly) data.entries.filter { it.isSelf } else data.entries

    for (e in source) {
        val start = Fmt.parse(e.startDate) ?: continue
        val end = Fmt.parse(e.endDate) ?: start
        val cursor = cal().apply { time = start; clearTime() }
        val last = cal().apply { time = end; clearTime() }
        var guard = 0
        while (!cursor.after(last) && guard < 400) {
            map.getOrPut(dayKey(cursor.time)) { mutableListOf() } += e
            cursor.add(Calendar.DAY_OF_MONTH, 1)
            guard++
        }
    }
    return map
}

/** Holiday titles by day key. */
private fun holidayMap(data: TeamCalendarResponse): Map<String, String> {
    val out = mutableMapOf<String, String>()
    for (h in data.holidays) {
        val d = Fmt.parse(h.startDate) ?: continue
        out[dayKey(d)] = h.title
    }
    return out
}

private fun firstName(full: String): String = full.trim().split(" ").firstOrNull().orEmpty().ifBlank { full }

private fun dayAccessibilityText(day: Date, entries: List<TeamLeaveEntry>, holiday: String?): String {
    val sb = StringBuilder(fullDate(day))
    holiday?.let { sb.append(", ").append(it) }
    if (entries.isNotEmpty()) {
        sb.append(", ").append(entries.size).append(" on leave: ")
        sb.append(entries.joinToString(", ") { if (it.isSelf) "You" else it.employeeName })
    }
    return sb.toString()
}

// MARK: - Grid / agenda helpers

/** The visible weeks: Monday before the 1st through the Sunday after the last day. */
private fun gridDays(anchor: Date): List<Date> {
    val first = startOfMonth(anchor)
    val c = cal().apply { time = first }
    val leading = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Mon = 0 … Sun = 6
    val count = c.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cells = ((leading + count + 6) / 7) * 7
    return (0 until cells).map { addDays(first, it - leading) }
}

private fun startOfDay(d: Date): Date = cal().apply { time = d; clearTime() }.time

private fun addDays(d: Date, n: Int): Date = cal().apply { time = d; add(Calendar.DAY_OF_MONTH, n) }.time

private fun sameDay(a: Date, b: Date): Boolean = dayKey(a) == dayKey(b)

private fun sameMonth(a: Date, b: Date): Boolean {
    val x = cal().apply { time = a }
    val y = cal().apply { time = b }
    return x.get(Calendar.YEAR) == y.get(Calendar.YEAR) && x.get(Calendar.MONTH) == y.get(Calendar.MONTH)
}

private fun agendaDate(d: Date): String = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(d)
