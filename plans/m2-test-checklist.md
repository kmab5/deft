# M2 — EPUB reader + M1 feedback test checklist

## 0.2.5 — final polish and merge authorization

You passed all twelve 0.2.4 checks and explicitly authorized the remaining minor fixes followed by merge/push without a further feedback round. The review below is preserved. Final changes cover the larger title, centered progress thumb and footer spacing, swipeable settings tabs with icons, adaptive grid title spacing, compact scan notices and the notification-permission control. Local verification and release evidence will be recorded in the handover entry.

## 0.2.4 — device feedback retest

Your completed 0.2.3 review and comments remain below. Install this release as an update, without uninstalling.

- [X] Update to **0.2.4**. Library folders, display sizes, filters, reader preferences, positions and bookmarks remain.
- [X] Open the image that previously showed a blank viewer in The Tainted Cup, a cover, a large raster image and an SVG. Images appear on the native zoom surface. Pinch around different points, pan when enlarged, close and press Back; return to the same passage. Malformed/unsupported images show a readable error and a working close button.
- [X] Slowly drag forward and backward until the next page is more than halfway visible, pause, then release: exactly one page turns. Return more than halfway toward the drag origin to cancel. Repeat at chapter boundaries, with quick flicks, with animation None and with system animations disabled. No page is skipped.
- [X] Verify tap zones are **35% left / 30% center / 35% right**. Center toggles HUD. System-edge Back swipes and text selection still work without unintended page turns.
- [X] Fullscreen book text has modest additional top space. The reader title is visibly larger, and the footer has space above the chapter name. Repeat with a long book title, landscape and 150% text; all controls remain reachable.
-- the title is not Large enough (the proportion between the back icon and the text isnt right, so make the text bigger)
- [X] Advanced search controls and regex help sit together in an elevated card, aligned with the search field in both Books and the reader. Toggle options and type an invalid expression; text wraps within the same gutters without pushing controls offscreen.
- [X] In-book search results each show **chapter and page** above the snippet. Open results from different chapters and different sections sharing one file if available; labels and passage navigation agree. Back returns to the original passage.
- [X] Tap an external website link in paged and continuous modes. A prompt names its actual domain. Cancel or Back stays in Deft. Confirm opens that URL in your browser; return to Deft without losing the passage. No website opens automatically and local/internal links still navigate within the book.
- [X] Grid/list Small, Medium and Large match their earlier widths and column counts; no reserved blank scrollbar gutter. Drag the visible thumb: it takes priority over the overlapped book area. Taps/ordinary scrolling away from the handle and after it fades still work. Continue and return-to-top remain accessible.
- [X] Rescan: **scan details** sits directly beneath the completion message, without the large empty gap. Message, status icon and close control align. Check multiple stacked notices and 150% text; details and dismissal still work.
-- make bottom padding of the scan notification smaller
-- in the library settings, when scanning, "allow scan notifications" is written, make it elevated and put a muted notification icon next to it if notification is not allowed and a normal icon if it is allowed.
- [X] Rapidly press the toolbar Back button on book details, Settings, Library settings, Reader settings and Ignored books. Repeat with system Back and transitions in progress. The app must never show an empty screen or pop past its library root. Normal Back from the library can still leave the app.
- [X] Repeat Back with reader contents/search/settings, the image viewer and external-link prompt open: close the top overlay first. Reopen Deft, rotate and background/foreground; reading position and previous successful 0.2.3 behavior remain intact.

----

- swiping in the reader settings sideways moves between the settings (text - theme - page), and add icons next to the names of these settings
- the circle in the progress bar that indicates current position isnt fully centered vertically on the bar itself, fix that
- add just a bit more padding to the top of the progress bar above the chapter indicator
- the space between the title of a book and its author in the library grid view is a bit too big when the titles are one line, when there are two lines, yes it works perfectly but even whe there are no books on the line that dtake up two lines, the gap still exists, it should be adaptive
- as the changes that i suggested are very minor, make these changes and merge onto main and push

No later milestone features were brought forward. M2 is accepted for final polish and merge as directed above.

## 0.2.3 — device feedback retest

All original 0.2.2 comments/results remain below. This section covers every new comment and the seven November 19 screenshots. Later milestone boundaries remain unchanged.

- [X] Update from 0.2.2 to **0.2.3** without uninstalling. Folders, library filters/sort, reader settings, positions and bookmarks remain.
- [X] Open **H Is for Homicide — Sue Grafton**. The formerly broken chapter renders without the duplicate-style error. Move through several chapters, switch paged/continuous mode and change typography. Original book files remain unchanged.
- [-] Open a disposable malformed or missing-chapter EPUB. Healthy text/chapters remain usable; any unreadable chapter does not trap navigation or prevent opening another book. An unreadable whole archive can still report an opening error.
- [\] Tap a raster image and an SVG cover. Image appears in its separate viewer; pinch zoom and pan work. Close or press Back and confirm the reading passage is unchanged. Repeat offline.
- [X] Long-press text, drag each selection handle, then release. Selection stays visible in paged and continuous modes; normal navigation returns after dismissing it. Copy still works.
- [X] In paged mode, test left/right taps across the outer **40%** on each side; the center **20%** toggles controls. Disabled tap-edge turns still toggles controls everywhere.
-- make it smaller a bit, 35% for each side, 30% for the center
- [/] With Slide animation, slowly drag horizontally: the page follows your finger. Return more than halfway toward the drag origin and release: the turn cancels. Try both directions and chapter boundaries. Vertical drags/pinch must not turn pages.
-- slowly dragging always cancels a page turn even if the new page is more than halfway visible on the screen
- [X] Swipe from either very edge for Android Back/system bars in fullscreen. This must not also turn a page. Repeat with gesture and three-button system navigation if available, animation None and Android animations disabled.
- [X] Continuous reading shows a subtle horizontal divider between chapters. Scroll through boundaries and change typography without losing the nearby passage.
- [X] Try Light, Sepia, Dark, Black and a custom theme: HUD and sheets are visibly distinct from the book background. Hidden-HUD status text changes with the theme and has side padding. Nonfullscreen reading leaves additional space below the system status bar.
- [X] The taller top bar shows the **book title**; the bottom shows the **chapter name**, percentage or **page** position. Slider endpoints align with the labels. Scrubbing previews a chapter and navigates on release; Jump Back still works. EPUB pages are reflowable position estimates, not printed-page numbers.
- [X] Expand/collapse a nested TOC: children are clearly indented beyond their parent. Reopen after scrolling/jumping; list state and current chapter remain useful.
- [X] In-book search does not execute while typing. Tap the right-hand **search** action or keyboard Search/Enter to run it. Check results, no results, clearing, cancellation and Back to the original passage.
- [X] Settings → Library → **advanced search** starts off. Enable it; both library and book search show match-case, whole-word and regex controls. Turn it off and verify those options no longer affect results. Restart Deft and verify the global setting persists.
- [X] Test literal search, case-sensitive words, whole words, and regex such as `chapter [0-9]+`. An invalid pattern produces a readable error. Lookaround/backreferences are unsupported and explained. A broad pattern such as `.*` shows compact results and keeps the app responsive. Repeated matching passages navigate to their respective positions.
- [X] In grid and list, drag the larger scrollbar handle to the middle and bottom. After it fades, book taps and ordinary scrolling near card right edges still work. Continue and return-to-top remain usable; final books are not covered.
- [/] Rescan: completion notice has a readable message, distinct aligned **scan details** action and easy close control. Details still lists added/skipped/error information. Multiple notices stack and dismiss smoothly; interacting pauses dismissal.
- [X] Inspect icons across library, book details, settings, notices and reader: established Material outline icons are consistent and meaningful. Icon-only controls have useful TalkBack labels.
- [-] Repeat reader controls/search, library, notices and settings at 150% text and in landscape. Use TalkBack on the advanced-search switch, search action, TOC, bookmarks and scrollbar; no unnamed new control or unreachable action.
- [X] Retest cold-launch last book, rotation, background/foreground, saved position/bookmarks, brightness and volume-key turns. Confirm explicitly when M2 is accepted for merge.


----

- add more margin to the top of the text of the book even for full screen reading (not too much)
- add padding above the chapter name in the bottom bar of the hud
- Make the title in the title bar of the reader much bigger
- the margin/padding/placement of the advanced search options is off, same with the help text that pops up, put it in an elevated card, make them align with everything else
- the image viewer is still not working, just a blank screen with the close icon
- it says external links stay closed ..., instead it should show a prompt asking whether the user is sure about opening a link at "domain name" then when the user confirms, open it in their default browser
- on in book search results, show which chapter and which page the search result is found in
- when trying to accomodate for the bigger thumb size of the scroll, you messed up the sizing of the grid/list display, i need it to be the same as before (the sizing), however, the scroll bar now works and is draggable, its okay if the scroll bar and the books overlap (scroll bar itself is higher so it takes priority)
- the scan complete notification is still off, why does the scan details have an entire line of its own? make it directly underneath scan complete
- i discovered a bug, when i spam clicked the back button from a book page, the screen just blanks. same with any back button. when i spam click any of them, they just blank out the screen. there is a screen recording of one of these incidents.

----

Coverage: corrupted content/image viewer/selection → steps 2–5; tap zones/swipe cancellation/system edges/dividers → 6–9; HUD/status/padding/progress/page wording/TOC → 10–12; explicit and advanced search → 13–15; draggable scrollbar/notices/icons → 16–18; accessibility/regression → 19–20.

Install `app/build/outputs/apk/release/deft-0.2.5.apk` over the existing Deft installation. Keep app data. Start with **0.2.4 — device feedback retest** above; earlier results and your comments are preserved as history. This includes the final M1 fixes already merged into main. M2 merge is authorized after the final local checks. Tick only steps actually tested, and put observations/screenshots under the relevant section or in `plans/feedback/`.

Suggested samples: two ordinary EPUBs with several chapters, one illustrated EPUB, a long chapter, non-Latin text, a protected EPUB if available, a damaged copy, one valid TTF/OTF and one invalid font file. Use disposable copies for missing/changed/forget checks. PDF and other formats are scanned/listed, but reading them belongs to later milestones.

## Install and preserved data

- [X] Android offers an update without asking to uninstall. Installed version is **0.2.1**. Existing folders, exclusions, books, covers and library preferences survive.
- [X] Existing large library opens normally. No repeated duplicate import or blank/reset library appears after the database upgrade.
- [X] Launch offline/in airplane mode. Scan, book details, reader, settings and font import work without internet or an account.

## Earlier M1 feedback — test in this build

- [X] Books has a clear title/count and a familiar icon toolbar for filter, sort and display. There is no permanent “title — a to z / scan complete / scan details” stack.
- [X] Filter, sort and display popups are compact and understandable. Try all file filters, empty filters, missing toggle, both sort directions and every independent grid/list size. Close/reopen to verify saved settings.
- [X] No old scan-complete notice appears on a fresh launch. Start a scan: its current activity is visible. Completion offers details temporarily, then disappears without interaction; interacting with the notice keeps it available long enough.
-- the scan complete notification on the bottom is badly designed, lots of misalignments and spacing errors
- [X] Scan details opens its own popup. Added/skipped/error categories show useful filenames and reasons. Counts agree with the outcome, including duplicates and filename metadata fallbacks.
- [X] Scan the large source again, including PDFs. Leave Deft, lock the phone briefly, and return. A user-started scan continues via its notification, or clearly reports an interrupted/resumable result. It never silently claims completion after stopping early.
- [X] Rescanning unchanged files is reasonably quick and does not create duplicates or repeatedly animate/reload the whole library. The earlier ~80 MB for ~1,400 books is a comparison point, not an error by itself.
- [X] Pick an exclusion outside its parent folder. The error slides up from the bottom rather than moving controls in the page body. Trigger several different errors: notices stack/dismiss independently without blocking controls permanently.
- [X] Folder add/remove/include/exclude controls have understandable icons. Remove confirmation still protects accidental actions; missing history is retained.
-- They look like handcrafted icons and they arent perfect? not sure if its necessary to change them but they are a bit jarring to look at (for example, the rescan icon, the add folder icon are slightly off)
- [X] Settings opens a category page containing **library** and **reader**. Back works correctly from each and from Library → Ignored books.

## Library and book details

- [X] Tap a book in grid and list views: the correct detail page shows cover/title/authors, available metadata/description, format and file paths/sizes. Long titles and descriptions scroll without hiding actions.
-- put the descripting in an elevated card of its own
-- the positioning of the continue/read button is off, its under the cover image and takes up an entire line which is empty, make it aligned vertically to the bottom of the cover image and to the right side
- [X] Read opens the selected EPUB. A saved book says Continue and displays its saved percentage. No invented rating, reading state, elapsed time or time-left estimate appears.
-- Rating, liking, reading state, elapsed time, time left estimate, none of these have been implemented yet
- [X] Read two different books. Books → Continue reading shows the most recently opened readable EPUB, its real percentage, and opens it directly at its saved position.
- [X] Protected, missing and unsupported-format books show a clear explanation when Read is tapped. The app remains usable and does not try to decrypt DRM.
-- Only epub support has been added so far, none of the other file types are supported

## Reading, movement and position restore

- [\] Open an EPUB with multiple chapters. Text, images, headings, italics, bold and internal footnote/chapter links render. Repeat with long chapters and non-Latin text.
- [\] In paged mode, tap left/right edges and swipe both ways, including across chapter boundaries. Centre tap toggles controls without turning a page.
-- Even in paged mode, the content scrolls
-- swiping up or down (in both paged and scrolled mode), moves between pages
- [\] Change to scroll mode. Scroll within and between chapters; the displayed chapter/percentage follows the actual location. Centre tap still shows controls.
-- Scroll mode not real, different chapters are paged, not scrolled
- [X] Compare page animation **none** and **slide**. Both work without accidental extra turns. Rapid gestures, first/last page and chapter boundaries stay stable.
- [X] Leave the reader and reopen using both detail → Continue and the Continue card. Return to the same passage, including a location well inside a long chapter.
- [\] Rotate while reading and while controls are open. Keep the current passage and selected preferences. Repeat after changing font size or switching paged/scroll.
- [X] Background Deft, open other apps, lock/unlock, then return. Location and controls remain usable. Force-stop after leaving the reader and relaunch: the saved position is retained.
- [-] Optional clock regression: on a test device, disable automatic time and move the clock backward, read further, leave/reopen, and open a second book. The new passage and latest Continue book still save. Restore automatic time afterward.
- [X] Open several books repeatedly and return to Books. No crash, stale book text, persistent loading screen or growing pile of temporary reader files remains after normal closes.
- [-] On a disposable sample, replace its bytes while keeping the same filename, then open without rescanning. Deft asks for a rescan rather than applying the old Book's position to different content.
- [\] A damaged/truncated EPUB produces a useful failure and a working back action. A book with a missing chapter does not leave the rest of the app stuck.

## Contents, progress and bookmarks

- [X] Contents shows nested chapter titles in reading order. Tap early/middle/late chapters and verify the correct passage opens.
-- allow the collapse/expand of nested ToC elements
-- on opening the ToC, it should either open at where the user left it or where the current chapter is at
- [\] Drag the progress slider to several positions. Chapter ticks and the chapter/percentage label are sensible; releasing completes the jump.
-- Dragging doesnt change anything until i release
- [X] After a TOC, progress, bookmark or internal-link jump, **go back** returns to the preceding passage. Ordinary page turns do not create unwanted jump history.
- [X] Add bookmarks in different chapters. The bookmark button indicates the current bookmarked location. Toggle it again to remove that location without removing other bookmarks.
- [X] Contents → Bookmarks lists saved entries. Rename one, leave/reopen the list, and verify its label. Tap it to return to its passage.
- [X] Delete a bookmark from the list. Other bookmarks remain. Close/reopen the app and confirm labels, locations and removals persist.
- [\] Long-press text, select and copy it. Selection handles and copying work without unexpected page turns. No nonfunctional highlight/note/search buttons are shown yet.
-- on dragging up or down, page switches

## Text controls — test in the book and in Settings → Reader

- [\] The single reader settings button opens **text / theme / page** tabs. Changes preview in the book behind the sheet and persist after closing it.
- [\] Choose **original** and verify an EPUB with a publisher font retains that font. Choose Literata again and verify the override applies.
- [\] Try all eleven bundled families: Literata, Source Serif 4, PT Serif, EB Garamond, Crimson Pro, Bitter, Lato, Open Sans, Inter, Atkinson Hyperlegible and OpenDyslexic. Names preview in their own fonts; book text changes. Check bold/italic passages.
- [\] Exercise minimum/maximum text sizes and line spacing, small/medium/large margins, left/justified alignment, and hyphenation on/off. Controls stay responsive and the current passage remains nearby after reflow.
- [\] Change settings in a book, then inspect Settings → Reader. Change them there, then reopen a different book. Both surfaces edit the same global preferences.
-- NONE of the settings work for the reader

## Imported fonts

- [-] Settings → Reader → Text: import a valid TTF, then an OTF if available. Each appears in the picker, survives restart, and renders in a book.
- [-] Cancel the font picker: nothing changes. Select malformed, unsupported or oversized data: a bottom notice explains the failure and existing fonts still work.
- [-] Remove an imported font. It disappears from the picker; removing the selected one falls back to Literata. Restart and verify the deleted font does not return.
- [-] Original font documents remain untouched. Fonts stay available after moving the source document because Deft keeps its own copy.

## Themes and brightness

- [/] Try Light, Sepia, Dark and Black. Book text/background, links, reader bars, sheets and notices follow the selected reader theme, independently of the app theme.
-- only reader bar and other app components change, nothing of the book itself changes
- [\] Change themes in a chapter with links, then turn into adjacent chapters already visited/preloaded. Their links use the new accent too.
- [\] Settings → Reader: create a custom theme with background, text and accent colours. Save, use it in a book, edit it, and verify the changes persist after restart.
- [-] Invalid hex values, unreadable contrast and empty names produce useful bottom notices. Cancelling an editor does not save partial changes.
- [-] Delete the selected custom theme. Active and day/night references fall back to valid built-ins; reopening the reader does not crash or keep a broken selection.
- [-] Enable follow system day/night and choose different light/dark themes. Switch Android light/dark mode while reading. Disable following and verify manual theme selection works again.
- [X] Disable “use system brightness” and move the slider, in the quick panel and the default settings. Brightness changes only in the reader; leaving it restores Android's brightness.
- [X] Re-enable system brightness. Lock/unlock or leave/return to the reader and verify the selected brightness policy is reapplied correctly.

## Page controls, immersive mode and lifecycle

- [\] Toggle the status line. When enabled it shows clock, battery and current progress without overlapping book content or controls; values refresh during reading.
-- header and progress bar overlap over book content (as they should) but they never hide, so i cant see the status bar if it ever appears
- [X] Enable volume-key page turns: down advances, up goes back, with one turn per press and no volume overlay. Disable it: normal Android volume controls return.
- [X] Enable keep screen on and wait beyond the phone's normal timeout while reading. Disable it and verify the normal timeout resumes. Leaving the reader always releases its screen-on setting.
- [\] Hide controls: reading is immersive. Reveal them or swipe system bars: navigation remains accessible. Check gesture navigation and landscape cutouts/insets.
- [X] Open settings/contents, use system Back, then Back again. Sheets close before leaving the reader; no invisible overlay intercepts taps afterward.

## Ignore, missing files and explicit Forget

- [X] Bookmark a book and note its passage. Detail → Ignore asks for confirmation, then removes it from Books and Continue. Library → Ignored books contains it.
- [X] Open its ignored detail and un-ignore it, or use the list's un-ignore action. The same Book, position and bookmarks return. Restart/rescan and verify the setting persists.
- [X] Remove/move a disposable source and rescan. Show missing reveals its detail and preserved history. Put the identical bytes back in a selected folder and scan: the same Book reconnects with its bookmarks/position.
- [X] Add the same EPUB in two selected folders. It remains one Book; reading still works if one copy disappears and the other is accessible.
- [X] Choose Forget on a disposable book, then cancel: nothing is lost. Confirm Forget: the Book and its position/bookmarks disappear; the original file remains. A later scan may discover it as a fresh Book.

## Accessibility, motion and privacy

- [-] Set Android font size to 150% or larger. Settings, font/theme editors, book details, contents/bookmarks and notices remain readable and scrollable; buttons do not overlap.
- [-] Use TalkBack through library cards, category rows, reader actions, tab choices, sliders and bookmark controls. Labels identify actions and selected states, with comfortable touch targets.
- [-] Disable Android animations. Popups, notices, selection, theme changes and page turns still complete immediately without getting stuck. Re-enable animations and check smooth transitions.
- [\] Try EPUB external links/remote images while offline. No browser/network prompt opens; local content continues working. Deft never asks for internet access or modifies source EPUBs.
- [-] Check the Samsung S25 FE/Android 16. If Android 8/9 hardware is available, repeat opening, turning, font import and restore there; otherwise mark that compatibility check unverified.

## Previous 0.2.0 handover evidence (superseded by the crash report)

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

- [\] Books toolbar, compact filter/sort/display popups, Continue card and scan-result notices feel clear and proportionate.
- [\] Settings chooser, Library actions, ignored list and book detail are consistent with paper/ink and make the next action obvious.
- [\] Reader typography, margins, content insets, chrome and status line feel comfortable in portrait and landscape.
- [\] Text/theme/page tabs, font choices, custom-theme editor, contents and bookmark controls have useful spacing without excessive blank space.
- [\] Page turns, chrome, sheets, notices, theme changes, list updates and screen transitions feel smooth. Record any abrupt change, flicker, clipped label or confusing icon with its steps/screenshot.
- [\] Overall feedback: note preferred defaults, remaining problems and any new ideas here. Confirm explicitly when the combined M1/M2 checklist is accepted.
-- Look under

## 0.2.1 recovery — retest first

The previous 0.2.0 reader crashed because the publisher-original font was passed to Readium as an empty custom font family. This patch skips that registration while keeping original publisher typography selectable. Prior checklist results and feedback remain above.

- [X] Install 0.2.1 over 0.2.0 without uninstalling. Existing library, folders and preferences survive.
- [X] Open each of the two EPUBs that crashed before. Both reach readable pages; turn several pages, go back, reopen and verify position restore.
- [X] In reader settings, select original, several bundled fonts and an imported font. Reopen both books with each selection; none crashes or shows a blank reader.
- [X] Set library format filters and reversed sort; navigate away, force-stop/reopen. Choices and results persist. Deselect all filters and repeat; scanning file types remain unchanged.
- [X] Scan status/progress stays inside its settings card. Folder actions, exclusions, file types and storage rows align at normal and 150% text.
- [X] Grid/list books have padding, rounded borders and subtle lift. Blank metadata reads `<no-title>` / `<no-author>`; every part of a card opens the right book.
- [X] Book detail has a clear cover/identity, Read or Continue action, actual saved progress and separate description/edition/file groups. Verify long titles, absent metadata, missing and protected books.
- [X] Reader settings typography, appearance and page controls are grouped, aligned and understandable. Try every existing control, font import/removal and custom theme create/edit/delete.
- [-] Quick settings stays within the reader screen and scrolls. At 150% text, narrow width and landscape, labels/values/actions remain readable and reachable with no clipping or overlapping.
- [X] Settings category and ignored-books pages match the grouped layout. Ignore/un-ignore preserves history; Forget still requires confirmation.


## Additional feedback

- Tapping zones for changing pages
- Tapping should hide the hud (title, progress bar)
- On jumps using the table of content or the progress bar, pressing back should go to the previous position, and the app should keep track of these jumps
- Pinching allows zoom on epubs, dont allow that
- Add an option to open last book on launch instead of the library
- Put continue the last read book as a floating button but keep the last read bar on top, but it hides on scroll down and appear on scroll up
- Not sure if bookmarking/notes have been implemented yet but it doesnt have it yet
- Add even more buffer space around the icon/logo of the app and Make it shorter vertically
- when editing the reader settings in one book, alter it for the entire app, make it persistent. liking a book doesnt work.
- on the bottom indicator in the book reader, allow the user to change from percentage to page number display in the settings
- there are these tiny section dividers on the bottom under the progress bar, remove them. consequently, when removing them, the progress bar should be shorter.
- images arent being rendered.
- on tapping images in an epub, it should open the image in its own viewer, the viewer should only have the image opened, no other items except the close button. users can zoom, pinch and drag, etc.
- when scrolling up in the main library, a button should pop up that allows users to instantly travel to the top
- add a scroll bar when scrolling, auto hide when not scrolling
- add a search in the book and in the library
- implement all the relevant and useful settings from lithium


Local `check assembleRelease` passed on 2026-11-18: 489 test executions, zero failures/errors/skips. Lint, ktlint, detekt and network isolation passed; four release-tooling Python tests also passed. The actual bundled-catalog regression failed before the fix and passed afterward. A real two-chapter EPUB was parsed by the offline engine and attached to an actual Readium navigator in both debug/release tests.

One whole-change audit found no remaining actionable regressions. Normal/150% paper/ink reader settings, book details, landscape book detail, settings chooser and ignored-book renders were inspected. References: `plans/design/m2-feedback.html`.

These device steps remain unverified until the author tests them. Robolectric attachment does not establish actual Chromium page rendering or phone lifecycle behavior. M1 is merged/pushed as `v0.1.2-1`; M2 stays on its feature branch pending acceptance. GitHub CI is not awaited.


## Verified 0.2.1 release

- Source commit: `63122dd5d9b76567149b10dd64482876237cbc4f`; embedded APK revision checked against it.
- Version: **0.2.1**, internal **0.2.1-1**, versionCode **20101**. Install over 0.2.0 with app data preserved.
- APK: `app/build/outputs/apk/release/deft-0.2.1.apk`, **5,826,189 bytes**.
- APK SHA-256: `1439d362f15f03d661e0a9f630318ed81ef1c4417d757f167ec44b3ac0954166`.
- Persistent certificate SHA-256: `adea6ec2edf4e9e2d809ed06300729e35ca156c06072736717b4a27449233e50`. The existing key was reused; no credentials are committed.
- Release verifier confirmed application ID/version, matching certificate, no INTERNET permission and R8 mapping. R8 minification and resource shrinking remain enabled.
- `check assembleRelease` passed; the source-stamped `:app:assembleRelease` then passed with no application source changes. Logs: `.superpowers/m2-verified-check-release.log` and `.superpowers/m2-0.2.1-stamped-release.log`.
- Matching APK/mapping/hash evidence is archived locally in `.superpowers/releases/0.2.1/` for future crash retracing.

## 0.2.2 — complete feedback retest

Keep the original results/comments above. Record this version's results here. The complete comment-to-change index is `plans/2026-11-19-m2-feedback-coverage.md`. The author confirmed that ratings/favorite/tracking stay in M3, highlights/notes in M5, PDF/CBZ/DjVu readers in M7 and converted formats in M8.

### Update, library and details

- [X] Install `deft-0.2.2.apk` over 0.2.1 without uninstalling. Library folders, filters/sort, books, reader preferences, positions and bookmarks remain.
- [X] Search Books by title, author and words split across both. Case and surrounding spaces do not matter. Search respects current filters and sort. Clear and close restore the previous collection; an unmatched query offers a useful empty state.
-- allow users to turn on/off more advanced searching in settings (implement advanced searching into features, including regex support)
- [X] The floating Continue opens the latest readable book at its saved position, even when search/filter results exclude that book. With no eligible book it is absent.
- [X] Scroll down in both grid and list: the top Continue summary hides. Scroll up: it returns and a return-to-top button appears. Press that button and verify the top is reached smoothly.
- [X] A scrollbar appears during scrolling and fades after stopping. Reach the final book and ensure floating buttons do not cover its actions or metadata.
-- its a bit too small to actually jold and drag, give it a bit more of a handle
- [/] Rescan and inspect the bottom completion notice: message, details action and close control align without crowding. Tap details and verify named added/skipped/error information still works. Notices dismiss automatically and stack safely.
-- it still needs work
- [/] Settings → Library: folder, add/exclude-folder and rescan icons have consistent geometry. Background scan/resume and filter/sort persistence still work.
-- yes they are better now but why are we using handcrafted icons? cant we use other sources of icons? like composable icons? crafting everything by seems like asking for errors down the line and we cant make them customizable/reactive as it would take too much effort, for the entire app (here are some suggestions for vector Composable icon libraries: compose-icons (by DevSrSouza / Composable Horizons), composablehorizons/compose-icons, Material Symbols & Material Design Icons, Simple Icons (Brand/Logo Icons))
- [X] Book detail: Read/Continue sits to the right of the cover at its bottom; description has its own subtle card. Repeat with a long title, absent metadata and 150% text; all content/actions remain reachable.
- [X] Inspect the launcher icon in a circular and rounded-square mask if available: more space surrounds the shorter book mark, with no clipping.

### EPUB content and navigation

- [X] Open the two previously affected books and a book with images, CSS, headings, bold/italic and local links. Text and images render correctly; changing font, size, line height, margins, alignment or hyphenation visibly changes the publication body.
-- some ebooks have errors and the app currently just entirely breaks, it should render all the parts EXCEPT the corrupted part (refer to screenshot, i also attached the corrupted ebook that caused that error)
- [X] Try original, left and justified alignment, publisher-original font, several bundled families and an imported font. Close/reopen the book and open a different book: the same global preferences apply.
- [X] Try Light, Sepia, Dark, Black and a custom theme. Book background/text/links and native reader UI agree, including already-visited neighboring chapters. Test system day/night switching.
-- the themes are constant for the app HUD and the book reader, which makes differentiating the book bg with the app bg, keep the app bg distinct
- [X] In paged mode, horizontal swipes and left/right taps turn pages. Vertical gestures do not turn pages. Pinching does not zoom the book. Repeat with page animation None and Android animations disabled.
-- the tap area for the left and right are too small, make the split of the screen 2-1-2 for left-center-right tap zones
- [X] In scroll mode, scroll across chapter boundaries in both directions. Adjacent chapters form one vertical reading flow, without horizontal chapter paging. Change typography near a boundary and verify the current passage remains nearby.
-- between chapter breaks, add a horizontal line
- [X] Center-tap to hide/show reader controls. Leave revealed controls idle and verify dismissal. Open a sheet and interact with it: it must stay open. The enabled status line is visible when controls hide.
- [\] Long-press text and drag both selection handles near page edges. Selection must remain stable without accidentally turning pages. Cancel selection and verify normal page navigation returns.
-- after long pressing and dragging the handle of the selection, it disappears automatically on release
- [X] Expand/collapse nested TOC groups. Open a chapter, reopen contents and verify the current chapter is visible. Scroll the list, close/reopen it and verify that list position is retained.
-- indent the nested contents a bit more
- [X] Drag the progress slider: chapter/progress preview changes before release. Release to navigate. The footer has no tiny section-divider ticks and uses compact spacing.
-- instead of "section .. of .." in the bottom bar, show the chapter name, and in the title bar at the top, write the title of the book and make the top bar a bit longer to distinctify it
- [X] Reader settings → Page → progress display: switch percentage/page number. The number denotes EPUB locations and must not claim printed page numbers. Check hidden-HUD status and visible progress controls.
-- instead of "location", use "page"
-- the left and right ends of the progress bar arent aligned with the text above them (the section text and the page text)
- [X] Make several jumps using TOC, slider, search and an internal link. System Back returns through those prior positions in order. Ordinary page turns do not add extra jump entries. Sheets/search/image viewer close before Back traverses reading history.
- [X] Search a word appearing in multiple chapters. Inspect snippets, open a result and verify its passage. Back returns to the starting passage. Test no matches, clearing, a new query and closing during search.
-- the search happens at each change, instead, only run when the user clicks a dedicated search button on the right or clicks enter on their keyboard
- [/] Tap a local image. The separate viewer shows only that image and close control. Pinch, pan, close and use system Back; the reader position is unchanged. Repeat with a large image and SVG cover if present.
-- a new window opens with only the close control but the image doesnt show
- [X] Add a bookmark from the reader, find it through the clearly labelled bookmarks entry, rename it, reopen the app, navigate to it and remove it. Notes/highlights remain outside this milestone.

### Lifecycle, errors and accessibility

- [X] Enable Settings → Reader → Page → open last book. Cold-launch Deft and verify the last readable EPUB opens. Back returns to the library without reopening it. Disable the option and cold-launch: library opens normally.
- [X] With launch-last-book enabled, ignore/remove the latest book or revoke its folder access. Startup must remain recoverable, without crashing or looping; history stays intact.
- [X] Rotate while reading, with settings open, with contents open and after a jump. Passage, reading mode and relevant controls restore. Background/foreground and force-stop/reopen retain saved reading position.
- [\] Use a disposable truncated/corrupt EPUB and a book containing a missing chapter/resource. An actionable failure or notice appears, navigation/back remains available, and reopening a healthy book still works.
- [X] Airplane mode: local content, fonts, search and image viewer work. Remote images, publisher scripts and external links cannot initiate network/browser access. Original source files remain untouched.
- [-] At 150% or larger Android text, inspect library/detail, all reader settings tabs, contents/bookmarks/search and notices in portrait/landscape. No clipped controls or overlapping text; all long content scrolls.
- [-] With TalkBack, check search, Continue, return-to-top, TOC expansion, bookmarks, progress and close actions. Disable system animations and repeat navigation; no operation gets stuck waiting for animation.
- [/] Retest brightness, volume-key turns, keep-screen-on and status-line toggles. Leaving the reader restores system window behavior.
-- the status line is still not changing color with the theme (also add just a bit of padding on the right and left)
- [X] Overall: compare Deft with the supplied recordings. Note remaining abrupt motion, confusing controls, excess space or content-rendering issues with a screenshot/recording and steps. Confirm explicitly when M2 is accepted for merge.

- [X] Reader Page settings: disable tap-edge turns. Edge taps now toggle controls instead of turning; swipes and optional volume keys still work. Re-enable and verify left/right turns return.
- [X] Disable fullscreen reading: system bars remain visible when HUD hides, with no overlap over book text/status. Re-enable fullscreen and verify immersive reading and transient system bars.
-- add a bit more padding on the top when fullscreen reading is turned off
- [X] Choose portrait, landscape, then system orientation. Each behaves as labelled; changing the setting preserves the passage and settings sheet. Close/reopen and verify the preference persists.

---

- i have put new screenshots, use them as reference
- the sizing and spacing of the progress bar component on the bottom HUD is off, fix it
- swipe gestures dont actually move the page moving as i drag my finger, only when i let go does the movement actually happen, allow users to see that movement and allow them to cancel a swipe if they return more than halfway back
- when dragging from the edges to use the android back, in fullscreen mode, it doesnt back immediately, instead it shows the notification bar, if the user does this action, dont treat this as a next/previous page swipe (i.e. if it is dragged from the very edge of the screen)


## Verified 0.2.2 release

- APK: `app/build/outputs/apk/release/deft-0.2.2.apk`, **5,896,135 bytes** (5.62 MiB). Install over 0.2.1 without uninstalling.
- Version **0.2.2**, internal **0.2.2-1**, versionCode **20201**.
- Source commit: `b37f231c3b473bdfa44b5acc601f513a9693abb8`; the APK's embedded version-control record matches it. Later handover documentation does not change application source.
- SHA-256: `bd19df04783d188e9a9d9e971d9f1a2d8aa062d5b0d1ce0ce73818728cf1517a`.
- Persistent certificate SHA-256: `adea6ec2edf4e9e2d809ed06300729e35ca156c06072736717b4a27449233e50`. The existing key was reused; no credentials are committed.
- `./gradlew check assembleRelease` passed: **539 test executions, 0 failures, 0 errors, 0 skipped**, including debug/release Android variants. ktlint, detekt, Android lint, network isolation and font budget checks passed. Four Python release-tooling tests passed.
- Installed Chromium verified actual sanitized EPUB resources and Readium scripts/styles, **33 font faces**, local images and live typography/theme changes. Reintroducing the legacy CSP in an isolated fixture reproduced the original blocked-script failure. Continuous-scroll checks passed adjacent chapters in one viewport, wheel crossing, resource/CSS isolation, repeated-search-result context, reflow, selection, missing-chapter recovery and bounded frames.
- The whole-change audit and focused follow-ups have no remaining actionable critical/important findings. Normal/large-text book details, settings and compact reader controls were inspected; visual reference: `plans/design/m2-feedback.html`.
- The source-stamped `:app:assembleRelease` passed after committing verified code. The release verifier confirmed certificate, application identity/version, no INTERNET permission and R8 mapping. R8 minification and resource shrinking remain enabled.
- APK, mapping and hash/source evidence are archived locally at `.superpowers/releases/0.2.2/`. Verification logs: `.superpowers/0.2.2-verified-check-release.log`, `.superpowers/0.2.2-stamped-release.log`, `.superpowers/0.2.2-reader-browser.log`, `.superpowers/0.2.2-continuous-browser.log`.
- No connected Android device or installed emulator was available. Actual phone gestures, rendering, lifecycle and accessibility remain unchecked until the device retest above. M2 remains on `feat/m2-epub-reader` pending acceptance; GitHub CI is not awaited.


## Verified 0.2.3 release

- APK: `app/build/outputs/apk/release/deft-0.2.3.apk`, **5,967,245 bytes** (5.69 MiB). Install over 0.2.2 without uninstalling.
- Version **0.2.3**, internal **0.2.3-1**, versionCode **20301**. Persistent release key reused; R8 minification and resource shrinking enabled.
- Source commit: `be11b9934ff4d1114255d079664624bff2864bcd`; the APK's embedded version-control record matches. Later handover documentation does not change application source.
- SHA-256: `a37e4a5b7ee1ff438a9ccf6fdc1109576315544be4dd2954af57343d604b36b3`.
- Certificate SHA-256: `adea6ec2edf4e9e2d809ed06300729e35ca156c06072736717b4a27449233e50`.
- `./gradlew check assembleRelease` passed after resuming interrupted final packaging. **603 test executions, 0 failures, 0 errors, 0 skipped**, across 132 XML reports including Android debug/release variants. ktlint, detekt, Android lint, network isolation and font budget checks passed. Four Python release-tooling tests passed.
- Actual Homicide EPUB: all 32 HTML/XHTML resources passed production normalization and real Readium CSS injection; source-file hash was unchanged. Original source content was not committed.
- Chromium checks passed Readium resources and all 33 font faces; continuous flow, dividers, selection, reflow, local resources and script isolation; PNG/SVG and the actual Homicide JPEG cover rendered, zoomed and panned.
- Whole-change audit: four actionable findings fixed together; scoped re-review clean. Inspected normal/large-text notices, Material icon renders and large-text reader chrome. Details: `plans/2026-11-20-m2-0.2.3-validation.md` and `plans/design/m2-feedback.html`.
- Source-stamped `:app:assembleRelease` passed; release verifier confirmed persistent certificate, application/version, no INTERNET permission and R8 mapping. APK/mapping/hash/source evidence archived locally at `.superpowers/releases/0.2.3/`.
- Logs: `.superpowers/0.2.3-resumed-check-release.log`, `.superpowers/0.2.3-stamped-release.log`, `.superpowers/0.2.3-image-browser.log`. No connected Android device/emulator; native gestures, selection handles and accessibility still require the retest above.
- Branch remains `feat/m2-epub-reader`; M2 is not merged. GitHub CI is not awaited.


## 0.2.4 — verified handover (2026-11-20)

- APK: `app/build/outputs/apk/release/deft-0.2.4.apk`, **6,079,233 bytes** (5.80 MiB). Install over your existing Deft installation without uninstalling.
- Version: **0.2.4**, versionCode **20401** (`deft.version=0.2.4-1`). R8 minification and resource shrinking remain enabled.
- Source commit embedded in the APK: `2ac258211401ac0fb6d45a695e9068f128dc95d5`. Subsequent handover documentation does not change its source.
- APK SHA-256: `76d1c19874a052d3706e5f24a08c36d02c72a302bde81babe735cfd60bc74d4b`.
- Persistent release certificate SHA-256: `adea6ec2edf4e9e2d809ed06300729e35ca156c06072736717b4a27449233e50`. Verified against the configured key and previous release identity; application ID remains `com.kmab.deft`, with no INTERNET permission.
- Full `ktlintFormat check assembleRelease` passed, with **673 tests across 146 reports**, zero failures/errors/skips. The final source-stamped `assembleRelease` also passed. The original Homicide EPUB regression, Chromium resource/font and continuous-reader checks, and four Python release-tooling tests passed.
- Combined audit findings were addressed, including exact slow-drag targets, TOC fragments in search labels and bounded SVG reference handling. The native image test verifies actual pixels, pinch zoom, pan and dismissal. Updated visual references are in `plans/design/m2-feedback.html`; diagnosis and limits are in `plans/2026-11-20-m2-0.2.4-validation.md`.
- APK, R8 mapping and verification evidence are archived locally in `.superpowers/releases/0.2.4/`.
- The USB connection was unreliable and you instructed us to assume no device access. No successful physical-device run is claimed. Start with the **0.2.4 device feedback retest** at the top; those steps remain unchecked. M2 remains on `feat/m2-epub-reader` pending your acceptance; GitHub CI is not awaited.
