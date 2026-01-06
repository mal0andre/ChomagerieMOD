package tech.maloandre.chomagerie.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import tech.maloandre.chomagerie.Chomagerie;
import tech.maloandre.chomagerie.config.ServerConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Paquet pour synchroniser la configuration du client vers le serveur
 */
public record ConfigSyncPayload(
		boolean shulkerRefillEnabled,
		boolean showRefillMessages,
		boolean filterByName,
		String shulkerNameFilter,
		String refillMessage,
		List<String> allowedItems
) implements CustomPayload {

	public static final CustomPayload.Id<ConfigSyncPayload> ID =
			new CustomPayload.Id<>(Identifier.of(Chomagerie.MOD_ID, "config_sync"));

	public static final PacketCodec<RegistryByteBuf, ConfigSyncPayload> CODEC = PacketCodec.of(
			(value, buf) -> {
				buf.writeBoolean(value.shulkerRefillEnabled);
				buf.writeBoolean(value.showRefillMessages);
				buf.writeBoolean(value.filterByName);
				buf.writeString(value.shulkerNameFilter);
				buf.writeString(value.refillMessage);
				buf.writeInt(value.allowedItems.size());
				for (String item : value.allowedItems) {
					buf.writeString(item);
				}
			},
			(buf) -> {
				boolean shulkerRefillEnabled = buf.readBoolean();
				boolean showRefillMessages = buf.readBoolean();
				boolean filterByName = buf.readBoolean();
				String shulkerNameFilter = buf.readString();
				String refillMessage = buf.readString();
				int allowedItemsSize = buf.readInt();
				List<String> allowedItems = new ArrayList<>();
				for (int i = 0; i < allowedItemsSize; i++) {
					allowedItems.add(buf.readString());
				}
				return new ConfigSyncPayload(
						shulkerRefillEnabled,
						showRefillMessages,
						filterByName,
						shulkerNameFilter,
						refillMessage,
						allowedItems
				);
			}
	);

	/**
	 * Enregistre le handler côté serveur
	 */
	public static void registerServerHandler() {
		ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();

			// Mettre à jour la configuration du joueur côté serveur
			ServerConfig config = ServerConfig.getInstance();

			// Marquer que le joueur a le mod installé
			config.setPlayerHasMod(player.getUuid(), true);

			// Appliquer sa configuration
			config.setShulkerRefillEnabled(player.getUuid(), payload.shulkerRefillEnabled);
			config.setShowRefillMessages(player.getUuid(), payload.showRefillMessages);
			config.setFilterByName(player.getUuid(), payload.filterByName);
			config.setShulkerNameFilter(player.getUuid(), payload.shulkerNameFilter);
			config.setRefillMessage(player.getUuid(), payload.refillMessage);
			config.setAllowedItems(player.getUuid(), payload.allowedItems);

			Chomagerie.LOGGER.info("Configuration synchronisée pour le joueur {} - ShulkerRefill: {}, Filtre: {}, Items: {} (Mod installé)",
					player.getName().getString(), payload.shulkerRefillEnabled, payload.filterByName ? payload.shulkerNameFilter : "désactivé",
					payload.allowedItems.isEmpty() ? "tous" : payload.allowedItems.size() + " autorisés");
		});
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

