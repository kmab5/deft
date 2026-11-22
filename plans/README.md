# Deft documentation index

M1–M4 are complete. The current release is **0.4.3**, build
[`v0.4.3-2`](https://github.com/kmab5/deft/releases/tag/v0.4.3-2), built and published
by GitHub Actions using the persistent Deft release key. M5 is next.

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
| M4 | [Organization](2026-11-21-m4-organization.md) | [Accepted checklist](m4-test-checklist.md) | [0.4.2 device-review build](2026-11-22-m4-042-validation.md) and [0.4.3-2 CI release](https://github.com/kmab5/deft/actions/runs/45057412923) |

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
