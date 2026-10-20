package tech.maloandre.chomagerie.client.overlay;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tech.maloandre.chomagerie.client.config.ChomagerieConfig;
import tech.maloandre.chomagerie.network.LockOverlayPayload;
import tech.maloandre.chomagerie.network.LockOverlaySubscriptionPayload;

import java.util.ArrayList;
import java.util.List;

/** Main-thread cache, immutable geometry per extracted frame, vanilla always-on-top rendering. */
public final class LockOverlay {
    private static final LongSet LOCKS = new LongOpenHashSet();
    private record OverlayFrame(List<AABB> boxes, GizmoStyle style) { }
    private static final RenderStateDataKey<OverlayFrame> FRAME = RenderStateDataKey.create(() -> "chomagerie lock overlay");
    private static Identifier dimension;
    private static boolean requested;
    private static int requestedDistance;

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(LockOverlayPayload.ID, (payload, context) -> {
            var client = context.client();
            if (!requested || client.level == null || !ChomagerieConfig.getInstance().lockOverlay.enabled
                    || !client.level.dimension().identifier().equals(payload.dimension())) return;
            if (payload.reset()) LOCKS.clear();
            for (long pos : payload.positions()) {
                if (payload.locked()) LOCKS.add(pos);
                else LOCKS.remove(pos);
            }
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> clear());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        ClientTickEvents.END_CLIENT_TICK.register(LockOverlay::tick);
        LevelExtractionEvents.END_EXTRACTION.register(context -> {
            List<AABB> boxes = new ArrayList<>();
            var client = Minecraft.getInstance();
            var settings = ChomagerieConfig.getInstance().lockOverlay;
            if (requested && settings.enabled && client.player != null
                    && context.level().dimension().identifier().equals(dimension)) {
                double rangeSquared = (double) settings.getDistance() * settings.getDistance();
                for (long packed : LOCKS) {
                    BlockPos pos = BlockPos.of(packed);
                    if (!context.level().hasChunkAt(pos)
                            || Vec3.atCenterOf(pos).distanceToSqr(client.player.position()) > rangeSquared) continue;
                    var state = context.level().getBlockState(pos);
                    if (state.isAir()) continue;
                    var shape = state.getShape(context.level(), pos);
                    if (shape.isEmpty()) boxes.add(new AABB(pos).inflate(0.002));
                    else for (AABB box : shape.toAabbs()) boxes.add(box.move(pos).inflate(0.002));
                }
            }
            int rgb = settings.getColorRgb();
            context.levelState().setData(FRAME, new OverlayFrame(List.copyOf(boxes),
                    GizmoStyle.strokeAndFill(0xFF000000 | rgb, 2.0F, 0x30000000 | rgb)));
        });
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> {
            OverlayFrame frame = context.levelState().getData(FRAME);
            if (frame == null || frame.boxes().isEmpty()) return;
            try (var ignored = context.levelRenderer().collectPerFrameRenderThreadGizmos()) {
                for (AABB box : frame.boxes()) Gizmos.cuboid(box, frame.style()).setAlwaysOnTop();
            }
        });
    }

    private static void tick(Minecraft client) {
        if (client.level == null || client.player == null || !ClientPlayNetworking.canSend(LockOverlaySubscriptionPayload.ID)) {
            clear();
            return;
        }
        Identifier currentDimension = client.level.dimension().identifier();
        if (!currentDimension.equals(dimension)) {
            clear();
            dimension = currentDimension;
        }
        var settings = ChomagerieConfig.getInstance().lockOverlay;
        int distance = settings.getDistance();
        if (requested != settings.enabled || (settings.enabled && requestedDistance != distance)) {
            // Clear immediately on disable; rendering applies a reduced range before the next server update.
            if (!settings.enabled) LOCKS.clear();
            requested = settings.enabled;
            requestedDistance = distance;
            ClientPlayNetworking.send(new LockOverlaySubscriptionPayload(requested, distance));
        }
    }

    private static void clear() {
        LOCKS.clear();
        dimension = null;
        requested = false;
        requestedDistance = 0;
    }

    private LockOverlay() { }
}
