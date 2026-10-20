package tech.maloandre.chomagerie.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.maloandre.chomagerie.Chomagerie;
import tech.maloandre.chomagerie.client.command.ChomagerieCommand;
import tech.maloandre.chomagerie.client.config.ChomagerieConfig;
import tech.maloandre.chomagerie.client.config.ChomagerieConfigScreen;
import tech.maloandre.chomagerie.client.network.ClientNetworkHandler;
import tech.maloandre.chomagerie.client.overlay.LockOverlay;
import tech.maloandre.chomagerie.config.ModState;

public class ChomagerieClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("chomagerie-client");
    private static final KeyMapping.Category CHOMAGERIE_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(Chomagerie.MOD_ID, "chomagerie")
    );
    private static KeyMapping openConfigKey;

    @Override
    public void onInitializeClient() {
        // Load configuration
        ChomagerieConfig config = ChomagerieConfig.getInstance();

        // Update global state with ShulkerRefill config
        ModState.setClientEnabled(config.shulkerRefill.isEnabled());

        if (config.shulkerRefill.isEnabled()) {
            LOGGER.info("Chomagerie - ShulkerRefill is enabled on client");
        } else {
            LOGGER.info("Chomagerie - ShulkerRefill is disabled on client");
        }

        // Initialize network handler
        ClientNetworkHandler.init();
        LockOverlay.register();
        registerKeyBindings();

        // Synchronize config to server on connection
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (ClientNetworkHandler.syncVersionWithServer()) {
                ClientNetworkHandler.sendConfigToServer();
                LOGGER.info("Configuration sent to server");
            }
        });

        // Register client commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            ChomagerieCommand.register(dispatcher);
        });
    }

    private static void registerKeyBindings() {
        openConfigKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.chomagerie.open_config",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_K,
                CHOMAGERIE_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.consumeClick()) {
                client.setScreenAndShow(new ChomagerieConfigScreen(null));
            }
        });
    }
}
