"""실제 Git 저장소(임시 bare 원격 포함)로 orbit-gitops 갱신 스크립트를 검사한다."""
import importlib.util
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

SCRIPT = Path(__file__).resolve().parents[1] / 'scripts' / 'update_gitops_image.py'
spec = importlib.util.spec_from_file_location('updater', SCRIPT)
u = importlib.util.module_from_spec(spec)
spec.loader.exec_module(u)
OLD_DIGEST = 'sha256:' + 'c' * 64
DIGEST = 'sha256:' + 'd' * 64
SOURCE = 'a' * 40
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


def kustomization(version='1.0.0', digest=OLD_DIGEST):
    return f'''# 이 주석과 다른 이미지는 그대로 남아야 한다.
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - deployment.yaml
images:
  - name: example/sidecar
    newTag: "1"
    digest: sha256:{'e' * 64}
  - name: {u.IMAGE}
    newTag: "{version}" # release
    digest: {digest}
'''


class FakeGitHub:
    def __init__(self, pulls=()):
        self.pulls = list(pulls)
        self.created = []
        self.closed = []

    def open_release_pulls(self):
        return list(self.pulls)

    def create_pull(self, branch, title, body):
        url = f'https://github.com/{u.REPOSITORY}/pull/{100 + len(self.created)}'
        self.created.append({'branch': branch, 'title': title, 'body': body})
        self.pulls.append({'number': 100 + len(self.created), 'branch': branch, 'url': url})
        return url

    def close_pull(self, number, comment):
        self.closed.append((number, comment))


def pull(number, branch):
    return {'number': number, 'branch': branch, 'url': f'https://github.com/{u.REPOSITORY}/pull/{number}'}


class UpdateGitopsImageTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='orbit-release-test-')
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.repo = self.root / 'gitops'
        init(self.repo, u.BRANCH)
        commit(self.repo, str(u.TARGET), kustomization())
        commit(self.repo, 'cdn/keep.txt', 'keep')
        self.remote = self.root / 'gitops.git'
        subprocess.run(['git', 'clone', '-q', '--bare', str(self.repo), str(self.remote)], check=True)
        git(self.repo, 'remote', 'add', 'origin', str(self.remote))
        self.before = self.remote_head()
        self.github = FakeGitHub()

    def update(self, version='1.0.1', digest=DIGEST):
        return u.update(self.repo, version, SOURCE, digest, RUN, self.github)

    def remote_head(self, branch=u.BRANCH):
        return git(self.remote, 'rev-parse', branch)

    def remote_file(self, branch=u.BRANCH):
        return git(self.remote, 'show', f'{branch}:{u.TARGET}')

    def push_fixture(self, content):
        commit(self.repo, str(u.TARGET), content)
        git(self.repo, 'push', '-q', 'origin', u.BRANCH)
        return self.remote_head()

    def assert_nothing_opened(self):
        self.assertEqual(self.github.created, [])
        self.assertEqual(git(self.remote, 'branch', '--list', u.RELEASE_PREFIX + '*'), '')

    def test_opens_pull_request_without_touching_main(self):
        result = self.update()
        self.assertTrue(result.startswith('opened: https://github.com/'))
        self.assertEqual(self.remote_head(), self.before)
        branch = u.RELEASE_PREFIX + '1.0.1'
        self.assertEqual(self.github.created[0]['branch'], branch)
        self.assertEqual(self.github.created[0]['title'], 'chore: orbit-server 1.0.1 배포')
        for value in ('Version: 1.0.1', SOURCE, DIGEST, RUN):
            self.assertIn(value, self.github.created[0]['body'])

    def test_release_branch_changes_only_orbit_server_image(self):
        self.update()
        branch = u.RELEASE_PREFIX + '1.0.1'
        text = self.remote_file(branch)
        self.assertIn('newTag: "1.0.1" # release', text)
        self.assertIn(f'digest: {DIGEST}', text)
        self.assertIn('# 이 주석과', text)
        self.assertIn('sha256:' + 'e' * 64, text)
        self.assertEqual(git(self.remote, 'diff', '--name-only', self.before, branch), str(u.TARGET))
        message = git(self.remote, 'show', '-s', '--format=%B', branch)
        self.assertTrue(message.startswith('chore: orbit-server 1.0.1 배포'))
        for value in ('Version: 1.0.1', SOURCE, DIGEST, RUN):
            self.assertIn(value, message)

    def test_rerun_with_open_pull_request_is_kept(self):
        self.update()
        head = self.remote_head(u.RELEASE_PREFIX + '1.0.1')
        self.assertTrue(self.update().startswith('exists: '))
        self.assertEqual(len(self.github.created), 1)
        self.assertEqual(self.remote_head(u.RELEASE_PREFIX + '1.0.1'), head)

    def test_rerun_after_failed_pull_request_overwrites_release_branch(self):
        git(self.repo, 'push', '-q', 'origin', f'{u.BRANCH}:refs/heads/{u.RELEASE_PREFIX}1.0.1')
        self.assertTrue(self.update().startswith('opened: '))
        self.assertIn('"1.0.1"', self.remote_file(u.RELEASE_PREFIX + '1.0.1'))

    def test_same_version_on_main_is_noop(self):
        self.push_fixture(kustomization('1.0.1', DIGEST))
        self.assertTrue(self.update().startswith('no-op:'))
        self.assert_nothing_opened()

    def test_late_older_release_does_not_downgrade_main(self):
        self.push_fixture(kustomization('1.2.0'))
        self.assertIn('더 높은 버전', self.update('1.1.9', 'sha256:' + 'f' * 64))
        self.assert_nothing_opened()

    def test_late_older_release_does_not_compete_with_open_higher_pull_request(self):
        self.github.pulls = [pull(7, u.RELEASE_PREFIX + '1.2.0')]
        self.assertIn('배포 PR이 열려 있음', self.update('1.1.9'))
        self.assert_nothing_opened()
        self.assertEqual(self.github.closed, [])

    def test_new_release_closes_only_older_release_pull_requests(self):
        self.github.pulls = [pull(5, u.RELEASE_PREFIX + '1.0.0'), pull(6, 'task/52-ota'),
                             pull(8, u.RELEASE_PREFIX + 'manual')]
        self.update('1.0.1')
        self.assertEqual([number for number, _ in self.github.closed], [5])
        self.assertIn('1.0.1', self.github.closed[0][1])

    def test_semver_compare_is_numeric(self):
        self.push_fixture(kustomization('1.9.0'))
        self.assertTrue(self.update('1.10.0').startswith('opened:'))
        self.github.pulls = [pull(9, u.RELEASE_PREFIX + '1.10.0')]
        self.assertTrue(self.update('1.9.1', 'sha256:' + 'f' * 64).startswith('skipped:'))

    def test_same_version_with_other_digest_is_rejected(self):
        self.push_fixture(kustomization('1.0.1', DIGEST))
        with self.assertRaisesRegex(u.UpdateError, '다른 digest'):
            self.update(digest='sha256:' + 'f' * 64)

    def test_missing_base_is_skipped_without_change(self):
        git(self.repo, 'rm', '-q', str(u.TARGET))
        git(self.repo, 'commit', '-qm', 'remove base')
        git(self.repo, 'push', '-q', 'origin', u.BRANCH)
        self.assertIn('아직 없음', self.update())
        self.assert_nothing_opened()

    def test_entry_without_digest_is_rejected(self):
        self.push_fixture(kustomization().replace(f'    digest: {OLD_DIGEST}\n', ''))
        with self.assertRaisesRegex(u.UpdateError, 'digest가 없습니다'):
            self.update()
        self.assert_nothing_opened()

    def test_duplicate_entry_is_rejected(self):
        text = kustomization()
        self.push_fixture(text + f'  - name: {u.IMAGE}\n    newTag: "1.0.0"\n    digest: {OLD_DIGEST}\n')
        with self.assertRaisesRegex(u.UpdateError, '정확히 하나'):
            self.update()

    def test_new_name_entry_is_supported(self):
        text = kustomization().replace(f'  - name: {u.IMAGE}\n', f'  - name: orbit-server\n    newName: {u.IMAGE}\n')
        self.push_fixture(text)
        self.assertTrue(self.update().startswith('opened:'))
        self.assertIn(f'newName: {u.IMAGE}', self.remote_file(u.RELEASE_PREFIX + '1.0.1'))

    def test_duplicate_yaml_key_is_rejected(self):
        self.push_fixture(kustomization().replace('kind: Kustomization', 'kind: Kustomization\nkind: Kustomization'))
        with self.assertRaisesRegex(u.UpdateError, '중복'):
            self.update()

    def test_alias_is_rejected(self):
        self.push_fixture(kustomization().replace('newTag: "1"', 'newTag: &tag "1"') + 'x: *tag\n')
        with self.assertRaisesRegex(u.UpdateError, 'anchor'):
            self.update()

    def test_moving_tag_in_manifest_fails_closed(self):
        self.push_fixture(kustomization('prod'))
        with self.assertRaisesRegex(u.UpdateError, '버전 형식'):
            self.update()
        self.assert_nothing_opened()

    def test_invalid_inputs_are_rejected(self):
        for args in (('1.0', DIGEST), ('v1.0.1', DIGEST), ('1.0.1', 'sha256:abc')):
            with self.subTest(args=args), self.assertRaisesRegex(u.UpdateError, '형식'):
                self.update(*args)
        self.assert_nothing_opened()

    def test_dirty_checkout_is_not_overwritten(self):
        (self.repo / 'my-work.txt').write_text('work')
        with self.assertRaisesRegex(u.UpdateError, '깨끗하지'):
            self.update()
        self.assertEqual((self.repo / 'my-work.txt').read_text(), 'work')

    def test_push_denied_fails_without_pull_request(self):
        hook = self.remote / 'hooks/pre-receive'
        hook.write_text('#!/bin/sh\nexit 1\n')
        hook.chmod(0o755)
        with self.assertRaisesRegex(u.UpdateError, 'push'):
            self.update()
        self.assertEqual(self.github.created, [])

    def test_open_release_pulls_ignores_forks_and_other_branches(self):
        def pr(number, ref, repo):
            return {'number': number, 'html_url': f'u{number}',
                    'head': {'ref': ref, 'repo': {'full_name': repo} if repo else None}}

        listing = [pr(1, u.RELEASE_PREFIX + '1.0.0', u.REPOSITORY), pr(2, u.RELEASE_PREFIX + '1.0.1', 'someone/fork'),
                   pr(3, 'task/1', u.REPOSITORY), pr(4, u.RELEASE_PREFIX + '1.0.2', None)]
        with patch.object(u.GitHub, '_api', return_value=listing):
            self.assertEqual(u.GitHub().open_release_pulls(),
                             [{'number': 1, 'branch': u.RELEASE_PREFIX + '1.0.0', 'url': 'u1'}])


if __name__ == '__main__':
    unittest.main()
