package com.tertiaryinfotech.hrportal.data

import com.tertiaryinfotech.hrportal.push.routeForLink
import com.tertiaryinfotech.hrportal.ui.screens.financeRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** iOS 1.21 parity (iOS commit 1895005): finance details, Team rows, push links with `?request=`. */
class Parity121Test {

    private inline fun <reified T> decode(json: String): T = HrmsJson.decodeFromString(json)

    @Test
    fun bankTransactionsDecodeNumbersOrStringsAndSearch() {
        val m = decode<BankTransactionsMonth>(
            """{"month":"2026-11","direction":"CREDIT","count":2,"total":"5740.50","transactions":[
                 {"id":"a","paymentDate":"2026-11-10T00:00:00.000Z","title":"","amount":"5240.50","direction":"CREDIT",
                  "rawDescription":"FAST PAYMENT ACME\nPython","remarks":"INV-1088"},
                 {"id":"b","paymentDate":"2026-11-03","title":"INTEREST","amount":500,"direction":"CREDIT","type":"Interest","paymentRef":null}]}""",
        )
        assertEquals(5740.5, m.total, 0.001)
        val (a, b) = m.transactions
        assertEquals(5240.5, a.amount, 0.001)
        assertEquals("2026-11-10", a.date)
        assertEquals("FAST PAYMENT ACME", a.displayTitle)
        assertEquals("INTEREST", b.displayTitle)
        assertTrue(a.matches("acme"))
        assertTrue(a.matches("inv-1088"))
        assertTrue(b.matches(" interest "))
        assertFalse(b.matches("acme"))
        assertTrue(b.matches(""))
    }

    @Test
    fun statementImportsCountLines() {
        val r = decode<StatementImportsResponse>(
            """{"imports":[{"batchId":"b1","from":"2026-10-01","to":"2026-10-31","credits":176,"debits":110,"creditTotal":182400,"debitTotal":"141250.10"}]}""",
        )
        val s = r.imports.single()
        assertEquals(286, s.lines)
        assertEquals(141250.1, s.debitTotal, 0.001)
        assertNull(s.statementRef)
    }

    @Test
    fun reconciliationDecodesDecimalStringsAndFilters() {
        val d = decode<ReconciliationDetail>(
            """{"run":{"id":"r","periodStart":"2026-10-01","periodEnd":"2026-10-31","status":"COMPLETED","matchRate":"98.5",
                       "bankClosingBalance":null,"qbClosingBalance":"1000.00"},
                "items":[
                 {"id":"1","status":"AMOUNT_MISMATCH","bankDate":"2026-10-14","bankAmount":"1250.00","bankDirection":"CREDIT","bankDescription":"ACME\nmore","amountDifference":"50.00","reviewed":false},
                 {"id":"2","status":"UNMATCHED_QB","qbDate":"2026-10-28","qbAmount":"45.00","qbDirection":"DEBIT","qbDocNumber":"E-77","reviewed":true},
                 {"id":"3","status":"MATCHED","bankDate":"2026-10-05","bankAmount":3200,"qbAmount":3200},
                 {"id":"4","status":"UNMATCHED_BANK","bankAmount":null,"qbDocNumber":"9"}]}""",
        )
        assertEquals(98.5, d.run.matchRate, 0.001)
        assertNull(d.run.bankClosingBalance)
        assertEquals(1000.0, d.run.qbClosingBalance!!, 0.001)
        val (mismatch, qbOnly, matched, bankOnly) = d.items
        assertEquals(1250.0, mismatch.amount!!, 0.001)
        assertEquals(50.0, mismatch.amountDifference!!, 0.001)
        assertEquals("ACME", mismatch.displayTitle)
        assertEquals("QuickBooks E-77", qbOnly.displayTitle)
        assertEquals("2026-10-28", qbOnly.date)
        assertEquals("DEBIT", qbOnly.direction)
        assertNull(bankOnly.amount)

        assertEquals(listOf("1", "4"), d.items.filter(ReconciliationFilter.TO_REVIEW::includes).map { it.id })
        assertEquals(listOf("1"), d.items.filter(ReconciliationFilter.MISMATCH::includes).map { it.id })
        assertEquals(listOf("4"), d.items.filter(ReconciliationFilter.BANK_ONLY::includes).map { it.id })
        assertEquals(listOf("2"), d.items.filter(ReconciliationFilter.QB_ONLY::includes).map { it.id })
        assertEquals(listOf("3"), d.items.filter(ReconciliationFilter.MATCHED::includes).map { it.id })
        assertEquals(4, d.items.count(ReconciliationFilter.ALL::includes))
        assertEquals(
            listOf("To review", "Amount differs", "Bank only", "QuickBooks only", "Matched", "All lines"),
            ReconciliationFilter.entries.map { it.title },
        )
        assertEquals("Amount differs", ReconciliationFilter.label("AMOUNT_MISMATCH"))
        assertEquals("Some State", ReconciliationFilter.label("SOME_STATE"))

        // Opens on To review; a run with nothing to review opens on every line.
        assertEquals(ReconciliationFilter.TO_REVIEW, ReconciliationFilter.initial(d.items, ReconciliationFilter.TO_REVIEW))
        assertEquals(ReconciliationFilter.ALL, ReconciliationFilter.initial(listOf(matched, qbOnly), ReconciliationFilter.TO_REVIEW))
        assertEquals(ReconciliationFilter.MATCHED, ReconciliationFilter.initial(listOf(matched), ReconciliationFilter.MATCHED))
    }

    @Test
    fun financeRowsOpenTheirDetailPendingClaimsOnlyForApprovers() {
        assertEquals("finance_income", financeRoute("income", canApprove = false))
        assertEquals("finance_expenses", financeRoute("expenses", canApprove = false))
        assertEquals("finance_statements", financeRoute("statements", canApprove = false))
        assertEquals("finance_reconciliation", financeRoute("reconciliation", canApprove = false))
        assertEquals("approvals", financeRoute("pending", canApprove = true))
        assertNull(financeRoute("pending", canApprove = false))
        assertEquals("accounting/income-tracking", BankDirection.CREDIT.webPath)
        assertEquals("accounting/expense-tracking", BankDirection.DEBIT.webPath)
    }

    @Test
    fun teamRowsShowTheEmploymentTypeAndSearchEveryField() {
        val e = Employee(id = "1", name = "Daniel Tan", position = "Developer", department = "Technology",
            email = "daniel.tan@example.com", employmentType = "PART_TIME")
        assertEquals("Part time", e.employmentLabel)
        assertTrue(e.isPartTime)
        assertEquals("Full time", e.copy(employmentType = "full_time").employmentLabel)
        assertEquals("Contract", e.copy(employmentType = "CONTRACT").employmentLabel)
        assertEquals("Intern", e.copy(employmentType = "INTERN").employmentLabel)
        assertNull(e.copy(employmentType = null).employmentLabel)
        listOf("daniel", "developer", "techno", "example.com", "part time", "").forEach { assertTrue(it, e.matches(it)) }
        assertFalse(e.matches("full time"))
    }

    @Test
    fun pushLinksIgnoreTheRequestQuery() {
        assertEquals("approvals", routeForLink("/approvals?request=abc", isApprover = true))
        assertEquals("leave", routeForLink("/leave?request=abc"))
        assertEquals("expenses", routeForLink("/expenses?request=x#top"))
        assertEquals("time_off", routeForLink("/time-off?request=1"))
        assertEquals("timesheet_admin", routeForLink("/timesheet/overview?request=9", isApprover = true))
        assertEquals("my_timesheet", routeForLink("/timesheet?request=9", mustClockIn = true))
        assertNull(routeForLink("/unknown?request=1"))
        assertNull(routeForLink(null))
    }

    @Test
    fun directoryRequestDefaultsToNoSearch() {
        val r = DirectoryRequest(AclpFilter.ACLP)
        assertEquals("", r.query)
        assertNull(r.tag)
    }
}
