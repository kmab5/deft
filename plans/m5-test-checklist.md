# M5 device test checklist

## 0.5.1 review

Install `deft-0.5.1.apk` over 0.5.0. Start with these checks, then revisit the selection, annotation, layout and tracking checks that the crash blocked below.

- [ ] From Books, open an EPUB with bookmarks and one without. Both display text.
- [ ] From Notes, open an existing bookmark. Verify the correct book and location, turn a page, and return to Notes. Repeat after switching reader modes.
- [ ] Save and reopen a highlight in paged mode, tap it, and edit its note. Repeat after switching modes and rotating.
- [ ] In global Notes, find a book by typing in the filter picker. Combine book, color and type, then select bookmarks. Check active filters, result rows, clearing and large-text layout against your screenshots.

## 0.5.0 device results

Install the signed release `deft-0.5.0.apk` over your current Deft installation.
Do not uninstall: this also tests the Room 7 → 8 upgrade and retained history.
Use disposable books/annotations for deletion checks. Record failures and screenshots
under the relevant section; leave a checkbox unchecked when it has not been tested.
M5 stays on its branch until you accept it. Page notes for fixed-layout formats
remain M8; EPUB is the reader supported by this milestone.

## Update and existing data

- [X] Update installs successfully without losing books, categories/types, bookmarks,
  reading positions, sessions, ratings, favorites or settings.
- [X] Books/Categories/Authors still work, including 0.4.5 bulk category actions and
  creating a category from selected Books. Opening/reading an EPUB still tracks time.
- [X] Notes is a working fourth tab. Normal taps and horizontal swipes reach it;
  Back, settings and last-scan menus behave like the other working tabs.

## Text selection and actions

- [-] Long press text in paged mode, then move both selection handles. The selected
  text and existing haptic feedback are correct. Selection alone creates no entry.
- [-] Repeat in continuous mode, including text across inline emphasis, links and
  paragraphs. Selecting/dragging does not unexpectedly turn pages or scroll away.
- [-] Copy uses the final selected passage, including handle adjustments. Cancel
  the toolbar and reselect elsewhere: it does not use an older selection.
- [-] Share text opens Android's share sheet with the correct passage. Cancel and
  return: reading and selection controls still work, with no unwanted annotation.
- [-] Define hands text to an installed dictionary/text-processing app. Without
  a compatible app, Deft gives a useful notice and continues working.
- [-] Search in book uses the selected text and existing offline search. Results
  jump to the right passage; go-back returns to the previous reading location.
  Turn a page or follow Contents within that chapter: the old search fill clears.

## Highlight and note creation

- [-] Save a purple highlight, then green, amber and gray passages. Each uses a
  soft visible fill and produces one Notes entry. Do this in both reading modes.
- [-] Open the editor, alter color/note and Cancel or Back: nothing is persisted.
- [-] Save a note with color set to none: the note exists but no fill appears in
  the reader. Plain selection without Save never becomes a colorless record.
- [-] Add a note to an existing colored highlight. Change its color to none and
  Save: the note survives and the fill disappears.
- [-] Remove both the color and note from an existing entry and Save: no empty
  annotation remains. A new empty/colorless draft creates no empty entry.
- [-] Repeatedly press Save during a slow operation: one annotation is created,
  and the editor closes only after saving succeeds.

## Range persistence and editing

- [-] Close/reopen the book and restart Deft: each highlight returns to the same
  words. Tap a colored passage to edit it, change color/note and save.
- [-] Highlight one of two identical phrases in a chapter. Only the chosen occurrence
  is decorated after reopening; the other occurrence remains untouched.
- [-] Highlight across styled text and paragraph boundaries. Change font, font size,
  line spacing, margins, theme and orientation: the intended words remain marked.
- [-] Select a passage beside a line break or inline image, including a repeated
  passage. Save/reopen in both modes: the same words remain marked.
- [-] Switch between paged and continuous mode. Stored highlights and notes remain
  anchored to the same text, including duplicate phrases and cross-node selections.
- [-] In continuous mode, scroll several chapters away and back so chapters reload.
  Saved decorations return; edits/deletions do not leave stale fills behind.
- [-] Rotate with an unsaved editor open. Draft text/color survive, Cancel remains
  safe, and Save creates only one entry. Repeat while a save is completing.
- [-] Delete an entry: cancel confirmation first, then confirm. Both Notes and the
  reader update, with no change to the source EPUB or unrelated entries.

## Global Notes

- [X] Notes shows saved highlights, colorless notes, attached notes and existing
  bookmarks, with passage/note text, book title and useful location information.
- [X] Filter by book, color and type independently, then combine them. Notes type
  includes notes attached to colored highlights; Highlights includes those colored
  entries too. Bookmarks filter shows bookmarks only.
- [X] Search passage text, note text, bookmark label, title and author. Search with
  `%`, `_`, apostrophes and non-Latin text: it behaves as literal text and never crashes.
- [X] Clear/reset search and filters, test zero results and an empty notes library.
  Rotate or leave/return: controls remain coherent and no hidden filter is confusing.
- [\] Tap a passage/bookmark: the correct book opens at its saved location, even
  when a different book was last opened. Reader Back returns normally.
- [\] Open a colorless note from Notes in both modes: it stays unfilled. Search
  for the same passage separately: the temporary search indication still works.
- [X] Edit/delete an entry from Notes and verify the reader reflects it. Rename a
  bookmark and check both Notes and the existing reader bookmark list.

## Per-book Notes and Markdown export

- [X] Book detail switches between details and notes. Its count and list are scoped
  to that book and update after saves/deletions; other books' entries stay separate.
- [X] Export the book's notes through Android's share sheet. Save/open the `.md`
  file: title/author, passages, notes, bookmarks and locations are readable and scoped.
- [/] Include multiline notes and Markdown punctuation. The export preserves content
  without broken structure or private SAF/file paths. Cancel sharing and retry safely.
- [X] Export a book with no notes: the result/empty-state handling is clear.
-- share button not visible which is the correct behavior

## Missing, ignored and forgotten books

- [X] Make a test EPUB unavailable and rescan. Notes/bookmarks remain visible,
  editable and exportable with a missing-file indication. Opening gives a useful notice.
- [X] Restore the same file and rescan: the existing annotations reattach and jump
  correctly, without duplication or transferred annotations to a different file.
- [X] Ignore a test book: its annotations disappear from global Notes and export.
  Un-ignore: the same annotations return. Ordinary missing files must stay visible.
- [X] Forget a disposable book using the existing confirmation: its annotations
  disappear permanently along with its other Deft history; the source file remains.

## Layout, accessibility and tracking

- [-] In Paper and Ink, portrait/landscape and 150% text: entries, filters, color
  choices and pinned save/cancel actions stay legible and reachable, including keyboard open.
- [X] TalkBack names every color (including none), actions and selected controls;
  actions have usable touch targets and important meaning does not rely on color alone.
- [-] Opening an editor or external share/dictionary app pauses reading time; returning
  resumes correctly. Editing notes must not artificially add page turns or reading time.
- [X] Airplane mode: notes, highlights, filters, local search and Markdown generation
  work. No remote services, accounts or permissions are required.

## Acceptance

- [\] M5 accepted; ready to merge the tested version into main.

Feedback:

- the book reader is completely ruined, no book opens, it automatically crashes, due to this i wasnt able to carry out most of the tests, however i had some saved bookmarks from before and i was able to test some of them. the crash log when i opened a book with bookmarks, opened a book from the bookmark, and opened a book without any bookmarks is as follows:
--------- beginning of crash
11-26 13:34:23.600 25056 25056 E AndroidRuntime: FATAL EXCEPTION: main
11-26 13:34:23.600 25056 25056 E AndroidRuntime: Process: com.kmab.deft, PID: 25056
11-26 13:34:23.600 25056 25056 E AndroidRuntime: java.lang.IllegalStateException: Can't access ViewModels from detached fragment
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at b5.v.g(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:51)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at be.u.a(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:79)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at androidx.lifecycle.b1.getValue(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:7)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at be.v.P(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:3)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at n8.h1.a(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:562)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at n8.j2.onViewAttachedToWindow(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:20)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at android.view.View.dispatchAttachedToWindow(View.java:24985)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:3954)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:3961)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at android.view.ViewGroup.addViewInner(ViewGroup.java:5835)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at android.view.ViewGroup.addView(ViewGroup.java:5595)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at android.view.ViewGroup.addView(ViewGroup.java:5535)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at android.view.ViewGroup.addView(ViewGroup.java:5507)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at o3.c.F(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:125)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at o2.h0.b(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:265)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at o2.h0.b(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:240)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at o2.h0.B(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:55)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at o2.c2.c(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:7)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at e1.r.d(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:144)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at e1.l0.R(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:30)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at e1.t.d(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:53)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at e1.l0.R(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:30)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at e1.a.Q(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:3)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at d1.x.e(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:93)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at d1.x.d(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:6)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at d1.y1.F(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:230)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at p2.u0.doFrame(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:7)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at p2.s0.doFrame(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:48)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at android.view.Choreographer$CallbackRecord.run(Choreographer.java:1986)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at android.view.Choreographer$CallbackRecord.run(Choreographer.java:1997)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at android.view.Choreographer.doCallbacks(Choreographer.java:1440)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at android.view.Choreographer.doFrame(Choreographer.java:1383)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at android.view.Choreographer$FrameDisplayEventReceiver.run(Choreographer.java:1957)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at android.os.Handler.handleCallback(Handler.java:1095)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at android.os.Handler.dispatchMessageImpl(Handler.java:135)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at android.os.Handler.dispatchMessage(Handler.java:125)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at android.os.Looper.loopOnce(Looper.java:321)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at android.os.Looper.loop(Looper.java:422)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at android.app.ActivityThread.main(ActivityThread.java:10616)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at java.lang.reflect.Method.invoke(Native Method)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:635)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:982)
11-26 13:34:23.600 25056 25056 E AndroidRuntime:        Suppressed: ad.f: [d1.l1@e2361da, p2.c2@57d460b, z{Cancelling}@9305ce8, t0@9566f01]
11-26 13:34:30.517 25366 25366 E AndroidRuntime: FATAL EXCEPTION: main
11-26 13:34:30.517 25366 25366 E AndroidRuntime: Process: com.kmab.deft, PID: 25366
11-26 13:34:30.517 25366 25366 E AndroidRuntime: java.lang.IllegalStateException: Can't access ViewModels from detached fragment
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at b5.v.g(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:51)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at be.u.a(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:79)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at androidx.lifecycle.b1.getValue(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:7)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at be.v.P(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:3)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at n8.h1.a(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:562)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at n8.j2.onViewAttachedToWindow(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:20)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at android.view.View.dispatchAttachedToWindow(View.java:24985)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:3954)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:3961)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at android.view.ViewGroup.addViewInner(ViewGroup.java:5835)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at android.view.ViewGroup.addView(ViewGroup.java:5595)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at android.view.ViewGroup.addView(ViewGroup.java:5535)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at android.view.ViewGroup.addView(ViewGroup.java:5507)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at o3.c.F(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:125)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at o2.h0.b(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:265)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at o2.h0.b(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:240)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at o2.h0.B(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:55)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at o2.c2.c(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:7)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at e1.r.d(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:144)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at e1.l0.R(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:30)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at e1.t.d(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:53)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at e1.l0.R(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:30)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at e1.a.Q(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:3)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at d1.x.e(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:93)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at d1.x.d(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:6)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at d1.y1.F(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:230)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at p2.u0.doFrame(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:7)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at p2.s0.doFrame(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:48)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at android.view.Choreographer$CallbackRecord.run(Choreographer.java:1986)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at android.view.Choreographer$CallbackRecord.run(Choreographer.java:1997)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at android.view.Choreographer.doCallbacks(Choreographer.java:1440)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at android.view.Choreographer.doFrame(Choreographer.java:1383)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at android.view.Choreographer$FrameDisplayEventReceiver.run(Choreographer.java:1957)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at android.os.Handler.handleCallback(Handler.java:1095)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at android.os.Handler.dispatchMessageImpl(Handler.java:135)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at android.os.Handler.dispatchMessage(Handler.java:125)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at android.os.Looper.loopOnce(Looper.java:321)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at android.os.Looper.loop(Looper.java:422)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at android.app.ActivityThread.main(ActivityThread.java:10616)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at java.lang.reflect.Method.invoke(Native Method)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:635)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:982)
11-26 13:34:30.517 25366 25366 E AndroidRuntime:        Suppressed: ad.f: [d1.l1@253f422, p2.c2@66d7eb3, z{Cancelling}@aabac70, t0@842bbe9]
11-26 13:34:38.146 25681 25681 E AndroidRuntime: FATAL EXCEPTION: main
11-26 13:34:38.146 25681 25681 E AndroidRuntime: Process: com.kmab.deft, PID: 25681
11-26 13:34:38.146 25681 25681 E AndroidRuntime: java.lang.IllegalStateException: Can't access ViewModels from detached fragment
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at b5.v.g(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:51)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at be.u.a(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:79)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at androidx.lifecycle.b1.getValue(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:7)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at be.v.P(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:3)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at n8.h1.a(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:562)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at n8.j2.onViewAttachedToWindow(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:20)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at android.view.View.dispatchAttachedToWindow(View.java:24985)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:3954)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:3961)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at android.view.ViewGroup.addViewInner(ViewGroup.java:5835)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at android.view.ViewGroup.addView(ViewGroup.java:5595)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at android.view.ViewGroup.addView(ViewGroup.java:5535)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at android.view.ViewGroup.addView(ViewGroup.java:5507)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at o3.c.F(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:125)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at o2.h0.b(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:265)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at o2.h0.b(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:240)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at o2.h0.B(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:55)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at o2.c2.c(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:7)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at e1.r.d(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:144)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at e1.l0.R(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:30)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at e1.t.d(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:53)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at e1.l0.R(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:30)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at e1.a.Q(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:3)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at d1.x.e(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:93)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at d1.x.d(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:6)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at d1.y1.F(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:230)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at p2.u0.doFrame(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:7)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at p2.s0.doFrame(r8-map-id-9a93bad07f741c3e2592a01c0270103d53641ddff72618d472c826fd3d561e6f:48)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at android.view.Choreographer$CallbackRecord.run(Choreographer.java:1986)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at android.view.Choreographer$CallbackRecord.run(Choreographer.java:1997)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at android.view.Choreographer.doCallbacks(Choreographer.java:1440)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at android.view.Choreographer.doFrame(Choreographer.java:1383)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at android.view.Choreographer$FrameDisplayEventReceiver.run(Choreographer.java:1957)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at android.os.Handler.handleCallback(Handler.java:1095)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at android.os.Handler.dispatchMessageImpl(Handler.java:135)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at android.os.Handler.dispatchMessage(Handler.java:125)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at android.os.Looper.loopOnce(Looper.java:321)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at android.os.Looper.loop(Looper.java:422)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at android.app.ActivityThread.main(ActivityThread.java:10616)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at java.lang.reflect.Method.invoke(Native Method)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:635)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:982)
11-26 13:34:38.146 25681 25681 E AndroidRuntime:        Suppressed: ad.f: [d1.l1@d2526c0, p2.c2@3202ff9, z{Cancelling}@6e87b3e, t0@67d8d9f]
- i have put some screenshots of the global notes tab, the filtering ui/ux needs work
