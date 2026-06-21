package com.yy.superbaddon.content;

import com.yy.superbaddon.SuperbAddonMod;
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe;
import com.yy.superbaddon.content.ResourcePaths.ResourceFilter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.registries.ForgeRegistries;
import com.yy.superbaddon.mixin.IngredientAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 内容控制查询层。持有一份不可变 {@link State}，所有钩子（mixin / 事件）只通过这里提问。
 * 配方删除、资源裁剪、创造栏过滤、容器/实体拦截全部从 items/vehicles 两张表按固定规则派生，
 * 不读任何用户手写的资源/配方列表。
 */
public final class ContentControlManager {

    private static final AtomicBoolean LOADED = new AtomicBoolean(false);
    private static volatile State state = State.EMPTY;

    /**
     * 这些不是可裁剪内容，而是 SuperbWarfare 自己的基础设施。
     * 把它们当普通 vehicle/item 裁掉会导致装配台、容器渲染器和所有载具容器一起炸。
     * 这里按 path 保护，原因很现实：SBW 附属包经常在自己的 namespace 下提供同名资源。
     */
    private static final Set<String> PROTECTED_INFRASTRUCTURE_PATHS = Set.of(
            "vehicle_assembling_table",
            "container"
    );

    private ContentControlManager() {
    }

    // ---- 生命周期 ----

    public static synchronized State reload() {
        ContentControlConfig.Parsed parsed = ContentControlConfig.load();

        List<ResourceFilter> filters = new ArrayList<>();
        parsed.items().forEach((id, mode) -> {
            if (!mode.blocksLoad()) return;
            if (isProtectedInfrastructureId(id)) {
                SuperbAddonMod.LOGGER.warn("Ignoring block_load resource pruning for protected infrastructure item {}", id);
                return;
            }
            filters.addAll(ResourcePaths.forItem(id));
        });
        parsed.vehicles().forEach((id, mode) -> {
            if (!mode.blocksLoad()) return;
            if (isProtectedInfrastructureId(id)) {
                SuperbAddonMod.LOGGER.warn("Ignoring block_load resource pruning for protected infrastructure vehicle {}", id);
                return;
            }
            filters.addAll(ResourcePaths.forVehicle(id));
        });
        parsed.blocks().forEach((id, mode) -> {
            if (!mode.blocksLoad()) return;
            if (isProtectedInfrastructureId(id)) {
                SuperbAddonMod.LOGGER.warn("Ignoring block_load resource pruning for protected infrastructure block {}", id);
                return;
            }
            filters.addAll(ResourcePaths.forBlock(id));
        });

        state = new State(parsed.items(), parsed.vehicles(), parsed.blocks(), parsed.worldgenOreNamespaces(), List.copyOf(filters));
        LOADED.set(true);

        SuperbAddonMod.LOGGER.info("Loaded SuperbAddon content control: {} items, {} vehicles, {} blocks, {} resource filters, {} disabled worldgen ore namespaces",
                state.items().size(), state.vehicles().size(), state.blocks().size(), state.resourceFilters().size(), state.worldgenOreNamespaces().size());
        SuperbAddonMod.LOGGER.debug("[content-control] items={}, vehicles={}, blocks={}, worldgenOres={}", state.items(), state.vehicles(), state.blocks(), state.worldgenOreNamespaces());
        return state;
    }

    public static State snapshot() {
        if (!LOADED.get()) reload();
        return state;
    }

    public static void ensureLoaded() {
        if (!LOADED.get()) reload();
    }

    // ---- 查询：配方 ----

    /**
     * 配方是否应被移除：直接看反序列化后 Recipe 的实际输出/必要输入。
     * 不读原始 JSON——重写发生在 reload 监听器阶段，最终配方表（含 KubeJS 改写）才是唯一数据源。
     */
    public static boolean shouldRemoveRecipe(ResourceLocation recipeId, Recipe<?> recipe) {
        if (recipe == null) return false;

        if (recipe instanceof VehicleAssemblingRecipe vehicleRecipe) {
            ResourceLocation vehicleId = ResourceLocation.tryParse(vehicleRecipe.result.entityTypeString);
            if (vehicleId != null && disablesRecipeOutput(snapshot(), vehicleId)) return true;
        }

        try {
            if (blocksItemStack(recipe.getResultItem(RegistryAccess.EMPTY))) return true;
        } catch (RuntimeException | LinkageError exception) {
            SuperbAddonMod.LOGGER.debug("[content-control] failed to inspect recipe result {}", recipeId, exception);
        }

        try {
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredientRequiresBlockedStack(ingredient)) return true;
            }
        } catch (RuntimeException | LinkageError exception) {
            SuperbAddonMod.LOGGER.debug("[content-control] failed to inspect recipe ingredients {}", recipeId, exception);
        }

        return false;
    }

    // ---- 查询：资源 ----

    public static boolean blocksResource(ResourceLocation id) {
        if (id == null) return false;
        for (ResourceFilter filter : snapshot().resourceFilters()) {
            if (filter.matches(id)) return true;
        }
        return false;
    }

    // ---- 查询：物品 / 容器 / 实体 ----

    public static boolean blocksItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        State snapshot = snapshot();

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (isProtectedInfrastructureId(itemId)) return false;

        ContentControlMode itemMode = itemMode(snapshot, itemId);
        if (itemMode != null && itemMode.blocksLoad()) return true;

        ContentControlMode blockMode = snapshot.blocks().get(itemId);
        if (blockMode != null && blockMode.blocksLoad()) return true;

        ResourceLocation vehicleId = containerEntityType(stack);
        if (vehicleId == null || isProtectedInfrastructureId(vehicleId)) return false;
        ContentControlMode vehicleMode = snapshot.vehicles().get(vehicleId);
        return vehicleMode != null && vehicleMode.blocksLoad();
    }

    public static boolean blocksVehicleEntity(ResourceLocation entityTypeId) {
        if (entityTypeId == null || isProtectedInfrastructureId(entityTypeId)) return false;
        ContentControlMode mode = snapshot().vehicles().get(entityTypeId);
        return mode != null && mode.blocksLoad();
    }

    public static boolean isProtectedInfrastructureId(ResourceLocation id) {
        return id != null && PROTECTED_INFRASTRUCTURE_PATHS.contains(id.getPath());
    }

    // ---- 查询：worldgen ----

    public static boolean disablesWorldgenOreNamespace(String namespace) {
        if (namespace == null) return false;
        return snapshot().worldgenOreNamespaces().contains(namespace.trim().toLowerCase(Locale.ROOT));
    }

    // ---- 内部派生逻辑 ----

    private static boolean disablesRecipeOutput(State snapshot, ResourceLocation output) {
        ContentControlMode itemMode = itemMode(snapshot, output);
        if (itemMode != null && itemMode.disablesRecipes()) return true;
        ContentControlMode vehicleMode = snapshot.vehicles().get(output);
        if (vehicleMode != null && vehicleMode.disablesRecipes()) return true;
        ContentControlMode blockMode = snapshot.blocks().get(output);
        return blockMode != null && blockMode.disablesRecipes();
    }

    /**
     * 物品的控制档位：先精确匹配，再把 {@code <gun>_blueprint} 归到 {@code <gun>} 的档位下。
     */
    private static ContentControlMode itemMode(State snapshot, ResourceLocation itemId) {
        if (itemId == null) return null;
        ContentControlMode mode = snapshot.items().get(itemId);
        if (mode != null) return mode;

        String path = itemId.getPath();
        if (!path.endsWith("_blueprint")) return null;
        ResourceLocation gunId = ResourceLocation.tryParse(
                itemId.getNamespace() + ":" + path.substring(0, path.length() - "_blueprint".length()));
        return gunId == null ? null : snapshot.items().get(gunId);
    }

    private static ResourceLocation containerEntityType(ItemStack stack) {
        CompoundTag tag = BlockItem.getBlockEntityData(stack);
        if (tag == null || !tag.contains("EntityType")) return null;
        return ResourceLocation.tryParse(tag.getString("EntityType"));
    }

    private static boolean ingredientRequiresBlockedStack(Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return false;

        Ingredient.Value[] values = ((IngredientAccessor) ingredient).superbaddon$getValues();
        if (values == null || values.length == 0) return false;

        boolean hasItemValue = false;
        for (Ingredient.Value value : values) {
            if (value instanceof Ingredient.TagValue) {
                // 如果存在标签，由于我们无法在 Reload 阶段解析出其绑定的项目（强行解析会污染缓存为 Barrier），
                // 且一般性标签（如 forge:rods/wooden）含有大量未禁用项目，因此整个 Ingredient 不属于“只包含被禁用项目”。
                return false;
            }
            if (value instanceof Ingredient.ItemValue) {
                hasItemValue = true;
            }
        }

        if (!hasItemValue) return false;

        for (Ingredient.Value value : values) {
            for (ItemStack stack : value.getItems()) {
                if (!blocksItemStack(stack)) return false;
            }
        }
        return true;
    }

    // ---- 状态 ----

    public record State(Map<ResourceLocation, ContentControlMode> items,
                        Map<ResourceLocation, ContentControlMode> vehicles,
                        Map<ResourceLocation, ContentControlMode> blocks,
                        Set<String> worldgenOreNamespaces,
                        List<ResourceFilter> resourceFilters) {
        static final State EMPTY = new State(Map.of(), Map.of(), Map.of(), Set.of(), List.of());
    }
}
