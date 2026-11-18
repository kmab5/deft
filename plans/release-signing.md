# Release signing

Set JAVA_HOME to JDK 17 and run `python scripts/setup-signing.py` once from the repository. The script creates a cryptographically random password and a persistent RSA-4096 PKCS12 identity in the current user's `.deft/signing` directory. Existing identity files are never overwritten. `keystore.properties` in the repository is ignored and points at that identity.

Back up the entire private signing directory securely, including its credentials. Losing it prevents update installs. Never commit or share it. Re-running setup restores a missing local properties file from that existing identity. A partial identity requires restoring its backup, never generating a replacement.

`./gradlew check assembleRelease` produces `app/build/outputs/apk/release/app-release.apk`. Release always enables R8 and resource shrinking. Verify with Android SDK `apksigner verify --print-certs`, compare the certificate fingerprint with the previous handover, and install using `adb install -r`. Application ID stays `com.kmab.deft`; the version code must never decrease.

CI may supply `DEFT_KEYSTORE_FILE`, `DEFT_KEYSTORE_PASSWORD`, `DEFT_KEY_ALIAS`, `DEFT_KEY_PASSWORD`. Without trusted secrets, `python scripts/setup-signing.py --ci` generates an ephemeral identity with alias `deft-ci-only`; these APKs are build checks only and must never be distributed. M10 adds trusted tag publication. No CI artifact upload publishes test-key APKs.
