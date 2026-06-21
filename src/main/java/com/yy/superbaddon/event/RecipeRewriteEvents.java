package com.yy.superbaddon.event;

import com.yy.superbaddon.SuperbAddonMod;
import com.yy.superbaddon.content.RecipeRewriteAccess;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 配方重写的挂载点。不能注入 RecipeManager 的方法：KubeJS 有配方脚本时会在 apply 的
 * HEAD 处 cancel（RETURN 注入永远不执行），且其写回是字段直赋、不经过 replaceRecipes。
 * AddReloadListenerEvent 追加的监听器排在原版 RecipeManager 之后串行 apply，
 * 此时拿到的必然是 KubeJS 等改写完成后的最终配方表。
 */
@Mod.EventBusSubscriber(modid = SuperbAddonMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RecipeRewriteEvents {

    private RecipeRewriteEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener((ResourceManagerReloadListener) manager ->
                ((RecipeRewriteAccess) event.getServerResources().getRecipeManager()).superbaddon$rewriteRecipeTable());
    }
}
