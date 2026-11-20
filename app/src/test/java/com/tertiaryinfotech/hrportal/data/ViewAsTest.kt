package com.tertiaryinfotech.hrportal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** View As (Trainer Admin view), web 1a394c4 view mode "trainer". */
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
        assertEquals(ViewAsMode.MINE, ViewAsRules.afterCapabilities(ViewAsMode.TRAINER, canManageTrainers = false))
        assertEquals(ViewAsMode.TRAINER, ViewAsRules.afterCapabilities(ViewAsMode.TRAINER, canManageTrainers = true))
        assertEquals(ViewAsMode.MINE, ViewAsRules.afterCapabilities(ViewAsMode.MINE, canManageTrainers = true))
    }

    @Test
    fun savedKeysRoundTripAndUnknownIsMyView() {
        ViewAsMode.entries.forEach { assertEquals(it, ViewAsMode.fromKey(it.key)) }
        assertEquals(ViewAsMode.MINE, ViewAsMode.fromKey(null))
        assertEquals(ViewAsMode.MINE, ViewAsMode.fromKey("staff"))
        assertEquals(listOf("My view", "Trainer Admin"), ViewAsMode.entries.map { it.label })
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
