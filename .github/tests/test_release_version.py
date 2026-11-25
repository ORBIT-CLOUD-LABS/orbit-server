import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import unittest.mock

SCRIPT = Path(__file__).resolve().parents[1] / 'scripts' / 'release_version.py'
spec = importlib.util.spec_from_file_location('release_version', SCRIPT)
v = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v)


class BumpFromLabelsTest(unittest.TestCase):
    def test_no_release_label(self):
        self.assertIsNone(v.bump_from_labels([]))
        self.assertIsNone(v.bump_from_labels(['work:chore', 'release:patchy', 'xrelease:patch']))

    def test_single_label(self):
        for bump in v.BUMPS:
            with self.subTest(bump=bump):
                self.assertEqual(v.bump_from_labels(['area:ota', 'release:' + bump]), bump)

    def test_two_labels_fail(self):
        with self.assertRaisesRegex(v.VersionError, '둘 이상'):
            v.bump_from_labels(['release:patch', 'release:major'])


class NextVersionTest(unittest.TestCase):
    def test_first_release_is_1_0_0_for_any_label(self):
        for bump in v.BUMPS:
            with self.subTest(bump=bump):
                self.assertEqual(v.next_version([], bump), (1, 0, 0))

    def test_bumps_from_latest_semver_not_text_order(self):
        tags = ['v1.9.0', 'v1.10.0', 'v1.2.3']
        self.assertEqual(v.next_version(tags, 'patch'), (1, 10, 1))
        self.assertEqual(v.next_version(tags, 'minor'), (1, 11, 0))
        self.assertEqual(v.next_version(tags, 'major'), (2, 0, 0))

    def test_ignores_non_release_tags(self):
        tags = ['phase1', 'v2', 'v1.0', 'v01.0.0', 'v3.0.0-rc1', 'release-9.9.9', 'v1.0.0']
        self.assertEqual(v.next_version(tags, 'patch'), (1, 0, 1))


class MainTest(unittest.TestCase):
    def run_main(self, labels, tags):
        with tempfile.NamedTemporaryFile('w', delete=False, encoding='utf-8') as stream:
            stream.write('\n'.join(tags))
        self.addCleanup(Path(stream.name).unlink)
        return v.main(['--labels-json', json.dumps(labels), '--tags-file', stream.name])

    def test_prints_version(self):
        with unittest.mock.patch('builtins.print') as printed:
            self.assertEqual(self.run_main(['release:minor'], ['v1.2.4']), 0)
        printed.assert_called_once_with('1.3.0')

    def test_prints_empty_without_label(self):
        with unittest.mock.patch('builtins.print') as printed:
            self.assertEqual(self.run_main(['work:docs'], ['v1.2.4']), 0)
        printed.assert_called_once_with('')

    def test_two_labels_exit_1(self):
        with unittest.mock.patch('builtins.print'):
            self.assertEqual(self.run_main(['release:minor', 'release:patch'], []), 1)

    def test_bad_labels_json_exit_1(self):
        with unittest.mock.patch('builtins.print'):
            self.assertEqual(v.main(['--labels-json', '{"a": 1}', '--tags-file', '/nonexistent']), 1)


if __name__ == '__main__':
    unittest.main()
