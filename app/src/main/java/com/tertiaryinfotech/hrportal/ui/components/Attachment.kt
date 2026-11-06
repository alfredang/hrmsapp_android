package com.tertiaryinfotech.hrportal.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.tertiaryinfotech.hrportal.ui.screens.createCaptureUri
import com.tertiaryinfotech.hrportal.ui.screens.queryFileName
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A photo or PDF the user has chosen but not uploaded yet. Nothing leaves the device until submit,
 *  so removing or retaking one leaves no orphan file on the server. */
data class PickedAttachment(val uri: Uri, val fileName: String, val mimeType: String) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
}

/** What actually gets uploaded for an attachment. */
class EncodedAttachment(val bytes: ByteArray, val fileName: String, val mimeType: String)

private const val MAX_PHOTO_SIDE = 2048
private const val JPEG_QUALITY = 75
private const val MAX_UPLOAD_BYTES = 5 * 1024 * 1024 // the server's /api/upload limit

/**
 * Reads an attachment for upload. Photos are downscaled to 2048px at JPEG quality 75, the same as
 * iOS `receiptJPEGData()`, because a raw camera file can exceed the server's 5 MB limit. PDFs go up
 * unchanged. Returns null with [onTooLarge] called if the result would be rejected anyway.
 */
suspend fun encodeAttachment(
    context: Context,
    attachment: PickedAttachment,
    onTooLarge: () -> Unit,
): EncodedAttachment? = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    if (!attachment.isImage) {
        val bytes = resolver.openInputStream(attachment.uri)?.use { it.readBytes() } ?: return@withContext null
        if (bytes.size > MAX_UPLOAD_BYTES) {
            withContext(Dispatchers.Main) { onTooLarge() }
            return@withContext null
        }
        return@withContext EncodedAttachment(bytes, attachment.fileName, attachment.mimeType)
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(attachment.uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_PHOTO_SIDE) sample *= 2
    val decoded = resolver.openInputStream(attachment.uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return@withContext null

    val longest = maxOf(decoded.width, decoded.height)
    val scaled = if (longest > MAX_PHOTO_SIDE) {
        val ratio = MAX_PHOTO_SIDE.toFloat() / longest
        Bitmap.createScaledBitmap(decoded, (decoded.width * ratio).toInt(), (decoded.height * ratio).toInt(), true)
    } else decoded

    // Bitmap decoding drops EXIF, so a portrait receipt would otherwise upload sideways.
    val rotation = resolver.openInputStream(attachment.uri)?.use { input ->
        when (ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } ?: 0f
    val upright = if (rotation != 0f) {
        Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, Matrix().apply { postRotate(rotation) }, true)
    } else scaled

    val out = ByteArrayOutputStream()
    upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
    EncodedAttachment(out.toByteArray(), attachment.fileName.substringBeforeLast('.') + ".jpg", "image/jpeg")
}

/**
 * Photo section shared by the claim and medical-leave forms (iOS `photoSection` / `mcPhotoSection`):
 * take a photo or pick from the library, then a preview with Retake and Remove. [allowPdf] adds the
 * PDF option Android has always offered, which the server accepts.
 */
@Composable
fun AttachmentSection(
    attachment: PickedAttachment?,
    onChange: (PickedAttachment?) -> Unit,
    onError: (String) -> Unit,
    takeLabel: String = "Take photo",
    baseName: String = "receipt",
    allowPdf: Boolean = true,
) {
    val context = LocalContext.current
    val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    var pendingCapture by remember { mutableStateOf<Uri?>(null) }

    fun pick(uri: Uri) {
        val resolver = context.contentResolver
        onChange(
            PickedAttachment(
                uri = uri,
                fileName = queryFileName(resolver, uri) ?: "$baseName.jpg",
                mimeType = resolver.getType(uri) ?: "image/jpeg",
            ),
        )
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let(::pick) }
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let(::pick) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingCapture
        if (ok && uri != null) onChange(PickedAttachment(uri, "$baseName.jpg", "image/jpeg"))
    }
    fun launchCamera() {
        val uri = createCaptureUri(context)
        pendingCapture = uri
        camera.launch(uri)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else onError("Camera permission is needed to take a photo.")
    }
    fun takePhoto() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            permission.launch(Manifest.permission.CAMERA)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (attachment != null) {
            if (attachment.isImage) {
                AsyncImage(
                    model = attachment.uri,
                    contentDescription = "Selected photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .clip(RoundedCornerShape(Brand.SmallCorner.dp)),
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.Description, contentDescription = null, tint = Brand.TextSecondary)
                    Text(attachment.fileName, color = Brand.TextPrimary, fontSize = 14.sp, maxLines = 1)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (attachment.isImage) {
                    PhotoButton("Retake", Icons.Filled.PhotoCamera, Brand.AccentGradient, Color.White, hasCamera, Modifier.weight(1f)) { takePhoto() }
                }
                PhotoButton("Remove", Icons.Filled.Delete, SolidColor(Brand.Field), Brand.Red, true, Modifier.weight(1f)) { onChange(null) }
            }
        } else {
            PhotoButton(takeLabel, Icons.Filled.PhotoCamera, Brand.AccentGradient, Color.White, hasCamera, Modifier.fillMaxWidth()) { takePhoto() }
            PhotoButton("Choose from library", Icons.Filled.PhotoLibrary, SolidColor(Brand.Field), Brand.TextPrimary, true, Modifier.fillMaxWidth()) { gallery.launch("image/*") }
            if (allowPdf) {
                Text(
                    "Attach a PDF instead", color = Brand.Accent, fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable { pdf.launch("application/pdf") }.padding(vertical = 4.dp),
                )
            }
            if (!hasCamera) {
                Text("Camera not available on this device. Choose from the photo library.", color = Brand.TextMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun PhotoButton(
    title: String,
    icon: ImageVector,
    fill: Brush,
    content: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(Brand.SmallCorner.dp)
    Box(
        modifier = modifier
            .alphaIf(!enabled, 0.45f)
            .height(44.dp)
            .clip(shape)
            .background(fill)
            .clickableIf(enabled, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            Text(title, color = content, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(end = 2.dp))
        }
    }
}
