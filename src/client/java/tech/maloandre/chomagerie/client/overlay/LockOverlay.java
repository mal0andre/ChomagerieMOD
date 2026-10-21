package tech.maloandre.chomagerie.client.overlay;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
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
    private record CachedShape(BlockState state, long tick, List<AABB> boxes) { }
    private record ChunkGeometry(AABB bounds, List<AABB> boxes) { }
    private static final Long2ObjectOpenHashMap<LongSet> BY_CHUNK = new Long2ObjectOpenHashMap<>();
    private static final Long2ObjectOpenHashMap<CachedShape> SHAPES = new Long2ObjectOpenHashMap<>();
    private static List<ChunkGeometry> geometry = List.of();
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
            if (payload.reset()) clearLocks();
            for (long pos : payload.positions()) {
                long chunk = ChunkPos.pack(BlockPos.of(pos));
                if (payload.locked()) {
                    if (LOCKS.add(pos)) BY_CHUNK.computeIfAbsent(chunk, key -> new LongOpenHashSet()).add(pos);
                } else if (LOCKS.remove(pos)) {
                    SHAPES.remove(pos);
                    LongSet positions = BY_CHUNK.get(chunk);
                    positions.remove(pos);
                    if (positions.isEmpty()) BY_CHUNK.remove(chunk);
                }
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
                var frustum = context.levelState().cameraRenderState.cullFrustum;
                for (ChunkGeometry chunk : geometry) {
                    if (frustum != null && !frustum.isVisible(chunk.bounds())) continue;
                    for (AABB box : chunk.boxes()) if (frustum == null || frustum.isVisible(box)) boxes.add(box);
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
            if (!settings.enabled) clearLocks();
            requested = settings.enabled;
            requestedDistance = distance;
            ClientPlayNetworking.send(new LockOverlaySubscriptionPayload(requested, distance));
        }
        refreshGeometry(client);
    }

    /** World reads at most once per client tick; static shapes survive subsequent ticks unchanged. */
    private static void refreshGeometry(Minecraft client) {
        if (!requested) { geometry = List.of(); return; }
        List<ChunkGeometry> chunks = new ArrayList<>();
        double rangeSquared = (double) requestedDistance * requestedDistance;
        long tick = client.level.getGameTime();
        for (var entry : BY_CHUNK.long2ObjectEntrySet()) {
            long chunk = entry.getLongKey();
            if (!client.level.hasChunk(ChunkPos.getX(chunk), ChunkPos.getZ(chunk))) {
                for (long packed : entry.getValue()) SHAPES.remove(packed);
                continue;
            }
            List<AABB> boxes = new ArrayList<>();
            for (long packed : entry.getValue()) {
                BlockPos pos = BlockPos.of(packed);
                if (Vec3.atCenterOf(pos).distanceToSqr(client.player.position()) > rangeSquared) continue;
                BlockState state = client.level.getBlockState(pos);
                if (state.isAir()) { SHAPES.remove(packed); continue; }
                CachedShape cached = SHAPES.get(packed);
                if (cached == null || cached.state() != state || (state.getBlock().hasDynamicShape() && cached.tick() != tick)) {
                    var shape = state.getShape(client.level, pos);
                    List<AABB> shapeBoxes = shape.isEmpty() ? List.of(new AABB(pos).inflate(0.002))
                            : shape.toAabbs().stream().map(box -> box.move(pos).inflate(0.002)).toList();
                    cached = new CachedShape(state, tick, shapeBoxes);
                    SHAPES.put(packed, cached);
                }
                boxes.addAll(cached.boxes());
            }
            if (!boxes.isEmpty()) {
                double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
                double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
                for (AABB box : boxes) {
                    minX = Math.min(minX, box.minX); minY = Math.min(minY, box.minY); minZ = Math.min(minZ, box.minZ);
                    maxX = Math.max(maxX, box.maxX); maxY = Math.max(maxY, box.maxY); maxZ = Math.max(maxZ, box.maxZ);
                }
                chunks.add(new ChunkGeometry(new AABB(minX, minY, minZ, maxX, maxY, maxZ), List.copyOf(boxes)));
            }
        }
        geometry = List.copyOf(chunks);
    }

    private static void clearLocks() {
        LOCKS.clear();
        BY_CHUNK.clear();
        SHAPES.clear();
        geometry = List.of();
    }

    private static void clear() {
        clearLocks();
        dimension = null;
        requested = false;
        requestedDistance = 0;
    }

    private LockOverlay() { }
}
