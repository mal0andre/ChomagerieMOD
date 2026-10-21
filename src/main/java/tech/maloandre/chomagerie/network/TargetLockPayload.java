package tech.maloandre.chomagerie.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TargetLockPayload(Identifier dimension, BlockPos pos, boolean known, boolean locked, Identifier block) implements CustomPacketPayload {
    public static final Type<TargetLockPayload> ID = new Type<>(Identifier.parse("chomagerie:target_lock"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TargetLockPayload> CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeIdentifier(value.dimension()); buf.writeBlockPos(value.pos()); buf.writeBoolean(value.known()); buf.writeBoolean(value.locked()); buf.writeIdentifier(value.block()); },
            buf -> new TargetLockPayload(buf.readIdentifier(), buf.readBlockPos(), buf.readBoolean(), buf.readBoolean(), buf.readIdentifier()));
    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}
