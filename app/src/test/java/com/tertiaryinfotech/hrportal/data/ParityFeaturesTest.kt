package com.tertiaryinfotech.hrportal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 1.15: calendar person filter, workforce breakdown, trainer tiles, staff / intern directory, Home
 * tiles. Payloads are written from the server's JSON shapes (web `src/app/api/mobile/...`).
 */
class ParityFeaturesTest {
    private inline fun <reified T> decode(json: String): T = HrmsJson.decodeFromString(json)

    // MARK: - Team calendar: "Leave shown for"

    private val approverCalendar = """{"year":2026,"canSeeTypes":true,"entries":[
        {"id":"a","employeeId":"s1","employeeName":"Bella Staff","startDate":"2026-11-10T00:00:00.000Z","endDate":"2026-11-12T00:00:00.000Z","days":3,"halfDay":false,"leaveType":"Annual Leave","isSelf":false,"group":"staff"},
        {"id":"b","employeeId":"s1","employeeName":"Bella Staff","days":1,"leaveType":"Medical Leave","group":"staff"},
        {"id":"c","employeeId":"i1","employeeName":"Ian Intern","days":2,"leaveType":"Medical Leave","group":"intern"},
        {"id":"d","employeeId":"gone","employeeName":"Left Already","days":1,"leaveType":"Annual Leave","group":"staff"}],
      "people":[{"id":"s2","name":"adam Staff","intern":false},{"id":"s1","name":"Bella Staff","intern":false},
                {"id":"i1","name":"Ian Intern","intern":true},{"id":"i2","name":"Zoe Intern","intern":true}],
      "holidays":[{"id":"h","title":"Deepavali","startDate":"2026-11-08T00:00:00.000Z","endDate":"2026-11-08T00:00:00.000Z"}]}"""

    @Test
    fun `decodes people and lists every active person with their leave count`() {
        val r = decode<TeamCalendarResponse>(approverCalendar)
        assertEquals(4, r.people!!.size)
        assertTrue(r.people!![2].intern)

        val list = CalendarPeople.build(r)!!
        // Grouped Staff then Interns, by name; people without leave are listed with 0.
        assertEquals(listOf("adam Staff" to 0, "Bella Staff" to 2), list.staff.map { it.name to it.leaveCount })
        assertEquals(listOf("Ian Intern" to 1, "Zoe Intern" to 0), list.interns.map { it.name to it.leaveCount })
        assertTrue(list.others.isEmpty())
        // Someone who has left is not listed, but their leave still counts under Everyone.
        assertNull(list.find("gone"))
        assertEquals(4, list.totalEntries)
        assertEquals("Bella Staff", list.find("s1")?.name)
        assertNull(list.find(null))
    }

    @Test
    fun `person filter keeps one person's leave and everything for Everyone`() {
        val r = decode<TeamCalendarResponse>(approverCalendar)
        assertEquals(listOf("a", "b"), CalendarPeople.filter(r.entries, "s1").map { it.id })
        assertEquals(4, CalendarPeople.filter(r.entries, null).size)
        assertTrue(CalendarPeople.filter(r.entries, "s2").isEmpty())
        // Holidays are a separate list and are never filtered.
        assertEquals(1, r.holidays.size)
    }

    @Test
    fun `older server without people - approver list is built from the entries`() {
        val r = decode<TeamCalendarResponse>(
            """{"year":2026,"canSeeTypes":true,"entries":[
                 {"id":"a","employeeId":"s1","employeeName":"Bella Staff","group":"staff"},
                 {"id":"b","employeeId":"s1","employeeName":"Bella Staff","group":"staff"},
                 {"id":"c","employeeId":"i1","employeeName":"Ian Intern","group":"intern"}],"holidays":[]}""",
        )
        assertNull(r.people)
        val list = CalendarPeople.build(r)!!
        assertEquals(listOf("Bella Staff" to 2), list.staff.map { it.name to it.leaveCount })
        assertEquals(listOf("Ian Intern" to 1), list.interns.map { it.name to it.leaveCount })

        // Before `group` existed: still a list for an approver, under one heading.
        val older = decode<TeamCalendarResponse>(
            """{"year":2026,"canSeeTypes":true,"entries":[{"id":"a","employeeId":"s1","employeeName":"Bella Staff"}],"holidays":[]}""",
        )
        val olderList = CalendarPeople.build(older)!!
        assertTrue(olderList.staff.isEmpty() && olderList.interns.isEmpty())
        assertEquals(listOf("Bella Staff"), olderList.others.map { it.name })
    }

    @Test
    fun `non-approver gets no person filter`() {
        val explicitNull = decode<TeamCalendarResponse>(
            """{"year":2026,"canSeeTypes":false,"entries":[{"id":"a","employeeId":"s1","employeeName":"Bella Staff","leaveType":null,"group":null}],"people":null,"holidays":[]}""",
        )
        assertNull(explicitNull.people)
        assertNull(CalendarPeople.build(explicitNull))
        val absent = decode<TeamCalendarResponse>("""{"year":2026,"entries":[{"id":"a","employeeName":"Bella Staff"}],"holidays":[]}""")
        assertNull(CalendarPeople.build(absent))
    }

    @Test
    fun `an approver with an empty roster still gets the control`() {
        val r = decode<TeamCalendarResponse>("""{"year":2027,"canSeeTypes":true,"entries":[],"people":[],"holidays":[]}""")
        val list = CalendarPeople.build(r)
        assertNotNull(list)
        assertTrue(list!!.all.isEmpty())
    }

    // MARK: - Analytics: workforce breakdown

    private fun analytics(breakdown: String) =
        """{"months":[{"key":"2026-11","label":"Nov","year":2026}],"finance":null,"financeRange":null,"leave":[],"people":[],
            "headcount":[{"month":"2026-10","staff":3,"interns":1},{"month":"2026-11","staff":4,"interns":2}]$breakdown}"""

    private val breakdownJson = ""","breakdown":[
        {"intern":false,"gender":"Female","ageGroup":"25–34","ethnicGroup":"Chinese"},
        {"intern":false,"gender":"Male","ageGroup":"35–44","ethnicGroup":"Malay"},
        {"intern":false,"gender":"Female","ageGroup":"55 and over","ethnicGroup":"Not recorded"},
        {"intern":false,"gender":"Female","ageGroup":"25–34","ethnicGroup":"Chinese"},
        {"intern":true,"gender":"Male","ageGroup":"Under 25","ethnicGroup":"Not recorded"},
        {"intern":true,"gender":"Other","ageGroup":"Not recorded","ethnicGroup":"Indian"}]"""

    @Test
    fun `decodes breakdown and tolerates null or absent`() {
        val d = decode<AnalyticsData>(analytics(breakdownJson))
        assertEquals(6, d.breakdown!!.size)
        assertEquals("25–34", d.breakdown!![0].ageGroup)
        assertTrue(d.breakdown!![4].intern)
        assertNull(decode<AnalyticsData>(analytics(""","breakdown":null""")).breakdown)
        assertNull(decode<AnalyticsData>(analytics("")).breakdown)
    }

    @Test
    fun `breakdown counts, fixed orders and percentages`() {
        val people = decode<AnalyticsData>(analytics(breakdownJson)).breakdown!!

        val staff = Demographics.view(people, AnalyticsWho.STAFF)
        assertEquals(4, staff.shown)
        assertEquals("staff", staff.noun)
        val (gender, age, ethnic) = staff.charts
        // Fixed order: Male before Female; percentages are whole numbers.
        assertEquals(listOf("Male" to 1, "Female" to 3), gender.slices.map { it.label to it.count })
        assertEquals(listOf(25, 75), gender.slices.map { it.percent })
        assertEquals(listOf("25–34" to 2, "35–44" to 1, "55 and over" to 1), age.slices.map { it.label to it.count })
        // The colour slot is the position in the fixed order, so 55 and over keeps the darkest step.
        assertEquals(listOf(1, 2, 4), age.slices.map { it.order })
        assertEquals(listOf("Chinese", "Malay", "Not recorded"), ethnic.slices.map { it.label })
        assertTrue(ethnic.slices.last().notRecorded)
        assertEquals(1, staff.ethnicMissing)
        staff.charts.forEach { assertEquals(4, it.total) }

        val interns = Demographics.view(people, AnalyticsWho.INTERNS)
        assertEquals(2, interns.shown)
        assertEquals(listOf("Under 25", "Not recorded"), interns.charts[1].slices.map { it.label })
        assertEquals(listOf("Male", "Other"), interns.charts[0].slices.map { it.label })

        val all = Demographics.view(people, AnalyticsWho.ALL)
        assertEquals(6, all.shown)
        assertEquals("people", all.noun)
        assertEquals(2, all.ethnicMissing)
    }

    @Test
    fun `breakdown edge cases - nobody, all not recorded, unknown and blank labels`() {
        val none = Demographics.view(emptyList(), AnalyticsWho.STAFF)
        assertEquals(0, none.shown)
        assertTrue(none.charts.all { it.slices.isEmpty() && it.total == 0 })

        val unrecorded = List(3) { BreakdownPerson(false, "Male", "25–34", "Not recorded") }
        val v = Demographics.view(unrecorded, AnalyticsWho.STAFF)
        assertEquals(3, v.ethnicMissing)
        assertEquals(v.shown, v.ethnicMissing)
        assertEquals(listOf(100), v.charts[2].slices.map { it.percent })

        // A label the app does not know yet is kept (after the known ones) so the total stays right;
        // a blank value counts as Not recorded.
        val odd = listOf(BreakdownPerson(false, "Non-binary", "25–34", ""), BreakdownPerson(false, "Male", "", "Others"))
        val o = Demographics.view(odd, AnalyticsWho.ALL)
        assertEquals(listOf("Male", "Non-binary"), o.charts[0].slices.map { it.label })
        assertEquals(listOf("25–34", "Not recorded"), o.charts[1].slices.map { it.label })
        assertEquals(1, o.ethnicMissing)
        assertEquals(0, Demographics.percent(1, 0))
        assertEquals(33, Demographics.percent(1, 3))
        assertEquals(67, Demographics.percent(2, 3))
    }

    @Test
    fun `sample data carries a breakdown that agrees with its headcount`() {
        val d = AnalyticsCalc.sample()
        assertEquals(ActiveCounts(14, 9), AnalyticsCalc.activeCounts(d))
        assertEquals(d.headcount.last().staff, d.breakdown!!.count { !it.intern })
        // The sample survives the JSON round trip the debug mock uses.
        assertEquals(d, decode<AnalyticsData>(HrmsJson.encodeToString(AnalyticsData.serializer(), d)))
    }

    // MARK: - Home tiles

    @Test
    fun `home counts come from the breakdown, else the last headcount month, else nothing`() {
        assertEquals(ActiveCounts(4, 2), AnalyticsCalc.activeCounts(decode(analytics(breakdownJson))))
        assertEquals(ActiveCounts(4, 2), AnalyticsCalc.activeCounts(decode(analytics(""))))
        assertNull(AnalyticsCalc.activeCounts(decode("""{"months":[],"leave":[],"people":[],"headcount":[]}""")))
    }

    // MARK: - Trainers: KPI tiles

    private val trainers = decode<TrainersResponse>(
        """{"count":6,"trainers":[
            {"id":"1","name":"A","status":"Active","trainingCerts":["ACLP","ACTA"]},
            {"id":"2","name":"B","status":"Active","trainingCerts":["ACTA"]},
            {"id":"3","name":"C","status":"Inactive","trainingCerts":[]},
            {"id":"4","name":"D","status":"Active","trainingCerts":["ACLP"]},
            {"id":"5","name":"E","status":"Applicant","trainingCerts":["ACLP"]},
            {"id":"6","name":"F","status":"Contacted","trainingCerts":["DACE"]}]}""",
    ).trainers

    @Test
    fun `trainer tiles count trainers only, never applicants`() {
        val s = TrainerSearch.stats(trainers)
        assertEquals(4, s.total)
        assertEquals(2, s.aclp)
        assertEquals(2, s.nonAclp)
        assertEquals(2, s.applicants)
        assertEquals(s.total, s.aclp + s.nonAclp)
        assertEquals(TrainerStats(0, 0, 0), TrainerSearch.stats(emptyList()))
    }

    @Test
    fun `tapping a tile lists exactly the trainers it counts`() {
        val s = TrainerSearch.stats(trainers)
        val aclp = TrainerSearch.filter(trainers, Accreditation.ACLP.applyTo(TrainerQuery()))
        assertEquals(listOf("1", "4"), aclp.map { it.id })
        assertEquals(s.aclp, aclp.size)
        val non = TrainerSearch.filter(trainers, Accreditation.NON_ACLP.applyTo(TrainerQuery()))
        assertEquals(listOf("2", "3"), non.map { it.id })
        assertEquals(s.nonAclp, non.size)
        assertEquals(s.nonAclp, Accreditation.NON_ACLP.count(trainers.filterNot(TrainerSearch::isApplicant)))
        assertEquals(s.total, TrainerSearch.filter(trainers, Accreditation.ANY.applyTo(TrainerQuery())).size)
        // Switching away from Non-ACLP drops the exclusion.
        assertTrue(Accreditation.ACTA.applyTo(Accreditation.NON_ACLP.applyTo(TrainerQuery())).withoutCerts.isEmpty())
    }

    // MARK: - Directory: Staff and Interns

    @Test
    fun `directory splits into staff and interns for approvers`() {
        val r = decode<EmployeesResponse>(
            """{"isAdmin":true,"employees":[
                {"id":"1","name":"Staff One","intern":false},{"id":"2","name":"Intern One","intern":true},
                {"id":"3","name":"Staff Two","intern":false},{"id":"4","name":"No Flag"}]}""",
        )
        assertEquals(listOf(false, true, false, null), r.employees.map { it.intern })
        val g = Directory.split(r.employees)!!
        assertEquals(listOf("Staff One", "Staff Two", "No Flag"), g.staff.map { it.name })
        assertEquals(listOf("Intern One"), g.interns.map { it.name })
    }

    @Test
    fun `directory stays one list when the flag is null or absent`() {
        val nulls = decode<EmployeesResponse>("""{"isAdmin":false,"employees":[{"id":"1","name":"A","intern":null},{"id":"2","name":"B","intern":null}]}""")
        assertNull(Directory.split(nulls.employees))
        val absent = decode<EmployeesResponse>("""{"isAdmin":true,"employees":[{"id":"1","name":"A"}]}""")
        assertNull(absent.employees[0].intern)
        assertNull(Directory.split(absent.employees))
        assertNull(Directory.split(emptyList()))
        assertFalse(Directory.split(decode<EmployeesResponse>("""{"employees":[{"id":"1","name":"A","intern":true}]}""").employees)!!.interns.isEmpty())
    }
}
