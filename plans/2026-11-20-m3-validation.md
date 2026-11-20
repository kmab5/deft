# M3 implementation validation

## Scope and authorization

The author requested full M3 after accepting M2. Work is on `feat/m3-reading-tracking`, based on main `0d0e1e6`. Plan `c038628` was committed before implementation. M3 is handed over with its own device checklist before merge.

## Implementation and coverage

History uses read-throughs and cumulative, stable-ID sessions. The latest read-through determines state; ratings, favorites and reviews remain independent of file availability. Library queries load lightweight state/assessment projections. Room migration 4→5 preserves existing books, folders, positions and bookmarks. Source-timestamped reader events are serialized; duration uses a monotonic clock and sessions retain UTC timestamps and the local zone offset.

Tests cover the five states and opening choices, minimum session length, idle/background behavior, checkpoint recovery, browsing/ignored exclusions, manual dates/deletion, assessment persistence, library filters and sorting, settings, and full/quiet finish flows. The first red batches confirmed missing UI/domain behavior before implementation (`.superpowers/m3-ui-red.log`, `.superpowers/m3-domain-red.log`). Foundation checks passed model, migration and settings tests. The initial integration pass passed 18 tracker-engine and 11 repository tests.

## Combined audit

One whole-branch audit identified two integration defects:

- A pending session whose book/read-through had been deleted could block later writes. Typed removed-target failures now retire that write while retaining retries for storage failures.
- A rejected finish-prompt claim for a Paused read-through could be cached before a qualifying session resumed it. Qualification now gates the first claim.

Real Room coordinator regressions cover deleted read-throughs, forgotten books and paused last-page resumption. Scoped follow-up marked both findings addressed. A subsequent complexity-only extraction separates successful-save bookkeeping without changing FIFO/retry behavior.

## Verification record

- Chromium EPUB regression: passed; 39 resources and 33 font faces checked. Only the absent favicon was blocked.
- Continuous-reader browser regression: passed. The Windows test harness now waits for Chromium exit and retries temporary-profile cleanup; its existing resolved-path guard is retained.
- Python release-tool tests: 4 passed.
- Local Compose renders cover book cards/history, large-text Tracking settings, and full/quiet finish flows. The reference is `plans/design/m3-feedback.html`.
- Final `check assembleRelease`: passed in 4m 58s (`.superpowers/m3-resume.log`). 836 passing JUnit test executions across JVM/debug/release variants (183 suites), zero failures, errors or skips. This includes ktlint, detekt, Android lint and network isolation checks.
- Release verifier: application ID `com.kmab.deft`, version `0.3.0`, version code `30001`, no INTERNET permission, R8 mapping present, and certificate identical to M2.
- Final visual review covered populated history/assessment in portrait, landscape and large text, Tracking/Appearance settings and settings navigation. Singular session/page-turn/minute labels were corrected and the affected checks and renders rerun.

The initial aggregate pass exposed a rating preference-key/bound name collision, formatting/static-check issues and a library test tapping a filter below the viewport. These were corrected as one integration fix batch. No physical-device test is claimed: the author instructed us to assume USB is unavailable.

## Conservative idle recovery

With a finite timeout, an idle gap is removed back to the last actual reading interaction. Thirty-second checkpoints persist confirmed reading only. Clean exits before timeout include the current dwell; crash recovery discards uncertain dwell. A crash may therefore lose the unconfirmed current-page time as well as up to 30 seconds of confirmed time. This tradeoff is recorded in the spec decision log and device checklist.

Overlays, loading, screen-off and rotation end the visible session. Returning starts another segment, and each segment needs 60 seconds to be retained. This avoids counting hidden publication time and is explicitly covered by the device checklist.

## Signed release artifact

- APK: `D:/Code/deft/deft-0.3.0.apk` (6,180,869 bytes).
- Version: `0.3.0-1`; version code `30001`.
- Embedded source revision: `cefab5e752e676b78afb1a387c8cb31c6cc78b72` (verified against the APK metadata).
- APK SHA-256: `a6a8130acf67db44322feee520b24c308512d5e92d1a8a6ded823833a90095ee`.
- Signing certificate SHA-256: `adea6ec2edf4e9e2d809ed06300729e35ca156c06072736717b4a27449233e50`.
- R8 minification and resource shrinking are enabled. The APK, matching mapping and identity evidence are archived locally under `.superpowers/releases/0.3.0/`.

Implementation commits are `a8f10d8` (persistence/tracker), `5f2c2bf` (reader integration) and `cefab5e` (UI/finish flow). The subsequent handover commit changes documentation and rendered references only. The feature branch is pushed without waiting for GitHub CI. M3 remains pending the author's device checklist acceptance before merge/tag.
