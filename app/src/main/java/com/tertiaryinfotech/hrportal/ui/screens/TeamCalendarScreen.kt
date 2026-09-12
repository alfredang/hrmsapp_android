package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Team calendar — a month grid of everyone's approved leave, mirroring the iOS
 * `TeamCalendarView` (month title, ‹ Today ›, a Sun–Sat grid with a chip per person per day)
 * and, through it, the web app's calendar. A segmented control narrows it to just your own leave.
 *
 * A colleague's leave *type* is only present when the server chose to disclose it (your own
 * leave, or an approver viewing). When it is null the chip reads just the name — the masking is
 * the server's decision, never reconstructed here.
 */

/** Chip tints — fixed across both themes so the legend always matches the grid (iOS parity). */
private val HolidayTint = Color(0xFFE04059)
private val ColleagueTint = Color(0xFFF2A633)

@Composable
fun TeamCalendarScreen() {
    // The displayed month drives the fetch: the API is year-scoped, so paging across a year
    // boundary must refetch. `monthAnchor` is always the first day of the shown month.
    var monthAnchor by remember { mutableStateOf(startOfMonth(Date())) }
    var mineOnly by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<DayBucket?>(null) }

    val year = yearOf(monthAnchor)

    // Keyed on the year so paging within a year reuses the loaded data and only a year change
    // re-runs the request.
    AsyncListScreen(
        key = year,
        fetch = { HrmsApi.teamCalendar(year) },
    ) { data ->
        item {
            MonthHeader(
                title = monthTitle(monthAnchor),
                onPrev = { monthAnchor = shiftMonth(monthAnchor, -1) },
                onNext = { monthAnchor = shiftMonth(monthAnchor, 1) },
                onToday = { monthAnchor = startOfMonth(Date()) },
            )
            Spacer(Modifier.height(14.dp))
            ScopePicker(mineOnly = mineOnly, onChange = { mineOnly = it })
            Spacer(Modifier.height(14.dp))
            WeekdayRow()
            Spacer(Modifier.height(6.dp))
            MonthGrid(
                monthAnchor = monthAnchor,
                data = data,
                mineOnly = mineOnly,
                onDayTap = { selected = it },
            )
            Spacer(Modifier.height(16.dp))
            Legend()
        }
    }

    selected?.let { bucket ->
        DayDetailSheet(bucket = bucket, onDismiss = { selected = null })
    }
}

// MARK: - Header

@Composable
private fun MonthHeader(
    title: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            color = Brand.TextPrimary,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            NavButton(Icons.Filled.ChevronLeft, "Previous month", onPrev)
            Text(
                "Today",
                color = Brand.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Brand.Surface)
                    .clickable(onClick = onToday)
                    // 48dp min touch target (Material) — the visual pill stays compact.
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            )
            NavButton(Icons.Filled.ChevronRight, "Next month", onNext)
        }
    }
}

@Composable
private fun NavButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Brand.Surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Brand.TextPrimary, modifier = Modifier.size(22.dp))
    }
}

/** Everyone / Only me — a two-segment toggle (Material's equivalent of the iOS segmented picker). */
@Composable
private fun ScopePicker(mineOnly: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Brand.Surface)
            .padding(3.dp),
    ) {
        SegmentButton("Everyone", selected = !mineOnly, modifier = Modifier.weight(1f)) { onChange(false) }
        SegmentButton("Only me", selected = mineOnly, modifier = Modifier.weight(1f)) { onChange(true) }
    }
}

@Composable
private fun SegmentButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Brand.Primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
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
private fun WeekdayRow() {
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { d ->
            Text(
                d,
                color = Brand.TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// MARK: - Grid

@Composable
private fun MonthGrid(
    monthAnchor: Date,
    data: TeamCalendarResponse,
    mineOnly: Boolean,
    onDayTap: (DayBucket) -> Unit,
) {
    val days = monthDays(monthAnchor)
    val byDay = indexEntries(data, mineOnly)
    val holidays = holidayMap(data)

    // A plain Column of Rows rather than LazyVerticalGrid: this grid already sits inside the
    // parent LazyColumn, and nesting a lazy vertical scroller inside another crashes Compose.
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        days.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { day ->
                    if (day == null) {
                        Spacer(Modifier.weight(1f).height(84.dp))
                    } else {
                        val k = dayKey(day)
                        DayCell(
                            day = day,
                            entries = byDay[k].orEmpty(),
                            holiday = holidays[k],
                            modifier = Modifier.weight(1f),
                            onTap = onDayTap,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Date,
    entries: List<TeamLeaveEntry>,
    holiday: String?,
    modifier: Modifier = Modifier,
    onTap: (DayBucket) -> Unit,
) {
    val isToday = isToday(day)
    val shown = if (holiday == null) 2 else 1
    val tappable = entries.isNotEmpty() || holiday != null

    Column(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isToday) Brand.SurfaceStrong else Brand.Surface)
            .border(1.dp, Brand.Border, RoundedCornerShape(8.dp))
            .then(
                if (tappable) {
                    Modifier.clickable { onTap(DayBucket(day, entries, holiday)) }
                } else {
                    Modifier
                },
            )
            .padding(4.dp)
            // One combined announcement per day — reading each chip separately makes the grid
            // unusable with TalkBack.
            .clearAndSetSemantics { contentDescription = dayAccessibilityText(day, entries, holiday) },
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isToday) Brand.Primary else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                dayOfMonth(day).toString(),
                color = if (isToday) Color.White else Brand.TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
            )
        }

        if (holiday != null) DayChip("Holiday", HolidayTint)

        entries.take(shown).forEach { e ->
            DayChip(
                text = if (e.isSelf) "You" else firstName(e.employeeName),
                tint = if (e.isSelf) Brand.Sky else ColleagueTint,
            )
        }

        if (entries.size > shown) {
            Text(
                "+${entries.size - shown} more",
                color = Brand.TextMuted,
                fontSize = 9.sp,
            )
        }
    }
}

@Composable
private fun DayChip(text: String, tint: Color) {
    Text(
        text,
        color = Color.Black.copy(alpha = 0.85f),
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(tint.copy(alpha = 0.85f))
            .padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendDot(Brand.Sky, "You")
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

// MARK: - Day detail sheet

/** One tapped day, for the detail sheet. */
data class DayBucket(
    val date: Date,
    val entries: List<TeamLeaveEntry>,
    val holiday: String?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailSheet(bucket: DayBucket, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = Brand.Background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                fullDate(bucket.date),
                color = Brand.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )

            bucket.holiday?.let { h ->
                Card {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Flag, contentDescription = null, tint = HolidayTint, modifier = Modifier.size(20.dp))
                        Text(h, color = Brand.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (bucket.entries.isEmpty() && bucket.holiday == null) {
                EmptyHint(Icons.Outlined.CalendarMonth, "Nobody is on leave.")
            }

            bucket.entries.forEach { e ->
                Card {
                    Text(
                        if (e.isSelf) "You" else e.employeeName,
                        color = if (e.isSelf) Brand.Sky else Brand.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(entryDetail(e), color = Brand.TextSecondary, fontSize = 11.sp)
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
 * Every day of the displayed month, padded with nulls so the 1st lands on its real weekday
 * column and the final row is complete.
 */
private fun monthDays(anchor: Date): List<Date?> {
    val c = cal().apply { time = anchor }
    val leading = c.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val count = c.getActualMaximum(Calendar.DAY_OF_MONTH)

    val out = mutableListOf<Date?>()
    repeat(leading) { out += null }
    for (d in 0 until count) {
        out += cal().apply { time = anchor; add(Calendar.DAY_OF_MONTH, d) }.time
    }
    while (out.size % 7 != 0) out += null
    return out
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
