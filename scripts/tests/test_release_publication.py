import os
from pathlib import Path
import subprocess
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from release_publication import release_version, validate_certificate


class ReleasePublicationTest(unittest.TestCase):
    def test_tag_must_match_gradle_including_build_number(self):
        self.assertEqual(('0.4.3', 40302), release_version('v0.4.3-2', 'deft.version=0.4.3-2\n'))
        for tag in ('v0.4.3-1', 'main', 'v0.4.3-2;bad', 'v0.4.3-100'):
            with self.subTest(tag=tag), self.assertRaises(ValueError):
                release_version(tag, 'deft.version=0.4.3-2\n')

    def test_publication_rejects_unrelated_and_ephemeral_certificates(self):
        expected = 'a' * 64
        validate_certificate('Signer #1 certificate SHA-256 digest: ' + expected, expected)
        for output in ('Signer #1 certificate SHA-256 digest: ' + 'b' * 64,
                       'CN=Deft CI Only\ncertificate SHA-256 digest: ' + expected, ''):
            with self.subTest(output=output), self.assertRaises(ValueError):
                validate_certificate(output, expected)

    def test_release_requires_all_signing_secrets_before_creating_any_key(self):
        env = {key: value for key, value in os.environ.items() if not key.startswith('DEFT_')}
        result = subprocess.run([sys.executable, str(Path(__file__).resolve().parents[1] / 'ci-signing.py'),
                                 '--require-release'], env=env, capture_output=True, text=True)
        self.assertNotEqual(0, result.returncode)
        self.assertIn('Persistent release signing secrets are required', result.stderr)
