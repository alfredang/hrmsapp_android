package com.tertiaryinfotech.hrportal.ui

import com.tertiaryinfotech.hrportal.ui.theme.Appearance
import com.tertiaryinfotech.hrportal.ui.theme.AppearanceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppearanceTest {
    @Test
    fun `two accounts start on Light, everyone else on System`() {
        assertEquals(AppearanceMode.LIGHT, Appearance.defaultFor("angch@tertiaryinfotech.com"))
        assertEquals(AppearanceMode.LIGHT, Appearance.defaultFor("  TanSC@TertiaryInfotech.com "))
        assertEquals(AppearanceMode.SYSTEM, Appearance.defaultFor("someone@tertiaryinfotech.com"))
        assertEquals(AppearanceMode.SYSTEM, Appearance.defaultFor(null))
    }

    @Test
    fun `saved keys round-trip and unknown keys are ignored`() {
        AppearanceMode.entries.forEach { assertEquals(it, AppearanceMode.fromKey(it.key)) }
        assertNull(AppearanceMode.fromKey("sepia"))
        assertNull(AppearanceMode.fromKey(null))
    }
}
