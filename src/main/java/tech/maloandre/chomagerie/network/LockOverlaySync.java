package tech.maloandre.chomagerie.network;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import tech.maloandre.chomagerie.blockstate.BlockStateLocks;
import tech.maloandre.chomagerie.blockstate.BlockToolsConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-thread-only, opt-in synchronization; vanilla clients never receive custom packets. */
public final class LockOverlaySync {
    public static final int MAX_POSITIONS_PER_SYNC = 4096;
    private static final Map<UUID, Subscription> SUBSCRIPTIONS = new HashMap<>();

    private static final class Subscription {
        int distance;
        Identifier dimension;
        final LockOverlayDelta delta = new LockOverlayDelta();
        Vec3 lastPosition;
        Long2LongOpenHashMap revisions = new Long2LongOpenHashMap();
        boolean lastEnabled;
        LongSet available = new LongOpenHashSet();
        Subscription(int distance) { this.distance = distance; }
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(LockOverlaySubscriptionPayload.ID, LockOverlaySubscriptionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LockOverlayPayload.ID, LockOverlayPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(LockOverlaySubscriptionPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            if (!ServerPlayNetworking.canSend(player, LockOverlayPayload.ID)) return;
            if (!payload.enabled()) {
                SUBSCRIPTIONS.remove(player.getUUID());
                ServerPlayNetworking.send(player, new LockOverlayPayload(player.level().dimension().identifier(), true, false, new long[0]));
            } else {
                Subscription subscription = SUBSCRIPTIONS.computeIfAbsent(player.getUUID(), key -> new Subscription(payload.distance()));
                subscription.distance = payload.distance();
                subscription.dimension = null; // Resubscription after travel or a setting change needs a fresh snapshot.
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (SUBSCRIPTIONS.isEmpty() || server.getTickCount() % 5 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                Subscription subscription = SUBSCRIPTIONS.get(player.getUUID());
                if (subscription != null && ServerPlayNetworking.canSend(player, LockOverlayPayload.ID)) sync(player, subscription);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SUBSCRIPTIONS.remove(handler.player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SUBSCRIPTIONS.clear());
    }

    private static void sync(ServerPlayer player, Subscription subscription) {
        var level = player.level();
        Identifier dimension = level.dimension().identifier();
        if (!dimension.equals(subscription.dimension)) {
            subscription.dimension = dimension;
            subscription.delta.clear();
            subscription.lastPosition = null;
            ServerPlayNetworking.send(player, new LockOverlayPayload(dimension, true, false, new long[0]));
        }
        var locks = BlockStateLocks.get(level);
        boolean enabled = BlockToolsConfig.get().blockStateLocking.enabled;
        LongSet available = new LongOpenHashSet();
        Long2LongOpenHashMap revisions = new Long2LongOpenHashMap();
        var center = player.position();
        int distance = subscription.distance;
        for (int x = ((int) Math.floor(center.x - distance)) >> 4; x <= ((int) Math.floor(center.x + distance)) >> 4; x++) {
            for (int z = ((int) Math.floor(center.z - distance)) >> 4; z <= ((int) Math.floor(center.z + distance)) >> 4; z++) {
                if (level.hasChunk(x, z)) {
                    long chunk = ChunkPos.pack(x, z);
                    available.add(chunk);
                    long revision = locks.chunkRevision(chunk);
                    if (revision != 0) revisions.put(chunk, revision);
                }
            }
        }
        if (!center.equals(subscription.lastPosition) || !revisions.equals(subscription.revisions)
                || enabled != subscription.lastEnabled || !available.equals(subscription.available)) {
            LongSet current = enabled ? locks.nearby(center, distance, available::contains) : new LongOpenHashSet();
            subscription.delta.update(current);
            subscription.lastPosition = center;
            subscription.revisions = revisions;
            subscription.lastEnabled = enabled;
            subscription.available = available;
        }
        for (var change : subscription.delta.drain(MAX_POSITIONS_PER_SYNC)) {
            ServerPlayNetworking.send(player, new LockOverlayPayload(dimension, false, change.locked(), change.positions()));
        }
    }

    private LockOverlaySync() { }
}
