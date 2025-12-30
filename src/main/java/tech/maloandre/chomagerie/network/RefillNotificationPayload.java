package tech.maloandre.chomagerie.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import tech.maloandre.chomagerie.Chomagerie;

public record RefillNotificationPayload(String itemName) implements CustomPayload {
    public static final Id<RefillNotificationPayload> ID = new Id<>(Identifier.of(Chomagerie.MOD_ID, "refill_notification"));

    public static final PacketCodec<PacketByteBuf, RefillNotificationPayload> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeString(value.itemName),
            buf -> new RefillNotificationPayload(buf.readString())
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

