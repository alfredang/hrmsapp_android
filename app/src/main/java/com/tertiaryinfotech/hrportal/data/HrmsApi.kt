package com.tertiaryinfotech.hrportal.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Read (and a few write) calls against the HRMS backend's mobile JSON API (/api/mobile/...)
 * plus a couple of existing endpoints. Rides the shared cookie jar, so it uses the same
 * authenticated session established at login. Mirrors Services/HRMSAPI.swift.
 */
class ApiException(val kind: Kind, message: String) : Exception(message) {
    enum class Kind { UNAUTHORIZED, HTTP, NETWORK, DECODING }
}

object HrmsApi {

    private val client get() = Net.client
    private val json get() = Net.json
    private val JSON_MEDIA = "application/json".toMediaType()

    // MARK: - Typed reads

    suspend fun summary() = get(Net.url("api/mobile/summary"), DashboardSummary.serializer())
    suspend fun leave() = get(Net.url("api/mobile/leave"), LeaveResponse.serializer())
    suspend fun employees() = get(Net.url("api/mobile/employees"), EmployeesResponse.serializer())
    suspend fun expenses() = get(Net.url("api/mobile/expenses"), ExpensesResponse.serializer())
    suspend fun payslips() = get(Net.url("api/mobile/payslips"), PayslipsResponse.serializer())
    suspend fun calendar() = get(Net.url("api/mobile/calendar"), CalendarResponse.serializer())
    suspend fun profile() = get(Net.url("api/mobile/profile"), ProfileResponse.serializer())
    suspend fun timesheet() = get(Net.url("api/timesheet"), TimesheetResponse.serializer())

    // MARK: - Apply for leave (POST /api/leave)

    suspend fun applyLeave(
        leaveTypeId: String, startDate: String, endDate: String, dayType: String, reason: String,
    ): Unit = withContext(Dispatchers.IO) {
        val body = JsonObject(
            mapOf(
                "leaveTypeId" to JsonPrimitive(leaveTypeId),
                "startDate" to JsonPrimitive(startDate),
                "endDate" to JsonPrimitive(endDate),
                "dayType" to JsonPrimitive(dayType),
                "reason" to JsonPrimitive(reason),
            )
        )
        val req = Request.Builder()
            .url(Net.url("api/leave"))
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()
        val resp = safe(req)
        val code = resp.code
        val text = resp.body?.string().orEmpty()
        resp.close()
        if (code !in 200..299) {
            val msg = try {
                (json.parseToJsonElement(text) as? JsonObject)?.get("error")?.jsonPrimitive?.content
            } catch (_: Exception) { null }
            throw ApiException(ApiException.Kind.HTTP, msg ?: "Could not submit your leave request.")
        }
    }

    // MARK: - Authenticated PDF download (existing payslip PDF route)

    suspend fun downloadPdf(path: String): ByteArray = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(Net.url(path))
            .header("Accept", "application/pdf")
            .get()
            .build()
        val resp = safe(req)
        val code = resp.code
        val bytes = resp.body?.bytes()
        resp.close()
        if (code !in 200..299 || bytes == null)
            throw ApiException(ApiException.Kind.HTTP, "Request failed ($code).")
        bytes
    }

    // MARK: - Plumbing

    private suspend fun <T> get(url: String, serializer: DeserializationStrategy<T>): T =
        withContext(Dispatchers.IO) {
            val req = Request.Builder().url(url).header("Accept", "application/json").get().build()
            val resp = safe(req)
            val code = resp.code
            val text = resp.body?.string().orEmpty()
            resp.close()
            if (code == 401 || code == 403)
                throw ApiException(ApiException.Kind.UNAUTHORIZED, "Your session expired. Please sign in again.")
            if (code !in 200..299)
                throw ApiException(ApiException.Kind.HTTP, "Request failed ($code).")
            try {
                json.decodeFromString(serializer, text)
            } catch (_: Exception) {
                throw ApiException(ApiException.Kind.DECODING, "Unexpected response from the server.")
            }
        }

    private fun safe(req: Request) = try {
        client.newCall(req).execute()
    } catch (e: Exception) {
        throw ApiException(ApiException.Kind.NETWORK, "Network error. Check your connection.")
    }
}
