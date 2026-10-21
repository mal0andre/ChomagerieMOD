package tech.maloandre.chomagerie.client.overlay;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import tech.maloandre.chomagerie.client.config.ChomagerieConfig;
import tech.maloandre.chomagerie.network.TargetLockPayload;
import tech.maloandre.chomagerie.network.TargetLockRequestPayload;

/** A tiny lock icon beside the crosshair; vanilla text and attack indicators remain untouched. */
public final class BlockToolHud {
    private static BlockPos requestedPos;
    private static Identifier requestedDimension;
    private static TargetLockPayload status;
    private static int ticks;
    private static int nextRequest;

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(TargetLockPayload.ID, (payload, context) -> {
            if (context.client().level != null && payload.dimension().equals(context.client().level.dimension().identifier())
                    && payload.dimension().equals(requestedDimension) && payload.pos().equals(requestedPos)) status = payload;
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> clear());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        ClientTickEvents.END_CLIENT_TICK.register(BlockToolHud::tick);
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, Identifier.parse("chomagerie:block_tools"), (graphics, delta) -> {
            if (!isTargetLocked(Minecraft.getInstance())) return;
            int x = graphics.guiWidth() / 2 + 10;
            int y = graphics.guiHeight() / 2 - 5;
            lockSilhouette(graphics, x + 1, y + 1, 0xB0000000);
            lockSilhouette(graphics, x, y, 0xFFE0E0E0);
            graphics.fill(x + 3, y + 5, x + 4, y + 8, 0xFF555555);
        });
    }

    private static BlockPos target(Minecraft client) {
        if (client.level == null || client.player == null || client.gui.screen() != null || client.gui.hud.isHidden()
                || !client.options.getCameraType().isFirstPerson()) return null;
        if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
        return client.level.hasChunkAt(hit.getBlockPos()) && client.player.isWithinBlockInteractionRange(hit.getBlockPos(), 0.0)
                ? hit.getBlockPos() : null;
    }

    private static void tick(Minecraft client) {
        ticks++;
        if (client.level == null || !ClientPlayNetworking.canSend(TargetLockRequestPayload.ID)) { clear(); return; }
        var settings = ChomagerieConfig.getInstance().lockOverlay;
        BlockPos pos = settings.showTargetLock ? target(client) : null;
        Identifier dimension = client.level.dimension().identifier();
        if (pos == null) {
            if (requestedPos != null) ClientPlayNetworking.send(new TargetLockRequestPayload(false, BlockPos.ZERO));
            requestedPos = null;
            requestedDimension = null;
            status = null;
            return;
        }
        if (!pos.equals(requestedPos) || !dimension.equals(requestedDimension)) status = null;
        if (ticks < nextRequest) return;
        if (!pos.equals(requestedPos) || !dimension.equals(requestedDimension) || status == null || !status.known()) {
            requestedPos = pos.immutable();
            requestedDimension = dimension;
            ClientPlayNetworking.send(new TargetLockRequestPayload(true, requestedPos));
            nextRequest = ticks + 5;
        }
    }

    public static boolean isTargetLocked(Minecraft client) {
        BlockPos pos = target(client);
        if (pos == null || !ChomagerieConfig.getInstance().lockOverlay.showTargetLock) return false;
        var state = client.level.getBlockState(pos);
        return !state.isAir() && status != null && status.known() && status.locked() && pos.equals(status.pos())
                && client.level.dimension().identifier().equals(status.dimension())
                && state.typeHolder().getRegisteredName().equals(status.block().toString());
    }

    // Seven by nine GUI pixels, plus a one-pixel shadow; no font or external texture required.
    private static void lockSilhouette(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.fill(x + 2, y, x + 5, y + 1, color);
        graphics.fill(x + 1, y + 1, x + 2, y + 3, color);
        graphics.fill(x + 5, y + 1, x + 6, y + 3, color);
        graphics.fill(x, y + 3, x + 7, y + 9, color);
    }

    private static void clear() {
        requestedPos = null;
        requestedDimension = null;
        status = null;
        ticks = 0;
        nextRequest = 0;
    }
    private BlockToolHud() { }
}
