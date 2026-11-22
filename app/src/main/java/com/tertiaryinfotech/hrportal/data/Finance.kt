package com.tertiaryinfotech.hrportal.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

// The details behind the accountant Home's Finance card (Alfred, 22 Nov 2026: "click on the expenses, income and
// bank reconciliation, bank statement to see the details"; iOS `Models/FinanceModels.swift`).
// Income / expenses and statements: /api/mobile/finance/*; reconciliation: the web's /api/accounting/reconciliation
// (runs) and /api/accounting/reconciliation/{id} (lines). Admin / accountant only, enforced by the server. Money
// comes as a number on the mobile routes and as a Decimal string on the accounting routes, so it is decoded leniently.

/** A number or a numeric string ("1234.50"); null when absent, null or unreadable. */
object FlexOptionalNumberSerializer : KSerializer<Double?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexOptionalNumber", PrimitiveKind.DOUBLE).nullable

    override fun deserialize(decoder: Decoder): Double? {
        val jsonDecoder = decoder as? JsonDecoder ?: return runCatching { decoder.decodeDouble() }.getOrNull()
        val element = jsonDecoder.decodeJsonElement()
        if (element is JsonNull) return null
        val p = element as? JsonPrimitive ?: return null
        return p.doubleOrNull ?: p.content.toDoubleOrNull()
    }

    override fun serialize(encoder: Encoder, value: Double?) {
        if (value == null) encoder.encodeNull() else encoder.encodeDouble(value)
    }
}

/** Income = bank credits, Expenses = bank debits. [webPath] is the web page that records them in QuickBooks. */
enum class BankDirection(val param: String, val title: String, val noun: String, val webPath: String) {
    CREDIT("CREDIT", "Income", "credit", "accounting/income-tracking"),
    DEBIT("DEBIT", "Expenses", "debit", "accounting/expense-tracking"),
}

/** `GET /api/mobile/finance/transactions?direction=&month=`: one month of bank credits or debits. */
@Serializable
data class BankTransactionsMonth(
    val month: String = "",
    val direction: String = "",
    val count: Int = 0,
    @Serializable(with = FlexNumberSerializer::class) val total: Double = 0.0,
    val transactions: List<BankTransaction> = emptyList(),
)

@Serializable
data class BankTransaction(
    val id: String,
    /** yyyy-MM-dd (or an ISO date-time; see [date]). */
    val paymentDate: String = "",
    val title: String = "",
    @Serializable(with = FlexNumberSerializer::class) val amount: Double = 0.0,
    val direction: String = "",
    val type: String? = null,
    val paymentType: String? = null,
    val paymentRef: String? = null,
    val invoiceNo: String? = null,
    val status: String? = null,
    val remarks: String? = null,
    val rawDescription: String? = null,
    val statementRef: String? = null,
) {
    /** The plain `yyyy-MM-dd` of [paymentDate]. */
    val date: String get() = paymentDate.take(10)

    /** The bank's title, else the first line of the raw statement text. */
    val displayTitle: String
        get() = title.trim().ifEmpty { rawDescription?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Bank transaction" }

    /** Search over title, statement text, type, references and remarks (case-insensitive). */
    fun matches(query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        return listOfNotNull(title, rawDescription, type, paymentRef, invoiceNo, remarks).any { it.lowercase().contains(q) }
    }
}

/** `GET /api/mobile/finance/statements`: one entry per bank statement import, newest first. */
@Serializable
data class StatementImportsResponse(val imports: List<StatementImport> = emptyList())

@Serializable
data class StatementImport(
    val batchId: String,
    val importedAt: String? = null,
    /** yyyy-MM-dd: first and last bank date in the import. */
    val from: String? = null,
    val to: String? = null,
    val credits: Int = 0,
    val debits: Int = 0,
    @Serializable(with = FlexNumberSerializer::class) val creditTotal: Double = 0.0,
    @Serializable(with = FlexNumberSerializer::class) val debitTotal: Double = 0.0,
    val statementRef: String? = null,
) {
    val lines: Int get() = credits + debits
}

/** `GET /api/accounting/reconciliation`: the latest runs, newest first. */
@Serializable
data class ReconciliationRunsResponse(val runs: List<ReconciliationRun> = emptyList())

@Serializable
data class ReconciliationRun(
    val id: String,
    val periodStart: String = "",
    val periodEnd: String = "",
    val status: String = "",
    val trigger: String? = null,
    val bankAccountLabel: String? = null,
    val qbAccountName: String? = null,
    val bankCount: Int = 0,
    val qbCount: Int = 0,
    val matchedCount: Int = 0,
    val mismatchCount: Int = 0,
    val unmatchedBankCount: Int = 0,
    val unmatchedQbCount: Int = 0,
    val reviewedCount: Int = 0,
    @Serializable(with = FlexNumberSerializer::class) val matchRate: Double = 0.0,
    @Serializable(with = FlexOptionalNumberSerializer::class) val bankClosingBalance: Double? = null,
    @Serializable(with = FlexOptionalNumberSerializer::class) val qbClosingBalance: Double? = null,
    val error: String? = null,
    val createdAt: String? = null,
)

/** `GET /api/accounting/reconciliation/{id}`: the run and every line (bank, QuickBooks or both). */
@Serializable
data class ReconciliationDetail(val run: ReconciliationRun, val items: List<ReconciliationItem> = emptyList())

@Serializable
data class ReconciliationItem(
    val id: String,
    val status: String = "",
    val matchMethod: String? = null,
    val bankDate: String? = null,
    @Serializable(with = FlexOptionalNumberSerializer::class) val bankAmount: Double? = null,
    val bankDirection: String? = null,
    val bankDescription: String? = null,
    val qbTxnType: String? = null,
    val qbDate: String? = null,
    @Serializable(with = FlexOptionalNumberSerializer::class) val qbAmount: Double? = null,
    val qbDirection: String? = null,
    val qbDocNumber: String? = null,
    val qbPayee: String? = null,
    val qbMemo: String? = null,
    @Serializable(with = FlexOptionalNumberSerializer::class) val amountDifference: Double? = null,
    val dayDifference: Int? = null,
    val reviewed: Boolean = false,
    val reviewNote: String? = null,
) {
    /** Needs a person's eye: not a match and not yet reviewed (the Home tile's "to review"). */
    val needsReview: Boolean get() = status != "MATCHED" && !reviewed
    val date: String? get() = (bankDate ?: qbDate)?.take(10)
    val amount: Double? get() = bankAmount ?: qbAmount
    val direction: String? get() = bankDirection ?: qbDirection
    val displayTitle: String
        get() = listOfNotNull(
            bankDescription?.lineSequence()?.firstOrNull(),
            qbPayee,
            qbDocNumber?.let { "QuickBooks $it" },
        ).firstOrNull { it.isNotBlank() } ?: "Reconciliation line"
}

/** The reconciliation line filters, in the order a reviewer works through them. */
enum class ReconciliationFilter(val title: String) {
    TO_REVIEW("To review"),
    MISMATCH("Amount differs"),
    BANK_ONLY("Bank only"),
    QB_ONLY("QuickBooks only"),
    MATCHED("Matched"),
    ALL("All lines"),
    ;

    fun includes(i: ReconciliationItem): Boolean = when (this) {
        TO_REVIEW -> i.needsReview
        MISMATCH -> i.status == "AMOUNT_MISMATCH"
        BANK_ONLY -> i.status == "UNMATCHED_BANK"
        QB_ONLY -> i.status == "UNMATCHED_QB"
        MATCHED -> i.status == "MATCHED"
        ALL -> true
    }

    companion object {
        fun label(status: String): String = when (status) {
            "MATCHED" -> "Matched"
            "AMOUNT_MISMATCH" -> "Amount differs"
            "UNMATCHED_BANK" -> "Bank only"
            "UNMATCHED_QB" -> "QuickBooks only"
            else -> status.split('_', ' ').filter { it.isNotEmpty() }
                .joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercase() } }
        }

        /** Opens on what needs a person; a run with nothing to review opens on every line. */
        fun initial(items: List<ReconciliationItem>, current: ReconciliationFilter): ReconciliationFilter =
            if (current == TO_REVIEW && items.none { it.needsReview }) ALL else current
    }
}
