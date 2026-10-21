package tech.maloandre.chomagerie.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TargetLockRequestPayload(boolean active, BlockPos pos) implements CustomPacketPayload {
    public static final Type<TargetLockRequestPayload> ID = new Type<>(Identifier.parse("chomagerie:target_lock_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TargetLockRequestPayload> CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeBoolean(value.active()); buf.writeBlockPos(value.pos()); },
            buf -> new TargetLockRequestPayload(buf.readBoolean(), buf.readBlockPos()));
    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}
