# M4 — categories, series and authors: device checklist

Current review build: **0.4.1-1**, version code **40101**, signed release `deft-0.4.1.apk`.

## Original 0.4.0 review (preserved)
Install over your existing Deft app; do not uninstall. The original device results and comments remain below. New 0.4.1 retests are at the end. Add comments and screenshots beneath each
section; put media in `plans/feedback/`.

## Upgrade and existing behavior

- [X] Update from 0.3.3 or your current M3 release without uninstalling. Library,
  folders, reader settings, bookmarks, ratings and reading history survive.
- [X] Open an EPUB, move through it, select text, leave and reopen. Position and
  session tracking still work. Test paged and continuous reading.
- [/] Idle overlay is visibly black; waking consumes the gesture without moving
  the page. Text selection gives feedback when system haptics are enabled.
-- feedback has been left in the m3 checklist
- [X] Bottom navigation has Books, Categories and Authors. Switching tabs and
  returning preserves useful state; detail/settings screens have working Back.
- [X] A scan notice still opens its detailed report and dismisses normally.
-- the scan complete notification still needs work, rework it from scratch and keep in mind the design issues i have been mentioning again and again throughout the development of this app

## Categories and types

- [X] Create a category with a name and description, without a type. Create a
  second category with the same name; both remain separate and identifiable.
- [X] Create custom types (for example TBR and Book club), assign several types
  to one category, rename a type and delete it. Categories and books remain.
- [X] Built-in Series cannot be renamed or deleted.
- [X] Blank names are rejected clearly. Canceling an editor leaves the original
  category unchanged; editing name/description/types persists after restart.
- [X] Category list/grid, selected type filter and grouping survive restart.
  Test All, a custom type and Untyped; multi-type categories appear appropriately.
-- more on this lower
- [X] Empty categories have a useful empty state, no invented average rating and
  an action to add books. Long names/descriptions wrap without hiding actions.
- [X] Default cover is a collage of member covers. Add a custom local image,
  restart, then remove it to restore the collage. Canceling image selection and
  selecting an unreadable image leave a usable screen and clear feedback.

## Membership and ordering

- [X] Long-press a book in both grid and list to select it. Tap more books, select
  all visible books, clear selection, and use Back. Selected books never launch.
-- design issues described on the bottoms
- [X] Search/filter first, then select all: only visible matching books are added.
  Rotation retains selection; forgotten or newly hidden books are not selected.
- [X] Add selected books to an existing category and to a newly created one.
  Duplicate additions do not create duplicate members. Success exits selection.
- [X] Add/remove members from category details and book details. Other categories,
  book files, ratings and history are unaffected.
- [\] Non-Series manual order supports drag movement and accessible move up/down.
  Scroll a longer list, move a book, leave and reopen: the order persists.
-- sorting for non-series categories shouldnt be under the three dots menu, it should be visible
-- also the manual reordering is hidden quite deep, make it more accessible (this should happen naturally when you make the sorting visible). in the manual reordering menu, give a bit more vertical padding for list elements
- [X] Title, author, date-added and rating sorts work. Date-added means when the
  book joined the category. Switch back to manual order and verify behavior.
- [X] Books category filter survives restart and combines with search/state/rating
  filters. Delete the selected category: Books returns to all categories.

## Series auto-fill and review

- [X] With auto-fill enabled, scan EPUBs containing `calibre:series` and indices
  1, 2 and 2.5. One ordered Series category appears with suggestions to review.
-- keep the numerical editing for orders but allow an easier way/mode of reordering items by dragging and dropping items
- [X] Metadata differing only in series-name case/extra whitespace joins the same
  automatic series. A manually created same-name series stays separate.
- [X] Confirm individual/all suggestions; the review indicator disappears when
  none remain. Edit a suggested position; rescanning preserves the edited value.
-- design needs work, more details on what kind expressed below
- [X] Decimal positions (including 0 or a prequel value) persist. Invalid text or
  nonfinite values are rejected without corrupting ordering.
- [X] Remove an automatically suggested member and rescan: it stays removed.
  Add it manually and confirm it stays present on later scans.
- [X] Disable Library → series auto-fill and scan new series books: existing
  categories stay, new suggestions stop. Re-enable: unchanged files are included.
- [X] Rename an automatic series, then scan another book using its old metadata
  name. The new suggestion reaches the renamed series.
- [X] Merge an automatic series into a manual target. Scan another matching book:
  it reaches the target through the alias, without recreating the source.
- [X] On a series book page, Next in series picks the lowest strictly later
  position that is not Finished. Finished entries are skipped, Abandoned is not.
  Check a book in multiple series and the final member with no later candidate.

## Progress, rating and merges

- [X] Finished/total progress and state breakdown update when member states change.
  Abandoned remains unresolved; Paused/Reading/Not read are not counted finished.
- [X] Missing files remain in category/author totals and remain manageable. Ignored
  books disappear from visible memberships/totals and return on un-ignore.
- [X] Rate several member books, leaving some unrated. Category mean uses rated
  books only and displays one decimal. Set a half-star override, then reset it:
  the computed mean returns. Changing a book rating updates computed ratings.
- [X] Merge two categories with overlapping members. The union has no duplicates,
  target membership positions win, types combine and source disappears.
-- when merging, allow sorting of categories
- [X] Cancel a merge/delete confirmation: nothing changes. Delete a category:
  books and history remain. The confirmation explains automatic recreation.

## Authors

- [X] Authors appear from effective book metadata; search finds expected authors.
  Open an author from both Authors and a book page.
- [X] Author detail shows books grouped by state, count, finished progress, rating
  and total active time. Coauthors each receive full credit for a shared book.
- [X] Rename an author; existing book labels update. Rescan/add a book with the old
  spelling: the retained alias resolves to the renamed author.
- [X] Merge two authors with both overlapping and distinct books. Books combine
  without duplicates; metadata overrides and histories survive; aliases redirect.
- [X] Cancel rename/merge, try a blank name and try a conflicting existing name.
  Errors are clear and appear as notices, with original data intact.
- [X] Ignore every book for one author: it leaves the visible list. Un-ignore one:
  the author returns with the same identity and retained metadata.

## How does it look and feel?

- [/] Categories list/grid and grouped/filter controls feel clear and compact in
  light and dark themes, portrait/landscape and 150% system text.
-- the design needs work for the top part of category tab (the part with new category, types, grid and grid by type, and underneath it (these two should be dsitinct), the filters) 
- [X] Category detail has clear hierarchy for cover, identity, progress, rating
  and member actions; editors/confirmation sheets fit above the keyboard.
- [X] Author list/detail and rename/merge controls feel consistent with Books.
- [X] Book detail author/category links and Next in series are easy to discover.
- [X] Multi-select count, selected outlines/checkmarks and Add to category make
  the action clear. Scrolling, sheet transitions and reorder motion are smooth.
- [X] TalkBack names controls, announces selection/state and offers reorder
  alternatives. Touch targets remain usable at large font sizes.

## Comments, screenshots and final acceptance

Add findings here (screen, steps, expected behavior, actual behavior).

- [\] M4 checklist passed; ready to merge this tested version into main.
- why is the dropdown menu from the three dots on the top right different across screens? in books, its last scan and settings with icons and a faint outline, in both categories and authors its just settings without its icon and without the different design
- allow sorting and filtering in both categories (add date created and whether in review or not here) and authors
- show cover images in categories listing (in smaller forms ofc to fit the smaller listing) and allow grid view of categories elements as well (just like the books tab)
- the design for the bar that appears when selecting books in books tab needs work (more padding, alignment issues), and when the number of books is very big, it overflows into the next line so make sure not to use too much empty space (for example the gap/padding for the cross icon is huge)
- the design when adding a new category needs work, alignment, positioning of buttons and empty space, no clear distinction between buttons, filters, toggles, hierarchy
- again with the design for the rating stars in the categories. fix the design, especially with the "use average" button. and MAKE PROPER USE OF EMPTY SPACE in the category description card with the add books and edit and drop down options
- the design when selecting books to add to a category directly from the category page needs work, not enough info being shown (show cover and other relevant info) and allow sorting and filtering here and when selecting books, add a floating tick button to confirm addition so users dont have to scroll all the way back up to confirm it
- allow sorting by file location for books wherever relevant
- allow sorting by author for books wherever relevant
- swiping sideways should switch between books - categories - authors
- dont list categories in the books filtering, its too much to contain there, if users want, they can list category bounded books in the categories tab

## 0.4.1 — review fixes to retest

Install `deft-0.4.1.apk` over the existing app. These new checks are unchecked;
local automated validation does not substitute for device acceptance.

- [ ] Idle pause uses a darker film and a clearly visible full-width pulsing black
  label strip with white text. Check light/sepia/dark/black; animations off stays
  steady. First wake gesture is consumed without a page turn or idle time added.
- [ ] Scan completion has a clear heading, concise counts and a separate details
  action. Icon/close align; long errors wrap. Notices sit above bottom tabs,
  slide/stack/dismiss, expire when untouched, and open the detailed scan report.
- [ ] Books, Categories and Authors overflow menus have matching outlined styling
  and icons for last scan/settings. Last scan is unavailable before a scan.
- [ ] Swipe left/right between Books, Categories and Authors. Ends do not wrap,
  vertical scrolling works, and horizontal rows/sliders keep their own gestures.
  Tab buttons, Back, details, sheets and restored scroll/selection still work.
- [ ] Books filters no longer contain the long category list. A category selected
  in 0.4.0 does not invisibly restrict Books after updating; use Categories instead.
- [ ] Books selection bar fits a large selection count at normal and 150% text.
  Clear, select all and add remain aligned and usable; selection survives rotation.
- [ ] File-location and author sorting work in Books, non-Series category members,
  the add-books picker and author details. Reverse direction works where offered.
  Multiple book files use a stable location, preferring a present file.
- [ ] Category top controls clearly distinguish create/manage actions from search,
  filters, sorting and display. Change list/grid and type grouping independently.
- [ ] Sort categories by name, creation date, book count and rating; filter review
  status, type and reading state. Combine with search, reset and restart the app.
- [ ] Sort/filter Authors by name/count/time/rating and reading state/rated status.
  Search, reset and restart retain appropriate preferences and stable ordering.
- [ ] Category list uses visible cover thumbnails; grid shows proper cards. Custom
  cover and collage fallback remain correct, including grouped categories.
- [ ] Category member list shows book covers and useful metadata; switch to member
  grid. Long titles and missing title/author labels fit, and books still open.
- [ ] Non-Series sorting is visible beside the book section. Manual ordering makes
  reorder easy to find; reorder rows have comfortable padding and accessible arrows.
- [ ] Series reorder supports drag/arrows plus existing decimal editing. Saving a
  reorder retains unique numeric slots (including fractions); duplicate positions
  are renumbered as explained in the sheet. Rescan preserves the edited order.
- [ ] Category rating and use-average/reset controls are compact and clearly grouped.
  Add books/edit/more actions use the available width; description remains readable.
- [ ] Suggested members have a distinct review section. Confirm one/all and edit
  positions; the review indicator clears appropriately and survives restart.
- [ ] New/edit category sheet separates details, type selection and cover controls.
  Save/cancel remain reachable above the keyboard; cancel leaves data untouched.
- [ ] Add-books picker shows covers, title, author, state/format/location. Search,
  sort and filter a large list; changing filters retains deliberate selection.
- [ ] Scroll far down the picker: floating tick and selected count remain reachable.
  Confirm adds exactly the selected books once; last row is not covered by the button.
- [ ] Category merge target selection supports search and sorting. Duplicate names
  remain identifiable; confirmation/cancel and union behavior remain correct.
- [ ] Inspect changed screens in both themes, landscape and 150% text; TalkBack
  announces sorting, filtering, selection, reorder and confirmation actions.
- [ ] M4 0.4.1 accepted; ready to merge this tested version into main.
