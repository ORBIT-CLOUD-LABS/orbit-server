"""Issue #70: exercise the updater against isolated, real Git repositories."""
import importlib.util
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

SCRIPT = Path(__file__).resolve().parents[1] / 'scripts' / 'update_gitops_image.py'
spec = importlib.util.spec_from_file_location('updater', SCRIPT)
u = importlib.util.module_from_spec(spec)
spec.loader.exec_module(u)
DIGEST = 'sha256:' + 'd' * 64
RUN = 'https://github.com/ORBIT-CLOUD-LABS/orbit-server/actions/runs/123/attempts/1'


def git(repo, *args):
    return subprocess.check_output(['git', '-C', str(repo), *args], text=True, stderr=subprocess.DEVNULL).strip()


def init(path, branch):
    subprocess.run(['git', 'init', '-q', '-b', branch, str(path)], check=True)
    git(path, 'config', 'user.name', 'Fixture')
    git(path, 'config', 'user.email', 'fixture@example.test')


def commit(repo, filename, content):
    path = repo / filename
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content)
    git(repo, 'add', filename)
    git(repo, 'commit', '-qm', 'fixture')
    return git(repo, 'rev-parse', 'HEAD')


def manifest(sha):
    return f'''# Keep this comment and sidecar untouched.
apiVersion: apps/v1
kind: Deployment
metadata:
  name: orbit-server
  namespace: orbit
spec:
  template:
    spec:
      containers:
        - name: server
          image: {u.IMAGE}:{sha} # release
        - name: sidecar
          image: example/sidecar:1
'''


class Issue70UpdaterTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='orbit70-test-')
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.source = self.root / 'source'
        init(self.source, 'phase1')
        self.a = commit(self.source, 'app.txt', 'A')
        self.b = commit(self.source, 'app.txt', 'B')
        self.source_remote = self.root / 'server.git'
        subprocess.run(['git', 'clone', '-q', '--bare', str(self.source), str(self.source_remote)], check=True)
        git(self.source, 'remote', 'add', 'origin', str(self.source_remote))
        self.repo = self.root / 'gitops'
        init(self.repo, 'main')
        commit(self.repo, str(u.TARGET), manifest(self.a))
        commit(self.repo, 'platform/keep.txt', 'keep')
        self.remote = self.root / 'gitops.git'
        subprocess.run(['git', 'clone', '-q', '--bare', str(self.repo), str(self.remote)], check=True)
        git(self.repo, 'remote', 'add', 'origin', str(self.remote))
        self.before = git(self.remote, 'rev-parse', 'main')

    def update(self, sha=None, digest=DIGEST):
        return u.update(self.source, self.repo, sha or self.b, digest, RUN)

    def remote_image(self):
        return git(self.remote, 'show', 'main:' + str(u.TARGET))

    def other_checkout(self):
        other = self.root / 'other'
        subprocess.run(['git', 'clone', '-q', str(self.remote), str(other)], check=True)
        git(other, 'config', 'user.name', 'Other')
        git(other, 'config', 'user.email', 'other@example.test')
        return other

    def test_issue70_updates_only_image_and_records_published_identity(self):
        self.assertTrue(self.update().startswith('updated:'))
        self.assertIn(f'"{u.IMAGE}:{self.b}@{DIGEST}" # release', self.remote_image())
        self.assertIn('# Keep this comment', self.remote_image())
        self.assertIn('example/sidecar:1', self.remote_image())
        self.assertEqual(git(self.remote, 'diff', '--name-only', self.before, 'main'), str(u.TARGET))
        message = git(self.remote, 'show', '-s', '--format=%B', 'main')
        for value in (self.b, DIGEST, RUN):
            self.assertIn(value, message)

    def test_issue70_same_image_rerun_is_noop(self):
        self.update()
        before = git(self.remote, 'rev-parse', 'main')
        self.assertTrue(self.update().startswith('no-op:'))
        self.assertEqual(git(self.remote, 'rev-parse', 'main'), before)

    def test_issue70_late_old_build_cannot_replace_new_image(self):
        self.update()
        before = git(self.remote, 'rev-parse', 'main')
        self.assertTrue(self.update(self.a).startswith('skipped:'))
        self.assertEqual(git(self.remote, 'rev-parse', 'main'), before)

    def test_issue70_same_source_changed_digest_is_rejected(self):
        self.update()
        with self.assertRaisesRegex(u.UpdateError, 'different digest'):
            self.update(digest='sha256:' + 'e' * 64)

    def test_issue70_missing_issue7_manifest_fails_without_remote_change(self):
        git(self.repo, 'rm', str(u.TARGET))
        git(self.repo, 'commit', '-qm', 'remove deployment')
        git(self.repo, 'push', 'origin', 'main')
        before = git(self.remote, 'rev-parse', 'main')
        with self.assertRaises(u.UpdateError):
            self.update()
        self.assertEqual(git(self.remote, 'rev-parse', 'main'), before)

    def test_issue70_kustomize_override_requires_new_contract(self):
        commit(self.repo, 'workloads/orbit-server/kustomization.yaml', 'resources: [deployment.yaml]\n')
        git(self.repo, 'push', 'origin', 'main')
        with self.assertRaisesRegex(u.UpdateError, 'Kustomize'):
            self.update()

    def test_issue70_ambiguous_deployment_is_rejected(self):
        commit(self.repo, str(u.TARGET), manifest(self.a) + '\n---\n' + manifest(self.a))
        git(self.repo, 'push', 'origin', 'main')
        with self.assertRaisesRegex(u.UpdateError, 'exactly one'):
            self.update()

    def test_issue70_duplicate_yaml_key_is_rejected(self):
        commit(self.repo, str(u.TARGET), manifest(self.a).replace('  name: orbit-server', '  name: ignored\n  name: orbit-server'))
        git(self.repo, 'push', 'origin', 'main')
        with self.assertRaisesRegex(u.UpdateError, 'duplicate'):
            self.update()

    def test_issue70_moving_tag_is_rejected(self):
        commit(self.repo, str(u.TARGET), manifest(self.a).replace(self.a, 'phase1'))
        git(self.repo, 'push', 'origin', 'main')
        with self.assertRaisesRegex(u.UpdateError, 'full source SHA'):
            self.update()

    def test_issue70_invalid_digest_is_rejected(self):
        with self.assertRaisesRegex(u.UpdateError, 'Invalid'):
            self.update(digest='not-a-digest')
        self.assertEqual(git(self.remote, 'rev-parse', 'main'), self.before)

    def test_issue70_dirty_checkout_is_not_overwritten(self):
        (self.repo / 'keep-my-work.txt').write_text('user work')
        with self.assertRaisesRegex(u.UpdateError, 'clean'):
            self.update()
        self.assertEqual((self.repo / 'keep-my-work.txt').read_text(), 'user work')

    def test_issue70_source_advances_before_push_so_nothing_is_published(self):
        real = u.current_source
        calls = []
        def advance(repo, sha):
            calls.append(sha)
            if len(calls) == 2:
                commit(self.source, 'app.txt', 'C')
                git(self.source, 'push', 'origin', 'phase1')
            return real(repo, sha)
        with patch.object(u, 'current_source', side_effect=advance):
            self.assertIn('advanced before push', self.update())
        self.assertEqual(git(self.remote, 'rev-parse', 'main'), self.before)
        self.assertEqual(git(self.repo, 'status', '--porcelain'), '')

    def test_issue70_concurrent_main_update_is_preserved_on_retry(self):
        other = self.other_checkout()
        real = u.git
        pushes = []
        def race(repo, *args, **kwargs):
            if args[0] == 'push':
                pushes.append(1)
                if len(pushes) == 1:
                    commit(other, 'platform/keep.txt', 'teammate change')
                    git(other, 'push', 'origin', 'main')
            return real(repo, *args, **kwargs)
        with patch.object(u, 'git', side_effect=race):
            self.assertTrue(self.update().startswith('updated:'))
        self.assertEqual(len(pushes), 2)
        self.assertEqual(git(self.remote, 'show', 'main:platform/keep.txt'), 'teammate change')
        self.assertIn(self.b, self.remote_image())

    def test_issue70_gitops_newer_than_rewound_phase1_is_not_downgraded(self):
        c = commit(self.source, 'app.txt', 'C')
        git(self.source, 'push', 'origin', 'phase1')
        self.update(c)
        before = git(self.remote, 'rev-parse', 'main')
        git(self.source_remote, 'update-ref', 'refs/heads/phase1', self.b)
        self.assertIn('newer source', self.update())
        self.assertEqual(git(self.remote, 'rev-parse', 'main'), before)

    def test_issue70_unknown_source_ancestry_fails_closed(self):
        commit(self.repo, str(u.TARGET), manifest('f' * 40))
        git(self.repo, 'push', 'origin', 'main')
        before = git(self.remote, 'rev-parse', 'main')
        with self.assertRaises(u.UpdateError):
            self.update()
        self.assertEqual(git(self.remote, 'rev-parse', 'main'), before)

    def test_issue70_permission_denial_is_failure_without_remote_change(self):
        hook = self.remote / 'hooks/pre-receive'
        hook.write_text('#!/bin/sh\nexit 1\n')
        hook.chmod(0o755)
        with self.assertRaisesRegex(u.UpdateError, 'permissions and rulesets'):
            self.update()
        self.assertEqual(git(self.remote, 'rev-parse', 'main'), self.before)

    def test_issue70_retry_count_is_bounded(self):
        other = self.other_checkout()
        real = u.git
        pushes = []
        def race(repo, *args, **kwargs):
            if args[0] == 'push':
                pushes.append(1)
                git(other, 'pull', '--ff-only')
                commit(other, 'platform/keep.txt', str(len(pushes)))
                git(other, 'push', 'origin', 'main')
            return real(repo, *args, **kwargs)
        with patch.object(u, 'git', side_effect=race):
            with self.assertRaisesRegex(u.UpdateError, 'all 3'):
                self.update()
        self.assertEqual(len(pushes), 3)
        self.assertIn(self.a, self.remote_image())


if __name__ == '__main__':
    unittest.main()
