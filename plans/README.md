# Deft documentation index

M1–M4 are complete. **0.4.5** includes category multi-select from 0.4.4 and
visible category creation from the Books selection picker, integrated directly
on main at the author's request. M5 is implemented on `feat/m5-notes` and awaits device acceptance of 0.5.0.

See the [0.4.5 implementation record](2026-11-25-m4-045-category-picker.md) and
[M4 device checks](m4-test-checklist.md#045--create-categories-from-selected-books).
Version tags publish permanent-key APKs through GitHub Actions; the handover tag
is `v0.4.5-1`. Earlier checklist results remain historical, not new device validation.

M5: [implementation plan](2026-11-25-m5-notes.md) and [device checklist](m5-test-checklist.md). The milestone remains unmerged until device acceptance.

## Current guidance

- [Product specification and decision log](2026-11-13-deft-design.md): approved behavior, architecture and constraints.
- [Roadmap](2026-11-13-deft-roadmap.md): milestone scope, acceptance criteria and dependencies.
- [Visual design](2026-11-13-deft-visual-design.md) and [visual reference](design/preview.html): design rules; dated device refinements take precedence over older examples.
- [Glossary](../GLOSSARY.md): domain terms used in code and copy.
- [Agent guide](../AGENTS.md): development, review, versioning and handover workflow.
- [Release signing and CI publication](release-signing.md): persistent-key setup, tag builds, release assets and retry procedure.
- [Dependency sources](dependency-sources.md): pinned library and font provenance.

## Remaining milestones

| Milestone | Initial version | Scope |
|---|---|---|
| M5 | 0.5.0 | Highlights, notes, selection actions and search refinements |
| M6 | 0.6.0 | Statistics, goals, year in review and optional auto-scroll |
| M7 | 0.7.0 | Edition matching, CSV/JSON exports and local backup/restore |
| M8 | 0.8.0 | PDF, CBZ and DjVu readers |
| M9 | 0.9.0 | TXT, FB2 and MOBI/AZW/AZW3 conversion |
| M10 | 1.0.0 | Final polish, accessibility, performance and F-Droid preparation |

On 2026-11-22, data exchange moved from M9 to M7, fixed-layout readers from M7 to
M8, and converters from M8 to M9. Data exchange uses metadata and persisted data
from M1–M6; it does not depend on either reader engine. M8/M9 must extend its
backup/export coverage for new persisted data and test restoring older backups.
Detailed plans for M5–M10 are written before their implementation.

## Completed milestones and review evidence

| Milestone | Implementation plan | Device review | Final milestone evidence |
|---|---|---|---|
| M1 | [Foundation](2026-11-13-m1-foundation.md) | [Checklist](m1-test-checklist.md) | [M1 polish and M2 recovery](2026-11-18-m1-polish-m2-recovery.md) |
| M2 | [EPUB reader](2026-11-18-m2-epub-reader.md) | [Checklist](m2-test-checklist.md) | [0.2.5 validation](2026-11-20-m2-0.2.5-validation.md) |
| M3 | [Tracking](2026-11-20-m3-reading-tracking.md) | [Checklist](m3-test-checklist.md) | [0.3.3 follow-up](2026-11-21-m3-033-validation.md) |
| M4 | [Organization](2026-11-21-m4-organization.md) | [Accepted checklist](m4-test-checklist.md) | [0.4.2 device-review build](2026-11-22-m4-042-validation.md), [0.4.3-2 CI release](https://github.com/kmab5/deft/actions/runs/45057412923), and [0.4.5 additions/CI repair](2026-11-25-m4-045-category-picker.md) |

Dated implementation, feedback and validation documents are historical records.
Their old branch status, unchecked implementation tasks and original milestone
numbers describe that handover, not work still pending today. Checklists preserve
the author's comments and results; do not rewrite them as new device-test claims.
Older M1 retest checklists and `plans/design/` galleries remain useful evidence.
The decision log likewise preserves its dated decisions and records later changes
as new rows.

[Format extensions and online fiction](2026-11-21-format-and-online-fiction-thoughts.md)
is a discussion, not approved implementation scope. Social features remain a
separate future app; online-source support and downloadable engines are outside
the approved v1 roadmap.

## Repository housekeeping

Keep planning material here; `docs/` is reserved for the future landing page.
Keep source fixtures, migrations, licenses, provenance and referenced screenshots.
Private signing material, APKs, build outputs, local feedback media, agent scratch
files and tool logs are excluded from Git. Local scratch files may include release
evidence or keys referenced by tooling: inspect them before any cleanup. Preserve
release artifacts and original feedback; do not clear Gradle/SDK caches merely to
tidy the working directory.
