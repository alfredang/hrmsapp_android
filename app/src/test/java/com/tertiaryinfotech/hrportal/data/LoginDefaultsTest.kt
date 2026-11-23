package com.tertiaryinfotech.hrportal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The first login screen's default per email (iOS `usesGoogleByDefault`, 22 Nov 2026). */
class LoginDefaultsTest {

    @Test
    fun companyEmailsUseGoogleEveryoneElseACode() {
        assertTrue(LoginDefaults.usesGoogle("angch@tertiaryinfotech.com", googleConfigured = true))
        assertTrue(LoginDefaults.usesGoogle("  Deepa@TertiaryInfotech.com ", googleConfigured = true))
        // No Google client id in this build: a code instead.
        assertFalse(LoginDefaults.usesGoogle("angch@tertiaryinfotech.com", googleConfigured = false))
        assertFalse(LoginDefaults.usesGoogle("intern@gmail.com", googleConfigured = true))
        assertFalse(LoginDefaults.usesGoogle("someone@tertiaryinfotech.com.evil.com", googleConfigured = true))
        // No email yet: Google shows too (23 Nov 2026), unless the build has no client id.
        assertTrue(LoginDefaults.usesGoogle("", googleConfigured = true))
        assertTrue(LoginDefaults.usesGoogle("  ", googleConfigured = true))
        assertTrue(LoginDefaults.usesGoogle(null, googleConfigured = true))
        assertFalse(LoginDefaults.usesGoogle("", googleConfigured = false))
    }

    @Test
    fun titleWelcomesBackOnceAnEmailIsFilled() {
        assertEquals("Sign in", LoginDefaults.title(""))
        assertEquals("Sign in", LoginDefaults.title(null))
        assertEquals("Welcome back", LoginDefaults.title("a@b.com"))
        assertEquals("Enter your email. Staff sign in with Google, interns and part-time staff with a code.", LoginDefaults.SUBTITLE)
    }
}
