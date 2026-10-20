package tech.maloandre.chomagerie.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.Holder;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tech.maloandre.chomagerie.blockstate.BlockToolsConfig;

import java.util.stream.Stream;

@Mixin(RecipeMap.class)
public abstract class DebugStickRecipeMixin {
    @ModifyExpressionValue(method = "create", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/HolderLookup;listElements()Ljava/util/stream/Stream;"))
    private static Stream<Holder.Reference<Recipe<?>>> chomagerie$filterRecipe(Stream<Holder.Reference<Recipe<?>>> recipes) {
        if (BlockToolsConfig.get().debugStick.craftingEnabled) return recipes;
        return recipes.filter(r -> !r.key().identifier().toString().equals("chomagerie:utility/debug_stick"));
    }
}
