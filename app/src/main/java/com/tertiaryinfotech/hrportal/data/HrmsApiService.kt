package com.tertiaryinfotech.hrportal.data

import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

// MARK: - Request bodies (kotlinx.serialization converter handles these directly)

@Serializable
data class TimesheetEntryBody(val date: String, val hours: Double)

@Serializable
data class SubmitTimesheetBody(val weekStart: String, val entries: List<TimesheetEntryBody>)

@Serializable
data class ApplyLeaveBody(
    val leaveTypeId: String, val startDate: String, val endDate: String,
    val dayType: String, val reason: String,
)

@Serializable
data class CreateExpenseBody(
    val categoryId: String, val description: String, val amount: Double, val expenseDate: String,
    val receiptUrl: String? = null, val receiptFileName: String? = null,
)

@Serializable
data class UpdateProfileBody(val personalInfo: Map<String, String>)

@Serializable
data class ChangePasswordBody(val currentPassword: String, val newPassword: String)

@Serializable
data class WoodsSquareRequestBody(val fromDate: String? = null, val toDate: String? = null, val note: String? = null)

/**
 * Retrofit definition of the HRMS backend's mobile JSON API (`/api/mobile/...`) plus the handful
 * of pre-existing endpoints the app also reads/writes. Every method returns `Response<T>` (rather
 * than a bare suspend `T`) so `HrmsApi`'s wrapper can inspect the status code and parse the
 * server's `{"error": "..."}` body on failure — identical semantics to the raw-OkHttp version
 * this replaces.
 */
interface HrmsApiService {

    @GET("api/mobile/summary")
    suspend fun summary(): Response<DashboardSummary>

    @GET("api/mobile/leave")
    suspend fun leave(): Response<LeaveResponse>

    @GET("api/mobile/employees")
    suspend fun employees(): Response<EmployeesResponse>

    @GET("api/mobile/expenses")
    suspend fun expenses(): Response<ExpensesResponse>

    @GET("api/mobile/payslips")
    suspend fun payslips(): Response<PayslipsResponse>

    @GET("api/mobile/calendar")
    suspend fun calendar(): Response<CalendarResponse>

    @GET("api/mobile/profile")
    suspend fun profile(): Response<ProfileResponse>

    @GET("api/mobile/woods-square")
    suspend fun woodsSquare(): Response<WoodsSquareResponse>

    @GET("api/mobile/attendance")
    suspend fun attendance(): Response<AttendanceResponse>

    @POST("api/mobile/attendance/clock-in")
    suspend fun clockIn(): Response<ClockPunchResult>

    @POST("api/mobile/attendance/clock-out")
    suspend fun clockOut(): Response<ClockPunchResult>

    @GET("api/notifications")
    suspend fun notifications(): Response<List<AppNotification>>

    @POST("api/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): Response<ResponseBody>

    @GET("api/timesheet")
    suspend fun timesheet(@Query("weekStart") weekStart: String?): Response<TimesheetResponse>

    @POST("api/timesheet")
    suspend fun submitTimesheet(@Body body: SubmitTimesheetBody): Response<ResponseBody>

    @POST("api/leave")
    suspend fun applyLeave(@Body body: ApplyLeaveBody): Response<ResponseBody>

    @POST("api/expenses")
    suspend fun createExpense(@Body body: CreateExpenseBody): Response<ResponseBody>

    @Multipart
    @POST("api/upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): Response<UploadResult>

    @PATCH("api/employees/{id}")
    suspend fun updateProfile(@Path("id") employeeId: String, @Body body: UpdateProfileBody): Response<ResponseBody>

    @PATCH("api/profile/password")
    suspend fun changePassword(@Body body: ChangePasswordBody): Response<ResponseBody>

    @POST("api/woods-square/access-requests")
    suspend fun requestWoodsSquareAccess(@Body body: WoodsSquareRequestBody): Response<ResponseBody>

    @DELETE("api/woods-square/access-requests/{id}")
    suspend fun cancelWoodsSquareRequest(@Path("id") id: String): Response<ResponseBody>

    @Streaming
    @GET
    suspend fun downloadPdf(@Url url: String): Response<ResponseBody>
}

@Serializable
data class ClockPunchResult(val id: String, val clockIn: String? = null, val clockOut: String? = null)
