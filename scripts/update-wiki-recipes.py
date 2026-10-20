#!/usr/bin/env python3
"""Regenerate only the five recipe-catalogue pages in the separate GitHub wiki clone."""
import argparse
from collections import Counter
import json
from pathlib import Path
from urllib.parse import quote

ROOT = Path(__file__).resolve().parents[1]
PAGES = {
    'Recettes-utilitaires': ('Recettes utilitaires et haut fourneau', 'Outils, blocs de lumière, conversions et recettes de haut fourneau.'),
    'Recettes-cuivre': ('Recettes de cuivre', 'Transformations des familles de cuivre, oxydation et variantes cirées.'),
    'Recettes-coraux': ('Conversions de coraux', 'Conversions entre blocs de corail, coraux et éventails.'),
    'Recettes-tailleur': ('Recettes du tailleur de pierre', 'Recettes du dossier stonecutter, notamment les dalles et escaliers en bois.'),
    'Recettes-vanilla': ('Recettes vanilla remplacées', 'Recettes du namespace minecraft livrées par Chomagerie ; elles remplacent les identifiants correspondants.'),
}
TYPES = {'crafting_shaped': 'Avec disposition', 'crafting_shapeless': 'Sans disposition',
         'stonecutting': 'Tailleur de pierre', 'blasting': 'Haut fourneau'}


def code(value):
    return '`' + str(value).replace('|', '&#124;').replace('`', '&#96;') + '`'


def ingredient(value):
    if isinstance(value, str):
        return code(value)
    if isinstance(value, list):
        return ' ou '.join(ingredient(v) for v in value)
    if isinstance(value, dict):
        if 'item' in value:
            return code(value['item'])
        if 'tag' in value:
            return code('#' + value['tag'])
    raise ValueError(f'Unsupported ingredient: {value!r}')


def row(path, data_root):
    relative = path.relative_to(data_root)
    namespace = relative.parts[0]
    recipe_path = Path(*relative.parts[2:]).with_suffix('').as_posix()
    recipe_id = f'{namespace}:{recipe_path}'
    data = json.loads(path.read_text(encoding='utf-8-sig'))
    kind = data['type'].removeprefix('minecraft:')
    if kind not in TYPES:
        raise ValueError(f'Unsupported recipe type: {kind}')
    if kind == 'crafting_shaped':
        grid = ' / '.join(line.replace(' ', '·') for line in data['pattern'])
        counts = Counter(''.join(data['pattern']).replace(' ', ''))
        entries = [f'{code(k)} = {counts[k]} × {ingredient(v)}' for k, v in data['key'].items()]
        inputs = 'Grille ' + code(grid) + '<br>' + '<br>'.join(entries)
    elif kind == 'crafting_shapeless':
        counts = Counter(json.dumps(x, sort_keys=True) for x in data['ingredients'])
        inputs = '<br>'.join(f'{n} × {ingredient(json.loads(x))}' for x, n in counts.items())
    else:
        inputs = ingredient(data['ingredient'])
        if 'cookingtime' in data:
            inputs += f'<br>{data["cookingtime"]} ticks ({data["cookingtime"] / 20:g} s à 20 TPS)'
        if 'experience' in data:
            inputs += f'<br>Expérience : {data["experience"]}'
    result = data['result']
    if isinstance(result, str):
        output = '1 × ' + code(result)
    else:
        output = f'{result.get("count", 1)} × {code(result.get("id", result.get("item")))}'
        if result.get('components'):
            output += '<br>Composants : ' + code(json.dumps(result['components'], ensure_ascii=False, separators=(',', ':')))
    if namespace == 'minecraft':
        page = 'Recettes-vanilla'
    elif recipe_path.startswith(('utility/copper/', 'utility/waxed_copper/')):
        page = 'Recettes-cuivre'
    elif recipe_path.startswith('corals/'):
        page = 'Recettes-coraux'
    elif recipe_path.startswith('stonecutter/'):
        page = 'Recettes-tailleur'
    else:
        page = 'Recettes-utilitaires'
    url = 'https://github.com/mal0andre/ChomagerieMOD/blob/master/' + quote(path.relative_to(ROOT).as_posix(), safe='/')
    return page, recipe_id, f'| [{code(recipe_id)}]({url}) | {TYPES[kind]} | {inputs} | {output} |'


def generate(wiki):
    data_root = ROOT / 'src/main/resources/data'
    grouped = {page: [] for page in PAGES}
    ids = set()
    for path in sorted(data_root.glob('*/recipe/**/*.json')):
        page, recipe_id, line = row(path, data_root)
        if recipe_id in ids:
            raise ValueError(f'Duplicate recipe: {recipe_id}')
        ids.add(recipe_id)
        grouped[page].append(line)
    if not (wiki / 'Home.md').is_file():
        raise ValueError(f'{wiki} must contain the prepared wiki with Home.md')
    for page, (title, intro) in PAGES.items():
        content = f'''# {title}

{intro}

**{len(grouped[page])} recettes**. Page générée depuis les JSON du code documenté ; les identifiants donnent accès au fichier source. Une case vide dans une grille est notée `·`. Les éventuels datapacks prioritaires restent applicables.

[Comprendre les tableaux et voir toutes les catégories](Recettes).

| Identifiant de recette et source | Fabrication | Ingrédients et disposition | Résultat |
| --- | --- | --- | --- |
'''
        content += '\n'.join(grouped[page]) + '\n\n[Retour aux recettes](Recettes) · [Accueil](Home)\n'
        (wiki / f'{page}.md').write_text(content)
    print(f'{len(ids)} recettes réparties dans {len(grouped)} pages.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--wiki-dir', type=Path, default=ROOT / 'wiki')
    generate(parser.parse_args().wiki_dir.resolve())
