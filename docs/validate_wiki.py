#!/usr/bin/env python3
"""Validate this repository's Wiki conventions without third-party dependencies.

Run from any directory: python3 docs/validate_wiki.py
Checks Markdown page/anchor links, local source-reference paths, strict fenced
JSON, and canonical bilingual coverage. Does not run Minecraft, resolve registry
IDs, check external URLs, or parse every extension of the Markdown language.
Historical docs/wiki-archive is deliberately outside the validation root.
"""
from __future__ import annotations

import argparse
import html
import json
import re
import sys
import unicodedata
from collections import Counter
from pathlib import Path
from urllib.parse import unquote, urlsplit

PAGE_PAIRS = (
    ('Home', '首页'),
    ('Installation', '安装'),
    ('Quick-Start', '快速开始'),
    ('FAQ', '常见问题'),
    ('Configuration-Overview', '配置总览'),
    ('Config-Directory-Structure', '配置目录结构'),
    ('Common-Configuration', '通用配置'),
    ('Content-Control', '内容控制'),
    ('Shell-Ejection', '抛壳配置'),
    ('Ammunition-Overrides', '弹药覆写'),
    ('Knockback', '击退配置'),
    ('Armor-Penetration', '护甲穿透'),
    ('Vehicle-Recipe-Override', '载具配方覆写'),
    ('MTS-Compatibility', 'MTS兼容'),
    ('Shell-Reloading', '弹壳复装'),
    ('Command-Reference', '命令参考'),
    ('Shell-Ejection-Scan', '抛壳扫描命令'),
    ('Resource-Trimming-Guide', '资源裁剪指南'),
    ('Recipe-Blocking-Guide', '配方禁用指南'),
    ('Recommended-Workflow', '推荐工作流'),
    ('Troubleshooting', '故障排查'),
    ('Compatibility', '兼容性'),
    ('Known-Issues', '已知问题'),
    ('Changelog', '更新日志'),
    ('Design-Notes', '设计说明'),
    ('Data-Format-Versions', '数据格式版本'),
)
SPECIAL_PAGES = ('Configuration', 'Home‐ZH', '_Sidebar.md', '_Sidebar', '_Footer')
FENCE = re.compile(r'^\s*(`{3,}|~{3,})([^\n]*)$')
MARKDOWN_LINK = re.compile(r'!?\[[^\]\n]*\]\(\s*<?([^\s)>]+)>?\s*\)')
HTML_LINK = re.compile(r'\bhref\s*=\s*[\"\']([^\"\']+)[\"\']', re.I)
EXPLICIT_ANCHOR = re.compile(r'\b(?:id|name)\s*=\s*[\"\']([^\"\']+)[\"\']', re.I)
SOURCE_PATH = re.compile(r'^/Yuuuunna/superbaddon/(?:blob|tree)/[^/]+(?:/(.*))?$')


def unique_object(pairs: list[tuple[str, object]]) -> dict[str, object]:
    result: dict[str, object] = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f'duplicate JSON key: {key}')
        result[key] = value
    return result


def reject_constant(value: str) -> None:
    raise ValueError(f'non-JSON numeric constant: {value}')


def strict_json(text: str) -> object:
    return json.loads(text, object_pairs_hook=unique_object,
                      parse_constant=reject_constant)


def split_fences(text: str) -> tuple[str, list[tuple[int, str, str]], list[str]]:
    """Replace fenced blocks by blank lines while retaining diagnostic positions."""
    visible: list[str] = []
    blocks: list[tuple[int, str, str]] = []
    errors: list[str] = []
    marker = ''
    language = ''
    start = 0
    body: list[str] = []
    for line_no, line in enumerate(text.splitlines(keepends=True), 1):
        match = FENCE.match(line.rstrip('\r\n'))
        if not marker:
            if match:
                marker, info = match.groups()
                language = info.strip().split()[0].lower() if info.strip() else ''
                start = line_no
                body = []
                visible.append('\n')
            else:
                visible.append(line)
        elif (match and match.group(1)[0] == marker[0]
              and len(match.group(1)) >= len(marker) and not match.group(2).strip()):
            blocks.append((start, language, ''.join(body)))
            marker = ''
            visible.append('\n')
        else:
            body.append(line)
            visible.append('\n')
    if marker:
        errors.append(f'line {start}: unclosed code fence')
    return ''.join(visible), blocks, errors


def link_targets(text: str) -> list[str]:
    # Inline examples such as `[label](missing)` are not navigational links.
    visible = re.sub(r'`+[^`\n]*`+', '', text)
    return ([m.group(1) for m in MARKDOWN_LINK.finditer(visible)]
            + [m.group(1) for m in HTML_LINK.finditer(visible)])


def heading_slug(text: str) -> str:
    text = html.unescape(re.sub(r'<[^>]*>', '', text)).strip().lower()
    text = re.sub(r'\[([^\]]+)\]\([^)]*\)', r'\1', text)
    return ''.join('-' if c.isspace() else c for c in text
                   if c.isspace() or c in '-_'
                   or unicodedata.category(c)[0] in 'LNM')


def page_anchors(text: str) -> set[str]:
    anchors = {html.unescape(m.group(1)) for m in EXPLICIT_ANCHOR.finditer(text)}
    occurrences: Counter[str] = Counter()
    for match in re.finditer(r'^#{1,6}\s+(.+?)\s*#*\s*$', text, re.M):
        slug = heading_slug(match.group(1))
        occurrence = occurrences[slug]
        occurrences[slug] += 1
        anchors.add(slug if occurrence == 0 else f'{slug}-{occurrence}')
    return anchors


def validate(repo_root: Path) -> tuple[list[str], dict[str, int]]:
    wiki = repo_root / 'docs' / 'wiki'
    errors: list[str] = []
    stats = {'pages': 0, 'pairs': len(PAGE_PAIRS), 'json_examples': 0,
             'internal_links': 0, 'source_links': 0}
    if not wiki.is_dir():
        return [f'Wiki directory not found: {wiki}'], stats
    pages: dict[str, str] = {}
    for path in sorted(wiki.glob('*.md')):
        try:
            pages[path.stem] = path.read_text(encoding='utf-8')
        except (OSError, UnicodeError) as exc:
            errors.append(f'{path.name}: cannot read UTF-8: {exc}')
    stats['pages'] = len(pages)
    canonical = {name for pair in PAGE_PAIRS for name in pair}
    for name in sorted(canonical | set(SPECIAL_PAGES)):
        if name not in pages:
            errors.append(f'missing required page: {name}.md')
    normalized: dict[str, str] = {}
    for name in pages:
        key = unicodedata.normalize('NFC', name).casefold()
        if key in normalized:
            errors.append(f'colliding page names: {name}, {normalized[key]}')
        normalized[key] = name
    visible_pages: dict[str, str] = {}
    links: dict[str, list[str]] = {}
    anchors: dict[str, set[str]] = {}
    for name, text in pages.items():
        visible, blocks, fence_errors = split_fences(text)
        visible_pages[name] = visible
        links[name] = link_targets(visible)
        anchors[name] = page_anchors(visible)
        errors.extend(f'{name}.md: {message}' for message in fence_errors)
        if not re.search(r'^#\s+\S', text, re.M) and name not in {'_Footer'}:
            errors.append(f'{name}.md: missing level-one title')
        if name in canonical:
            if len(text.strip()) < 500:
                errors.append(f'{name}.md: canonical page appears to be a stub')
            if re.search(r'^(?:TODO|TBD|待补充|待编写)\s*$', visible, re.M | re.I):
                errors.append(f'{name}.md: standalone placeholder')
        for line, language, body in blocks:
            if language != 'json':
                continue
            stats['json_examples'] += 1
            try:
                strict_json(body)
            except (ValueError, TypeError) as exc:
                errors.append(f'{name}.md:{line}: invalid JSON: {exc}')
    for en, zh in PAGE_PAIRS:
        for source, counterpart in ((en, zh), (zh, en)):
            targets = {unquote(urlsplit(html.unescape(t)).path).removesuffix('.md')
                       for t in links.get(source, [])}
            if source in pages and counterpart not in targets:
                errors.append(f'{source}.md: missing language link to {counterpart}')
    sidebar_targets = {unquote(urlsplit(t).path).removesuffix('.md')
                       for t in links.get('_Sidebar', [])}
    for name in sorted(canonical - sidebar_targets):
        errors.append(f'_Sidebar.md: missing canonical page link: {name}')
    for source, targets in links.items():
        for raw_target in targets:
            target = html.unescape(raw_target)
            try:
                parts = urlsplit(target)
            except ValueError as exc:
                errors.append(f'{source}.md: malformed link {target!r}: {exc}')
                continue
            if parts.scheme or parts.netloc:
                if parts.hostname == 'github.com':
                    match = SOURCE_PATH.match(unquote(parts.path))
                    if match:
                        relative = match.group(1) or ''
                        stats['source_links'] += 1
                        local = (repo_root / relative).resolve()
                        if not local.is_relative_to(repo_root.resolve()) or not local.exists():
                            errors.append(f'{source}.md: missing local source path: {relative}')
                continue
            stats['internal_links'] += 1
            path = unquote(parts.path)
            if path.startswith('./'):
                path = path[2:]
            page = path.removesuffix('.md') if path else source
            if page not in pages:
                errors.append(f'{source}.md: broken page link: {target}')
            elif parts.fragment and unquote(parts.fragment) not in anchors[page]:
                errors.append(f'{source}.md: broken anchor link: {target}')
    return errors, stats


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo-root', type=Path,
                        default=Path(__file__).resolve().parents[1])
    args = parser.parse_args(argv)
    errors, stats = validate(args.repo_root.resolve())
    print('Wiki validation: ' + ', '.join(f'{key}={value}' for key, value in stats.items()))
    for error in errors:
        print(f'ERROR: {error}', file=sys.stderr)
    if errors:
        print(f'FAILED: {len(errors)} error(s)', file=sys.stderr)
        return 1
    print('PASS: page coverage, language navigation, local links and strict JSON examples')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
