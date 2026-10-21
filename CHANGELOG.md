## Release

### [Unreleased]

#### Added
- Illustrate all 291 wiki recipes with Minecraft-rendered inventory icons, native crafting grids and exact output counts; add real gameplay screenshots for lock tools, overlays, colors and the compact padlock.
- Add operator-only `/chomagerie reload` for validated hot reload of block-tools.json, with readable JSON errors, preservation of the active policy on rejection, and automatic vanilla data reload when the Debug Stick crafting switch changes.
- Add a configurable shortcut (H by default) to toggle and save the lock overlay without opening the menu.
- Add an optional small padlock beside the crosshair for the nearby aimed locked block, even with the area overlay disabled; keep the vanilla Debug Stick property messages.
- Add server tests for progressive delivery of 20,000 locks, chunk revision invalidation, target validation, and packet codecs, plus a graphical test for the compact padlock, absence of duplicate property text, and key rebinding.

#### Changed
- Restrict new honeycomb locks to blocks with at least one state property with multiple values; propertyless blocks consume no honeycomb for locking, existing locks remain axe-removable, and vanilla copper waxing is preserved.
- Cache static overlay shapes between ticks, refresh dynamic shapes per tick, and cull geometry outside the camera frustum while preserving through-wall highlights.
- Recompute stationary server snapshots only when nearby chunk locks, loaded chunks, or settings change; changes in distant chunks no longer invalidate them.
- Limit overlay synchronization to 4,096 position changes per player every five ticks, prioritize removals, and progressively deliver all remaining positions.
- Preserve vanilla clients and servers without the new channels; F1, menus, and third-person views hide the padlock without replacing vanilla action-bar messages.

### [1.6.0] - 2026-10-20

#### Added
- Add a client-side color picker for the locked-block overlay, with immediate preview, saved preferences, and red as the default.
- Add an optional client overlay showing server-synchronized locked blocks through walls with customizable colors, with a dedicated menu tab and an 8–128 block viewing range (32 by default).
- Synchronize nearby lock changes only to opted-in compatible clients, with dimension/disconnection cleanup and bounded packet batches.
- Add Survival Debug Stick interactions ported from JustS-js/debugStickSurvival, including property selection, reverse cycling with Shift, and vanilla property messages.
- Add persistent honeycomb locks for the complete BlockState of every targetable block, including blocks without properties.
- Allow Debug Stick edits to update the persisted locked state; right-click with an axe to unlock and resume normal block behavior.
- Add server-side block-tool configuration with unrestricted Debug Stick defaults, optional feedback and effects, and creative-mode item/tool preservation.
- Add 17 Chomagerie server GameTests covering block tools, persistence, cleanup, permissions, overlay range and packet bounds, plus a graphical client test for through-wall rendering, color changes, controls, and saved preferences.
- Provide a complete GitHub Wiki with installation, player guides, server configuration, troubleshooting, validation, and JustS attribution (CC0-1.0).
- Generate the complete 291-recipe wiki catalogue from source JSON files and validate wiki navigation and JSON examples.

#### Changed
- Replace repository documentation with the GitHub Wiki and provide scripts to maintain its recipe catalogue and validate pages.
- Handle block tools before normal chest, door, and other block interactions while respecting Fabric protection callbacks.
- Keep the original shaped Debug Stick recipe using gold, netherite, redstone, and sticks, with an option to disable it.

#### Fixed
- Update source, issue, and license links to the current repository and include LICENSE.txt in the distributable JAR.
- Open the configuration keybind directly without loading the optional ModMenu API.
- Register the configuration key through Fabric before options load, preserving saved key bindings and compatibility with client GameTests.
- Preserve JSON escapes when expanding mod metadata during resource processing.

### [1.5.4] - 2026-10-17

#### Added
- Add stonecutter recipes for poplar wood: one plank produces two slabs or one stair.
- Add the improved poplar trapdoor recipe: six planks produce six trapdoors.
- Match poplar crafting yields with the other wood types supported by Chomagerie.

### [1.5.3] - 2026-10-17

#### Changed
- Update to Minecraft 26.3, Fabric Loader 0.19.5, and Fabric API 0.163.0.
- Update MaLiLib to 0.30.2 and ModMenu to 21.0.0.
- Update Loom and Gradle for Minecraft 26.3.

#### Fixed
- Adapt the configuration keybind to SDL keyboard input.
- Adapt shulker contents access and farmland protection to the Minecraft 26.3 APIs.
- Fix automatic GitHub and Modrinth publishing, release defaults, and artifact selection.

### [1.5.2] - 2026-08-19

#### Added
- Add a configurable Minecraft keybind to open the Chomagerie configuration menu.
- Add MaLiLib-based configuration categories for All, ShulkerRefill, and Team Tag.
- Add support for multiple shulker box names in the ShulkerRefill name filter.
- Add support for multi-stop Team Tag gradients.
- Add a MaLiLib color-list editor and color picker for Team Tag gradient colors.
- Add English fallback language entries for the new configuration menu.

#### Changed
- Replace the previous configuration UI with a MaLiLib configuration screen.
- Team Tag configuration is now handled through the menu instead of `/chomteam` and `/teamtag` commands.
- Gradient colors are now edited as a color list instead of separate start/end text fields.
- ShulkerRefill name filtering now accepts a list of exact names instead of a single exact name.

#### Fixed
- Fix the Minecraft controls category label for the Chomagerie keybind.
- Fix a startup crash caused by registering key mappings before Minecraft options were available.
- Fix missing language fallback behavior by defaulting unavailable language entries to English.
- Fix the MaLiLib tab layout so the All tab stays inside the screen.
- Fix raw translation keys showing in the gradient color list menu.
