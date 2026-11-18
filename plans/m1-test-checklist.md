# M1 device test checklist

Use the signed, R8-minified release APK at `app/build/outputs/apk/release/app-release.apk`. The agent's handover must identify the tested commit, version, certificate fingerprint and automated verification results. Unchecked items below require device verification; this file does not claim they passed.

## Verified release — 2026-11-15

- Version: `0.1.0-1`, Android version code `10001`, application ID `com.kmab.deft`.
- APK size: 1,794,996 bytes.
- APK SHA-256: `8d59de787b1e43a431718053bec68d13af184e5f725c0ead98826f1a6909b980`.
- Signing certificate SHA-256: `adea6ec2edf4e9e2d809ed06300729e35ca156c06072736717b4a27449233e50`.
- Combined `ktlintFormat check assembleRelease` passed. Recorded JVM/Robolectric results total 226 test executions with no failures or errors; four release-tooling tests also passed.
- The negative network-isolation check rejected temporary OkHttp in `:core:common`. The clean dependency graph and merged release manifest passed afterward.
- `scripts/verify-release.py` confirmed the persistent certificate, application ID/version, absence of INTERNET permission and R8 mapping output. Resource shrinking is enabled too.
- Source commit and feature-branch CI status are identified in the chat handover. Device and update-install acceptance remain unchecked: no device or emulator was connected.

Keep the private signing directory described in [release-signing.md](release-signing.md) backed up. Future release builds reuse that identity; do not replace it.

## Install and update

- [X] Install the release APK. Confirm Android identifies it as Deft and the approved book-and-ribbon icon appears.
- [X] Open the app without a network connection. It launches without a crash.
- [-] For a subsequent handover, install the newer release APK over the previous Deft release without uninstalling. Library folders, settings and books remain intact.
- [X] Confirm the new APK uses the same release certificate and a nondecreasing version code. A debug-key build is not an update test for a release-key build.

## Library folders and scanning

- [X] With no folders selected, the Books screen shows “no books yet.” and an “add a folder” action.
- [/] Add a folder containing EPUBs. Covers, titles and authors appear; a book without a cover gets a generated cover.
-- Not all books have been added successfully. I had a folder with over 1.5k epubs books, only around 500 loaded successfully.
- [X] Restart Deft. The folder grant and library remain available.
- [X] Put identical bytes in two selected, non-overlapping folders. They appear as one Book.
- [X] Delete one of those files externally and rescan. The Book remains present.
- [X] Delete its last file externally and rescan. It disappears from the default list but appears with a missing badge when “show missing” is enabled.
- [X] Restore the same bytes at a different location and rescan. The original Book reattaches without duplication.
- [X] Move a file between selected folders and rescan. Repeat in the reverse direction; neither operation creates a duplicate Book.
- [X] Replace a file with different contents. The new content is a new Book; the old Book remains missing.
- [-] Exclude a subfolder. Only its books become missing. Remove the exclusion and confirm they reattach.
-- Exclusion mechanism not provided
- [-] Try to exclude a folder outside the selected root. Deft rejects it with a clear explanation.
-- Same as before
- [/] Try to add an already selected root, its parent, or its child. Deft explains that overlapping folders are already covered.
-- Explanation not clear enough, I tested a biig folder with multiple subfolders and some of its subfolders already selected. it wholesale reject the bigger parent folder even though there are other subfolders that havent been covered. make explanation more explicit and clear to remove already covered subfolders.
- [X] Remove a selected folder in Deft. Its files remain untouched on disk and its books become missing.
- [X] Revoke a folder permission or disconnect removable storage externally, then rescan. Deft reports the unavailable folder without falsely marking its books missing.
- [X] Cancel or interrupt a scan. A later scan completes correctly; unseen files are not falsely marked missing by the interrupted scan.
- [X] Disable and re-enable a supported file type. Check the resulting library after rescanning.
- [X] Confirm all nine supported formats have a setting; RTF, LIT and unrelated files are not added.
- [/] Add a malformed EPUB and a zero-byte EPUB beside a valid EPUB. Scanning continues, fallback titles appear and errors are reported.
-- report display methods not clear enough
- [-] Add a DRM-protected EPUB fixture. It is labelled “protected”; no decryption is attempted.
- [\] Pull to refresh and use “rescan now”. Scan progress and the resulting summary are understandable.
-- report display not clear at all

## Books and settings

- [X] Switch between grid and list, then restart. The chosen view persists.
- [X] Sort by title, author and date added. Results follow the selection.
- [X] Change format filters and “show missing” in the filter-and-sort sheet. Changes apply without an extra confirmation step.
- [X] Open Settings → Library and return to Books using Android back navigation.
- [X] Book reading and later-milestone controls are not falsely presented as working in M1.
- [/] With a large library, scrolling remains responsive and scanning does not freeze the interface.
-- Not all books are being scanned

## How does it look?

- [-] Check Books in both paper and ink themes, grid and list, populated and empty states.
-- Book reader features not implemented yet
- [\] Check the filter-and-sort sheet, folder settings, exclusion controls, confirmation dialogs and scan summary in both themes.
-- the design of the entire is very very bad, it needs significant work.
- [-] Compare fonts, spacing, square-cornered controls, semantic colours and the app icon with the approved visual design.
- [-] Increase Android font size. Text stays usable, controls remain reachable and labels do not overlap.
- [-] Use TalkBack to navigate folder controls, filters, view switching and book information.
- [X] Check touch targets and contrast; enable reduced motion and confirm the interface remains usable.


Record device model, Android version, APK version, failed steps and visual feedback when returning this checklist. The feature branch remains unmerged until the author confirms it passed.

## Additional audit regressions

- [X] With two selected folders, revoke access to one and rescan. The other still updates, and Deft explains that a folder is unavailable.
- [-] Open the exclusion folder picker, rotate the device or recreate the app activity, then choose a subfolder. It is excluded from the original root rather than added as another library folder.
-- not provided in app
- [-] Scan a book with a very large cover image. Scanning finishes without a crash and shows a thumbnail.
- [-] On Android 8 or 9, verify parent/child folder rejection and subfolder exclusions through the compatibility path.
-- device not available, skip, assume pass

[X] - passed
[-] - unapplicable
[/] - partial passed
[\] - failed

- Device: Samsung S25 FE
- Android version: 16
- APK version: 0.1.0-1
- General feedback:
	- Refer to screenshots in plans/feedback folder
	- the design (UI/UX) of the app is very unpolished, unintuitive and not pleasing for the eye, it needs serious work
	- with a large dataset, the app is slightly slow and takes up a very big amount of data for its db
	- allow several sizes for the home page display for both grid and list displays
	- add "last read" for sorting and allow inverse sorting
	- for the app icon, add a buffer space around the app icon (make the icon itself smaller)


## Final M1 follow-up — 2026-11-18

The author reports almost all prior tests passed and explicitly authorizes merging M1 after the listed fixes and local verification. Original test notes above are preserved. M1 is0.1.2; the next M2 APK includes these fixes and is the update for phones already running0.2.0.

- [ ] Choose a format filter, sort and direction; navigate to Settings and back, then force-stop/reopen. The previous choices remain selected and results agree.
- [ ] Deselect all view formats, reopen, then reset. Empty selection stays empty until reset and never changes Settings → Library scan formats.
- [ ] Scan settings keeps status, progress and controls inside one coherent panel. Check idle/active/completed states and notification controls at150% system text.
- [ ] Folder controls, exclusions, file-type choices and storage rows align and remain accessible at large text and landscape width.
- [ ] Both book grid and list use padded, subtly outlined/lifted cards. Empty metadata reads `<no-title>` and `<no-author>`; long metadata still truncates cleanly.

Local `check assembleRelease` passed on 2026-11-18: 362 test executions, zero failures/errors; ktlint, detekt, lint and network isolation passed. The final normal/150% paper/ink renders were inspected; large-text grids widen to two columns. A whole-change audit found no actionable regressions. Device checks above remain for the combined M2 patch handover; they are not claimed as tested by the agent.
