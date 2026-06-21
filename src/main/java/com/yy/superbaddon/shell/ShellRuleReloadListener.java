package com.yy.superbaddon.shell;

import com.yy.superbaddon.SuperbAddonMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ShellRuleReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();

    public ShellRuleReloadListener() {
        super(GSON, "shell_ejection");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
        List<ShellRule> loaded = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonElement element = entry.getValue();
            try {
                loaded.addAll(ShellRuleParser.parse(id, element));
            } catch (RuntimeException exception) {
                SuperbAddonMod.LOGGER.error("Failed to load shell ejection rule {}", id, exception);
            }
        }

        ShellRuleSet.replaceDataPack(loaded);
        List<ShellRule> external = ExternalShellRuleLoader.loadExternalRules();
        ShellRuleSet.replaceExternal(external);
        int ammoConsumers = ShellAmmoRuleApplier.apply(ShellRuleSet.snapshot());
        SuperbAddonMod.LOGGER.info("Loaded {} shell ejection rules: {} datapack, {} external config, {} ammo consumers applied",
                ShellRuleSet.size(), ShellRuleSet.dataPackSize(), ShellRuleSet.externalSize(), ammoConsumers);
    }
}
