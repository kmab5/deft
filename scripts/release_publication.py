"""Stage only a verified, persistently signed tag build for GitHub Releases."""
import argparse
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys
import zipfile


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
    version, _ = release_version(tag, (root / 'gradle.properties').read_text())
    source = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip()
    tagged = subprocess.check_output(['git', 'rev-parse', tag + '^{commit}'], cwd=root, text=True).strip()
    if source != tagged:
        raise ValueError('Checked-out source does not match the release tag.')
    output = root / 'app/build/outputs/apk/release'
    destination = root / 'build/release-publication'
    destination.mkdir(parents=True, exist_ok=True)
    for stale in destination.glob('*.apk'):
        stale.unlink()
    expected = set(('arm64-v8a', 'armeabi-v7a', 'x86', 'x86_64'))
    seen = set()
    for item in json.loads((output / 'output-metadata.json').read_text())['elements']:
        filters = item['filters']
        abi = filters[0]['value'] if len(filters) == 1 and filters[0]['filterType'] == 'ABI' else None
        if (filters and abi not in expected) or abi in seen:
            raise ValueError(f'Unexpected release output filters: {filters}')
        apk = output / item['outputFile']
        with zipfile.ZipFile(apk) as archive:
            packaged = {name.split('/')[1] for name in archive.namelist() if name.startswith('lib/') and name.endswith('.so')}
        if packaged != ({abi} if abi else expected):
            raise ValueError(f'{apk.name} has unexpected native architectures: {packaged}')
        subprocess.run([sys.executable, str(root / 'scripts/verify-release.py'), '--pinned-certificate',
                        '--apk', str(apk)], check=True)
        suffix = f'-{abi}' if abi else ''
        shutil.copyfile(apk, destination / f'deft-{version}{suffix}.apk')
        seen.add(abi)
    if seen != expected | {None}:
        raise ValueError('Release requires a universal APK and all four architecture APKs.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--tag', required=True)
    stage(parser.parse_args().tag)
