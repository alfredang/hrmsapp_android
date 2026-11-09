package com.tertiaryinfotech.hrportal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Coded `POST /api/leave` refusals, as src/app/api/leave/route.ts sends them. */
class LeaveErrorsTest {

    @Test
    fun parsesAlExceedsBalance() {
        val body = """{"error":"Only 6 day(s) of annual leave are available. Apply for the other 2 as No Pay Leave.",
            "code":"AL_EXCEEDS_BALANCE","alAvailable":6,"excessDays":2,
            "npl":{"cap":17.5,"used":1,"pending":2,"remaining":14.5},"suggestedLeaveTypeCode":"NPL"}"""
        val r = LeaveErrors.parse(body) as LeaveRejection.AlExceedsBalance
        assertTrue(r.message.startsWith("Only 6 day(s)"))
        assertEquals(6.0, r.alAvailable, 0.0)
        assertEquals(2.0, r.excessDays, 0.0)
        assertEquals("NPL", r.suggestedLeaveTypeCode)
        assertEquals(NplStatus(cap = 17.5, used = 1.0, pending = 2.0, remaining = 14.5), r.npl)
    }

    @Test
    fun parsesInternMcNotPaid() {
        val r = LeaveErrors.parse("""{"error":"Interns have no paid MC.","code":"INTERN_MC_NOT_PAID","suggestedLeaveTypeCode":"NPL"}""")
        assertTrue(r is LeaveRejection.InternMcNotPaid)
        r as LeaveRejection.InternMcNotPaid
        assertEquals("Interns have no paid MC.", r.message)
        assertEquals("NPL", r.suggestedLeaveTypeCode)
    }

    @Test
    fun parsesNplCapExceeded() {
        val r = LeaveErrors.parse(
            """{"error":"Over the NPL cap","code":"NPL_CAP_EXCEEDED","npl":{"cap":7,"used":3,"pending":2,"remaining":2},"requested":3}""",
        ) as LeaveRejection.NplCapExceeded
        assertEquals("Over the NPL cap", r.message)
        assertEquals(2.0, r.npl!!.remaining, 0.0)
        assertEquals(3.0, r.requested!!, 0.0)
    }

    @Test
    fun npLStatusAcceptsDecimalStrings() {
        val r = LeaveErrors.parse(
            """{"code":"NPL_CAP_EXCEEDED","npl":{"cap":"10.5","used":"1","pending":0,"remaining":"9.5"}}""",
        ) as LeaveRejection.NplCapExceeded
        assertEquals(10.5, r.npl!!.cap, 0.0)
        assertEquals(9.5, r.npl!!.remaining, 0.0)
    }

    @Test
    fun missingFieldsFallBackToDefaults() {
        val r = LeaveErrors.parse("""{"code":"AL_EXCEEDS_BALANCE"}""") as LeaveRejection.AlExceedsBalance
        assertTrue(r.message.isNotBlank())
        assertEquals("NPL", r.suggestedLeaveTypeCode)
        assertEquals(0.0, r.excessDays, 0.0)
        assertNull(r.npl)
    }

    @Test
    fun uncodedOrMalformedBodiesAreNotRejections() {
        assertNull(LeaveErrors.parse("""{"error":"Insufficient leave balance","available":1,"requested":3}"""))
        assertNull(LeaveErrors.parse("""{"error":"x","code":"SOMETHING_NEW"}"""))
        assertNull(LeaveErrors.parse("<html>Bad gateway</html>"))
        assertNull(LeaveErrors.parse(""))
        assertNull(LeaveErrors.parse(null))
    }

    @Test
    fun applyLeaveMapsCoded400sToLeaveRejectedException() {
        val coded = ApiException(
            ApiException.Kind.HTTP, "Interns have no paid MC.", 400,
            """{"error":"Interns have no paid MC.","code":"INTERN_MC_NOT_PAID","suggestedLeaveTypeCode":"NPL"}""",
        )
        val mapped = HrmsApi.leaveFailure(coded)
        assertTrue(mapped is LeaveRejectedException)
        assertTrue((mapped as LeaveRejectedException).rejection is LeaveRejection.InternMcNotPaid)
        assertEquals("Interns have no paid MC.", mapped.message)
    }

    @Test
    fun applyLeaveKeepsOtherFailuresAsApiException() {
        val plain = ApiException(ApiException.Kind.HTTP, "Leave overlaps", 400, """{"error":"Leave overlaps"}""")
        assertSame(plain, HrmsApi.leaveFailure(plain))
        val network = ApiException(ApiException.Kind.NETWORK, "Network error.")
        assertSame(network, HrmsApi.leaveFailure(network))
        val serverError = ApiException(ApiException.Kind.HTTP, "boom", 500, """{"code":"AL_EXCEEDS_BALANCE"}""")
        assertSame(serverError, HrmsApi.leaveFailure(serverError))
    }

    @Test
    fun internRolesMustBeExactlyIntern() {
        assertTrue(LeaveRules.isIntern(listOf("INTERN")))
        assertFalse(LeaveRules.isIntern(listOf("ADMIN", "INTERN")))
        assertFalse(LeaveRules.isIntern(listOf("STAFF")))
        assertFalse(LeaveRules.isIntern(emptyList()))
        assertTrue(LeaveRules.isIntern(null, "INTERN"))
        assertFalse(LeaveRules.isIntern(null, "STAFF"))
        assertTrue(SessionUser(roles = listOf("INTERN"), role = "INTERN").isIntern)
        assertFalse(SessionUser(roles = listOf("ADMIN", "INTERN"), role = "ADMIN").isIntern)
    }

    @Test
    fun internSickReasonMirrorsTheWebForm() {
        assertEquals("Sick leave (MC)", LeaveRules.internSickReason(""))
        assertEquals("Sick leave (MC)", LeaveRules.internSickReason("Sick leave (MC)"))
        assertEquals("Sick leave (MC) — fever", LeaveRules.internSickReason("fever"))
        assertEquals("Sick leave (MC) — fever", LeaveRules.internSickReason("Sick leave (MC) — fever"))
    }

    @Test
    fun medicalCodes() {
        assertTrue(LeaveRules.isMedical("MC"))
        assertTrue(LeaveRules.isMedical("sl"))
        assertFalse(LeaveRules.isMedical("AL"))
        assertFalse(LeaveRules.isMedical(null))
    }
}
