package com.yy.superbaddon.event;

import com.yy.superbaddon.SuperbAddonMod;
import com.yy.superbaddon.content.ContentControlManager;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;

@Mod.EventBusSubscriber(modid = SuperbAddonMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ContentControlEvents {
    private ContentControlEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        Iterator<Map.Entry<ItemStack, CreativeModeTab.TabVisibility>> iterator = event.getEntries().iterator();
        while (iterator.hasNext()) {
            if (ContentControlManager.blocksItemStack(iterator.next().getKey())) iterator.remove();
        }
    }
}
