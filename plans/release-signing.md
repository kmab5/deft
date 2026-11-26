# Release signing

Set JAVA_HOME to JDK 17 and run `python scripts/setup-signing.py` once from the repository. The script creates a cryptographically random password and a persistent RSA-4096 PKCS12 identity in the current user's `.deft/signing` directory. Existing identity files are never overwritten. `keystore.properties` in the repository is ignored and points at that identity.

Back up the entire private signing directory securely, including its credentials. Losing it prevents update installs. Never commit or share it. Re-running setup restores a missing local properties file from that existing identity. A partial identity requires restoring its backup, never generating a replacement.

`./gradlew check assembleRelease` produces `app/build/outputs/apk/release/app-release.apk`. Release always enables R8 and resource shrinking. Verify with Android SDK `apksigner verify --print-certs`, compare the certificate fingerprint with the previous handover, and install using `adb install -r`. Application ID stays `com.kmab.deft`; the version code must never decrease.

GitHub Actions builds and checks every push. Branch builds use an ephemeral `deft-ci-only` identity and never publish APKs. Version tags (`vA.B.C-D`) require four encrypted repository secrets: `DEFT_KEYSTORE_BASE64` (the existing PKCS12 file, base64-encoded), `DEFT_KEYSTORE_PASSWORD`, `DEFT_KEY_ALIAS` and `DEFT_KEY_PASSWORD`. They must describe the same persistent key used for local releases. Missing or incomplete secrets fail publication; no replacement key is generated for a release.

CI runs checks separately from release packaging and limits release packaging to one Gradle worker. Version tags build the universal APK and four standalone APKs for arm64-v8a, armeabi-v7a, x86 and x86_64. CI checks the native libraries in each APK, verifies the signature against the pinned permanent certificate, and checks the version, offline manifest and R8 mapping before upload.

The Actions release artifact and GitHub Release assets contain only those five APKs. The release description includes installation guidance, commit changes since the previous app version and a comparison link. Tags on main publish formal releases, including 0.x versions. Unmerged review tags publish prereleases.

To retry publication, rerun the tag's Actions run or manually dispatch the **build** workflow at that tag. Existing release assets are replaced only after the build and verification pass again. Keep tags immutable; CI/build-only revisions increment D (for example `v0.4.3-2`) while retaining app version `0.4.3` and APK name `deft-0.4.3.apk`.
