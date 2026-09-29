"""Regression tests for the documentation validator; no game runtime required."""
import importlib.util
import tempfile
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location('wiki_validator', Path(__file__).with_name('validate_wiki.py'))
v = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v)


class ValidatorTests(unittest.TestCase):
    def build(self, root):
        w = root / 'docs' / 'wiki'
        w.mkdir(parents=True)
        for en, zh in v.PAGE_PAIRS:
            for name, other in ((en, zh), (zh, en)):
                text = f'# {name}\n\n[Language]({other})\n\n' + 'Verified explanatory prose. ' * 30
                text += '\n<a name="fields"></a>\n[Jump](#fields)\n\n```json\n{"format":5,"rules":[]}\n```\n'
                (w / f'{name}.md').write_text(text, encoding='utf-8')
        for name in v.SPECIAL_PAGES:
            (w / f'{name}.md').write_text(f'# {name}\n[Home](Home)\n', encoding='utf-8')
        (w / '_Sidebar.md').write_text('# Sidebar\n' + '\n'.join(f'[Page]({n})' for p in v.PAGE_PAIRS for n in p), encoding='utf-8')
        return w

    def test_complete_fixture(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            self.build(root)
            errors, stats = v.validate(root)
            self.assertEqual(errors, [])
            self.assertEqual(stats['pages'], 57)
            self.assertEqual(stats['json_examples'], 52)

    def test_broken_link_and_anchor(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            w = self.build(root)
            with (w / 'Home.md').open('a', encoding='utf-8') as f:
                f.write('\n[Bad](Missing)\n[Bad anchor](Home#absent)\n')
            errors, _ = v.validate(root)
            self.assertTrue(any('broken page link' in e for e in errors))
            self.assertTrue(any('broken anchor' in e for e in errors))

    def test_json_rejects_duplicates_and_constants(self):
        for text in ['{"x":1,"x":2}', '{"x":NaN}', '{"x":Infinity}', '{"x":1,}']:
            with self.subTest(text=text), self.assertRaises(ValueError):
                v.strict_json(text)

    def test_unicode_and_fences(self):
        text = '# 标题\n\n```text\n[Ignored](Missing)\n```\n[Real](首页)\n'
        visible, blocks, errors = v.split_fences(text)
        self.assertEqual(errors, [])
        self.assertEqual(v.link_targets(visible), ['首页'])
        self.assertIn('标题', v.page_anchors(visible))
        self.assertTrue(v.split_fences('```json\n{}\n')[2])

    def test_source_path(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            w = self.build(root)
            (root / 'example.txt').write_text('x', encoding='utf-8')
            with (w / 'Home.md').open('a', encoding='utf-8') as f:
                f.write('\n[Code](https://github.com/Yuuuunna/superbaddon/blob/main/example.txt)\n')
            errors, _ = v.validate(root)
            self.assertEqual(errors, [])
            (root / 'example.txt').unlink()
            errors, _ = v.validate(root)
            self.assertTrue(any('missing local source path' in e for e in errors))

    def test_missing_pair_and_sidebar(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            w = self.build(root)
            (w / '首页.md').unlink()
            (w / '_Sidebar.md').write_text('# Sidebar\n', encoding='utf-8')
            errors, _ = v.validate(root)
            self.assertTrue(any('missing required page: 首页' in e for e in errors))
            self.assertTrue(any('missing canonical page link' in e for e in errors))


if __name__ == '__main__':
    unittest.main(verbosity=2)
