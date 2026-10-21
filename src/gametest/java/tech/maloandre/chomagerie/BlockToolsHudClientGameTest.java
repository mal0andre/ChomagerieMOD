package tech.maloandre.chomagerie;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.CameraType;
import tech.maloandre.chomagerie.client.config.ChomagerieConfigScreen;
import javax.imageio.ImageIO;
import java.nio.file.Path;
import java.io.IOException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DebugStickState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import tech.maloandre.chomagerie.blockstate.BlockStateLocks;
import tech.maloandre.chomagerie.client.config.ChomagerieConfig;
import tech.maloandre.chomagerie.client.overlay.BlockToolHud;
import java.util.Arrays;

public class BlockToolsHudClientGameTest implements FabricClientGameTest {
    private static final BlockPos TARGET = new BlockPos(0, -59, 0);

    @Override public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var settings = ChomagerieConfig.getInstance().lockOverlay;
            settings.enabled = false; settings.showTargetLock = true;
            ChomagerieConfig.getInstance().save();
        });
        try (var world = context.worldBuilder().create()) {
            var connection = world.getConnection(); var server = world.getServer();
            server.runCommand("gamemode creative @p");
            server.runCommand("tp @p 0.5 -60 3.5 180 0");
            server.runOnServer(instance -> {
                var level = connection.getServerLevel(); var player = connection.getServerPlayer();
                var state = Blocks.GLASS_PANE.defaultBlockState().setValue(BlockStateProperties.WEST, true);
                level.setBlock(TARGET, state, 18);
                BlockStateLocks.get(level).lock(TARGET, state);
                var stick = new ItemStack(Items.DEBUG_STICK);
                stick.set(DataComponents.DEBUG_STICK_STATE, DebugStickState.EMPTY.withProperty(state.typeHolder(), BlockStateProperties.WEST));
                player.setItemInHand(InteractionHand.MAIN_HAND, stick);
                player.containerMenu.broadcastChanges();
            });
            connection.waitForChunksRender();
            context.getInput().lookAt(TARGET);
            context.waitFor(BlockToolHud::isTargetLocked);
            context.runOnClient(client -> check(!ChomagerieConfig.getInstance().lockOverlay.enabled,
                    "Target icon must not activate the area overlay"));
            server.runOnServer(instance -> {
                var level = connection.getServerLevel();
                BlockStateLocks.get(level).setManually(level, TARGET, level.getBlockState(TARGET).setValue(BlockStateProperties.EAST, true), 18);
            });
            context.waitTicks(6);
            check(context.computeOnClient(BlockToolHud::isTargetLocked), "Icon must survive changes to locked block properties");
            server.runOnServer(instance -> BlockStateLocks.get(connection.getServerLevel()).remove(TARGET));
            context.waitFor(client -> !BlockToolHud.isTargetLocked(client));
            server.runOnServer(instance -> {
                var level = connection.getServerLevel(); BlockStateLocks.get(level).lock(TARGET, level.getBlockState(TARGET));
            });
            context.waitFor(BlockToolHud::isTargetLocked);
            Path locked = context.takeScreenshot("target-lock-icon");
            context.runOnClient(client -> ChomagerieConfig.getInstance().lockOverlay.showTargetLock = false);
            context.waitTick();
            check(!context.computeOnClient(BlockToolHud::isTargetLocked), "Lock icon must be optional");
            Path hidden = context.takeScreenshot("target-lock-icon-disabled");
            int[] viewport = context.computeOnClient(client -> new int[]{client.getWindow().getGuiScaledWidth() / 2,
                    client.getWindow().getGuiScaledHeight() / 2, client.getWindow().getGuiScale()});
            check(brightPixels(locked, viewport, 9, -6, 10, 12) > brightPixels(hidden, viewport, 9, -6, 10, 12) + 20,
                    "A small lock must actually render beside the crosshair");
            check(Math.abs(brightPixels(locked, viewport, -80, 17, 160, 24)
                    - brightPixels(hidden, viewport, -80, 17, 160, 24)) <= 5 * viewport[2] * viewport[2],
                    "No duplicate property or lock text may render below the crosshair");
            context.runOnClient(client -> ChomagerieConfig.getInstance().lockOverlay.showTargetLock = true);
            context.waitFor(BlockToolHud::isTargetLocked);
            context.runOnClient(client -> client.gui.hud.toggle());
            check(!context.computeOnClient(BlockToolHud::isTargetLocked), "F1 must hide the icon");
            context.waitTicks(6);
            context.runOnClient(client -> client.gui.hud.toggle());
            context.waitFor(BlockToolHud::isTargetLocked);
            context.setScreen(() -> new ChomagerieConfigScreen(null));
            check(!context.computeOnClient(BlockToolHud::isTargetLocked), "Menus must hide the icon");
            context.waitTicks(6);
            context.setScreen(() -> null);
            context.waitFor(BlockToolHud::isTargetLocked);
            context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            check(!context.computeOnClient(BlockToolHud::isTargetLocked), "Icon must follow the first-person crosshair");
            context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
            context.getInput().lookAt(180, -45);
            context.waitFor(client -> !BlockToolHud.isTargetLocked(client));
            context.getInput().lookAt(TARGET);
            context.waitFor(BlockToolHud::isTargetLocked);
            KeyMapping toggle = context.computeOnClient(client -> Arrays.stream(client.options.keyMappings)
                    .filter(key -> key.getName().equals("key.chomagerie.toggle_lock_overlay")).findFirst().orElseThrow());
            context.runOnClient(client -> check(Arrays.stream(client.options.keyMappings).noneMatch(other -> other != toggle
                    && other.getCategory() != KeyMapping.Category.DEBUG && toggle.same(other)),
                    "Default shortcut must not conflict with ordinary vanilla controls"));
            context.runOnClient(client -> { toggle.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_F8)); KeyMapping.resetMapping(); });
            context.getInput().pressKey(InputConstants.KEY_H);
            check(!context.computeOnClient(client -> ChomagerieConfig.getInstance().lockOverlay.enabled), "Previous binding must no longer toggle the overlay");
            context.getInput().pressKey(InputConstants.KEY_F8);
            context.waitFor(client -> ChomagerieConfig.getInstance().lockOverlay.enabled);
            context.getInput().pressKey(InputConstants.KEY_F8);
            context.waitFor(client -> !ChomagerieConfig.getInstance().lockOverlay.enabled);
            context.runOnClient(client -> {
                ChomagerieConfig.getInstance().reload();
                check(!ChomagerieConfig.getInstance().lockOverlay.enabled, "Shortcut state must be persisted");
            });
        }
    }
    private static int brightPixels(Path path, int[] viewport, int dx, int dy, int width, int height) {
        try {
            var image = ImageIO.read(path.toFile());
            int scale = viewport[2], count = 0;
            int left = (viewport[0] + dx) * scale, top = (viewport[1] + dy) * scale;
            for (int y = top; y < top + height * scale; y++) for (int x = left; x < left + width * scale; x++) {
                int color = image.getRGB(x, y);
                if (((color >>> 16) & 255) > 180 && ((color >>> 8) & 255) > 180 && (color & 255) > 180) count++;
            }
            return count;
        } catch (IOException error) { throw new AssertionError("Cannot read icon screenshot", error); }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
