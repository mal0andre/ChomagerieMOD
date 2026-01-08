package tech.maloandre.chomagerie.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;
import tech.maloandre.chomagerie.config.ModState;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            // Reload configuration to ensure it's up to date
            ChomagerieConfig config = ChomagerieConfig.getInstance();
            config.reload();

            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("Chomagerie Configuration"));

            // ShulkerRefill category
            ConfigCategory shulkerRefillCategory = builder.getOrCreateCategory(Text.literal("ShulkerRefill"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            // Option to show refill messages
            shulkerRefillCategory.addEntry(entryBuilder.startBooleanToggle(
                            Text.literal("Show Messages"),
                            config.shulkerRefill.shouldShowRefillMessages()
                    )
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Displays a message when an item is refilled from a shulker box"))
                    .setSaveConsumer(newValue -> {
                        config.shulkerRefill.setShowRefillMessages(newValue);
                    })
                    .build());

            // Option to enable/disable ShulkerRefill
            shulkerRefillCategory.addEntry(entryBuilder.startBooleanToggle(
                            Text.literal("Enable ShulkerRefill"),
                            config.shulkerRefill.isEnabled()
                    )
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Enables or disables the automatic refill system from shulker boxes"))
                    .setSaveConsumer(newValue -> {
                        config.shulkerRefill.setEnabled(newValue);
                        ModState.setClientEnabled(newValue);
                    })
                    .build());


            // Option to play sounds during refill
            shulkerRefillCategory.addEntry(entryBuilder.startBooleanToggle(
                            Text.literal("Play Sounds"),
                            config.shulkerRefill.shouldPlaySounds()
                    )
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Plays a sound when an item is refilled from a shulker box"))
                    .setSaveConsumer(newValue -> {
                        config.shulkerRefill.setPlaySounds(newValue);
                    })
                    .build());

            // Option to filter by shulker box name
            shulkerRefillCategory.addEntry(entryBuilder.startBooleanToggle(
                            Text.literal("Filter by Shulker Name"),
                            config.shulkerRefill.isFilterByNameEnabled()
                    )
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("Only uses shulker boxes with a specific name for refill"))
                    .setSaveConsumer(newValue -> {
                        config.shulkerRefill.setFilterByName(newValue);
                    })
                    .build());

            // Option to set the name of shulker boxes to use
            shulkerRefillCategory.addEntry(entryBuilder.startStrField(
                            Text.literal("Shulker Box Name"),
                            config.shulkerRefill.getShulkerNameFilter()
                    )
                    .setDefaultValue("restock same")
                    .setTooltip(Text.literal("Only shulker boxes with this exact name will be used for refill"))
                    .setSaveConsumer(newValue -> {
                        config.shulkerRefill.setShulkerNameFilter(newValue);
                    })
                    .build());

            // ============== UI Category ==============
            ConfigCategory uiCategory = builder.getOrCreateCategory(Text.literal("Interface Utilisateur"));

            // Option to show HUD
            uiCategory.addEntry(entryBuilder.startBooleanToggle(
                            Text.literal("Afficher l'interface HUD"),
                            config.ui.isShowHUD()
                    )
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Affiche l'interface utilisateur en jeu"))
                    .setSaveConsumer(newValue -> {
                        config.ui.setShowHUD(newValue);
                    })
                    .build());

            // Option for HUD opacity
            uiCategory.addEntry(entryBuilder.startDoubleField(
                            Text.literal("Opacité de l'interface"),
                            (double) config.ui.getHudOpacity()
                    )
                    .setDefaultValue(1.0)
                    .setTooltip(Text.literal("Ajuste la transparence de l'interface HUD (0.0 - 1.0)"))
                    .setSaveConsumer(newValue -> {
                        config.ui.setHudOpacity(newValue.floatValue());
                    })
                    .build());

            // Option for HUD position
            uiCategory.addEntry(entryBuilder.startStrField(
                            Text.literal("Position de l'interface"),
                            config.ui.getHudPosition()
                    )
                    .setDefaultValue("top-right")
                    .setTooltip(Text.literal("Position de l'interface: top-left, top-right, bottom-left, bottom-right"))
                    .setSaveConsumer(newValue -> {
                        config.ui.setHudPosition(newValue);
                    })
                    .build());

            // Option for compact mode
            uiCategory.addEntry(entryBuilder.startBooleanToggle(
                            Text.literal("Mode compact"),
                            config.ui.isCompactMode()
                    )
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("Réduit la taille de l'interface pour un affichage plus discret"))
                    .setSaveConsumer(newValue -> {
                        config.ui.setCompactMode(newValue);
                    })
                    .build());

            // ============== Notifications Category ==============
            ConfigCategory notificationsCategory = builder.getOrCreateCategory(Text.literal("Notifications"));

            // Option to enable notifications
            notificationsCategory.addEntry(entryBuilder.startBooleanToggle(
                            Text.literal("Activer les notifications"),
                            config.notifications.isNotificationsEnabled()
                    )
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Active ou désactive toutes les notifications"))
                    .setSaveConsumer(newValue -> {
                        config.notifications.setNotificationsEnabled(newValue);
                    })
                    .build());

            // Option to notify on success
            notificationsCategory.addEntry(entryBuilder.startBooleanToggle(
                            Text.literal("Notifier en cas de succès"),
                            config.notifications.shouldNotifyOnSuccess()
                    )
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Affiche une notification quand une action réussit"))
                    .setSaveConsumer(newValue -> {
                        config.notifications.setNotifyOnSuccess(newValue);
                    })
                    .build());

            // Option to notify on error
            notificationsCategory.addEntry(entryBuilder.startBooleanToggle(
                            Text.literal("Notifier en cas d'erreur"),
                            config.notifications.shouldNotifyOnError()
                    )
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Affiche une notification quand une erreur se produit"))
                    .setSaveConsumer(newValue -> {
                        config.notifications.setNotifyOnError(newValue);
                    })
                    .build());

            // Option to use action bar
            notificationsCategory.addEntry(entryBuilder.startBooleanToggle(
                            Text.literal("Utiliser la barre d'action"),
                            config.notifications.shouldUseActionBar()
                    )
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Affiche les notifications dans la barre d'action au lieu du chat"))
                    .setSaveConsumer(newValue -> {
                        config.notifications.setUseActionBar(newValue);
                    })
                    .build());

            // Option for notification duration
            notificationsCategory.addEntry(entryBuilder.startIntField(
                            Text.literal("Durée des notifications (secondes)"),
                            config.notifications.getNotificationDuration()
                    )
                    .setDefaultValue(3)
                    .setTooltip(Text.literal("Durée d'affichage des notifications en secondes"))
                    .setSaveConsumer(newValue -> {
                        config.notifications.setNotificationDuration(newValue);
                    })
                    .build());

            // ============== About Category ==============
            ConfigCategory aboutCategory = builder.getOrCreateCategory(Text.literal("À propos"));

            // Placeholder entries for about information
            aboutCategory.addEntry(entryBuilder.startStrField(
                            Text.literal("Mod Chomagerie"),
                            "v1.4"
                    )
                    .setTooltip(Text.literal("Version du mod Chomagerie"))
                    .build());

            aboutCategory.addEntry(entryBuilder.startStrField(
                            Text.literal("Auteur"),
                            "MaloAndre"
                    )
                    .setTooltip(Text.literal("Créateur du mod"))
                    .build());

            aboutCategory.addEntry(entryBuilder.startStrField(
                            Text.literal("Description"),
                            "Gestion automatique de l'inventaire"
                    )
                    .setTooltip(Text.literal("Le mod Chomagerie améliore la gestion de votre inventaire"))
                    .build());

            // Future categories (commented examples)
            // ConfigCategory autoCraftCategory = builder.getOrCreateCategory(Text.literal("AutoCraft"));
            // ConfigCategory storageCategory = builder.getOrCreateCategory(Text.literal("Storage Manager"));

            builder.setSavingRunnable(() -> {
                config.save();
                ModState.setClientEnabled(config.shulkerRefill.enabled);
            });

            return builder.build();
        };
    }
}

