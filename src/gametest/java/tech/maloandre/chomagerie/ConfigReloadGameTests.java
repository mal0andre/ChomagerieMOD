package tech.maloandre.chomagerie;

import com.google.gson.Gson;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.level.block.Blocks;
import tech.maloandre.chomagerie.blockstate.BlockStateLocks;
import tech.maloandre.chomagerie.blockstate.BlockToolsConfig;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ConfigReloadGameTests {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Feedback implements CommandSource {
        final List<String> messages = new ArrayList<>();
        @Override public void sendSystemMessage(Component message) { messages.add(message.getString()); }
        @Override public boolean acceptsSuccess() { return true; }
        @Override public boolean acceptsFailure() { return true; }
        @Override public boolean shouldInformAdmins() { return false; }
        boolean completed() { return messages.stream().anyMatch(s -> s.contains("Réglages appliqués")); }
    }

    @GameTest(maxTicks = 600)
    public void reloadValidatesPreservesAndUpdatesRecipes(GameTestHelper h) {
        // Existing block-tool tests temporarily edit the singleton; wait until their selection loops finish.
        h.runAfterDelay(100, () -> {
            var server = h.getLevel().getServer();
            // Vanilla reload pumps server tasks; run it outside GameTest's scheduled-task iterator.
            server.schedule(new net.minecraft.server.TickTask(server.getTickCount(), () -> start(h)));
        });
    }

    private void start(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var previous = BlockToolsConfig.get();
        var path = BlockToolsConfig.path();
        try {
            byte[] original = Files.readAllBytes(path);
            h.runBeforeTestEnd(() -> {
                BlockToolsConfig.apply(previous);
                try { Files.write(path, original); }
                catch (Exception e) { throw new RuntimeException(e); }
            });
            var feedback = new Feedback();
            var source = server.createCommandSourceStack().withSource(feedback);
            var dispatcher = server.getCommands().getDispatcher();
            var node = dispatcher.getRoot().getChild("chomagerie").getChild("reload");
            check(!node.canUse(source.withPermission(PermissionSet.NO_PERMISSIONS)), "Non-operators must not reload");
            check(!node.canUse(source.withPermission(LevelBasedPermissionSet.MODERATOR)), "Level one must not reload");
            check(node.canUse(source.withPermission(LevelBasedPermissionSet.GAMEMASTER)), "Level two operators must reload");
            Files.delete(path);
            check(dispatcher.execute("chomagerie reload", source) == 0 && BlockToolsConfig.get() == previous,
                    "Missing file must preserve the active policy");
            check(!Files.exists(path) && feedback.messages.getLast().contains("fichier introuvable"), "Missing file error must explain the failure without writing defaults");
            String[][] invalid = {
                    {"{\n\"debugStick\":", "line"},
                    {"null", "$"},
                    {"{\"debugStick\":null}", "debugStick"},
                    {"{\"debugStick\":{\"enabled\":\"false\"}}", "debugStick.enabled"},
                    {"{\"blockStateLocking\":{\"enabled\":1}}", "blockStateLocking.enabled"},
                    {"{\"debugStick\":{\"enabledd\":false}}", "debugStick.enabledd"},
                    {"{\"debugStick\":{\"allowedBlocks\":[{\"id\":\"INVALID ID\",\"properties\":{}}]}}", "allowedBlocks[0].id"},
                    {"{\"debugStick\":{\"forbiddenProperties\":{\"east\":[null]}}}", "forbiddenProperties.east[0]"}
            };
            for (String[] test : invalid) {
                Files.writeString(path, test[0]);
                check(dispatcher.execute("chomagerie reload", source) == 0, "Invalid candidate must fail: " + test[0]);
                check(BlockToolsConfig.get() == previous && Files.readString(path).equals(test[0]), "Failure must preserve file and policy");
                check(feedback.messages.getLast().contains(test[1]), "Error must identify invalid location: " + feedback.messages.getLast());
            }
            var pos = h.absolutePos(new BlockPos(2, 2, 2));
            var locks = BlockStateLocks.get(h.getLevel());
            locks.lock(pos, Blocks.GLASS_PANE.defaultBlockState());
            var candidate = new BlockToolsConfig();
            candidate.debugStick.craftingEnabled = previous.debugStick.craftingEnabled;
            candidate.blockStateLocking.enabled = false;
            Files.writeString(path, new Gson().toJson(candidate));
            check(dispatcher.execute("chomagerie reload", source) == 1 && !BlockToolsConfig.get().blockStateLocking.enabled,
                    "Valid locking change must apply immediately");
            check(locks.contains(pos), "Reload must preserve saved locks");
            candidate.blockStateLocking.enabled = true;
            candidate.debugStick.craftingEnabled = !previous.debugStick.craftingEnabled;
            Files.writeString(path, new Gson().toJson(candidate));
            feedback.messages.clear();
            check(dispatcher.execute("chomagerie reload", source) == 1, "Recipe change must start vanilla data reload");
            if (!feedback.completed()) {
                check(dispatcher.execute("chomagerie reload", source) == 0 && feedback.messages.getLast().contains("déjà en cours"),
                        "Overlapping reloads must be rejected while the data reload is pending");
            }
            int[] stage = {0};
            h.onEachTick(() -> {
                if (!feedback.completed()) return;
                try {
                    var key = ResourceKey.create(Registries.RECIPE, Identifier.parse("chomagerie:utility/debug_stick"));
                    boolean recipePresent = server.getRecipeManager().byKey(key).isPresent();
                    if (stage[0] == 0) {
                        check(recipePresent == candidate.debugStick.craftingEnabled, "Craft switch must update live recipe manager");
                        check(locks.contains(pos), "Resource reload must preserve saved locks");
                        Files.write(path, original);
                        feedback.messages.clear();
                        stage[0] = 1;
                        server.schedule(new net.minecraft.server.TickTask(server.getTickCount(), () -> {
                            try {
                                check(server.getCommands().getDispatcher().execute("chomagerie reload", source) == 1,
                                        "Restore must reload successfully");
                            } catch (Exception e) { throw new RuntimeException(e); }
                        }));
                    } else {
                        check(recipePresent == previous.debugStick.craftingEnabled, "Original recipe policy must be restored live");
                        locks.remove(pos);
                        h.succeed();
                    }
                } catch (Exception e) { throw new RuntimeException(e); }
            });
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
