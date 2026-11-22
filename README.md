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
| **Home (per role)** | **Staff:** annual leave and MC left with **Apply for leave / Apply for MC**. **Interns** (and accountants who must clock in): a **Clock in / out** card at the top. **Admins:** active staff and interns, Pending approvals and a **Waiting for approval** card (leave, MC, time off, claims, missed punches). **Accountants:** finance only: pending claims, income and expenses with credit / debit counts, bank statements, bank reconciliation; **every Finance row opens its detail** (see Finance details; pending claims open Approvals for approvers only). The Welcome card opens My Profile |
| **Leave** | View balances (including the **No Pay Leave cap and days left**) and full request history, and apply for leave in a few taps (server computes working days and proration). **Medical leave (MC)** attaches an MC photo. AL beyond the balance offers **Apply as No Pay Leave instead**; interns' sick leave goes in as No Pay Leave |
| **Approvals** *(admins only)* | Leave / MC / Claims tabs, one line per request (name, status, type, dates, days, applied, reason); tap a line for the full reason and Approve / Reject — enforced server-side (role ∈ Manager / HR / Admin) |
| **Payslips** | Browse personal payslips and open the official PDF, rendered natively in-app via `PdfRenderer` |
| **Expenses** | Submit and track expense / medical claims with a receipt photo and clear status badges |
| **Team** | Search the company directory by name, position, department, email or employment type; each row shows the full name, the department ("No department" when missing) with a **Full time / Part time / Contract / Intern** pill, and the email for approvers. Approvers see **Staff** and **Interns** as two lists with counts; admins tap a person for their **clock-in, MC and leave history** — the Days clocked / MC days / Leave days tiles open each full history |
| **Calendar** | Public holidays, personal events and approved leave, grouped by month |
| **Clock In / Out** *(interns, accountants who must clock in)* | Simple **clock in / out** with a live elapsed timer and a monthly daily check-in/out history with total hours — an open punch shows **hours so far** today, or **"No clock-out"** for an earlier day. Admins and staff do not clock in |
| **Timesheet** | Interns and accountants who clock in: **Timesheet**, their days and hours by month with a link to the weekend / public-holiday grid. Everyone else (and accountants too) logs weekend / public-holiday hours under **Off In Lieu**. Admins: one **Timesheet** screen — missing-punch and Off In Lieu approvals plus each intern's days worked and hours per month, drilling into their daily clock-in / out |
| **Time Off** | Hourly time off; approvers see everyone's requests (Pending / All, one line each) and approve or reject |
| **Leave & MC Summary** (admin) | Every active staff member's and intern's MC and leave for the month, with totals, a Staff / Interns filter and a per-person drill-down |
| **Trainer contacts** *(everyone, More → People)* | Trainer contact list for every staff member and intern: search, then call / WhatsApp / email (`/api/mobile/trainers/contacts`, no NRIC) |
| **Trainers** *(Trainer Admins, More → People)* | **Home search** (Trainer Admin Home and the Home "Trainer Admin" section): type a name, skill, certification or course to see matching skill chips, the first 5 trainers and "See all N trainers"; empty, it shows popular skills; a chip or "See all" opens the directory on All with that search. Trainer directory: Total / ACLP / Non-ACLP trainer counts and an ACLP / Non-ACLP / All filter (opens on ACLP, sorted by name), search with skill / certification suggestions (with counts) that become removable filter chips, an Accreditation menu (ACTA / DACE / DDDLP / Finance / IBF Fit), compact rows with the matching skill first, and call / WhatsApp / email / copy contacts; full profiles with NRIC masked behind Show |
| **Notifications** | Top bar reads **TIA HRMS**. In-app bell with unread badge and a notifications list, plus **push alerts** (Firebase Cloud Messaging, channel `hrms_alerts`): interns get missed clock-in/out and timesheet reminders, admins get every new leave / MC / NPL, time-off and expense request; tapping opens the right screen (a `?request=<id>` on the link is ignored for routing) |
| **Calendar** | Planner-app layout: Month / List, Monday-first grid with adjacent-month days greyed, chips + "+N", today circled, every Singapore public holiday on a light grey cell with its name (with emoji), agenda for the selected day below. Leave is colour-coded by type with an emoji (annual, medical, no pay, other); approved time off and intern Off Days show; off days are shaded. Approvers: **Leave & time off for** Everyone or one person (every active staff member and intern, grouped, with counts), birthdays, and that person's leave and time-off totals for the month |
| **More menu** | Mirrors the web sidebar for the current view: My Stuff · People · Processes (leave, time off, clock in/out, timesheet / Off In Lieu, approvals) · Finance (payslips, claims) · Accounting (web links: expense / income tracking, bank reconciliation, bank statements) · Documents (SOP) · System (Settings). Admins do not apply for leave; accountants have no leave, MC or time off |
| **Finance details** *(admins, accountants)* | From the accountant Home's Finance card: **Income / Expenses** (one month of bank credits or debits with a month switcher, total and count, search by payee / reference / type, tap a line for every field and the raw statement text), **Bank statements** (each import with its dates, credits and debits, "Imported up to"), **Bank reconciliation** (latest DBS against QuickBooks run: to review / all clear, Matched / Amount differs / Bank only / QuickBooks only tiles, a line filter that opens on To review, earlier runs). Read-only; each screen links to its web page (`/api/mobile/finance/*`, `/api/accounting/reconciliation`) |
| **View As** | **Alfred only** (`angch@`, `ViewAsRules.VIEW_AS_EMAILS`, web view-as-policy): preview the **Admin / Staff / Intern / Accountant** screens from Home or More; only what is shown changes, forms still act as you, and clocking in is off while previewing. **Trainer Admins** (TRAINER_ADMIN) switch **My view / Trainer Admin**; everyone else sees no switch and a saved preview is ignored. The Trainer Admin view has tabs Home / Trainers / Calendar / More; its Home is the title card, the switch, the trainer search, the counts and the directory link |
| **Force update** | The app checks the Play version at launch and on resume; an older app shows a blocking **Update required** screen (`/api/public/app-version`, and HTTP 426 from the server) |
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
│   │   ├── Finance.kt               # Bank transactions, statements, reconciliation (number-or-string money)
│   │   └── FlexNumberSerializer.kt
│   └── ui/
│       ├── AuthViewModel.kt         # Single source of truth
│       ├── LoginScreen.kt
│       ├── RootScreen.kt
│       ├── screens/                 # Dashboard, Leave, Team, Payslips, Finance details, …
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
> (versionCode 29: Analytics screen for approvers, calendar All / Staff / Interns filter, push registration fix) was released to the production track on 2026-11-20 and is subject to Google review before it reaches users. v1.15 (versionCode 30: calendar person filter, workforce breakdown, trainer counts, Staff / Interns lists, approver Home figures) was released to the production track on 2026-11-20 and is subject to Google review before it reaches users. v1.16 (versionCode 31: calendar colours and emoji, time off and Off Days, birthdays, off-day shading and month totals, Work Schedule card, Home leave summary / finance / trainer tiles, No Pay Leave 14 days) was released to the production track on 2026-11-20 and is subject to Google review before it reaches users. v1.17 (versionCode 32: accountants clock in with Attendance and Timesheet, View As → Trainer Admin, "Trainer Management" renamed "Trainer Admin") was released to the production track on 2026-11-20 and is subject to Google review before it reaches users. **v1.18 (versionCode 33: iOS 1.20 parity — Home per role, View As previews, admin Leave & MC tab, accountant finance Home, More = web sidebar, Edit Profile fix, force update) was released to the production track on 2026-11-22** and is subject to Google review before it reaches users. v1.21 (versionCode 34: iOS 1.21 parity — Finance details, Trainer Admin Home search, "TIA HRMS" title, View As for Alfred only, Team rows with employment type and email) is committed locally and not yet released.

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
