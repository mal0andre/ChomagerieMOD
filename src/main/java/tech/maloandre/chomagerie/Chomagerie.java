package tech.maloandre.chomagerie;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.maloandre.chomagerie.config.ServerConfig;
import tech.maloandre.chomagerie.event.ItemStackDepletedCallback;
import tech.maloandre.chomagerie.network.ConfigSyncPayload;
import tech.maloandre.chomagerie.util.ShulkerRefillHandler;

public class Chomagerie implements ModInitializer {

	public static final String MOD_ID = "chomagerie";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initialisation de Chomagerie - Système de refill automatique activé");

		// Initialiser la configuration serveur
		ServerConfig.getInstance();

		// Enregistrer le type de paquet réseau
		PayloadTypeRegistry.playC2S().register(ConfigSyncPayload.ID, ConfigSyncPayload.CODEC);

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
					ShulkerRefillHandler.tryRefillFromShulker(player, slot, item);
				} else if (!ServerConfig.getInstance().playerHasMod(player.getUuid())) {
					// Le joueur n'a pas le mod, on ne fait rien (silencieux)
					LOGGER.debug("Refill ignoré pour {} - Mod non installé", player.getName().getString());
				}
			}
		});
	}
}
