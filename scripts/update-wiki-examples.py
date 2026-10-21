#!/usr/bin/env python3
"""Publish actual GameTest screenshots and inventory illustrations into the local wiki clone."""
import argparse
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
FONT = '/usr/share/fonts/truetype/freefont/FreeSans.ttf'
BOLD = '/usr/share/fonts/truetype/freefont/FreeSansBold.ttf'


def latest(folder, name):
    matches = list(folder.glob('*_' + name + '.png'))
    if not matches:
        raise ValueError(f'No screenshot for {name}; run the optional wiki asset export first')
    return max(matches, key=lambda p: p.stat().st_mtime_ns)


def main(args):
    output = args.wiki_dir / 'images/examples'
    output.mkdir(parents=True, exist_ok=True)
    screenshots = {
        'overlay-off': 'locks-disabled', 'overlay-red': 'locks-through-wall',
        'overlay-blue': 'locks-custom-blue', 'overlay-settings': 'locks-configuration',
        'target-padlock': 'target-lock-icon', 'target-padlock-off': 'target-lock-icon-disabled',
        'fence-before': 'wiki-lock-01-before', 'fence-honey': 'wiki-lock-02-honey',
        'fence-debug-stick': 'wiki-lock-03-debug-stick', 'fence-axe': 'wiki-lock-04-axe',
    }
    for name, source in screenshots.items():
        with Image.open(latest(args.screenshots, source)) as picture:
            picture.save(output / (name + '.png'), optimize=True)

    # Comparisons retain each entire frame; labels live outside the original screenshot.
    def comparison(filename, entries):
        width, shot_width, shot_height = 1200, 588, 330
        canvas = Image.new('RGB', (width, (shot_height + 54) * ((len(entries) + 1) // 2) + 12), '#f6f5f0')
        draw = ImageDraw.Draw(canvas)
        for index, (name, label) in enumerate(entries):
            x, y = 8 + index % 2 * 600, index // 2 * (shot_height + 54)
            draw.text((x, y + 12), label, font=ImageFont.truetype(BOLD, 22), fill='#292c30')
            image = Image.open(output / (name + '.png')).convert('RGB')
            image = image.resize((shot_width, shot_height), Image.Resampling.LANCZOS)
            canvas.paste(image, (x, y + 48))
        canvas.save(output / filename, optimize=True)

    comparison('overlay-comparison.png', [('overlay-off', 'Overlay désactivé'), ('overlay-red', 'Overlay activé : escalier derrière le mur')])
    comparison('locking-steps.png', [('fence-before', '1. Barrière isolée — rayon de miel en main'),
                                    ('fence-honey', '2. Verrouillée — la nouvelle voisine ne la relie pas'),
                                    ('fence-debug-stick', '3. Debug Stick — connexion modifiée, verrou gardé'),
                                    ('fence-axe', '4. Hache — verrou retiré, comportement vanilla')])
    full = Image.open(output / 'target-padlock.png').convert('RGB')
    cx, cy = full.width // 2, full.height // 2
    crop = full.crop((cx - 52, cy - 38, cx + 88, cy + 46)).resize((560, 336), Image.Resampling.NEAREST)
    zoom = Image.new('RGB', (584, 394), '#f6f5f0')
    draw = ImageDraw.Draw(zoom)
    draw.text((12, 12), 'Cadenas près du viseur · détail agrandi ×4', font=ImageFont.truetype(BOLD, 23), fill='#292c30')
    zoom.paste(crop, (12, 46))
    zoom.save(output / 'target-padlock-zoom.png', optimize=True)

    tools = Image.new('RGB', (1020, 236), '#c6c6c6')
    draw = ImageDraw.Draw(tools)
    for index, (item, title, subtitle) in enumerate([
        ('honeycomb', 'Rayon de miel', 'Verrouiller l’état'), ('debug_stick', 'Debug Stick', 'Modifier en gardant le verrou'),
        ('iron_axe', 'Hache', 'Retirer le verrou')]):
        x = index * 340
        draw.rectangle((x + 132, 24, x + 208, 100), fill='#373737')
        draw.rectangle((x + 135, 27, x + 208, 100), fill='#ffffff')
        draw.rectangle((x + 135, 27, x + 205, 97), fill='#8b8b8b')
        icon = Image.open(args.icons_dir / ('minecraft_' + item + '.png')).convert('RGB').resize((64, 64), Image.Resampling.NEAREST)
        tools.paste(icon, (x + 138, 30))
        draw.text((x + 170, 120), title, font=ImageFont.truetype(BOLD, 27), anchor='mt', fill='#292929')
        draw.text((x + 170, 166), subtitle, font=ImageFont.truetype(FONT, 22), anchor='mt', fill='#424242')
        if index < 2:
            draw.polygon([(x + 316, 55), (x + 332, 65), (x + 316, 75)], fill='#6b6b6b')
    tools.save(output / 'locking-tools.png', optimize=True)
    print(f'{len(screenshots)} captures et 4 illustrations écrites dans {output}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--wiki-dir', type=Path, default=ROOT / 'wiki')
    parser.add_argument('--screenshots', type=Path, default=ROOT / 'build/run/clientGameTest/screenshots')
    parser.add_argument('--icons-dir', type=Path, default=ROOT / 'build/wiki/item-icons')
    main(parser.parse_args())
