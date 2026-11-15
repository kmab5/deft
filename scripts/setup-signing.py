"""Create Deft's permanent private signing identity once. Never replaces an existing key."""
import argparse
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import tempfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--ci", action="store_true", help="Create an ephemeral CI-only identity")
args = parser.parse_args()
root = Path(__file__).resolve().parent.parent
private = Path(tempfile.mkdtemp(prefix="deft-ci-")) if args.ci else Path.home() / ".deft" / "signing"
private.mkdir(parents=True, exist_ok=True)
key = private / ("ci-only.p12" if args.ci else "deft-release.p12")
credentials = private / "keystore.properties"
local = root / "keystore.properties"
if key.exists() or credentials.exists():
    if not key.is_file() or not credentials.is_file():
        raise SystemExit("Incomplete signing identity. Restore its backup; no files were replaced.")
    saved = dict(line.split("=", 1) for line in credentials.read_text(encoding="utf-8").splitlines() if "=" in line)
    if Path(saved.get("storeFile", "")).resolve() != key.resolve():
        raise SystemExit("Signing configuration does not match the persistent key. Restore its backup.")
    if local.exists() and local.read_bytes() != credentials.read_bytes():
        raise SystemExit("Local signing configuration differs from the persistent identity. No files were replaced.")
    env = os.environ.copy()
    env["DEFT_EXISTING_PASSWORD"] = saved.get("storePassword", "")
    java = Path(env["JAVA_HOME"]) / "bin" / ("keytool.exe" if os.name == "nt" else "keytool")
    result = subprocess.run([str(java), "-list", "-keystore", str(key), "-storepass:env", "DEFT_EXISTING_PASSWORD",
        "-alias", saved.get("keyAlias", "")], env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    if result.returncode:
        raise SystemExit("Persistent signing identity cannot be opened. Restore its backup; no replacement was generated.")
    if not local.exists():
        shutil.copyfile(credentials, local)
    print("Existing signing identity preserved.")
    raise SystemExit(0)
if local.exists():
    raise SystemExit("Local signing configuration already exists; refusing to replace it.")
password = secrets.token_urlsafe(48)
env = os.environ.copy()
env["DEFT_NEW_KEY_PASSWORD"] = password
java = Path(env["JAVA_HOME"]) / "bin" / ("keytool.exe" if os.name == "nt" else "keytool")
subprocess.run([str(java), "-genkeypair", "-keystore", str(key), "-storetype", "PKCS12",
    "-storepass:env", "DEFT_NEW_KEY_PASSWORD", "-keypass:env", "DEFT_NEW_KEY_PASSWORD",
    "-alias", "deft-ci-only" if args.ci else "deft-release", "-keyalg", "RSA", "-keysize", "4096",
    "-validity", "36500", "-dname", "CN=Deft CI Only" if args.ci else "CN=Deft, O=Samuel Worash Getaneh"],
    env=env, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
text = (f"storeFile={key.as_posix()}\nstorePassword={password}\nkeyAlias="
    + ("deft-ci-only" if args.ci else "deft-release") + f"\nkeyPassword={password}\n")
with credentials.open("x", encoding="utf-8") as out:
    out.write(text)
shutil.copyfile(credentials, local)
if os.name != "nt":
    private.chmod(0o700)
    key.chmod(0o600)
    credentials.chmod(0o600)
    local.chmod(0o600)
print("CI-only signing identity created." if args.ci else "Persistent release signing identity created. Back up the private signing directory securely.")
