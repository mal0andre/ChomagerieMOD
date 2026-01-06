package tech.maloandre.chomagerie.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import tech.maloandre.chomagerie.client.config.ChomagerieConfig;
import tech.maloandre.chomagerie.config.ModState;

import java.util.ArrayList;
import java.util.List;

public class ChomagerieCommand {

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(ClientCommandManager.literal("chomagerie")
						.then(ClientCommandManager.literal("shulkerrefill")
								.then(ClientCommandManager.literal("toggle")
										.executes(ChomagerieCommand::toggleShulkerRefill))
								.then(ClientCommandManager.literal("enable")
										.executes(context -> setShulkerRefillEnabled(context, true)))
								.then(ClientCommandManager.literal("disable")
										.executes(context -> setShulkerRefillEnabled(context, false)))
								.then(ClientCommandManager.literal("status")
										.executes(ChomagerieCommand::showShulkerRefillStatus))
								.then(ClientCommandManager.literal("message")
										.then(ClientCommandManager.argument("message", StringArgumentType.greedyString())
												.executes(ChomagerieCommand::setRefillMessage)))
								.then(ClientCommandManager.literal("items")
										.then(ClientCommandManager.literal("add")
												.then(ClientCommandManager.argument("itemId", StringArgumentType.string())
														.executes(ChomagerieCommand::addItem)))
										.then(ClientCommandManager.literal("remove")
												.then(ClientCommandManager.argument("itemId", StringArgumentType.string())
														.executes(ChomagerieCommand::removeItem)))
										.then(ClientCommandManager.literal("list")
												.executes(ChomagerieCommand::listItems))
										.then(ClientCommandManager.literal("clear")
												.executes(ChomagerieCommand::clearItems))
								)
						)
				// Futures commandes pour d'autres features
				// .then(ClientCommandManager.literal("autocraft")...)
		);
	}

	private static int toggleShulkerRefill(CommandContext<FabricClientCommandSource> context) {
		ChomagerieConfig config = ChomagerieConfig.getInstance();
		boolean newState = !config.shulkerRefill.isEnabled();
		config.shulkerRefill.setEnabled(newState);
		ModState.setClientEnabled(newState);
		config.save();

		if (newState) {
			context.getSource().sendFeedback(Text.literal("§a[Chomagerie] ShulkerRefill activé"));
		} else {
			context.getSource().sendFeedback(Text.literal("§c[Chomagerie] ShulkerRefill désactivé"));
		}

		return 1;
	}

	private static int setShulkerRefillEnabled(CommandContext<FabricClientCommandSource> context, boolean enabled) {
		ChomagerieConfig config = ChomagerieConfig.getInstance();
		config.shulkerRefill.setEnabled(enabled);
		ModState.setClientEnabled(enabled);
		config.save();

		if (enabled) {
			context.getSource().sendFeedback(Text.literal("§a[Chomagerie] ShulkerRefill activé"));
		} else {
			context.getSource().sendFeedback(Text.literal("§c[Chomagerie] ShulkerRefill désactivé"));
		}

		return 1;
	}

	private static int showShulkerRefillStatus(CommandContext<FabricClientCommandSource> context) {
		ChomagerieConfig config = ChomagerieConfig.getInstance();
		String status = config.shulkerRefill.isEnabled() ? "§aactivé" : "§cdésactivé";
		context.getSource().sendFeedback(Text.literal("§e[Chomagerie] ShulkerRefill est actuellement " + status));
		return 1;
	}

	private static int setRefillMessage(CommandContext<FabricClientCommandSource> context) {
		ChomagerieConfig config = ChomagerieConfig.getInstance();
		String message = StringArgumentType.getString(context, "message");
		config.shulkerRefill.setRefillMessage(message);
		config.save();

		context.getSource().sendFeedback(Text.literal("§a[Chomagerie] Message de recharge mis à jour: " + message));
		return 1;
	}

	private static int addItem(CommandContext<FabricClientCommandSource> context) {
		ChomagerieConfig config = ChomagerieConfig.getInstance();
		String itemId = StringArgumentType.getString(context, "itemId");

		List<String> allowedItems = new ArrayList<>(config.shulkerRefill.getAllowedItems());
		if (!allowedItems.contains(itemId)) {
			allowedItems.add(itemId);
			config.shulkerRefill.setAllowedItems(allowedItems);
			config.save();
			context.getSource().sendFeedback(Text.literal("§a[Chomagerie] Item ajouté: " + itemId));
		} else {
			context.getSource().sendFeedback(Text.literal("§c[Chomagerie] Item déjà présent: " + itemId));
		}
		return 1;
	}

	private static int removeItem(CommandContext<FabricClientCommandSource> context) {
		ChomagerieConfig config = ChomagerieConfig.getInstance();
		String itemId = StringArgumentType.getString(context, "itemId");

		List<String> allowedItems = new ArrayList<>(config.shulkerRefill.getAllowedItems());
		if (allowedItems.remove(itemId)) {
			config.shulkerRefill.setAllowedItems(allowedItems);
			config.save();
			context.getSource().sendFeedback(Text.literal("§a[Chomagerie] Item retiré: " + itemId));
		} else {
			context.getSource().sendFeedback(Text.literal("§c[Chomagerie] Item non trouvé: " + itemId));
		}
		return 1;
	}

	private static int listItems(CommandContext<FabricClientCommandSource> context) {
		ChomagerieConfig config = ChomagerieConfig.getInstance();
		List<String> allowedItems = config.shulkerRefill.getAllowedItems();

		if (allowedItems.isEmpty()) {
			context.getSource().sendFeedback(Text.literal("§e[Chomagerie] Liste vide - Tous les items peuvent être rechargés"));
		} else {
			context.getSource().sendFeedback(Text.literal("§e[Chomagerie] Items autorisés pour le refill:"));
			for (String itemId : allowedItems) {
				context.getSource().sendFeedback(Text.literal("  §7- " + itemId));
			}
		}
		return 1;
	}

	private static int clearItems(CommandContext<FabricClientCommandSource> context) {
		ChomagerieConfig config = ChomagerieConfig.getInstance();
		config.shulkerRefill.setAllowedItems(new ArrayList<>());
		config.save();
		context.getSource().sendFeedback(Text.literal("§a[Chomagerie] Liste vidée - Tous les items peuvent maintenant être rechargés"));
		return 1;
	}
}

