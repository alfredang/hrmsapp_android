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
class ApiException(
    val kind: Kind,
    message: String,
    /** HTTP status for [Kind.HTTP] / [Kind.UNAUTHORIZED]; 0 otherwise. */
    val status: Int = 0,
    /** The raw error body, so callers can read coded errors (see [LeaveErrors]). */
    val body: String? = null,
) : Exception(message) {
    enum class Kind { UNAUTHORIZED, HTTP, NETWORK, DECODING }
}

object HrmsApi {

    private val service get() = Net.hrmsApiService
    private val json get() = Net.json

    // MARK: - Typed reads

    /** Unauthenticated — safe to call before sign-in to drive the login card's logo/name. */

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
    /** Team drill-down (approvers): one person's clock-ins, MC and leave. */
    suspend fun employeeRecords(id: String) = unwrap { service.employeeRecords(id) }
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
    /** Admin roll-up: every active staff member's and intern's leave + MC for the month. */
    suspend fun leaveSummary(month: String) = unwrap { service.leaveSummary(month) }
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
        leaveTypeId: String, startDate: String, endDate: String, dayType: String,
        halfDayPosition: String? = null, reason: String, documentUrl: String? = null,
        documentFileName: String? = null, otDaysUsed: Double? = null, useAdvance: Boolean = false,
    ) {
        try {
            unwrapUnit("Could not submit your leave request.") {
                service.applyLeave(
                    ApplyLeaveBody(
                        leaveTypeId, startDate, endDate, dayType, halfDayPosition, reason,
                        documentUrl, documentFileName, otDaysUsed, useAdvance,
                    ),
                )
            }
        } catch (e: ApiException) {
            throw leaveFailure(e)
        }
    }

    /** Coded refusals (AL over balance, intern MC, NPL cap) become [LeaveRejectedException], which
     *  carries what the form needs to offer No Pay Leave instead; anything else stays as it was. */
    internal fun leaveFailure(e: ApiException): Exception {
        val rejection = if (e.kind == ApiException.Kind.HTTP && e.status == 400) LeaveErrors.parse(e.body) else null
        return if (rejection != null) LeaveRejectedException(rejection) else e
    }

    // MARK: - Trainer directory (TRAINER_ADMIN only; 401 signed out, 403 otherwise)

    suspend fun trainers(query: TrainerQuery): TrainersResponse = trainerCall {
        service.trainers(
            q = query.q.trim().takeIf { it.isNotEmpty() },
            certs = query.certs.toList(),
            tags = query.tags.toList(),
            finance = if (query.finance) "1" else null,
            ibfFit = if (query.ibfFit) "1" else null,
            status = query.status.param,
        )
    }

    suspend fun trainer(id: String): TrainerProfile = trainerCall { service.trainer(id) }.trainer

    private suspend fun <T> trainerCall(call: suspend () -> Response<T>): T = try {
        unwrap(defaultError = "Could not load the trainer directory.", call = call)
    } catch (e: ApiException) {
        throw when (e.status) {
            403 -> ApiException(ApiException.Kind.HTTP, "The trainer directory is for Trainer Admins only.", 403, e.body)
            404 -> ApiException(ApiException.Kind.HTTP, "Trainer not found.", 404, e.body)
            else -> e
        }
    }

    // MARK: - Submit a claim with its receipt (POST /api/mobile/claims, multipart)

    /** The single-shot claim route iOS uses. It stores the receipt, raises the claim and files the
     *  receipt in the employee's "Expense Claims" or "Medical Claims" Drive folder. */
    suspend fun submitClaim(
        claimType: String, categoryId: String?, description: String, amount: Double,
        expenseDate: String, receipt: ByteArray, fileName: String, mimeType: String,
    ): Unit = unwrapUnit("Could not submit your claim.") {
        val text = "text/plain".toMediaType()
        service.submitClaim(
            claimType = claimType.toRequestBody(text),
            categoryId = categoryId?.toRequestBody(text),
            description = description.toRequestBody(text),
            amount = String.format(java.util.Locale.US, "%.2f", amount).toRequestBody(text),
            expenseDate = expenseDate.toRequestBody(text),
            receipt = MultipartBody.Part.createFormData("receipt", fileName, receipt.toRequestBody(mimeType.toMediaType())),
        )
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
        // Only a 401 means the session is gone. A 403 is a real refusal with its own message, for
        // example "Cannot edit a past week", so it falls through to the server's error below.
        if (resp.code() == 401)
            throw ApiException(ApiException.Kind.UNAUTHORIZED, "Your session expired. Please sign in again.", 401)
        if (!resp.isSuccessful) {
            val errText = resp.errorBody()?.string().orEmpty()
            android.util.Log.e("HrmsApi", "HTTP ${resp.code()} ${resp.raw().request.url}: $errText")
            throw ApiException(ApiException.Kind.HTTP, errorMessage(errText) ?: defaultError, resp.code(), errText)
        }
    }

    /** The server's `{"error": "..."}` message, or null when the body isn't that shape. */
    internal fun errorMessage(errText: String): String? = try {
        (json.parseToJsonElement(errText) as? JsonObject)?.get("error")?.jsonPrimitive?.content
    } catch (_: Exception) { null }

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
