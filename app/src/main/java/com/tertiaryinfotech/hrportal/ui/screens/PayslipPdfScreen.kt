package com.tertiaryinfotech.hrportal.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.ui.components.BrandScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Renders the authenticated payslip PDF natively. The PDF is downloaded with the session
 * cookie, written to a temp file, and rasterised with the platform [PdfRenderer] — the Android
 * equivalent of the iOS PDFKit viewer. No third-party PDF dependency.
 */
@Composable
fun PayslipPdfScreen(nav: NavController) {
    val payslip = PayslipSelection.current

    BrandScaffold(title = "Payslip", onBack = { nav.popBackStack() }) { inner ->
        Box(modifier = Modifier.padding(inner).fillMaxSize(), contentAlignment = Alignment.Center) {
            if (payslip == null) {
                ErrorState("This payslip is no longer available.")
                return@Box
            }

            val result by produceState<PdfResult>(initialValue = PdfResult.Loading, payslip.id) {
                value = try {
                    val bytes = HrmsApi.downloadPdf(payslip.pdfPath)
                    val bitmaps = withContext(Dispatchers.IO) { renderPdf(bytes) }
                    if (bitmaps.isEmpty()) PdfResult.Error else PdfResult.Ready(bitmaps)
                } catch (_: Exception) {
                    PdfResult.Error
                }
            }

            when (val r = result) {
                is PdfResult.Loading -> CircularProgressIndicator(color = Color.White)
                is PdfResult.Error -> ErrorState("Could not load this payslip PDF.")
                is PdfResult.Ready -> {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                    ) {
                        r.pages.forEach { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Payslip page",
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).background(Color.White),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorState(message: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(40.dp))
        Text(message, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp))
    }
}

private sealed interface PdfResult {
    data object Loading : PdfResult
    data object Error : PdfResult
    data class Ready(val pages: List<Bitmap>) : PdfResult
}

/** Rasterise every page of [bytes] to an ARGB bitmap at ~2x for crisp on-screen text. */
private fun renderPdf(bytes: ByteArray): List<Bitmap> {
    val file = File.createTempFile("payslip", ".pdf")
    file.writeBytes(bytes)
    val pages = mutableListOf<Bitmap>()
    try {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val scale = 2
                        val bmp = Bitmap.createBitmap(page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(AndroidColor.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pages.add(bmp)
                    }
                }
            }
        }
    } finally {
        file.delete()
    }
    return pages
}
