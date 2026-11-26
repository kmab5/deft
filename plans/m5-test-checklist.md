# M5 device test checklist

## 0.5.2 review

Install `deft-0.5.2.apk` over 0.5.1 and check the following in paged and continuous mode.

- [ ] Take a hardware-button screenshot in Books and another in the reader. Check both original filenames in Gallery for `_Deft`.
- [ ] Hold a word without releasing. Feel the first selection tick, then move each handle across several characters and feel the changes.
- [ ] Select text near the top, middle and bottom of a scrolled chapter. The floating menu follows the visible selection. Open an installed text-processing app from the menu and verify it receives the final selection.
- [ ] In a yellow-accent reader theme, choose purple. Check all four colors for visibility in light and dark reading themes. Switch between fill and underline, reopen the book and switch reading modes. Existing highlights keep their fill, and colorless notes stay unmarked.
- [ ] Mark a multiline passage in continuous mode. Only the selected lines are marked. Scroll away and back, change font size and tap the passage to edit.
- [ ] Open the reader contents panel. Find highlights and colorless notes in the new tab and jump to their locations. Test an empty book too.
- [ ] Swipe both ways between Book details and Notes. Check the divider between an excerpt and its attached note, and confirm the exported Markdown labels the note.

## 0.5.1 review

Install `deft-0.5.1.apk` over 0.5.0. Start with these checks, then revisit the selection, annotation, layout and tracking checks that the crash blocked below.

- [X] From Books, open an EPUB with bookmarks and one without. Both display text.
- [X] From Notes, open an existing bookmark. Verify the correct book and location, turn a page, and return to Notes. Repeat after switching reader modes.
- [X] Save and reopen a highlight in paged mode, tap it, and edit its note. Repeat after switching modes and rotating.
- [X] In global Notes, find a book by typing in the filter picker. Combine book, color and type, then select bookmarks. Check active filters, result rows, clearing and large-text layout against your screenshots.

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

- [/] Long press text in paged mode, then move both selection handles. The selected
  text and existing haptic feedback are correct. Selection alone creates no entry.
-- the haptics arent timed right. it should vibrate on long press, not on letting go of the screen. again it should vibrate on every move of the handles (it currently doesnt vibrate at all on movement of handles)
- [/] Repeat in continuous mode, including text across inline emphasis, links and
  paragraphs. Selecting/dragging does not unexpectedly turn pages or scroll away.
-- the selection menu appears far away from the actual selected text on scroll mode, not immediately above the text
- [X] Copy uses the final selected passage, including handle adjustments. Cancel
  the toolbar and reselect elsewhere: it does not use an older selection.
- [X] Share text opens Android's share sheet with the correct passage. Cancel and
  return: reading and selection controls still work, with no unwanted annotation.
- [X] Define hands text to an installed dictionary/text-processing app. Without
  a compatible app, Deft gives a useful notice and continues working.
- [X] Search in book uses the selected text and existing offline search. Results
  jump to the right passage; go-back returns to the previous reading location.
  Turn a page or follow Contents within that chapter: the old search fill clears.
-- in the actions in the selection menu, i have screenshoted the way lithium uses theirs. they dont only have their own defined actions but allow other apps context menu options to appear. lets try to be like theirs

## Highlight and note creation

- [/] Save a purple highlight, then green, amber and gray passages. Each uses a
  soft visible fill and produces one Notes entry. Do this in both reading modes.
-- the in book purple color is actually yellow (my theme main color), make it purple
-- the highlighting isnt actually very visible, make the color much bolder
-- allow underline toggle for highlighting (to make the highlighting only underline in the selected color instead of a full bg highlight)
- [X] Open the editor, alter color/note and Cancel or Back: nothing is persisted.
- [X] Save a note with color set to none: the note exists but no fill appears in
  the reader. Plain selection without Save never becomes a colorless record.
- [X] Add a note to an existing colored highlight. Change its color to none and
  Save: the note survives and the fill disappears.
-- in the global notes tab, if a highlight has a note, add a faint separation line between the note and the highlight, if it doesnt have a note, leave as is
- [X] Remove both the color and note from an existing entry and Save: no empty
  annotation remains. A new empty/colorless draft creates no empty entry.
- [X] Repeatedly press Save during a slow operation: one annotation is created,
  and the editor closes only after saving succeeds.
-- in the table of content and book marks tab, add another tab for highlights/notes

## Range persistence and editing

- [X] Close/reopen the book and restart Deft: each highlight returns to the same
  words. Tap a colored passage to edit it, change color/note and save.
- [X] Highlight one of two identical phrases in a chapter. Only the chosen occurrence
  is decorated after reopening; the other occurrence remains untouched.
- [X] Highlight across styled text and paragraph boundaries. Change font, font size,
  line spacing, margins, theme and orientation: the intended words remain marked.
- [X] Select a passage beside a line break or inline image, including a repeated
  passage. Save/reopen in both modes: the same words remain marked.
- [X] Switch between paged and continuous mode. Stored highlights and notes remain
  anchored to the same text, including duplicate phrases and cross-node selections.
-- a slight design issue in the continuous mode, refer to the screenshots
- [X] In continuous mode, scroll several chapters away and back so chapters reload.
  Saved decorations return; edits/deletions do not leave stale fills behind.
- [X] Rotate with an unsaved editor open. Draft text/color survive, Cancel remains
  safe, and Save creates only one entry. Repeat while a save is completing.
- [X] Delete an entry: cancel confirmation first, then confirm. Both Notes and the
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
- [X] Tap a passage/bookmark: the correct book opens at its saved location, even
  when a different book was last opened. Reader Back returns normally.
- [X] Open a colorless note from Notes in both modes: it stays unfilled. Search
  for the same passage separately: the temporary search indication still works.
- [X] Edit/delete an entry from Notes and verify the reader reflects it. Rename a
  bookmark and check both Notes and the existing reader bookmark list.

## Per-book Notes and Markdown export

- [X] Book detail switches between details and notes. Its count and list are scoped
  to that book and update after saves/deletions; other books' entries stay separate.
-- swiping sideways should switch beetween the two tabs
- [X] Export the book's notes through Android's share sheet. Save/open the `.md`
  file: title/author, passages, notes, bookmarks and locations are readable and scoped.
- [X] Include multiline notes and Markdown punctuation. The export preserves content
  without broken structure or private SAF/file paths. Cancel sharing and retry safely.
-- indicate notes as notes
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

- [X] In Paper and Ink, portrait/landscape and 150% text: entries, filters, color
  choices and pinned save/cancel actions stay legible and reachable, including keyboard open.
- [X] TalkBack names every color (including none), actions and selected controls;
  actions have usable touch targets and important meaning does not rely on color alone.
- [X] Opening an editor or external share/dictionary app pauses reading time; returning
  resumes correctly. Editing notes must not artificially add page turns or reading time.
- [X] Airplane mode: notes, highlights, filters, local search and Markdown generation
  work. No remote services, accounts or permissions are required.

## Acceptance

- [\] M5 accepted; ready to merge the tested version into main.

Feedback:

- plenty of screenshots, go through them all and fix the issues they show
