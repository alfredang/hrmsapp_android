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

    suspend fun summary() = unwrap { service.summary() }
    suspend fun leave() = unwrap { service.leave() }
    suspend fun employees() = unwrap { service.employees() }
    suspend fun expenses() = unwrap { service.expenses() }
    suspend fun payslips() = unwrap { service.payslips() }
    suspend fun calendar() = unwrap { service.calendar() }
    suspend fun profile() = unwrap { service.profile() }
    suspend fun woodsSquare() = unwrap { service.woodsSquare() }
    suspend fun attendance() = unwrap { service.attendance() }
    suspend fun notifications() = unwrap { service.notifications() }
    suspend fun timesheet(weekStart: String? = null) = unwrap { service.timesheet(weekStart) }

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
        leaveTypeId: String, startDate: String, endDate: String, dayType: String, reason: String,
    ): Unit = unwrapUnit("Could not submit your leave request.") {
        service.applyLeave(ApplyLeaveBody(leaveTypeId, startDate, endDate, dayType, reason))
    }

    // MARK: - Create an expense claim (POST /api/expenses)

    suspend fun createExpense(
        categoryId: String, description: String, amount: Double, expenseDate: String,
        receiptUrl: String? = null, receiptFileName: String? = null,
    ): Unit = unwrapUnit("Could not submit your expense claim.") {
        service.createExpense(CreateExpenseBody(categoryId, description, amount, expenseDate, receiptUrl, receiptFileName))
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

    // MARK: - Woods Square access requests

    suspend fun requestWoodsSquareAccess(fromDate: String?, toDate: String?, note: String?): Unit = unwrapUnit(
        "Could not submit your access request.",
    ) {
        service.requestWoodsSquareAccess(WoodsSquareRequestBody(fromDate, toDate, note))
    }

    suspend fun cancelWoodsSquareRequest(id: String): Unit = unwrapUnit("Could not cancel your request.") {
        service.cancelWoodsSquareRequest(id)
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
