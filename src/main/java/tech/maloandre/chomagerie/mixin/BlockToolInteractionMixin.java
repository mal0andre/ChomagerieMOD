package tech.maloandre.chomagerie.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.maloandre.chomagerie.blockstate.LockInteractions;

/** After Fabric's HEAD protection callbacks, before chests/doors/signs handle the click. */
@Mixin(ServerPlayerGameMode.class)
public abstract class BlockToolInteractionMixin {
    @Inject(method = "useItemOn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;", ordinal = 0), cancellable = true)
    private void chomagerie$toolsBeforeBlockInteraction(ServerPlayer player, Level level, ItemStack stack,
                                                        InteractionHand hand, BlockHitResult hit,
                                                        CallbackInfoReturnable<InteractionResult> cir) {
        if (!stack.is(Items.DEBUG_STICK) && !stack.is(Items.HONEYCOMB) && !stack.is(ItemTags.AXES)) return;
        UseOnContext context = new UseOnContext(player, hand, hit);
        InteractionResult result = stack.is(Items.DEBUG_STICK)
                ? stack.getItem().useOn(context) : LockInteractions.use(context);
        if (result != InteractionResult.PASS) cir.setReturnValue(result);
    }
}
