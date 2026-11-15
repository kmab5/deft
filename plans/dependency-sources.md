# Foundation dependency sources

Pinned versions are defined in `gradle/libs.versions.toml`, the build conventions and module Gradle files. Build tooling runs on the installed JDK 17 and Gradle 8.13; Android API 36 is the installed stable Android 16 SDK. No SDK/JDK/Gradle installation is part of setup. Missing Maven libraries may be resolved with the author's approval (2026-11-15).

- AGP 8.13.2 and Kotlin 2.2.21 are a compatible pinned combination using the installed Gradle/JDK: https://developer.android.com/build/releases/past-releases/agp-8-13-0-release-notes
- Compose BOM 2025.10.00: https://developer.android.com/develop/ui/compose/bom and Google's Maven metadata `https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/maven-metadata.xml`.
- Android API 36: https://developer.android.com/about/versions/16/setup-sdk
- AndroidX / Compose / Room / Navigation / Activity are Apache-2.0: https://android.googlesource.com/platform/frameworks/support/+/androidx-main/LICENSE.txt
- Hilt / Dagger (Apache-2.0): https://github.com/google/dagger
- Coil 3.3.0 core and Compose core (Apache-2.0), verified from its published POM: https://repo.maven.apache.org/maven2/io/coil-kt/coil3/coil-core/3.3.0/coil-core-3.3.0.pom . No Coil network module is included; image requests accept app-private files only.
- Kotlin / KSP (Apache-2.0): https://github.com/JetBrains/kotlin and https://github.com/google/ksp
- Robolectric (MIT), JUnit (EPL-1.0), ktlint (MIT), detekt (Apache-2.0): JVM-only build/test dependencies, never packaged into the app.
- Space Grotesk: https://github.com/floriankarsten/space-grotesk (SIL OFL-1.1). Bundled 400/500/600 subsets.
- Space Mono: https://github.com/googlefonts/spacemono (SIL OFL-1.1). Bundled 400/700 subsets.
- App font source is Google Fonts' original TTF resources; subsets keep existing Latin, Latin Extended, Greek, Cyrillic and punctuation glyphs, including Android system fallback where upstream Space fonts lack glyphs (notably full Greek/Cyrillic coverage). OFL notices are packaged in assets/licenses. No reader fonts ship until M2.
- Original outline icon geometry follows Lucide (ISC): https://github.com/lucide-icons/lucide/blob/main/LICENSE . Only used icons are included, with square caps/joins.
- Palette source: https://github.com/kmab5/kmab-brand/blob/main/design-tokens.css . Full purple/green/amber scales are preserved; semantic paper accessibility adjustments are logged in the spec.

No app dependency provides Google Play Services, Firebase, analytics, networking, or proprietary binaries.
- XMLPull API 1.1.3.1 (Public Domain): https://repo.maven.apache.org/maven2/xmlpull/xmlpull/1.1.3.1/xmlpull-1.1.3.1.pom and https://www.xmlpull.org/ . Compile-only in the pure-JVM EPUB module; Android supplies the public factory/API and tokenizer at runtime. Namespace-aware token parsing rejects DTD declarations and unresolved entities before metadata assembly; it never resolves external XML resources.
- kXML2 2.3.0: BSD-style license for org.kxml2 and Public Domain XmlPull API, verified from the published upstream POM: https://repo.maven.apache.org/maven2/net/sf/kxml/kxml2/2.3.0/kxml2-2.3.0.pom . JVM test runtime only, supplying the XMLPull factory implementation outside Android. Neither the full jar nor kxml2-min is packaged into the app: both published artifacts contain copies of Android's XMLPull API classes, which break R8's library/program class relationship. The full jar also has XMLPull's legacy comma-separated factory metadata, interpreted as an invalid Java service entry by R8.
