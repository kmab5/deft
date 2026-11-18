# M2 — EPUB reader + M1 feedback test checklist

Install `app/build/outputs/apk/release/deft-0.2.0.apk` over the existing Deft installation. Keep app data. This combines M2 with the pending M1 feedback changes; neither milestone is merged until you approve this checklist. Tick only steps actually tested, and put observations/screenshots under the relevant section or in `plans/feedback/`.

Suggested samples: two ordinary EPUBs with several chapters, one illustrated EPUB, a long chapter, non-Latin text, a protected EPUB if available, a damaged copy, one valid TTF/OTF and one invalid font file. Use disposable copies for missing/changed/forget checks. PDF and other formats are scanned/listed, but reading them belongs to later milestones.

## Install and preserved data

- [ ] Android offers an update without asking to uninstall. Installed version is **0.2.0**. Existing folders, exclusions, books, covers and library preferences survive.
- [ ] Existing large library opens normally. No repeated duplicate import or blank/reset library appears after the database upgrade.
- [ ] Launch offline/in airplane mode. Scan, book details, reader, settings and font import work without internet or an account.

## Earlier M1 feedback — test in this build

- [ ] Books has a clear title/count and a familiar icon toolbar for filter, sort and display. There is no permanent “title — a to z / scan complete / scan details” stack.
- [ ] Filter, sort and display popups are compact and understandable. Try all file filters, empty filters, missing toggle, both sort directions and every independent grid/list size. Close/reopen to verify saved settings.
- [ ] No old scan-complete notice appears on a fresh launch. Start a scan: its current activity is visible. Completion offers details temporarily, then disappears without interaction; interacting with the notice keeps it available long enough.
- [ ] Scan details opens its own popup. Added/skipped/error categories show useful filenames and reasons. Counts agree with the outcome, including duplicates and filename metadata fallbacks.
- [ ] Scan the large source again, including PDFs. Leave Deft, lock the phone briefly, and return. A user-started scan continues via its notification, or clearly reports an interrupted/resumable result. It never silently claims completion after stopping early.
- [ ] Rescanning unchanged files is reasonably quick and does not create duplicates or repeatedly animate/reload the whole library. The earlier ~80 MB for ~1,400 books is a comparison point, not an error by itself.
- [ ] Pick an exclusion outside its parent folder. The error slides up from the bottom rather than moving controls in the page body. Trigger several different errors: notices stack/dismiss independently without blocking controls permanently.
- [ ] Folder add/remove/include/exclude controls have understandable icons. Remove confirmation still protects accidental actions; missing history is retained.
- [ ] Settings opens a category page containing **library** and **reader**. Back works correctly from each and from Library → Ignored books.

## Library and book details

- [ ] Tap a book in grid and list views: the correct detail page shows cover/title/authors, available metadata/description, format and file paths/sizes. Long titles and descriptions scroll without hiding actions.
- [ ] Read opens the selected EPUB. A saved book says Continue and displays its saved percentage. No invented rating, reading state, elapsed time or time-left estimate appears.
- [ ] Read two different books. Books → Continue reading shows the most recently opened readable EPUB, its real percentage, and opens it directly at its saved position.
- [ ] Protected, missing and unsupported-format books show a clear explanation when Read is tapped. The app remains usable and does not try to decrypt DRM.

## Reading, movement and position restore

- [ ] Open an EPUB with multiple chapters. Text, images, headings, italics, bold and internal footnote/chapter links render. Repeat with long chapters and non-Latin text.
- [ ] In paged mode, tap left/right edges and swipe both ways, including across chapter boundaries. Centre tap toggles controls without turning a page.
- [ ] Change to scroll mode. Scroll within and between chapters; the displayed chapter/percentage follows the actual location. Centre tap still shows controls.
- [ ] Compare page animation **none** and **slide**. Both work without accidental extra turns. Rapid gestures, first/last page and chapter boundaries stay stable.
- [ ] Leave the reader and reopen using both detail → Continue and the Continue card. Return to the same passage, including a location well inside a long chapter.
- [ ] Rotate while reading and while controls are open. Keep the current passage and selected preferences. Repeat after changing font size or switching paged/scroll.
- [ ] Background Deft, open other apps, lock/unlock, then return. Location and controls remain usable. Force-stop after leaving the reader and relaunch: the saved position is retained.
- [ ] Optional clock regression: on a test device, disable automatic time and move the clock backward, read further, leave/reopen, and open a second book. The new passage and latest Continue book still save. Restore automatic time afterward.
- [ ] Open several books repeatedly and return to Books. No crash, stale book text, persistent loading screen or growing pile of temporary reader files remains after normal closes.
- [ ] On a disposable sample, replace its bytes while keeping the same filename, then open without rescanning. Deft asks for a rescan rather than applying the old Book's position to different content.
- [ ] A damaged/truncated EPUB produces a useful failure and a working back action. A book with a missing chapter does not leave the rest of the app stuck.

## Contents, progress and bookmarks

- [ ] Contents shows nested chapter titles in reading order. Tap early/middle/late chapters and verify the correct passage opens.
- [ ] Drag the progress slider to several positions. Chapter ticks and the chapter/percentage label are sensible; releasing completes the jump.
- [ ] After a TOC, progress, bookmark or internal-link jump, **go back** returns to the preceding passage. Ordinary page turns do not create unwanted jump history.
- [ ] Add bookmarks in different chapters. The bookmark button indicates the current bookmarked location. Toggle it again to remove that location without removing other bookmarks.
- [ ] Contents → Bookmarks lists saved entries. Rename one, leave/reopen the list, and verify its label. Tap it to return to its passage.
- [ ] Delete a bookmark from the list. Other bookmarks remain. Close/reopen the app and confirm labels, locations and removals persist.
- [ ] Long-press text, select and copy it. Selection handles and copying work without unexpected page turns. No nonfunctional highlight/note/search buttons are shown yet.

## Text controls — test in the book and in Settings → Reader

- [ ] The single reader settings button opens **text / theme / page** tabs. Changes preview in the book behind the sheet and persist after closing it.
- [ ] Choose **original** and verify an EPUB with a publisher font retains that font. Choose Literata again and verify the override applies.
- [ ] Try all eleven bundled families: Literata, Source Serif 4, PT Serif, EB Garamond, Crimson Pro, Bitter, Lato, Open Sans, Inter, Atkinson Hyperlegible and OpenDyslexic. Names preview in their own fonts; book text changes. Check bold/italic passages.
- [ ] Exercise minimum/maximum text sizes and line spacing, small/medium/large margins, left/justified alignment, and hyphenation on/off. Controls stay responsive and the current passage remains nearby after reflow.
- [ ] Change settings in a book, then inspect Settings → Reader. Change them there, then reopen a different book. Both surfaces edit the same global preferences.

## Imported fonts

- [ ] Settings → Reader → Text: import a valid TTF, then an OTF if available. Each appears in the picker, survives restart, and renders in a book.
- [ ] Cancel the font picker: nothing changes. Select malformed, unsupported or oversized data: a bottom notice explains the failure and existing fonts still work.
- [ ] Remove an imported font. It disappears from the picker; removing the selected one falls back to Literata. Restart and verify the deleted font does not return.
- [ ] Original font documents remain untouched. Fonts stay available after moving the source document because Deft keeps its own copy.

## Themes and brightness

- [ ] Try Light, Sepia, Dark and Black. Book text/background, links, reader bars, sheets and notices follow the selected reader theme, independently of the app theme.
- [ ] Change themes in a chapter with links, then turn into adjacent chapters already visited/preloaded. Their links use the new accent too.
- [ ] Settings → Reader: create a custom theme with background, text and accent colours. Save, use it in a book, edit it, and verify the changes persist after restart.
- [ ] Invalid hex values, unreadable contrast and empty names produce useful bottom notices. Cancelling an editor does not save partial changes.
- [ ] Delete the selected custom theme. Active and day/night references fall back to valid built-ins; reopening the reader does not crash or keep a broken selection.
- [ ] Enable follow system day/night and choose different light/dark themes. Switch Android light/dark mode while reading. Disable following and verify manual theme selection works again.
- [ ] Disable “use system brightness” and move the slider, in the quick panel and the default settings. Brightness changes only in the reader; leaving it restores Android's brightness.
- [ ] Re-enable system brightness. Lock/unlock or leave/return to the reader and verify the selected brightness policy is reapplied correctly.

## Page controls, immersive mode and lifecycle

- [ ] Toggle the status line. When enabled it shows clock, battery and current progress without overlapping book content or controls; values refresh during reading.
- [ ] Enable volume-key page turns: down advances, up goes back, with one turn per press and no volume overlay. Disable it: normal Android volume controls return.
- [ ] Enable keep screen on and wait beyond the phone's normal timeout while reading. Disable it and verify the normal timeout resumes. Leaving the reader always releases its screen-on setting.
- [ ] Hide controls: reading is immersive. Reveal them or swipe system bars: navigation remains accessible. Check gesture navigation and landscape cutouts/insets.
- [ ] Open settings/contents, use system Back, then Back again. Sheets close before leaving the reader; no invisible overlay intercepts taps afterward.

## Ignore, missing files and explicit Forget

- [ ] Bookmark a book and note its passage. Detail → Ignore asks for confirmation, then removes it from Books and Continue. Library → Ignored books contains it.
- [ ] Open its ignored detail and un-ignore it, or use the list's un-ignore action. The same Book, position and bookmarks return. Restart/rescan and verify the setting persists.
- [ ] Remove/move a disposable source and rescan. Show missing reveals its detail and preserved history. Put the identical bytes back in a selected folder and scan: the same Book reconnects with its bookmarks/position.
- [ ] Add the same EPUB in two selected folders. It remains one Book; reading still works if one copy disappears and the other is accessible.
- [ ] Choose Forget on a disposable book, then cancel: nothing is lost. Confirm Forget: the Book and its position/bookmarks disappear; the original file remains. A later scan may discover it as a fresh Book.

## Accessibility, motion and privacy

- [ ] Set Android font size to 150% or larger. Settings, font/theme editors, book details, contents/bookmarks and notices remain readable and scrollable; buttons do not overlap.
- [ ] Use TalkBack through library cards, category rows, reader actions, tab choices, sliders and bookmark controls. Labels identify actions and selected states, with comfortable touch targets.
- [ ] Disable Android animations. Popups, notices, selection, theme changes and page turns still complete immediately without getting stuck. Re-enable animations and check smooth transitions.
- [ ] Try EPUB external links/remote images while offline. No browser/network prompt opens; local content continues working. Deft never asks for internet access or modifies source EPUBs.
- [ ] Check the Samsung S25 FE/Android 16. If Android 8/9 hardware is available, repeat opening, turning, font import and restore there; otherwise mark that compatibility check unverified.

## Automated handover evidence

- Source commit: `f9b59581284cddc7d47e9f80e53194f0d1ad1e67` on `feat/m2-epub-reader`, including the pending M1 feedback changes. Version **0.2.0**, internal **0.2.0-1**, versionCode **20001**.
- `./gradlew check assembleRelease` passed locally on 2026-11-18: **449 test executions, 0 failures, 0 errors, 0 skipped**, across JVM and Android debug/release variants. Includes Room migration chains, reader persistence, offline EPUB policy/opening, font/theme management, Compose behavior and existing scanner regressions. ktlint, detekt, Android lint, network isolation and font budgets passed.
- Release-tooling Python tests: **4 passed**. The final APK is checked with `scripts/verify-release.py` for the configured persistent certificate, application identity/version, absence of INTERNET permission and R8 mapping. R8 minification and resource shrinking are enabled.
- One whole-M2 audit completed; fixes cover XML-safe XHTML, None/reduced-motion page turns, live link accents in preloaded chapters, and ordered position saves across clock rollback. The final reader tests also cover the large-text bookmark editor and asynchronous ZIP cleanup.
- Paper/ink and 150% text renders inspected for book details, reader settings and reader chrome. The chrome render uses a Compose content fixture; it does **not** verify Chromium's actual EPUB rendering.
- No Android device or installed emulator executable was available. Actual Readium rendering, gestures, lifecycle behavior, background scanning and all device steps above remain **unverified until physically tested**.
- Branch is pushed without waiting for GitHub CI. Neither M1 nor M2 is merged/tagged; combined device acceptance is still required.

- Final APK: `app/build/outputs/apk/release/deft-0.2.0.apk`, **5,825,725 bytes** (5.56 MiB). `:app:assembleRelease` passed after committing the verified source; the APK embeds the source revision above. No application code changed after the combined check.
- APK SHA-256: `84e3e7854bd1a02af7acf3c8e3a8e769c36762cbd17ecbe4ff66169ef6870aab`.
- Signing certificate SHA-256: `adea6ec2edf4e9e2d809ed06300729e35ca156c06072736717b4a27449233e50`, matching the persistent Deft certificate used for M1. No key or credentials are committed.
- Local verification logs: `.superpowers/m2-resumed-check-release.log` and `.superpowers/m2-source-stamped-release.log` (ignored build evidence). The first final attempt was interrupted during packaging; the resumed full command completed successfully.

## How does it look?

- [ ] Books toolbar, compact filter/sort/display popups, Continue card and scan-result notices feel clear and proportionate.
- [ ] Settings chooser, Library actions, ignored list and book detail are consistent with paper/ink and make the next action obvious.
- [ ] Reader typography, margins, content insets, chrome and status line feel comfortable in portrait and landscape.
- [ ] Text/theme/page tabs, font choices, custom-theme editor, contents and bookmark controls have useful spacing without excessive blank space.
- [ ] Page turns, chrome, sheets, notices, theme changes, list updates and screen transitions feel smooth. Record any abrupt change, flicker, clipped label or confusing icon with its steps/screenshot.
- [ ] Overall feedback: note preferred defaults, remaining problems and any new ideas here. Confirm explicitly when the combined M1/M2 checklist is accepted.
