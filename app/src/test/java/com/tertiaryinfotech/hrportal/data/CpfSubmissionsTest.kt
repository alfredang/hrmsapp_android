package com.tertiaryinfotech.hrportal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** CPF EZPay submissions (web, 26 Nov 2026): the list, the payslip link and who sees them. */
class CpfSubmissionsTest {

    private inline fun <reified T> decode(json: String): T = HrmsJson.decodeFromString(json)

    @Test
    fun listDecodesInServerOrderWithNullFields() {
        val r = decode<CpfSubmissionsResponse>(
            """{"cpfSubmissions":[
                 {"id":"f2","name":"CPF Nov 2026.pdf","month":11,"year":2026,"periodLabel":"November 2026",
                  "sizeBytes":421888,"uploadedAt":"2026-11-26T02:15:00.000Z","downloadPath":"/api/payroll/cpf-submission/files/f2"},
                 {"id":"f1","name":"scan.pdf","month":null,"year":null,"periodLabel":null,"sizeBytes":null,
                  "uploadedAt":null,"downloadPath":"/api/payroll/cpf-submission/files/f1"},
                 {"id":"f0","name":"older.pdf","downloadPath":"/api/payroll/cpf-submission/files/f0","extra":"ignored"}]}""",
        )
        assertEquals("server order is kept (latest first)", listOf("f2", "f1", "f0"), r.cpfSubmissions.map { it.id })
        val first = r.cpfSubmissions[0]
        assertEquals(11, first.month)
        assertEquals(2026, first.year)
        assertEquals(421888L, first.sizeBytes)
        assertEquals("/api/payroll/cpf-submission/files/f2", first.downloadPath)
        val bare = r.cpfSubmissions[1]
        assertNull(bare.month)
        assertNull(bare.year)
        assertNull(bare.periodLabel)
        assertNull(bare.sizeBytes)
        assertNull(bare.uploadedAt)
        assertNull("absent fields default to null", r.cpfSubmissions[2].periodLabel)
        assertTrue(decode<CpfSubmissionsResponse>("{}").cpfSubmissions.isEmpty())
    }

    @Test
    fun payslipCpfSubmissionIsOptional() {
        val r = decode<PayslipsResponse>(
            """{"payslips":[
                 {"id":"p1","netSalary":3600,"status":"PAID","pdfPath":"/api/payroll/payslip/p1/pdf",
                  "cpfSubmission":{"id":"f2","name":"CPF Nov 2026.pdf","periodLabel":"November 2026","downloadPath":"/api/payroll/cpf-submission/files/f2"}},
                 {"id":"p2","netSalary":3600,"status":"PAID","pdfPath":"/api/payroll/payslip/p2/pdf","cpfSubmission":null},
                 {"id":"p3","netSalary":3600,"status":"PAID","pdfPath":"/api/payroll/payslip/p3/pdf"},
                 {"id":"p4","pdfPath":"/p4","cpfSubmission":{"id":"f9","name":"x.pdf","downloadPath":"/api/payroll/cpf-submission/files/f9"}}]}""",
        )
        val cpf = r.payslips[0].cpfSubmission
        assertNotNull(cpf)
        assertEquals("f2", cpf!!.id)
        assertEquals("/api/payroll/cpf-submission/files/f2", cpf.downloadPath)
        assertEquals("November 2026", CpfSubmissions.title(cpf))
        assertNull("explicit null (staff, or no statement on file)", r.payslips[1].cpfSubmission)
        assertNull("an older server does not send it", r.payslips[2].cpfSubmission)
        val noPeriod = r.payslips[3].cpfSubmission!!
        assertNull(noPeriod.periodLabel)
        assertEquals("falls back to the file name", "x.pdf", CpfSubmissions.title(noPeriod))
    }

    @Test
    fun rowTitleAndSubtitle() {
        val full = CpfSubmission("f2", "CPF Nov 2026.pdf", 11, 2026, "November 2026", 421888, "2026-11-26T02:15:00.000Z", "/d")
        assertEquals("November 2026", CpfSubmissions.title(full))
        val sub = CpfSubmissions.subtitle(full)
        assertTrue(sub, sub.startsWith("CPF Nov 2026.pdf · Uploaded "))
        assertTrue(sub, sub.endsWith(" · 412 KB"))

        val bare = CpfSubmission(id = "f1", name = "scan.pdf", periodLabel = " ")
        assertEquals("blank period falls back to the file name", "scan.pdf", CpfSubmissions.title(bare))
        assertEquals("the file name is not repeated under itself", "", CpfSubmissions.subtitle(bare))
        assertEquals("only what is known is shown", "412 KB", CpfSubmissions.subtitle(bare.copy(sizeBytes = 421888)))
        assertEquals("CPF statement", CpfSubmissions.title(CpfSubmission(id = "f0")))
    }

    @Test
    fun fileSizes() {
        assertEquals("812 B", CpfSubmissions.fileSize(812))
        assertEquals("1 KB", CpfSubmissions.fileSize(1024))
        assertEquals("412 KB", CpfSubmissions.fileSize(421888))
        assertEquals("1.3 MB", CpfSubmissions.fileSize(1_363_149))
        assertNull(CpfSubmissions.fileSize(-1))
    }

    @Test
    fun onlyAdminAndAccountantViewsSeeThem() {
        // ShownRole(admin, intern, mustClockIn, accountant, preview)
        assertTrue("admin view", CpfSubmissions.canView(ShownRole(true, false, false, false, false)))
        assertTrue("accountant", CpfSubmissions.canView(ShownRole(false, false, true, true, false)))
        assertTrue("admin previewing the accountant view", CpfSubmissions.canView(ShownRole(false, false, true, true, true)))
        assertFalse("staff", CpfSubmissions.canView(ShownRole(false, false, false, false, false)))
        assertFalse("intern", CpfSubmissions.canView(ShownRole(false, true, true, false, false)))
        assertFalse("admin previewing staff", CpfSubmissions.canView(ShownRole(false, false, false, false, true)))
    }
}
