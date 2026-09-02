package com.yy.superbaddon.knockback;

import com.atsuishio.superbwarfare.entity.mixin.ICustomKnockback;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class KnockbackRuntime {
    private static final double VANILLA_HURT_KNOCKBACK = 0.4D;
    private static final ThreadLocal<KnockbackSpec> SHOOT_PROFILE = new ThreadLocal<>();
    private static final ThreadLocal<LivingEntity> DIRECT_TARGET = new ThreadLocal<>();

    private KnockbackRuntime() {
    }

    public static void beginShoot(KnockbackSpec spec) {
        if (spec == null || spec.isPureInherit()) {
            SHOOT_PROFILE.remove();
        } else {
            SHOOT_PROFILE.set(spec);
        }
    }

    public static void endShoot() {
        SHOOT_PROFILE.remove();
    }

    public static Entity attachToProjectile(Entity entity) {
        KnockbackSpec spec = SHOOT_PROFILE.get();
        if (spec != null && entity instanceof KnockbackCarrier carrier) {
            carrier.superbaddon$setKnockbackSpec(spec);
        }
        return entity;
    }

    public static CustomExplosion attachToExplosion(CustomExplosion explosion, Entity source) {
        if (explosion instanceof KnockbackCarrier carrier) {
            carrier.superbaddon$setKnockbackSpec(specFrom(source));
        }
        return explosion;
    }

    public static void beginDirect(DamageSource source, Entity target) {
        if (!(target instanceof LivingEntity living) || source == null || source.is(DamageTypeTags.IS_EXPLOSION)) {
            return;
        }
        // Respect 0.8.9.1's knockback suppression tag (DamageHandler skips vanilla knockback for it).
        if (source.is(ModTags.DamageTypes.NO_HURT_EFFECT)) {
            return;
        }

        KnockbackSpec spec = specFrom(source.getDirectEntity());
        if (spec == null || spec.direct().isPureInherit()) {
            return;
        }

        ICustomKnockback knockback = ICustomKnockback.getInstance(living);
        if (knockback.superbWarfare$getKnockbackStrength() >= 0.0D) {
            return;
        }

        knockback.superbWarfare$setKnockbackStrength(spec.direct().apply(VANILLA_HURT_KNOCKBACK));
        DIRECT_TARGET.set(living);
    }

    public static void endDirect() {
        LivingEntity living = DIRECT_TARGET.get();
        if (living != null) {
            ICustomKnockback.getInstance(living).superbWarfare$resetKnockbackStrength();
            DIRECT_TARGET.remove();
        }
    }

    public static boolean applyDirectBypass(DamageSource source, Entity target) {
        if (!(target instanceof LivingEntity living) || source == null || source.is(DamageTypeTags.IS_EXPLOSION)) {
            return false;
        }
        if (source.is(ModTags.DamageTypes.NO_HURT_EFFECT)) {
            return false;
        }

        Entity sourceEntity = source.getEntity();
        if (sourceEntity == null) {
            return false;
        }

        ICustomKnockback knockback = ICustomKnockback.getInstance(living);
        boolean reset = setDirectStrengthIfMissing(knockback, source);
        try {
            double xRatio = sourceEntity.getX() - living.getX();
            double zRatio = sourceEntity.getZ() - living.getZ();
            while (xRatio * xRatio + zRatio * zRatio < 1.0E-4D) {
                xRatio = (Math.random() - Math.random()) * 0.01D;
                zRatio = (Math.random() - Math.random()) * 0.01D;
            }

            if (!source.is(DamageTypeTags.NO_IMPACT)) {
                living.hurtMarked = true;
            }
            living.knockback(VANILLA_HURT_KNOCKBACK, xRatio, zRatio);
            living.indicateDamage(xRatio, zRatio);
            return true;
        } finally {
            if (reset) {
                knockback.superbWarfare$resetKnockbackStrength();
            }
        }
    }

    public static KnockbackSpec currentExplosion(CustomExplosion explosion) {
        KnockbackSpec spec = explosion instanceof KnockbackCarrier carrier
                ? carrier.superbaddon$getKnockbackSpec()
                : null;
        return spec == null ? KnockbackSpec.inherit() : spec;
    }

    private static KnockbackSpec specFrom(Entity source) {
        if (source instanceof KnockbackCarrier carrier) {
            return carrier.superbaddon$getKnockbackSpec();
        }
        return null;
    }

    private static boolean setDirectStrengthIfMissing(ICustomKnockback knockback, DamageSource source) {
        if (knockback.superbWarfare$getKnockbackStrength() >= 0.0D) {
            return false;
        }

        KnockbackSpec spec = specFrom(source.getDirectEntity());
        if (spec == null || spec.direct().isPureInherit()) {
            return false;
        }

        knockback.superbWarfare$setKnockbackStrength(spec.direct().apply(VANILLA_HURT_KNOCKBACK));
        return true;
    }
}
