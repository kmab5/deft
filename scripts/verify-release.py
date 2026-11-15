"""Verify the actual release APK against the configured public certificate and offline rules."""
import argparse
import hashlib
import os
from pathlib import Path
import re
import subprocess
from release_sdk import resolve_sdk

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--allow-ci", action="store_true", help="Permit an explicitly CI-only certificate inside CI")
args = parser.parse_args()
root = Path(__file__).resolve().parent.parent
try:
    sdk = resolve_sdk(root, os.environ)
except ValueError as error:
    raise SystemExit(str(error)) from error
suffix = ".bat" if os.name == "nt" else ""
tools = sorted((sdk / "build-tools").glob("*"), key=lambda p: tuple(int(n) for n in p.name.split(".") if n.isdigit()))
if not tools:
    raise SystemExit("Android build tools are missing.")
apk = root / "app/build/outputs/apk/release/app-release.apk"
mapping = root / "app/build/outputs/mapping/release/mapping.txt"
if not apk.is_file() or not mapping.is_file() or not mapping.stat().st_size:
    raise SystemExit("Signed APK or R8 mapping is missing; run assembleRelease first.")
verified = subprocess.run([str(tools[-1] / ("apksigner" + suffix)), "verify", "--print-certs", str(apk)], capture_output=True, text=True, check=True).stdout
ci_only = "CN=Deft CI Only" in verified
if ci_only and not (args.allow_ci and os.environ.get("CI") == "true"):
    raise SystemExit("CI-only APK must never be handed over or distributed.")
values = {}
props = root / "keystore.properties"
if props.is_file():
    def unescape(value):
        def replace(match):
            if match.group(1):
                return chr(int(match.group(1), 16))
            char = match.group(2)
            return {"n": "\n", "r": "\r", "t": "\t", "f": "\f"}.get(char, char)
        return re.sub(r"\\u([0-9a-fA-F]{4})|\\(.)", replace, value)
    values = {name: unescape(value) for name, value in
        (line.split("=", 1) for line in props.read_text().splitlines() if "=" in line)}
def setting(name, env):
    return os.environ.get(env) or values.get(name) or ""
env = os.environ.copy()
env["DEFT_VERIFY_PASSWORD"] = setting("storePassword", "DEFT_KEYSTORE_PASSWORD")
keytool = Path(env["JAVA_HOME"]) / "bin" / ("keytool.exe" if os.name == "nt" else "keytool")
cert = subprocess.run([str(keytool), "-exportcert", "-keystore", setting("storeFile", "DEFT_KEYSTORE_FILE"),
    "-alias", setting("keyAlias", "DEFT_KEY_ALIAS"), "-storepass:env", "DEFT_VERIFY_PASSWORD"],
    env=env, capture_output=True, check=True).stdout
expected = hashlib.sha256(cert).hexdigest()
actual = re.search(r"certificate SHA-256 digest: ([0-9a-f]+)", verified)
if not actual or actual.group(1) != expected:
    raise SystemExit("APK certificate differs from the configured persistent signing identity.")
aapt = tools[-1] / ("aapt.exe" if os.name == "nt" else "aapt")
manifest = subprocess.run([str(aapt), "dump", "badging", str(apk)], capture_output=True, text=True, check=True).stdout
if "android.permission.INTERNET" in manifest:
    raise SystemExit("Release APK unexpectedly requests INTERNET.")
version = re.search(r"deft.version=(\d+)\.(\d+)\.(\d+)-(\d+)", (root / "gradle.properties").read_text())
a, b, c, d = map(int, version.groups())
code = a * 1000000 + b * 10000 + c * 100 + d
if f"name='com.kmab.deft' versionCode='{code}' versionName='{a}.{b}.{c}'" not in manifest:
    raise SystemExit("APK application identity or version differs from Gradle configuration.")
if not ci_only:
    previous = Path(setting("storeFile", "DEFT_KEYSTORE_FILE")).parent / "last-verified-version-code"
    if previous.exists() and code < int(previous.read_text().strip()):
        raise SystemExit("Release versionCode decreased; update deft.version before handover.")
    previous.write_text(str(code), encoding="ascii")
print("Release verified: configured certificate, application identity/version, no INTERNET, R8 mapping present.")
