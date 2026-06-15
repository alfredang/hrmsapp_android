# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

**Tertiary HRMS — native Android app** (Kotlin + Jetpack Compose, Material 3, MVVM). The Google
Play build of Tertiary Infotech Academy's HR Management System, **rebuilt fully native — no
WebView, no cross-platform runtime**. It is the Android sibling of the native iOS app
(`../iOS/TertiaryHRMSiOSApp`) and is a faithful 1:1 port of its features, theme, and API layer.
Min SDK **24 (Android 7)**, target SDK **36**, phone-first, theme **Premier Blue** (navy →
premier-blue → azure gradient, `ui/theme/Theme.kt`).

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
  browser would. Both **email + password** and **email one-time-code (OTP)** sign-in are supported.
- **Data endpoints**: the same additive, read-only `/api/mobile/*` namespace feeds this app
  (`summary`, `profile`, `leave`, `employees`, `expenses`, `payslips`, `calendar`), plus the
  pre-existing `/api/timesheet`, `/api/leave` POST, and `/api/payroll/payslip/{id}/pdf`.
- Changing the deployed URL means updating `Net.BASE_URL` in `data/Net.kt`.

## Features

Mirrors the iOS app: Login (password + OTP), Dashboard, Leave (+ apply), Team directory, Payslips
(native **PdfRenderer** viewer), Expenses, Calendar, Timesheet, Profile. **Accounting** and heavy
**admin authoring** flows are intentionally excluded (web-only), same as iOS.

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
typography, ≥48dp touch targets. The app runs on a dark, branded Premier Blue surface
(`darkColorScheme` + gradient brushes). Keep parity with the iOS app — when a feature changes on one
platform, mirror it on the other.
