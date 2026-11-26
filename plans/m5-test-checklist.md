# M5 device test checklist — 0.5.0

Install the signed release `deft-0.5.0.apk` over your current Deft installation.
Do not uninstall: this also tests the Room 7 → 8 upgrade and retained history.
Use disposable books/annotations for deletion checks. Record failures and screenshots
under the relevant section; leave a checkbox unchecked when it has not been tested.
M5 stays on its branch until you accept it. Page notes for fixed-layout formats
remain M8; EPUB is the reader supported by this milestone.

## Update and existing data

- [ ] Update installs successfully without losing books, categories/types, bookmarks,
  reading positions, sessions, ratings, favorites or settings.
- [ ] Books/Categories/Authors still work, including 0.4.5 bulk category actions and
  creating a category from selected Books. Opening/reading an EPUB still tracks time.
- [ ] Notes is a working fourth tab. Normal taps and horizontal swipes reach it;
  Back, settings and last-scan menus behave like the other working tabs.

## Text selection and actions

- [ ] Long press text in paged mode, then move both selection handles. The selected
  text and existing haptic feedback are correct. Selection alone creates no entry.
- [ ] Repeat in continuous mode, including text across inline emphasis, links and
  paragraphs. Selecting/dragging does not unexpectedly turn pages or scroll away.
- [ ] Copy uses the final selected passage, including handle adjustments. Cancel
  the toolbar and reselect elsewhere: it does not use an older selection.
- [ ] Share text opens Android's share sheet with the correct passage. Cancel and
  return: reading and selection controls still work, with no unwanted annotation.
- [ ] Define hands text to an installed dictionary/text-processing app. Without
  a compatible app, Deft gives a useful notice and continues working.
- [ ] Search in book uses the selected text and existing offline search. Results
  jump to the right passage; go-back returns to the previous reading location.
  Turn a page or follow Contents within that chapter: the old search fill clears.

## Highlight and note creation

- [ ] Save a purple highlight, then green, amber and gray passages. Each uses a
  soft visible fill and produces one Notes entry. Do this in both reading modes.
- [ ] Open the editor, alter color/note and Cancel or Back: nothing is persisted.
- [ ] Save a note with color set to none: the note exists but no fill appears in
  the reader. Plain selection without Save never becomes a colorless record.
- [ ] Add a note to an existing colored highlight. Change its color to none and
  Save: the note survives and the fill disappears.
- [ ] Remove both the color and note from an existing entry and Save: no empty
  annotation remains. A new empty/colorless draft creates no empty entry.
- [ ] Repeatedly press Save during a slow operation: one annotation is created,
  and the editor closes only after saving succeeds.

## Range persistence and editing

- [ ] Close/reopen the book and restart Deft: each highlight returns to the same
  words. Tap a colored passage to edit it, change color/note and save.
- [ ] Highlight one of two identical phrases in a chapter. Only the chosen occurrence
  is decorated after reopening; the other occurrence remains untouched.
- [ ] Highlight across styled text and paragraph boundaries. Change font, font size,
  line spacing, margins, theme and orientation: the intended words remain marked.
- [ ] Select a passage beside a line break or inline image, including a repeated
  passage. Save/reopen in both modes: the same words remain marked.
- [ ] Switch between paged and continuous mode. Stored highlights and notes remain
  anchored to the same text, including duplicate phrases and cross-node selections.
- [ ] In continuous mode, scroll several chapters away and back so chapters reload.
  Saved decorations return; edits/deletions do not leave stale fills behind.
- [ ] Rotate with an unsaved editor open. Draft text/color survive, Cancel remains
  safe, and Save creates only one entry. Repeat while a save is completing.
- [ ] Delete an entry: cancel confirmation first, then confirm. Both Notes and the
  reader update, with no change to the source EPUB or unrelated entries.

## Global Notes

- [ ] Notes shows saved highlights, colorless notes, attached notes and existing
  bookmarks, with passage/note text, book title and useful location information.
- [ ] Filter by book, color and type independently, then combine them. Notes type
  includes notes attached to colored highlights; Highlights includes those colored
  entries too. Bookmarks filter shows bookmarks only.
- [ ] Search passage text, note text, bookmark label, title and author. Search with
  `%`, `_`, apostrophes and non-Latin text: it behaves as literal text and never crashes.
- [ ] Clear/reset search and filters, test zero results and an empty notes library.
  Rotate or leave/return: controls remain coherent and no hidden filter is confusing.
- [ ] Tap a passage/bookmark: the correct book opens at its saved location, even
  when a different book was last opened. Reader Back returns normally.
- [ ] Open a colorless note from Notes in both modes: it stays unfilled. Search
  for the same passage separately: the temporary search indication still works.
- [ ] Edit/delete an entry from Notes and verify the reader reflects it. Rename a
  bookmark and check both Notes and the existing reader bookmark list.

## Per-book Notes and Markdown export

- [ ] Book detail switches between details and notes. Its count and list are scoped
  to that book and update after saves/deletions; other books' entries stay separate.
- [ ] Export the book's notes through Android's share sheet. Save/open the `.md`
  file: title/author, passages, notes, bookmarks and locations are readable and scoped.
- [ ] Include multiline notes and Markdown punctuation. The export preserves content
  without broken structure or private SAF/file paths. Cancel sharing and retry safely.
- [ ] Export a book with no notes: the result/empty-state handling is clear.

## Missing, ignored and forgotten books

- [ ] Make a test EPUB unavailable and rescan. Notes/bookmarks remain visible,
  editable and exportable with a missing-file indication. Opening gives a useful notice.
- [ ] Restore the same file and rescan: the existing annotations reattach and jump
  correctly, without duplication or transferred annotations to a different file.
- [ ] Ignore a test book: its annotations disappear from global Notes and export.
  Un-ignore: the same annotations return. Ordinary missing files must stay visible.
- [ ] Forget a disposable book using the existing confirmation: its annotations
  disappear permanently along with its other Deft history; the source file remains.

## Layout, accessibility and tracking

- [ ] In Paper and Ink, portrait/landscape and 150% text: entries, filters, color
  choices and pinned save/cancel actions stay legible and reachable, including keyboard open.
- [ ] TalkBack names every color (including none), actions and selected controls;
  actions have usable touch targets and important meaning does not rely on color alone.
- [ ] Opening an editor or external share/dictionary app pauses reading time; returning
  resumes correctly. Editing notes must not artificially add page turns or reading time.
- [ ] Airplane mode: notes, highlights, filters, local search and Markdown generation
  work. No remote services, accounts or permissions are required.

## Acceptance

- [ ] M5 accepted; ready to merge the tested version into main.

Feedback:

