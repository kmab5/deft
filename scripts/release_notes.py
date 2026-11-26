"""Write release notes from the changes since the preceding app version on main."""
from pathlib import Path
import subprocess
import sys

from release_publication import release_version


def git(*args):
    return subprocess.check_output(['git', *args], text=True, encoding='utf8').strip()


def release_notes(tag):
    root = Path(__file__).resolve().parents[1]
    version, _ = release_version(tag, (root / 'gradle.properties').read_text())
    previous = git('describe', '--tags', '--always', '--abbrev=0', '--first-parent',
                   '--match', 'v[0-9]*.[0-9]*.[0-9]*-[0-9]*', '--exclude', f'v{version}-*', tag)
    span = f'{previous}..{tag}' if previous.startswith('v') else tag
    changes = git('log', '--no-merges', '--format=%s (%h)', span).splitlines()
    return '\n'.join([
        f'## Deft {version} · build {tag.rsplit("-", 1)[1]}', '',
        f'Install **deft-{version}.apk** for automatic compatibility, or choose the APK matching '
        'your device architecture. All five APKs use the same Deft signing key and can update an existing installation.',
        '', '## Changelog', '', *(f'- {change}' for change in changes),
        '', *([f'[Full comparison](https://github.com/kmab5/deft/compare/{previous}...{tag})']
              if previous.startswith('v') else []),
    ])


if __name__ == '__main__':
    print(release_notes(sys.argv[1]))
