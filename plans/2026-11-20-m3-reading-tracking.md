# M3 reading tracking implementation plan

> Execute with subagent-driven-development and test-driven-development. The author requests complete implementation, grouped checks and one combined audit. Do not wait for CI. Do not merge M3 until device acceptance.

**Goal:** Accurate automatic reading history, read-through state management, ratings/favorites/reviews and finished-book celebration, delivered as signed 0.3.0.

**Architecture:** A deterministic elapsed-time SessionTracker in `:tracking` records through transactional Room repositories. Reader lifecycle and classified reading events drive it. Persisted domain snapshots feed book/library screens and DataStore supplies global Tracking preferences. State is derived from the latest read-through; no duplicated mutable Book state.

**Stack:** Existing Kotlin/Compose/Room/Hilt/Coroutines/DataStore only. No new external dependencies.

## Constraints

- Read the spec §8, §9, §15 and visual §8–10; preserve M2 reader behavior.
- Files remain read-only; missing/ignored books retain history; only confirmed Forget removes it.
- No network, DRM circumvention or new tool downloads. Reuse D:/Code/android-sdk, existing Gradle and JDK17/JDK21. Missing dependencies only permitted.
- Sessions require resumed visible screen-on reader; discard active duration below60 seconds; checkpoint every30 seconds; monotonic elapsed time prevents wall-clock jumps inflating duration.
- Reflowable/fixed idle defaults5/10 minutes, integer1–60 or Off. Exclude idle gap retrospectively when timeout reached; reopen starts a new session. Off releases keep-awake after15 minutes of no reading interaction.
- Browsing sessions stay visible in book history, excluded from every aggregate. Ignored books are excluded from aggregate queries.
- State transition prompts exactly match §8.3. End prompt once per read-through; finish is never automatic. Reset to Not read removes read-throughs only with explicit history-loss confirmation.
- Every Room migration has an upgrade test. Source history survives migration. Ratings are null or half steps0.5–5; private plain-text review.
- UI uses existing grouped elevated cards, icons, 48dp targets, animated changes, accessible large text and quiet/reduced-motion celebration fallback.
- Root owns Gradle invocations and integration commits; implementers coordinate requests rather than launching concurrent builds. Untracked error.log is unrelated.

## Shared interfaces and ownership

Task1 defines domain types in core/model, persistence in core/database, repositories in tracking and TrackingSettingsRepository in core/datastore. Publish exact interface signatures to other implementers immediately after defining them. Required concepts: BookState, ReadThroughOutcome, ReadThrough, ReadingSession, TrackingPreferences, BookTrackingSnapshot; ReadThroughRepository for observe/get/open-choice/manual transitions/date edits/deletion/once-only finish claim; SessionRepository for idempotent checkpoints/finalize/recover; RatingRepository or equivalent book assessment methods. SessionTracker consumes repositories without depending on reader classes. `:tracking` may depend on core/model/database/datastore/common, never feature modules.

Task2 owns SessionTracker and all feature/reader integration. Task3 owns feature/book, feature/library, feature/settings, feature/celebrate and navigation to these surfaces. Root owns settings.gradle.kts, gradle.properties and cross-cutting docs. No overlapping edits without communication.

## Task1 — durable history and policy

Files: core/model tracking domain types; core/database entities/dao/AppDatabase/DatabaseMigrations/DatabaseModule/Converters and schema5; core/datastore TrackingSettingsRepository; tracking repository implementations and build.gradle.kts.

- [x] Write tests for all §8.3 transitions, latest-state derivation, half-star validation, manually finished dates, date ordering, deleting latest read-through, duplicate finish prompt claims, browsing exclusion and ignored exclusion.
- [x] Write migration4→5 test preserving existing books/locators/bookmarks and default assessment values. Check foreign-key behavior for Forget versus missing/ignore.
- [x] Implement transactionally. First qualifying session creates/resumes read-through; abandoned Resume keeps ID; Start over and finished Re-read create new ID. Just browsing links sessions to finished read-through without changing outcome. End-prompt claim is persisted atomically. Checkpoint updates use stable session ID and cumulative values, never additive reapplication.
- [x] Implement recovery of interrupted checkpoints without adding unobserved time; discard sub-minute recovered sessions. Validate user dates and preserve session history on pause/abandon/finish.
- [x] Persist settings with validated defaults/ranges, celebrations on and indicator off. Publish repository API before UI work depends on it.
- [x] Root runs relevant core/model/database/datastore/tracking tests and static checks, then commits the verified implementation.

## Task2 — accurate reader sessions

Files: tracking/SessionTracker and fake-clock tests; feature/reader ReaderEvents/ReaderInteraction/ReaderViewModel/ReaderActivity/ReaderScreen integration and tests.

- [x] Test 3 minutes of active reading plus6 minutes idle records180 seconds, resume-after-idle,59/60-second boundary,30-second checkpoint, screen off/background, wall-clock jumps, repeated visibility, book switches, rotation, browsing, end threshold and Off15-minute keep-awake release.
- [x] Implement serialized tracker using monotonic timestamps and wall-clock metadata/zone offset. Persist locator/progression/position/page event count. Avoid dropped lifecycle events and cancel/flush deterministically on leave.
- [x] Integrate reader events only after successful rendering; loading/errors/DRM never count. Actual page turns/scroll steps/selections renew activity; TOC/search/progress jumps and reflow do not count as pages read.
- [x] Show abandoned/finished open choices before accruing time; re-read resets navigator to start. End threshold prompts once per read-through; Yes finishes and routes to book celebration, No continues. Manual completion also uses same flow.
- [x] Apply keep-screen-on15-minute release only when applicable idle timeout Off; next actual reading interaction restores it. Screen off ends tracking even if keep-awake changed. Optional quiet indicator follows setting.
- [x] Root runs fake-clock and reader lifecycle regressions, then commits the verified implementation.

## Task3 — book/library/settings and celebration

Files: feature/book tracking cards/actions/history; feature/library state chips/filter/sort; feature/settings Tracking/Appearance screens; feature/celebrate module; app routing as necessary.

- [x] Test manual state changes/date editing/deletion and missing/protected book assessments. History shows read-throughs and individual sessions including labelled browsing entries; count/timing summaries exclude browsing.
- [x] Add rating half-stars/clear, favorite and saved private review with accessible controls. Keep edits/errors inside compact grouped cards/sheets and bottom notices.
- [x] Add actual five-state library indicators and persisted state/favorite/rating filters plus rating sort, preserving previous filter/sort choices. Do not add later-milestone categories/stats.
- [x] Add Tracking settings for both1–60/Off timeouts, quiet indicator and documented60-second minimum. Add Appearance celebrations toggle (on default). Existing Reader and Library settings unchanged.
- [x] Build finish celebration per visual§8.1: purple→green fields, seeded speed lines/sparks, cover rise, finished text, time/sessions/days counters, automatically exits~2.8s or tap to skip into rating/favorite sheet. Quiet version when disabled/system animations off. Personal-best and goals remain M6.
- [x] Test full/quiet completion, dismissal/restoration, large-text book/settings layouts, library filters and no replay on ordinary navigation. Root runs grouped tests and records renders.

## Task4 — integrate, audit, release handover

- [x] Self-review all spec rows against implementation; run combined audit over branch and address findings in one fix batch.
- [x] Update decision log for idle cutoff/checkpoint treatment, manual Not read confirmation and any other implementation decisions. Update glossary only for new domain terms, visual reference and roadmap M3 handover state.
- [x] Set deft.version=0.3.0-1, build `check assembleRelease` with persistent release signing/R8/resource shrinking. Run release identity/certificate/offline verifier, preserve mapping and source SHA. No GitHub CI wait.
- [x] Write plans/m3-test-checklist.md with every transition, idle timing, persistence/crash/upgrade, ratings/history, browsing exclusions, large text, dark/light, motion-off and previous reader regression checks.
- [x] Commit/push feat/m3-reading-tracking. Give deft-0.3.0.apk and checklist. Await device acceptance before merge/tag, unlike the explicitly authorized final M2 merge.
