# Release signing

Set JAVA_HOME to JDK 17 and run `python scripts/setup-signing.py` once from the repository. The script creates a cryptographically random password and a persistent RSA-4096 PKCS12 identity in the current user's `.deft/signing` directory. Existing identity files are never overwritten. `keystore.properties` in the repository is ignored and points at that identity.

Back up the entire private signing directory securely, including its credentials. Losing it prevents update installs. Never commit or share it. Re-running setup restores a missing local properties file from that existing identity. A partial identity requires restoring its backup, never generating a replacement.

`./gradlew check assembleRelease` produces `app/build/outputs/apk/release/app-release.apk`. Release always enables R8 and resource shrinking. Verify with Android SDK `apksigner verify --print-certs`, compare the certificate fingerprint with the previous handover, and install using `adb install -r`. Application ID stays `com.kmab.deft`; the version code must never decrease.

GitHub Actions builds and checks every push. Branch builds use an ephemeral `deft-ci-only` identity and never publish APKs. Version tags (`vA.B.C-D`) require four encrypted repository secrets: `DEFT_KEYSTORE_BASE64` (the existing PKCS12 file, base64-encoded), `DEFT_KEYSTORE_PASSWORD`, `DEFT_KEY_ALIAS` and `DEFT_KEY_PASSWORD`. They must describe the same persistent key used for local releases. Missing or incomplete secrets fail publication; no replacement key is generated for a release.

After `check assembleDebug assembleRelease` and Python tooling tests pass, a tag build verifies the APK identity, version, offline manifest, R8 mapping and permanent certificate against `scripts/release-certificate.sha256`. The tag must match `deft.version` and the checked-out commit. Only that verified bundle is uploaded to Actions artifacts. A separate job with `contents: write` creates or updates the tag's GitHub Release and attaches `deft-A.B.C.apk`, its SHA-256 checksum, compressed R8 mapping and `release.json` (source commit, version code, certificate and workflow URL). Major-zero releases are prereleases.

To retry publication, rerun the tag's Actions run or manually dispatch the **build** workflow at that tag. Existing release assets are replaced only after the build and verification pass again. Keep tags immutable; CI/build-only revisions increment D (for example `v0.4.3-2`) while retaining app version `0.4.3` and APK name `deft-0.4.3.apk`.
