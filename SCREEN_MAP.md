# Tertiary HRMS — Screen Map

Per-screen breakdown of every route in the web app (`tertiary-hrms`, Next.js 14 App Router,
`src/app/`): API calls, data displayed, user actions, navigation targets, and loading/error/empty
states. Companion to [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) (which covers visual tokens + the same
route inventory at a lighter level of detail). Use this doc when porting behavior 1:1 to the
native Android/iOS clients.

---

## Auth flow (cross-cutting)

Config: `src/lib/auth.config.ts` (edge-safe: `trustHost`, JWT sessions maxAge 30 days,
`pages: { signIn: "/login", error: "/login" }`), `src/lib/auth.ts` (full NextAuth setup, Node
runtime), `src/app/api/auth/[...nextauth]/route.ts`, `src/app/api/auth/send-otp/route.ts`
(custom OTP endpoint), `src/middleware.ts` (route gate).

**Providers:**
- `Google` (only registered if `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` env vars are set) —
  `prompt: select_account`, `access_type: offline`.
- `Credentials` id `"credentials"` — email/password, bcrypt-checked against `User.password`
  (falls back to `DEFAULT_EMPLOYEE_PASSWORD` env or `"Password123"` if unset).
- `Credentials` id `"otp"` — email + 6-digit code validated against `prisma.otpCode` (unused,
  unexpired, matching); marks `used: true` on success.

**Sequence:**
1. **Middleware gate** (`src/middleware.ts`) — runs on every request except `_next/static`,
   `_next/image`, `favicon.ico`, and files with extensions. Only checks for a session-cookie's
   *presence* (`authjs.session-token` / `__Secure-authjs.session-token` / `next-auth.session-token`
   / `__Secure-next-auth.session-token`), not JWT validity. Public allow-list: `/login`,
   `/register`, `/api/auth`, `/api/cron`, `/api/uploadthing`, `/api/public`, `/api/webhooks`,
   `/privacy-policy`, `/myinfo`, `/api/myinfo/auth`, `/api/myinfo/callback`. No cookie + non-public
   route → redirect `/login`. Has cookie + hitting `/login`/`/register`/`/` → redirect `/dashboard`.
2. **OTP path**: `POST /api/auth/send-otp` `{email}` → invalidates prior unused OTPs, creates a new
   `OtpCode` (30-min expiry), emails via SMTP (nodemailer) or Gmail OAuth fallback → returns
   `{success, message, emailConfigured}`. Verify: `signIn("otp", {email, otp, redirect:false})` →
   internally hits `/api/auth/callback/otp` → on success `window.location.href = "/dashboard"`
   (hard nav, not `router.push`).
3. **Password path**: `signIn("credentials", {email, password, redirect:false})` → same hard-nav
   success handling.
4. **Google OAuth path**: `signIn("google", {callbackUrl:"/dashboard"})` → full redirect to Google
   consent → `/api/auth/callback/google` → `signIn` callback creates a new `User` (`roles:["STAFF"]`,
   blank password) if none exists for that email, or reuses an existing one (rejects if
   `employee.status === "INACTIVE"`) → JWT callback stamps `role`, `roles`, `employeeId`,
   `needsSetup` onto the token.
5. **Session shape**: `session.user = {id, email, role, roles, employeeId?, needsSetup?, name?}`.
6. **Post-login routing**: pages call `auth()` server-side; if `session.user.employeeId` is
   missing, `/pending-setup` is the intended landing.
7. **Sign-out**: `GET /api/auth/csrf` → `POST /api/auth/signout` (`redirect:"manual"`, body
   `{csrfToken, callbackUrl:"/login"}`, avoiding NextAuth's built-in `AUTH_URL` redirect) →
   `window.location.href = "/login"`.
8. **Dev bypass**: `isDevAuthSkipped()` short-circuits middleware + page-level `auth()` checks,
   defaulting to the seeded `admin@tertiaryinfotech.com` ADMIN account; login page exposes
   `handleSkipLogin("admin"|"staff"|"staff2")` dev buttons behind `NEXT_PUBLIC_TEST_PASSWORD`.

---

## Route: `/login`
`src/app/(auth)/login/page.tsx` — client component, `<Suspense fallback={<LoginSkeleton/>}>` around
`LoginForm` (needs `useSearchParams`).

**API calls**
| Method | Path | Trigger |
|---|---|---|
| GET | `/api/public/branding` | On mount — loads `{name, shortName, logo}` for the login card |
| POST | `/api/auth/send-otp` `{email}` | "Send OTP" submit |
| POST | `/api/auth/send-otp` `{email}` | "Resend OTP" click |
| `signIn("otp", {email, otp, redirect:false})` | internal → `/api/auth/callback/otp` | "Verify & Sign In" submit |
| `signIn("credentials", {email, password, redirect:false})` | internal | Password form submit |
| `signIn("google", {callbackUrl:"/dashboard"})` | internal, full redirect | "Sign in with Google" |
| `signIn("credentials", {...seeded creds})` | internal | dev "skip login" buttons |

**Data displayed**: `branding.name`/`shortName`/`logo` (image or initials avatar); step-specific
fields — email input, 6-digit OTP input (numeric, auto-focus), password input (visibility toggle);
"Remember my email" persisted to `localStorage` (`hrms_remembered_email`,
`hrms_remember_email`); OAuth error mapped via `OAUTH_ERROR_MESSAGES` keyed on `?error=`
(`OAuthSignin`, `OAuthCallback`, `OAuthCreateAccount`, `OAuthAccountNotLinked`, `Callback`,
`CredentialsSignin`, `AccessDenied`, `Default`).

**User actions**: Send OTP (submit); "Sign in with password instead"; Back (from OTP/password
step); Resend OTP (disabled `isResending`, re-enabled after 5s); Verify & Sign In (submit); toggle
password visibility; Sign in with Google; Remember-my-email checkbox.

**Navigation**: `window.location.href = "/dashboard"` after OTP/password success (hard nav);
`signIn("google",...)` triggers full-page redirect to Google then back via callback. No `Link`s.

**Loading state**: `<Suspense fallback={<LoginSkeleton/>}>` — pulsing gray blocks for logo/
title/subtitle/fields. Per-action `isLoading` swaps button label + spinner, e.g.
`Loader2 spin + "Sending OTP..."`; Google button → `"Redirecting to Google..."`; Resend →
`"Resending..."`.

**Error state**: Inline banner `bg-red-950/50 border-red-800 text-red-400` above the form, set from
invalid email format, failed send/resend, invalid/expired OTP, invalid credentials, or mapped
OAuth callback errors.

**Empty state**: N/A (form-only screen).

---

## Route: `/`
`src/app/page.tsx` — redirects immediately to `/dashboard`. No API calls, data, actions, or states.

## Route: `/dashboard`
`src/app/(dashboard)/dashboard/page.tsx` — async Server Component (`dynamic='force-dynamic'`); no
client fetches — data loaded via direct Prisma (`getAdminStats`/`getStaffStats`/
`getRecentActivity`).

**API calls**: None (server-rendered). Child components (`StatsCards`, `QuickActions`,
`RecentActivity`) receive data as props, no fetches of their own.

**Data displayed**:
- Header: `Welcome Back, {displayName}` (from `employee.name`, defaults `"User"`).
- **StatsCards** (admin): `pendingLeaves` (excl. MC), `pendingMC`, `pendingClaims`.
- **StatsCards** (staff): `leaveBalance` (prorated AL entitlement + carriedOver − used − pending),
  `mcBalance` (not prorated), `expenseClaimAmount` (sum of APPROVED claims this year), `otStats`
  (Off-In-Lieu: `earned`/`used`/`autoDeducted`/`remaining` — card only shown if `earned>0` or
  `remaining<0`).
- **QuickActions** — admin: Create Staff → `/employees/new`, Manage Leaves → `/leave`, Manage
  Claims → `/expenses`, View Calendar → `/calendar`. Staff: Request Leave → `/leave/request`,
  Submit Expense → `/expenses/submit`, View Calendar → `/calendar`.
- **RecentActivity** — last 5 expense claims (`employee.name`, formatted amount, `category.name`,
  relative date). Note: a `leaves` prop is fetched and passed but the component only destructures
  `expenses` — leave activity is silently dropped from the UI (dead prop).

**User actions**: Each Quick Action is a clickable `Link` card (`ChevronRight` affordance).

**Navigation**: `Link` to `/employees/new`, `/leave`, `/expenses`, `/calendar`, `/leave/request`,
`/expenses/submit`.

**Loading state**: Shared route-group `(dashboard)/loading.tsx` — `Loader2 spin` + `"Loading…"`,
centered, `min-h-[60vh]`.

**Error state**: No local error UI; unhandled errors bubble to the nearest `error.tsx` boundary.

**Empty state**: `RecentActivity` → `"No recent activity"` when `expenses.length === 0`.

## Route: `/pending-setup`
`src/app/(dashboard)/pending-setup/page.tsx` — async Server Component.

**API calls**: None — pure `auth()` check + conditional redirect.

**Data displayed**: `session.user.email`; static copy "Account Pending Setup" / "Your account has
been created successfully. An HR administrator needs to complete your employee profile before you
can access the system."

**User actions**: None.

**Navigation (redirects)**: `redirect("/login")` if no session; `redirect("/dashboard")` if
`employeeId` already set (page is self-obsoleting once HR finishes onboarding).

**Loading/Error state**: Shared route-group spinner during nav; no fetches to fail.

**Empty state**: N/A — this page *is* the empty/incomplete-profile state for the app.

---

## Route: `/profile`
`src/app/(dashboard)/profile/page.tsx` — async Server Component (`dynamic="force-dynamic"`).

**API calls (page)**: None directly — Prisma `user.findUnique` (+`employee.department`,
`employee.salaryInfo`) and `department.findMany`.

**API calls (children)**:
| Component | Method | Path | Trigger |
|---|---|---|---|
| `EmployeeDetailEditable` | POST | `/api/upload` (multipart) | Avatar camera-icon → file picker, while editing |
| `EmployeeDetailEditable` | PATCH | `/api/employees/{id}` `{personalInfo, employmentInfo, roles}` | "Save" |
| `PasswordChangeCard` | PATCH | `/api/profile/password` `{currentPassword, newPassword}` | "Change Password" |
| `ThemeCard` | — (local `next-themes` `setTheme`, no network) | — | Dark/Light buttons |

**Data displayed**: "My Profile" / "Your personal information". If no linked employee: "Your
employee profile has not been set up yet. Please contact HR." Else full `EmployeeDetailEditable`
(same component as `/employees/[id]`, `canEdit={true}` always here): name, position, employeeId,
status badge, avatar, email (masked if placeholder), phone, DOB, gender, nationality,
educationLevel, NRIC, address, department, role badges, startDate, endDate, employmentType,
workdays (Mon–Sun toggle chips), status. Plus `ThemeCard` and `PasswordChangeCard`.

**User actions**: "Edit Employee" toggle; avatar upload (edit mode only); per-field edits (email,
phone, DOB, gender/education/department selects, NRIC, address, role chips, dates, employment-type
select, workday chips, status); "Cancel" (resets to `initialForm`); "Save" (PATCH, then
`router.refresh()`); Theme Dark/Light buttons; password card 3 inputs + eye-toggles + "Change
Password" (client validates match + min length 6 before PATCH).

**Navigation**: None — in-place edits via `router.refresh()`.

**Loading state**: Save button → spinner in place of check icon while `saving`; avatar upload
button spins while `uploading`; password button → `"Changing..."` while `isLoading`.

**Error state**: `EmployeeDetailEditable` — inline red banner
(`border-red-800 bg-red-950/40 text-red-300`). `PasswordChangeCard` — toast, e.g. "New passwords
do not match", "Password must be at least 6 characters", server failure; success toast "Password
changed".

**Empty state**: "Your employee profile has not been set up yet. Please contact HR." when
`user.employee` is null.

## Route: `/employees`
`src/app/(dashboard)/employees/page.tsx` — async Server Component (`dynamic='force-dynamic'`).
*(Admin-facing — excluded from staff/intern native clients per CLAUDE.md.)*

**API calls (page)**: None directly — Prisma `employee.findMany`/`findUnique` (scoped admin vs
staff), `department.findMany`.

**API calls (`EmployeeList` child)**: None — pure client-side filter/search/pagination over the
server-provided array.

**Data displayed**: Header "Employees" + "{count} team member(s)". Table: Employee ID, Name, Role
badges (sorted ADMIN/STAFF/ACCOUNTANT/INTERN/HR/MANAGER), Status badge
(ACTIVE/ON_LEAVE/TERMINATED/RESIGNED/INACTIVE), Job Function, Email (masked if placeholder), Tel,
Start/End Date, edit-pencil. Non-admin viewers see only their own record (list of 1, filters
hidden).

**User actions**: Search input (name/email/position); Department `Select`; Status `Select`
(default "ACTIVE"); "Create Staff"/"Create Intern" (admin only); row click → full navigation;
pencil icon → client nav; Previous/Next pagination (`PAGE_SIZE=20`).

**Navigation**: `Link` → `/employees/new`, `/employees/new-intern`, `/employees/{id}` (pencil);
`window.location.href = "/employees/{id}"` (row click, hard nav — inconsistent with the rest of
the app's client-side nav).

**Loading state**: Shared route-group spinner during server nav; client filtering/pagination is
instant (in-memory).

**Error state**: None present (no client fetches to fail).

**Empty state**: "No employees found" when filtered results are empty.

## Route: `/employees/[id]`
`src/app/(dashboard)/employees/[id]/page.tsx` — async Server Component (`dynamic='force-dynamic'`).

**API calls (page)**: None directly — Prisma for `employee` (+`user.roles`, `department`,
`salaryInfo`, current+last-year `leaveBalances`, `leaveRequests`), `departments`,
`candidateManagers`, `currentManagers`.

**API calls (children)**:
| Component | Method | Path | Trigger |
|---|---|---|---|
| `EmployeeDetailEditable` | POST | `/api/upload` | Avatar upload (edit mode) |
| `EmployeeDetailEditable` | PATCH | `/api/employees/{id}` | "Save" |
| `InternCompensationCard` (interns) | PUT | `/api/employees/{id}/intern-allowance` `{allowance}` | Save |
| `StaffCompensationCard` (non-interns) | PUT | `/api/employees/{id}/salary-info` `{basicSalary}` | Save |
| `ManagersCard` | PUT | `/api/employees/{id}/managers` `{managerIds[]}` | "Add" / Trash icon |
| `AdminOtLogPanel` | GET | `/api/ot-log?employeeId={id}` | On mount |
| `AdminOtLogPanel` | POST | `/api/ot-log` `{employeeId, date, type, note, daysEarned}` | "Save Entry" |

**Data displayed**: `EmployeeDetailEditable` fields (same as Profile), `canEdit` = ADMIN only.
`InternCompensationCard`: `allowance`, Net Take-Home, CPF $0 (interns exempt). `StaffCompensationCard`
(if `cpfApplicable`): `basicSalary`, `allowances`, computed gross/CPF-EE/CPF-ER/net, `payNow`, CPF
rates. `ManagersCard`: manager list (name/email/position), candidate dropdown. Leave Balance cards
(current + last year): entitlement, prorated allocation, carried-over, used, pending, remaining
(colored). Leave History table: type, dates, days, status badge, approver, applied-on.
`AdminOtLogPanel`: date, type badge (Weekend/Public Holiday/Other), note, recordedBy, `+Nd`.

**User actions**: Same edit/save/cancel/avatar flow as Profile (gated `canEdit`=ADMIN);
compensation cards pencil→number input→Save/Cancel; Managers `Select`+"Add", per-manager Trash;
OT Log "Add Entry" toggle → form (Date, Type select, Days 0.5–3 step 0.5, Note) → Save/Cancel.

**Navigation**: None directly (`router.refresh()` after child saves). Redirects: `/login` if
unauthenticated; `/employees` if a STAFF views someone else's profile; `notFound()` if employee
doesn't exist.

**Loading state**: Compensation/Managers Save buttons → `Loader2` spinner while saving/removing;
`AdminOtLogPanel` → `Loader2 + "Loading..."` while fetching logs.

**Error state**: `EmployeeDetailEditable` inline red banner; Compensation/Managers cards → toast
(e.g. "Save failed", "Invalid allowance — must be a non-negative number"). `AdminOtLogPanel` — **no
error toast**: GET/POST failures fail silently (empty array / form stays open, no feedback).

**Empty state**: `ManagersCard` → "No manager assigned. Approval emails will fall back to the
company default approver." (no managers) / "All eligible employees are already assigned as
managers." (no candidates). `AdminOtLogPanel` → "No Off In Lieu entries recorded yet." Leave
Balance/History/OT sections are omitted entirely (not shown with empty text) if the employee has
zero balance records.

## Route: `/employees/new`
`src/app/(dashboard)/employees/new/page.tsx` — async Server Component wrapping client
`AddEmployeeForm` (`intent="STAFF"`).

**API calls (page)**: None — `department.findMany`, `auth()` check.

**API calls (form)**: `POST /api/employees` `{personalInfo, employmentInfo, salaryInfo?,
role:"STAFF"}` on "Create Staff" (final tab).

**Data displayed**: "Create Staff" / "Register a new staff member". 3 tabs: Personal (fullName,
email, phone, DOB, gender, nationality default "Singaporean", NRIC, address, educationLevel
default "DIPLOMA", school), Employment (departmentId, position, employmentType default
"FULL_TIME", startDate default today, endDate, status default "ACTIVE", monthlyLeaveRate,
managerId), Salary (basicSalary, allowances, bankName, bankAccountNumber, payNow, cpfApplicable
default true, cpfEmployeeRate default 20.0, cpfEmployerRate default 17.0). Draft auto-saved to
`localStorage` (`add-staff-draft`, full state + active tab) every 2s and on tab change.

**User actions**: Next/Back tab nav; "Reset tab" (confirm dialog, resets only current tab);
back-arrow → `router.push("/employees")`; "Create Staff" (validates all 3 tabs via Zod, jumps to
first invalid tab with a toast of `error.issues[0].message`; on success clears draft, toasts
"Employee created", navigates).

**Navigation**: Back-arrow → `/employees`; on success → `/employees/{newId}`. Server redirects:
`/login` unauthenticated; `/employees` if role not ADMIN/HR.

**Loading state**: "Create Staff" → `Loader2 + "Creating..."`, disabled during submit.

**Error state**: Destructive toasts — per-tab "Personal/Employment/Salary Info Incomplete" (first
Zod issue message), generic creation-failure toast.

**Empty state**: N/A (creation form).

## Route: `/employees/new-intern`
`src/app/(dashboard)/employees/new-intern/page.tsx` — same `AddEmployeeForm` component,
`intent="INTERN"`.

**API calls**: `POST /api/employees` `{personalInfo, employmentInfo:{...,employmentType:"INTERN"},
role:"INTERN"}` (no `salaryInfo` — Salary tab hidden entirely) on "Create Intern".

**Data displayed**: "Create Intern" / "Register a new intern". Only 2 tabs (Personal, Employment;
`employmentType` locked to INTERN). Draft in `localStorage` (`add-intern-draft`, separate key).

**User actions**: Next (Personal→Employment); "Create Intern" directly on Employment tab (no
further step); Back; Reset tab; back-arrow → `/employees`.

**Navigation**: Back-arrow → `/employees`; success → `/employees/{newId}`. Server redirects same as
`/employees/new`.

**Loading state**: "Create Intern" → `Loader2 + "Creating..."`.

**Error state**: Same destructive-toast pattern (Salary validation skipped since `isIntern`).

**Empty state**: N/A.

---

## Route: `/leave` (redirect)
`src/app/(dashboard)/leave/page.tsx` — server component, unconditional `redirect("/leave/annual")`.
No API calls, data, actions, or states.

## Route: `/leave/annual`
`src/app/(dashboard)/leave/annual/page.tsx` — server component rendering `LeaveBalanceCards`,
`OtBreakdownDialog`, `LeaveList`.

**API calls**: Page: none via fetch (server Prisma: `getLeaveBalance`, `getOtLeaveBalance`,
`getLeaveRequests`). `OtBreakdownDialog`: `GET /api/ot-log` or `?employeeId={id}` when opened.
`LeaveList` (client, fires on interaction):
- `POST /api/leave/bulk-approve` `{ids}` — bulk "Approve N" (admin)
- `POST /api/leave/bulk-reject` `{ids, reason}` — bulk "Reject N" (admin, after `prompt()`)
- `POST /api/leave/{id}/approve` `{approvalComment}` — row Approve (admin)
- `POST /api/leave/{id}/reject` `{reason}` — row Reject confirm (admin)
- `POST /api/leave/{id}/cancel` — staff "Yes, Cancel" (own PENDING)
- `POST /api/leave/{id}/reset` — admin "Yes, Reset" (APPROVED/REJECTED → PENDING)

**Data displayed**: Header "Annual Leave", subtitle varies by role. "Paid Leave" pill (staff).
Carry-over expiry banner (Q4, staff): "You have {carriedOver} days of carried-over annual leave
expiring on 31 Dec {year}. Please clear them before they are forfeited." `LeaveBalanceCards`
(staff): carriedOver, allocation, taken, rejected, proRated, derived remaining — clicking
Remaining/Earned opens a monthly accrual breakdown modal. OT (Off In Lieu) block (staff): earned,
used, autoDeducted ("Deficit"), remaining — click opens `OtBreakdownDialog` (date, type, note,
daysEarned, recordedBy). Admin KPI cards: Total/Pending/Approved/Rejected. `LeaveList` fields:
employee.name/department, leaveType.name/code, startDate/endDate, days, dayType, halfDayPosition,
reason, documentUrl/documentFileName, status, createdAt, otDaysUsed ("+Nd OIL"), deficitDays,
approvalComment/rejectionReason, approver.name.

**User actions**: "Request Leave" (staff, → `/leave/request`); search; date-range filters; status
filter pills; column sort; row/select-all checkboxes → bulk toolbar (Approve N/Reject
N/Clear); per-row admin Approve (optional comment)/Reject (optional reason, confirm)/Reset (confirm);
per-row staff Edit (PENDING)/Cancel (confirm); "View Document" → `DocumentPreviewModal`.

**Navigation**: `Link` → `/leave/request` (staff), `/leave/edit/{id}` (staff, PENDING).
`router.refresh()` after every mutation.

**Loading state**: No page skeleton (server-rendered, `force-dynamic`). Button text swaps:
"Cancelling...", "Approving...", "Rejecting...", "Resetting..." (buttons disabled during request).
`OtBreakdownDialog` → "Loading..." while fetching.

**Error state**: Toast-only, `variant:"destructive"` (e.g. "Failed to cancel leave request", bulk
failure titled "Bulk approve/reject failed" + `err.message`). Guard text (no employee profile):
"Your employee profile has not been set up yet. Please contact HR."

**Empty state**: `LeaveList` → "No leave requests found". `OtBreakdownDialog` → "No Off In Lieu
records found" + "Off In Lieu days are credited when admin approves your weekend or public holiday
work submissions."

## Route: `/leave/medical`
`src/app/(dashboard)/leave/medical/page.tsx` — near-identical structure to `/leave/annual` for MC,
reuses `LeaveList`.

**API calls**: Page: server Prisma only (`getMcLeaveType`, `getMcBalance`, `getMcRequests`).
`LeaveList`: identical endpoint set to Annual Leave (`bulk-approve`, `bulk-reject`, `{id}/approve`,
`{id}/reject`, `{id}/cancel`, `{id}/reset`).

**Data displayed**: Header "Medical Leave" (Stethoscope icon); subtitle varies (admin/intern/
partially-paid/paid staff). Paid/Unpaid pills ("Unpaid Leave" + note for interns, or split
paid/unpaid day counts, or "Paid Leave"). Balance cards (staff): entitlement (with paid/unpaid
breakdown if partial), used ("MC Taken"), pending, remaining (red if negative). Admin KPI cards.
`LeaveList` — same fields as Annual, filtered to MC.

**User actions**: Same set as Annual's `LeaveList`, plus "Request MC" → `/leave/request?type=MC`.

**Navigation**: `Link` → `/leave/request?type=MC` (pre-selects MC on the request form),
`/leave/edit/{id}`. `router.refresh()` after mutations.

**Loading state**: Same button-text-swap pattern, no page skeleton.

**Error state**: Same toast pattern; same no-profile guard text.

**Empty state**: "No leave requests found".

## Route: `/leave/request`
`src/app/(dashboard)/leave/request/page.tsx` — server wrapper + client `LeaveRequestForm`.

**API calls**: Page: server Prisma only (`getLeaveTypes`, `getOtBalance`, `getAlData`).
`LeaveRequestForm`: `GET /api/public-holidays?year={year}` (on `startDate` year change, to compute
working days); `POST /api/upload` (multipart, only for MC document attachments, before the leave
POST); `POST /api/leave` `{leaveTypeId, startDate, endDate, days, dayType, halfDayPosition, reason,
documentUrl, documentFileName, otDaysUsed}` on submit.

**Data displayed**: Leave-type dropdown with dynamic labels (AL_OT shows OT balance, AL shows
earned-to-date, others show days/year). Auto-calculated "Number of Working Days" with a
calendar-day → working-day breakdown (weekends/PH excluded). AL-specific "Leave Balance Summary"
(earnedToday, alPersonalEntitlement, otBalance). Off-In-Lieu toggle + slider to apply OT days
against AL deficit/advance. Deficit/Advance/Earned breakdown panel with color-coded advisory text.
Insufficient-OT-balance warning banner.

**User actions**: Select Leave Type; Start/End DatePickers; Day Type toggle (Full/AM/PM, single-day
AL); half-day position toggle (multi-day AL); "Use Off In Lieu" toggle + range slider; Reason
textarea (optional); Upload MC document (image/PDF, max 5MB, required for MC) + remove; "Cancel" →
`router.back()`; "Submit Request"/"Submit with N.Xd deficit" (disabled unless
leaveTypeId/startDate/endDate/days set, days ≥0.5, not exceeding OT balance for AL_OT).

**Navigation**: Success → `router.push("/leave")` + `router.refresh()`. Cancel → `router.back()`.

**Loading state**: Submit button → "Submitting..." while `isLoading`, disabled.

**Error state**: Toast — "Failed to submit leave request" (or server message). Pre-submit
validation toast: "Insufficient Off In Lieu — You only have {otBalance} Off In Lieu day(s)
available." Upload failure throws before the submit toast.

**Empty state**: N/A (form). Leave-type dropdown would just have no options if none exist (no
explicit empty copy).

## Route: `/leave/edit/[id]`
`src/app/(dashboard)/leave/edit/[id]/page.tsx` — server gatekeeper + client `LeaveEditForm`.

**API calls**: Page: server Prisma lookup only. `LeaveEditForm`: `POST /api/upload` (if new
document selected); `PATCH /api/leave/{id}` `{startDate, endDate, days, dayType,
halfDayPosition, reason, documentUrl?/documentFileName? (or null if removed)}` on submit.

**Data displayed**: Read-only Leave Type ("cannot be changed"); Start/End DatePickers pre-filled;
Day Type/half-day toggle (AL only); auto-calculated Number of Days; Reason textarea pre-filled;
existing document filename + Preview (Eye)/Remove (X).

**User actions**: Edit Start/End Date, Day Type/half-day, Reason; replace/remove document (preview
via blob URL); "Cancel" → `/leave`; "Save Changes" (disabled unless startDate/endDate/days set,
days ≥0.5).

**Navigation**: Success → `router.push("/leave")` + refresh. Cancel → `/leave`. `Link` "Back to
Leave" on all gate-failure states.

**Loading state**: Submit → "Saving..." while `isLoading`.

**Error state**: Toast "Failed to update leave request". Server-side gate plain-text (not toast):
not found ("Leave request not found."), not owner ("You do not have permission to edit this leave
request."), not PENDING ("This leave request can no longer be edited (status: {status})." + "Only
pending leave requests can be edited. Please contact HR if you need changes."), not authenticated
→ `redirect("/login")`.

**Empty state**: N/A (single-record form).

---

## Route: `/expenses`
`src/app/(dashboard)/expenses/page.tsx` — server component + client `ExpenseList`.

**API calls**: Page: server Prisma only (`getExpenseStats`, `getExpenseClaims`, `getCategories`).
`ExpenseList`:
- `POST /api/expenses/bulk-approve` `{ids}` — bulk Approve N (finance/admin)
- `POST /api/expenses/bulk-reject` `{ids, reason}` — bulk Reject N (after `prompt()`)
- `POST /api/expenses/{id}/approve` — row Approve
- `POST /api/expenses/{id}/reject` — row Reject confirm
- `POST /api/expenses/{id}/cancel` — staff Cancel confirm (own PENDING)
- `POST /api/expenses/{id}/reset` — admin Reset (APPROVED/REJECTED → PENDING)
- `POST /api/expenses/{id}/acknowledge` — staff "Yes, Received" (marks APPROVED as settled)

**Data displayed**: Header "Expense Claims", subtitle by role. Staff stats card: total (sum of
APPROVED this year), claims count. Finance/Admin KPI cards: Total/Pending/Approved/Rejected.
`ExpenseList` fields: employee.name, category.name/code, description, amount, expenseDate, status
(PENDING/APPROVED/REJECTED/CANCELLED/PAID), receiptUrl/receiptFileName, approver.name, createdAt.

**User actions**: "New Expense" (staff, → `/expenses/submit`); search; date-range filters; status
filter (incl. Paid); column sort; row/select-all checkboxes → bulk toolbar; admin per-row PENDING
Approve/Reject (confirm); admin APPROVED/REJECTED "Reset to Pending" (confirm); staff PENDING
Edit/Cancel (confirm); staff APPROVED "Payment Received" (confirm, calls acknowledge); "View
Receipt" → `DocumentPreviewModal`.

**Navigation**: `Link` → `/expenses/submit`, `/expenses/edit/{id}` (staff, PENDING).
`router.refresh()` after every mutation.

**Loading state**: Row/bulk button text swaps ("Approving...", "Rejecting...", "Cancelling...",
"Resetting...", "Confirming..."). No page skeleton.

**Error state**: Toast-based ("Failed to approve expense", bulk "Bulk approve/reject failed" +
message). Guard text: "Your employee profile has not been set up yet. Please contact HR."

**Empty state**: "No expense claims found" (both admin and staff views).

## Route: `/expenses/submit`
`src/app/(dashboard)/expenses/submit/page.tsx` — server wrapper + client `ExpenseSubmitForm`.

**API calls**: Page: server Prisma (`getCategories`) only. Form: `POST /api/upload` (multipart, if
receipt attached, before expense POST); `POST /api/expenses` `{categoryId, description, amount,
expenseDate, receiptUrl, receiptFileName}` on submit.

**Data displayed**: Category dropdown (shows "Max claimable: ${maxAmount}" hint if set); Receipt
label toggles required/optional based on `category.requiresReceipt`.

**User actions**: Select Category; Description textarea (required); Amount (min 0.01, step 0.01,
required); Expense Date (max = today); upload receipt (image/PDF, max 5MB; required if category
demands it) + remove; "Cancel" → `router.back()`; "Submit Claim" (disabled unless
categoryId/description/amount/expenseDate set).

**Navigation**: Success → `router.push("/expenses")` + refresh. Cancel → `router.back()`.

**Loading state**: Submit button sequence: "Uploading..." → "Submitting..." → "Submit Claim".

**Error state**: Toast "Failed to submit expense claim" (covers upload + submit failures).

**Empty state**: N/A (form). No categories = empty dropdown (no explicit copy).

## Route: `/expenses/edit/[id]`
`src/app/(dashboard)/expenses/edit/[id]/page.tsx` — server gatekeeper + client `ExpenseEditForm`.

**API calls**: Page: server Prisma lookup + categories. Form: `POST /api/upload` (if new receipt);
`PATCH /api/expenses/{id}` `{categoryId, description, amount, expenseDate,
receiptUrl?/receiptFileName? (or null if removed)}` on submit.

**Data displayed**: Category dropdown pre-selected; Description/Amount/Expense Date pre-filled
(max = today); existing receipt filename + Preview/Remove.

**User actions**: Edit Category/Description/Amount/Date; replace/remove receipt (preview via blob
URL); "Cancel" → `/expenses`; "Save Changes" (disabled unless all fields set).

**Navigation**: Success → `router.push("/expenses")` + refresh. Cancel → `/expenses`. `Link` "Back
to Expenses" on gate-failure screens.

**Loading state**: Submit sequence: "Uploading..." → "Saving..." → "Save Changes".

**Error state**: Toast "Failed to update expense claim". Server gate plain-text: not found
("Expense claim not found."), not owner, not PENDING ("...can no longer be edited (status:
{status})." + contact-HR note), not authenticated → `redirect("/login")`.

**Empty state**: N/A (single-record form).

---

## Route: `/payroll`
`src/app/(dashboard)/payroll/page.tsx` — server component + client `PayrollList`.

**API calls**: Page: server Prisma (`getPayslips`) + Google-Drive helper (server-side, try/catch
logged, not a fetch). `PayrollList`:
- `GET /api/payroll/payslip/{id}/pdf` — "Download Payslip" (both views); blob downloaded via
  synthetic `<a download>` click.
- `PATCH /api/payroll/payslip/{id}` `{grossSalary, basicSalary}` — HR "Save" in Edit Gross Pay
  modal.
- `POST /api/payroll/payslip/{id}/regenerate` — HR "Regenerate PDF" icon.
- `POST /api/payroll/payslip/{id}/email` — HR "Email payslip to employee" icon.

**Data displayed**: Header "Payroll", subtitle by role. `PayrollList` fields: employee.name/
employeeId/department, payPeriodStart/End (MM/YYYY), basicSalary, allowances, grossSalary,
cpfEmployee, cpfEmployer, incomeTax, totalDeductions, netSalary, status (DRAFT/GENERATED/PAID). HR
columns: Date/Employee/Gross/CPF(ER)/CPF(EE)/Net Pay/PDF. Staff columns: Date/Gross/CPF(ER)/
CPF(EE)/Net Pay/PDF. Pagination: "Showing {start+1}–{end} of {total}", page size 50.

**User actions**: "Process Payroll" (finance/HR → `/payroll/generate`); "My Payroll Folder"
(staff, external Drive link, `target="_blank"`, only if resolved); search (HR)/status filter;
Download Payslip; HR: Edit gross pay (modal, Save/Cancel), Regenerate PDF, Email payslip;
Previous/Next pagination.

**Navigation**: `Link` → `/payroll/generate`. External `<a>` → Drive folder (staff). Refreshes
(not route changes) after gross-pay save and regenerate success.

**Loading state**: "Regenerate"/"Email" buttons → spinning `Loader2` while in flight per-id; Edit
modal Save → "Saving...". No page skeleton.

**Error state**: Toast "Regenerate failed"/"Email failed" + `err.message`. Edit-gross-pay
validation/failure uses `alert()` (not toast) — "Gross pay must be a non-negative number" /
`err.message`. Download PDF failure only `console.error`'d — **no user-visible error**.

**Empty state**: "No payslips found" (both views).

## Route: `/payroll/generate`
`src/app/(dashboard)/payroll/generate/page.tsx` — fully client component, no server data fetch.

**API calls**: `POST /api/payroll/upload` (multipart `{file, month, year}`) on "Upload & Process
Payroll"; `POST /api/payroll/generate` `{month, year}` on "Auto-Generate Payroll".

**Data displayed**: Month (Jan–Dec) / Year (current + 2 prior) selectors. Expected Excel columns
hint: "Employee ID | Name | Basic Salary | Allowances | Overtime | Bonus | CPF Employee | CPF
Employer | Income Tax | Other Deductions". Upload result: message, totalRows, created, updated,
errors, errorDetails[]. Generate result: message, created, skipped, errors.

**User actions**: Select Month/Year; "Choose File" (`.xlsx,.xls`) + remove; "Upload & Process
Payroll" (disabled while loading/no file); "Auto-Generate Payroll" (disabled while loading, skips
employees who already have a payslip for the period); "Back to Payroll" → `router.push("/payroll")`.

**Navigation**: "Back to Payroll" → `/payroll` (disabled while loading). No auto-redirect after
action — results shown inline.

**Loading state**: "Upload..." → `Loader2 + "Uploading..."`; "Auto-Generate..." → `Loader2 +
"Processing..."`; both buttons + Choose File disabled while loading.

**Error state**: Toast "Failed to upload/generate payroll". Success toasts too ("Payroll
uploaded"/"Payroll generated" with counts). Inline red error-detail list in result panel if
present.

**Empty state**: N/A (utility page — result panels only render after an action, initially `null`).

---

## Route: `/calendar`
`src/app/(dashboard)/calendar/page.tsx` — server component (`force-dynamic`) + client
`CalendarView`.

**API calls**: None client-side — server Prisma (`calendarEvent.findMany`, `leaveRequest.
findMany`) only. Navigating into a day/Add Event is a full page nav, not a fetch.

**Data displayed**: Legend (Holiday-red, Meeting-blue, Training-purple, Company Event-green,
Leave-amber). Month grid with per-day event chips (up to 2, `+N more` overflow). Event fields:
id, title, start/end, allDay, type, color, description. Shown: own MEETING/TRAINING/COMPANY_EVENT,
all HOLIDAY, and LEAVE (all employees if admin/admin-view, else own approved leave only).

**User actions**: "Add Event" → `/calendar/new`; Previous/Next month; "Today"; click a day cell.

**Navigation**: `Link` → `/calendar/new`, `/calendar/day/{YYYY-MM-DD}` (day cell click).

**Loading state**: None (server-rendered, `force-dynamic`).

**Error state**: "Your profile has not been set up yet. Please contact HR." if no resolvable user.

**Empty state**: No explicit "no events" message — empty days just render blank.

## Route: `/calendar/day/[date]`
`src/app/(dashboard)/calendar/day/[date]/page.tsx` — server component + client
`CalendarEventCard` per event.

**API calls**: Page: server Prisma only. `CalendarEventCard`: `DELETE /api/calendar/{id}` on "Yes,
Delete" confirm.

**Data displayed**: Date header + "{N} events, {M} leaves" or "Nothing scheduled". Events card:
color dot by type, title, type badge, description, start–end range. Leave card ("My Leaves"/
"People on Leave"): employee.name, leaveType badge, start–end, days, half-day markers,
department. Sorted meetings-first then by startDate.

**User actions**: "Back to Calendar"; "Add Event" → `/calendar/new?date={date}`; per event (owner
or admin): Edit → `/calendar/edit/{id}`; Delete → inline confirm (Yes, Delete/Cancel).

**Navigation**: `Link` → `/calendar` (top, and fallback screens), `/calendar/new?date={date}`.
`router.push` → `/calendar/edit/{id}`. `router.refresh()` after delete.

**Loading state**: Delete button → "Deleting..." while in flight (button disabled); no global
loading state.

**Error state**: Invalid date param → "Invalid Date" / `The date "{date}" is not valid. Please use
the format YYYY-MM-DD.` No employee profile → contact-HR text. Delete failure → toast "Error" +
server message or "Failed to delete event". Delete success → toast "Event deleted".

**Empty state**: Events card — "No events scheduled". Leave card — "You have no leave on this day"
(staff) / "No one is on leave" (admin).

## Route: `/calendar/new`
`src/app/(dashboard)/calendar/new/page.tsx` — server component + client `CalendarEventForm`
(create mode).

**API calls**: `POST /api/calendar` `{title, description, startDate, endDate, allDay, type}` on
submit.

**Data displayed**: Empty form, optionally pre-filled from `?date=` (`allDay:true` default). Fields:
Event Type select, Title, Description (optional), Start/End Date, All-day checkbox.

**User actions**: Fill fields; "Cancel" → `router.back()`; "Create Event" (disabled until
title/startDate/eventType set; shows "Creating..." while loading).

**Navigation**: Success → `router.push("/calendar")` + refresh. Cancel → back.

**Loading state**: Submit → "Creating...".

**Error state**: Toast "Failed to create event" or server message.

**Empty state**: N/A.

## Route: `/calendar/edit/[id]`
`src/app/(dashboard)/calendar/edit/[id]/page.tsx` — server component + client `CalendarEventForm`
(edit mode).

**API calls**: `PATCH /api/calendar/{id}` `{title, description, startDate, endDate, allDay, type}`
— fired only after a two-step confirm ("Save changes?" → Confirm).

**Data displayed**: Pre-filled form from the existing event.

**User actions**: Edit fields; "Cancel" → `router.back()`; "Save Changes" (not a submit button —
opens inline confirm: Confirm ("Saving..." while in flight) / Cancel).

**Navigation**: Success → `router.push('/calendar/day/{startDate}')` + refresh. Cancel → back.
Guard screens (not found / not owner) show `Link` "Back to Calendar".

**Loading state**: Confirm button → "Saving...".

**Error state**: Toast "Failed to update event". "Event not found." (deleted/missing). "You do not
have permission to edit this event." (non-owner, non-admin).

**Empty state**: N/A.

---

## Route: `/timesheet`
`src/app/(dashboard)/timesheet/page.tsx` — server component (redirects admin/accountant-view users
to `/dashboard`) + client `WeeklyTimesheet`.

**API calls**: `GET /api/timesheet?weekStart={ws}` — on mount and on week-nav change. `POST
/api/timesheet` `{weekStart, entries:[{date, hours}]}` (only submittable non-work days with
hours>0) — on "Submit for Approval" confirm.

**Data displayed**: Per-day entries: date, dayName, isWeekend, isPublicHoliday, phName,
isNonWorkDay, hours, otCredited (Off-In-Lieu earned), status (PENDING/APPROVED/REJECTED),
adminComment, isSubmittable. Week-range label. Hour options: Off (0h) / 4h (Half) / 8h (Full).
Status badges with adminComment shown under. OT preview: 8h→1 day, 4h→0.5 day, else 0.

**User actions**: Week nav (Previous/Next, disabled going past current; "Jump to current week"
when not on it); per non-work-day hour-selector (interactive only if submittable and not already
APPROVED); "Submit for Approval" (disabled if saving or all-zero) → confirmation modal (Go
Back/Submit).

**Navigation**: None (pure in-page state).

**Loading state**: `Loader2 spin + "Loading..."` while fetching the week. Submit button shows
spinner while saving.

**Error state**: Inline message next to Submit (`savedMsg`), red if it includes "Failed", green
(emerald) otherwise.

**Empty state**: "No weekends or public holidays this week." (`nonWorkDays.length === 0`).
"No hours to submit." if submitting with zero entries. Persistent footer note: "Log hours worked
on weekends and public holidays. Submit by 11:30 PM SGT. Off In Lieu days are credited after admin
approval."

## Route: `/timesheet/overview` (admin-only)
`src/app/(dashboard)/timesheet/overview/page.tsx` — server component (redirects non-admin to
`/timesheet`) + client `AdminTimesheetOverview`.

**API calls**: `GET /api/timesheet/admin-overview?weekStart={ws}` (mount + week nav); `GET
/api/timesheet/admin` (mount, and re-fetched after any approve/reject); `POST /api/timesheet/admin`
`{entryId, action:"APPROVE"|"REJECT", comment?}` on Approve/Reject.

**Data displayed**: Off-In-Lieu Approvals table: employeeName/Code, dayName+date, PH badge, hours,
otCredited, status + adminComment. Pending count badge. Weekly overview table: per-employee
per-day hours cell (color = status), totalHours, overall status badge. Summary cards: total
employees, approved, pending, not-submitted. Status filter chips (All/Pending/Approved/Not
Submitted) with counts.

**User actions**: Week nav (Next disabled at current week, "Current week" jump); status filter
chips; per pending entry "Review" → inline comment textarea + Approve/Reject/Cancel.

**Navigation**: None.

**Loading state**: `Loader2 + "Loading..."` for both the OIL section and the weekly table
independently.

**Error state**: Toast "Error" + server message or "Failed" on approve/reject failure. Success
toast "Approved"/"Rejected".

**Empty state**: "No weekend/public holiday submissions yet." (OIL table). "No employees match
this filter." (weekly table). Footer note: "Shows weekend and public holiday submissions only. Off
In Lieu days are credited after admin approval."

---

## Route: `/woods-square` (admin-only, admin view)
`src/app/(dashboard)/woods-square/page.tsx` → client `WoodsSquareInvite` (tabbed: Send Invites /
Requests / Activity Log / Overview / Settings), composing `WoodsSquareHealthBanner`,
`WoodsSquareOverview`, `WoodsSquareSettings` → `WoodsSquareManageList`.

**API calls**:
- `GET /api/habitap/generate-pin` — mount (Activity Log), on focus/visibility change.
- `GET /api/woods-square/access-requests` — mount, polled on focus/visibility, refreshed after
  decline/approve/send, manual Refresh button.
- `GET /api/woods-square/send-status` — mount then polled every 4s (send-in-progress pill).
- `GET /api/habitap/test-connection` — "Check connection" button (Send tab + Settings tab).
- `POST /api/habitap/generate-pin` `{staffIds, window:{fromDate, fromTime, toDate, toTime}}` — core
  send action, fired by: "Send N invite(s)" (Send tab), single-request "Invite" (confirm dialog),
  "Invite All (N)" bulk (confirm dialog, one call per distinct date-window group).
- `PATCH /api/woods-square/access-requests/{id}` `{status:"DECLINED"}` — "Decline" (Requests tab).
- `PATCH /api/woods-square/roster` `{updates:[{employeeId, woodsSquareInvite, woodsSquareEmail}]}` —
  debounced 1s auto-save (Settings tab roster toggles).
- `PATCH /api/woods-square/schedule` `{enabled, testMode, testRecipientIds, testFireAt}` — debounced
  1s auto-save; also called synchronously before "Run now".
- `POST /api/woods-square/schedule/run-now` — "Run now" button.

**Data displayed**: Send tab — staff picker (name/email/avatar, "No email on file" warning),
selected-staff tray, date-window pickers + presets, `windowDaysInclusive` validation (max 7 days),
send result (invitedCount/skippedCount+dates/failedCount+errors), send-status pill. Requests tab —
pending `AccessRequest` list (employee.name, role badges, age-colored "Requested on" pill,
window/"No specific dates", note); collapsible past requests (Fulfilled/Declined/Expired).
Activity Log — `HabitapInviteLog` rows (createdAt, name/email linked to `/woods-square/{id}`, role
badges, window, status pill), paginated 20/page, filter chips w/ counts. Overview tab — summary
(no own API calls). Settings tab — roster toggle + delivery-email override per employee; scheduler
enabled/testMode toggles, test recipients, fire-at datetime, next-run date, "Run now".

**User actions**: Tab switches; Send: select-all/clear/search/date-presets/"Send N invite(s)"/
"Check connection"; Requests: Refresh/"Invite All (N)" (confirm)/per-request Invite (confirm) or
Decline (confirm); Activity Log: Refresh/status filter/pagination; Manage list: per-employee
toggle, email input, Add all/Clear all, Paste emails (bulk match+tick); Scheduler: toggles, test
recipients, fire-at picker, "Run now" (native confirm), Check connection.

**Navigation**: `Link` → `/woods-square/{staffId}` (per staff row and per Activity Log row).

**Loading state**: Send button → `Loader2 + "Sending invites…"` + progress bar + caption ("Logging
in to Woods Square and sending — this can take a moment." or bulk "Sending window {n} of {total}
…"). Send-status pill: pulsing amber dot + "Send in progress" if another send is running. Check
connection → `Loader2 + "Checking…"`.

**Error state**: Toast "Failed to send invites"/"Couldn't send request" + description. Result panel
lists failed sends per-person. Toast "Connection failed". Toast "Couldn't decline request" (dialog
stays open).

**Empty state**: "No pending requests." (Requests). "No staff found" (search). Selected tray: "No
staff selected yet — pick people from the list." Activity Log: "No invites sent yet." / filtered
"No {failed/skipped/sent} invites."

## Route: `/woods-square/[id]` (admin-only)
`src/app/(dashboard)/woods-square/[id]/page.tsx` → client `WoodsSquareStaffActions`,
`InviteStatusPill`.

**API calls**: `POST /api/habitap/generate-pin` — "Send invite" dialog (`{staffIds:[id],
window:{...}}`, no resend flag) or "Resend PIN" dialog (`resend:true` + clamped `resendWindow` if
the pass already started). No GET fetches — `employee`/`invites` come from Prisma server-side.

**Data displayed**: Hero — name, initials avatar, state pill (Active now/Upcoming/Expired/"No
pass"), email · employeeId. Stat cards: invites sent, failed (red if >0), current pass
(window + state pill + note e.g. "3 days left"/"Starts in 2 days"/"Ended {date}"). Invite history
table: createdAt, access window + state pill, eventId or "—", status pill (+ error as tooltip).

**User actions**: "Resend PIN" (only if a current pass with an unfinished window exists) → dialog
(computed window, Cancel/Resend); "Send invite" (only if usable email + on roster) → dialog
(date-range picker + presets, Cancel/Send).

**Navigation**: `Link` → `/woods-square` ("Back to Woods Square Invite", and from empty-history
message).

**Loading state**: Send/Resend buttons → `Loader2 + "Sending…"`/`"Resending…"`.

**Error state**: Toast "Couldn't send invite"/"Couldn't resend PIN" + server error, or fallback
"They already have an invite covering these dates." / "No invite went out — try again."

**Empty state**: "No invites yet" + "This staff member hasn't been sent a Woods Square invite. Send
one from the invite page." (linked). "No pass" in hero + stat card.

## Route: `/woods-square-access` (staff self-service)
`src/app/(dashboard)/woods-square-access/page.tsx` (redirects admins in admin-view to
`/woods-square`) → client `WoodsSquareAccessCard` → `RequestAccessButton`.

**API calls**: `DELETE /api/woods-square/access-requests/{id}` — "Cancel request" confirm, then
`router.refresh()`. `POST /api/woods-square/access-requests` `{fromDate, toDate, note}` — "Send
request" in the request dialog. No GETs — `myInvites`/`myRequests` (max 10) are server props.

**Data displayed**: Pass card — holder name, employeeId, status word (Activated/Scheduled/Expired/
"Not activated"), validity window, progress bar, countdown text. "Other passes" tab — other valid
invites with countdown/status. "My requests" tab — fromDate–toDate or "No specific dates", note,
status badge (Pending/Fulfilled/Declined/Expired — PENDING past its toDate displays as Expired),
"Requested {date}". PIN reminder + entry-method caption.

**User actions**: "Request access" (only if `onRoster`) → dialog: date-range + presets, note (max
300 chars), Cancel/Send request (disabled if pending count ≥ max, shows "Max {N} pending requests
reached"). Tabs: Other passes / My requests. Per pending non-expired request: "Cancel" → confirm
dialog (Keep request/Cancel request). Info icon → static "Mobile Access Card Info" modal.

**Navigation**: None (`router.refresh()` only, after cancel).

**Loading state**: Cancel button → "Cancelling…"; Send button → `Loader2 + "Sending…"`.

**Error state**: Cancel failure has **no toast/visible error** — fire-and-forget fetch with no
catch, only the button re-enabling. Request dialog: toast "Couldn't send request" + server error;
client-side validation toasts "End date must be on or after the start date", "Window too long —
Woods Square allows up to {N} days."

**Empty state**: No employee profile → contact-HR text. No pass → "No active pass. Tap Request
access above — your entry PIN is emailed to you once it's granted." No other passes → "No other
passes." No requests → "No requests yet." Fully empty → "No passes or requests yet. Use Request
access above to get started."

---

## Settings (ADMIN + admin-view only — guarded by `settings/layout.tsx`, redirects otherwise to
`/dashboard`; excluded from staff/intern native clients)

### `/settings` (redirect)
Server-side `redirect("/settings/company")`. No API/data/actions.

### `/settings/company`
`CompanySettingsForm`. **API**: `POST /api/upload` (logo, edit mode); `PATCH
/api/settings/company` `{name, shortName, uen, address, phone, email, website, logo,
approvalEmails}` on Save; `POST /api/leave/rollover` `{fromYear}` exists in code but its UI section
is commented out (unreachable). **Data**: Company Info card (logo, name, shortName, UEN, phone,
email, website, address — "—" if empty); Approval Settings (approvalEmails badges). **Actions**:
Edit/Cancel toggle; logo upload/remove; field inputs (Name required, Email/Website format-
validated via zod); approval-email add (regex + dedupe check)/remove; Save/Cancel. **Navigation**:
none; `router.refresh()` after save. **Loading**: Save → "Saving..."; logo upload → spinner.
**Error**: inline zod field errors; toast "Failed to update company settings"/"Logo upload
failed". **Empty**: "No approval emails configured."; unset fields show "—".

### `/settings/credentials`
Four cards: `GmailCredentialsCard`, `QuickBooksCredentialsCard`, `ClaudeCredentialsCard`,
`WoodsSquareCredentialsCard`. **API**: each does `PATCH /api/settings/credentials` with its own
key set on Save (Gmail: `GMAIL_EMAIL_USER/CLIENT_ID/CLIENT_SECRET/REFRESH_TOKEN`; QuickBooks:
`QUICKBOOKS_CLIENT_ID/CLIENT_SECRET/REFRESH_TOKEN/REALM_ID/REDIRECT_URI/PROXY_URL`; Claude:
`CLAUDE_API_KEY`; Woods Square: `HABITAP_USERNAME/PASSWORD`). Gmail also: `POST
/api/settings/test-gmail` `{to}` on "Test". QuickBooks also: `GET /api/quickbooks/oauth/connect`
opened via `window.open()` popup on "Connect"/"Reconnect". **Data**: masked secret values (first/
last chars + `•`), "Not set" when empty, "Configured"/"Connected" badges. **Actions**: Edit/Cancel
per card; Eye/EyeOff reveal toggles per secret field; Save; Gmail Test; QuickBooks Connect/
Reconnect. **Navigation**: none in-app; QuickBooks Connect opens an external popup.
**Loading**: Save → "Saving..."; Gmail Test → spinner, disabled while testing. **Error**: toast
"Email, Client ID and Client Secret are required" (validation) / "Failed to save credentials";
Gmail test failure toast titled with the failing step + hint. **Empty**: "Not set" per unset
field; QuickBooks Proxy URL → "Not set (direct QB API mode)"; Redirect URI → "Auto (derived from
app URL)".

### `/settings/cron-jobs`
`CronJobsPanel`. **API**: `PATCH /api/settings/auto-deactivate-interns` `{enabled}` (toggle,
optimistic + revert-on-error); `POST /api/admin/test-email` ("Send Test Email"); `POST
/api/cron/run-now` `{path, method}` (per-job "Run Now"). 3 hardcoded jobs: Monthly Payroll
Generation (`GET /api/cron/payroll`, 28th @ 09:00 SGT), Timesheet Reminder (`GET
/api/cron/timesheet-reminder`, daily 17:30 SGT), Deactivate Expired Interns (`GET
/api/cron/deactivate-expired-interns`, daily 00:30 SGT). **Data**: per job — name, status badge
(active/planned), method badge, description, schedule text, cron expression, full callable URL.
**Actions**: toggle switch; Send Test Email; per-job Run Now; per-job Copy URL (clipboard, shows
"Copied" 1.5s). **Navigation**: none. **Loading**: Run Now → "Running..."; Send Test Email →
"Sending...". **Error**: toast "Could not update setting" (+ revert)/"Email failed"/"Run failed".
**Empty**: N/A (static 3-job list).

### `/settings/email-templates` (index)
No client fetch — server Prisma `emailTemplate.findMany({key, updatedAt})`. **Data**: 8 template
rows (OTP, LEAVE_REQUEST, LEAVE_APPROVED, LEAVE_REJECTED, EXPENSE_REQUEST, EXPENSE_APPROVED,
EXPENSE_REJECTED, PASSWORD_RESET) each with a "Customised"(amber)/"Default"(gray) badge.
**Actions**: click a row → navigates to its editor. **Navigation**: `Link` →
`/settings/email-templates/{slug}` (slug = lowercased, underscores→hyphens). **Loading/Error/
Empty**: none (static list, server-rendered).

### `/settings/email-templates/[slug]`
`EmailTemplateEditor`. **API**: `PUT /api/settings/email-templates/{key}` `{subject, body}` on
Save; `DELETE /api/settings/email-templates/{key}` on "Reset to Default" (after `confirm()`).
**Data**: label/description header; Available Variables list (`{VAR}` chips); Subject input; Body
textarea; live Preview with dummy substitutions (e.g. `EMPLOYEE_NAME`→"Jane Doe Tan"); status pill
"Unsaved changes"/"Up to date". **Actions**: edit Subject/Body (live preview); Reset to Default
(disabled unless customized); Save (disabled unless changed). **Navigation**: none;
`router.refresh()` after save/reset. **Loading**: Save/Reset icons spin while in flight. **Error**:
toast "Save failed"/"Reset failed" + message. **Empty**: preview shows "(empty)" subject / "Body
is empty." if both blank.

### `/settings/leave-policy`
Fully client component. **API**: `GET /api/settings/leave-types` on mount; `PATCH
/api/settings/leave-types` `{id, defaultDays, internDefaultDays, paidDays, carryOver,
maxCarryOver}` per-row Save. **Data**: per leave-type card (AL/SL/MC/CL/NPL, `AL_OT` filtered out)
— code badge, Paid/Unpaid badge, editable Staff days/year, Intern days/year ("0 = same as staff"),
Paid days (staff, if paid, "0 = all days are paid"), Max carry-forward ("0 = unlimited", disabled
if carry-over off), Carry-forward toggle. Proration note banner explaining the `ceil(days/12 ×
months)` accrual formula and 1-year carry-forward expiry. **Actions**: numeric inputs per field;
Carry-forward toggle; per-card Save (spinner while saving). **Navigation**: none. **Loading**:
page-level `Loader2` while initial fetch in flight; per-row Save spinner. **Error**: toast "Save
failed" + message; a failed initial fetch silently stops the spinner with no visible error (empty
list results). **Empty**: none explicit if list is empty — no cards render, no message. Success
toast "Saved — {name} policy updated successfully."

### `/settings/payslip-template`
`PayslipTemplateEditor`. **API**: `PUT /api/settings/payslip-template` `{payslipRemarks, payslipTitle,
payslipHeaderNote}` on Save. **Data**: editable Title/Header Note/Footer-Remarks (with
`{PAYMENT_DATE}`/`{EMPLOYEE_NAME}`/`{NRIC}`/`{MONTH}` placeholders); live full mock-payslip
preview (company logo/name, header, title, employee-details table, salary-breakdown table,
remarks). Available-placeholders reference list. **Actions**: edit fields; "Use default template"
(client-side reset, no API call); Save. **Navigation**: none; `router.refresh()` after save.
**Loading**: Save → "Saving...". **Error**: toast "Save failed" + message. **Empty**: remarks
preview shows italic "Footer is empty." if body is blank.

### `/settings/webhooks`
`WebhooksPanel`. **API**: `PATCH /api/settings/webhooks/{id}` `{enabled}` (toggle); `DELETE
/api/settings/webhooks/{id}` (after `confirm()`); `POST /api/settings/webhooks` `{name,
description, httpMethod, authToken, enabled}` (create); `PUT /api/settings/webhooks/{id}` (update).
**Data**: Built-in Webhooks (4 static: Leave/Expense Approve/Decline, all `GET
/api/public/{leave|expense}-approval/respond?token=...&action=...`) — name, method badge, status
badge, "Built-in" badge, description, full URL. Custom Webhooks — name, method badge, Enabled/
Disabled badge, "Bearer auth" badge (if token set), description, full URL. **Actions**: "Create
Webhook"; per custom webhook Edit/Enable-Disable/Delete (confirm); Copy URL (every row, toast "URL
copied"); form: Name/Description/Auth Token inputs, HTTP Method toggle, Enabled toggle, Save/
Cancel/"Back to webhooks". **Navigation**: none (internal view-state list/create/edit switching);
`router.refresh()` after create/update/toggle/delete. **Loading**: form Save spinner; custom-card
buttons disabled while busy. **Error**: toast "Action failed"/"Delete failed"/"Save failed" +
message; inline validation toast "Name is required". **Empty**: "No custom webhooks yet. Click
Create Webhook to add one." (dashed-border box).

---

## Accounting (ADMIN/ACCOUNTANT only; excluded from staff/intern native clients)

### `/accounting`
`AccountingClient`. **API**: `POST /api/accounting/parse-statement` (multipart, file picked);
`POST /api/accounting/transactions` `{transactions:[...]}` (auto-fires right after a successful
parse). Server-side (page): `bankTransaction.aggregate` (DEBIT + CREDIT) for summary. **Data**:
access-denied fallback "You do not have access to this section." if not authorized. Expense
Tracking card (total debit + count); Income Tracking card (total credit + count); "Bank Statements"
external Drive-folder link card. Upload summary text after import (imported filename, saved/
skipped counts, credit/debit split, settled/verify/drive notes). **Actions**: "Upload Statement"
(hidden file input, `.xlsx/.xls`); click Expense/Income Tracking cards; click Bank Statements
(opens Drive in new tab). **Navigation**: `Link` → `/accounting/expense-tracking`,
`/accounting/income-tracking`; external `<a target="_blank">` to a Google Drive folder;
`router.replace(pathname, {scroll:false})` then `router.refresh()` after successful upload.
**Loading**: Upload button → `Loader2 + "Importing…"`. **Error**: inline red text from thrown
parse/save errors ("Failed to parse statement"/"Failed to save"). **Empty**: "No transactions
found in this statement." if a parsed file yields zero rows.

### `/accounting/expense-tracking`
`AccountingClient` (upload) + `ExpenseFilters` + `TransactionsTable` + `Pagination`. **API**:
server Prisma count/findMany (`direction:"DEBIT"`, filtered by status/category/date-range/search,
page size 200, default `status=Pending`). Client (via shared `TransactionsTable`): `POST
/api/accounting/transactions/generate-expense` `{id}` (per-row, only here since
`showGenerateExpense`); `PATCH /api/accounting/transactions/{id}` (bulk "Mark Manually Settled" →
`{status:"Settled"}`, and every inline cell edit — title/paymentDate/amount/gstIncluded/
paymentType/type/paymentRef/invoiceNo/status/remarks); `DELETE /api/accounting/transactions/{id}`.
Plus the upload flow from `/accounting` is present here too. **Data**: header, filter bar (Status/
Category/From-To/Search), table columns (Title/Payment Date/Amount/GST/Payment Type/Category/Bank
Ref/Invoice No/Status/QB Review/Remark/Delete), toolbar "{N} records" + running total, pagination
"Showing X–Y of Z". **Actions**: search (commits on Enter/blur), Status/Category selects, From/To
dates, Reset (shown when any filter non-default); inline cell edits; row/select-all-pending
checkboxes → bulk Generate QB Expense(s)/Mark Manually Settled (confirm); per-row Confirm
Settlement (status="QB Created"); per-row Delete (confirm); pagination Previous/Next/numbered.
**Navigation**: filter/pagination changes via `router.replace(?{qs}, {scroll:false})` (no `Link`s).
**Loading**: `useTransition()` pending state on filter controls; per-cell spinner while saving;
bulk buttons show "Creating/Settling {n} of {total}…". **Error**: per-cell inline error text;
per-row `generateErrors` under Status cell; delete failure via `alert()`; bulk failure toast
"Generation Failed — All {n} failed — check the error messages on each row." **Empty**: "No
expenses match the current filters."

### `/accounting/income-tracking`
Mirrors expense-tracking with `direction:"CREDIT"`. Auto-backfills missing `invoiceNo` via
TC-number regex extraction from title on every load (server-side, not user-triggered). **API
differences**: `POST /api/accounting/transactions/receive-payment` `{id}` (per-row "Receive
Payment", replaces generate-expense) → sets `status:"Settled"`, `qbExpenseNo`; no bulk "Mark
Manually Settled" (that's gated on `showGenerateExpense`). **Data differences**: `titleLabel`
="Customer Name", no GST column, Receipt No column shown, no Category column, filter
`categoryLabel`="Payment Type" (options: All/Bank Transfer/PayNow/GIRO/CC/Cash/e-invoice).
**Actions**: same filter/edit/delete interactions, plus bulk "Receive Payment(s)" (sequential
per-row). **Loading**: bulk → "Processing {n} of {total}…"; per-row status cell → "Processing…" +
spinner. **Error**: `receiveErrors` inline; bulk toast "Processing Failed"/"Payments Received (with
errors)". **Empty**: "No income matches the current filters."

*(Shared `TransactionsTable` persists column widths to `localStorage` per direction
(`acct-cols-v3-{direction}`), supports draggable column resize — client-only, no API.)*

---

## MyInfo (Singpass callback — own root layout, `(myinfo)`)

### `/myinfo/authorize`
Client component, `<Suspense>`-wrapped (needs `useSearchParams`). **API**: none via fetch — the
"Connect via Singpass" control is a plain `<a href="/api/myinfo/auth?token=...">` anchor (full nav
triggers the server-side Singpass redirect). **Data**: Singpass-branded header; "Complete Your
Employee Profile" heading; "Data that will be retrieved" list (Full name, DOB, Gender, Nationality,
Mobile, Address, Education level, NRIC); PDPA notice. **Actions**: "Connect via Singpass" button
(anchor navigation). **Navigation**: full-page anchor to `/api/myinfo/auth?token={encoded}`.
**Loading**: `<Suspense fallback="Loading…">`. **Error**: "Invalid link. Please ask HR to resend
the invite." if no `token` param. **Empty**: N/A.

### `/myinfo/success`
Static server component. **Data**: green checkmark, "Profile Updated", "Your personal information
has been successfully retrieved from Singpass and saved to your employee profile.", "You can close
this window." No API/actions/navigation/states.

### `/myinfo/error`
Client component, `<Suspense>`-wrapped. **Data**: red X-circle, "Something went wrong", reason-
specific message via `?reason=` (`expired_link`, `missing_token`, `missing_params`,
`access_denied`, fallback `An error occurred: {reason}. Please contact HR.`, default
`unknown_error`). This page *is* the error state itself — no separate error UI. No API/actions/
navigation/empty-state.

---

## Standalone

### `/privacy-policy`
Fully static server component, no dashboard chrome, no auth required. 13 numbered legal sections
(Introduction; Information We Collect — Personal/Financial/Usage/Authentication Data; How We Use
Information; Data Sharing; Storage & Security (bcrypt, TLS/SSL); Retention; Your Rights (PDPA);
Cookies; Third-Party Services (Google OAuth, UploadThing, AI Services); Account Management &
Deletion (`info@tertiaryinfo.tech`, 30-day SLA); Children's Privacy; Changes to Policy; Contact
Us). "Last updated: 11 March 2026", dynamic copyright year. No API/actions/navigation/states.

---

## Cross-cutting notes worth carrying into native-client work

- **Hard nav vs. SPA nav inconsistency**: login success and the Employees table row-click both use
  `window.location.href` (full reload) instead of `router.push`/`Link`, unlike the rest of the app.
- **`RecentActivity` unused prop**: dashboard fetches `leaves` and passes it down, but the
  component only renders `expenses` — leave activity never appears in Recent Activity on the web.
- **`AdminOtLogPanel` has no error toast**: failed GET/POST to `/api/ot-log` fail silently.
- **Payroll PDF download failures are silent**: only `console.error`'d, no toast/banner.
- **Woods Square access-request cancel has no error surface**: fire-and-forget fetch, no catch.
- **Settings leave-policy fetch failure is silent**: stops the spinner but shows no error and no
  empty-state text if the list ends up blank.
- **Shared components across routes**: `EmployeeDetailEditable` is used identically on `/profile`
  (`canEdit` always true) and `/employees/[id]` (`canEdit` = admin only); `AddEmployeeForm` is
  shared between `/employees/new` and `/employees/new-intern` via an `intent` prop; `LeaveList` is
  shared between `/leave/annual` and `/leave/medical`; `TransactionsTable` is shared between
  `/accounting/expense-tracking` and `/accounting/income-tracking`.
- **Toasts are the dominant error-surfacing pattern app-wide** (`useToast()` / shadcn
  `<Toaster/>`, `variant:"destructive"` for errors) — a handful of routes fall back to native
  `alert()`/`confirm()`/`prompt()` instead (Payroll gross-pay edit, several delete confirmations,
  Woods Square "Run now"), which native clients should replace with proper platform dialogs rather
  than porting literally.
- **Query-param-driven state** (Accounting filters/pagination) uses `router.replace` +
  `useTransition()`, not `router.push` — doesn't add browser history entries; native clients have
  no direct equivalent concern but should treat these as non-modal, in-place filter updates.

---

**Relevant to Android/iOS clients** (staff/intern only, per `CLAUDE.md`): Login, Dashboard,
Profile, Leave (annual/medical + request/edit), Expenses (+ submit/edit), Payroll, Calendar,
Timesheet, Woods Square (self-service `/woods-square-access` only). Everything under Employees,
Settings, Accounting, and admin-only Woods Square/Timesheet-overview screens is web-only.
