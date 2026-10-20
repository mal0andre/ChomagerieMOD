package tech.maloandre.chomagerie.blockstate;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Runs at item use, after vanilla/Fabric interaction checks, rather than claiming UseBlockCallback early. */
public final class LockInteractions {
    public static InteractionResult use(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player))
            return InteractionResult.PASS;
        var config = BlockToolsConfig.get().blockStateLocking;
        if (!config.enabled) return InteractionResult.PASS;
        var stack = context.getItemInHand();
        boolean honeycomb = stack.is(Items.HONEYCOMB);
        boolean axe = stack.is(ItemTags.AXES) && config.allowAxeUnlock;
        if (!honeycomb && !axe) return InteractionResult.PASS;
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        var locks = BlockStateLocks.get(level);
        boolean locked = locks.contains(pos);
        if (!locked && (!honeycomb || !config.allows(state))) return InteractionResult.PASS;
        if (!SurvivalDebugStick.mayEdit(player, level, pos)) return InteractionResult.FAIL;
        if (honeycomb) {
            if (locked) return InteractionResult.SUCCESS;
            locks.lock(pos, state);
            if (config.consumeHoneycomb && !player.isCreative()) stack.shrink(1);
        } else {
            locks.remove(pos);
            BlockState recalculated = Block.updateFromNeighbourShapes(state, level, pos);
            Block.updateOrDestroy(state, recalculated, level, pos, Block.UPDATE_ALL);
            BlockState current = level.getBlockState(pos);
            if (!current.isAir()) {
                // Resume scheduled and redstone behavior even if the previously queued tick was consumed.
                level.scheduleTick(pos, current.getBlock(), 1);
                level.neighborChanged(pos, current.getBlock(), null);
                current.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
            }
            if (!player.isCreative()) stack.hurtAndBreak(1, player, context.getHand());
        }
        if (config.playEffects) {
            // Vanilla wax particles and sounds are understood by unmodified clients.
            level.levelEvent(null, honeycomb ? 3003 : 3004, pos, 0);
            level.playSound(null, pos, honeycomb ? SoundEvents.HONEYCOMB_WAX_ON : SoundEvents.AXE_WAX_OFF.value(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        if (config.showFeedbackMessages) {
            player.sendOverlayMessage(Component.literal(honeycomb ? "État du bloc verrouillé." : "État du bloc déverrouillé."));
        }
        return InteractionResult.SUCCESS;
    }
}
