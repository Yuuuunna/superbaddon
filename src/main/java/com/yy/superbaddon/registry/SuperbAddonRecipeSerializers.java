package com.yy.superbaddon.registry;

import com.yy.superbaddon.SuperbAddonMod;
import com.yy.superbaddon.recipe.ShellReloadingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class SuperbAddonRecipeSerializers {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, SuperbAddonMod.MODID);

    public static final RegistryObject<RecipeSerializer<ShellReloadingRecipe>> SHELL_RELOADING =
            RECIPE_SERIALIZERS.register("shell_reloading", ShellReloadingRecipe.Serializer::new);

    private SuperbAddonRecipeSerializers() {
    }

    public static void register(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
    }
}
