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

- [ ] Install the release APK. Confirm Android identifies it as Deft and the approved book-and-ribbon icon appears.
- [ ] Open the app without a network connection. It launches without a crash.
- [ ] For a subsequent handover, install the newer release APK over the previous Deft release without uninstalling. Library folders, settings and books remain intact.
- [ ] Confirm the new APK uses the same release certificate and a nondecreasing version code. A debug-key build is not an update test for a release-key build.

## Library folders and scanning

- [ ] With no folders selected, the Books screen shows “no books yet.” and an “add a folder” action.
- [ ] Add a folder containing EPUBs. Covers, titles and authors appear; a book without a cover gets a generated cover.
- [ ] Restart Deft. The folder grant and library remain available.
- [ ] Put identical bytes in two selected, non-overlapping folders. They appear as one Book.
- [ ] Delete one of those files externally and rescan. The Book remains present.
- [ ] Delete its last file externally and rescan. It disappears from the default list but appears with a missing badge when “show missing” is enabled.
- [ ] Restore the same bytes at a different location and rescan. The original Book reattaches without duplication.
- [ ] Move a file between selected folders and rescan. Repeat in the reverse direction; neither operation creates a duplicate Book.
- [ ] Replace a file with different contents. The new content is a new Book; the old Book remains missing.
- [ ] Exclude a subfolder. Only its books become missing. Remove the exclusion and confirm they reattach.
- [ ] Try to exclude a folder outside the selected root. Deft rejects it with a clear explanation.
- [ ] Try to add an already selected root, its parent, or its child. Deft explains that overlapping folders are already covered.
- [ ] Remove a selected folder in Deft. Its files remain untouched on disk and its books become missing.
- [ ] Revoke a folder permission or disconnect removable storage externally, then rescan. Deft reports the unavailable folder without falsely marking its books missing.
- [ ] Cancel or interrupt a scan. A later scan completes correctly; unseen files are not falsely marked missing by the interrupted scan.
- [ ] Disable and re-enable a supported file type. Check the resulting library after rescanning.
- [ ] Confirm all nine supported formats have a setting; RTF, LIT and unrelated files are not added.
- [ ] Add a malformed EPUB and a zero-byte EPUB beside a valid EPUB. Scanning continues, fallback titles appear and errors are reported.
- [ ] Add a DRM-protected EPUB fixture. It is labelled “protected”; no decryption is attempted.
- [ ] Pull to refresh and use “rescan now”. Scan progress and the resulting summary are understandable.

## Books and settings

- [ ] Switch between grid and list, then restart. The chosen view persists.
- [ ] Sort by title, author and date added. Results follow the selection.
- [ ] Change format filters and “show missing” in the filter-and-sort sheet. Changes apply without an extra confirmation step.
- [ ] Open Settings → Library and return to Books using Android back navigation.
- [ ] Book reading and later-milestone controls are not falsely presented as working in M1.
- [ ] With a large library, scrolling remains responsive and scanning does not freeze the interface.

## How does it look?

- [ ] Check Books in both paper and ink themes, grid and list, populated and empty states.
- [ ] Check the filter-and-sort sheet, folder settings, exclusion controls, confirmation dialogs and scan summary in both themes.
- [ ] Compare fonts, spacing, square-cornered controls, semantic colours and the app icon with the approved visual design.
- [ ] Increase Android font size. Text stays usable, controls remain reachable and labels do not overlap.
- [ ] Use TalkBack to navigate folder controls, filters, view switching and book information.
- [ ] Check touch targets and contrast; enable reduced motion and confirm the interface remains usable.

Record device model, Android version, APK version, failed steps and visual feedback when returning this checklist. The feature branch remains unmerged until the author confirms it passed.

## Additional audit regressions

- [ ] With two selected folders, revoke access to one and rescan. The other still updates, and Deft explains that a folder is unavailable.
- [ ] Open the exclusion folder picker, rotate the device or recreate the app activity, then choose a subfolder. It is excluded from the original root rather than added as another library folder.
- [ ] Scan a book with a very large cover image. Scanning finishes without a crash and shows a thumbnail.
- [ ] On Android 8 or 9, verify parent/child folder rejection and subfolder exclusions through the compatibility path.
