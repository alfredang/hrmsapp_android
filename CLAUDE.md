# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

**Tertiary HRMS — native Android app** (Kotlin + Jetpack Compose, Material 3, MVVM). The Google
Play build of Tertiary Infotech Academy's HR Management System, **rebuilt fully native — no
WebView, no cross-platform runtime**. It is the Android sibling of the native iOS app
(`../iOS/hrmsapp`) and **mirrors its features, UI/UX, and API layer 1:1** — the iOS app is the
source of truth for both *functionality* and *visual design*. Min SDK **24 (Android 7)**, target
SDK **36**, phone-first.

**Theme — Premier Blue (pixel-matched to iOS).** The app uses the iOS app's **Premier Blue** design
system, not the web app's flat gray skin. The signature backdrop is the navy → premier → azure
diagonal gradient (`navy #0A1F44` → `premier #1D4ED8` → `azure #3B82F6`, exact hex ported from the
iOS `Theme/Theme.swift`); cards are translucent frosted panels on top of it, text is white, the
accent/button/tab colour is premier blue, and `sky #94C5FD` is the secondary accent. All tokens live
in `ui/theme/Theme.kt` (`Brand.*`) + `Type.kt`. **Unlike iOS (dark-only today), Android keeps a
light / dark toggle** — both themed in Premier Blue (dark = the iOS gradient; light = a Premier-Blue
light remap: near-white / very-light-blue surfaces, blue accents, navy ink text), switched at
runtime via `Brand.applyTheme` and persisted in `ThemePrefs` from the Profile screen. iOS will gain
the same toggle later. When the iOS app's design changes, mirror it here too.

## Relationship to the web app (hrms.tertiaryinfo.tech) and Coolify

Identical to the iOS app: this native app is a **client of the existing HRMS web backend** — it has
no database of its own.

- The web app is a **Next.js 14** application on **Coolify** at `https://hrms.tertiaryinfo.tech`,
  backed by **PostgreSQL**. The app pulls all data from that deployment over HTTPS; it never talks
  to PostgreSQL directly.
- **Authentication** reuses the web app's **NextAuth (Auth.js)** session. `data/AuthService.kt`
  performs the standard NextAuth flow: `GET /api/auth/csrf` → `POST /api/auth/callback/{credentials
  |otp}` → `GET /api/auth/session`. The session cookie is stored in a `PersistentCookieJar`
  (SharedPreferences-backed, shared by one OkHttp client) and reused for every call, exactly as a
  browser would. Both **email + password** and **email one-time-code (OTP)** sign-in are supported.
- **Data endpoints**: the same additive, read-only `/api/mobile/*` namespace feeds this app
  (`summary`, `profile`, `leave`, `employees`, `expenses`, `payslips`, `calendar`), plus the
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
- **Approvals** (admins only — role ∈ {ADMIN, HR, MANAGER}) — approve / reject pending **leave &
  expense** requests in-app (`GET /api/mobile/approvals`; `POST /api/{leave|expenses}/{id}/{approve
  |reject}`). **Enforced server-side** (403 for staff/interns); the client only shows the UI when
  `summary.isAdmin`. The two account admins (**Tan Soik Ching**, **Alfred Ang Chew Hoe**) hold role
  ADMIN and can action this queue.
- **Team** — company directory (richer contact fields for supervisory roles).
- **Payslips** — list + native **PdfRenderer** viewer for the authenticated payslip PDF.
- **Expenses** — personal expense/medical claims with status, plus **submit a claim** with a
  receipt photo.
- **Timesheet** — a simple **clock in / out** with a live elapsed timer and a last-7-days log
  (ported from the iOS `ClockView`; punches stored via `/api/mobile/attendance` +
  `attendance/clock-{in,out}`). *Not* a weekly OT grid.
- **Calendar** — public holidays, the user's events, and approved leave, grouped by month.
- **Notifications** — top-bar **bell with unread badge** + list (`/api/notifications`).
- **Profile** — full employee record + self-service edit + change password, and the **light / dark
  theme toggle** (Android-only for now).

**Excluded / removed** (same product scope as iOS): **Accounting** and heavy **admin-authoring**
flows (web-only). **Woods Square building access** is **not** part of this app (removed — it isn't in
the iOS app either).

**Google Sign-In** is present in the iOS app but **hidden in this Android v1** — it needs an Android
OAuth client id registered against the app's signing SHA-1. Password + OTP are fully functional. To
enable later, add the Android client and a native sign-in flow, then surface the button.

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
```

`compileSdk`/`targetSdk` 36 requires AGP 8.9+. Only system frameworks + a tiny dependency set
(Compose, OkHttp, kotlinx.serialization, Navigation) — no heavyweight third-party libraries.

## Architecture

Single-activity (`MainActivity`) Compose app. `AuthViewModel` (`AndroidViewModel`) is the single
source of truth, driving `data/AuthService` (NextAuth sign-in) and `data/HrmsApi` (typed reads +
apply-leave + PDF download), both riding one shared OkHttp client + `PersistentCookieJar`.
`RootScreen` routes loading → `LoginScreen` → `MainScaffold` (Home / Leave / Team / More) via a
`NavHost`; "More" sub-modules are nested destinations. Reusable Premier Blue controls live in
`ui/components/`.

## Signing & Google Play

- App id **`com.tertiaryinfotech.hrportal`** (matches the iOS bundle id). Play app signing is
  recommended; the local **upload key** lives in `keystore/upload-keystore.jks` with credentials in
  `keystore.properties` — **both gitignored. Back them up securely; the password is not recoverable.**
- Release signing is wired in `app/build.gradle.kts` and only activates when `keystore.properties`
  is present, so CI/other machines build unsigned debug fine.
- Play listing assets + a step-by-step submission guide live in `store/`.

## Conventions

Follow the bundled `mobile-android-design` skill: Material Design 3, Compose idioms, large
typography, ≥48dp touch targets. The app runs on the web app's flat dark surface (`darkColorScheme`
in `ui/theme/Theme.kt` — `Brand.Background`/`Brand.Surface`, no gradients), indigo accent, and
pastel status-badge/banner tints (`StatusTint`, `IconTint`, `BannerTint`) copied from the web app's
Tailwind classes. Keep parity with the iOS app for *functional* behavior — when a feature changes
on one platform, mirror it on the other — but for *visual* design, the web app is the source of
truth.
