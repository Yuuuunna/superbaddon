package com.yy.superbaddon.mixin;

import com.yy.superbaddon.SuperbAddonMod;
import com.yy.superbaddon.content.ContentControlManager;
import com.yy.superbaddon.content.RecipeRewriteAccess;
import com.yy.superbaddon.content.VehicleRecipeOverrideManager;
import com.google.common.collect.ImmutableMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * 只负责暴露私有配方表并执行重写，不注入任何方法。
 * 重写时机由 event/RecipeRewriteEvents（AddReloadListenerEvent）保证，
 * 原因见该类注释：apply/replaceRecipes 两个注入点在 KubeJS 环境下都会被绕过。
 */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin implements RecipeRewriteAccess {
    @Shadow
    private Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipes;

    @Shadow
    private Map<ResourceLocation, Recipe<?>> byName;

    @Override
    public void superbaddon$rewriteRecipeTable() {
        ContentControlManager.reload();
        VehicleRecipeOverrideManager.reload();

        // 单一数据源：只看最终反序列化配方表。KubeJS 增删改完的结果就是这张表；
        // 原始 JSON 在 KubeJS 接管时只是改写前的旧数据，按它判定反而是错的。
        LinkedHashSet<ResourceLocation> blockedRecipeIds = new LinkedHashSet<>();
        for (Map<ResourceLocation, Recipe<?>> typedRecipes : recipes.values()) {
            for (Map.Entry<ResourceLocation, Recipe<?>> entry : typedRecipes.entrySet()) {
                if (ContentControlManager.shouldRemoveRecipe(entry.getKey(), entry.getValue())
                        || VehicleRecipeOverrideManager.isDisabledRecipe(entry.getValue())) {
                    blockedRecipeIds.add(entry.getKey());
                }
            }
        }

        boolean hasInputOrCategoryOverrides = VehicleRecipeOverrideManager.hasInputOrCategoryOverrides();
        if (blockedRecipeIds.isEmpty() && !hasInputOrCategoryOverrides) return;
        if (!blockedRecipeIds.isEmpty()) {
            SuperbAddonMod.LOGGER.info(
                    "[content-control] removed {} recipes blocked by content_control / vehicle override: {}",
                    blockedRecipeIds.size(), blockedRecipeIds);
        }

        LinkedHashMap<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> filteredByType = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, Recipe<?>> filteredByName = new LinkedHashMap<>();
        int appliedOverrides = 0;

        for (Map.Entry<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> typeEntry : recipes.entrySet()) {
            LinkedHashMap<ResourceLocation, Recipe<?>> filteredRecipes = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, Recipe<?>> recipeEntry : typeEntry.getValue().entrySet()) {
                if (blockedRecipeIds.contains(recipeEntry.getKey())) continue;

                Recipe<?> original = recipeEntry.getValue();
                Recipe<?> recipe = VehicleRecipeOverrideManager.applyOverride(original);
                if (recipe != original) appliedOverrides++;

                filteredRecipes.put(recipeEntry.getKey(), recipe);
                filteredByName.put(recipeEntry.getKey(), recipe);
            }
            if (!filteredRecipes.isEmpty()) filteredByType.put(typeEntry.getKey(), ImmutableMap.copyOf(filteredRecipes));
        }

        recipes = ImmutableMap.copyOf(filteredByType);
        byName = ImmutableMap.copyOf(filteredByName);

        if (appliedOverrides > 0) {
            SuperbAddonMod.LOGGER.info(
                    "[vehicle-recipe-override] applied {} input/category overrides", appliedOverrides);
        } else if (hasInputOrCategoryOverrides) {
            SuperbAddonMod.LOGGER.warn(
                    "[vehicle-recipe-override] configured input/category overrides but matched 0 vehicle assembling recipes");
        }
    }
}
