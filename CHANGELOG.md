## Release

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
