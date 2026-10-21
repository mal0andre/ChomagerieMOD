package tech.maloandre.chomagerie.network;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import tech.maloandre.chomagerie.blockstate.BlockStateLocks;
import tech.maloandre.chomagerie.blockstate.BlockToolsConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** One nearby target per client; independent of overlay synchronization and never loads chunks. */
public final class TargetLockSync {
    private static final Map<UUID, Target> TARGETS = new HashMap<>();
    private static final class Target {
        final Identifier dimension;
        final BlockPos pos;
        TargetLockPayload last;
        Target(Identifier dimension, BlockPos pos) { this.dimension = dimension; this.pos = pos.immutable(); }
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(TargetLockRequestPayload.ID, TargetLockRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TargetLockPayload.ID, TargetLockPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TargetLockRequestPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            if (!ServerPlayNetworking.canSend(player, TargetLockPayload.ID)) return;
            if (!payload.active()) TARGETS.remove(player.getUUID());
            else TARGETS.put(player.getUUID(), new Target(player.level().dimension().identifier(), payload.pos()));
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (TARGETS.isEmpty() || server.getTickCount() % 5 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                Target target = TARGETS.get(player.getUUID());
                if (target == null || !ServerPlayNetworking.canSend(player, TargetLockPayload.ID)) continue;
                TargetLockPayload response = snapshot(player, target.dimension, target.pos);
                if (!response.equals(target.last)) {
                    ServerPlayNetworking.send(player, response);
                    target.last = response;
                }
                if (!response.known()) TARGETS.remove(player.getUUID());
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> TARGETS.remove(handler.player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> TARGETS.clear());
    }
    public static TargetLockPayload snapshot(ServerPlayer player, Identifier dimension, BlockPos pos) {
        var level = player.level();
        boolean known = dimension.equals(level.dimension().identifier()) && level.hasChunkAt(pos)
                && player.isWithinBlockInteractionRange(pos, 0.0) && !level.getBlockState(pos).isAir();
        var state = known ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState();
        boolean locked = known && BlockToolsConfig.get().blockStateLocking.enabled && BlockStateLocks.get(level).contains(pos);
        return new TargetLockPayload(dimension, pos, known, locked, Identifier.parse(state.typeHolder().getRegisteredName()));
    }
    private TargetLockSync() { }
}
