package tech.maloandre.chomagerie.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.maloandre.chomagerie.event.ItemStackDepletedCallback;

import java.util.HashMap;
import java.util.Map;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {

    @Shadow
    @Final
    public PlayerEntity player;

    @Shadow
    @Final
    private DefaultedList<ItemStack> main;

    @Unique
    private final Map<Integer, ItemStack> chomagerie$previousStacks = new HashMap<>();

    /**
     * Surveille quand un stack se vide par décrémentation
     */
    @Inject(method = "updateItems", at = @At("HEAD"))
    private void onUpdateItems(CallbackInfo ci) {
        if (player == null || player.getEntityWorld().isClient()) {
            return;
        }

        PlayerInventory inventory = (PlayerInventory) (Object) this;

        // Vérifier chaque slot de la hotbar et de l'inventaire principal
        for (int i = 0; i < main.size(); i++) {
            ItemStack currentStack = inventory.getStack(i);
            ItemStack previousStack = chomagerie$previousStacks.get(i);

            if (previousStack != null && !previousStack.isEmpty() && currentStack.isEmpty()) {
                // Le stack s'est vidé, déclencher l'événement
                ItemStackDepletedCallback.EVENT.invoker().onItemStackDepleted(
                        player, i, previousStack.getItem(), previousStack
                );
            }

            // Mettre à jour l'état précédent
            if (!currentStack.isEmpty()) {
                chomagerie$previousStacks.put(i, currentStack.copy());
            } else {
                chomagerie$previousStacks.remove(i);
            }
        }
    }
}

