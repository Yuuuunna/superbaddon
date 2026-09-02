package com.yy.superbaddon.penetration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yy.superbaddon.SuperbAddonMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class ArmorPenetrationConfig {
    public static final Path DIRECTORY = FMLPaths.CONFIGDIR.get().resolve(SuperbAddonMod.MODID);
    public static final Path FILE = DIRECTORY.resolve("armor_penetration.json");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static Parsed parsed;

    private ArmorPenetrationConfig() {
    }

    public record Parsed(
            boolean enabled,
            Map<ResourceLocation, Float> armorPenetrationByWeapon,
            Set<ResourceLocation> fullArmorBypassWeapons,
            ArmorPenetrationRules.ExplosionFalloff explosionFalloff
    ) {
    }

    public record WeaponArmorPenetrationUpdate(int partialAdded, int fullBypassAdded) {
        public boolean changed() {
            return this.partialAdded > 0 || this.fullBypassAdded > 0;
        }
    }

    public static synchronized Parsed get() {
        if (parsed == null) {
            parsed = load();
        }
        return parsed;
    }

    public static synchronized void reload() {
        parsed = load();
    }

    public static boolean enabled() {
        return get().enabled();
    }

    public static float getArmorPenetration(ResourceLocation weaponId) {
        if (!enabled()) {
            return 0.0F;
        }
        return get().armorPenetrationByWeapon().getOrDefault(weaponId, 0.0F);
    }

    public static boolean isFullArmorBypassWeapon(ResourceLocation weaponId) {
        return enabled() && get().fullArmorBypassWeapons().contains(weaponId);
    }

    public static ArmorPenetrationRules.ExplosionFalloff explosionFalloff() {
        return get().explosionFalloff();
    }

    public static int partialCount() {
        return get().armorPenetrationByWeapon().size();
    }

    public static int fullBypassCount() {
        return get().fullArmorBypassWeapons().size();
    }

    private static Parsed load() {
        ensureFile();
        JsonObject root = readConfigRoot();

        boolean enabled = bool(root, "enabled", true);
        Map<ResourceLocation, Float> partial = new LinkedHashMap<>();
        Set<ResourceLocation> full = new LinkedHashSet<>();
        parsePartial(root.get("armor_penetration_by_weapon"), partial);
        parsePartial(root.get("armorPenetrationByWeapon"), partial);
        parseFull(root.get("full_armor_bypass_weapons"), full);
        parseFull(root.get("fullArmorBypassWeapons"), full);

        ArmorPenetrationRules.ExplosionFalloff falloff = parseExplosionFalloff(root);
        return new Parsed(enabled, Collections.unmodifiableMap(partial), Collections.unmodifiableSet(full), falloff);
    }

    public static synchronized WeaponArmorPenetrationUpdate addMissingWeaponArmorPenetration(
            Map<ResourceLocation, Float> partialDefaults,
            Set<ResourceLocation> fullBypassDefaults
    ) {
        ensureFile();
        JsonObject root = readConfigRoot();

        Map<ResourceLocation, Float> currentPartial = new LinkedHashMap<>();
        Set<ResourceLocation> currentFullBypass = new LinkedHashSet<>();
        parsePartial(root.get("armor_penetration_by_weapon"), currentPartial);
        parsePartial(root.get("armorPenetrationByWeapon"), currentPartial);
        parseFull(root.get("full_armor_bypass_weapons"), currentFullBypass);
        parseFull(root.get("fullArmorBypassWeapons"), currentFullBypass);

        Set<ResourceLocation> configured = new LinkedHashSet<>();
        configured.addAll(currentPartial.keySet());
        configured.addAll(currentFullBypass);

        int fullAdded = 0;
        for (ResourceLocation weaponId : fullBypassDefaults) {
            if (configured.add(weaponId)) {
                currentFullBypass.add(weaponId);
                fullAdded++;
            }
        }

        int partialAdded = 0;
        for (Map.Entry<ResourceLocation, Float> entry : partialDefaults.entrySet()) {
            ResourceLocation weaponId = entry.getKey();
            if (configured.add(weaponId)) {
                currentPartial.put(weaponId, ArmorPenetrationRules.clampPenetration(entry.getValue()));
                partialAdded++;
            }
        }

        if (partialAdded > 0 || fullAdded > 0) {
            root.add("armor_penetration_by_weapon", partialObject(currentPartial));
            root.add("full_armor_bypass_weapons", fullArray(currentFullBypass));
            writeConfigRoot(root);
            parsed = load();
        }

        return new WeaponArmorPenetrationUpdate(partialAdded, fullAdded);
    }

    private static ArmorPenetrationRules.ExplosionFalloff parseExplosionFalloff(JsonObject root) {
        JsonObject explosion = object(root, "explosion");
        float minimumOuter = penetrationNumber(explosion, "minimum_outer_penetration",
                penetrationNumber(root, "explosionMinArmorPenetration", 0.35F));
        float inner = penetrationNumber(explosion, "inner_radius_factor",
                penetrationNumber(root, "explosionInnerRadiusFactor", 0.35F));
        float power = floatNumber(explosion, "outer_falloff_power",
                floatNumber(root, "explosionOuterFalloffPower", 2.0F));

        return new ArmorPenetrationRules.ExplosionFalloff(
                ArmorPenetrationRules.clampPenetration(minimumOuter),
                ArmorPenetrationRules.clampPenetration(inner),
                Math.max(power, 0.1F)
        );
    }

    private static void parsePartial(JsonElement element, Map<ResourceLocation, Float> target) {
        if (element == null || element.isJsonNull()) {
            return;
        }

        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                ResourceLocation id = parseResourceLocation(entry.getKey());
                Float value = parsePenetrationElement(entry.getValue());
                if (id != null && value != null) {
                    target.put(id, value);
                }
            }
            return;
        }

        if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                if (entry == null || !entry.isJsonPrimitive()) {
                    continue;
                }
                String[] parts = entry.getAsString().split("=", 2);
                if (parts.length != 2) {
                    continue;
                }
                ResourceLocation id = parseResourceLocation(parts[0].trim());
                Float value = ArmorPenetrationRules.parsePenetrationValue(parts[1]).orElse(null);
                if (id != null && value != null) {
                    target.put(id, value);
                }
            }
        }
    }

    private static void parseFull(JsonElement element, Set<ResourceLocation> target) {
        if (element == null || element.isJsonNull()) {
            return;
        }

        if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                if (entry == null || !entry.isJsonPrimitive()) {
                    continue;
                }
                ResourceLocation id = parseResourceLocation(entry.getAsString());
                if (id != null) {
                    target.add(id);
                }
            }
            return;
        }

        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (!entry.getValue().isJsonPrimitive() || entry.getValue().getAsBoolean()) {
                    ResourceLocation id = parseResourceLocation(entry.getKey());
                    if (id != null) {
                        target.add(id);
                    }
                }
            }
        }
    }

    private static Float parsePenetrationElement(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        if (element.getAsJsonPrimitive().isNumber()) {
            return ArmorPenetrationRules.clampPenetration(element.getAsFloat());
        }
        return ArmorPenetrationRules.parsePenetrationValue(element.getAsString()).orElse(null);
    }

    private static ResourceLocation parseResourceLocation(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new ResourceLocation(raw.trim());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static JsonObject readConfigRoot() {
        if (Files.isRegularFile(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                JsonElement element = JsonParser.parseReader(reader);
                if (element != null && element.isJsonObject()) {
                    return element.getAsJsonObject();
                }
            } catch (IOException | RuntimeException exception) {
                SuperbAddonMod.LOGGER.error("Failed to load armor penetration config {}", FILE, exception);
            }
        }
        return defaultConfig();
    }

    private static void writeConfigRoot(JsonObject root) {
        try {
            Files.createDirectories(DIRECTORY);
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException | RuntimeException exception) {
            SuperbAddonMod.LOGGER.error("Failed to save armor penetration config {}", FILE, exception);
        }
    }

    private static void ensureFile() {
        try {
            Files.createDirectories(DIRECTORY);
            if (!Files.exists(FILE)) {
                writeConfigRoot(defaultConfig());
            }
        } catch (IOException exception) {
            SuperbAddonMod.LOGGER.error("Failed to create armor penetration config {}", FILE, exception);
        }
    }

    private static JsonObject defaultConfig() {
        JsonObject root = new JsonObject();
        root.addProperty("_note", "SuperbAddon armor penetration for SuperbWarfare vehicle weapons. "
                + "Keys are projectile/entity/gun ids. Values accept 0.35 or 35%; values are clamped to 0..1. "
                + "Near 1.0 is treated as full bypass. Existing user entries are not overwritten by autofill.");
        root.addProperty("enabled", true);

        Map<ResourceLocation, Float> partial = new LinkedHashMap<>();
        partial.put(new ResourceLocation("superbwarfare:small_cannon_shell"), 0.80F);
        partial.put(new ResourceLocation("superbwarfare:grapeshot"), 0.35F);
        root.add("armor_penetration_by_weapon", partialObject(partial));

        Set<ResourceLocation> full = new LinkedHashSet<>();
        full.add(new ResourceLocation("superbwarfare:cannon_shell"));
        full.add(new ResourceLocation("superbwarfare:small_rocket"));
        full.add(new ResourceLocation("superbwarfare:medium_rocket"));
        full.add(new ResourceLocation("superbwarfare:rpg_rocket_standard"));
        full.add(new ResourceLocation("superbwarfare:rpg_rocket_tbg"));
        full.add(new ResourceLocation("superbwarfare:wire_guide_missile"));
        full.add(new ResourceLocation("superbwarfare:javelin_missile"));
        full.add(new ResourceLocation("superbwarfare:agm_65"));
        full.add(new ResourceLocation("superbwarfare:kh_39"));
        full.add(new ResourceLocation("superbwarfare:igla_9k38_missile"));
        full.add(new ResourceLocation("superbwarfare:ru_9m336_missile"));
        full.add(new ResourceLocation("superbwarfare:swarm_drone"));
        full.add(new ResourceLocation("superbwarfare:mk_82"));
        full.add(new ResourceLocation("superbwarfare:sc_50"));
        full.add(new ResourceLocation("superbwarfare:sc_250"));
        full.add(new ResourceLocation("superbwarfare:mortar_shell"));
        root.add("full_armor_bypass_weapons", fullArray(full));

        JsonObject explosion = new JsonObject();
        explosion.addProperty("minimum_outer_penetration", 0.35F);
        explosion.addProperty("inner_radius_factor", 0.35F);
        explosion.addProperty("outer_falloff_power", 2.0F);
        root.add("explosion", explosion);
        return root;
    }

    private static JsonObject partialObject(Map<ResourceLocation, Float> partial) {
        JsonObject object = new JsonObject();
        for (Map.Entry<ResourceLocation, Float> entry : partial.entrySet()) {
            object.addProperty(entry.getKey().toString(), ArmorPenetrationRules.formatPenetrationValue(entry.getValue()));
        }
        return object;
    }

    private static JsonArray fullArray(Set<ResourceLocation> full) {
        JsonArray array = new JsonArray();
        for (ResourceLocation id : full) {
            array.add(id.toString());
        }
        return array;
    }

    private static JsonObject object(JsonObject root, String key) {
        if (root == null) {
            return null;
        }
        JsonElement element = root.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static boolean bool(JsonObject root, String key, boolean fallback) {
        JsonElement element = root.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsBoolean();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static float penetrationNumber(JsonObject root, String key, float fallback) {
        if (root == null) {
            return fallback;
        }
        JsonElement element = root.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        Float parsed = parsePenetrationElement(element);
        return parsed == null ? fallback : parsed;
    }

    private static float floatNumber(JsonObject root, String key, float fallback) {
        if (root == null) {
            return fallback;
        }
        JsonElement element = root.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsFloat();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }
}
