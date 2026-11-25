package com.tertiaryinfotech.hrportal.data

import com.tertiaryinfotech.hrportal.util.Fmt
import java.util.Locale

/**
 * CPF EZPay submissions (web, 26 Nov 2026): the company's monthly CPF filing statements, archived to the shared Drive
 * folder when payroll is generated from them. Each one lists every employee's NRIC and wages, so only the admin view
 * (ADMIN / HR / MANAGER) and the accountant view see them; the server refuses anyone else (403). Pure, unit-tested in
 * `CpfSubmissionsTest`.
 */
object CpfSubmissions {

    /** Who sees the CPF Submissions list and a payslip's "CPF statement" button: the admin and accountant views. */
    fun canView(shown: ShownRole): Boolean = shown.admin || shown.accountant

    /** The row title: the period ("November 2026"), else the file name. */
    fun title(s: CpfSubmission): String = s.periodLabel?.takeIf { it.isNotBlank() } ?: s.name.ifBlank { "CPF statement" }

    /**
     * File name, upload date and size, whichever are known: "CPF Nov 2026.pdf · Uploaded 26 Nov 2026 · 412 KB".
     * The file name is left out when it is already the title.
     */
    fun subtitle(s: CpfSubmission): String = listOfNotNull(
        s.name.takeIf { it.isNotBlank() && it != title(s) },
        s.uploadedAt?.let { Fmt.parse(it) }?.let { "Uploaded ${Fmt.date(s.uploadedAt)}" },
        s.sizeBytes?.let { fileSize(it) },
    ).joinToString(" · ")

    /** The title of a payslip's statement link: its period, else its file name. */
    fun title(p: PayslipCpfSubmission): String = p.periodLabel?.takeIf { it.isNotBlank() } ?: p.name.ifBlank { "CPF statement" }

    /** "812 B", "412 KB", "1.3 MB". Null or negative sizes are not shown. */
    fun fileSize(bytes: Long): String? = when {
        bytes < 0 -> null
        bytes < 1024 -> "$bytes B"
        bytes < 1024L * 1024 -> "${(bytes + 512) / 1024} KB"
        else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

/** The PDF the shared viewer opens: an authenticated server path and the title shown above it. */
data class PdfDocument(val path: String, val title: String)
