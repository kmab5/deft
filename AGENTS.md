# Deft — agent guide

Deft is an offline, accountless Android ebook reader with a built-in reading tracker (Kotlin, Jetpack Compose, GPL-3.0). The reader and tracker live in one app so reading time is recorded exactly, without the user having to remember anything.

## Where things are

- `plans/2026-11-13-deft-design.md` is the **spec**: what Deft is, plus every decision already made. Read it before planning or building any feature. Section numbers (§n) in other docs point into it.
- `plans/2026-11-13-deft-roadmap.md` lists the **milestones** (M1–M10), each with its scope, acceptance criteria and the interfaces it hands on to later milestones.
- `plans/2026-11-13-deft-visual-design.md` is the **visual design**: colours, type, components, screens, motion, celebrations, copy. Every UI change follows it; `plans/design/preview.html` is its visual reference. It is a living doc: the author's feedback on a device overrides it, logged in the spec's §20.
- `GLOSSARY.md` holds the domain **terms**. Use them in code, UI copy and docs.
- `plans/YYYY-MM-DD-mN-<name>.md` holds the task-level **milestone plans**. M1 is `plans/2026-11-13-m1-foundation.md`.
- `docs/` is reserved for the GitHub Pages landing page. Planning material goes in `plans/`.

## Workflow

1. Find the first milestone in the roadmap that isn't finished (the latest `v0.B.0-*` tag on `main` names the last merged one).
2. Branch from `main`: `feat/mN-<name>` (fixes: `fix/<topic>`). There are no pull requests. Push the branch so CI runs on it.
3. If the milestone has no plan in `plans/` yet, write one with superpowers:writing-plans, using the spec and the roadmap. Commit it on the branch before writing code.
4. Execute the plan task by task with superpowers:subagent-driven-development. Each change goes red → green → commit (superpowers:test-driven-development). Shape modules with mattpocock-skills:codebase-design.
5. Before handover, run superpowers:requesting-code-review on the branch and superpowers:verification-before-completion. `./gradlew check assembleRelease` must be green.
6. **Handover:** build `./gradlew assembleRelease` with R8 minification and resource shrinking enabled, signed with the persistent Deft release key, and write `plans/mN-test-checklist.md` (the roadmap acceptance criteria plus anything risky, as tickable device steps). Give the author the signed release APK path and the checklist. Reuse the same release key for every release so updates preserve app data; never regenerate or commit the key or its credentials.
7. **Merge only after the author says the checklist passed:** `git merge --no-ff` into `main`, then tag and push the tag (see Versioning).
8. When a decision changes, or the spec is silent and you make a call, add a row to the spec's **Decision log** (§20) in the same commit.

## Versioning

Tags are `vA.B.C-D`: A major, B feature, C fix, D build (a rebuild of the same A.B.C with only build, CI or packaging changes; resets to 1 when A, B or C changes). Tag **every** merge to `main`: a milestone starts a new B (M1 starts at `v0.1.0-1`), a fix bumps C, a build-only change bumps D. Increment C for each feature/fix revision handed over within a milestone, including pre-merge device feedback (for example `0.1.1-1`). Update `deft.version` in `gradle.properties` before building it. Name the test APK `deft-A.B.C.apk`, without milestone or feedback suffixes. The eventual merge/tag preserves the latest tested version; never downgrade its version code. `versionCode = A*1_000_000 + B*10_000 + C*100 + D`. Every tag publishes a GitHub Release (pre-release while A = 0). M10 ships `v1.0.0-1`.

## Guardrails

These come from the spec. They are hard rules.

- **Book files are read-only.** Deft reads them through SAF and never writes, renames or deletes them. User edits to metadata are stored as database overrides.
- **History is permanent.** Deleting a file marks its Book as missing. A Book is removed only when the user explicitly chooses to forget it.
- **Network lives only in `:data:matching`**, and runs only when the user explicitly asks. `:checkNetworkIsolation` enforces this.
- **DRM is detected and labelled ("Protected").** It is never decrypted, and no code that breaks DRM is added, in any branch.
- **Dependencies are GPL-3.0-compatible and F-Droid-clean.** Native libraries (PDFium, djvulibre, libmobi) are built from source. Never add Google Play Services, Firebase or analytics.
- **Every Room schema change ships with a migration and a migration test.**
- **App ID and root package: `com.kmab.deft`.** It is permanent.
- **The social features** (profiles, clubs, chat) **belong to a future, separate app.** Deft's only link to it is the exported Reading Profile (§12.3).

## Commits

Author every commit as `Samuel Worash Getaneh <samuelworash@gmail.com>`. The repo-local git config already sets this, so verify it with `git config user.email`. Use Conventional Commit messages (`feat(scanner): …`, `fix: …`, `build: …`).

Every commit is small and focused on one change, and leaves the build and tests green. Finish the code fully: no commented-out code, and no TODO without an issue link. `main` only receives merge commits from verified branches, plus planning and doc commits.
