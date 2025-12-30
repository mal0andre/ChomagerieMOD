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

    public static class RefillResult {
        public final boolean success;
        public final String itemName;

        public RefillResult(boolean success, String itemName) {
            this.success = success;
            this.itemName = itemName;
        }
    }

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
     * Cherche et extrait un item d'une shulker box dans un inventaire donné
     * @param sourceInventory L'inventaire à parcourir (peut être l'ender chest)
     * @param targetInventory L'inventaire où mettre l'item extrait (inventaire du joueur)
     * @param targetSlot Le slot où mettre l'item extrait
     * @param itemToRefill L'item à rechercher
     * @param filterByName Si true, filtrer par nom de shulker
     * @param nameFilter Le nom à rechercher (ignoré si filterByName est false)
     * @return true si un item a été trouvé et extrait
     */
    private static boolean tryRefillFromInventoryShulkers(Inventory sourceInventory, Inventory targetInventory,
                                                          int targetSlot, Item itemToRefill,
                                                          boolean filterByName, String nameFilter) {
        // Parcourir l'inventaire à la recherche de shulker boxes
        for (int i = 0; i < sourceInventory.size(); i++) {
            ItemStack stack = sourceInventory.getStack(i);

            if (isShulkerBox(stack.getItem())) {
                // Si le filtrage par nom est activé, vérifier le nom de la shulker
                if (filterByName) {
                    // Récupérer le nom de la shulker box
                    String shulkerName = stack.getName().getString();

                    // Si la shulker n'a pas de nom ou si le nom ne correspond pas, on l'ignore
                    if (shulkerName == null || !shulkerName.equals(nameFilter)) {
                        continue; // Le nom ne correspond pas
                    }
                }

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

                        // Mettre l'item dans le slot du joueur (targetInventory)
                        targetInventory.setStack(targetSlot, refillStack);

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
                        sourceInventory.markDirty();
                        targetInventory.markDirty();

                        return true; // Refill effectué
                    }
                }
            }
        }
        return false; // Aucun item trouvé
    }

    /**
     * Tente de recharger un slot vide depuis les shulker boxes dans l'inventaire et l'ender chest
     * @param player Le joueur
     * @param emptySlot Le slot qui vient de se vider
     * @param itemToRefill L'item à recharger
     * @param filterByName Si true, filtrer par nom de shulker
     * @param nameFilter Le nom à rechercher (ignoré si filterByName est false)
     * @return RefillResult contenant le succès et le nom de l'item
     */
    public static RefillResult tryRefillFromShulker(PlayerEntity player, int emptySlot, Item itemToRefill,
                                                    boolean filterByName, String nameFilter) {
        if (player.getEntityWorld().isClient()) {
            return new RefillResult(false, ""); // Ne fonctionne que côté serveur
        }

        Inventory inventory = player.getInventory();

        // Vérifier si le joueur a déjà l'item dans son inventaire principal
        if (hasItemInMainInventory(inventory, itemToRefill)) {
            return new RefillResult(false, ""); // Ne pas refill si l'item est déjà présent ailleurs dans l'inventaire
        }

        String itemName = itemToRefill.getName().getString();

        // 1. Essayer d'abord dans l'inventaire principal
        if (tryRefillFromInventoryShulkers(inventory, inventory, emptySlot, itemToRefill, filterByName, nameFilter)) {
            return new RefillResult(true, itemName); // Refill effectué depuis l'inventaire
        }

        // 2. Si rien trouvé, chercher dans l'ender chest
        Inventory enderChest = player.getEnderChestInventory();
        boolean success = tryRefillFromInventoryShulkers(enderChest, inventory, emptySlot, itemToRefill, filterByName, nameFilter);
        return new RefillResult(success, success ? itemName : "");
    }
}

