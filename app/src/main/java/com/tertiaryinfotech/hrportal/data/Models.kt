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

// MARK: - Public company branding (/api/public/branding) — unauthenticated, drives the login card

@Serializable
data class BrandingResponse(
    val name: String = "",
    val shortName: String? = null,
    val logo: String? = null,
) {
    /** Mirrors the web login page's `displayName = branding.shortName || branding.name ||
     *  "HR Portal"` exactly — including using the (possibly misspelled) `shortName` first, since
     *  that's genuinely what the live company-settings data returns and what the reference app
     *  displays everywhere. */
    val displayName: String
        get() = shortName?.takeIf { it.isNotBlank() } ?: name.takeIf { it.isNotBlank() } ?: "HR Portal"

    /** Mirrors the web login page's own initials fallback: prefer a short name, else derive from
     *  the first two words of the full name, else the app's own default mark. */
    val initials: String
        get() {
            val source = displayName.trim()
            if (source.isEmpty() || source == "HR Portal") return "TI"
            val letters = source.split(' ').filter { it.isNotBlank() }.take(2).mapNotNull { it.firstOrNull() }
            val s = letters.joinToString("").uppercase()
            return s.ifEmpty { "TI" }
        }
}

// MARK: - Dashboard summary (/api/mobile/summary)

@Serializable
data class DashboardSummary(
    val name: String? = null,
    val role: String? = null,
    val isAdmin: Boolean = false,
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
