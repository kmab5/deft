# Deft — agent guide

Deft is an offline, accountless Android ebook reader with a built-in reading tracker (Kotlin, Jetpack Compose, GPL-3.0). The reader and tracker live in one app so reading time is recorded exactly, without the user having to remember anything.

## Where things are

- `plans/2026-11-13-deft-design.md` is the **spec**: what Deft is, plus every decision already made. Read it before planning or building any feature. Section numbers (§n) in other docs point into it.
- `plans/2026-11-13-deft-roadmap.md` lists the **milestones** (M1–M10), each with its scope, acceptance criteria and the interfaces it hands on to later milestones.
- `plans/YYYY-MM-DD-mN-<name>.md` holds the task-level **milestone plans**. M1 is `plans/2026-11-13-m1-foundation.md`.
- `docs/` is reserved for the GitHub Pages landing page. Planning material goes in `plans/`.

## Workflow

1. Find the first milestone in the roadmap that isn't finished.
2. If it has no plan in `plans/` yet, write one with the superpowers:writing-plans skill, using the spec and the roadmap. Commit the plan before writing code.
3. Execute the plan task by task with superpowers:subagent-driven-development, test-first (red → green → commit).
4. A milestone is done when its roadmap acceptance criteria are verified and `./gradlew check assembleRelease` is green.
5. When a decision changes, or the spec is silent and you make a call, add a row to the spec's **Decision log** (§20) in the same commit.

## Guardrails

These come from the spec. They are hard rules.

- **Book files are read-only.** Deft reads them through SAF and never writes, renames or deletes them. User edits to metadata are stored as database overrides.
- **History is permanent.** Deleting a file marks its Book as missing. A Book is removed only when the user explicitly chooses to forget it.
- **Network lives only in `:data:matching`**, and runs only when the user explicitly asks. `:checkNetworkIsolation` enforces this.
- **DRM is detected and labelled ("Protected").** It is never decrypted, and no code that breaks DRM is added, in any branch.
- **Dependencies are GPL-3.0-compatible and F-Droid-clean.** Native libraries (PDFium, djvulibre, libmobi) are built from source. Never add Google Play Services, Firebase or analytics.
- **Every Room schema change ships with a migration and a migration test.**
- **The social features** (profiles, clubs, chat) **belong to a future, separate app.** Deft's only link to it is the exported Reading Profile (§12.3).

## Commits

Author every commit as `Samuel Worash Getaneh <samuelworash@gmail.com>`. The repo-local git config already sets this, so verify it with `git config user.email`. Use Conventional Commit messages (`feat(scanner): …`, `fix: …`, `build: …`).
