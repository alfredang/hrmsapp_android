package com.tertiaryinfotech.hrportal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Payload shapes from the HRMS backend, decoded with the app's own JSON settings. */
class ModelParsingTest {

    private inline fun <reified T> decode(json: String): T = HrmsJson.decodeFromString(json)

    @Test
    fun summaryCarriesCanManageTrainers() {
        assertTrue(decode<DashboardSummary>("""{"name":"A","isAdmin":false,"canManageTrainers":true,"expenseYtd":0}""").canManageTrainers)
        // Older servers don't send it: default false, so the tab stays hidden.
        assertFalse(decode<DashboardSummary>("""{"name":"A","isAdmin":true}""").canManageTrainers)
    }

    @Test
    fun leaveResponseNplRowGivesCapAndRemaining() {
        val r = decode<LeaveResponse>(
            """{"balances":[
                 {"code":"AL","name":"Annual Leave","paid":true,"entitlement":7,"used":1,"pending":0,"proRated":5.5,"available":4.5},
                 {"code":"NPL","name":"No Pay Leave","paid":false,"entitlement":0,"used":2,"pending":1,"earned":0,"autoDeducted":0,"proRated":17.5,"available":14.5}],
               "types":[{"id":"lt-al","code":"AL","name":"Annual Leave","paid":true,"defaultDays":7},
                        {"id":"lt-npl","code":"NPL","name":"No Pay Leave","paid":false,"defaultDays":0}],
               "requests":[]}""",
        )
        val npl = LeaveRules.nplFromBalances(r.balances)!!
        assertEquals(17.5, npl.cap, 0.0)
        assertEquals(14.5, npl.remaining, 0.0)
        assertEquals(2.0, npl.used, 0.0)
        assertEquals(1.0, npl.pending, 0.0)
        assertEquals("lt-npl", LeaveRules.typeIdFor(r.types, "NPL"))
        assertEquals("lt-npl", LeaveRules.typeIdFor(r.types, "npl"))
        assertNull(LeaveRules.typeIdFor(r.types, "CL"))
    }

    @Test
    fun nplRowWithoutCapIsIgnored() {
        val r = decode<LeaveResponse>("""{"balances":[{"code":"NPL","name":"No Pay Leave","proRated":null,"available":3}]}""")
        assertNull(LeaveRules.nplFromBalances(r.balances))
        assertNull(LeaveRules.nplFromBalances(emptyList()))
    }

    @Test
    fun trainerListDecodes() {
        val r = decode<TrainersResponse>(
            """{"count":2,"trainers":[
                {"id":"t1","name":"TAN WEI MING","nickname":null,"phone":"+65 9123 4567","email":"a@b.c","trainerType":"ACLP",
                 "status":"Active","inTms":true,"trainingCerts":["ACLP","ACTA"],"certifications":["PMP"],"skills":["Python","Power BI"],
                 "education":null,"summary":null,"financeBackground":true,"ibfFitDeclaration":false,"classesCount":34,
                 "lastClassDate":"2026-10-28","courseTitles":["Power BI"]},
                {"id":"t2","name":"LIM","status":"Applicant","trainingCerts":[],"classesCount":0,"lastClassDate":null,"someNewField":1}]}""",
        )
        assertEquals(2, r.count)
        val t = r.trainers[0]
        assertEquals("TAN WEI MING", t.name)
        assertEquals(listOf("ACLP", "ACTA"), t.trainingCerts)
        assertTrue(t.financeBackground)
        assertEquals(34, t.classesCount)
        assertEquals(listOf("PMP", "Python", "Power BI"), t.topTags())
        assertEquals(listOf("PMP", "Python"), t.topTags(2))
        assertNull(r.trainers[1].lastClassDate)
        assertTrue(r.trainers[1].certifications.isEmpty())
    }

    @Test
    fun trainerDetailToleratesFreeFormJsonColumns() {
        val r = decode<TrainerDetailResponse>(
            """{"trainer":{"id":"t1","name":"TAN WEI MING","nric":"S8123456D","phone":"91234567","email":"a@b.c",
                "trainingCerts":["ACLP"],"certifications":[],"skills":[],"experience":["Analyst, DBS"],
                "financeBackground":false,"financeEvidence":[],"ibfFitDeclaration":true,"ibfFitFiles":["fit.pdf"],
                "evidenceFiles":[],"moreFolders":["x"],"sourceKey":"tms:1","editedAt":"2026-11-01T00:00:00.000Z",
                "courses":[{"title":"Power BI","code":"TGS-1","classes":21,"first":"2023-02-06","last":"2026-10-28"},
                           {"title":"Python","code":null,"classes":"13","first":null,"last":"2026-08-19"}],
                "recentClasses":[{"start":"2026-10-28","title":"Power BI","run":42,"status":"Completed"},
                                 {"start":"2026-08-19","title":"Python","run":"17","status":null}],
                "calendarClasses":null,"classesCount":34,"lastClassDate":"2026-10-28","adminNotes":null}}""",
        )
        val t = r.trainer
        assertEquals("S8123456D", t.nric)
        assertEquals(2, t.courses!!.size)
        assertEquals(21.0, t.courses!![0].classes, 0.0)
        assertEquals(13.0, t.courses!![1].classes, 0.0)
        assertEquals("", t.courses!![1].code)
        assertEquals("42", t.recentClasses!![0].run)
        assertEquals("17", t.recentClasses!![1].run)
        assertEquals("", t.recentClasses!![1].status)
        assertNull(t.calendarClasses)
        assertEquals(listOf("fit.pdf"), t.ibfFitFiles)
        assertEquals(listOf("Analyst, DBS"), t.experience)
    }

    @Test
    fun nricIsMaskedLikeTheWeb() {
        assertEquals("S****456D", TrainerFormat.maskNric("S8123456D"))
        assertEquals("T****567A", TrainerFormat.maskNric(" T1234567A "))
        assertEquals("****", TrainerFormat.maskNric("S123"))
        assertEquals("", TrainerFormat.maskNric(null))
        assertEquals("", TrainerFormat.maskNric(""))
    }

    @Test
    fun contactLinks() {
        assertEquals("tel:+6591234567", TrainerFormat.telUri("+65 9123-4567"))
        assertEquals("https://linkedin.com/in/x", TrainerFormat.webUrl("linkedin.com/in/x"))
        assertEquals("https://www.linkedin.com/in/x", TrainerFormat.webUrl("https://www.linkedin.com/in/x"))
    }

    @Test
    fun trainerQueryDefaults() {
        val q = TrainerQuery()
        assertEquals("trainers", q.status.param)
        assertTrue(q.certs.isEmpty())
        assertEquals(TrainerQuery(certs = setOf("ACLP")), TrainerQuery(certs = setOf("ACLP")))
    }
}
