# Changelog

All notable changes to Tertiary HRMS (Android) are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com); the app adheres to
[Semantic Versioning](https://semver.org).

## [1.4] — 2026-08-04 · versionCode 16

### Added
- **Time Off** — submit and cancel hourly time-off requests (Exams / Emergency / Others)
  with HH:mm start and end times and a live hours preview.
- **Timesheet** — weekly weekend / public-holiday Off-In-Lieu grid: mark each day Off,
  Half or Full, preview the OT total, and submit the week.

### Changed
- Drawer: **Timesheet** now opens the weekly grid; the punch screen is relabelled
  **Clock In / Out**. Drawer highlighting uses longest-prefix matching.

### Play submission
- Track: **Production**, full rollout, 177 countries/regions.
- Production access granted 2026-08-28 (3rd application); submitted for review 2026-08-28.
- **Approved and live on Google Play** — https://play.google.com/store/apps/details?id=com.tertiaryinfotech.hrportal

## [1.3] — 2026-07-27 · versionCode 15

### Added
- **Premier Blue** theme with full iOS visual parity (navy → premier → azure gradient,
  frosted translucent cards, light / dark toggle persisted in `ThemePrefs`).
- **Approvals** queue for admins (ADMIN / HR / MANAGER) — approve or reject pending
  leave and expense requests in-app.
- **Clock in / out** timesheet with a live elapsed timer and a last-7-days punch log.

### Changed
- Leave, calendar, timesheet and expense screens redesigned; expense payment-received flow.
- Hamburger navigation polish.

## [1.2] — 2026-06-14 · versionCode 14

### Added
- Initial Google Play release: native Kotlin + Jetpack Compose rebuild of the HRMS
  employee app (no WebView), mirroring the native iOS app 1:1.
- Login (email + password and email OTP) over the web app's NextAuth session, with
  persistent login via a SharedPreferences-backed cookie jar.
- Dashboard, Leave (apply + MC photo upload), Team directory, Payslips (native
  PdfRenderer viewer), Expenses, Calendar, Notifications and Profile.

### Play submission
- Track: Closed testing (All Testers), Singapore.
