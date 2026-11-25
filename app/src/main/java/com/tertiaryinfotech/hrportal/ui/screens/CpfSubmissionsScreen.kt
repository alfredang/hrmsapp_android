package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.ApiException
import com.tertiaryinfotech.hrportal.data.CpfSubmission
import com.tertiaryinfotech.hrportal.data.CpfSubmissions
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.PdfDocument
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.theme.Brand

/** Holds the CPF statement to open, so the `cpf_pdf` screen can download and render it (as [PayslipSelection]). */
object CpfPdfSelection {
    var current: PdfDocument? = null
}

/** The list, or the server's refusal (403: not an admin or accountant), which shows a polite note, not an error. */
private sealed interface CpfList {
    data class Files(val files: List<CpfSubmission>) : CpfList
    data object NotAllowed : CpfList
}

/**
 * CPF Submissions (admins and accountants; More → Finance → CPF Submissions, and the link at the top of Payslips):
 * the company's CPF EZPay statements from `/api/mobile/cpf-submissions`, latest first as the server sends them.
 * A tap opens the PDF in the payslip viewer, downloaded with the same session.
 */
@Composable
fun CpfSubmissionsScreen(nav: NavController) {
    AsyncListScreen(fetch = {
        try {
            CpfList.Files(HrmsApi.cpfSubmissions().cpfSubmissions)
        } catch (e: ApiException) {
            if (e.status == 403) CpfList.NotAllowed else throw e
        }
    }) { data ->
        item {
            Text(
                "CPF Submissions", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                modifier = Modifier.semantics { testTag = "cpf.heading" },
            )
        }
        item { Spacer14() }
        when (data) {
            is CpfList.NotAllowed -> item {
                EmptyHint(Icons.Filled.Lock, "CPF submissions are for admins and accountants only.")
            }
            is CpfList.Files -> if (data.files.isEmpty()) {
                item { EmptyHint(Icons.Filled.Description, "No CPF submissions yet") }
            } else {
                items(data.files, key = { it.id }) { s ->
                    CpfSubmissionRow(s) {
                        CpfPdfSelection.current = PdfDocument(s.downloadPath, CpfSubmissions.title(s))
                        nav.navigate("cpf_pdf")
                    }
                    Spacer12()
                }
            }
        }
    }
}

@Composable
private fun CpfSubmissionRow(s: CpfSubmission, onOpen: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.semantics { testTag = "cpf.row.${s.id}" }) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Description, contentDescription = null, tint = Brand.Green, modifier = Modifier.size(24.dp))
            Column(
                modifier = Modifier.padding(horizontal = 12.dp).weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    CpfSubmissions.title(s), color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                val sub = CpfSubmissions.subtitle(s)
                if (sub.isNotBlank()) {
                    Text(sub, color = Brand.TextSecondary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(Icons.Filled.FileDownload, contentDescription = "Open the PDF", tint = Brand.Accent, modifier = Modifier.size(22.dp))
        }
    }
}

/** One CPF statement PDF, in the shared payslip viewer. */
@Composable
fun CpfPdfScreen() {
    val doc = CpfPdfSelection.current
    if (doc == null || doc.path.isBlank()) {
        AuthenticatedPdfUnavailable("This CPF statement is no longer available.")
        return
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            doc.title, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )
        AuthenticatedPdfView(
            path = doc.path,
            key = doc.path,
            errorText = "Could not load this CPF statement.",
            pageDescription = "CPF statement page",
        )
    }
}
