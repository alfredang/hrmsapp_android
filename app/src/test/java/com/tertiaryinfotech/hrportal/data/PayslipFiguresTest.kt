package com.tertiaryinfotech.hrportal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Payslip pay breakdown (web, 26 Nov 2026): decoding the new fields and the lines shown. */
class PayslipFiguresTest {

    private inline fun <reified T> decode(json: String): T = HrmsJson.decodeFromString(json)

    private val staffJson = """{"id":"p1","grossSalary":4500,"cpfEmployee":900,"cpfEmployer":765,"intern":false,
        "unpaidLeaveDays":1.5,"unpaidLeaveDeduction":310.34,"paybackDays":1,"paybackAmount":206.9,
        "totalDeductions":1210.34,"netSalary":3289.66,"status":"PAID","pdfPath":"/p1"}"""

    @Test
    fun newFieldsDecode() {
        val p = decode<Payslip>(staffJson)
        assertEquals(765.0, p.cpfEmployer, 0.0)
        assertFalse(p.intern)
        assertEquals(1.5, p.unpaidLeaveDays, 0.0)
        assertEquals(310.34, p.unpaidLeaveDeduction, 0.0)
        assertEquals(1.0, p.paybackDays, 0.0)
        assertEquals(206.9, p.paybackAmount, 0.0)
        // Decimal strings and nulls are read too.
        val s = decode<Payslip>("""{"id":"p2","cpfEmployer":"765.00","unpaidLeaveDays":null,"paybackAmount":"12.5"}""")
        assertEquals(765.0, s.cpfEmployer, 0.0)
        assertEquals(0.0, s.unpaidLeaveDays, 0.0)
        assertEquals(12.5, s.paybackAmount, 0.0)
    }

    @Test
    fun olderServerWithoutTheFieldsStillDecodes() {
        val p = decode<PayslipsResponse>(
            """{"payslips":[{"id":"p1","grossSalary":4500,"cpfEmployee":900,"netSalary":3600,"status":"PAID","pdfPath":"/x"}]}""",
        ).payslips.single()
        assertEquals(0.0, p.cpfEmployer, 0.0)
        assertFalse(p.intern)
        assertEquals(0.0, p.unpaidLeaveDays, 0.0)
        assertEquals(0.0, p.unpaidLeaveDeduction, 0.0)
        assertEquals(0.0, p.paybackDays, 0.0)
        assertEquals(0.0, p.paybackAmount, 0.0)
        assertNull(p.cpfSubmission)
        // Only gross, the CPF breakdown and take-home: no payback or No Pay Leave line when there is none.
        assertEquals(
            listOf("Gross pay", "CPF (Employee)", "CPF (Employer)", "Total CPF", "Take-home pay"),
            PayslipFigures.lines(p).map { it.label },
        )
    }

    @Test
    fun staffLinesShowTheCpfBreakdown() {
        val lines = PayslipFigures.lines(decode(staffJson))
        assertEquals(
            listOf(
                "Gross pay", "Payback added back (1 day worked)", "No Pay Leave (1.5 days)",
                "CPF (Employee)", "CPF (Employer)", "Total CPF", "Take-home pay",
            ),
            lines.map { it.label },
        )
        val byLabel = lines.associateBy { it.label }
        assertEquals(PayslipFigures.Kind.ADD, byLabel.getValue("Payback added back (1 day worked)").kind)
        assertEquals(206.9, byLabel.getValue("Payback added back (1 day worked)").amount!!, 0.0)
        assertEquals(PayslipFigures.Kind.DEDUCT, byLabel.getValue("No Pay Leave (1.5 days)").kind)
        assertEquals(PayslipFigures.Kind.DEDUCT, byLabel.getValue("CPF (Employee)").kind)
        val employer = byLabel.getValue("CPF (Employer)")
        assertEquals(PayslipFigures.Kind.INFO, employer.kind)
        assertEquals("Paid by the company", employer.note)
        assertEquals(1665.0, byLabel.getValue("Total CPF").amount!!, 0.0)
        val net = lines.last()
        assertEquals(PayslipFigures.Kind.NET, net.kind)
        assertEquals(3289.66, net.amount!!, 0.0)
    }

    @Test
    fun internsHaveNoCpfAndMcCountsAsUnpaid() {
        val p = decode<Payslip>(
            """{"id":"i1","grossSalary":1200,"cpfEmployee":0,"cpfEmployer":0,"intern":true,
                "unpaidLeaveDays":2,"unpaidLeaveDeduction":109.09,"paybackDays":0,"paybackAmount":0,
                "netSalary":1090.91,"status":"PAID","pdfPath":"/i1"}""",
        )
        assertTrue(p.intern)
        val lines = PayslipFigures.lines(p)
        assertEquals(
            listOf("Gross pay", "MC / No Pay Leave (2 days)", "CPF: None (interns have no CPF)", "Take-home pay"),
            lines.map { it.label },
        )
        val none = lines[2]
        assertEquals(PayslipFigures.Kind.NOTE, none.kind)
        assertNull(none.amount)
        assertTrue(lines.none { it.label.startsWith("CPF (") || it.label == "Total CPF" })
    }

    @Test
    fun dayCounts() {
        assertEquals("1 day", PayslipFigures.days(1.0))
        assertEquals("0.5 days", PayslipFigures.days(0.5))
        assertEquals("3 days", PayslipFigures.days(3.0))
        assertEquals("2.5 days", PayslipFigures.days(2.5))
    }
}
