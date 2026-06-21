package com.yy.superbaddon.content;

import java.util.Locale;

/**
 * 按目标 ID 控制主模组内容的禁用级别。只有三档，没有同义词。
 * <ul>
 *     <li>{@link #KEEP} 完全不干预。</li>
 *     <li>{@link #RECIPES_ONLY} 只移除输出该目标的配方，不裁剪资源、不阻止加载。</li>
 *     <li>{@link #BLOCK_LOAD} 彻底禁用：移除配方 + 裁剪资源 + 过滤创造栏 + 拦截容器/实体。</li>
 * </ul>
 */
public enum ContentControlMode {
    KEEP,
    RECIPES_ONLY,
    BLOCK_LOAD;

    public boolean disablesRecipes() {
        return this != KEEP;
    }

    public boolean blocksLoad() {
        return this == BLOCK_LOAD;
    }

    /**
     * 解析配置里的 mode 字符串。只认这三个 token，其余一律 {@link #KEEP}。
     */
    public static ContentControlMode from(String raw) {
        if (raw == null) return KEEP;
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "recipes_only" -> RECIPES_ONLY;
            case "block_load" -> BLOCK_LOAD;
            default -> KEEP;
        };
    }
}
