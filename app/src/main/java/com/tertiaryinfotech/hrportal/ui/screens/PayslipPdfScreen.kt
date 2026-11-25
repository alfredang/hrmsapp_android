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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.material.icons.filled.Description
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import com.tertiaryinfotech.hrportal.data.CpfSubmissions
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.PdfDocument
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.CollapsibleSection
import com.tertiaryinfotech.hrportal.ui.components.PremierButton
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Renders the authenticated payslip PDF natively. The PDF is downloaded with the session
 * cookie, written to a temp file, and rasterised with the platform [PdfRenderer] — the Android
 * equivalent of the iOS PDFKit viewer. No third-party PDF dependency.
 *
 * The pay breakdown ([PayBreakdown]) sits above the PDF in a section that folds away.
 * Admins and accountants ([showCpf], `CpfSubmissions.canView`) also get a "CPF statement" button when the payslip
 * names the CPF EZPay statement it was generated from; it opens that PDF in the same viewer (`cpf_pdf`).
 */
@Composable
fun PayslipPdfScreen(nav: NavController, showCpf: Boolean = false) {
    val payslip = PayslipSelection.current
    if (payslip == null) {
        AuthenticatedPdfUnavailable("This payslip is no longer available.")
        return
    }
    val cpf = payslip.cpfSubmission?.takeIf { showCpf && it.downloadPath.isNotBlank() }
    Column(modifier = Modifier.fillMaxSize()) {
        // The pay figures above the PDF (Alfred, 26 Nov 2026); folds away to give the PDF the screen.
        CollapsibleSection(
            title = "Pay breakdown",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).semantics { testTag = "payslip.breakdown" },
        ) {
            Card(padding = 14) { PayBreakdown(payslip) }
        }
        if (cpf != null) {
            PremierButton(
                title = "CPF statement",
                icon = Icons.Filled.Description,
                fullWidth = false,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    .semantics { testTag = "payslip.cpfStatement" },
            ) {
                CpfPdfSelection.current = PdfDocument(cpf.downloadPath, CpfSubmissions.title(cpf))
                nav.navigate("cpf_pdf")
            }
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            AuthenticatedPdfView(
                path = payslip.pdfPath,
                key = payslip.id,
                errorText = "Could not load this payslip PDF.",
                pageDescription = "Payslip page",
            )
        }
    }
}

/**
 * The shared viewer: downloads [path] with the session ([HrmsApi.downloadPdf]) and shows every page,
 * with pinch to zoom and drag to pan. Used by payslips and CPF statements.
 */
@Composable
fun AuthenticatedPdfView(path: String, key: Any, errorText: String, pageDescription: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val result by produceState<PdfResult>(initialValue = PdfResult.Loading, key) {
            value = try {
                val bytes = HrmsApi.downloadPdf(path)
                val bitmaps = withContext(Dispatchers.IO) { renderPdf(bytes) }
                if (bitmaps.isEmpty()) PdfResult.Error else PdfResult.Ready(bitmaps)
            } catch (_: Exception) {
                PdfResult.Error
            }
        }

        when (val r = result) {
            is PdfResult.Loading -> CircularProgressIndicator(color = Brand.TextPrimary)
            is PdfResult.Error -> ErrorState(errorText)
            is PdfResult.Ready -> {
                // Pinch to zoom and drag to pan, as with PDFKit on iOS.
                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offset = if (scale == 1f) Offset.Zero else offset + pan
                            }
                        }
                        .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                ) {
                    r.pages.forEach { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = pageDescription,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).background(Color.White),
                        )
                    }
                }
            }
        }
    }
}

/** The viewer's error state on its own, for a document that is no longer selected. */
@Composable
fun AuthenticatedPdfUnavailable(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { ErrorState(message) }
}

@Composable
private fun ErrorState(message: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Brand.TextSecondary,
            modifier = Modifier.size(40.dp))
        Text(message, color = Brand.TextSecondary, fontSize = 14.sp,
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
    val file = File.createTempFile("document", ".pdf")
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
