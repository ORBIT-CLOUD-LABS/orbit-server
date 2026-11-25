#!/usr/bin/env python3
"""릴리스한 orbit-server 이미지를 orbit-gitops ota/base에 반영한다.

ota/base/kustomization.yaml의 images 항목 하나만 바꾼다. OTA Main·Sub overlay가 이 base를 쓰므로
커밋 하나로 두 클러스터가 같은 digest로 바뀐다.

    images:
      - name: ghcr.io/orbit-cloud-labs/orbit-server
        newTag: "1.2.4"
        digest: sha256:<64자리>

- 같은 버전·digest가 이미 있으면 아무것도 하지 않는다 (재실행).
- 더 높은 버전이 이미 있으면 덮어쓰지 않는다 (늦게 끝난 오래된 실행).
- 같은 버전에 다른 digest가 있으면 실패한다 (버전 태그는 옮기지 않는다).
- ota/base가 아직 없으면 경고만 하고 끝낸다.
- push가 동시 변경으로 거절되면 최신 브랜치에서 다시 시도한다.
"""
import argparse
import copy
import json
import os
from pathlib import Path
import re
import subprocess
import sys

import yaml

IMAGE = 'ghcr.io/orbit-cloud-labs/orbit-server'
TARGET = Path('ota/base/kustomization.yaml')
BRANCH = 'develop'
SHA = re.compile(r'[0-9a-f]{40}')
DIGEST = re.compile(r'sha256:[0-9a-f]{64}')
VERSION = re.compile(r'(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)')
RUN_URL = re.compile(r'https://github\.com/ORBIT-CLOUD-LABS/orbit-server/actions/runs/[0-9]+(?:/attempts/[0-9]+)?')


class UpdateError(RuntimeError):
    pass


def git(repo, *args, check=True):
    result = subprocess.run(['git', '-C', str(repo), *args], text=True, capture_output=True)
    if check and result.returncode:
        # Git 설정·인증 헤더·원격 URL이 로그에 나오지 않게 출력은 남기지 않는다.
        raise UpdateError(f'git {args[0]} 실패 (exit {result.returncode})')
    return result


def fields(node):
    if not isinstance(node, yaml.MappingNode):
        raise UpdateError('YAML mapping이 아닙니다')
    result = {}
    for key, value in node.value:
        if not isinstance(key, yaml.ScalarNode) or key.value in result:
            raise UpdateError('scalar가 아니거나 중복된 YAML 키가 있습니다')
        result[key.value] = value
    return result


def parse_version(text):
    match = VERSION.fullmatch(text or '')
    if not match:
        raise UpdateError(f'버전 형식이 아닙니다: {text!r}')
    return tuple(int(part) for part in match.groups())


def image_entry(text):
    document = yaml.safe_load(text)
    node = yaml.compose(text)
    # anchor·alias가 있으면 값 하나를 고쳐도 다른 곳까지 바뀔 수 있다.
    for token in yaml.scan(text):
        if isinstance(token, (yaml.AnchorToken, yaml.AliasToken)):
            raise UpdateError('YAML anchor·alias는 지원하지 않습니다')
    if not isinstance(document, dict) or document.get('kind', 'Kustomization') != 'Kustomization':
        raise UpdateError('Kustomization 파일이 아닙니다')
    images_node = fields(node).get('images')
    if not isinstance(images_node, yaml.SequenceNode):
        raise UpdateError('images 목록이 없습니다')
    matches = []
    for index, (entry, entry_node) in enumerate(zip(document['images'], images_node.value)):
        if not isinstance(entry, dict) or IMAGE not in (entry.get('name'), entry.get('newName')):
            continue
        entry_fields = fields(entry_node)
        for key in ('newTag', 'digest'):
            if not isinstance(entry_fields.get(key), yaml.ScalarNode):
                raise UpdateError(f'orbit-server images 항목에 {key}가 없습니다')
        matches.append((index, entry, entry_fields))
    if len(matches) != 1:
        raise UpdateError('orbit-server images 항목이 정확히 하나여야 합니다')
    return document, matches[0]


def target_path(repo):
    path = repo / TARGET
    if not path.exists():
        return None
    if not path.is_file() or path.is_symlink() or path.resolve() != repo.resolve() / TARGET:
        raise UpdateError('ota/base/kustomization.yaml이 checkout 안의 일반 파일이 아닙니다')
    return path


def replace_values(text, entry_fields, version, digest):
    edits = sorted(
        [(entry_fields['newTag'], json.dumps(version)), (entry_fields['digest'], digest)],
        key=lambda item: item[0].start_mark.index,
        reverse=True,
    )
    for node, value in edits:
        text = text[:node.start_mark.index] + value + text[node.end_mark.index:]
    return text


def update(repo, version, source_sha, digest, run_url, attempts=3):
    if not VERSION.fullmatch(version) or not SHA.fullmatch(source_sha) or not DIGEST.fullmatch(digest):
        raise UpdateError('버전, 소스 SHA, digest 형식이 잘못됐습니다')
    if not RUN_URL.fullmatch(run_url):
        raise UpdateError('Actions 실행 링크 형식이 잘못됐습니다')
    if git(repo, 'status', '--porcelain').stdout:
        raise UpdateError('orbit-gitops checkout이 깨끗하지 않습니다')
    wanted = parse_version(version)
    for _ in range(attempts):
        git(repo, 'fetch', '--no-tags', 'origin', f'refs/heads/{BRANCH}')
        base = git(repo, 'rev-parse', 'FETCH_HEAD').stdout.strip()
        # 처음에 깨끗했던 Actions 전용 checkout만 다시 맞춘다.
        git(repo, 'reset', '--hard', base)
        path = target_path(repo)
        if path is None:
            return 'skipped: orbit-gitops에 ota/base/kustomization.yaml이 아직 없음'
        text = path.read_text(encoding='utf-8')
        document, (index, entry, entry_fields) = image_entry(text)
        current = parse_version(str(entry['newTag']))
        if current == wanted:
            if entry['digest'] == digest:
                return 'no-op: 이미 같은 버전과 digest'
            raise UpdateError('같은 버전에 다른 digest가 있습니다. 버전 태그를 옮기지 않습니다')
        if current > wanted:
            return f'skipped: 더 높은 버전 {entry["newTag"]}이 이미 반영됨'
        changed = replace_values(text, entry_fields, version, digest)
        expected = copy.deepcopy(document)
        expected['images'][index]['newTag'] = version
        expected['images'][index]['digest'] = digest
        if yaml.safe_load(changed) != expected:
            raise UpdateError('이미지 항목 밖까지 바뀌었습니다')
        path.write_text(changed, encoding='utf-8')
        git(repo, 'diff', '--check')
        if git(repo, 'diff', '--name-only').stdout.splitlines() != [str(TARGET)]:
            raise UpdateError('예상하지 않은 파일이 바뀌었습니다')
        git(repo, 'add', '--', str(TARGET))
        git(repo, 'commit', '-m', f'chore: orbit-server {version} 배포', '-m',
            f'Version: {version}\nSource-Repository: ORBIT-CLOUD-LABS/orbit-server\n'
            f'Source-SHA: {source_sha}\nImage-Digest: {digest}\nBuild-Run: {run_url}')
        # 일반 push가 compare-and-swap 역할을 한다. 그 사이 브랜치가 바뀌면 거절된다.
        result = git(repo, 'push', 'origin', f'HEAD:refs/heads/{BRANCH}', check=False)
        if result.returncode == 0:
            return 'updated: ' + git(repo, 'rev-parse', 'HEAD').stdout.strip()
        git(repo, 'fetch', '--no-tags', 'origin', f'refs/heads/{BRANCH}')
        if git(repo, 'rev-parse', 'FETCH_HEAD').stdout.strip() == base:
            raise UpdateError('브랜치 변경 없이 push가 거절됐습니다. App 권한과 ruleset 우회 설정을 확인하세요')
    raise UpdateError(f'{attempts}번 모두 push 중 orbit-gitops가 바뀌었습니다')


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--gitops', type=Path, required=True)
    args = parser.parse_args()
    try:
        result = update(args.gitops, os.environ['VERSION'], os.environ['SOURCE_SHA'],
                        os.environ['IMAGE_DIGEST'], os.environ['BUILD_RUN_URL'])
    except (UpdateError, KeyError, ValueError, OSError, yaml.YAMLError) as error:
        print(f'orbit-gitops 갱신 실패: {error}', file=sys.stderr)
        return 1
    print(result)
    if result.startswith('skipped:'):
        print(f'::warning::{result}')
    if os.environ.get('GITHUB_STEP_SUMMARY'):
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a', encoding='utf-8') as summary:
            summary.write(f'orbit-gitops 갱신: {result}\n')
    return 0


if __name__ == '__main__':
    sys.exit(main())
