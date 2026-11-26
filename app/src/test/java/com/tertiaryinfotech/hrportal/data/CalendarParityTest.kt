package com.tertiaryinfotech.hrportal.data

import com.tertiaryinfotech.hrportal.ui.screens.WorkScheduleRows
import com.tertiaryinfotech.hrportal.ui.screens.birthdayTitle
import com.tertiaryinfotech.hrportal.ui.screens.timeOffLabel
import com.tertiaryinfotech.hrportal.ui.screens.timeOffTitle
import com.tertiaryinfotech.hrportal.util.Fmt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

/**
 * 1.16: calendar colours, time off, birthdays, off-day shading and month totals; Work Schedule;
 * Home role tiles. Payloads follow the server's JSON (web `src/app/api/mobile/...`, 20 Nov 2026).
 */
class CalendarParityTest {
    private inline fun <reified T> decode(json: String): T = HrmsJson.decodeFromString(json)

    /** Local noon, like the server tests' `new Date(y, m - 1, day, 12)`. */
    private fun d(y: Int, m: Int, day: Int): Date = Calendar.getInstance().apply {
        clear(); set(y, m - 1, day, 12, 0, 0)
    }.time

    // MARK: - Decoding

    private val approverCalendar = """{"year":2026,"canSeeTypes":true,
      "entries":[
        {"id":"a","employeeId":"s1","employeeName":"BELLA STAFF","startDate":"2026-11-10T00:00:00.000Z","endDate":"2026-11-11T00:00:00.000Z","days":2,"halfDay":false,"leaveType":"Annual Leave","category":"ANNUAL_LEAVE","isSelf":false,"group":"staff"},
        {"id":"b","employeeId":"i1","employeeName":"IAN INTERN","days":1,"leaveType":"Medical Leave","category":"MEDICAL_LEAVE","group":"intern"}],
      "people":[{"id":"s1","name":"BELLA STAFF","intern":false,"workdays":[1,2,3,4,5]},
                {"id":"i1","name":"IAN INTERN","intern":true,"workdays":[1,2,3]},
                {"id":"s2","name":"CARL STAFF","intern":false,"workdays":[]}],
      "holidays":[{"id":"h","title":"🎄 Christmas Day","startDate":"2026-12-25T00:00:00.000Z","endDate":"2026-12-25T00:00:00.000Z"}],
      "timeOff":[
        {"id":"t1","employeeId":"i1","employeeName":"IAN INTERN","date":"2026-11-13T00:00:00.000Z","startTime":"14:00","endTime":"16:00","hours":2,"offDay":false,"isSelf":false,"group":"intern"},
        {"id":"t2","employeeId":"i1","employeeName":"IAN INTERN","date":"2026-11-16T00:00:00.000Z","startTime":"09:00","endTime":"18:00","hours":0,"offDay":true,"isSelf":false,"group":"intern"},
        {"id":"t3","employeeId":"s1","employeeName":"BELLA STAFF","date":"2026-11-17T00:00:00.000Z","startTime":"10:00","endTime":"11:30","hours":"1.5","offDay":false,"isSelf":false,"group":"staff"}],
      "birthdays":[{"id":"bd-s1-2026","employeeId":"s1","name":"BELLA STAFF","date":"2026-11-24T04:00:00.000Z"},
                   {"id":"bd-i1-2026","employeeId":"i1","name":"IAN INTERN","date":"2026-03-02T04:00:00.000Z"}],
      "ownWorkdays":[1,2,3,4,5],
      "categories":[{"key":"HOLIDAY","label":"Public Holiday","color":"#ef4444","emoji":""},
                    {"key":"ANNUAL_LEAVE","label":"Annual Leave","color":"#f59e0b","emoji":"🏖️"},
                    {"key":"TIME_OFF","label":"Time Off","color":"#06b6d4","emoji":"⏰"},
                    {"key":"BIRTHDAY","label":"Birthday","color":"not-a-colour","emoji":"🎂"}]}"""

    @Test
    fun `decodes the new team calendar fields`() {
        val r = decode<TeamCalendarResponse>(approverCalendar)
        assertEquals("ANNUAL_LEAVE", r.entries[0].category)
        assertEquals(listOf(1, 2, 3), r.people!![1].workdays)
        assertEquals(emptyList<Int>(), r.people!![2].workdays)
        assertEquals(3, r.timeOff.size)
        assertEquals("14:00", r.timeOff[0].startTime)
        assertEquals(2.0, r.timeOff[0].hours, 0.0)
        assertTrue(r.timeOff[1].offDay)
        // Hours may arrive as a decimal string.
        assertEquals(1.5, r.timeOff[2].hours, 0.0)
        assertEquals("intern", r.timeOff[0].group)
        assertEquals("BELLA STAFF", r.birthdays!![0].name)
        assertEquals(listOf(1, 2, 3, 4, 5), r.ownWorkdays)
        assertEquals("🎄 Christmas Day", r.holidays[0].title)
        assertEquals("⏰", r.categories!!.first { it.key == "TIME_OFF" }.emoji)
    }

    @Test
    fun `an older server without the new fields decodes to safe defaults`() {
        val r = decode<TeamCalendarResponse>(
            """{"year":2026,"canSeeTypes":false,"entries":[{"id":"a","employeeId":"s1","employeeName":"A","leaveType":null}],"holidays":[]}""",
        )
        assertNull(r.entries[0].category)
        assertTrue(r.timeOff.isEmpty())
        assertNull(r.birthdays)
        assertNull(r.ownWorkdays)
        assertNull(r.categories)
        // The legend falls back to the web table.
        assertEquals(CalendarCategories.FALLBACK, CalendarCategories.table(r))
        // A masked colleague's leave ("category": null) stays null.
        assertNull(decode<TeamLeaveEntry>("""{"id":"x","category":null}""").category)
    }

    @Test
    fun `server legend is used, with a bad colour replaced by the fallback`() {
        val table = CalendarCategories.table(decode<TeamCalendarResponse>(approverCalendar))
        assertEquals(listOf("HOLIDAY", "ANNUAL_LEAVE", "TIME_OFF", "BIRTHDAY"), table.map { it.key })
        assertEquals("#d946ef", table.first { it.key == "BIRTHDAY" }.color)
        assertEquals(0xF59E0B, CalendarCategories.parseHex("#f59e0b"))
        assertNull(CalendarCategories.parseHex("f59e0b"))
        // The fallback table matches the web's colours and emoji.
        val fb = CalendarCategories.FALLBACK.associateBy { it.key }
        assertEquals("#ec4899", fb["MEDICAL_LEAVE"]!!.color)
        assertEquals("🤒", fb["MEDICAL_LEAVE"]!!.emoji)
        assertEquals("💸", fb["NO_PAY_LEAVE"]!!.emoji)
        assertEquals("📋", fb["OTHER_LEAVE"]!!.emoji)
        assertEquals("🏖️", fb["ANNUAL_LEAVE"]!!.emoji)
        assertEquals("⏰", fb["TIME_OFF"]!!.emoji)
        assertEquals("🎂", fb["BIRTHDAY"]!!.emoji)
        assertEquals("", fb["HOLIDAY"]!!.emoji)
    }

    @Test
    fun `decodes summary role tiles, null when absent`() {
        val s = decode<DashboardSummary>(
            """{"name":"A","isAdmin":false,
              "leaveSummary":[{"label":"Paid leave taken","value":"5 days"},{"label":"Annual leave balance","value":"6 days"},
                              {"label":"MC taken","value":"2 days"},{"label":"No-pay leave taken","value":"1 day","hint":"13 of 14 left"}],
              "financeKpis":{"pendingClaims":3,"pendingClaimsAmount":"412.50","incomeMonth":12000,"incomeYtd":150000.5,
                             "expensesMonth":8000,"expensesYtd":90000,"monthLabel":"November","year":2026},
              "trainerKpis":{"total":40,"aclp":25,"nonAclp":15}}""",
        )
        assertEquals(4, s.leaveSummary!!.size)
        assertEquals("13 of 14 left", s.leaveSummary!![3].hint)
        assertNull(s.leaveSummary!![0].hint)
        assertEquals(412.5, s.financeKpis!!.pendingClaimsAmount, 0.0)
        assertEquals("November", s.financeKpis!!.monthLabel)
        assertEquals(TrainerKpis(40, 25, 15), s.trainerKpis)

        val old = decode<DashboardSummary>("""{"name":"A","isAdmin":true,"leaveSummary":null,"financeKpis":null}""")
        assertNull(old.leaveSummary)
        assertNull(old.financeKpis)
        assertNull(old.trainerKpis)
        assertNull(HomeTiles.leave(old, isIntern = false))
    }

    @Test
    fun `decodes the profile work schedule`() {
        val p = decode<ProfileResponse>(
            """{"employee":{"id":"e1","name":"A","position":"Trainer","department":"Operations",
               "workdays":[1,2,3],"workingDays":["Mon","Tue","Wed"],"offDays":["Thu","Fri","Sat","Sun"],"officeHours":"9am to 6pm"}}""",
        ).employee!!
        assertEquals(listOf(1, 2, 3), p.workdays)
        assertEquals("9am to 6pm", p.officeHours)
        assertTrue(WorkScheduleRows.shown(p))
        assertEquals(
            listOf(
                "Title" to "Trainer", "Department" to "Operations", "Working hours" to "9am to 6pm",
                "Working days" to "Mon, Tue, Wed", "Off days" to "Thu, Fri, Sat, Sun",
            ),
            WorkScheduleRows.of(p),
        )
        // Older server: no schedule fields, card hidden.
        val old = decode<ProfileResponse>("""{"employee":{"id":"e1","name":"A"}}""").employee!!
        assertFalse(WorkScheduleRows.shown(old))
        // Only `workdays`: the names are computed as the web's splitWorkWeek does.
        val bare = decode<ProfileResponse>("""{"employee":{"id":"e1","name":"A","workdays":[]}}""").employee!!
        assertEquals("Mon, Tue, Wed, Thu, Fri", WorkScheduleRows.of(bare)[3].second)
        assertEquals("Sat, Sun", WorkScheduleRows.of(bare)[4].second)
        assertEquals("—", WorkScheduleRows.of(bare)[0].second)
    }

    @Test
    fun `work week split matches the web`() {
        assertEquals(listOf("Mon", "Tue", "Wed", "Thu", "Fri") to listOf("Sat", "Sun"), WorkWeek.split(null))
        assertEquals(listOf("Mon", "Wed", "Sat") to listOf("Tue", "Thu", "Fri", "Sun"), WorkWeek.split(listOf(6, 1, 3)))
        assertEquals(WorkWeek.split(null), WorkWeek.split(emptyList()))
    }

    // MARK: - Month totals: the same cases as the server's calendar-summary.test.ts

    private fun leave(category: String, start: Date, end: Date, days: Double, personId: String = "p1") =
        SummaryEvent(category, start, end, allDay = true, personId = personId, days = days)

    @Test
    fun `adds up each kind of leave and time off for the person in the month`() {
        val events = listOf(
            leave("ANNUAL_LEAVE", d(2026, 11, 2), d(2026, 11, 3), 2.0),
            leave("ANNUAL_LEAVE", d(2026, 11, 6), d(2026, 11, 6), 0.5),
            leave("MEDICAL_LEAVE", d(2026, 11, 10), d(2026, 11, 10), 1.0),
            leave("NO_PAY_LEAVE", d(2026, 11, 12), d(2026, 11, 13), 2.0),
            leave("OTHER_LEAVE", d(2026, 11, 16), d(2026, 11, 16), 1.0),
            SummaryEvent("TIME_OFF", d(2026, 11, 18), d(2026, 11, 18), allDay = false, personId = "p1", hours = 2.5),
            SummaryEvent("TIME_OFF", d(2026, 11, 19), d(2026, 11, 19), allDay = false, personId = "p1", hours = 1.0),
            SummaryEvent("TIME_OFF", d(2026, 11, 20), d(2026, 11, 20), allDay = true, personId = "p1", hours = 0.0),
        )
        assertEquals(
            PersonMonthSummary(
                annualLeaveDays = 2.5, medicalLeaveDays = 1.0, noPayLeaveDays = 2.0, otherLeaveDays = 1.0,
                timeOffHours = 3.5, offDays = 1,
            ),
            CalendarSummary.personMonthSummary(events, "p1", 2026, 10),
        )
    }

    @Test
    fun `ignores other people, other months and non-leave events`() {
        val events = listOf(
            leave("ANNUAL_LEAVE", d(2026, 11, 2), d(2026, 11, 2), 1.0, "p2"),
            leave("ANNUAL_LEAVE", d(2026, 10, 26), d(2026, 10, 26), 1.0),
            SummaryEvent("MEETING", d(2026, 11, 4), d(2026, 11, 4), allDay = false, personId = "p1"),
            SummaryEvent("TIME_OFF", d(2026, 12, 1), d(2026, 12, 1), allDay = false, personId = "p1", hours = 2.0),
        )
        assertEquals(PersonMonthSummary(), CalendarSummary.personMonthSummary(events, "p1", 2026, 10))
    }

    @Test
    fun `splits a leave that crosses the month end by working days`() {
        // Thu 29 Oct – Tue 3 Nov 2026: 4 working days (29, 30 Oct; 2, 3 Nov).
        val events = listOf(leave("ANNUAL_LEAVE", d(2026, 10, 29), d(2026, 11, 3), 4.0))
        assertEquals(2.0, CalendarSummary.personMonthSummary(events, "p1", 2026, 9).annualLeaveDays, 0.0)
        assertEquals(2.0, CalendarSummary.personMonthSummary(events, "p1", 2026, 10).annualLeaveDays, 0.0)
    }

    @Test
    fun `leaves public holidays out of the split`() {
        // Fri 30 Oct – Tue 3 Nov, with Mon 2 Nov a holiday: 2 working days, one in each month.
        val events = listOf(leave("MEDICAL_LEAVE", d(2026, 10, 30), d(2026, 11, 3), 2.0))
        val holidays = setOf("2026-11-02")
        assertEquals(1.0, CalendarSummary.personMonthSummary(events, "p1", 2026, 9, holidays).medicalLeaveDays, 0.0)
        assertEquals(1.0, CalendarSummary.personMonthSummary(events, "p1", 2026, 10, holidays).medicalLeaveDays, 0.0)
    }

    @Test
    fun `weekend-only leave crossing a month falls back to calendar days`() {
        // Sat 31 Oct – Sun 1 Nov 2026, charged 1 day: no working day, so split by calendar days.
        val events = listOf(leave("OTHER_LEAVE", d(2026, 10, 31), d(2026, 11, 1), 1.0))
        assertEquals(0.5, CalendarSummary.personMonthSummary(events, "p1", 2026, 10).otherLeaveDays, 0.0)
    }

    @Test
    fun `month total rows read like the web pills`() {
        // Payback replaced Time Off (Alfred, 26 Nov 2026): time off no longer shows in the totals.
        val rows = CalendarSummary.rows(
            PersonMonthSummary(annualLeaveDays = 1.0, medicalLeaveDays = 2.5, timeOffHours = 3.5, offDays = 2, paybackDays = 1.5),
        )
        assertEquals(
            listOf("Annual Leave" to "1 day", "Medical Leave" to "2.5 days", "No Pay Leave" to "0 days", "Payback" to "1.5 days"),
            rows.map { it.first to it.second },
        )
        assertEquals(CalendarCategories.PAYBACK, rows.last().third)
        assertEquals("#10b981", CalendarCategories.PAYBACK_CHIP.color)
        assertTrue(rows.none { it.first == "Time Off" })
        // Other Leave only when above zero; one payback day is singular.
        val withOther = CalendarSummary.rows(PersonMonthSummary(otherLeaveDays = 1.0, paybackDays = 1.0))
        assertEquals("Other Leave" to "1 day", withOther[3].first to withOther[3].second)
        assertEquals("Payback" to "1 day", withOther[4].first to withOther[4].second)
    }

    // MARK: - Paybacks (web calendar-summary.test.ts, 26 Nov 2026)

    @Test
    fun `counts approved paybacks on the off day worked, in that month only`() {
        val ev = listOf(
            SummaryEvent("PAYBACK", d(2026, 9, 5), d(2026, 9, 5), allDay = true, personId = "p1", days = 1.0),
            SummaryEvent("PAYBACK", d(2026, 9, 12), d(2026, 9, 12), allDay = true, personId = "p1", days = 0.5),
            SummaryEvent("PAYBACK", d(2026, 10, 3), d(2026, 10, 3), allDay = true, personId = "p1", days = 1.0),
            SummaryEvent("PAYBACK", d(2026, 9, 6), d(2026, 9, 6), allDay = true, personId = "p2", days = 1.0),
        )
        assertEquals(1.5, CalendarSummary.personMonthSummary(ev, "p1", 2026, 8).paybackDays, 0.0)
        assertEquals(1.0, CalendarSummary.personMonthSummary(ev, "p1", 2026, 9).paybackDays, 0.0)
        assertEquals(1.0, CalendarSummary.personMonthSummary(ev, "p2", 2026, 8).paybackDays, 0.0)
        // A payback is not leave: nothing else moves.
        assertEquals(0.0, CalendarSummary.personMonthSummary(ev, "p1", 2026, 8).noPayLeaveDays, 0.0)
    }

    /** Local noon (no zone), so the days land the same in any test time zone. */
    private val paybackCalendar = """{"year":2026,"canSeeTypes":true,
      "entries":[
        {"id":"n1","employeeId":"s1","employeeName":"BELLA STAFF","startDate":"2026-09-28T12:00:00","endDate":"2026-10-02T12:00:00","days":5,"leaveType":"No Pay Leave","category":"NO_PAY_LEAVE"},
        {"id":"n2","employeeId":"i1","employeeName":"IAN INTERN","startDate":"2026-09-14T12:00:00","endDate":"2026-09-14T12:00:00","days":1,"leaveType":"No Pay Leave","category":"NO_PAY_LEAVE"},
        {"id":"a1","employeeId":"s1","employeeName":"BELLA STAFF","startDate":"2026-09-07T12:00:00","endDate":"2026-09-07T12:00:00","days":1,"leaveType":"Annual Leave","category":"ANNUAL_LEAVE"}],
      "holidays":[],
      "paybacks":[
        {"id":"pb1","employeeId":"s1","leaveRequestId":"n1","nplDate":"2026-09-28T12:00:00","workDate":"2026-09-19T12:00:00","days":1,"isSelf":false},
        {"id":"pb2","employeeId":"s1","leaveRequestId":"n1","nplDate":"2026-09-29T12:00:00","workDate":"2026-10-03T12:00:00","days":0.5,"isSelf":false},
        {"id":"pb3","employeeId":"i1","leaveRequestId":"n2","nplDate":"2026-09-14T12:00:00","workDate":"2026-09-26T12:00:00","days":1,"isSelf":false},
        {"id":"pb4","employeeId":"i1","leaveRequestId":"n2","nplDate":"2026-09-14T12:00:00","workDate":"2026-09-27T12:00:00","days":1,"isSelf":false}]}"""

    @Test
    fun `paybacks decode, count for the picked person only, and come off No Pay Leave`() {
        val r = decode<TeamCalendarResponse>(paybackCalendar)
        assertEquals(4, r.paybacks!!.size)
        assertEquals(0.5, r.paybacks!![1].days, 0.0)
        val events = CalendarSummary.events(r)
        val holidays = CalendarSummary.holidayKeys(r)
        // Bella: 5 No Pay Leave days less 1.5 paid back = 3.5, split by working days (Mon 28 Sep – Fri 2 Oct:
        // 3 in September, 2 in October). Her paybacks count on the day worked: 19 Sep, 3 Oct.
        val sep = CalendarSummary.personMonthSummary(events, "s1", 2026, 8, holidays)
        val oct = CalendarSummary.personMonthSummary(events, "s1", 2026, 9, holidays)
        assertEquals(2.1, sep.noPayLeaveDays, 0.0)
        assertEquals(1.4, oct.noPayLeaveDays, 0.0)
        assertEquals(1.0, sep.paybackDays, 0.0)
        assertEquals(0.5, oct.paybackDays, 0.0)
        assertEquals(1.0, sep.annualLeaveDays, 0.0)
        // Ian: paid back more than the 1 day taken: No Pay Leave never goes below 0; Bella's paybacks are not his.
        val ian = CalendarSummary.personMonthSummary(events, "i1", 2026, 8, holidays)
        assertEquals(0.0, ian.noPayLeaveDays, 0.0)
        assertEquals(2.0, ian.paybackDays, 0.0)
        assertEquals(0.0, CalendarSummary.personMonthSummary(events, "i1", 2026, 9, holidays).paybackDays, 0.0)
    }

    @Test
    fun `an older server without paybacks keeps No Pay Leave as charged`() {
        val r = decode<TeamCalendarResponse>(paybackCalendar.replace(Regex(""",\s*"paybacks":\[[\s\S]*]}$"""), "}"))
        assertNull(r.paybacks)
        val sep = CalendarSummary.personMonthSummary(CalendarSummary.events(r), "s1", 2026, 8)
        assertEquals(3.0, sep.noPayLeaveDays, 0.0)
        assertEquals(0.0, sep.paybackDays, 0.0)
    }

    @Test
    fun `summary events come from categorised leave and all time off`() {
        val r = decode<TeamCalendarResponse>(approverCalendar)
        val events = CalendarSummary.events(r)
        // Entry "b" has no start date, so it cannot be placed: one leave plus three time off.
        assertEquals(1 + 3, events.size)
        val ian = events.filter { it.personId == "i1" && it.category == "TIME_OFF" }
        assertEquals(listOf(false, true), ian.map { it.allDay })
        // A masked entry (no category) is not counted.
        val masked = r.copy(entries = r.entries.map { it.copy(category = null) })
        assertEquals(3, CalendarSummary.events(masked).size)
    }

    // MARK: - Off-day shading

    @Test
    fun `staff and interns shade their own off days`() {
        val r = decode<TeamCalendarResponse>(
            """{"year":2026,"entries":[],"holidays":[],"ownWorkdays":[1,2,3],
               "timeOff":[{"id":"t","employeeId":"me","date":"2026-11-10T00:00:00.000Z","offDay":true,"isSelf":true}]}""",
        )
        val s = OffDays.schedule(r, approver = false, mineOnly = false, personId = null)!!
        assertEquals(listOf(1, 2, 3), s.workdays)
        assertTrue(OffDays.isOff(s, d(2026, 11, 12), emptyList())) // Thursday
        assertTrue(OffDays.isOff(s, d(2026, 11, 15), emptyList())) // Sunday
        assertFalse(OffDays.isOff(s, d(2026, 11, 9), emptyList())) // Monday
        // A Tuesday with an approved full-day Off Day is shaded; hourly time off is not.
        assertTrue(OffDays.isOff(s, d(2026, 11, 10), r.timeOff))
        assertFalse(OffDays.isOff(s, d(2026, 11, 10), listOf(r.timeOff[0].copy(offDay = false))))
        // Empty workdays mean Mon–Fri.
        val empty = OffDays.schedule(r.copy(ownWorkdays = emptyList()), approver = false, mineOnly = false, personId = null)!!
        assertEquals(OffDays.DEFAULT_WORKDAYS, empty.workdays)
        assertTrue(OffDays.isOff(empty, d(2026, 11, 14), emptyList())) // Saturday
        assertFalse(OffDays.isOff(empty, d(2026, 11, 13), emptyList())) // Friday
        // No ownWorkdays (older server, or no employee record): nothing shaded.
        assertNull(OffDays.schedule(r.copy(ownWorkdays = null), approver = false, mineOnly = false, personId = null))
        assertFalse(OffDays.isOff(null, d(2026, 11, 15), emptyList()))
    }

    @Test
    fun `approvers shade only the person they picked`() {
        val r = decode<TeamCalendarResponse>(approverCalendar)
        // Everyone: no single schedule.
        assertNull(OffDays.schedule(r, approver = true, mineOnly = false, personId = null))
        val ian = OffDays.schedule(r, approver = true, mineOnly = false, personId = "i1")!!
        assertEquals(OffDaySchedule(listOf(1, 2, 3), "i1"), ian)
        assertTrue(OffDays.isOff(ian, d(2026, 11, 12), emptyList())) // Thursday
        // Ian's Off Day on Mon 16 Nov shades it; Bella's would not.
        val monday = d(2026, 11, 16)
        assertTrue(OffDays.isOff(ian, monday, r.timeOff.filter { it.employeeId == "i1" }))
        assertFalse(OffDays.isOff(ian, monday, listOf(r.timeOff[1].copy(employeeId = "s1"))))
        // A person with no workdays on record works Mon–Fri.
        assertEquals(OffDays.DEFAULT_WORKDAYS, OffDays.schedule(r, approver = true, mineOnly = false, personId = "s2")!!.workdays)
        // "Only me" uses the approver's own schedule.
        assertEquals(OffDaySchedule(listOf(1, 2, 3, 4, 5), null), OffDays.schedule(r, approver = true, mineOnly = true, personId = "i1"))
        // An older server with no workdays anywhere: nothing shaded.
        val old = r.copy(people = r.people!!.map { it.copy(workdays = null) }, ownWorkdays = null)
        assertNull(OffDays.schedule(old, approver = true, mineOnly = false, personId = "i1"))
    }

    @Test
    fun `iso weekday is Monday 1 to Sunday 7`() {
        assertEquals(1, OffDays.isoWeekday(d(2026, 11, 16)))
        assertEquals(6, OffDays.isoWeekday(d(2026, 11, 21)))
        assertEquals(7, OffDays.isoWeekday(d(2026, 11, 22)))
    }

    // MARK: - Filters, counts, titles

    @Test
    fun `time off and birthdays follow the person filter and Only me`() {
        val r = decode<TeamCalendarResponse>(approverCalendar)
        assertEquals(listOf("t1", "t2"), CalendarFilters.timeOff(r.timeOff, mineOnly = false, personId = "i1").map { it.id })
        assertEquals(3, CalendarFilters.timeOff(r.timeOff, mineOnly = false, personId = null).size)
        assertTrue(CalendarFilters.timeOff(r.timeOff, mineOnly = true, personId = null).isEmpty())
        assertEquals(listOf("bd-s1-2026"), CalendarFilters.birthdays(r, mineOnly = false, personId = "s1").map { it.id })
        assertEquals(2, CalendarFilters.birthdays(r, mineOnly = false, personId = null).size)
        // Only me: the viewer's own birthday, found from their own leave.
        val mine = r.copy(entries = r.entries.map { if (it.id == "a") it.copy(isSelf = true) else it })
        assertEquals(listOf("bd-s1-2026"), CalendarFilters.birthdays(mine, mineOnly = true, personId = null).map { it.id })
        assertTrue(CalendarFilters.birthdays(r, mineOnly = true, personId = null).isEmpty())
        // Non-approvers get no birthdays at all.
        assertTrue(CalendarFilters.birthdays(r.copy(birthdays = null), mineOnly = false, personId = null).isEmpty())
    }

    @Test
    fun `person filter counts leave and time off`() {
        val list = CalendarPeople.build(decode<TeamCalendarResponse>(approverCalendar))!!
        assertEquals(1 + 1, list.find("s1")!!.leaveCount) // one leave + one time off
        assertEquals(1 + 2, list.find("i1")!!.leaveCount)
        assertEquals(0, list.find("s2")!!.leaveCount)
        assertEquals(5, list.totalEntries)
    }

    @Test
    fun `time off and birthday titles`() {
        val r = decode<TeamCalendarResponse>(approverCalendar)
        assertEquals("Time off 2:00 PM – 4:00 PM", timeOffLabel(r.timeOff[0]))
        assertEquals("Off Day", timeOffLabel(r.timeOff[1]))
        assertEquals("⏰ IAN INTERN · Off Day", timeOffTitle(r.timeOff[1], approver = true))
        assertEquals("⏰ Time off 2:00 PM – 4:00 PM", timeOffTitle(r.timeOff[0].copy(isSelf = true), approver = false))
        assertEquals("⏰ Off Day", timeOffTitle(r.timeOff[1].copy(isSelf = true), approver = true))
        assertEquals("🎂 BELLA STAFF birthday", birthdayTitle(r.birthdays!![0]))
    }

    // MARK: - Home tiles

    @Test
    fun `leave tiles are shown as given, with the intern MC hint`() {
        val s = DashboardSummary(
            leaveSummary = listOf(
                SummaryStat("Paid leave taken", "0 days"), SummaryStat("MC taken", "1 day"),
                SummaryStat("No-pay leave taken", "2 days", "12 of 14 left"),
            ),
        )
        val staff = HomeTiles.leave(s, isIntern = false)!!
        assertEquals(HomeTile("No-pay leave taken", "2 days", "12 of 14 left"), staff[2])
        assertNull(staff[1].hint)
        assertEquals("Sick leave is No Pay Leave", HomeTiles.leave(s, isIntern = true)!![1].hint)
        assertNull(HomeTiles.leave(DashboardSummary(leaveSummary = emptyList()), isIntern = false))
    }

    @Test
    fun `finance and trainer tiles read like the web`() {
        val f = FinanceKpis(2, 412.5, 12000.0, 150000.0, 8000.0, 90000.0, "November", 2026)
        val tiles = HomeTiles.finance(f)
        assertEquals("Pending expense claims", tiles[0].label)
        assertEquals("2", tiles[0].value)
        assertEquals("${Fmt.money(412.5)} waiting for approval", tiles[0].hint)
        assertEquals("Income, November", tiles[1].label)
        assertEquals("${Fmt.money(150000.0)} in 2026", tiles[1].hint) // iOS 1.20 wording; counts prefix when sent
        assertEquals("Expenses, November", tiles[2].label)
        assertEquals(Fmt.money(8000.0), tiles[2].value)
        assertEquals("Nothing waiting", HomeTiles.finance(f.copy(pendingClaims = 0))[0].hint)

        val t = HomeTiles.trainers(TrainerKpis(40, 25, 15))
        assertEquals(listOf("Trainers" to "40", "ACLP trainers" to "25", "Non-ACLP trainers" to "15"), t.map { it.label to it.value })
        assertNotNull(t[0].hint)
    }
}
