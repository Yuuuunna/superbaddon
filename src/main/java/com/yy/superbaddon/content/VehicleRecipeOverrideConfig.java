package com.yy.superbaddon.content;

import com.yy.superbaddon.SuperbAddonMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 载具配方覆写的唯一配置来源：{@code config/superbaddon/vehicle_recipe_override.json}。
 * 固定 schema，按载具实体 id（{@code result.entity}）匹配 {@code superbwarfare:vehicle_assembling} 配方：
 * <pre>
 * {
 *   "groups": {
 *     "cheap_aircraft": {
 *       "vehicles": ["superbwarfare:ah_6", "superbwarfare:a_10a"],
 *       "category": "aircraft",
 *       "inputs": ["8 minecraft:iron_block", "superbwarfare:large_motor"]
 *     }
 *   },
 *   "overrides": {
 *     "superbwarfare:ah_6":  { "inputs": ["4 minecraft:diamond_block"] },
 *     "superbwarfare:tom_6": { "disabled": true }
 *   }
 * }
 * </pre>
 * 规则：单体 overrides 整份替换组规则；一个载具只能属一组（重复归属告警并跳过其组覆写）；
 * 字段各自独立校验，非法字段丢弃并告警，其余字段照常生效。
 */
public final class VehicleRecipeOverrideConfig {
    public static final Path DIRECTORY = FMLPaths.CONFIGDIR.get().resolve(SuperbAddonMod.MODID);
    public static final Path FILE = DIRECTORY.resolve("vehicle_recipe_override.json");

    /** 与主模组 VehicleAssemblingIngredient 完全一致的材料字符串语法。 */
    private static final Pattern INGREDIENT_PATTERN =
            Pattern.compile("^(?<count>(\\d+)?)\\s*(x\\s*)?(?<prefix>#?)(?<id>\\w+:\\S+)$");

    /** 合法的装配台分类，对齐 VehicleAssemblingRecipe.Category.typeName。 */
    private static final Set<String> VALID_CATEGORIES =
            Set.of("land", "defense", "aircraft", "civilian", "water", "misc");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private VehicleRecipeOverrideConfig() {
    }

    /**
     * 覆写规则：三个字段各自独立可选。{@code category}/{@code inputs} 为 null 表示不改动。
     */
    public record OverrideSpec(boolean disabled, String category, List<String> inputs) {
        public boolean hasCategory() {
            return category != null;
        }

        public boolean hasInputs() {
            return inputs != null && !inputs.isEmpty();
        }

        public boolean isMeaningful() {
            return disabled || hasCategory() || hasInputs();
        }
    }

    /**
     * 从磁盘读取配置；文件不存在时写出一个空默认文件再返回空表。
     * 返回的 map 键为载具实体 id，已完成组展开、优先级合并与字段校验。
     */
    public static Map<ResourceLocation, OverrideSpec> load() {
        ensureFile();

        if (!Files.isRegularFile(FILE)) return Map.of();

        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root == null || !root.isJsonObject()) return Map.of();
            JsonObject object = root.getAsJsonObject();

            // 组展开：vehicleId -> 组的原始规则；单体 overrides 整份替换组规则。
            Map<ResourceLocation, RawSpec> merged = resolveGroups(object.get("groups"));
            merged.putAll(parseOverrides(object.get("overrides")));

            Map<ResourceLocation, OverrideSpec> result = new LinkedHashMap<>();
            merged.forEach((id, raw) -> {
                OverrideSpec spec = validate(id, raw);
                if (spec.isMeaningful()) result.put(id, spec);
            });
            return Map.copyOf(result);
        } catch (IOException | RuntimeException exception) {
            SuperbAddonMod.LOGGER.error("Failed to load vehicle recipe override config {}", FILE, exception);
            return Map.of();
        }
    }

    /** 解析前的原始规则，字段未校验。 */
    private record RawSpec(Boolean disabled, String category, List<String> inputs) {
    }

    private static Map<ResourceLocation, RawSpec> resolveGroups(JsonElement element) {
        Map<ResourceLocation, RawSpec> resolved = new LinkedHashMap<>();
        if (element == null || !element.isJsonObject()) return resolved;

        Set<ResourceLocation> conflicted = new LinkedHashSet<>();
        for (Map.Entry<String, JsonElement> groupEntry : element.getAsJsonObject().entrySet()) {
            String groupName = groupEntry.getKey();
            if (groupEntry.getValue() == null || !groupEntry.getValue().isJsonObject()) {
                SuperbAddonMod.LOGGER.warn("Ignoring vehicle override group '{}' with non-object value", groupName);
                continue;
            }
            JsonObject groupObject = groupEntry.getValue().getAsJsonObject();
            RawSpec spec = parseSpec(groupObject);

            for (ResourceLocation id : parseVehicleList(groupObject.get("vehicles"), groupName)) {
                if (conflicted.contains(id)) continue;
                if (resolved.containsKey(id)) {
                    SuperbAddonMod.LOGGER.warn("Vehicle {} belongs to multiple override groups; skipping its group override", id);
                    conflicted.add(id);
                    resolved.remove(id);
                    continue;
                }
                resolved.put(id, spec);
            }
        }
        return resolved;
    }

    private static Map<ResourceLocation, RawSpec> parseOverrides(JsonElement element) {
        Map<ResourceLocation, RawSpec> out = new LinkedHashMap<>();
        if (element == null || !element.isJsonObject()) return out;
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            ResourceLocation id = parseVehicleId(entry.getKey());
            if (id == null) continue;
            if (entry.getValue() == null || !entry.getValue().isJsonObject()) {
                SuperbAddonMod.LOGGER.warn("Ignoring vehicle override '{}' with non-object value", entry.getKey());
                continue;
            }
            out.put(id, parseSpec(entry.getValue().getAsJsonObject()));
        }
        return out;
    }

    private static RawSpec parseSpec(JsonObject object) {
        Boolean disabled = null;
        JsonElement disabledElement = object.get("disabled");
        if (disabledElement != null && disabledElement.isJsonPrimitive() && disabledElement.getAsJsonPrimitive().isBoolean()) {
            disabled = disabledElement.getAsBoolean();
        }

        String category = null;
        JsonElement categoryElement = object.get("category");
        if (categoryElement != null && categoryElement.isJsonPrimitive()) {
            category = categoryElement.getAsString().trim().toLowerCase(Locale.ROOT);
        }

        List<String> inputs = null;
        JsonElement inputsElement = object.get("inputs");
        if (inputsElement != null && inputsElement.isJsonArray()) {
            inputs = new ArrayList<>();
            for (JsonElement item : inputsElement.getAsJsonArray()) {
                if (item != null && item.isJsonPrimitive()) inputs.add(item.getAsString().trim());
            }
        }

        return new RawSpec(disabled, category, inputs);
    }

    private static OverrideSpec validate(ResourceLocation id, RawSpec raw) {
        boolean disabled = raw.disabled() != null && raw.disabled();

        String category = null;
        if (raw.category() != null) {
            if (VALID_CATEGORIES.contains(raw.category())) {
                category = raw.category();
            } else {
                SuperbAddonMod.LOGGER.warn("Dropping invalid category '{}' for vehicle override {}", raw.category(), id);
            }
        }

        List<String> inputs = null;
        if (raw.inputs() != null) {
            if (raw.inputs().isEmpty()) {
                SuperbAddonMod.LOGGER.warn("Dropping empty inputs for vehicle override {} (use \"disabled\": true to disable)", id);
            } else if (allInputsValid(raw.inputs())) {
                inputs = List.copyOf(raw.inputs());
            } else {
                SuperbAddonMod.LOGGER.warn("Dropping inputs for vehicle override {}: contains invalid ingredient string", id);
            }
        }

        return new OverrideSpec(disabled, category, inputs);
    }

    private static boolean allInputsValid(List<String> inputs) {
        for (String input : inputs) {
            if (input == null || !INGREDIENT_PATTERN.matcher(input).matches()) return false;
        }
        return true;
    }

    private static List<ResourceLocation> parseVehicleList(JsonElement element, String groupName) {
        List<ResourceLocation> ids = new ArrayList<>();
        if (element == null || !element.isJsonArray()) {
            SuperbAddonMod.LOGGER.warn("Vehicle override group '{}' has no 'vehicles' array", groupName);
            return ids;
        }
        for (JsonElement item : element.getAsJsonArray()) {
            if (item == null || !item.isJsonPrimitive()) continue;
            ResourceLocation id = parseVehicleId(item.getAsString());
            if (id != null) ids.add(id);
        }
        return ids;
    }

    private static ResourceLocation parseVehicleId(String raw) {
        if (raw == null) return null;
        ResourceLocation id = ResourceLocation.tryParse(raw.trim().toLowerCase(Locale.ROOT));
        if (id == null) SuperbAddonMod.LOGGER.warn("Ignoring vehicle override target with invalid id '{}'", raw);
        return id;
    }

    private static void ensureFile() {
        try {
            Files.createDirectories(DIRECTORY);
            if (Files.exists(FILE)) return;
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(defaultConfig(), writer);
            }
        } catch (IOException exception) {
            SuperbAddonMod.LOGGER.error("Failed to create vehicle recipe override config {}", FILE, exception);
        }
    }

    private static JsonObject defaultConfig() {
        JsonObject root = new JsonObject();
        root.addProperty("_note", "SuperbAddon 载具配方覆写。按载具实体 id 匹配 superbwarfare:vehicle_assembling 配方。"
                + "字段：disabled(禁用装配)、category(land|defense|aircraft|civilian|water|misc)、inputs(整份替换材料表，语法同主模组配方，如 '3 #superbwarfare:storage_blocks/steel')。"
                + "groups：vehicles 列出成员，组内共用同一份覆写；一个载具只能属一组。"
                + "overrides：单体覆写，整份替换其所属组的规则。三个字段各自可选。");
        root.add("groups", new JsonObject());
        root.add("overrides", new JsonObject());
        return root;
    }
}
