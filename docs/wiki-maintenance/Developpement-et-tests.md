# Développement et tests

## Structure et versions

Le mod ID est `chomagerie`, avec les packages `tech.maloandre.chomagerie`. Le projet sépare `src/main` (serveur et code commun), `src/client` (interface et réseau client) et `src/gametest` (mod de test distinct, non livré dans le JAR de production).

Minecraft 26.3 utilise ici les noms Mojang ; aucun mapping Yarn supplémentaire n'est ajouté. La cible est Java 25, avec Fabric Loader 0.19.5, Fabric API 0.163.0+26.3 et Loom 1.17.1.

## Compiler

Depuis la racine du dépôt avec un JDK approprié :

```bash
bash ./gradlew build
```

Le JAR de production est `build/libs/chomagerie-1.6.1.jar`, selon la version actuelle de `gradle.properties`. Le JAR `-sources.jar` contient les sources et ne sert pas à installer le mod.

Pour lancer seulement les GameTests :

```bash
bash ./gradlew runGameTest
```

Les tests utilisent un serveur dédié isolé dans `build/run/gameTest`. La tâche `build` inclut les GameTests via `check`. La tâche JUnit `test` indique `NO-SOURCE` car aucun test JUnit n'est défini.

## Dernière validation exécutée

Le 20 octobre 2026, la commande `bash ./gradlew build --offline` pour la version **1.6.0** a réussi avec **18 GameTests requis réussis** : 17 tests Chomagerie et le test du framework Fabric. Les nouveaux Mixins ont été chargés dans l'environnement SERVER. Les JSON du JAR ont été parsés, la recette d'origine vérifiée et les classes de test sont absentes du JAR livré.

Le JDK local utilisé pour cette validation est 26.0.2.1, tout en compilant vers Java 25 (bytecode 69). Cela ne remplace pas une exécution manuelle sur chaque environnement serveur.

## Architecture des outils

| Classe | Responsabilité |
| --- | --- |
| `BlockToolsConfig` | Fichier serveur, règles et valeurs permissives par défaut |
| `SurvivalDebugStick` | Callback d'attaque JustS et contrôles d'édition |
| `DebugStickMixin` | Parcours JustS des propriétés et valeurs, adapté à 26.3 |
| `BlockStateLocks` | SavedData par dimension, index par chunk et écritures manuelles cohérentes |
| `LockOverlaySync` | Abonnements client, portée limitée et synchronisation différentielle des positions |
| `LockOverlay` | Cache client, géométrie extraite et surbrillance de couleur personnalisable à travers les blocs |
| `LockInteractions` | Miel, hache, consommation, usure et effets |
| `BlockStateLockMixin` | Conservation des états et suspension des ticks/réactions de voisinage |
| `LockCleanupMixin` | Refus des écritures de propriétés et nettoyage des remplacements |
| `BlockToolInteractionMixin` | Priorité des outils sur coffres, portes et autres interactions |
| `BlockToolItemUseMixin` | Parcours d'utilisation d'objet pour les verrous |
| `DebugStickRecipeMixin` | Retrait de la recette de la table serveur lorsque désactivée |

Le stockage n'utilise ni scan de tous les verrous à chaque tick ni écriture disque immédiate après chaque interaction. Les paquets vanilla suffisent pour les outils de blocs. L’overlay utilise des paquets dédiés uniquement pour les joueurs qui l’activent et annoncent leur compatibilité. Les fonctions historiques de préférences, tags et refill possèdent leur protocole dédié.

## Couverture des GameTests

| Test | Vérifications |
| --- | --- |
| `paneLockDebugAndUnlock` | Miel, consommation unique, voisin non verrouillé, Debug Stick, référence mise à jour, hache, usure et retour au comportement normal |
| `allConnectionFamiliesAndAdjacentLocks` | Vitres et couleurs, fer, barrières et murets vanilla ; deux verrous adjacents |
| `replacementCleanupAndPropertylessBlock` | Refus sans consommation sur pierre, sable, terre et verre ; retrait des anciens verrous, cirage vanilla du cuivre, remplacement et destruction |
| `spectatorAdventureCreativeAndAttackDebounce` | Survie, Spectateur, Aventure, Créatif non opérateur, gratuité en Créatif et attaques répétées |
| `restrictionsAndUntrustedSelection` | Configuration par défaut permissive (bûches et `waterlogged` inclus), interdictions explicites par tag et propriété, listes de valeurs en blacklist |
| `fabricProtectionVetoAndEditingSwitch` | Veto Fabric enregistré après Chomagerie et désactivation de l'édition des blocs verrouillés |
| `craftingRecipeAndDisableSwitch` | Recette d’origine avec disposition imposée, absence du craft bâton/chorus et retrait effectif de la recette lorsqu'elle est désactivée |
| `reverseCyclingAndNoAllowedValues` | Maj inverse les propriétés/valeurs, propriétés sans valeurs autorisées refusées sans boucle |
| `justSSelectionCyclesEveryPaneProperty` | START/ABORT de minage à cinq ticks d'intervalle, sélection de `east`, `west`, `north`, `south`, `waterlogged`, édition ciblée et Maj en sens inverse |
| `justSSelectionCyclesEveryLockedPaneProperty` | Même parcours sur une vitre verrouillée ; chaque édition devient la référence persistante et les clics refusés ne décalent pas le minuteur |
| `everyRegisteredBlockAndEveryPropertyCanLock` | Éligibilité de tous les types non-air du registre ; toutes les propriétés et restauration complète des blocs modifiables |
| `interactiveBlocksAndFullStateGuard` | Miel/hache/Debug Stick sur coffres, portes, leviers, fours, escaliers et cultures ; écritures ordinaires refusées |
| `gravityAndScheduledTicksResumeAfterUnlock` | Enclume verrouillée suspendue puis gravité réactivée au déverrouillage |
| `fullSnapshotSurvivesNeighborsAndFailedManualWrite` | `waterlogged` et connexions figés ensemble, édition manuelle et restauration de la référence après écriture impossible |
| `overlayRangeIndexAndCleanup` | Portée sphérique, coordonnées négatives, chunks indisponibles, suppression et recréation des entrées |
| `overlayPayloadBoundsAndRoundTrip` | Sérialisation de 4096 positions, dimensions, reset, limites de portée et rejet des tailles invalides |
| `reloadValidatesPreservesAndUpdatesRecipes` | Commande opérateur niveau 2, fichier absent, JSON invalide, erreurs de champs, configuration active préservée, application immédiate, verrous conservés et recette mise à jour puis restaurée sans redémarrage |
| `savedDataDiskRoundTrip` | Écriture, fermeture et réouverture du stockage Minecraft sur disque, restauration de la valeur de référence |

Les tests sont exécutés sur un serveur GameTest Fabric dédié avec le JDK installé **26.0.2.1**, en conservant la **cible Java 25** du projet. Le bytecode livré est de version 69 (Java 25). Les versions Minecraft/Fabric du dépôt ne sont pas modifiées.

La validation ne comprend pas de session graphique multijoueur avec de vrais clients vanilla, de redémarrage complet d'un serveur de production, de piston/explosion exécuté en jeu ou de rechargement effectif de chunk. Le chemin commun de remplacement est testé ; les limites des éditeurs externes et commandes remplaçant un bloc par le même type sont détaillées dans [Verrouillage des blocs](Verrouillage-des-blocs).

## Test graphique de l’overlay

```bash
bash ./gradlew runClientGameTest
```

Ce test facultatif ouvre un monde plat temporaire dans `build/run/clientGameTest`, compare les pixels rouges de captures avec un bloc verrouillé derrière un mur, puis vérifie la portée, le changement immédiat vers le bleu, le déverrouillage et la désactivation. Il ouvre aussi le menu avec **K**, sélectionne l’onglet de l’overlay et vérifie la sauvegarde des contrôles, y compris la couleur et son rechargement. Les captures sont enregistrées par le framework dans le dossier de test. Cette tâche nécessite un environnement graphique compatible ; elle ne fait pas partie de `build`.

Le test client a également réussi le 20 octobre 2026 en rendu OpenGL hors écran (`SDL_VIDEODRIVER=offscreen SDL_AUDIODRIVER=dummy`). La capture `locks-through-wall` montre le bloc rouge derrière le mur de pierre ; les captures hors portée, déverrouillée et désactivée ne contiennent plus cette surbrillance.

## Nouveautés de Chomagerie 1.6.1

- `BlockToolHud` dessine un petit cadenas de 7 × 9 pixels GUI à droite du viseur, sans police ni texture externe, et utilise un petit canal distinct pour le verrou du seul bloc visé. Les propriétés du Debug Stick restent dans les messages vanilla.
- `TargetLockSync` vérifie la dimension, la portée et les chunks déjà chargés ; il ne transmet aucun statut aux clients sans canal compatible et ne charge pas de chunks pour une demande.
- `BlockStateLocks` conserve des révisions par chunk en mémoire. `LockOverlaySync` ne recalcule la sélection qu’après changement de position, de configuration, de chargement de chunks ou de révision locale.
- `LockOverlayDelta` étale le transfert complet sur plusieurs passages bornés, en gardant la trace des positions réellement envoyées ; un déplacement annule les anciennes additions en attente.
- Le cache client conserve les formes statiques, actualise les formes dynamiques par tick et utilise le frustum géométrique de la caméra, sans cacher les verrous derrière les murs.

Les nouveaux tests `overlayRevisionAndProgressiveDelivery` et `targetedLockAndServerSelection` vérifient notamment 20 000 positions, les budgets réseau, le déplacement pendant un transfert, les révisions de chunks, les paquets et le refus de cibles distantes ou d’une autre dimension. `BlockToolsHudClientGameTest` vérifie les pixels du cadenas, l’absence de texte supplémentaire sous le viseur, le verrou sans overlay, F1, les menus, la vue à la troisième personne, la désactivation de l’icône et la réaffectation du raccourci.

Validation du **21 octobre 2026** : `bash ./gradlew build runClientGameTest --offline` a réussi avec **20 tests serveur requis** (19 Chomagerie et le test Fabric) et **deux tests graphiques client** en OpenGL hors écran. Les captures vérifient le petit cadenas à côté du viseur avec l’overlay de zone désactivé et l’absence de texte supplémentaire. Le raccourci réassigné, le déverrouillage, F1, les menus, la troisième personne et l’option du cadenas sont vérifiés.

## Rechargement serveur (1.6.1)

`ChomagerieReloadCommand` enregistre `/chomagerie reload` côté serveur. `BlockToolsConfig.read` valide un candidat complet sans modifier l’instance active ni le fichier. Le remplacement a lieu sur le thread serveur après validation. Un changement de `craftingEnabled` passe par le rechargement des ressources vanilla pour conserver la synchronisation des recettes. Le test serveur de rechargement attend la fin des tests qui modifient temporairement la politique globale. La sous-commande client `reload` relaie une commande vanilla au serveur pour éviter que Fabric ne l’intercepte à cause de la racine client `/chomagerie`. Les permissions restent contrôlées côté serveur. Le test client du HUD vérifie aussi la présence de la sous-commande serveur et son exécution depuis un client équipé des commandes ShulkerRefill.

Validation serveur du **21 octobre 2026** : `build --offline` a réussi avec **21 tests requis** (20 Chomagerie et le test Fabric). Le nouveau test vérifie les droits opérateur, les diagnostics et la conservation de la configuration en cas d’erreur, puis applique et restaure le réglage de craft dans le gestionnaire de recettes actif.

Validation complète du rechargement : `build runClientGameTest --offline` a réussi avec **21 tests serveur et deux tests graphiques client**. La commande envoyée par le client moddé atteint le serveur et reçoit la confirmation de rechargement ; les contrôles et le rendu du cadenas et de l’overlay restent vérifiés.

## Export des illustrations du wiki

`WikiAssetsClientGameTest`, activé seulement par `CHOMAGERIE_WIKI_ASSETS`, rend les objets avec `GuiGraphicsExtractor.item`, y compris les variantes de lumière avec leur composant `minecraft:block_state`. Il capture ensuite les trois outils sur une barrière. Les 291 images de recettes sont composées depuis les JSON, les icônes exportées et les textures d’interface du jeu. Les comparatifs gardent les captures complètes ; le détail du cadenas est explicitement marqué comme agrandi. Voir [Publication du wiki](Publication-du-wiki) pour les commandes.

Validation des illustrations : l’export graphique a réussi avec **285 icônes et variantes**, les captures réelles des outils et les tests de rendu. Les scripts produisent **291 images de recettes** et **14 images de démonstration**. `build --offline` conserve ses **21 tests serveur requis réussis** ; le validateur contrôle les 27 pages et toutes les images référencées.
