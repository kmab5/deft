# M3 — reading tracking device checklist

Install `deft-0.3.0.apk` as an update; do not uninstall. The persistent release key preserves M1/M2 data. Complete these on your phone before M3 merge. Local checks, audit findings and signed artifact identity are recorded in [the validation report](2026-11-20-m3-validation.md).

## Upgrade and previous reader behavior
- [ ] Update from 0.2.5: folders, books, covers, missing/ignored state, filters, fonts/themes, bookmarks and positions remain. Previously opened M2 books have no fabricated reading history.
- [ ] Open EPUBs in paged and continuous modes. Try page turns, slow swipes, chapter changes, search, TOC jumps, selection, bookmarks, image zoom, external-link confirmation and Back. M2 behavior remains intact.
- [ ] Open a missing or Protected book: clear existing message; no reading time is created. Rating/favorite/review and manual state remain usable.

## Session accuracy
- [ ] Open a not-read book, read/interact for less than 60 seconds, leave: no saved session or automatic Reading state.
- [ ] Read for at least 60 seconds, leave: a session and first read-through appear, state Reading. Reopen and read again: another session under the same read-through.
- [ ] Set reflowable idle timeout 1 minute. Read with regular page/scroll interactions for 3 minutes, stop interacting for at least 2 minutes, then leave: idle gap is excluded. Repeat with default 5-minute timeout and 6-minute idle gap; total is about 3 minutes, not 9 minutes.
- [ ] After idle pause, turn a page and keep reading; only resumed reading is added. Repeated idle/resume does not duplicate earlier time.
- [ ] Background the app, lock the screen, switch books and rotate during reading. Background/locked time is excluded, and rotation does not double-count. Rotation and reader overlays split visible sessions; each segment still needs 60 seconds to be retained.
- [ ] With idle timeout Off and keep-screen-on enabled, leave untouched for 15 minutes: keep-awake is released. After the screen sleeps, time stops. Reading interaction restores keep-awake appropriately.
- [ ] After a qualifying session has checkpointed, force-stop Deft and reopen: saved history survives without counting the app-closed interval. No duplicate session appears. With a finite idle timeout, crash recovery keeps confirmed reading only: the current page’s unconfirmed dwell and up to 30 seconds of confirmed time may be lost.
- [ ] Change device clock while reading: duration is sensible, not inflated by the wall-clock jump.
- [ ] Page turns/scroll steps count reading movement. TOC/search/progress jumps, typography reflow and opening settings do not create huge page counts or reset reading idle incorrectly.
- [ ] Enable the quiet reader indicator, then disable it. Its state follows real tracking without obstructing text or gestures.

## Reading states and read-throughs
- [ ] Not read becomes Reading only after a qualifying saved session.
- [ ] Mark Reading as Paused; short reopen alone preserves Paused, a qualifying session resumes the same read-through.
- [ ] Mark Abandoned; reopening offers Resume or Start over. Resume retains the read-through; Start over creates another. Cancel/Back starts neither choice silently.
- [ ] Finish a book manually; pick a finish date. Reopen: Re-read starts a new read-through and resets position; Just browsing retains finished state and position.
- [ ] Read a browsing session for at least 60 seconds. It appears labelled Browsing in book history, but is excluded from reading totals/finish summary.
- [ ] Reach the last page/99% while Reading: Mark as finished appears once. Decline, navigate away/back and reopen: it does not repeat for that read-through. A new read-through can prompt again.
- [ ] Accept Mark as finished: completion date and state update once; the finish flow appears without duplicate sessions.
- [ ] Manually select all five states. Finished asks for date; resetting to Not read warns before removing read-through history. Cancel keeps it.
- [ ] Edit read-through start/end dates; invalid reversed dates are rejected. Delete one read-through with confirmation; latest remaining read-through determines state and its sessions remain intact. Cancelling deletion changes nothing.
- [ ] Ignore and un-ignore a book; history returns intact. Remove/move source files and rescan; missing books retain history. Only confirmed Forget removes it permanently.

## Ratings, library and settings
- [ ] Set ratings 0.5, 3.5 and 5, clear the rating, toggle favorite and save a multiline review. Reopen/restart; values persist, no file is modified.
- [ ] Use state, favorite and rating filters with existing format/missing/search filters. Change rating sort/direction, restart app: choices remain. Book states update immediately after reading/manual changes.
- [ ] Tracking settings accept both reflowable/fixed 1–60 minute timeouts and Off; defaults are 5/10 minutes. Restart and verify persistence; no invalid value is accepted.
- [ ] Appearance celebrations toggle persists. Tracking controls explain the minimum-session behavior without unnecessary technical details.

## How does it look? Finish flow and visual review
- [ ] Finish with animations enabled: purple/green fields, cover, finished label and time/session/day counters play smoothly; it exits automatically around 2.8 seconds to rating/favorite sheet.
- [ ] Tap during celebration: skip cleanly to rating/favorite. Dismiss sheet and revisit book: celebration does not replay.
- [ ] Disable celebrations; finish another read-through: quiet finished/rating sheet only. Repeat with Android animation scale off.
- [ ] Rotate/background during finish flow: no duplicate finish or lost state; controls remain usable.
- [ ] Review book history/rating/review cards, Tracking/Appearance settings and library states in light/dark themes, portrait/landscape and 150% text. No clipped labels, awkward blank space or inaccessible actions.
- [ ] Enable volume-key page turns: one subtle tick per enabled press, none on release/repeat or when disabled. State confirmation and celebration burst use subtle feedback, respecting Android haptic settings.
- [ ] TalkBack announces state/rating/favorite/history controls; touch targets are usable. Motion, sheets, errors and state changes match the polished M2 UI.

## Feedback

Write observations beneath the relevant checkbox; add screenshots/recordings to the feedback folder. M4 organization, M5 highlights/notes and M6 global stats/goals remain separate milestones.
