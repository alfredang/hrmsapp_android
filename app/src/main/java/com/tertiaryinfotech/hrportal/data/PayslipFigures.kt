package com.tertiaryinfotech.hrportal.data

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * The pay breakdown on a payslip (Alfred, 26 Nov 2026): gross pay, the payback add-back, the No Pay Leave (interns:
 * MC / No Pay Leave) deduction, the CPF breakdown for staff or "no CPF" for interns, and the take-home pay. The same
 * lines on the Payslips list and above the PDF. Pure, unit-tested in `PayslipFiguresTest`.
 */
object PayslipFigures {

    enum class Kind {
        /** Gross pay. */
        GROSS,
        /** Added to pay ("+"). */
        ADD,
        /** Taken off the pay ("−"). */
        DEDUCT,
        /** A figure that is not part of take-home pay (employer CPF, total CPF). */
        INFO,
        /** Text only, no amount (interns' "CPF: None"). */
        NOTE,
        /** Take-home pay, emphasised. */
        NET,
    }

    data class Line(val label: String, val amount: Double?, val kind: Kind, val note: String? = null)

    fun lines(p: Payslip): List<Line> = buildList {
        add(Line("Gross pay", p.grossSalary, Kind.GROSS))
        if (p.paybackDays > 0) {
            add(Line("Payback added back (${days(p.paybackDays)} worked)", p.paybackAmount, Kind.ADD))
        }
        if (p.unpaidLeaveDays > 0) {
            val label = if (p.intern) "MC / No Pay Leave" else "No Pay Leave"
            add(Line("$label (${days(p.unpaidLeaveDays)})", p.unpaidLeaveDeduction, Kind.DEDUCT))
        }
        if (p.intern) {
            add(Line("CPF: None (interns have no CPF)", null, Kind.NOTE))
        } else {
            add(Line("CPF (Employee)", p.cpfEmployee, Kind.DEDUCT))
            add(Line("CPF (Employer)", p.cpfEmployer, Kind.INFO, note = "Paid by the company"))
            add(Line("Total CPF", totalCpf(p), Kind.INFO))
        }
        add(Line("Take-home pay", p.netSalary, Kind.NET))
    }

    /** Employee + employer CPF, summed in decimal so cents do not drift. */
    fun totalCpf(p: Payslip): Double =
        BigDecimal.valueOf(p.cpfEmployee).add(BigDecimal.valueOf(p.cpfEmployer)).setScale(2, RoundingMode.HALF_UP).toDouble()

    /** "1 day", "1.5 days", "3 days". */
    fun days(d: Double): String {
        val n = BigDecimal.valueOf(d).stripTrailingZeros().toPlainString()
        return if (d == 1.0) "1 day" else "$n days"
    }
}
