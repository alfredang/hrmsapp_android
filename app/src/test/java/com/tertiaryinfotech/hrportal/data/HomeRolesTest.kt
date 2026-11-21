package com.tertiaryinfotech.hrportal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 1.18 (iOS 1.20 parity, 22 Nov 2026): role Homes, More = web sidebar, accountants, force update. */
class HomeRolesTest {
    private inline fun <reified T> decode(json: String): T = HrmsJson.decodeFromString(json)

    @Test
    fun accountantMeansAccountantWithoutApproverRole() {
        assertTrue(LeaveRules.isAccountant(listOf("ACCOUNTANT")))
        assertTrue(LeaveRules.isAccountant(listOf("STAFF", "accountant")))
        assertFalse(LeaveRules.isAccountant(listOf("ADMIN", "ACCOUNTANT", "TRAINER_ADMIN")))
        assertFalse(LeaveRules.isAccountant(listOf("INTERN")))
        assertFalse(LeaveRules.isAccountant(null))
        assertTrue(LeaveRules.isApprover(listOf("ADMIN", "STAFF"), "ADMIN"))
        assertFalse(LeaveRules.isApprover(listOf("ACCOUNTANT"), "ACCOUNTANT"))
    }

    @Test
    fun pendingApprovalsSplitLeaveAndMcAndCountOnlyPendingTimeOff() {
        val approvals = decode<ApprovalsResponse>("""{"leaves":[{"id":"1","leaveCode":"AL"},{"id":"2","leaveCode":"MC"},
            {"id":"3","leaveCode":"sl"}],"claims":[{"id":"c1"}]}""")
        val timeOff = decode<List<TimeOffRequest>>("""[{"id":"t1","status":"PENDING"},{"id":"t2","status":"APPROVED"},{"id":"t3","status":"pending"}]""")
        val p = PendingApprovals.from(approvals, timeOff, punches = 3)
        assertEquals(PendingApprovals(leave = 1, mc = 2, timeOff = 2, claims = 1, punches = 3), p)
        assertEquals(9, p.total)
    }

    @Test
    fun versionsCompareNumericallyAndNoLiveVersionNeverForces() {
        assertTrue(AppVersion.compare("1.9", "1.10") < 0)
        assertEquals(0, AppVersion.compare("1.18", "1.18.0"))
        assertTrue(AppVersion.mustUpdate("1.17", "1.18"))
        assertFalse(AppVersion.mustUpdate("1.18", "1.18"))
        assertFalse(AppVersion.mustUpdate("1.19", "1.18"))
        assertFalse(AppVersion.mustUpdate("1.18", null))
        assertFalse(AppVersion.mustUpdate("1.18", ""))
    }

    @Test
    fun financeRowsAddCountsStatementsAndReconciliation() {
        val f = decode<FinanceKpis>("""{"pendingClaims":0,"incomeMonth":140742.99,"incomeYtd":3089763.18,"expensesMonth":364908.01,
            "expensesYtd":3279320.55,"monthLabel":"Nov","year":2026,"incomeCountMonth":172,"expenseCountMonth":107,
            "statementImports":33,"statementLatestDate":"2026-11-16","reconciliation":null}""")
        val rows = HomeTiles.finance(f)
        assertEquals(listOf("pending", "income", "expenses", "statements", "reconciliation"), rows.map { it.id })
        assertTrue(rows[1].hint!!.startsWith("172 credits · "))
        assertTrue(rows[2].hint!!.startsWith("107 debits · "))
        assertEquals("33", rows[3].value)
        assertEquals("Not run", rows[4].value)
        // An older server: only the first three rows.
        val old = decode<FinanceKpis>("""{"pendingClaims":1,"incomeMonth":1,"monthLabel":"Nov","year":2026}""")
        assertEquals(listOf("pending", "income", "expenses"), HomeTiles.finance(old).map { it.id })
        val run = f.copy(reconciliation = FinanceReconciliation("COMPLETED", "2026-10-01", "2026-10-31", 268, 4, ""))
        assertEquals("4 to review", HomeTiles.finance(run).last().value)
    }

    private fun ids(sections: List<MoreMenu.Section>) = sections.associate { s -> s.title to s.rows.map { it.id } }

    @Test
    fun moreFollowsTheWebSidebarForEachRole() {
        val admin = ids(MoreMenu.sections(ShownRole(true, false, false, false, false), trainerView = false, canManageTrainers = true))
        assertEquals(listOf("My Stuff", "People", "Processes", "Finance", "Accounting", "Documents", "System"), admin.keys.toList())
        assertEquals(listOf("staff", "interns", "trainers"), admin["People"])
        assertEquals(listOf("approvals", "leavemgmt", "timeoff", "timesheet"), admin["Processes"])
        assertEquals(listOf("payslips", "claims"), admin["Finance"])

        val staff = ids(MoreMenu.sections(ShownRole(false, false, false, false, false), false, false))
        assertEquals(listOf("AL", "MC", "timeoff", "offinlieu"), staff["Processes"])
        assertEquals(listOf("trainercontacts"), staff["People"])
        assertFalse("Accounting" in staff || "System" in staff)

        val intern = ids(MoreMenu.sections(ShownRole(false, true, true, false, false), false, false))
        assertEquals(listOf("AL", "MC", "timeoff", "clock", "mytimesheet"), intern["Processes"])
        assertEquals("interns: no Payslips (web hideForIntern)", listOf("claims"), intern["Finance"])

        val accountant = ids(MoreMenu.sections(ShownRole(false, false, true, true, true), false, canManageTrainers = true))
        assertEquals(listOf("clock", "mytimesheet"), accountant["Processes"])
        assertEquals(listOf("payslips"), accountant["Finance"])
        assertEquals(listOf("expensetracking", "incometracking", "reconciliation", "statements"), accountant["Accounting"])
        assertEquals("a preview hides the Trainer Admin directory", listOf("trainercontacts"), accountant["People"])

        val trainer = ids(MoreMenu.sections(ShownRole(false, false, false, false, false), trainerView = true, canManageTrainers = true))
        assertEquals(mapOf("My Stuff" to listOf("profile", "calendar"), "People" to listOf("trainers"), "Documents" to listOf("sop")), trainer)
        assertEquals(MoreMenu.BANK_STATEMENTS_FOLDER, MoreMenu.url(MoreMenu.BANK_STATEMENTS_FOLDER))
        assertEquals("https://hrms.tertiaryinfotech.com/sop", MoreMenu.url("sop"))
    }
}
