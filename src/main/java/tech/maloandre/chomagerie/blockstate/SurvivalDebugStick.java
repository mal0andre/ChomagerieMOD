package tech.maloandre.chomagerie.blockstate;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/** Attack handler copied from JustS-js/debugStickSurvival/SDSMod.java (CC0),
 * adapted to Mojang 26.3 names and server-only execution. */
public final class SurvivalDebugStick {
    public static void register() {
        // Preserve default-phase protection vetoes before running the original handler.
        Identifier phase = Identifier.fromNamespaceAndPath("chomagerie", "debug_stick");
        AttackBlockCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, phase);
        AttackBlockCallback.EVENT.register(phase, (player, world, hand, pos, direction) -> {
            if (player.isSpectator() || player.isCreative() || world.isClientSide()) {
                return InteractionResult.PASS;
            }
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(Items.DEBUG_STICK)) {
                return InteractionResult.PASS;
            }
            CustomData component = stack.get(DataComponents.CUSTOM_DATA);
            if (component != null) {
                CompoundTag nbtData = component.copyTag();
                long lastModified = nbtData.getLong("LastModified").orElse(0L);
                if (world.getGameTime() < lastModified + 5) {
                    return InteractionResult.PASS;
                }
            }
            CompoundTag newNbtData = new CompoundTag();
            newNbtData.putLong("LastModified", world.getGameTime());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(newNbtData));
            stack.getItem().canDestroyBlock(stack, world.getBlockState(pos), world, pos, player);
            return InteractionResult.PASS;
        });
    }

    public static boolean mayEdit(ServerPlayer player, ServerLevel level, BlockPos pos) {
        return !player.isSpectator() && player.mayBuild() && level.mayInteract(player, pos)
                && level.getWorldBorder().isWithinBounds(pos) && player.isWithinBlockInteractionRange(pos, 0.0);
    }
}
