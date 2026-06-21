package com.yy.superbaddon.content;

import com.yy.superbaddon.SuperbAddonMod;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

import java.util.concurrent.atomic.AtomicBoolean;

public record WorldgenOreCondition(String namespace) implements ICondition {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final ResourceLocation ID = new ResourceLocation(SuperbAddonMod.MODID, "worldgen_ores");

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) CraftingHelper.register(Serializer.INSTANCE);
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        ContentControlManager.reload();
        boolean enabled = ContentControlManager.disablesWorldgenOreNamespace(namespace);
        SuperbAddonMod.LOGGER.info("Worldgen ore removal condition for namespace {} = {}", namespace, enabled);
        return enabled;
    }

    private enum Serializer implements IConditionSerializer<WorldgenOreCondition> {
        INSTANCE;

        @Override
        public void write(JsonObject json, WorldgenOreCondition value) {
            json.addProperty("namespace", value.namespace());
        }

        @Override
        public WorldgenOreCondition read(JsonObject json) {
            return new WorldgenOreCondition(GsonHelper.getAsString(json, "namespace", "superbwarfare"));
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }
    }
}
