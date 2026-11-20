<div align="center">

# TIA HRMS — Android

[![Google Play](https://img.shields.io/badge/Google%20Play-Download-414141?logo=googleplay&logoColor=white)](https://play.google.com/store/apps/details?id=com.tertiaryinfotech.hrportal)
[![Platform](https://img.shields.io/badge/Platform-Android%207%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/minSdk-24-orange)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/targetSdk-36-blue)](https://developer.android.com)

**Your HR on the go — leave, payslips, expenses, team and timesheet, rebuilt fully native.**

<a href="https://play.google.com/store/apps/details?id=com.tertiaryinfotech.hrportal">
  <img alt="Get it on Google Play" height="80"
       src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png">
</a>

[Download on Google Play](https://play.google.com/store/apps/details?id=com.tertiaryinfotech.hrportal) · [Web App](https://hrms.tertiaryinfotech.com) · [Report Bug](https://github.com/alfredang/hrmsapp_android/issues) · [Request Feature](https://github.com/alfredang/hrmsapp_android/issues)

</div>

## Screenshot

<div align="center">

![Screenshot](screenshot.png)

</div>

## About

**TIA HRMS for Android** (formerly Tertiary HRMS) is the Google Play build of Tertiary Infotech Academy's Human
Resource Management System — **rebuilt fully native in Kotlin + Jetpack Compose**, with no WebView
and no cross-platform runtime. It is the Android sibling of the native iOS app and a faithful 1:1
port of its features, the TIA HRMS **premium-blue** brand (navy → royal-blue gradient, frosted cards, gold accent), and API layer.

The app is a **secure client** of the existing HRMS web backend — it has no database of its own.
All data stays on the company server and is accessed over encrypted HTTPS, reusing the web app's
existing single sign-on session, exactly as a browser would.

### Features

| Module | Description |
|--------|-------------|
| **Login** | Email + password or one-time email code (OTP), via the web app's NextAuth session. **Persistent** — stays signed in across app restarts |
| **Dashboard** | Annual, medical and OT leave balances plus expenses at a glance; approvers (Manager / HR / Admin) also see **Full-time staff, Interns and Pending approvals** |
| **Leave** | View balances (including the **No Pay Leave cap and days left**) and full request history, and apply for leave in a few taps (server computes working days and proration). **Medical leave (MC)** attaches an MC photo. AL beyond the balance offers **Apply as No Pay Leave instead**; interns' sick leave goes in as No Pay Leave |
| **Approvals** *(admins only)* | Leave / MC / Claims tabs, one line per request (name, status, type, dates, days, applied, reason); tap a line for the full reason and Approve / Reject — enforced server-side (role ∈ Manager / HR / Admin) |
| **Payslips** | Browse personal payslips and open the official PDF, rendered natively in-app via `PdfRenderer` |
| **Expenses** | Submit and track expense / medical claims with a receipt photo and clear status badges |
| **Team** | Search the company directory; approvers see **Staff** and **Interns** as two lists with counts; admins tap a person for their **clock-in, MC and leave history** — the Days clocked / MC days / Leave days tiles open each full history |
| **Calendar** | Public holidays, personal events and approved leave, grouped by month |
| **Clock In / Out** *(interns, accountants, admins)* | Simple **clock in / out** with a live elapsed timer and a monthly daily check-in/out history with total hours — an open punch shows **hours so far** today, or **"No clock-out"** for an earlier day |
| **Timesheet** | Interns and accountants who clock in: **Timesheet**, their days and hours by month with a link to the weekend / public-holiday grid. Everyone else (and accountants too) logs weekend / public-holiday hours under **Off In Lieu**. Admins: one **Timesheet** screen — missing-punch and Off In Lieu approvals plus each intern's days worked and hours per month, drilling into their daily clock-in / out |
| **Time Off** | Hourly time off; approvers see everyone's requests (Pending / All, one line each) and approve or reject |
| **Leave & MC Summary** (admin) | Every active staff member's and intern's MC and leave for the month, with totals, a Staff / Interns filter and a per-person drill-down |
| **Trainers** *(everyone, More)* | Trainer contact list for every staff member and intern: search, then call / WhatsApp / email (`/api/mobile/trainers/contacts`, no NRIC) |
| **Trainer Admin** *(Trainer Admins, More → Manage)* | Trainer directory: Total / ACLP / Non-ACLP trainer counts and an ACLP / Non-ACLP / All filter (opens on ACLP, sorted by name), search with skill / certification suggestions (with counts) that become removable filter chips, an Accreditation menu (ACTA / DACE / DDDLP / Finance / IBF Fit), compact rows with the matching skill first, and call / WhatsApp / email / copy contacts; full profiles with NRIC masked behind Show |
| **Notifications** | In-app bell with unread badge and a notifications list, plus **push alerts** (Firebase Cloud Messaging, channel `hrms_alerts`): interns get missed clock-in/out and timesheet reminders, admins get every new leave / MC / NPL, time-off and expense request; tapping opens the right screen |
| **Calendar** | Planner-app layout: Month / List, Monday-first grid with adjacent-month days greyed, chips + "+N", today circled, every Singapore public holiday on a light grey cell with its name (with emoji), agenda for the selected day below. Leave is colour-coded by type with an emoji (annual, medical, no pay, other); approved time off and intern Off Days show; off days are shaded. Approvers: **Leave & time off for** Everyone or one person (every active staff member and intern, grouped, with counts), birthdays, and that person's leave and time-off totals for the month |
| **More menu** | View As (Trainer Admins) · Applications (MC, Leave, Time Off, Expense Claims, Timesheet / Off In Lieu) · Staff (Calendar, Payslips, My Profile, Trainers) · Attendance (Time In/Out: interns, accountants, admins) · Manage |
| **View As → Trainer Admin** *(Trainer Admins)* | A view with only the trainer pages: tabs Home / Trainers / Calendar / More; Home shows the Trainers, ACLP and Non-ACLP counts (each opens the directory on that filter) and a link to the directory. Saved on the device; back to My view on sign-out |
| **Analytics** (approvers) | 12 months of bank revenue and expenses, leave and MC per person, and headcount. Filters: period, All / Staff / Interns, person, leave type, expense category. Tap a bar for its figures. **Workforce breakdown**: gender, age group and ethnic group donuts for Staff / Interns / All |
| **Profile** | Full employee record, a **Work Schedule** card (working hours, working days, off days) and change password. **Appearance**: Light, Dark or System (default System, saved on the device) |

> **Secure by design** — for authorized Tertiary Infotech employees only. Sign in with email and
> password or a one-time email code; the session cookie is reused for every call over HTTPS. No
> advertising or analytics SDKs.

## Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | Kotlin (JVM target 17) |
| **UI** | Jetpack Compose, Material Design 3 |
| **Architecture** | Single-activity MVVM, `AndroidViewModel` as single source of truth |
| **Navigation** | Navigation Compose |
| **Networking** | OkHttp 4 + `PersistentCookieJar` (SharedPreferences-backed) |
| **Serialization** | kotlinx.serialization (JSON) |
| **PDF** | Android `PdfRenderer` (native payslip viewer) |
| **Build** | Gradle 8.14.3 (wrapper), AGP 8.9+ |
| **Backend** | Next.js 14 + PostgreSQL on Coolify (`hrms.tertiaryinfotech.com`) — consumed read-only |

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│  MainActivity (single Activity, Jetpack Compose)               │
│  RootScreen → LoginScreen | MainScaffold (Home/Leave/Team/More)│
└───────────────────────────┬──────────────────────────────────┘
                            │  state
┌───────────────────────────▼──────────────────────────────────┐
│  AuthViewModel  (AndroidViewModel — single source of truth)    │
└───────────────┬───────────────────────────┬──────────────────┘
                │                           │
      ┌──────────▼─────────┐      ┌──────────▼──────────┐
      │  AuthService       │      │  HrmsApi            │
      │  NextAuth sign-in  │      │  typed reads +      │
      │  csrf→callback→    │      │  apply-leave +      │
      │  session           │      │  PDF download       │
      └──────────┬─────────┘      └──────────┬──────────┘
                │                           │
          ┌──────▼───────────────────────────▼──────┐
          │  One shared OkHttp client                │
          │  + PersistentCookieJar (cookie reuse)    │
          └──────────────────┬───────────────────────┘
                            │ HTTPS
          ┌──────────────────▼───────────────────────┐
          │  HRMS web backend (Next.js 14 + Postgres) │
          │  /api/auth/*  ·  /api/mobile/*  ·  /api/… │
          └───────────────────────────────────────────┘
```

## Project Structure

```
hrmsapp_android/
├── app/src/main/java/com/tertiaryinfotech/hrportal/
│   ├── MainActivity.kt              # Single activity host
│   ├── TertiaryHrmsApp.kt           # App entry / composition root
│   ├── data/
│   │   ├── AuthService.kt           # NextAuth sign-in flow
│   │   ├── HrmsApi.kt               # Typed reads + apply-leave + PDF
│   │   ├── Net.kt                   # BASE_URL + shared OkHttp client
│   │   ├── PersistentCookieJar.kt   # SharedPreferences-backed cookies
│   │   ├── Models.kt                # Serializable DTOs
│   │   └── FlexNumberSerializer.kt
│   └── ui/
│       ├── AuthViewModel.kt         # Single source of truth
│       ├── LoginScreen.kt
│       ├── RootScreen.kt
│       ├── screens/                 # Dashboard, Leave, Team, Payslips, …
│       ├── components/              # Reusable brand controls
│       └── theme/Theme.kt           # TIA HRMS premium-blue theme (BRAND.md)
├── store/                           # Play listing assets + submission guide
├── scripts/                         # Asset generation helpers
└── app/build.gradle.kts
```

## Getting Started

### Prerequisites

- **JDK 17** (the one bundled with Android Studio works well)
- **Android SDK** with platform 36 installed
- An Android device or emulator running **Android 7 (API 24)** or newer

### Build & Run

No Android Studio GUI required — the project builds from the CLI.

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"

# Debug build + install on a running emulator/device
./gradlew :app:assembleDebug
$ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Changing the deployed backend URL means updating `Net.BASE_URL` in
[`data/Net.kt`](app/src/main/java/com/tertiaryinfotech/hrportal/data/Net.kt).

## Deployment (Google Play)

> **Live on Google Play** — [TIA HRMS](https://play.google.com/store/apps/details?id=com.tertiaryinfotech.hrportal) is published and available for download. It is owned by the corporate
> developer account **Tertiary Infotech Academy Pte Ltd** (`sales@tertiarycourses.com.sg`); v1.14
> (versionCode 29: Analytics screen for approvers, calendar All / Staff / Interns filter, push registration fix) was released to the production track on 2026-11-20 and is subject to Google review before it reaches users. v1.15 (versionCode 30: calendar person filter, workforce breakdown, trainer counts, Staff / Interns lists, approver Home figures) was released to the production track on 2026-11-20 and is subject to Google review before it reaches users. v1.16 (versionCode 31: calendar colours and emoji, time off and Off Days, birthdays, off-day shading and month totals, Work Schedule card, Home leave summary / finance / trainer tiles, No Pay Leave 14 days) was released to the production track on 2026-11-20 and is subject to Google review before it reaches users. v1.17 (versionCode 32: accountants clock in with Attendance and Timesheet, View As → Trainer Admin, "Trainer Management" renamed "Trainer Admin") was released to the production track on 2026-11-20 and is subject to Google review before it reaches users.

```bash
# Signed release artefacts (require keystore.properties + keystore/ — gitignored)
./gradlew :app:bundleRelease      # → app-release.aab  (Google Play)
./gradlew :app:assembleRelease    # → app-release.apk  (sideload)
```

- App id **`com.tertiaryinfotech.hrportal`** (matches the iOS bundle id).
- Play app signing is recommended; the local **upload key** lives in `keystore/upload-keystore.jks`
  with credentials in `keystore.properties` — **both gitignored. Back them up securely; the
  password is not recoverable.**
- Release signing only activates when `keystore.properties` is present, so CI/other machines build
  unsigned debug fine.
- Play listing assets and a step-by-step submission guide live in [`store/`](store/).

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

When a feature changes on Android, **mirror it on the iOS app** to keep parity.

## Developed By

**Tertiary Infotech Academy Pte. Ltd.**
[hrms.tertiaryinfotech.com](https://hrms.tertiaryinfotech.com)

## Acknowledgements

- Built with [Jetpack Compose](https://developer.android.com/jetpack/compose) and
  [Material Design 3](https://m3.material.io)
- Networking by [OkHttp](https://square.github.io/okhttp/) and
  [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)
- Native sibling of the TIA HRMS iOS app

---

<div align="center">

If you find this project useful, please consider giving it a ⭐

</div>
