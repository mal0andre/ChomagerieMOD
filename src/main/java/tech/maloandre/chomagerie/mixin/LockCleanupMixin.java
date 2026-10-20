package tech.maloandre.chomagerie.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.maloandre.chomagerie.blockstate.BlockStateLocks;
import tech.maloandre.chomagerie.blockstate.BlockToolsConfig;

@Mixin(LevelChunk.class)
public abstract class LockCleanupMixin {
    @Inject(method = "setBlockState", at = @At("HEAD"), cancellable = true)
    private void chomagerie$guardLockedState(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
        if (((LevelChunk) (Object) this).getLevel() instanceof ServerLevel level
                && BlockToolsConfig.get().blockStateLocking.enabled
                && BlockStateLocks.get(level).blocksWrite(pos, state)) cir.setReturnValue(null);
    }

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void chomagerie$removeReplacedLock(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
        BlockState previous = cir.getReturnValue();
        if (previous != null && previous.getBlock() != state.getBlock()
                && ((LevelChunk) (Object) this).getLevel() instanceof ServerLevel level)
            BlockStateLocks.get(level).remove(pos);
    }
}
