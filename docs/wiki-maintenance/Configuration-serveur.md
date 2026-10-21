# Configuration serveur

## Fichiers et responsabilités

| Emplacement | Usage |
| --- | --- |
| `config/chomagerie/players.json` | Préférences synchronisées, présence du mod client et identité des tags, par UUID |
| `config/chomagerie/block-tools.json` | Politique du Debug Stick et du verrouillage au miel |
| Données du monde | Verrous persistants, scoreboard et règles de jeu |

Les outils de blocs génèrent leur configuration au premier démarrage. Depuis Chomagerie 1.6.1, utiliser **`/chomagerie reload`** après modification de `block-tools.json` (opérateur de niveau 2 minimum, ou console serveur). Ne pas confondre ces réglages avec `config/chomagerie.json`, qui est le fichier client.

## Configuration complète par défaut des outils

```json
{
  "debugStick": {
    "enabled": true,
    "craftingEnabled": true,
    "whitelistMode": false,
    "allowedBlocks": [],
    "forbiddenBlocks": [],
    "allowedTags": [],
    "forbiddenTags": [],
    "allowedProperties": {},
    "forbiddenProperties": {}
  },
  "blockStateLocking": {
    "enabled": true,
    "consumeHoneycomb": true,
    "allowAxeUnlock": true,
    "allowDebugStickEditing": true,
    "showFeedbackMessages": true,
    "playEffects": true
  }
}
```

**Aucun bloc, tag, propriété ou valeur n'est interdit par défaut au Debug Stick**, y compris `waterlogged`. Depuis Chomagerie 1.6.1, un nouveau verrou au miel exige au moins une propriété de bloc avec plusieurs valeurs ; les blocs sans propriété restent exclus, sans consommation de rayon pour verrouiller. L'ancienne option `blockStateLocking.allowedBlocks` est ignorée si elle subsiste dans un ancien fichier.

## Options des outils

| Option | Effet |
| --- | --- |
| `debugStick.enabled` | Activer le parcours étendu ; le Créatif opérateur conserve le chemin vanilla |
| `debugStick.craftingEnabled` | Charger ou retirer la recette Chomagerie du serveur |
| `debugStick.whitelistMode` | `true` refuse ce qui n'est pas explicitement autorisé ; `false` autorise le reste |
| `blockStateLocking.enabled` | Activer les interactions et la conservation des états |
| `consumeHoneycomb` | Consommer un rayon lors d'un nouveau verrouillage en Survie |
| `allowAxeUnlock` | Autoriser le déverrouillage à la hache |
| `allowDebugStickEditing` | Autoriser l'édition des blocs verrouillés, en Survie et Créatif |
| `showFeedbackMessages` | Afficher la confirmation dans la barre d'action |
| `playEffects` | Jouer sons et particules de cire vanilla |

Désactiver le verrouillage ne supprime pas les enregistrements du monde : les verrous reprennent effet à la réactivation.

## Ajouter volontairement des restrictions

Les listes `allowedBlocks`, `forbiddenBlocks`, `allowedTags` et `forbiddenTags` contiennent des entrées de cette forme :

```json
{
  "id": "minecraft:glass_pane",
  "properties": {
    "east": ["false", "true"],
    "north": ["all"]
  }
}
```

Les tags utilisent un identifiant **sans `#`**. Un identifiant sans namespace désigne Minecraft. Dans une entrée autorisée, `"properties": {}` donne accès à toutes les propriétés ; dans une entrée interdite, `{}` refuse le bloc ou le tag entier. Une liste de valeurs `[]` n'autorise aucune valeur. `"all"` désigne toutes les valeurs dans une liste, ou toutes les propriétés lorsqu'il s'agit d'une clé.

Exemple de fragment pour interdire volontairement la modification de l'eau contenue dans un bloc :

```json
{
  "forbiddenProperties": {
    "waterlogged": ["all"]
  }
}
```

Ce fragment appartient à `debugStick`, il ne remplace pas à lui seul le fichier complet. **Les interdictions priment toujours**. Ensuite, les règles de bloc priment sur les règles de tag ; plusieurs entrées correspondantes sont appliquées conjointement. Une liste explicite de valeurs reste contraignante en mode blacklist.

Au démarrage, un fichier d'outils invalide désactive les outils et leur recette, sans écraser le fichier. Lors d’un rechargement, une erreur laisse au contraire les **derniers réglages actifs** en place. La commande indique le fichier et le champ fautif ; une erreur de syntaxe JSON fournit aussi sa ligne et sa colonne. Les détails sont disponibles dans les logs.

Les protections vanilla et les callbacks Fabric restent applicables. Voir [Verrouillage des blocs](Verrouillage-des-blocs), [Protection des cultures](Protection-des-cultures) et [Sauvegardes et migrations](Sauvegardes-et-migrations).

## Recharger sans redémarrer (1.6.1)

1. Modifier `config/chomagerie/block-tools.json` sur le serveur, puis enregistrer le fichier.
2. Exécuter `/chomagerie reload` en jeu avec les permissions opérateur de niveau 2, ou `chomagerie reload` dans la console.
3. Attendre le message « Réglages appliqués ». En cas d’erreur, corriger le champ indiqué puis relancer la commande.

Les restrictions du Debug Stick, la consommation de miel, les effets et l’activation du verrouillage sont mis à jour à chaud. Les verrous sauvegardés ne sont pas supprimés. Si `debugStick.craftingEnabled` change, la commande utilise automatiquement le rechargement vanilla des données Minecraft pour mettre à jour les recettes et les clients ; cela peut prendre quelques instants et déclenche aussi les traitements habituels des datapacks. Un second rechargement est refusé pendant cette opération. Si elle échoue, les réglages précédents sont rétablis et l’erreur est signalée.

Exemple d’erreur : `blockStateLocking.consumeHoneycomb : true ou false est attendu (sans guillemets)`. Écrire `true`, et non `"true"`. Les noms d’options inconnus sont refusés pour détecter les fautes de frappe ; l’ancienne option supprimée `blockStateLocking.allowedBlocks` reste tolérée et ignorée. Les options omises conservent leurs valeurs par défaut.

La commande recharge uniquement `block-tools.json`. Les préférences joueurs (`players.json`) sont gérées par le mod, les règles de culture s’appliquent directement avec `/gamerule`, et les réglages client restent dans leur menu. Un client vanilla peut utiliser cette commande sur un serveur équipé du mod.
