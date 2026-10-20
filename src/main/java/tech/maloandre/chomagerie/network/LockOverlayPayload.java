package tech.maloandre.chomagerie.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Bounded batches of additions/removals. Reset also replaces the previous dimension's cache. */
public record LockOverlayPayload(Identifier dimension, boolean reset, boolean locked, long[] positions) implements CustomPacketPayload {
    public static final int MAX_POSITIONS = 4096;
    public static final Type<LockOverlayPayload> ID = new Type<>(Identifier.parse("chomagerie:lock_overlay"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LockOverlayPayload> CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeIdentifier(value.dimension());
                buf.writeBoolean(value.reset());
                buf.writeBoolean(value.locked());
                buf.writeLongArray(value.positions());
            }, buf -> {
                Identifier dimension = buf.readIdentifier();
                boolean reset = buf.readBoolean();
                boolean locked = buf.readBoolean();
                int size = buf.readVarInt();
                if (size < 0 || size > MAX_POSITIONS) throw new DecoderException("Invalid lock overlay batch size: " + size);
                long[] positions = new long[size];
                for (int i = 0; i < size; i++) positions[i] = buf.readLong();
                return new LockOverlayPayload(dimension, reset, locked, positions);
            });

    public LockOverlayPayload {
        if (positions.length > MAX_POSITIONS) throw new IllegalArgumentException("Lock overlay batch too large");
    }

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}
