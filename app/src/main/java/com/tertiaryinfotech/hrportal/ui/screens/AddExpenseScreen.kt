package com.tertiaryinfotech.hrportal.ui.screens

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tertiaryinfotech.hrportal.data.ApiException
import com.tertiaryinfotech.hrportal.data.ExpenseCategory
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.ui.components.BrandScaffold
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.components.StatusBanner
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.ui.theme.GradientScreen
import com.tertiaryinfotech.hrportal.util.Fmt
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * New expense claim form. Posts to the existing /api/expenses endpoint; a receipt (optional —
 * the server never requires one, only web's client-side hint does) uploads via the legacy
 * multipart /api/upload route first, then its URL is attached to the claim. Presented as a
 * full-screen dialog, mirroring ApplyLeaveScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    categories: List<ExpenseCategory>,
    onDismiss: () -> Unit,
    onSubmitted: () -> Unit,
) {
    var categoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: "") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var receiptUrl by remember { mutableStateOf<String?>(null) }
    var receiptFileName by remember { mutableStateOf<String?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val df = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val selectedCategory = categories.firstOrNull { it.id == categoryId }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            uploading = true
            error = null
            try {
                val resolver = context.contentResolver
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw ApiException(ApiException.Kind.NETWORK, "Could not read the file.")
                val mime = resolver.getType(uri) ?: "application/octet-stream"
                val name = queryFileName(resolver, uri) ?: "receipt"
                val result = HrmsApi.uploadFile(bytes, name, mime)
                receiptUrl = result.url
                receiptFileName = result.fileName
            } catch (e: Exception) {
                error = (e as? ApiException)?.message ?: "Could not upload the receipt."
            } finally {
                uploading = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        GradientScreen {
            BrandScaffold(title = "New expense", onBack = onDismiss) { inner ->
                Column(
                    modifier = Modifier
                        .padding(inner)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    error?.let { StatusBanner(isError = true, text = it) }

                    FieldLabel("Category")
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        FieldBox(modifier = Modifier.menuAnchor()) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    selectedCategory?.name ?: "Select…",
                                    color = Color.White, modifier = Modifier.weight(1f),
                                )
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White)
                            }
                        }
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            categories.forEach { c ->
                                DropdownMenuItem(text = { Text(c.name) }, onClick = { categoryId = c.id; expanded = false })
                            }
                        }
                    }

                    FieldLabel("Description")
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("e.g. Client lunch", color = Brand.TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                    )

                    FieldLabel("Amount (SGD)")
                    TextField(
                        value = amount,
                        onValueChange = { v -> amount = v.filter { it.isDigit() || it == '.' } },
                        placeholder = { Text("0.00", color = Brand.TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                    )

                    FieldLabel("Date")
                    var showDate by remember { mutableStateOf(false) }
                    FieldBox(modifier = Modifier.clickable { showDate = true }) {
                        Text(Fmt.date(df.format(Date(dateMillis))), color = Color.White)
                    }
                    if (showDate) {
                        DateField(
                            initial = dateMillis,
                            onPick = { dateMillis = it; showDate = false },
                            onCancel = { showDate = false },
                        )
                    }

                    FieldLabel("Receipt (optional)")
                    FieldBox(modifier = Modifier.clickable(enabled = !uploading) { filePicker.launch("*/*") }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AttachFile, contentDescription = null, tint = Brand.TextSecondary)
                            Text(
                                when {
                                    uploading -> "Uploading…"
                                    receiptFileName != null -> receiptFileName!!
                                    else -> "Attach a photo or PDF"
                                },
                                color = if (receiptFileName != null) Color.White else Brand.TextMuted,
                                modifier = Modifier.padding(start = 10.dp).weight(1f),
                            )
                            if (uploading) {
                                CircularProgressIndicator(color = Brand.Primary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    PremierButton(
                        title = "Submit claim", icon = Icons.Filled.Send,
                        loading = submitting,
                        enabled = categoryId.isNotEmpty() && description.isNotEmpty() && amount.toDoubleOrNull() != null && !uploading,
                    ) {
                        val amt = amount.toDoubleOrNull()
                        if (amt == null) { error = "Enter a valid amount."; return@PremierButton }
                        error = null; submitting = true
                        scope.launch {
                            try {
                                HrmsApi.createExpense(categoryId, description, amt, df.format(Date(dateMillis)), receiptUrl, receiptFileName)
                                submitting = false; done = true
                            } catch (ex: Exception) {
                                submitting = false
                                error = (ex as? ApiException)?.message ?: "Could not submit your expense claim."
                            }
                        }
                    }
                }
            }
        }
    }

    if (done) {
        AlertDialog(
            onDismissRequest = onSubmitted,
            confirmButton = { TextButton(onClick = onSubmitted) { Text("Done") } },
            title = { Text("Claim submitted") },
            text = { Text("Your expense claim was submitted for approval.") },
        )
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Brand.Border,
    unfocusedContainerColor = Brand.Border,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedIndicatorColor = Brand.Primary,
    unfocusedIndicatorColor = Color.Transparent,
)

private fun queryFileName(resolver: ContentResolver, uri: Uri): String? {
    var name: String? = null
    resolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cursor.moveToFirst()) name = cursor.getString(idx)
    }
    return name
}
