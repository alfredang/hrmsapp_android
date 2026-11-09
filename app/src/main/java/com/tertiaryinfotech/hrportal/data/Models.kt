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
)

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
)

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
    val totalDeductions: Double = 0.0,
    val netSalary: Double = 0.0,
    val status: String = "",
    val pdfPath: String = "",
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
)

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
)

/** Notification types relevant to a staff/intern view — mirrors `EMPLOYEE_TYPES`
 *  in the web's `notification-bell.tsx`. */
val STAFF_NOTIFICATION_TYPES = setOf(
    "LEAVE_APPROVED", "LEAVE_REJECTED",
    "OT_APPROVED", "OT_REJECTED",
    "INFO",
)

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
) {
    /** Medical leave (MC / sick leave) — shown apart from other leave. */
    val medical: List<LeaveRequest> get() = leave.filter { it.leaveCode.uppercase() in MEDICAL_CODES }
    val otherLeave: List<LeaveRequest> get() = leave.filter { it.leaveCode.uppercase() !in MEDICAL_CODES }

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
)

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
