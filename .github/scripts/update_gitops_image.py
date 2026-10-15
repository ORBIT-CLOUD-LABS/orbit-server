#!/usr/bin/env python3
"""Update only the Phase1 server Deployment image after a successful publish."""
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
TARGET = Path('workloads/orbit-server/deployment.yaml')
SHA = re.compile(r'[0-9a-f]{40}')
DIGEST = re.compile(r'sha256:[0-9a-f]{64}')
IMAGE_REF = re.compile(re.escape(IMAGE) + r':([0-9a-f]{40})(?:@(sha256:[0-9a-f]{64}))?')
RUN_URL = re.compile(r'https://github\.com/ORBIT-CLOUD-LABS/orbit-server/actions/runs/[0-9]+(?:/attempts/[0-9]+)?')


class UpdateError(RuntimeError):
    pass


def git(repo, *args, check=True):
    result = subprocess.run(['git', '-C', str(repo), *args], text=True, capture_output=True)
    if check and result.returncode:
        # Do not echo Git configuration, authentication headers, or remote URLs.
        raise UpdateError(f'git {args[0]} failed (exit {result.returncode})')
    return result


def fields(node):
    if not isinstance(node, yaml.MappingNode):
        raise UpdateError('Expected a YAML mapping')
    result = {}
    for key, value in node.value:
        if not isinstance(key, yaml.ScalarNode) or key.value in result:
            raise UpdateError('Non-scalar or duplicate YAML key')
        result[key.value] = value
    return result


def validate_nodes(node, seen=None):
    seen = set() if seen is None else seen
    if id(node) in seen:
        raise UpdateError('YAML aliases are not supported for the image update')
    seen.add(id(node))
    if isinstance(node, yaml.MappingNode):
        for value in fields(node).values():
            validate_nodes(value, seen)
    if isinstance(node, yaml.SequenceNode):
        for value in node.value:
            validate_nodes(value, seen)


def deployment_image(path):
    text = path.read_text()
    documents = list(yaml.safe_load_all(text))
    nodes = list(yaml.compose_all(text))
    matches = []
    for index, (document, node) in enumerate(zip(documents, nodes)):
        if node is None:
            continue
        validate_nodes(node)
        if not isinstance(document, dict) or document.get('kind') != 'Deployment':
            continue
        if document.get('metadata', {}).get('name') != 'orbit-server':
            continue
        if document.get('apiVersion') != 'apps/v1':
            raise UpdateError('Expected apps/v1 Deployment')
        if document['metadata'].get('namespace', 'orbit') != 'orbit':
            raise UpdateError('Expected the orbit namespace')
        pod = document['spec']['template']['spec']
        node_pod = fields(fields(fields(fields(node)['spec'])['template'])['spec'])
        containers = node_pod['containers']
        if not isinstance(containers, yaml.SequenceNode):
            raise UpdateError('Expected a containers sequence')
        for container_index, container in enumerate(pod['containers']):
            image = container.get('image', '')
            if image.split('@')[0].split(':')[0] != IMAGE:
                continue
            if not IMAGE_REF.fullmatch(image):
                raise UpdateError('Existing server image must have a full source SHA tag')
            image_node = fields(containers.value[container_index])['image']
            if not isinstance(image_node, yaml.ScalarNode) or '\n' in image_node.value:
                raise UpdateError('Expected a scalar image reference')
            matches.append((index, container_index, image, image_node))
    if len(matches) != 1:
        raise UpdateError('Expected exactly one orbit-server Deployment image in deployment.yaml')
    return text, documents, matches[0]


def current_source(source, source_sha):
    git(source, 'fetch', '--no-tags', 'origin', 'refs/heads/phase1')
    return git(source, 'rev-parse', 'FETCH_HEAD').stdout.strip() == source_sha


def ancestor(source, before, after):
    git(source, 'cat-file', '-e', before + '^{commit}')
    git(source, 'cat-file', '-e', after + '^{commit}')
    result = git(source, 'merge-base', '--is-ancestor', before, after, check=False)
    if result.returncode not in (0, 1):
        raise UpdateError('Cannot establish source commit ancestry')
    return result.returncode == 0


def ensure_contract(repo):
    directory = repo / TARGET.parent
    if not directory.is_dir():
        raise UpdateError('GitOps #7 server manifests are not on main yet')
    if any((directory / name).exists() for name in ('kustomization.yaml', 'kustomization.yml', 'Kustomization')):
        raise UpdateError('Kustomize detected: agree the authoritative image field with #7 first')
    path = repo / TARGET
    if not path.is_file() or path.is_symlink() or path.resolve() != repo.resolve() / TARGET:
        raise UpdateError('Expected a regular deployment.yaml inside the GitOps checkout')
    return path


def update(source, repo, source_sha, digest, run_url, attempts=3):
    if not SHA.fullmatch(source_sha) or not DIGEST.fullmatch(digest) or not RUN_URL.fullmatch(run_url):
        raise UpdateError('Invalid source SHA, image digest, or build run URL')
    if git(repo, 'status', '--porcelain').stdout:
        raise UpdateError('GitOps checkout must be clean before the updater runs')
    desired = f'{IMAGE}:{source_sha}@{digest}'
    for attempt in range(attempts):
        if not current_source(source, source_sha):
            return 'skipped: source is no longer phase1 HEAD'
        git(repo, 'fetch', '--no-tags', 'origin', 'refs/heads/main')
        base = git(repo, 'rev-parse', 'FETCH_HEAD').stdout.strip()
        # Only this initially clean, disposable Actions checkout is reset on retries.
        git(repo, 'reset', '--hard', base)
        path = ensure_contract(repo)
        text, documents, (doc_index, container_index, previous, node) = deployment_image(path)
        old_sha, old_digest = IMAGE_REF.fullmatch(previous).groups()
        if old_sha == source_sha and old_digest and old_digest != digest:
            raise UpdateError('Same source SHA has a different digest; do not republish this tag')
        if previous == desired:
            return 'no-op: published image is already selected'
        if not ancestor(source, old_sha, source_sha):
            if ancestor(source, source_sha, old_sha):
                return 'skipped: GitOps already selects a newer source'
            raise UpdateError('GitOps source is unrelated to the published source')
        changed = text[:node.start_mark.index] + json.dumps(desired) + text[node.end_mark.index:]
        expected = copy.deepcopy(documents)
        expected[doc_index]['spec']['template']['spec']['containers'][container_index]['image'] = desired
        if list(yaml.safe_load_all(changed)) != expected:
            raise UpdateError('YAML change exceeds the selected image field')
        path.write_text(changed)
        git(repo, 'diff', '--check')
        if git(repo, 'diff', '--name-only').stdout.splitlines() != [str(TARGET)]:
            raise UpdateError('Unexpected GitOps file change')
        git(repo, 'add', '--', str(TARGET))
        git(repo, 'commit', '-m', 'chore: 서버 배포 이미지 갱신', '-m',
            f'Source-Repository: ORBIT-CLOUD-LABS/orbit-server\nSource-SHA: {source_sha}\n'
            f'Image-Digest: {digest}\nBuild-Run: {run_url}')
        if not current_source(source, source_sha):
            git(repo, 'reset', '--hard', base)
            return 'skipped: source advanced before push'
        # An ordinary push is the compare-and-swap: concurrent updates reject it.
        result = git(repo, 'push', 'origin', 'HEAD:refs/heads/main', check=False)
        if result.returncode == 0:
            return 'updated: ' + git(repo, 'rev-parse', 'HEAD').stdout.strip()
        git(repo, 'fetch', '--no-tags', 'origin', 'refs/heads/main')
        if git(repo, 'rev-parse', 'FETCH_HEAD').stdout.strip() == base:
            raise UpdateError('Push rejected without a concurrent main change; check App permissions and rulesets')
    raise UpdateError(f'GitOps changed during all {attempts} push attempts')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--gitops', type=Path, required=True)
    args = parser.parse_args()
    try:
        result = update(args.source, args.gitops, os.environ['SOURCE_SHA'],
                        os.environ['IMAGE_DIGEST'], os.environ['BUILD_RUN_URL'])
    except (UpdateError, KeyError, ValueError, OSError, yaml.YAMLError) as error:
        print(f'GitOps update failed: {error}', file=sys.stderr)
        return 1
    print(result)
    if os.environ.get('GITHUB_STEP_SUMMARY'):
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as summary:
            summary.write(f'GitOps image update: {result}\n')
    return 0


if __name__ == '__main__':
    sys.exit(main())
