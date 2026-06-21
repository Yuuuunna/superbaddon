package com.yy.superbaddon.registry;

import com.yy.superbaddon.SuperbAddonMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ShellEjectionItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SuperbAddonMod.MODID);

    public static final RegistryObject<Item> BRASS_CASING = item("brass_casing");
    public static final RegistryObject<Item> SHOTGUN_HULL = item("shotgun_hull");
    public static final RegistryObject<Item> HEAVY_CASING = item("heavy_casing");
    public static final RegistryObject<Item> AUTOCANNON_CASING = item("autocannon_casing");
    public static final RegistryObject<Item> LARGE_SHELL_STUB = item("large_shell_stub");

    private ShellEjectionItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private static RegistryObject<Item> item(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties()));
    }
}
