# M4 category multi-select — 0.4.4

> Execute with subagent-driven-development and test-driven-development. Batch verification and the final review, per the author's workflow preference. Only the root agent runs Gradle.

**Goal:** long-press multi-select for Categories, following Books, with atomic category bulk operations.

**Architecture:** keep category selection in the screen's saved state, keyed by category ID. Repository commands own validation and one Room transaction per user action. Use existing category type/merge rules and shared design-system dialogs, sheets, motion and haptics. No Room schema change.

**Constraints:** preserve book files, Book rows and reading history; preserve target metadata/positions on merge; use existing release identity, R8 and resource shrinking. Branch `fix/m4-category-selection`; version `0.4.4-1`. Preserve original device feedback. No merge before author acceptance.

## Interaction and operation contract

- A normal tap opens a category. Long press enters selection; subsequent taps toggle. A checkmark and accent outline indicate selection in list/grid, including every appearance of a category grouped under multiple types.
- A pinned, animated selection bar shows count, clear, select all visible and an actions menu. Back clears selection after dismissing any active sheet/dialog. Search/filter changes prune hidden IDs like Books; sort/display/group changes preserve still-visible IDs. Saved state survives rotation. Selecting all deduplicates categories shown in multiple type groups.
- Core actions: add/remove selected types without replacing unrelated types; merge selected categories into an explicitly chosen destination; delete selected categories after confirmation. A destination may be one of the selected categories and must survive. Existing target details and book positions win. Remaining source conflicts use ascending category ID for deterministic results.
- Type removal follows existing Series behavior, clearing series aliases/auto-created status when Series is removed. Adding Series does not turn a user category into an auto-created category.
- Bulk deletion only deletes categories and their category relations. Books/history stay. Explain that scans may recreate categories for source series metadata when auto-fill is enabled.
- Any stale category/type rejects the entire transaction. Failed operations retain selection and show existing bottom notices. Disable repeat submissions while busy. Clear selection only after success. Clean removed custom covers only after database success.
- Optional scope clarification has been sent for rating/review actions. Default is the three core operations above; update this plan if the author chooses more.

## Task 1 — transactional bulk commands and regression tests

Files: `library/organization/.../OrganizationRepository.kt`, `CategoryOperations.kt`, a focused bulk command helper if needed, and `src/test/.../CategoryBulkOperationsTest.kt`.

Public interfaces:
```kotlin
suspend fun deleteCategories(ids: Set<Long>)
suspend fun updateCategoryTypes(ids: Set<Long>, typeIds: Set<Long>, add: Boolean)
suspend fun mergeCategories(sourceIds: Set<Long>, targetId: Long)
```

- [ ] First add tests exercising stale-ID rollback, empty inputs, preservation of books/history and unrelated metadata/types, Series alias semantics, and multi-source merge with overlap/target-in-selection.
- [ ] Run `:library:organization:testDebugUnitTest --tests '*CategoryBulkOperationsTest'` and record the initial failure.
- [ ] Implement the commands behind the repository transaction, reusing existing category rules. Keep source ordering deterministic.
- [ ] Run the new tests and existing organization rules together; commit the completed domain change.

## Task 2 — category selection, actions and UI regressions

Files: `feature/organization/.../CategoriesScreen.kt`, selection/actions composables, `OrganizationViewModel.kt` as needed, and `src/test/.../CategorySelectionTest.kt`.

- [ ] First add UI tests for long-press vs open, multiple selection, clear/select-all, duplicate group appearances, visible filtering, restoration, type actions, merge/delete confirmation and cancellation. Exercise a narrow screen at 150% text.
- [ ] Run the targeted test class to establish failures before production UI changes.
- [ ] Implement the pinned selection bar and accessible selection states, type sheet, target picker and confirmations. Preserve browsing controls and responsive layouts. Use the Task 1 commands and existing error/busy handling.
- [ ] Run UI/domain regressions together and inspect native renders in both palettes, narrow large-text and landscape. Commit the completed UI change.

## Task 3 — combined verification and release handover

- [ ] Update spec §7/§20, visual design, roadmap M4 and append a 0.4.4 device retest section to `plans/m4-test-checklist.md` without altering original results.
- [ ] Increment `deft.version=0.4.4-1`; run formatting and full `check assembleRelease` with installed JDK17/JDK21/SDK. Run Python release tooling tests and strict release verification.
- [ ] Run one consolidated branch code review, fix material findings and re-run affected checks only as needed.
- [ ] Commit final changes, build the signed handover from committed source, archive APK/mapping/hash evidence and give the author `deft-0.4.4.apk` plus the checklist. Push the branch and revision tag to trigger direct GitHub Actions release publication. Do not merge into main without acceptance.
