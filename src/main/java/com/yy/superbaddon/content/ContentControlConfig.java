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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 内容控制的唯一配置来源：{@code config/superbaddon/content_control.json}。
 * 固定 schema，没有同义词、没有目录扫描、没有多形态：
 * <pre>
 * {
 *   "items":    { "superbwarfare:taser": "block_load", "superbwarfare:ak_47": "recipes_only" },
 *   "vehicles": { "superbwarfare:ah_6": "block_load" },
 *   "worldgen_ores": { "superbwarfare": true }
 * }
 * </pre>
 */
public final class ContentControlConfig {
    public static final Path DIRECTORY = FMLPaths.CONFIGDIR.get().resolve(SuperbAddonMod.MODID);
    public static final Path FILE = DIRECTORY.resolve("content_control.json");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private ContentControlConfig() {
    }

    /**
     * 解析后的原始配置。目标 id 已规范化，mode 已是枚举，已剔除 KEEP。
     */
    public record Parsed(Map<ResourceLocation, ContentControlMode> items,
                         Map<ResourceLocation, ContentControlMode> vehicles,
                         Map<ResourceLocation, ContentControlMode> blocks,
                         Set<String> worldgenOreNamespaces) {
    }

    /**
     * 从磁盘读取配置；文件不存在时写出一个空默认文件再返回空配置。
     */
    public static Parsed load() {
        ensureFile();

        Map<ResourceLocation, ContentControlMode> items = new LinkedHashMap<>();
        Map<ResourceLocation, ContentControlMode> vehicles = new LinkedHashMap<>();
        Map<ResourceLocation, ContentControlMode> blocks = new LinkedHashMap<>();
        Set<String> worldgenOres = new LinkedHashSet<>();

        if (Files.isRegularFile(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(reader);
                if (root != null && root.isJsonObject()) {
                    JsonObject object = root.getAsJsonObject();
                    parseTargets(object.get("items"), items);
                    parseTargets(object.get("vehicles"), vehicles);
                    parseTargets(object.get("blocks"), blocks);
                    parseWorldgenOres(object.get("worldgen_ores"), worldgenOres);
                }
            } catch (IOException | RuntimeException exception) {
                SuperbAddonMod.LOGGER.error("Failed to load content control config {}", FILE, exception);
            }
        }

        return new Parsed(Map.copyOf(items), Map.copyOf(vehicles), Map.copyOf(blocks), Set.copyOf(worldgenOres));
    }

    private static void parseTargets(JsonElement element, Map<ResourceLocation, ContentControlMode> out) {
        if (element == null || !element.isJsonObject()) return;
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey().trim().toLowerCase(java.util.Locale.ROOT));
            if (id == null) {
                SuperbAddonMod.LOGGER.warn("Ignoring content control target with invalid id '{}'", entry.getKey());
                continue;
            }
            JsonElement value = entry.getValue();
            if (value == null || !value.isJsonPrimitive()) continue;
            ContentControlMode mode = ContentControlMode.from(value.getAsString());
            if (mode != ContentControlMode.KEEP) out.put(id, mode);
        }
    }

    private static void parseWorldgenOres(JsonElement element, Set<String> out) {
        if (element == null || !element.isJsonObject()) return;
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            JsonElement value = entry.getValue();
            if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean() && value.getAsBoolean()) {
                String namespace = entry.getKey().trim().toLowerCase(java.util.Locale.ROOT);
                if (!namespace.isEmpty()) out.add(namespace);
            }
        }
    }

    private static void ensureFile() {
        try {
            Files.createDirectories(DIRECTORY);
            if (Files.exists(FILE)) return;
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(defaultConfig(), writer);
            }
        } catch (IOException exception) {
            SuperbAddonMod.LOGGER.error("Failed to create content control config {}", FILE, exception);
        }
    }

    private static JsonObject defaultConfig() {
        JsonObject root = new JsonObject();
        root.addProperty("_note", "SuperbAddon content control. key = '<namespace>:<id>', value = keep | recipes_only | block_load. "
                + "recipes_only 只删输出该目标的配方；block_load 还会裁剪资源(模型/纹理/geo/动画/声音/数据)、过滤创造栏、拦截容器与实体生成。"
                + "items=枪械/物品, vehicles=载具实体, blocks=方块(BlockItem 与方块同名, 按方块资源布局裁剪 blockstate/方块模型/方块纹理/gui/掉落表, 不拦截放置与 BlockEntity)。"
                + "vehicle_assembling_table/container 是基础设施，block_load 不会裁剪它们的资源或实体，避免装配台自爆。"
                + "worldgen_ores 按命名空间禁用主模组矿物生成(不改已生成区块、不卸载方块/物品)。");
        root.add("items", new JsonObject());
        root.add("vehicles", new JsonObject());
        root.add("blocks", new JsonObject());

        JsonObject worldgenOres = new JsonObject();
        worldgenOres.addProperty("superbwarfare", false);
        root.add("worldgen_ores", worldgenOres);
        return root;
    }
}
