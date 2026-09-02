package com.yy.superbaddon.mixin;

import com.yy.superbaddon.recipe.ShellReloadingRecipe;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {
    @Shadow
    @Final
    private CraftingContainer craftSlots;

    @Unique
    private ShellReloadingRecipe superbaddon$pendingShellReloadingRecipe;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void superbaddon$captureShellReloadingRecipe(Player player, ItemStack stack, CallbackInfo ci) {
        if (player.level().isClientSide) return;

        superbaddon$pendingShellReloadingRecipe = player.level().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, craftSlots, player.level())
                .filter(ShellReloadingRecipe.class::isInstance)
                .map(ShellReloadingRecipe.class::cast)
                .orElse(null);
    }

    @Inject(method = "onTake", at = @At("RETURN"))
    private void superbaddon$consumeShellReloadingCasing(Player player, ItemStack stack, CallbackInfo ci) {
        if (player.level().isClientSide) return;
        ShellReloadingRecipe recipe = superbaddon$pendingShellReloadingRecipe;
        superbaddon$pendingShellReloadingRecipe = null;
        if (recipe != null) {
            recipe.consumeExtraCasing(craftSlots);
        }
    }
}
