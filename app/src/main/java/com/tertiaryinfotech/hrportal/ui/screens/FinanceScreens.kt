package com.tertiaryinfotech.hrportal.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.hrportal.data.ApiException
import com.tertiaryinfotech.hrportal.data.AuthException
import com.tertiaryinfotech.hrportal.data.BankDirection
import com.tertiaryinfotech.hrportal.data.BankTransaction
import com.tertiaryinfotech.hrportal.data.BankTransactionsMonth
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.MoreMenu
import com.tertiaryinfotech.hrportal.data.ReconciliationDetail
import com.tertiaryinfotech.hrportal.data.ReconciliationFilter
import com.tertiaryinfotech.hrportal.data.ReconciliationItem
import com.tertiaryinfotech.hrportal.data.ReconciliationRun
import com.tertiaryinfotech.hrportal.data.StatementImport
import com.tertiaryinfotech.hrportal.ui.components.AsyncContent
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.LoadState
import com.tertiaryinfotech.hrportal.ui.components.StatTile
import com.tertiaryinfotech.hrportal.ui.components.StatusPill
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

// Details behind the accountant Home's Finance card (Alfred, 22 Nov 2026; iOS `Views/FinanceViews.swift`). Read-only:
// recording to QuickBooks, importing statements and running a reconciliation stay on the web, linked at the foot of
// each screen. Routes: finance_income, finance_expenses, finance_statements, finance_reconciliation.

/** The route behind each Finance card row on Home (null: not tappable). Pending claims open Approvals for approvers only. */
fun financeRoute(rowId: String, canApprove: Boolean): String? = when (rowId) {
    "income" -> "finance_income"
    "expenses" -> "finance_expenses"
    "statements" -> "finance_statements"
    "reconciliation" -> "finance_reconciliation"
    "pending" -> if (canApprove) "approvals" else null
    else -> null
}

// MARK: - Income / Expenses

/** One month of bank credits (Income) or debits (Expenses), searchable; a tap shows the whole line. */
@Composable
fun BankTransactionsScreen(direction: BankDirection) {
    var month by rememberSaveable { mutableStateOf(AttendanceMonth.current()) }
    var state by remember { mutableStateOf<LoadState<BankTransactionsMonth>>(LoadState.Loading) }
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<BankTransaction?>(null) }
    val scope = rememberCoroutineScope()
    val load: suspend () -> Unit = {
        if (state !is LoadState.Loaded) state = LoadState.Loading
        attempt { HrmsApi.bankTransactions(direction, month) }?.let { state = it }
    }
    LaunchedEffect(month) { load() }

    AsyncContent(state = state, load = load) { data ->
        val shown = data.transactions.filter { it.matches(query) }
        val tint = if (direction == BankDirection.CREDIT) Brand.Green else Brand.TextPrimary
        FinanceList(scope, load) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScreenHeading(direction.title, Modifier.weight(1f))
                    MonthSwitcher(month) { month = it }
                }
            }
            item { Spacer14() }
            item {
                val label = AttendanceMonth.label(data.month.ifEmpty { month })
                FinanceHeadline(
                    label = "${direction.title} in $label",
                    value = Fmt.money(data.total),
                    caption = "${data.count} bank ${direction.noun}${if (data.count == 1) "" else "s"}",
                    tint = tint,
                    tag = "finance.${direction.param.lowercase()}.total",
                )
            }
            item { Spacer14() }
            item { FinanceSearchField(query, { query = it }, "Payee, reference or type") }
            item { Spacer14() }
            if (shown.isEmpty()) {
                item {
                    EmptyHint(
                        Icons.Filled.Search,
                        if (query.isBlank()) "No bank ${direction.noun}s in ${AttendanceMonth.label(month)}." else "Nothing matches “$query”.",
                    )
                }
            } else {
                if (query.isNotBlank()) {
                    item {
                        Text(
                            "${shown.size} of ${data.count} · ${Fmt.money(shown.sumOf { it.amount })}",
                            color = Brand.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
                        )
                    }
                }
                groupedRows(shown, key = { it.id }) { t ->
                    LineRow(
                        date = AttendanceMonth.shortDay(t.date),
                        title = t.displayTitle,
                        subtitle = listOfNotNull(t.type, t.status).joinToString(" · "),
                        subtitleColor = Brand.TextSecondary,
                        amount = Fmt.money(t.amount),
                        amountColor = tint,
                        tag = "finance.txn.${t.id}",
                    ) { selected = t }
                }
            }
            item { Spacer14() }
            item { WebFooterLink("Record in QuickBooks on the web", direction.webPath) }
        }
    }

    selected?.let { t ->
        FinanceSheet(
            title = t.displayTitle,
            status = { t.status?.takeIf { it.isNotBlank() }?.let { StatusPill(it) } },
            fields = transactionFields(t),
            longTitle = "Bank statement text",
            longText = listOfNotNull(t.rawDescription, t.remarks?.let { "Remarks: $it" })
                .map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n\n").ifEmpty { null },
            onDismiss = { selected = null },
        )
    }
}

private fun transactionFields(t: BankTransaction): List<Pair<String, String>> = buildList {
    add("Date" to AttendanceMonth.dayLabel(t.date))
    add("Amount" to Fmt.money(t.amount))
    add("Direction" to if (t.direction == "CREDIT") "Credit (income)" else "Debit (expense)")
    listOf("Type" to t.type, "Payment" to t.paymentType, "Reference" to t.paymentRef, "Invoice no." to t.invoiceNo, "Statement" to t.statementRef)
        .forEach { (label, value) -> value?.trim()?.takeIf { it.isNotEmpty() }?.let { add(label to it) } }
}

// MARK: - Bank statements

/** Every bank statement import, newest first: the dates it covers and its credits and debits. */
@Composable
fun BankStatementsScreen() {
    var state by remember { mutableStateOf<LoadState<List<StatementImport>>>(LoadState.Idle) }
    val scope = rememberCoroutineScope()
    val load: suspend () -> Unit = {
        if (state !is LoadState.Loaded) state = LoadState.Loading
        attempt { HrmsApi.statementImports() }?.let { state = it }
    }

    AsyncContent(state = state, load = load) { imports ->
        FinanceList(scope, load) {
            item { ScreenHeading("Bank statements") }
            item { Spacer14() }
            item {
                val n = imports.size
                FinanceHeadline(
                    label = "Imported up to",
                    value = imports.mapNotNull { it.to?.take(10) }.maxOrNull()?.let { AttendanceMonth.dayLabel(it) } ?: "None yet",
                    caption = "$n import${if (n == 1) "" else "s"} · ${imports.sumOf { it.lines }} bank lines",
                    tint = Brand.TextPrimary,
                    tag = "finance.statements.total",
                )
            }
            item { Spacer14() }
            if (imports.isEmpty()) {
                item { EmptyHint(Icons.Filled.Description, "No bank statements imported yet.") }
            } else {
                groupedRows(imports, key = { it.batchId }) { s -> StatementRow(s) }
            }
            item { Spacer14() }
            item { WebFooterLink("Open the DBS statements folder (Google Drive)", MoreMenu.BANK_STATEMENTS_FOLDER) }
            item { WebFooterLink("Import a statement on the web", "accounting") }
        }
    }
}

@Composable
private fun StatementRow(s: StatementImport) {
    val period = listOfNotNull(s.from, s.to).map { AttendanceMonth.shortDay(it) }
    val range = if (period.size == 2) "${period[0]} – ${period[1]} ${s.to?.take(4).orEmpty()}" else "No dates"
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp).semantics { testTag = "finance.statement.${s.batchId}" },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(range, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text("Imported ${Fmt.date(s.importedAt)}", color = Brand.TextSecondary, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconLabel(Icons.Filled.NorthEast, "${s.credits} in · ${Fmt.money(s.creditTotal)}", Brand.Green)
            IconLabel(Icons.Filled.SouthEast, "${s.debits} out · ${Fmt.money(s.debitTotal)}", Brand.Red)
        }
        s.statementRef?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = Brand.TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun IconLabel(icon: ImageVector, text: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Text(text, color = tint, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.padding(start = 4.dp))
    }
}

// MARK: - Bank reconciliation

/**
 * The latest reconciliation run (DBS against QuickBooks) and its lines, opening on the ones to review. Earlier runs
 * are one menu away. Reviewing and re-running stay on the web.
 */
@Composable
fun ReconciliationScreen() {
    var runs by remember { mutableStateOf<LoadState<List<ReconciliationRun>>>(LoadState.Idle) }
    var runId by rememberSaveable { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<ReconciliationDetail?>(null) }
    var detailError by remember { mutableStateOf<String?>(null) }
    var filter by rememberSaveable { mutableStateOf(ReconciliationFilter.TO_REVIEW) }
    var selected by remember { mutableStateOf<ReconciliationItem?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun loadDetail() {
        val id = runId ?: run { detail = null; return }
        detailError = null
        if (detail?.run?.id != id) detail = null
        try {
            val d = HrmsApi.reconciliation(id)
            detail = d
            filter = ReconciliationFilter.initial(d.items, filter)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            detailError = failure(e, "Could not load this run.")
        }
    }

    val loadRuns: suspend () -> Unit = {
        if (runs !is LoadState.Loaded) runs = LoadState.Loading
        attempt { HrmsApi.reconciliationRuns() }?.let { r ->
            runs = r
            val list = (r as? LoadState.Loaded)?.value
            if (list != null) {
                if (runId == null || list.none { it.id == runId }) runId = list.firstOrNull()?.id
                loadDetail()
            }
        }
    }

    AsyncContent(state = runs, load = loadRuns) { list ->
        FinanceList(scope, loadRuns) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScreenHeading("Bank reconciliation", Modifier.weight(1f))
                    if (list.size > 1) {
                        RunMenu(list, runId) { id ->
                            if (id != runId) {
                                runId = id
                                scope.launch { loadDetail() }
                            }
                        }
                    }
                }
            }
            item { Spacer14() }
            val d = detail
            when {
                list.isEmpty() -> item { EmptyHint(Icons.Filled.Balance, "No reconciliation has been run yet.") }
                d != null -> {
                    item { ReconciliationSummary(d) }
                    item { Spacer14() }
                    item { FilterMenu(d.items, filter) { filter = it } }
                    item { Spacer14() }
                    val shown = d.items.filter(filter::includes)
                    if (shown.isEmpty()) {
                        item {
                            EmptyHint(
                                Icons.Filled.Verified,
                                if (filter == ReconciliationFilter.TO_REVIEW) "Nothing left to review." else "No ${filter.title.lowercase()} lines.",
                            )
                        }
                    } else {
                        groupedRows(shown, key = { it.id }) { i ->
                            LineRow(
                                date = i.date?.let { AttendanceMonth.shortDay(it) } ?: "—",
                                title = i.displayTitle,
                                subtitle = ReconciliationFilter.label(i.status) + if (i.reviewed) " · Reviewed" else "",
                                subtitleColor = if (i.needsReview) Brand.Gold600 else Brand.TextSecondary,
                                amount = i.amount?.let { Fmt.money(it) } ?: "—",
                                amountColor = if (i.direction == "CREDIT") Brand.Green else Brand.TextPrimary,
                                tag = "reconciliation.item.${i.id}",
                            ) { selected = i }
                        }
                    }
                }
                detailError != null -> item { EmptyHint(Icons.Filled.Warning, detailError.orEmpty()) }
                else -> item {
                    Box(Modifier.fillMaxWidth().padding(top = 30.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Brand.TextPrimary)
                    }
                }
            }
            item { Spacer14() }
            item { WebFooterLink("Review or run a reconciliation on the web", "accounting/reconciliation") }
        }
    }

    selected?.let { i ->
        FinanceSheet(
            title = i.displayTitle,
            status = { LabelPill(ReconciliationFilter.label(i.status), if (i.needsReview) Brand.Gold600 else Brand.Green) },
            fields = reconciliationFields(i),
            longTitle = "Bank statement text",
            longText = i.bankDescription?.trim()?.ifEmpty { null },
            onDismiss = { selected = null },
        )
    }
}

/** "1 Oct – 31 Oct 2026". */
private fun period(r: ReconciliationRun): String =
    "${AttendanceMonth.shortDay(r.periodStart)} – ${AttendanceMonth.shortDay(r.periodEnd)} ${r.periodEnd.take(4)}"

@Composable
private fun ReconciliationSummary(d: ReconciliationDetail) {
    val r = d.run
    val toReview = d.items.count { it.needsReview }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val status = if (r.status.equals("COMPLETED", ignoreCase = true)) "" else
            " · " + r.status.lowercase().replaceFirstChar { it.uppercase() }
        FinanceHeadline(
            label = "DBS against QuickBooks, ${period(r)}",
            value = if (toReview > 0) "$toReview to review" else "All clear",
            caption = String.format(Locale.US, "%.1f", r.matchRate) + "% of ${r.bankCount} bank lines matched · run ${Fmt.date(r.createdAt)}$status",
            tint = if (toReview > 0) Brand.Gold600 else Brand.Green,
            tag = "reconciliation.headline",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("${r.matchedCount}", "Matched", Modifier.weight(1f), Icons.Filled.CheckCircle, Brand.Green)
            StatTile("${r.mismatchCount}", "Amount differs", Modifier.weight(1f), Icons.Filled.Error, Brand.Gold600)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("${r.unmatchedBankCount}", "Bank only", Modifier.weight(1f), Icons.Filled.AccountBalance, Brand.Accent)
            StatTile("${r.unmatchedQbCount}", "QuickBooks only", Modifier.weight(1f), Icons.AutoMirrored.Filled.MenuBook, Brand.Accent)
        }
        r.error?.takeIf { it.isNotBlank() }?.let { Text(it, color = Brand.Red, fontSize = 12.sp) }
    }
}

@Composable
private fun RunMenu(list: List<ReconciliationRun>, current: String?, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { open = true }
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .semantics { testTag = "reconciliation.runs" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.History, contentDescription = null, tint = Brand.Accent, modifier = Modifier.size(18.dp))
            Text("Runs", color = Brand.Accent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Brand.Dialog) {
            list.forEach { r ->
                DropdownMenuItem(
                    text = { Text(period(r), color = Brand.TextPrimary) },
                    leadingIcon = if (r.id == current) ({ Icon(Icons.Filled.Check, contentDescription = null, tint = Brand.Accent) }) else null,
                    onClick = { open = false; onPick(r.id) },
                )
            }
        }
    }
}

@Composable
private fun FilterMenu(items: List<ReconciliationItem>, current: ReconciliationFilter, onPick: (ReconciliationFilter) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Brand.Surface)
                .border(1.dp, Brand.Border, shape)
                .clickable { open = true }
                .padding(horizontal = 14.dp, vertical = 11.dp)
                .semantics { testTag = "reconciliation.filter" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.FilterList, contentDescription = null, tint = Brand.Accent, modifier = Modifier.size(20.dp))
            Text(
                "${current.title} · ${items.count(current::includes)}", color = Brand.Accent, fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp, modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = Brand.Accent, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Brand.Dialog) {
            ReconciliationFilter.entries.forEach { f ->
                DropdownMenuItem(
                    text = { Text("${f.title} (${items.count(f::includes)})", color = Brand.TextPrimary) },
                    leadingIcon = if (f == current) ({ Icon(Icons.Filled.Check, contentDescription = null, tint = Brand.Accent) }) else null,
                    onClick = { open = false; onPick(f) },
                    modifier = Modifier.semantics { testTag = "reconciliation.filter.${f.name}" },
                )
            }
        }
    }
}

private fun reconciliationFields(i: ReconciliationItem): List<Pair<String, String>> = buildList {
    fun add(label: String, value: String?) { value?.trim()?.takeIf { it.isNotEmpty() }?.let { add(label to it) } }
    add("Bank date", i.bankDate?.let { AttendanceMonth.dayLabel(it.take(10)) })
    add("Bank amount", i.bankAmount?.let { Fmt.money(it) + if (i.bankDirection == "CREDIT") " in" else " out" })
    add("QuickBooks date", i.qbDate?.let { AttendanceMonth.dayLabel(it.take(10)) })
    add("QuickBooks amount", i.qbAmount?.let { Fmt.money(it) + if (i.qbDirection == "CREDIT") " in" else " out" })
    add("QuickBooks entry", listOfNotNull(i.qbTxnType, i.qbDocNumber).joinToString(" "))
    add("Payee", i.qbPayee)
    i.amountDifference?.takeIf { it != 0.0 }?.let { add("Difference", Fmt.money(it)) }
    i.dayDifference?.takeIf { it != 0 }?.let { add("Days apart", "$it") }
    add("Reviewed", if (i.reviewed) i.reviewNote?.let { "Yes: $it" } ?: "Yes" else "Not yet")
}

// MARK: - Shared pieces

/** Runs [block] into a [LoadState]; null when cancelled (the screen went away), so nothing is overwritten. */
private suspend fun <T> attempt(block: suspend () -> T): LoadState<T>? = try {
    LoadState.Loaded(block())
} catch (e: CancellationException) {
    null
} catch (e: Exception) {
    LoadState.Failed(failure(e, "Could not load."))
}

private fun failure(e: Exception, fallback: String): String = when (e) {
    is ApiException, is AuthException -> e.message ?: fallback
    else -> fallback
}

/** A pull-to-refresh list with the screen padding. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinanceList(scope: CoroutineScope, load: suspend () -> Unit, body: LazyListScope.() -> Unit) {
    var refreshing by remember { mutableStateOf(false) }
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { scope.launch { refreshing = true; load(); refreshing = false } },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), content = body)
    }
}

@Composable
private fun ScreenHeading(text: String, modifier: Modifier = Modifier) {
    Text(text, color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = modifier)
}

/** One headline figure with its caption, for the top of a finance screen. */
@Composable
private fun FinanceHeadline(label: String, value: String, caption: String, tint: Color, tag: String) {
    Card(modifier = Modifier.semantics { testTag = tag }) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = Brand.TextSecondary, fontSize = 14.sp)
            Text(value, color = tint, fontWeight = FontWeight.Bold, fontSize = 28.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(caption, color = Brand.TextSecondary, fontSize = 12.sp)
        }
    }
}

/** Rows drawn as one card: the first and last rounded, hairlines between. */
private fun <T> LazyListScope.groupedRows(rows: List<T>, key: (T) -> Any, row: @Composable (T) -> Unit) {
    itemsIndexed(rows, key = { _, r -> key(r) }) { i, r ->
        val c = Brand.CardCorner.dp
        val shape = RoundedCornerShape(
            topStart = if (i == 0) c else 0.dp, topEnd = if (i == 0) c else 0.dp,
            bottomStart = if (i == rows.lastIndex) c else 0.dp, bottomEnd = if (i == rows.lastIndex) c else 0.dp,
        )
        Column(Modifier.fillMaxWidth().clip(shape).background(Brand.Surface)) {
            if (i > 0) Box(Modifier.fillMaxWidth().padding(start = 14.dp).height(1.dp).background(Brand.TextPrimary.copy(alpha = 0.08f)))
            row(r)
        }
    }
}

/** Date, title over a subtitle, and the amount on the right; a tap opens the detail sheet. */
@Composable
private fun LineRow(
    date: String,
    title: String,
    subtitle: String,
    subtitleColor: Color,
    amount: String,
    amountColor: Color,
    tag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .semantics { testTag = tag },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(date, color = Brand.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.width(48.dp))
        Column(Modifier.padding(start = 8.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = subtitleColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(amount, color = amountColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun FinanceSearchField(value: String, onChange: (String) -> Unit, placeholder: String) {
    TextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, color = Brand.TextMuted) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Brand.TextSecondary) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().semantics { testTag = "finance.search" },
        shape = RoundedCornerShape(Brand.SmallCorner.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Brand.Field,
            unfocusedContainerColor = Brand.Field,
            focusedTextColor = Brand.TextPrimary,
            unfocusedTextColor = Brand.TextPrimary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

/** A web page of the HRMS (a path) or a full URL, for the foot of a read-only screen. */
@Composable
private fun WebFooterLink(title: String, path: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(MoreMenu.url(path))))
                } catch (_: ActivityNotFoundException) { /* no browser installed */ }
            }
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = Brand.Accent, modifier = Modifier.size(16.dp))
        Text(title, color = Brand.Accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun LabelPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/** The whole record: a title with its status, label / value lines and the long statement text (selectable). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinanceSheet(
    title: String,
    status: @Composable () -> Unit,
    fields: List<Pair<String, String>>,
    longTitle: String,
    longText: String?,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Brand.Dialog,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.weight(1f).padding(end = 8.dp))
                status()
            }
            Card(padding = 4, containerColor = Brand.Field) {
                fields.forEach { (label, value) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp)) {
                        Text(label, color = Brand.TextSecondary, fontSize = 14.sp, modifier = Modifier.width(130.dp))
                        Text(value, color = Brand.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    }
                }
            }
            if (longText != null) {
                Text(longTitle, color = Brand.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                SelectionContainer { Text(longText, color = Brand.TextPrimary, fontSize = 14.sp) }
            }
        }
    }
}
