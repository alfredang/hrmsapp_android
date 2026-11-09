package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.ApprovalsResponse
import com.tertiaryinfotech.hrportal.data.EmployeeRecords
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.PendingClaim
import com.tertiaryinfotech.hrportal.data.PendingLeave
import com.tertiaryinfotech.hrportal.ui.components.AsyncContent
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.DecisionButtons
import com.tertiaryinfotech.hrportal.ui.components.FilterTabs
import com.tertiaryinfotech.hrportal.ui.components.LoadState
import com.tertiaryinfotech.hrportal.ui.components.NoteDialog
import com.tertiaryinfotech.hrportal.ui.components.RecordDetailSheet
import com.tertiaryinfotech.hrportal.ui.components.RecordHeaders
import com.tertiaryinfotech.hrportal.ui.components.RecordRow
import com.tertiaryinfotech.hrportal.ui.components.RecordTable
import com.tertiaryinfotech.hrportal.ui.components.RefreshableColumn
import com.tertiaryinfotech.hrportal.ui.components.StatusBanner
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import kotlinx.coroutines.launch

/**
 * Approvals queue (MANAGER/HR/ADMIN only) — pending leave, MC and expense claims from
 * `/api/mobile/approvals`, with Approve / Reject calling the existing web routes. Access is
 * **enforced server-side** (403 for non-admins); the client only shows this screen when
 * `summary.isAdmin`.
 *
 * Layout (web one-line tables, 464200b): filter tabs Leave / MC / Claims with counts, then one line
 * per request — name, status, type, dates, days, applied, reason (truncated). Tapping a line opens a
 * bottom sheet with the full reason and the Approve / Reject buttons.
 */
private enum class ApprovalTab { LEAVE, MC, CLAIMS }

private data class Selected(val kind: HrmsApi.ApprovalKind, val row: RecordRow)

private val CLAIM_HEADERS = RecordHeaders(type = "Category", dates = "Expense date", amount = "Amount", reason = "Description")

@Composable
fun ApprovalsScreen(nav: NavController) {
    var state by remember { mutableStateOf<LoadState<ApprovalsResponse>>(LoadState.Idle) }
    var tab by rememberSaveable { mutableStateOf(ApprovalTab.LEAVE.name) }
    var actingId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Selected?>(null) }
    var rejecting by remember { mutableStateOf<Selected?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        if (state !is LoadState.Loaded) state = LoadState.Loading
        state = try {
            LoadState.Loaded(HrmsApi.approvals())
        } catch (e: Exception) {
            LoadState.Failed(e.message ?: "Could not load.")
        }
    }

    suspend fun decide(target: Selected, approve: Boolean, reason: String?) {
        actingId = target.row.id
        error = null
        try {
            HrmsApi.decide(target.kind, target.row.id, approve, reason)
            selected = null
            load()
        } catch (e: Exception) {
            error = e.message ?: "Could not update the request."
        } finally {
            actingId = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncContent(state = state, load = { load() }) { a ->
            val (mc, leave) = a.leaves.partition { it.leaveCode.uppercase() in EmployeeRecords.MEDICAL_CODES }
            RefreshableColumn(scope, { load() }, Arrangement.spacedBy(14.dp)) {
                Text("Approvals", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                FilterTabs(
                    options = listOf(
                        ApprovalTab.LEAVE.name to "Leave · ${leave.size}",
                        ApprovalTab.MC.name to "MC · ${mc.size}",
                        ApprovalTab.CLAIMS.name to "Claims · ${a.claims.size}",
                    ),
                    selected = tab,
                    onSelect = { tab = it },
                )
                error?.let { StatusBanner(isError = true, text = it) }

                when (ApprovalTab.valueOf(tab)) {
                    ApprovalTab.LEAVE -> LeaveTable(leave, "No leave requests waiting.") {
                        selected = Selected(HrmsApi.ApprovalKind.LEAVE, it)
                    }
                    ApprovalTab.MC -> LeaveTable(mc, "No MC requests waiting.") {
                        selected = Selected(HrmsApi.ApprovalKind.LEAVE, it)
                    }
                    ApprovalTab.CLAIMS -> {
                        if (a.claims.isEmpty()) ApprovalEmptyCard("No expense claims waiting.")
                        else RecordTable(a.claims.map(::claimRow), headers = CLAIM_HEADERS, onOpen = {
                            selected = Selected(HrmsApi.ApprovalKind.CLAIM, it)
                        })
                    }
                }
                Text("Tap a request for the full reason and to approve or reject.", color = Brand.TextMuted, fontSize = 12.sp)
            }
        }
    }

    selected?.let { s ->
        RecordDetailSheet(
            row = s.row,
            onDismiss = { selected = null },
            headers = if (s.kind == HrmsApi.ApprovalKind.CLAIM) CLAIM_HEADERS else RecordHeaders(),
        ) {
            DecisionButtons(
                busy = actingId == s.row.id,
                onApprove = { scope.launch { decide(s, true, null) } },
                onReject = { rejecting = s },
            )
        }
    }

    rejecting?.let { target ->
        NoteDialog(
            title = "Reject request",
            message = "Reject ${target.row.name}'s ${target.row.type.lowercase()} request?",
            confirmLabel = "Reject",
            confirmColor = Brand.Red,
            onConfirm = { reason ->
                rejecting = null
                scope.launch { decide(target, false, reason) }
            },
            onDismiss = { rejecting = null },
        )
    }
}

@Composable
private fun LeaveTable(rows: List<PendingLeave>, empty: String, onOpen: (RecordRow) -> Unit) {
    if (rows.isEmpty()) ApprovalEmptyCard(empty) else RecordTable(rows.map(::leaveRow), onOpen = onOpen)
}

private fun leaveRow(r: PendingLeave) = RecordRow(
    id = r.id,
    name = r.employee,
    status = "PENDING",
    type = r.leaveType,
    dates = Fmt.range(r.startDate, r.endDate),
    amount = Fmt.num(r.days) + dayTypeSuffix(r.dayType),
    applied = r.createdAt?.let { Fmt.date(it) },
    reason = r.reason,
    details = buildList { if (!r.documentUrl.isNullOrBlank()) add("Document" to "Attached (view on the web app)") },
)

private fun claimRow(c: PendingClaim) = RecordRow(
    id = c.id,
    name = c.employee,
    status = "PENDING",
    type = c.category ?: "Claim",
    dates = Fmt.date(c.expenseDate),
    amount = Fmt.money(c.amount),
    applied = c.createdAt?.let { Fmt.date(it) },
    reason = c.description,
    details = buildList { if (!c.receiptUrl.isNullOrBlank()) add("Receipt" to "Attached") },
)

/** " (AM)" / " (PM)" for half days, so the Days column stays one short line. */
internal fun dayTypeSuffix(dayType: String?): String = when (dayType?.uppercase()) {
    "AM_HALF" -> " (AM)"
    "PM_HALF" -> " (PM)"
    else -> ""
}

@Composable
private fun ApprovalEmptyCard(text: String) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.Verified, contentDescription = null, tint = Brand.Green, modifier = Modifier.size(20.dp))
            Text(text, color = Brand.TextSecondary, fontSize = 14.sp)
        }
    }
}
