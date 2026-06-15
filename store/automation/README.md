# Play Store upload automation (same method as the Sudoku app)

Automated AAB uploads to Google Play via the Play Developer API. **The app must already
exist in Play Console** — the API cannot create a new listing, accept agreements, or fill
the content-rating / data-safety forms. Do those once in the UI (see `../STORE_LISTING.md`
and `../SUBMISSION_GUIDE.md`), then use this for every release.

## One-time setup

1. **Create the app in Play Console** as `com.tertiaryinfotech.hrportal` and complete the
   "App content" declarations.
2. **Service account** — reuse the same Google Cloud service account you use for the Sudoku
   app (simplest), or create a new one:
   - Play Console → *Setup → API access* → link the Google Cloud project.
   - Cloud Console → IAM → Service Accounts → (reuse existing or create).
   - Play Console → *Users and permissions* → grant that service account **Release manager**
     (or Admin) on this app.
3. **JSON key** — Cloud Console → the service account → Keys → Add key → JSON. Save it
   privately, e.g. `~/.secrets/play-hrms.json`. **Never commit it.**

Python deps (`google-auth`, `google-api-python-client`) — install if missing:
```bash
pip3 install google-auth google-api-python-client
```

## Upload a build

```bash
# 1) Build the AAB (from the project root):
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew :app:bundleRelease

# 2) Upload to the internal track first (smoke-test via the opt-in link):
python3 store/automation/upload_to_play.py \
  --json-key ~/.secrets/play-hrms.json \
  --aab app/build/outputs/bundle/release/app-release.aab \
  --package com.tertiaryinfotech.hrportal \
  --track internal \
  --release-name "1.0 (1)" \
  --notes "First release of the native Tertiary HRMS Android app."

# 3) When happy, push to production:
python3 store/automation/upload_to_play.py \
  --json-key ~/.secrets/play-hrms.json \
  --aab app/build/outputs/bundle/release/app-release.aab \
  --package com.tertiaryinfotech.hrportal \
  --track production --status completed
```

## Future releases
Each upload needs a unique, higher `versionCode`. Edit `app/build.gradle.kts`
(`versionCode`/`versionName`), rebuild the AAB, and re-run the script.

## Alternative: fastlane
`fastlane/Appfile` + `fastlane/Fastfile` provide `internal` / `release` lanes that do the
same thing. Point them at the key via `SUPPLY_JSON_KEY=~/.secrets/play-hrms.json`.
