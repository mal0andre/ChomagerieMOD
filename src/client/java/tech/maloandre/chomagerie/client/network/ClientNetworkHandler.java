package tech.maloandre.chomagerie.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import tech.maloandre.chomagerie.client.config.ChomagerieConfig;
import tech.maloandre.chomagerie.network.ConfigSyncPayload;
import tech.maloandre.chomagerie.network.RefillNotificationPayload;

/**
 * Gestionnaire réseau côté client
 */
public class ClientNetworkHandler {

    /**
     * Envoie la configuration client au serveur
     */
    public static void sendConfigToServer() {
        if (!ClientPlayNetworking.canSend(ConfigSyncPayload.ID)) {
            return;
        }

        ChomagerieConfig config = ChomagerieConfig.getInstance();
        ConfigSyncPayload payload = new ConfigSyncPayload(
            config.shulkerRefill.isEnabled(),
            config.shulkerRefill.shouldShowRefillMessages()
        );

        ClientPlayNetworking.send(payload);
    }

    /**
     * Initialise les handlers réseau côté client
     */
    public static void init() {
        // Handler pour les notifications de refill
        ClientPlayNetworking.registerGlobalReceiver(RefillNotificationPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                ChomagerieConfig config = ChomagerieConfig.getInstance();
                MinecraftClient client = context.client();

                // Afficher le message si activé
                if (config.shulkerRefill.shouldShowRefillMessages() && client.player != null) {
                    client.player.sendMessage(
                        Text.literal("§7[§6Refill§7] §a" + payload.itemName() + " rechargé depuis une shulker box"),
                        true // Afficher dans l'actionbar
                    );
                }

                // Jouer le son si activé
                if (config.shulkerRefill.shouldPlaySounds() && client.player != null && client.world != null) {
                    client.world.playSound(
                        client.player,
                        client.player.getBlockPos(),
                        SoundEvents.ENTITY_ITEM_PICKUP,
                        SoundCategory.PLAYERS,
                        0.5f, // Volume
                        1.2f  // Pitch
                    );
                }
            });
        });
    }
}

