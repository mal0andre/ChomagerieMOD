package tech.maloandre.chomagerie;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.maloandre.chomagerie.event.ItemStackDepletedCallback;
import tech.maloandre.chomagerie.util.ShulkerRefillHandler;

public class Chomagerie implements ModInitializer {

	public static final String MOD_ID = "chomagerie";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initialisation de Chomagerie - Système de refill automatique activé");

		// Enregistrer l'événement de refill automatique depuis les shulker boxes
		ItemStackDepletedCallback.EVENT.register((player, slot, item, previousStack) -> {
			if (!player.getEntityWorld().isClient()) {
				ShulkerRefillHandler.tryRefillFromShulker(player, slot, item);
			}
		});
	}
}
