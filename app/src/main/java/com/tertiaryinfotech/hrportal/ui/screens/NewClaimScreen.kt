package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.ApiException
import com.tertiaryinfotech.hrportal.data.ExpenseCategory
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.ui.components.AttachmentSection
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.PickedAttachment
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.PremierField
import com.tertiaryinfotech.hrportal.ui.components.StatusBanner
import com.tertiaryinfotech.hrportal.ui.components.encodeAttachment
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Submit an expense or medical claim with a receipt (iOS `NewClaimView`). The backend files the
 * receipt in the employee's own Google Drive folder ("Expense Claims" or "Medical Claims") and
 * raises the claim for approval.
 */
@Composable
fun NewClaimScreen(nav: NavController) {
    var isMedical by remember { mutableStateOf(false) }
    var attachment by remember { mutableStateOf<PickedAttachment?>(null) }
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    val todayMillis = remember { System.currentTimeMillis() }
    var dateMillis by remember { mutableStateOf(todayMillis) }
    var categoryId by remember { mutableStateOf<String?>(null) }
    var categories by remember { mutableStateOf<List<ExpenseCategory>>(emptyList()) }
    var submitting by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDate by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val df = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    LaunchedEffect(Unit) {
        categories = try {
            HrmsApi.expenses().categories.filter { !it.code.equals("MEDICAL", ignoreCase = true) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    val amount = amountText.replace(',', '.').toDoubleOrNull()
    val canSubmit = attachment != null && description.isNotBlank() && (amount ?: 0.0) > 0 &&
        (isMedical || categoryId != null)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card {
            CardHeading("Claim type")
            Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ClaimTypeButton("Expense", Icons.Filled.CreditCard, !isMedical, Modifier.weight(1f)) { isMedical = false }
                ClaimTypeButton("Medical", Icons.Filled.MedicalServices, isMedical, Modifier.weight(1f)) { isMedical = true }
            }
        }

        Card {
            CardHeading("Receipt photo")
            Box(Modifier.padding(top = 10.dp)) {
                AttachmentSection(
                    attachment = attachment,
                    onChange = { attachment = it },
                    onError = { error = it },
                )
            }
        }

        Card {
            CardHeading("Details")
            Column(modifier = Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (isMedical) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MedicalServices, contentDescription = null, tint = Brand.Pink, modifier = Modifier.size(18.dp))
                        Text("Filed under Medical Claim", color = Brand.TextPrimary.copy(alpha = 0.85f), fontSize = 15.sp)
                    }
                } else {
                    CategoryPicker(categories, categoryId) { categoryId = it }
                }

                PremierField(
                    title = "Description (e.g. Clinic visit, taxi receipt)",
                    icon = Icons.AutoMirrored.Filled.Notes,
                    value = description, onValueChange = { description = it },
                )
                PremierField(
                    title = "Amount (SGD)",
                    icon = Icons.Filled.AttachMoney,
                    value = amountText,
                    onValueChange = { v -> amountText = v.filter { it.isDigit() || it == '.' || it == ',' } },
                    keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().height(44.dp).clickable { showDate = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Brand.TextPrimary.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
                    Text("Receipt date", color = Brand.TextPrimary.copy(alpha = 0.85f), fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text(Fmt.date(df.format(Date(dateMillis))), color = Brand.Accent, fontSize = 15.sp)
                }
            }
        }

        error?.let { StatusBanner(isError = true, text = it) }

        PremierButton(
            title = if (submitted) "Submitted ✓" else "Submit claim",
            icon = if (submitted) null else Icons.AutoMirrored.Filled.Send,
            loading = submitting,
            enabled = canSubmit && !submitted,
        ) {
            val picked = attachment ?: return@PremierButton
            val value = amount ?: return@PremierButton
            error = null
            submitting = true
            scope.launch {
                try {
                    val encoded = encodeAttachment(context, picked, onTooLarge = { error = "That file is over the 5 MB limit." })
                    if (encoded == null) {
                        if (error == null) error = "Could not read the photo. Try another one."
                    } else {
                        HrmsApi.submitClaim(
                            claimType = if (isMedical) "MEDICAL" else "EXPENSE",
                            categoryId = if (isMedical) null else categoryId,
                            description = description.trim(),
                            amount = value,
                            expenseDate = df.format(Date(dateMillis)),
                            receipt = encoded.bytes,
                            fileName = encoded.fileName,
                            mimeType = encoded.mimeType,
                        )
                        submitted = true
                        nav.previousBackStackEntry?.savedStateHandle?.set("claim_submitted", true)
                        delay(700)
                        nav.popBackStack()
                    }
                } catch (e: Exception) {
                    error = (e as? ApiException)?.message ?: "Could not submit your claim."
                } finally {
                    submitting = false
                }
            }
        }

        Text(
            "Your receipt is filed to your personal Google Drive folder under " +
                "${if (isMedical) "Medical Claims" else "Expense Claims"} and sent for approval.",
            color = Brand.TextPrimary.copy(alpha = 0.6f), fontSize = 11.sp,
        )
    }

    if (showDate) {
        DateField(
            initial = dateMillis,
            maxMillis = todayMillis, // receipts can't be dated in the future
            onPick = { dateMillis = it; showDate = false },
            onCancel = { showDate = false },
        )
    }
}

@Composable
private fun CardHeading(text: String) {
    Text(text, color = Brand.TextPrimary.copy(alpha = 0.9f), fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
}

@Composable
private fun ClaimTypeButton(title: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(Brand.SmallCorner.dp))
            .background(if (selected) Brand.AccentGradient else SolidColor(Brand.Field))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val content = if (selected) Color.White else Brand.TextPrimary
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            Text(title, color = content, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun CategoryPicker(categories: List<ExpenseCategory>, selectedId: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(Brand.Corner.dp)
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Brand.ControlHeight.dp)
                .clip(shape)
                .background(Brand.Field)
                .border(1.dp, Brand.FieldBorder, shape)
                .clickable { expanded = true }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.Label, contentDescription = null, tint = Brand.TextSecondary, modifier = Modifier.size(20.dp))
            Text(
                categories.firstOrNull { it.id == selectedId }?.name ?: "Select category",
                color = if (selectedId == null) Brand.TextMuted else Brand.TextPrimary,
                fontSize = 16.sp, modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = Brand.TextMuted, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { c ->
                DropdownMenuItem(text = { Text(c.name) }, onClick = { onSelect(c.id); expanded = false })
            }
        }
    }
}
