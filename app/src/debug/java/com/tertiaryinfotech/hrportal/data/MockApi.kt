package com.tertiaryinfotech.hrportal.data

import android.content.Context
import android.util.Log
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.File

/**
 * Debug-only stand-in for the HRMS backend, the Android counterpart of iOS `-uiPreview`, so screens
 * can be checked on an emulator without production data or credentials. Off unless the file exists:
 *
 *   adb shell run-as com.tertiaryinfotech.hrportal sh -c 'echo admin > files/ui_preview'
 *
 * Content is admin, staff, intern or signedout. Writes are logged under the tag "MockApi", not sent.
 * Admin also holds TRAINER_ADMIN (Trainers tab). `POST /api/leave` answers like the real server's
 * coded refusals so the No Pay Leave flows can be previewed: AL → AL_EXCEEDS_BALANCE, MC from an
 * intern → INTERN_MC_NOT_PAID, NPL over 4 days → NPL_CAP_EXCEEDED; anything else succeeds.
 */
object MockApi {
    fun install(builder: OkHttpClient.Builder, context: Context) {
        val flag = File(context.filesDir, "ui_preview")
        if (!flag.exists()) return
        val role = flag.readText().trim().ifEmpty { "admin" }
        builder.addInterceptor(Interceptor { chain ->
            val request = chain.request()
            val path = request.url.encodedPath
            var body: String? = null
            if (request.method != "GET") {
                body = request.body?.let { b ->
                    if (b.contentType()?.subtype == "form-data") "[multipart ${b.contentLength()} bytes]"
                    else Buffer().also { b.writeTo(it) }.readUtf8()
                }
                Log.i("MockApi", "${request.method} $path $body")
            }
            val leaveError = if (request.method == "POST" && path == "/api/leave") leaveRefusal(body.orEmpty(), role) else null
            val json = leaveError ?: respond(path, request.method, role)
            val code = when {
                leaveError != null -> 400
                json == null -> 404
                else -> 200
            }
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(if (code == 200) "OK" else "Error")
                .body((json ?: "{\"error\":\"Not mocked\"}").toResponseBody("application/json".toMediaType()))
                .build()
        })
    }

    private fun respond(path: String, method: String, role: String): String? = when {
        path == "/api/auth/session" -> if (role == "signedout") "null" else session(role)
        path.startsWith("/api/auth/") -> "{\"csrfToken\":\"mock\",\"url\":\"\"}"
        method != "GET" -> "{\"url\":\"/api/uploads/mock.jpg\",\"fileName\":\"mock.jpg\",\"id\":\"x\"}"
        path == "/api/mobile/summary" -> summary(role)
        path == "/api/mobile/leave" -> LEAVE
        path == "/api/public-holidays" -> HOLIDAYS
        path == "/api/mobile/expenses" -> EXPENSES
        path == "/api/mobile/payslips" -> PAYSLIPS
        path == "/api/mobile/approvals" -> APPROVALS
        path == "/api/mobile/employees" -> EMPLOYEES.replace("ISADMIN", (role == "admin").toString())
        path.startsWith("/api/mobile/employees/") -> RECORDS
        path == "/api/mobile/profile" -> profile(role)
        path == "/api/mobile/attendance" -> ATTENDANCE
        path == "/api/mobile/attendance/history" -> HISTORY
        path == "/api/mobile/attendance/summary" -> ATTENDANCE_SUMMARY
        path == "/api/mobile/leave-summary" -> LEAVE_SUMMARY
        path == "/api/mobile/team-calendar" -> TEAM_CALENDAR
        path == "/api/mobile/calendar" -> CALENDAR
        path == "/api/notifications" -> NOTIFICATIONS
        path == "/api/time-off" -> if (role == "admin") TIME_OFF_ALL else TIME_OFF
        path == "/api/attendance/corrections" -> CORRECTIONS
        path == "/api/timesheet/admin" -> OIL
        path == "/api/timesheet" -> TIMESHEET
        path == "/api/mobile/trainers" -> TRAINERS
        path.startsWith("/api/mobile/trainers/") -> TRAINER
        else -> null
    }

    private const val NPL_STATUS = "{\"cap\":7,\"used\":1,\"pending\":2,\"remaining\":4}"

    /** The real server's coded refusals, keyed off the mock leave type ids. */
    private fun leaveRefusal(body: String, role: String): String? = when {
        "\"lt-al\"" in body ->
            """{"error":"You have 6 day(s) of annual leave available, so 2 day(s) of this request exceed your balance. Apply for the extra days as No Pay Leave (4 day(s) left this year).","code":"AL_EXCEEDS_BALANCE","alAvailable":6,"excessDays":2,"npl":$NPL_STATUS,"suggestedLeaveTypeCode":"NPL"}"""
        "\"lt-mc\"" in body && role == "intern" ->
            """{"error":"Interns do not have paid medical leave. Please apply for sick leave as No Pay Leave and attach your medical certificate.","code":"INTERN_MC_NOT_PAID","suggestedLeaveTypeCode":"NPL"}"""
        "\"lt-npl\"" in body && Regex("\"startDate\":\"(\\d{4}-\\d{2}-\\d{2})\".*\"endDate\":\"(\\d{4}-\\d{2}-\\d{2})\"").find(body)?.let { m ->
            val (s, e) = m.destructured
            java.time.LocalDate.parse(e).toEpochDay() - java.time.LocalDate.parse(s).toEpochDay() >= 4
        } == true ->
            """{"error":"This request (5 day(s)) is more than your remaining No Pay Leave (4 of 7 day(s)).","code":"NPL_CAP_EXCEEDED","npl":$NPL_STATUS,"requested":5}"""
        else -> null
    }

    private fun session(role: String): String {
        val r = role.uppercase()
        return """{"user":{"id":"u1","email":"sarah.lim@example.com","name":"Sarah Lim","role":"$r","roles":["$r"],"employeeId":"e1"}}"""
    }

    private fun summary(role: String) = """{"name":"Sarah Lim","role":"${role.uppercase()}","isAdmin":${role == "admin"},"canManageTrainers":${role == "admin"},
        "alAvailable":6,"mcAvailable":12,"otRemaining":1.5,"expenseYtd":428.5,"pendingLeaves":3,"pendingClaims":2}"""

    private fun profile(role: String) = """{"employee":{"id":"e1","employeeId":"E0007","name":"Sarah Lim","email":"sarah.lim@example.com",
        "phone":"+65 9123 4567","position":"Training Executive","department":"Operations","employmentType":"FULL_TIME",
        "nationality":"SINGAPOREAN","gender":"FEMALE","educationLevel":"DEGREE","dateOfBirth":null,"startDate":"2024-03-01",
        "status":"ACTIVE","roles":["${role.uppercase()}"],"role":"${role.uppercase()}"}}"""

    private const val LEAVE = """{
      "balances":[
        {"code":"AL","name":"Annual Leave","paid":true,"entitlement":14,"carriedOver":1,"used":3,"pending":1,"earned":0,"proRated":9,"available":6},
        {"code":"MC","name":"Medical Leave","paid":true,"entitlement":14,"used":2,"pending":0,"available":12},
        {"code":"NPL","name":"No Pay Leave","paid":false,"entitlement":0,"used":1,"pending":2,"proRated":7,"available":4},
        {"code":"CL","name":"Compassionate Leave","paid":true,"entitlement":3,"available":3},
        {"code":"AL_OT","name":"Off In Lieu","paid":true,"earned":2,"used":0.5,"available":1.5}
      ],
      "types":[
        {"id":"lt-al","code":"AL","name":"Annual Leave","paid":true,"defaultDays":7},
        {"id":"lt-cl","code":"CL","name":"Compassionate Leave","paid":true,"defaultDays":3},
        {"id":"lt-mc","code":"MC","name":"Medical Leave","paid":true,"defaultDays":14},
        {"id":"lt-npl","code":"NPL","name":"No Pay Leave","paid":false,"defaultDays":7},
        {"id":"lt-ot","code":"AL_OT","name":"Off In Lieu","paid":true,"defaultDays":0}
      ],
      "requests":[
        {"id":"r1","leaveType":"No Pay Leave","leaveCode":"NPL","startDate":"2026-11-24","endDate":"2026-11-25","days":2,"dayType":"FULL_DAY","status":"PENDING","reason":"Family matters","createdAt":"2026-11-08T02:00:00.000Z"},
        {"id":"r2","leaveType":"Annual Leave","leaveCode":"AL","startDate":"2026-10-12","endDate":"2026-10-14","days":3,"dayType":"FULL_DAY","status":"APPROVED","reason":"School holidays with the kids","createdAt":"2026-09-20T02:00:00.000Z"},
        {"id":"r3","leaveType":"Medical Leave","leaveCode":"MC","startDate":"2026-09-03","endDate":"2026-09-04","days":2,"dayType":"FULL_DAY","status":"APPROVED"},
        {"id":"r4","leaveType":"Compassionate Leave","leaveCode":"CL","startDate":"2026-08-03","endDate":"2026-08-03","days":1,"dayType":"FULL_DAY","status":"REJECTED","rejectionReason":"Please use annual leave"}
      ]}"""

    private const val HOLIDAYS = """{"dates":["2026-12-25","2027-01-01"]}"""

    private const val EXPENSES = """{"approvedTotal":428.5,
      "categories":[{"id":"c1","code":"TRAVEL","name":"Travel"},{"id":"c2","code":"MEALS","name":"Meals"},{"id":"c3","code":"MEDICAL","name":"Medical Claim"}],
      "claims":[
        {"id":"x1","description":"Taxi to client site","amount":32.5,"currency":"SGD","category":"Travel","expenseDate":"2026-11-02","status":"PENDING"},
        {"id":"x2","description":"Clinic visit","amount":45,"currency":"SGD","category":"Medical Claim","expenseDate":"2026-10-18","status":"APPROVED","receiptUrl":"/api/uploads/r.jpg"},
        {"id":"x3","description":"Team lunch","amount":120,"currency":"SGD","category":"Meals","expenseDate":"2026-09-30","status":"REJECTED","rejectionReason":"Missing receipt details"}
      ]}"""

    private const val PAYSLIPS = """{"payslips":[
        {"id":"p1","payPeriodStart":"2026-10-01","payPeriodEnd":"2026-10-31","paymentDate":"2026-10-31","grossSalary":4500,"cpfEmployee":900,"netSalary":3600,"status":"PAID","pdfPath":"/x.pdf"},
        {"id":"p2","payPeriodStart":"2026-09-01","payPeriodEnd":"2026-09-30","paymentDate":"2026-09-30","grossSalary":4500,"cpfEmployee":900,"netSalary":3600,"status":"PAID","pdfPath":"/y.pdf"}
      ]}"""

    private const val APPROVALS = """{
      "leaves":[
        {"id":"l1","employee":"Daniel Tan","leaveType":"No Pay Leave","leaveCode":"NPL","startDate":"2026-11-24","endDate":"2026-11-27","days":4,"dayType":"FULL_DAY","reason":"Personal trip to visit family overseas; flights already booked for the 24th, returning on the 27th night.","createdAt":"2026-11-08T03:00:00.000Z"},
        {"id":"l2","employee":"Sarah Lim","leaveType":"Annual Leave","leaveCode":"AL","startDate":"2026-12-22","endDate":"2026-12-24","days":2.5,"dayType":"FULL_DAY","reason":"Year-end holiday","createdAt":"2026-11-07T06:30:00.000Z"},
        {"id":"l3","employee":"Kenneth Wang","leaveType":"Medical Leave","leaveCode":"MC","startDate":"2026-11-06","endDate":"2026-11-06","days":1,"dayType":"FULL_DAY","reason":"Fever and sore throat, MC attached","documentUrl":"/api/uploads/mc.jpg","createdAt":"2026-11-06T02:10:00.000Z"}],
      "claims":[{"id":"c1","employee":"Amanda Halim","category":"Travel","description":"Grab to venue for the Python class at Woods Square","amount":18.4,"expenseDate":"2026-11-03","createdAt":"2026-11-04T01:00:00.000Z"}]}"""

    private const val EMPLOYEES = """{"isAdmin":ISADMIN,"employees":[
        {"id":"e1","name":"Sarah Lim","position":"Training Executive","department":"Operations","email":"sarah.lim@example.com","phone":"+65 9123 4567"},
        {"id":"e2","name":"Daniel Tan","position":"Developer","department":"Technology","email":"daniel.tan@example.com"}]}"""

    private const val RECORDS = """{"employee":{"id":"e2","employeeCode":"E0012","name":"Daniel Tan","position":"Developer","department":"Technology","employmentType":"FULL_TIME"},
      "attendance":{"days":[{"id":"d1","date":"2026-11-05","clockIn":"2026-11-05T01:05:00.000Z","clockOut":"2026-11-05T10:02:00.000Z","hours":8.95}],"totalHours":8.95,"daysWorked":1},
      "leave":[{"id":"r3","leaveType":"Medical Leave","leaveCode":"MC","startDate":"2026-09-03","endDate":"2026-09-04","days":2,"status":"APPROVED"},
               {"id":"r1","leaveType":"No Pay Leave","leaveCode":"NPL","startDate":"2026-11-24","endDate":"2026-11-27","days":4,"status":"PENDING"}]}"""

    private const val ATTENDANCE = """{"today":null,"recent":[]}"""

    private const val HISTORY = """{"month":"2026-11","days":[
        {"id":"d1","date":"2026-11-05","clockIn":"2026-11-05T01:05:00.000Z","clockOut":"2026-11-05T10:02:00.000Z","hours":8.95},
        {"id":"d2","date":"2026-11-04","clockIn":"2026-11-04T01:00:00.000Z","clockOut":"2026-11-04T10:00:00.000Z","hours":9}],"totalHours":17.95,"daysWorked":2}"""

    private const val ATTENDANCE_SUMMARY = """{"month":"2026-11","totalHours":70.5,"employees":[
        {"id":"e5","name":"Amanda Halim","employeeCode":"I0002","isIntern":true,"daysWorked":4,"totalHours":32,"lastPunchDate":"2026-11-05","clockedInNow":true},
        {"id":"e6","name":"Kenneth Wang","employeeCode":"I0012","isIntern":true,"daysWorked":4,"totalHours":29.5,"lastPunchDate":"2026-11-05"},
        {"id":"e2","name":"Daniel Tan","employeeCode":"E0012","isIntern":false,"daysWorked":1,"totalHours":9,"lastPunchDate":"2026-11-04"}]}"""

    private const val CORRECTIONS = """[
        {"id":"pc1","employeeId":"e5","employeeName":"Amanda Halim","employeeCode":"I0002","date":"2026-11-05","kind":"CLOCK_OUT",
         "requestedClockIn":null,"requestedClockOut":"2026-11-05T10:00:00.000Z","currentClockIn":"2026-11-05T01:02:00.000Z","currentClockOut":null,
         "reason":"Forgot to clock out after the evening class.","status":"PENDING","rejectionReason":null,"createdAt":"2026-11-06T01:00:00.000Z"}]"""

    private const val OIL = """[
        {"id":"oil1","employeeId":"e6","employeeCode":"I0012","employeeName":"Kenneth Wang","date":"2026-11-08","dayName":"Sun","isPublicHoliday":true,"hours":8,"otCredited":0,"status":"PENDING","adminComment":null},
        {"id":"oil2","employeeId":"e2","employeeCode":"E0012","employeeName":"Daniel Tan","date":"2026-11-01","dayName":"Sat","isPublicHoliday":false,"hours":4,"otCredited":0.5,"status":"APPROVED","adminComment":null}]"""

    private const val LEAVE_SUMMARY = """{"company":"Demo","month":"2026-11","balancesYear":2026,
      "totals":{"headcount":{"staff":2,"interns":1,"total":3},"employeesWithLeave":2,
        "byLeaveType":[{"code":"AL","name":"Annual Leave","approvedDays":4,"pendingDays":2,"employeesOnLeave":2},{"code":"NPL","name":"No Pay Leave","approvedDays":1,"pendingDays":4,"employeesOnLeave":1}],
        "mc":{"approvedDays":3,"pendingDays":0,"employeesOnMc":2},"timeOff":{"approvedHours":3,"pendingHours":1}},
      "employees":[
        {"employeeId":"E0007","name":"Sarah Lim","category":"STAFF","position":"Training Executive","department":"Operations","mcDays":2,
         "leaveTaken":{"MC":{"approved":2,"pending":0},"NPL":{"approved":1,"pending":4}},
         "requests":[{"leaveType":"NPL","leaveTypeName":"No Pay Leave","status":"PENDING","startDate":"2026-11-24","endDate":"2026-11-27","totalDays":4,"daysInMonth":4}],
         "balances":[{"code":"AL","name":"Annual Leave","entitlement":7,"usedYtd":1,"pending":0,"available":6},{"code":"MC","name":"Medical Leave","entitlement":14,"usedYtd":2,"pending":0,"available":12}]},
        {"employeeId":"I0002","name":"Amanda Halim","category":"INTERN","position":"Intern","mcDays":1,"leaveTaken":{"MC":{"approved":1,"pending":0}},
         "requests":[],"timeOff":{"approvedHours":3,"pendingHours":1,"requests":[]},"balances":[]}
      ]}"""

    private const val TEAM_CALENDAR = """{"year":2026,"canSeeTypes":true,"entries":[
        {"id":"t1","employeeId":"e2","employeeName":"Daniel Tan","startDate":"2026-11-10","endDate":"2026-11-12","days":3,"leaveType":"No Pay Leave"},
        {"id":"t2","employeeId":"e1","employeeName":"Sarah Lim","startDate":"2026-11-19","endDate":"2026-11-19","days":1,"leaveType":"Annual Leave","isSelf":true}],
      "holidays":[{"id":"h1","title":"Deepavali","startDate":"2026-11-08","endDate":"2026-11-08"}]}"""

    private const val CALENDAR = """{"events":[{"id":"ev1","title":"Deepavali","startDate":"2026-11-08","endDate":"2026-11-08","allDay":true,"type":"HOLIDAY"}]}"""

    private const val NOTIFICATIONS = """[{"id":"n1","title":"Leave approved","message":"Your annual leave was approved.","type":"LEAVE_APPROVED","read":false,"link":"/leave","createdAt":"2026-11-05T02:00:00.000Z"}]"""

    private const val TIME_OFF_ALL = """[
        {"id":"to2","employeeId":"e5","date":"2026-11-12","startTime":"14:00","endTime":"17:00","hours":3,"reason":"EXAMS","reasonDetail":"Final-year exam: Data Structures paper at NTU","status":"PENDING","createdAt":"2026-11-07T04:00:00.000Z","employee":{"name":"Amanda Halim"}},
        {"id":"to3","employeeId":"e6","date":"2026-11-10","startTime":"09:00","endTime":"11:00","hours":2,"reason":"EMERGENCY","status":"PENDING","createdAt":"2026-11-09T23:30:00.000Z","employee":{"name":"Kenneth Wang"}},
        {"id":"to4","employeeId":"e5","date":"2026-10-21","startTime":"10:00","endTime":"12:00","hours":2,"reason":"OTHERS","reasonDetail":"Dental appointment","status":"APPROVED","createdAt":"2026-10-15T04:00:00.000Z","employee":{"name":"Amanda Halim"}}]"""

    private const val TIME_OFF = """[{"id":"to1","date":"2026-11-07","startTime":"10:00","endTime":"12:00","hours":2,"reason":"UNPAID_LEAVE","status":"PENDING"}]"""

    private const val TIMESHEET = """{"isLocked":false,"days":[
        {"date":"2026-11-02","dayName":"Mon","hours":0,"isNonWorkDay":false,"isSubmittable":false},
        {"date":"2026-11-07","dayName":"Sat","hours":8,"status":"PENDING","isNonWorkDay":true,"isSubmittable":true},
        {"date":"2026-11-08","dayName":"Sun","hours":0,"isNonWorkDay":true,"isSubmittable":true,"isPublicHoliday":true,"phName":"Deepavali"}]}"""

    private const val TRAINERS = """{"count":3,"trainers":[
        {"id":"tr1","name":"TAN WEI MING","nickname":"Wei Ming","phone":"+65 9123 4567","email":"weiming.tan@example.com","trainerType":"ACLP","status":"Active","inTms":true,
         "trainingCerts":["ACLP","ACTA"],"certifications":["PMP","CCAR-F"],"skills":["Power BI","Python","Agile"],"education":"MSc Business Analytics, NUS",
         "financeBackground":true,"ibfFitDeclaration":true,"classesCount":34,"lastClassDate":"2026-10-28","courseTitles":["Data Analytics with Power BI"]},
        {"id":"tr2","name":"NUR AISYAH BINTE RAHMAN","phone":"+65 8234 5678","email":"aisyah@example.com","trainerType":"non-ACLP","status":"Active","inTms":true,
         "trainingCerts":["ACTA"],"certifications":["AWS Solutions Architect"],"skills":["Cloud","Docker"],"financeBackground":false,"ibfFitDeclaration":false,
         "classesCount":12,"lastClassDate":"2026-09-15","courseTitles":["AWS Cloud Practitioner"]},
        {"id":"tr3","name":"LIM KAH HENG (DR)","email":"kh.lim@example.com","trainerType":"Applicant","status":"Applicant","inTms":false,
         "trainingCerts":["DACE"],"certifications":["CFA"],"skills":["Corporate Finance","Excel"],"financeBackground":true,"ibfFitDeclaration":false,
         "classesCount":0,"lastClassDate":null,"courseTitles":[]}
      ]}"""

    private const val TRAINER = """{"trainer":{"id":"tr1","name":"TAN WEI MING","nickname":"Wei Ming","nric":"S8123456D","phone":"+65 9123 4567",
        "email":"weiming.tan@example.com","secondaryEmail":"wm.tan@example.org","nationality":"Singaporean","linkedinUrl":"linkedin.com/in/example",
        "trainerType":"ACLP","status":"Active","inTms":true,"trainingCerts":["ACLP","ACTA"],"certifications":["PMP","CCAR-F","Tableau Desktop Specialist"],
        "skills":["Power BI","Python","Agile","Data Storytelling"],"education":"MSc Business Analytics, National University of Singapore",
        "experience":["Senior Data Analyst, DBS Bank (2016 - 2022)","Freelance trainer, analytics and BI (2022 - present)"],
        "summary":"Analytics trainer with ten years in banking data teams.","financeBackground":true,"financeEvidence":["DBS employment letter.pdf"],
        "ibfFitDeclaration":true,"ibfFitFiles":["IBF Fit and Proper - Tan Wei Ming.pdf"],"actaFolder":"TAN WEI MING","evidenceFiles":["ACLP cert.pdf","ACTA cert.pdf"],
        "courses":[{"title":"Data Analytics with Power BI","code":"TGS-2023012345","classes":21,"first":"2023-02-06","last":"2026-10-28"},
                   {"title":"Python for Finance","code":"TGS-2024056789","classes":13,"first":"2024-04-15","last":"2026-08-19"}],
        "recentClasses":[{"start":"2026-10-28","title":"Data Analytics with Power BI","run":42,"status":"Completed"},
                         {"start":"2026-08-19","title":"Python for Finance","run":"17","status":"Completed"}],
        "calendarClasses":[],"classesCount":34,"lastClassDate":"2026-10-28","adminNotes":null}}"""
}

