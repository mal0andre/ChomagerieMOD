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
            // Recharger la configuration pour s'assurer qu'elle est à jour
            ChomagerieConfig config = ChomagerieConfig.getInstance();
            config.reload();

            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("Configuration Chomagerie"));

            // Catégorie ShulkerRefill
            ConfigCategory shulkerRefillCategory = builder.getOrCreateCategory(Text.literal("ShulkerRefill"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            // Option pour afficher les messages de refill
            shulkerRefillCategory.addEntry(entryBuilder.startBooleanToggle(
                    Text.literal("Afficher les messages"),
                    config.shulkerRefill.shouldShowRefillMessages()
                )
                .setDefaultValue(true)
                .setTooltip(Text.literal("Affiche un message quand un item est rechargé depuis une shulker box"))
                .setSaveConsumer(newValue -> {
                    config.shulkerRefill.setShowRefillMessages(newValue);
                })
                .build());

            // Option pour activer/désactiver ShulkerRefill
            shulkerRefillCategory.addEntry(entryBuilder.startBooleanToggle(
                    Text.literal("Activer ShulkerRefill"),
                    config.shulkerRefill.isEnabled()
                )
                .setDefaultValue(true)
                .setTooltip(Text.literal("Active ou désactive le système de refill automatique depuis les shulker boxes"))
                .setSaveConsumer(newValue -> {
                    config.shulkerRefill.setEnabled(newValue);
                    ModState.setClientEnabled(newValue);
                })
                .build());


            // Option pour jouer des sons lors du refill
            shulkerRefillCategory.addEntry(entryBuilder.startBooleanToggle(
                    Text.literal("Jouer des sons"),
                    config.shulkerRefill.shouldPlaySounds()
                )
                .setDefaultValue(true)
                .setTooltip(Text.literal("Joue un son quand un item est rechargé depuis une shulker box"))
                .setSaveConsumer(newValue -> {
                    config.shulkerRefill.setPlaySounds(newValue);
                })
                .build());

            // Option pour filtrer par nom de shulker box
            shulkerRefillCategory.addEntry(entryBuilder.startBooleanToggle(
                    Text.literal("Filtrer par nom de shulker"),
                    config.shulkerRefill.isFilterByNameEnabled()
                )
                .setDefaultValue(false)
                .setTooltip(Text.literal("N'utilise que les shulker boxes avec un nom spécifique pour le refill"))
                .setSaveConsumer(newValue -> {
                    config.shulkerRefill.setFilterByName(newValue);
                })
                .build());

            // Option pour définir le nom des shulker boxes à utiliser
            shulkerRefillCategory.addEntry(entryBuilder.startStrField(
                    Text.literal("Nom des shulker boxes"),
                    config.shulkerRefill.getShulkerNameFilter()
                )
                .setDefaultValue("restock same")
                .setTooltip(Text.literal("Seules les shulker boxes avec ce nom exact seront utilisées pour le refill"))
                .setSaveConsumer(newValue -> {
                    config.shulkerRefill.setShulkerNameFilter(newValue);
                })
                .build());

            // Futures catégories (exemples commentés)
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

