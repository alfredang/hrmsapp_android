package com.tertiaryinfotech.hrportal.data

/**
 * View As (web view modes, 20–22 Nov 2026; iOS `ViewAs`). Approvers (ADMIN / HR / MANAGER) on the View As list
 * ([ViewAsRules.VIEW_AS_EMAILS], Alfred only since 22 Nov 2026) can preview the Staff, Intern and Accountant screens; Trainer Admin holders can switch to the Trainer Admin view. MINE is the
 * person's own view ("Admin" for approvers). Only what is SHOWN changes: forms, approvals and every API call
 * still act as the signed-in person, and the server enforces RBAC.
 */
enum class ViewAsMode(val key: String, val label: String) {
    MINE("mine", "My view"),
    STAFF("staff", "Staff"),
    INTERN("intern", "Intern"),
    ACCOUNTANT("accountant", "Accountant"),
    TRAINER("trainer", "Trainer Admin");

    /** The menu title: an approver's own view is "Admin". */
    fun title(isApprover: Boolean): String = if (this == MINE && isApprover) "Admin" else label

    /** A role preview (Staff / Intern / Accountant), as opposed to the own or Trainer Admin view. */
    val isRolePreview: Boolean get() = this == STAFF || this == INTERN || this == ACCOUNTANT

    companion object {
        fun fromKey(key: String?): ViewAsMode = entries.firstOrNull { it.key == key } ?: MINE
    }
}

/**
 * What the screens show for the current view (iOS `showsAdmin` / `showsIntern` / `showsMustClockIn` /
 * `showsAccountant`). Business rules and forms keep the real roles.
 */
data class ShownRole(
    val admin: Boolean,
    val intern: Boolean,
    val mustClockIn: Boolean,
    val accountant: Boolean,
    /** An approver previewing another role: clock-in is disabled, the summary is fetched with `?view=`. */
    val preview: Boolean,
)

/** Pure View As rules, unit-tested in `ViewAsTest`. */
object ViewAsRules {
    /**
     * Previewing other roles (Staff / Intern / Accountant) is Alfred's only (22 Nov 2026; web
     * `src/lib/view-as-policy.ts`, iOS `AuthViewModel.viewAsEmails`). Trainer Admins (Deepa, Tan Soik Ching)
     * keep the My view / Trainer Admin switch.
     */
    val VIEW_AS_EMAILS = listOf("angch@tertiaryinfotech.com")

    /** Whether this signed-in email may preview other roles. */
    fun canViewAs(email: String?): Boolean = (email ?: "").trim().lowercase() in VIEW_AS_EMAILS

    /**
     * The views this person may pick: their own, the role previews for approvers allowed to preview
     * ([canViewAs]), Trainer Admin for holders.
     */
    fun available(isApprover: Boolean, canManageTrainers: Boolean, canViewAs: Boolean): List<ViewAsMode> = buildList {
        add(ViewAsMode.MINE)
        if (canViewAs && isApprover) addAll(listOf(ViewAsMode.STAFF, ViewAsMode.INTERN, ViewAsMode.ACCOUNTANT))
        if (canManageTrainers) add(ViewAsMode.TRAINER)
    }

    /** An approver allowed to preview ([canViewAs]) previewing another role (Staff / Intern / Accountant). */
    fun isRolePreview(saved: ViewAsMode, isApprover: Boolean, canViewAs: Boolean): Boolean =
        canViewAs && isApprover && saved.isRolePreview

    /**
     * Whether the Trainer Admin view is in force. A saved Trainer Admin choice is trusted until
     * `/api/mobile/summary` has answered ([capabilitiesKnown]), so the tabs do not flicker on launch;
     * once it has, only a holder of the role ([canManageTrainers]) gets it.
     */
    fun isTrainerView(saved: ViewAsMode, canManageTrainers: Boolean, capabilitiesKnown: Boolean): Boolean =
        saved == ViewAsMode.TRAINER && (canManageTrainers || !capabilitiesKnown)

    /** The saved choice to keep after the summary answered: a choice no longer allowed goes back to My view. */
    fun afterCapabilities(saved: ViewAsMode, canManageTrainers: Boolean, isApprover: Boolean, canViewAs: Boolean): ViewAsMode =
        if (saved in available(isApprover, canManageTrainers, canViewAs)) saved else ViewAsMode.MINE

    /**
     * What the screens show. [roles] / [role] are the signed-in person's real roles. A saved role preview is
     * ignored unless the person may preview ([canViewAs]).
     */
    fun shown(saved: ViewAsMode, roles: List<String>?, role: String?, trainerView: Boolean, canViewAs: Boolean): ShownRole {
        val approver = LeaveRules.isApprover(roles, role)
        val preview = isRolePreview(saved, approver, canViewAs)
        return if (preview) {
            ShownRole(
                admin = false,
                intern = saved == ViewAsMode.INTERN,
                mustClockIn = saved == ViewAsMode.INTERN || saved == ViewAsMode.ACCOUNTANT,
                accountant = saved == ViewAsMode.ACCOUNTANT,
                preview = true,
            )
        } else {
            ShownRole(
                admin = approver && !trainerView,
                intern = LeaveRules.isIntern(roles, role),
                mustClockIn = LeaveRules.mustClockIn(roles, role),
                accountant = LeaveRules.isAccountant(roles, role),
                preview = false,
            )
        }
    }

    /** The `?view=` of `/api/mobile/summary`: the previewed role, so the server adds its tiles. */
    fun summaryView(saved: ViewAsMode, isApprover: Boolean, canViewAs: Boolean): String? =
        if (isRolePreview(saved, isApprover, canViewAs)) saved.key else null

    /** Home tiles of the Trainer Admin view, each with the directory filter it opens. */
    fun trainerTiles(t: TrainerKpis): List<Pair<HomeTile, AclpFilter>> =
        HomeTiles.trainers(t).zip(listOf(AclpFilter.ALL, AclpFilter.ACLP, AclpFilter.NON_ACLP))
}

/**
 * Approver Home: what is waiting for a decision, by type (the web dashboard's pending counts, less No Pay Leave
 * payback, which the app does not handle). Built from the same feeds as Approvals and the admin Timesheet, so the
 * Home numbers match those screens (iOS `PendingApprovals`).
 */
data class PendingApprovals(
    val leave: Int = 0,
    val mc: Int = 0,
    val timeOff: Int = 0,
    val claims: Int = 0,
    val punches: Int = 0,
) {
    val total: Int get() = leave + mc + timeOff + claims + punches

    companion object {
        fun from(approvals: ApprovalsResponse, timeOff: List<TimeOffRequest>, punches: Int): PendingApprovals {
            val mc = approvals.leaves.count { LeaveRules.isMedical(it.leaveCode) }
            return PendingApprovals(
                leave = approvals.leaves.size - mc,
                mc = mc,
                timeOff = timeOff.count { it.status.equals("PENDING", ignoreCase = true) },
                claims = approvals.claims.size,
                punches = punches,
            )
        }
    }
}

/** Force update: dotted version strings compared numerically ("1.9" < "1.10"). */
object AppVersion {
    fun compare(a: String, b: String): Int {
        val x = a.split('.').map { it.trim().toIntOrNull() ?: 0 }
        val y = b.split('.').map { it.trim().toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = (x.getOrElse(i) { 0 }).compareTo(y.getOrElse(i) { 0 })
            if (d != 0) return d
        }
        return 0
    }

    /** True when the store has a newer version than this app (null live version = no forcing). */
    fun mustUpdate(current: String, live: String?): Boolean =
        !live.isNullOrBlank() && compare(current, live) < 0
}
