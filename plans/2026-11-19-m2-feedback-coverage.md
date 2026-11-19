# 0.2.2 feedback coverage

The author's original results and comments remain in `m2-test-checklist.md`. This index covers the entire checklist, including inline comments and Additional feedback. Implementation and verification evidence is recorded before handover; unchecked device steps are not treated as passed.

## Evidence reviewed

All 22 JPG screenshots in `plans/feedback/` and both complete recording spans were reviewed. The recordings were sampled every three seconds, with full-size screenshots used for settings and layout details.

| Reference | Evidence |
|---|---|
| Deft recording, 0–21 s | Unstyled white publication body, stuck controls, reading-mode change not affecting content. |
| Deft, 21–54 s | Inconsistent page/scroll gesture behavior. |
| Deft, 54–84 s | Font, text size and dark theme change native settings but fail to restyle the book. |
| Deft, 84–99 s | Persistent controls and excess blank space. |
| Lithium, 0–21 s | In-book search snippets, result jump and Back restoring the prior position. |
| Lithium, 24–30 s | Library title/author search. |
| Lithium, 39–51 s | Progress seeking; image-only viewer with zoom, pan and close. |
| Lithium, 57–99 s | Control visibility, text selection, contents and bookmarks. |
| Screenshots | Scan notice alignment; folder/rescan icons; description/action placement; broken images; text/theme/flow/brightness/alignment/margin/line-spacing/font controls. |

## Requirement map

| Author feedback | 0.2.2 work / disposition |
|---|---|
| Text, images, headings, emphasis and internal links fail | Repair publication resource security policy; actual Chromium regression covers bundled reader script, styles, local images and preferences. |
| All typography settings, themes, imported fonts and adjacent-chapter link colors fail | Same resource fix; retain shared DataStore preferences and reapply settings to active/preloaded content. |
| Paged content scrolls; vertical gestures turn pages; scroll chapters remain paged | Correct gesture routing; implement genuine vertical adjacent-chapter scrolling, preserving original chapter locators. |
| Rotation with reading controls fails | Restore reader position, mode and controls without reopening the book or losing theme settings; device retest required. |
| Damaged/truncated/missing chapters | Preserve actionable failure paths and return navigation; exercise malformed/resource failure fixtures. |
| TOC cannot collapse; reopen loses position | Expand/collapse chapter hierarchy, remember list position and reveal current chapter initially. |
| Slider only updates on release | Show target chapter/progress while dragging; commit the jump on release. |
| Tiny progress ticks / oversized footer | Remove section ticks and use compact progress controls. |
| Back should undo multiple jumps | Maintain jump history for TOC, seek, search and internal links; ordinary turns do not add history. |
| Selection handles change pages | Keep native selection gestures out of page-turn handling. |
| HUD never hides, hides status line | Center-tap toggle, idle dismissal and unobscured status line; controls remain accessible with system motion disabled. |
| Tap zones | Left/right page zones in paged mode; center controls; vertical scroll gestures stay scrolling. |
| EPUB pinch zoom | Disable page zoom; enable zoom only in the dedicated image viewer. |
| Tappable images, close-only viewer | Load publication-local image bytes into a separate zoom/pan viewer; no source modifications or network. |
| In-book and library search | Offline text-result navigation; title/author library filtering with a clear action and useful empty state. |
| Open last book at launch | Persistent opt-in; fresh launches only, eligible Continue book, wait for resumed activity. |
| Floating Continue; header hides down/reappears up | Add floating Continue action and directional Continue header. |
| Scroll-to-top on upward scrolling; auto-hide scrollbar | Shared list/grid scroll controls with motion-aware visibility. |
| Description needs a card; Read/Continue beside cover bottom | Contained description and responsive cover/identity/action composition. |
| Scan notice alignment/spacing | Separate message/action/close layout, retaining stacked bottom notices and timed dismissal. |
| Hand-drawn-looking folder/rescan icons | Consistent outline geometry and corrected circular curves. |
| More launcher inset; shorter mark | Increase safe space and reduce mark height in both foreground and monochrome assets. |
| Percentage or page number | Global progress-display preference; EPUB location numbers must be labelled honestly, not presented as printed pages. |
| Useful Lithium settings | Keep font/size, line spacing, margins, paged/scroll, system/manual brightness, four built-in themes and global settings; add publisher-original alignment. |
| Bookmarks unclear | Expose bookmark navigation explicitly; retain add/rename/remove and persistence. |
| Ratings/likes, reading states and time estimates | Author confirmed later milestone boundaries; M3 tracking owns these features. No fake counters or inert controls. |
| Notes/highlights | M5, as explicitly confirmed by the author. Existing text selection remains available. |
| Other formats | Keep M7 fixed-layout and M8 converter scope; 0.2.2 reader remains EPUB. Library scanning support is distinct from reading support. |
| Accessibility, motion, privacy and general polish | Repeat normal/large-text renders, reduced-motion behavior, labels and offline resource checks; retain full device checklist. |
| Previously passing update/scanning/filter/history behaviors | Preserve them and include regression checks alongside reader changes. No Room schema change planned. |

## Verification record

`./gradlew check assembleRelease` passed on 2026-11-19: 539 test executions across JVM/Android variants, zero failures/errors/skips. ktlint, detekt, Android lint, font budget and network isolation passed. Four Python release-tooling tests passed.

Installed-Chromium tests passed for actual sanitized EPUB + bundled Readium JavaScript/CSS, all 33 font faces from the actual post-registration navigator configuration, local images, typography/theme changes and script/network restrictions. Restoring the old CSP in an isolated copy reproduced the blocked-script failure; the corrected policy passed. The continuous-reader regression passed shared-viewport chapters, wheel boundary crossing, original resource URLs, CSS isolation, sandboxing, repeated-result context, reflow anchoring, selection, missing-chapter recovery and bounded frames.

The whole-change audit and focused follow-ups have no remaining actionable critical/important findings. Normal/large-text book and settings renders and compact reader chrome were inspected; reference: `plans/design/m2-feedback.html`. No Android device or installed emulator was available. Phone-specific rendering, gestures, lifecycle and accessibility remain device checks. Final signed APK identity/hash/source evidence is recorded in the test checklist.

## Full Lithium settings reference

`Screenshot_20261118_194633_Lithium.jpg` includes keep-screen-on, fullscreen, app theme, backups/sync, edge taps, volume turns, external-link confirmation, drawer gestures, rotation, launch-last-book, library folders, folder drawer and book-wide progress. 0.2.2 includes the relevant reader controls, including fullscreen/tap-edge/orientation preferences. Deft already follows Android's app theme and has SAF library roots. External links remain closed under the offline guardrail; no folder drawer exists; backups are M9 and sync is outside the offline/accountless app. Notes/highlight actions shown in the recording remain M5 by the author's explicit scope decision.
