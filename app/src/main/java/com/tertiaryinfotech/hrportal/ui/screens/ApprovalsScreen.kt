package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.ApprovalsResponse
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.PendingClaim
import com.tertiaryinfotech.hrportal.data.PendingLeave
import com.tertiaryinfotech.hrportal.ui.components.AsyncContent
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.LoadState
import com.tertiaryinfotech.hrportal.ui.components.StatusBanner
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import kotlinx.coroutines.launch

/**
 * Approvals queue (MANAGER/HR/ADMIN only) — pending leave requests and expense claims from
 * `/api/mobile/approvals`, with Approve / Reject actions that call the existing web routes.
 * Ported from the native iOS `ApprovalsView`. Access is **enforced server-side** (the endpoint
 * returns 403 for non-admins); the client only shows this screen when `summary.isAdmin` (see
 * [DashboardScreen] / [MainScaffold]). The two account admins (Tan Soik Ching, Alfred Ang Chew
 * Hoe) hold role ADMIN and therefore see + can act on this queue.
 */
private data class RejectTarget(val kind: HrmsApi.ApprovalKind, val id: String, val who: String)

@Composable
fun ApprovalsScreen(nav: NavController) {
    var state by remember { mutableStateOf<LoadState<ApprovalsResponse>>(LoadState.Idle) }
    var actingId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var rejecting by remember { mutableStateOf<RejectTarget?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        state = LoadState.Loading
        state = try {
            LoadState.Loaded(HrmsApi.approvals())
        } catch (e: Exception) {
            LoadState.Failed(e.message ?: "Could not load.")
        }
    }

    suspend fun decide(kind: HrmsApi.ApprovalKind, id: String, approve: Boolean, reason: String?) {
        actingId = id
        error = null
        try {
            HrmsApi.decide(kind, id, approve, reason)
            load()
        } catch (e: Exception) {
            error = e.message ?: "Could not update the request."
        } finally {
            actingId = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncContent(state = state, load = { load() }) { a ->
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                error?.let { StatusBanner(isError = true, text = it) }

                SectionTitle("Pending leave (${a.leaves.size})")
                if (a.leaves.isEmpty()) ApprovalEmptyCard("No leave requests waiting.")
                a.leaves.forEach { r ->
                    LeaveCard(r, busy = actingId == r.id, actionsEnabled = actingId == null,
                        onApprove = { scope.launch { decide(HrmsApi.ApprovalKind.LEAVE, r.id, true, null) } },
                        onReject = { rejecting = RejectTarget(HrmsApi.ApprovalKind.LEAVE, r.id, r.employee) })
                }

                SectionTitle("Pending claims (${a.claims.size})")
                if (a.claims.isEmpty()) ApprovalEmptyCard("No expense claims waiting.")
                a.claims.forEach { c ->
                    ClaimCard(c, busy = actingId == c.id, actionsEnabled = actingId == null,
                        onApprove = { scope.launch { decide(HrmsApi.ApprovalKind.CLAIM, c.id, true, null) } },
                        onReject = { rejecting = RejectTarget(HrmsApi.ApprovalKind.CLAIM, c.id, c.employee) })
                }
            }
        }
    }

    rejecting?.let { target ->
        AlertDialog(
            onDismissRequest = { rejecting = null; rejectReason = "" },
            title = { Text("Reject request") },
            text = {
                Column {
                    Text("Reject ${target.who}'s request?", color = Brand.TextSecondary)
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        placeholder = { Text("Reason (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val reason = rejectReason
                    val t = target
                    rejecting = null; rejectReason = ""
                    scope.launch { decide(t.kind, t.id, false, reason) }
                }) { Text("Reject", color = Brand.Red) }
            },
            dismissButton = { TextButton(onClick = { rejecting = null; rejectReason = "" }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun LeaveCard(r: PendingLeave, busy: Boolean, actionsEnabled: Boolean, onApprove: () -> Unit, onReject: () -> Unit) {
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(r.employee, color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.clip(CircleShape).background(Brand.Amber.copy(alpha = 0.22f)).padding(horizontal = 10.dp, vertical = 4.dp),
                ) { Text(Fmt.days(r.days), color = Brand.Amber, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
            }
            Text(r.leaveType, color = Brand.Blue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(dateRange(r.startDate, r.endDate), color = Brand.TextPrimary, fontSize = 14.sp)
            r.reason?.takeIf { it.isNotEmpty() }?.let { Text(it, color = Brand.TextMuted, fontSize = 13.sp) }
            ActionRow(busy, actionsEnabled, onApprove, onReject)
        }
    }
}

@Composable
private fun ClaimCard(c: PendingClaim, busy: Boolean, actionsEnabled: Boolean, onApprove: () -> Unit, onReject: () -> Unit) {
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(c.employee, color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text(Fmt.money(c.amount), color = Color(0xFFF97316), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            c.category?.let { Text(it, color = Brand.Blue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
            Text(c.description, color = Brand.TextPrimary, fontSize = 14.sp)
            Text(Fmt.date(c.expenseDate), color = Brand.TextMuted, fontSize = 13.sp)
            ActionRow(busy, actionsEnabled, onApprove, onReject)
        }
    }
}

@Composable
private fun ActionRow(busy: Boolean, enabled: Boolean, onApprove: () -> Unit, onReject: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ActionButton("Approve", Icons.Filled.CheckCircle, Brand.Green, busy = busy, enabled = enabled, modifier = Modifier.weight(1f), onClick = onApprove)
        ActionButton("Reject", Icons.Filled.Cancel, Brand.Red, busy = false, enabled = enabled, modifier = Modifier.weight(1f), onClick = onReject)
    }
}

@Composable
private fun ActionButton(title: String, icon: ImageVector, tint: Color, busy: Boolean, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(Brand.Corner.dp))
            .background(tint.copy(alpha = 0.25f))
            .border(1.dp, tint.copy(alpha = 0.5f), RoundedCornerShape(Brand.Corner.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier.padding()),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(start = 6.dp))
        }
    }
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

private fun dateRange(start: String?, end: String?): String {
    val s = Fmt.date(start); val e = Fmt.date(end)
    return if (s == e) s else "$s → $e"
}
