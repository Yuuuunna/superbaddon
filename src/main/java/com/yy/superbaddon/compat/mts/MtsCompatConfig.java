package com.yy.superbaddon.compat.mts;

import com.yy.superbaddon.SuperbAddonMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Optional MinecraftTransportSimulator / Immersive Vehicles compatibility.
 *
 * <p>Do not reference MTS classes here.  This addon must keep loading when MTS is absent.</p>
 */
public final class MtsCompatConfig {
    public static final Path DIRECTORY = FMLPaths.CONFIGDIR.get().resolve(SuperbAddonMod.MODID);
    public static final Path FILE = DIRECTORY.resolve("mts_compat.json");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static Parsed parsed;

    private MtsCompatConfig() {
    }

    public record Parsed(
            boolean enabled,
            boolean jerrycanEnabled,
            boolean fuelPumpEnabled,
            boolean allowNbtOnlyJerrycanDetection,
            int defaultEnergyPerBucket,
            int pumpMbPerTick,
            int pumpSearchRadius,
            Map<String, Integer> fluidEnergyPerBucket,
            Set<ResourceLocation> jerrycanItemAllowlist,
            boolean bridgeControls
    ) {
    }

    public static synchronized Parsed get() {
        if (parsed == null) parsed = load();
        return parsed;
    }

    public static synchronized void reload() {
        parsed = load();
    }

    public static boolean enabled() {
        return get().enabled();
    }

    public static boolean jerrycanEnabled() {
        Parsed config = get();
        return config.enabled() && config.jerrycanEnabled();
    }

    public static boolean fuelPumpEnabled() {
        Parsed config = get();
        return config.enabled() && config.fuelPumpEnabled();
    }

    /** Persisted default for the SuperbWarfare control bridge (toggled at runtime by the keybind). */
    public static boolean bridgeControls() {
        return get().bridgeControls();
    }

    /**
     * Writes back just {@code controls.bridge_controls}, preserving every other user setting, and
     * updates the in-memory cache so the toggle survives a restart. A failed write still updates the
     * cache so the current session reflects the toggle.
     */
    public static synchronized void setBridgeControls(boolean value) {
        Parsed current = get();
        try {
            ensureFile();
            JsonObject root;
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                JsonElement element = JsonParser.parseReader(reader);
                root = element != null && element.isJsonObject() ? element.getAsJsonObject() : defaultConfig();
            }
            JsonObject controls = object(root, "controls");
            if (controls == null) {
                controls = new JsonObject();
                root.add("controls", controls);
            }
            controls.addProperty("bridge_controls", value);
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException | RuntimeException exception) {
            SuperbAddonMod.LOGGER.error("Failed to persist MTS bridge toggle to {}", FILE, exception);
        }
        parsed = new Parsed(
                current.enabled(), current.jerrycanEnabled(), current.fuelPumpEnabled(),
                current.allowNbtOnlyJerrycanDetection(), current.defaultEnergyPerBucket(),
                current.pumpMbPerTick(), current.pumpSearchRadius(),
                current.fluidEnergyPerBucket(), current.jerrycanItemAllowlist(), value);
    }

    public static int pumpMbPerTick() {
        return Math.max(1, get().pumpMbPerTick());
    }

    public static int pumpSearchRadius() {
        return Math.max(32, get().pumpSearchRadius());
    }

    /**
     * The single fuel test.  A fluid is fuel only when {@code fluid_energy_per_bucket} lists it with
     * a positive value; everything else -- water, milk, an unknown modded fluid, or an entry the pack
     * author deliberately set to 0 -- is not fuel and must never be drained.  This mirrors MTS's own
     * {@code JSONConfigSettings.Fuel.fuels}, where absence from the map means "not a fuel".
     *
     * <p>{@code default_energy_per_bucket} is the escape hatch for servers that really do want every
     * fluid to burn; it defaults to 0 (off).</p>
     *
     * @return the energy one bucket is worth, or empty when the fluid is not fuel
     */
    public static OptionalInt fuelEnergyPerBucket(String fluid) {
        Parsed config = get();
        Integer listed = config.fluidEnergyPerBucket().get(normalizeFluid(fluid));
        int perBucket = listed != null ? listed : config.defaultEnergyPerBucket();
        return perBucket > 0 ? OptionalInt.of(perBucket) : OptionalInt.empty();
    }

    /**
     * Energy for {@code mb} of fluid, rounded <em>down</em>.  Rounding up would hand out energy that
     * was never paid for in fluid; rounding to nearest can return 0 for a small amount that a caller
     * has already drained.  Callers must treat a 0 return as "do not drain".
     */
    public static int energyForMb(String fluid, double mb) {
        int perBucket = fuelEnergyPerBucket(fluid).orElse(0);
        if (mb <= 0 || perBucket <= 0) return 0;
        long energy = (long) Math.floor(perBucket * (mb / 1000.0D));
        if (energy <= 0) return 0;
        return (int) Math.min(Integer.MAX_VALUE, energy);
    }

    /**
     * Millibuckets to drain for at most {@code energy}, rounded <em>down</em> so the drained fluid is
     * always fully paid for.  Returns 0 when a single millibucket would already be worth more energy
     * than the target can take.
     */
    public static int mbForEnergyFloor(String fluid, int energy) {
        int perBucket = fuelEnergyPerBucket(fluid).orElse(0);
        if (energy <= 0 || perBucket <= 0) return 0;
        return (int) Math.min(Integer.MAX_VALUE, (long) Math.floor(energy * 1000.0D / perBucket));
    }

    public static boolean isAllowedJerrycan(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        Parsed config = get();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id != null) {
            if (config.jerrycanItemAllowlist().contains(id)) return true;
            if ("mts".equals(id.getNamespace())) return true;
        }

        String itemClass = stack.getItem().getClass().getName().toLowerCase(Locale.ROOT);
        if (itemClass.contains("minecrafttransportsimulator") || itemClass.contains("immersivevehicles")) return true;

        if (config.allowNbtOnlyJerrycanDetection()) return true;

        return false;
    }

    private static Parsed load() {
        ensureFile();

        boolean enabled = true;
        boolean jerrycanEnabled = true;
        boolean fuelPumpEnabled = true;
        boolean allowNbtOnlyJerrycanDetection = true;
        int defaultEnergyPerBucket = 0;
        int pumpMbPerTick = 10;
        int pumpSearchRadius = 32;
        boolean bridgeControls = false;
        Map<String, Integer> fluids = defaultFluidMap();
        Set<ResourceLocation> jerrycanItems = new LinkedHashSet<>();

        if (Files.isRegularFile(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                JsonElement rootElement = JsonParser.parseReader(reader);
                if (rootElement != null && rootElement.isJsonObject()) {
                    JsonObject root = rootElement.getAsJsonObject();
                    enabled = bool(root, "enabled", enabled);
                    defaultEnergyPerBucket = nonNegativeInt(root, "default_energy_per_bucket", defaultEnergyPerBucket);

                    JsonObject jerrycan = object(root, "jerrycan");
                    if (jerrycan != null) {
                        jerrycanEnabled = bool(jerrycan, "enabled", jerrycanEnabled);
                        allowNbtOnlyJerrycanDetection = bool(jerrycan, "allow_nbt_only_detection", allowNbtOnlyJerrycanDetection);
                        parseItemAllowlist(jerrycan.get("allowed_items"), jerrycanItems);
                    }

                    JsonObject fuelPump = object(root, "fuel_pump");
                    if (fuelPump != null) {
                        fuelPumpEnabled = bool(fuelPump, "enabled", fuelPumpEnabled);
                        pumpMbPerTick = positiveInt(fuelPump, "mb_per_tick", pumpMbPerTick);
                        pumpSearchRadius = positiveInt(fuelPump, "search_radius", pumpSearchRadius);
                    }

                    JsonObject controls = object(root, "controls");
                    if (controls != null) {
                        bridgeControls = bool(controls, "bridge_controls", bridgeControls);
                    }

                    JsonObject fluidMap = object(root, "fluid_energy_per_bucket");
                    if (fluidMap != null) {
                        fluids.clear();
                        for (Map.Entry<String, JsonElement> entry : fluidMap.entrySet()) {
                            JsonElement value = entry.getValue();
                            if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                                fluids.put(normalizeFluid(entry.getKey()), Math.max(0, value.getAsInt()));
                            }
                        }
                    }
                }
            } catch (IOException | RuntimeException exception) {
                SuperbAddonMod.LOGGER.error("Failed to load MTS compat config {}", FILE, exception);
            }
        }

        if (defaultEnergyPerBucket > 0) {
            SuperbAddonMod.LOGGER.warn("MTS compat: default_energy_per_bucket={} -- every fluid not listed in "
                    + "fluid_energy_per_bucket (water included) will be accepted as fuel. Set it to 0 to allow only "
                    + "the listed fluids.", defaultEnergyPerBucket);
        }

        return new Parsed(
                enabled,
                jerrycanEnabled,
                fuelPumpEnabled,
                allowNbtOnlyJerrycanDetection,
                defaultEnergyPerBucket,
                pumpMbPerTick,
                pumpSearchRadius,
                Map.copyOf(fluids),
                Set.copyOf(jerrycanItems),
                bridgeControls
        );
    }

    private static void parseItemAllowlist(JsonElement element, Set<ResourceLocation> out) {
        if (element == null || !element.isJsonArray()) return;
        for (JsonElement item : element.getAsJsonArray()) {
            if (item == null || !item.isJsonPrimitive()) continue;
            ResourceLocation id = ResourceLocation.tryParse(item.getAsString().trim().toLowerCase(Locale.ROOT));
            if (id != null) out.add(id);
        }
    }

    private static Map<String, Integer> defaultFluidMap() {
        // SuperbWarfare vehicles store energy in the millions (e.g. the AH-6 has MaxEnergy 5,000,000
        // and burns hundreds of FE per tick).  One MTS bucket must therefore be worth a meaningful
        // slice of that tank, not a few tens of thousands.  Over-large values are harmless: the
        // vehicle's energy storage clamps the intake to its capacity.
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("lava", 5_000_000);
        map.put("fuel", 7_500_000);
        map.put("gasoline", 8_750_000);
        map.put("diesel", 10_000_000);
        map.put("biodiesel", 7_500_000);
        return map;
    }

    /**
     * MTS tank fluid names carry no namespace ({@code EntityFluidTank.getFluid()} returns "lava",
     * the owning mod lives in a separate field), so strip any namespace a config author typed and
     * key everything by path.  One form, one lookup, no fallback chain.
     */
    private static String normalizeFluid(String fluid) {
        if (fluid == null) return "";
        String normalized = fluid.trim().toLowerCase(Locale.ROOT);
        int colon = normalized.indexOf(':');
        return colon >= 0 && colon + 1 < normalized.length() ? normalized.substring(colon + 1) : normalized;
    }

    private static JsonObject object(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()
                ? element.getAsBoolean()
                : fallback;
    }

    private static int nonNegativeInt(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) return fallback;
        return Math.max(0, element.getAsInt());
    }

    private static int positiveInt(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) return fallback;
        return Math.max(1, element.getAsInt());
    }

    private static void ensureFile() {
        try {
            Files.createDirectories(DIRECTORY);
            if (Files.exists(FILE)) return;
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(defaultConfig(), writer);
            }
        } catch (IOException exception) {
            SuperbAddonMod.LOGGER.error("Failed to create MTS compat config {}", FILE, exception);
        }
    }

    private static JsonObject defaultConfig() {
        JsonObject root = new JsonObject();
        root.addProperty("_note", "Optional MinecraftTransportSimulator / Immersive Vehicles compatibility. No hard dependency is declared. "
                + "Jerrycan consumes the MTS jerrycanFluid NBT and charges SuperbWarfare vehicles through Forge ENERGY. "
                + "Fuel pump drains the MTS pump tank reflectively and charges the nearest SuperbWarfare vehicle.");
        root.addProperty("_note_fuels", "fluid_energy_per_bucket is a whitelist: only the fluids listed here with a "
                + "positive value can fuel a vehicle. Anything absent (water, milk, modded fluids) or set to 0 is "
                + "rejected and never drained. Set default_energy_per_bucket above 0 only if you want EVERY fluid to "
                + "burn at that rate. Use MTS fluid names without a namespace, e.g. \"lava\", not \"minecraft:lava\".");
        root.addProperty("enabled", true);
        root.addProperty("default_energy_per_bucket", 0);

        JsonObject jerrycan = new JsonObject();
        jerrycan.addProperty("enabled", true);
        jerrycan.addProperty("allow_nbt_only_detection", true);
        jerrycan.add("allowed_items", GSON.toJsonTree(new String[0]));
        root.add("jerrycan", jerrycan);

        JsonObject fuelPump = new JsonObject();
        fuelPump.addProperty("enabled", true);
        fuelPump.addProperty("mb_per_tick", 10);
        fuelPump.addProperty("search_radius", 32);
        root.add("fuel_pump", fuelPump);

        JsonObject controls = new JsonObject();
        controls.addProperty("bridge_controls", false);
        root.add("controls", controls);

        JsonObject fluids = new JsonObject();
        defaultFluidMap().forEach(fluids::addProperty);
        root.add("fluid_energy_per_bucket", fluids);
        return root;
    }
}
