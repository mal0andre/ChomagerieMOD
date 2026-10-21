#!/usr/bin/env python3
"""Prepare unlabelled game screenshots and native inventory illustrations for the player wiki."""
import argparse
import io
from pathlib import Path
from zipfile import ZipFile
from PIL import Image, ImageDraw, ImageOps

ROOT = Path(__file__).resolve().parents[1]


def latest(folder, name):
    matches = list(folder.glob('*_' + name + '.png'))
    return max(matches, key=lambda p: p.stat().st_mtime_ns) if matches else None


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
        screenshot = latest(args.screenshots, source)
        target = output / (name + '.png')
        if screenshot:
            with Image.open(screenshot) as picture:
                picture.save(target, optimize=True)
        elif not target.is_file():
            raise ValueError(f'Missing screenshot {source}; export the wiki assets first')

    def comparison(filename, entries):
        # Keep the game frames intact. Captions belong in Markdown, never on the image.
        width, height, gap = 588, 330, 12
        rows = (len(entries) + 1) // 2
        canvas = Image.new('RGB', (width * 2 + gap, height * rows + gap * (rows - 1)), '#c6c6c6')
        for index, name in enumerate(entries):
            with Image.open(output / (name + '.png')) as picture:
                frame = ImageOps.contain(picture.convert('RGB'), (width, height), Image.Resampling.LANCZOS)
            x = index % 2 * (width + gap) + (width - frame.width) // 2
            y = index // 2 * (height + gap) + (height - frame.height) // 2
            canvas.paste(frame, (x, y))
        canvas.save(output / filename, optimize=True)

    comparison('overlay-comparison.png', ['overlay-off', 'overlay-red'])
    comparison('locking-steps.png', ['fence-before', 'fence-honey', 'fence-debug-stick', 'fence-axe'])
    with Image.open(output / 'target-padlock.png') as full:
        cx, cy = full.width // 2, full.height // 2
        full.crop((cx - 52, cy - 38, cx + 88, cy + 46)).resize(
            (560, 336), Image.Resampling.NEAREST).save(output / 'target-padlock-zoom.png', optimize=True)

    with ZipFile(args.minecraft_jar) as archive:
        texture = Image.open(io.BytesIO(archive.read(
            'assets/minecraft/textures/gui/container/crafting_table.png'))).convert('RGBA')
    tools = texture.crop((0, 0, 176, 166))
    ImageDraw.Draw(tools).rectangle((8, 8, 167, 75), fill='#c6c6c6')
    slot = texture.crop((29, 16, 47, 34))
    for x, item in zip((43, 79, 115), ('honeycomb', 'debug_stick', 'iron_axe')):
        tools.paste(slot, (x, 34))
        with Image.open(args.icons_dir / ('minecraft_' + item + '.png')) as icon:
            tools.paste(icon.resize((16, 16), Image.Resampling.NEAREST), (x + 1, 35))
    tools.resize((704, 664), Image.Resampling.NEAREST).save(output / 'locking-tools.png', optimize=True)
    print(f'{len(screenshots)} screenshots and 4 unlabelled illustrations prepared in {output}')


if __name__ == '__main__':
    version = next(line.split('=', 1)[1] for line in (ROOT / 'gradle.properties').read_text(errors='replace').splitlines()
                   if line.startswith('minecraft_version='))
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--wiki-dir', type=Path, default=ROOT / 'wiki')
    parser.add_argument('--screenshots', type=Path, default=ROOT / 'build/run/clientGameTest/screenshots')
    parser.add_argument('--icons-dir', type=Path, default=ROOT / 'build/wiki/item-icons')
    parser.add_argument('--minecraft-jar', type=Path, default=Path.home() / '.gradle/caches/fabric-loom' / version / 'minecraft-client.jar')
    main(parser.parse_args())
