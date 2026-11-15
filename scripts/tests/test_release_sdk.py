import sys
from pathlib import Path
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from release_sdk import resolve_sdk


class ReleaseSdkTest(unittest.TestCase):
    def test_local_properties_without_environment(self):
        with tempfile.TemporaryDirectory(prefix="deft-sdk-test-") as temporary:
            root = Path(temporary)
            sdk = root / "installed-sdk"
            sdk.mkdir()
            value = sdk.as_posix().replace(":", "\\:")
            (root / "local.properties").write_text(f"sdk.dir={value}\n")
            self.assertEqual(sdk, resolve_sdk(root, {}))

    def test_sdk_environment_precedence(self):
        with tempfile.TemporaryDirectory(prefix="deft-sdk-test-") as temporary:
            root = Path(temporary)
            self.assertEqual(root, resolve_sdk(root, {"ANDROID_HOME": str(root)}))
            self.assertEqual(root, resolve_sdk(root, {"ANDROID_SDK_ROOT": str(root)}))

    def test_missing_sdk_is_not_current_directory(self):
        with tempfile.TemporaryDirectory(prefix="deft-sdk-test-") as temporary:
            with self.assertRaisesRegex(ValueError, "installed Android SDK"):
                resolve_sdk(Path(temporary), {})

    def test_missing_sdk_property_has_clear_error(self):
        with tempfile.TemporaryDirectory(prefix="deft-sdk-test-") as temporary:
            root = Path(temporary)
            (root / "local.properties").write_text("other=value\n")
            with self.assertRaisesRegex(ValueError, "installed Android SDK"):
                resolve_sdk(root, {})
