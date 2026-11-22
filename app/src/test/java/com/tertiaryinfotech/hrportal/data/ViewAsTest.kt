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
        assertEquals(ViewAsMode.MINE, ViewAsRules.afterCapabilities(ViewAsMode.TRAINER, canManageTrainers = false, isApprover = true, canViewAs = true))
        assertEquals(ViewAsMode.TRAINER, ViewAsRules.afterCapabilities(ViewAsMode.TRAINER, canManageTrainers = true, isApprover = false, canViewAs = false))
        assertEquals(ViewAsMode.MINE, ViewAsRules.afterCapabilities(ViewAsMode.MINE, canManageTrainers = true, isApprover = false, canViewAs = false))
        // Role previews are for approvers on the View As list only.
        assertEquals(ViewAsMode.STAFF, ViewAsRules.afterCapabilities(ViewAsMode.STAFF, canManageTrainers = false, isApprover = true, canViewAs = true))
        assertEquals(ViewAsMode.MINE, ViewAsRules.afterCapabilities(ViewAsMode.STAFF, canManageTrainers = false, isApprover = true, canViewAs = false))
        assertEquals(ViewAsMode.MINE, ViewAsRules.afterCapabilities(ViewAsMode.ACCOUNTANT, canManageTrainers = true, isApprover = false, canViewAs = true))
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
    fun onlyAlfredPreviewsRolesTrainerAdminsOnlyTheirView() {
        assertEquals(
            listOf(ViewAsMode.MINE, ViewAsMode.STAFF, ViewAsMode.INTERN, ViewAsMode.ACCOUNTANT, ViewAsMode.TRAINER),
            ViewAsRules.available(isApprover = true, canManageTrainers = true, canViewAs = true),
        )
        // tansc@ (ADMIN + TRAINER_ADMIN, not on the list): My view / Trainer Admin only.
        assertEquals(listOf(ViewAsMode.MINE, ViewAsMode.TRAINER), ViewAsRules.available(isApprover = true, canManageTrainers = true, canViewAs = false))
        // Another approver: no switch at all.
        assertEquals(listOf(ViewAsMode.MINE), ViewAsRules.available(isApprover = true, canManageTrainers = false, canViewAs = false))
        assertEquals(listOf(ViewAsMode.MINE, ViewAsMode.TRAINER), ViewAsRules.available(isApprover = false, canManageTrainers = true, canViewAs = false))
        assertEquals(listOf(ViewAsMode.MINE), ViewAsRules.available(isApprover = false, canManageTrainers = false, canViewAs = false))
        // The list is matched on the trimmed, lower-cased email.
        assertTrue(ViewAsRules.canViewAs(" AngCH@TertiaryInfotech.com "))
        assertFalse(ViewAsRules.canViewAs("tansc@tertiaryinfotech.com"))
        assertFalse(ViewAsRules.canViewAs(null))
    }

    @Test
    fun aSavedPreviewIsIgnoredOffTheList() {
        val roles = listOf("ADMIN", "ACCOUNTANT", "TRAINER_ADMIN")
        val shown = ViewAsRules.shown(ViewAsMode.STAFF, roles, "ADMIN", trainerView = false, canViewAs = false)
        assertFalse(shown.preview)
        assertTrue(shown.admin)
        assertFalse(ViewAsRules.isRolePreview(ViewAsMode.INTERN, isApprover = true, canViewAs = false))
        assertTrue(ViewAsRules.isRolePreview(ViewAsMode.INTERN, isApprover = true, canViewAs = true))
        assertEquals(null, ViewAsRules.summaryView(ViewAsMode.ACCOUNTANT, isApprover = true, canViewAs = false))
    }

    @Test
    fun shownRoleFollowsThePreviewWhileFormsKeepTheRealRoles() {
        val all = listOf("STAFF", "MANAGER", "HR", "ADMIN", "INTERN", "ACCOUNTANT", "TRAINER_ADMIN")
        val own = ViewAsRules.shown(ViewAsMode.MINE, all, "ADMIN", trainerView = false, canViewAs = true)
        assertEquals(ShownRole(admin = true, intern = false, mustClockIn = false, accountant = false, preview = false), own)
        assertEquals(ShownRole(false, intern = true, mustClockIn = true, accountant = false, preview = true),
            ViewAsRules.shown(ViewAsMode.INTERN, all, "ADMIN", trainerView = false, canViewAs = true))
        assertEquals(ShownRole(false, intern = false, mustClockIn = true, accountant = true, preview = true),
            ViewAsRules.shown(ViewAsMode.ACCOUNTANT, all, "ADMIN", trainerView = false, canViewAs = true))
        assertEquals(ShownRole(false, false, false, false, preview = true),
            ViewAsRules.shown(ViewAsMode.STAFF, all, "ADMIN", trainerView = false, canViewAs = true))
        // A real accountant (no approver role): accountant screens, clocks in, no preview.
        assertEquals(ShownRole(false, false, mustClockIn = true, accountant = true, preview = false),
            ViewAsRules.shown(ViewAsMode.MINE, listOf("STAFF", "ACCOUNTANT"), "STAFF", trainerView = false, canViewAs = true))
        // A non-approver cannot preview: a stale saved STAFF choice shows their own screens.
        assertEquals(ShownRole(false, intern = true, mustClockIn = true, accountant = false, preview = false),
            ViewAsRules.shown(ViewAsMode.STAFF, listOf("INTERN"), "INTERN", trainerView = false, canViewAs = true))
        // tansc@ (ADMIN, ACCOUNTANT, TRAINER_ADMIN) in her own view: admin screens.
        assertTrue(ViewAsRules.shown(ViewAsMode.MINE, listOf("ADMIN", "ACCOUNTANT", "TRAINER_ADMIN"), "ADMIN", false, canViewAs = false).admin)
    }

    @Test
    fun previewsFetchTheSummaryForTheirRole() {
        assertEquals("staff", ViewAsRules.summaryView(ViewAsMode.STAFF, isApprover = true, canViewAs = true))
        assertEquals("intern", ViewAsRules.summaryView(ViewAsMode.INTERN, isApprover = true, canViewAs = true))
        assertEquals("accountant", ViewAsRules.summaryView(ViewAsMode.ACCOUNTANT, isApprover = true, canViewAs = true))
        assertEquals(null, ViewAsRules.summaryView(ViewAsMode.MINE, isApprover = true, canViewAs = true))
        assertEquals(null, ViewAsRules.summaryView(ViewAsMode.TRAINER, isApprover = true, canViewAs = true))
        assertEquals(null, ViewAsRules.summaryView(ViewAsMode.STAFF, isApprover = false, canViewAs = true))
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
