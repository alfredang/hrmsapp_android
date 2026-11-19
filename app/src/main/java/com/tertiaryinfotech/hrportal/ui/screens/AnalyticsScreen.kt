package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.hrportal.BuildConfig
import com.tertiaryinfotech.hrportal.data.AnalyticsCalc
import com.tertiaryinfotech.hrportal.data.AnalyticsData
import com.tertiaryinfotech.hrportal.data.AnalyticsFilter
import com.tertiaryinfotech.hrportal.data.AnalyticsView
import com.tertiaryinfotech.hrportal.data.AnalyticsWho
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.MonthValues
import com.tertiaryinfotech.hrportal.ui.components.AsyncContent
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.LoadState
import com.tertiaryinfotech.hrportal.ui.components.RefreshableColumn
import com.tertiaryinfotech.hrportal.ui.theme.Brand

// Categorical slots validated for the web dashboard on the light and dark cards. Colour follows
// the entity (Leave / MC / No Pay Leave match the web chart); text always uses the ink tokens.
private val SeriesBlue = Color(0xFF5B82F0)
private val SeriesTeal = Color(0xFF22978B)
private val SeriesAmber = Color(0xFFBF8420)

private data class Series(val key: String, val label: String, val color: Color)

private val FINANCE = listOf(Series("revenue", "Revenue", SeriesBlue), Series("expenses", "Expenses", SeriesAmber))
private val LEAVE = listOf(Series("LEAVE", "Leave", SeriesBlue), Series("MC", "MC", SeriesTeal), Series("NPL", "No Pay Leave", SeriesAmber))
private val HEAD = listOf(Series("staff", "Staff", SeriesBlue), Series("interns", "Interns", SeriesTeal))

/**
 * Approvers: the last 12 months of bank revenue and expenses, leave and MC per person, and
 * headcount, with one filter row that drives every chart and tile. Mirrors the web
 * `/dashboard/analytics` page; all the numbers come from [AnalyticsCalc.view].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalyticsScreen() {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<LoadState<AnalyticsData>>(LoadState.Idle) }

    suspend fun load() {
        if (state !is LoadState.Loaded) state = LoadState.Loading
        state = try {
            LoadState.Loaded(HrmsApi.analytics())
        } catch (e: Exception) {
            // Debug builds fall back to sample data so the screen can be checked without an approver login.
            if (BuildConfig.DEBUG) LoadState.Loaded(AnalyticsCalc.sample()) else LoadState.Failed(e.message ?: "Could not load.")
        }
    }

    var period by rememberSaveable { mutableStateOf(12) }
    var who by rememberSaveable { mutableStateOf(AnalyticsWho.ALL) }
    var personId by rememberSaveable { mutableStateOf<String?>(null) }
    var leaveOff by rememberSaveable { mutableStateOf(listOf<String>()) }
    var typeOff by rememberSaveable { mutableStateOf(listOf<String>()) }

    AsyncContent(state = state, load = { load() }) { data ->
        val filter = AnalyticsFilter(period, who, personId, leaveOff.toSet(), typeOff.toSet())
        val v = remember(data, filter) { AnalyticsCalc.view(data, filter) }

        RefreshableColumn(scope, { load() }) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Analytics", color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Text(v.rangeLabel, color = Brand.TextSecondary, fontSize = 12.sp)
                }
                if (!filter.isDefault) {
                    Text(
                        "Reset", color = Brand.Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { period = 12; who = AnalyticsWho.ALL; personId = null; leaveOff = emptyList(); typeOff = emptyList() }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }

            // Filters: one block above every chart
            Segments(listOf("3" to "3 months", "6" to "6 months", "12" to "12 months"), period.toString()) { period = it.toInt() }
            Segments(listOf("ALL" to "All", "STAFF" to "Staff", "INTERNS" to "Interns"), who.name) {
                who = AnalyticsWho.valueOf(it)
                personId = null
            }
            PersonPicker(v) { personId = it }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LEAVE.forEach { s ->
                    val on = s.key !in leaveOff
                    ToggleChip(s.label, s.color, on) { leaveOff = if (on) leaveOff + s.key else leaveOff - s.key }
                }
            }

            // Summary tiles: the same filtered rows as the charts below
            if (v.hasFinance) {
                TileRow(
                    Triple("Revenue", AnalyticsCalc.money(v.revenue), v.rangeLabel),
                    Triple("Expenses", AnalyticsCalc.money(v.expenses), if (typeOff.isEmpty()) "All categories" else "${typeOff.size} hidden"),
                )
            }
            TileRow(
                if (v.hasFinance) Triple("Net", AnalyticsCalc.money(v.net), "Revenue less expenses") else
                    Triple("No Pay Leave", "${AnalyticsCalc.days(v.leaveTotals["NPL"] ?: 0.0)} d", "Approved days"),
                Triple("Headcount", v.headNow.toString(), "${if (v.headDelta >= 0) "+" else "−"}${kotlin.math.abs(v.headDelta)} over ${v.headByMonth.size} months"),
            )
            TileRow(
                Triple("Leave", "${AnalyticsCalc.days(v.leaveTotals["LEAVE"] ?: 0.0)} d", v.selectedPerson?.name ?: whoLabel(who)),
                Triple("MC", "${AnalyticsCalc.days(v.leaveTotals["MC"] ?: 0.0)} d", "${v.peopleOnMc} ${if (v.peopleOnMc == 1) "person" else "people"}"),
            )

            if (v.hasFinance) {
                ChartCard(
                    "Revenue and expenses",
                    "Bank transactions by payment month" + (data.financeRange?.let { " · data from ${it.from} to ${it.to}" } ?: ""),
                    FINANCE.map { it to AnalyticsCalc.money(if (it.key == "revenue") v.revenue else v.expenses) },
                ) {
                    if (!v.hasFinanceRows) Empty("No bank transactions in this period.") else {
                        BarChart(
                            v.financeByMonth, FINANCE, grouped = true, axis = AnalyticsCalc::moneyAxis, value = AnalyticsCalc::money,
                            footer = { "Net" to AnalyticsCalc.money((it.values["revenue"] ?: 0.0) - (it.values["expenses"] ?: 0.0)) },
                        )
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("By category · tap to hide", color = Brand.TextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            if (typeOff.isNotEmpty()) {
                                Text("Show all", color = Brand.Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { typeOff = emptyList() }.padding(6.dp))
                            }
                        }
                        val max = v.categories.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
                        v.categories.forEach { c ->
                            val on = c.type !in typeOff
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { typeOff = if (on) typeOff + c.type else typeOff - c.type }
                                    .padding(vertical = 9.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    c.type, color = if (on) Brand.TextPrimary else Brand.TextFaint, fontSize = 13.sp, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis, textDecoration = if (on) null else TextDecoration.LineThrough,
                                    modifier = Modifier.width(112.dp),
                                )
                                Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(8.dp).clip(RoundedCornerShape(3.dp)).background(Brand.Field)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth((c.amount / max).toFloat().coerceIn(0.02f, 1f))
                                            .clip(RoundedCornerShape(3.dp))
                                            .background((if (c.isRevenue) SeriesBlue else SeriesAmber).copy(alpha = if (on) 1f else 0.35f)),
                                    )
                                }
                                Text(AnalyticsCalc.money(c.amount), color = if (on) Brand.TextPrimary else Brand.TextFaint, fontSize = 13.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }

            val leaveSeries = LEAVE.filter { it.key in v.leaveSeries }
            ChartCard(
                "Leave and MC" + (v.selectedPerson?.let { " · ${it.name}" } ?: ""),
                "Approved days, by the month the leave starts",
                leaveSeries.map { it to "${AnalyticsCalc.days(v.leaveTotals[it.key] ?: 0.0)} d" },
            ) {
                when {
                    leaveSeries.isEmpty() -> Empty("Every leave type is switched off.")
                    v.byPerson.isEmpty() -> Empty("No approved leave in this period.")
                    else -> {
                        BarChart(
                            v.leaveByMonth, leaveSeries, grouped = false, axis = AnalyticsCalc::days, value = { "${AnalyticsCalc.days(it)} d" },
                            footer = { m -> "Total" to "${AnalyticsCalc.days(leaveSeries.sumOf { m.values[it.key] ?: 0.0 })} d" },
                        )
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("By person · tap a name to show only that person", color = Brand.TextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        }
                        TableHeader(listOf("Name") + leaveSeries.map { if (it.key == "NPL") "NPL" else it.label } + "Total")
                        v.byPerson.forEach { p ->
                            val selected = v.selectedPerson?.id == p.person.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) Brand.Field else Color.Transparent)
                                    .clickable { personId = if (selected) null else p.person.id }
                                    .padding(vertical = 9.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(p.person.name, color = Brand.TextPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(if (p.person.intern) "Intern" else "Staff", color = Brand.TextMuted, fontSize = 11.sp)
                                }
                                leaveSeries.forEach { NumberCell(AnalyticsCalc.days(p.of(it.key))) }
                                NumberCell(AnalyticsCalc.days(p.total), strong = true)
                            }
                        }
                    }
                }
            }

            val headSeries = HEAD.filter { it.key in v.headSeries }
            ChartCard(
                "Number of staff",
                "People employed on the last day of each month (today for the current month)",
                headSeries.map { s -> s to "${(v.headByMonth.lastOrNull()?.values?.get(s.key) ?: 0.0).toInt()} now" },
            ) {
                BarChart(
                    v.headByMonth, headSeries, grouped = false, axis = AnalyticsCalc::days, value = { it.toInt().toString() },
                    footer = if (headSeries.size > 1) ({ m -> "Total" to m.values.values.sum().toInt().toString() }) else null,
                )
                Text("Change by month", color = Brand.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                TableHeader(listOf("Month") + headSeries.map { it.label } + listOf("Total", "Change"))
                v.headChanges.forEach { c ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(c.title, color = Brand.TextPrimary, fontSize = 13.sp, maxLines = 1, modifier = Modifier.weight(1f))
                        headSeries.forEach { NumberCell(if (it.key == "staff") c.staff.toString() else c.interns.toString()) }
                        NumberCell(c.total.toString(), strong = true)
                        NumberCell(c.change?.let { if (it == 0) "0" else (if (it > 0) "+" else "−") + kotlin.math.abs(it) } ?: "")
                    }
                }
            }
        }
    }
}

private fun whoLabel(who: AnalyticsWho) = when (who) {
    AnalyticsWho.ALL -> "Staff and interns"
    AnalyticsWho.STAFF -> "Staff"
    AnalyticsWho.INTERNS -> "Interns"
}

/** Segmented control with a clear selected state (brand fill, white label). */
@Composable
private fun Segments(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(modifier = Modifier.fillMaxWidth().clip(shape).background(Brand.Surface).border(1.dp, Brand.Border, shape).padding(3.dp)) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) Brand.Premier else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (on) Color.White else Brand.TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PersonPicker(v: AnalyticsView, onPick: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Brand.Surface)
                .border(1.dp, Brand.Border, shape)
                .clickable { open = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Person", color = Brand.TextSecondary, fontSize = 14.sp)
            Text(
                v.selectedPerson?.name ?: "Everyone", color = Brand.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Brand.TextSecondary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Brand.Dialog) {
            DropdownMenuItem(text = { Text("Everyone", color = Brand.TextPrimary) }, onClick = { onPick(null); open = false })
            v.selectablePeople.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.name + if (p.active) "" else " (left)", color = Brand.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = { onPick(p.id); open = false },
                )
            }
        }
    }
}

@Composable
private fun ToggleChip(label: String, color: Color, on: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(Brand.Surface)
            .border(1.dp, if (on) Brand.BorderLight else Brand.Border, shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$label, ${if (on) "shown" else "hidden"}" }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(if (on) color else Brand.TextFaint))
        Text(
            label, color = if (on) Brand.TextPrimary else Brand.TextFaint, fontSize = 13.sp, maxLines = 1,
            textDecoration = if (on) null else TextDecoration.LineThrough, modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun TileRow(a: Triple<String, String, String>, b: Triple<String, String, String>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(a, b).forEach { (label, value, sub) ->
            Card(modifier = Modifier.weight(1f), padding = 14) {
                Text(label, color = Brand.TextSecondary, fontSize = 12.sp, maxLines = 1)
                Text(value, color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp))
                Text(sub, color = Brand.TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChartCard(title: String, sub: String, legend: List<Pair<Series, String>>, content: @Composable () -> Unit) {
    Card {
        Text(title, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(sub, color = Brand.TextMuted, fontSize = 12.sp)
        if (legend.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                legend.forEach { (s, total) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(s.color))
                        Text(s.label, color = Brand.TextPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 5.dp))
                        Text(total, color = Brand.TextMuted, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(start = 5.dp))
                    }
                }
            }
        }
        content()
    }
}

@Composable
private fun TableHeader(cols: List<String>) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp, start = 4.dp, end = 4.dp)) {
        cols.forEachIndexed { i, c ->
            if (i == 0) Text(c, color = Brand.TextMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
            else Text(c, color = Brand.TextMuted, fontSize = 11.sp, textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.width(48.dp))
        }
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Brand.Border))
}

@Composable
private fun NumberCell(text: String, strong: Boolean = false) {
    Text(
        text, color = if (strong) Brand.TextPrimary else Brand.TextSecondary, fontSize = 13.sp, textAlign = TextAlign.End, maxLines = 1,
        fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.width(48.dp),
    )
}

@Composable
private fun Empty(text: String) {
    Text(text, color = Brand.TextMuted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp))
}

/**
 * Stacked or grouped bars on one axis. Tapping a column shows its figures underneath (a phone has
 * no hover). Bars have 4dp rounded tops and a 2dp gap between stacked segments and grouped bars.
 */
@Composable
private fun BarChart(
    data: List<MonthValues>,
    series: List<Series>,
    grouped: Boolean,
    axis: (Double) -> String,
    value: (Double) -> String,
    footer: ((MonthValues) -> Pair<String, String>)?,
) {
    var selected by remember(data.map { it.key }) { mutableStateOf<Int?>(null) }
    fun colMax(m: MonthValues) = if (grouped) series.maxOfOrNull { m.values[it.key] ?: 0.0 } ?: 0.0 else series.sumOf { m.values[it.key] ?: 0.0 }
    val top = AnalyticsCalc.niceCeil(data.maxOfOrNull { colMax(it) } ?: 0.0)
    val grid = Brand.Border
    val highlight = Brand.Field
    val summary = data.joinToString("; ") { m -> m.title + ": " + series.joinToString(", ") { "${it.label} ${value(m.values[it.key] ?: 0.0)}" } }

    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(150.dp)) {
        Column(modifier = Modifier.width(40.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
            listOf(top, top / 2, 0.0).forEach { Text(axis(it), color = Brand.TextMuted, fontSize = 10.sp, maxLines = 1, modifier = Modifier.padding(end = 6.dp)) }
        }
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 6.dp)
                .semantics { contentDescription = summary }
                .pointerInput(data.size) {
                    detectTapGestures { o ->
                        if (data.isNotEmpty()) {
                            val i = (o.x / (size.width / data.size.toFloat())).toInt().coerceIn(0, data.size - 1)
                            selected = if (selected == i) null else i
                        }
                    }
                },
        ) {
            val h = size.height
            val slot = size.width / data.size.coerceAtLeast(1)
            val gap = 2.dp.toPx()
            val r = 4.dp.toPx()
            selected?.let { drawRect(highlight, topLeft = Offset(slot * it, 0f), size = Size(slot, h)) }
            listOf(0f, 0.5f, 1f).forEach { f -> drawLine(grid, Offset(0f, h * f), Offset(size.width, h * f), strokeWidth = 1.dp.toPx()) }
            val groupW = minOf(slot * 0.66f, (if (grouped) 40.dp else 26.dp).toPx())
            data.forEachIndexed { i, m ->
                val cx = slot * i + slot / 2
                if (grouped) {
                    val barW = (groupW - gap * (series.size - 1)) / series.size
                    series.forEachIndexed { j, s ->
                        val v = m.values[s.key] ?: 0.0
                        if (v > 0) {
                            val bh = maxOf(1f, (v / top).toFloat() * h)
                            val x = cx - groupW / 2 + j * (barW + gap)
                            drawPath(roundedTop(x, h - bh, barW, bh, r), s.color)
                        }
                    }
                } else {
                    var base = h
                    val shown = series.filter { (m.values[it.key] ?: 0.0) > 0 }
                    shown.forEachIndexed { j, s ->
                        val bh = maxOf(1f, ((m.values[s.key] ?: 0.0) / top).toFloat() * h)
                        val y = base - bh
                        if (j == shown.lastIndex) drawPath(roundedTop(cx - groupW / 2, y, groupW, bh, r), s.color)
                        else drawRect(s.color, topLeft = Offset(cx - groupW / 2, y), size = Size(groupW, bh))
                        base = y - gap
                    }
                }
            }
        }
    }
    Row(modifier = Modifier.fillMaxWidth().padding(start = 40.dp)) {
        data.forEachIndexed { i, m ->
            Text(
                // Twelve columns on a phone: three letters keep every label on one line.
                if (data.size > 6) m.label.take(3) else m.label,
                color = if (selected == i) Brand.TextPrimary else Brand.TextMuted, fontSize = 10.sp, maxLines = 1, softWrap = false,
                textAlign = TextAlign.Center, fontWeight = if (selected == i) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
        }
    }
    val m = selected?.let { data.getOrNull(it) }
    if (m == null) {
        Text("Tap a bar for the figures", color = Brand.TextFaint, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp, start = 40.dp))
    } else {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(10.dp)).background(Brand.Field).padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(m.title, color = Brand.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            series.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(s.color))
                    Text(s.label, color = Brand.TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f).padding(start = 6.dp))
                    Text(value(m.values[s.key] ?: 0.0), color = Brand.TextPrimary, fontSize = 12.sp)
                }
            }
            footer?.invoke(m)?.let { (label, v) ->
                Row {
                    Text(label, color = Brand.TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f).padding(start = 15.dp))
                    Text(v, color = Brand.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun roundedTop(x: Float, y: Float, w: Float, h: Float, r: Float): Path {
    val rr = minOf(r, h, w / 2)
    return Path().apply {
        addRoundRect(RoundRect(Rect(x, y, x + w, y + h), topLeft = CornerRadius(rr), topRight = CornerRadius(rr)))
    }
}
