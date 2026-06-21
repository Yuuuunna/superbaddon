package com.yy.superbaddon.content;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 把一个 block_load 目标 (类型 + id) 映射成需要裁剪的资源过滤器列表。
 * 路径模板写死在这里——这是主模组 SuperbWarfare 的固定资产布局，
 * 不需要也不应该让用户在配置里手写。
 */
public final class ResourcePaths {

    private ResourcePaths() {
    }

    /**
     * 一条资源过滤规则。命名空间必须相同；{@code prefix=true} 时按目录/前缀匹配，
     * 否则要求路径完全相等。
     */
    public record ResourceFilter(String namespace, String path, boolean prefix) {
        public boolean matches(ResourceLocation resource) {
            if (!namespace.equals(resource.getNamespace())) return false;
            String other = resource.getPath();
            if (!prefix) return path.equals(other);
            return other.equals(path)
                    || other.startsWith(path + "/")
                    || other.startsWith(path + ".")
                    || other.startsWith(path + "_");
        }
    }

    /**
     * 枪械/物品被 block_load 时要裁剪的客户端美术资源 + 服务端数据。
     * 配方不在这里裁剪——配方在 RecipeManager 层按输出删除。
     */
    public static List<ResourceFilter> forItem(ResourceLocation id) {
        String ns = id.getNamespace();
        String path = id.getPath();
        List<ResourceFilter> filters = new ArrayList<>();
        exact(filters, ns, "sbw/guns/" + path + ".json");          // 数据 + 资源两端都有
        prefix(filters, ns, "models/item/" + path);
        prefix(filters, ns, "models/displaysettings/" + path);
        prefix(filters, ns, "textures/item/" + path);
        prefix(filters, ns, "textures/gun_icon/" + path);
        prefix(filters, ns, "geo/" + path);
        prefix(filters, ns, "geo/lod/" + path);
        prefix(filters, ns, "animations/" + path);
        return filters;
    }

    /**
     * 载具被 block_load 时要裁剪的客户端美术资源 + 服务端数据。
     */
    public static List<ResourceFilter> forVehicle(ResourceLocation id) {
        String ns = id.getNamespace();
        String path = id.getPath();
        List<ResourceFilter> filters = new ArrayList<>();
        exact(filters, ns, "sbw/vehicles/" + path + ".json");
        prefix(filters, ns, "textures/vehicle_icon/" + path);
        // 0.8.9 旧版载具渲染走 GeckoLib/VehicleModel，后续版本部分资源改成 bedrock 目录。
        // 两套路径都要覆盖，否则只删 sbw/vehicles 数据会留下半截资源引用。
        prefix(filters, ns, "geo/" + path);
        prefix(filters, ns, "geo/lod/" + path);
        prefix(filters, ns, "animations/" + path);
        prefix(filters, ns, "textures/entity/" + path);
        prefix(filters, ns, "textures/bedrock/vehicle/" + path);
        prefix(filters, ns, "textures/bedrock/vehicle_lod/" + path);
        prefix(filters, ns, "models/bedrock/vehicle/" + path);
        prefix(filters, ns, "models/bedrock/vehicle_lod/" + path);
        prefix(filters, ns, "animations/bedrock/vehicle/" + path);
        prefix(filters, ns, "sounds/vehicle/" + path);
        return filters;
    }

    /**
     * 方块被 block_load 时要裁剪的客户端美术资源 + 服务端数据。
     * BlockItem 与方块同名，配方/创造栏/物品判定按 item id 复用，不在这里裁剪。
     * 数据端只裁掉 loot_tables/blocks/<id>：破坏后不掉落，对齐枪械的 sbw/guns 数据裁剪。
     */
    public static List<ResourceFilter> forBlock(ResourceLocation id) {
        String ns = id.getNamespace();
        String path = id.getPath();
        List<ResourceFilter> filters = new ArrayList<>();
        prefix(filters, ns, "blockstates/" + path);
        prefix(filters, ns, "models/block/" + path);
        prefix(filters, ns, "models/item/" + path);
        prefix(filters, ns, "models/bedrock/block/" + path);   // geo 模型
        prefix(filters, ns, "textures/block/" + path);
        prefix(filters, ns, "textures/bedrock/block/" + path); // geo 纹理
        prefix(filters, ns, "textures/item/" + path);
        prefix(filters, ns, "textures/entity/" + path);        // BlockEntity 渲染纹理
        prefix(filters, ns, "textures/gui/" + path);
        exact(filters, ns, "loot_tables/blocks/" + path + ".json");
        return filters;
    }

    private static void exact(List<ResourceFilter> filters, String ns, String path) {
        filters.add(new ResourceFilter(ns, path, false));
    }

    private static void prefix(List<ResourceFilter> filters, String ns, String path) {
        filters.add(new ResourceFilter(ns, path, true));
    }
}
