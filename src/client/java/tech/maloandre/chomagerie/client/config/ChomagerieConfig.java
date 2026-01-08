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

    // ShulkerRefill configuration
    public ShulkerRefillConfig shulkerRefill = new ShulkerRefillConfig();

    // UI configuration
    public UIConfig ui = new UIConfig();

    // Notifications configuration
    public NotificationsConfig notifications = new NotificationsConfig();

    // Future configurations (examples)
    // public AutoCraftConfig autoCraft = new AutoCraftConfig();
    // public StorageManagerConfig storageManager = new StorageManagerConfig();

    private ChomagerieConfig() {
    }

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
                    if (config.ui == null) {
                        config.ui = new UIConfig();
                    }
                    if (config.notifications == null) {
                        config.notifications = new NotificationsConfig();
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
                    if (loaded.ui != null) {
                        this.ui = loaded.ui;
                    }
                    if (loaded.notifications != null) {
                        this.notifications = loaded.notifications;
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
        public boolean filterByName = false;
        public String shulkerNameFilter = "restock same";

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

        public boolean isFilterByNameEnabled() {
            return filterByName;
        }

        public void setFilterByName(boolean filter) {
            this.filterByName = filter;
        }

        public String getShulkerNameFilter() {
            return shulkerNameFilter;
        }

        public void setShulkerNameFilter(String name) {
            this.shulkerNameFilter = name;
        }
    }

    // Classe interne pour la configuration UI
    public static class UIConfig {
        public boolean showHUD = true;
        public float hudOpacity = 1.0f;
        public String hudPosition = "top-right";
        public boolean compactMode = false;

        public boolean isShowHUD() {
            return showHUD;
        }

        public void setShowHUD(boolean show) {
            this.showHUD = show;
        }

        public float getHudOpacity() {
            return hudOpacity;
        }

        public void setHudOpacity(float opacity) {
            this.hudOpacity = opacity;
        }

        public String getHudPosition() {
            return hudPosition;
        }

        public void setHudPosition(String position) {
            this.hudPosition = position;
        }

        public boolean isCompactMode() {
            return compactMode;
        }

        public void setCompactMode(boolean compact) {
            this.compactMode = compact;
        }
    }

    // Classe interne pour la configuration Notifications
    public static class NotificationsConfig {
        public boolean enableNotifications = true;
        public boolean notifyOnSuccess = true;
        public boolean notifyOnError = true;
        public boolean useActionBar = true;
        public int notificationDuration = 3;

        public boolean isNotificationsEnabled() {
            return enableNotifications;
        }

        public void setNotificationsEnabled(boolean enabled) {
            this.enableNotifications = enabled;
        }

        public boolean shouldNotifyOnSuccess() {
            return notifyOnSuccess;
        }

        public void setNotifyOnSuccess(boolean notify) {
            this.notifyOnSuccess = notify;
        }

        public boolean shouldNotifyOnError() {
            return notifyOnError;
        }

        public void setNotifyOnError(boolean notify) {
            this.notifyOnError = notify;
        }

        public boolean shouldUseActionBar() {
            return useActionBar;
        }

        public void setUseActionBar(boolean use) {
            this.useActionBar = use;
        }

        public int getNotificationDuration() {
            return notificationDuration;
        }

        public void setNotificationDuration(int duration) {
            this.notificationDuration = duration;
        }
    }
}

