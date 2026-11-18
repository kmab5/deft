# M1 device feedback implementation plan

**Goal:** Address every actionable note from the Samsung S25 FE test of 0.1.0-1, preserving the author's original checklist.

**Architecture:** Retain Kotlin/Compose, Room, DataStore and read-only SAF. Improve scanner reliability and observability, library presentation and folder management in independent batches, then run combined tests and one whole-change audit as requested by the author.

**Constraints:** No book-file writes, lost history, network access, invented reading events or replacement signing keys. Reuse installed tools. Push the branch without waiting for GitHub CI. No merge before device acceptance.

## 1. Large libraries and scan feedback

- [ ] Reproduce large initial imports and interruption with scanner regression tests; inspect provider traversal and metadata processing costs.
- [ ] Fix demonstrated bottlenecks/failures and make long scans resumable or safely scheduled; preserve incomplete-root missing-file safeguards.
- [ ] Expose processed counts and meaningful completed/partial/error summaries, including why file and Book counts differ.
- [ ] Measure storage components and avoid redundant cover caching or unnecessary database churn.

## 2. Books controls and presentation

- [ ] Add persisted small/medium/large display sizes for grid and list, reverse sorting and last-read sorting; null last-read values remain unread with deterministic ordering.
- [ ] Add any required Room migration and migration tests; preserve existing data and preferences.
- [ ] Redesign compact, clearly labelled filter/sort/display controls, readable two-line titles and balanced cover spacing. Keep the established paper/ink palette and typography.

## 3. Settings, folders and icon

- [ ] Remove the empty intermediate Settings page in M1. Present scan information near the top, then clear folder cards and compact file-type controls.
- [ ] Make excluded folders and the exclude action visible, retain picker state across recreation and improve overlap explanations with corrective instructions.
- [ ] Add launcher foreground and monochrome mark padding without changing identity.

## 4. Verification and handover

- [ ] Record visual/behavior decisions in the spec and visual reference.
- [ ] Run targeted regressions followed by `check assembleRelease`, audit the complete changes, and verify the persistent signing certificate and R8 output.
- [ ] Preserve original feedback and supply a fresh retest checklist with each feedback item's disposition. Android 8/9 device checks remain skipped, not claimed passed; reader functionality remains M2.
- [ ] Hand over the signed release APK with an update-compatible version and local verification evidence. Push without waiting for CI; do not merge or tag.
