# M2 EPUB reader implementation plan

> **For agentic workers:** Use subagent-driven-development with the author's batched implementation/verification override. Write regression tests alongside each implementation slice; only the root runs Gradle, after sources are frozen. One whole-change audit at the end.

**Goal:** Read local EPUBs with persistent positions/bookmarks and complete global reader controls, then hand over one signed release containing M1 feedback fixes and M2.

**Architecture:** Keep pure EPUB metadata extraction intact. Add Android `format/readium` for offline publication opening and navigator adaptation, `feature/reader` for a Compose-hosted navigator activity, `feature/reader-settings` for reusable global settings controls, and `feature/book` for book details. Room owns positions/bookmarks; the existing DataStore owns global preferences/custom themes. Format code knows nothing about Room. App navigation connects library, detail, settings and reader launcher.

**Tech stack:** Existing Kotlin 2.2.21 / AGP 8.13.2 / Gradle 8.13 / SDK 36; Readium 3.2.0 shared/streamer/navigator, Compose, Hilt, Room and DataStore. Core-library desugaring 2.1.5 is a project dependency, not a toolchain download. Readium metadata and its transitive AAR requirements have been checked against the existing tools.

## Global constraints

- `com.kmab.deft`, Android minSdk26. GPL-3.0-compatible/F-Droid-clean dependencies only.
- Book files are read-only. Access via retained SAF grants; no DRM decryption, LCP module, analytics or HTTP client dependency.
- Deny Readium HTTP access; block external publication resources and links; keep INTERNET absent. Trusted navigator scripts may run, publication-supplied scripts must not.
- Preserve history on missing files and ignore. Forget is explicitly confirmed and coordinated with scanner writes.
- Every Room change includes exported schema and migration tests. Schema3 upgrades to4 with positions and Bookmark.
- Reader settings are global. Settings gains Library and Reader categories now that two real sections exist.
- Reader fonts follow visual §3.2: original, Literata(default), Source Serif4, PT Serif, EB Garamond, Crimson Pro, Bitter, Lato, Open Sans, Inter, Atkinson Hyperlegible, OpenDyslexic. Bundled reader assets <=3MB; record any face-budget compromise.
- Use current compact controls, meaningful icons, timed bottom errors, smooth system-scaled transitions, readable large-text layouts and 48dp targets.
- M3 owns sessions/states/time estimates; M5 owns highlight/note/search actions. M2 must not fabricate those data or display non-working actions.
- Branch `feat/m2-epub-reader` stacks on pending M1 commit9970fc0 at the author's request. No merge/tag until combined device acceptance. Version0.2.0-1/code20001; APK `deft-0.2.0.apk`, persistent release certificate and R8/resource shrinking.

## Shared interfaces

Pure types in `core/model`:

```kotlin
data class ReadingPosition(val locatorJson: String, val totalProgression: Double)
enum class ReaderMargins { SMALL, MEDIUM, LARGE }
enum class ReaderAlignment { START, JUSTIFY }
enum class PageAnimation { NONE, SLIDE }
data class ReaderPreferences(
    val fontFamily: String = "Literata", val fontSize: Double = 1.0,
    val lineHeight: Double = 1.5, val margins: ReaderMargins = ReaderMargins.MEDIUM,
    val alignment: ReaderAlignment = ReaderAlignment.START, val hyphenation: Boolean = true,
    val themeId: String = "light", val followSystem: Boolean = false,
    val lightThemeId: String = "light", val darkThemeId: String = "dark",
    val brightness: Float = 0.5f, val useSystemBrightness: Boolean = true,
    val scroll: Boolean = false, val pageAnimation: PageAnimation = PageAnimation.SLIDE,
    val showStatusLine: Boolean = true, val volumeKeys: Boolean = false,
    val keepScreenOn: Boolean = true,
)
data class ReaderTheme(val id: String, val name: String, val background: String, val text: String, val accent: String)
data class ReaderFontFace(val assetPath: String? = null, val privateFilePath: String? = null,
    val italic: Boolean = false, val weight: Int = 400)
data class ReaderFont(val id: String, val name: String, val faces: List<ReaderFontFace>, val imported: Boolean = false)
```

`ReaderSettingsRepository` in core/datastore exposes `preferences: Flow<ReaderPreferences>`, `customThemes: Flow<List<ReaderTheme>>`, `suspend update(transform: (ReaderPreferences) -> ReaderPreferences)`, `upsertTheme(ReaderTheme)`, `deleteTheme(id:String)`. Validate finite/clamped typography/brightness, colors, names and reserved IDs. Atomic deletion resets all selected theme references.

`ReaderFontRepository` in feature/reader-settings exposes `fonts: StateFlow<List<ReaderFont>>`, `suspend importFont(uri:Uri): ReaderFont`, `suspend removeFont(id:String)`. Copy/validate bounded TTF/OTF to private storage, use safe generated names, persist catalog, expose bundled and imported faces, reset selected font on removal. No raw source URI/path is trusted by WebView.

`BookRepository` expands in library/scanner with `observeBook(id): Flow<BookDetails?>`, `getBook(id): BookDetails?`, `observeIgnoredBooks(): Flow<List<BookListItem>>`, `observeContinueReading(): Flow<BookListItem?>`, `savePosition(id, ReadingPosition)`, `recordOpened(id)`, `setIgnored(id,Boolean)`, `forget(id)`, `observeBookmarks(id): Flow<List<BookmarkEntity>>`, `addBookmark(id,position,label):Long`, `renameBookmark(id,label)`, `removeBookmark(id)`. BookDetails in core/database/model contains `book:BookEntity`, `authorsDisplay:String`, `files:List<BookFileEntity>`. BookEntity gains currentLocator(nullable) and totalProgression(default0); BookListItem gains totalProgression(default0). BookmarkEntity contains id/bookId/locator/totalProgression/label/createdAt, indexed foreign key with CASCADE on explicit Book deletion. Saving a successfully rendered position updates lastReadAt; recordOpened updates lastOpenedAt. No sessions or reading-state inference. The application writer serializes/coalesces pending positions; wall-clock rollback never rejects a newer ordered location. Read/open timestamps remain monotonic, and open ordering advances by at least 1 ms across books.

`ReaderPreferencesPanel(modifier:Modifier=Modifier, manageAssets:Boolean=false)` in feature/reader-settings edits injected shared preferences, three tabs text/theme/page. Reader invokes it in its themed sheet; Settings invokes it with management enabled. `ReaderSettingsScreen(onBack)` is the full-screen entry.

`ReaderLauncher.open(bookId:Long)` in feature/reader opens ReaderActivity. `ReaderEvents.events: SharedFlow<ReaderEvent>` emits book identity and positions for PageTurn, ScrollStep, Selection and Visibility; distinguish initial/restored location/programmatic jumps from reading turns. Root app consumes only launcher; M3 consumes events later.

## Task 1 — Persistence and global preferences

Files: core/model reader types; core/database BookEntity/BookmarkEntity, DAOs, BookDetails/BookListItem, AppDatabase, migrations/module/schema4; core/datastore ReaderSettingsRepository; library/scanner BookRepository; corresponding unit/migration/repository tests.

- [x] Write tests: v3 books/lastReadAt survive migration; bookmark/locator round-trip; ignore survives scan and preserves bookmarks; forget removes Book/bookmarks/files only, never source; missing books retain positions; continue excludes ignored/missing/protected; preference edits preserve concurrent unrelated fields; malformed persisted values fall back; deleted custom themes reset references.
- [x] Implement narrow SQL updates for position/timestamps/ignore to avoid overwriting concurrent scanner metadata. Coordinate forget through ScanCoordination and use transactions. Validate locators/progression before writes; no orphan bookmark resurrection after forget.
- [x] Export schema4 with migration3→4 and test old schemas1/2 chain. Root verifies once integrated.

## Task 2 — Reader preferences and font assets

Files: new feature/reader-settings module/build, ReaderPreferencesPanel/ReaderSettingsScreen, ReaderFontRepository/catalog/import validation and tests; reader-fonts assets/licenses; font acquisition/subset provenance and budget check.

- [x] Write preference panel tests for each control and custom theme/font lifecycle, real150% font scale, icons/semantics, validation errors as notices. Font tests cover malformed/oversize imports, private filenames, reopen and selected-font removal fallback.
- [x] Implement compact text/theme/page tabs: original and named font preview, size/line height, margins/alignment/hyphenation; built-in/custom theme swatches and editor with background/text/accent, follow-system light/dark picks; brightness/system toggle; paged/scroll, none/slide, status, volume keys and keep awake.
- [x] Full settings additionally add/remove fonts and create/edit/delete custom themes. Reader sheet and full screen use identical repository fields, and changes apply live. Built-in theme colors match visual tokens; custom colors require readable contrast and valid hex.
- [x] Bundle licensed subset fonts within3MB with reproducible provenance/size assertion. Use local tooling; project font files/dependencies may be downloaded, installed Android/Gradle/JDK tools remain unchanged.

## Task 3 — Offline Readium engine and complete reader shell

Files: format/readium module with offline opener/resource policy/font bridge/preferences mapping/navigation tests; feature/reader module with ReaderActivity, launcher, events, lifecycle/persistence, chrome, contents/bookmarks/progress/settings sheets; manifest/resources/tests/fixtures.

- [x] Write tests for offline URI/resource policy, hostile scripts/external URLs, protected/error cases, preferences mapping, locator restore/fallback, actual page/scroll events versus jumps, volume keys/brightness/window cleanup, bookmark toggle/rename/remove and jump-return behavior.
- [x] Open present EPUB copies only, fail clearly for missing/protected/other formats. Check changed-content identity before attaching saved positions; never modify source files. Readium parser uses deny-all HttpClient and empty content protections; container/HTML filtering blocks source scripts/remote resources while preserving trusted pagination scripts. Retain manifest-level no-network guard.
- [x] Host real EpubNavigatorFragment in Compose through FragmentActivity with controlled restoration. Close publications/assets on destruction, cancel stale opens, serialize/coalesce position writes and flush on exit/background. Restore persisted locator, with bounded totalProgression fallback when necessary.
- [x] Full-bleed content; animated tap-center bars. Back/title/contents/bookmark/settings only (search arrives M5). TOC tree and bookmark list support navigation and label edits. Progress slider shows chapter ticks and truthful chapter/percentage, with go-back after jumps. No time-left estimate before M3.
- [x] Apply all global controls live. Follow-system colors, local imported/bundled fonts, brightness override reset on leave, volume key down/up consumption only when enabled, immersive system bars, optional clock/battery/chapter status, keep-awake lifecycle cleanup. Paged and scroll input emit meaningful ReaderEvents for M3.

## Task 4 — Library, book details and Settings navigation

Files: feature/book new module; feature/library cards/screen/viewmodel/navigation; feature/settings category list/library ignored entry/ignored list; app navigation/module wiring; tests/rendered reference.

- [x] Write UI/repository integration tests: tapping book opens detail; read/continue calls launcher with correct ID; continue card restores last readable EPUB; ignored list restores ignored books; forget requires confirmation and retains source file; missing/protected/unsupported explanations are honest.
- [x] Implement basic book detail with cover, title/authors, metadata, description, Read/Continue, percentage, file format/path/size and protected/missing labels, overflow Ignore/Un-ignore and Forget. Omit M3/M4/M5 statistics/categories/notes controls until real.
- [x] Add Continue reading to Books without fabricated reading state/time; clickable grid/list cards lead to details. Add settings chooser Library/Reader and Library→Ignored books. Preserve all pending M1 feedback behavior.
- [x] Wire modules, desugaring, version0.2.0, reader launch and global notice hosts. Keep existing compiler/toolchains. Add dependency license/source notes and decision log.

## Task 5 — Whole milestone verification and handover

- [x] Freeze sources; root runs batched `ktlintFormat check` using installed JDK17/JDK21/SDK and no concurrent Gradle. One whole-change audit checks M2 coverage, offline/security, migration/history, lifecycle/events, accessibility and maintainability. Fix failures as a batch.
- [x] Run final `check assembleRelease`; verify actual APK certificate, identity/versionCode20001, no INTERNET, R8 mapping and resource shrinking. Reuse persistent key. Run release-tooling tests and inspect paper/ink/reader settings renders.
- [x] Create `plans/m2-test-checklist.md`: upgrade/M1 regressions, every reader control, font/theme management, EPUB edge cases, navigation/bookmarks/restore/lifecycle, ignored/forget/history, privacy, accessibility and a final how-does-it-look section. Clearly distinguish automated checks from device-only checks.
- [x] Hand over `app/build/outputs/apk/release/deft-0.2.0.apk`, checklist, source/hash/certificate evidence. Push branch without waiting for GitHub CI. No merge/tag before the author's combined acceptance.
