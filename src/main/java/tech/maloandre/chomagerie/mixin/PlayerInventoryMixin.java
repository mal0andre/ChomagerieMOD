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


    /**
     * Surveille quand un stack se vide par décrémentation dans le slot sélectionné uniquement
     */
    @Inject(method = "updateItems", at = @At("HEAD"))
    private void onUpdateItems(CallbackInfo ci) {
        if (player == null || player.getEntityWorld().isClient()) {
            return;
        }

        PlayerInventory inventory = (PlayerInventory) (Object) this;
        int currentSelectedSlot = getSelectedSlot();
        ItemStack currentStack = inventory.getStack(currentSelectedSlot);

        // Vérifier si le slot sélectionné s'est vidé
        if (chomagerie$lastSelectedItem != null && currentStack.isEmpty()) {
            // Le stack s'est vidé, déclencher l'événement
            ItemStackDepletedCallback.EVENT.invoker().onItemStackDepleted(
                    player, currentSelectedSlot, chomagerie$lastSelectedItem, null
            );
            chomagerie$lastSelectedItem = null;
        }
        // Si l'item a changé (pas le même), réinitialiser le suivi
        else if (!currentStack.isEmpty()) {
            if (chomagerie$lastSelectedItem != currentStack.getItem()) {
                chomagerie$lastSelectedItem = currentStack.getItem();
            }
        }
    }
}

