# deft - read like you mean it!

Deft is an offline Android ebook reader with a reading tracker built in. Open a book and the clock starts; finish it and it's recorded. No accounts, no cloud, no forgotten timers.

- **Reader:** EPUB, MOBI, AZW, AZW3, FB2, TXT, PDF, DjVu (`.djvu`/`.djv`), CBZ. Themes, fonts, size, line spacing, margins, alignment, bookmarks, table of contents, highlights and notes.
- **Library:** built from the folders you choose. Your own categories (series, TBR, book clubs, anything), automatic authors, five reading states, half-star ratings and favorites.
- **Tracker:** accurate reading time, sessions, streaks, goals, per-book/category/author stats, and a year in review.
- **Portable:** optional edition matching, export to Goodreads/StoryGraph/Hardcover CSV, and local zip backups.

Status: M1–M3 are merged (`v0.3.3-1`). M3 adds automatic sessions, read-through states, ratings, favorites, private reviews and the finish flow, with idle-film and selection-feedback refinements in 0.3.3. M4 organization is implemented on `feat/m4-organization` as `0.4.2-1`, awaiting device review. The feature list above describes the full roadmap; global stats/goals, highlights/notes and additional readers remain in later milestones. See [`plans/`](plans/) and the [M4 test checklist](plans/m4-test-checklist.md).

Distribution: [GitHub Releases](https://github.com/kmab5/deft/releases). F-Droid is planned.

## Building

Use the installed Android SDK (API 36), JDK 17 for Gradle/production compilation, and JDK 21 for API 36 Robolectric tests. Set `ANDROID_HOME` (or an ignored `local.properties` with `sdk.dir`) and `JAVA_HOME`; set `JAVA_21_HOME` when the test JDK is outside standard installation paths. The wrapper uses Gradle 8.13.

```sh
python scripts/setup-signing.py
./gradlew check assembleRelease
python scripts/verify-release.py
```

The first command creates the persistent private release identity once; subsequent runs preserve it. Back it up securely. See [release signing](plans/release-signing.md) for local and CI setup. The signed, R8-minified and resource-shrunk APK is `app/build/outputs/apk/release/app-release.apk`.

After `check`, real-browser EPUB regressions can run with the already-installed Chromium and Node 22+ (no downloads):

```sh
node format/readium/src/test/browser/verify-reader.cjs
node scripts/tests/test_continuous_reader.cjs
```

The first consumes the real publication/font fixtures exported by the unit tests; the second verifies continuous chapter scrolling and resource isolation.

The foundation app has no `INTERNET` permission. `checkNetworkIsolation` rejects HTTP-client dependencies outside the future explicit matching module. The planned edition-matching feature will be the only user-requested network path.

Component screenshot tests compare ink/paper galleries, dialogs and sheets with committed PNGs. To intentionally update baselines, run `./gradlew :core:designsystem:testDebugUnitTest -Pdeft.recordScreenshots=true` (quote the dotted `-P` argument in PowerShell), then inspect every changed image. Normal `check` never updates baselines.

## License

[GPL-3.0](LICENSE)

### made with love 💖 [kmab](https://kmab5.github.io)
