package tech.maloandre.chomagerie.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import tech.maloandre.chomagerie.Chomagerie;
import tech.maloandre.chomagerie.blockstate.BlockToolsConfig;

import java.nio.file.NoSuchFileException;
import java.util.HashSet;
import java.util.Set;

/** Server-thread-only reload. A rejected candidate never replaces the running policy. */
public final class ChomagerieReloadCommand {
    private static final Set<MinecraftServer> RELOADING = new HashSet<>();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("chomagerie")
                        .then(Commands.literal("reload")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(context -> reload(context.getSource())))));
    }

    private static int reload(CommandSourceStack source) {
        var server = source.getServer();
        if (RELOADING.contains(server)) {
            source.sendFailure(Component.literal("[Chomagerie] Un rechargement est déjà en cours."));
            return 0;
        }
        final BlockToolsConfig candidate;
        try {
            candidate = BlockToolsConfig.read(BlockToolsConfig.path());
        } catch (Exception e) {
            String reason = e instanceof NoSuchFileException ? "fichier introuvable" : e.getMessage();
            if (reason == null) reason = e.getClass().getSimpleName();
            // Gson appends a documentation URL; the line, column and JSON path are sufficient here.
            reason = reason.split("\\RSee ", 2)[0];
            source.sendFailure(Component.literal("[Chomagerie] Impossible de recharger config/chomagerie/block-tools.json : "
                    + reason + ". Les réglages actifs sont conservés."));
            Chomagerie.LOGGER.warn("Rejected block-tools reload from {}: {}", source.getTextName(), reason);
            return 0;
        }
        var previous = BlockToolsConfig.get();
        BlockToolsConfig.apply(candidate);
        if (previous.debugStick.craftingEnabled == candidate.debugStick.craftingEnabled) {
            success(source);
            return 1;
        }
        // The recipe is filtered when Minecraft builds its recipe map: use the vanilla reload path
        // when this flag changes so recipe books, open containers and unmodified clients stay in sync.
        RELOADING.add(server);
        source.sendSuccess(() -> Component.literal("[Chomagerie] Configuration valide ; rechargement des données Minecraft pour appliquer le craft du Debug Stick…"), false);
        try {
            server.reloadResources(server.getPackRepository().getSelectedIds()).whenCompleteAsync((ignored, error) -> {
                RELOADING.remove(server);
                if (error == null) success(source);
                else failedRecipes(source, previous, error);
            }, server);
        } catch (Exception e) {
            RELOADING.remove(server);
            failedRecipes(source, previous, e);
            return 0;
        }
        return 1;
    }

    private static void success(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("[Chomagerie] config/chomagerie/block-tools.json rechargé. Réglages appliqués."), true);
    }

    private static void failedRecipes(CommandSourceStack source, BlockToolsConfig previous, Throwable error) {
        BlockToolsConfig.apply(previous);
        source.sendFailure(Component.literal("[Chomagerie] Échec du rechargement des données Minecraft. Les réglages précédents ont été rétablis ; consulte la console du serveur."));
        Chomagerie.LOGGER.error("Could not reload recipes after block-tools configuration change", error);
    }

    private ChomagerieReloadCommand() { }
}
