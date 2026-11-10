# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

**TIA HRMS (formerly Tertiary HRMS) — native Android app** (Kotlin + Jetpack Compose, Material 3, MVVM). The Google
Play build of Tertiary Infotech Academy's HR Management System, **rebuilt fully native — no
WebView, no cross-platform runtime**. It is the Android sibling of the native iOS app
(`../iOS/hrmsapp`) and **mirrors its features, UI/UX, and API layer 1:1** — the iOS app is the
source of truth for both *functionality* and *visual design*. Min SDK **24 (Android 7)**, target
SDK **36**, phone-first.

**Theme — one light TIA HRMS premium-blue theme (Alfred, 10 Nov 2026; BRAND.md,
`~/projects/tertiary/tia-hrms-brand/`).** Same as the web app (tertiary-hrms 464200b): the top and
tab bars are navy-950 (`Brand.Bar` / `OnBar` / `BarAccent` = royal-300), content sits on a
near-white → mist-blue backdrop (`GradientScreen`), cards are white with a light-blue hairline, text
is navy ink, royal-600 `#2F5BE6` is the accent (`Brand.Accent` for text/icons, `Brand.Primary` as a
fill), and gold-400 `#F5B83D` is the single sparing warm accent (unread badge, accreditation
badges; always a fill with navy text). **There is no dark theme, no dynamic colour and no theme
switch** — don't bring them back. All tokens are plain `val`s in `ui/theme/Theme.kt` (`Brand.*`) +
`Type.kt`. The iOS app is still the source of truth for structure; its colours should follow this
light theme. The launcher icon is the iOS AppIcon (`tia-hrms-brand/icon-1024.png`); the adaptive
foreground is rendered glyph-only from `make_app_icon.swift`. Never bring back the old
purple/indigo "HR" icon.

**Neat, interactive lists (Nov 2026).** Admin lists of leave, MC, time off and claims use
`ui/components/RecordTable.kt`: one line per record in a fixed column order (name, status, type,
dates, days/hours, applied, reason), the table scrolls sideways as one block, and tapping a line (or
its truncated reason) opens `RecordDetailSheet` with the full reason and the actions. Long pages
fold with `CollapsibleSection`; filters are `FilterTabs` with counts; headline numbers use
`KpiStrip`.

## Relationship to the web app (hrms.tertiaryinfotech.com) and Coolify

Identical to the iOS app: this native app is a **client of the existing HRMS web backend** — it has
no database of its own.

- The web app is a **Next.js 14** application on **Coolify** at `https://hrms.tertiaryinfotech.com`,
  backed by **PostgreSQL**. The app pulls all data from that deployment over HTTPS; it never talks
  to PostgreSQL directly.
- **Authentication** reuses the web app's **NextAuth (Auth.js)** session. `data/AuthService.kt`
  performs the standard NextAuth flow: `GET /api/auth/csrf` → `POST /api/auth/callback/{credentials
  |otp}` → `GET /api/auth/session`. The session cookie is stored in a `PersistentCookieJar`
  (SharedPreferences-backed, shared by one OkHttp client) and reused for every call, exactly as a
  browser would. **Email + password**, **email one-time-code (OTP)**, and **Google** sign-in are
  all supported.
- **Google sign-in** (`data/GoogleSignInService.kt`) uses **Chrome Custom Tabs** with an OAuth 2.0
  **authorization-code + PKCE** flow — no Play Services SDK, so the dependency set stays tiny. It
  exchanges the code for a Google `id_token` and POSTs it to the pre-existing
  **`POST /api/auth/google-mobile`**, which verifies the token with Google, checks the audience
  against `GOOGLE_CLIENT_ID` / `GOOGLE_IOS_CLIENT_ID` / `GOOGLE_ANDROID_CLIENT_ID`, blocks INACTIVE
  employees, and sets the same NextAuth session cookie. Setup:
  1. **Google Cloud Console** → *Credentials → Create credentials → OAuth client ID → **Android***,
     package `com.tertiaryinfotech.hrportal`, SHA-1 = the **Play App Signing** certificate's
     fingerprint (Play Console → Test and release → App integrity), **not** the upload key's.
  2. Put that id in the gitignored `local.properties` as `GOOGLE_ANDROID_CLIENT_ID=...`. It feeds
     `BuildConfig.GOOGLE_CLIENT_ID` and the redirect-scheme manifest placeholder. Left unset, the
     app **hides the Google button** entirely (same behaviour as the iOS build without `GIDClientID`).
  3. On the **web app**, paste the same id into *Settings → Credentials → **Mobile Sign-In
     (Google)*** so the backend accepts tokens minted for the Android client.
- **Data endpoints**: the same additive, read-only `/api/mobile/*` namespace feeds this app
  (`summary`, `profile`, `leave`, `employees`, `expenses`, `payslips`, `calendar`,
  `team-calendar`, `trainers`), plus the
  pre-existing `/api/timesheet`, `/api/leave` POST, and `/api/payroll/payslip/{id}/pdf`.
- Changing the deployed URL means updating `Net.BASE_URL` in `data/Net.kt`.

## Features

Mirrors the iOS app 1:1:

- **Login** — email + **password** and email **OTP** sign-in (role-based via the web NextAuth
  session). **Persistent login**: the session cookie lives in `PersistentCookieJar`
  (SharedPreferences-backed) and `AuthViewModel.bootstrap()` restores it on launch, so a signed-in
  user stays signed in across app restarts and never has to re-log-in until they sign out.
- **Dashboard** — welcome header, KPI balance tiles (AL / MC / Expenses YTD / Off-in-Lieu), Quick
  Actions, Recent Activity, and (admins only) the **Approvals queue** cards.
- **Leave** — balances + full request history + **apply for leave** with a live working-days
  preview. **Medical leave (MC)** is applied here as a leave type, attaching an **MC photo**
  (native camera / gallery via `ActivityResultContracts` → `/api/upload` → attached to the request).
  **Leave rules (server-enforced, Nov 2026):** `POST /api/leave` answers 400 with a `code` —
  `AL_EXCEEDS_BALANCE` (dialog offers *Apply as No Pay Leave instead*, resubmitting the same dates
  and day type with the NPL type id from the loaded types), `INTERN_MC_NOT_PAID` (interns, roles
  exactly `["INTERN"]`, have no paid MC; the form already switches MC to No Pay Leave up front with
  reason "Sick leave (MC)" and keeps the certificate) and `NPL_CAP_EXCEEDED`. Parsing lives in
  `data/LeaveErrors.kt` (`LeaveRejectedException`). The NPL balance row carries `proRated` = cap and
  `available` = remaining.
- **Trainers (contacts)** — More → **Trainers**, every signed-in user (11 Nov 2026): read-only
  `GET /api/mobile/trainers/contacts` (name, email, phone, certs, skills, courses; demo accounts get
  an empty list), searchable on the device; detail `trainer_contact/{id}` has Call / WhatsApp / Email.
- **Trainer Management** (Trainer Admins only — `summary.canManageTrainers`; server 403 otherwise) —
  More → Manage → Trainer Management (route `trainers`; no longer a bottom tab). The directory loads once (`/api/mobile/trainers?status=all`, no NRIC) and is filtered on the
  device by `data/Trainers.kt` `TrainerSearch` (a 1:1 port of the web's `src/lib/trainers.ts`,
  unit-tested): a search box with skill / certification suggestions and counts that become
  removable chips, an Accreditation menu (`Accreditation` enum: ACLP / ACTA / DACE / DDDLP /
  Finance / IBF Fit), Trainers / Applicants / All, compact rows with the matching expertise first,
  and a contact sheet (call, WhatsApp `https://wa.me/65XXXXXXXX` for 8-digit SG numbers, email,
  copy). The profile (`trainer_profile/{id}`) has Call / WhatsApp / Email buttons, copy icons, NRIC
  masked with Show, and long sections (experience, courses, classes) that fold. Names are uppercase
  by design.
- **Approvals** (admins only — role ∈ {ADMIN, HR, MANAGER}) — Leave / MC / Claims tabs of
  one-line records; a tap opens the full reason with approve / reject (`GET /api/mobile/approvals`;
  `POST /api/{leave|expenses}/{id}/{approve|reject}`). **Enforced server-side** (403 for staff/interns); the client only shows the UI when
  `summary.isAdmin`. The two account admins (**Tan Soik Ching**, **Alfred Ang Chew Hoe**) hold role
  ADMIN and can action this queue.
- **Team** — company directory (richer contact fields for supervisory roles). Approvers tap a person
  for `EmployeeRecordsScreen` (`/api/mobile/employees/{id}/records`, approver-or-self): clock-in,
  MC and leave history; the three stat tiles open `team_records_clock/{id}` and
  `team_records_leave/{id}?kind=MC|LEAVE`. Routes start with `team` so the Team tab chrome stays.
- **Payslips** — list + native **PdfRenderer** viewer for the authenticated payslip PDF.
- **Expenses** — personal expense/medical claims with status, plus **submit an Expense or Medical
  claim** with a receipt (`POST /api/mobile/claims`, which files it in the employee's Drive folder).
- **Clock In / Out** (route `timesheet`) — a simple **clock in / out** with a live elapsed timer and
  a monthly daily check-in/out history with total hours (`AttendanceHistorySection`, ported from
  iOS; punches via `/api/mobile/attendance` + `attendance/clock-{in,out}`, history via
  `/api/mobile/attendance/history?month=`).
- **Timesheet — only interns have timesheets** (web 464200b). More shows **Timesheet** to interns
  and **Off In Lieu** to everyone else; both open the weekly weekend / public-holiday grid
  (`WeeklyTimesheetScreen`, `timesheet_week`). Approvers also get the admin **Timesheet**
  (`AdminTimesheetScreen`, route `timesheet_admin`; `intern_attendance` is kept as an alias): a
  collapsible **Needs approval** section with pending missing-punch requests
  (`GET /api/attendance/corrections?scope=all&status=PENDING`, `POST /api/attendance/corrections/{id}`
  `{action: approve|reject, comment}`) and pending Off In Lieu entries (`GET/POST /api/timesheet/admin`
  `{entryId, action: APPROVE|REJECT, comment}`), then each intern's days worked and hours for the
  month (`/api/mobile/attendance/summary?month=`, Interns / Everyone tabs) drilling into
  `intern_attendance_detail/{id}`. The separate Intern Attendance menu entry is gone.
- **Time Off** — hourly time off. `GET /api/time-off` returns everyone's requests to approvers, so
  they get Pending / All tabs of one-line records and approve / reject in the sheet
  (`POST /api/time-off/{id}/approve` `{approvalComment}` / `reject` `{reason}`); requesters can
  cancel their own pending request.
- **Leave & MC Summary** (approvers only; More menu) — `LeaveSummaryScreen` over
  `/api/mobile/leave-summary?month=`, drilling into `mc_leave_summary_detail/{employeeId}`. The route
  is prefixed `mc_` on purpose: tab/title resolution is `startsWith`-based, so `leave_summary` would
  be claimed by the Leave tab.
- **Calendar** — the **team month grid** of everyone's approved leave (`/api/mobile/team-calendar`)
  with an Everyone / Only me filter and a per-day detail sheet, mirroring the iOS `TeamCalendarView`.
  A colleague's leave *type* is masked server-side unless the viewer is that person or an approver.
  The older agenda-style list (public holidays + the user's own events) remains at the
  `calendar_list` route, reachable from **More**.
- **Notifications** — top-bar **bell with unread badge** + list (`/api/notifications`), every type
  addressed to the user except UI toasts (`isVisibleNotification`).
- **Push alerts (FCM, v1.12)** — Firebase project `tia-hrms`, `app/google-services.json` (git-ignored, the
  repo is public; fetch it with `firebase apps:sdkconfig ANDROID 1:1066509799584:android:7db099d0fe076f34cd79c5
  --project tia-hrms --out app/google-services.json` before building).
  `push/Push.kt`: channel `hrms_alerts`, `HrmsMessagingService`, token → `POST /api/mobile/push-token`
  after sign-in / restored session, `DELETE` before sign-out; Android 13+ permission asked once in
  `MainScaffold`; a tapped alert's `link` routes via `push/LinkRoutes.kt` `routeForLink` (admins:
  approval requests → Approvals). The server sends with the `tia-hrms` adminsdk service account
  (kept server-side, never in this repo).
- **Certificate chips** — `CertLabel.short()` shows ACLP / ACTA / DACE / DDDLP (DDDPL / ALCP typos
  fixed) else ≤14 chars; chip rows stay one line. The profile's Accreditation lists only what is
  held (plus an "IBF" chip for a Fit & Proper declaration) — never missing items.
- **Profile** — full employee record + self-service edit + change password.

**Excluded / removed** (same product scope as iOS): **Accounting** and heavy **admin-authoring**
flows (web-only). **Woods Square building access** is **not** part of this app (removed — it isn't in
the iOS app either).

**Google Sign-In** is implemented (see above) and appears only when the build carries a
`GOOGLE_ANDROID_CLIENT_ID` in `local.properties`; password + OTP are always available. Leave it unset
until the OAuth client has **"Enable custom URI scheme"** switched on under its Advanced Settings in
Google Cloud Console: as of 2026-11-06 Google rejects this flow with `invalid_request`.

## Build & run

No Android Studio GUI required — the project builds from the CLI. Gradle 8.14.3 (wrapper) + the JDK
bundled with Android Studio.

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"

# Debug build + install on a running emulator/device
./gradlew :app:assembleDebug
$ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk

# Signed release artefacts (require keystore.properties + keystore/ — gitignored)
./gradlew :app:bundleRelease      # → app/build/outputs/bundle/release/app-release.aab  (Google Play)
./gradlew :app:assembleRelease    # → app/build/outputs/apk/release/app-release.apk     (sideload)

# JVM unit tests (payload parsing, coded leave errors, trainer formatting)
./gradlew :app:testDebugUnitTest
```

`compileSdk`/`targetSdk` 36 requires AGP 8.9+. Only system frameworks + a tiny dependency set
(Compose, OkHttp, kotlinx.serialization, Navigation) — no heavyweight third-party libraries.

## Architecture

Single-activity (`MainActivity`) Compose app. `AuthViewModel` (`AndroidViewModel`) is the single
source of truth, driving `data/AuthService` (NextAuth sign-in) and `data/HrmsApi` (typed reads +
apply-leave + PDF download), both riding one shared OkHttp client + `PersistentCookieJar`.
`RootScreen` routes loading → `LoginScreen` → `MainScaffold` via a `NavHost`. The bottom nav is
**five tabs — Home / Leave / Calendar / Team / More** for everyone — matching the iOS `MainTabView`
1:1 (Trainers moved into More in v1.12), shown on every screen. The remaining modules (Payslips, Expenses, Profile, Timesheet, Time Off, Approvals) are
nested destinations reached from More or Home, with a back chevron. All destinations render **bare**:
`MainScaffold` supplies the top bar and tab bar, so wrapping one in `BrandScaffold` would stack a
second app bar.
Reusable brand controls live in `ui/components/`.

## Signing & Google Play

- App id **`com.tertiaryinfotech.hrportal`** (matches the iOS bundle id). Play app signing is
  recommended; the local **upload key** lives in `keystore/upload-keystore.jks` with credentials in
  `keystore.properties` — **both gitignored. Back them up securely; the password is not recoverable.**
- Release signing is wired in `app/build.gradle.kts` and only activates when `keystore.properties`
  is present, so CI/other machines build unsigned debug fine.
- Play listing assets + a step-by-step submission guide live in `store/`.

## Conventions

Follow the bundled `mobile-android-design` skill: Material Design 3, Compose idioms, large
typography, >=48dp touch targets. **The iOS app is the source of truth for both behaviour and visual
design**: when a feature or screen changes on one platform, mirror it on the other. Use `Brand.Accent`
for text, icons and selected states, and `Brand.Primary` only as a fill, because premier-blue text is
unreadable on the gradient.

Deliberate Android-only extras: the expense filter panel,
receipt viewing, "Payment Received", PDF receipts, the annual-leave advance / Off In Lieu panel, and
Add Event on the calendar list.

## Previewing without a backend

Debug builds contain `MockApi` (`app/src/debug`), the counterpart of iOS `-uiPreview`. Release builds
get the no-op in `app/src/release`.

```bash
adb shell "run-as com.tertiaryinfotech.hrportal sh -c 'mkdir -p files; echo admin > files/ui_preview'"
adb shell am force-stop com.tertiaryinfotech.hrportal
```

Use admin, staff, intern or signedout. Writes are logged under the `MockApi` logcat tag, not sent.
Delete the file to go back to the real backend.
