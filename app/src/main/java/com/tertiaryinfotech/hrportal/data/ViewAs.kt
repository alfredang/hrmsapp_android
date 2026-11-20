package com.tertiaryinfotech.hrportal.data

/**
 * View As (Alfred, 20 Nov 2026; web 1a394c4 view mode "trainer"): a Trainer Admin holder can switch the app
 * between their normal view and a Trainer Admin view that shows only the trainer directory and its counts.
 */
enum class ViewAsMode(val key: String, val label: String) {
    MINE("mine", "My view"),
    TRAINER("trainer", "Trainer Admin");

    companion object {
        fun fromKey(key: String?): ViewAsMode = entries.firstOrNull { it.key == key } ?: MINE
    }
}

/** Pure View As rules, unit-tested in `ViewAsTest`. */
object ViewAsRules {
    /**
     * Whether the Trainer Admin view is in force. A saved Trainer Admin choice is trusted until
     * `/api/mobile/summary` has answered ([capabilitiesKnown]), so the tabs do not flicker on launch;
     * once it has, only a holder of the role ([canManageTrainers]) gets it.
     */
    fun isTrainerView(saved: ViewAsMode, canManageTrainers: Boolean, capabilitiesKnown: Boolean): Boolean =
        saved == ViewAsMode.TRAINER && (canManageTrainers || !capabilitiesKnown)

    /** The saved choice to keep after the summary answered: a user who lost the role goes back to My view. */
    fun afterCapabilities(saved: ViewAsMode, canManageTrainers: Boolean): ViewAsMode =
        if (canManageTrainers) saved else ViewAsMode.MINE

    /** Home tiles of the Trainer Admin view, each with the directory filter it opens. */
    fun trainerTiles(t: TrainerKpis): List<Pair<HomeTile, AclpFilter>> =
        HomeTiles.trainers(t).zip(listOf(AclpFilter.ALL, AclpFilter.ACLP, AclpFilter.NON_ACLP))
}
