# Deft — Visual Design

*Status: agreed 2026-11-13. **Living document:** everything here can change after the author sees it on a device. Record every change in the Decision log (§20) of `plans/2026-11-13-deft-design.md`.*

Companion to the product spec `plans/2026-11-13-deft-design.md`. The visual reference is `plans/design/preview.html`; when the preview and this doc disagree, this doc wins and the preview gets fixed.

---

## 1. Identity in one paragraph

Deft is a kmab product with its own face. From **kmab-brand** (github.com/kmab5/kmab-brand) it takes the ink-dark neutrals, Space Grotesk and Space Mono, square-capped line icons, lowercase labels, and the rule that **colour carries meaning** (kmab's F1-timing tiers). Its own identity comes from three things kmab doesn't have: **paper** (a warm light mode), **Literata** (a reading serif for titles), and **its own mark**, a stylised book. The result should read as quiet, typographic and precise, with one loud moment: finishing a book.

## 2. Colour

### 2.1 Meaning
Purple is Deft's accent **and** the colour of reading. kmab's tiers map onto reading states:

| Reading state | Colour | kmab meaning |
|---|---|---|
| reading | purple | personal best, current effort |
| finished | green | solid |
| paused | amber | side quest |
| abandoned | muted grey | — |
| not read | dim outline only | — |

These colours drive state chips, the bar under each cover, progress-bar segments, and stats. Purple also marks **personal bests** in stats. Use nothing else for state; never use these colours for decoration.

Material You dynamic colour is **off**, with no toggle.

### 2.2 Tokens
App chrome follows the system light/dark setting.

| Token | Dark (ink) | Light (paper) | Use |
|---|---|---|---|
| `bg` | `#0E0E11` | `#F4F1EA` | screen background |
| `surface` | `#17171B` | `#FBF9F4` | cards, sheets, bars |
| `surface2` | `#1E1E24` | `#EDE9DF` | pressed, inputs, chips |
| `line` | `#2A2A31` | `#DDD8CC` | dividers, outlines |
| `line2` | `#3A3A44` | `#C9C3B5` | strong outlines, not-read outline |
| `text` | `#EDEBE4` | `#17171B` | primary text |
| `muted` | `#9A9AA6` | `#5E5E68` | secondary text, abandoned |
| `dim` | `#66666F` | `#8A8A93` | tertiary text, disabled |
| `accent` / `reading` | `#B24BFF` | `#9940DB` | accent, reading, PB, focus |
| `onAccent` | `#0E0E11` | `#FFFFFF` | text on accent fills |
| `finished` | `#3DE08A` | `#2B9D61` (text: `#21794B`) | finished |
| `paused` | `#F5C451` | `#AC8939` (text: `#846A2C`) | paused |
| `abandoned` | `#9A9AA6` | `#5E5E68` | abandoned |
| `error` | `#FF6B6B` | `#C2413A` | errors only |
| `scrim` | `#0E0E11` @ 85% | `#17171B` @ 60% | modal scrims, celebration |

Full kmab tint/shade scales (50–900) for purple, green and amber live in `core/designsystem/Palette.kt`. Values are in the kmab `design-tokens.css`: primary `#B24BFF`, secondary `#3DE08A`, tertiary `#F5C451`. In light mode use shades 600–700 for fills and bars, and 700–800 for text.

Contrast: body text ≥ 4.5:1, bars and icons ≥ 3:1 on their background. Verify every pair in a unit test (`ContrastTest`).

### 2.3 Reader themes (separate from app chrome, spec §10.1)
| Theme | Background | Text | Link/accent |
|---|---|---|---|
| light | `#FBF9F4` | `#1B1B1F` | `#7D34B2` |
| sepia | `#F1E7D0` | `#4A3B28` | `#7D34B2` |
| dark | `#17171B` | `#D8D5CC` | `#C373FF` |
| black | `#000000` | `#B8B6AE` | `#C373FF` |

Black theme text is deliberately dimmer to avoid glare on OLED. The reader's bars, sheets and panels take their colours from the **current reader theme**, never from the app theme.

## 3. Typography

| Role | Font | Use |
|---|---|---|
| UI | **Space Grotesk** (400, 500, 600) | all interface text |
| Telemetry | **Space Mono** (400, 700) | numbers, times, percentages, chips, badges, eyebrows, stats |
| Books | **Literata** (variable, opsz) | book titles, author/category page headings, celebration words |

All three are SIL OFL. Bundle them as `res/font` resources, subset to Latin, Latin Extended, Greek and Cyrillic. Other scripts fall back to Android's system fonts.

### 3.1 Scale (sp; line height in brackets)
| Token | Font | Size | Weight |
|---|---|---|---|
| `display` | Literata | 40 (48) | 600 |
| `headline` | Literata | 28 (36) | 600 |
| `title` | Literata | 20 (28) | 600 |
| `cardTitle` | Literata | 14 (18) | 600, 2 lines max |
| `section` | Space Grotesk | 18 (24) | 600 |
| `body` | Space Grotesk | 15 (22) | 400 |
| `label` | Space Grotesk | 14 (20) | 500 |
| `mono` | Space Mono | 12 (16) | 400 |
| `monoSmall` | Space Mono | 11 (14) | 400 |
| `statLarge` | Space Mono | 32 (36) | 700 |
| `statMedium` | Space Mono | 20 (24) | 700 |

Respect system font scaling everywhere in the app chrome. Use tabular figures for all numbers (Space Mono is monospaced already).

### 3.2 Reader fonts (spec §10.1)
Bundled: **Literata** (default), **Source Serif 4**, **EB Garamond**, **Crimson Pro**, **Atkinson Hyperlegible**, **Inter**, **OpenDyslexic**. Also offered: "publisher default" and any TTF/OTF the user imports. Use variable fonts where they exist; subset as in §3. Budget: **≤ 2 MB** for all reader fonts combined, checked by a Gradle size assertion. If a font breaks the budget, drop its italics before dropping the font, and say so in the Decision log.

## 4. Shape, space, elevation

- **Corners:** 2dp covers; 4dp chips, inputs, buttons, small badges; 8dp cards, sheets, dialogs. **No pill shapes** anywhere (buttons included).
- **Spacing:** 4dp base. Screen gutter 16dp; grid gap 12dp; section gap 24dp.
- **Elevation:** flat. Separate surfaces with a 1dp `line` or a `surface` step, never with drop shadows. The only shadow in the app is the lifted cover in a celebration (§8).
- **Touch targets:** ≥ 48dp, even where the drawn element is smaller.

## 5. Iconography

- **Lucide** (ISC) redrawn to kmab's grammar: 24dp grid, 1.75 stroke, square caps, square joins. Ship them as Compose `ImageVector`s in `core/designsystem/icons/`. Include only the icons used.
- Outline only. Don't mix filled and outline icons in one group. The single exception: the **favourite heart fills** when on.
- Sizes 16, 20, 24. Pair any icon whose meaning isn't obvious with a label.
- Icon colour follows meaning: `accent` for active/current, `finished` for success, `paused` for non-critical; otherwise `muted`.

## 6. The mark and app icon

- The Deft mark is a **stylised book**, drawn as an original shape, in kmab's line grammar (one stroke weight, square caps), purple on ink. Three candidates are in the preview:
  - **(a)** an open book seen from the side, two page strokes meeting at the spine;
  - **(b)** a closed book standing up, with a purple ribbon;
  - **(c)** an open book with a ribbon falling from the spine.

  **The author picks one after seeing the preview.** Until then, M1 ships candidate (c) as a placeholder.
- App icon: adaptive icon (ink background, purple mark), plus a **monochrome layer** for Android 13+ themed icons. Wordmark: "deft" in Space Grotesk 600, lowercase, set beside the mark only in About and on the landing page.

## 7. Components

| Component | Spec |
|---|---|
| **Top app bar** | `bg` colour, title lowercase in `section`; no elevation; a 1dp `line` appears on scroll |
| **Bottom navigation** | 64dp, `surface`, 1dp top `line`. Five items: books · categories · authors · notes · stats. 24dp outline icon plus `monoSmall` lowercase label. Active item: icon and label in `accent`, plus a **2dp `accent` bar along the item's top edge** that slides between items (smooth, §9). No Material pill indicator. |
| **Buttons** | Primary: `accent` fill, `onAccent` label, 4dp corners, 44dp tall (48dp touch). Secondary: 1dp `line2` outline, `text` label. Text button: `accent` label. Labels lowercase `label`. |
| **State chip** | 4dp corners, `surface2`, a 6dp square dot in the state colour, lowercase `mono` label. Selected: 1dp outline in the state colour. |
| **Filter chip** | Same shape; selected = `accent` outline + `accent` text. |
| **Book cover (grid)** | 2:3, 2dp corners, 1dp `line` outline. Under it: a **2dp bar in the state colour, filled to progress** (not read: a `line2` hairline track only). Then `cardTitle` (2 lines) and the first author in `monoSmall` `muted`. |
| **Generated cover** | When a book has no cover: a block in a tint picked from the title's hash (one of 8 muted kmab tints, stable per book), title in Literata 600 and author in Space Mono, both left-aligned with 12dp padding. |
| **Badges** | "missing" and "protected": `monoSmall` lowercase on a `scrim`-coloured 4dp tag in the cover's top-left corner. "protected" adds a 12dp lock icon. |
| **Book row (list)** | 56×84dp cover, title (`title`, 1 line), authors (`mono`), state chip, progress as % in `mono`, time read. |
| **Progress bar (detail)** | 6dp, `surface2` track, 2dp corners. Book: one segment. Category/author/series: stacked segments in state colours (finished, reading+paused, abandoned), with a `mono` legend. |
| **Rating** | Ten half-star hit areas over five outline stars; filled portions in `accent`. Number beside it in `mono` ("4.5"). |
| **Sheets** | `surface`, 8dp top corners, 24dp padding; drag handle 32×4dp `line2`. |
| **Dialogs** | `surface`, 8dp corners, title lowercase `section`, body in sentence case. |
| **Snackbar** | `text` on `surface2`, 4dp corners, `accent` action. |
| **Stats tile** | `surface`, 8dp corners, eyebrow in `monoSmall` `muted` lowercase, value in `statLarge`, delta in `mono` (`finished` colour if up, `muted` if down; never red). |
| **Personal-best strip** | Full-width 32dp band, `accent` fill, `onAccent` `mono` text: "personal best · fastest finish". |
| **Continue card** | Top of books: `surface` card with the cover (48×72), "continue" eyebrow, title, a `mono` line "43% · 2h 10m left", and a 2dp purple bar. Tap opens the reader at the saved position. |

## 8. Celebrations

Setting **"celebrations"** (Settings → Appearance), **on** by default. When it's off, or the system has animations turned off, every celebration falls back to its **quiet version**.

### 8.1 Finishing a book: full-screen, ~1.5 s, tap to skip
| t (ms) | What happens |
|---|---|
| 0–250 | The scrim fades in to cover the full screen. The cover moves (shared element) to the centre, scales to 1.15× and lifts, with the app's only shadow. |
| 150–600 | The 2dp state bar under the cover becomes 4dp and **sweeps purple → green**, left to right. |
| 350–1000 | **Streak burst:** 28–36 square-capped strokes radiate from the cover's edges in purple, green and amber, with random angles and lengths (seeded by book ID, so the same book always gets the same burst). They decelerate, then fade. Drawn in one `Canvas` pass. |
| 600–1100 | **"finished."** (Literata `display`) rises 16dp and fades in. Below it, the total time and session count **count up** in `statMedium`. |
| 1000–1400 | If this is a personal best, the **personal-best strip** slides in from the left: "personal best · longest book". |
| 1400+ | The finish sheet rises (rating, favourite, "done"). The burst layer is cleared. |

A tap anywhere jumps straight to the end state. Under 1 s the user can still rate without waiting.

**Quiet version:** the finish sheet only, fading in, with a one-line "finished." header.

### 8.2 Other celebrations
| Moment | Treatment | Quiet version |
|---|---|---|
| Yearly goal reached | Full sequence, cover replaced by the goal ring closing; words "goal reached." + "24 books this year" | snackbar |
| Series finished (last book in a Series category) | Full sequence with the series' covers fanned behind; words "series finished." + series name | snackbar |
| Streak milestone (7, 30, 100, 365 days) | Streak burst only, from the streak tile, shown when the reader is closed; "30 days in a row" in `mono` | snackbar |
| Personal best during a session | Personal-best strip only, at the top of the book page after the reader closes | none (the stat is still marked purple) |

If several celebrations happen at once, play only the biggest: series finished > goal reached > book finished > streak > personal best. Mention the others in the finish sheet.

### 8.3 Personal bests tracked
Longest session, most time in a day, longest book finished, fastest finish (pages per hour, minimum 100 pages), longest streak, most books in a month.

## 9. Motion

**Rule one: smooth.** Every change of state animates: nothing pops, jumps or reflows abruptly. Every animation can be interrupted: retarget from the current value, never restart from the beginning or block input (except celebrations, which a tap skips). Hold 60 fps (120 on capable devices) on a mid-range phone: animate with `graphicsLayer` (alpha, translation, scale), not with layout changes.

| Token | Value | Use |
|---|---|---|
| `short` | 150 ms | chips, toggles, icon state, bar colour |
| `medium` | 250 ms | screen transitions, sheets, nav indicator |
| `long` | 400 ms | shared-element cover → book page |
| `enter` easing | cubic (0.05, 0.7, 0.1, 1.0) | anything appearing |
| `exit` easing | cubic (0.3, 0.0, 0.8, 0.15) | anything leaving |
| `standard` easing | cubic (0.2, 0.0, 0.0, 1.0) | in-place changes |
| springs | damping ratio 1.0 (**no bounce**), stiffness medium-low | drag release, reorder, nav bar indicator |

- Screen transitions: fade + 24dp slide in the direction of navigation.
- Cover → book page: shared-element transition (`SharedTransitionLayout`).
- Lists: item insert, remove and reorder animate (`animateItem`). Drag-to-reorder categories lifts the item with a `surface2` fill, no shadow.
- Progress bars and counters animate to new values; never snap.
- Reader page turn: "none" or "slide" (spec §10.1). Slide follows the finger and settles with the no-bounce spring.
- System "remove animations" on → all durations 0 except crossfades at `short`.

## 10. Haptics

Only these, all subtle (`HapticFeedbackConstants`):
- long-press selection → `LONG_PRESS`
- confirming a state change (finished, abandoned, paused) → `CONFIRM` (API 30+), else `CONTEXT_CLICK`
- volume-key page turns (setting, **off** by default) → `CLOCK_TICK`
- the moment the celebration burst fires → `CONFIRM`

Nothing else vibrates.

## 11. Screens

The sketches show layout only; the preview shows the look. Labels are lowercase.

### 11.1 books
```
┌────────────────────────────────┐
│ books                 ⌕  ⇅  ⋮  │
│ ┌────────────────────────────┐ │
│ │▕cov▏ continue               │ │
│ │▕   ▏ The Well of Ascension  │ │
│ │      43% · 2h 10m left      │ │
│ │ ▬▬▬▬▬▬▬▬▬───────────        │ │
│ └────────────────────────────┘ │
│ [all][■ reading][■ paused][…]  │
│ ┌───┐ ┌───┐ ┌───┐              │
│ │   │ │   │ │   │              │
│ └───┘ └───┘ └───┘              │
│ ▬▬▬── ▬▬▬▬▬ ─────              │  ← state bar = progress
│ Title Title Title              │
│ AUTHR AUTHR AUTHR              │
├────────────────────────────────┤
│ ▔▔▔                            │  ← 2dp accent bar
│ books categ authors notes stats│
└────────────────────────────────┘
```
Empty, no folders: "no books yet." / "Point Deft at a folder and it will find them." / [add a folder].

### 11.2 book page
Large cover (shared element) on a `surface` band. Title in `headline`, authors in `mono` (each one taps through to the author page). A state control (segmented: reading · paused · finished · abandoned; "not read" shown only as the initial state). Rating + favourite. Primary button "continue" / "read". Segmented progress bar with % and time left. Sections: categories (with series position "#2"), next in series, read-throughs (a timeline: dates in `mono`, outcome chip), stats tiles (time, sessions, speed), notes (count, tap to open), edition match, file (paths, format, size, badges). Overflow: ignore, forget, edit metadata.

### 11.3 categories / category page
List of category cards: a 2×2 collage of member covers, name (Literata `title`), types as `mono` tags, a segmented progress bar, a rating in `mono` (with "· yours" when overridden). A type filter row of chips sits on top. Category page: header with the collage, the progress legend, rating (override/reset), an ordered member list (series shows "#1, #2, #2.5" in `mono`), drag handles in reorder mode. A "review" dot in `paused` marks suggested auto-fill members.

### 11.4 authors / author page
List rows: name (Space Grotesk 500), `mono` "7 books · 5 finished", mini progress bar. Author page: name in `headline`, the progress legend, books in a horizontal cover row by state, time and average rating tiles.

### 11.5 notes
Filter chips (book, colour, type). Each entry: a 3dp left rule in the highlight colour, the passage in Literata 15 italic, the note in Space Grotesk, book title + location in `monoSmall`. Highlight colours: purple, green, amber, and a neutral `line2` grey (so they don't clash with state colours, they show only as a soft 24% fill in the reader).

### 11.6 stats
A range selector (week · month · year · all · custom) as filter chips. Tiles: time read, books finished, current streak, pages. Heatmap: a 7-row calendar grid, cells 12dp with 3dp gaps, `surface2` empty, purple at four intensities (25/50/75/100%). Charts: flat bars (2dp corners), no gridlines apart from one baseline in `line`, values in `mono` on tap. Personal bests listed with the purple marker. "year in review" entry card at the bottom.

### 11.7 year in review
Vertical full-bleed pages, one stat each, swiped like a story: the big number in `statLarge` at 64sp, a caption in Literata, top covers. Transitions follow §9. Under celebrations-off it's a plain scroll.

### 11.8 reader
Content full-bleed. A tap in the centre reveals the top bar (back, title, toc, bookmark, search, ⋮) and the bottom bar (chapter progress slider with chapter ticks, then aa, theme, brightness), coloured from the reader theme, fading over `medium`. **aa sheet:** font list (each name set in its own font), size −/+, line spacing, margins, alignment, hyphenation, paged/scroll, all with live preview. Status line (optional): clock · chapter % · battery, in Space Mono 10sp at 60% text opacity.

### 11.9 settings
Grouped list, lowercase group headers in `monoSmall` `muted`: library · reader · tracking · goals · appearance (theme, celebrations) · matching · backup & export · storage · about.

## 12. Voice and copy

Follow kmab's writing style (github.com/kmab5/kmab-brand, `brand-kit/08_WRITING`). The rules that matter for UI:
- Labels, headings, buttons, tabs and menu items are **lowercase**. Book titles and author names stay exactly as their metadata has them.
- Body sentences (dialogs, empty states) use normal sentence case and a full stop.
- Short sentences. Plain words. A little dry humour in empty states. No exclamation marks, no "Oops", no "Awesome", no Title Case. Use em dashes rarely.
- Say what happened and what to do next. Never blame the user.

### Copy deck
| Where | Title | Body | Actions |
|---|---|---|---|
| books, no folders | no books yet. | Point Deft at a folder and it will find them. | add a folder |
| books, folders but none found | nothing here. | No supported books in these folders. Check the file types in settings. | rescan · settings |
| notes, empty | nothing highlighted yet. | Long-press any passage while reading. | — |
| stats, empty | no sessions yet. | Open a book. The clock starts itself. | — |
| reached the end | reached the end. | Mark it finished? | finished · not yet |
| opening an abandoned book | you set this one down. | Pick up where you left off, or start over? | resume · start over |
| opening a finished book | you've finished this. | Re-reading starts a new read-through. Browsing doesn't count toward stats. | re-read · just browsing |
| protected book | this file is protected. | It's locked with DRM, so Deft can't open it. You can still rate it, sort it and track it. | ok |
| missing book | file missing. | Deft can't find this file anymore. Its history is safe. | ok |
| ignore | ignore this? | It disappears from your library and stats. You can bring it back in settings. | ignore · cancel |
| forget | forget this book? | Its history, notes and rating will be deleted for good. This can't be undone. | forget · keep |
| remove folder | remove this folder? | Its books will be marked missing. Their history stays. | remove · cancel |
| exclusion outside root | — | Pick a folder inside {root}. | ok |
| scan summary | — | added 12 · missing 1 · 0 errors | — |
| celebration: book | finished. | {time} · {n} sessions | done |
| celebration: goal | goal reached. | {n} books this year. | done |
| celebration: series | series finished. | {series} | done |
| celebration: streak | — | {n} days in a row | — |
| personal best strip | — | personal best · {what} | — |

## 13. Implementation notes
- `core/designsystem` holds everything here as Compose code: `DeftColors` (light/dark), `DeftReaderThemes`, `DeftTypography`, `DeftShapes`, `DeftMotion`, `DeftIcons`, and components (`StateChip`, `CoverImage`, `GeneratedCover`, `StateBar`, `SegmentedProgress`, `RatingBar`, `StatTile`, `PersonalBestStrip`, `DeftBottomBar`).
- Celebrations live in `feature/celebrate` (`CelebrationHost` overlay in `app`, fed by a `CelebrationEvents` flow from tracking and stats).
- Screenshot tests (Roborazzi or Paparazzi; Paparazzi needs no device) for every component in both themes, plus `ContrastTest` and the font-size budget check. Update the goldens whenever the author's visual feedback changes something.
