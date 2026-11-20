package com.tertiaryinfotech.hrportal.data

import com.tertiaryinfotech.hrportal.util.Fmt

/** One Home tile: label, figure and the optional line under it. */
data class HomeTile(val label: String, val value: String, val hint: String? = null)

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

    /** Accountants: claims waiting, then this month's bank income and expenses. */
    fun finance(f: FinanceKpis): List<HomeTile> = listOf(
        HomeTile(
            "Pending expense claims", f.pendingClaims.toString(),
            if (f.pendingClaims > 0) "${Fmt.money(f.pendingClaimsAmount)} waiting for approval" else "Nothing waiting",
        ),
        HomeTile("Income, ${f.monthLabel}", Fmt.money(f.incomeMonth), "${Fmt.money(f.incomeYtd)} in ${f.year} so far"),
        HomeTile("Expenses, ${f.monthLabel}", Fmt.money(f.expensesMonth), "${Fmt.money(f.expensesYtd)} in ${f.year} so far"),
    )

    /** Trainer Admins: the same three counts as the tiles on Trainer Admin. */
    fun trainers(t: TrainerKpis): List<HomeTile> = listOf(
        HomeTile("Trainers", t.total.toString(), "All trainers on record"),
        HomeTile("ACLP trainers", t.aclp.toString(), "Hold the ACLP"),
        HomeTile("Non-ACLP trainers", t.nonAclp.toString(), "No ACLP on record"),
    )
}
