# Publication et maintenance du wiki

Le wiki GitHub possède son **propre dépôt Git**, distinct du dépôt de code :

```text
https://github.com/mal0andre/ChomagerieMOD.wiki.git
```

Les fichiers Markdown à la racine deviennent les pages du wiki. Le dossier **`images/` doit être publié avec les pages** : il contient les recettes illustrées et les captures de jeu. `Home.md` est l'accueil ; `_Sidebar.md` fournit la navigation et `_Footer.md` le pied de page.

## Publier la copie préparée

Depuis le dossier du projet, si le clone `wiki/` a déjà été préparé et commité :

```bash
git -C wiki status
git -C wiki push origin master
```

L'authentification utilisée est celle de Git sur ton ordinateur. Ne pas placer de mot de passe ou de token dans les pages du wiki, ni les commiter dans le dépôt. Aucun changement du dépôt de code n'est envoyé par cette commande.

## Modifier une page ensuite

```bash
git -C wiki pull --ff-only
# Modifier les fichiers Markdown concernés dans wiki/.
git -C wiki add --all
git -C wiki commit -m "docs: update Chomagerie wiki"
git -C wiki push origin master
```

Sur une autre machine, cloner le dépôt `.wiki.git`. Il doit avoir été initialisé par une première page sur GitHub avant le premier clone. Ne pas forcer le push : récupérer d'abord les pages modifiées sur GitHub et résoudre les éventuels conflits.

## Mettre à jour le catalogue des recettes

Les images sont générées depuis les recettes JSON et le rendu d’inventaire de Minecraft 26.3. Elles ne sont pas des dessins approximatifs des objets. Les grilles utilisent les textures d’interface du jeu ; les quantités blanches utilisent ses chiffres d’inventaire.

Prérequis : Python avec **Pillow**, un JDK compatible, les ressources Minecraft préparées par Gradle et un environnement graphique. Sous Linux, le rendu hors écran peut utiliser SDL. Le script utilise la police FreeSans installée sur la machine pour les titres.

```bash
# Préparer la liste des objets et variantes de lumière à rendre.
python3 scripts/update-wiki-recipes.py --prepare-icons

# Exporter les icônes et les scènes en jeu dans un monde temporaire.
CHOMAGERIE_WIKI_ASSETS="$PWD/build/wiki/item-icons" \
SDL_VIDEODRIVER=offscreen SDL_AUDIODRIVER=dummy \
bash ./gradlew runClientGameTest

# Construire les pages et leurs images, puis les vérifier.
python3 scripts/update-wiki-recipes.py
python3 scripts/update-wiki-examples.py
python3 scripts/verify-wiki.py
```

Le test `WikiAssetsClientGameTest` exporte les icônes uniquement lorsque `CHOMAGERIE_WIKI_ASSETS` est défini ; il n’ajoute aucun écran au mod livré. Les autres tests continuent de vérifier le rendu de l’overlay. Les scènes montrent des clics au miel, au Debug Stick et à la hache sur une barrière. L’exemple derrière un mur utilise un escalier, qui possède des propriétés modifiables.

Lorsque seuls les textes de recettes changent et que toutes les icônes sont déjà exportées, relancer uniquement les deux scripts de génération puis le vérificateur. Les chemins Minecraft, le fichier de traduction française, le dossier d’icônes et le clone du wiki peuvent être précisés avec les options des scripts (`--help`).

Le vérificateur contrôle les liens de pages, les exemples JSON, la couverture des **291 recettes**, l’intégrité des PNG référencés et les sections dépliables. Les URL d’images utilisent `raw.githubusercontent.com/wiki/mal0andre/ChomagerieMOD/` ; le wiki et ses images doivent donc être envoyés ensemble. Un aperçu local ou une archive peut être utilisé avant publication. Vérifier le diff dans le dépôt de code **et** dans le dépôt du wiki.

## Règles de rédaction

- Faire correspondre la documentation au code effectivement distribué et signaler les nouveautés Unreleased.
- Mettre à jour les liens de navigation lorsqu'une page est ajoutée ou renommée.
- Vérifier les exemples JSON, les noms d'options et les permissions dans les classes concernées.
- Séparer les résultats de tests exécutés des scénarios encore à vérifier en jeu.
- Conserver les attributions et les limites du verrouillage intégral.

Référence : [GitHub — ajouter ou modifier des pages de wiki](https://docs.github.com/en/communities/documenting-your-project-with-wikis/adding-or-editing-wiki-pages).
