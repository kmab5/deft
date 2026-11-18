# M1 library polish and M2 recovery plan

> **For agentic workers:** Use subagent-driven-development with the author's batched checks/audit override. Only the root runs Gradle. Work in the explicitly assigned checkout and files.

**Goal:** Finish the accepted M1 foundation, merge/tag/push it, then repair the M2 launch crash and redesign its settings and book detail for a signed patch handover.

**Architecture:** Keep scanner configuration separate from persisted library view filters. Use the existing paper/ink theme, icon set and 4 dp spacing scale, with 16 dp gutters and 24 dp section separation. Library cards gain the explicitly requested subtle border, 1 dp lift, 8 dp corners and padding. Settings use meaningful groups and aligned controls. M2 retains the Readium host and global preference model; fix the failing font registration contract at its source.

**Tech stack:** Installed Kotlin/Compose/Room/DataStore/Readium, JDK17/21, Gradle8.13, SDK at D:/Code/android-sdk. No toolchain downloads.

## Constraints and order

- Source books remain read-only; missing/ignored books retain history. No new network, DRM or database schema changes.
- User explicitly authorizes merging M1 after these fixes and local checks. M2 remains unmerged pending a new device test. Reader settings first exists in M2 and is repaired there.
- M1 patch is 0.1.2-1 (code10201); M2 patch is 0.2.1-1 (code20101). M1's separate milestone release is older than the installed M2 preview, so only the M2 patch is handed over for updating that installation.
- Reuse the persistent signing key, R8/resource shrinking. Push branches/tags without waiting for CI. Commit as Samuel Worash Getaneh <samuelworash@gmail.com>.

## Stage A — M1 in the isolated foundation checkout

- [x] Persist `LibrarySettings.formatFilter` with a distinct string-set preference key, default all formats, preserving an explicitly empty set. Add `setFormatFilter(format, enabled)` using atomic DataStore edit. BooksViewModel queries this durable setting rather than SavedStateHandle. Verify sort/direction/filter survive actual DataStore close/reopen and a fresh view model; rapid toggles cannot lose edits or change scan formats.
- [x] Restyle BookCard and BookRow with a shared 8 dp rounded surface, 1 dp border/lift and 8–12 dp interior padding. Blank/whitespace title/author displays `<no-title>` / `<no-author>` consistently including generated covers; do not alter stored metadata. Preserve all display sizes and 48 dp targets.
- [x] Redesign LibrarySettingsScreen and LibraryStorage: self-contained scan panel with status, compact action and contained progress; coherent folder header/actions, aligned file-type controls, and grouped storage rows. Keep active/idle/error states and large text usable, with no nested decorative cards or arbitrary empty areas.
- [x] Add regression tests for persistence and missing metadata. Render library/settings in paper/ink at normal and 150% font scale; inspect one batch, correct findings together.
- [x] Record visual/spec decisions and update M1 checklist without overwriting author feedback. Run combined check assembleRelease and whole-change audit; fix failures. Verify M1 certificate, manifest identity and minification explicitly without lowering the private marker from the newer M2 preview.
- [ ] Commit/push M1, merge --no-ff into main, tag v0.1.2-1, push main/tag. This merge is explicitly requested by the author. Do not wait for CI.

## Stage B — M2 after merging M1

- [ ] Merge main into feat/m2-epub-reader, preserving M2 routes/progress/Continue and applying foundation fixes; resolve version to0.2.1-1.
- [ ] Retrace the preserved 0.2.0 mapping. Reproduce the real catalog-to-navigator configuration failure before fixing it: publisher-original has no font faces and must not be registered as a custom font family. Test actual navigator configuration with all bundled families and imported faces, not only preference conversion. Inspect adjacent host lifecycle/font routing assumptions.
- [ ] Redesign reader preferences as compact, aligned typography/appearance/reading sections with clear control groups and visible values. Preserve every setting and custom font/theme operation, live application, and accessible scrolling at large text.
- [ ] Redesign book detail around cover + identity + Read/Continue, a compact progress area, and separate description/file information groups. Adapt the header for narrow/large-text layouts; retain ignore/forget confirmations and honest missing/protected messages.
- [ ] Record decisions, add crash regression and update M2 checklist. Run one batched verification/render/audit pass and final check assembleRelease. Verify persistent certificate/version20101/no INTERNET/R8, stamp committed source, archive mapping and APK evidence, push branch and hand over deft-0.2.1.apk. No M2 merge until accepted.
