package com.yy.superbaddon.event;

import com.yy.superbaddon.SuperbAddonMod;
import com.yy.superbaddon.content.ContentControlManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = SuperbAddonMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ContentControlRuntimeEvents {
    private ContentControlRuntimeEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity == null) return;

        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (!ContentControlManager.blocksVehicleEntity(id)) return;

        event.setCanceled(true);
        entity.discard();
        SuperbAddonMod.LOGGER.debug("[content-control] blocked vehicle entity join: {}", id);
    }
}
