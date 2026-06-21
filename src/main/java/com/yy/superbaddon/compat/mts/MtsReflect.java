package com.yy.superbaddon.compat.mts;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

final class MtsReflect {
    private MtsReflect() {
    }

    static boolean isLikelyFuelPump(BlockEntity blockEntity) {
        if (blockEntity == null) return false;

        // MTS does not expose its TileEntityFuelPump as the MC BlockEntity.  getBlockEntity()
        // returns a Forge BuilderTileEntity wrapper, and the real pump (with getTank/fuel fields)
        // lives in the builder's "tileEntity" field.  Unwrap before inspecting.
        Object pump = unwrap(blockEntity);
        String className = pump.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        if (className.contains("fuelpump") || className.contains("fuel_pump")) {
            return tankFrom(pump).isPresent();
        }

        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(blockEntity.getBlockState().getBlock());
        if (blockId == null) return false;
        String path = blockId.getPath().toLowerCase(java.util.Locale.ROOT);
        return path.contains("fuel") && path.contains("pump") && tankFrom(pump).isPresent();
    }

    /**
     * Returns the real MTS fuel-pump tile entity wrapped inside the Forge builder, or the
     * block entity itself when no wrapper is present.  All pump field/tank access must go
     * through this; the builder shell exposes none of the pump's methods or fields.
     */
    static Object pumpTile(BlockEntity blockEntity) {
        return unwrap(blockEntity);
    }

    private static Object unwrap(BlockEntity blockEntity) {
        if (blockEntity == null) return null;
        Field field = findField(blockEntity.getClass(), "tileEntity");
        if (field != null) {
            try {
                field.setAccessible(true);
                Object inner = field.get(blockEntity);
                if (inner != null) return inner;
            } catch (IllegalAccessException | RuntimeException ignored) {
            }
        }
        return blockEntity;
    }

    static Optional<TankAccess> tankFrom(Object provider) {
        if (provider == null) return Optional.empty();
        try {
            Method getTank = provider.getClass().getMethod("getTank");
            Object tank = getTank.invoke(provider);
            if (tank == null) return Optional.empty();
            return Optional.of(new TankAccess(tank));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    static void recordPumpDispense(Object pump, double amount) {
        if (pump == null || amount <= 0) return;
        tryAddDoubleField(pump, "fuelDispensedThisConnection", amount);
        tryAddDoubleField(pump, "fuelDispensedThisPurchase", amount);
        trySetBooleanField(pump, "hasChanged", true);
    }

    static boolean isCreativePump(Object pump) {
        return tryGetBooleanField(pump, "isCreative", false);
    }

    static boolean isPurchaseComplete(Object pump) {
        if (pump == null || isCreativePump(pump)) return false;
        int purchased = tryGetIntField(pump, "fuelPurchased", 0);
        double dispensed = tryGetDoubleField(pump, "fuelDispensedThisPurchase", 0.0D);
        return purchased > 0 && dispensed >= purchased;
    }

    private static void tryAddDoubleField(Object target, String name, double amount) {
        Field field = findField(target.getClass(), name);
        if (field == null) return;
        try {
            field.setAccessible(true);
            field.setDouble(target, field.getDouble(target) + amount);
        } catch (IllegalAccessException | RuntimeException ignored) {
        }
    }

    private static void trySetBooleanField(Object target, String name, boolean value) {
        Field field = findField(target.getClass(), name);
        if (field == null) return;
        try {
            field.setAccessible(true);
            field.setBoolean(target, value);
        } catch (IllegalAccessException | RuntimeException ignored) {
        }
    }

    private static boolean tryGetBooleanField(Object target, String name, boolean fallback) {
        Field field = findField(target.getClass(), name);
        if (field == null) return fallback;
        try {
            field.setAccessible(true);
            return field.getBoolean(target);
        } catch (IllegalAccessException | RuntimeException ignored) {
            return fallback;
        }
    }

    private static int tryGetIntField(Object target, String name, int fallback) {
        Field field = findField(target.getClass(), name);
        if (field == null) return fallback;
        try {
            field.setAccessible(true);
            return field.getInt(target);
        } catch (IllegalAccessException | RuntimeException ignored) {
            return fallback;
        }
    }

    private static double tryGetDoubleField(Object target, String name, double fallback) {
        Field field = findField(target.getClass(), name);
        if (field == null) return fallback;
        try {
            field.setAccessible(true);
            return field.getDouble(target);
        } catch (IllegalAccessException | RuntimeException ignored) {
            return fallback;
        }
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    static final class TankAccess {
        private final Object tank;
        private final Method getFluidLevel;
        private final Method getFluid;
        private final Method drain;

        private TankAccess(Object tank) throws NoSuchMethodException {
            this.tank = tank;
            Class<?> type = tank.getClass();
            this.getFluidLevel = type.getMethod("getFluidLevel");
            this.getFluid = type.getMethod("getFluid");
            this.drain = type.getMethod("drain", double.class, boolean.class);
        }

        double level() {
            try {
                Object value = getFluidLevel.invoke(tank);
                return value instanceof Number number ? number.doubleValue() : 0.0D;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return 0.0D;
            }
        }

        String fluid() {
            try {
                Object value = getFluid.invoke(tank);
                return value == null ? "" : value.toString();
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return "";
            }
        }

        double drain(double maxAmount, boolean doDrain) {
            try {
                Object value = drain.invoke(tank, maxAmount, doDrain);
                return value instanceof Number number ? number.doubleValue() : 0.0D;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return 0.0D;
            }
        }
    }
}
