package com.tertiaryinfotech.hrportal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** View As (role previews and the Trainer Admin view); web view modes, iOS `ViewAs`. */
class ViewAsTest {

    @Test
    fun trainerViewNeedsTheRoleOnceTheSummaryAnswered() {
        assertTrue(ViewAsRules.isTrainerView(ViewAsMode.TRAINER, canManageTrainers = true, capabilitiesKnown = true))
        assertFalse(ViewAsRules.isTrainerView(ViewAsMode.TRAINER, canManageTrainers = false, capabilitiesKnown = true))
        // Before the summary answers, the saved choice holds (no tab flicker on launch).
        assertTrue(ViewAsRules.isTrainerView(ViewAsMode.TRAINER, canManageTrainers = false, capabilitiesKnown = false))
        assertFalse(ViewAsRules.isTrainerView(ViewAsMode.MINE, canManageTrainers = true, capabilitiesKnown = true))
        assertFalse(ViewAsRules.isTrainerView(ViewAsMode.MINE, canManageTrainers = false, capabilitiesKnown = false))
    }

    @Test
    fun losingTheRoleFallsBackToMyView() {
        assertEquals(ViewAsMode.MINE, ViewAsRules.afterCapabilities(ViewAsMode.TRAINER, canManageTrainers = false, isApprover = true))
        assertEquals(ViewAsMode.TRAINER, ViewAsRules.afterCapabilities(ViewAsMode.TRAINER, canManageTrainers = true, isApprover = false))
        assertEquals(ViewAsMode.MINE, ViewAsRules.afterCapabilities(ViewAsMode.MINE, canManageTrainers = true, isApprover = false))
        // Role previews are for approvers only.
        assertEquals(ViewAsMode.STAFF, ViewAsRules.afterCapabilities(ViewAsMode.STAFF, canManageTrainers = false, isApprover = true))
        assertEquals(ViewAsMode.MINE, ViewAsRules.afterCapabilities(ViewAsMode.ACCOUNTANT, canManageTrainers = true, isApprover = false))
    }

    @Test
    fun savedKeysRoundTripAndUnknownIsMyView() {
        ViewAsMode.entries.forEach { assertEquals(it, ViewAsMode.fromKey(it.key)) }
        assertEquals(ViewAsMode.MINE, ViewAsMode.fromKey(null))
        assertEquals(ViewAsMode.MINE, ViewAsMode.fromKey("admin"))
        assertEquals(listOf("My view", "Staff", "Intern", "Accountant", "Trainer Admin"), ViewAsMode.entries.map { it.label })
        assertEquals("Admin", ViewAsMode.MINE.title(isApprover = true))
        assertEquals("My view", ViewAsMode.MINE.title(isApprover = false))
    }

    @Test
    fun approversPreviewEveryRoleTrainerAdminsOnlyTheirView() {
        assertEquals(
            listOf(ViewAsMode.MINE, ViewAsMode.STAFF, ViewAsMode.INTERN, ViewAsMode.ACCOUNTANT, ViewAsMode.TRAINER),
            ViewAsRules.available(isApprover = true, canManageTrainers = true),
        )
        assertEquals(listOf(ViewAsMode.MINE, ViewAsMode.TRAINER), ViewAsRules.available(isApprover = false, canManageTrainers = true))
        assertEquals(listOf(ViewAsMode.MINE), ViewAsRules.available(isApprover = false, canManageTrainers = false))
    }

    @Test
    fun shownRoleFollowsThePreviewWhileFormsKeepTheRealRoles() {
        val all = listOf("STAFF", "MANAGER", "HR", "ADMIN", "INTERN", "ACCOUNTANT", "TRAINER_ADMIN")
        val own = ViewAsRules.shown(ViewAsMode.MINE, all, "ADMIN", trainerView = false)
        assertEquals(ShownRole(admin = true, intern = false, mustClockIn = false, accountant = false, preview = false), own)
        assertEquals(ShownRole(false, intern = true, mustClockIn = true, accountant = false, preview = true),
            ViewAsRules.shown(ViewAsMode.INTERN, all, "ADMIN", trainerView = false))
        assertEquals(ShownRole(false, intern = false, mustClockIn = true, accountant = true, preview = true),
            ViewAsRules.shown(ViewAsMode.ACCOUNTANT, all, "ADMIN", trainerView = false))
        assertEquals(ShownRole(false, false, false, false, preview = true),
            ViewAsRules.shown(ViewAsMode.STAFF, all, "ADMIN", trainerView = false))
        // A real accountant (no approver role): accountant screens, clocks in, no preview.
        assertEquals(ShownRole(false, false, mustClockIn = true, accountant = true, preview = false),
            ViewAsRules.shown(ViewAsMode.MINE, listOf("STAFF", "ACCOUNTANT"), "STAFF", trainerView = false))
        // A non-approver cannot preview: a stale saved STAFF choice shows their own screens.
        assertEquals(ShownRole(false, intern = true, mustClockIn = true, accountant = false, preview = false),
            ViewAsRules.shown(ViewAsMode.STAFF, listOf("INTERN"), "INTERN", trainerView = false))
        // tansc@ (ADMIN, ACCOUNTANT, TRAINER_ADMIN) in her own view: admin screens.
        assertTrue(ViewAsRules.shown(ViewAsMode.MINE, listOf("ADMIN", "ACCOUNTANT", "TRAINER_ADMIN"), "ADMIN", false).admin)
    }

    @Test
    fun previewsFetchTheSummaryForTheirRole() {
        assertEquals("staff", ViewAsRules.summaryView(ViewAsMode.STAFF, isApprover = true))
        assertEquals("intern", ViewAsRules.summaryView(ViewAsMode.INTERN, isApprover = true))
        assertEquals("accountant", ViewAsRules.summaryView(ViewAsMode.ACCOUNTANT, isApprover = true))
        assertEquals(null, ViewAsRules.summaryView(ViewAsMode.MINE, isApprover = true))
        assertEquals(null, ViewAsRules.summaryView(ViewAsMode.TRAINER, isApprover = true))
        assertEquals(null, ViewAsRules.summaryView(ViewAsMode.STAFF, isApprover = false))
    }

    @Test
    fun homeTilesOpenTheirOwnDirectoryFilter() {
        val tiles = ViewAsRules.trainerTiles(TrainerKpis(total = 40, aclp = 25, nonAclp = 15))
        assertEquals(
            listOf(
                Triple("Trainers", "40", AclpFilter.ALL),
                Triple("ACLP trainers", "25", AclpFilter.ACLP),
                Triple("Non-ACLP trainers", "15", AclpFilter.NON_ACLP),
            ),
            tiles.map { (t, f) -> Triple(t.label, t.value, f) },
        )
    }
}
