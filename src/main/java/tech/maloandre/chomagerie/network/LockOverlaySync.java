package tech.maloandre.chomagerie.network;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import tech.maloandre.chomagerie.blockstate.BlockStateLocks;
import tech.maloandre.chomagerie.blockstate.BlockToolsConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-thread-only, opt-in synchronization; vanilla clients never receive custom packets. */
public final class LockOverlaySync {
    private static final Map<UUID, Subscription> SUBSCRIPTIONS = new HashMap<>();

    private static final class Subscription {
        int distance;
        Identifier dimension;
        LongSet sent = new LongOpenHashSet();
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
            if (server.getTickCount() % 5 != 0) return;
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
            subscription.sent.clear();
            ServerPlayNetworking.send(player, new LockOverlayPayload(dimension, true, false, new long[0]));
        }
        LongSet current = BlockToolsConfig.get().blockStateLocking.enabled
                ? BlockStateLocks.get(level).nearby(player.position(), subscription.distance,
                    chunk -> level.hasChunk(ChunkPos.getX(chunk), ChunkPos.getZ(chunk)))
                : new LongOpenHashSet();
        LongSet removed = new LongOpenHashSet(subscription.sent);
        removed.removeAll(current);
        LongSet added = new LongOpenHashSet(current);
        added.removeAll(subscription.sent);
        sendBatches(player, dimension, false, removed);
        sendBatches(player, dimension, true, added);
        subscription.sent = current;
    }

    private static void sendBatches(ServerPlayer player, Identifier dimension, boolean locked, LongSet positions) {
        var iterator = positions.iterator();
        int remaining = positions.size();
        while (remaining > 0) {
            long[] batch = new long[Math.min(remaining, LockOverlayPayload.MAX_POSITIONS)];
            for (int i = 0; i < batch.length; i++) batch[i] = iterator.nextLong();
            ServerPlayNetworking.send(player, new LockOverlayPayload(dimension, false, locked, batch));
            remaining -= batch.length;
        }
    }

    private LockOverlaySync() { }
}
