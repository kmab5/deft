# M4 — categories, types, series and authors

> **For agentic workers:** Use subagent-driven development, test-driven development and codebase-design. Execute focused implementation tasks, then one combined verification/audit as the author requested. Only the root agent runs Gradle on this 8 GB host.

**Goal:** Fully implement roadmap M4 on `feat/m4-organization`, preserving the accepted EPUB and tracking experience.

**Architecture:** A transactional `library/organization` module owns organization rules and consistent snapshots. Room 7 stores categories, types, memberships, series aliases and explicit auto-fill exclusions. The scanner calls this module after indexing; category/author screens, Books selection and book details share its interface. A separate `feature/organization` keeps these screens focused while existing Books and reader modules retain their responsibilities.

**Tech stack:** Existing Kotlin/Compose/Room/Hilt/Flow/Coil stack only; SDK 36, minSdk 26, existing Gradle/JDKs. No new external dependency is required.

## Global constraints

- Book files are read-only; all edits affect Deft-owned data only.
- Ignored books disappear from organization and aggregates; missing books remain included. Forget alone deletes book history.
- No network outside `:data:matching`; this milestone adds no network behavior.
- App ID `com.kmab.deft`; persistent release key, R8/resource shrinking, release-only handover.
- Version `0.4.0-1`, code 40001, artifact `deft-0.4.0.apk`.
- Every schema change includes exported schema and migration coverage. Preserve all author feedback and untracked `error.log`.
- Commit authorship Samuel Worash Getaneh <samuelworash@gmail.com>; Conventional Commits. M4 stays on its branch until the author accepts its checklist. No GitHub CI wait.
- User explicitly directs the preceding 0.3.3 fixes onto main; M4 begins only after that verified fix is committed and pushed.

## Decisions and interface contract

The approved spec is sections 6.4, 7, 14 and 15. Existing visual language remains: outlined, subtly lifted cards, 16dp gutters, adaptive layouts, icons, lowercase chrome, proper author/title casing, 48dp controls, smooth reduced-motion-aware transitions. Activate only Books/Categories/Authors tabs; Notes/Stats arrive later.

`core/model/CategoryOrder.kt` defines `MANUAL, TITLE, AUTHOR, DATE_ADDED, RATING`. Series always use numeric position regardless of stored non-series order. Position accepts finite decimal values, including zero/prequels, within ±1e9. Same-position distinct books remain, with deterministic title/ID ordering. When merging duplicate book memberships, target position and confirmation win. Category date-added sorting uses membership added time.

Room entities in `core/database/entity/CategoryEntities.kt`:

```kotlin
CategoryTypeEntity(id: Long = 0, name: String, normalizedName: String, builtIn: Boolean = false)
CategoryEntity(id: Long = 0, name: String, description: String? = null,
    parentId: Long? = null, autoCreated: Boolean = false, coverPath: String? = null,
    order: CategoryOrder = CategoryOrder.MANUAL, ratingOverride: Double? = null, createdAt: Instant)
CategoryTypeLinkEntity(categoryId: Long, typeId: Long)
CategoryMemberEntity(categoryId: Long, bookId: Long, position: Double, suggested: Boolean, addedAt: Instant)
CategorySeriesAliasEntity(normalizedSeriesName: String, categoryId: Long)
CategoryAutoFillExclusionEntity(categoryId: Long, bookId: Long)
```

Category names are non-unique. Type normalized names are unique; built-in Series has ID 1 and cannot be renamed/deleted. `parentId` has SET NULL self-FK but is always null through v1 APIs. Join/member/alias/exclusion foreign keys cascade only organization records, never books or history. Explicit removal of an auto-filled membership records an exclusion so rescans respect the user's decision; manual add clears it. Deleting an auto-created category can recreate it on a future scan while auto-fill is on; disclose this in the confirmation. Editing an auto-created category name preserves its previous series key as an alias. Removing Series from a category clears aliases and auto-created ownership, allowing future scans to create a separate auto-created Series category.

`library/organization` public snapshots (Android library may reference Room's BookListItem):

```kotlin
data class OrganizedBook(val book: BookListItem, val state: BookState = NOT_STARTED,
    val rating: Double? = null, val activeSeconds: Long = 0)
data class OrganizationStats(val total: Int = 0, val finished: Int = 0, val reading: Int = 0,
    val paused: Int = 0, val abandoned: Int = 0, val activeSeconds: Long = 0,
    val averageRating: Double? = null)
data class CategoryBook(val book: OrganizedBook, val membership: CategoryMemberEntity)
data class CategorySummary(val category: CategoryEntity, val types: List<CategoryTypeEntity>,
    val books: List<CategoryBook>, val stats: OrganizationStats) {
    val isSeries: Boolean; val needsReview: Boolean; val rating: Double?
}
data class AuthorSummary(val author: AuthorEntity, val books: List<OrganizedBook>, val stats: OrganizationStats)
data class SeriesNext(val categoryId: Long, val categoryName: String, val book: OrganizedBook, val position: Double)
data class OrganizationSnapshot(val categories: List<CategorySummary> = emptyList(),
    val authors: List<AuthorSummary> = emptyList(), val types: List<CategoryTypeEntity> = emptyList(),
    val books: List<OrganizedBook> = emptyList()) {
    fun nextInSeries(bookId: Long): List<SeriesNext>
}
```

`OrganizationRepository(db: AppDatabase)` is injectable singleton; all writes and snapshot reads use Room transactions. Observe invalidation of source tables, load bounded projections once per table, derive maps/grouping in O(books+memberships); no per-book queries, no loading all session rows just for aggregates. Latest read-through identity determines state. Session totals exclude browsing and rows below their captured minimum. Co-authored books count fully for each effective author; prefer overridden author links consistently with Books.

```kotlin
fun observe(): Flow<OrganizationSnapshot>
suspend fun get(): OrganizationSnapshot
suspend fun saveCategory(id: Long? = null, name: String, description: String? = null,
    typeIds: Set<Long> = emptySet(), order: CategoryOrder = MANUAL, coverPath: String? = null): Long
suspend fun deleteCategory(id: Long)
suspend fun saveType(id: Long? = null, name: String): Long
suspend fun deleteType(id: Long)
suspend fun addBooks(categoryId: Long, bookIds: Set<Long>)
suspend fun removeMember(categoryId: Long, bookId: Long)
suspend fun setPosition(categoryId: Long, bookId: Long, position: Double)
suspend fun reorder(categoryId: Long, visibleBookIds: List<Long>)
suspend fun confirmMembers(categoryId: Long, bookIds: Set<Long>)
suspend fun setRating(categoryId: Long, rating: Double?)
suspend fun mergeCategories(sourceId: Long, targetId: Long)
suspend fun renameAuthor(id: Long, name: String)
suspend fun mergeAuthors(sourceId: Long, targetId: Long)
suspend fun autoFill()
```

Names are bounded to 240 characters and descriptions to 10,000. Validate blank/overlong names, missing IDs, duplicate types, nonfinite positions, invalid half-star ratings, self merges and invalid reorder lists. Manual reordering is non-Series MANUAL only and accepts exactly the visible membership set; ignored members remain stored. Metadata/target attributes stay on merges; union types/members/aliases, target membership wins, transfer exclusions only where there is no unioned membership. The explicit merge brings all source members, including suggestions, and clears exclusions for those members. Author rename retains old normalized name as alias; collision with another canonical/alias name requires merge. Author merge deduplicates links preserving effective overrides/positions and redirects all aliases before deleting the source author. Neither operation touches book files/history.

## Task 1 — persistent organization rules

**Files:** create CategoryOrder, CategoryEntities, OrganizationDao, OrganizationMigration and Room schema 7; modify AppDatabase, DatabaseModule, DatabaseMigrations and existing migration chains. Create `library/organization/build.gradle.kts`, manifest, `OrganizationModels.kt`, `OrganizationRepository.kt`, focused internal category/author/auto-fill helpers and tests. Register module.

- [ ] Add failing repository/migration tests before implementation. Representative acceptance:

```kotlin
val manual = repo.saveCategory(name = "Earthsea", typeIds = setOf(1))
repo.autoFill()
val automatic = repo.get().categories.single { it.category.autoCreated }
assertNotEquals(manual, automatic.category.id)
assertTrue(automatic.needsReview)
repo.mergeCategories(automatic.category.id, manual)
repo.autoFill()
assertEquals(1, repo.get().categories.size)
repo.setRating(manual, 4.5)
assertEquals(4.5, repo.get().categories.single().rating)
repo.setRating(manual, null)
assertEquals(3.75, repo.get().categories.single().rating)
```

Use fixtures covering case/whitespace series names, 1/2/2.5 indices, absent index append, existing confirmed position untouched, explicit removal suppression, disabled auto-fill caller, ignored books, missing members, abandoned unresolved, browsing excluded, duplicate authors/aliases, target conflict, effective author overrides, empty categories, deleting category/type with history intact and valid/invalid decimal/reorder/rating inputs. Migration test upgrades 6→7 and an earlier complete chain, checks existing sessions/locators/ratings, seeds Series and verifies SET NULL parent FK.
- [ ] Run grouped red tests, implement the contract, run green tests. Export schema via KSP.
- [ ] Commit the complete verified foundation as `feat(organization): add categories and author rules`.

## Task 2 — scanner and persistent preferences

**Files:** `ScanRunner.kt`, scanner Gradle/tests; LibrarySettings/Repository/tests; LibrarySettingsScreen/ViewModel/tests.

- [ ] Add `seriesAutoFill=true`, nullable `categoryFilter` and `categoryTypeFilter`, `groupCategoriesByType=false`, `categoryGridView=false`; explicit typed setters and persistence regressions.
- [ ] Inject OrganizationRepository into ScanRunner; after each successful indexing batch, call autoFill only when enabled. This processes stored metadata on unchanged existing EPUBs too. Keep existing scan lock, continuation and reconciliation behavior; auto-fill failures must be reported, not falsely marked successful.
- [ ] Add the Library settings series auto-fill switch with explanation and explicit rescan when enabling. Existing category membership remains when disabled.
- [ ] Test real `calibre:series` EPUB scan creates one ordered suggested Series category, disabling prevents additions, re-enabling handles unchanged fingerprints, alias merge survives rescan and ignored metadata is excluded.
- [ ] Commit scanner/preferences integration after its grouped tests.

## Task 3 — category and author experience

**Files:** new `feature/organization` module with navigation, view-model, categories list/cards, category detail/member/reorder/editor sheets, type editor, author list/detail/editor, reusable membership picker/progress/collage components, bounded private category cover import and Compose/render tests. Existing feature/book screens/view-model/navigation may be updated by this task. No root Books/app files are owned by this task.

- [ ] Build real Categories and Authors destinations and detail routes (`categories`, `category/{categoryId}`, `authors`, `author/{authorId}`). Top-level overflow opens existing settings. Use guarded Back on details.
- [ ] Category cards show cover collage (up to four nonignored covers) or imported custom cover, types, review indicator, progress and computed/overridden rating. Persist grid/list and type filter/group controls; show all/untyped handling and empty/error/loading states.
- [ ] Create/edit/delete categories, description, types and bounded image cover; remove custom cover returns collage. Type CRUD protects Series. Duplicate category names remain identifiable by types/count/ID in merge/picker destinations.
- [ ] Detail uses grouped identity/progress/rating/actions, ordered members, decimal series edit, individual/all suggestion confirmation, member removal, add-book picker, ordering choices, drag reordering plus accessible move up/down actions, target-select/confirm merge and destructive category confirmation. Rating override/reset clearly distinguishes computed mean.
- [ ] Authors list/search and detail show counts, all five state contributions, finished progress, total time and computed rating, books grouped by state, rename and confirmed merge. Only authors with nonignored effective books appear.
- [ ] Book page author names navigate to authors; category membership and positions link to categories, allow adding/removing through shared controls, and show each eligible next-in-series candidate. Missing/Protected books remain manageable.
- [ ] Reuse existing design tokens and safe image loader, with notices for errors. Render paper, ink, 150% text and landscape; test create/edit/confirm/merge/error controls and route callbacks with real snapshots.
- [ ] Commit verified UI.

## Task 4 — Books selection, app navigation and handover

**Files:** BooksScreen/Card/Row/ViewModel/Controls/Navigation and selection components/tests; DeftApp, app Gradle/tests; DeftBottomBar (optional visible destination indices preserving existing default goldens); docs/checklist/reference/version.

- [ ] Enable long-press selection in grid and list, selection indication/count, select all visible, clear/back exit, and Add to category with create-new option. Apply only explicitly selected IDs, reconcile stale/forgotten IDs, preserve selection through rotation, exit after successful addition; no accidental book launch while selecting.
- [ ] Add persisted category filter beside existing filters; all/uncategorized choices if exposed must have defined semantics. Deleted selected category falls back to all instead of trapping an empty library.
- [ ] Show Books/Categories/Authors bottom navigation only on those top-level destinations. Preserve each tab's state, navigation root, scan notices, reader completion routing and settings entry. Future tabs stay hidden.
- [ ] Run focused red/green tests during development, then one whole-M4 audit and `ktlintFormat check assembleRelease`, single Gradle worker. Fix failures together. Inspect generated UI renders and verify Room migration, scanner acceptance, privacy guardrails and signed release identity.
- [ ] Update spec decision log, visual reference, README/roadmap status and `plans/m4-test-checklist.md` with thorough tickable device steps including upgrade, types, series, aliases, merges, ignore/missing, reorder, ratings, selection, author links, accessibility and scanner cancellation.
- [ ] Commit/push `feat/m4-organization`; archive mapping and exact source evidence; give signed `deft-0.4.0.apk` and checklist. Do not merge M4 before author acceptance.

## Validation commands

Use the installed helper with one worker and existing JDK17/JDK21/SDK paths. Group tasks, never run concurrent Gradle:

```powershell
powershell -NoProfile -File .superpowers/m3-gradle.ps1 -Tasks ':core:database:testDebugUnitTest :library:organization:testDebugUnitTest :core:datastore:testDebugUnitTest :library:scanner:testDebugUnitTest' -Log m4-foundation -Workers 1
powershell -NoProfile -File .superpowers/m3-gradle.ps1 -Tasks 'ktlintFormat check assembleRelease' -Log m4-check -Workers 1
python scripts/verify-release.py
```

No emulator/device is assumed available. Distinguish local Compose/Robolectric render and behavioral evidence from phone verification. Do not wait for GitHub CI.
