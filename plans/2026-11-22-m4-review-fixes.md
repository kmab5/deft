# M4 0.4.1 — device review fixes

> Execute with subagent-driven-development and test-driven-development. The author's approved device feedback is the design brief; proceed without another design approval. Root alone runs Gradle with one worker. One combined final audit/test pass follows implementation, per author preference.

**Goal:** Address every comment in the reviewed M4 checklist and include the main-branch idle contrast adjustment.
**Architecture:** Extend existing organization rules and query projections, keep browsing preferences in DataStore, reuse shared chrome, preserve the current Room schema and offline boundaries.
**Tech stack:** Existing Kotlin, Compose, Room, Hilt; installed SDK/JDK/Gradle only.

## Global constraints
- Version 0.4.1-1, code 40101; signed R8/resource-shrunk release deft-0.4.1.apk, persistent key.
- Files read-only; history permanent; ignored excluded, missing retained; no network or new external dependencies.
- Keep all original author checklist comments. Leave unrelated M1 checklist edits and error.log untouched.
- M3 fix already committed/pushed on main without a build by explicit request. M4 remains on its branch for review.
- Existing design tokens, subtle bordered cards, 48dp targets, reduced-motion behavior, no new design approval gate.
- Root controls Gradle, commits, docs and final packaging. Implementer does not run Gradle or commit.

## Task 1 — organization experience (implementer)
Read plans/m4-test-checklist.md and all seven current plans/feedback JPGs. Own feature/organization, and organization browsing preferences in core/datastore (new separate repository/files preferred; existing LibrarySettings may be extended if necessary). Do not edit root-owned files below.
- [x] Separate top primary actions (new category/types) from filter/sort/display controls. Persist category and author sorting/direction/filters. Categories: name/date created/book count/rating, review status (all/needs review/reviewed), type and reading-state filters. Authors: name/book count/time/rating plus reading-state and rated/unrated filters. Support query search and reset, stable tie order. Date-created is category sorting; no artificial author creation date.
- [x] Show member cover thumbnails in category list and a real adaptive category grid (not horizontal strips), retaining custom covers/collages and existing grouping/type preferences.
- [x] Redesign category identity/detail action hierarchy, compact rating controls and use-average placement, concise separate suggestion-review panel. Keep existing rating override/reset semantics.
- [x] Make non-Series sort visible beside books heading; manual reorder visible when active. Series show reorder beside member list while retaining decimal editing. Add vertical padding to reorder rows and use root's reorderSeries API for Series.
- [x] Redesign category editor with grouped name/description/types/cover controls and distinct anchored save/cancel actions, keyboard-safe. Preserve drafts/cancellation safety.
- [x] Redesign category add-books picker: cover/title/authors/state/format/location, search, author/title/date/rating/file-location sorts, state/format/missing filters. Keep selection across filter changes; show a fixed floating tick confirmation with selected count so scrolling never hides it. Ensure long lists lazy and bottom padding prevents FAB overlap.
- [x] Category merge target chooser supports search/sorting (name/date/book count/rating) and clear target identity, confirmation retained.
- [x] Author detail book lists also offer useful title/author/file-location/date/rating sorts while retaining state grouping.
- [x] Use shared DeftScreenMenu on Categories/Authors with identical last-scan/settings icons, border and styling as Books. organizationGraph gains onScanDetails:()->Unit={}, hasScanDetails:Boolean=false; root wires it.
- [x] Add targeted preference/query/Compose behavior tests before behavior implementation; tell root when red tests are ready. Add native render coverage for header/grid/editor/picker/detail at phone and large text dimensions. Write concise report to .superpowers/review-041/ui-report.md.

Interfaces supplied by root:
- BookListItem.fileLocation: String = "" (canonical displayPath, present copy preferred, then case-insensitive path/id; no file means empty).
- BookSort.FILE_LOCATION and CategoryOrder.FILE_LOCATION, existing AUTHOR retained.
- OrganizationRepository.reorderSeries(categoryId:Long, visibleBookIds:List<Long>):Unit. Exact permutation only; preserve unique sorted numeric slots, duplicate slots renumber 1..N. Explain this in reorder sheet; still permit decimal edits.
- core.designsystem.DeftScreenMenu(onSettings:()->Unit, onScanDetails:(()->Unit)?=null) common top menu; nullable callback disables last-scan action.

## Task 2 — data, Books and shared chrome (root)
- [x] Add FILE_LOCATION sorts and stable canonical location projection in all BookListItem queries, organization snapshots and non-Series order; tests verify multiple present/missing paths and deterministic ties. No schema change.
- [x] Add transactional Series reorder with stale/duplicate/permutation validation, preserve numeric slots when unique, retain ignored members and scanner aliases/exclusions; regression tests.
- [x] Remove category list from Books filters and stop applying saved categoryFilter values. Retain storage compatibility, prevent invisible stale filtering; update obsolete regression.
- [x] Compact Books selection bar: short count, close without excess inset, select-all icon, clear primary assignment action; fit large counts and 150% text. Preserve restoration.
- [x] Common styled top overflow; scan notices redesigned with a clear status/message/action hierarchy, aligned icon/close, compact details action, and placement above bottom navigation. Preserve timeout, stack/accessibility pause and detailed report.
- [x] Horizontal gestures switch only Books/Categories/Authors with matching transitions, no wrapping; preserve tab/detail state and do not steal child horizontal scrolling/sliders or selection gestures. Test forward/back/boundaries.

## Task 3 — complete verification and handover (root)
- [x] Run grouped meaningful regression tests then full ktlintFormat check assembleRelease, one worker. Inspect native renders. One combined independent source audit; fix findings together.
- [x] Append 0.4.1 retest checklist mapping every review comment; update spec decisions/visual design/validation. Commit focused verified changes, push branch, package exact commit signed release and archive mapping/evidence. No M4 merge and no CI wait.
