package com.tertiaryinfotech.hrportal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.hrportal.ui.theme.Brand

/**
 * One leave / MC / time-off / claim record as a single table line (web one-line tables, tertiary-hrms
 * 464200b). Column order is fixed: name, status, type, dates, amount (days or hours), applied, then
 * the reason, which is truncated and opens [RecordDetailSheet] with the full text.
 */
data class RecordRow(
    val id: String,
    val name: String? = null,
    val status: String,
    val type: String,
    val dates: String,
    /** "2", "0.5", "2 h", "$18.40". */
    val amount: String,
    val applied: String? = null,
    val reason: String? = null,
    /** Extra label/value lines shown only in the detail sheet (rejection reason, comment, etc.). */
    val details: List<Pair<String, String>> = emptyList(),
)

/** Header captions, so the same table serves leave (Days / Reason) and claims (Amount / Description). */
data class RecordHeaders(
    val name: String = "Name",
    val status: String = "Status",
    val type: String = "Type",
    val dates: String = "Dates",
    val amount: String = "Days",
    val applied: String = "Applied",
    val reason: String = "Reason",
)

private object Col {
    val name = 150.dp
    val status = 96.dp
    val type = 132.dp
    val dates = 172.dp
    val amount = 64.dp
    val applied = 100.dp
    val reason = 240.dp
}

/**
 * A neat one-line-per-record table in a card. The whole table scrolls sideways as one block (so the
 * columns stay aligned); a fade on the right edge says there is more. Tapping a row, or its reason,
 * calls [onOpen] — normally to show [RecordDetailSheet].
 */
@Composable
fun RecordTable(
    rows: List<RecordRow>,
    onOpen: (RecordRow) -> Unit,
    modifier: Modifier = Modifier,
    showName: Boolean = true,
    headers: RecordHeaders = RecordHeaders(),
) {
    val scroll = rememberScrollState()
    Card(modifier = modifier, padding = 0) {
        Box {
            Column(modifier = Modifier.horizontalScroll(scroll)) {
                Row(
                    modifier = Modifier.background(Brand.SurfaceStrong).padding(horizontal = 6.dp).height(36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showName) HeaderCell(headers.name, Col.name)
                    HeaderCell(headers.status, Col.status)
                    HeaderCell(headers.type, Col.type)
                    HeaderCell(headers.dates, Col.dates)
                    HeaderCell(headers.amount, Col.amount)
                    HeaderCell(headers.applied, Col.applied)
                    HeaderCell(headers.reason, Col.reason)
                }
                rows.forEachIndexed { i, r ->
                    if (i > 0) Box(Modifier.height(1.dp).background(Brand.Border).width(tableWidth(showName)))
                    Row(
                        modifier = Modifier
                            .clickable { onOpen(r) }
                            .padding(horizontal = 6.dp)
                            .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (showName) TextCell(r.name.orEmpty(), Col.name, weight = FontWeight.SemiBold)
                        Box(Modifier.width(Col.status).padding(horizontal = 6.dp)) { StatusPill(r.status) }
                        TextCell(r.type, Col.type)
                        TextCell(r.dates, Col.dates, color = Brand.TextSecondary)
                        TextCell(r.amount, Col.amount, weight = FontWeight.SemiBold)
                        TextCell(r.applied ?: "—", Col.applied, color = Brand.TextSecondary)
                        TextCell(
                            r.reason?.takeIf { it.isNotBlank() } ?: "—", Col.reason,
                            color = if (r.reason.isNullOrBlank()) Brand.TextFaint else Brand.Accent,
                        )
                    }
                }
            }
            if (scroll.canScrollForward) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(28.dp)
                        .fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, Brand.Surface))),
                )
            }
        }
    }
}

private fun tableWidth(showName: Boolean): Dp =
    (if (showName) Col.name else 0.dp) + Col.status + Col.type + Col.dates + Col.amount + Col.applied + Col.reason + 12.dp

@Composable
private fun HeaderCell(text: String, width: Dp) {
    Text(
        text.uppercase(), color = Brand.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        maxLines = 1, modifier = Modifier.width(width).padding(horizontal = 6.dp),
    )
}

@Composable
private fun TextCell(text: String, width: Dp, color: Color = Brand.TextPrimary, weight: FontWeight = FontWeight.Normal) {
    Text(
        text, color = color, fontSize = 13.sp, fontWeight = weight, maxLines = 1, overflow = TextOverflow.Ellipsis,
        softWrap = false, modifier = Modifier.width(width).padding(horizontal = 6.dp),
    )
}

/**
 * Bottom sheet with every field of one [RecordRow] and the full reason text (selectable). [actions]
 * holds the row's buttons, e.g. [DecisionButtons] for a pending request.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailSheet(
    row: RecordRow,
    onDismiss: () -> Unit,
    headers: RecordHeaders = RecordHeaders(),
    actions: (@Composable ColumnScope.() -> Unit)? = null,
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
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(row.name ?: row.type, color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    if (row.name != null) Text(row.type, color = Brand.TextSecondary, fontSize = 14.sp)
                }
                StatusPill(row.status)
            }
            Card(padding = 4, containerColor = Brand.Field) {
                SheetLine(headers.dates, row.dates)
                SheetLine(headers.amount, row.amount)
                row.applied?.let { SheetLine(headers.applied, it) }
                row.details.forEach { (k, v) -> SheetLine(k, v) }
            }
            Text(headers.reason, color = Brand.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            SelectionContainer {
                Text(
                    row.reason?.takeIf { it.isNotBlank() } ?: "No ${headers.reason.lowercase()} given.",
                    color = if (row.reason.isNullOrBlank()) Brand.TextMuted else Brand.TextPrimary, fontSize = 15.sp,
                )
            }
            actions?.invoke(this)
        }
    }
}

@Composable
private fun SheetLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp)) {
        Text(label, color = Brand.TextSecondary, fontSize = 14.sp, modifier = Modifier.width(110.dp))
        Text(value, color = Brand.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}

/** Approve / Reject pair for a pending item. [busy] shows a spinner on Approve. */
@Composable
fun DecisionButtons(busy: Boolean, onApprove: () -> Unit, onReject: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TintButton("Approve", Icons.Filled.CheckCircle, Brand.Green, busy, Modifier.weight(1f), onApprove)
        TintButton("Reject", Icons.Filled.Cancel, Brand.Red, false, Modifier.weight(1f), onReject)
    }
}

/** A soft tinted button: pale fill, coloured stroke, coloured label (readable on the light theme). */
@Composable
fun TintButton(
    title: String,
    icon: ImageVector?,
    tint: Color,
    busy: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(Brand.SmallCorner.dp)
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(shape)
            .background(tint.copy(alpha = 0.10f))
            .border(1.dp, tint.copy(alpha = 0.45f), shape)
            .clickable(enabled = !busy, onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) {
            CircularProgressIndicator(color = tint, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
            if (icon != null) Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(
                title, color = tint, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, softWrap = false,
                modifier = Modifier.padding(start = if (icon != null) 6.dp else 0.dp),
            )
        }
    }
}

/** Compact filter tabs with counts ("Leave 3 · MC 1 · Claims 2"): capsule chips, selected one filled. */
@Composable
fun FilterTabs(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(CircleShape)
                    .background(if (on) Brand.Primary else Brand.Surface)
                    .border(1.dp, if (on) Brand.Primary else Brand.Border, CircleShape)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label, color = if (on) Color.White else Brand.TextPrimary, fontSize = 14.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1,
                )
            }
        }
    }
}

/**
 * Confirm dialog with an optional note, used for Reject (reason) and Approve-with-comment. The note
 * is passed to [onConfirm] as typed (may be blank).
 */
@Composable
fun NoteDialog(
    title: String,
    message: String,
    confirmLabel: String,
    confirmColor: Color,
    placeholder: String = "Reason (optional)",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Brand.Dialog,
        title = { Text(title, color = Brand.TextPrimary) },
        text = {
            Column {
                Text(message, color = Brand.TextSecondary)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text(placeholder) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(note) }) { Text(confirmLabel, color = confirmColor) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
