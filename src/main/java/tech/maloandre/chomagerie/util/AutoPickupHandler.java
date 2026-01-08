package tech.maloandre.chomagerie.util;

import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import tech.maloandre.chomagerie.Chomagerie;

import java.util.ArrayList;
import java.util.List;

public class AutoPickupHandler {

    /**
     * Checks if an item is a shulker box
     */
    public static boolean isShulkerBox(Item item) {
        return item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    /**
     * Tries to store the picked up item in an appropriate shulker box
     * Only stores items that cannot be stacked in the player's existing inventory
     *
     * @param player       The player who picked up the item
     * @param pickedStack  The item stack that was picked up (used to identify the item type)
     * @param filterByName If true, filter by shulker box name
     * @param nameFilter   The name to search for (ignored if filterByName is false)
     * @return PickupResult containing success status and details
     */
    public static PickupResult tryStoreInShulker(PlayerEntity player, ItemStack pickedStack,
                                                 boolean filterByName, String nameFilter) {
        if (player.getEntityWorld().isClient()) {
            return new PickupResult(false, "", 0); // Only works server-side
        }

        if (pickedStack.isEmpty() || isShulkerBox(pickedStack.getItem())) {
            return new PickupResult(false, "", 0); // Don't store shulker boxes in shulker boxes
        }

        Inventory inventory = player.getInventory();
        Item itemToStore = pickedStack.getItem();
        int maxStackSize = itemToStore.getMaxCount();

        Chomagerie.LOGGER.info("AutoPickup triggered for {} (filter: {}, name: {})",
            itemToStore.getName().getString(), filterByName, nameFilter);

        // Find all shulker boxes that already contain this item type
        List<Integer> shulkerSlots = new ArrayList<>();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);

            if (isShulkerBox(stack.getItem())) {
                // If name filtering is enabled, check the name of the shulker
                if (filterByName) {
                    String shulkerName = stack.getName().getString();
                    if (shulkerName == null || !shulkerName.equals(nameFilter)) {
                        continue;
                    }
                }

                // Check the contents of the shulker box
                ContainerComponent container = stack.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);
                boolean hasItemType = container.stream()
                        .anyMatch(shulkerItem -> !shulkerItem.isEmpty() && shulkerItem.getItem() == itemToStore);

                if (hasItemType) {
                    shulkerSlots.add(i);
                    Chomagerie.LOGGER.info("Found matching shulker at slot {}", i);
                }
            }
        }

        if (shulkerSlots.isEmpty()) {
            Chomagerie.LOGGER.info("No matching shulker found for {}", itemToStore.getName().getString());
            return new PickupResult(false, "", 0); // No matching shulker found
        }

        // Find all non-full stacks of this item in the player's inventory (excluding shulkers)
        // to determine what can still be stacked
        int totalCanStack = 0;
        List<Integer> nonFullSlots = new ArrayList<>();

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && stack.getItem() == itemToStore && !isShulkerBox(stack.getItem())) {
                if (stack.getCount() < maxStackSize) {
                    int canAdd = maxStackSize - stack.getCount();
                    totalCanStack += canAdd;
                    nonFullSlots.add(i);
                }
            }
        }

        Chomagerie.LOGGER.info("Can still stack {} items in player inventory", totalCanStack);

        // Now transfer only items that are in FULL stacks or exceed stacking capacity
        int storedTotal = 0;

        for (int shulkerSlot : shulkerSlots) {
            ItemStack shulkerStack = inventory.getStack(shulkerSlot);

            // Transfer only FULL stacks or surplus items
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack playerStack = inventory.getStack(i);

                if (!playerStack.isEmpty() &&
                    playerStack.getItem() == itemToStore &&
                    !isShulkerBox(playerStack.getItem())) {

                    // Only transfer if this is a FULL stack (no room to stack more)
                    if (playerStack.getCount() >= maxStackSize) {
                        int stored = transferItemToShulker(shulkerStack, playerStack, inventory);
                        storedTotal += stored;

                        if (playerStack.isEmpty()) {
                            inventory.setStack(i, ItemStack.EMPTY);
                        }
                    }
                }
            }

            // If we've stored items, break (only use first matching shulker)
            if (storedTotal > 0) {
                Chomagerie.LOGGER.info("Stored {} surplus items in shulker at slot {}", storedTotal, shulkerSlot);
                break;
            }
        }

        if (storedTotal > 0) {
            inventory.markDirty();
            return new PickupResult(true, itemToStore.getName().getString(), storedTotal);
        }

        return new PickupResult(false, "", 0);
    }

    /**
     * Transfers items from player inventory to a shulker box
     *
     * @param shulkerStack  The shulker box stack
     * @param itemToTransfer The item stack to transfer from player inventory (will be modified)
     * @param inventory     The player's inventory
     * @return The number of items successfully transferred
     */
    private static int transferItemToShulker(ItemStack shulkerStack, ItemStack itemToTransfer,
                                            Inventory inventory) {
        int maxStackSize = itemToTransfer.getItem().getMaxCount();
        int remainingToTransfer = itemToTransfer.getCount();

        ContainerComponent container = shulkerStack.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);
        List<ItemStack> contents = new ArrayList<>();
        container.stream().forEach(contents::add);

        List<ItemStack> newContents = new ArrayList<>(contents);

        // First, try to stack with existing items of the same type
        for (int i = 0; i < newContents.size() && remainingToTransfer > 0; i++) {
            ItemStack existingStack = newContents.get(i);
            if (!existingStack.isEmpty() && existingStack.getItem() == itemToTransfer.getItem()) {
                int currentCount = existingStack.getCount();
                if (currentCount < maxStackSize) {
                    int canAdd = Math.min(remainingToTransfer, maxStackSize - currentCount);
                    existingStack.setCount(currentCount + canAdd);
                    remainingToTransfer -= canAdd;
                }
            }
        }

        // Then, try to add to empty slots (up to 27 slots in a shulker box)
        while (remainingToTransfer > 0 && newContents.size() < 27) {
            int toAdd = Math.min(remainingToTransfer, maxStackSize);
            ItemStack newStack = itemToTransfer.copy();
            newStack.setCount(toAdd);
            newContents.add(newStack);
            remainingToTransfer -= toAdd;
        }

        // If we managed to transfer some items, update the shulker and player inventory
        int transferredCount = itemToTransfer.getCount() - remainingToTransfer;
        if (transferredCount > 0) {
            shulkerStack.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(newContents));
            itemToTransfer.decrement(transferredCount);
            inventory.markDirty();
        }

        return transferredCount;
    }

    /**
     * Result of a pickup storage attempt
     *
     * @param success     Whether any items were stored
     * @param itemName    The name of the item
     * @param storedCount The number of items stored
     */
    public record PickupResult(boolean success, String itemName, int storedCount) {
    }
}

