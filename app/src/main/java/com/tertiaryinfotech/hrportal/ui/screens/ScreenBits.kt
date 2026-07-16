package com.tertiaryinfotech.hrportal.ui.screens

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import java.io.File

@Composable
fun Spacer12() = Spacer(Modifier.height(12.dp))

@Composable
fun Spacer18() = Spacer(Modifier.height(18.dp))

@Composable
fun Spacer14() = Spacer(Modifier.height(14.dp))

/** A white section heading, matching the iOS `.headline` section titles. */
@Composable
fun SectionTitle(text: String) {
    Text(text, color = Brand.TextPrimary.copy(alpha = 0.9f), fontWeight = FontWeight.Bold, fontSize = 17.sp)
}

/** A field label used above form inputs (apply-leave, new-expense, etc.). */
@Composable
fun FieldLabel(text: String) {
    Text(text, color = Brand.TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
}

/** A tappable/static field surface used to host form content (dropdowns, date pickers). */
@Composable
fun FieldBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Brand.Corner.dp))
            .background(Brand.Border)
            .padding(14.dp),
    ) { content() }
}

/** A native Material date picker dialog, clamped to [minMillis] when given. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(initial: Long, minMillis: Long? = null, onPick: (Long) -> Unit, onCancel: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial)
    DatePickerDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(onClick = {
                val picked = state.selectedDateMillis ?: initial
                onPick(if (minMillis != null && picked < minMillis) minMillis else picked)
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}

/** Reads a picked file's display name for upload previews (apply-leave MC doc, new-expense
 *  receipt) — shared since [ContentResolver] queries aren't otherwise exposed per-file. */
fun queryFileName(resolver: ContentResolver, uri: Uri): String? {
    var name: String? = null
    resolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cursor.moveToFirst()) name = cursor.getString(idx)
    }
    return name
}

/** A cache-dir file shared via FileProvider so the camera app can write a captured photo into
 *  this app's sandbox — shared by the new-expense receipt and apply-leave MC document uploads. */
fun createCaptureUri(context: Context): Uri {
    val dir = File(context.cacheDir, "captures").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** "Take Photo" / "Choose from Files" chooser — shown when tapping a document/receipt upload
 *  field, so the same field can offer both a live camera capture and a file/gallery pick. */
@Composable
fun AttachOptionsDialog(onDismiss: () -> Unit, onTakePhoto: () -> Unit, onChooseFile: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add document") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onTakePhoto).padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = Brand.Primary)
                    Text("Take Photo", color = Brand.TextPrimary, modifier = Modifier.padding(start = 14.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onChooseFile).padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.AttachFile, contentDescription = null, tint = Brand.Primary)
                    Text("Choose from Files", color = Brand.TextPrimary, modifier = Modifier.padding(start = 14.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
