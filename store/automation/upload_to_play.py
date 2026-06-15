#!/usr/bin/env python3
"""
Upload a signed AAB to a Google Play track via the Play Developer API.

Prerequisites (one-time, account-side — see store/automation/README.md):
  1. The app already exists in Play Console (the API cannot create a new app).
  2. A Google Cloud service account is linked to Play Console with the
     "Release manager" (or Admin) permission on this app.
  3. Its JSON key is downloaded locally and kept OUT of git.

Usage:
  python3 upload_to_play.py \
    --json-key /path/to/service-account.json \
    --aab ../../app/build/outputs/bundle/release/app-release.aab \
    --package com.tertiaryinfotech.sudokuapp \
    --track internal \
    [--release-name "1.0 (1)"] [--notes "First release"] [--status completed]

Tracks: internal | alpha | beta | production
Status: completed (live) | draft | inProgress (staged rollout)

No secrets are stored in this file; the JSON key path is passed at runtime.
"""
import argparse
import sys

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

SCOPE = "https://www.googleapis.com/auth/androidpublisher"


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--json-key", required=True, help="Service-account JSON key path")
    ap.add_argument("--aab", required=True, help="Path to the signed .aab")
    ap.add_argument("--package", required=True, help="applicationId, e.g. com.tertiaryinfotech.sudokuapp")
    ap.add_argument("--track", default="internal", choices=["internal", "alpha", "beta", "production"])
    ap.add_argument("--release-name", default=None)
    ap.add_argument("--notes", default="", help="Release notes (en-US)")
    ap.add_argument("--status", default="completed", choices=["completed", "draft", "inProgress"])
    args = ap.parse_args()

    creds = service_account.Credentials.from_service_account_file(args.json_key, scopes=[SCOPE])
    service = build("androidpublisher", "v3", credentials=creds, cache_discovery=False)
    edits = service.edits()

    print(f"Opening edit for {args.package} …")
    edit_id = edits.insert(body={}, packageName=args.package).execute()["id"]

    print(f"Uploading {args.aab} …")
    media = MediaFileUpload(args.aab, mimetype="application/octet-stream", resumable=True)
    bundle = edits.bundles().upload(
        packageName=args.package, editId=edit_id, media_body=media
    ).execute()
    version_code = bundle["versionCode"]
    print(f"  uploaded versionCode {version_code}")

    release = {
        "versionCodes": [str(version_code)],
        "status": args.status,
    }
    if args.release_name:
        release["name"] = args.release_name
    if args.notes:
        release["releaseNotes"] = [{"language": "en-US", "text": args.notes}]

    edits.tracks().update(
        packageName=args.package,
        editId=edit_id,
        track=args.track,
        body={"track": args.track, "releases": [release]},
    ).execute()
    print(f"  assigned to track '{args.track}' (status={args.status})")

    edits.commit(packageName=args.package, editId=edit_id).execute()
    print(f"✅ Committed. versionCode {version_code} is now on the '{args.track}' track.")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as e:  # noqa: BLE001
        print(f"❌ Upload failed: {e}", file=sys.stderr)
        sys.exit(1)
