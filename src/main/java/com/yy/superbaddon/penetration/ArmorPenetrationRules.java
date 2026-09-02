package com.yy.superbaddon.penetration;

import java.util.Locale;
import java.util.Optional;

public final class ArmorPenetrationRules {
    public static final float FULL_BYPASS_THRESHOLD = 0.999F;

    private ArmorPenetrationRules() {
    }

    public static Optional<Float> parsePenetrationValue(String rawValue) {
        if (rawValue == null) {
            return Optional.empty();
        }

        String value = rawValue.trim();
        boolean percent = value.endsWith("%");
        if (percent) {
            value = value.substring(0, value.length() - 1).trim();
        }

        try {
            float parsed = Float.parseFloat(value);
            return Optional.of(clampPenetration(percent ? parsed / 100.0F : parsed));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public static float clampPenetration(float value) {
        if (Float.isNaN(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    public static boolean isFullBypass(float value) {
        return clampPenetration(value) >= FULL_BYPASS_THRESHOLD;
    }

    public static String formatPenetrationValue(float value) {
        return String.format(Locale.ROOT, "%.2f", clampPenetration(value));
    }

    public static DamageSplit splitDamage(
            float requestedAmount,
            float penetration,
            float absorption,
            FloatReducer armorReducer,
            FloatReducer magicReducer
    ) {
        float clampedPenetration = clampPenetration(penetration);
        float bypassDamage = requestedAmount * clampedPenetration;
        float defendableDamage = requestedAmount - bypassDamage;
        float amountAfterArmor = armorReducer.apply(defendableDamage);
        float amountAfterMagic = magicReducer.apply(amountAfterArmor);
        float healthDamage = Math.max(amountAfterMagic - absorption, 0.0F);
        float absorptionConsumed = amountAfterMagic - healthDamage;

        return new DamageSplit(
                clampedPenetration,
                bypassDamage,
                defendableDamage,
                amountAfterArmor,
                amountAfterMagic,
                absorptionConsumed,
                bypassDamage + healthDamage
        );
    }

    public static float explosionPenetrationAt(float sourcePenetration, float distanceRatio, ExplosionFalloff settings) {
        float base = clampPenetration(sourcePenetration);
        if (base <= 0.0F) {
            return 0.0F;
        }

        float ratio = Math.max(0.0F, Math.min(1.0F, distanceRatio));
        float floor = Math.min(base, clampPenetration(settings.minimumOuterPenetration()));
        float inner = clampPenetration(settings.innerRadiusFactor());
        float falloffPower = Math.max(settings.outerFalloffPower(), 0.1F);
        float falloff;

        if (ratio <= inner) {
            float innerRatio = inner <= 0.0F ? 1.0F : ratio / inner;
            falloff = 0.15F * innerRatio * innerRatio;
        } else {
            float outerRatio = (ratio - inner) / Math.max(1.0F - inner, 0.0001F);
            falloff = 0.15F + 0.85F * (float) Math.pow(outerRatio, falloffPower);
        }

        return base + falloff * (floor - base);
    }

    @FunctionalInterface
    public interface FloatReducer {
        float apply(float amount);
    }

    public record DamageSplit(
            float penetration,
            float bypassDamage,
            float defendableDamage,
            float amountAfterArmor,
            float amountAfterMagic,
            float absorptionConsumed,
            float healthDamage
    ) {
    }

    public record ExplosionFalloff(
            float minimumOuterPenetration,
            float innerRadiusFactor,
            float outerFalloffPower
    ) {
    }
}
