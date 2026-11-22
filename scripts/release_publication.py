"""Stage only a verified, persistently signed tag build for GitHub Releases."""
import argparse
import gzip
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys

from release_sdk import resolve_sdk


def release_version(tag, properties):
    match = re.fullmatch(r'v(\d+)\.(\d+)\.(\d+)-(\d+)', tag)
    configured = re.search(r'^deft.version=(\S+)$', properties, re.MULTILINE)
    if not match or not configured or configured.group(1) != tag[1:]:
        raise ValueError('Release tag must match deft.version exactly.')
    a, b, c, d = map(int, match.groups())
    if not (0 <= b <= 99 and 0 <= c <= 99 and 1 <= d <= 99):
        raise ValueError('Release version components exceed versionCode bounds.')
    return f'{a}.{b}.{c}', a * 1000000 + b * 10000 + c * 100 + d


def validate_certificate(output, expected):
    actual = re.search(r'certificate SHA-256 digest: ([0-9a-f]{64})', output)
    if 'CN=Deft CI Only' in output or not actual or actual.group(1) != expected:
        raise ValueError('APK must use the permanent Deft release certificate.')


def stage(tag):
    root = Path(__file__).resolve().parent.parent
    version, code = release_version(tag, (root / 'gradle.properties').read_text())
    source = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip()
    tagged = subprocess.check_output(['git', 'rev-parse', tag + '^{commit}'], cwd=root, text=True).strip()
    if source != tagged:
        raise ValueError('Checked-out source does not match the release tag.')
    subprocess.run([sys.executable, str(root / 'scripts/verify-release.py')], check=True)
    sdk = resolve_sdk(root, os.environ)
    tools = sorted((sdk / 'build-tools').glob('*'), key=lambda p: tuple(int(n) for n in p.name.split('.') if n.isdigit()))
    apk = root / 'app/build/outputs/apk/release/app-release.apk'
    signer = tools[-1] / ('apksigner.bat' if os.name == 'nt' else 'apksigner')
    output = subprocess.check_output([str(signer), 'verify', '--print-certs', str(apk)], text=True)
    certificate = (root / 'scripts/release-certificate.sha256').read_text().strip()
    validate_certificate(output, certificate)
    destination = root / 'build/release-publication'
    destination.mkdir(parents=True, exist_ok=True)
    name = f'deft-{version}.apk'
    shutil.copyfile(apk, destination / name)
    digest = hashlib.sha256(apk.read_bytes()).hexdigest()
    (destination / (name + '.sha256')).write_text(f'{digest}  {name}\n', encoding='utf-8')
    mapping = root / 'app/build/outputs/mapping/release/mapping.txt'
    (destination / 'mapping.txt.gz').write_bytes(gzip.compress(mapping.read_bytes(), mtime=0))
    metadata = dict(tag=tag, version=version, version_code=code, source_commit=source,
                    apk=name, sha256=digest, certificate_sha256=certificate,
                    workflow_run=f"https://github.com/{os.environ['GITHUB_REPOSITORY']}/actions/runs/{os.environ['GITHUB_RUN_ID']}")
    (destination / 'release.json').write_text(json.dumps(metadata, indent=2) + '\n', encoding='utf-8')
    (destination / 'release-notes.md').write_text(
        f'Deft {version}, build {tag.rsplit("-", 1)[1]}.\n\n'
        'Built and published by GitHub Actions after checks and release packaging verification. '
        'The APK is R8-minified, resource-shrunk and signed with the persistent Deft release key. '
        'Install over your existing Deft app to preserve its data.\n\n'
        f'Build: {metadata["workflow_run"]}\n\nSource: `{source}`\n\n'
        'The checksum, R8 mapping and release metadata are attached for verification and crash diagnosis.\n', encoding='utf-8')
    print(json.dumps(metadata, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--tag', required=True)
    stage(parser.parse_args().tag)
