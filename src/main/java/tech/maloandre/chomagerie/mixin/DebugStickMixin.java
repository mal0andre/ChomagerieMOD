package tech.maloandre.chomagerie.mixin;

import tech.maloandre.chomagerie.blockstate.BlockToolsConfig;
import tech.maloandre.chomagerie.blockstate.BlockStateLocks;
import tech.maloandre.chomagerie.blockstate.SurvivalDebugStick;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.DebugStickState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DebugStickItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

/** Port of JustS-js/debugStickSurvival/DebugStickMixin.java (CC0).
 * The original property/value cycling is retained; server policy and persistent locks are integrated. */
@Mixin(DebugStickItem.class)
public abstract class DebugStickMixin extends Item {
    public DebugStickMixin(Properties settings) {
        super(settings);
    }

    @Shadow
    private static void message(ServerPlayer player, Component message) {}

    @Shadow
    private static <T extends Comparable<T>> String getNameHelper(BlockState state, Property<T> property) {return null;}

    @Shadow
    private static <T extends Comparable<T>> BlockState cycleState(BlockState state, Property<T> property, boolean inverse) {
        return null;
    }

    @Shadow
    private static <T> T getRelative(Iterable<T> values, T value, boolean inverse) {
        return null;
    }

    @Inject(at = @At("HEAD"), method = "handleInteraction", cancellable = true)
    private void onUSE(ServerPlayer player, BlockState state, LevelAccessor world, BlockPos pos, boolean update, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!(world instanceof ServerLevel level) || !SurvivalDebugStick.mayEdit(player, level, pos)) {
            cir.setReturnValue(false);
            return;
        }
        if (update && BlockStateLocks.get(level).contains(pos)
                && !BlockToolsConfig.get().blockStateLocking.allowDebugStickEditing) {
            cir.setReturnValue(false);
            return;
        }
        if (!player.canUseGameMasterBlocks() && !BlockToolsConfig.get().debugStick.enabled) {
            cir.setReturnValue(false);
            return;
        }
        // if the player already does have the rights to use Debug Stick, the mod should not interfere
        if (player.canUseGameMasterBlocks()) {return;}

        Block block = state.getBlock();
        Holder<Block> registryEntry = state.typeHolder();
        StateDefinition<Block, BlockState> stateManager = (registryEntry.value()).getStateDefinition();
        Collection<Property<?>> collection = stateManager.getProperties();

        // check if block is modifiable by the config
        if (!isBlockAllowedToModify(block) || collection.isEmpty()) {
            message(player, Component.translatable(this.descriptionId + ".empty", new Object[]{registryEntry.getRegisteredName()}));
            cir.setReturnValue(false);
            return;
        }

        // https://minecraft.wiki/w/Debug_Stick
        // to remember the data of which property for which block is chosen,
        // Minecraft Devs decided to use Component for Debug Stick.
        // Who am I to disagree? (btw thx to @MrBretze for example code)
        DebugStickState stateComponent = stack.get(DataComponents.DEBUG_STICK_STATE);

        if (stateComponent == null) {
            return;
        }

        Property<?> property = stateComponent.properties().get(registryEntry);

        if (update) {
            // change value of property
            if (property == null) {
                property = getNextProperty(collection, null, block, false);
            }
            // check if given property is allowed
            if (!isPropertyModifiable(property, block)) {
                message(player, Component.translatable(this.descriptionId + ".empty", new Object[]{registryEntry.getRegisteredName()}));
                cir.setReturnValue(false);
                return;
            }

            // generate new state of chosen block with modified property
            BlockState newState = getNextBlockState(state, property, player.isSecondaryUseActive());
            // update chosen block with its new state
            BlockStateLocks.get(level).setManually(level, pos, newState, 18);
            // send the player a message of successful modifying
            message(
                    player,
                    Component.translatable(
                            this.descriptionId + ".update",
                            new Object[]{property.getName(), getNameHelper(newState, property)}
                    )
            );
        } else {
            // select next property
            property = getNextProperty(collection, property, block, player.isSecondaryUseActive());
            // check if given property is allowed
            if (!isPropertyModifiable(property, block)) {
                message(player, Component.translatable(this.descriptionId + ".empty", new Object[]{registryEntry.getRegisteredName()}));
                cir.setReturnValue(false);
                return;
            }
            // save chosen property in the NBT data of Debug Stick
            stack.set(DataComponents.DEBUG_STICK_STATE, stateComponent.withProperty(registryEntry, property));

            // send the player a message of successful selecting
            message(
                    player,
                    Component.translatable(
                            this.descriptionId + ".select",
                            new Object[]{property.getName(), getNameHelper(state, property)}
                    )
            );
        }
        cir.setReturnValue(true);
    }

    /**
     * Choose next property that is appropriate for the configuration file
     * */
    @Unique
    private Property<?> getNextProperty(Collection<Property<?>> collection, Property<?> property, Block block, boolean inverse) {
        int i = 0;
        do { // simply scrolling through the list of properties until suitable is found
            property = getRelative(collection, property, inverse);
            i++;
        } while (i < collection.size() && !isPropertyModifiable(property, block));
        return property;
    }

    /**
     * Choose next property that is appropriate for the configuration file
     * */
    @Unique
    private <T extends Comparable<T>> BlockState getNextBlockState(BlockState state, Property<T> property, boolean inverse) {
        int i = 0;
        Collection<T> collection = property.getPossibleValues();
        T value =  state.getValue(property);
        do { // simply scrolling through the list of property values until suitable is found
            value = getRelative(collection, value, inverse);
            i++;
        } while (i < collection.size() && !isPropertyValueAllowed(state.getBlock(), property, value));
        if (!isPropertyValueAllowed(state.getBlock(), property, value) && i == collection.size()) {
            return state;
        }
        return state.setValue(property, value);
    }

    /**
     * Check via config if chosen block is able to be modified in survival
     * */
    @Unique
    private boolean isBlockAllowedToModify(Block block) {
        return block.getStateDefinition().getProperties().stream().anyMatch(p -> isPropertyModifiable(p, block));
    }

    /**
     * Check via config if chosen block state is allowed
     * */
    @Unique
    private <T extends Comparable<T>> boolean isPropertyValueAllowed(Block block, Property<T> property, T value) {
        return BlockToolsConfig.get().debugStick.allows(block.defaultBlockState(), property.getName(), property.getName(value));
    }

    /**
     * Check via config if chosen property is able to be modified in survival
     * */
    @Unique
    private boolean isPropertyModifiable(Property<?> property, Block block) {
        return property != null && block != null && property.getPossibleValues().stream().anyMatch(v -> isPropertyValueAllowedUnchecked(block, property, v));
    }
    @Unique
    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean isPropertyValueAllowedUnchecked(Block block, Property property, Comparable value) {
        return isPropertyValueAllowed(block, property, value);
    }

    @Redirect(method = "handleInteraction", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean chomagerie$writeCreativeManualState(LevelAccessor world, BlockPos pos, BlockState state, int flags) {
        return world instanceof ServerLevel level
                ? BlockStateLocks.get(level).setManually(level, pos, state, flags)
                : world.setBlock(pos, state, flags);
    }

}
