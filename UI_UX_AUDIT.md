# UI/UX Completeness Audit — Native Android vs. Web Reference

Every native screen compared directly against its real implementation in `tertiary-hrms`
(`C:\ITP\Tertiary Infotech\tertiary-hrms`) — the actual React/Next.js components, which is also
what the Capacitor wrapper (`tertiary-hrms/capacitor.config.ts` + `mobile/android`) renders,
confirming that "match the Capacitor app" and "match the web app" are the same target. This is a
**functional/structural completeness** audit — colors, fonts, and spacing are covered separately
in [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md); screen-by-screen expected behavior is in
[SCREEN_MAP.md](SCREEN_MAP.md). Admin/accounting-only web features are correctly out of scope per
`CLAUDE.md` and are not counted as gaps below.

## Highest-priority gaps

Ranked by how much real functionality is missing, not by effort to fix:

1. **Apply for Leave form is missing most of its logic**, not just the MC upload: no MC document
   upload, no AL Off-In-Lieu offset panel/slider, no working-days preview, no half-day-on-multi-day
   support, no AL_OT balance validation, weak submit-button gating. This is the single biggest gap
   in the app — see §4.
2. **No Edit or Cancel flow for Leave or Expense requests.** Once submitted, a PENDING leave or
   expense claim can never be corrected or withdrawn from the native app — no `/leave/edit/[id]`
   or `/expenses/edit/[id]` equivalent exists at all.
3. **Woods Square status is actively wrong, not just unstyled.** The API always returns raw
   `status: "SENT"` for every invite; the web app computes Activated/Scheduled/Expired from the
   date range and never shows "Sent". Native's `StatusPill` has no case for `"SENT"` and falls
   through to a generic tint — every pass, active or long-expired, shows the same meaningless
   "Sent" pill. The entire hero "digital pass" card UI (status word, countdown, progress bar) is
   also missing — see §7.
4. **Dashboard has no admin/manager variant.** Only the staff 4-tile layout exists; an admin never
   sees Pending Leaves/Pending MC/Pending Claims or the admin Quick Actions — see §2.
5. **Payroll is missing Gross Salary and both CPF figures** (CPF Employer isn't even fetched into
   the model), and there's no way to save/export a payslip PDF from the device — see §6.
6. **Login has no Google Sign-In** — though this is a known, previously-deferred decision (needs an
   Android OAuth client registered against the app's signing SHA-1), not a new finding.

---

## 1. Login

**Kotlin:** `ui/LoginScreen.kt`, `ui/AuthViewModel.kt`
**Web:** `src/app/(auth)/login/page.tsx`

- **No Google Sign-In** — divider ("Or continue with"), Google button, and the whole OAuth code
  path are absent. (Previously deferred pending an Android OAuth client — not new, but still a gap
  vs. the reference.)
- No OAuth error-code mapping (`OAuthSignin`/`OAuthAccountNotLinked`/etc.) — moot without Google
  sign-in, but note the whole error surface doesn't exist natively.
- **"Email delivery not configured" fallback copy is lost.** Web shows
  `"OTP generated. Email delivery not yet configured — please use password login or ask your
  admin."` when `emailConfigured === false`; native always shows the same hardcoded "We emailed a
  6-digit code..." regardless of what the API reports.
- **No resend-in-progress state.** Web disables "Resend OTP" and shows "Resending..." for 5s.
  Native's resend link is always tappable with no in-flight indicator.
- Resend success message should be distinct copy ("A new OTP has been sent...") — native reuses
  the initial-send message.

## 2. Dashboard

**Kotlin:** `ui/screens/DashboardScreen.kt`
**Web:** `src/app/(dashboard)/dashboard/page.tsx` + `stats-cards.tsx`, `quick-actions.tsx`,
`recent-activity.tsx`

- **No admin/manager dashboard variant at all.** Web shows completely different stat cards for
  admins — Pending Leaves / Pending MC / Pending Claims — plus a view-toggle to switch between
  admin/staff views. Native hardcodes only the staff layout.
- **"Off In Lieu" tile should be conditional**, not always shown. Web only renders it when
  `otEarned > 0 || otRemaining < 0` (grid reflows 3↔4 columns). Native always renders it, falling
  back to "—".
- **Quick Actions has no admin variant** (Create Staff / Manage Leaves / Manage Claims / View
  Calendar) — native always shows the 3 staff actions.
- Extra "My balances" section header and a role-badge pill exist natively with no web counterpart
  in that position — not wrong, just added copy/structure the reference doesn't have.
- `RecentActivity` (expense-only, ignoring `leaves`) actually **matches** web's real behavior —
  web's own component silently ignores its `leaves` prop too. Not a gap.

## 3. Leave — Annual / Medical (list + balances)

**Kotlin:** `ui/screens/LeaveScreen.kt`
**Web:** `leave/annual/page.tsx`, `leave/medical/page.tsx`, `components/leave/leave-balance-cards.tsx`,
`components/leave/leave-list.tsx`

- **No page subtitle** under the header (role/leave-aware description text on web).
- **No Paid/Unpaid status pill row** at the top — web shows split "{N} days Paid" + "{N} days
  Unpaid" pills for partially-paid MC, and intern-specific "Unpaid Leave" messaging; native only
  has a per-card boolean tag.
- **No carry-over expiry warning banner** (Q4-only, Annual Leave page).
- **No AL Off-In-Lieu balance card** (Earned/Used/Deficit/Remaining + `OtBreakdownDialog`) —
  entirely absent from the Leave screen (Off In Lieu is dashboard-only in native).
- **Balance cards are a different, thinner feature set.** Missing: "Leave(s) Rejected" count,
  the Earned-to-Date vs. Yearly-Entitlement vs. Remaining distinction (native conflates all three
  into one `available` number), the monthly-accrual-breakdown drill-down modal, and the
  Advance-vs-Deficit labeling for negative balances.
- **No admin KPI row** (Total/Pending/Approved/Rejected) — no admin mode exists for this screen.
- **Request list is read-only.** Missing entirely: search, status filter, date-range filter,
  sortable columns, "Applied" date, document/MC preview button, OT-used/deficit badges,
  approval-comment/rejection-reason display, per-row **Edit**/**Cancel** (staff), per-row
  **Approve**/**Reject**/**Reset** (admin), bulk-select + bulk actions.
- **No Edit-leave-request flow at all** — no `/leave/edit/[id]` equivalent screen or route.

## 4. Apply for Leave

**Kotlin:** `ui/screens/ApplyLeaveScreen.kt` (`ApplyLeaveSheet`)
**Web:** `leave/request/page.tsx` + `leave-request-form.tsx`

- **No Medical Certificate upload** (the original reported gap, confirmed) — native shows a static
  "email HR" text hint instead of the real required file-upload control (image/PDF, max 5MB,
  wired to `POST /api/upload`), and it's positioned differently (right after the type picker
  instead of near the end of the form).
- **Leave-type pre-selection ignores context** — web supports `?type=MC` from the "Request MC"
  button; native always defaults to AL regardless of which tab the user came from.
- **Dropdown options are missing their day-count suffix** (`"{name} ({N} days available)"` on web).
- **No computed "Number of Working Days" preview**, no calendar-days→working-days breakdown text,
  no zero-working-days warning.
- **Day-type (Full/AM/PM) control shown for the wrong scope** — web only shows it for AL; native
  shows it for any single-day request including MC.
- **No multi-day half-day support** (web's "Include a half-day?" toggle + first/last-day picker).
- **Entire AL Off-In-Lieu offset panel is missing**: Leave Balance Summary box, the OT-offset
  toggle, the day-amount slider, and the Earned/Advance/OIL-used/Deficit breakdown panel.
- **No "Insufficient Off In Lieu" validation/banner** for AL_OT type.
- Reason field is single-line `TextField`, not a multi-line textarea.
- **Submit button lacks deficit-aware label** (`"Submit with {N}d deficit"`) and proper gating —
  native only requires a non-empty type, not valid dates/day-count/OT-balance.
- No explicit in-form Cancel button (only the screen back-arrow).
- Field order differs from web's Type → Dates → Day-type → Working-days preview → OT
  banner → AL balance panel → Reason → MC upload → Cancel/Submit.

## 5. Expenses (list / submit / edit)

**Kotlin:** `ui/screens/ExpensesScreen.kt`, `AddExpenseScreen.kt`
**Web:** `expenses/page.tsx`, `expense-list.tsx`, `expense-submit-form.tsx`, `expense-edit-form.tsx`

- **Stat card**: labeled "Approved Total" instead of "Total Expenses", missing the "{count} claims"
  sub-line.
- **No search, no date-range filter, no status filter** on the list.
- **No receipt viewer** — `receiptUrl` exists on the model but is never rendered as a tappable
  action, even though it's fetched.
- **No Edit flow at all** for a PENDING claim — biggest single gap in this section.
- **No Cancel action** for a PENDING claim.
- **No "Payment Received" acknowledge action** for an APPROVED claim.
- **`ExpenseCategory` model is missing `maxAmount` and `requiresReceipt` fields entirely** — so the
  "Max claimable: $X" hint and mandatory-receipt validation can't be implemented without a
  data-layer change first.
- Description field is single-line, not a multi-line textarea.
- No explicit in-form Cancel button.

## 6. Payroll

**Kotlin:** `ui/screens/PayslipsScreen.kt`, `PayslipPdfScreen.kt`
**Web:** `payroll/page.tsx` + `payroll-list.tsx` (staff branch)

- **No "My Payroll Folder" button** (links to the employee's Drive folder) — likely intentional
  given no Drive integration exists, but flagged since it's a staff-visible web control.
- **No status filter** on the list.
- **Gross Salary and both CPF figures are missing from the row/card.** `cpfEmployer` isn't even in
  the Kotlin `Payslip` model — that data isn't fetched at all. `cpfEmployee` is fetched but unused.
- Native shows a status pill (DRAFT/GENERATED/PAID) that the web list doesn't actually render for
  this view — extra info, not missing info, but worth knowing about.
- **No pagination** (web paginates at 50/page).
- **No download/save/share action for the PDF** — `PayslipPdfScreen` only supports in-app viewing
  via `PdfRenderer`, there's no way to export the file off-device.

## 7. Calendar

**Kotlin:** `ui/screens/CalendarScreen.kt`
**Web:** `calendar/page.tsx` + `calendar-view.tsx`

- **No month-grid view** — web renders an actual 7-column calendar grid; native is a flat agenda
  list grouped by month heading. Structurally different browsing model, not a styling issue.
- **No month navigation** (prev/next/"Today") — native just lists everything the API returns.
- **No event-type legend card** (color key for Holiday/Meeting/Training/Company Event/Leave).
- **No tap-through to a day-detail view** — web's day cells link to `/calendar/day/[date]`; native
  rows aren't tappable.

## 8. Timesheet

**Kotlin:** `ui/screens/TimesheetScreen.kt`
**Web:** `timesheet/page.tsx` + `weekly-timesheet.tsx`

- Page header title/subtitle differs — web: "Weekly Timesheet" + a description under the title;
  native: "Timesheet" with no header subtitle (only the closing footnote survives).
- **No live per-row Off-In-Lieu preview while a day is still editable** — only shown once a day is
  no longer submittable.
- **No "Today" badge/highlight** on the current date's row.
- Mobile row values have no field labels (web explicitly labels "Hours"/"Off In Lieu"/"Status").
- Status copy: web shows "Pending approval" for PENDING; native's shared `StatusPill` shows plain
  "Pending".
- Confirm-submit dialog is missing its warning icon (copy text matches).
- Clock-in/out card is confirmed **intentional native-only functionality** — not a gap.

## 9. Woods Square Access

**Kotlin:** `ui/screens/WoodsSquareScreen.kt`
**Web:** `woods-square-access/page.tsx` + `woods-square-access-card.tsx`, `request-access-button.tsx`

The largest gap of the read-only screens — web is a purpose-built "digital pass" UI; native is a
raw list of the two backing arrays.

- **Status is actually wrong, not just unstyled** (see Highest-priority §3) — every invite shows a
  meaningless "Sent" pill regardless of whether the pass is active, upcoming, or expired.
- **Entire hero pass-card UI is missing**: branding, holder avatar/name/ID, computed status word
  (Activated/Scheduled/Expired/Not activated), countdown text, validity date range with progress
  bar, "Entry via PIN" caption.
- No "Mobile Access Card Info" dialog, no standing PIN-reminder banner.
- **No "Other passes"/"My requests" tabs with counts** — flat, unlabeled sections instead.
- Other-pass rows show only raw status + date range, missing the computed Active-now/Upcoming/
  Expired pills and countdown text.
- **No roster gating** on the Request button (web hides it entirely if not on the invite roster).
- **No pending-request cap enforcement** (web disables Request + shows "Max N pending requests
  reached").
- No date-preset quick-select chips in the request dialog.
- **No max-window validation UI** (live "{N}-day window" feedback, over-limit warning, Send
  disabled when over) — native only checks "both dates or neither".
- Copy differences: intro text, note placeholder, submit button label ("Send request" vs "Submit
  request").
- **No cancel-confirmation dialog** — native cancels immediately on tap, no undo step.
- **Request status set is incomplete** — web has Pending/Fulfilled/Declined/Expired (client-computed
  "Expired" for a lapsed PENDING request); native has no FULFILLED case and never recomputes
  Expired, so a stale request still shows "Pending" *and* still shows a cancel button.
- Empty-state copy/structure differs (web has one combined empty state pointing at the Request
  button; native has two independent generic empty hints).

## 10. Profile (view / edit)

**Kotlin:** `ui/screens/ProfileScreen.kt`, `EditProfileScreen.kt`
**Web:** `profile/page.tsx` → `employee-detail-editable.tsx`, `password-change-card.tsx`, `theme-card.tsx`

- **Self-edit of Employment Details is missing.** The web backend explicitly allows a signed-in
  employee (`isSelf`) to PATCH their own `employmentInfo`/`roles` — Job Function, User Type
  (role toggle chips), Start/End Date, Employment Type, Workdays, Status. Native's edit form
  deliberately only submits personal-info fields (this may be an intentional native scoping
  decision, but it's a real behavior difference from the actual web permission model — worth an
  explicit decision either way, not a silent omission).
- **No avatar/photo upload** in edit mode (web: camera-icon overlay → file picker → `/api/upload`).
- **"End Date" and "Workdays" aren't shown anywhere**, even read-only.
- Field grouping differs from web's clean Personal-Info-card / Employment-Details-card split —
  native mixes personal and employment fields across its two cards differently.
- Copy: web labels the department field "Job Function"; native labels it "Department". Web's edit
  entry point is a labeled "Edit Employee" button; native is an icon-only pencil.
- Email masking (`.noemail@` placeholder → "—") isn't ported; native always shows the raw email.
- **Password change screen matches web at parity** — no gaps found there.
- **Entire "Theme" card is missing** (Dark/Light toggle) — may be intentional since the app is
  dark-only by design, but it's a control the web reference has that native drops entirely.

## 11. Notifications

**Kotlin:** `ui/screens/NotificationsScreen.kt`
**Web:** `components/layout/notification-bell.tsx` (dropdown — closest web equivalent; there's no
separate full-page notifications view on web to compare 1:1 against)

- **No "Mark all read" bulk action** — `HrmsApi.kt` has no mark-all-read call at all, only
  per-item marking.
- **No unread-count indicator on the screen itself** (web shows "{N} new" next to the heading;
  native only badges the bell icon in the top bar, not the list screen's own header).
- **No footer shortcut link** ("View leave page →").
- Type-filter set (`STAFF_NOTIFICATION_TYPES`) matches web's `EMPLOYEE_TYPES` exactly — no gap.
- Native's per-type route mapping is actually broader/more correct than web's simple `router.push`
  — a positive difference, not a gap.

## 12. Shared chrome — drawer / top bar vs. sidebar / header

**Kotlin:** `ui/screens/MainScaffold.kt`, `ui/components/BrandComponents.kt` (`TopBarActions`)
**Web:** `components/layout/header.tsx`, sidebar component, `mobile-bottom-nav.tsx`, `notification-bell.tsx`

- **No "Refresh data" button** in the top bar (web has a dedicated `RefreshCw` action).
- **No theme toggle** in the top bar (ties back to the missing Profile Theme card — app is
  currently dark-only by design).
- Profile dropdown menu (name/email header, My Profile, Sign out) **matches web at parity** — no
  gap, already fixed this session.
- **Drawer header has a typo**: "Tertiary Infotech **Acadmey**" (missing the "e"), and it's a
  hardcoded string rather than driven by `companyShortName`/`companyLogo` from settings the way
  web's sidebar is.
- **No version/build-info footer** in the drawer (web shows `version {date} ({commit})`).
- **Nav icon mismatches**: Timesheet (web `ClipboardList` vs. native clock icon), Woods Square
  (web `KeyRound` vs. native `MeetingRoom`), Leave (web `Clock` vs. native `Schedule`) — visually
  similar but different icon assets.
- **Leave nav item loses its web sub-navigation** (web nests "Annual Leave"/"Medical Leave" as
  expandable children under "Leave"; native's drawer treats Leave as one flat destination — the
  in-screen AL/MC tab inside `LeaveScreen` does cover this at the screen level, just not at the
  drawer/nav level).
- Nav item list/order otherwise matches exactly for a staff view (Dashboard → My Profile → Leave →
  Expense Claims → Payroll → Calendar → Timesheet → Woods Square Access).
- Bottom-tab substitution of Profile for web's finance-only Accounting tab is a reasonable,
  intentional structural difference given the staff-only scope — not a gap.
