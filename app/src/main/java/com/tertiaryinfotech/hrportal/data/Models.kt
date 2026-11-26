package com.tertiaryinfotech.hrportal.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Mirrors Models/HRModels.swift + Models/SessionUser.swift from the iOS app.
// All shapes match the HRMS backend's /api/mobile/* JSON (plus a few existing endpoints).

// MARK: - Session

@Serializable
data class SessionUser(
    val id: String? = null,
    val email: String? = null,
    val name: String? = null,
    val role: String? = null,
    val roles: List<String>? = null,
    val employeeId: String? = null,
) {
    val displayName: String
        get() = when {
            !name.isNullOrEmpty() -> name
            !email.isNullOrEmpty() -> email
            else -> "Employee"
        }

    val primaryRole: String get() = role ?: roles?.firstOrNull() ?: "STAFF"

    /** Roles exactly ["INTERN"] — interns have no paid MC (their sick leave is No Pay Leave). */
    val isIntern: Boolean get() = LeaveRules.isIntern(roles, role)

    /** Interns and accountants without an approver role: they clock in / out and get the clock-in reminders. */
    val mustClockIn: Boolean get() = LeaveRules.mustClockIn(roles, role)

    val initials: String
        get() {
            val source = if (!name.isNullOrEmpty()) name else (email ?: "?")
            val parts = source.split(' ', '.', '@').filter { it.isNotBlank() }
            val letters = parts.take(2).mapNotNull { it.firstOrNull() }
            val s = letters.joinToString("").uppercase()
            return if (s.isEmpty()) "?" else s
        }
}

@Serializable
data class SessionResponse(
    val user: SessionUser? = null,
    val expires: String? = null,
)

// MARK: - Dashboard summary (/api/mobile/summary)

@Serializable
data class DashboardSummary(
    val name: String? = null,
    val role: String? = null,
    val isAdmin: Boolean = false,
    /** Holds TRAINER_ADMIN: show the Trainers tab (backed by /api/mobile/trainers). */
    val canManageTrainers: Boolean = false,
    val alAvailable: Double? = null,
    val mcAvailable: Double? = null,
    val otRemaining: Double? = null,
    val expenseYtd: Double = 0.0,
    val pendingLeaves: Int = 0,
    val pendingClaims: Int = 0,
    /**
     * 1.16: the signed-in person's own leave tiles (paid leave taken, AL balance, MC taken, NPL taken
     * with "N of 14 left"), the same summary as the web dashboard and the decision email. Shown as
     * given. Null for someone without an employee record and absent from an older server.
     */
    val leaveSummary: List<SummaryStat>? = null,
    /** 1.16: accountants (not admins) only, else null. */
    val financeKpis: FinanceKpis? = null,
    /** 1.16: Trainer Admins only, else null. */
    val trainerKpis: TrainerKpis? = null,
)

/** One tile of [DashboardSummary.leaveSummary]: "MC taken" / "2 days", with an optional hint line. */
@Serializable
data class SummaryStat(val label: String = "", val value: String = "", val hint: String? = null)

/** Web `FinanceKpis` (src/lib/dashboard-kpis.ts): claims waiting, and this month's bank income / expenses. */
@Serializable
data class FinanceKpis(
    val pendingClaims: Int = 0,
    @Serializable(with = FlexNumberSerializer::class) val pendingClaimsAmount: Double = 0.0,
    @Serializable(with = FlexNumberSerializer::class) val incomeMonth: Double = 0.0,
    @Serializable(with = FlexNumberSerializer::class) val incomeYtd: Double = 0.0,
    @Serializable(with = FlexNumberSerializer::class) val expensesMonth: Double = 0.0,
    @Serializable(with = FlexNumberSerializer::class) val expensesYtd: Double = 0.0,
    /** "November". */
    val monthLabel: String = "",
    val year: Int = 0,
    /** Bank credits / debits this month, statement imports and the latest reconciliation (web e46f631, 22 Nov 2026). */
    val incomeCountMonth: Int? = null,
    val expenseCountMonth: Int? = null,
    val statementImports: Int? = null,
    /** yyyy-MM-dd: the last bank date the imported statements cover. */
    val statementLatestDate: String? = null,
    val statementLastImportedAt: String? = null,
    val reconciliation: FinanceReconciliation? = null,
)

/** The latest bank reconciliation run (DBS against QuickBooks); [toReview] = unreviewed lines that are not a match. */
@Serializable
data class FinanceReconciliation(
    val status: String = "",
    val periodStart: String = "",
    val periodEnd: String = "",
    val matched: Int = 0,
    val toReview: Int = 0,
    val createdAt: String = "",
)

/** Web `TrainerKpis`: every trainer on record (applicants excluded), with and without the ACLP. */
@Serializable
data class TrainerKpis(val total: Int = 0, val aclp: Int = 0, val nonAclp: Int = 0)

// MARK: - Leave (/api/mobile/leave)

@Serializable
data class LeaveResponse(
    val balances: List<LeaveBalance> = emptyList(),
    val types: List<LeaveType> = emptyList(),
    val requests: List<LeaveRequest> = emptyList(),
)

@Serializable
data class LeaveBalance(
    val code: String,
    val name: String,
    val paid: Boolean = false,
    val entitlement: Double = 0.0,
    val carriedOver: Double = 0.0,
    val used: Double = 0.0,
    val pending: Double = 0.0,
    val earned: Double = 0.0,
    val autoDeducted: Double = 0.0,
    val proRated: Double? = null,
    val available: Double = 0.0,
)

@Serializable
data class LeaveType(
    val id: String,
    val code: String,
    val name: String,
    val paid: Boolean = false,
    val defaultDays: Int = 0,
)

// MARK: - Public holidays (/api/public-holidays) — feeds the Apply-for-Leave working-days preview

@Serializable
data class PublicHolidaysResponse(
    val dates: List<String> = emptyList(),
    val holidays: List<PublicHolidayEntry> = emptyList(),
)

@Serializable
data class PublicHolidayEntry(val date: String = "", val name: String = "", val source: String? = null)

@Serializable
data class LeaveRequest(
    val id: String,
    val leaveType: String = "",
    val leaveCode: String = "",
    val startDate: String? = null,
    val endDate: String? = null,
    val days: Double = 0.0,
    val dayType: String = "",
    val status: String = "",
    val reason: String? = null,
    val approver: String? = null,
    val approvedAt: String? = null,
    val rejectionReason: String? = null,
    val createdAt: String? = null,
)

// MARK: - Employees (/api/mobile/employees)

@Serializable
data class EmployeesResponse(
    val isAdmin: Boolean = false,
    val employees: List<Employee> = emptyList(),
)

@Serializable
data class Employee(
    val id: String,
    val employeeId: String = "",
    val name: String = "",
    val position: String? = null,
    val department: String? = null,
    val avatarUrl: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val employmentType: String? = null,
    val startDate: String? = null,
    /** Approvers only: true for an intern. Null for everyone else and from an older server. */
    val intern: Boolean? = null,
) {
    /** "Full time" / "Part time" / "Contract" / "Intern" (approvers only; null when not sent). iOS `employmentLabel`. */
    val employmentLabel: String?
        get() = when (employmentType?.uppercase()) {
            "FULL_TIME" -> "Full time"
            "PART_TIME" -> "Part time"
            "CONTRACT" -> "Contract"
            "INTERN" -> "Intern"
            else -> null
        }

    val isPartTime: Boolean get() = employmentType.equals("PART_TIME", ignoreCase = true)

    /** Team search: name, position, department, email and employment type (case-insensitive). */
    fun matches(query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        return listOfNotNull(name, position, department, email, employmentLabel).any { it.lowercase().contains(q) }
    }
}

/** The directory split into staff and interns for approvers. */
data class DirectoryGroups(val staff: List<Employee>, val interns: List<Employee>)

object Directory {
    /**
     * Staff and interns as two lists, or null when the server sent no `intern` flag at all
     * (a non-approver, or an older server): the screen then keeps its single list.
     * A person without the flag, next to others that have it, counts as staff.
     */
    fun split(employees: List<Employee>): DirectoryGroups? {
        if (employees.none { it.intern != null }) return null
        val (interns, staff) = employees.partition { it.intern == true }
        return DirectoryGroups(staff, interns)
    }
}

// MARK: - Expenses (/api/mobile/expenses)

@Serializable
data class ExpensesResponse(
    val approvedTotal: Double = 0.0,
    val categories: List<ExpenseCategory> = emptyList(),
    val claims: List<ExpenseClaim> = emptyList(),
)

@Serializable
data class ExpenseCategory(
    val id: String,
    val code: String = "",
    val name: String = "",
)

@Serializable
data class ExpenseClaim(
    val id: String,
    val description: String = "",
    val amount: Double = 0.0,
    val currency: String = "SGD",
    val category: String? = null,
    val expenseDate: String? = null,
    val status: String = "",
    val receiptUrl: String? = null,
    val approver: String? = null,
    val approvedAt: String? = null,
    val rejectionReason: String? = null,
    val createdAt: String? = null,
)

// MARK: - Payslips (/api/mobile/payslips)

@Serializable
data class PayslipsResponse(
    val payslips: List<Payslip> = emptyList(),
)

@Serializable
data class Payslip(
    val id: String,
    val payPeriodStart: String? = null,
    val payPeriodEnd: String? = null,
    val paymentDate: String? = null,
    val basicSalary: Double = 0.0,
    val allowances: Double = 0.0,
    val overtime: Double = 0.0,
    val bonus: Double = 0.0,
    val grossSalary: Double = 0.0,
    val cpfEmployee: Double = 0.0,
    // Pay breakdown (web, 26 Nov 2026). All defaulted, so an older server still decodes; shown by `PayslipFigures`.
    /** Employer CPF, paid by the company on top of the gross pay. */
    @Serializable(with = FlexNumberSerializer::class) val cpfEmployer: Double = 0.0,
    /** Interns have no CPF: both CPF figures are 0 and the app says so instead of showing 0.00. */
    val intern: Boolean = false,
    /** Unpaid days taken that month (No Pay Leave; MC too for interns) and the amount deducted. */
    @Serializable(with = FlexNumberSerializer::class) val unpaidLeaveDays: Double = 0.0,
    @Serializable(with = FlexNumberSerializer::class) val unpaidLeaveDeduction: Double = 0.0,
    /** Approved paybacks (off days worked) added back; already included in [grossSalary]. */
    @Serializable(with = FlexNumberSerializer::class) val paybackDays: Double = 0.0,
    @Serializable(with = FlexNumberSerializer::class) val paybackAmount: Double = 0.0,
    val totalDeductions: Double = 0.0,
    val netSalary: Double = 0.0,
    val status: String = "",
    val pdfPath: String = "",
    /**
     * The CPF EZPay statement this payslip was generated from (web, 26 Nov 2026). Admins and accountants only:
     * null for staff and interns, when no statement is on file, and from an older server that does not send it.
     */
    val cpfSubmission: PayslipCpfSubmission? = null,
)

/** The statement link on a payslip; [downloadPath] downloads with the session, like [Payslip.pdfPath]. */
@Serializable
data class PayslipCpfSubmission(
    val id: String,
    val name: String = "",
    val periodLabel: String? = null,
    val downloadPath: String = "",
)

// MARK: - CPF submissions (/api/mobile/cpf-submissions; admins and accountants, 403 otherwise)

@Serializable
data class CpfSubmissionsResponse(
    /** Latest first, as the server sorts them; keep that order. */
    val cpfSubmissions: List<CpfSubmission> = emptyList(),
)

/** One CPF EZPay "Confirm Employee Details" PDF in the shared Drive folder. */
@Serializable
data class CpfSubmission(
    val id: String,
    val name: String = "",
    val month: Int? = null,
    val year: Int? = null,
    /** "November 2026"; null when the file name carries no month. */
    val periodLabel: String? = null,
    val sizeBytes: Long? = null,
    /** ISO-8601. */
    val uploadedAt: String? = null,
    /** "/api/payroll/cpf-submission/files/<id>". */
    val downloadPath: String = "",
)

// MARK: - Calendar (/api/mobile/calendar)

@Serializable
data class CalendarResponse(
    val events: List<CalendarEvent> = emptyList(),
)

@Serializable
data class CalendarEvent(
    val id: String,
    val title: String = "",
    val startDate: String? = null,
    val endDate: String? = null,
    val allDay: Boolean = false,
    val type: String = "",
    val color: String? = null,
    val description: String? = null,
)

// MARK: - Google sign-in (Google token endpoint + POST /api/auth/google-mobile)

/** Google's token-endpoint reply. Only `id_token` is used; the error fields surface a real
 *  reason instead of a bare HTTP status when the exchange fails. */
@Serializable
data class GoogleTokenResponse(
    @SerialName("id_token") val idToken: String? = null,
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
)

@Serializable
data class GoogleMobileBody(val idToken: String)

/** The shape the backend uses for error replies (`{ "error": "..." }`). */
@Serializable
data class ApiErrorBody(val error: String? = null)

// MARK: - Team calendar (/api/mobile/team-calendar?year=)

/**
 * Company-wide approved leave for the team calendar month grid (iOS `TeamCalendarResponse`).
 *
 * [canSeeTypes] reports whether the server disclosed leave *types* to this viewer. A
 * colleague's [TeamLeaveEntry.leaveType] is masked server-side unless the viewer is that
 * person or an approver — medical leave would otherwise leak health information company-wide.
 * The client must never infer a type when the server withheld one.
 */
@Serializable
data class TeamCalendarResponse(
    val year: Int = 0,
    val canSeeTypes: Boolean = false,
    val entries: List<TeamLeaveEntry> = emptyList(),
    val holidays: List<TeamHoliday> = emptyList(),
    /**
     * Approvers only: every active staff member and intern, with or without leave, for the
     * "Leave shown for" filter. Null for everyone else and absent from an older server.
     */
    val people: List<TeamCalendarPerson>? = null,
    /** 1.16: approved time off and intern Off Days. Approvers get everyone's, others only their own. */
    val timeOff: List<TeamTimeOff> = emptyList(),
    /** 1.16: approvers only (null otherwise): one all-day birthday per person per year, never the age. */
    val birthdays: List<TeamBirthday>? = null,
    /** 1.16: the viewer's own workdays, ISO 1 = Mon … 7 = Sun (null without an employee record or on an older server). */
    val ownWorkdays: List<Int>? = null,
    /** 1.16: the colour + emoji legend. Absent on an older server: [CalendarCategories.FALLBACK] is used. */
    val categories: List<CalendarCategoryInfo>? = null,
    /**
     * Approved No Pay Leave paybacks (web 26 Nov 2026; absent on an older server): approvers get everyone's,
     * others their own. They feed the month totals' "Payback" chip and take paid-back days off No Pay Leave.
     */
    val paybacks: List<TeamPayback>? = null,
)

/**
 * An approved payback on the team calendar: an off day worked ([workDate], UTC midnight of the calendar date)
 * that pays back [days] (1 or 0.5) of the No Pay Leave request [leaveRequestId] (= a [TeamLeaveEntry.id]).
 */
@Serializable
data class TeamPayback(
    val id: String = "",
    val employeeId: String = "",
    val leaveRequestId: String = "",
    val nplDate: String? = null,
    val workDate: String? = null,
    @Serializable(with = FlexNumberSerializer::class) val days: Double = 1.0,
    val isSelf: Boolean = false,
)

@Serializable
data class TeamCalendarPerson(
    val id: String = "",
    val name: String = "",
    val intern: Boolean = false,
    /** 1.16: ISO weekdays this person works (1 = Mon … 7 = Sun); empty means Mon–Fri. */
    val workdays: List<Int>? = null,
)

/** Approved time off on the team calendar. [offDay] is a full day (shown as all-day "Off Day"). */
@Serializable
data class TeamTimeOff(
    val id: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val date: String? = null,
    /** "HH:mm". */
    val startTime: String = "",
    val endTime: String = "",
    @Serializable(with = FlexNumberSerializer::class) val hours: Double = 0.0,
    val offDay: Boolean = false,
    val isSelf: Boolean = false,
    /** "staff" or "intern" for approvers, else null. */
    val group: String? = null,
)

/** A birthday on the approvers' calendar; [date] is SGT noon of the day. */
@Serializable
data class TeamBirthday(val id: String = "", val employeeId: String = "", val name: String = "", val date: String? = null)

/** One legend row: key (HOLIDAY, ANNUAL_LEAVE, …), label, "#RRGGBB" and the emoji ("" when none). */
@Serializable
data class CalendarCategoryInfo(val key: String = "", val label: String = "", val color: String = "", val emoji: String = "")

@Serializable
data class TeamLeaveEntry(
    val id: String,
    val employeeId: String = "",
    val employeeName: String = "",
    val department: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    @Serializable(with = FlexNumberSerializer::class) val days: Double = 0.0,
    val halfDay: Boolean = false,
    /** Null when the server masked it — render the name alone, never a guess. */
    val leaveType: String? = null,
    val isSelf: Boolean = false,
    /** "staff" or "intern", sent to approvers only (null for everyone else and from older servers). */
    val group: String? = null,
    /**
     * 1.16: ANNUAL_LEAVE / MEDICAL_LEAVE / NO_PAY_LEAVE / OTHER_LEAVE where the type is shown (own
     * leave, approvers); null when masked, which keeps the neutral leave colour and no emoji.
     */
    val category: String? = null,
)

@Serializable
data class TeamHoliday(
    val id: String,
    val title: String = "",
    val startDate: String? = null,
    val endDate: String? = null,
)

// MARK: - Profile (/api/mobile/profile)

@Serializable
data class ProfileResponse(
    val employee: EmployeeProfile? = null,
)

@Serializable
data class EmployeeProfile(
    val id: String,
    val employeeId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String? = null,
    val position: String? = null,
    val department: String? = null,
    val employmentType: String = "",
    val nationality: String = "",
    val nric: String? = null,
    val gender: String = "",
    val educationLevel: String? = null,
    val dateOfBirth: String? = null,
    val address: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val status: String = "",
    val avatarUrl: String? = null,
    val monthlyLeaveRate: Double? = null,
    val roles: List<String> = emptyList(),
    val role: String = "",
    /** 1.16 Work Schedule: ISO weekdays worked (1 = Mon … 7 = Sun). Null on an older server. */
    val workdays: List<Int>? = null,
    /** 1.16: ["Mon", "Tue", …] and the rest of the week as off days. */
    val workingDays: List<String>? = null,
    val offDays: List<String>? = null,
    /** 1.16: "9am to 6pm". */
    val officeHours: String? = null,
)

// MARK: - Notifications (/api/notifications)

@Serializable
data class AppNotification(
    val id: String,
    val title: String = "",
    val message: String = "",
    val type: String = "",
    val read: Boolean = false,
    val link: String? = null,
    val createdAt: String? = null,
    /** Approvers: the request a request notice is about (web 23 Nov 2026). Use [requestAction]. */
    val action: NoticeAction? = null,
) {
    /**
     * The request this notice can approve / reject: the server's [action], else derived from the type
     * and the `?request=<id>` in the link (a server from before 23 Nov 2026). Null for other notices.
     */
    val requestAction: NoticeAction?
        get() {
            action?.takeIf { it.kind in NoticeAction.KINDS }?.let { return it }
            val kind = NoticeAction.KIND_BY_TYPE[type] ?: return null
            val id = link?.substringAfter('?', "")?.split('&')
                ?.firstOrNull { it.startsWith("request=") }?.removePrefix("request=")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
            return id?.takeIf { it.isNotBlank() }?.let { NoticeAction(kind, it) }
        }
}

/** [kind]: leave | timeOff | expense | payback | correction (web `RequestActionKind`; iOS `NoticeAction`). */
@Serializable
data class NoticeAction(val kind: String = "", val requestId: String = "") {
    companion object {
        val KIND_BY_TYPE = mapOf(
            "LEAVE_SUBMITTED" to "leave", "TIME_OFF_SUBMITTED" to "timeOff", "EXPENSE_SUBMITTED" to "expense",
            "PAYBACK_SUBMITTED" to "payback", "ATTENDANCE_CORRECTION_SUBMITTED" to "correction",
        )
        val KINDS = KIND_BY_TYPE.values.toSet()
    }
}

/** UI-only toast events the web bell also skips; everything else is a real alert for this user. */
private val HIDDEN_NOTIFICATION_TYPES = setOf("ADD_TOAST", "UPDATE_TOAST", "REMOVE_TOAST", "DISMISS_TOAST")

/** The server returns only this user's own notifications, so every alert type is shown — interns
 *  see missed clock-in/out and timesheet reminders, admins see new approval requests. */
fun isVisibleNotification(type: String): Boolean = type !in HIDDEN_NOTIFICATION_TYPES

// MARK: - Upload (/api/upload)

@Serializable
data class UploadResult(
    val url: String,
    val fileName: String,
)

// MARK: - Approvals (/api/mobile/approvals) — admin only (MANAGER/HR/ADMIN)

@Serializable
data class ApprovalsResponse(
    val leaves: List<PendingLeave> = emptyList(),
    val claims: List<PendingClaim> = emptyList(),
    /** No Pay Leave paybacks waiting (web 23 Nov 2026). Absent on an older server → empty. */
    val paybacks: List<PendingPayback> = emptyList(),
)

/**
 * A No Pay Leave payback waiting for approval: the person worked an off day ([workDate]) to pay back
 * a No Pay Leave day ([nplDate]); [clocked] = their punch that day. iOS `PendingPayback`.
 */
@Serializable
data class PendingPayback(
    val id: String,
    val employee: String = "",
    val nplDate: String? = null,
    val workDate: String? = null,
    val days: Double = 1.0,
    val note: String? = null,
    val clocked: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class PendingLeave(
    val id: String,
    val employee: String = "",
    val leaveType: String = "",
    val leaveCode: String = "",
    val startDate: String? = null,
    val endDate: String? = null,
    val days: Double = 0.0,
    val dayType: String = "",
    val reason: String? = null,
    val documentUrl: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class PendingClaim(
    val id: String,
    val employee: String = "",
    val category: String? = null,
    val description: String = "",
    val amount: Double = 0.0,
    val expenseDate: String? = null,
    val receiptUrl: String? = null,
    val createdAt: String? = null,
)

// MARK: - Attendance / clock-in-out (/api/mobile/attendance)

@Serializable
data class AttendanceResponse(
    val today: AttendancePunch? = null,
    val recent: List<AttendancePunch> = emptyList(),
)

@Serializable
data class AttendancePunch(
    val id: String,
    val date: String? = null,
    val clockIn: String? = null,
    val clockOut: String? = null,
)

// MARK: - Attendance history (/api/mobile/attendance/history?month=YYYY-MM[&employeeId=])

/** One employee's punches for a month, newest first, plus the month's totals. */
@Serializable
data class AttendanceHistory(
    val month: String = "",
    val employee: AttendanceEmployee? = null,
    val days: List<AttendanceDay> = emptyList(),
    val totalHours: Double = 0.0,
    val daysWorked: Int = 0,
)

@Serializable
data class AttendanceEmployee(
    val id: String = "",
    val name: String = "",
    val employeeCode: String = "",
)

/** A day's punch. `date` is a plain `yyyy-MM-dd`; `hours` is null until clocked out. */
@Serializable
data class AttendanceDay(
    val id: String,
    val date: String = "",
    val clockIn: String? = null,
    val clockOut: String? = null,
    val hours: Double? = null,
)

// MARK: - One person's records, approvers (/api/mobile/employees/{id}/records)

/** Team drill-down: one person's clock-ins, MC and leave. Approver-or-self, enforced server-side. */
@Serializable
data class EmployeeRecords(
    val employee: RecordsEmployee,
    val attendance: RecordsAttendance = RecordsAttendance(),
    val leave: List<LeaveRequest> = emptyList(),
    /** Web 25 Nov 2026; null on an older server. */
    val intern: Boolean? = null,
    /** Interns and accountants without an approver role (web `mustClockIn`). Staff do not clock in. */
    val mustClockIn: Boolean? = null,
    /** Annual Leave, MC and No Pay Leave: taken and the balance out of the allowance. */
    val balances: List<RecordsBalance>? = null,
    /** No Pay Leave paybacks, newest first (web 25 Nov 2026; null on an older server). */
    val paybacks: List<RecordsPayback>? = null,
    /** Expense claims, newest first (web 25 Nov 2026; null on an older server). */
    val claims: List<ExpenseClaim>? = null,
) {
    /** Medical leave (MC / sick leave) — shown apart from other leave. */
    val medical: List<LeaveRequest> get() = leave.filter { it.leaveCode.uppercase() in MEDICAL_CODES }
    val otherLeave: List<LeaveRequest> get() = leave.filter { it.leaveCode.uppercase() !in MEDICAL_CODES }
    val noPayLeave: List<LeaveRequest> get() = leave.filter { it.leaveCode.uppercase() == "NPL" }

    /** Clock-in history only for people who clock in (Alfred, 25 Nov 2026: staff have none). */
    val showsClock: Boolean get() = mustClockIn ?: intern ?: true

    companion object {
        val MEDICAL_CODES = setOf("MC", "SL")
    }
}

@Serializable
data class RecordsEmployee(
    val id: String = "",
    val employeeCode: String = "",
    val name: String = "",
    val position: String? = null,
    val department: String? = null,
    val employmentType: String? = null,
)

/** A No Pay Leave payback: an off day worked ([workDate]) to pay back an NPL day ([nplDate]). */
@Serializable
data class RecordsPayback(
    val id: String,
    val nplDate: String? = null,
    val workDate: String? = null,
    val days: Double = 1.0,
    val note: String? = null,
    val status: String = "",
    val approver: String? = null,
    val approvedAt: String? = null,
    val rejectionReason: String? = null,
    val createdAt: String? = null,
)

/** One leave balance on a person's records: days taken, and [available] of [allowance] left. */
@Serializable
data class RecordsBalance(
    val code: String = "",
    val name: String = "",
    val taken: Double = 0.0,
    val pending: Double = 0.0,
    val available: Double = 0.0,
    val allowance: Double = 0.0,
) {
    val isMedical: Boolean get() = code.uppercase() in EmployeeRecords.MEDICAL_CODES
    val isNoPay: Boolean get() = code.uppercase() == "NPL"
    /** "5 taken" */
    val takenText: String get() = "${days(taken)} taken"
    /** "7/12 balance" */
    val balanceText: String get() = "${days(available)}/${days(allowance)} balance"

    companion object {
        fun days(v: Double): String =
            if (v % 1.0 == 0.0) v.toLong().toString() else String.format(java.util.Locale.US, "%.1f", v)
    }
}

/**
 * `GET /api/mobile/employees/{id}/pay-month?month=YYYY-MM` (web 26 Nov 2026): the four figures for working out
 * one person's pay that month, counted the way the payslip counts them. [mcLabel] is "No Pay MC" for interns and
 * "Paid MC" for staff ([mcPaid]). Mirrors iOS `PayMonthSummary`.
 */
@Serializable
data class PayMonthSummary(
    /** "YYYY-MM" */
    val month: String = "",
    val intern: Boolean = false,
    @Serializable(with = FlexNumberSerializer::class) val annualLeaveDays: Double = 0.0,
    @Serializable(with = FlexNumberSerializer::class) val noPayLeaveDays: Double = 0.0,
    @Serializable(with = FlexNumberSerializer::class) val mcDays: Double = 0.0,
    val mcPaid: Boolean = true,
    val mcLabel: String = "",
    @Serializable(with = FlexNumberSerializer::class) val paybackDays: Double = 0.0,
) {
    /** One card of the "Month for pay" grid; [color] is 0xRRGGBB. */
    data class Tile(val label: String, val value: String, val hint: String, val color: Long)

    /** Annual Leave, No Pay Leave, MC, Payback: the 2 × 2 grid in reading order. */
    val tiles: List<Tile>
        get() = listOf(
            Tile("Annual Leave", days(annualLeaveDays), "Paid", 0xF59E0B),
            Tile("No Pay Leave", days(noPayLeaveDays), "Deducted", 0x64748B),
            Tile(mcLabel.ifBlank { if (mcPaid) "Paid MC" else "No Pay MC" }, days(mcDays), if (mcPaid) "Paid" else "Deducted", 0xEC4899),
            Tile("Payback", days(paybackDays), "Off days worked, added back", 0x10B981),
        )

    companion object {
        /** "1 day", "2.5 days", "0.25 days", "0 days": up to 2 decimals, no trailing zeros. */
        fun days(n: Double): String {
            // Not BigDecimal.stripTrailingZeros: Android keeps the scale of a zero ("0.00").
            val text = String.format(java.util.Locale.US, "%.2f", n).trimEnd('0').trimEnd('.')
                .let { if (it == "-0") "0" else it }
            return "$text ${if (text == "1") "day" else "days"}"
        }
    }
}

/** Every punch on record, newest first, with all-time totals. */
@Serializable
data class RecordsAttendance(
    val days: List<AttendanceDay> = emptyList(),
    val totalHours: Double = 0.0,
    val daysWorked: Int = 0,
)

// MARK: - Intern attendance summary, admin (/api/mobile/attendance/summary?month=YYYY-MM)

@Serializable
data class AttendanceSummary(
    val month: String = "",
    val employees: List<AttendanceSummaryRow> = emptyList(),
    val totalHours: Double = 0.0,
)

@Serializable
data class AttendanceSummaryRow(
    val id: String,
    val name: String = "",
    val employeeCode: String = "",
    val isIntern: Boolean = true,
    val daysWorked: Int = 0,
    val totalHours: Double = 0.0,
    val lastPunchDate: String? = null,
    val clockedInNow: Boolean = false,
)

// MARK: - Leave & MC summary, admin (/api/mobile/leave-summary?month=YYYY-MM)

/** Monthly leave + MC roll-up of every active staff member and intern. Days are working days
 *  inside the month; leaves crossing a month boundary are split server-side. */
@Serializable
data class LeaveSummary(
    val company: String = "",
    val month: String = "",
    val balancesYear: Int = 0,
    val totals: LeaveSummaryTotals = LeaveSummaryTotals(),
    val employees: List<LeaveSummaryEmployee> = emptyList(),
)

@Serializable
data class LeaveSummaryTotals(
    val headcount: LeaveSummaryHeadcount = LeaveSummaryHeadcount(),
    val employeesWithLeave: Int = 0,
    val byLeaveType: List<LeaveSummaryTypeTotal> = emptyList(),
    val mc: LeaveSummaryMc = LeaveSummaryMc(),
    val timeOff: LeaveSummaryHours = LeaveSummaryHours(),
)

@Serializable
data class LeaveSummaryHeadcount(val staff: Int = 0, val interns: Int = 0, val total: Int = 0)

@Serializable
data class LeaveSummaryTypeTotal(
    val code: String = "",
    val name: String = "",
    val approvedDays: Double = 0.0,
    val pendingDays: Double = 0.0,
    val employeesOnLeave: Int = 0,
)

@Serializable
data class LeaveSummaryMc(val approvedDays: Double = 0.0, val pendingDays: Double = 0.0, val employeesOnMc: Int = 0)

@Serializable
data class LeaveSummaryHours(val approvedHours: Double = 0.0, val pendingHours: Double = 0.0)

@Serializable
data class LeaveSummaryAmount(val approved: Double = 0.0, val pending: Double = 0.0)

@Serializable
data class LeaveSummaryEmployee(
    val employeeId: String = "",
    val name: String = "",
    val email: String = "",
    /** "STAFF" or "INTERN". */
    val category: String = "STAFF",
    val position: String? = null,
    val department: String? = null,
    val status: String = "",
    val mcDays: Double = 0.0,
    /** Keyed by leave type code (AL, MC, CL, NPL, AL_OT). */
    val leaveTaken: Map<String, LeaveSummaryAmount> = emptyMap(),
    val requests: List<LeaveSummaryRequest> = emptyList(),
    val timeOff: LeaveSummaryTimeOff? = null,
    val balances: List<LeaveSummaryBalance> = emptyList(),
) {
    val isIntern: Boolean get() = category == "INTERN"
    fun approved(code: String) = leaveTaken[code]?.approved ?: 0.0
    val pendingDays: Double get() = leaveTaken.values.sumOf { it.pending }
    /** Approved days of every type other than AL and MC. */
    val otherApproved: Double get() = leaveTaken.filterKeys { it != "AL" && it != "MC" }.values.sumOf { it.approved }
}

@Serializable
data class LeaveSummaryRequest(
    val leaveType: String = "",
    val leaveTypeName: String = "",
    val status: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val totalDays: Double = 0.0,
    val daysInMonth: Double = 0.0,
    val dayType: String? = null,
    val halfDayPosition: String? = null,
    val hasDocument: Boolean = false,
)

@Serializable
data class LeaveSummaryTimeOff(
    val approvedHours: Double = 0.0,
    val pendingHours: Double = 0.0,
    val requests: List<LeaveSummaryTimeOffRequest> = emptyList(),
)

@Serializable
data class LeaveSummaryTimeOffRequest(
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val hours: Double = 0.0,
    val reason: String = "",
    val status: String = "",
    /** Full-day intern Off Day (server ≥ 13 Nov 2026). */
    val offDay: Boolean = false,
)

@Serializable
data class LeaveSummaryBalance(
    val code: String = "",
    val name: String = "",
    val entitlement: Double = 0.0,
    val carriedOver: Double = 0.0,
    val usedYtd: Double = 0.0,
    val pending: Double = 0.0,
    val available: Double = 0.0,
)

// MARK: - Time off (existing /api/time-off) — hourly requests, mostly for interns

/**
 * One hourly time-off request. `GET /api/time-off` returns a bare JSON array of these (no object
 * envelope), ordered date-descending; non-admin users only receive their own rows. `hours` arrives
 * as a decimal string (`"2.50"`), hence [FlexNumberSerializer]. Every field is defaulted per house
 * convention so backend shape drift never breaks decoding.
 */
@Serializable
data class TimeOffRequest(
    val id: String = "",
    val employeeId: String = "",
    val date: String? = null,
    val startTime: String = "",
    val endTime: String = "",
    @Serializable(with = FlexNumberSerializer::class) val hours: Double = 0.0,
    val reason: String = "",
    val reasonDetail: String? = null,
    val status: String = "",
    val approvedAt: String? = null,
    val approvalComment: String? = null,
    val rejectedAt: String? = null,
    val rejectionReason: String? = null,
    val createdAt: String? = null,
    val employee: TimeOffEmployee? = null,
    /** Full-day intern Off Day. The server sends it as reason OTHERS + this flag, so builds
     *  that predate OFF_DAY keep working. */
    val offDay: Boolean = false,
) {
    val isOffDay: Boolean get() = offDay || reason.equals("OFF_DAY", ignoreCase = true)
}

/** Minimal nested employee on a [TimeOffRequest] — only the name is rendered. */
@Serializable
data class TimeOffEmployee(val name: String = "")

// MARK: - Timesheet (existing /api/timesheet)

@Serializable
data class TimesheetResponse(
    val weekStart: String = "",
    val isLocked: Boolean = false,
    val days: List<TimesheetDay> = emptyList(),
)

/**
 * `hours` / `otCredited` arrive as decimal strings from the existing endpoint, so they are
 * decoded as [FlexNumber] (accepts a JSON number or a numeric string) — same tolerance the
 * iOS model implements with a custom decoder.
 */
@Serializable
data class TimesheetDay(
    val date: String = "",
    val dayName: String = "",
    val isWeekend: Boolean = false,
    val isPublicHoliday: Boolean = false,
    val phName: String? = null,
    val isNonWorkDay: Boolean = false,
    @Serializable(with = FlexNumberSerializer::class) val hours: Double = 0.0,
    @Serializable(with = FlexNumberSerializer::class) val otCredited: Double = 0.0,
    val status: String? = null,
    val adminComment: String? = null,
    val isSubmittable: Boolean = false,
)

// MARK: - Missing-punch corrections (existing /api/attendance/corrections)

/** A missing clock-in / clock-out request. Admins list everyone's with `scope=all&status=PENDING`
 *  and approve / reject via `POST /api/attendance/corrections/{id}`. Times are ISO instants. */
@Serializable
data class AttendanceCorrection(
    val id: String,
    val employeeId: String = "",
    val employeeName: String = "",
    val employeeCode: String = "",
    /** `yyyy-MM-dd` */
    val date: String = "",
    /** CLOCK_IN, CLOCK_OUT or BOTH. */
    val kind: String = "",
    val requestedClockIn: String? = null,
    val requestedClockOut: String? = null,
    val currentClockIn: String? = null,
    val currentClockOut: String? = null,
    val reason: String = "",
    val status: String = "",
    val rejectionReason: String? = null,
    val createdAt: String? = null,
)

// MARK: - Off In Lieu approvals, admin (existing /api/timesheet/admin)

/** Weekend / public-holiday hours logged on the weekly timesheet, last 30 days. */
@Serializable
data class OilEntry(
    val id: String,
    val employeeId: String = "",
    val employeeCode: String = "",
    val employeeName: String = "",
    /** `yyyy-MM-dd` */
    val date: String = "",
    val dayName: String = "",
    val isPublicHoliday: Boolean = false,
    @Serializable(with = FlexNumberSerializer::class) val hours: Double = 0.0,
    @Serializable(with = FlexNumberSerializer::class) val otCredited: Double = 0.0,
    val status: String = "",
    val adminComment: String? = null,
)
