package tech.maloandre.chomagerie.mixin;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.maloandre.chomagerie.blockstate.LockInteractions;

@Mixin(ItemStack.class)
public abstract class BlockToolItemUseMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void chomagerie$waxConnections(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = LockInteractions.use(context);
        if (result != InteractionResult.PASS) cir.setReturnValue(result);
    }
}
