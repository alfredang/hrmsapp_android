package com.tertiaryinfotech.hrportal.data

/**
 * More tab = the web sidebar (Alfred, 22 Nov 2026; iOS `MoreView.sections`): My Stuff, People, Processes,
 * Finance, Accounting, Documents and System, each row shown under the same rules as the web
 * (`src/components/layout/sidebar.tsx` `isVisible`). Screens the app has natively open in the app;
 * accounting, SOP and settings open the web page. Departures from the web, by product decision:
 * Clock In/Out only for people who must clock in (never admins or staff), Approvals as its own row
 * for admins, no Woods Square, no NPL Payback. The Trainer Admin view shows My Stuff, People →
 * Trainers and Documents only (web TRAINER_VIEW_HREFS). Pure, unit-tested in `MoreMenuTest`.
 */
object MoreMenu {
    sealed interface Target {
        /** A screen in the app (a NavHost route). */
        data class Route(val route: String) : Target
        /** The leave form preset to a type ("AL" | "MC"). */
        data class Apply(val code: String) : Target
        /** A web page: a path on the HRMS web app, or a full URL. */
        data class Web(val path: String) : Target
    }

    /** [icon] is a key the screen maps to an icon and tint. */
    data class Row(val id: String, val title: String, val icon: String, val target: Target)
    data class Section(val title: String, val rows: List<Row>)

    /** Web sidebar "Bank Statements": the shared Drive folder of DBS statements. */
    const val BANK_STATEMENTS_FOLDER = "https://drive.google.com/drive/u/1/folders/1U6MCWuKZQ4wWZqn36AVHUeOD7a-URAaY"

    private fun route(id: String, title: String, icon: String, route: String) = Row(id, title, icon, Target.Route(route))
    private fun web(id: String, title: String, icon: String, path: String) = Row(id, title, icon, Target.Web(path))

    /** The sections, as the web sidebar shows them for the current view; empty sections are dropped. */
    fun sections(shown: ShownRole, trainerView: Boolean, canManageTrainers: Boolean): List<Section> {
        val myStuff = Section("My Stuff", listOf(
            route("profile", "My Profile", "profile", "profile"),
            route("calendar", "Calendar", "calendar", "calendar_list"),
        ))
        val documents = Section("Documents", listOf(web("sop", "SOP", "sop", "sop")))
        if (trainerView) {
            return listOf(
                myStuff,
                Section("People", listOf(route("trainers", "Trainers", "trainers", "trainers"))),
                documents,
            )
        }
        val admin = shown.admin
        val accountant = shown.accountant && !admin
        val intern = shown.intern && !admin
        val staff = !admin && !accountant && !intern

        val people = buildList {
            if (admin) {
                add(route("staff", "Staff", "staff", "team_group/staff"))
                add(route("interns", "Interns", "interns", "team_group/interns"))
            }
            if (canManageTrainers && !shown.preview) add(route("trainers", "Trainers", "trainers", "trainers"))
            // Everyone can reach a trainer (app-only contact list; the directory is Trainer Admin's).
            else add(route("trainercontacts", "Trainer contacts", "trainercontacts", "trainer_contacts"))
        }

        val processes = buildList {
            if (admin) {
                add(route("approvals", "Approvals", "approvals", "approvals"))
                add(route("leavemgmt", "Leave Management", "leave", "mc_leave_summary"))
                add(route("timeoff", "Time Off Approvals", "timeoff", "time_off"))
                add(route("timesheet", "Timesheet", "timesheet_admin", "timesheet_admin"))
            } else if (!accountant) {
                add(Row("AL", "Annual Leave", "annual", Target.Apply("AL")))
                add(Row("MC", "Medical Leave", "medical", Target.Apply("MC")))
                add(route("timeoff", "Time Off Application", "timeoff", "time_off"))
            }
            if (!admin && shown.mustClockIn) add(route("clock", "Clock In/Out", "clock", "timesheet"))
            if (intern || accountant) add(route("mytimesheet", "Timesheet", "mytimesheet", "my_timesheet"))
            if (staff) add(route("offinlieu", "Off In Lieu", "offinlieu", "timesheet_week"))
        }

        val finance = buildList {
            if (!intern) add(route("payslips", "Payslips", "payslips", "payslips"))
            if (admin) add(route("claims", "Claim Management", "claims", "approvals"))
            else if (!accountant) add(route("claims", "Expense Claims", "claims", "expenses"))
        }

        val accounting = if (admin || accountant) listOf(
            web("expensetracking", "Expense Tracking", "expensetracking", "accounting/expense-tracking"),
            web("incometracking", "Income Tracking", "incometracking", "accounting/income-tracking"),
            web("reconciliation", "Bank Reconciliation", "reconciliation", "accounting/reconciliation"),
            web("statements", "Bank Statements", "statements", BANK_STATEMENTS_FOLDER),
        ) else emptyList()

        return listOf(
            myStuff,
            Section("People", people),
            Section("Processes", processes),
            Section("Finance", finance),
            Section("Accounting", accounting),
            documents,
            Section("System", if (admin) listOf(web("settings", "Settings", "settings", "settings")) else emptyList()),
        ).filter { it.rows.isNotEmpty() }
    }

    /** The full URL of a web row. */
    fun url(path: String): String =
        if (path.startsWith("http")) path else Net.BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
}
