# Tertiary HRMS — Design System Reference

Source of truth: the web app at `tertiary-hrms` (Next.js 14 + Tailwind + shadcn/ui,
`hrms.tertiaryinfo.tech`). This document captures its design tokens, component conventions, and
full screen inventory so the native Android and iOS clients can be pixel/behavior-matched. Per
`CLAUDE.md`, the web app is always the design source of truth — when it changes, this doc and the
native apps should follow.

## 1. Color tokens

Web is dark-first: hardcoded Tailwind `gray-950/900/800/700` classes rather than the shadcn
semantic tokens for most surfaces (a light theme exists and remaps those specific classes at
runtime via `html.light` overrides in `globals.css`).

| Token | Dark (default) | Light | Android (`Brand`) |
|---|---|---|---|
| Page background | `bg-gray-950` `#030712` | `#ffffff` | `Brand.Background` |
| Card/section surface | `bg-gray-900` `#111827` | `#f8fafc` | `Brand.Surface` |
| Elevated/secondary surface | `bg-gray-800` `#1F2937` | `#f1f5f9` | `Brand.Border` |
| Hover surface | `bg-gray-700` `#374151` | `#e2e8f0` | `Brand.BorderLight` |
| Primary / accent | `#6366F1` (indigo-500, HSL `239 84% 67%`) | same | `Brand.Primary` |
| Text primary | `text-white` | `#0f172a` | `Brand.TextPrimary` |
| Text secondary | `text-gray-400` `#9CA3AF` | `#475569` | `Brand.TextSecondary` |
| Text muted | `text-gray-500` `#6B7280` | `#64748b` | `Brand.TextMuted` |
| Border | `border-gray-800` | `#e2e8f0` | `Brand.Border` |
| Destructive | `#EF4444` (red-500) | same | `Brand.Red` |

shadcn semantic tokens (`tailwind.config.ts` → `globals.css` HSL vars) exist in parallel for
shadcn primitives themselves (`--background`, `--card`, `--primary`, `--border`, `--ring`, etc.)
and always resolve `--primary` to the same indigo (`239 84% 67%`) in both themes. `--radius` is
`0.75rem` (12dp) — matches `Brand.Corner` on Android.

Status accent colors (solid, used for icons/values/calendar tints):

```
Amber #F59E0B   Green #22C55E   Red #EF4444
Blue  #3B82F6   Purple #A855F7  Emerald #10B981
```

## 2. Typography

- **Font**: Inter, loaded via `next/font/google` (`layout.tsx:2,9`, `subsets: ["latin"]`), applied
  as the body font everywhere — no per-section font swaps.
- **Weights in use**: `font-normal` (body), `font-medium` (labels/nav), `font-semibold` (card
  titles, emphasis), `font-bold` (KPI numbers, headings).
- **Scale**: `text-xs` (11–12px, meta/timestamps), `text-sm` (14px, body/table default), `text-base`
  (16px), `text-lg`/`text-xl` (section headers), `text-2xl` (`CardTitle` default, KPI numbers),
  `text-3xl`+ (page hero headers only).

## 3. Spacing, radius, elevation

- **Card padding**: real feature cards mostly use `p-4`–`p-5` (48 occurrences), not the shadcn
  `Card` default of `p-6` (33 occurrences, reserved for spacious/marketing contexts). Android's
  `PremierField`/`Card` components should default to a 16dp (`p-4`) equivalent.
- **Grid/section gaps**: `gap-4` dominates (51 uses) over `gap-6` (8, reserved for large
  section-level grids like the dashboard's 3-column layout). Stat-card grids commonly use
  `gap-3 sm:gap-4`.
- **Page container**: dashboard pages are full-bleed, no `max-w-*` wrapper — `<main>` uses
  `py-6 px-4 pb-20 sm:px-6 lg:px-8 lg:pb-6` (the `pb-20`/mobile-nav clearance is why Android screens
  need bottom-safe-area padding too). Narrow single-column forms (e.g. `employees/new`) opt into
  `max-w-4xl mx-auto space-y-6`.
- **Border radius**: `rounded-lg` (12px, buttons/inputs/selects — Android's `Brand.Corner`),
  `rounded-xl` (cards, icon swatches, dropdown panels), `rounded-2xl` (larger feature panels/modals,
  e.g. notification dropdown, some Woods Square panels), `rounded-full` (avatars/pills/badges/dots).
- **Elevation**: no layered shadow scale — **flat/bordered surfaces for static content**
  (`rounded-xl border border-gray-800 bg-gray-950`). `shadow-*` is reserved almost exclusively for
  **floating/overlay surfaces**: popovers (`shadow-md`), dropdown menus (`shadow-md`/`shadow-lg`),
  the slide-in sheet panel (`shadow-lg`), toasts (`shadow-lg`), the notification dropdown
  (`shadow-2xl`). Mirrors Android's own flat Material `containerColor` + border approach — no
  elevation/gradients anywhere in the app, matching `CLAUDE.md`'s "no gradients" rule.

## 4. Components (shadcn/ui, `src/components/ui/`)

| Component | Variants | Key classes |
|---|---|---|
| **Button** | `default`, `destructive`, `outline`, `secondary`, `ghost`, `link`, custom **`success`** (`bg-green-600 hover:bg-green-700`) | base `rounded-lg text-sm font-medium`; sizes `default` `h-10 px-4 py-2`, `sm` `h-9 px-3`, `lg` `h-11 px-8`, `icon` `h-10 w-10` |
| **Card** | — | `rounded-xl border bg-card shadow-sm`; `CardHeader` `p-6 space-y-1.5`; `CardTitle` `text-2xl font-semibold`; `CardContent`/`CardFooter` `p-6 pt-0` (real usage overrides to `p-4`–`p-5`, see §3) |
| **Badge** | `default`, `secondary`, `destructive`, `outline`, custom **`success`**/`warning`/`info` | `rounded-full border px-2.5 py-0.5 text-xs font-semibold`; success `bg-green-100 text-green-800 border-green-200` (light-pastel triad, see §5) |
| **Input / Select trigger** | — | `h-10 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm` |
| **Tabs** | — | `TabsList` `h-10 rounded-lg bg-muted p-1`; `TabsTrigger` `rounded-md px-3 py-1.5`, active `bg-background shadow-sm` |
| **Avatar** | — | `h-10 w-10 rounded-full`; fallback `bg-muted` |
| **Dropdown menu** | — | `rounded-md border bg-popover p-1 shadow-md` (nested submenu `shadow-lg`); items `rounded-sm px-2 py-1.5 text-sm` |
| **Toast** | `default`, `destructive`, custom **`success`** | `rounded-md border p-6 pr-8 shadow-lg` |
| **Sheet** (stands in for a `Dialog` — no `dialog.tsx` file exists) | `side="left"/"right"` | `fixed inset-y-0 h-full w-72 border bg-white p-0 shadow-lg` slide-in panel, wraps Radix Dialog |

Notable gaps vs. a typical shadcn install: **no `dialog.tsx`, `table.tsx`, `skeleton.tsx`, or
`separator.tsx`** despite the Radix packages being installed — tables are hand-rolled `<table>`
markup per feature, loading states are custom pulse divs, and modals go through `sheet.tsx`
instead of a centered dialog.

## 5. Icons

Library: **lucide-react** (`^0.469.0`), used across 86 files. Sizing conventions:

| Context | Size |
|---|---|
| Sidebar top-level nav | `h-5 w-5 shrink-0` |
| Sidebar nested/chevron | `h-4 w-4 shrink-0` |
| Button icons | `h-4 w-4` (dominant — 263 occurrences) |
| Stat-card icons | `h-6 w-6 sm:h-7 sm:w-7` inside a `p-2.5 sm:p-3 rounded-xl` tinted swatch |
| Inline status/notification glyphs | `h-3 w-3`–`h-3.5 w-3.5` |
| Header bell (inside `h-8 w-8` button) | `h-5 w-5` |

## 6. Status/semantic color conventions

Two patterns coexist in the web app today — both should be treated as ground truth (Android's
`StatusTint`/`IconTint` objects already mirror pattern A):

**A — Light pastel badges** (leave/expense status tables, `leave-list.tsx` / `expense-list.tsx`):
```
PENDING    bg-amber-100 text-amber-800 border-amber-200
APPROVED   bg-green-100 text-green-800 border-green-200
REJECTED   bg-red-100   text-red-800   border-red-200
CANCELLED  bg-gray-100  text-gray-800  border-gray-200
PAID       bg-blue-100  text-blue-800  border-blue-200   (expenses only)
```

**B — Dark-tinted icon/text** (notifications, inline chips): `text-green-400` (leave approved),
`text-red-400` (rejected), `text-blue-400` (submitted), `text-emerald-400` (OT approved),
`text-amber-400` (OT pending), `text-purple-400` (Woods Square request); unread-count pill
`bg-red-500/20 text-red-400`.

**C — Stat-card icon swatches** (`dashboard/stats-cards.tsx`): same light-pastel-bg/darker-icon
pairing as A — `bg-amber-100`/`text-amber-600`, `bg-red-100`/`text-red-600`, `bg-purple-100`/
`text-purple-600`, `bg-blue-100`/`text-blue-600`, `bg-green-100`/`text-green-600`,
`bg-emerald-100`/`text-emerald-600`.

Note: patterns A/C use the light-mode `-100/-800` triad even inside the dark app shell — a known
inconsistency in the web app itself, not something to "fix" independently on native clients.

## 7. Screen inventory (`src/app/`)

Two root layouts: `(dashboard)`/`(auth)` share `src/app/layout.tsx`; `(myinfo)` (Singpass) defines
its own standalone `<html>/<body>` shell.

### Layouts
- **`layout.tsx`** — Inter font, theme/session/toast providers, PWA manifest/service-worker.
- **`(auth)/layout.tsx`** — pass-through, no chrome.
- **`(dashboard)/layout.tsx`** — desktop `Sidebar` (role/branding-aware, collapsible) + `Header`
  (user menu, avatar, admin view-mode switcher) + `MobileBottomNav` + `PullToRefresh` + footer.
- **`settings/layout.tsx`** — guards `/settings/*` to ADMIN + admin view; shared header.
- **`(myinfo)/layout.tsx`** — standalone dark shell for Singpass callback pages.

### Auth
- **`/login`** — email → OTP → password-fallback flow, Google OAuth, "remember email", dev
  skip-login shortcuts.

### Dashboard
- **`/`** — redirects to `/dashboard`.
- **`/dashboard`** — welcome header, role-aware `StatsCards` (admin: pending leave/MC/claims;
  staff: leave/MC/OT balances + expense total), `QuickActions` + `RecentActivity`.
- **`/pending-setup`** — holding page for authenticated users with no linked employee record.

### Profile
- **`/profile`** — `EmployeeDetailEditable`, `ThemeCard` (light/dark toggle), `PasswordChangeCard`.

### Employees (admin-facing; excluded from staff/intern native clients)
- **`/employees`**, **`/employees/[id]`**, **`/employees/new`**, **`/employees/new-intern`**.

### Leave
- **`/leave`** → redirects to `/leave/annual`.
- **`/leave/annual`** — CTA, carry-over expiry banner, `LeaveBalanceCards`, OT balance panel,
  admin KPI cards, `LeaveList`.
- **`/leave/medical`** — mirrors annual layout; paid/unpaid pills differ intern vs staff.
- **`/leave/request`** — `LeaveRequestForm`, accepts `?type=` preselect.
- **`/leave/edit/[id]`** — edit a pending request (owner-only, PENDING-only).

### Expenses
- **`/expenses`** — CTA, staff stats card or finance KPI row, `ExpenseList` with category filter.
- **`/expenses/submit`** — `ExpenseSubmitForm` with receipt upload.
- **`/expenses/edit/[id]`** — edit pending claim (owner-only).

### Payroll
- **`/payroll`** — `PayrollList` grouped by pay period; "Process Payroll" (finance) or "My Payroll
  Folder" Drive link (staff).
- **`/payroll/generate`** — admin processing tool (Excel upload + auto-generate).

### Calendar
- **`/calendar`** — `CalendarView` merging holidays/meetings/training/company events/leave.
- **`/calendar/day/[date]`** — day drill-down, events + people-on-leave cards.
- **`/calendar/new`**, **`/calendar/edit/[id]`** — `CalendarEventForm`.

### Timesheet
- **`/timesheet`** — staff/intern weekend & PH hour logging, `WeeklyTimesheet`.
- **`/timesheet/overview`** — admin-only compliance dashboard.

### Woods Square
- **`/woods-square`** — admin invite console (Send/Requests/Manage tabs).
- **`/woods-square/[id]`** — per-employee invite detail (admin-only).
- **`/woods-square-access`** — staff self-service: PIN/pass window, invite history, requests.

### Settings (ADMIN + admin-view only — excluded from staff/intern native clients)
- **`/settings`** → `/settings/company`; **`company`**, **`credentials`**, **`cron-jobs`**,
  **`email-templates`** (+ `[slug]`), **`leave-policy`**, **`payslip-template`**, **`webhooks`**.

### Accounting (excluded from staff/intern native clients)
- **`/accounting`**, **`/accounting/expense-tracking`**, **`/accounting/income-tracking`**.

### MyInfo (Singpass callback — own root layout)
- **`/myinfo/authorize`**, **`/myinfo/success`**, **`/myinfo/error`**.

### Standalone
- **`/privacy-policy`** — static public document, no dashboard chrome.

---

**Relevant to Android/iOS clients** (per `CLAUDE.md`, staff/intern only, no admin/accounting):
Login, Dashboard, Profile, Leave (annual/medical + request/edit), Expenses (+ submit/edit),
Payroll, Calendar, Timesheet, Woods Square (self-service `/woods-square-access` only).
