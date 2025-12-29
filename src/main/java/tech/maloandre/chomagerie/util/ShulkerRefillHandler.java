package tech.maloandre.chomagerie.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.block.ShulkerBoxBlock;

import java.util.ArrayList;
import java.util.List;

public class ShulkerRefillHandler {

    /**
     * Vérifie si un item est une shulker box
     */
    public static boolean isShulkerBox(Item item) {
        return item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    /**
     * Vérifie si le joueur a déjà l'item dans son inventaire principal (hors shulker boxes)
     * @param inventory L'inventaire du joueur
     * @param itemToCheck L'item à rechercher
     * @return true si l'item est présent dans l'inventaire principal
     */
    private static boolean hasItemInMainInventory(Inventory inventory, Item itemToCheck) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);

            // Ignorer les shulker boxes
            if (isShulkerBox(stack.getItem())) {
                continue;
            }

            // Vérifier si on a trouvé l'item
            if (!stack.isEmpty() && stack.getItem() == itemToCheck) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tente de recharger un slot vide depuis les shulker boxes dans l'inventaire
     * @param player Le joueur
     * @param emptySlot Le slot qui vient de se vider
     * @param itemToRefill L'item à recharger
     */
    public static void tryRefillFromShulker(PlayerEntity player, int emptySlot, Item itemToRefill) {
        if (player.getEntityWorld().isClient()) {
            return; // Ne fonctionne que côté serveur
        }

        Inventory inventory = player.getInventory();

        // Vérifier si le joueur a déjà l'item dans son inventaire principal
        if (hasItemInMainInventory(inventory, itemToRefill)) {
            return; // Ne pas refill si l'item est déjà présent ailleurs dans l'inventaire
        }

        // Parcourir l'inventaire à la recherche de shulker boxes
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);

            if (isShulkerBox(stack.getItem())) {
                // Vérifier le contenu de la shulker box
                ContainerComponent container = stack.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);

                // Convertir le stream en liste
                List<ItemStack> contents = new ArrayList<>();
                container.stream().forEach(contents::add);

                if (contents.isEmpty()) {
                    continue;
                }

                // Chercher l'item dans la shulker box
                for (int j = 0; j < contents.size(); j++) {
                    ItemStack shulkerItem = contents.get(j);

                    if (!shulkerItem.isEmpty() && shulkerItem.getItem() == itemToRefill) {
                        // On a trouvé l'item ! Le transférer vers le slot vide
                        int amountToTake = Math.min(shulkerItem.getCount(), itemToRefill.getMaxCount());

                        ItemStack refillStack = shulkerItem.copy();
                        refillStack.setCount(amountToTake);

                        // Mettre l'item dans le slot du joueur
                        inventory.setStack(emptySlot, refillStack);

                        // Retirer l'item de la shulker box
                        shulkerItem.decrement(amountToTake);

                        // Créer un nouveau conteneur avec le contenu modifié
                        List<ItemStack> newContents = new ArrayList<>();
                        for (int k = 0; k < contents.size(); k++) {
                            ItemStack itemInSlot = contents.get(k);
                            if (k == j) {
                                if (!shulkerItem.isEmpty()) {
                                    newContents.add(shulkerItem);
                                }
                            } else {
                                newContents.add(itemInSlot);
                            }
                        }

                        // Mettre à jour la shulker box avec le nouveau contenu
                        stack.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(newContents));
                        inventory.markDirty();

                        return; // Refill effectué, on arrête
                    }
                }
            }
        }
    }
}

