"""Configure trusted CI signing, or an explicit ephemeral CI identity. Never prints secrets."""
import base64
import os
from pathlib import Path
import subprocess
import sys
import tempfile

root = Path(__file__).resolve().parent.parent
names = ["DEFT_KEYSTORE_BASE64", "DEFT_KEYSTORE_PASSWORD", "DEFT_KEY_ALIAS", "DEFT_KEY_PASSWORD"]
values = [os.environ.get(name) for name in names]
if any(values) and not all(values):
    raise SystemExit("Signing secrets are incomplete. No replacement identity generated.")
if all(values):
    private = Path(tempfile.mkdtemp(prefix="deft-signing-"))
    private.chmod(0o700)
    key = private / "release.p12"
    key.write_bytes(base64.b64decode(values[0], validate=True))
    key.chmod(0o600)
    # Escape Java properties metacharacters in supplied credentials.
    def escape(value):
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r").replace("=", "\\=").replace(":", "\\:").replace(" ", "\\ ")
    properties = {"storeFile": key.as_posix(), "storePassword": values[1], "keyAlias": values[2], "keyPassword": values[3]}
    (root / "keystore.properties").write_text("".join(f"{k}={escape(v)}\n" for k, v in properties.items()), encoding="utf-8")
    (root / "keystore.properties").chmod(0o600)
else:
    subprocess.run([sys.executable, str(root / "scripts/setup-signing.py"), "--ci"], check=True)
