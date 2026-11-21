package com.tertiaryinfotech.hrportal.data

import com.tertiaryinfotech.hrportal.util.Fmt

/** One Home tile: label, figure and the optional line under it. */
data class HomeTile(val label: String, val value: String, val hint: String? = null, val id: String = label)

/**
 * 1.16 Home tiles from `/api/mobile/summary`, worded as the web dashboard (`stats-cards.tsx`,
 * `role-kpis.tsx`). Pure, so it is unit-tested; the screen only draws.
 */
object HomeTiles {
    /**
     * The person's own leave tiles exactly as the server sent them (the decision-email summary), or
     * null when absent so Home keeps its own balance tiles. Interns have no paid MC, so "MC taken"
     * gets "Sick leave is No Pay Leave" when the server gave no hint.
     */
    fun leave(s: DashboardSummary, isIntern: Boolean): List<HomeTile>? {
        val stats = s.leaveSummary?.filter { it.label.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: return null
        return stats.map { st ->
            val hint = st.hint?.takeIf { it.isNotBlank() }
                ?: if (isIntern && st.label == "MC taken") "Sick leave is No Pay Leave" else null
            HomeTile(st.label, st.value, hint)
        }
    }

    /**
     * Accountants (iOS `financeCard`): claims waiting, this month's bank income and expenses with their
     * credit / debit counts, then Bank statements and Bank reconciliation when the server sends them.
     * Each tile's [HomeTile.id] names its row.
     */
    fun finance(f: FinanceKpis): List<HomeTile> {
        fun count(n: Int?, noun: String) = n?.let { "$it $noun${if (it == 1) "" else "s"} · " } ?: ""
        val rows = mutableListOf(
            HomeTile(
                "Pending expense claims", f.pendingClaims.toString(),
                if (f.pendingClaims > 0) "${Fmt.money(f.pendingClaimsAmount)} waiting for approval" else "Nothing waiting",
                id = "pending",
            ),
            HomeTile("Income, ${f.monthLabel}", Fmt.money(f.incomeMonth),
                count(f.incomeCountMonth, "credit") + "${Fmt.money(f.incomeYtd)} in ${f.year}", id = "income"),
            HomeTile("Expenses, ${f.monthLabel}", Fmt.money(f.expensesMonth),
                count(f.expenseCountMonth, "debit") + "${Fmt.money(f.expensesYtd)} in ${f.year}", id = "expenses"),
        )
        f.statementImports?.let { n ->
            rows += HomeTile("Bank statements", "$n",
                f.statementLatestDate?.let { "Imported up to ${Fmt.date(it)}" } ?: "None imported yet", id = "statements")
        }
        if (f.statementImports != null || f.reconciliation != null) {
            val r = f.reconciliation
            rows += if (r != null) {
                HomeTile(
                    "Bank reconciliation", if (r.toReview > 0) "${r.toReview} to review" else "All clear",
                    "${Fmt.date(r.periodStart)} – ${Fmt.date(r.periodEnd)} · ${r.matched} matched" +
                        if (r.status.equals("COMPLETED", ignoreCase = true)) "" else " · ${r.status.lowercase().replaceFirstChar { it.uppercase() }}",
                    id = "reconciliation",
                )
            } else {
                HomeTile("Bank reconciliation", "Not run", "Run it on the web: Accounting → Reconciliation", id = "reconciliation")
            }
        }
        return rows
    }

    /** Trainer Admins: the same three counts as the tiles on Trainer Admin. */
    fun trainers(t: TrainerKpis): List<HomeTile> = listOf(
        HomeTile("Trainers", t.total.toString(), "All trainers on record"),
        HomeTile("ACLP trainers", t.aclp.toString(), "Hold the ACLP"),
        HomeTile("Non-ACLP trainers", t.nonAclp.toString(), "No ACLP on record"),
    )
}
