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
    fun `ACLP, Non-ACLP and All each list exactly the trainers their tile counts`() {
        val s = TrainerSearch.stats(trainers)
        fun ids(f: AclpFilter) = TrainerSearch.directory(trainers, TrainerQuery(aclp = f)).map { it.id }
        assertEquals(listOf("1", "4"), ids(AclpFilter.ACLP))
        // Non-ACLP = "ACLP" is not in trainingCerts: ACTA only, or nothing on record.
        assertEquals(listOf("2", "3"), ids(AclpFilter.NON_ACLP))
        assertEquals(listOf("1", "2", "3", "4"), ids(AclpFilter.ALL))
        AclpFilter.entries.forEach { assertEquals(it.name, it.count(s), ids(it).size) }
        assertEquals(listOf("ACLP", "Non-ACLP", "All"), AclpFilter.entries.map { it.label })
        // A bare query (the API call that loads the directory) is not narrowed.
        assertEquals(AclpFilter.ALL, TrainerQuery().aclp)
        // The filter also applies to the Applicants list, and combines with an accreditation.
        assertEquals(listOf("5"), TrainerSearch.directory(trainers, TrainerQuery(aclp = AclpFilter.ACLP, status = TrainerStatusFilter.APPLICANTS)).map { it.id })
        assertEquals(listOf("2"), TrainerSearch.directory(trainers, Accreditation.ACTA.applyTo(TrainerQuery(aclp = AclpFilter.NON_ACLP))).map { it.id })
        assertTrue(TrainerSearch.directory(trainers, Accreditation.DACE.applyTo(TrainerQuery(aclp = AclpFilter.ACLP))).isEmpty())
    }

    @Test
    fun `the directory is sorted A to Z by name, ignoring case`() {
        val list = decode<TrainersResponse>(
            """{"trainers":[
                {"id":"1","name":"TAN WEI MING","status":"Active","trainingCerts":["ACLP"]},
                {"id":"2","name":"goh siew ling","status":"Active","trainingCerts":["ACLP"]},
                {"id":"3","name":"  ALFRED ANG (DR)","status":"Active","trainingCerts":["ACLP"]},
                {"id":"4","name":"MUTHU KUMAR","status":"Active","trainingCerts":[]},
                {"id":"5","name":"Goh Siew Ling","status":"Active","trainingCerts":["ACLP"]}]}""",
        ).trainers
        assertEquals(listOf("3", "2", "5", "4", "1"), TrainerSearch.sortedByName(list).map { it.id })
        assertEquals(listOf("3", "2", "5", "1"), TrainerSearch.directory(list, TrainerQuery(aclp = AclpFilter.ACLP)).map { it.id })
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
