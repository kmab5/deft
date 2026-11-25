# Deft — Design

*Approved 2026-11-13; living specification. M1–M4 are complete, the accepted release is `v0.4.3-2`, and M5 is next. Current milestone order is recorded in §17 and the roadmap.*

> deft — read like you mean it.

Deft is an offline, accountless Android ebook reader with a built-in reading tracker. It reads the user's local ebook files with Lithium-level simplicity and turns every minute of reading into accurate, personal data: states, time, sessions, ratings, categories, series, and stats.

This document is the source of truth for *what* Deft is and the decisions already made about *how*. The roadmap (`plans/2026-11-13-deft-roadmap.md`) breaks it into milestones; each milestone has its own task-level plan in `plans/`.

---

## 1. Why Deft exists

The author reads a lot. Their reading life was spread across three tools:

- **Lithium** for reading, with series and lists maintained by hand.
- **Bookmory** for timing reading sessions — with constant app-switching and forgotten timers that ruin the data.
- **bookledger** (a personal Python catalog) built to keep track of what had been read — a stopgap that still needed manual entry for every new book.

Tracking matters to them for two reasons: to see their own reading behavior, and to feed sites like Goodreads / StoryGraph / Hardcover whose recommendation algorithms depend on accurate history and ratings ("Letterboxd for books").

**Deft puts the reader and the tracker in the same app**, so tracking is accurate by default: opening a book starts the clock, leaving it stops it, finishing it records it. Nothing to remember.

**Audience:** first the author (a heavy reader with a large local library), then the portfolio, then any stranger who wants the same thing. Strangers are welcome but do not drive the design.

**What success looks like:** the author stops using Lithium, Bookmory and bookledger, reads only in Deft, and can at any time export an accurate history to Goodreads/StoryGraph/Hardcover.

## 2. Goals, non-goals, anti-goals

### Goals (v1)
1. A fast, minimal reader for: **EPUB, MOBI, AZW, AZW3, FB2, TXT** (reflowable) and **PDF, DjVu, CBZ** (fixed-layout). Nine formats (the original list of twelve counted `.djv` and `.djvu` separately); RTF and LIT are out of scope (§19).
2. A library built automatically from user-chosen folders and file types.
3. Automatic, accurate reading-time tracking and reading-state management.
4. User-defined categories with user-defined types (Series is a built-in type), automatic authors.
5. Ratings (half-stars), favorites, short reviews; category ratings computed with override.
6. Rich stats: per book, category, author, series, and overall; a yearly goal; a year-in-review.
7. Highlights, notes, bookmarks, TOC, in-book search.
8. Opt-in matching of local books to recognized editions (ISBN / work IDs) so history can be exported.
9. Export to Goodreads-format CSV (accepted by Goodreads, StoryGraph, Hardcover) and to a documented Deft JSON profile.
10. Local backup/restore as a zip.
11. Open source, GPL-3.0.

### Non-goals (v1)
- **The social app.** Profiles, follows, friends, book clubs, chat and voice messages will live in a **separate, future app**. Deft only provides a stable, documented export contract it can consume (§12). Do not build any social feature, server, or account into Deft.
- Adding books that don't exist as files (no manual "I read this paperback" entries; that was bookledger's model and is explicitly rejected).
- Cloud sync, accounts, multi-device sync.
- Opening DRM-protected files (see §6.5).
- Audiobooks, OPDS catalogs, store integrations, TTS, translation, AI features.
- Editing or writing to the user's book files — **Deft never modifies a book file**.
- Home-screen widgets, Wear OS, tablets-specific layouts beyond what Compose gives for free.
- iOS / desktop.
- Importing reading history from any other app (bookledger, Bookmory, Lithium, Goodreads, StoryGraph). Deft starts fresh; history begins with the first session read in Deft.
- The landing page (it will live in `docs/` for GitHub Pages later; not part of this build).

### Anti-goals (failure even if it "works")
- **Settings sprawl.** Lithium's appeal is restraint. If the reader grows a page of knobs, it failed. Every reader option must earn its place.
- **Inaccurate time.** If sessions count idle time, forgotten-open books, or double-count, the core promise is broken. Better to undercount than overcount.
- **Losing history.** Deleting, moving, or renaming a file must never destroy reading data.
- **Silent network.** Deft must never contact the network except when the user explicitly asks for edition matching or cover download.
- **Mandatory metadata chores.** The library must be useful with zero manual tagging; matching and categorizing are optional improvements.

## 3. Constraints

- **Platform:** Android, Kotlin, Jetpack Compose, Material 3 (custom-themed), `minSdk 26`, target the latest stable SDK.
- **Offline & accountless.** No analytics, no crash reporting that phones home, no ads.
- **License:** GPL-3.0-only for all Deft code. All dependencies must be GPL-3.0-compatible (Apache-2.0, BSD, MIT, LGPL, GPL-2.0-or-later are fine; GPL-2.0-only, AGPL-incompatible combos, and proprietary SDKs are not in the core build).
- **Storage access:** Storage Access Framework (SAF) persisted tree URIs. Do **not** request `MANAGE_EXTERNAL_STORAGE`.
- **Legal:** no DRM circumvention code, ever, in any branch.
- **Application ID / root package:** `com.kmab.deft` (permanent once released).
- **Git authorship:** commits are authored as `Samuel Worash Getaneh <samuelworash@gmail.com>`; repo is `github.com/kmab5/deft`.
- **Versioning:** tags `vA.B.C-D` — A major release, B feature release, C fix, D build (rebuild of the same A.B.C with only build/CI/packaging changes; resets to 1 when A, B or C changes; max 99). `versionName = "A.B.C"`, `versionCode = A*1_000_000 + B*10_000 + C*100 + D`. Pre-1.0: each milestone bumps B (M1 → `v0.1.0-1`); M10's public release is `v1.0.0-1`. Feedback revisions within a milestone increment C before handover, including pre-merge revisions; APK filenames are `deft-A.B.C.apk`. The merge preserves the latest tested version. Every merge to `main` is tagged; every tag publishes a GitHub Release. F-Droid submission at `v1.0.0`.
- **Distribution:** GitHub Releases (signed APK) and **F-Droid**. F-Droid requires every native dependency (PDFium, djvulibre, libmobi) to build from source reproducibly, no proprietary dependencies, no Google Play Services, no non-free network services enabled by default. Design the native build (NDK + CMake, sources as git submodules or fetched by pinned hash) for this from phase 1. Google Play is a possible later channel, not a v1 target.

## 4. Approaches considered

| | Approach | Trade-off | Verdict |
|---|---|---|---|
| **A** | **Readium Kotlin Toolkit for reflowable text + in-app converters** that turn MOBI/AZW/AZW3/FB2/TXT into EPUB; a custom fixed-layout viewer for PDF (PDFium), DjVu (djvulibre), CBZ (images). | We write and maintain converters (legacy MOBI is the hard one). In return every reflowable format gets identical theming, fonts, pagination, highlights and search. | **Chosen.** |
| B | MuPDF for most formats. | Less code up front, but EPUB renders as fixed pages with weak CSS/theme control (no Lithium-quality typography), AGPL, still missing DjVu/AZW3, highlights at a low level. | Rejected. |
| C | crengine (KOReader's engine) via JNI. | Great coverage and typography, but a large e-ink-oriented C++ codebase with its own rendering model; heavy to bridge into Compose. | Rejected. |

Refinement of A decided during design: Readium is used **only for reflowable content**. PDF, DjVu and CBZ go through one Deft-owned fixed-layout viewer backed by a `PageSource` interface, so all three behave identically (fit modes, zoom, inversion, page notes).

## 5. Architecture

### 5.1 Stack (decided)
- Kotlin, Coroutines/Flow, Jetpack Compose, Material 3, Navigation Compose.
- **Room** (SQLite) for all reading data; **DataStore (Proto or Preferences)** for settings.
- **Hilt** for DI.
- **WorkManager** for library scanning and conversion jobs.
- **Readium Kotlin Toolkit** (latest stable 3.x) — streamer, shared, navigator (EPUB), search, decorations.
- **PDFium** for PDF, via an Android binding the builder can **build from source in CI** (F-Droid requirement, see §3). Prebuilt-`.so` bindings are only acceptable if the same binaries can be reproduced from source in the F-Droid build recipe.
- **djvulibre** via NDK/JNI for DjVu (GPL-2.0-or-later → compatible).
- **libmobi** (LGPL-3.0, C) via JNI is the recommended base for MOBI/AZW/AZW3 → EPUB; a pure-Kotlin parser is acceptable if the builder finds libmobi impractical.
- `juniversalchardet` (or equivalent) for TXT encoding detection.
- OkHttp/Ktor client + kotlinx.serialization, used **only** by the matching module.
- Testing: JUnit5/Kotest for JVM tests, Robolectric where needed, Compose UI tests, Room migration tests.

### 5.2 Module layout (Gradle)
```
app/                      # Application, navigation graph, DI wiring
core/model/               # Pure Kotlin domain types (no Android)
core/database/            # Room entities, DAOs, migrations
core/datastore/           # Settings
core/common/              # Dispatchers, time, result types, hashing
core/designsystem/        # Theme, typography, components
feature/library/          # Books, Authors, Categories screens
feature/book/             # Book detail page
feature/reader/           # Reader shell: reflowable (Readium) + fixed-layout
feature/notes/            # Highlights/notes/bookmarks lists
feature/stats/            # Stats, goals, year in review
feature/settings/         # Settings screens
library/scanner/          # SAF walking, fingerprinting, metadata extraction
format/epub/              # Readium wiring
format/convert/           # Converter API + TXT, FB2 converters (Kotlin)
format/mobi/              # MOBI/AZW/AZW3 converter (JNI to libmobi)
format/pdf/               # PDFium PageSource
format/djvu/              # djvulibre PageSource (NDK)
format/cbz/               # Image PageSource
tracking/                 # Session tracker, state machine
data/matching/            # Edition matching providers (only networked module)
data/exchange/            # Backup zip, CSV export, Deft profile export
```
Builder may merge small modules; the boundaries that matter are: **domain types have no Android deps; only `data/matching` touches the network; format modules expose a common interface and know nothing about the database.**

### 5.3 Format interfaces
```kotlin
// Reflowable formats other than EPUB
interface BookConverter {
    val formats: Set<BookFormat>
    val version: Int                       // bump when output changes
    suspend fun convert(input: InputStream, out: File): ConversionResult // writes an EPUB 3 file
}

// Fixed-layout formats
interface PageSource : Closeable {
    val pageCount: Int
    suspend fun pageSize(index: Int): Size
    suspend fun render(index: Int, target: Bitmap, region: Rect, scale: Float)
    suspend fun tableOfContents(): List<TocEntry>   // empty if none
}

// Every format
interface MetadataExtractor {
    suspend fun extract(input: InputStream): ExtractedMetadata // title, authors, series+index, language, isbn, publisher, description, cover bytes, drm flag
}
```
Converted EPUBs are cached in app-private storage keyed by `(contentHash, converterVersion)`. The cache is evictable (size limit in settings, default 1 GB, LRU) and regenerated on demand. Conversion runs on first open with a progress indicator, or ahead of time via a WorkManager job when the book is added (setting: "Prepare books in background", default **on**).

**Locator stability:** reading positions for converted books are Readium `Locator`s against the converted EPUB. Always store `totalProgression` alongside; if the converter version changes and a locator can't be resolved, fall back to `totalProgression`.

## 6. Library

### 6.1 Sources
Settings → Library:
- **Folders:** list of SAF tree URIs, add/remove. Subfolders are included unless **excluded**: each folder has an optional list of excluded subfolders. Excluding a subfolder treats its books like removed files (marked missing, history kept); un-excluding reattaches them.
- **File types:** checkboxes for each of the 9 formats (all on by default).
- **Rescan** button; automatic rescan on app start (debounced, at most once per 6 h) and pull-to-refresh on the Books screen.

### 6.2 Book identity
- A **Book** is identified by the **SHA-256 of its file content**. Scanning first compares `(uri, size, lastModified)` to skip unchanged files, then hashes new/changed files.
- The same content found at several paths → **one Book with several BookFile rows**.
- Different files of the same work (an EPUB and a PDF of the same title) → **separate Books** in v1. Edition matching can link them to the same work ID; merging is a future feature.
- **A changed file is a different Book.** If a file is edited in place (e.g. Calibre writes metadata into it) or replaced by a corrected edition, its new content hash makes it a new Book with fresh stats; the old Book becomes missing and keeps its history. This is intentional (decided by the author): separate files have separate stats. No "is this the same book?" heuristics, no history transfer.
- A file that disappears → its BookFile is marked missing. A Book with no present files is **missing**: hidden from the Books screen by default (filter "Show missing"), but still counted everywhere in stats, history, categories and exports, shown with a "File missing" badge.
- A missing Book whose hash reappears (new path, restored backup) **reattaches automatically** with all history intact.
- **Ignore:** any book can be ignored ("Not a book": manuals, receipts, papers). An ignored book is hidden from every list and excluded from every stat, progress bar, export and series auto-fill; its history is kept. Un-ignore from Settings → Library → Ignored books restores everything. Ignore survives rescans (it's keyed to the Book, i.e. the content hash).
- **Forget** ("Remove from Deft and forget history") is the only permanent deletion, with confirmation.

### 6.3 Metadata
Extracted per format on scan: title, authors, series name + index, language, publisher, published date, description, ISBN(s), page count (fixed-layout) and cover image (saved as a thumbnail in app storage). Sources:
- EPUB: OPF metadata; series from `belongs-to-collection` (EPUB3) or `calibre:series`/`calibre:series_index`.
- MOBI/AZW/AZW3: EXTH records (title 503, author 100, ISBN 104, publisher 101, cover).
- FB2: `title-info` (`book-title`, `author`, `sequence name/number`, `coverpage`).
- PDF: Info dict / XMP. DjVu: metadata annotations if present. CBZ: `ComicInfo.xml` if present.
- TXT: filename heuristics (`Series - 01 - Title`, `Title (Series #1)`, `Author - Title`).

Users can **edit** title, authors, series and cover inside Deft. Edits are stored in the database as overrides (`*_override` columns or a separate table) — the original extracted values remain, and **files are never modified**.

### 6.4 Authors
- Created automatically from book metadata. Names normalized: "Sanderson, Brandon" → "Brandon Sanderson"; whitespace/case collapsed for matching.
- Users can **merge** authors (aliases) and rename. A book can have several authors.
- Author page: books, state breakdown, progress (finished/total), total time, average rating of the author's rated books.
- A co-authored book belongs fully to each of its authors: it appears on every author page, counts in each author's progress, and its reading time is credited **in full** to each author (never split).

### 6.5 DRM
- During metadata extraction, detect DRM: EPUB `META-INF/encryption.xml` with non-font-obfuscation algorithms or `rights.xml`/`license.lcpl`; MOBI/AZW encryption type ≠ 0; KFX containers; PDF encryption dictionary that blocks opening (password-protected PDFs: prompt for password — that's not DRM).
- DRM books appear in the library with a **"Protected"** badge. Tapping "Read" shows a short explanation ("This file is DRM-protected. Deft can't open it.").
- They can still be categorized, rated, favorited, matched and exported. They have no reading sessions.
- **Readium LCP** is a possible future, separately licensed build flavor. Not in v1. No other DRM will ever be supported.

## 7. Categories, types, and series

- A **Category** is a user-created named collection of books: name, optional description, optional cover (defaults to a collage of member covers), an ordering mode, and a set of **types**.
- A **CategoryType** is a user-created label (e.g. "TBR", "Book club", "Self-help"). One type, **Series**, is built-in and cannot be deleted. A category can have zero or more types; the Categories screen can filter/group by type.
- **Ordering:** every category supports manual ordering (drag to reorder) or a sort (title, author, date added, rating). A **Series**-typed category is always ordered by a **decimal position** per member (1, 2, 2.5, …), editable.
- **Names are not unique.** Categories are identified by ID; two categories may share a name.
- **No nesting in v1.** The schema carries a nullable `parentId` (FK to Category, `ON DELETE SET NULL`) from the first schema version that contains categories (M4), always null in v1, so nesting can be added later as UI + rules (SQLite recursive CTEs) without a data migration.
- **Auto-fill:** when scanning finds series metadata, Deft adds the book (at the given position) to a Series-typed category **that auto-fill itself created** (`autoCreated = true`) whose name matches the series name case-insensitively; if none exists it creates one. Auto-fill never attaches to a category the user created. If the user merges an auto-created series into one of their own categories, the series name is recorded as an alias on the target (`CategorySeriesAlias`) and auto-fill uses that category from then on. Auto-added memberships are flagged `suggested` until the user edits or confirms them; the category shows a small "review" indicator while any member is suggested. Users can disable auto-fill in settings.
- Book page shows **"Next in series"** (the lowest-positioned member after this one that is not finished).
- **Category progress:** finished members ÷ total members (missing books included, ignored books excluded). **Abandoned books are unresolved:** they stay in the denominator, so a category containing an abandoned book never shows 100%. The bar shows finished, reading/paused, and abandoned as distinct segments. The same rule applies to author and series progress.
- Users can merge two categories (members are unioned; on position conflicts the target's positions win).
- **Category multi-select:** long press selects by ID, then taps toggle; select-all includes visible categories once even when grouped under multiple types. Back clears selection after closing any modal. Search/type filters prune hidden selections; rotation and display/sort changes preserve visible selections. Bulk actions add/remove chosen types without replacing other types, merge into an explicitly chosen destination (which may be selected), or delete categories without deleting books/history. Each action is atomic, rejects stale IDs, and clears selection only after success. Merge keeps destination details/positions, with remaining conflicts resolved by ascending source ID. Removing Series clears its aliases/auto-created status, following single-category editing.
- **Category rating:** by default the average of its rated books, shown with one decimal. The user can **override** with their own rating; a reset returns to the computed average. UI shows which one is in effect.

## 8. Reading states, read-throughs and sessions

### 8.1 States
`NOT_STARTED` (displayed "Not read"), `READING`, `FINISHED`, `PAUSED`, `ABANDONED`.

### 8.2 Read-throughs
A **ReadThrough** is one attempt at reading a book: `startedAt`, `endedAt?`, `outcome` (`IN_PROGRESS`, `FINISHED`, `PAUSED`, `ABANDONED`), and its sessions. A book's state is derived from its latest read-through (`NOT_STARTED` if none). This gives re-reads, read counts, and per-month finish stats for free.

### 8.3 Transitions
| From | Event | Result |
|---|---|---|
| Not read | first session recorded | new read-through, `READING` |
| Reading | reaches end (last page / ≥ 99% progression) | prompt "Mark as finished?" (Yes → `FINISHED`, sets `endedAt`; also offers rating) — prompt only once per read-through |
| Reading | user marks Paused / Abandoned / Finished | as chosen |
| Paused | opened & session recorded | resume same read-through → `READING` |
| Abandoned | opened | prompt: "Resume" (same read-through → `READING`) or "Start over" (new read-through) |
| Finished | opened | prompt: "Re-read" (new read-through, position reset to start) or "Just browsing" (no state change; sessions are recorded with `browsing = true` and appear in the book's own session list, but are **excluded** from daily reading time, the heatmap, streaks, goals and all aggregate stats) |
| any | user sets state manually on book page | allowed; manual finish asks for finish date (default now) |

Users can edit read-through dates and delete read-throughs from the book page.

### 8.4 Session tracking (the core promise)
- A session is active while the reader screen is **resumed and visible** and the screen is on.
- **Idle pause:** if there is no page turn, scroll, or selection for the idle timeout, the session stops accruing; the next interaction resumes it. Idle time is never counted. Two user-adjustable timeouts (any whole number of minutes 1–60, plus **Off**): reflowable default **5 min**, fixed-layout default **10 min**.
- **Idle timeout Off safety net:** when the applicable timeout is Off, "keep screen on" is released after **15 min** without interaction, so the screen sleeps normally and the session ends on screen-off. Nothing is counted after the screen turns off. *Exception:* fixed-layout pages with a zoom/pan in the last idle window count as interaction.
- Session **ends** when the reader is left, the app goes to background, or the screen turns off.
- Sessions shorter than the chosen minimum are discarded: default **30 seconds**, adjustable from **1–300 seconds** in Settings → Tracking. Each session captures its minimum when it starts; changing the preference affects new sessions, never accepted history. Legacy sessions retain their 60-second minimum.
- Each session stores: book, read-through, `startedAt`, `endedAt`, `activeSeconds`, `startLocator`, `endLocator`, `startProgression`, `endProgression`, and `pagesTurned` (count of page-turn/scroll-step events).
- **Pages read** for stats: fixed-layout → distinct page delta; reflowable → delta in Readium *positions* (≈1024 chars each), labelled "pages" in UI.
- Sessions checkpoint confirmed reading every ~30 s. With a finite idle timeout, crash recovery also discards unconfirmed current-page dwell; the 30-second loss bound applies to confirmed reading. A clean exit before timeout includes the current dwell. See the M3 decision in §20.
- A quiet in-reader indicator is optional (setting, default off).

## 9. Ratings, favorites, reviews
- **Rating:** 0.5–5.0 in half-star steps, optional, per book (not per read-through in v1).
- **Favorite:** boolean heart.
- **Review:** optional plain text, per book. Private by default (it's an offline app); flagged as exportable in the profile export only if the user chooses.
- Authors get a computed average, no manual rating. Categories: §7.

## 10. The reader

### 10.1 Reflowable (EPUB and converted formats, via Readium)
- **Layout:** paged (tap zones left/right/center, swipes; page-turn animation: none/slide) or continuous scroll.
- **Typography:** font family (a small bundled set — e.g. a serif, a sans, a dyslexia-friendly font, "Publisher default" — plus user-imported TTF/OTF copied into app storage), **font size**, **line spacing**, **margins**, **text alignment** (start/justified), hyphenation (on/off).
- **Themes:** Light, Sepia, Dark, Black (AMOLED), plus user-created custom themes (background, text, link/accent colors). Option: follow system day/night (pick a light theme and a dark theme).
- **Brightness:** a slider in the reader quick panel (window brightness, not system), with "use system brightness" toggle; default in settings. **No brightness gesture.**
- **Auto-scroll (M6, after speed metrics):** optional reflowable scrolling at measured reading speed or a custom interval. Explicit start/pause; touch, selection, obscuring overlays and background pause it. Automated motion must not count as a human reading interaction, keep idle sessions alive or train speed estimates. With insufficient manual reading data, use a custom interval.
- **Other:** keep screen on (default on while reading), volume keys turn pages (default off), status bar line (clock, battery, chapter progress / book %, toggleable), full-screen/immersive.
- **Navigation:** TOC, bookmarks list, progress slider with chapter ticks, "go back" after a jump.
- **Selection:** highlight in 4 colors, add note, copy, share text, **define** (hands off via `ACTION_PROCESS_TEXT`/`ACTION_DEFINE` to an installed dictionary app), search in book.
- **Search:** Readium search service; results list with context.
- Reader settings are **global** (apply to all books). Exception: fixed-layout settings are per-book (fit/zoom/inversion).

### 10.2 Fixed-layout (PDF, DjVu, CBZ — Deft's own viewer)
- Page-by-page (horizontal) or continuous vertical scroll.
- Fit width / fit page / fit height; pinch zoom; double-tap zoom; crop margins (auto-detect, PDF/DjVu).
- Dark mode by plain color inversion.
- Right-to-left reading toggle for CBZ (manga).
- TOC (PDF outline, DjVu bookmarks; none for CBZ), bookmarks, **page notes** (a note attached to a page; no text highlighting in v1).
- Password-protected PDFs prompt for a password (not stored).

### 10.3 Reader quick panel
One tap in the center opens: top bar (back, title, TOC, bookmark toggle, search, more) and bottom bar (progress slider, Aa typography sheet, theme, brightness). Kept minimal — this is the place settings sprawl would show up first.

## 11. Notes, bookmarks, highlights
- **Bookmark:** locator + auto-generated label (chapter + snippet), optional custom name.
- **Highlight:** locator range, color, selected text, optional note, created/updated timestamps. M5 offers four colors plus a colorless/no-highlight choice. Selecting text alone does not persist a highlight; colorless removes its visible decoration while retaining any explicitly saved note.
- **Page note** (fixed-layout): page index + note.
- **Notes screen** (global): every highlight/note/bookmark across books, filter by book/color/type, search, tap to jump into the book.
- Per-book notes tab on the book page. Export a book's notes as Markdown (share sheet).
- Notes survive file deletion like all other data.

## 12. Data exchange

**Delivery: M7**, after notes/highlights (M5) and stats/goals (M6). Matching and exports consume library metadata and stored reading data; backup operates on the database, settings and app-owned assets. Neither fixed-layout rendering (M8) nor conversion (M9) is a prerequisite. Scanned books can be matched/exported even when their reader is not yet implemented. When M8/M9 add page notes, preferences or other persisted fields, they must extend export/backup coverage and test restoration of older M7 backups. Rebuildable conversion caches are excluded from backups.

### 12.1 Edition matching (opt-in, the only networked feature)
- Purpose: attach a recognized identity (ISBN-10/13, Open Library work/edition IDs, Google Books ID, Hardcover ID) to a local book so exports are importable elsewhere.
- Entry points: book page "Match edition", bulk "Match selected" in the library, and Settings → Matching (enable providers; Hardcover needs a user-supplied API token).
- Providers, in order: **Open Library** (no key) → **Google Books** (no key needed at low volume) → **Hardcover** (if token). Query by ISBN from file metadata when present; otherwise title + first author.
- UI shows candidates (cover, title, author, year, ISBN); user confirms one or "No match". Bulk mode auto-accepts only exact ISBN hits and queues the rest for review.
- Matching stores the identifiers and canonical title/authors in a separate `EditionMatch` record. It **does not overwrite** local metadata unless the user taps "Use matched metadata" (which writes overrides, never the file). Cover download is a separate explicit action.
- No network call happens anywhere else in the app. Make this testable (single HTTP client provided only to `data/matching`).

### 12.2 Export: Goodreads-format CSV
- One CSV that Goodreads, StoryGraph and Hardcover all import. Columns follow the Goodreads library export header exactly (`Book Id, Title, Author, Author l-f, Additional Authors, ISBN, ISBN13, My Rating, Average Rating, Publisher, Binding, Number of Pages, Year Published, Original Publication Year, Date Read, Date Added, Bookshelves, Bookshelves with positions, Exclusive Shelf, My Review, Spoiler, Private Notes, Read Count, Owned Copies`). Builder verifies against a current Goodreads export and each target's import docs.
- Mapping: `FINISHED` → `read`; `READING`/`PAUSED` → `currently-reading`; `NOT_STARTED` → `to-read`; `ABANDONED` → shelf `did-not-finish` (StoryGraph/Hardcover recognize DNF shelves — verify). Categories → `Bookshelves` (slugified). Rating → rounded to nearest whole star for Goodreads compatibility (half-up), with the exact value kept in the JSON export. Date Read = latest finished read-through. Read Count = finished read-throughs.
- Options: include only matched books (default) or all; include unmatched books by title/author with a warning count; include reviews (default off).

### 12.3 Export: Deft Reading Profile (JSON) — contract for the future social app
- A versioned, documented JSON file (`deft-profile.json`, `"schemaVersion": 1`) containing: app version, export time, books (local ID, title, authors, edition match identifiers, format, state, rating, favorite, review if opted-in, read-throughs with dates, total active time), categories (name, types, members with positions, rating/override), authors, and aggregate stats. **Excludes** file paths, notes/highlights (opt-in toggle), and sessions' locators.
- Schema is committed as `plans/deft-profile.schema.json` (JSON Schema) during implementation, with a sample.
- Delivered via the Android share sheet (MIME `application/json`, filename `deft-profile-YYYY-MM-DD.json`). Deft only *sends*; the future social app will *receive*. A permission-gated `ContentProvider` for one-tap import is a **future** item; leave a seam (`ProfileExporter` producing the model) so it can be added without refactoring.

### 12.4 Backup & restore (local zip)
- `deft-backup-YYYY-MM-DD.zip`: `manifest.json` (schemaVersion, app version, DB version, created at, list of library folder display paths), consistent SQLite copy (checkpoint WAL first, or Room-safe copy), settings, imported fonts, custom covers/thumbnails. **Never book files.**
- Restore: replaces all Deft data after confirmation; runs migrations if the backup's DB version is older; refuses newer. Afterwards the user re-grants folders (SAF permissions don't survive reinstall; the manifest's folder list is shown as a checklist), rescans, and books reattach by hash.
- Optional automatic backup: weekly into a user-chosen SAF folder, keeping the last N (default 4). Default off.

## 13. Stats

All stats are computed from sessions and read-throughs; nothing is stored redundantly except optional cached aggregates.

- **Per book:** total active time, sessions count, average session length, average speed (pages/hour), estimated time left (remaining pages ÷ the user's speed on this book, falling back to overall speed), started/finished dates per read-through, read count.
- **Per category / author / series:** finished/total progress bar, time spent, average rating, state breakdown.
- **Overall (with range selector: week / month / year / all time / custom):**
  - books finished per period (bar chart);
  - reading time per day (calendar heatmap);
  - current & longest streak (a day counts if ≥ 1 session; streak threshold configurable, default 1 min);
  - time-of-day and day-of-week distribution;
  - format distribution, book length distribution;
  - rating distribution;
  - abandoned rate (abandoned / (finished + abandoned));
  - pages read, average speed.
- **Goals:** yearly books-finished goal with progress & pace ("3 ahead of schedule"); optional daily minutes goal.
- **Year in review:** a scrollable summary screen per calendar year: totals, top-rated, longest book, most-read author, busiest month, favorite time of day.
- "This month" style counts use the **device time zone at the time of the session** (store session timestamps as UTC instant + zone offset).

## 14. Screens and navigation

Bottom navigation (5): **Books · Categories · Authors · Notes · Stats**. Settings from the top-bar overflow on every top-level screen. "Continue reading" card pinned at the top of Books (last opened book, one tap to resume).

```
Books
┌──────────────────────────────┐
│ Deft            🔍  ⇅  ⋮     │
│ ┌──────────────────────────┐ │
│ │ ▶ Continue: Mistborn 2   │ │
│ │   43% · 2h 10m left      │ │
│ └──────────────────────────┘ │
│ [All][Reading][Not read][…]  │  ← state chips
│ ┌───┐ ┌───┐ ┌───┐            │
│ │cov│ │cov│ │cov│  grid/list │
│ └───┘ └───┘ └───┘            │
├──────────────────────────────┤
│ Books Categories Authors Notes Stats │
└──────────────────────────────┘
```
- **Books:** grid/list, sort (title, author, recently read, date added, rating, progress, file location), filter (state, format, favorite, rating, missing, protected, matched). Multi-select → add to category, set state, match editions.
- **Book page:** cover, title/author (tap author → author page), state control, rating/favorite, progress, "Read"/"Continue", categories (with positions), next in series, read-throughs, stats, notes tab, edition match, file info (paths, format, size, missing/protected).
- **Categories:** list/grid grouped or filtered by type; each with progress bar and rating. Category page: ordered members, progress, stats, rating override, edit types, merge.
- **Authors:** list with progress and counts; author page.
- **Notes:** §11. **Stats:** §13. **Reader:** §10.
- **Settings:** Library (folders + excluded subfolders, file types, rescan, series auto-fill, ignored books), Reader (defaults for §10), Tracking (idle timeouts reflowable/fixed incl. Off, min session, streak threshold), Goals, Appearance (app theme light/dark/system, accent), Matching (providers, Hardcover token), Backup & export, Storage (conversion cache size, clear), About (version, license, source link, third-party licenses).

Visual design: **`plans/2026-11-13-deft-visual-design.md`** (colours, type, components, every screen, motion, celebrations, copy). The sketches above are layout only; the visual doc wins on look and copy.

## 15. Data model (Room, logical)

```
Book(id, contentHash UNIQUE, format, ignored, title, sortTitle, titleOverride?, language, publisher, publishedDate,
     description, isbn10?, isbn13?, pageCount?, coverPath?, coverOverridePath?, drmProtected,
     addedAt, lastOpenedAt?, favorite, rating?, review?, currentLocator?, currentProgression,
     readerPrefsJson? /*fixed-layout per-book*/, convertedVersion?)
BookFile(id, bookId, uri, displayPath, size, lastModified, missing, lastSeenAt)
Author(id, name, sortName, normalizedName UNIQUE)
AuthorAlias(aliasNormalizedName UNIQUE, authorId)
BookAuthor(bookId, authorId, position, overridden)
CategoryType(id, name UNIQUE, builtIn /*Series*/)
Category(id, name /*not unique*/, description?, parentId? /*FK ON DELETE SET NULL; always null in v1*/, autoCreated, coverPath?, orderMode, sortField?, ratingOverride?, createdAt)
CategorySeriesAlias(normalizedSeriesName UNIQUE, categoryId)
CategoryTypeLink(categoryId, typeId)
CategoryMember(categoryId, bookId, position REAL, suggested, addedAt)
ReadThrough(id, bookId, startedAt, endedAt?, outcome, finishPromptShown)
ReadingSession(id, bookId, readThroughId, browsing, startedAtUtc, endedAtUtc, zoneOffsetMinutes, activeSeconds,
     startLocator?, endLocator?, startProgression, endProgression, startPosition?, endPosition?, pagesTurned)
Bookmark(id, bookId, locator, label, createdAt)
Highlight(id, bookId, locator, text, color, note?, createdAt, updatedAt)
PageNote(id, bookId, pageIndex, note, createdAt, updatedAt)
EditionMatch(bookId PK, provider, isbn10?, isbn13?, openLibraryWorkId?, openLibraryEditionId?,
     googleBooksId?, hardcoverId?, matchedTitle, matchedAuthors, publishedYear?, pages?, confirmedAt)
Goal(year, booksTarget?, dailyMinutesTarget?)
LibraryFolder(treeUri UNIQUE, displayName, addedAt)
ExcludedFolder(treeUri, documentId /*SAF document id of the subfolder*/, displayPath, PRIMARY KEY(treeUri, documentId))
```
Rules: deleting a `BookFile` never cascades to `Book`. A `Book` is only deleted by explicit user action ("Remove from Deft and forget history"), with confirmation. Room schema export is on; every schema change ships a migration and a migration test.

## 16. Quality bar
- **Tests:** unit tests for every converter and metadata extractor against fixture files in `testdata/` (public-domain books, e.g. Project Gutenberg, Standard Ebooks; small synthetic files for edge cases); session tracker and state machine with a fake clock; stats calculators with fixed datasets; CSV/JSON exporters against golden files; Room migration tests; Compose UI tests for library and reader shell flows.
- **Performance targets:** library of 5 000 books scrolls at 60 fps on a mid-range device; cold start to Books < 1.5 s; opening an already-converted book < 1 s; initial scan of 1 000 files completes in the background without blocking UI.
- **Accessibility:** TalkBack labels, 48 dp touch targets, dynamic font scaling in app chrome, sufficient contrast in all built-in themes.
- **CI:** GitHub Actions — build, ktlint/detekt, unit tests, assembleRelease on every push to any branch; every version tag builds a signed APK and publishes a GitHub Release (pre-release while major = 0). Signing key from repository secrets. See §3 for versioning.
- **Privacy check:** a test asserting no module other than `data/matching` depends on the HTTP client; `INTERNET` permission is required only for matching (document this in the README).

## 17. Implementation order (milestones — acceptance criteria in the roadmap)
1. **M1 — Foundation:** repo, build tooling, design system, database, settings, SAF folders, scanner and Books screen. Complete.
2. **M2 — EPUB reader:** settings, TOC, bookmarks, progress and reader/library search. Complete.
3. **M3 — Tracking:** sessions, read-throughs, states, ratings, favorites and finish flow. Complete.
4. **M4 — Organization:** categories, types, series auto-fill and authors. Complete.
5. **M5 — Highlights, notes and search:** selection actions, persistent notes/highlights, Notes screen and Markdown export.
6. **M6 — Stats:** goals, year in review, personal bests and opt-in auto-scroll after speed metrics.
7. **M7 — Data exchange:** edition matching, CSV export, Reading Profile JSON and local backup/restore.
8. **M8 — Fixed-layout viewer:** PDF, CBZ, then DjVu; extend backup/export coverage for its persisted data.
9. **M9 — Converters:** TXT, FB2, then MOBI/AZW/AZW3; extend backup/export coverage while excluding rebuildable caches.
10. **M10 — First public release:** polish, accessibility, performance and F-Droid preparation. Signed GitHub Actions release publication is already implemented in `v0.4.3-2` and remains a regression check.

## 18. Left to the builder
- Exact libraries where the doc names candidates (PDFium binding, libmobi vs pure Kotlin, charset detector, chart library or hand-drawn Compose charts).
- TXT chapter-detection heuristics.
- Cache eviction details, WorkManager constraints.
- Exact wording of prompts and empty states.

## 19. Future (explicitly not v1)
- Separate social app (profiles, follows, current read, book clubs with text + short voice chat) consuming the Deft profile.
- ContentProvider for one-tap profile import.
- Readium LCP build flavor.
- Importing history from Goodreads/StoryGraph CSV onto matched local books.
- RTF and LIT support (LIT needs a clean-room LZX/LIT parser; existing references contain DRM-removal code that must not be reused).
- Merging different-format files of the same work into one Book.
- Per-read-through ratings, TTS, widgets, landing page in `docs/`.

## 20. Decision log
Record every decision made or changed after approval: date, decision, reason.

Historical rows retain the milestone numbers used when the decision was made. On 2026-11-22, data exchange moved from M9 to M7, fixed-layout readers from M7 to M8, and converters from M8 to M9; §17 and the roadmap use the current numbering.

| Date | Decision | Reason |
|---|---|---|
| 2026-11-13 | Book identity is pure content hash; a modified file is a new Book with separate stats. | Author: separate files have separate stats. |
| 2026-11-13 | No import of history from other apps (bookledger, Bookmory, Lithium, Goodreads). | Author: Deft is a fresh, separate app. |
| 2026-11-13 | RTF and LIT dropped from v1. | Author: not needed; LIT has no clean, DRM-free reference implementation. |
| 2026-11-13 | Distribution: GitHub Releases + F-Droid. | Author. Requires reproducible from-source native builds. |
| 2026-11-13 | Planning docs live in `plans/`, not `docs/`. | `docs/` is reserved for the GitHub Pages landing page. |
| 2026-11-13 | "Ignore book" (hidden, excluded from stats, history kept, reversible) and excluded subfolders (books marked missing, reattach on un-exclude). | Grilling: scanned folders contain non-books. |
| 2026-11-13 | Abandoned books are unresolved in category/author/series progress; never 100% with a DNF. | Author. |
| 2026-11-13 | Co-authored books credit full time to each author. | Grilling. |
| 2026-11-13 | Idle timeouts adjustable (1–60 min or Off): reflowable 5, fixed-layout 10; Off releases keep-screen-on after 15 min idle. | Author: reading speeds differ. |
| 2026-11-13 | "Just browsing" sessions excluded from daily time, heatmap, streaks, goals, aggregates. | Author. |
| 2026-11-13 | No category nesting in v1; nullable `parentId` (ON DELETE SET NULL) present from the categories table's first version. Category names not unique. Series auto-fill only uses auto-created Series categories (+ merge aliases). | Author. |
| 2026-11-13 | App ID `com.kmab.deft`; tags `vA.B.C-D`, versionCode `A·10⁶+B·10⁴+C·10²+D`; planning commit `v0.0.0-1`. | Author. |
| 2026-11-13 | No PRs: branch per milestone/fix, local signed release APK + test checklist handover, merge commit to `main` after author confirms, then tag. | Author: single device/repo. |
| 2026-11-13 | Visual design agreed: kmab-derived structure (ink neutrals, Space Grotesk/Mono, square-capped Lucide, colour = meaning) + Deft's own paper light mode, Literata, and book mark; purple = accent and "reading"; Material You off; lowercase UI copy. Full spec in `plans/2026-11-13-deft-visual-design.md`. | Design grilling with the author. |
| 2026-11-13 | Celebrations (full-screen, smooth, skippable; toggle "celebrations", default on) for book finished, goal, series, streak milestones, personal bests. | Author: "a bit more hurrah", but switchable. |
| 2026-11-13 | Visual feedback is gathered at each milestone handover, not at separate checkpoints; the visual design is a living doc. | Author. |
| 2026-11-14 | Visual feedback round 1: mark (b) reworked (thicker, softened corners, 1:1.2, ribbon on top); accent one step lighter (`#C373FF` ink / `#B24BFF` paper); app is sans only (Literata moves to the reader); reader adds original/publisher font, PT Serif, Bitter, Lato, Open Sans (budget 3 MB); smaller type scale; calmer books screen with one filter & sort sheet; all reader settings behind one button; new full-screen, self-ending finish (~2.8 s). | Author feedback on the preview. |
| 2026-11-14 | Final mark: closed book (b), off-white strokes (stroke 11) and purple ribbon on ink. Literata stays the reader default. OpenDyslexic ships in the app. Settings → reader exposes every in-book reader setting plus app-wide ones (fonts, custom themes, fixed-layout defaults); both edit the same global values. Design phase closed. | Author feedback round 2. |
| 2026-11-15 | Generate one persistent release signing key in M1, keep it and credentials outside Git, and reuse it for every release. Enable R8 minification and resource shrinking. Hand over signed release APKs for device testing. | Author: install updates without uninstalling or losing data; test the release configuration. |
| 2026-11-15 | M1 follows the final visual design for the filter-and-sort sheet, lowercase copy, final mark, reusable components and screenshot checks. | Reconcile the earlier task plan with approved visual feedback and roadmap scope. |
| 2026-11-15 | Reject overlapping selected library roots with an explanation that subfolders are already included. Persist folder-removal reconciliation intent; incomplete scans never mark unseen files missing. | One unambiguous root per BookFile prevents false missing states, and cancellation must not lose removal requests. |
| 2026-11-15 | Paper enabled semantic text uses accent `#933AD3`, finished `#207549`, paused `#7E652A`, error `#BA3C36`; paused bars use `#A78636`. Original kmab scales remain unchanged. | Contrast audit: original paused bar on bg is 2.913:1; original semantic text on surface2 is below 4.5:1. Adjusted enabled text is at least 4.574:1 on all three surfaces, state bars at least 3.049:1 on bg. |
| 2026-11-15 | Foundation pins AGP 8.13.2, Kotlin 2.2.21, Gradle 8.13 and Android API 36, using the installed JDK 17/toolchain. | Keep the approved JDK 17 CI contract and installed SDK/Gradle; latest compatible pinned dependencies are resolved without installing toolchains. |
| 2026-11-15 | Use Compose BOM 2025.10.00 for the foundation, with explicitly registered Kotlin JVM convention plugins instead of precompiled Kotlin DSL scripts. | Reuse the author's installed Gradle 8.13/JDK 17/SDK 36 and cached compatible Compose artifacts. Avoid an unnecessary second Kotlin compiler download for build logic. This does not assert that newer versions are incompatible. |
| 2026-11-15 | Run Robolectric tests on API 36 with JDK 21; Gradle and production JVM toolchains stay on 17. CI makes both JDKs available. | The author already has JDK 21 and the API 36 Robolectric runtime cached. API 36 Robolectric requires Java 21; this tests the real target and avoids downloading another large SDK runtime. |
| 2026-11-15 | Paper finished/paused meaningful fills use `#29965C` / `#9F8033`, with contrast tests covering bg, surface and surface2. | Detail progress tracks and chip dots also appear on surface2. The original finished fill is only 2.841:1 there; revised meaningful fills are at least 3.087:1 on all three surfaces. |
| 2026-11-15 | Disable platform cloud backup and automatic device transfer explicitly, including Android 12+ extraction rules. | Deft backup/restore is the explicit local ZIP workflow; private reading data must not enter an account-backed platform backup. |
| 2026-11-15 | UI font subsets retain all available glyphs in the requested Latin, Latin Extended, Greek and Cyrillic ranges; unavailable glyphs use Android system fallback. | Upstream Space Grotesk and Space Mono do not contain a full Greek/Cyrillic alphabet. Preserve the approved faces without inventing glyphs or blocking metadata in other scripts. Reader fonts remain an M2 task. |
| 2026-11-15 | Domain sort keys use locale-independent lowercase. Author matching uses NFKD, removes combining marks, and preserves Unicode letters/numbers; sort names move the last whitespace-delimited word first. | Stable matching and ordering across device locales, without dropping non-Latin author names. Compound family names remain intact in display names; the simple sort-name heuristic can be corrected by later author edits. |
| 2026-11-15 | Keep common utilities JVM-only; use Hilt core annotations and KSP-generated module metadata for dispatcher bindings. | Hashing and time remain usable in plain JVM tests without bringing Android into common. |
| 2026-11-15 | Library list queries treat an empty format filter as all formats, sort date added newest first, and break ties by title then Book ID. Reconciliation marks selected roots missing and restores seen URIs in one transaction, in batches of 900 URIs. | Explicit stable DAO semantics for the scanner and library; batches stay below SQLite variable limits without independent NOT IN updates. |
| 2026-11-15 | M1 EPUB metadata uses a private temporary ZIP spool capped at 512 MiB, XML entries at 4 MiB and covers at 20 MiB; the spool is removed on success and failure. kXML2 tokenizes XML with DTDs and custom entities rejected. | Entry order must not affect metadata, memory/disk use must be bounded, and metadata parsing must work on Android without external XML resolution. Oversized or unsafe books fall back through MetadataException. |
| 2026-11-15 | Registry falls back only for MetadataException/IOException; cancellation, fatal errors and programming failures propagate. Duplicate extractors for a format are rejected. Filename fills missing title/authors; absent embedded series name selects the complete filename series pair, ignoring an orphan embedded index. An embedded series name keeps its index, filling a missing index only when filename series matches after case/whitespace normalization. | Task 6: preserve trustworthy series pairs, deterministic dispatch and caller cancellation; expected malformed-book/read errors remain reportable. |
| 2026-11-15 | Scanner skips hashing only when size is nonnegative and lastModified is positive and both match the stored URI fingerprint. Unknown provider values require hashing. Move/restore reports are classified after all complete-root reconciliations; selected roots override stale removal requests. DocumentTree exposes provider relationship checks for same-document/descendant root rejection before writes. EPUB temporary ZIP creation explicitly selects the current java.io.tmpdir. | Avoid stale identity with unknown provider stamps, root-order-dependent move counts, and false missing on folder re-adds. Relationship failures cannot trigger reconciliation. Android supplies its private cache as java.io.tmpdir; explicit selection avoids JVM-cached directory ambiguity. |
| 2026-11-15 | A file indexing failure preserves its prior presence: present rows remain present, missing rows remain missing. Only successful identity verification can restore a missing row. | Root reconciliation must not attach unverified replacement bytes to the old content-hash Book merely because its former URI exists. |
| 2026-11-15 | Folder removal deletes the selected folder and records a Room outbox entry in one transaction (schema v2). A singleton lock serializes folder edits with scan snapshots and outbox acknowledgement. Intent survives WorkManager replacement/cancellation and is acknowledged only after reconciliation; selected roots win on re-add. Startup schedules pending removals even within the six-hour debounce. | Close crash windows between folder deletion and durable intent, and prevent an older worker from consuming a newer removal or releasing a re-added folder grant. |
| 2026-11-15 | Only LibraryFolderRepository schedules folder removals; ScanScheduler.scanNow() has no removedRoots argument. WorkManager input contains no removal intent. SAF persists READ grants only, validates overlaps in both directions with provider document relationships, and rolls back newly acquired grants after failed additions. | Keep replaceable work requests independent of permanent removal intent and preserve existing selected-folder access. |
| 2026-11-15 | Automatic scans use unique KEEP work, manual scans use REPLACE, with no charging/network constraint. A completed scanner report records lastScanAt, including reports with unavailable roots or per-file errors; cancellation and thrown failures do not. Covers use application filesDir. Remove only WorkManager's AndroidX Startup initializer and provide its Hilt worker factory through the Application configuration. | Scanning is offline and explicitly requested or debounced at startup; completed attempts remain observable without retry loops for individual unreadable books, and private derived covers never touch source folders. |
| 2026-11-15 | EPUB tokenization uses the public XMLPull factory/interface: Android supplies its platform parser, while pure-JVM tests use kXML2 as a test-runtime dependency. XMLPull is compile-only; namespace processing and explicit DTD/unresolved-entity rejection remain. | Both published kXML2 artifacts bundle Android XMLPull API classes, causing R8 library/program conflicts after scanner integration. Keep platform APIs out of app dependencies instead of transforming vendor jars or suppressing R8 errors. |
| 2026-11-15 | Books format filters affect the displayed library and survive navigation-state restoration; they do not change the file types enabled for scanning. M1 omits future tabs, search, reader actions and fabricated reading progress. Covers load only from app-private files through Coil without network components. | Keep filtering distinct from source discovery and expose only working M1 features. |
| 2026-11-15 | Check folder accessibility before ancestry validation, retaining unavailable roots without blocking healthy ones. Decode cover bounds first and sample pixels to a maximum 1,200-pixel edge before making the 600-pixel thumbnail. Persist the exclusion picker operation across activity recreation. | Final M1 audit: preserve independent scans, bound bitmap memory, and deliver picker results to the intended operation. |
| 2026-11-15 | Finish the remaining M1 implementation as a batch, then run combined verification and one whole-milestone audit instead of separate review gates for each task. | Author requested a faster workflow during implementation. Failed checks still require fixes and verification before handover. |
| 2026-11-18 | M1 device feedback replaces the sparse settings and oversized combined sheet with visible folder actions, focused filters/sort/display sheets, three independently persisted grid/list sizes, two-line titles, and a prominent detailed scan panel. Add launcher mark padding and storage breakdown. | Author tested on Samsung S25 FE / Android 16 and found controls undiscoverable and presentation unpolished. |
| 2026-11-18 | Add reverse sorting and nullable lastReadAt in Room schema 3 with migration; last-read sorting keeps unread Books last in both directions and breaks ties by title/ID. Never infer reading from imports or lastOpenedAt. | Author requested last-read sorting before the reader milestone; preserve truthful history. |
| 2026-11-18 | User-started scans request a dataSync foreground worker without an artificial time slice; automatic scans and refused foreground starts use four-minute batches with at most six attempts and an actionable recovery message. Continuations reuse committed fingerprints; unchanged files avoid redundant writes. Report checked/skipped/duplicate/fallback/unreadable outcomes separately. | Device import stopped short; remove demonstrated database churn and make interruption/resumption explicit without falsely marking unseen files missing. |
| 2026-11-18 | Pre-merge M1 test APKs retain the pending milestone version 0.1.0-1 and persistent certificate; source commit and APK hash distinguish handovers. | Android accepts same-version replacement updates, preserving the planned first milestone tag and avoiding a later version-code downgrade. No released main tag is rebuilt or replaced. |
| 2026-11-18 | Unchanged BookFile rows retain lastSeenAt until reindex/reattachment; presence is determined by complete traversal reconciliation, not that timestamp. Notification permission is optional and never gates scanning. | Avoid thousands of unnecessary writes and list invalidations; user confirmed scanning stopped after leaving the app. |
| 2026-11-18 | Named scan diagnostics retain the latest three reports in app-private storage, capped at 10,000 records and 8 MiB per report, with explicit omitted counts. Checkpoint totals and names through same-ID continuation/cancellation; open details observe completed writes. | Author requested actionable added/skipped/error details; keep WorkManager output bounded and preserve truthful interrupted-scan results. |
| 2026-11-18 | Second device review restores a flat icon-led Books toolbar, compact checkbox/radio control popups and adjacent display controls. Completed scans and errors use animated bottom notices, roughly six seconds with accessibility extension and interaction pause; historical outcomes do not replay. A separate scan-details popup lists named added/skipped/error records in bounded private storage. Animate layout, selection, popup and folder changes while respecting system motion scaling. | Author rejected the first revision's oversized controls, whitespace and permanent scan rows. |
| 2026-11-18 | Settings opens Library directly while it is the only implemented section; restore the category chooser when a second real section arrives. | Author's navigation clarification; avoid an unnecessary one-option screen in M1. |
| 2026-11-18 | Supersedes the earlier same-version pre-merge decision: feedback edits increment C. This handover is 0.1.1 (internal 0.1.1-1, code 10101), named deft-0.1.1.apk. The eventual M1 merge keeps the latest tested version and persistent certificate. | Explicit author correction: use patch versioning and simple APK names. |
| 2026-11-18 | Implement M2 on a branch stacked on pending M1 feedback so the author tests both together. Version 0.2.0-1/code 20001, APK deft-0.2.0.apk. Run combined local verification and one audit; push without waiting for GitHub CI. Main stays unmerged and untagged until combined acceptance. | Explicit author request to finish M2 before the next test handover and avoid CI waiting. |
| 2026-11-18 | Readium 3.2.0 opens verified private EPUB snapshots through a deny-all HTTP client. Sanitize publication HTML/SVG before trusted navigator scripts, restrict local resources, and reject external links. No INTERNET permission or DRM decryption modules. | Integrate real paged/scroll EPUB reading with the offline/read-only guardrails; a changed source hash cannot inherit the old Book's locator. |
| 2026-11-18 | Room schema 4 stores currentLocator, totalProgression and cascading Bookmark rows. Global reader preferences/custom themes live in DataStore; imported fonts are bounded private copies. Ignore preserves all history, and explicit Forget is serialized with scanner writes. | M2 persistence and lifecycle contracts; narrow updates avoid overwriting metadata or resurrecting forgotten records. |
| 2026-11-18 | Bundle all eleven reader families with regular/italic/bold WOFF2 faces and tiny native family-name previews, totaling 1,703,528 bytes. Preserve pinned provenance and OFL notices; modified internal names use the Deft prefix. | Meet the 3 MB reader-font budget without dropping faces and show font names in the actual typeface. |
| 2026-11-18 | Settings now has Library/Reader categories and Library → Ignored books. Book details, Continue and reader chrome expose real M2 functionality only: progress/bookmarks are real; time estimates, state statistics, search, highlighting and notes remain with later milestones. | Restore the settings chooser when its second real section arrives and avoid nonfunctional controls or fabricated reading data. |
| 2026-11-18 | Position saves follow an application-owned serialized writer, coalesced per Book. A backward wall-clock correction cannot reject a newer locator; lastReadAt stays monotonic and lastOpenedAt advances beyond the latest open by at least 1 ms when needed. XHTML sanitization preserves XML syntax/entities; selected link accents refresh on resource changes. Native finger-following swipes remain for Slide, while None/system-disabled motion uses immediate turns. | Whole-M2 audit corrections for persistence, actual EPUB MIME/rendering and live appearance/motion consistency. |
| 2026-11-18 | Library view format filters move from navigation SavedStateHandle to their own DataStore string-set key; empty selections persist and never change scan formats. Sort/direction remain in DataStore and are verified through fresh repository/view-model lifetimes. | Author reported filter/sort selections resetting after use. |
| 2026-11-18 | Book grid/list surfaces gain an explicitly requested subtle 1dp shadow, 1dp outline, 8dp corners and padding; grid widths scale with system text size; absent title/author display `<no-title>`/`<no-author>`. Settings gains a contained scan status panel and coherent aligned folders/file-type/storage sections. | Device feedback overrides the earlier all-flat card treatment and rejects the settings layout's grouping and whitespace. |
| 2026-11-18 | Author explicitly requests M1 merge/tag/push after these fixes and local verification; M1 is0.1.2-1. M2 remains a separate unmerged0.2.1-1 patch for reader settings/book detail and crash recovery. Only the newer M2 APK is offered to update the installed0.2.0 preview. | Supersedes the combined-acceptance hold for M1 only; reader settings first exists in M2. No CI waiting. |
| 2026-11-18 | M2 reader settings uses grouped typography/paragraph, appearance/brightness and page/reading controls, with aligned values, visible theme swatches and a bounded scrolling quick panel. Book detail uses an adaptive cover/identity/action surface followed by distinct description, labelled edition metadata and file groups. | Author rejected the earlier vertical stacks, spacing, alignment and weak grouping. Preserve all implemented behavior and accessibility at large text. |
| 2026-11-18 | Publisher-original typography remains a selectable preference mapped to no custom family; font declarations are registered only for families containing font faces. Regression coverage uses the actual bundled catalog and a real EPUB-to-Readium fragment attachment path. | Retraced 0.2.0 crash: Readium rejects the empty original-family declaration before any book opens. Simulated attachment does not replace device WebView reading tests. |
| 2026-11-19 | Version 0.2.2-1 addresses the author's complete M2 checklist and Deft/Lithium media. Bring offline EPUB and library search, an image-only zoom/pan viewer, remembered jump history, collapsible contents, launch-last-book and progress-display choices into M2. Ratings/favorite/tracking remain M3, highlights/notes M5, fixed-layout formats M7 and conversions M8. | Explicit author clarification: keep later milestones while fixing and extending the EPUB/library experience. |
| 2026-11-19 | Library adds a floating Continue action, directional Continue header, upward-scroll return-to-top and a timed scroll thumb. Book description becomes an elevated card; Read/Continue aligns beside the cover bottom. Scan notices separate text, action and dismissal; launcher artwork has more inset and a shorter mark. | Author's device feedback overrides the prior layout and icon proportions. |
| 2026-11-19 | Preserve publisher-original alignment as a selectable global setting. EPUB page-number display denotes stable publication locations, not printed-page or viewport-page counts. Open-last-book is opt-in and runs only on a fresh launch with an eligible Continue book. | Apply useful Lithium options without misleading reflowable-book counts, losing existing preference defaults or creating activity recreation loops. |
| 2026-11-19 | Serve all bundled/imported fonts from the publication's private virtual font paths. Use an internal CSS alias for OpenDyslexic while keeping its stored ID and displayed name. Replace invalid underscore-containing CSP host sources with self/path-scoped sources, while retaining sanitization, local interception and network denial. | Real Chromium regressions reproduced blocked reader scripts/styles/images, cross-origin font failures and Readium's duplicate built-in OpenDyslexic declaration. |
| 2026-11-19 | Genuine continuous EPUB scrolling uses a lazy vertical host of isolated, script-disabled chapter frames with original URLs/locators and passage anchoring across reflow. Paged mode keeps Readium's navigator. Preserve saved UI registry state on recreation while excluding only the publication-dependent fragment state. | Readium 3.2's own scroll mode still pages between chapters; the author explicitly requested one vertical flow and working rotation with controls open. |
| 2026-11-19 | Add persistent tap-edge turns, fullscreen reading and system/portrait/landscape orientation controls from the full Lithium settings reference. Defaults retain edge taps, immersive reading and system orientation. Nonfullscreen mode reserves system-bar insets. | Relevant offline reader controls requested by the author; sync, backup, folder drawers and external-link prompts are not copied into this patch. |

| 2026-11-20 | 0.2.3 preserves publisher root CSS in a stylesheet before Readium adds its own root style. Allow only embedded image data through the isolated viewer; retain network/file denial. Android selection toolbar teardown no longer clears DOM ranges. | The actual Homicide EPUB is valid XML; Readium duplicated its root style attribute. The viewer intercepted its own images. Toolbar recreation can occur while moving selection handles. |
| 2026-11-20 | Paged Slide forwards native finger movement; returning more than halfway cancels. Tap zones are 40/20/40. Reserve system gesture insets with a 24dp edge fallback; continuous chapters gain subtle dividers. | Direct device feedback requests reversible dragging, wider edge taps, system-back priority and chapter separation. |
| 2026-11-20 | Native reader controls use distinct theme-derived surfaces with readable text; top bar shows book title, footer chapter name and page position. The status line explicitly uses book ink and padded edges. | Device screenshots showed indistinguishable surfaces, black status text on dark paper and misaligned progress controls. Page wording supersedes the earlier location label; positions remain reflowable EPUB estimates. |
| 2026-11-20 | Opt-in advanced search is shared by library and reader; match-case/whole-word/regex options remain session-local. RE2/J 1.8 (BSD-3-Clause) provides linear matching. In-book search runs only on Search/Enter; results retain match positions and bounded snippets. | Author requests advanced search without per-keystroke EPUB searches. Limit patterns to256 characters, chapter bytes to8MB, matching text to2M characters and results to500; report incomplete results instead of stalling. |
| 2026-11-20 | Use AndroidX Material Outlined icons with mirrored directional variants throughout application actions; preserve launcher branding. Library gets an8dp thumb,48dp drag rail in a dedicated56dp gutter. Notices separate header/dismiss from a left-aligned action row. | Replace handcrafted action paths as requested; keep scrollbar interaction clear of book cards and scan notices legible at large text. |
| 2026-11-20 | 0.2.4 guards toolbar/system Back by the originating current resumed destination and preserves the navigation root. | The author's recording reproduced repeated outgoing callbacks popping past the library into an empty NavHost. |
| 2026-11-20 | External HTTP(S) links now show the actual destination domain and open in the user's browser only after confirmation; Deft retains no INTERNET permission and never browses automatically. | Explicit device feedback supersedes the earlier closed-external-link notice. |
| 2026-11-20 | Standalone images use bounded native raster/SVG decoding and Compose rendering with focal pinch zoom and bounded pan. Input is capped at 32MiB, decoded side 4096 and total4,194,304 pixels. Reject unsafe or cyclic/deep/over-expanded SVG before rendering. | The WebView image-only surface still appeared blank on the author's phone despite desktop checks. Native rendering removes HTML transport and provides pixel-level local tests and visible failure recovery. |
| 2026-11-20 | Tap zones become 35/30/35. Slow drags past halfway use an exact native Readium page target; reversal cancels. Reader title grows, footer gains top spacing, fullscreen text gains 8dp top padding. | Author adjusted tap proportions and reported velocity-only snap-back plus cramped title/footer/text spacing. The native adapter is pinned to Readium 3.2 and must be reviewed on upgrades. |
| 2026-11-20 | Restore 16dp library gutters and place only the visible scrollbar handle above book cards. Advanced controls/help share aligned raised cards; search results label the actual TOC section and EPUB page position. Scan-details action stacks directly below the completion message. | Author explicitly accepts handle/book overlap to preserve prior grid/list sizing and rejects the controls/notice spacing. |
| 2026-11-20 | 0.2.5 uses 22sp reader headline text, 16dp footer top spacing and equal-height thumb/track layout boxes; visible track stays 3dp. Reader settings gain swipeable Text/Theme/Page tabs with icons. Grid titles take their actual one/two-line height; scan notices reduce bottom inset and scan notification permission has an elevated icon control. | Final author review passed all 0.2.4 checks and requests these minor presentation refinements. Author explicitly approves merging M2 after local verification without another review round. |

| 2026-11-20 | M3 persists confirmed reading duration every 30 seconds. Finite idle timeout trims the unconfirmed tail to the last page/scroll/selection event; a clean exit before timeout includes its current dwell. After a crash, unconfirmed page dwell is discarded, so the 30-second checkpoint bound applies to confirmed reading, not speculative dwell. | Reconcile the 3-minute reading + 6-minute idle acceptance with crash recovery and the explicit preference to undercount rather than count forgotten-open books. |
| 2026-11-20 | Manual Not read requires confirmation that read-throughs and their sessions will be removed; deleting an individual read-through also requires confirmation. Latest read-through is ordered by creation identity, independent of edited dates. | All five manual states must be supported while reading state remains derived from history and date edits must not silently change which attempt is current. |
| 2026-11-20 | Reader overlays and rotation/loading gaps close the current visible session; returning starts a new session. Finish prompts close a qualifying last-page dwell before appearing, and global timeout changes preserve already confirmed time. | Only unobscured, resumed content is eligible; prompt/settings time must not inflate reading and re-created navigators must not double-count. |

| 2026-11-20 | M3 device revision 0.3.1 compares session identity at persisted millisecond precision. Session minimum defaults to 30 seconds, adjustable 1–300 seconds and captured per session; Room 6 migration assigns legacy rows 60 seconds. | Device sessions failed after the first checkpoint because Instant nanoseconds differed after database round-trip; author requests a shorter editable minimum without losing history. |
| 2026-11-20 | Book tracking uses a direct animated favorite heart, aligned half-star geometry, a compact state/assessment group, review action and history rows with a management menu. Dates use a calendar picker. Finish celebration holds after introduction until tap or an eased-in close control. | Author device feedback and eight screenshots reject spacing/alignment and automatic celebration exit. |
| 2026-11-20 | Tracking indicator sits in the center of the status line; page stays left, clock/battery right with charging bolt. Notices center vertically, switches have contrasting off thumbs, and settings titles gain vertical padding. Appearance exposes persistent app System/Light/Dark independent of reader themes; About moves forward from M10 with version, license, privacy and source. | Explicit M3 review requests; remaining M4–M10 scope is unchanged. |

| 2026-11-21 | Final M3 polish 0.3.2 adds a faint idle film with pulsing label, dismissed by a consumed wake touch; resumed reading uses the same position without a page count. Back and direct assessment choices get subtle system-respecting haptics; native WebView word-selection feedback remains native to avoid duplicate pulses. | All 0.3.1 retests passed. Author requests idle visibility and haptics, and authorizes verification followed by merge without another review gate. |
| 2026-11-21 | Ratings use a compact adjustable star row with adjacent clear/favorite actions; state choices wrap at content width with semantic colors, icons and selection indication. Timer Apply sits beside its field. About links kmab to kmab5.github.io and groups icon-led source/license actions. | Five screenshots and final M3 review identify remaining layout issues. |
| 2026-11-21 | M5 records four highlight colors plus colorless/no-highlight; M6 includes optional speed/custom-interval auto-scroll after speed metrics, without treating automatic motion as reading input. Downloadable format engines and online-fiction sources remain discussion only. | Author asks to place these ideas in appropriate milestones and requests thoughts, not implementation, for extensions and online sources. |

| 2026-11-21 | 0.3.3 makes the idle film explicitly black and visible, and emits system-respecting LONG_PRESS feedback only when a real nonempty text selection begins. The shared observer survives toolbar recreation and stops on clear/detach. | All 0.3.2 checks passed, but device feedback found the film invisible and native selection feedback absent. Author explicitly requests this fix commit directly on main, then M4 on a new branch. |
| 2026-11-21 | M4 uses Room 7 and a transactional organization repository. Series keys normalize case/whitespace only; finite decimal positions allow zero/prequels. Explicit removals survive auto-fill, merges union members with target positions winning and redirect aliases. Ignored books are excluded, missing books remain, and abandoned remains unresolved. | Makes the approved category/author rules deterministic without changing source files or history. |
| 2026-11-21 | M4 enables Books/Categories/Authors tabs, bounded private category covers, persisted category/type filters and display, accessible drag order, aggregate author/category summaries and bulk category assignment. Notes and global Stats remain later milestones. | Implements M4 with usable navigation while preserving milestone boundaries and existing visual language. |
| 2026-11-21 | Category/type names are limited to 240 characters, descriptions to 10,000, and series positions to finite values within ±1 billion. Custom category covers copy at most 16 MiB of local image input and store a sampled JPEG up to 960px; imported files never become remote URLs. Submitted cover import, database commit and cleanup finish consistently across activity cancellation. | Bounds keep user edits and local storage predictable while preserving source-file read-only behavior and cover/database consistency. |

| 2026-11-22 | Idle film is 55% black, with a full-width centered black label band pulsing from the film opacity to about 87%; the white label pulses between 65% and full opacity. Reduced motion uses the steady dark band. | Device review still found the label invisible. Author requests this visual adjustment directly on main without a build; M4 includes and verifies it. |

| 2026-11-22 | 0.4.1 separates organization actions from browsing controls, adds persistent category/author sorting and filters, cover-rich member list/grid and picker, visible ordering, and a fixed picker confirmation. Category filtering is removed from Books; legacy saved values are ignored there. | Author device checklist and seven screenshots request clearer hierarchy, compact controls and category browsing through its dedicated tab. |
| 2026-11-22 | File-location sorting uses a canonical BookFile displayPath, preferring present files, then case-insensitive path and file ID. Series drag reorders the exact visible member set into existing unique numeric slots; duplicate slots are renumbered 1..N, explained before saving. Ignored memberships remain untouched. | Supports requested file-location sorting and drag ordering while preserving decimal positions, identity and scanner decisions. |
| 2026-11-22 | All working top-level tabs share last-scan/settings menu styling and support nonwrapping horizontal swipe navigation. Scan notices use separate status, counts and details action and sit above bottom navigation. | Device review found inconsistent menus, awkward scan notices and missing swipe navigation. |
| 2026-11-22 | 0.4.2 uses aligned organization search/browse controls with secondary filters in labelled sheets. Category rating becomes an exact-value summary opening a draft editor with save/cancel and restore-average; Add books and ordering belong to the member section. | The author accepted the behavior, notices and suggestion review but rejected remaining spacing, alignment and category action/rating hierarchy in five device screenshots. |
| 2026-11-22 | Explicitly flagged older-checklist feedback is handled first on main, incrementing main's patch version, committing/tagging/pushing before resuming the active milestone. Small direct main fixes are authorized in that workflow. Requested milestone revision tags identify a handover without authorizing its merge. | Author's workflow clarification in the 0.4.1 review; use actual main versions rather than the illustrative numbers in the comment. |
| 2026-11-22 | 0.4.3 uses the shared DeftSwitch for category filters, matching the settings off-state thumb, track and outline colors. | Author accepted M4 and requested this final contrast fix followed by merge/tag/push without local build verification. |
| 2026-11-22 | 0.4.3 build 2 enables signed GitHub Actions tag publication now: full checks, permanent-certificate and tag/source validation, APK/checksum/mapping/provenance uploads, then a separate release job. Branch builds remain CI-only and unpublished. | Author requests CI to build and publish the latest 0.4.3 APK directly and to monitor the complete run. Reuses the existing key via encrypted repository secrets; preserves v0.4.3-1. |
| 2026-11-22 | Move data exchange from M9 to M7, fixed-layout readers from M7 to M8, and converters from M8 to M9. M7 depends on M1-M6 data, not reader engines; M8/M9 extend export/backup coverage and test older-backup restoration. Conversion caches remain rebuildable and excluded. | Author requests earlier matching, exports and backup if independent. Dependency review of sections 5, 11-13 and 15 confirms this ordering; feature scope is preserved. |
| 2026-11-22 | Refresh current release guidance to v0.4.3-2, index current versus historical documents, preserve author checklists/design evidence, and exclude local agent scratch/tool logs from Git. Documentation-only maintenance does not change the APK version or create a release tag. | Author requests repository/documentation cleanup before the next milestone. No tracked APKs, stray logs, empty files or duplicate screenshots were found; the obsolete local tool error log is removable. |
| 2026-11-23 | Add category long-press multi-select in 0.4.4, with add/remove types, explicit-destination merge and category-only deletion. Match Books selection behavior, deduplicate grouped appearances, and use one transaction per bulk action. Ratings and suggestion-review bulk actions remain outside this addition. | Author requested category multi-item operations before M5 and confirmed these three actions. Preserve destination metadata, book files and permanent history. |
| 2026-11-25 | 0.4.5 includes the 0.4.4 category multi-select addition on main by explicit author approval. Books → add to category keeps search and create category visible outside a bounded category list. Creation reuses the existing editor and atomically assigns exactly the selected books; cancel returns to the picker. | The author requests direct-main work after repairing red CI. The existing create action was hidden after long category lists. No schema or milestone-boundary change. |
