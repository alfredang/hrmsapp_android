package com.tertiaryinfotech.hrportal.data

import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
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
    val leaveTypeId: String, val startDate: String, val endDate: String, val days: Double,
    val dayType: String, val halfDayPosition: String? = null, val reason: String,
    val documentUrl: String? = null, val documentFileName: String? = null,
    val otDaysUsed: Double? = null,
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

/** Optional rejection reason for leave/expense reject (matches the web routes' `{reason}` body). */
@Serializable
data class RejectReasonBody(val reason: String? = null)

/** `reasonDetail` is required by the server when [reason] is OTHERS (400 otherwise). */
@Serializable
data class CreateTimeOffBody(
    val date: String, val startTime: String, val endTime: String,
    val reason: String, val reasonDetail: String? = null,
)

@Serializable
data class CreateCalendarEventBody(
    val title: String, val description: String? = null, val startDate: String, val endDate: String,
    val allDay: Boolean = false, val type: String,
)

/**
 * Retrofit definition of the HRMS backend's mobile JSON API (`/api/mobile/...`) plus the handful
 * of pre-existing endpoints the app also reads/writes. Every method returns `Response<T>` (rather
 * than a bare suspend `T`) so `HrmsApi`'s wrapper can inspect the status code and parse the
 * server's `{"error": "..."}` body on failure — identical semantics to the raw-OkHttp version
 * this replaces.
 */
interface HrmsApiService {

    @GET("api/public/branding")
    suspend fun branding(): Response<BrandingResponse>

    @GET("api/mobile/summary")
    suspend fun summary(): Response<DashboardSummary>

    // MARK: - Approvals (admin only — MANAGER/HR/ADMIN, enforced server-side with 403)
    @GET("api/mobile/approvals")
    suspend fun approvals(): Response<ApprovalsResponse>

    @POST("api/leave/{id}/approve")
    suspend fun approveLeave(@Path("id") id: String): Response<ResponseBody>

    @POST("api/leave/{id}/reject")
    suspend fun rejectLeave(@Path("id") id: String, @Body body: RejectReasonBody): Response<ResponseBody>

    @POST("api/expenses/{id}/approve")
    suspend fun approveExpense(@Path("id") id: String): Response<ResponseBody>

    @POST("api/expenses/{id}/reject")
    suspend fun rejectExpense(@Path("id") id: String, @Body body: RejectReasonBody): Response<ResponseBody>

    @GET("api/mobile/leave")
    suspend fun leave(): Response<LeaveResponse>

    /** Existing (non-mobile-namespaced) route the web's own leave-request form calls to compute
     *  its working-days preview — same one ported here for the Apply-for-Leave screen. */
    @GET("api/public-holidays")
    suspend fun publicHolidays(@Query("year") year: Int): Response<PublicHolidaysResponse>

    @GET("api/mobile/employees")
    suspend fun employees(): Response<EmployeesResponse>

    @GET("api/mobile/expenses")
    suspend fun expenses(): Response<ExpensesResponse>

    @GET("api/mobile/payslips")
    suspend fun payslips(): Response<PayslipsResponse>

    @GET("api/mobile/calendar")
    suspend fun calendar(): Response<CalendarResponse>

    /** Company-wide approved leave for the team calendar grid (iOS `HRMSAPI.teamCalendar`).
     *  Only APPROVED leave is returned, and colleagues' leave types are masked server-side. */
    @GET("api/mobile/team-calendar")
    suspend fun teamCalendar(@Query("year") year: Int): Response<TeamCalendarResponse>

    /** Existing (non-mobile-namespaced) route the web's own `/calendar/new` page calls — same
     *  NextAuth session-cookie auth this app already rides for every other call, so no new
     *  mobile-scoped endpoint was needed. */
    @POST("api/calendar")
    suspend fun createCalendarEvent(@Body body: CreateCalendarEventBody): Response<ResponseBody>

    @GET("api/mobile/profile")
    suspend fun profile(): Response<ProfileResponse>

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

    /** Bare JSON array — no envelope object (unlike the /api/mobile/... endpoints). */
    @GET("api/time-off")
    suspend fun timeOff(): Response<List<TimeOffRequest>>

    @POST("api/time-off")
    suspend fun createTimeOff(@Body body: CreateTimeOffBody): Response<ResponseBody>

    @POST("api/time-off/{id}/cancel")
    suspend fun cancelTimeOff(@Path("id") id: String): Response<ResponseBody>

    @GET("api/timesheet")
    suspend fun timesheet(@Query("weekStart") weekStart: String?): Response<TimesheetResponse>

    @POST("api/timesheet")
    suspend fun submitTimesheet(@Body body: SubmitTimesheetBody): Response<ResponseBody>

    @POST("api/leave")
    suspend fun applyLeave(@Body body: ApplyLeaveBody): Response<ResponseBody>

    @POST("api/expenses")
    suspend fun createExpense(@Body body: CreateExpenseBody): Response<ResponseBody>

    @POST("api/expenses/{id}/acknowledge")
    suspend fun acknowledgeExpense(@Path("id") id: String): Response<ResponseBody>

    @Multipart
    @POST("api/upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): Response<UploadResult>

    @PATCH("api/employees/{id}")
    suspend fun updateProfile(@Path("id") employeeId: String, @Body body: UpdateProfileBody): Response<ResponseBody>

    @PATCH("api/profile/password")
    suspend fun changePassword(@Body body: ChangePasswordBody): Response<ResponseBody>

    @Streaming
    @GET
    suspend fun downloadPdf(@Url url: String): Response<ResponseBody>
}

@Serializable
data class ClockPunchResult(val id: String, val clockIn: String? = null, val clockOut: String? = null)
