# M4 — categories, series and authors: device checklist

Build: **0.4.0-1**, version code **40001**, signed release `deft-0.4.0.apk`.
Install over your existing Deft app; do not uninstall. All steps below are for
device review and start unchecked. Add comments and screenshots beneath each
section; put media in `plans/feedback/`.

## Upgrade and existing behavior

- [ ] Update from 0.3.3 or your current M3 release without uninstalling. Library,
  folders, reader settings, bookmarks, ratings and reading history survive.
- [ ] Open an EPUB, move through it, select text, leave and reopen. Position and
  session tracking still work. Test paged and continuous reading.
- [ ] Idle overlay is visibly black; waking consumes the gesture without moving
  the page. Text selection gives feedback when system haptics are enabled.
- [ ] Bottom navigation has Books, Categories and Authors. Switching tabs and
  returning preserves useful state; detail/settings screens have working Back.
- [ ] A scan notice still opens its detailed report and dismisses normally.

## Categories and types

- [ ] Create a category with a name and description, without a type. Create a
  second category with the same name; both remain separate and identifiable.
- [ ] Create custom types (for example TBR and Book club), assign several types
  to one category, rename a type and delete it. Categories and books remain.
- [ ] Built-in Series cannot be renamed or deleted.
- [ ] Blank names are rejected clearly. Canceling an editor leaves the original
  category unchanged; editing name/description/types persists after restart.
- [ ] Category list/grid, selected type filter and grouping survive restart.
  Test All, a custom type and Untyped; multi-type categories appear appropriately.
- [ ] Empty categories have a useful empty state, no invented average rating and
  an action to add books. Long names/descriptions wrap without hiding actions.
- [ ] Default cover is a collage of member covers. Add a custom local image,
  restart, then remove it to restore the collage. Canceling image selection and
  selecting an unreadable image leave a usable screen and clear feedback.

## Membership and ordering

- [ ] Long-press a book in both grid and list to select it. Tap more books, select
  all visible books, clear selection, and use Back. Selected books never launch.
- [ ] Search/filter first, then select all: only visible matching books are added.
  Rotation retains selection; forgotten or newly hidden books are not selected.
- [ ] Add selected books to an existing category and to a newly created one.
  Duplicate additions do not create duplicate members. Success exits selection.
- [ ] Add/remove members from category details and book details. Other categories,
  book files, ratings and history are unaffected.
- [ ] Non-Series manual order supports drag movement and accessible move up/down.
  Scroll a longer list, move a book, leave and reopen: the order persists.
- [ ] Title, author, date-added and rating sorts work. Date-added means when the
  book joined the category. Switch back to manual order and verify behavior.
- [ ] Books category filter survives restart and combines with search/state/rating
  filters. Delete the selected category: Books returns to all categories.

## Series auto-fill and review

- [ ] With auto-fill enabled, scan EPUBs containing `calibre:series` and indices
  1, 2 and 2.5. One ordered Series category appears with suggestions to review.
- [ ] Metadata differing only in series-name case/extra whitespace joins the same
  automatic series. A manually created same-name series stays separate.
- [ ] Confirm individual/all suggestions; the review indicator disappears when
  none remain. Edit a suggested position; rescanning preserves the edited value.
- [ ] Decimal positions (including 0 or a prequel value) persist. Invalid text or
  nonfinite values are rejected without corrupting ordering.
- [ ] Remove an automatically suggested member and rescan: it stays removed.
  Add it manually and confirm it stays present on later scans.
- [ ] Disable Library → series auto-fill and scan new series books: existing
  categories stay, new suggestions stop. Re-enable: unchanged files are included.
- [ ] Rename an automatic series, then scan another book using its old metadata
  name. The new suggestion reaches the renamed series.
- [ ] Merge an automatic series into a manual target. Scan another matching book:
  it reaches the target through the alias, without recreating the source.
- [ ] On a series book page, Next in series picks the lowest strictly later
  position that is not Finished. Finished entries are skipped, Abandoned is not.
  Check a book in multiple series and the final member with no later candidate.

## Progress, rating and merges

- [ ] Finished/total progress and state breakdown update when member states change.
  Abandoned remains unresolved; Paused/Reading/Not read are not counted finished.
- [ ] Missing files remain in category/author totals and remain manageable. Ignored
  books disappear from visible memberships/totals and return on un-ignore.
- [ ] Rate several member books, leaving some unrated. Category mean uses rated
  books only and displays one decimal. Set a half-star override, then reset it:
  the computed mean returns. Changing a book rating updates computed ratings.
- [ ] Merge two categories with overlapping members. The union has no duplicates,
  target membership positions win, types combine and source disappears.
- [ ] Cancel a merge/delete confirmation: nothing changes. Delete a category:
  books and history remain. The confirmation explains automatic recreation.

## Authors

- [ ] Authors appear from effective book metadata; search finds expected authors.
  Open an author from both Authors and a book page.
- [ ] Author detail shows books grouped by state, count, finished progress, rating
  and total active time. Coauthors each receive full credit for a shared book.
- [ ] Rename an author; existing book labels update. Rescan/add a book with the old
  spelling: the retained alias resolves to the renamed author.
- [ ] Merge two authors with both overlapping and distinct books. Books combine
  without duplicates; metadata overrides and histories survive; aliases redirect.
- [ ] Cancel rename/merge, try a blank name and try a conflicting existing name.
  Errors are clear and appear as notices, with original data intact.
- [ ] Ignore every book for one author: it leaves the visible list. Un-ignore one:
  the author returns with the same identity and retained metadata.

## How does it look and feel?

- [ ] Categories list/grid and grouped/filter controls feel clear and compact in
  light and dark themes, portrait/landscape and 150% system text.
- [ ] Category detail has clear hierarchy for cover, identity, progress, rating
  and member actions; editors/confirmation sheets fit above the keyboard.
- [ ] Author list/detail and rename/merge controls feel consistent with Books.
- [ ] Book detail author/category links and Next in series are easy to discover.
- [ ] Multi-select count, selected outlines/checkmarks and Add to category make
  the action clear. Scrolling, sheet transitions and reorder motion are smooth.
- [ ] TalkBack names controls, announces selection/state and offers reorder
  alternatives. Touch targets remain usable at large font sizes.

## Comments, screenshots and final acceptance

Add findings here (screen, steps, expected behavior, actual behavior).

- [ ] M4 checklist passed; ready to merge this tested version into main.
