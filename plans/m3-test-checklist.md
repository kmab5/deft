# M3 — reading tracking device checklist

**Archived device review:** M3 merged as `v0.3.2-1`, with follow-up `v0.3.3-1` and the later idle-label adjustment included in accepted M4. The author authorized the final fixes and merge without another feedback gate. Original reviews and comments are preserved below; see the [documentation index](README.md) for current releases.

## Original 0.3.0 review

Install `deft-0.3.0.apk` as an update; do not uninstall. The persistent release key preserves M1/M2 data. Complete these on your phone before M3 merge. Local checks, audit findings and signed artifact identity are recorded in [the validation report](2026-11-20-m3-validation.md).

## Upgrade and previous reader behavior
- [X] Update from 0.2.5: folders, books, covers, missing/ignored state, filters, fonts/themes, bookmarks and positions remain. Previously opened M2 books have no fabricated reading history.
- [X] Open EPUBs in paged and continuous modes. Try page turns, slow swipes, chapter changes, search, TOC jumps, selection, bookmarks, image zoom, external-link confirmation and Back. M2 behavior remains intact.
- [X] Open a missing or Protected book: clear existing message; no reading time is created. Rating/favorite/review and manual state remain usable.

## Session accuracy
- [X] Open a not-read book, read/interact for less than 60 seconds, leave: no saved session or automatic Reading state.
- [\] Read for at least 60 seconds, leave: a session and first read-through appear, state Reading. Reopen and read again: another session under the same read-through.
-- cant save any reading session: gives the error "Reading history couldnt be saved. Check available storage; Deft will retry" (i have over 30gbs free, it isnt a storage issue) also the text and the info icon arent centered in the notification box
-- drop the minimum limit to 30 seconds to change state (make it editable in the settings)
- [-] Set reflowable idle timeout 1 minute. Read with regular page/scroll interactions for 3 minutes, stop interacting for at least 2 minutes, then leave: idle gap is excluded. Repeat with default 5-minute timeout and 6-minute idle gap; total is about 3 minutes, not 9 minutes.
- [-] After idle pause, turn a page and keep reading; only resumed reading is added. Repeated idle/resume does not duplicate earlier time.
- [-] Background the app, lock the screen, switch books and rotate during reading. Background/locked time is excluded, and rotation does not double-count. Rotation and reader overlays split visible sessions; each segment still needs 60 seconds to be retained.
- [-] With idle timeout Off and keep-screen-on enabled, leave untouched for 15 minutes: keep-awake is released. After the screen sleeps, time stops. Reading interaction restores keep-awake appropriately.
- [-] After a qualifying session has checkpointed, force-stop Deft and reopen: saved history survives without counting the app-closed interval. No duplicate session appears. With a finite idle timeout, crash recovery keeps confirmed reading only: the current page’s unconfirmed dwell and up to 30 seconds of confirmed time may be lost.
- [-] Change device clock while reading: duration is sensible, not inflated by the wall-clock jump.
- [-] Page turns/scroll steps count reading movement. TOC/search/progress jumps, typography reflow and opening settings do not create huge page counts or reset reading idle incorrectly.
-- cant test these, any reading session isnt saving
- [X] Enable the quiet reader indicator, then disable it. Its state follows real tracking without obstructing text or gestures.
-- move the indicator from above the status line to on the status line on the center (left aligned: page, center: reading status, right: time and battery (for the battery icon, if the phone is charging, add a small lightning text next to it))

## Reading states and read-throughs
- [-] Not read becomes Reading only after a qualifying saved session.
- [-] Mark Reading as Paused; short reopen alone preserves Paused, a qualifying session resumes the same read-through.
- [X] Mark Abandoned; reopening offers Resume or Start over. Resume retains the read-through; Start over creates another. Cancel/Back starts neither choice silently.
- [X] Finish a book manually; pick a finish date. Reopen: Re-read starts a new read-through and resets position; Just browsing retains finished state and position.
- [-] Read a browsing session for at least 60 seconds. It appears labelled Browsing in book history, but is excluded from reading totals/finish summary.
- [X] Reach the last page/99% while Reading: Mark as finished appears once. Decline, navigate away/back and reopen: it does not repeat for that read-through. A new read-through can prompt again.
- [\] Accept Mark as finished: completion date and state update once; the finish flow appears without duplicate sessions.
- [X] Manually select all five states. Finished asks for date; resetting to Not read warns before removing read-through history. Cancel keeps it.
- [X] Edit read-through start/end dates; invalid reversed dates are rejected. Delete one read-through with confirmation; latest remaining read-through determines state and its sessions remain intact. Cancelling deletion changes nothing.
-- Add an Android date picker to make things easier
- [X] Ignore and un-ignore a book; history returns intact. Remove/move source files and rescan; missing books retain history. Only confirmed Forget removes it permanently.

## Ratings, library and settings
- [X] Set ratings 0.5, 3.5 and 5, clear the rating, toggle favorite and save a multiline review. Reopen/restart; values persist, no file is modified.
-- dont use an actual toggle button for favoriting, use the heart icon itself, for a favorite, a filled out heart with a little popping animation when favoriting and unfavoriting
-- the alignment of half star ratings is Off
-- the general design of the new additions (the rating, the change state button, the favorite button, the padding and spacing in the your review section, the reading history section) needs a lot of work, refer to the screenshots; the design is haphazardous, and unorganized, wrong usage of empty space, unintuitive and outside of the norm, spacing and alignment issues are proliferent.
- [X] Use state, favorite and rating filters with existing format/missing/search filters. Change rating sort/direction, restart app: choices remain. Book states update immediately after reading/manual changes.
- [X] Tracking settings accept both reflowable/fixed 1–60 minute timeouts and Off; defaults are 5/10 minutes. Restart and verify persistence; no invalid value is accepted.
- [X] Appearance celebrations toggle persists. Tracking controls explain the minimum-session behavior without unnecessary technical details.
-- make it last until the user taps the screen or the eased in appearance of the close button

## How does it look? Finish flow and visual review
- [X] Finish with animations enabled: purple/green fields, cover, finished label and time/session/day counters play smoothly; it exits automatically around 2.8 seconds to rating/favorite sheet.
- [X] Tap during celebration: skip cleanly to rating/favorite. Dismiss sheet and revisit book: celebration does not replay.
- [X] Disable celebrations; finish another read-through: quiet finished/rating sheet only. Repeat with Android animation scale off.
- [X] Rotate/background during finish flow: no duplicate finish or lost state; controls remain usable.
- [X] Review book history/rating/review cards, Tracking/Appearance settings and library states in light/dark themes, portrait/landscape and 150% text. No clipped labels, awkward blank space or inaccessible actions.
- [X] Enable volume-key page turns: one subtle tick per enabled press, none on release/repeat or when disabled. State confirmation and celebration burst use subtle feedback, respecting Android haptic settings.
- [-] TalkBack announces state/rating/favorite/history controls; touch targets are usable. Motion, sheets, errors and state changes match the polished M2 UI.

## Feedback

Write observations beneath the relevant checkbox; add screenshots/recordings to the feedback folder. M4 organization, M5 highlights/notes and M6 global stats/goals remain separate milestones.

- the turned off form of any toggle button has too little contrast with the circle inside
- add an about page in the settings
- in the settings, add a bit more vertical padding to the titles (settings, library settings, reader, ...)
- currently, the app theme itself cant be changed, only the reader theme, implement an app theme changer in the appearance settings


## 0.3.1 — device retest

Install `deft-0.3.1.apk` over the existing app; do not uninstall. The same release key is used. Local validation and the timestamp failure explanation are in [the 0.3.1 report](2026-11-20-m3-031-validation.md). These boxes are for device results, not local automated checks.

### Session saving and accuracy
- [X] Upgrade: books, folders, reader settings, positions, bookmarks, manual states, dates, ratings/favorite/reviews and existing history remain.
- [X] Tracking → minimum session defaults to **30 seconds**. Read a not-read book for 35–45 seconds, turn a page and leave: a session is saved and state becomes Reading. No save-error notice appears.
- [X] Read the same book for 2 minutes with several page turns, leave, reopen and read again: two sessions under one read-through; the first session does not duplicate. Test both paged and continuous reading.
- [X] Leave after less than 30 seconds: no new session. Set minimum to 10 seconds, read 15 seconds and leave: session is saved and its duration is shown in seconds. Change minimum to 120; previous shorter accepted sessions remain in history/totals after restart.
- [X] Edit minimum to 1 and 300 seconds; both save. Blank, 0, negative, decimal and 301 are rejected. Restart retains the last valid value. Restore 30 seconds for the remaining tests.
- [X] With 1-minute idle pause, read with regular interactions for 3 minutes, then leave untouched for 2 minutes and exit: only active reading counts. Repeat with default 5 minutes and a 6-minute idle gap: about 3 minutes, not 9.
-- for idle pauses, show a very translucent film over the screen (like accidental touch protection of android) that disappears when the screen is touched with a very faint "idling" text that pulses
- [X] After idle pause, turn a page and read again; resumed reading is added once. Page turns/scroll count movement, while TOC/search/progress jumps and typography changes do not inflate page counts.
- [X] Background, lock, rotate and switch books: hidden/locked time is excluded. Each visible segment needs the selected minimum. Return and continue without duplicate time.
- [X] With idle pause Off, leave untouched for 15 minutes: keep-awake releases, screen-off stops time, and the next reading interaction restores keep-awake.
- [X] Read/interact long enough to checkpoint, force-stop and reopen: confirmed history survives without counting closed time or duplicating a session. Current-page unconfirmed dwell and up to 30 seconds of confirmed time may be lost after force-stop.
- [X] Change the device clock while reading: duration remains sensible. Mark Reading as Paused: a short reopen preserves Paused; a qualifying session resumes the same read-through.
- [X] Open a finished book as Just browsing and read for 35–45 seconds: labelled Browsing session appears, excluded from reading totals. Re-read instead creates a new read-through and resets position.
- [X] Reach the end and accept the finish prompt: session saves, state finishes, celebration appears once. Declining still suppresses repeats for that read-through.

### Book controls and dates
- [X] State control opens all five states. Rating, heart and review have clear grouping, aligned spacing and no wasted rows at normal and 150% text in both app themes.
--
- [X] Set 0.5, 3.5 and 5 stars: filled areas align with outlines, the value is readable, and clear rating works. Tap the heart on/off: filled/outline state, a subtle pop each way, and persisted value after restart.
- [X] Write/edit/clear a multiline review; text stays readable and controls remain usable. Reading history clearly separates read-throughs with dates and real totals. Open session details and the history management menu.
- [X] Manual Finished opens a calendar. Cancel, reopen and choose a date; no state changes on cancel. Edit start/end dates through calendars, including year/month navigation; invalid reversed dates are rejected. Cancel/delete confirmations preserve existing history unless explicitly confirmed.
- [X] Ignore/un-ignore and missing files preserve history. Rating and manual state remain usable on missing/Protected books. Filters and rating/state sorts still update and persist.

### Reader, appearance and completion
- [X] Enable reading indicator: page on left, reading/paused centered, clock/battery on right. Plug/unplug charging: the bolt follows charging. Test 150% text with no overlap. Disable status details: the separately enabled reading indicator remains centered. Disable indicator: it disappears.
- [X] Any notice has centered message/icon/close, including a multiline error and a scan notice with action. Notices still slide, stack and dismiss correctly.
- [X] Every settings switch has a clearly visible off thumb/track in Light, Dark and reader themes. Settings, Library, Reader, Tracking, Appearance and About titles have consistent vertical breathing room.
- [X] Appearance → System/Light/Dark changes library, book and settings immediately, including readable system-bar icons. Restart retains it. System follows Android; reader theme remains independently selected.
- [X] About shows installed version 0.3.1, author, GPL license, source and offline privacy information. Source opens the browser only after tapping its action. Back returns to Settings.
- [X] Finish with celebrations enabled: intro plays and then stays on screen beyond 5 seconds. Close eases in. Tap anywhere or close to open rating/favorite; do not auto-exit. Tap early also skips cleanly.
- [X] Rotate/background the held celebration, then dismiss: no replay or duplicate completion. With celebrations or Android animation scale off, finish goes directly to the quiet sheet. Heart motion respects disabled animations.
- [X] TalkBack announces state, half-star rating, favorite selected state, history actions, date and theme choices. At large text/landscape all actions remain reachable, without clipping.

### Feedback for 0.3.1

Add observations under the relevant checkbox and new screenshots/recordings to `plans/feedback/`. M3 remains on its feature branch until this review is accepted.

- in the timer settings, put the apply buttons next to the inputs instead of on a new line
- in the about, instead of Samuel Worash Getaneh, make it kmab and make it link to kmab5.github.io, and for the gpl-3.0 link, put it next to the source code buttons and give them both icons
- the rating stars ui/design is still bad, the half ratings are working but the organizations and sizing and empty speace and orientation is bad, fix it, the other redesigned parts are good
- in the reading state selector, use different colors for each state abd use icons for them and the orientation and spacing, the way they take up the entire width does not look good, fix the design
- we need haptics in our app, when selecting words, when celebrating, on back, etc etc
- for when we start higlighting features, write down allowing different color highlights (including a colorless one that means no highlight when selecting)
- feature implementation ideas, put these in the correct m phase where they belong:
-- auto scroll feature based on reading speed or custom input intervals
- i just want your thoughts on these ideas, no implementation, just a reality check:
-- as of now, we havent implemented file type support for any other types except epub. can we make these file type support additional features/plugins that users can download when they want? can we make it modular and allow the development of any kind of file support as long as they are coded? maybe we can take it further by making a module for online book sources like royalroad, ao3, etc etc. ideas on this?
-- i want your thoughts alone (without the modular thing) for how we could support online fiction platforms (like royalroad) and how we can implement it into our app


## 0.3.2 — final polish coverage

The author accepted 0.3.1 and requested these final changes before merging M3.
These optional device smoke checks document changes; they are not claimed as
physical-device tests or a new merge gate.

- [X] Upgrade from 0.3.1 without uninstalling; session history and all preferences remain.
- [X] Set idle pause to one minute, stop interacting, and see a faint film with pulsing
  `idling`. Tap once: film fades, tracking resumes, page stays unchanged. A subsequent
  tap/gesture reads normally. No idle time or fake page turn enters history.
-- the translucent film is completely invisible, make it more opaque and black
- [X] Open/close reader settings, background/lock, then return: no stale idle film
  covers dialogs or starts tracking while hidden. Disable animations: idle label is steady.
- [X] Timer Apply buttons sit beside their fields in normal and 150% text. Apply and
  keyboard Done both save valid values and reject invalid values; Off still works.
- [X] About links `kmab` to https://kmab5.github.io. Icon-led source and GPL actions
  are grouped and open the correct external pages only after a tap.
- [X] Book and finish-sheet rating controls are compact, with clear and heart aligned.
  Select half stars by touch and TalkBack; clear/favorite and large text remain usable.
- [X] Reading-state choices wrap with icons and semantic colors, and clearly identify
  the current state. Confirm Finished/Not read prompts still protect dates/history.
- [X] With Android touch vibration enabled, native word selection, Back, direct
  assessment changes, state confirmation and celebration have subtle feedback.
  Selection-handle movement and stale/repeated Back do not add app-generated pulses.
  Disable system touch vibration: Deft does not bypass that setting.
-- selecting texts isnt giving haptics

### Requested planning notes and thoughts

- M5: four highlight colors plus colorless/no-highlight; selecting text alone does
  not persist a highlight, and removing color retains any explicitly saved note.
- M6 after speed metrics: opt-in auto-scroll using measured speed or a custom interval,
  with idle/visibility safeguards and no false reading interactions from automatic motion.
- [Format extensions and online fiction: my assessment](2026-11-21-format-and-online-fiction-thoughts.md).
  This is discussion only; neither feature is implemented or approved by these notes.

## 0.3.3 follow-up

The author passed the 0.3.2 checks and requested a darker black idle film and reliable text-selection feedback before M4. These changes are included in the M4 test build too.

- [/] Idle pause visibly dims light, sepia and dark pages with a 38% black film; the faint label pulses, or stays steady with animations off. The consumed wake touch keeps the same page and does not count idle time.
-- "idling" label is still not visible, add a darker bg for it, snapchat style, fully horizontal bar, make this one significantly darker so the text is visible, and make it pulse with the text from the opacity of the film to its own darker opacity, make the film darker
- [X] Selecting a word in paged and continuous EPUB gives one subtle long-press pulse with system vibration enabled. Moving handles or recreating the toolbar does not repeat it. Clearing and selecting again pulses again.
- [X] Background/lock during selection stops observation; returning does not vibrate for a stale selection. Disable Android touch vibration: no app-generated selection pulse.

### 2026-11-22 adjustment

The idle film is now 55% black with a full-width, darker pulsing strip and white `idling` label. Reduced motion keeps the strip steady and visible. Committed directly on main without a build as requested; included in the next M4 release verification. Original review comments above are preserved.
