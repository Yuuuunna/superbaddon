package com.yy.superbaddon;

import com.yy.superbaddon.content.ContentControlManager;
import com.yy.superbaddon.content.VehicleRecipeOverrideManager;
import com.yy.superbaddon.compat.mts.MtsCompatConfig;
import com.yy.superbaddon.content.WorldgenOreCondition;
import com.yy.superbaddon.registry.ShellEjectionItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(SuperbAddonMod.MODID)
public final class SuperbAddonMod {

    public static final String MODID = "superbaddon";
    public static final Logger LOGGER = LogManager.getLogger(SuperbAddonMod.class);

    public SuperbAddonMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ShellEjectionItems.register(modBus);
        WorldgenOreCondition.register();
        ContentControlManager.reload();
        VehicleRecipeOverrideManager.reload();
        MtsCompatConfig.reload();
        LOGGER.info("SuperbAddon shell ejection loaded.");
    }
}
