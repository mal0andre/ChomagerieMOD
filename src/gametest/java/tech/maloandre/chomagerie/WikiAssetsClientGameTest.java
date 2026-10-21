package tech.maloandre.chomagerie;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Optional documentation export, using Minecraft's actual inventory item renderer. */
public class WikiAssetsClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        String output = System.getenv("CHOMAGERIE_WIKI_ASSETS");
        if (output == null || output.isBlank()) return;
        try (var world = context.worldBuilder().create()) {
            Path directory = Path.of(output);
            List<String> ids = Files.readAllLines(directory.resolve("items.txt"));
            int[] viewport = context.computeOnClient(client -> new int[]{client.getWindow().getGuiScaledWidth(),
                    client.getWindow().getGuiScaledHeight(), client.getWindow().getGuiScale()});
            int columns = (viewport[0] - 16) / 24;
            int rows = (viewport[1] - 16) / 24;
            int perPage = columns * rows;
            if (perPage <= 0) throw new IllegalStateException("Window too small for item atlas");
            for (int start = 0; start < ids.size(); start += perPage) {
                int first = start;
                var stacks = ids.subList(start, Math.min(start + perPage, ids.size())).stream().map(id -> {
                    var parts = id.split("@level=", 2);
                    var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(parts[0]));
                    if (item == null || item == Items.AIR) throw new IllegalArgumentException("No inventory item for " + id);
                    var stack = new ItemStack(item);
                    if (parts.length == 2) stack.set(net.minecraft.core.component.DataComponents.BLOCK_STATE,
                            new net.minecraft.world.item.component.BlockItemStateProperties(java.util.Map.of("level", parts[1])));
                    return stack;
                }).toList();
                context.setScreen(() -> new Screen(Component.literal("Wiki inventory atlas")) {
                    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
                        graphics.fill(0, 0, width, height, 0xFF8B8B8B);
                        for (int index = 0; index < stacks.size(); index++) {
                            graphics.item(stacks.get(index), 8 + index % columns * 24, 8 + index / columns * 24);
                        }
                    }
                });
                context.waitTicks(4);
                var screenshot = ImageIO.read(context.takeScreenshot("wiki-items-" + start).toFile());
                int scale = viewport[2];
                for (int index = 0; index < stacks.size(); index++) {
                    int x = (8 + index % columns * 24) * scale;
                    int y = (8 + index / columns * 24) * scale;
                    ImageIO.write(screenshot.getSubimage(x, y, 16 * scale, 16 * scale), "png",
                            directory.resolve(ids.get(first + index).replace(':', '_') + ".png").toFile());
                }
            }
            context.setScreen(() -> null);
            var server = world.getServer();
            var connection = world.getConnection();
            var target = new net.minecraft.core.BlockPos(0, -60, 0);
            server.runCommand("gamemode creative @p");
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            server.runCommand("fill -3 -61 -2 4 -61 3 minecraft:smooth_stone");
            server.runCommand("tp @p 0.5 -60 4 180 0");
            server.runOnServer(instance -> {
                var level = connection.getServerLevel();
                level.setBlock(target, net.minecraft.world.level.block.Blocks.OAK_FENCE.defaultBlockState(), 3);
                var player = connection.getServerPlayer();
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.HONEYCOMB));
                player.containerMenu.broadcastChanges();
            });
            context.runOnClient(client -> {
                var config = tech.maloandre.chomagerie.client.config.ChomagerieConfig.getInstance().lockOverlay;
                config.enabled = false;
                config.showTargetLock = true;
                config.color = "#FF3030";
                if (client.gui.hud.isHidden()) client.gui.hud.toggle();
            });
            connection.waitForChunksRender();
            context.waitTicks(10);
            context.getInput().lookAt(target);
            context.waitTicks(6);
            cleanHud(context);
            context.takeScreenshot("wiki-lock-01-before");
            server.runOnServer(instance -> {
                useTool(connection.getServerPlayer(), new ItemStack(Items.HONEYCOMB), target);
                var level = connection.getServerLevel();
                if (!tech.maloandre.chomagerie.blockstate.BlockStateLocks.get(level).contains(target))
                    throw new IllegalStateException("Honeycomb did not lock the example fence");
                level.setBlock(target.east(), net.minecraft.world.level.block.Blocks.OAK_FENCE.defaultBlockState(), 3);
            });
            context.runOnClient(client -> tech.maloandre.chomagerie.client.config.ChomagerieConfig.getInstance().lockOverlay.enabled = true);
            context.waitTicks(10);
            connection.waitForClientboundPackets();
            cleanHud(context);
            context.takeScreenshot("wiki-lock-02-honey");
            server.runOnServer(instance -> {
                var state = connection.getServerLevel().getBlockState(target);
                var stick = new ItemStack(Items.DEBUG_STICK);
                stick.set(net.minecraft.core.component.DataComponents.DEBUG_STICK_STATE,
                        net.minecraft.world.item.component.DebugStickState.EMPTY.withProperty(state.typeHolder(),
                                net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST));
                useTool(connection.getServerPlayer(), stick, target);
            });
            context.waitTicks(6);
            cleanHud(context);
            context.takeScreenshot("wiki-lock-03-debug-stick");
            server.runOnServer(instance -> {
                useTool(connection.getServerPlayer(), new ItemStack(Items.IRON_AXE), target);
                if (tech.maloandre.chomagerie.blockstate.BlockStateLocks.get(connection.getServerLevel()).contains(target))
                    throw new IllegalStateException("Axe did not unlock the example fence");
            });
            context.waitTicks(10);
            cleanHud(context);
            context.takeScreenshot("wiki-lock-04-axe");
        } catch (Exception e) { throw new RuntimeException("Could not export wiki inventory assets", e); }
    }
    private static void cleanHud(ClientGameTestContext context) {
        context.runOnClient(client -> {
            client.gui.toastManager().clear();
            client.gui.hud.setOverlayMessage(Component.empty(), false);
            client.gui.hud.getChat().clearMessages(true);
        });
        context.waitTick();
    }

    private static void useTool(net.minecraft.server.level.ServerPlayer player, ItemStack tool, net.minecraft.core.BlockPos pos) {
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, tool);
        var result = player.gameMode.useItemOn(player, player.level(), tool, net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),
                        net.minecraft.core.Direction.UP, pos, false));
        if (!result.consumesAction()) throw new IllegalStateException("Example tool interaction failed: " + tool);
        player.containerMenu.broadcastChanges();
    }
}
