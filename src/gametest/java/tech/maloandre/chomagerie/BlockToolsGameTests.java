package tech.maloandre.chomagerie;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DebugStickState;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import tech.maloandre.chomagerie.blockstate.BlockStateLocks;
import tech.maloandre.chomagerie.blockstate.BlockToolsConfig;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BlockToolsGameTests {
    private static final BlockPos LOCAL = new BlockPos(2, 2, 2);
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static ServerPlayer player(GameTestHelper h, GameType mode) {
        var level = h.getLevel();
        var player = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "BlockToolsTest"), ClientInformation.createDefault()) {
            @Override public GameType gameMode() { return mode; }
            @Override public void sendOverlayMessage(Component message) { }
        };
        mode.updatePlayerAbilities(player.getAbilities());
        var pos = h.absolutePos(LOCAL);
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + 1.5);
        return player;
    }
    private static InteractionResult use(ServerPlayer player, ItemStack stack, BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
    }
    private static ItemStack debug(BlockState state, Property<?> property) {
        ItemStack stick = new ItemStack(Items.DEBUG_STICK);
        stick.set(DataComponents.DEBUG_STICK_STATE, DebugStickState.EMPTY.withProperty(state.typeHolder(), property));
        return stick;
    }

    @GameTest
    public void paneLockDebugAndUnlock(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(LOCAL); var player = player(h, GameType.SURVIVAL);
        level.setBlock(pos, Blocks.GLASS_PANE.defaultBlockState(), Block.UPDATE_ALL);
        var honey = new ItemStack(Items.HONEYCOMB, 3);
        check(use(player, honey, pos).consumesAction(), "Honeycomb must lock the pane");
        check(honey.getCount() == 2, "Consume exactly one honeycomb");
        use(player, honey, pos);
        check(honey.getCount() == 2, "Already locked must not consume again");
        level.setBlock(pos.east(), Block.updateFromNeighbourShapes(Blocks.GLASS_PANE.defaultBlockState(), level, pos.east()), Block.UPDATE_ALL);
        check(!level.getBlockState(pos).getValue(BlockStateProperties.EAST), "Locked east must remain false");
        check(level.getBlockState(pos.east()).getValue(BlockStateProperties.WEST), "Unlocked neighbor must connect");
        level.removeBlock(pos.east(), false);
        use(player, debug(level.getBlockState(pos), BlockStateProperties.EAST), pos);
        check(level.getBlockState(pos).getValue(BlockStateProperties.EAST), "Debug stick must change locked reference");
        level.setBlock(pos.east(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.removeBlock(pos.east(), false);
        check(level.getBlockState(pos).getValue(BlockStateProperties.EAST), "Manual connection must survive neighbor removal");
        var axe = new ItemStack(Items.IRON_AXE);
        use(player, axe, pos);
        check(!BlockStateLocks.get(level).contains(pos), "Axe must unlock");
        check(axe.getDamageValue() == 1, "Survival axe must lose one durability");
        check(!level.getBlockState(pos).getValue(BlockStateProperties.EAST), "Unlock must recalculate connections");
        level.setBlock(pos.east(), Block.updateFromNeighbourShapes(Blocks.GLASS_PANE.defaultBlockState(), level, pos.east()), Block.UPDATE_ALL);
        check(level.getBlockState(pos).getValue(BlockStateProperties.EAST), "Unlocked pane must connect normally");
        h.succeed();
    }

    @GameTest
    public void allConnectionFamiliesAndAdjacentLocks(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(LOCAL);
        var blocks = net.minecraft.core.registries.BuiltInRegistries.BLOCK.stream()
                .filter(b -> b == Blocks.GLASS_PANE || b == Blocks.IRON_BARS
                        || Blocks.STAINED_GLASS_PANE.asList().contains(b)
                        || b.defaultBlockState().is(net.minecraft.tags.BlockTags.FENCES)
                        || b.defaultBlockState().is(net.minecraft.tags.BlockTags.WALLS)).toList();
        check(blocks.size() >= 30, "Lockable tag must include all vanilla families and colors");
        var player = player(h, GameType.SURVIVAL);
        for (Block block : blocks) {
            level.removeBlock(pos, false); level.removeBlock(pos.east(), false);
            BlockState original = block.defaultBlockState();
            level.setBlock(pos, original, 18);
            use(player, new ItemStack(Items.HONEYCOMB), pos);
            check(BlockStateLocks.get(level).contains(pos), "Compatible family must lock: " + block);
            level.setBlock(pos.east(), original, Block.UPDATE_ALL);
            check(level.getBlockState(pos) == original, "Locked connections changed: " + block);
            use(player, new ItemStack(Items.HONEYCOMB), pos.east());
            BlockState other = level.getBlockState(pos.east());
            level.setBlock(pos.above(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            level.removeBlock(pos.above(), false);
            check(level.getBlockState(pos) == original && level.getBlockState(pos.east()) == other, "Adjacent locks must remain independent");
            level.removeBlock(pos.east(), false);
            check(level.getBlockState(pos) == original, "Removal changed locked family: " + block);
        }
        h.succeed();
    }

    @GameTest
    public void replacementCleanupAndPropertylessBlock(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(LOCAL); var locks = BlockStateLocks.get(level);
        var player = player(h, GameType.SURVIVAL); var honey = new ItemStack(Items.HONEYCOMB, 3);
        level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3); use(player, honey, pos);
        check(honey.getCount() == 2 && locks.contains(pos), "Propertyless stone must also lock and consume honeycomb");
        level.setBlock(pos, Blocks.GLASS_PANE.defaultBlockState(), 3); use(player, honey, pos);
        level.setBlock(pos, Blocks.STAINED_GLASS_PANE.red().defaultBlockState(), 3);
        check(!locks.contains(pos), "Command-style replacement must remove lock");
        use(player, honey, pos); level.destroyBlock(pos, false);
        check(!locks.contains(pos), "Destruction must remove lock");
        level.setBlock(pos, Blocks.STAINED_GLASS_PANE.red().defaultBlockState(), 3);
        check(!locks.contains(pos), "New block must not inherit lock");
        h.succeed();
    }

    @GameTest
    public void spectatorAdventureCreativeAndAttackDebounce(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(LOCAL);
        var original = Blocks.GLASS_PANE.defaultBlockState(); level.setBlock(pos, original, 3);
        for (var mode : List.of(GameType.SPECTATOR, GameType.ADVENTURE)) {
            var player = player(h, mode);
            use(player, new ItemStack(Items.HONEYCOMB), pos);
            use(player, debug(original, BlockStateProperties.EAST), pos);
            AttackBlockCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND, pos, Direction.UP);
            check(level.getBlockState(pos) == original && !BlockStateLocks.get(level).contains(pos), "Restricted game mode modified block");
        }
        var creative = player(h, GameType.CREATIVE); var honey = new ItemStack(Items.HONEYCOMB, 2);
        use(creative, honey, pos); check(honey.getCount() == 2, "Creative honeycomb consumption");
        var axe = new ItemStack(Items.IRON_AXE); use(creative, axe, pos);
        check(axe.getDamageValue() == 0, "Creative axe damage");
        // JustS extends interaction to non-op Creative too; operator Creative stays vanilla.
        use(creative, debug(original, BlockStateProperties.EAST), pos);
        check(level.getBlockState(pos).getValue(BlockStateProperties.EAST), "JustS interaction must work for non-op Creative too");
        level.setBlock(pos, original, 18);
        var survival = player(h, GameType.SURVIVAL); var stick = new ItemStack(Items.DEBUG_STICK);
        survival.setItemInHand(InteractionHand.MAIN_HAND, stick);
        var result = AttackBlockCallback.EVENT.invoker().interact(survival, level, InteractionHand.MAIN_HAND, pos, Direction.UP);
        var selected = stick.get(DataComponents.DEBUG_STICK_STATE);
        for (int i = 0; i < 10; i++) AttackBlockCallback.EVENT.invoker().interact(survival, level, InteractionHand.MAIN_HAND, pos, Direction.UP);
        check(result == InteractionResult.PASS && selected.equals(stick.get(DataComponents.DEBUG_STICK_STATE)), "JustS callback must pass and throttle repeated attempts");
        check(level.getBlockState(pos) == original, "Left click must not break block");
        h.succeed();
    }

    @GameTest
    public void restrictionsAndUntrustedSelection(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(LOCAL); var player = player(h, GameType.SURVIVAL);
        var original = Blocks.GLASS_PANE.defaultBlockState(); level.setBlock(pos, original, 3);
        use(player, debug(original, BlockStateProperties.WATERLOGGED), pos);
        check(level.getBlockState(pos).getValue(BlockStateProperties.WATERLOGGED), "Default policy must allow waterlogged editing");
        level.setBlock(pos, Blocks.OAK_LOG.defaultBlockState(), 3);
        var log = level.getBlockState(pos); use(player, new ItemStack(Items.DEBUG_STICK), pos);
        check(level.getBlockState(pos) != log, "Default policy must allow logs");
        var policy = new BlockToolsConfig.DebugStick();
        policy.allowedBlocks = List.of(new BlockToolsConfig.Entry("minecraft:glass_pane"));
        policy.forbiddenProperties = Map.of("east", List.of("all"));
        check(!policy.allows(original, "east", "true"), "Block allow must not override global deny");
        policy.forbiddenProperties = Map.of();
        policy.allowedBlocks = List.of(new BlockToolsConfig.Entry("minecraft:glass_pane", Map.of("east", List.of("false"))));
        policy.whitelistMode = false;
        check(!policy.allows(original, "east", "true"), "Explicit allowed values must constrain blacklist mode");
        policy.forbiddenTags = List.of(new BlockToolsConfig.Entry("c:glass_panes"));
        check(!policy.allows(original, "east", "false"), "Denied tag must override allowed block");
        h.succeed();
    }

    @GameTest
    public void fabricProtectionVetoAndEditingSwitch(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(LOCAL); var protectedPlayer = player(h, GameType.SURVIVAL);
        var state = Blocks.GLASS_PANE.defaultBlockState(); level.setBlock(pos, state, 3);
        // Register AFTER Chomagerie: default-phase protection must still run first.
        AttackBlockCallback.EVENT.register((p, l, hand, at, direction) -> p == protectedPlayer ? InteractionResult.FAIL : InteractionResult.PASS);
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((p, l, hand, hit) -> p == protectedPlayer ? InteractionResult.FAIL : InteractionResult.PASS);
        var stick = new ItemStack(Items.DEBUG_STICK); protectedPlayer.setItemInHand(InteractionHand.MAIN_HAND, stick);
        AttackBlockCallback.EVENT.invoker().interact(protectedPlayer, level, InteractionHand.MAIN_HAND, pos, Direction.UP);
        check(stick.getOrDefault(DataComponents.DEBUG_STICK_STATE, DebugStickState.EMPTY).properties().isEmpty(), "Protection callback must veto selection");
        var honey = new ItemStack(Items.HONEYCOMB, 2); protectedPlayer.setItemInHand(InteractionHand.MAIN_HAND, honey);
        protectedPlayer.gameMode.useItemOn(protectedPlayer, level, honey, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        check(honey.getCount() == 2 && !BlockStateLocks.get(level).contains(pos), "Protection callback must veto honeycomb");
        var editor = player(h, GameType.SURVIVAL); use(editor, honey, pos);
        var config = BlockToolsConfig.get().blockStateLocking;
        boolean before = config.allowDebugStickEditing;
        try {
            config.allowDebugStickEditing = false;
            use(editor, debug(state, BlockStateProperties.EAST), pos);
            check(level.getBlockState(pos) == state, "Disabled editing must deny debug stick on locked block");
        } finally { config.allowDebugStickEditing = before; }
        h.succeed();
    }

    @GameTest
    public void craftingRecipeAndDisableSwitch(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                net.minecraft.resources.Identifier.parse("chomagerie:utility/debug_stick"));
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.NETHERITE_INGOT),
                new ItemStack(Items.REDSTONE), new ItemStack(Items.STICK), new ItemStack(Items.GOLD_INGOT),
                new ItemStack(Items.STICK), new ItemStack(Items.REDSTONE), ItemStack.EMPTY));
        var recipe = server.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, h.getLevel()).orElseThrow();
        check(recipe.id().equals(key) && recipe.value().assemble(input).is(Items.DEBUG_STICK), "Original shaped recipe must craft vanilla debug stick");
        var removedInput = net.minecraft.world.item.crafting.CraftingInput.of(2, 1,
                List.of(new ItemStack(Items.STICK), new ItemStack(Items.CHORUS_FRUIT)));
        check(server.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,
                removedInput, h.getLevel()).isEmpty(), "Stick and chorus fruit recipe must be absent");
        var config = BlockToolsConfig.get().debugStick;
        boolean before = config.craftingEnabled;
        try {
            config.craftingEnabled = false;
            var recipes = net.minecraft.world.item.crafting.RecipeMap.create(
                    server.reloadableRegistries().lookup().lookupOrThrow(net.minecraft.core.registries.Registries.RECIPE));
            check(recipes.byKey(key) == null, "Disabled recipe must be removed from recipe map");
            check(recipes.getRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, h.getLevel()).findAny().isEmpty(), "Disabled recipe must not craft");
        } finally { config.craftingEnabled = before; }
        h.succeed();
    }

    @GameTest
    public void reverseCyclingAndNoAllowedValues(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(LOCAL); var player = player(h, GameType.SURVIVAL);
        var original = Blocks.OAK_STAIRS.defaultBlockState(); level.setBlock(pos, original, 18);
        var property = BlockStateProperties.HORIZONTAL_FACING;
        var stick = debug(original, property);
        use(player, stick, pos);
        check(level.getBlockState(pos).getValue(property) != original.getValue(property), "Forward value cycle");
        player.setShiftKeyDown(true); use(player, stick, pos);
        check(level.getBlockState(pos).getValue(property) == original.getValue(property), "Shift must reverse values");
        player.setShiftKeyDown(false);
        stick.getItem().canDestroyBlock(stick, original, level, pos, player);
        var forward = stick.get(DataComponents.DEBUG_STICK_STATE).properties().get(original.typeHolder());
        check(forward != property, "Left click must select another property");
        player.setShiftKeyDown(true);
        stick.getItem().canDestroyBlock(stick, original, level, pos, player);
        check(stick.get(DataComponents.DEBUG_STICK_STATE).properties().get(original.typeHolder()) == property, "Shift must reverse property selection");
        var config = BlockToolsConfig.get().debugStick;
        var before = config.allowedBlocks;
        try {
            config.allowedBlocks = List.of(new BlockToolsConfig.Entry("minecraft:oak_stairs", Map.of("facing", List.of())));
            check(use(player, stick, pos) == InteractionResult.FAIL, "No allowed values must refuse use and terminate");
            check(level.getBlockState(pos) == original, "No allowed values changed state");
        } finally { config.allowedBlocks = before; }
        h.succeed();
    }

    @GameTest(maxTicks = 80)
    public void justSSelectionCyclesEveryPaneProperty(GameTestHelper h) {
        checkJustSPropertyCycle(h, false);
    }

    @GameTest(maxTicks = 80)
    public void justSSelectionCyclesEveryLockedPaneProperty(GameTestHelper h) {
        checkJustSPropertyCycle(h, true);
    }

    private static void checkJustSPropertyCycle(GameTestHelper h, boolean locked) {
        var level = h.getLevel(); var pos = h.absolutePos(LOCAL); var player = player(h, GameType.SURVIVAL);
        level.setBlock(pos, Blocks.GLASS_PANE.defaultBlockState(), 18);
        if (locked) useViaGameMode(player, new ItemStack(Items.HONEYCOMB), pos);
        var stick = new ItemStack(Items.DEBUG_STICK);
        player.setItemInHand(InteractionHand.MAIN_HAND, stick);
        var properties = List.copyOf(Blocks.GLASS_PANE.getStateDefinition().getProperties());
        var visited = new java.util.HashSet<String>();
        for (int step = 0; step < properties.size() * 2; step++) {
            int index = step;
            h.runAfterDelay(1 + step * 5, () -> {
                boolean reverse = index >= properties.size();
                player.setShiftKeyDown(reverse);
                // Run actual START/ABORT processing, including Fabric callbacks and the item path.
                player.gameMode.handleBlockBreakAction(pos,
                        net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                        Direction.UP, level.getMaxY(), 0);
                player.gameMode.handleBlockBreakAction(pos,
                        net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                        Direction.UP, level.getMaxY(), 0);
                var selected = stick.get(DataComponents.DEBUG_STICK_STATE).properties().get(Blocks.GLASS_PANE.builtInRegistryHolder());
                int expected = reverse ? Math.floorMod(properties.size() - 2 - (index - properties.size()), properties.size()) : index;
                check(selected == properties.get(expected), "JustS property order/reverse failed: expected " + properties.get(expected) + ", got " + selected);
                visited.add(selected.getName());
                BlockState before = level.getBlockState(pos);
                useViaGameMode(player, stick, pos);
                check(level.getBlockState(pos) == before.cycle(selected), "Right click must change only selected " + selected.getName());
                if (locked) check(BlockStateLocks.get(level).preserve(pos, before) == level.getBlockState(pos), "Manual edit must become the locked reference");
                long lastModified = stick.get(DataComponents.CUSTOM_DATA).copyTag().getLong("LastModified").orElseThrow();
                for (int retry = 0; retry < 4; retry++) {
                    int delay = retry + 1;
                    h.runAfterDelay(delay, () -> {
                        var priorSelection = stick.get(DataComponents.DEBUG_STICK_STATE);
                        AttackBlockCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND, pos, Direction.UP);
                        check(priorSelection.equals(stick.get(DataComponents.DEBUG_STICK_STATE)), "Click inside five ticks must not advance");
                        check(stick.get(DataComponents.CUSTOM_DATA).copyTag().getLong("LastModified").orElseThrow() == lastModified,
                                "Rejected click must not move the timer and starve later selections");
                    });
                }
            });
        }
        h.runAfterDelay(properties.size() * 10 + 1, () -> {
            check(visited.containsAll(java.util.Set.of("east", "west", "north", "south", "waterlogged")), "Every pane property must be selectable");
            check(level.getBlockState(pos).is(Blocks.GLASS_PANE), "Selection must never destroy pane");
            h.succeed();
        });
    }

    @GameTest
    public void everyRegisteredBlockAndEveryPropertyCanLock(GameTestHelper h) {
        var locks = new BlockStateLocks();
        int checked = 0;
        for (Block block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            BlockState state = block.defaultBlockState();
            if (state.isAir()) continue; // Air cannot be targeted with an item.
            check(BlockToolsConfig.get().blockStateLocking.allows(state), "Rejected block: " + block);
            locks.lock(LOCAL, state);
            for (Property<?> property : state.getProperties()) {
                check(locks.preserve(LOCAL, state.cycle(property)) == state, "Snapshot must retain property: " + property);
            }
            check(locks.preserve(LOCAL, Blocks.AIR.defaultBlockState()) == state, "Neighbor support loss must preserve full state");
            checked++;
        }
        check(checked > 1000, "Registry-wide coverage must include all block families");
        h.succeed();
    }

    private static InteractionResult useViaGameMode(ServerPlayer player, ItemStack stack, BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player.gameMode.useItemOn(player, player.level(), stack, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    @GameTest
    public void interactiveBlocksAndFullStateGuard(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(LOCAL); var player = player(h, GameType.SURVIVAL);
        for (Block block : List.of(Blocks.CHEST, Blocks.OAK_DOOR, Blocks.LEVER, Blocks.FURNACE, Blocks.OAK_STAIRS, Blocks.WHEAT)) {
            level.removeBlock(pos, false);
            level.setBlock(pos, block.defaultBlockState(), 18);
            var original = level.getBlockState(pos);
            var honey = new ItemStack(Items.HONEYCOMB, 2);
            check(useViaGameMode(player, honey, pos).consumesAction(), "Interactive block must accept honeycomb: " + block);
            var locks = BlockStateLocks.get(level);
            check(locks.contains(pos) && honey.getCount() == 1, "Interactive block must lock before activation: " + block);
            for (Property<?> property : original.getProperties()) {
                BlockState changed = original.cycle(property);
                if (changed == original) continue;
                check(!level.setBlock(pos, changed, 3) && level.getBlockState(pos) == original,
                        "Ordinary writes must not change locked " + block + "/" + property.getName());
                useViaGameMode(player, debug(original, property), pos);
                check(level.getBlockState(pos) == changed, "Debug stick must edit any locked property: " + property.getName());
                level.setBlock(pos, original, 3);
                check(level.getBlockState(pos) == changed, "Manual reference must survive ordinary writes");
                // Return to the original through the same explicit manual-write path.
                locks.setManually(level, pos, original, 18);
            }
            var axe = new ItemStack(Items.IRON_AXE);
            check(useViaGameMode(player, axe, pos).consumesAction() && !locks.contains(pos), "Axe must unlock interactive block before its normal action");
            check(axe.getDamageValue() == 1, "Axe must lose exactly one durability");
        }
        h.succeed();
    }

    @GameTest(maxTicks = 40)
    public void gravityAndScheduledTicksResumeAfterUnlock(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(LOCAL); var player = player(h, GameType.SURVIVAL);
        level.removeBlock(pos.below(), false);
        level.setBlock(pos, Blocks.SAND.defaultBlockState(), 18);
        useViaGameMode(player, new ItemStack(Items.HONEYCOMB), pos);
        h.runAfterDelay(8, () -> {
            check(level.getBlockState(pos).is(Blocks.SAND), "Locked sand must not fall on its scheduled tick");
            useViaGameMode(player, new ItemStack(Items.IRON_AXE), pos);
        });
        h.runAfterDelay(16, () -> {
            check(!level.getBlockState(pos).is(Blocks.SAND), "Unlocked sand must resume gravity");
            check(!BlockStateLocks.get(level).contains(pos), "Falling block must not leave a lock");
            h.succeed();
        });
    }

    @GameTest
    public void fullSnapshotSurvivesNeighborsAndFailedManualWrite(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(LOCAL); var player = player(h, GameType.SURVIVAL);
        var state = Blocks.GLASS_PANE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
        level.setBlock(pos, state, 18);
        useViaGameMode(player, new ItemStack(Items.HONEYCOMB), pos);
        level.setBlock(pos.east(), Blocks.STONE.defaultBlockState(), 3);
        check(level.getBlockState(pos) == state, "Waterlogged and connections must both remain locked");
        useViaGameMode(player, debug(state, BlockStateProperties.WATERLOGGED), pos);
        var changed = level.getBlockState(pos);
        check(!changed.getValue(BlockStateProperties.WATERLOGGED), "Debug stick must update locked waterlogged");
        level.removeBlock(pos.east(), false);
        check(level.getBlockState(pos) == changed, "Full manual state must remain the reference");
        var invalid = new BlockPos(pos.getX(), level.getMaxY() + 5, pos.getZ());
        var locks = BlockStateLocks.get(level); locks.lock(invalid, state);
        try {
            check(!locks.setManually(level, invalid, changed, 18), "Out-of-bounds manual write must fail");
            check(locks.preserve(invalid, changed) == state, "Failed write must roll back locked reference");
        } finally { locks.remove(invalid); }
        h.succeed();
    }

    @GameTest
    public void overlayRangeIndexAndCleanup(GameTestHelper h) {
        var locks = new BlockStateLocks();
        var origin = new BlockPos(-1, 64, -1);
        var edge = origin.offset(8, 0, 0);
        var outside = origin.offset(8, 1, 0);
        var distant = origin.offset(2048, 0, 0);
        var state = Blocks.STONE.defaultBlockState();
        for (var pos : List.of(origin, edge, outside, distant)) locks.lock(pos, state);
        var center = Vec3.atCenterOf(origin);
        var visible = locks.nearby(center, 8, chunk -> true);
        check(visible.size() == 2 && visible.contains(origin.asLong()) && visible.contains(edge.asLong()),
                "Include negative chunk coordinates and exact spherical boundary, exclude distant/diagonal locks");
        long negativeChunk = net.minecraft.world.level.ChunkPos.pack(-1, -1);
        var loaded = locks.nearby(center, 8, chunk -> chunk != negativeChunk);
        check(!loaded.contains(origin.asLong()) && loaded.contains(edge.asLong()), "Unavailable chunks must not enter overlay snapshot");
        locks.lock(origin, Blocks.DIRT.defaultBlockState());
        check(locks.nearby(center, 8, chunk -> true).size() == 2, "Updating a lock must not duplicate its index entry");
        locks.remove(origin);
        locks.remove(origin);
        locks.remove(edge);
        check(locks.nearby(center, 8, chunk -> true).isEmpty(), "Unlocked/replaced positions must disappear, repeated removal must be safe");
        locks.lock(origin, state);
        check(locks.nearby(center, 8, chunk -> true).contains(origin.asLong()), "Removed chunk index must be recreated on relock");
        h.succeed();
    }

    @GameTest
    public void overlayPayloadBoundsAndRoundTrip(GameTestHelper h) {
        var dimension = net.minecraft.resources.Identifier.parse("minecraft:the_nether");
        long[] positions = new long[tech.maloandre.chomagerie.network.LockOverlayPayload.MAX_POSITIONS];
        for (int i = 0; i < positions.length; i++) positions[i] = new BlockPos(-i, i % 384 - 64, i).asLong();
        var raw = io.netty.buffer.Unpooled.buffer();
        var buf = new net.minecraft.network.RegistryFriendlyByteBuf(raw, h.getLevel().registryAccess());
        try {
            var expected = new tech.maloandre.chomagerie.network.LockOverlayPayload(dimension, true, true, positions);
            var codec = tech.maloandre.chomagerie.network.LockOverlayPayload.CODEC;
            codec.encode(buf, expected);
            var decoded = codec.decode(buf);
            check(decoded.dimension().equals(dimension) && decoded.reset() && decoded.locked()
                    && java.util.Arrays.equals(decoded.positions(), positions), "Full overlay batch must survive network round trip");
            buf.clear();
            var subscriptionCodec = tech.maloandre.chomagerie.network.LockOverlaySubscriptionPayload.CODEC;
            // Even a manually encoded out-of-range request must be clamped on the server.
            buf.writeBoolean(true); buf.writeVarInt(Integer.MAX_VALUE);
            check(subscriptionCodec.decode(buf).distance() == 128, "Clamp excessive client distance");
            buf.clear(); buf.writeBoolean(false); buf.writeVarInt(-100);
            var disabled = subscriptionCodec.decode(buf);
            check(!disabled.enabled() && disabled.distance() == 8, "Disable must survive decoding, clamp negative distance");
            for (int invalidSize : new int[]{-1, positions.length + 1}) {
                buf.clear(); buf.writeIdentifier(dimension); buf.writeBoolean(false); buf.writeBoolean(true); buf.writeVarInt(invalidSize);
                boolean rejected = false;
                try { codec.decode(buf); } catch (io.netty.handler.codec.DecoderException expectedFailure) { rejected = true; }
                check(rejected, "Reject invalid overlay batch before allocating positions");
            }
        } finally { buf.release(); }
        h.succeed();
    }

    @GameTest
    public void savedDataDiskRoundTrip(GameTestHelper h) throws Exception {
        var level = h.getLevel(); var pos = h.absolutePos(LOCAL);
        var expected = Blocks.GLASS_PANE.defaultBlockState().setValue(BlockStateProperties.EAST, true);
        var dir = Files.createTempDirectory("chomagerie-lock-test-");
        try {
            try (var storage = new SavedDataStorage(dir, level.getServer().getFixerUpper(), level.registryAccess())) {
                var saved = storage.computeIfAbsent(BlockStateLocks.TYPE); saved.lock(pos, expected);
                storage.saveAndJoin();
            }
            try (var storage = new SavedDataStorage(dir, level.getServer().getFixerUpper(), level.registryAccess())) {
                var loaded = storage.computeIfAbsent(BlockStateLocks.TYPE);
                check(loaded.contains(pos), "Lock lost after disk reload");
                check(loaded.nearby(Vec3.atCenterOf(pos), 8, chunk -> true).contains(pos.asLong()),
                        "Overlay chunk index must be rebuilt after disk reload");
                check(loaded.preserve(pos, Blocks.GLASS_PANE.defaultBlockState()) == expected, "Reference lost after disk reload");
            }
        } finally {
            try (var paths = Files.walk(dir)) {
                for (var path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        h.succeed();
    }
}
