# Changelog

All notable changes to Tertiary HRMS (Android) are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com); the app adheres to
[Semantic Versioning](https://semver.org).

## [1.23] — 2026-11-23 (versionCode 44)

iOS 1.22 (50) parity: the login now matches the iOS app 1:1.

### Fixed
- **Continue with Google is back.** Every release since 1.5 hid it: the client id lived only in the gitignored `local.properties` and was never set. `build.gradle.kts` now defaults to the iOS app's OAuth client (a native client with no secret, custom-scheme redirect, an audience the server already accepts). Google refuses custom schemes for Android-type clients.
- The sign-in and launch screens are always light (white card on the light backdrop), whatever the phone or in-app appearance (`Appearance.signedIn`).

### Changed
- Continue with Google shows before an email is typed; Google's account chooser picks the account. "Email me a sign-in code" is enabled once an email is typed.

### Release notes (Play, en-US)
- Sign in with Google: tap Continue with Google and pick your account. No need to type your email first.
- The sign-in page is now always light.

### Play submission
- Production track, full rollout, committed through the Android Publisher API on 23 Nov 2026 (read back: `1.23 (44)` completed, replacing `1.22 (43)`); subject to Google review. Corporate account confirmed by API: the service account is a member of `6132721113732535566` with account-wide permissions and gets 403 on the personal account.

## [1.22] — 2026-11-23 (versionCode 43)

iOS 1.22 (49) parity.

### Fixed
- Continue with Google always opens Google's account chooser (`prompt=select_account` on every sign-in; `login_hint` only prefills). `hd` removed: it hid personal accounts, which part-time staff and interns may use, and opened a type-your-email page.

### Release notes (Play, en-US)
- Continue with Google now always lets you choose which Google account to use.

### Play submission
- Production track, full rollout, released through the Android Publisher API on 23 Nov 2026 (read back: `1.22 (43)` completed). Corporate account confirmed by API: the service account is a member of `6132721113732535566` with account-wide permissions and gets 403 on the personal account; the public listing's developer is Tertiary Infotech Academy Pte Ltd.

## [1.21] — 2026-11-23 · versionCode 42

iOS 1.21 parity (builds 41–48). versionCodes 34–40 shipped 1.21 earlier; 41 was never uploaded and is included here.

### Release notes (Play, en-US)
- Sign-in shows the academy logo: staff continue with Google, interns and part-time staff get an emailed code.
- More lists Accounting first, then Claims and Payslips, as on the website.
- Accountants: tap Income, Expenses, Bank statements or Bank reconciliation on Home for details.
- Trainer Admins: search trainers and skills from Home; switch views under your name.
- Staff and interns get an SOP tab; Team is for admins.
- The calendar shows your own leave and the holidays.

### Changed
- `MoreMenu.sections`: Accounting before Finance; Finance = Claims, then Payslips (web sidebar order, Alfred, 23 Nov 2026). `HomeRolesTest` updated.
- (41) Academy logo on the sign-in header; Back to sign in on the code step.

### Play submission
- Production track, full rollout, released through the Android Publisher API on 23 Nov 2026 (read back: `1.21 (42)` completed).


iOS 1.20 parity (builds 36–37 and the More rebuild).

### Release notes (Play, en-US)
- A Home for each role: interns clock in from Home; staff see AL and MC left with Apply buttons; admins see staff, interns and what is waiting for approval; accountants see the finance figures.
- Admins can View As Staff, Intern or Accountant.
- More now follows the website menu: My Stuff, People, Processes, Finance, Accounting, Documents, System.
- Analytics removed. Edit Profile always opens your details.
- When a newer version is out, the app asks you to update.

### Added
- **Role Homes** (`DashboardScreen`): `HomeClockCard` (today's punch, one-tap Clock in / out, History) for people who must
  clock in; `BalanceCard` (Annual leave left / MC left, interns "No pay", Apply for leave / Apply for MC → the form preset to
  AL / MC); admin Overview (active staff / interns from `/api/mobile/employees`, Pending = sum of the new **Waiting for
  approval** card: Leave, MC, Time off, Expense claims → Approvals; Missed punches → admin Timesheet; `PendingApprovals`);
  accountant Home = clock card + Finance card only. Per-role quick actions (admin, intern, staff); none for accountants.
  Welcome card: chevron beside "Welcome back", name on one line across the full width (shrinks to fit), opens My Profile.
- **View As for approvers** (`ViewAsMode` STAFF / INTERN / ACCOUNTANT; `ViewAsRules.available / shown / summaryView`;
  `ViewAsMenu` on Home and More, options tagged `viewAs.<key>`). Previews fetch `/api/mobile/summary?view=…`; forms keep
  the real roles; clocking in is disabled (Home card and the Clock screen) while previewing.
- **Finance card**: credit / debit counts, Bank statements (imports, covered up to) and Bank reconciliation (latest run,
  lines to review, or "Not run") from the optional `financeKpis` fields of web e46f631 (`HomeTiles.finance`).
- **More = the web sidebar** (`data/MoreMenu.kt`): My Stuff, People, Processes, Finance, Accounting (web links + the Drive
  Bank Statements folder), Documents → SOP, System → Settings; rows tagged `more.row.<id>` / `more.apply.<AL|MC>`.
- **Force update**: every HRMS request sends `X-App-Platform: android`, `X-App-Version`, `X-App-Build`; at launch and on
  resume `GET /api/public/app-version` (Play's live version) and any HTTP 426 show the blocking "Update required" screen
  (`ui/UpdateGate.kt`, `AppVersion`). A failed check never blocks.
- Unit tests: `HomeRolesTest`, new `ViewAsTest` cases.

### Changed
- Admin Leave tab = Leave & MC management (no Apply, no own balances). Accountants have no Leave tab and no leave, MC or
  time off anywhere (`LeaveRules.isAccountant`). Clock In/Out is never shown to admins or staff.
- Edit Profile captures the record when tapped (never opens blank).

### Removed
- Analytics (screen, `/api/mobile/analytics` calls, `data/Analytics.kt`, tests), as on iOS 1.20.

### Play submission
- 2026-11-22 (SGT): uploaded through the Android Publisher API and released to the **production** track as "1.18 (33)",
  status `completed` (read back after commit; the API does not report Google's review state). Account ownership
  (corporate, `sales@tertiarycourses.com.sg`) is from the saved transfer note, not re-checked in the console.
- Force update for Android starts only when the server's Android live version is set to 1.18 (after the release is live).

## [1.17] — 2026-11-20 · versionCode 32

### Release notes (Play, en-US)
- Accountants now clock in and out like interns: More → Attendance → Time In/Out, and a Timesheet of your days and hours.
- Trainer Admins can switch More → View As to "Trainer Admin" for a view with just the trainer directory and its counts.
- "Trainer Management" is now called "Trainer Admin".

### Added
- **`mustClockIn`** (`LeaveRules.mustClockIn(roles, role)`, `SessionUser.mustClockIn`): the web rule of
  `src/lib/attendance-policy.ts` (20 Nov 2026), case-insensitive: roles exactly `["INTERN"]`, or `ACCOUNTANT` with none
  of `ADMIN`, `HR`, `MANAGER`. Unit-tested in `LeaveErrorsTest`.
- **My Timesheet** (route `my_timesheet`, `MyTimesheetScreen` in `AttendanceHistoryScreen.kt`), the port of iOS
  `MyTimesheetView`: the month's clock-in / clock-out days and hours, then a link to the weekend / public-holiday grid
  (Off In Lieu).
- **View As → Trainer Admin** (web 1a394c4, view mode "trainer"), Trainer Admin holders only (`summary.canManageTrainers`).
  A My view / Trainer Admin control at the top of More, saved on the device (`hrms_view_as`), reset to My view on
  sign-out, on a fresh sign-in and when the role is gone. In the Trainer Admin view: tabs Home / Trainers / Calendar /
  More (Leave and Team hidden); Home shows "Trainer Admin", the Trainers / ACLP trainers / Non-ACLP trainers tiles (each
  opens the directory on that filter) and "Open trainer directory"; More shows View As, My Profile, Calendar, Trainer
  Admin and sign out. Switching to Trainer Admin opens the Trainers tab; switching back opens Home. Rules in
  `data/ViewAs.kt` (`ViewAsRules`), unit-tested in `ViewAsTest`; state in `ui/ViewAs.kt`. Push routing is the same
  in both views.

### Changed
- **More**: the "Intern" group is now **Attendance** (Time In/Out) and also shows for accountants who clock in.
  Applications: interns get **Timesheet** (My Timesheet); accountants who clock in get **Timesheet** and **Off In Lieu**;
  everyone else keeps Off In Lieu. Manage → **Trainer Admin** (was Trainer Management).
- **Home**: the trainer row is titled **Trainer Admin**; its tiles now open the directory on their own filter.
- **Push / notifications**: a `/timesheet` link opens My Timesheet for anyone who must clock in (it opened the weekly
  grid); `/attendance` still opens the clock screen.

### Play submission
- 2026-11-21 (SGT): uploaded through the Android Publisher API and released to the **production** track, read back
  after commit as `1.17 (32)`, status `completed`; subject to Google review before it reaches users. Account ownership
  (corporate, `sales@tertiarycourses.com.sg`) is from the saved transfer note, not re-checked in the console.
- The App Review demo password stored in `~/.claude/.env` was refused by production (`CredentialsSignin`) for both
  `angch+appreview@` and `appreview@tertiaryinfotech.com`; the account was not changed. Check Play's App access sign-in
  details before the review needs them.

## [1.16] — 2026-11-20 · versionCode 31

### Release notes (Play, en-US)
- Calendar: leave is colour-coded by type with an emoji (annual, medical, no pay, other). Approved time off and Off Days now show.
- Calendar: off days are shaded. Approvers see birthdays and, for one person, their leave and time off totals for the month.
- My Profile: a new Work Schedule card with your working hours, working days and off days.
- Home: your leave summary as on the web; finance figures for accountants and trainer counts for Trainer Admins.
- No Pay Leave is now 14 days a year.

### Added
- **Calendar colours** (`ui/screens/TeamCalendarScreen.kt`, `data/CalendarLogic.kt`): leave chips and agenda bars use the
  colour of their `category` from `/api/mobile/team-calendar` (`categories` legend, with the web table hard-coded as
  `CalendarCategories.FALLBACK`), with the category emoji in front (🏖️ annual, 🤒 medical, 💸 no pay, 📋 other). A
  colleague's masked leave (`category` null) keeps the neutral You / Colleague colour and no emoji. Category chips are
  the web's tinted style (light tint, darker ink in light, the colour itself in dark). Holiday titles arrive with their
  emoji and are shown as sent.
- **Time off and Off Days** on the calendar from the new `timeOff` field (⏰, Time Off colour): "Off Day" for a full day,
  else "Time off 2:00 PM – 4:00 PM"; approvers see whose it is. It follows Everyone / Only me and the person filter.
- **Birthdays** for approvers (`birthdays`, 🎂 "NAME birthday", no age), following the person filter.
- **Off-day shading** (`OffDays`): when one schedule applies (your own `ownWorkdays`, or the person an approver picked;
  none for Everyone) a day is shaded if its weekday is not a workday (Mon–Fri when empty) or it has an approved full-day
  Off Day for that person. The legend then shows an "Off day" swatch.
- **Month totals** for the person an approver picked: Annual / Medical / No Pay Leave in days, Other Leave when above 0,
  and Time Off in hours plus Off Days. `CalendarSummary.personMonthSummary` is a 1:1 port of the web
  `personMonthSummary` (leave crossing a month is split by working days, public holidays excluded).
- The legend lists the categories shown, and wraps. The person filter reads "Leave & time off for" and counts both.
- **My Profile → Work Schedule** card: Title, Department, Working hours, Working days, Off days (new `workdays`,
  `workingDays`, `offDays`, `officeHours` profile fields); hidden on an older server.
- **Home**: staff and interns see the server's `leaveSummary` tiles as given (label, value, hint; interns' MC tile says
  sick leave is No Pay Leave), replacing the local AL / MC tiles; accountants get Pending expense claims, Income and
  Expenses for the month (`financeKpis`); Trainer Admins get Trainers / ACLP / Non-ACLP counts (`trainerKpis`) that
  open Trainer Management. Without those fields Home is as before. `StatTile` takes an optional hint line.

### Changed
- **No Pay Leave** is a flat 14 days a year (no proration): the Leave and Apply Leave screens show the server's cap and
  say "capped at N days a year"; the old "21 days, prorated" wording is gone.
- Debug mock: categories, time off, birthdays, workdays, Home tiles and the profile schedule.

## [1.15] — 2026-11-20 · versionCode 30

### Release notes (Play, en-US)
- Calendar: approvers can show leave for one person. The list has every active staff member and intern, grouped, with their number of leave entries.
- Analytics: new workforce breakdown by gender, age group and ethnic group, for staff, interns or everyone.
- Trainer Management: Total, ACLP and Non-ACLP trainer counts at the top, with an ACLP / Non-ACLP / All filter. The list opens on ACLP trainers, sorted by name.
- Team: approvers see Staff and Interns as two lists.
- Home: approvers see full-time staff, interns and pending approvals at a glance.

### Added
- **Calendar "Leave shown for"** (`ui/screens/TeamCalendarScreen.kt`, `data/CalendarPeople.kt`): approvers pick Everyone or
  one person. The list is the new `people` field of `/api/mobile/team-calendar` (every active staff member and intern,
  with or without leave), grouped Staff then Interns, each with their leave-entry count. People who have left are not
  listed; their leave still shows under Everyone. Holidays always show. When `people` is absent (an older server) an
  approver's list is built from the leave entries; non-approvers get no filter.
- **Analytics workforce breakdown** (`AnalyticsScreen.kt`, `Demographics` in `data/Analytics.kt`): three donuts (Gender,
  Age group, Ethnic group) with counts and percentages, and a Staff / Interns / All control (Staff first), from the new
  `breakdown` field of `/api/mobile/analytics`. A note says how many people have no ethnic group recorded. The section
  is hidden when the field is null or absent.
- **Trainer Management tiles and ACLP filter**: Total trainers, ACLP trainers, Non-ACLP trainers, counted on the device
  over the whole directory (`TrainerSearch.stats`); applicants are not counted. A new ACLP / Non-ACLP / All control
  with counts (`AclpFilter`) sits under the status tabs. The screen opens on ACLP and the list is sorted A to Z by
  name (`TrainerSearch.directory`). The tiles set the same filter (Total = All) and clear the others; the active tile
  is highlighted. Non-ACLP means "ACLP" is not in `trainingCerts`. ACLP is no longer in the Accreditation menu.
- **Team**: Staff / Interns tabs with counts for approvers (default Staff), from the new `intern` field of
  `/api/mobile/employees`. One list, as before, when the field is null or absent.
- **Home**: the approver row is now three figures, Full-time staff, Interns and Pending approvals (leave and MC plus
  claims), matching the web dashboard. The counts come from `/api/mobile/analytics`; if that call fails or has no
  counts, the previous Pending leave / Pending claims tiles show instead.
- `KpiStrip` figures can be tapped (`Kpi.onClick`), can show as the active one (`Kpi.selected`), and their labels may
  wrap to two lines.
- Debug mock (`app/src/debug`): analytics, the calendar roster, intern flags and two more trainers, so approver screens
  can be previewed with `ui_preview` = admin.

### Changed
- The calendar's All / Staff / Interns control (1.14) is replaced by the person filter.

### Play submission
- Uploaded through the Android Publisher API on 2026-11-20 and released to the **production** track
  (status `completed`, read back as `1.15 (30)`); subject to Google review before it reaches users.
  Account ownership (corporate, `sales@tertiarycourses.com.sg`) is from the saved transfer note, not re-checked in the console.
- The web build that adds `people`, `breakdown` and `intern` (web `144d203`) was live before the upload.

## [1.14] — 2026-11-20 · versionCode 29

### Release notes (Play, en-US)
- New Analytics screen for approvers (More → Manage): 12 months of revenue and expenses, leave and MC per person, and headcount, with filters.
- Calendar: approvers can show leave for All, Staff or Interns.
- Notifications on this phone now register correctly, including approvals and decisions on leave, MC, time off and expense claims.

### Added
- **Analytics** (`ui/screens/AnalyticsScreen.kt`, More → Manage → Analytics, approvers only): `GET /api/mobile/analytics`,
  the same data as the web `/dashboard/analytics`. One filter block (3 / 6 / 12 months, All / Staff / Interns, person,
  leave type) drives the six tiles and all three charts: Revenue and expenses (grouped bars + a category list, tap a
  category to hide it), Leave and MC (stacked bars + By person, tap a name to slice), Number of staff (stacked bars +
  change by month). Tap a bar for its figures. Filtering and totals live in `AnalyticsCalc` (`data/Analytics.kt`).
- **Calendar**: All / Staff / Interns filter on team leave for approvers, from the new `group` field on
  `/api/mobile/team-calendar` entries. Hidden when no entry carries a group (non-approvers, older server).
- Debug builds only: "Analytics (sample data)" under More for non-approvers, and a sample-data fallback, so the screen
  can be checked with the App Review login.

### Fixed
- **Push registration never reached the server.** `PushTokenBody.platform` had a default value, which
  kotlinx.serialization leaves out of the JSON; the server requires `platform` and answered 400, and the app ignores a
  failed registration. No Android phone was registered in production. The field is now always sent
  (`HrmsApi.PUSH_PLATFORM`), with a unit test on the encoded body.

### Play submission
- 2026-11-20: uploaded through the Publisher API and released to the **production** track as "1.14 (29)", status `completed` (read back after commit), corporate account sales@tertiarycourses.com.sg. The Analytics screen needs the web build that adds `/api/mobile/analytics`.

## [1.13] — 2026-11-19 · versionCode 28

### Release notes (Play, en-US)
- New Appearance setting in My Profile: choose Light, Dark or System. System follows your device.

### Added
- My Profile → Appearance: Light, Dark or System. Default is System; the choice is saved on the device
  (`hrms_appearance` SharedPreferences) and applies at once, with no restart. Two accounts start on Light
  (`Appearance.LIGHT_DEFAULT_EMAILS`) when nothing is saved.
- Dark palette matching the web app (`#030712` page, `#111827` cards and bars, near-white text,
  royal-300 accent text, lightened status colours); a dark launch splash on Android 12+ dark devices.

### Changed
- `Brand.*`, `StatusTint`, `IconTint` and `BannerTint` are theme-aware getters (`ui/theme/Appearance.kt`).
  Accent text and icons use `Brand.Accent` instead of `Brand.Premier`; system-bar icons follow the theme.
- Replaces the one-light-theme rule of 10 Nov 2026. Light is unchanged.

### Play submission
- 2026-11-19: uploaded through the Publisher API and released to the **production** track as "1.13 (28)", status `completed` (read back after commit), corporate account sales@tertiarycourses.com.sg.

## [1.12.3] — 2026-11-13 · versionCode 27

### Release notes (Play, en-US)
- Interns: request a full-day Off Day in Time Off. Once approved, no clock-in or clock-out is needed that day.

### Added
- Request Time Off: "Off Day" reason (first segment). Picking it hides the start/end times; the app sends 00:00–23:59 and the server stores a 0-hour full day (`OFF_DAY`).
- `TimeOffRequest.offDay` / `isOffDay` (server sends an Off Day as reason `OTHERS` + `offDay: true` so older builds keep decoding); `LeaveSummaryTimeOffRequest.offDay`.

### Changed
- Time Off lists, cancel dialog and Leave Summary show an Off Day as "Full day" / "Day" instead of times and hours.

## [1.12.2] — 2026-11-11 · versionCode 26

### Release notes (Play, en-US)
- Calendar redesigned like the Planner app: month grid with agenda below.

### Changed
- Team calendar follows the Planner app's layout: Month / List toggle, month title with ‹ ›, a
  Monday-first edge-to-edge grid with thin separators, adjacent-month days greyed, up to two chips
  per day plus "+N", today as a filled premium-blue circle on a tinted cell, tap to select a day, and
  the day's agenda (leave and holidays as cards) below. List mode shows upcoming leave and holidays
  grouped by day. Everyone / Only me and the grey public-holiday cells with the name are kept.

## [1.12.1] — 2026-11-11 · versionCode 25

### Release notes (Play, en-US)
- Clearer More menu: Applications, Staff and Intern sections.

### Changed
- More is grouped as Applications (MC, Leave, Time Off, Expense Claims, Off In Lieu for staff),
  Staff (Calendar, Payslips, My Profile, Trainers), Intern (Time In/Out and Timesheet — interns and
  admins only), then Manage.

## [1.12] — 2026-11-11 · versionCode 24

### Release notes (Play, en-US)
- Push notifications for approvals, clock-in/out and timesheet reminders.
- Trainer contact list under More for all staff.
- Calendar weeks start on Monday, with all public holidays shown.
- Clearer white initials on avatars.
- New premium-blue app icon, matching the TC LMS SG app.

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
