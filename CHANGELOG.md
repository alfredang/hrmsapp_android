# Changelog

All notable changes to Tertiary HRMS (Android) are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com); the app adheres to
[Semantic Versioning](https://semver.org).

## [1.12] — 2026-11-11 · versionCode 24

### Release notes (Play, en-US)
- Push notifications for approvals, clock-in/out and timesheet reminders.
- Trainer contact list under More for all staff.
- Clearer white initials on avatars.

### Added
- Push alerts via Firebase Cloud Messaging (project `tia-hrms`): channel `hrms_alerts`, Android 13+
  permission prompt, token registration with `/api/mobile/push-token`, tap-to-open by link.
- More → Trainers: searchable trainer contact list for every user (call, WhatsApp, email).
- More → Manage group (Approvals, Timesheet, Leave & MC Summary, Trainer Management).

### Changed
- Trainer Management moved from the bottom tab bar into More → Manage (still Trainer Admins only).
- Notifications list and unread badge now include every alert for the user (missed clock-in/out,
  timesheet reminders, approval requests for admins).
- Calendars start the week on Monday; public-holiday days have a light grey cell and show the
  holiday's name.
- Certificate chips show the short code and stay on one line; the Accreditation section lists only
  what the trainer holds, with an "IBF" chip for a Fit & Proper declaration.

## [1.11] — 2026-11-10 · versionCode 23

Neat, well-structured, interactive screens and one light theme, matching the web app (464200b).

### Release notes (Play, en-US)
- New light look: one clean, bright theme across the app.
- Cleaner lists: leave, MC, claims and time off show one line each. Tap a line to read the full reason.
- Trainer search with suggestions, and call, WhatsApp or email a trainer in one tap.
- One Timesheet for interns.
- Managers can approve or reject time-off requests in the app.

### Changed
- **One light premium-blue theme.** Navy bars, near-white → mist-blue backdrop, white cards, navy
  text, royal-blue accents, gold only for badges. The dark theme and the Dark / Light card on
  Profile are removed (`ThemePrefs` deleted); status pills are lighter.
- **Approvals**: Leave / MC / Claims tabs with counts; one line per request (name, status, type,
  dates, days, applied, reason). Tap a line for the full reason and Approve / Reject.
- **Leave tab** and **Team › MC / Leave history**: one-line records; the reason opens a sheet.
  Balances fold away.
- **Time Off**: approvers see everyone's requests (Pending / All) and approve or reject in-app;
  requesters cancel their own pending request from the sheet.
- **Timesheet**: only interns have timesheets. Interns see *Timesheet*, everyone else *Off In Lieu*
  (same weekly page). Admins get one **Timesheet** screen — Needs approval (missing punches and
  Off In Lieu, approve / reject) and each intern's days and hours by month — replacing the separate
  Intern Attendance entry.
- **Trainers**: search with skill / certification suggestions and counts that become removable
  chips, an Accreditation menu, compact rows with the matching skill first, and a contact sheet
  (call, WhatsApp, email, copy). Profile: Call / WhatsApp / Email buttons, copy icons, folding
  sections. The directory loads once and filters on the device (port of the web filter).
- Notification links: `/timesheet/overview` opens the admin Timesheet, `/timesheet` the weekly
  page, `/attendance` Clock In / Out, `/time-off` Time Off.

### Added
- `RecordTable` / `RecordDetailSheet`, `CollapsibleSection`, `FilterTabs`, `KpiStrip`,
  `TintButton`, `NoteDialog` components.
- Unit tests for the trainer search port, WhatsApp links and the correction / Off In Lieu payloads.

### Play submission
- Production track, full rollout (`completed`), corporate account `sales@tertiarycourses.com.sg`;
  uploaded 2026-11-10 through the Android Publisher API (shared service account), release
  "1.11 (23)", en-US notes as above. Listing and store assets unchanged.

## [1.10] — 2026-11-09 · versionCode 22

The app is now **TIA HRMS**, with the new leave rules and a trainer directory.

### Added
- **No Pay Leave option when annual leave exceeds your balance.** If the server says a request is
  more AL than you have (`AL_EXCEEDS_BALANCE`), the app shows its message and the No Pay Leave left
  this year, and offers **Apply as No Pay Leave instead**, which resubmits the same dates and day
  type as No Pay Leave (reusing any uploaded document).
- **Intern sick leave goes in as No Pay Leave.** Interns (roles exactly `INTERN`) have no paid MC:
  picking Medical Leave switches the form to No Pay Leave up front, prefills the reason
  "Sick leave (MC)" and keeps the medical-certificate attachment, as the web form does. A server
  `INTERN_MC_NOT_PAID` refusal offers the same switch.
- **No Pay Leave balance.** The Leave tab shows the No Pay Leave cap to date and the days left
  (21 days a year, prorated by full months served), and the form shows cap / taken / pending /
  remaining and blocks requests over the cap (`NPL_CAP_EXCEEDED` is shown as a message).
- **Trainer directory for Trainer Admins** — a Trainers tab, shown only when
  `summary.canManageTrainers` is true: search, Trainers / Applicants / All, filters for ACLP, ACTA,
  DACE, DDDLP, Finance background and IBF Fit declaration, and a profile with identity and contact
  (tap to call or email, NRIC masked with Show), accreditation, certifications, education, skills,
  work experience, courses taught and recent classes (`/api/mobile/trainers`).
- JVM unit tests for the JSON payloads, the coded leave errors and the trainer formatting.

### Changed
- **Renamed to TIA HRMS** (launcher label, sign-in header, More footer, Play listing).
- **New premium-blue branding** (BRAND.md): navy-950 → navy-800 → royal-600 backdrop, navy top and
  tab bars in both themes, royal-300 / royal-600 accents, mist secondary text and gold-400 as the
  single warm accent (unread badge, accreditation badges).
- Launcher icon matches the iOS app icon exactly: adaptive foreground rendered from
  `make_app_icon.swift`, gradient background vector, themed monochrome layer, new legacy/round
  mipmaps, 512 Play icon and feature graphic.
- AL beyond the balance is no longer described as a deficit: the form says it needs No Pay Leave.

### Play submission
- Production track, full rollout (`completed`), corporate account `sales@tertiarycourses.com.sg`;
  uploaded 2026-11-09 through the Android Publisher API (shared service account), release
  "1.10 (22)". The same edit renamed the en-US listing to TIA HRMS and replaced the icon, feature
  graphic and the 8 phone screenshots (`store/screenshots/`).

## [1.9] — 2026-11-06 · versionCode 21

Full parity with the iOS app, in both features and look.

### Added
- **Medical claims** — New claim now offers Expense or Medical and files the receipt in the matching
  Google Drive folder, the same flow as iOS.
- **Snap a claim** and **Clock in / out** quick actions on Home.
- Photo preview with Retake and Remove on claims and medical certificates.
- Pull to refresh on Clock, Timesheet, Time Off, Approvals, Intern Attendance, Leave & MC Summary and
  team records.
- Pinch to zoom on payslips, AL and MC available tiles on the Leave & MC Summary detail, and Privacy
  and Terms links on More.
- **Use advance annual leave** switch on the leave form.

### Changed
- New look matching iOS: login, top bar with the company logo, tab bar on every screen, Home, More,
  Leave, Payslips, Expenses, Profile and Clock, with iOS buttons, cards, status pills and colours.
- The Leave tab lists every leave type's balance and requests in one list.
- Signing in now asks for your email first, then your password or a one-time code.

### Fixed
- **No Pay and Compassionate Leave were invisible after applying**, because the Leave tab only showed
  Annual and Medical.
- Removed the default password hint from the login screen.
- Saving your profile no longer overwrites a blank date of birth with today's date.
- Leave spanning December and January now skips the new year's public holidays.
- Photos are resized before upload, so large camera photos no longer hit the 5 MB limit.
- Clearing a weekend or holiday on the timesheet now works, and locked weeks say so.
- The real reason is shown when the server refuses an action, instead of "session expired".
- The keyboard no longer covers form fields, and Next moves to the next field.

### Removed
- The hamburger menu, role switcher and avatar menu. Everything in them is on the More tab.

### Play submission
- Production track, full rollout, corporate account `sales@tertiarycourses.com.sg`.

## [1.8] — 2026-11-02 · versionCode 20

Team drill-down parity with iOS (v1.9 + v1.12).

### Added
- **Team member records** (ADMIN/HR/MANAGER) — tap a colleague on the Team tab for their clock-in
  history, MC history and leave history (`/api/mobile/employees/{id}/records`, approver-or-self,
  enforced server-side). The **Days clocked**, **MC days** and **Leave days** tiles each open the
  full history: every clock in/out grouped by month with hours, or the full MC / leave list.

### Play submission
- First release from the corporate account `sales@tertiarycourses.com.sg` (Developer ID
  `6132721113732535566`) after the app transfer. Production track, full rollout; also carries the
  1.7 Leave & MC Summary, which was never uploaded to Play.

## [1.7] — 2026-10-21 · versionCode 19

### Added
- **Leave & MC Summary** (ADMIN/HR/MANAGER, More menu + drawer) — the month's leave and MC roll-up
  of every active staff member and intern (`/api/mobile/leave-summary?month=`): headcount, MC days
  and people on MC, annual-leave days, pending days, an All / Staff / Interns filter, and one row per
  person (MC, AL, other leave, intern time-off hours, pending badge). Tapping a person shows that
  month's requests (cross-month leave split by working days) and their year balances. Same data
  as the AI-agent report on the web backend.

## [1.6] — 2026-10-08 · versionCode 18

Attendance history parity with the web app and iOS (v1.8).

### Added
- **Daily check-in history** — Clock In / Out now shows a month's check-in / check-out times per
  day with total hours and days worked (`/api/mobile/attendance/history?month=`), with month paging.
  Replaces the last-7-days log.
- **Intern Attendance** (ADMIN/HR/MANAGER, More menu + drawer) — each intern's days worked, total
  hours, last check-in and a live "Working" badge for the month (`/api/mobile/attendance/summary`),
  tapping through to that intern's daily history.

### Play submission
- 2026-10-08: AAB versionCode 18 uploaded to the **Production** track via the Android Publisher
  API (shared service account), full rollout, sent for Google review on commit.

## [1.5] — 2026-09-12 · versionCode 17

Brings the Android app to feature and UX parity with the native iOS app (v1.7).

### Added
- **Team Calendar** — the whole team's approved leave as a month grid (`/api/mobile/team-calendar`),
  with an Everyone / Only me filter, ‹ Today ›, month paging and a per-day detail sheet. Multi-day
  requests render on every day they span. A colleague's leave *type* stays masked unless the server
  discloses it (you, or an approver), so medical leave never leaks company-wide.
- **Google Sign-In** — native authorization-code + PKCE flow over Chrome Custom Tabs (no Play
  Services SDK), exchanging the Google `id_token` at `POST /api/auth/google-mobile` for the same
  NextAuth session cookie password and OTP use. The button is hidden unless the build carries a
  `GOOGLE_ANDROID_CLIENT_ID`, so it can never dead-end the user.

### Changed
- **Five bottom tabs — Home / Leave / Calendar / Team / More** — matching the iOS `MainTabView`
  1:1. The **Team** and **More** screens existed but were previously unreachable; both are now
  first-class destinations. The hamburger drawer is kept as a secondary path (now including Team),
  so nothing that was reachable before became unreachable.
- The Calendar tab is the new team grid; the previous agenda-style list remains at `calendar_list`,
  reachable from More.
- **Backend host corrected to `https://hrms.tertiaryinfotech.com`** (was `hrms.tertiaryinfo.tech`).
  Both alias the same Coolify deployment, but NextAuth reports its callbacks on the canonical host,
  which is also what the iOS app uses.

### Fixed
- The More tab showed a hardcoded "v1.0"; it now reads the real version from `BuildConfig`.

### Play submission
- Track: **Production**, full rollout, 177 countries/regions.
- Uploaded and submitted for review **2026-09-12** via the Android Publisher API
  (service account `codex-play-publisher@kael-projects.iam.gserviceaccount.com`, granted
  app-scoped release permissions on the app that day). Console read-back:
  *Active · Release 17 (1.5) in review*.
- Submitted from the **personal** `angch@tertiaryinfotech.com` account: the move to
  **sales@tertiarycourses.com.sg** was still blocked on the Play "Transfer apps" form, which
  requires the receiving account's Developer ID and registration transaction ID (console-only —
  no Publisher API endpoint exists for it). Publishing first was deliberate: an app transfer
  carries the app and its full release history, so v1.5 follows the app across whenever the
  transfer completes.
- **Google Sign-In ships dormant.** The release has no `GOOGLE_ANDROID_CLIENT_ID`, so the button
  is hidden; password + OTP are unaffected. Enabling it needs an Android OAuth client registered
  against the **Play App Signing** SHA-1, best done once under the account that ends up owning
  the app.

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
