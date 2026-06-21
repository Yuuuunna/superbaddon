package com.yy.superbaddon.content;

import com.yy.superbaddon.SuperbAddonMod;
import com.yy.superbaddon.content.VehicleRecipeOverrideConfig.OverrideSpec;
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingIngredient;
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 载具配方覆写查询层。持有一份不可变的 {@code 载具实体id -> OverrideSpec}，
 * 所有钩子（mixin）只通过这里提问：哪些配方该禁用、某个配方该替换成什么。
 * 所有对主模组 {@link VehicleAssemblingRecipe} 的认知都收敛在这一个文件里。
 */
public final class VehicleRecipeOverrideManager {

    private static final AtomicBoolean LOADED = new AtomicBoolean(false);
    private static volatile Map<ResourceLocation, OverrideSpec> overrides = Map.of();

    private VehicleRecipeOverrideManager() {
    }

    // ---- 生命周期 ----

    public static synchronized void reload() {
        overrides = VehicleRecipeOverrideConfig.load();
        LOADED.set(true);

        long disabled = overrides.values().stream().filter(OverrideSpec::disabled).count();
        long mutated = overrides.values().stream().filter(s -> s.hasInputs() || s.hasCategory()).count();
        SuperbAddonMod.LOGGER.info("Loaded SuperbAddon vehicle recipe overrides: {} disabled, {} input/category overrides",
                disabled, mutated);
        SuperbAddonMod.LOGGER.debug("[vehicle-recipe-override] {}", overrides);
    }

    private static Map<ResourceLocation, OverrideSpec> snapshot() {
        if (!LOADED.get()) reload();
        return overrides;
    }

    // ---- 查询 ----

    /** 是否存在需要替换 inputs/category 的覆写（决定 mixin 是否需要重建配方表）。 */
    public static boolean hasInputOrCategoryOverrides() {
        for (OverrideSpec spec : snapshot().values()) {
            if (spec.hasInputs() || spec.hasCategory()) return true;
        }
        return false;
    }

    /** 该配方是否为被标记禁用的载具装配配方（按 result.entity 匹配）。 */
    public static boolean isDisabledRecipe(Recipe<?> recipe) {
        OverrideSpec spec = specFor(recipe);
        return spec != null && spec.disabled();
    }

    /**
     * 对单个配方应用 inputs/category 覆写。命中则返回新构造的替换配方，否则原样返回。
     * 不改 result，只替换材料表与分类，确保装配台输出实体不变。
     */
    public static Recipe<?> applyOverride(Recipe<?> recipe) {
        if (!(recipe instanceof VehicleAssemblingRecipe vehicleRecipe)) return recipe;
        OverrideSpec spec = specFor(vehicleRecipe);
        if (spec == null || (!spec.hasInputs() && !spec.hasCategory())) return recipe;

        VehicleAssemblingRecipe.Category category = spec.hasCategory()
                ? categoryByName(spec.category())
                : vehicleRecipe.category;
        List<VehicleAssemblingIngredient> inputs = spec.hasInputs()
                ? buildInputs(spec.inputs())
                : new ArrayList<>(vehicleRecipe.inputs);

        return new VehicleAssemblingRecipe(vehicleRecipe.getId(), category, vehicleRecipe.result, inputs);
    }

    // ---- 内部 ----

    private static OverrideSpec specFor(Recipe<?> recipe) {
        if (!(recipe instanceof VehicleAssemblingRecipe vehicleRecipe)) return null;
        return specFor(vehicleRecipe);
    }

    private static OverrideSpec specFor(VehicleAssemblingRecipe recipe) {
        ResourceLocation entity = ResourceLocation.tryParse(recipe.result.entityTypeString);
        return entity == null ? null : snapshot().get(entity);
    }

    private static List<VehicleAssemblingIngredient> buildInputs(List<String> strings) {
        List<VehicleAssemblingIngredient> inputs = new ArrayList<>(strings.size());
        for (String str : strings) {
            VehicleAssemblingIngredient ingredient = new VehicleAssemblingIngredient();
            ingredient.setIngredientString(str);
            // 立即解析：同时落定 ingredientObject 与 count，避免读 count 时尚未触发懒解析。
            ingredient.deserializeFromString(str);
            inputs.add(ingredient);
        }
        return inputs;
    }

    private static VehicleAssemblingRecipe.Category categoryByName(String name) {
        for (VehicleAssemblingRecipe.Category category : VehicleAssemblingRecipe.Category.values()) {
            if (category.typeName.equals(name)) return category;
        }
        return VehicleAssemblingRecipe.Category.MISC;
    }
}
