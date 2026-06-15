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
    val gender: String = "",
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
