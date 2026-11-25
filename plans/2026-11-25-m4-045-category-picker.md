# 0.4.5 — repair CI, then expose category creation from Books

**Authorization:** the author requests work directly on `main`, with CI repaired before feature changes. They explicitly approved including the 0.4.4 category multi-select addition in this release. Keep their existing M1 checklist edits untouched. No new branch or pull request.

**Scope:** repair the observed asynchronous test failures, integrate 0.4.4, and make category creation immediately available from Books → select books → add to category. Reuse the existing category editor and atomic `createCategoryWithBooks` operation. No schema change, new dependency, book-file modification or history deletion.

## 1. CI repair first

- [x] Inspect the failed main run `45061790837` and 0.4.4 tag run `45158770634`, including their XML reports.
- [x] Diagnose `LibrarySettingsScreenTest`: an unconfined Main dispatcher lets DataStore resume Compose on an IO thread; the removal test checks permission release after the outbox commit but before the removal job completes.
- [ ] Remove the fixture's Main override, keep teardown nonblocking on Android Main, and await removal completion before asserting the outbox and permission release. Preserve the assertions and production behavior.
- [ ] Run settings formatting/static analysis and Debug/Release unit suites together. Commit and push the CI repair on main before feature edits; observe its GitHub run while completing the remaining work.

## 2. Integrate the approved 0.4.4 addition

- [ ] Merge `fix/m4-category-selection` into main with `--no-ff`, preserving both its feature and the CI repair. Stay on main.
- [ ] Keep existing tags immutable. The requested combined handover is `v0.4.5-1`; bump `deft.version` before building the handover.

## 3. Keep creation available in the Books picker

Files: `feature/organization/.../CategoryMembershipPicker.kt`, focused picker tests under the same module, and existing editor/repository interfaces as needed.

- [ ] Add a failing regression for the create action remaining visible with many categories, including large text/landscape. Exercise the real picker/editor save and cancel paths with an injected test ViewModel.
- [ ] Keep search and the create action outside a bounded lazy category list. Use the existing Deft button, folder-plus icon, dividers and sheet styling. Creation remains available with no categories or no search matches.
- [ ] Saving creates one category and assigns exactly the selected book IDs through the existing transaction. Cancelling returns to the picker without creating a category or reporting success. Existing-category assignment remains unchanged.
- [ ] Run the organization UI/domain regressions together; inspect the affected picker renders. Commit the feature after green tests.

## 4. Verify and hand over 0.4.5

- [ ] Update the spec decision log, visual guidance, roadmap/index and append 0.4.5 device checks to the M4 checklist without rewriting earlier review results.
- [ ] Perform one consolidated code review. Run `./gradlew check assembleRelease` with the installed JDKs/Android SDK; run release-tooling tests and strict release verification.
- [ ] Commit final source/version/docs on main, then package from that committed source. Reuse the permanent release key, R8 and resource shrinking. Archive the APK checksum, matching mapping and source provenance outside Git; AGP embeds source-control information, so compute the checksum after the final commit.
- [ ] Push main and `v0.4.5-1`. Supply `deft-0.4.5.apk` and the new checklist section. Confirm the CI repair's result; do not wait unnecessarily for the final tag publication when the verified local APK is ready.
