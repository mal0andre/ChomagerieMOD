package tech.maloandre.chomagerie;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.maloandre.chomagerie.config.ServerConfig;
import tech.maloandre.chomagerie.event.ItemStackDepletedCallback;
import tech.maloandre.chomagerie.network.ConfigSyncPayload;
import tech.maloandre.chomagerie.network.RefillNotificationPayload;
import tech.maloandre.chomagerie.util.ShulkerRefillHandler;

public class Chomagerie implements ModInitializer {

	public static final String MOD_ID = "chomagerie";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initialisation de Chomagerie - Système de refill automatique activé");

		// Initialiser la configuration serveur
		ServerConfig.getInstance();

		// Enregistrer les types de paquets réseau
		PayloadTypeRegistry.playC2S().register(ConfigSyncPayload.ID, ConfigSyncPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(RefillNotificationPayload.ID, RefillNotificationPayload.CODEC);

		// Enregistrer le handler réseau côté serveur
		ConfigSyncPayload.registerServerHandler();

		// Détecter les joueurs qui se connectent
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			// Ici, on ne fait rien. Si le joueur a le mod, il enverra sa config automatiquement
			// Si après quelques secondes il n'a pas envoyé de config, on suppose qu'il n'a pas le mod
			LOGGER.debug("Joueur {} connecté, attente de la configuration...", handler.player.getName().getString());
		});

		// Enregistrer l'événement de refill automatique depuis les shulker boxes
		ItemStackDepletedCallback.EVENT.register((player, slot, item, previousStack) -> {
			if (!player.getEntityWorld().isClient()) {
				// Vérifier si le mod est activé pour ce joueur côté serveur
				// Cette méthode vérifie maintenant aussi si le joueur a le mod installé
				boolean isEnabled = ServerConfig.getInstance().isShulkerRefillEnabled(player.getUuid());

				if (isEnabled) {
					// Récupérer les paramètres de filtrage du joueur
					ServerConfig config = ServerConfig.getInstance();
					boolean filterByName = config.isFilterByNameEnabled(player.getUuid());
					String nameFilter = config.getShulkerNameFilter(player.getUuid());

					ShulkerRefillHandler.RefillResult result = ShulkerRefillHandler.tryRefillFromShulker(
						player, slot, item, filterByName, nameFilter
					);

					// Si le refill a réussi, envoyer une notification au client
					if (result.success && player instanceof ServerPlayerEntity serverPlayer) {
						ServerPlayNetworking.send(serverPlayer, new RefillNotificationPayload(result.itemName));
					}
				} else if (!ServerConfig.getInstance().playerHasMod(player.getUuid())) {
					// Le joueur n'a pas le mod, on ne fait rien (silencieux)
					LOGGER.debug("Refill ignoré pour {} - Mod non installé", player.getName().getString());
				}
			}
		});
	}
}
