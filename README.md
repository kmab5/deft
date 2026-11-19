# deft - read like you mean it!

Deft is an offline Android ebook reader with a reading tracker built in. Open a book and the clock starts; finish it and it's recorded. No accounts, no cloud, no forgotten timers.

- **Reader:** EPUB, MOBI, AZW, AZW3, FB2, TXT, PDF, DjVu (`.djvu`/`.djv`), CBZ. Themes, fonts, size, line spacing, margins, alignment, bookmarks, table of contents, highlights and notes.
- **Library:** built from the folders you choose. Your own categories (series, TBR, book clubs, anything), automatic authors, five reading states, half-star ratings and favorites.
- **Tracker:** accurate reading time, sessions, streaks, goals, per-book/category/author stats, and a year in review.
- **Portable:** optional edition matching, export to Goodreads/StoryGraph/Hardcover CSV, and local zip backups.

Status: M1 is merged; the M2 EPUB reader is in device testing at 0.2.2. The feature list above describes the full roadmap; tracking, ratings, notes and additional reading formats remain in later milestones. See [`plans/`](plans/) for the design and roadmap, and the [M2 test checklist](plans/m2-test-checklist.md) for implemented behavior and device retests.

Distribution (planned): GitHub Releases and F-Droid.

## Building

Use the installed Android SDK (API 36), JDK 17 for Gradle/production compilation, and JDK 21 for API 36 Robolectric tests. Set `ANDROID_HOME` (or an ignored `local.properties` with `sdk.dir`) and `JAVA_HOME`; set `JAVA_21_HOME` when the test JDK is outside standard installation paths. The wrapper uses Gradle 8.13.

```sh
python scripts/setup-signing.py
./gradlew check assembleRelease
python scripts/verify-release.py
```

The first command creates the persistent private release identity once; subsequent runs preserve it. Back it up securely. See [release signing](plans/release-signing.md) for local and CI setup. The signed, R8-minified and resource-shrunk APK is `app/build/outputs/apk/release/app-release.apk`.

The foundation app has no `INTERNET` permission. `checkNetworkIsolation` rejects HTTP-client dependencies outside the future explicit matching module. The planned edition-matching feature will be the only user-requested network path.

Component screenshot tests compare ink/paper galleries, dialogs and sheets with committed PNGs. To intentionally update baselines, run `./gradlew :core:designsystem:testDebugUnitTest -Pdeft.recordScreenshots=true` (quote the dotted `-P` argument in PowerShell), then inspect every changed image. Normal `check` never updates baselines.

## License

[GPL-3.0](LICENSE)

### made with love 💖 [kmab](https://kmab5.github.io)
