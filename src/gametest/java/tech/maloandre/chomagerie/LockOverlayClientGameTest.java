package tech.maloandre.chomagerie;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import tech.maloandre.chomagerie.blockstate.BlockStateLocks;
import tech.maloandre.chomagerie.client.config.ChomagerieConfig;
import tech.maloandre.chomagerie.client.config.ChomagerieConfigScreen;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Path;

/** Opt-in graphical test: ./gradlew runClientGameTest. Uses a disposable flat test world. */
public class LockOverlayClientGameTest implements FabricClientGameTest {
    private static final BlockPos TARGET = new BlockPos(0, -59, 0);

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            ChomagerieConfig.getInstance().lockOverlay.enabled = false;
            ChomagerieConfig.getInstance().lockOverlay.distance = 32;
            ChomagerieConfig.getInstance().lockOverlay.color = ChomagerieConfig.LockOverlayConfig.DEFAULT_COLOR;
        });
        try (var world = context.worldBuilder().create()) {
            var server = world.getServer();
            var connection = world.getConnection();
            server.runCommand("gamemode creative @p");
            server.runCommand("tp @p 0.5 -60 12.5 180 0");
            server.runCommand("fill -4 -60 8 4 -55 8 minecraft:stone");
            server.runOnServer(instance -> {
                var level = connection.getServerLevel();
                level.setBlock(TARGET, Blocks.STONE.defaultBlockState(), 3);
                BlockStateLocks.get(level).lock(TARGET, level.getBlockState(TARGET));
            });
            context.runOnClient(client -> { if (!client.gui.hud.isHidden()) client.gui.hud.toggle(); });
            context.waitTicks(10);
            connection.waitForChunksRender();
            int baseline = redPixels(context.takeScreenshot("locks-disabled"));
            context.runOnClient(client -> ChomagerieConfig.getInstance().lockOverlay.enabled = true);
            synchronize(context, connection);
            int highlighted = redPixels(context.takeScreenshot("locks-through-wall"));
            check(highlighted > baseline + 10, "Locked block must render red through the stone wall");
            context.runOnClient(client -> ChomagerieConfig.getInstance().lockOverlay.distance = 8);
            synchronize(context, connection);
            check(redPixels(context.takeScreenshot("locks-out-of-range")) <= baseline + 5,
                    "Reducing range must hide the locked block");
            context.runOnClient(client -> ChomagerieConfig.getInstance().lockOverlay.distance = 32);
            synchronize(context, connection);
            check(redPixels(context.takeScreenshot("locks-range-restored")) > baseline + 10,
                    "Increasing range must restore the overlay");
            context.runOnClient(client -> ChomagerieConfig.getInstance().lockOverlay.color = "#3030FF");
            context.waitTicks(2);
            Path blue = context.takeScreenshot("locks-custom-blue");
            check(coloredPixels(blue, true) > 10 && redPixels(blue) <= baseline + 5,
                    "Changing color must immediately recolor both outline and fill through the wall");
            context.runOnClient(client -> ChomagerieConfig.getInstance().lockOverlay.color = ChomagerieConfig.LockOverlayConfig.DEFAULT_COLOR);
            server.runOnServer(instance -> BlockStateLocks.get(connection.getServerLevel()).remove(TARGET));
            synchronize(context, connection);
            check(redPixels(context.takeScreenshot("locks-unlocked")) <= baseline + 5,
                    "Server unlock must remove the overlay");
            server.runOnServer(instance -> BlockStateLocks.get(connection.getServerLevel()).lock(TARGET, Blocks.STONE.defaultBlockState()));
            synchronize(context, connection);
            context.runOnClient(client -> ChomagerieConfig.getInstance().lockOverlay.enabled = false);
            synchronize(context, connection);
            check(redPixels(context.takeScreenshot("locks-toggle-off")) <= baseline + 5,
                    "Disabling overlay must clear highlighted blocks");
            context.runOnClient(client -> { if (client.gui.hud.isHidden()) client.gui.hud.toggle(); });
            context.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_K);
            context.waitForScreen(ChomagerieConfigScreen.class);
            double[] tab = context.computeOnClient(client -> {
                int tabWidth = Math.min(112, Math.max(40, (client.gui.screen().width - 20) / 4 - 2));
                double scale = client.getWindow().getGuiScale();
                return new double[]{(10 + 3 * (tabWidth + 2) + tabWidth / 2.0) * scale, 50 * scale};
            });
            context.getInput().setCursorPos(tab[0], tab[1]);
            context.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
            context.runOnClient(client -> {
                var screen = (ChomagerieConfigScreen) client.gui.screen();
                check(screen.getConfigs().size() == 4, "Overlay tab must contain toggle, distance, color and lock icon option");
                var enabled = (fi.dy.masa.malilib.config.options.ConfigBoolean) screen.getConfigs().get(0).getConfig();
                var distance = (fi.dy.masa.malilib.config.options.ConfigInteger) screen.getConfigs().get(1).getConfig();
                enabled.setBooleanValue(true);
                distance.setIntegerValue(48);
                var color = (fi.dy.masa.malilib.config.options.ConfigColor) screen.getConfigs().get(2).getConfig();
                color.setIntegerValue(0xFF3030FF);
                var settings = ChomagerieConfig.getInstance().lockOverlay;
                check(settings.enabled && settings.getDistance() == 48 && settings.getColorRgb() == 0x3030FF,
                        "Menu controls must update saved overlay preferences");
                ChomagerieConfig.getInstance().reload();
                check(ChomagerieConfig.getInstance().lockOverlay.getColorRgb() == 0x3030FF, "Color must survive configuration reload");
                settings.color = null;
                check(settings.getColorRgb() == 0xFF3030, "Missing color must default to red");
                settings.color = "invalid";
                check(settings.getColorRgb() == 0xFF3030, "Malformed color must default to red");
            });
            context.waitTicks(2);
            context.takeScreenshot("locks-configuration");
            context.setScreen(() -> null);
        }
    }

    private static void synchronize(ClientGameTestContext context,
            net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection connection) {
        context.waitTick();
        connection.waitForServerboundPackets();
        context.waitTicks(6);
        connection.waitForClientboundPackets();
        context.waitTick();
    }

    private static int redPixels(Path screenshot) {
        return coloredPixels(screenshot, false);
    }

    private static int coloredPixels(Path screenshot, boolean blue) {
        try {
            var image = ImageIO.read(screenshot.toFile());
            int count = 0;
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                int color = image.getRGB(x, y);
                int r = (color >>> 16) & 255;
                int g = (color >>> 8) & 255;
                int b = color & 255;
                int primary = blue ? b : r;
                int other = blue ? r : b;
                if (primary > 120 && primary > g * 1.5 && primary > other * 1.5) count++;
            }
            return count;
        } catch (IOException e) { throw new AssertionError("Cannot read overlay screenshot", e); }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
