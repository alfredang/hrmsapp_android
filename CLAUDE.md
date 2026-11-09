# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

**TIA HRMS (formerly Tertiary HRMS) — native Android app** (Kotlin + Jetpack Compose, Material 3, MVVM). The Google
Play build of Tertiary Infotech Academy's HR Management System, **rebuilt fully native — no
WebView, no cross-platform runtime**. It is the Android sibling of the native iOS app
(`../iOS/hrmsapp`) and **mirrors its features, UI/UX, and API layer 1:1** — the iOS app is the
source of truth for both *functionality* and *visual design*. Min SDK **24 (Android 7)**, target
SDK **36**, phone-first.

**Theme — TIA HRMS premium blue (BRAND.md, `~/projects/tertiary/tia-hrms-brand/`) on the iOS
layout.** The iOS app is the source of truth for structure and components; colours follow BRAND.md.
The signature backdrop is the navy-950 → navy-800 → royal-600 diagonal gradient (`#0D1638` →
`#1A2F86` → `#2F5BE6`); cards are translucent frosted panels on top of it, text is white with mist
`#C9D8FF` secondary text, royal-300 `#8FB0FF` is the accent on dark and royal-600 on light, the top
and tab bars are navy in **both** themes (`Brand.Bar` / `OnBar` / `BarAccent`), and gold-400
`#F5B83D` is the single sparing warm accent (unread badge, accreditation badges; always a fill with
navy text). All tokens live in `ui/theme/Theme.kt` (`Brand.*`) + `Type.kt`. The launcher icon is the
iOS AppIcon (`tia-hrms-brand/icon-1024.png`); the adaptive foreground is rendered glyph-only from
`make_app_icon.swift`. Never bring back the old purple/indigo "HR" icon. **Unlike iOS (dark-only today), Android keeps a
light / dark toggle** — both themed in Premier Blue (dark = the iOS gradient; light = a Premier-Blue
light remap: near-white / very-light-blue surfaces, blue accents, navy ink text), switched at
runtime via `Brand.applyTheme` and persisted in `ThemePrefs` from the Profile screen. iOS will gain
the same toggle later. When the iOS app's design changes, mirror it here too.

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
- **Trainers** (Trainer Admins only — `summary.canManageTrainers`; server 403 otherwise) — a sixth
  tab: search, Trainers / Applicants / All segmented control, ACLP / ACTA / DACE / DDDLP / Finance /
  IBF Fit filter pills, and a profile (`trainer_profile/{id}`) with tap-to-call/email, NRIC masked
  with Show, accreditation, certifications, education, skills, experience, courses taught and recent
  classes (`/api/mobile/trainers`, `/api/mobile/trainers/{id}`). Names are uppercase by design.
- **Approvals** (admins only — role ∈ {ADMIN, HR, MANAGER}) — approve / reject pending **leave &
  expense** requests in-app (`GET /api/mobile/approvals`; `POST /api/{leave|expenses}/{id}/{approve
  |reject}`). **Enforced server-side** (403 for staff/interns); the client only shows the UI when
  `summary.isAdmin`. The two account admins (**Tan Soik Ching**, **Alfred Ang Chew Hoe**) hold role
  ADMIN and can action this queue.
- **Team** — company directory (richer contact fields for supervisory roles). Approvers tap a person
  for `EmployeeRecordsScreen` (`/api/mobile/employees/{id}/records`, approver-or-self): clock-in,
  MC and leave history; the three stat tiles open `team_records_clock/{id}` and
  `team_records_leave/{id}?kind=MC|LEAVE`. Routes start with `team` so the Team tab chrome stays.
- **Payslips** — list + native **PdfRenderer** viewer for the authenticated payslip PDF.
- **Expenses** — personal expense/medical claims with status, plus **submit an Expense or Medical
  claim** with a receipt (`POST /api/mobile/claims`, which files it in the employee's Drive folder).
- **Timesheet** — a simple **clock in / out** with a live elapsed timer and a monthly daily
  check-in/out history with total hours (`AttendanceHistorySection`, ported from iOS; punches via
  `/api/mobile/attendance` + `attendance/clock-{in,out}`, history via
  `/api/mobile/attendance/history?month=`). The separate weekly weekend/holiday grid is
  `WeeklyTimesheetScreen`.
- **Intern Attendance** (approvers only; More menu) — `InternAttendanceScreen` over
  `/api/mobile/attendance/summary?month=`, drilling into `intern_attendance_detail/{id}`.
- **Leave & MC Summary** (approvers only; More menu) — `LeaveSummaryScreen` over
  `/api/mobile/leave-summary?month=`, drilling into `mc_leave_summary_detail/{employeeId}`. The route
  is prefixed `mc_` on purpose: tab/title resolution is `startsWith`-based, so `leave_summary` would
  be claimed by the Leave tab.
- **Calendar** — the **team month grid** of everyone's approved leave (`/api/mobile/team-calendar`)
  with an Everyone / Only me filter and a per-day detail sheet, mirroring the iOS `TeamCalendarView`.
  A colleague's leave *type* is masked server-side unless the viewer is that person or an approver.
  The older agenda-style list (public holidays + the user's own events) remains at the
  `calendar_list` route, reachable from **More**.
- **Notifications** — top-bar **bell with unread badge** + list (`/api/notifications`).
- **Profile** — full employee record + self-service edit + change password, and the **light / dark
  theme toggle** (Android-only for now).

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
**five tabs — Home / Leave / Calendar / Team / More** — matching the iOS `MainTabView` 1:1, plus
**Trainers** before More for Trainer Admins, shown on every screen. The remaining modules (Payslips, Expenses, Profile, Timesheet, Time Off, Approvals) are
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

Deliberate Android-only extras: the Dark / Light theme card on Profile, the expense filter panel,
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
