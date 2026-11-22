# M4 implementation and validation

> **Historical record:** M1-M4 are complete. This document preserves the implementation/review state at its original handover; pending status and task checkboxes below are not current work. See the [documentation index](README.md) and [roadmap](2026-11-13-deft-roadmap.md) for current status and milestone numbering.

M4 lives on `feat/m4-organization`; it awaits the author's device checklist
before merge. The preceding reader fixes were verified and pushed directly to
main as `v0.3.3-1`, as requested. M4 uses `0.4.0-1` (code 40001).

## Implemented scope

- Room 7 categories, types, memberships, series aliases and explicit exclusions;
  migration coverage preserves bookmarks, locators, assessments and sessions.
- Transactional category/type/member edits, decimal series positions, manual
  ordering, deterministic sorts, category merges and rating overrides/reset.
- Default-on series auto-fill during scans, including unchanged files; manual
  categories stay separate until explicitly merged. Suggestions require review,
  aliases survive merges/renames, and explicit removals survive rescans.
- Effective author pages, rename/merge aliases and complete coauthor credit;
  aggregate counts/time/ratings exclude ignored books and retain missing books.
- Categories/Authors tabs, category covers and collages, grouping/filter/display
  preferences, bulk Books assignment, book-detail links and Next in series.
- Bounded local cover import, no source-file writes and no network behavior.

## Regression and review record

The organization contract tests failed against empty scaffolding before the
implementation. The first functional batch passed all 21 database and 21
organization tests, plus scanner tests exercising real calibre-series EPUBs.
The initial migration test had a Kotlin `use` harness error; its replacement
builds a schema-6 database and verifies the actual 6→7 migration. DataStore
preferences and three-tab navigation tests also passed.

The first integrated UI build compiled the application and exposed style issues,
test dependencies added after Gradle configuration, and an existing toggle-count
assertion that needed to include the new series switch. These were corrected
together. Further regressions cover atomic create-and-add rollback, a failed
series-organization scan retaining imported books, restored selection and an
open category draft, and category filters falling back after deletion.

The combined source audit identified five issues: selection cleared by initial
loading, an unsaved bulk-picker callback, reordered coauthor credits, cover-save
cancellation, and quadratic bulk membership matching. Fixes and regression
coverage are included in the final verification run. Physical-device drag,
haptics and visual acceptance remain in `m4-test-checklist.md` because USB access
is unavailable.

## Final verification

`ktlintFormat check assembleRelease` passed with one worker in 4m 22s
(`.superpowers/m4-verified.log`, 2,058 tasks). The final reports contain **995
test executions**: 478 debug, 478 release and 39 JVM; zero failures, errors or
skips. This includes Room migration, transactional organization, scanner,
preference restoration, bulk assignment, editor/merge/error controls and the
existing reader/tracking regressions. Network isolation, lint, detekt and ktlint
are included in `check`.

The integrated UI test failures were harness issues: the release Compose test
activity was missing, callback-only waits did not advance paused Main work,
and a startup scan notice intercepted the Authors tap on the small smoke-test
display. Both variants now pass with the activity, the established test Main
dispatcher and explicit notice dismissal. No production behavior was weakened
to pass these tests.

Inspected four native Robolectric captures: category cards, category detail with
missing metadata, ink author detail and ink authors in landscape at 150% text.
The render harness now supplies the same background/content colors as the
production Scaffold. See [the visual reference](design/m4-organization.html).

`scripts/verify-release.py` passed: persistent release certificate, application
ID `com.kmab.deft`, version `0.4.0` / code `40001`, no INTERNET permission and
R8 mapping present. Minification and resource shrinking remain enabled. The
handover APK is `deft-0.4.0.apk`; its exact commit, SHA-256, size and mapping are
archived under `.superpowers/releases/0.4.0/` after packaging the committed source.

All five source-audit findings are closed. Device acceptance remains unchecked
in [m4-test-checklist.md](m4-test-checklist.md), including release upgrade,
physical drag, image-picker rotation, haptics and TalkBack. No USB phone was
available. M4 remains on its feature branch; GitHub CI completion is not a
handover gate.
