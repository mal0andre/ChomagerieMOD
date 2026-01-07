package tech.maloandre.chomagerie.util;

import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ShulkerRefillHandler {

    /**
     * Checks if an item is a shulker box
     */
    public static boolean isShulkerBox(Item item) {
        return item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    /**
     * Checks if the player already has the item in their main inventory (excluding shulker boxes)
     *
     * @param inventory   The player's inventory
     * @param itemToCheck The item to search for
     * @return true if the item is present in the main inventory
     */
    private static boolean hasItemInMainInventory(Inventory inventory, Item itemToCheck) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);

            // Ignore shulker boxes
            if (isShulkerBox(stack.getItem())) {
                continue;
            }

            // Check if we found the item
            if (!stack.isEmpty() && stack.getItem() == itemToCheck) {
                return true;
            }
        }
        return false;
    }

    /**
     * Searches for and extracts an item from a shulker box in a given inventory
     *
     * @param sourceInventory The inventory to search (can be the ender chest)
     * @param targetInventory The inventory where to put the extracted item (player inventory)
     * @param targetSlot      The slot where to put the extracted item
     * @param itemToRefill    The item to search for
     * @param filterByName    If true, filter by shulker box name
     * @param nameFilter      The name to search for (ignored if filterByName is false)
     * @return true if an item was found and extracted
     */
    private static boolean tryRefillFromInventoryShulkers(Inventory sourceInventory, Inventory targetInventory,
                                                          int targetSlot, Item itemToRefill,
                                                          boolean filterByName, String nameFilter) {
        // Search the inventory for shulker boxes
        for (int i = 0; i < sourceInventory.size(); i++) {
            ItemStack stack = sourceInventory.getStack(i);

            if (isShulkerBox(stack.getItem())) {
                // If name filtering is enabled, check the name of the shulker
                if (filterByName) {
                    // Get the name of the shulker box
                    String shulkerName = stack.getName().getString();

                    // If the shulker has no name or if the name doesn't match, ignore it
                    if (shulkerName == null || !shulkerName.equals(nameFilter)) {
                        continue; // Name doesn't match
                    }
                }

                // Check the contents of the shulker box
                ContainerComponent container = stack.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);

                // Convert the stream to a list
                List<ItemStack> contents = new ArrayList<>();
                container.stream().forEach(contents::add);

                if (contents.isEmpty()) {
                    continue;
                }

                // Search for the item in the shulker box
                for (int j = 0; j < contents.size(); j++) {
                    ItemStack shulkerItem = contents.get(j);

                    if (!shulkerItem.isEmpty() && shulkerItem.getItem() == itemToRefill) {
                        // Found the item! Transfer it to the empty slot
                        int amountToTake = Math.min(shulkerItem.getCount(), itemToRefill.getMaxCount());

                        ItemStack refillStack = shulkerItem.copy();
                        refillStack.setCount(amountToTake);

                        // Put the item in the player's slot (targetInventory)
                        targetInventory.setStack(targetSlot, refillStack);

                        // Remove the item from the shulker box
                        shulkerItem.decrement(amountToTake);

                        // Create a new container with the modified contents
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

                        // Update the shulker box with the new contents
                        stack.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(newContents));
                        sourceInventory.markDirty();
                        targetInventory.markDirty();

                        return true; // Refill completed
                    }
                }
            }
        }
        return false; // No item found
    }

    /**
     * Attempts to refill an empty slot from shulker boxes in inventory and ender chest
     *
     * @param player       The player
     * @param emptySlot    The slot that just became empty
     * @param itemToRefill The item to refill
     * @param filterByName If true, filter by shulker box name
     * @param nameFilter   The name to search for (ignored if filterByName is false)
     * @return RefillResult containing success status and item name
     */
    public static RefillResult tryRefillFromShulker(PlayerEntity player, int emptySlot, Item itemToRefill,
                                                    boolean filterByName, String nameFilter) {
        if (player.getEntityWorld().isClient()) {
            return new RefillResult(false, ""); // Only works server-side
        }

        Inventory inventory = player.getInventory();

        // Check if the player already has the item in their main inventory
        if (hasItemInMainInventory(inventory, itemToRefill)) {
            return new RefillResult(false, ""); // Don't refill if the item is already present elsewhere in inventory
        }

        String itemName = itemToRefill.getName().getString();

        // 1. Try first in the main inventory
        if (tryRefillFromInventoryShulkers(inventory, inventory, emptySlot, itemToRefill, filterByName, nameFilter)) {
            return new RefillResult(true, itemName); // Refill completed from inventory
        }

        // 2. If nothing found, search in the ender chest
        Inventory enderChest = player.getEnderChestInventory();
        boolean success = tryRefillFromInventoryShulkers(enderChest, inventory, emptySlot, itemToRefill, filterByName, nameFilter);
        return new RefillResult(success, success ? itemName : "");
    }

    public record RefillResult(boolean success, String itemName) {
    }
}

