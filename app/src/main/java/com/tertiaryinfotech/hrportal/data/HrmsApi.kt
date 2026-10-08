package com.tertiaryinfotech.hrportal.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

/**
 * Read (and a few write) calls against the HRMS backend's mobile JSON API (/api/mobile/...)
 * plus a couple of existing endpoints. Backed by Retrofit (`HrmsApiService`, `data/Net.kt`),
 * riding the shared cookie jar so it uses the same authenticated session established at login.
 * Every public method here keeps its original signature and exception vocabulary
 * (`ApiException`) unchanged — only the transport underneath moved to Retrofit. Mirrors
 * Services/HRMSAPI.swift.
 */
class ApiException(val kind: Kind, message: String) : Exception(message) {
    enum class Kind { UNAUTHORIZED, HTTP, NETWORK, DECODING }
}

object HrmsApi {

    private val service get() = Net.hrmsApiService
    private val json get() = Net.json

    // MARK: - Typed reads

    /** Unauthenticated — safe to call before sign-in to drive the login card's logo/name. */
    suspend fun branding() = unwrap { service.branding() }

    suspend fun summary() = unwrap { service.summary() }

    // MARK: - Approvals (admin only; server returns 403 for non-admins)
    suspend fun approvals() = unwrap { service.approvals() }

    enum class ApprovalKind { LEAVE, CLAIM }

    /** Approve or reject a pending leave/expense. On reject, an optional [reason] is sent. */
    suspend fun decide(kind: ApprovalKind, id: String, approve: Boolean, reason: String? = null): Unit =
        unwrapUnit(if (approve) "Could not approve the request." else "Could not reject the request.") {
            when (kind) {
                ApprovalKind.LEAVE -> if (approve) service.approveLeave(id) else service.rejectLeave(id, RejectReasonBody(reason?.takeIf { it.isNotBlank() }))
                ApprovalKind.CLAIM -> if (approve) service.approveExpense(id) else service.rejectExpense(id, RejectReasonBody(reason?.takeIf { it.isNotBlank() }))
            }
        }
    suspend fun leave() = unwrap { service.leave() }
    suspend fun publicHolidays(year: Int) = unwrap { service.publicHolidays(year) }
    suspend fun employees() = unwrap { service.employees() }
    suspend fun expenses() = unwrap { service.expenses() }
    suspend fun payslips() = unwrap { service.payslips() }
    suspend fun calendar() = unwrap { service.calendar() }
    suspend fun teamCalendar(year: Int) = unwrap { service.teamCalendar(year) }
    suspend fun profile() = unwrap { service.profile() }
    suspend fun attendance() = unwrap { service.attendance() }
    /** One month of daily check-in/out history; admins may pass another employee's id. */
    suspend fun attendanceHistory(month: String, employeeId: String? = null) =
        unwrap { service.attendanceHistory(month, employeeId) }
    /** Admin roll-up: each intern's days worked + total hours for the month. */
    suspend fun attendanceSummary(month: String) = unwrap { service.attendanceSummary(month) }
    suspend fun notifications() = unwrap { service.notifications() }
    suspend fun timesheet(weekStart: String? = null) = unwrap { service.timesheet(weekStart) }

    // MARK: - Hourly time off (existing /api/time-off)

    suspend fun timeOff() = unwrap { service.timeOff() }

    suspend fun createTimeOff(
        date: String, startTime: String, endTime: String, reason: String, reasonDetail: String? = null,
    ): Unit = unwrapUnit("Could not submit your time-off request.") {
        service.createTimeOff(CreateTimeOffBody(date, startTime, endTime, reason, reasonDetail))
    }

    suspend fun cancelTimeOff(id: String): Unit = unwrapUnit("Could not cancel the request.") {
        service.cancelTimeOff(id)
    }

    // MARK: - Clock in/out (POST /api/mobile/attendance/clock-in|out)

    suspend fun clockIn() = unwrap(defaultError = "Could not clock in.") { service.clockIn() }
    suspend fun clockOut() = unwrap(defaultError = "Could not clock out.") { service.clockOut() }

    // MARK: - Submit weekend/PH timesheet hours (POST /api/timesheet)

    suspend fun submitTimesheet(weekStart: String, entries: List<Pair<String, Double>>): Unit = unwrapUnit(
        "Could not submit your hours.",
    ) {
        service.submitTimesheet(SubmitTimesheetBody(weekStart, entries.map { (d, h) -> TimesheetEntryBody(d, h) }))
    }

    // MARK: - Apply for leave (POST /api/leave)

    suspend fun applyLeave(
        leaveTypeId: String, startDate: String, endDate: String, days: Double, dayType: String,
        halfDayPosition: String? = null, reason: String, documentUrl: String? = null,
        documentFileName: String? = null, otDaysUsed: Double? = null,
    ): Unit = unwrapUnit("Could not submit your leave request.") {
        service.applyLeave(
            ApplyLeaveBody(
                leaveTypeId, startDate, endDate, days, dayType, halfDayPosition, reason,
                documentUrl, documentFileName, otDaysUsed,
            ),
        )
    }

    // MARK: - Create an expense claim (POST /api/expenses)

    suspend fun createExpense(
        categoryId: String, description: String, amount: Double, expenseDate: String,
        receiptUrl: String? = null, receiptFileName: String? = null,
    ): Unit = unwrapUnit("Could not submit your expense claim.") {
        service.createExpense(CreateExpenseBody(categoryId, description, amount, expenseDate, receiptUrl, receiptFileName))
    }

    // MARK: - Acknowledge payment received on an approved expense claim (POST /api/expenses/{id}/acknowledge)

    suspend fun acknowledgeExpense(id: String): Unit = unwrapUnit("Could not confirm payment received.") {
        service.acknowledgeExpense(id)
    }

    // MARK: - Upload a file (POST /api/upload) — used for expense receipts

    suspend fun uploadFile(bytes: ByteArray, fileName: String, mimeType: String): UploadResult {
        val part = MultipartBody.Part.createFormData("file", fileName, bytes.toRequestBody(mimeType.toMediaType()))
        return unwrap(defaultError = "Could not upload the file.") { service.uploadFile(part) }
    }

    // MARK: - Update own personal profile fields (PATCH /api/employees/{id})

    suspend fun updateProfile(employeeId: String, personalInfo: Map<String, String?>): Unit = unwrapUnit(
        "Could not update your profile.",
    ) {
        service.updateProfile(employeeId, UpdateProfileBody(personalInfo.filterValues { it != null }.mapValues { it.value!! }))
    }

    // MARK: - Change own password (PATCH /api/profile/password)

    suspend fun changePassword(currentPassword: String, newPassword: String): Unit = unwrapUnit(
        "Could not change your password.",
    ) {
        service.changePassword(ChangePasswordBody(currentPassword, newPassword))
    }

    // MARK: - Create a calendar event (POST /api/calendar)

    suspend fun createCalendarEvent(
        title: String, description: String?, startDate: String, endDate: String, allDay: Boolean, type: String,
    ): Unit = unwrapUnit("Could not create the event.") {
        service.createCalendarEvent(CreateCalendarEventBody(title, description, startDate, endDate, allDay, type))
    }

    // MARK: - Notifications

    suspend fun markNotificationRead(id: String): Unit = unwrapUnit("Could not update notification.") {
        service.markNotificationRead(id)
    }

    // MARK: - Authenticated PDF download (existing payslip PDF route)

    suspend fun downloadPdf(path: String): ByteArray = withContext(Dispatchers.IO) {
        val resp = safeCall { service.downloadPdf(Net.url(path)) }
        checkSuccess(resp, "Request failed (${resp.code()}).")
        resp.body()?.bytes() ?: throw ApiException(ApiException.Kind.DECODING, "Unexpected response from the server.")
    }

    // MARK: - Plumbing

    private suspend fun <T> safeCall(call: suspend () -> Response<T>): Response<T> = withContext(Dispatchers.IO) {
        try {
            call()
        } catch (e: Exception) {
            android.util.Log.e("HrmsApi", "Network/decode failure", e)
            throw ApiException(ApiException.Kind.NETWORK, "Network error. Check your connection.")
        }
    }

    /** Throws [ApiException] if [resp] isn't a 2xx, parsing the server's `{"error": "..."}` body
     *  when present (falling back to [defaultError]) — identical semantics to the raw-OkHttp
     *  version this replaces. */
    private fun checkSuccess(resp: Response<*>, defaultError: String) {
        if (resp.code() == 401 || resp.code() == 403)
            throw ApiException(ApiException.Kind.UNAUTHORIZED, "Your session expired. Please sign in again.")
        if (!resp.isSuccessful) {
            val errText = resp.errorBody()?.string().orEmpty()
            android.util.Log.e("HrmsApi", "HTTP ${resp.code()} ${resp.raw().request.url}: $errText")
            val msg = try {
                (json.parseToJsonElement(errText) as? JsonObject)?.get("error")?.jsonPrimitive?.content
            } catch (_: Exception) { null }
            throw ApiException(ApiException.Kind.HTTP, msg ?: defaultError)
        }
    }

    private suspend fun <T> unwrap(defaultError: String = "Request failed.", call: suspend () -> Response<T>): T {
        val resp = safeCall(call)
        checkSuccess(resp, defaultError)
        return resp.body() ?: throw ApiException(ApiException.Kind.DECODING, "Unexpected response from the server.")
    }

    private suspend fun <T> unwrapUnit(defaultError: String, call: suspend () -> Response<T>) {
        val resp = safeCall(call)
        checkSuccess(resp, defaultError)
    }
}
