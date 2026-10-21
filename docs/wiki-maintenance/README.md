# Wiki maintenance

The GitHub Wiki is an English player guide. Keep developer procedures, server configuration internals and publication instructions in this repository.

The original French technical notes are preserved here for reference:

- [Server configuration](Configuration-serveur.md)
- [Backups and migration](Sauvegardes-et-migrations.md)
- [Development and tests](Developpement-et-tests.md)
- [Previous publication workflow](Publication-du-wiki.md)
- [Historical release notes](Historique.md)

These are archived notes; their old wiki page links and old image dimensions may no longer apply. Use the commands below for the current workflow.

## Regenerate player assets

The `wiki/` directory is a separate Git checkout. Recipe illustrations use Minecraft's workstation textures and rendered item icons, with an empty player inventory. Do not draw titles, ingredient names, captions or explanatory text onto images. Native stack counts and numbers built into item icons are retained. Put explanations in Markdown.

```bash
python3 scripts/update-wiki-recipes.py --prepare-icons
CHOMAGERIE_WIKI_ASSETS="$PWD/build/wiki/item-icons" bash gradlew runClientGameTest
python3 scripts/update-wiki-recipes.py
python3 scripts/update-wiki-examples.py
python3 scripts/verify-wiki.py
```

If item icons and screenshots are already available, only run the final three commands. The recipe generator defaults to Minecraft's English names. Its optional `--language-file` argument overrides the item-name dictionary.

The validator checks navigation, image integrity, full workstation dimensions (704 × 664), and exact coverage of the source recipes. Review the code and wiki checkouts separately. Keep the pages and their `images/` directory together when publishing the wiki.

## Offline preview

With Node.js and the `marked` package available, run `node scripts/preview-wiki.cjs`. Open `build/wiki/preview/index.html` to browse all player pages locally. Images are copied into the preview, so it does not depend on the currently published GitHub Wiki.
