package tech.maloandre.chomagerie.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** An opt-in request for lock positions, never a request to modify locks. */
public record LockOverlaySubscriptionPayload(boolean enabled, int distance) implements CustomPacketPayload {
    public static final int MIN_DISTANCE = 8;
    public static final int MAX_DISTANCE = 128;
    public static final int DEFAULT_DISTANCE = 32;
    public static final Type<LockOverlaySubscriptionPayload> ID = new Type<>(Identifier.parse("chomagerie:lock_overlay_subscription"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LockOverlaySubscriptionPayload> CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeBoolean(value.enabled()); buf.writeVarInt(value.distance()); },
            buf -> new LockOverlaySubscriptionPayload(buf.readBoolean(), buf.readVarInt()));

    public LockOverlaySubscriptionPayload {
        distance = clampDistance(distance);
    }

    public static int clampDistance(int distance) {
        return Math.clamp(distance, MIN_DISTANCE, MAX_DISTANCE);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}
