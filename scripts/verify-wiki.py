#!/usr/bin/env python3
"""Check wiki navigation, JSON examples, required pages and complete recipe coverage."""
import argparse
import json
from pathlib import Path
import re
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parents[1]


def verify(wiki):
    pages = {p.stem: p for p in wiki.glob('*.md')}
    required = {'Home', '_Sidebar', '_Footer', 'Installation', 'Debug-Stick', 'Verrouillage-des-blocs',
                'Configuration-serveur', 'Recettes', 'Developpement-et-tests', 'Publication-du-wiki'}
    errors = [f'Missing page: {name}' for name in sorted(required - pages.keys())]
    links = examples = 0
    recipe_ids = []
    for name, path in sorted(pages.items()):
        text = path.read_text()
        if not text.strip():
            errors.append(f'{path.name}: empty page')
        if len(re.findall(r'^```', text, re.M)) % 2:
            errors.append(f'{path.name}: unclosed fenced block')
        for target in re.findall(r'\]\(([^)]+)\)', text):
            if re.match(r'^(?:https?://|mailto:|#)', target):
                continue
            slug = unquote(target.split('#')[0]).removesuffix('.md')
            links += 1
            if slug not in pages:
                errors.append(f'{path.name}: broken local link: {target}')
        for example in re.findall(r'^```json\s*\n(.*?)\n```', text, re.M | re.S):
            examples += 1
            try:
                json.loads(example)
            except ValueError as error:
                errors.append(f'{path.name}: invalid JSON example: {error}')
        if name.startswith('Recettes-'):
            recipe_ids.extend(re.findall(r'^\| \[`([^`]+)`\]\(https://github.com/', text, re.M))
    expected = set()
    for path in (ROOT / 'src/main/resources/data').glob('*/recipe/**/*.json'):
        parts = path.relative_to(ROOT / 'src/main/resources/data').parts
        expected.add(parts[0] + ':' + Path(*parts[2:]).with_suffix('').as_posix())
    if set(recipe_ids) != expected or len(recipe_ids) != len(expected):
        errors.append(f'Recipe catalogue mismatch: {len(recipe_ids)} rows for {len(expected)} source recipes')
    for name in pages.keys() - {'_Sidebar', '_Footer', 'Home'}:
        # Every page must have at least one incoming local link from another page.
        if not any(f']({name})' in p.read_text() for other, p in pages.items() if other != name):
            errors.append(f'{name}: no incoming navigation link')
    if errors:
        raise SystemExit('\n'.join(errors))
    print(f'Wiki valide : {len(pages)} fichiers Markdown, {links} liens internes, '
          f'{examples} exemples JSON et {len(recipe_ids)} recettes.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--wiki-dir', type=Path, default=ROOT / 'wiki')
    verify(parser.parse_args().wiki_dir.resolve())
