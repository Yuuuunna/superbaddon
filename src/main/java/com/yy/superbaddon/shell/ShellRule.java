package com.yy.superbaddon.shell;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ShellRule {
    private final ResourceLocation id;
    private final int priority;
    private final Target target;
    private final AmmoOverrideSpec ammoOverride;
    private final List<AmmoEjection> ammoEjections;
    private final ShellEjectionSpec defaultEjection;

    private ShellRule(
            ResourceLocation id,
            int priority,
            Target target,
            AmmoOverrideSpec ammoOverride,
            List<AmmoEjection> ammoEjections,
            ShellEjectionSpec defaultEjection
    ) {
        this.id = id;
        this.priority = priority;
        this.target = target;
        this.ammoOverride = ammoOverride == null ? AmmoOverrideSpec.disabled() : ammoOverride;
        this.ammoEjections = List.copyOf(ammoEjections == null ? List.of() : ammoEjections);
        this.defaultEjection = defaultEjection;
    }

    public ResourceLocation id() {
        return id;
    }

    public int priority() {
        return priority;
    }

    public Set<String> guns() {
        return target.guns();
    }

    public Set<String> vehicles() {
        return target.vehicles();
    }

    public Set<String> weapons() {
        return target.weapons();
    }

    public Set<Integer> seats() {
        return target.seats();
    }

    public AmmoOverrideSpec ammoOverride() {
        return ammoOverride;
    }

    public List<AmmoOption> ammoOptions() {
        return ammoOverride.allowed();
    }

    public boolean applyAllowedAmmo() {
        return ammoOverride.enabled();
    }

    public boolean replaceAllowedAmmo() {
        return ammoOverride.replace();
    }

    public boolean canApplyAmmoOptions() {
        return ammoOverride.active() && (!guns().isEmpty() || !vehicles().isEmpty());
    }

    public boolean matches(ShellContext context) {
        return target.matches(context) && selectedEjection(context).isPresent();
    }

    public Optional<ShellEjectionSpec> selectedEjection(ShellContext context) {
        for (AmmoEjection entry : ammoEjections) {
            if (entry.ammo().matches(context.ammoId(), context.ammoSpec())) {
                return Optional.of(entry.ejection());
            }
        }

        if (defaultEjection != null) {
            return Optional.of(defaultEjection);
        }

        return singleAllowedAmmoFallback(context);
    }

    private Optional<ShellEjectionSpec> singleAllowedAmmoFallback(ShellContext context) {
        if (ammoEjections.size() != 1) return Optional.empty();
        if (!ammoOverride.active() || ammoOverride.allowed().size() != 1) return Optional.empty();

        AmmoOption allowed = ammoOverride.allowed().get(0);
        if (!allowed.matches(context.ammoId(), context.ammoSpec())) return Optional.empty();

        return Optional.of(ammoEjections.get(0).ejection());
    }

    public boolean matchesWeaponName(String vehicleId, String weaponName) {
        return target.matchesWeaponName(vehicleId, weaponName);
    }

    public static ShellRule fromJson(ResourceLocation id, JsonObject json) {
        JsonObject targetObject = GsonHelper.getAsJsonObject(json, "target", new JsonObject());
        JsonObject ammoOverrideObject = GsonHelper.getAsJsonObject(json, "ammo_override", new JsonObject());
        JsonObject ejectionObject = GsonHelper.getAsJsonObject(json, "ejection", new JsonObject());

        int priority = GsonHelper.getAsInt(json, "priority", 0);
        Target target = Target.fromJson(targetObject);
        AmmoOverrideSpec ammoOverride = AmmoOverrideSpec.fromJson(ammoOverrideObject);
        EjectionTable ejections = EjectionTable.fromJson(ejectionObject);

        return new ShellRule(id, priority, target, ammoOverride, ejections.byAmmo(), ejections.defaultEjection());
    }

    public static String normalizeId(String raw) {
        if (raw == null) return "";
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) return "";
        if (!value.contains(":")) return value;
        ResourceLocation location = ResourceLocation.tryParse(value);
        return location == null ? value : location.toString();
    }

    public static String weaponId(String vehicleId, String weaponName) {
        if (vehicleId == null || vehicleId.isBlank() || weaponName == null || weaponName.isBlank()) return "";
        return normalizeId(vehicleId + "/" + weaponName);
    }

    private record AmmoEjection(AmmoOption ammo, ShellEjectionSpec ejection) {
    }

    private record EjectionTable(List<AmmoEjection> byAmmo, ShellEjectionSpec defaultEjection) {
        private static EjectionTable fromJson(JsonObject object) {
            if (object == null || object.size() == 0) return new EjectionTable(List.of(), null);

            ArrayList<AmmoEjection> byAmmo = new ArrayList<>();
            if (object.has("by_ammo") && object.get("by_ammo").isJsonObject()) {
                JsonObject table = object.getAsJsonObject("by_ammo");
                for (Map.Entry<String, JsonElement> entry : table.entrySet()) {
                    AmmoOption ammo = AmmoOption.fromString(entry.getKey());
                    if (ammo.matchKeys().isEmpty()) continue;
                    if (!entry.getValue().isJsonObject()) continue;
                    byAmmo.add(new AmmoEjection(ammo, ShellEjectionSpec.fromJson(entry.getValue().getAsJsonObject())));
                }
            }

            ShellEjectionSpec defaultEjection = null;
            if (object.has("default") && object.get("default").isJsonObject()) {
                defaultEjection = ShellEjectionSpec.fromJson(object.getAsJsonObject("default"));
            }
            return new EjectionTable(List.copyOf(byAmmo), defaultEjection);
        }
    }

    private record Target(
            Set<String> guns,
            Set<String> projectiles,
            Set<String> vehicles,
            Set<String> weapons,
            Set<Integer> seats,
            boolean requiresVehicle,
            int minProjectilesPerShot
    ) {
        private static Target fromJson(JsonObject object) {
            return new Target(
                    readIdSet(object, "guns"),
                    readIdSet(object, "projectiles"),
                    readIdSet(object, "vehicles"),
                    readIdSet(object, "weapons"),
                    readIntSet(object, "seats"),
                    GsonHelper.getAsBoolean(object, "requires_vehicle", false),
                    GsonHelper.getAsInt(object, "min_projectiles_per_shot", 0)
            );
        }

        private boolean matches(ShellContext context) {
            if (requiresVehicle && isBlank(context.vehicleId())) return false;
            if (minProjectilesPerShot > 0 && context.projectileAmount() < minProjectilesPerShot) return false;
            if (!guns.isEmpty() && !guns.contains(context.gunId())) return false;
            if (!projectiles.isEmpty() && !projectiles.contains(context.projectileId())) return false;
            if (!vehicles.isEmpty() && !vehicles.contains(context.vehicleId())) return false;
            if (!seats.isEmpty() && !seats.contains(context.seatIndex())) return false;
            return weapons.isEmpty() || matchesWeapon(context);
        }

        private boolean matchesWeapon(ShellContext context) {
            return matchesWeaponName(context.vehicleId(), context.weaponName()) || weapons.contains(context.weaponId());
        }

        private boolean matchesWeaponName(String vehicleId, String weaponName) {
            if (weapons.isEmpty()) return true;
            String name = normalizeId(weaponName);
            if (!name.isBlank() && weapons.contains(name)) return true;
            String combined = weaponId(vehicleId, name);
            return !combined.isBlank() && weapons.contains(combined);
        }
    }

    private static Set<String> readIdSet(JsonObject object, String key) {
        if (!object.has(key)) return Collections.emptySet();

        LinkedHashSet<String> result = new LinkedHashSet<>();
        JsonElement element = object.get(key);
        if (element.isJsonPrimitive()) {
            addId(result, element.getAsString());
        } else if (element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                if (item.isJsonPrimitive()) addId(result, item.getAsString());
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static Set<Integer> readIntSet(JsonObject object, String key) {
        if (!object.has(key)) return Collections.emptySet();

        LinkedHashSet<Integer> result = new LinkedHashSet<>();
        JsonElement element = object.get(key);
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            result.add(element.getAsInt());
        } else if (element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                if (item.isJsonPrimitive() && item.getAsJsonPrimitive().isNumber()) {
                    result.add(item.getAsInt());
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static void addId(Set<String> output, String raw) {
        String id = normalizeId(raw);
        if (!id.isBlank()) output.add(id);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
