# Fichiers et validation des outils de blocs

## Fichiers modifiés

- `build.gradle` : source set GameTest séparé, tests serveur dans `check`, préservation des échappements JSON lors de l'expansion des métadonnées.
- `src/main/java/tech/maloandre/chomagerie/Chomagerie.java` : chargement de la configuration et enregistrement du callback d'attaque.
- `src/main/resources/chomagerie.mixins.json` : déclaration des six Mixins ciblés.
- `README.md` : instructions d'utilisation et lien vers la configuration détaillée.
- `CHANGELOG.md` : entrée `Unreleased` décrivant les fonctionnalités et la validation.

La recette existante `src/main/resources/data/chomagerie/recipe/utility/debug_stick.json` est conservée à l’identique (or, netherite, redstone et bâtons), sans doublon.

## Fichiers créés

Sous `src/main/java/tech/maloandre/chomagerie/blockstate/` :

- `BlockToolsConfig.java` : configuration serveur et règles de restriction.
- `SurvivalDebugStick.java` : callback `SDSMod.java` repris de JustS, timer `LastModified` et permissions.
- `BlockStateLocks.java` : persistance par dimension et conservation de tous les BlockStates.
- `LockInteractions.java` : verrouillage au miel, déverrouillage à la hache, consommation, usure et effets.

Sous `src/main/java/tech/maloandre/chomagerie/mixin/` :

- `DebugStickMixin.java` : code de parcours de JustS porté aux noms Mojang 26.3, règles permissives et écriture dans le verrou.
- `BlockStateLockMixin.java` : conservation intégrale des états et suspension des ticks/réactions de voisinage.
- `LockCleanupMixin.java` : garde des états verrouillés et nettoyage après remplacement de bloc dans les chunks.
- `BlockToolItemUseMixin.java` : interaction avec le miel et la hache dans le parcours d'utilisation d'objet.
- `BlockToolInteractionMixin.java` : traitement des outils avant activation des blocs interactifs et après les callbacks de protection.
- `DebugStickRecipeMixin.java` : suppression de la recette de la table serveur si désactivée.

Autres fichiers :

- `src/gametest/java/tech/maloandre/chomagerie/BlockToolsGameTests.java` : tests fonctionnels dans Minecraft.
- `src/gametest/resources/fabric.mod.json` : entrée du mod de test, exclu du JAR livré.
- `docs/block-tools.md` : usage, configuration, architecture et attribution du projet source.
- `docs/block-tools-validation.md` : présent inventaire et bilan de validation.

## Résultats exécutés

- `bash ./gradlew build --offline` : **BUILD SUCCESSFUL** ; **16 GameTests requis réussis** (15 tests Chomagerie et le test du framework Fabric), dont les nouveaux tests START/ABORT + clic droit de toutes les propriétés des vitres.
- Chargement des six nouveaux Mixins sur environnement **SERVER** : validé par le serveur GameTest.
- Tous les types non-air du registre vérifiés sans liste de compatibilité ; snapshots complets contrôlés pour chacune de leurs propriétés.
- Compilation des source sets principal, client et GameTest : réussie.
- Le JDK installé 26 est utilisé avec la cible Java 25 conservée.
- JAR inspecté : JSON valides, ancien tag restrictif absent, nouveau Mixin interactif présent, recette d’origine conservée et aucune classe de test livrée.
- `git diff --check` : aucune erreur d’espacement.
- Aucun test JUnit existant : la tâche `test` indique `NO-SOURCE`. Les tests de comportement exécutés sont les GameTests.

## Couverture des GameTests

| Test | Vérifications |
| --- | --- |
| `paneLockDebugAndUnlock` | Miel, consommation unique, voisin non verrouillé, Debug Stick, référence mise à jour, hache, usure et retour au comportement normal |
| `allConnectionFamiliesAndAdjacentLocks` | Tous les blocs du tag par défaut : vitres et couleurs, fer, barrières et murets ; deux verrous adjacents |
| `replacementCleanupAndPropertylessBlock` | Verrouillage de la pierre sans propriétés, remplacement direct, destruction, nouvelle pose sans héritage |
| `spectatorAdventureCreativeAndAttackDebounce` | Survie, Spectateur, Aventure, Créatif non opérateur, gratuité en Créatif et attaques répétées |
| `restrictionsAndUntrustedSelection` | Configuration par défaut permissive (bûches et `waterlogged` inclus), interdictions explicites par tag et propriété, listes de valeurs en blacklist |
| `fabricProtectionVetoAndEditingSwitch` | Veto Fabric enregistré après Chomagerie et désactivation de l'édition des blocs verrouillés |
| `craftingRecipeAndDisableSwitch` | Recette d’origine avec disposition imposée, absence du craft bâton/chorus et retrait effectif de la recette lorsqu'elle est désactivée |
| `reverseCyclingAndNoAllowedValues` | Maj inverse les propriétés/valeurs, propriétés sans valeurs autorisées refusées sans boucle |
| `justSSelectionCyclesEveryPaneProperty` | START/ABORT de minage à cinq ticks d'intervalle, sélection de `east`, `west`, `north`, `south`, `waterlogged`, édition ciblée et Maj en sens inverse |
| `justSSelectionCyclesEveryLockedPaneProperty` | Même parcours sur une vitre verrouillée ; chaque édition devient la référence persistante et les clics refusés ne décalent pas le minuteur |
| `everyRegisteredBlockAndEveryPropertyCanLock` | Tous les types non-air du registre, toutes leurs propriétés et restauration d'un état complet |
| `interactiveBlocksAndFullStateGuard` | Miel/hache/Debug Stick sur coffres, portes, leviers, fours, escaliers et cultures ; écritures ordinaires refusées |
| `gravityAndScheduledTicksResumeAfterUnlock` | Sable verrouillé suspendu puis gravité réactivée au déverrouillage |
| `fullSnapshotSurvivesNeighborsAndFailedManualWrite` | `waterlogged` et connexions figés ensemble, édition manuelle et restauration de la référence après écriture impossible |
| `savedDataDiskRoundTrip` | Écriture, fermeture et réouverture du stockage Minecraft sur disque, restauration de la valeur de référence |

Les tests sont exécutés sur un serveur GameTest Fabric dédié avec le JDK installé **26.0.2.1**, en conservant la **cible Java 25** du projet. Le bytecode livré est de version 69 (Java 25). Les versions Minecraft/Fabric du dépôt ne sont pas modifiées.

La validation ne comprend pas de session graphique multijoueur avec de vrais clients vanilla, de redémarrage complet d'un serveur de production, de piston/explosion exécuté en jeu ou de rechargement effectif de chunk. Le chemin commun de remplacement est testé ; les limites des éditeurs externes et commandes remplaçant un bloc par le même type sont détaillées dans `block-tools.md`.
