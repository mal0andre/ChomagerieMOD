#!/usr/bin/env python3
"""Build the wiki's visual recipe catalogue from source JSON and Minecraft-rendered icons."""
import argparse
from collections import Counter, defaultdict
import html
import io
import json
from pathlib import Path
from urllib.parse import quote
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
MEDIA = 'https://raw.githubusercontent.com/wiki/mal0andre/ChomagerieMOD/'
PAGES = {
    'Recettes-utilitaires': ('Recettes utilitaires et haut fourneau', 'Outils, blocs de lumière, conversions et fonte des blocs bruts.'),
    'Recettes-cuivre': ('Recettes de cuivre', 'Transformations des familles de cuivre, oxydation et variantes cirées.'),
    'Recettes-coraux': ('Conversions de coraux', 'Conversions entre blocs de corail, coraux et éventails.'),
    'Recettes-tailleur': ('Recettes du tailleur de pierre', 'Dalles et escaliers en bois, dont le peuplier.'),
    'Recettes-vanilla': ('Recettes vanilla remplacées', 'Recettes du namespace minecraft remplacées par Chomagerie.'),
}
TYPES = {'crafting_shaped': 'Fabrication', 'crafting_shapeless': 'Sans disposition',
         'stonecutting': 'Tailleur de pierre', 'blasting': 'Haut fourneau'}
REPRESENTATIVES = {'#minecraft:logs': 'minecraft:oak_log'}


def ingredient_id(value):
    if isinstance(value, str):
        return value
    if isinstance(value, list):
        return ingredient_id(value[0])
    if 'item' in value:
        return value['item']
    return '#' + value['tag']


def result(data):
    value = data['result']
    return (value, 1, {}) if isinstance(value, str) else (
        value.get('id', value.get('item')), value.get('count', 1), value.get('components', {}))


def recipes():
    data_root = ROOT / 'src/main/resources/data'
    for path in sorted(data_root.glob('*/recipe/**/*.json')):
        parts = path.relative_to(data_root).parts
        namespace, name = parts[0], Path(*parts[2:]).with_suffix('').as_posix()
        data = json.loads(path.read_text(encoding='utf-8-sig'))
        kind = data['type'].removeprefix('minecraft:')
        if kind not in TYPES:
            raise ValueError(f'Unsupported recipe type: {kind}')
        if namespace == 'minecraft':
            page = 'Recettes-vanilla'
        elif name.startswith(('utility/copper/', 'utility/waxed_copper/')):
            page = 'Recettes-cuivre'
        elif name.startswith('corals/'):
            page = 'Recettes-coraux'
        elif name.startswith('stonecutter/'):
            page = 'Recettes-tailleur'
        else:
            page = 'Recettes-utilitaires'
        yield {'id': f'{namespace}:{name}', 'data': data, 'kind': kind, 'page': page, 'path': path,
               'image': f'images/recipes/{namespace}/{name}.png'}


def inputs(recipe):
    data, kind = recipe['data'], recipe['kind']
    if kind == 'crafting_shaped':
        # Preserve JSON row and column offsets exactly, including intentional blanks.
        return [data['key'].get(char) for row in data['pattern'] for char in row]
    return data['ingredients'] if kind == 'crafting_shapeless' else [data['ingredient']]


def required_icons(all_recipes):
    ids = {'minecraft:honeycomb', 'minecraft:iron_axe', 'minecraft:debug_stick', 'minecraft:glass_pane',
           'minecraft:glass', 'minecraft:crafting_table', 'minecraft:blast_furnace', 'minecraft:stonecutter'}
    for recipe in all_recipes:
        for value in inputs(recipe):
            if value is not None:
                name = ingredient_id(value)
                ids.add(REPRESENTATIVES.get(name, name))
        output_id, _, components = result(recipe['data'])
        ids.add(output_id)
        if 'minecraft:block_state' in components:
            ids.add(output_id + '@level=' + components['minecraft:block_state']['level'])
    if any(name.startswith('#') for name in ids):
        raise ValueError('Every tag needs a documented representative icon')
    return sorted(ids)


class Renderer:
    def __init__(self, jar, icons, language):
        from PIL import Image, ImageFont
        self.Image = Image
        self.icons = icons
        self.scale = 4
        with ZipFile(jar) as archive:
            self.textures = {name: Image.open(io.BytesIO(archive.read(
                f'assets/minecraft/textures/gui/container/{name}.png'))).convert('RGBA')
                for name in ['crafting_table', 'furnace']}
            self.font_sheet = Image.open(io.BytesIO(archive.read('assets/minecraft/textures/font/ascii.png'))).convert('RGBA')
            self.english = json.loads(archive.read('assets/minecraft/lang/en_us.json'))
        self.language = json.loads(language.read_text())
        self.font = ImageFont.truetype('/usr/share/fonts/truetype/freefont/FreeSans.ttf', 28)

    def name(self, name):
        if name.startswith('#'):
            return 'Bûche au choix' if name == '#minecraft:logs' else name
        key = name.replace(':', '.')
        return next((dictionary[prefix + key] for dictionary in [self.language, self.english]
                     for prefix in ['item.', 'block.'] if prefix + key in dictionary), name)

    def paste_item(self, canvas, name, x, y, components=None):
        name = REPRESENTATIVES.get(name, name)
        if components and 'minecraft:block_state' in components:
            name += '@level=' + components['minecraft:block_state']['level']
        icon = self.Image.open(self.icons / (name.replace(':', '_') + '.png')).convert('RGB')
        canvas.paste(icon.resize((64, 64), self.Image.Resampling.NEAREST), (x * 4, y * 4))

    def number(self, canvas, count, x, y):
        if count == 1:
            return
        # Inventory stack digits, including their native one-pixel shadow.
        for index, char in enumerate(str(count)):
            code = ord(char)
            glyph = self.font_sheet.crop((code % 16 * 8, code // 16 * 8, code % 16 * 8 + 8, code // 16 * 8 + 8))
            mask = glyph.getchannel('A').resize((32, 32), self.Image.Resampling.NEAREST)
            at = ((x + 17 - len(str(count)) * 6 + index * 6) * 4, (y + 9) * 4)
            canvas.paste((63, 63, 63), (at[0] + 4, at[1] + 4), mask)
            canvas.paste((255, 255, 255), at, mask)

    def render(self, recipe, target):
        from PIL import ImageDraw
        kind, data = recipe['kind'], recipe['data']
        if kind.startswith('crafting_'):
            canvas = self.textures['crafting_table'].crop((0, 0, 176, 78)).resize((704, 312), self.Image.Resampling.NEAREST)
            if kind == 'crafting_shaped':
                for row, line in enumerate(data['pattern']):
                    for column, char in enumerate(line):
                        if char != ' ':
                            self.paste_item(canvas, ingredient_id(data['key'][char]), 30 + column * 18, 17 + row * 18)
            else:
                for index, value in enumerate(data['ingredients']):
                    self.paste_item(canvas, ingredient_id(value), 30 + index % 3 * 18, 17 + index // 3 * 18)
            output_x, output_y = 124, 35
            label_x = 28
        elif kind == 'blasting':
            canvas = self.textures['furnace'].crop((0, 0, 176, 78)).resize((704, 312), self.Image.Resampling.NEAREST)
            self.paste_item(canvas, ingredient_id(data['ingredient']), 56, 17)
            output_x, output_y, label_x = 116, 35, 8
        else:
            # Compact inventory-style conversion: standard slots, native crafting arrow, exact item models.
            canvas = self.textures['crafting_table'].crop((0, 0, 176, 78)).resize((704, 312), self.Image.Resampling.NEAREST)
            draw = ImageDraw.Draw(canvas)
            draw.rectangle((8, 52, 350, 292), fill=(198, 198, 198))
            slot = self.textures['crafting_table'].crop((29, 16, 47, 34)).resize((72, 72), self.Image.Resampling.NEAREST)
            canvas.paste(slot, (56 * 4 - 4, 35 * 4 - 4))
            self.paste_item(canvas, ingredient_id(data['ingredient']), 56, 35)
            output_x, output_y, label_x = 124, 35, 8
        output_id, count, components = result(data)
        self.paste_item(canvas, output_id, output_x, output_y, components)
        self.number(canvas, count, output_x, output_y)
        ImageDraw.Draw(canvas).text((label_x * 4, 4 * 4), TYPES[kind], font=self.font, fill=(64, 64, 64))
        bottom = self.textures['crafting_table'].crop((0, 163, 176, 166)).resize((704, 12), self.Image.Resampling.NEAREST)
        canvas.paste(bottom, (0, 300))
        target.parent.mkdir(parents=True, exist_ok=True)
        canvas.save(target, optimize=True)


def source_url(recipe):
    return 'https://github.com/mal0andre/ChomagerieMOD/blob/master/' + quote(recipe['path'].relative_to(ROOT).as_posix(), safe='/')


def entry(recipe, renderer):
    output_id, count, components = result(recipe['data'])
    title = f'{count} × {renderer.name(output_id)}'
    if 'minecraft:block_state' in components:
        title += ' — niveau ' + components['minecraft:block_state']['level']
    items = Counter(ingredient_id(value) for value in inputs(recipe) if value is not None)
    legend = ', '.join(f'{amount} × {renderer.name(name)}' for name, amount in items.items())
    alt = f'{TYPES[recipe["kind"]]} : {legend} → {title}'
    body = f'### {title}\n\n![{alt}]({MEDIA}{quote(recipe["image"], safe="/")})\n\n'
    notes = []
    if recipe['kind'] == 'crafting_shapeless':
        notes.append('Sans disposition : les ingrédients peuvent être placés dans n’importe quel ordre.')
    if any(name.startswith('#') for name in items):
        notes.append('La bûche de chêne représente le tag `#minecraft:logs` : toute bûche compatible est acceptée.')
    if recipe['kind'] == 'blasting':
        data = recipe['data']
        notes.append(f'{data["cookingtime"] / 20:g} s à 20 TPS · {data["experience"]} XP · combustible compatible au choix.')
    if components:
        notes.append('Composants : `' + json.dumps(components, ensure_ascii=False, separators=(',', ':')) + '`.')
    if notes:
        body += ' '.join(notes) + '\n\n'
    body += '<details>\n<summary>Ingrédients et fichier source</summary>\n\n'
    body += '\n'.join(f'- {amount} × {renderer.name(name)} — `{name}`' for name, amount in items.items())
    body += f'\n\nRecette : [`{recipe["id"]}`]({source_url(recipe)})\n\n</details>\n\n'
    return body


def generate(args):
    all_recipes = list(recipes())
    if args.prepare_icons:
        args.icons_dir.mkdir(parents=True, exist_ok=True)
        (args.icons_dir / 'items.txt').write_text('\n'.join(required_icons(all_recipes)) + '\n')
        print(f'{len(required_icons(all_recipes))} icônes à rendre dans {args.icons_dir}')
        return
    if not (args.wiki_dir / 'Home.md').is_file():
        raise ValueError('The wiki clone must contain Home.md')
    missing = [name for name in required_icons(all_recipes) if not (args.icons_dir / (name.replace(':', '_') + '.png')).is_file()]
    if missing:
        raise ValueError(f'{len(missing)} icônes manquantes. Lancer --prepare-icons puis le test client WikiAssetsClientGameTest.')
    renderer = Renderer(args.minecraft_jar, args.icons_dir, args.language_file)
    grouped = defaultdict(list)
    manifest = []
    for recipe in all_recipes:
        renderer.render(recipe, args.wiki_dir / recipe['image'])
        grouped[recipe['page']].append(recipe)
        manifest.append({'id': recipe['id'], 'image': recipe['image']})
    for page, (title, intro) in PAGES.items():
        text = f'# {title}\n\n{intro}\n\n**{len(grouped[page])} recettes**, illustrées avec les objets et blocs rendus dans Minecraft. Les quantités et les grilles proviennent directement des JSON du mod.\n\n[Comprendre les visuels et voir les catégories](Recettes).\n\n'
        if page == 'Recettes-cuivre':
            families = defaultdict(list)
            for recipe in grouped[page]:
                family = recipe['path'].parent.name
                # Some folder names end in a plural whereas the item uses a singular.
                families[family].append(recipe)
            for family, members in sorted(families.items()):
                name = renderer.name('minecraft:' + {'copper_slab': 'cut_copper_slab', 'copper_stairs': 'cut_copper_stairs'}.get(family, family))
                text += f'<details>\n<summary><strong>{html.escape(name)} — {len(members)} recettes</strong></summary>\n\n'
                text += ''.join(entry(recipe, renderer) for recipe in members) + '</details>\n\n'
        else:
            text += ''.join(entry(recipe, renderer) for recipe in grouped[page])
        text += '[Retour aux recettes](Recettes) · [Accueil](Home)\n'
        (args.wiki_dir / (page + '.md')).write_text(text)
    manifest_path = args.wiki_dir / 'images/recipes/manifest.json'
    manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + '\n')
    print(f'{len(all_recipes)} recettes illustrées dans {len(grouped)} pages.')


if __name__ == '__main__':
    version = next(line.split('=', 1)[1] for line in (ROOT / 'gradle.properties').read_text(errors='replace').splitlines() if line.startswith('minecraft_version='))
    cache = Path.home() / '.gradle/caches/fabric-loom'
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--wiki-dir', type=Path, default=ROOT / 'wiki')
    parser.add_argument('--icons-dir', type=Path, default=ROOT / 'build/wiki/item-icons')
    parser.add_argument('--minecraft-jar', type=Path, default=cache / version / 'minecraft-client.jar')
    parser.add_argument('--language-file', type=Path)
    parser.add_argument('--prepare-icons', action='store_true')
    args = parser.parse_args()
    if not args.prepare_icons and args.language_file is None:
        indexes = sorted((cache / 'assets/indexes').glob(version + '-*.json'))
        if not indexes:
            parser.error('Minecraft assets unavailable; specify --language-file and --minecraft-jar')
        index = json.loads(indexes[-1].read_text())
        language_hash = index['objects']['minecraft/lang/fr_fr.json']['hash']
        args.language_file = cache / 'assets/objects' / language_hash[:2] / language_hash
    generate(args)
