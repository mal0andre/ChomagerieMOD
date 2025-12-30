package tech.maloandre.chomagerie.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import tech.maloandre.chomagerie.client.network.ClientNetworkHandler;
import tech.maloandre.chomagerie.config.ModState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ChomagerieConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("chomagerie.json");

    private static ChomagerieConfig instance;

    // Configuration ShulkerRefill
    public ShulkerRefillConfig shulkerRefill = new ShulkerRefillConfig();

    // Futures configurations (exemples)
    // public AutoCraftConfig autoCraft = new AutoCraftConfig();
    // public StorageManagerConfig storageManager = new StorageManagerConfig();

    private ChomagerieConfig() {}

    public static ChomagerieConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static ChomagerieConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                ChomagerieConfig config = GSON.fromJson(json, ChomagerieConfig.class);
                if (config != null) {
                    // Assurer que les sous-configs sont initialisées
                    if (config.shulkerRefill == null) {
                        config.shulkerRefill = new ShulkerRefillConfig();
                    }
                    // Synchroniser ModState avec la configuration chargée
                    ModState.setClientEnabled(config.shulkerRefill.enabled);
                    return config;
                }
            } catch (IOException e) {
                System.err.println("Erreur lors du chargement de la configuration de Chomagerie: " + e.getMessage());
            }
        }

        // Créer une nouvelle configuration par défaut
        ChomagerieConfig config = new ChomagerieConfig();
        config.save();
        // Synchroniser ModState avec la configuration par défaut
        ModState.setClientEnabled(config.shulkerRefill.enabled);
        return config;
    }

    public void save() {
        try {
            String json = GSON.toJson(this);
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, json);
            // Synchroniser ModState après la sauvegarde
            ModState.setClientEnabled(this.shulkerRefill.isEnabled());
            // Envoyer la configuration au serveur
            ClientNetworkHandler.sendConfigToServer();
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde de la configuration de Chomagerie: " + e.getMessage());
        }
    }

    /**
     * Recharge la configuration depuis le fichier
     */
    public void reload() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                ChomagerieConfig loaded = GSON.fromJson(json, ChomagerieConfig.class);
                if (loaded != null) {
                    // Mettre à jour l'instance actuelle avec les valeurs chargées
                    if (loaded.shulkerRefill != null) {
                        this.shulkerRefill = loaded.shulkerRefill;
                    }
                    // Synchroniser ModState avec la configuration rechargée
                    ModState.setClientEnabled(this.shulkerRefill.isEnabled());
                }
            } catch (IOException e) {
                System.err.println("Erreur lors du rechargement de la configuration de Chomagerie: " + e.getMessage());
            }
        }
    }

    // Classe interne pour la configuration ShulkerRefill
    public static class ShulkerRefillConfig {
        public boolean enabled = true;
        public boolean showRefillMessages = true;
        public boolean playSounds = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean shouldShowRefillMessages() {
            return showRefillMessages;
        }

        public void setShowRefillMessages(boolean show) {
            this.showRefillMessages = show;
        }

        public boolean shouldPlaySounds() {
            return playSounds;
        }

        public void setPlaySounds(boolean play) {
            this.playSounds = play;
        }
    }
}

