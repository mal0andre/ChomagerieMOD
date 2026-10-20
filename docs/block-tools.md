# Debug Stick de survie et verrouillage intégral des états

Ces fonctionnalités s'exécutent sur le serveur Fabric de Chomagerie, avec ses dépendances habituelles. Les joueurs peuvent utiliser un client vanilla 26.3 : aucun paquet personnalisé ni nouvel item n'est nécessaire. Les autres fonctionnalités client de Chomagerie restent optionnelles pour ces outils.

## Utilisation

La recette d'origine de Chomagerie est conservée pour obtenir **1 Debug Stick vanilla**, sous l'identifiant `chomagerie:utility/debug_stick` :

```text
  G N
R S G
S R
```

Disposition exacte : `" GN"`, `"RSG"`, `"SR "`. `G` = lingot d'or, `N` = lingot de netherite, `R` = poudre de redstone, `S` = bâton (deux or, une netherite, deux redstone et deux bâtons).

En Survie :

| Interaction | Résultat |
| --- | --- |
| Clic gauche | Propriété suivante, sans casser le bloc |
| Maj + clic gauche | Propriété précédente |
| Clic droit | Valeur suivante autorisée |
| Maj + clic droit | Valeur précédente autorisée |

Les messages de sélection sont ceux de Minecraft. La sélection est conservée dans le composant vanilla `minecraft:debug_stick_state`. Le clic gauche reprend le code de `SDSMod.java` de JustS : délai de cinq ticks dans `minecraft:custom_data.LastModified`, appel à `DebugStickItem.canDestroyBlock`, puis retour `PASS`. Une tentative refusée pendant ces cinq ticks ne décale pas le minuteur. Le traitement ajouté des paquets de fin de minage a été supprimé.

Le parcours des propriétés et des valeurs reprend `DebugStickMixin.java` de JustS, avec les fonctions de parcours vanilla et les propriétés du `StateDefinition`. Les noms Yarn ont été adaptés aux noms Mojang 26.3 ; l'écriture manuelle reste intégrée au verrou persistant. Les règles de configuration restent permissives par défaut et la recette d'origine de Chomagerie reste conservée.

Comme chez JustS, le Créatif opérateur utilise le fonctionnement vanilla ; le Créatif non opérateur utilise le parcours étendu selon la configuration. Le Spectateur et l’Aventure ne peuvent pas modifier les blocs avec ces outils.

Pour figer l'état de n'importe quel bloc :

1. Poser le bloc de son choix (vitre, escalier, levier, porte, coffre, culture, sable, etc.).
2. Cliquer dessus avec un rayon de miel : **tout son BlockState** est verrouillé. Un rayon est consommé en Survie, aucun en Créatif. Répéter sur un bloc déjà verrouillé ne coûte rien.
3. Ses propriétés (`facing`, `open`, `powered`, `age`, `waterlogged`, connexions, etc.) restent figées malgré les mises à jour ordinaires.
4. Modifier une propriété avec le Debug Stick : sa nouvelle valeur devient immédiatement la référence persistante du verrou.
5. Faire un **clic droit avec une hache** pour retirer le verrou. Ce clic est réservé au déverrouillage : il n'écorce pas le bloc et ne retire pas une autre cire en même temps. La hache perd un point de durabilité uniquement en Survie.

La hache sert donc à **annuler le verrouillage**, comme pour retirer de la cire. Les voisins sont recalculés et les ticks du bloc reprennent. Un bloc stable reste en place ; un état rendu physiquement impossible redevient soumis aux règles vanilla : par exemple, du sable sans support tombe après déverrouillage et une torche sans support peut tomber en objet.

Les sons et particules de cire vanilla et un message dans la barre d'action confirment l'opération. Le miel et la hache sont traités avant l'ouverture des coffres ou l'activation des portes. Le Debug Stick passe également avant ces interactions.

## Tous les types de blocs

Aucune liste de blocs ou de tags ne limite désormais le verrouillage : **tous les types de blocs sont acceptés**, y compris ceux sans propriété (pierre, bedrock, etc.). L'air n'est pas une cible cliquable. Le précédent tag `chomagerie:state_lockable` n'est plus utilisé.

Le verrou porte sur le **BlockState entier**, la même notion d'état que manipule le Debug Stick. Les ticks planifiés et aléatoires ainsi que les réactions de voisinage du bloc verrouillé sont suspendus. Les autres écritures changeant les propriétés du même bloc sont refusées. Cela couvre notamment la redstone, la croissance, l'orientation, l'humidité et les connexions.

Le contenu d'un coffre, les objets d'un four et les autres données d'entités de bloc ne font pas partie du BlockState : leur inventaire et leur traitement ne sont pas gelés. Le verrou ne rend pas le bloc indestructible : casse, explosion, remplacement et déplacement par piston peuvent le retirer et effacent son enregistrement. Les fluides autour du bloc et les autres blocs continuent de fonctionner.

## Configuration serveur

Le fichier **`config/chomagerie/block-tools.json`** est créé au démarrage, à côté de `players.json`. Redémarrer le serveur après modification. Les préférences client et `players.json` ne contrôlent pas ces fonctionnalités. Un fichier invalide est conservé pour correction et désactive les outils et leur recette ; il n'est pas remplacé par une configuration plus permissive.

La section `debugStick` contient :

| Option | Défaut / rôle |
| --- | --- |
| `enabled` | `true`, autoriser le Debug Stick en Survie |
| `craftingEnabled` | `true`, charger la recette Chomagerie |
| `whitelistMode` | `false`, autoriser ce qui n'est pas interdit |
| `allowedBlocks`, `forbiddenBlocks` | Listes d'entrées par bloc |
| `allowedTags`, `forbiddenTags` | Listes d'entrées par tag, sans `#` |
| `allowedProperties`, `forbiddenProperties` | Objets associant propriétés et listes de valeurs |

La configuration par défaut ne restreint aucun bloc, tag, propriété ou valeur : `whitelistMode` vaut `false`, les six listes/objets de règles sont vides. `waterlogged` est donc également autorisé. Les permissions de construction et les modes de jeu restent vérifiés.

Pour rendre une configuration existante permissive, définir `whitelistMode` à `false`, vider `allowedBlocks`, `forbiddenBlocks`, `allowedTags`, `forbiddenTags` (`[]`) et `allowedProperties`, `forbiddenProperties` (`{}`). Les configurations déjà présentes sur un autre serveur ne sont pas écrasées automatiquement. Les exemples ci-dessous permettent d'ajouter volontairement des restrictions.

Une entrée s'écrit par exemple :

```json
{
  "id": "minecraft:glass_pane",
  "properties": {
    "east": ["false", "true"],
    "north": ["all"]
  }
}
```

Dans une entrée autorisée, `"properties": {}` autorise toutes les propriétés et valeurs du bloc/tag, sous réserve des interdictions. Dans une entrée interdite, `{}` interdit le bloc/tag entier. Une liste `[]` n'autorise aucune valeur. `"all"` dans une liste désigne toutes les valeurs ; une clé de propriété `"all"` désigne toutes les propriétés. Les identifiants sans namespace désignent Minecraft.

**Les interdictions l'emportent toujours**, y compris les interdictions globales et par tag face à une autorisation explicite d'un bloc. Ensuite, les autorisations de bloc priment sur les autorisations par tag ; plusieurs entrées correspondantes sont appliquées conjointement. Une liste explicite de valeurs reste contraignante en mode blacklist. Une propriété sans valeur autorisée est ignorée pendant la sélection. Une sélection provenant d'un autre objet ou du Créatif est revalidée à chaque utilisation.

Exemple de règles globales :

```json
"allowedProperties": { "facing": ["north", "south"] },
"forbiddenProperties": { "waterlogged": ["all"], "powered": ["true"] }
```

La section `blockStateLocking` contient :

| Option | Défaut |
| --- | --- |
| `enabled` | `true` |
| `consumeHoneycomb` | `true` |
| `allowAxeUnlock` | `true` |
| `allowDebugStickEditing` | `true` (Survie et Créatif sur les blocs verrouillés) |
| `showFeedbackMessages` | `true` |
| `playEffects` | `true` |

L'ancienne option `blockStateLocking.allowedBlocks` est ignorée si elle figure encore dans un fichier existant. Elle ne restreint plus le verrouillage. `enabled: false` suspend les verrous et les interactions sans effacer les données. Les anciens verrous reprennent effet lors de la réactivation.

## Persistance, nettoyage et protections

Un `SavedData` propre à chaque dimension stocke les positions et leurs états de référence. L'accès utilise une table de hachage à clés `long`, sans scan des positions ni traitement global à chaque tick. Le jeu sauvegarde les données sales avec le monde ; les interactions ne déclenchent pas d'écriture disque immédiate. Le fichier est géré par Minecraft sous `data/chomagerie/block_state_locks.dat` dans chaque dimension.

Les recalculs de voisinage retournent l'état enregistré pour les positions verrouillées. Les ticks planifiés/aléatoires et les réactions de voisinage de ces positions sont suspendus. Une vérification dans `LevelChunk.setBlockState` refuse les autres changements de propriétés du même type de bloc, y compris ceux demandés par une entité de bloc. Cette vérification est nécessaire au verrouillage de tous les états ; elle effectue un accès direct à la table des verrous, sans parcours global ni boucle de correction.

Le chemin d'écriture explicite du Debug Stick publie la nouvelle référence avant l'écriture pour que tous ces contrôles restent cohérents, en Survie comme en Créatif. En cas d'échec de l'écriture, la référence précédente est restaurée.

Le remplacement par un autre type de bloc dans `LevelChunk.setBlockState` efface le verrou : destruction, explosion, piston ou commande passant par ce chemin vanilla. Le verrou **ne suit pas un piston**. Une commande tentant de modifier les propriétés du même type de bloc est également bloquée tant que le verrou existe ; déverrouiller le bloc avant cette opération. `/fill` et les éditeurs externes qui écrivent directement les sections de chunk en contournant `LevelChunk.setBlockState` ne sont pas garantis. Les modifications de fichiers de monde hors ligne ne sont pas surveillées.

Les modifications exigent le droit de construire, la portée d'interaction, la bordure du monde et `ServerLevel.mayInteract` (protection du spawn incluse). Le clic gauche laisse les callbacks Fabric de protection de la phase par défaut décider avant la phase Chomagerie. Le clic droit intervient dans l'utilisation de l'objet, après les callbacks `UseBlockCallback` et les contrôles réseau vanilla. Aucun système de zones n'est présent dans ce dépôt. Une protection tierce qui ne filtre que la casse, ou utilise une phase ultérieure personnalisée, doit également protéger les interactions ; une compatibilité universelle avec tous les mods de claims n'est pas supposée.

## Développement et vérification

Versions conservées : Minecraft **26.3**, Fabric Loader **0.19.5**, Fabric API **0.163.0+26.3**, Loom **1.17.1**, cible Java **25**. Minecraft 26.3 fournit les noms Mojang ; aucun mapping Yarn supplémentaire n'est utilisé.

Le projet sépare `src/main` et `src/client`. Les points d'entrée, les Mixins existants (inventaire/farmland), les recettes et la configuration joueur ont été examinés. La datagen existante n'a aucun fournisseur de recette : la recette est donc un JSON unique. Le stockage joueur existant est indépendant du monde, d'où un nouveau `SavedData` plutôt qu'une modification de `players.json`.

La suite dans `src/gametest` est un mod de test distinct, absent du JAR livré. `bash ./gradlew build` compile et lance les GameTests sur serveur dédié isolé dans `build/run/gameTest`. `bash ./gradlew runGameTest` permet de relancer ces tests seuls. Aucun monde utilisateur n'est utilisé.

Les tests automatisés ne remplacent pas une session de plusieurs vrais clients vanilla, une fermeture/réouverture complète du serveur ni une validation avec les plugins de zones du serveur cible. La persistance est testée par fermeture et réouverture du stockage Minecraft sur disque.

## Attribution

Adapté et inspiré de [JustS-js/debugStickSurvival](https://github.com/JustS-js/debugStickSurvival), branche `1.21`, commit `664555d601dc6a20929d85e4f176b1481fe15821`, sous licence **CC0-1.0**. Les fichiers `SDSMod.java`, `DebugStickMixin.java`, `Config.java`, `SDSConfig.java`, `SDSRecipeGenerator.java` et `sds.mixins.json` ont été lus avant l'implémentation. Le code de `SDSMod.java` et de `DebugStickMixin.java` a été repris pour les interactions, avec adaptation des noms Minecraft 26.3, des contrôles serveur et des écritures de verrous. Le minuteur `CUSTOM_DATA.LastModified` de cinq ticks est conservé. Les règles par bloc/tag/propriété/valeur restent disponibles, permissives par défaut ; la recette d’origine de Chomagerie est conservée. Aucune dépendance au mod SDS n'est ajoutée.
