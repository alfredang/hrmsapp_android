package com.tertiaryinfotech.hrportal.data

import com.tertiaryinfotech.hrportal.push.routeForLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Approve / Reject on a request notice, and No Pay Leave paybacks in Approvals (23 Nov 2026, iOS NoticeActionTests). */
class NoticeActionTest {
    private inline fun <reified T> decode(json: String): T = HrmsJson.decodeFromString(json)

    @Test
    fun serverActionIsUsed() {
        val n = decode<AppNotification>("""{"id":"n","type":"PAYBACK_SUBMITTED","link":"/leave/payback?request=P1","action":{"kind":"payback","requestId":"P1"}}""")
        assertEquals(NoticeAction("payback", "P1"), n.requestAction)
    }

    @Test
    fun olderServerDerivesActionFromTypeAndLink() {
        assertEquals(NoticeAction("leave", "L1"), decode<AppNotification>("""{"id":"n","type":"LEAVE_SUBMITTED","link":"/leave?request=L1"}""").requestAction)
        assertEquals("correction", decode<AppNotification>("""{"id":"n","type":"ATTENDANCE_CORRECTION_SUBMITTED","link":"/timesheet/overview?request=C1","action":null}""").requestAction?.kind)
    }

    @Test
    fun infoNoticesAndLinksWithoutIdHaveNoAction() {
        assertNull(decode<AppNotification>("""{"id":"n","type":"HR_COMPLIANCE","link":"/dashboard"}""").requestAction)
        assertNull(decode<AppNotification>("""{"id":"n","type":"LEAVE_SUBMITTED","link":"/leave"}""").requestAction)
    }

    @Test
    fun approvalsDecodePaybacksAndCountThem() {
        assertEquals(emptyList<PendingPayback>(), decode<ApprovalsResponse>("""{"leaves":[],"claims":[]}""").paybacks)
        val a = decode<ApprovalsResponse>("""{"leaves":[],"claims":[],"paybacks":[{"id":"P31","employee":"FEMINA JASMIN","nplDate":"2026-10-30T00:00:00.000Z","workDate":"2026-10-31T00:00:00.000Z","days":1,"note":null,"clocked":null}]}""")
        assertEquals(listOf("P31"), a.paybacks.map { it.id })
        assertEquals(1, PendingApprovals.from(a, emptyList()).total)
    }

    @Test
    fun paybackNoticeOpensApprovalsForApprovers() {
        assertEquals("approvals", routeForLink("/leave/payback?request=P1", "PAYBACK_SUBMITTED", isApprover = true))
    }
}
