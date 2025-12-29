# Chomagerie - Auto Refill depuis Shulker Box

## Description

Ce mod Minecraft Fabric ajoute une fonctionnalité de rechargement automatique des items depuis les shulker boxes présentes dans votre inventaire.

## Fonctionnalités

### Rechargement automatique intelligent
Lorsque vous **utilisez complètement** un stack d'items dans votre main (placement de blocs, consommation, etc.) :
- Le mod détecte automatiquement que votre stack s'est vidé par utilisation
- Il recherche des shulker boxes dans votre inventaire
- Il trouve le même type d'item dans les shulker boxes
- Il transfère automatiquement un stack depuis la shulker box vers votre main

**Important :** Le refill se déclenche uniquement lors de l'**utilisation active** d'items (placement de blocs, etc.), pas lors de manipulations d'inventaire (déplacer un stack, le jeter, etc.)

### Côté serveur uniquement
Ce mod fonctionne **côté serveur** uniquement. Cela signifie que :
- Le serveur doit avoir le mod installé
- Les joueurs n'ont **pas besoin** d'installer le mod côté client
- Tous les joueurs connectés au serveur bénéficient automatiquement de cette fonctionnalité

## Installation

### Sur un serveur
1. Téléchargez le fichier `.jar` du mod
2. Placez-le dans le dossier `mods/` de votre serveur Fabric
3. Assurez-vous que Fabric API est également installé
4. Redémarrez le serveur

### En solo (optionnel)
Vous pouvez également installer le mod en solo pour tester :
1. Placez le fichier `.jar` dans le dossier `mods/` de votre client Minecraft
2. Assurez-vous que Fabric API est installé
3. Lancez le jeu

## Utilisation

1. Placez des shulker boxes (de n'importe quelle couleur) dans votre inventaire
2. Remplissez ces shulker boxes avec des items (blocs, nourriture, etc.)
3. Utilisez normalement vos items
4. Quand un stack se vide complètement, il est automatiquement rechargé depuis la première shulker box contenant cet item

### Exemple
- Vous avez 64 blocs de pierre dans votre main (slot 0)
- Vous avez une shulker box dans votre inventaire contenant 1000 blocs de pierre
- Vous placez les 64 blocs de pierre
- Automatiquement, 64 nouveaux blocs de pierre apparaissent dans votre main depuis la shulker box

## Compatibilité

- **Minecraft** : 1.21+
- **Fabric Loader** : Dernière version
- **Fabric API** : Requis

## Développement

### Compilation
```bash
./gradlew build
```

Le fichier `.jar` compilé se trouve dans `build/libs/`

### Structure du projet
- `src/main/java/tech/maloandre/chomagerie/` - Code principal du mod
  - `Chomagerie.java` - Classe principale et enregistrement des événements
  - `event/ItemStackDepletedCallback.java` - Événement personnalisé pour détecter les stacks vides
  - `mixin/PlayerInventoryMixin.java` - Mixin pour intercepter les changements d'inventaire
  - `util/ShulkerRefillHandler.java` - Logique de rechargement depuis les shulker boxes

## Technique

### Comment ça fonctionne
1. Un **Mixin** sur `PlayerInventory.updateItems()` surveille constamment l'inventaire du joueur
2. Il compare l'état actuel de chaque slot avec l'état précédent
3. Quand un slot passe de "non-vide" à "vide", il déclenche un événement
4. L'événement appelle le handler qui :
   - Parcourt l'inventaire à la recherche de shulker boxes
   - Lit le contenu de chaque shulker box (composant `CONTAINER`)
   - Cherche l'item correspondant
   - Transfère le stack vers le slot vide
   - Met à jour le contenu de la shulker box

### Performance
- Le système ne fonctionne que côté serveur (pas de calculs inutiles côté client)
- La vérification se fait uniquement dans `updateItems()` qui est appelée à chaque tick
- L'algorithme s'arrête dès qu'un refill est effectué

## Licence

All Rights Reserved

## Auteur

MaloAndre - Serveur Chomagerie

