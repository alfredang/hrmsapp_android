package com.tertiaryinfotech.hrportal.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CertLabelTest {
    @Test fun longTitlesCollapseToTheirCode() {
        assertEquals("ACLP", CertLabel.short("Advanced Certificate in Learning and Performance (ACLP, 2023)"))
        assertEquals("DDDLP", CertLabel.short("dddpl"))
        assertEquals("ACLP", CertLabel.short("ALCP"))
        assertEquals("ACTA", CertLabel.short("ACTA"))
    }

    @Test fun otherLongTitlesAreTruncated() {
        val s = CertLabel.short("Graduate Certificate in Workplace Learning (GCWLP)")
        assertEquals(14, s.length)
        assertEquals('…', s.last())
    }

    @Test fun duplicatesCollapse() {
        assertEquals(listOf("ACLP"), CertLabel.shortAll(listOf("ACLP", "Advanced Certificate in Learning and Performance (ACLP)")))
    }

    @Test fun everyAlertTypeIsShownExceptToasts() {
        assertEquals(true, isVisibleNotification("ATTENDANCE_MISSED"))
        assertEquals(true, isVisibleNotification("LEAVE_SUBMITTED"))
        assertEquals(false, isVisibleNotification("ADD_TOAST"))
    }
}
