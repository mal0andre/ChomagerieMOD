package tech.maloandre.chomagerie.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.maloandre.chomagerie.event.ItemStackDepletedCallback;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {

    @Shadow
    @Final
    public PlayerEntity player;
    @Unique
    private Item chomagerie$lastSelectedItem = null;
    @Unique
    private ItemStack chomagerie$lastSelectedStack = null;
    @Unique
    private int chomagerie$lastSelectedCount = 0;
    @Unique
    private int chomagerie$lastSelectedSlot = -1;
    @Unique
    private int chomagerie$lastUsedStatValue = 0;

    // Surveillance de l'offhand
    @Unique
    private Item chomagerie$lastOffhandItem = null;
    @Unique
    private ItemStack chomagerie$lastOffhandStack = null;
    @Unique
    private int chomagerie$lastOffhandCount = 0;
    @Unique
    private int chomagerie$lastOffhandUsedStatValue = 0;

    @Shadow
    public int getSelectedSlot() {
        return 0; // Stub, sera remplacé par Shadow
    }

    @Unique
    private int chomagerie$getUsedStat(Item item) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            return serverPlayer.getStatHandler().getStat(Stats.USED.getOrCreateStat(item));
        }
        return 0;
    }

    /**
     * Vérifie si un item est un seau rempli (eau, lave, neige poudreuse, lait)
     */
    @Unique
    private boolean chomagerie$isFilledBucket(Item item) {
        return item == Items.WATER_BUCKET || 
               item == Items.LAVA_BUCKET || 
               item == Items.POWDER_SNOW_BUCKET ||
               item == Items.MILK_BUCKET ||
               item == Items.PUFFERFISH_BUCKET ||
               item == Items.SALMON_BUCKET ||
               item == Items.COD_BUCKET ||
               item == Items.TROPICAL_FISH_BUCKET ||
               item == Items.AXOLOTL_BUCKET ||
               item == Items.TADPOLE_BUCKET;
    }


    /**
     * Surveille quand un stack se vide COMPLÈTEMENT par utilisation dans le slot sélectionné uniquement
     */
    @Inject(method = "updateItems", at = @At("HEAD"))
    private void onUpdateItems(CallbackInfo ci) {
        if (player == null || player.getEntityWorld().isClient()) {
            return;
        }

        PlayerInventory inventory = (PlayerInventory) (Object) this;

        // Surveiller la main principale
        chomagerie$checkMainHand(inventory);

        // Surveiller l'offhand
        chomagerie$checkOffhand(inventory);
    }

    /**
     * Vérifie et surveille la main principale (hotbar slot sélectionné)
     */
    @Unique
    private void chomagerie$checkMainHand(PlayerInventory inventory) {
        int currentSelectedSlot = getSelectedSlot();
        ItemStack currentStack = inventory.getStack(currentSelectedSlot);

        // Si le joueur a changé de slot, réinitialiser la surveillance
        if (chomagerie$lastSelectedSlot != currentSelectedSlot) {
            chomagerie$lastSelectedSlot = currentSelectedSlot;
            chomagerie$lastSelectedItem = currentStack.isEmpty() ? null : currentStack.getItem();
            chomagerie$lastSelectedStack = currentStack.isEmpty() ? null : currentStack.copy();
            chomagerie$lastSelectedCount = currentStack.getCount();
            if (chomagerie$lastSelectedItem != null) {
                chomagerie$lastUsedStatValue = chomagerie$getUsedStat(chomagerie$lastSelectedItem);
            } else {
                chomagerie$lastUsedStatValue = 0;
            }
            return; // Ne rien faire d'autre ce tick
        }

        // Cas 1 : On surveillait un item et le slot est maintenant complètement vide
        if (chomagerie$lastSelectedItem != null && currentStack.isEmpty()) {
            // Vérifier que c'était une consommation naturelle (count passé de 1 à 0)
            // Si le count était > 1, le joueur a probablement déplacé/jeté le stack manuellement
            if (chomagerie$lastSelectedCount == 1) {
                // Vérifier si la statistique USED a augmenté (indique une pose de bloc ou une consommation d'item)
                int currentUsedStat = chomagerie$getUsedStat(chomagerie$lastSelectedItem);
                if (currentUsedStat > chomagerie$lastUsedStatValue) {
                    // Le stack s'est vidé naturellement (dernier bloc posé, dernier item consommé, etc.)
                    ItemStackDepletedCallback.EVENT.invoker().onItemStackDepleted(
                            player, currentSelectedSlot, chomagerie$lastSelectedItem, chomagerie$lastSelectedStack
                    );
                }
            }
            // Réinitialiser la surveillance
            chomagerie$lastSelectedItem = null;
            chomagerie$lastSelectedStack = null;
            chomagerie$lastSelectedCount = 0;
            chomagerie$lastUsedStatValue = 0;
        }
        // Cas 1.5 : On surveillait un seau rempli et maintenant c'est un seau vide (item remplacé)
        else if (chomagerie$lastSelectedItem != null && !currentStack.isEmpty() &&
                 currentStack.getItem() == Items.BUCKET &&
                 chomagerie$isFilledBucket(chomagerie$lastSelectedItem)) {
            // Un seau rempli est devenu un seau vide, c'est une utilisation valide
            int currentUsedStat = chomagerie$getUsedStat(chomagerie$lastSelectedItem);
            if (currentUsedStat > chomagerie$lastUsedStatValue) {
                // Le seau a été utilisé naturellement
                ItemStackDepletedCallback.EVENT.invoker().onItemStackDepleted(
                        player, currentSelectedSlot, chomagerie$lastSelectedItem, chomagerie$lastSelectedStack
                );
            }
            // Réinitialiser la surveillance pour le nouveau seau vide
            chomagerie$lastSelectedItem = Items.BUCKET;
            chomagerie$lastSelectedStack = currentStack.copy();
            chomagerie$lastSelectedCount = currentStack.getCount();
            chomagerie$lastUsedStatValue = chomagerie$getUsedStat(Items.BUCKET);
        }
        // Cas 2 : On a un item dans le slot sélectionné
        else if (!currentStack.isEmpty()) {
            Item currentItem = currentStack.getItem();
            int currentCount = currentStack.getCount();

            // Si c'est le même item qu'avant
            if (chomagerie$lastSelectedItem == currentItem) {
                // Mettre à jour uniquement si le count a diminué (utilisation normale)
                // Si le count a augmenté ou changé drastiquement, c'est une manipulation manuelle
                if (currentCount < chomagerie$lastSelectedCount) {
                    chomagerie$lastSelectedCount = currentCount;
                    chomagerie$lastSelectedStack = currentStack.copy();
                    chomagerie$lastUsedStatValue = chomagerie$getUsedStat(currentItem);
                } else if (currentCount > chomagerie$lastSelectedCount) {
                    // Le count a augmenté (ajout manuel, stack, etc.), on réinitialise
                    chomagerie$lastSelectedCount = currentCount;
                    chomagerie$lastSelectedStack = currentStack.copy();
                    chomagerie$lastUsedStatValue = chomagerie$getUsedStat(currentItem);
                }
            }
            // Si l'item a changé, commencer à surveiller le nouveau
            else {
                chomagerie$lastSelectedItem = currentItem;
                chomagerie$lastSelectedCount = currentCount;
                chomagerie$lastSelectedStack = currentStack.copy();
                chomagerie$lastUsedStatValue = chomagerie$getUsedStat(currentItem);
            }
        }
        // Cas 3 : Le slot est vide et on ne surveillait rien
        else {
            chomagerie$lastSelectedCount = 0;
            chomagerie$lastSelectedStack = null;
            chomagerie$lastUsedStatValue = 0;
        }
    }

    /**
     * Vérifie et surveille l'offhand (slot 40 dans l'inventaire du joueur)
     */
    @Unique
    private void chomagerie$checkOffhand(PlayerInventory inventory) {
        final int OFFHAND_SLOT = 40; // Le slot de l'offhand est toujours 40
        ItemStack currentStack = inventory.getStack(OFFHAND_SLOT);

        // Cas 1 : On surveillait un item et le slot est maintenant complètement vide
        if (chomagerie$lastOffhandItem != null && currentStack.isEmpty()) {
            // Vérifier que c'était une consommation naturelle (count passé de 1 à 0)
            if (chomagerie$lastOffhandCount == 1) {
                // Vérifier si la statistique USED a augmenté
                int currentUsedStat = chomagerie$getUsedStat(chomagerie$lastOffhandItem);
                if (currentUsedStat > chomagerie$lastOffhandUsedStatValue) {
                    // Le stack s'est vidé naturellement
                    ItemStackDepletedCallback.EVENT.invoker().onItemStackDepleted(
                            player, OFFHAND_SLOT, chomagerie$lastOffhandItem, chomagerie$lastOffhandStack
                    );
                }
            }
            // Réinitialiser la surveillance
            chomagerie$lastOffhandItem = null;
            chomagerie$lastOffhandStack = null;
            chomagerie$lastOffhandCount = 0;
            chomagerie$lastOffhandUsedStatValue = 0;
        }
        // Cas 1.5 : On surveillait un seau rempli et maintenant c'est un seau vide (item remplacé)
        else if (chomagerie$lastOffhandItem != null && !currentStack.isEmpty() &&
                 currentStack.getItem() == Items.BUCKET &&
                 chomagerie$isFilledBucket(chomagerie$lastOffhandItem)) {
            // Un seau rempli est devenu un seau vide, c'est une utilisation valide
            int currentUsedStat = chomagerie$getUsedStat(chomagerie$lastOffhandItem);
            if (currentUsedStat > chomagerie$lastOffhandUsedStatValue) {
                // Le seau a été utilisé naturellement
                ItemStackDepletedCallback.EVENT.invoker().onItemStackDepleted(
                        player, OFFHAND_SLOT, chomagerie$lastOffhandItem, chomagerie$lastOffhandStack
                );
            }
            // Réinitialiser la surveillance pour le nouveau seau vide
            chomagerie$lastOffhandItem = Items.BUCKET;
            chomagerie$lastOffhandStack = currentStack.copy();
            chomagerie$lastOffhandCount = currentStack.getCount();
            chomagerie$lastOffhandUsedStatValue = chomagerie$getUsedStat(Items.BUCKET);
        }
        // Cas 2 : On a un item dans l'offhand
        else if (!currentStack.isEmpty()) {
            Item currentItem = currentStack.getItem();
            int currentCount = currentStack.getCount();

            // Si c'est le même item qu'avant
            if (chomagerie$lastOffhandItem == currentItem) {
                // Mettre à jour uniquement si le count a diminué (utilisation normale)
                if (currentCount < chomagerie$lastOffhandCount) {
                    chomagerie$lastOffhandCount = currentCount;
                    chomagerie$lastOffhandStack = currentStack.copy();
                    chomagerie$lastOffhandUsedStatValue = chomagerie$getUsedStat(currentItem);
                } else if (currentCount > chomagerie$lastOffhandCount) {
                    // Le count a augmenté (ajout manuel, stack, etc.), on réinitialise
                    chomagerie$lastOffhandCount = currentCount;
                    chomagerie$lastOffhandStack = currentStack.copy();
                    chomagerie$lastOffhandUsedStatValue = chomagerie$getUsedStat(currentItem);
                }
            }
            // Si l'item a changé, commencer à surveiller le nouveau
            else {
                chomagerie$lastOffhandItem = currentItem;
                chomagerie$lastOffhandCount = currentCount;
                chomagerie$lastOffhandStack = currentStack.copy();
                chomagerie$lastOffhandUsedStatValue = chomagerie$getUsedStat(currentItem);
            }
        }
        // Cas 3 : Le slot est vide et on ne surveillait rien
        else {
            chomagerie$lastOffhandCount = 0;
            chomagerie$lastOffhandStack = null;
            chomagerie$lastOffhandUsedStatValue = 0;
        }
    }
}

