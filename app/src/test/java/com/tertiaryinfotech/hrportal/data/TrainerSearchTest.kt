package com.tertiaryinfotech.hrportal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The on-device directory filter must behave like the web's `src/lib/trainers.ts`. */
class TrainerSearchTest {

    private val tan = TrainerListItem(
        id = "1", name = "TAN WEI MING", phone = "9123 4567", email = "wm@example.com", status = "Active",
        trainingCerts = listOf("ACLP", "ACTA"), certifications = listOf("PMP", "CCAR-F"),
        skills = listOf("Power BI", "Python"), financeBackground = true, ibfFitDeclaration = true,
        courseTitles = listOf("Data Analytics with Power BI"),
    )
    private val aisyah = TrainerListItem(
        id = "2", name = "NUR AISYAH", phone = "+65 8234 5678", status = "Active",
        trainingCerts = listOf("ACTA"), certifications = listOf("AWS Solutions Architect"), skills = listOf("python", "Docker"),
    )
    private val lim = TrainerListItem(
        id = "3", name = "LIM KAH HENG (DR)", status = "Applicant", trainingCerts = listOf("DDDLP"),
        certifications = listOf("CFA"), skills = listOf("Excel"), financeBackground = true,
    )
    private val all = listOf(tan, aisyah, lim)

    private fun ids(q: TrainerQuery) = TrainerSearch.filter(all, q).map { it.id }

    @Test
    fun statusSplitsTrainersFromApplicants() {
        assertEquals(listOf("1", "2"), ids(TrainerQuery(status = TrainerStatusFilter.TRAINERS)))
        assertEquals(listOf("3"), ids(TrainerQuery(status = TrainerStatusFilter.APPLICANTS)))
        assertEquals(listOf("1", "2", "3"), ids(TrainerQuery(status = TrainerStatusFilter.ALL)))
    }

    @Test
    fun everyQueryTermMustMatchAndAliasesWork() {
        assertEquals(listOf("1"), ids(TrainerQuery(q = "power bi", status = TrainerStatusFilter.ALL)))
        assertEquals(listOf("1", "2"), ids(TrainerQuery(q = "PYTHON", status = TrainerStatusFilter.ALL)))
        // "DDDPL" is the common misspelling of DDDLP; "ALCP" of ACLP.
        assertEquals(listOf("3"), ids(TrainerQuery(q = "dddpl", status = TrainerStatusFilter.ALL)))
        assertEquals(listOf("1"), ids(TrainerQuery(q = "alcp", status = TrainerStatusFilter.ALL)))
        assertEquals(emptyList<String>(), ids(TrainerQuery(q = "python excel", status = TrainerStatusFilter.ALL)))
    }

    @Test
    fun tagsAndAccreditationMustAllHold() {
        assertEquals(listOf("1", "2"), ids(TrainerQuery(tags = setOf("Python"), status = TrainerStatusFilter.ALL)))
        assertEquals(listOf("1"), ids(TrainerQuery(tags = setOf("Python", "PMP"), status = TrainerStatusFilter.ALL)))
        val base = TrainerQuery(status = TrainerStatusFilter.ALL)
        assertEquals(listOf("1"), ids(Accreditation.ACLP.applyTo(base)))
        assertEquals(listOf("1", "2"), ids(Accreditation.ACTA.applyTo(base)))
        assertEquals(listOf("1", "3"), ids(Accreditation.FINANCE.applyTo(base)))
        assertEquals(listOf("1"), ids(Accreditation.IBF.applyTo(base)))
        assertEquals(listOf("1", "2", "3"), ids(Accreditation.ANY.applyTo(Accreditation.ACLP.applyTo(base))))
    }

    @Test
    fun vocabularyCountsTrainersCertificationsFirstWithoutCaseDuplicates() {
        val vocab = TrainerSearch.vocabulary(listOf(tan, aisyah))
        val python = vocab.single { it.tag.equals("python", ignoreCase = true) }
        assertEquals(2, python.count)
        assertEquals(TagCount.Kind.SKILL, python.kind)
        assertEquals(TagCount.Kind.CERTIFICATION, vocab.first().kind)
        assertEquals(1, vocab.count { it.tag.equals("python", ignoreCase = true) })
    }

    @Test
    fun suggestionsPreferPrefixMatchesAndSkipChosenTags() {
        val vocab = TrainerSearch.vocabulary(all)
        assertEquals("Power BI", TrainerSearch.suggestions(vocab, "po", emptyList()).first().tag)
        assertTrue(TrainerSearch.suggestions(vocab, "", listOf("Python")).none { it.tag.equals("python", true) })
        // "architect" only appears mid-word, so it comes from the contains pass.
        assertEquals("AWS Solutions Architect", TrainerSearch.suggestions(vocab, "architect", emptyList()).single().tag)
    }

    @Test
    fun matchingExpertiseComesFirst() {
        assertEquals(
            listOf("Python", "PMP", "Power BI"),
            TrainerSearch.orderByMatch(listOf("PMP", "Power BI", "Python"), listOf("pyth")),
        )
    }

    @Test
    fun whatsAppAddsSingaporeCodeToEightDigitNumbers() {
        assertEquals("https://wa.me/6591234567", TrainerSearch.whatsAppUrl("9123 4567"))
        assertEquals("https://wa.me/6582345678", TrainerSearch.whatsAppUrl("+65 8234 5678"))
        assertNull(TrainerSearch.whatsAppUrl(null))
        assertNull(TrainerSearch.whatsAppUrl("n/a"))
    }

    @Test
    fun correctionAndOilPayloadsDecode() {
        val c = HrmsJson.decodeFromString<List<AttendanceCorrection>>(
            """[{"id":"c1","employeeId":"e5","employeeName":"Amanda","employeeCode":"I0002","date":"2026-11-05","kind":"CLOCK_OUT",
                "requestedClockIn":null,"requestedClockOut":"2026-11-05T10:00:00.000Z","currentClockIn":"2026-11-05T01:00:00.000Z",
                "currentClockOut":null,"reason":"Forgot","status":"PENDING","rejectionReason":null,"createdAt":"2026-11-06T01:00:00.000Z"}]""",
        ).single()
        assertEquals("CLOCK_OUT", c.kind)
        val o = HrmsJson.decodeFromString<List<OilEntry>>(
            """[{"id":"t1","employeeId":"e5","employeeCode":"I0002","employeeName":"Amanda","date":"2026-11-08","dayName":"Sun",
                "isPublicHoliday":true,"hours":"8","otCredited":0,"status":"PENDING","adminComment":null}]""",
        ).single()
        assertEquals(8.0, o.hours, 0.0)
        assertTrue(o.isPublicHoliday)
    }
}
