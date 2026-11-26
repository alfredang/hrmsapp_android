package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.hrportal.data.CalendarCategories
import com.tertiaryinfotech.hrportal.data.CalendarCategoryInfo
import com.tertiaryinfotech.hrportal.data.CalendarFilters
import com.tertiaryinfotech.hrportal.data.CalendarPeople
import com.tertiaryinfotech.hrportal.data.CalendarPeopleList
import com.tertiaryinfotech.hrportal.data.CalendarPersonOption
import com.tertiaryinfotech.hrportal.data.CalendarSummary
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.OffDaySchedule
import com.tertiaryinfotech.hrportal.data.OffDays
import com.tertiaryinfotech.hrportal.data.TeamBirthday
import com.tertiaryinfotech.hrportal.data.TeamCalendarResponse
import com.tertiaryinfotech.hrportal.data.TeamLeaveEntry
import com.tertiaryinfotech.hrportal.data.TeamTimeOff
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.theme.Appearance
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
 * narrows it to your own leave; approvers also get "Leave shown for" (Everyone or one person).
 *
 * A colleague's leave *type* is only present when the server chose to disclose it (your own
 * leave, or an approver viewing). When it is null the card reads "On leave" — the masking is the
 * server's decision, never reconstructed here.
 *
 * 1.16 (web parity, 20 Nov 2026): leave is coloured by its `category` with the category emoji in
 * front (a masked colleague's leave keeps the neutral colour, no emoji); approved time off and Off
 * Days (⏰) and, for approvers, birthdays (🎂) show too; off days are shaded when one schedule
 * applies; an approver who picks a person sees that person's totals for the month shown; the legend
 * lists what is shown. Pure rules live in `data/CalendarLogic.kt`.
 */

/** Light grey fill for a public-holiday or off-day cell (today's highlight still wins). */
private val HolidayCell: Color get() = Brand.HolidayCell
/** Neutral tints for leave whose type the server masked (and for an older server without categories). */
private val ColleagueTint = Color(0xFFF2A633)
/** Fallback holiday colour when the legend has no HOLIDAY row. */
private val HolidayTint = Color(0xFFEF4444)
private val SelfTint = Brand.Premier
private val GridLine: Color get() = Brand.Border

/**
 * [admin]: what the screens show (View As aware). Only admins get Everyone / Only me; everyone else sees their own
 * leave, MC, No Pay Leave, time off and the holidays (the server returns only those since 22 Nov 2026), captioned
 * "My leave and holidays".
 */
@Composable
fun TeamCalendarScreen(admin: Boolean = true) {
    val today = startOfDay(Date())
    // `monthAnchor` is always the first day of the shown month; the API is year-scoped.
    var monthAnchor by remember { mutableStateOf(startOfMonth(today)) }
    var selectedDay by remember { mutableStateOf(today) }
    var mineOnly by remember { mutableStateOf(false) }
    var listMode by remember { mutableStateOf(false) }
    // Approvers: whose leave shows. Null = everyone (the web calendar's "Leave shown for").
    var personId by rememberSaveable { mutableStateOf<String?>(null) }

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
        // Approvers only (null otherwise). A person picked earlier who is not in this year's list
        // (they left, or the list changed) falls back to Everyone.
        val people = CalendarPeople.build(data)
        val approver = people != null
        val person = people?.find(personId)
        val palette = Palette(CalendarCategories.table(data))
        val shownPerson = if (mineOnly) null else person?.id
        val byDay = indexItems(data, mineOnly, shownPerson, approver)
        val holidays = holidayMap(data)
        // One schedule (your own, or the person picked) shades its off days; Everyone shades nothing.
        val schedule = OffDays.schedule(data, approver, mineOnly, shownPerson)
        val totals = if (!listMode && approver && shownPerson != null) {
            CalendarSummary.rows(
                CalendarSummary.personMonthSummary(
                    CalendarSummary.events(data), shownPerson, yearOf(monthAnchor), monthOf(monthAnchor),
                    CalendarSummary.holidayKeys(data),
                ),
            )
        } else null

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TwoWay("Month", "List", second = listMode) { listMode = it }
                if (admin) {
                    TwoWay("Everyone", "Only me", second = mineOnly) { mineOnly = it }
                } else {
                    Text(
                        "My leave and holidays", color = Brand.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp).semantics { testTag = "calendar.scope" },
                    )
                }
                if (people != null && !mineOnly) {
                    PersonFilter(people, person) { personId = it }
                }
                if (totals != null && person != null) {
                    MonthTotals("${monthTitle(monthAnchor)} \u00b7 ${person.name}", totals, palette)
                }
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
                MonthGrid(monthAnchor, selectedDay, byDay, holidays, schedule, palette) { day ->
                    selectedDay = day
                    if (!sameMonth(day, monthAnchor)) monthAnchor = startOfMonth(day)
                }
                Legend(legendItems(data, byDay, palette, schedule != null))
            }
            val k = dayKey(selectedDay)
            agenda(selectedDay, isToday(selectedDay), byDay[k].orEmpty(), holidays[k], palette)
        } else {
            upcoming(today, byDay, holidays, palette)
        }
    }
}

// MARK: - Items

/**
 * One thing on a day: a leave, a time off / Off Day, or a birthday. [category] is the legend key;
 * null only for leave whose type the server masked, drawn in the neutral You / Colleague tint.
 */
@Immutable
internal data class CalItem(
    val id: String,
    /** The short grid chip: "🏖️ Daniel", "⏰ You", "🎂 Priya". */
    val chip: String,
    /** The agenda card's first line. */
    val title: String,
    val detail: String,
    val category: String?,
    val isSelf: Boolean,
    val offDay: Boolean = false,
    val personId: String = "",
)

/** Legend colours by category key, with the neutral fallbacks. Read inside composition (theme-aware). */
private class Palette(table: List<CalendarCategoryInfo>) {
    private val byKey = table.associateBy { it.key }
    val order: List<String> = table.map { it.key }

    fun info(key: String): CalendarCategoryInfo? = byKey[key] ?: CalendarCategories.FALLBACK.firstOrNull { it.key == key }
        ?: CalendarCategories.PAYBACK_CHIP.takeIf { key == it.key }
    fun label(key: String): String = info(key)?.label ?: key
    fun emoji(key: String?): String = key?.let { info(it)?.emoji }.orEmpty()
    fun color(key: String): Color =
        info(key)?.color?.let(CalendarCategories::parseHex)?.let { Color(0xFF000000 or it.toLong()) }
            ?: if (key == CalendarCategories.HOLIDAY) HolidayTint else Color(0xFF6B7280)

    /** Bar / dot colour of an item. */
    fun of(item: CalItem): Color = item.category?.let(::color) ?: if (item.isSelf) SelfTint else ColleagueTint
}

/** "🏖️ Daniel" — the emoji only when there is one. */
private fun withEmoji(emoji: String, text: String): String = if (emoji.isEmpty()) text else "$emoji $text"

/** "Time off 2:00 PM – 4:00 PM", or "Off Day" for a full day. */
internal fun timeOffLabel(t: TeamTimeOff): String =
    if (t.offDay) "Off Day" else "Time off ${timeOff12h(t.startTime)} \u2013 ${timeOff12h(t.endTime)}"

/** Agenda title of a time off: approvers see whose it is ("⏰ AMANDA HALIM · Off Day"). */
internal fun timeOffTitle(t: TeamTimeOff, approver: Boolean, emoji: String = "\u23F0"): String {
    val label = timeOffLabel(t)
    return withEmoji(emoji, if (approver && !t.isSelf) "${t.employeeName} \u00b7 $label" else label)
}

/** "🎂 DANIEL TAN birthday" (never the age). */
internal fun birthdayTitle(b: TeamBirthday, emoji: String = "\uD83C\uDF82"): String = withEmoji(emoji, "${b.name} birthday")

private fun leaveItem(e: TeamLeaveEntry, palette: Palette): CalItem {
    val emoji = palette.emoji(e.category)
    val who = if (e.isSelf) "You" else e.employeeName
    return CalItem(
        id = "l:${e.id}",
        chip = withEmoji(emoji, if (e.isSelf) "You" else firstName(e.employeeName)),
        title = withEmoji(emoji, who),
        detail = entryDetail(e),
        category = e.category,
        isSelf = e.isSelf,
        personId = e.employeeId,
    )
}

private fun timeOffItem(t: TeamTimeOff, approver: Boolean, palette: Palette): CalItem {
    val emoji = palette.emoji(CalendarCategories.TIME_OFF).ifEmpty { "\u23F0" }
    val chipText = when {
        approver && !t.isSelf -> firstName(t.employeeName)
        approver -> "You"
        t.offDay -> "Off Day"
        else -> "Time off"
    }
    val detail = if (t.offDay) "Time Off \u00b7 All day" else "Time Off \u00b7 ${formatHours(t.hours)}"
    return CalItem(
        id = "t:${t.id}",
        chip = withEmoji(emoji, chipText),
        title = timeOffTitle(t, approver, emoji),
        detail = detail,
        category = CalendarCategories.TIME_OFF,
        isSelf = t.isSelf,
        offDay = t.offDay,
        personId = t.employeeId,
    )
}

private fun birthdayItem(b: TeamBirthday, palette: Palette): CalItem {
    val emoji = palette.emoji(CalendarCategories.BIRTHDAY).ifEmpty { "\uD83C\uDF82" }
    return CalItem(
        id = "b:${b.id}",
        chip = withEmoji(emoji, firstName(b.name)),
        title = birthdayTitle(b, emoji),
        detail = "Birthday \u00b7 All day",
        category = CalendarCategories.BIRTHDAY,
        isSelf = false,
        personId = b.employeeId,
    )
}

private fun formatHours(h: Double): String = "${CalendarSummary.num(h)} h"

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

/**
 * Approvers: "Leave & time off for" Everyone or one person. Every active staff member and intern is
 * listed, grouped Staff then Interns, each with their number of leave and time-off entries. Holidays
 * always show; birthdays follow the pick.
 */
@Composable
private fun PersonFilter(people: CalendarPeopleList, selected: CalendarPersonOption?, onPick: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Brand.Surface)
                .border(1.dp, if (selected != null) Brand.Accent else Brand.Border, shape)
                .clickable(onClickLabel = "Choose whose leave and time off is shown") { open = true }
                .padding(start = 14.dp, end = 8.dp, top = 13.dp, bottom = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Leave & time off for", color = Brand.TextSecondary, fontSize = 14.sp, maxLines = 1)
            Text(
                selected?.name ?: "Everyone", color = Brand.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Brand.TextSecondary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Brand.Dialog) {
            PersonMenuItem("Everyone", people.totalEntries, selected == null) { onPick(null); open = false }
            listOf("Staff" to people.staff, "Interns" to people.interns, "People" to people.others).forEach { (title, list) ->
                if (list.isNotEmpty()) {
                    Text(
                        "$title (${list.size})".uppercase(), color = Brand.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
                    )
                    list.forEach { p ->
                        PersonMenuItem(p.name, p.leaveCount, selected?.id == p.id) { onPick(p.id); open = false }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonMenuItem(name: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Row(modifier = Modifier.widthIn(min = 220.dp, max = 280.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name, color = if (selected) Brand.Accent else Brand.TextPrimary, fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                Text(count.toString(), color = Brand.TextMuted, fontSize = 13.sp, maxLines = 1, modifier = Modifier.padding(start = 16.dp))
            }
        },
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = "$name, $count leave and time-off ${if (count == 1) "entry" else "entries"}" },
    )
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
            "Today", color = Brand.Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onToday).padding(horizontal = 12.dp, vertical = 12.dp),
        )
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month", tint = Brand.Accent)
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month", tint = Brand.Accent)
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

// MARK: - Month totals (approvers, one person picked)

/** The picked person's leave and time off in the month shown, one line each (web month-total pills). */
@Composable
private fun MonthTotals(title: String, rows: List<Triple<String, String, String>>, palette: Palette) {
    Card(padding = 14) {
        Text(
            title, color = Brand.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 4.dp),
        )
        rows.forEach { (label, value, key) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(palette.color(key)))
                Text(
                    label, color = Brand.TextSecondary, fontSize = 13.sp, maxLines = 1,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                Text(value, color = Brand.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

// MARK: - Grid (edge to edge, thin separators)

@Composable
private fun MonthGrid(
    monthAnchor: Date,
    selectedDay: Date,
    byDay: Map<String, List<CalItem>>,
    holidays: Map<String, String>,
    schedule: OffDaySchedule?,
    palette: Palette,
    onSelect: (Date) -> Unit,
) {
    // A plain Column of Rows: this grid sits inside the parent LazyColumn.
    Column(modifier = Modifier.fillMaxWidth().border(0.5.dp, GridLine)) {
        gridDays(monthAnchor).chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    val k = dayKey(day)
                    val items = byDay[k].orEmpty()
                    DayCell(
                        day = day,
                        inMonth = sameMonth(day, monthAnchor),
                        selected = sameDay(day, selectedDay),
                        items = items,
                        holiday = holidays[k],
                        offDay = isOffDay(schedule, day, items),
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(day) },
                    )
                }
            }
        }
    }
}

/** Off-day shading for a cell, from the day's (already filtered) time off. */
private fun isOffDay(schedule: OffDaySchedule?, day: Date, items: List<CalItem>): Boolean =
    OffDays.isOff(
        schedule, day,
        items.filter { it.category == CalendarCategories.TIME_OFF }
            .map { TeamTimeOff(employeeId = it.personId, offDay = it.offDay, isSelf = it.isSelf) },
    )

@Composable
private fun DayCell(
    day: Date,
    inMonth: Boolean,
    selected: Boolean,
    items: List<CalItem>,
    holiday: String?,
    offDay: Boolean,
    palette: Palette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val today = isToday(day)
    // (text, colour, tinted): category items are a tint of their colour; masked leave keeps the solid neutral chip.
    val chips = buildList {
        holiday?.let { add(Triple(it, palette.color(CalendarCategories.HOLIDAY), true)) }
        items.forEach { add(Triple(it.chip, palette.of(it), it.category != null)) }
    }
    Column(
        modifier = modifier
            .height(78.dp)
            .background(
                when {
                    today -> Brand.SurfaceStrong
                    selected -> Brand.Premier.copy(alpha = 0.08f)
                    // Off days are shaded inside the shown month only, as on the web.
                    holiday != null || (offDay && inMonth) -> HolidayCell
                    else -> Brand.Surface
                },
            )
            .border(0.25.dp, GridLine)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 3.dp)
            .clearAndSetSemantics { contentDescription = dayAccessibilityText(day, items, holiday, offDay) },
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
                    selected -> Brand.Accent
                    else -> Brand.TextPrimary
                },
                fontSize = 12.sp,
                fontWeight = if (today || selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
        Column(modifier = Modifier.fillMaxWidth().alpha(if (inMonth) 1f else 0.45f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            chips.take(2).forEachIndexed { i, (text, color, tinted) ->
                // A holiday's own name may wrap to two lines when it is the only chip.
                DayChip(text, color, tinted, maxLines = if (i == 0 && holiday != null && chips.size == 1) 2 else 1)
            }
            if (chips.size > 2) {
                Text("+${chips.size - 2}", color = Brand.TextMuted, fontSize = 9.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }
}

/**
 * A category chip is the web's Planner style: a light tint of the colour, the text a darker shade
 * of it in light and the colour itself in dark. A neutral (masked) leave chip stays solid.
 */
@Composable
private fun DayChip(text: String, color: Color, tinted: Boolean, maxLines: Int = 1) {
    val ink = when {
        tinted -> if (Appearance.isDark) color else darken(color, 0.45f)
        color == ColleagueTint -> Brand.Navy // navy on amber
        else -> Color.White
    }
    Text(
        text,
        color = ink,
        fontSize = 9.sp,
        lineHeight = 11.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(if (tinted) color.copy(alpha = if (Appearance.isDark) 0.22f else 0.16f) else color)
            .padding(horizontal = 3.dp, vertical = 1.dp),
    )
}

/** Web `darken()`: each channel scaled towards black, so chip text stays readable on its own tint. */
private fun darken(c: Color, amount: Float): Color =
    Color(red = c.red * (1 - amount), green = c.green * (1 - amount), blue = c.blue * (1 - amount), alpha = 1f)

/** One legend swatch. [cell] draws the off-day cell fill with a hairline instead of a colour. */
private data class LegendEntry(val label: String, val color: Color, val cell: Boolean = false)

/**
 * The categories shown, in legend order: holidays, each kind of leave, time off and birthdays that
 * appear this year under the current filter; the neutral You / Colleague swatches for masked leave;
 * and "Off day" when shading is on.
 */
private fun legendItems(data: TeamCalendarResponse, byDay: Map<String, List<CalItem>>, palette: Palette, shading: Boolean): List<LegendEntry> {
    val items = byDay.values.flatten().distinctBy { it.id }
    val present = items.mapNotNull { it.category }.toSet() +
        (if (data.holidays.isNotEmpty()) setOf(CalendarCategories.HOLIDAY) else emptySet())
    val order = (palette.order + CalendarCategories.FALLBACK.map { it.key }).distinct()
    return buildList {
        order.filter { it in present }.forEach { add(LegendEntry(palette.label(it), palette.color(it))) }
        if (items.any { it.category == null && it.isSelf }) add(LegendEntry("You", SelfTint))
        if (items.any { it.category == null && !it.isSelf }) add(LegendEntry("Colleague", ColleagueTint))
        if (shading) add(LegendEntry("Off day", HolidayCell, cell = true))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(entries: List<LegendEntry>) {
    if (entries.isEmpty()) return
    FlowRow(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        entries.forEach { LegendDot(it) }
    }
}

@Composable
private fun LegendDot(e: LegendEntry) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        val shape = RoundedCornerShape(2.dp)
        Box(
            Modifier.size(10.dp).clip(shape).background(e.color)
                .let { if (e.cell) it.border(1.dp, Brand.BorderLight, shape) else it },
        )
        Text(e.label, color = Brand.TextSecondary, fontSize = 11.sp, maxLines = 1)
    }
}

// MARK: - Agenda (selected day) and List mode

private fun LazyListScope.agenda(day: Date, isToday: Boolean, items: List<CalItem>, holiday: String?, palette: Palette) {
    item { AgendaHeader(if (isToday) "Today · ${agendaDate(day)}" else agendaDate(day)) }
    holiday?.let { h -> item { HolidayCard(h, palette) } }
    if (items.isEmpty() && holiday == null) {
        item { Box(Modifier.padding(horizontal = 16.dp)) { EmptyHint(Icons.Outlined.CalendarMonth, "Nobody is on leave.") } }
    }
    items.forEach { e -> item { ItemCard(e, palette) } }
}

private fun LazyListScope.upcoming(today: Date, byDay: Map<String, List<CalItem>>, holidays: Map<String, String>, palette: Palette) {
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
        holidays[k]?.let { h -> item { HolidayCard(h, palette) } }
        byDay[k].orEmpty().forEach { e -> item { ItemCard(e, palette) } }
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
private fun HolidayCard(name: String, palette: Palette) {
    Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Card {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Flag, contentDescription = null, tint = palette.color(CalendarCategories.HOLIDAY), modifier = Modifier.size(20.dp))
                Column {
                    Text(name, color = Brand.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Public holiday", color = Brand.TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

/** Leave, time off or a birthday: a colour bar, the title and one detail line. */
@Composable
private fun ItemCard(e: CalItem, palette: Palette) {
    Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(4.dp).height(34.dp).clip(RoundedCornerShape(2.dp)).background(palette.of(e)))
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        e.title,
                        color = if (e.isSelf && e.category != CalendarCategories.TIME_OFF) Brand.Accent else Brand.TextPrimary,
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(e.detail, color = Brand.TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        people = a.people ?: b.people,
        timeOff = (a.timeOff + b.timeOff).distinctBy { it.id },
        birthdays = if (a.birthdays == null && b.birthdays == null) null
        else (a.birthdays.orEmpty() + b.birthdays.orEmpty()).distinctBy { it.id },
        ownWorkdays = a.ownWorkdays ?: b.ownWorkdays,
        categories = a.categories ?: b.categories,
        paybacks = if (a.paybacks == null && b.paybacks == null) null
        else (a.paybacks.orEmpty() + b.paybacks.orEmpty()).distinctBy { it.id },
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

/** 0-based month, as the web's `getMonth()`. */
private fun monthOf(d: Date): Int = cal().apply { time = d }.get(Calendar.MONTH)

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
 * Every item by day: each multi-day leave exploded into the days it covers (so a request spanning
 * a week shows on every one of those days), then time off and birthdays on their date. Leave, time
 * off and birthdays follow Only me and the person filter. The guard bounds a pathological range (a
 * bad server date) to a year.
 */
private fun indexItems(data: TeamCalendarResponse, mineOnly: Boolean, personId: String?, approver: Boolean): Map<String, List<CalItem>> {
    val palette = Palette(CalendarCategories.table(data))
    val map = mutableMapOf<String, MutableList<CalItem>>()
    val source = if (mineOnly) data.entries.filter { it.isSelf } else CalendarPeople.filter(data.entries, personId)

    for (e in source) {
        val start = Fmt.parse(e.startDate) ?: continue
        val end = Fmt.parse(e.endDate) ?: start
        val item = leaveItem(e, palette)
        val cursor = cal().apply { time = start; clearTime() }
        val last = cal().apply { time = end; clearTime() }
        var guard = 0
        while (!cursor.after(last) && guard < 400) {
            map.getOrPut(dayKey(cursor.time)) { mutableListOf() } += item
            cursor.add(Calendar.DAY_OF_MONTH, 1)
            guard++
        }
    }
    for (t in CalendarFilters.timeOff(data.timeOff, mineOnly, personId)) {
        val d = Fmt.parse(t.date) ?: continue
        map.getOrPut(dayKey(d)) { mutableListOf() } += timeOffItem(t, approver, palette)
    }
    for (b in CalendarFilters.birthdays(data, mineOnly, personId)) {
        val d = Fmt.parse(b.date) ?: continue
        map.getOrPut(dayKey(d)) { mutableListOf() } += birthdayItem(b, palette)
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

private fun dayAccessibilityText(day: Date, items: List<CalItem>, holiday: String?, offDay: Boolean): String {
    val sb = StringBuilder(fullDate(day))
    holiday?.let { sb.append(", ").append(it) }
    if (offDay) sb.append(", off day")
    items.forEach { sb.append(", ").append(it.title) }
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
