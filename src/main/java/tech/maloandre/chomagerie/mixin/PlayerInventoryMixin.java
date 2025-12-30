package tech.maloandre.chomagerie.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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

    @Shadow
    public int getSelectedSlot() {
        return 0; // Stub, sera remplacé par Shadow
    }

    @Unique
    private Item chomagerie$lastSelectedItem = null;

    @Unique
    private int chomagerie$lastSelectedCount = 0;

    @Unique
    private int chomagerie$lastSelectedSlot = -1;


    /**
     * Surveille quand un stack se vide COMPLÈTEMENT par utilisation dans le slot sélectionné uniquement
     */
    @Inject(method = "updateItems", at = @At("HEAD"))
    private void onUpdateItems(CallbackInfo ci) {
        if (player == null || player.getEntityWorld().isClient()) {
            return;
        }

        PlayerInventory inventory = (PlayerInventory) (Object) this;
        int currentSelectedSlot = getSelectedSlot();
        ItemStack currentStack = inventory.getStack(currentSelectedSlot);

        // Si le joueur a changé de slot, réinitialiser la surveillance
        if (chomagerie$lastSelectedSlot != currentSelectedSlot) {
            chomagerie$lastSelectedSlot = currentSelectedSlot;
            chomagerie$lastSelectedItem = currentStack.isEmpty() ? null : currentStack.getItem();
            chomagerie$lastSelectedCount = currentStack.getCount();
            return; // Ne rien faire d'autre ce tick
        }

        // Cas 1: On surveillait un item et le slot est maintenant complètement vide
        if (chomagerie$lastSelectedItem != null && currentStack.isEmpty()) {
            // Vérifier que c'était une consommation naturelle (count passé de 1 à 0)
            // Si le count était > 1, le joueur a probablement déplacé/jeté le stack manuellement
            if (chomagerie$lastSelectedCount == 1) {
                // Le stack s'est vidé naturellement (dernier bloc posé, dernier item consommé, etc.)
                ItemStackDepletedCallback.EVENT.invoker().onItemStackDepleted(
                        player, currentSelectedSlot, chomagerie$lastSelectedItem, null
                );
            }
            // Réinitialiser la surveillance
            chomagerie$lastSelectedItem = null;
            chomagerie$lastSelectedCount = 0;
        }
        // Cas 2: On a un item dans le slot sélectionné
        else if (!currentStack.isEmpty()) {
            Item currentItem = currentStack.getItem();
            int currentCount = currentStack.getCount();

            // Si c'est le même item qu'avant
            if (chomagerie$lastSelectedItem == currentItem) {
                // Mettre à jour uniquement si le count a diminué (utilisation normale)
                // Si le count a augmenté ou changé drastiquement, c'est une manipulation manuelle
                if (currentCount < chomagerie$lastSelectedCount) {
                    chomagerie$lastSelectedCount = currentCount;
                } else if (currentCount > chomagerie$lastSelectedCount) {
                    // Le count a augmenté (ajout manuel, stack, etc.), on réinitialise
                    chomagerie$lastSelectedCount = currentCount;
                }
            }
            // Si l'item a changé, commencer à surveiller le nouveau
            else {
                chomagerie$lastSelectedItem = currentItem;
                chomagerie$lastSelectedCount = currentCount;
            }
        }
        // Cas 3: Le slot est vide et on ne surveillait rien
        else {
            chomagerie$lastSelectedItem = null;
            chomagerie$lastSelectedCount = 0;
        }
    }
}

