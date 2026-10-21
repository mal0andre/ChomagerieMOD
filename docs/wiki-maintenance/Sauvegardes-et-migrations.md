# Sauvegardes et migrations

## Sauvegarder le mod

Avant une mise à jour ou un changement de configuration important, arrêter proprement le serveur et conserver :

1. Le monde complet, avec toutes ses dimensions et ses données de scoreboard.
2. `config/chomagerie/players.json`.
3. `config/chomagerie/block-tools.json`.
4. Les préférences client `config/chomagerie.json` si elles doivent être conservées.

Sauvegarder seulement `players.json` ne sauvegarde pas les verrous : ceux-ci appartiennent au monde.

## Stockage des verrous

Chaque dimension possède un `SavedData` identifié par `chomagerie:block_state_locks`. Minecraft le sauvegarde sous son dossier de données, dans `data/chomagerie/block_state_locks.dat`. Dans le monde de test 26.3, ce dossier se trouve notamment sous `world/dimensions/minecraft/overworld/data/` pour la dimension principale.

Une table de positions stocke les références complètes de BlockState. Les données sont marquées sales après modification et sauvegardées par Minecraft ; il n'y a pas d'écriture disque forcée à chaque clic, ni de parcours global de tous les verrous à chaque tick.

## Détruire ou remplacer un bloc

Un remplacement par un type différent via `LevelChunk.setBlockState` supprime le verrou. Un bloc reposé après destruction n'hérite pas du verrou précédent. Le verrou ne se déplace pas avec un piston.

Un remplacement direct du même type avec d'autres propriétés est refusé tant que le verrou est actif. Pour une commande ou un outil de construction, déverrouiller d'abord si ce changement d'état est voulu.

## Anciens fichiers

- L'ancien nom unique `shulkerNameFilter` est repris lors de la normalisation du filtre à plusieurs noms.
- Les couleurs de dégradé à deux extrémités sont normalisées vers une liste.
- L'ancienne option `blockStateLocking.allowedBlocks` est ignorée. Depuis Chomagerie 1.6.1, seuls les blocs ayant une propriété avec plusieurs valeurs acceptent de nouveaux verrous. Les anciens verrous sur des blocs sans propriété restent lisibles et retirables à la hache.
- Pour enlever d'anciennes restrictions du Debug Stick, régler `whitelistMode=false` et vider les listes et objets de règles dans sa section. Les anciens fichiers d'un autre serveur ne sont pas automatiquement remplacés.

## Désactivation et retrait du mod

`blockStateLocking.enabled=false` suspend les verrous sans effacer leurs références. Une réactivation les remet en service. Retirer le mod supprime son mécanisme de conservation des états : les blocs reviennent aux mises à jour normales du jeu. Garder une sauvegarde avant ce changement.

Les éditions de sections de chunks qui contournent les méthodes habituelles du jeu, ainsi que les modifications hors ligne des fichiers, ne sont pas surveillées.
