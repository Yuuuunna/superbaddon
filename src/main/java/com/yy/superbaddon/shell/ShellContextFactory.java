package com.yy.superbaddon.shell;

import com.atsuishio.superbwarfare.data.gun.AmmoConsumer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.data.gun.ShootParameters;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

public final class ShellContextFactory {
    private ShellContextFactory() {
    }

    public static Optional<ShellContext> fromShootParameters(ShootParameters parameters) {
        if (parameters == null) return Optional.empty();
        GunData data = parameters.data;
        Entity shooter = parameters.shooter;
        if (data == null || shooter == null) return Optional.empty();

        Entity storageTarget = storageTarget(shooter, parameters.ammoSupplier);
        String vehicleId = entityId(storageTarget);
        int seatIndex = seatIndex(storageTarget, shooter);
        String weaponName = weaponName(storageTarget, data, seatIndex);
        String weaponId = weaponId(vehicleId, weaponName);

        return Optional.of(new ShellContext(
                parameters.level,
                shooter,
                storageTarget,
                itemId(data.stack()),
                ammoId(data),
                ammoSpec(data),
                projectileId(data),
                vehicleId,
                weaponId,
                weaponName,
                seatIndex,
                parameters.shootPosition,
                parameters.shootDirection,
                intProp(data, GunProp.PROJECTILE_AMOUNT, 1)
        ));
    }

    private static Entity storageTarget(Entity shooter, Entity ammoSupplier) {
        if (ammoSupplier instanceof VehicleEntity) return ammoSupplier;
        Entity root = shooter.getRootVehicle();
        return root == null ? shooter : root;
    }

    private static int seatIndex(Entity storageTarget, Entity shooter) {
        if (!(storageTarget instanceof VehicleEntity vehicle)) return -1;
        try {
            return vehicle.getSeatIndex(shooter);
        } catch (RuntimeException ignored) {
            return -1;
        }
    }

    private static String weaponName(Entity storageTarget, GunData data, int seatIndex) {
        if (!(storageTarget instanceof VehicleEntity vehicle)) return "";

        try {
            String selected = ShellRule.normalizeId(vehicle.getGunName(seatIndex));
            if (!selected.isBlank()) return selected;
        } catch (RuntimeException ignored) {
        }

        return weaponNameByData(vehicle, data);
    }

    private static String weaponNameByData(VehicleEntity vehicle, GunData data) {
        String stackMatch = "";
        int stackMatches = 0;

        for (Map.Entry<String, GunData> entry : vehicle.getGunDataMap().entrySet()) {
            GunData candidate = entry.getValue();
            if (candidate == data) return ShellRule.normalizeId(entry.getKey());

            if (ItemStack.matches(candidate.stack(), data.stack())) {
                stackMatch = ShellRule.normalizeId(entry.getKey());
                stackMatches++;
            }
        }

        return stackMatches == 1 ? stackMatch : "";
    }

    private static String weaponId(String vehicleId, String weaponName) {
        if (vehicleId == null || vehicleId.isBlank() || weaponName == null || weaponName.isBlank()) return "";
        return ShellRule.weaponId(vehicleId, weaponName);
    }

    private static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    private static String ammoId(GunData data) {
        try {
            AmmoConsumer consumer = data.selectedAmmoConsumer();
            ItemStack stack = consumer.stack();
            String id = itemId(stack);
            if (!id.isBlank()) return id;
            if (consumer.getAmmo() != null) return AmmoOption.extractAmmoId(consumer.getAmmo());
        } catch (RuntimeException ignored) {
        }
        return "";
    }

    private static String ammoSpec(GunData data) {
        try {
            AmmoConsumer consumer = data.selectedAmmoConsumer();
            return consumer.getAmmo() == null ? "" : AmmoOption.normalizeAmmoSpec(consumer.getAmmo());
        } catch (RuntimeException ignored) {
        }
        return "";
    }

    private static String projectileId(GunData data) {
        try {
            Object projectile = data.get(GunProp.PROJECTILE);
            if (projectile == null) return "";
            Method method = projectile.getClass().getMethod("getItemId");
            Object id = method.invoke(projectile);
            return id == null ? "" : ShellRule.normalizeId(id.toString());
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return "";
        }
    }

    private static int intProp(GunData data, GunProp<?, Integer> prop, int fallback) {
        try {
            Integer value = data.get(prop);
            return value == null ? fallback : value;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String entityId(Entity entity) {
        if (entity == null) return null;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id == null ? null : id.toString();
    }
}
