package com.yy.superbaddon.knockback;

import com.google.gson.JsonObject;
import com.yy.superbaddon.SuperbAddonMod;
import net.minecraft.util.GsonHelper;

public final class KnockbackChannelSpec {
    public static final KnockbackChannelSpec INHERIT =
            new KnockbackChannelSpec(KnockbackMode.INHERIT, 0.0, false, 0.0, false, 0.0, false);

    private final KnockbackMode mode;
    private final double value;
    private final boolean hasValue;
    private final double min;
    private final boolean hasMin;
    private final double max;
    private final boolean hasMax;

    private KnockbackChannelSpec(
            KnockbackMode mode,
            double value,
            boolean hasValue,
            double min,
            boolean hasMin,
            double max,
            boolean hasMax
    ) {
        this.mode = mode == null ? KnockbackMode.INHERIT : mode;
        this.value = value;
        this.hasValue = hasValue;
        this.min = min;
        this.hasMin = hasMin;
        this.max = max;
        this.hasMax = hasMax;
    }

    public static KnockbackChannelSpec fromJson(JsonObject object, String path) {
        if (object == null || object.size() == 0) return INHERIT;

        KnockbackMode mode = KnockbackMode.from(GsonHelper.getAsString(object, "mode", "inherit"));
        if (mode == null) {
            SuperbAddonMod.LOGGER.warn("Invalid knockback mode at {}; using inherit", path);
            return INHERIT;
        }

        boolean hasValue = hasValidNumber(object, "value");
        double value = hasValue ? object.get("value").getAsDouble() : 0.0;
        if ((mode == KnockbackMode.SCALE || mode == KnockbackMode.FIXED) && !hasValue) {
            SuperbAddonMod.LOGGER.warn("Missing knockback value at {}; using inherit", path);
            return INHERIT;
        }

        boolean hasMin = hasValidNumber(object, "min");
        double min = hasMin ? object.get("min").getAsDouble() : 0.0;
        if (object.has("min") && !hasMin) {
            SuperbAddonMod.LOGGER.warn("Invalid knockback min at {}; ignoring min", path);
        }

        boolean hasMax = hasValidNumber(object, "max");
        double max = hasMax ? object.get("max").getAsDouble() : 0.0;
        if (object.has("max") && !hasMax) {
            SuperbAddonMod.LOGGER.warn("Invalid knockback max at {}; ignoring max", path);
        }
        if (mode == KnockbackMode.CAP && !hasMax) {
            SuperbAddonMod.LOGGER.warn("Missing knockback max at {}; using inherit", path);
            return INHERIT;
        }

        if (mode == KnockbackMode.INHERIT && !hasMin && !hasMax) return INHERIT;
        return new KnockbackChannelSpec(mode, value, hasValue, min, hasMin, max, hasMax);
    }

    public double apply(double original) {
        double result = switch (mode) {
            case INHERIT -> original;
            case SCALE -> original * value;
            case FIXED -> value;
            case CAP -> original;
            case DISABLED -> 0.0;
        };

        if (hasMin) result = Math.max(min, result);
        if (hasMax) result = Math.min(max, result);
        return Math.max(0.0, result);
    }

    public boolean isPureInherit() {
        return mode == KnockbackMode.INHERIT && !hasMin && !hasMax;
    }

    private static boolean hasValidNumber(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive() || !object.get(key).getAsJsonPrimitive().isNumber()) {
            return false;
        }
        double value = object.get(key).getAsDouble();
        return Double.isFinite(value) && value >= 0.0;
    }
}
