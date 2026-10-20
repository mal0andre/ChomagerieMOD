package tech.maloandre.chomagerie.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.maloandre.chomagerie.blockstate.BlockStateLocks;
import tech.maloandre.chomagerie.blockstate.BlockToolsConfig;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateLockMixin {
    @Unique
    private static boolean chomagerie$locked(LevelReader world, BlockPos pos) {
        return world instanceof ServerLevel level && BlockToolsConfig.get().blockStateLocking.enabled
                && BlockStateLocks.get(level).contains(pos);
    }

    @Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
    private void chomagerie$preserveState(LevelReader world, ScheduledTickAccess ticks, BlockPos pos,
                                          Direction direction, BlockPos neighborPos, BlockState neighborState,
                                          RandomSource random, CallbackInfoReturnable<BlockState> cir) {
        if (chomagerie$locked(world, pos))
            cir.setReturnValue(BlockStateLocks.get((ServerLevel) world).preserve(pos, (BlockState) (Object) this));
    }

    @Inject(method = {"tick", "randomTick"}, at = @At("HEAD"), cancellable = true)
    private void chomagerie$freezeBlockTick(ServerLevel world, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (chomagerie$locked(world, pos)) ci.cancel();
    }

    @Inject(method = "handleNeighborChanged", at = @At("HEAD"), cancellable = true)
    private void chomagerie$freezeNeighborAction(Level world, BlockPos pos, Block neighbor, Orientation orientation,
                                                boolean movedByPiston, CallbackInfo ci) {
        if (chomagerie$locked(world, pos)) ci.cancel();
    }
}
