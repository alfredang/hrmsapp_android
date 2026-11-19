package com.tertiaryinfotech.hrportal.data

import com.tertiaryinfotech.hrportal.ui.screens.filterByGroup
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsTest {
    private inline fun <reified T> decode(json: String): T = HrmsJson.decodeFromString(json)

    /** One payload shaped like the web `AnalyticsData` (src/lib/analytics.ts). */
    private val payload = """{
      "months":[{"key":"2026-09","label":"Sept","year":2026},{"key":"2026-10","label":"Oct","year":2026},{"key":"2026-11","label":"Nov","year":2026}],
      "finance":[{"month":"2026-10","type":"Income","revenue":1000.5,"expenses":0},
                 {"month":"2026-10","type":"Trainer Fee","revenue":0,"expenses":400},
                 {"month":"2026-11","type":"Income","revenue":500,"expenses":0},
                 {"month":"2026-11","type":"Payroll","revenue":0,"expenses":700},
                 {"month":"2026-09","type":"Payroll","revenue":0,"expenses":50}],
      "financeRange":{"from":"2026-09-02","to":"2026-11-16"},
      "leave":[{"month":"2026-10","personId":"s1","category":"LEAVE","days":2},
               {"month":"2026-10","personId":"i1","category":"MC","days":1.5},
               {"month":"2026-11","personId":"s1","category":"MC","days":1},
               {"month":"2026-11","personId":"i1","category":"NPL","days":3},
               {"month":"2026-09","personId":"gone","category":"LEAVE","days":9}],
      "people":[{"id":"s1","name":"STAFF ONE","code":"E0001","intern":false,"active":true},
                {"id":"i1","name":"INTERN ONE","code":"I0001","intern":true,"active":false}],
      "headcount":[{"month":"2026-09","staff":10,"interns":2},{"month":"2026-10","staff":11,"interns":2},{"month":"2026-11","staff":11,"interns":4}],
      "somethingNew":true}"""

    private val data: AnalyticsData get() = decode(payload)

    @Test
    fun `decodes the server payload and tolerates a missing finance block`() {
        val d = data
        assertEquals(3, d.months.size)
        assertEquals(1000.5, d.finance!![0].revenue, 0.0)
        assertEquals("2026-09-02", d.financeRange?.from)
        assertEquals(1.5, d.leave[1].days, 0.0)
        assertTrue(d.people[1].intern)
        val noFinance = decode<AnalyticsData>("""{"months":[],"finance":null,"financeRange":null,"leave":[],"people":[],"headcount":[]}""")
        assertNull(noFinance.finance)
        assertFalse(AnalyticsCalc.view(noFinance, AnalyticsFilter()).hasFinance)
    }

    @Test
    fun `tiles and charts come from the same filtered rows`() {
        val v = AnalyticsCalc.view(data, AnalyticsFilter())
        assertEquals("Sept 2026 to Nov 2026", v.rangeLabel)
        assertEquals(1500.5, v.revenue, 1e-9)
        assertEquals(1150.0, v.expenses, 1e-9)
        assertEquals(v.revenue, v.financeByMonth.sumOf { it.values["revenue"]!! }, 1e-9)
        assertEquals(v.expenses, v.financeByMonth.sumOf { it.values["expenses"]!! }, 1e-9)
        // A leave row whose person is not in `people` is dropped, as on the web.
        assertEquals(2.0, v.leaveTotals["LEAVE"]!!, 1e-9)
        assertEquals(2.5, v.leaveTotals["MC"]!!, 1e-9)
        assertEquals(listOf("INTERN ONE", "STAFF ONE"), v.byPerson.map { it.person.name })
        assertEquals(2, v.peopleOnMc)
        assertEquals(15, v.headNow)
        assertEquals(3, v.headDelta)
        assertEquals(listOf(2, 1, null), v.headChanges.map { it.change })
    }

    @Test
    fun `period, group, person, leave type and category filters`() {
        val one = AnalyticsCalc.view(data, AnalyticsFilter(period = 1))
        assertEquals(500.0, one.revenue, 1e-9)
        assertEquals("Nov 2026 to Nov 2026", one.rangeLabel)

        val interns = AnalyticsCalc.view(data, AnalyticsFilter(who = AnalyticsWho.INTERNS))
        assertEquals(listOf("INTERN ONE"), interns.byPerson.map { it.person.name })
        assertEquals(listOf("interns"), interns.headSeries)
        assertEquals(4, interns.headNow)
        // Finance is not a per-person figure: the group filter leaves it alone.
        assertEquals(1500.5, interns.revenue, 1e-9)

        val person = AnalyticsCalc.view(data, AnalyticsFilter(personId = "s1"))
        assertEquals("STAFF ONE", person.selectedPerson?.name)
        assertEquals(1.0, person.leaveTotals["MC"]!!, 1e-9)
        // A staff member picked earlier no longer applies once the group is Interns.
        assertNull(AnalyticsCalc.view(data, AnalyticsFilter(who = AnalyticsWho.INTERNS, personId = "s1")).selectedPerson)

        val noMc = AnalyticsCalc.view(data, AnalyticsFilter(leaveOff = setOf("MC")))
        assertEquals(listOf("LEAVE", "NPL"), noMc.leaveSeries)
        assertEquals(0.0, noMc.leaveTotals["MC"]!!, 1e-9)

        val noPayroll = AnalyticsCalc.view(data, AnalyticsFilter(typeOff = setOf("Payroll")))
        assertEquals(400.0, noPayroll.expenses, 1e-9)
        // The category list still shows the hidden one, so it can be switched back on.
        assertTrue(noPayroll.categories.any { it.type == "Payroll" })
    }

    @Test
    fun `axis and money formatting`() {
        assertEquals(4.0, AnalyticsCalc.niceCeil(3.0), 0.0)
        assertEquals(10.0, AnalyticsCalc.niceCeil(9.0), 0.0)
        assertEquals(30.0, AnalyticsCalc.niceCeil(23.0), 0.0)
        assertEquals(800_000.0, AnalyticsCalc.niceCeil(727_000.0), 0.0)
        assertEquals("$3,089,763", AnalyticsCalc.money(3_089_763.18))
        assertEquals("-$189,557", AnalyticsCalc.money(-189_557.4))
        assertEquals("$0", AnalyticsCalc.money(-0.2))
        assertEquals("$1.2M", AnalyticsCalc.moneyAxis(1_200_000.0))
        assertEquals("$400k", AnalyticsCalc.moneyAxis(400_000.0))
        assertEquals("1.5", AnalyticsCalc.days(1.5))
        assertEquals("2", AnalyticsCalc.days(2.0))
    }

    @Test
    fun `sample data is internally consistent`() {
        val v = AnalyticsCalc.view(AnalyticsCalc.sample(), AnalyticsFilter())
        assertEquals(12, v.financeByMonth.size)
        assertTrue(v.byPerson.isNotEmpty())
        assertEquals(23, v.headNow)
    }

    @Test
    fun `team calendar group filter`() {
        val r = decode<TeamCalendarResponse>(
            """{"year":2026,"canSeeTypes":true,"entries":[
                 {"id":"a","employeeName":"A","group":"staff"},{"id":"b","employeeName":"B","group":"intern"},
                 {"id":"c","employeeName":"C","group":null},{"id":"d","employeeName":"D"}],"holidays":[]}""",
        )
        assertEquals(listOf("staff", "intern", null, null), r.entries.map { it.group })
        assertEquals(4, filterByGroup(r.entries, "all").size)
        assertEquals(listOf("a"), filterByGroup(r.entries, "staff").map { it.id })
        assertEquals(listOf("b"), filterByGroup(r.entries, "intern").map { it.id })
    }

    @Test
    fun `push token body always carries the platform`() {
        // The server rejects a body without "platform"; a default value would be left out of the JSON.
        val json = HrmsJson.encodeToString(PushTokenBody("tok-123", HrmsApi.PUSH_PLATFORM))
        assertTrue(json, json.contains("\"platform\":\"android\""))
        assertTrue(json, json.contains("\"token\":\"tok-123\""))
    }
}
