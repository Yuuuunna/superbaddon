package com.yy.superbaddon.compat;

import com.yy.superbaddon.SuperbAddonMod;
import com.yy.superbaddon.shell.AmmoOption;
import com.yy.superbaddon.shell.ShellRule;
import com.atsuishio.superbwarfare.data.CustomData;
import com.atsuishio.superbwarfare.data.DataLoader;
import com.atsuishio.superbwarfare.data.ObjectToList;
import com.atsuishio.superbwarfare.data.StringToObject;
import com.atsuishio.superbwarfare.data.gun.AmmoConsumer;
import com.atsuishio.superbwarfare.data.gun.DefaultGunData;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.vehicle.DefaultVehicleData;
import com.atsuishio.superbwarfare.data.vehicle.subdata.SeatInfo;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SuperbWarfareDataPatcher {
    private SuperbWarfareDataPatcher() {
    }

    public static int applyAmmoOverrides(Collection<ShellRule> rules) {
        Map<TargetKey, AmmoPatch> patches = collectPatches(rules);
        int changed = 0;

        for (Map.Entry<TargetKey, AmmoPatch> entry : patches.entrySet()) {
            TargetKey key = entry.getKey();
            AmmoPatch patch = entry.getValue();
            if (key.kind == TargetKind.GUN) {
                DefaultGunData data = getGunData(key.ownerId);
                if (data == null) continue;
                if (applyPatch(data, patch) > 0) {
                    clearDefaultGunDataCaches(data);
                    changed++;
                }
            } else if (applyVehicleWeaponPatch(key, patch) > 0) {
                changed++;
            }
        }

        if (changed > 0) {
            invalidateAllMainModCaches();
            SuperbAddonMod.LOGGER.info("Applied {} shell ejection ammo consumer changes to {} weapon targets", changed, patches.size());
        }
        return changed;
    }

    private static Map<TargetKey, AmmoPatch> collectPatches(Collection<ShellRule> rules) {
        LinkedHashMap<TargetKey, AmmoPatch> patches = new LinkedHashMap<>();
        for (ShellRule rule : rules) {
            if (!rule.canApplyAmmoOptions()) continue;
            collectGunPatches(patches, rule);
            collectVehiclePatches(patches, rule);
        }
        return patches;
    }

    private static void collectGunPatches(Map<TargetKey, AmmoPatch> patches, ShellRule rule) {
        for (String gunId : rule.guns()) {
            TargetKey key = TargetKey.gun(gunId);
            patches.computeIfAbsent(key, ignored -> new AmmoPatch()).add(rule);
        }
    }

    private static void collectVehiclePatches(Map<TargetKey, AmmoPatch> patches, ShellRule rule) {
        for (String vehicleId : rule.vehicles()) {
            DefaultVehicleData vehicleData = getVehicleData(vehicleId);
            if (vehicleData == null) continue;

            Map<String, DefaultGunData> weapons = vehicleData.weapons();
            if (weapons.isEmpty()) continue;

            LinkedHashSet<String> candidates = vehicleWeaponCandidates(vehicleId, vehicleData, weapons.keySet(), rule);
            for (String weapon : candidates) {
                TargetKey key = TargetKey.vehicleWeapon(vehicleId, weapon);
                patches.computeIfAbsent(key, ignored -> new AmmoPatch()).add(rule);
            }
        }
    }

    private static DefaultGunData getGunData(String gunId) {
        DefaultGunData data = CustomData.GUN_DATA.get(gunId);
        if (data == null && gunId.contains(":")) data = CustomData.GUN_DATA.get(gunId.substring(gunId.indexOf(':') + 1));
        return data;
    }

    private static DefaultVehicleData getVehicleData(String vehicleId) {
        DefaultVehicleData data = CustomData.VEHICLE_DATA.get(vehicleId);
        if (data == null && vehicleId.contains(":")) data = CustomData.VEHICLE_DATA.get(vehicleId.substring(vehicleId.indexOf(':') + 1));
        return data;
    }

    private static LinkedHashSet<String> vehicleWeaponCandidates(String vehicleId, DefaultVehicleData vehicleData, Set<String> weaponNames, ShellRule rule) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();

        if (!rule.seats().isEmpty()) {
            List<SeatInfo> seats = vehicleData.seats();
            for (Integer seatIndex : rule.seats()) {
                if (seatIndex == null || seatIndex < 0 || seatIndex >= seats.size()) continue;
                SeatInfo seat = seats.get(seatIndex);
                for (String weapon : seat.weapons()) {
                    if (weapon != null && weaponNames.contains(weapon)) candidates.add(weapon);
                }
            }
        } else {
            candidates.addAll(weaponNames);
        }

        if (!rule.weapons().isEmpty()) {
            candidates.removeIf(weapon -> !rule.matchesWeaponName(vehicleId, weapon));
        }
        return candidates;
    }

    private static int applyPatch(DefaultGunData data, AmmoPatch patch) {
        ObjectToList<StringToObject<AmmoConsumer>> ammoConsumers = data.getAmmoConsumers();
        if (ammoConsumers == null) {
            ammoConsumers = new ObjectToList<>();
            data.setAmmoConsumers(ammoConsumers);
        }

        int before = ammoConsumers.list.size();
        if (patch.hasReplace()) {
            ammoConsumers.list.clear();
            addAmmoOptions(ammoConsumers, patch.replacements());
        }
        addAmmoOptions(ammoConsumers, patch.appends());
        return ammoConsumers.list.size() == before && !patch.hasReplace() ? 0 : 1;
    }

    private static int applyVehicleWeaponPatch(TargetKey key, AmmoPatch patch) {
        DefaultVehicleData vehicleData = getVehicleData(key.ownerId);
        if (vehicleData == null) return 0;

        Map<String, JsonObject> rawWeapons = rawVehicleWeapons(vehicleData);
        JsonObject rawWeapon = rawWeapons.get(key.weaponName);
        if (rawWeapon == null) return 0;

        JsonArray ammoType = buildAmmoType(rawWeapon.get("AmmoType"), patch);
        rawWeapon.add("AmmoType", ammoType);

        clearDefaultVehicleDataCaches(vehicleData);
        return 1;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, JsonObject> rawVehicleWeapons(DefaultVehicleData vehicleData) {
        try {
            Field field = DefaultVehicleData.class.getDeclaredField("weapons");
            field.setAccessible(true);
            Object raw = field.get(vehicleData);
            if (!(raw instanceof Map<?, ?> rawMap)) return Map.of();

            LinkedHashMap<String, JsonObject> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() instanceof String key && entry.getValue() instanceof JsonObject value) {
                    result.put(key, value);
                }
            }
            return result;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            SuperbAddonMod.LOGGER.warn("Failed to access SuperbWarfare vehicle weapon raw data for shell ammo override", exception);
            return Map.of();
        }
    }

    private static JsonArray buildAmmoType(JsonElement existingElement, AmmoPatch patch) {
        LinkedHashMap<String, JsonElement> elements = new LinkedHashMap<>();
        if (!patch.hasReplace()) addExistingAmmoElements(elements, existingElement);
        addAmmoElements(elements, patch.replacements());
        addAmmoElements(elements, patch.appends());

        JsonArray array = new JsonArray();
        for (JsonElement element : elements.values()) array.add(element);
        return array;
    }

    private static void addExistingAmmoElements(LinkedHashMap<String, JsonElement> output, JsonElement element) {
        if (element == null || element.isJsonNull()) return;
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) addExistingAmmoElement(output, child);
        } else {
            addExistingAmmoElement(output, element);
        }
    }

    private static void addExistingAmmoElement(LinkedHashMap<String, JsonElement> output, JsonElement element) {
        if (element == null || element.isJsonNull()) return;
        AmmoOption option = AmmoOption.fromJson(element);
        String key = ammoOptionKey(option);
        if (key.isBlank()) key = "raw/" + output.size();
        output.putIfAbsent(key, element.deepCopy());
    }

    private static void addAmmoElements(LinkedHashMap<String, JsonElement> output, List<AmmoOption> options) {
        for (AmmoOption option : options) {
            String key = ammoOptionKey(option);
            if (!key.isBlank()) output.putIfAbsent(key, option.toConsumerElement());
        }
    }

    private static String ammoOptionKey(AmmoOption option) {
        if (option == null) return "";
        if (!option.spec().isBlank()) return option.spec();
        return option.ammoId();
    }

    private static void addAmmoOptions(ObjectToList<StringToObject<AmmoConsumer>> ammoConsumers, List<AmmoOption> options) {
        LinkedHashSet<String> existing = existingAmmoKeys(ammoConsumers);
        for (AmmoOption option : options) {
            if (!isNewAmmo(existing, option)) continue;
            AmmoConsumer consumer = createConsumer(option);
            if (consumer == null) continue;
            ammoConsumers.list.add(new StringToObject<>(consumer));
            existing.addAll(ammoKeys(consumer));
            existing.addAll(option.matchKeys());
        }
    }

    private static boolean isNewAmmo(Set<String> existing, AmmoOption option) {
        for (String key : option.matchKeys()) {
            if (existing.contains(key)) return false;
        }
        return true;
    }

    private static LinkedHashSet<String> existingAmmoKeys(ObjectToList<StringToObject<AmmoConsumer>> ammoConsumers) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (StringToObject<AmmoConsumer> entry : ammoConsumers.list) {
            if (entry == null || entry.value == null) continue;
            result.addAll(ammoKeys(entry.value));
        }
        return result;
    }

    private static LinkedHashSet<String> ammoKeys(AmmoConsumer consumer) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        try {
            if (!consumer.initialized()) consumer.init();
        } catch (RuntimeException ignored) {
        }

        try {
            if (consumer.getAmmo() != null) {
                String spec = AmmoOption.normalizeAmmoSpec(consumer.getAmmo());
                if (!spec.isBlank()) result.add(spec);
                String id = AmmoOption.extractAmmoId(spec);
                if (!id.isBlank()) result.add(id);
            }
        } catch (RuntimeException ignored) {
        }

        try {
            ItemStack stack = consumer.stack();
            ResourceLocation id = stack == null || stack.isEmpty() || stack.getItem() == Items.AIR
                    ? null
                    : ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id != null) result.add(id.toString());
        } catch (RuntimeException ignored) {
        }
        return result;
    }

    private static AmmoConsumer createConsumer(AmmoOption option) {
        try {
            JsonObject consumerJson = option.consumerJson();
            AmmoConsumer consumer;
            if (consumerJson != null) {
                consumer = DataLoader.GSON.fromJson(consumerJson, AmmoConsumer.class);
                if ((consumer.getAmmo() == null || consumer.getAmmo().isBlank()) && !option.spec().isBlank()) {
                    consumer.setAmmo(option.spec());
                }
                consumer.init();
            } else {
                consumer = new AmmoConsumer();
                consumer.deserializeFromString(!option.spec().isBlank() ? option.spec() : option.ammoId());
            }
            return consumer;
        } catch (RuntimeException | LinkageError exception) {
            SuperbAddonMod.LOGGER.warn("Failed to create ammo consumer for shell rule ammo {}", option.spec(), exception);
            return null;
        }
    }

    private static void clearDefaultGunDataCaches(DefaultGunData data) {
        clearField(data, "ammoConsumersCache");
        try {
            DataLoader.JSON_OBJECT_CACHE.invalidate(data);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private static void clearDefaultVehicleDataCaches(DefaultVehicleData data) {
        clearField(data, "processedWeapons");
        try {
            DataLoader.JSON_OBJECT_CACHE.invalidate(data);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private static void clearField(Object target, String fieldName) {
        if (target == null) return;
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, null);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static void invalidateAllMainModCaches() {
        try {
            GunData.DATA_CACHE.invalidateAll();
        } catch (RuntimeException | LinkageError ignored) {
        }
        invalidateVehicleDataCache();
        try {
            DataLoader.JSON_OBJECT_CACHE.invalidateAll();
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private static void invalidateVehicleDataCache() {
        try {
            Class<?> vehicleData = Class.forName("com.atsuishio.superbwarfare.data.vehicle.VehicleData");
            Field field = vehicleData.getDeclaredField("dataCache");
            field.setAccessible(true);
            Object cache = field.get(null);
            cache.getClass().getMethod("invalidateAll").invoke(cache);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private enum TargetKind {
        GUN,
        VEHICLE_WEAPON
    }

    private record TargetKey(TargetKind kind, String ownerId, String weaponName) {
        private static TargetKey gun(String gunId) {
            return new TargetKey(TargetKind.GUN, ShellRule.normalizeId(gunId), "");
        }

        private static TargetKey vehicleWeapon(String vehicleId, String weaponName) {
            return new TargetKey(TargetKind.VEHICLE_WEAPON, ShellRule.normalizeId(vehicleId), weaponName);
        }
    }

    private static final class AmmoPatch {
        private int replacePriority = Integer.MIN_VALUE;
        private final LinkedHashMap<String, AmmoOption> replacements = new LinkedHashMap<>();
        private final LinkedHashMap<String, AmmoOption> appends = new LinkedHashMap<>();

        private void add(ShellRule rule) {
            if (rule.replaceAllowedAmmo()) {
                if (rule.priority() > replacePriority) {
                    replacePriority = rule.priority();
                    replacements.clear();
                }
                if (rule.priority() == replacePriority) {
                    addAll(replacements, rule.ammoOptions());
                }
            } else {
                addAll(appends, rule.ammoOptions());
            }
        }

        private boolean hasReplace() {
            return replacePriority != Integer.MIN_VALUE;
        }

        private List<AmmoOption> replacements() {
            return new ArrayList<>(replacements.values());
        }

        private List<AmmoOption> appends() {
            return new ArrayList<>(appends.values());
        }

        private static void addAll(LinkedHashMap<String, AmmoOption> output, List<AmmoOption> options) {
            for (AmmoOption option : options) {
                String key = option.spec().isBlank() ? option.ammoId() : option.spec();
                if (!key.isBlank()) output.putIfAbsent(key, option);
            }
        }
    }
}
