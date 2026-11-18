# M1 device feedback implementation plan

**Goal:** Address every actionable note from the Samsung S25 FE test of 0.1.0-1, preserving the author's original checklist.

**Architecture:** Retain Kotlin/Compose, Room, DataStore and read-only SAF. Improve scanner reliability and observability, library presentation and folder management in independent batches, then run combined tests and one whole-change audit as requested by the author.

**Constraints:** No book-file writes, lost history, network access, invented reading events or replacement signing keys. Reuse installed tools. Push the branch without waiting for GitHub CI. No merge before device acceptance.

## 1. Large libraries and scan feedback

- [x] Reproduce large initial imports and interruption with scanner regression tests; inspect provider traversal and metadata processing costs.
- [x] Fix demonstrated bottlenecks/failures and make long scans resumable or safely scheduled; preserve incomplete-root missing-file safeguards.
- [x] Expose processed counts and meaningful completed/partial/error summaries, including why file and Book counts differ.
- [x] Show separate storage components and avoid redundant cover caching or unnecessary database churn; the author confirmed total storage is acceptable.

## 2. Books controls and presentation

- [x] Add persisted small/medium/large display sizes for grid and list, reverse sorting and last-read sorting; null last-read values remain unread with deterministic ordering.
- [x] Add any required Room migration and migration tests; preserve existing data and preferences.
- [x] Redesign compact, clearly labelled filter/sort/display controls, readable two-line titles and balanced cover spacing. Keep the established paper/ink palette and typography.

## 3. Settings, folders and icon

- [x] Remove the empty intermediate Settings page in M1. Present scan information near the top, then clear folder cards and compact file-type controls.
- [x] Make excluded folders and the exclude action visible, retain picker state across recreation and improve overlap explanations with corrective instructions.
- [x] Add launcher foreground and monochrome mark padding without changing identity.

## 4. Verification and handover

- [x] Record visual/behavior decisions in the spec and visual reference.
- [x] Run targeted regressions followed by `check assembleRelease`, audit the complete changes, and verify the persistent signing certificate and R8 output.
- [x] Preserve original feedback and supply a fresh retest checklist with each feedback item's disposition. Android 8/9 device checks remain skipped, not claimed passed; reader functionality remains M2.
- [x] Hand over the signed release APK with an update-compatible version and local verification evidence. Push without waiting for CI; do not merge or tag.

## Combined audit and verification

The whole-change audit found two issues: repeated four-minute retries could stall on slow providers, and status labels could clip on small covers. Manual foreground execution with finite fallback retries resolves the first; list labels beside metadata and compact grid status icons resolve the second. Scoped re-review found no remaining blocking defect.

The final `ktlintFormat check` pass succeeded with 282 recorded JVM/Robolectric test executions and zero failures/errors. Four Python release-tooling tests also passed. Phone-size captures were visually inspected in paper/ink, including real 150% system font scaling. The 1,500-EPUB interrupted-import regression and 5,000-unchanged-file regression passed. These are automated checks, not physical Samsung performance measurements.

The author clarified that about 80 MB for 1,400 Books is acceptable. Thumbnail quality remains unchanged. PDF scanning was disabled in the supplied screenshots; settings now makes disabled file types explicit. Home/screen-off continuation, actual update installation and TalkBack remain device acceptance checks.

`check assembleRelease` also passed on the frozen final sources. The release verifier confirmed the persistent signing identity, application/version, no INTERNET permission and R8 mapping. Release metadata is in the feedback retest checklist. The branch is handed over for acceptance, not merged or tagged.
