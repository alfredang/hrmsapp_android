package com.tertiaryinfotech.hrportal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Team → person records (web 25 Nov 2026): Leave / MC / No Pay Leave balance tiles; clock-in only for interns. */
class RecordsBalanceTest {
    private fun decode(json: String): EmployeeRecords = HrmsJson.decodeFromString(json)

    private val base = """
        "employee": {"id":"e1","employeeCode":"E0005","name":"Jyoti"},
        "attendance": {"days": [], "totalHours": 0, "daysWorked": 0},
        "leave": [
          {"id":"l1","leaveType":"Annual Leave","leaveCode":"AL","startDate":"2026-11-16T00:00:00Z","endDate":"2026-11-17T00:00:00Z","days":2,"dayType":"FULL_DAY","status":"APPROVED"},
          {"id":"l2","leaveType":"No Pay Leave","leaveCode":"NPL","startDate":"2026-10-01T00:00:00Z","endDate":"2026-10-01T00:00:00Z","days":1,"dayType":"FULL_DAY","status":"APPROVED"}
        ]
    """

    @Test fun decodesBalancesAndTileText() {
        val r = decode("""{$base, "intern": false, "mustClockIn": false, "balances": [
          {"code":"AL","name":"Annual Leave","taken":5,"pending":0,"available":7,"allowance":12},
          {"code":"MC","name":"MC","taken":1.5,"pending":0,"available":12.5,"allowance":14},
          {"code":"NPL","name":"No Pay Leave","taken":1,"pending":0,"available":13,"allowance":14}
        ]}""")
        val b = r.balances!!
        assertEquals("5 taken", b[0].takenText)
        assertEquals("7/12 balance", b[0].balanceText)
        assertEquals("12.5/14 balance", b[1].balanceText)
        assertTrue(b[1].isMedical)
        assertTrue(b[2].isNoPay)
        assertEquals(listOf("l2"), r.noPayLeave.map { it.id })
    }

    @Test fun decodesPaybacksAndClaims() {
        val r = decode("""{$base, "paybacks": [
          {"id":"pb1","nplDate":"2026-11-11T00:00:00.000Z","workDate":"2026-11-22T00:00:00.000Z","days":1,"note":null,"status":"PENDING","approver":null,"approvedAt":null,"rejectionReason":null,"createdAt":"2026-11-23T01:00:00.000Z"}
        ], "claims": [
          {"id":"c1","description":"Grab to client","amount":28.4,"currency":"SGD","category":"Travel","categoryCode":"TRV","expenseDate":"2026-11-20T00:00:00.000Z","status":"APPROVED","receiptUrl":null,"driveWebViewLink":null,"approver":"Alfred Ang","approvedAt":null,"rejectionReason":null,"createdAt":null}
        ]}""")
        assertEquals("PENDING", r.paybacks!!.single().status)
        assertEquals(28.4, r.claims!!.single().amount, 0.001)
        assertNull(decode("{$base}").claims)
    }

    @Test fun clockHistoryOnlyForPeopleWhoClockIn() {
        assertFalse(decode("""{$base, "intern": false, "mustClockIn": false}""").showsClock)
        assertTrue(decode("""{$base, "intern": true, "mustClockIn": true}""").showsClock)
        val old = decode("{$base}")
        assertNull(old.balances)
        assertTrue(old.showsClock)
    }

    // MARK: - "Month for pay" (GET /api/mobile/employees/{id}/pay-month, web 26 Nov 2026)

    private fun payMonth(json: String): PayMonthSummary = HrmsJson.decodeFromString(json)

    @Test fun payMonthDecodesAStaffMonthIntoFourTiles() {
        val p = payMonth("""{"month":"2026-09","intern":false,"annualLeaveDays":2,"noPayLeaveDays":1.5,"mcDays":1,
            "mcPaid":true,"mcLabel":"Paid MC","paybackDays":0.5}""")
        assertEquals("2026-09", p.month)
        assertEquals(
            listOf(
                listOf("Annual Leave", "2 days", "Paid"),
                listOf("No Pay Leave", "1.5 days", "Deducted"),
                listOf("Paid MC", "1 day", "Paid"),
                listOf("Payback", "0.5 days", "Off days worked, added back"),
            ),
            p.tiles.map { listOf(it.label, it.value, it.hint) },
        )
        assertEquals(listOf(0xF59E0BL, 0x64748BL, 0xEC4899L, 0x10B981L), p.tiles.map { it.color })
    }

    @Test fun payMonthInternMcIsNoPayAndDeducted() {
        val p = payMonth("""{"month":"2026-11","intern":true,"annualLeaveDays":0,"noPayLeaveDays":0,"mcDays":2,
            "mcPaid":false,"mcLabel":"No Pay MC","paybackDays":0,"extra":"ignored"}""")
        assertTrue(p.intern)
        assertEquals(Triple("No Pay MC", "2 days", "Deducted"), p.tiles[2].let { Triple(it.label, it.value, it.hint) })
        assertEquals("0 days", p.tiles[0].value)
    }

    @Test fun payMonthDaysUpToTwoDecimalsWithoutTrailingZeros() {
        assertEquals("0 days", PayMonthSummary.days(0.0))
        assertEquals("1 day", PayMonthSummary.days(1.0))
        assertEquals("2 days", PayMonthSummary.days(2.0))
        assertEquals("10 days", PayMonthSummary.days(10.0))
        assertEquals("2.5 days", PayMonthSummary.days(2.5))
        assertEquals("0.25 days", PayMonthSummary.days(0.25))
        assertEquals("1.33 days", PayMonthSummary.days(4.0 / 3))
        assertEquals("1 day", PayMonthSummary.days(0.999))
    }
}
