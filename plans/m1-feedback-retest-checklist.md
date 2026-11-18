# M1 feedback retest

This follows the author's Samsung S25 FE / Android 16 review. The original marks and notes remain in [m1-test-checklist.md](m1-test-checklist.md). [Rendered screens](design/m1-feedback.html) show the revised layouts. The source commit, signed APK hash and automated results are recorded in the handover after verification.

## Verified handover — 2026-11-18

- Tested source commit: `c9dccb768706743af803b843d784e819410b2942`.
- Release APK: `app/build/outputs/apk/release/deft-0.1.0-m1-feedback-c9dccb76.apk` (1,828,412 bytes).
- Pending M1 version: `0.1.0-1`; Android version name `0.1.0`, version code `10001`; app ID `com.kmab.deft`.
- APK SHA-256: `f90c2c74e5cc8621671e9e8fd281cdfa1e6abf9ad1817806c330d52a718f146e`.
- Certificate SHA-256: `adea6ec2edf4e9e2d809ed06300729e35ca156c06072736717b4a27449233e50` — unchanged from the previous handover.
- `ktlintFormat check` and final `check assembleRelease` passed. Recorded results: 282 JVM/Robolectric test executions, zero failures/errors; four Python release-tooling tests passed.
- Signed release verification passed: configured persistent certificate, expected identity/version, no INTERNET permission and R8 mapping. R8 minification and resource shrinking are enabled.
- Whole-change audit findings were fixed and re-reviewed. Paper/ink phone renders, including 150% system font scaling, were inspected. The 1,500-EPUB resume and 5,000-unchanged-file regressions passed.
- No Android device was connected. Physical update-install, background scanning, TalkBack and Android 8/9 acceptance are not claimed passed.
- Feature branch is pushed for CI; CI completion is not a handover gate. Merge/tag await the author's device acceptance.

## What changed for each feedback point

| Feedback | Response |
|---|---|
| Only part of a large EPUB folder imported; stopped after leaving the app | User-started scans request foreground execution. Interrupted background work resumes from committed fingerprints, with bounded retry and an explicit recovery message. Unseen files retain their previous presence until traversal completes. |
| PDFs were not included | The screenshots show PDF scanning disabled. Settings now states how many types are enabled and that unchecked types are not scanned. Enable PDF in Settings; Books filters do not control scanning. |
| Exclusions were not provided/discoverable | Every folder card has a visible **exclude a subfolder** button and lists existing exclusions with **include** actions. No hidden accordion. |
| Parent folder rejected without useful guidance | The message names the already-selected child and explains removing selected child roots before adding their parent, preserving source files/history and setting up exclusions again. |
| Malformed files and scan outcomes unclear | Progress shows checked-file counts. Details separate added Books, duplicate files, skipped types, filename fallback, unreadable files and incomplete/unavailable folders. Previous results are hidden during a running scan. |
| UI was sparse, oversized and unintuitive | Settings opens directly to Library settings. Books has focused filters, sort and display panels, two-line titles and more compact typography. Scan information appears at the top of Settings. |
| Large library slightly slow | Unchanged files avoid per-file reads from Room and redundant writes/missing-flag rewrites. Private thumbnails avoid a second Coil disk cache. Physical-device scrolling still needs the check below. |
| Large storage size | Author clarified that about 80 MB for 1,400 Books is acceptable. Cover quality is retained. Settings shows database, thumbnail and temporary-cache sizes separately. |
| Several grid/list sizes | Small, medium and large are available for both layouts, remembered separately. |
| Last-read and inverse sorting | Added last read and explicit sort direction. Unread Books remain last in either direction. M1 creates no reading history; those Books tie by title until the reader records actual reading. |
| Launcher icon needs padding | Foreground and monochrome marks are smaller within the same icon. |
| Paper/ink check interpreted as reader themes | These checks concern the Books library and settings; the reader remains M2. App chrome follows Android light/dark mode. |
| Android 8/9 device unavailable | Skipped and unverified on hardware, rather than recorded as passed. Existing automated compatibility tests remain part of verification. |

## Install over the existing app

The pre-merge milestone remains `0.1.0-1` (version code `10001`). The rebuilt APK uses the same persistent certificate and supports replacement installation without uninstalling. Use its SHA-256 from the handover to identify this build.

- [ ] Install the new release APK **over** the previous release. Keep all existing Books, selected folders, exclusions and settings.
- [ ] Confirm the launcher icon has more space around the mark, including Android's themed-icon mode if enabled.

## Scanning and folders

- [ ] Enable PDF in Library settings. Rescan a folder with a known EPUB and PDF; both appear when the Books filters include those types. Disable PDF and confirm the settings explanation is clear.
- [ ] Rescan the large EPUB folder. Leave Deft with Home, use another app and lock the screen for several minutes. Return and inspect progress/results; let it complete. Compare checked files, duplicate files and skipped files with the actual folder contents.
- [ ] While scanning in Settings, optionally allow scan notifications. Check progress and cancellation from the notification; refusing notification permission must not prevent scanning.
- [ ] If Android restricts background work, confirm Deft preserves imported Books and provides an actionable message. Open Deft and explicitly rescan to finish. Record any persistent cutoff, the displayed counts and whether Android battery restrictions are enabled.
- [ ] Open scan details. Confirm malformed/zero-byte EPUBs are included with filename fallback and their issues are explained; unreadable files and unavailable folders are distinct.
- [ ] Exclude a subfolder using the visible button. Its Books become missing after a complete scan. Tap **include**, rescan, and confirm the same Books return.
- [ ] Try excluding a folder outside the chosen root, then try adding a parent of a selected root. Both explanations identify the corrective action.
- [ ] Start the exclusion picker, rotate the device, then choose a subfolder. It remains an exclusion of the original root.
- [ ] Interrupt a scan or revoke one root's grant. No unseen Books are falsely marked missing; a healthy root still updates.
- [ ] Read the storage breakdown after a completed scan. Record the database/thumbnail/cache figures only if they look unexpectedly large.

## Books and presentation

- [ ] Try small, medium and large grid sizes, then all three list sizes. Switch layouts and restart; each layout retains its own size.
- [ ] Try title, author, date added and last read in both directions. M1's unread Books stay ordered consistently without fabricated reading dates.
- [ ] Use filters and **show missing**. Changes apply immediately. Reset restores all displayed formats without changing scanning settings.
- [ ] Scroll through the large library while scanning and after completion. Note any repeated pauses or blank cover loading.
- [ ] Check populated/empty Books, filters, sort, display, folders and scan details in Android light and dark mode.
- [ ] Increase Android font size; all controls remain reachable. Use TalkBack for folder actions, filters, sort, display sizes and book information.
- [ ] Confirm removal confirmation still leaves the original files untouched and preserves history.

## Previously unavailable fixture checks

- [ ] If available, import a DRM-protected EPUB and confirm **protected**; no decryption is attempted.
- [ ] If available, scan a very large cover image and confirm a thumbnail appears without crashing.
- [ ] Android 8/9 hardware: skip if unavailable and record as unverified.

Record the APK hash, failed step, screenshot and relevant scan details when returning feedback. Do not uninstall to work around an update failure. The feature branch remains unmerged until device acceptance.
