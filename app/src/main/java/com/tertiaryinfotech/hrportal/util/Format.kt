package com.tertiaryinfotech.hrportal.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Shared formatting for ISO dates and SGD money coming off the API. Mirrors `Theme/Formatters.swift`. */
object Fmt {

    private val isoParsers: List<SimpleDateFormat> = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd",
    ).map { pattern ->
        SimpleDateFormat(pattern, Locale.US).apply {
            if (pattern.endsWith("'Z'") || pattern == "yyyy-MM-dd") timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    fun parse(iso: String?): Date? {
        if (iso.isNullOrBlank()) return null
        for (p in isoParsers) {
            try {
                return p.parse(iso)
            } catch (_: Exception) { /* try next */ }
        }
        return null
    }

    /** Medium date, e.g. "14 Jun 2026". Returns an em dash for null/unparseable input. */
    fun date(iso: String?, short: Boolean = false): String {
        val d = parse(iso) ?: return "—"
        val pattern = if (short) "d/M/yy" else "d MMM yyyy"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(d)
    }

    fun money(value: Double, currency: String = "SGD"): String {
        return try {
            NumberFormat.getCurrencyInstance(Locale("en", "SG")).apply {
                this.currency = Currency.getInstance(currency)
                maximumFractionDigits = 2
            }.format(value)
        } catch (_: Exception) {
            String.format(Locale.US, "%.2f", value)
        }
    }

    fun days(value: Double): String {
        val s = if (value % 1.0 == 0.0) String.format(Locale.US, "%.0f", value)
        else String.format(Locale.US, "%.1f", value)
        return "$s day${if (value == 1.0) "" else "s"}"
    }

    /** Plain number: drops the decimal when whole (1.0 -> "1", 1.5 -> "1.5"). */
    fun num(value: Double): String =
        if (value % 1.0 == 0.0) String.format(Locale.US, "%.0f", value)
        else String.format(Locale.US, "%.1f", value)
}
