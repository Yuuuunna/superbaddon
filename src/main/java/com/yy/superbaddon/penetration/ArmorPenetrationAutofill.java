package com.yy.superbaddon.penetration;

import com.atsuishio.superbwarfare.data.CustomData;
import com.atsuishio.superbwarfare.data.gun.DefaultGunData;
import com.atsuishio.superbwarfare.data.gun.ProjectileInfo;
import com.atsuishio.superbwarfare.data.vehicle.DefaultVehicleData;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ArmorPenetrationAutofill {
    private static final String GENERIC_PROJECTILE = "superbwarfare:projectile";

    private ArmorPenetrationAutofill() {
    }

    public record ScanResult(
            Map<ResourceLocation, Float> partial,
            Set<ResourceLocation> fullBypass,
            int vehicleCount,
            int weaponCount,
            int skippedGenericWeaponCount
    ) {
    }

    private record WeaponDefault(float penetration, boolean fullBypass) {
    }

    public static ScanResult scanVehicleWeapons() {
        LinkedHashMap<ResourceLocation, Float> partial = new LinkedHashMap<>();
        LinkedHashSet<ResourceLocation> fullBypass = new LinkedHashSet<>();
        int vehicleCount = 0;
        int weaponCount = 0;
        int skippedGeneric = 0;

        for (Map.Entry<String, DefaultVehicleData> vehicleEntry : CustomData.VEHICLE_DATA.entrySet()) {
            DefaultVehicleData vehicleData = vehicleEntry.getValue();
            if (vehicleData == null) {
                continue;
            }

            vehicleCount++;
            for (Map.Entry<String, DefaultGunData> weaponEntry : vehicleData.weapons().entrySet()) {
                DefaultGunData weaponData = weaponEntry.getValue();
                if (weaponData == null) {
                    continue;
                }

                weaponCount++;
                ProjectileInfo projectileInfo = weaponData.projectile();
                String projectileRawId = projectileInfo == null ? "" : projectileInfo.getItemId();
                ResourceLocation projectileId = parseResourceLocation(projectileRawId);
                ResourceLocation weaponId = parseResourceLocation(weaponData.getId());
                WeaponDefault weaponDefault = classifyWeapon(
                        vehicleEntry.getKey(),
                        weaponEntry.getKey(),
                        projectileRawId,
                        weaponData
                );
                if (weaponDefault == null) {
                    continue;
                }

                boolean wrote = false;
                if (weaponId != null) {
                    writeDefault(weaponId, weaponDefault, partial, fullBypass);
                    wrote = true;
                }
                if (projectileId != null && !GENERIC_PROJECTILE.equals(projectileId.toString())) {
                    writeDefault(projectileId, weaponDefault, partial, fullBypass);
                    wrote = true;
                }
                if (!wrote) {
                    skippedGeneric++;
                }
            }
        }

        return new ScanResult(Map.copyOf(partial), Set.copyOf(fullBypass), vehicleCount, weaponCount, skippedGeneric);
    }

    private static void writeDefault(
            ResourceLocation id,
            WeaponDefault weaponDefault,
            Map<ResourceLocation, Float> partial,
            Set<ResourceLocation> fullBypass
    ) {
        if (weaponDefault.fullBypass()) {
            partial.remove(id);
            fullBypass.add(id);
        } else if (!fullBypass.contains(id)) {
            partial.merge(id, weaponDefault.penetration(), Math::max);
        }
    }

    @Nullable
    private static WeaponDefault classifyWeapon(
            String vehicleId,
            String weaponKey,
            String projectileId,
            DefaultGunData weaponData
    ) {
        float configuredBypass = ArmorPenetrationRules.clampPenetration((float) weaponData.getBypassesArmor());
        if (ArmorPenetrationRules.isFullBypass(configuredBypass)) {
            return new WeaponDefault(1.0F, true);
        }

        String text = normalize(vehicleId + " " + weaponKey + " " + projectileId + " "
                + safe(weaponData.getName()) + " " + safe(weaponData.getShellType()) + " "
                + weaponData.getGunType());
        String projectilePath = normalize(projectileId);

        if (isFullBypassWeapon(projectilePath, text, weaponData)) {
            return new WeaponDefault(1.0F, true);
        }
        if (configuredBypass > 0.0F) {
            return new WeaponDefault(configuredBypass, false);
        }
        if (isAutocannon(projectilePath, text)) {
            return new WeaponDefault(0.80F, false);
        }
        if (containsAny(text, "14_5", "14.5", "145mm", "14mm")) {
            return new WeaponDefault(0.55F, false);
        }
        if (containsAny(text, "12_7", "12.7", "127mm", "50_cal", ".50", "m2hb")) {
            return new WeaponDefault(0.50F, false);
        }
        if (containsAny(text, "7_62", "7.62", "762mm", "5_8", "5.8", "58mm",
                "machine_gun", "coax", "coaxial", "mg")) {
            return new WeaponDefault(0.35F, false);
        }

        return null;
    }

    private static boolean isFullBypassWeapon(String projectilePath, String text, DefaultGunData weaponData) {
        if ("cannon_shell".equals(projectilePath) || "mortar_shell".equals(projectilePath)) {
            return true;
        }
        if (containsAny(text, "105mm", "120mm", "125mm", "rocket", "missile", "tow", "agm", "akd",
                "pl_8", "pl_12", "fb_10", "fim_92", "javelin", "igla", "bomb", "nuke", "nuclear")) {
            return true;
        }
        return weaponData.damage >= 250.0D
                || weaponData.getExplosionDamage() >= 80.0D
                || weaponData.getExplosionRadius() >= 8.0D;
    }

    private static boolean isAutocannon(String projectilePath, String text) {
        return "small_cannon_shell".equals(projectilePath)
                || containsAny(text, "20mm", "20_mm", "25mm", "25_mm", "30mm", "30_mm",
                "35mm", "35_mm", "40mm", "40_mm", "autocannon", "auto_cannon");
    }

    private static boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return safe(value).toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static String safe(@Nullable String value) {
        return value == null ? "" : value;
    }

    @Nullable
    private static ResourceLocation parseResourceLocation(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new ResourceLocation(raw.trim());
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
