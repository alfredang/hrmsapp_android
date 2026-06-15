# Google Play submission — step by step

This app is built and signed; what remains is the Play Console setup, which is gated behind your
Google Play developer account. Two paths:

- **Path A (manual, recommended for the first release):** create the app and upload the AAB in the
  Play Console web UI. ~30–45 min. No extra setup.
- **Path B (automated, for future updates):** set up a Google Cloud **service account**, grant it
  access in Play Console, and Claude can then push AABs via the Play Developer API
  (`androidpublisher`). The first app **must still be created in the UI** — the API cannot create a
  brand-new listing.

The artefact to upload is:
```
app/build/outputs/bundle/release/app-release.aab   (versionCode 1, versionName 1.0)
```

---

## 0. Prerequisites (one-time, yours)
- A **Google Play Console** developer account ($25 one-time, already paid in your case).
- A **privacy policy URL** that's publicly reachable. The web app already states the data practices;
  a short page at e.g. `https://hrms.tertiaryinfotech.com/privacy` (or a Google Site) is enough. It
  must mention: what's collected (work email), why (authentication), that it's not sold/shared, and
  how to request deletion (contact HR).
- A **demo account** (test employee email + password) for the review team — App Review cannot get
  past the login without one. Same as Apple required.

## 1. Create the app
Play Console → **Create app**:
- App name: `Tertiary HRMS`
- Default language: English (United States)
- App or game: **App**
- Free or paid: **Free**
- Declarations: confirm it meets policies + US export laws.

## 2. App signing
Accept **Play App Signing** (default). Upload our key as the **upload key**. Google holds the final
app-signing key; we sign uploads with `keystore/upload-keystore.jks`.
- Upload key SHA-1: `22:49:5D:BA:D0:98:76:41:E5:7C:AA:3B:47:BA:AF:36:9B:EC:60:4D`
- Upload key SHA-256: `CF:36:3B:C6:C9:C7:5E:DB:1A:EA:F5:56:7D:ED:DA:7E:8B:6A:65:AC:C2:09:D8:61:5A:A1:3C:42:CF:1B:54:06`
(If you ever enable Google Sign-In, register the **Play app-signing** SHA-1 shown in Console →
Setup → App signing, not just this upload SHA-1.)

## 3. Store listing
Copy everything from `store/STORE_LISTING.md`. Upload:
- App icon: `store/play_store_icon_512.png`
- Feature graphic (1024×500): **to be produced** (Premier Blue banner + wordmark).
- Phone screenshots (≥2): `store/screenshots/` has the login screen; add Dashboard/Leave/Payslips
  captures once you can sign in with the demo account (I can capture these on the emulator with a
  working credential).

## 4. App content (left nav → Policy → App content)
Complete each declaration:
- **Privacy policy**: paste the URL from step 0.
- **Data safety**: use the table in `store/STORE_LISTING.md` (Email → App functionality, linked, not
  tracking, not shared, encrypted in transit).
- **Ads**: No ads.
- **Content rating**: fill the IARC questionnaire (business app, no objectionable content).
- **Target audience**: 18+ (employees); not directed at children.
- **Government apps / financial features**: No.
- **News app**: No.

## 5. Create a release
Start with **Testing → Internal testing** (fastest; installable by you + testers immediately while
review is light), then promote to **Production** when happy.
- Release → **Create new release** → upload `app-release.aab`.
- Release name: `1.0 (1)`. Release notes: "First release of the native Tertiary HRMS Android app."
- Add the **demo account** credentials in the review notes (for Production: Production → review
  notes; for testing tracks, add testers by email).

## 6. Roll out
Submit for review. Internal testing is usually available within minutes; Production review for a new
app typically takes a few hours to a couple of days.

---

## Path B — automated uploads (chosen)
A ready-to-run uploader lives at `store/upload_to_play.py` (Android Publisher API v3). Setup:

1. **Create the app once in the Console** (steps 1–5 above) — the API cannot create a new listing.
2. **Google Cloud**: in the project linked to your Play account, enable the **Google Play Android
   Developer API**, then create a **service account** (no roles needed in GCP itself).
3. **Play Console → Users and permissions → Invite new users**: invite the service-account email
   (looks like `name@project.iam.gserviceaccount.com`), grant **Admin (all apps)** or per-app
   "Release to testing tracks" + "Release to production".
4. Create a **JSON key** for the service account and save it as `store/service-account.json`
   (already gitignored).
5. Upload:
   ```bash
   pip install google-api-python-client google-auth
   python3 store/upload_to_play.py --track internal     # internal testing
   python3 store/upload_to_play.py --track production    # full production rollout
   ```
   The script uploads `app/build/outputs/bundle/release/app-release.aab`, attaches it to the track
   with release notes, and commits the edit. Bump `versionCode` in `app/build.gradle.kts` for each
   new upload (Play rejects duplicates).

Once you drop `store/service-account.json` in place, hand it back to me and I'll run the upload.
