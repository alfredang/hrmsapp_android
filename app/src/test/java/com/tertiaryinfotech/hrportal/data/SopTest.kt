package com.tertiaryinfotech.hrportal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Native SOPs (`/api/mobile/sops`) and the SOP tab in Team's place for everyone but admins (iOS 22 Nov 2026). */
class SopTest {

    private inline fun <reified T> decode(json: String): T = HrmsJson.decodeFromString(json)

    private val sops = decode<SopsResponse>(
        """{"sops":[
             {"slug":"apply-leave","title":"Applying for leave","category":"Human Resources","summary":"How to apply.",
              "owner":"HR","triggers":["You are unwell (MC)"],
              "steps":[{"title":"Check your balance","detail":"Open Leave.","owner":"Employee"},
                       {"title":"Apply","detail":"Pick dates.","items":["MC on the day"],"timing":"Before the leave","note":"Wait for Approved.",
                        "links":[{"label":"Leave","href":"/leave"}]}],
              "references":[{"label":"HR","value":"hr@x.com","href":"mailto:hr@x.com"},{"label":"Policy","value":"Handbook"}]},
             {"slug":"class-setup","title":"Setting up a class","category":"Operations","summary":"Create the run.",
              "steps":[{"title":"Publish"}],"extra":"ignored"},
             {"slug":"bare","title":null,"category":null,"steps":null},
             {"slug":"appeal","title":"Appeal","category":"Human Resources","summary":"","steps":[]}]}""",
    ).sops

    @Test
    fun decodesLenientlyWithDefaults() {
        assertEquals(4, sops.size)
        val bare = sops[2]
        assertEquals("bare", bare.title)
        assertEquals("Other", bare.category)
        assertEquals("", bare.summary)
        assertTrue(bare.steps.isEmpty())
        assertNull(bare.references)
        val step = sops[1].steps.single()
        assertEquals("Publish", step.title)
        assertEquals("", step.detail)
        assertEquals("2 steps · HR", sops[0].stepsLine)
        assertEquals("1 step", sops[1].stepsLine)
        assertNull(sops[0].references!![1].href)
    }

    @Test
    fun everyWordMustMatchAcrossTheProcedure() {
        val leave = sops[0]
        assertTrue(leave.matches(""))
        assertTrue(leave.matches("leave"))
        assertTrue(leave.matches("APPLY  balance"))
        assertTrue(leave.matches("mc day"))            // a step item
        assertTrue(leave.matches("approved"))          // a step note
        assertTrue(leave.matches("employee hr"))       // step owner + owner
        assertTrue(leave.matches("unwell"))            // trigger
        assertFalse(leave.matches("leave publish"))
        assertTrue(sops[1].matches("operations publish"))
    }

    @Test
    fun categoriesKeepTheServerOrder() {
        assertEquals(listOf("Human Resources", "Operations", "Other"), SopRules.orderedCategories(sops))
    }

    @Test
    fun sitePathsOpenOnTheHrmsWebsite() {
        assertEquals("https://hrms.tertiaryinfotech.com/leave", SopRules.linkUrl("/leave"))
        assertEquals("mailto:hr@x.com", SopRules.linkUrl("mailto:hr@x.com"))
        assertEquals("https://example.com/a", SopRules.linkUrl("https://example.com/a"))
        assertNull(SopRules.linkUrl(" "))
    }

    @Test
    fun teamTabIsForAdminsOnlyEveryoneElseGetsSop() {
        // Admins: no Leave tab, SOP after Team (25 Nov 2026).
        assertEquals(listOf("home", "calendar", "team", "sops", "more"), MainTabs.routes(trainerView = false, admin = true, accountant = false))
        // tansc@ (ADMIN + ACCOUNTANT) in her own view is shown as an admin.
        assertEquals(listOf("home", "calendar", "team", "sops", "more"), MainTabs.routes(trainerView = false, admin = true, accountant = true))
        assertEquals(listOf("home", "leave", "calendar", "sops", "more"), MainTabs.routes(trainerView = false, admin = false, accountant = false))
        // Accountants: Income in Calendar's place, Expenses in Team's place.
        assertEquals(listOf("home", "finance_income", "finance_expenses", "more"), MainTabs.routes(trainerView = false, admin = false, accountant = true))
        // Trainer Admin view: no calendar; SOP after Trainers (25 Nov 2026).
        assertEquals(listOf("home", "trainers", "sops", "more"), MainTabs.routes(trainerView = true, admin = false, accountant = false))
    }

    @Test
    fun moreDocumentsOpensTheNativeSopList() {
        val roles = listOf(
            ShownRole(true, false, false, false, false) to false,
            ShownRole(false, false, false, false, false) to false,
            ShownRole(false, false, false, false, false) to true,
        )
        roles.forEach { (shown, trainerView) ->
            val row = MoreMenu.sections(shown, trainerView, canManageTrainers = true).first { it.title == "Documents" }.rows.single()
            assertEquals("sop", row.id)
            assertEquals(MoreMenu.Target.Route("sop_list"), row.target)
        }
    }
}
