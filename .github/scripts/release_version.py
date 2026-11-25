#!/usr/bin/env python3
"""머지된 PR의 릴리스 라벨과 기존 v* 태그로 다음 릴리스 버전을 계산한다.

- 라벨 release:patch|minor|major 중 0개면 빈 줄을 출력한다 (릴리스하지 않음).
- 2개 이상이면 실패한다 (PR 검사 release-label이 막지만 한 번 더 확인).
- 첫 릴리스는 라벨과 관계없이 1.0.0이다.
- 출력은 v를 뺀 버전(1.2.4)이다. Git 태그는 v1.2.4, 이미지 태그는 1.2.4로 쓴다.
"""
import argparse
import json
import re
import sys

BUMPS = ('patch', 'minor', 'major')
LABEL = re.compile(r'release:(patch|minor|major)')
TAG = re.compile(r'v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)')
FIRST = (1, 0, 0)


class VersionError(RuntimeError):
    pass


def bump_from_labels(labels):
    bumps = sorted({match.group(1) for label in labels if (match := LABEL.fullmatch(label))})
    if len(bumps) > 1:
        raise VersionError('릴리스 라벨이 둘 이상입니다: ' + ', '.join('release:' + bump for bump in bumps))
    return bumps[0] if bumps else None


def latest_version(tags):
    versions = [tuple(int(part) for part in match.groups()) for tag in tags if (match := TAG.fullmatch(tag.strip()))]
    return max(versions, default=None)


def next_version(tags, bump):
    if bump not in BUMPS:
        raise VersionError(f'알 수 없는 증가 단위: {bump}')
    latest = latest_version(tags)
    if latest is None:
        return FIRST
    major, minor, patch = latest
    if bump == 'major':
        return major + 1, 0, 0
    if bump == 'minor':
        return major, minor + 1, 0
    return major, minor, patch + 1


def format_version(version):
    return '.'.join(str(part) for part in version)


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--labels-json', required=True, help='PR 라벨 이름의 JSON 배열')
    parser.add_argument('--tags-file', required=True, help='git tag --list 출력 파일')
    args = parser.parse_args(argv)
    try:
        labels = json.loads(args.labels_json)
        if not isinstance(labels, list) or not all(isinstance(label, str) for label in labels):
            raise VersionError('--labels-json은 문자열 배열이어야 합니다')
        with open(args.tags_file, encoding='utf-8') as stream:
            tags = stream.read().splitlines()
        bump = bump_from_labels(labels)
        print(format_version(next_version(tags, bump)) if bump else '')
    except (VersionError, ValueError, OSError) as error:
        print(f'버전 계산 실패: {error}', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
