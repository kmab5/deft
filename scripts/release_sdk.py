"""Locate the existing SDK without installing a toolchain."""
from pathlib import Path


def resolve_sdk(root, environ):
    for name in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        value = environ.get(name)
        if value and Path(value).is_dir():
            return Path(value)
    properties = root / "local.properties"
    if properties.is_file():
        for line in properties.read_text().splitlines():
            name, separator, value = line.partition("=")
            if separator and name.strip() == "sdk.dir" and value.strip():
                sdk = Path(value.strip().replace("\\:", ":").replace("\\\\", "\\"))
                if sdk.is_dir():
                    return sdk
    raise ValueError("Set ANDROID_HOME or sdk.dir to the installed Android SDK.")
