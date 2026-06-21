package com.yy.superbaddon.content;

/**
 * 由 RecipeManagerMixin 实现：把内容控制屏蔽 + 载具配方覆写应用到 RecipeManager 私有配方表。
 * 唯一调用方是 {@code event/RecipeRewriteEvents} 注册的 reload 监听器，
 * 时机保证在原版 RecipeManager（含 KubeJS 在其 apply 内的全部改写）之后、主线程执行。
 */
public interface RecipeRewriteAccess {

    void superbaddon$rewriteRecipeTable();
}
