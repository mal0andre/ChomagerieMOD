# Chomagerie 🧀

📖 [Wiki GitHub complet](https://github.com/mal0andre/ChomagerieMOD/wiki) · [Installation](https://github.com/mal0andre/ChomagerieMOD/wiki/Installation) · [Configuration](https://github.com/mal0andre/ChomagerieMOD/wiki/Configuration-serveur)

Chomagerie adds quality-of-life features for the Chomagerie server: automatic shulker refills, configurable player tags, useful recipes, crop protection, and block construction tools.

**Version 1.6.0 · Minecraft 26.3 · Fabric.** This release adds the Survival Debug Stick, persistent block state locks, and a configurable client overlay. Update Chomagerie on both client and server together; modded connections require matching versions. See [release notes](CHANGELOG.md) and the [GitHub Wiki](https://github.com/mal0andre/ChomagerieMOD/wiki).

Development additions after 1.6.0 include a configurable overlay shortcut (H by default), an optional compact padlock beside the crosshair for targeted locked blocks, cached geometry, frustum culling, and progressive server synchronization for dense builds. See the Unreleased changelog.

## Features ✨

### Automatic Shulker Refill 📦

When you completely use up a stack, Chomagerie can automatically replace it with the same item found inside your shulker boxes.

* Keeps the refill in the same inventory slot.
* Searches your inventory first, then your Ender Chest.
* Can display a message and play a sound when a refill happens.
* Can filter refill sources by shulker box name.
* Supports multiple exact shulker box names in the filter list.

### Player Tags 🎨

You can display a small colored tag before your username.

* The tag is displayed without brackets.
* Your username keeps its normal color.
* Manual Minecraft `&` color codes are supported.
* Solid color, gradient, and rainbow modes are available.
* Gradients support multiple color stops.
* Gradient colors can be edited with the MaLiLib color-list editor and color picker.

### Configuration Menu

The configuration menu uses MaLiLib and can be opened from ModMenu or with the configurable Minecraft keybind.

### Team Management 🛡️

Menu categories:

* All
* ShulkerRefill
* Team Tag

The keybind is available in Minecraft controls under the `key.chomagerie.category` category.

### Crop Protection 🌾

The server can control when farmland can be trampled.

* Prevent players from trampling farmland.
* Prevent mobs from trampling farmland.
* Allow trampling only when wearing leather boots, depending on the server configuration.

### Survival Debug Stick and Block State Locks 🪄

Craft a vanilla Debug Stick with the original shaped recipe: two gold ingots, one netherite ingot, two redstone dust and two sticks (see the layout in the documentation). By default all blocks, properties and values are editable, including waterlogged. In Survival, left-click selects a property and right-click changes its value; hold Shift to cycle backwards.

Right-click a block with at least one editable state property with honeycomb to freeze its entire block state (orientation, growth, redstone properties, connections, etc.). Debug Stick edits become the new locked values. Right-click with an axe to remove the lock and resume normal behavior; that click does not also strip the block. Block entity inventories and processing are separate from block states. Blocks without editable properties, such as sand, stone and glass blocks, cannot receive new locks and consume no honeycomb for locking. Existing locks remain removable with an axe; vanilla copper waxing still works. Locks persist with the world. These features work on a dedicated Fabric server with vanilla clients.

With Chomagerie on both client and server, enable **Lock Overlay** in the **K** menu to highlight locked blocks through walls. A color picker lets you choose the highlight color (red by default). Viewing range is adjustable from 8 to 128 blocks (32 by default); the overlay is disabled by default.

Operators can reload `config/chomagerie/block-tools.json` with `/chomagerie reload` (permission level 2). Invalid files leave the active settings intact and report the offending field. Changing the Debug Stick crafting switch also reloads Minecraft data automatically.

Server policy, supported blocks, persistence details and test instructions: [GitHub Wiki](https://github.com/mal0andre/ChomagerieMOD/wiki/Verrouillage-des-blocs). Inspired and adapted from [JustS-js/debugStickSurvival](https://github.com/JustS-js/debugStickSurvival) (CC0-1.0).

### Useful Recipes 🔨

Chomagerie also adds useful recipes to simplify crafting on the server.

* Stonecutter recipes.
* Coral conversions.
* Utility recipes.
* Copper-related recipes.
* A few adjustments to vanilla recipes.

## Installation 🚀

The mod is designed for Fabric.

Required:

* Fabric Loader
* Fabric API
* MaLiLib

Recommended:

* ModMenu, for opening the configuration menu from the Mods screen.

## License 📜

All Rights Reserved.

* Use in modpacks is allowed with attribution.
* Redistribution, modification, or reuse is not allowed without explicit permission.
