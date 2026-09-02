package com.yy.superbaddon.util;

import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.yy.superbaddon.api.IBypassHurt;
import com.yy.superbaddon.penetration.ArmorPenetrationConfig;
import com.yy.superbaddon.penetration.ArmorPenetrationRules;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class CopiedHurtCalculator {
    private static final ThreadLocal<ExplosionContext> EXPLOSION_CONTEXT = new ThreadLocal<>();

    /**
     * 0.8.9.1 defers shockwave damage past the synchronous Builder.explode window
     * (Mod.queueServerWork), so damage landing outside EXPLOSION_CONTEXT resolves its explosion
     * context here instead, keyed by the (deferred) damage source instance.  Entries vanish once
     * the source is garbage-collected — the deferred runnable holds it for at most ~100 ticks.
     */
    private static final Map<DamageSource, ExplosionContext> DEFERRED_EXPLOSIONS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private CopiedHurtCalculator() {
    }

    @FunctionalInterface
    public interface DamageReducer {
        float apply(DamageSource source, float amount);
    }

    public enum StopReason {
        NONE,
        CLIENT_SIDE,
        DEAD_OR_DYING,
        NON_POSITIVE
    }

    public record Result(
            StopReason stopReason,
            float requestedAmount,
            float armorPenetration,
            float bypassDamage,
            float defendableDamage,
            float predictedAmountAfterArmor,
            float predictedAmountAfterMagic,
            float predictedAbsorptionConsumed,
            float predictedHealthDamage
    ) {
        public boolean dealsDamage() {
            return this.stopReason == StopReason.NONE
                    && (this.predictedHealthDamage > 0.0F || this.predictedAbsorptionConsumed > 0.0F);
        }
    }

    public static Result calculate(
            LivingEntity self,
            DamageSource source,
            float requestedAmount,
            float armorPenetration,
            DamageReducer armorReducer,
            DamageReducer magicReducer
    ) {
        if (requestedAmount <= 0.0F) {
            return stopped(StopReason.NON_POSITIVE, requestedAmount);
        }
        if (self.level().isClientSide) {
            return stopped(StopReason.CLIENT_SIDE, requestedAmount);
        }
        if (self.isDeadOrDying()) {
            return stopped(StopReason.DEAD_OR_DYING, requestedAmount);
        }

        ArmorPenetrationRules.DamageSplit split = ArmorPenetrationRules.splitDamage(
                requestedAmount,
                armorPenetration,
                self.getAbsorptionAmount(),
                amount -> armorReducer.apply(source, amount),
                amount -> magicReducer.apply(source, amount)
        );

        return new Result(
                StopReason.NONE,
                requestedAmount,
                split.penetration(),
                split.bypassDamage(),
                split.defendableDamage(),
                split.amountAfterArmor(),
                split.amountAfterMagic(),
                split.absorptionConsumed(),
                split.healthDamage()
        );
    }

    @Nullable
    public static Boolean tryHurtWithArmorPenetration(DamageSource source, Entity entity, float amount) {
        if (!(entity instanceof LivingEntity livingEntity) || !isSupportedVehicleWeaponDamage(source)) {
            return null;
        }

        Penetration penetration = penetrationFor(source, entity);
        if (!penetration.applies()) {
            return null;
        }

        if (!(livingEntity instanceof IBypassHurt bypassHurt)) {
            return null;
        }

        if (!canApplyBypassDamage(livingEntity, source, amount)) {
            return false;
        }

        entity.invulnerableTime = 0;

        if (penetration.fullBypass()) {
            return bypassHurt.superbaddon$setHealthHurtBypass(source, amount);
        }

        Result result = bypassHurt.superbaddon$calculateCopiedHurt(source, amount, penetration.value());
        if (!result.dealsDamage()) {
            return false;
        }

        if (result.predictedAbsorptionConsumed() > 0.0F) {
            livingEntity.setAbsorptionAmount(Math.max(
                    0.0F,
                    livingEntity.getAbsorptionAmount() - result.predictedAbsorptionConsumed()
            ));
        }

        if (result.predictedHealthDamage() <= 0.0F) {
            return true;
        }

        return bypassHurt.superbaddon$setHealthHurtBypass(source, result.predictedHealthDamage());
    }

    public static void beginExplosion(
            @Nullable DamageSource source,
            @Nullable Entity directSource,
            Vec3 center,
            float radius
    ) {
        ExplosionContext previous = EXPLOSION_CONTEXT.get();
        ExplosionContext context = buildExplosionContext(source, directSource, center, radius);
        EXPLOSION_CONTEXT.set(new ExplosionContext(
                context.center(),
                context.radius(),
                context.sourcePenetration(),
                previous
        ));
    }

    public static void endExplosion() {
        ExplosionContext context = EXPLOSION_CONTEXT.get();
        if (context == null || context.previous() == null) {
            EXPLOSION_CONTEXT.remove();
        } else {
            EXPLOSION_CONTEXT.set(context.previous());
        }
    }

    /**
     * Register the explosion context for damage dealt after the synchronous window closes
     * (0.8.9.1 staged block destruction + delayed shockwave).  Called with the damage source the
     * deferred hurt calls will carry, so {@link #penetrationFor} can fall back to this lookup.
     */
    public static void attachDeferredExplosion(
            @Nullable DamageSource source,
            @Nullable Entity directSource,
            Vec3 center,
            float radius
    ) {
        if (source == null) {
            return;
        }
        DEFERRED_EXPLOSIONS.put(source, buildExplosionContext(source, directSource, center, radius));
    }

    private static ExplosionContext buildExplosionContext(
            @Nullable DamageSource source,
            @Nullable Entity directSource,
            Vec3 center,
            float radius
    ) {
        Penetration sourcePenetration = penetrationForEntity(directSource);
        if (!sourcePenetration.applies() && source != null) {
            sourcePenetration = penetrationForEntity(source.getDirectEntity());
        }
        return new ExplosionContext(center, radius, sourcePenetration.value(), null);
    }

    private static boolean canApplyBypassDamage(LivingEntity livingEntity, DamageSource source, float amount) {
        if (amount <= 0.0F || livingEntity.level().isClientSide || livingEntity.isDeadOrDying()) {
            return false;
        }
        if (livingEntity.isInvulnerableTo(source) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_FIRE) && livingEntity.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            return false;
        }
        if (livingEntity instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }

        Entity attacker = source.getEntity();
        return attacker == null
                || !livingEntity.isAlliedTo(attacker)
                || livingEntity.getTeam() == null
                || livingEntity.getTeam().isAllowFriendlyFire();
    }

    private static Penetration penetrationFor(DamageSource source, Entity target) {
        if (isSupportedExplosionDamage(source)) {
            ExplosionContext context = EXPLOSION_CONTEXT.get();
            if (context == null) {
                context = DEFERRED_EXPLOSIONS.get(source);
            }
            return context == null ? Penetration.NONE : context.penetrationAt(target.position());
        }

        if (isSupportedProjectileHitDamage(source)) {
            Penetration direct = penetrationForEntity(source.getDirectEntity());
            if (direct.applies()) {
                return direct;
            }
            return penetrationForEntity(source.getEntity());
        }

        return Penetration.NONE;
    }

    private static Penetration penetrationForEntity(@Nullable Entity entity) {
        if (entity == null) {
            return Penetration.NONE;
        }

        if (entity instanceof ProjectileEntity projectile) {
            Penetration fromGunId = penetrationForWeapon(parseResourceLocation(projectile.getGunItemId()));
            if (fromGunId.applies()) {
                return fromGunId;
            }
        }

        Penetration fromEntityType = penetrationForWeapon(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
        if (fromEntityType.applies()) {
            return fromEntityType;
        }

        if (entity instanceof ProjectileEntity projectile) {
            return Penetration.of(projectile.getBypassArmorRate());
        }

        return Penetration.NONE;
    }

    private static Penetration penetrationForWeapon(@Nullable ResourceLocation weaponId) {
        if (weaponId == null) {
            return Penetration.NONE;
        }
        if (ArmorPenetrationConfig.isFullArmorBypassWeapon(weaponId)) {
            return Penetration.FULL;
        }
        return Penetration.of(ArmorPenetrationConfig.getArmorPenetration(weaponId));
    }

    @Nullable
    private static ResourceLocation parseResourceLocation(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new ResourceLocation(raw);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static boolean isSupportedVehicleWeaponDamage(DamageSource source) {
        return isSupportedProjectileHitDamage(source) || isSupportedExplosionDamage(source);
    }

    private static boolean isSupportedProjectileHitDamage(DamageSource source) {
        return source.is(ModDamageTypes.PROJECTILE_HIT)
                || source.is(ModDamageTypes.GRAPESHOT_HIT);
    }

    private static boolean isSupportedExplosionDamage(DamageSource source) {
        return source.is(ModDamageTypes.PROJECTILE_EXPLOSION)
                || source.is(ModDamageTypes.CUSTOM_EXPLOSION);
    }

    private static Result stopped(StopReason reason, float requestedAmount) {
        return new Result(
                reason,
                requestedAmount,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F
        );
    }

    private record ExplosionContext(Vec3 center, float radius, float sourcePenetration, @Nullable ExplosionContext previous) {
        private Penetration penetrationAt(Vec3 targetPosition) {
            float base = ArmorPenetrationRules.clampPenetration(this.sourcePenetration);
            if (base <= 0.0F) {
                return Penetration.NONE;
            }
            if (this.radius <= 0.0F) {
                return Penetration.of(base);
            }

            float distanceRatio = (float) Math.min(1.0D, this.center.distanceTo(targetPosition) / this.radius);
            return Penetration.of(ArmorPenetrationRules.explosionPenetrationAt(
                    base,
                    distanceRatio,
                    ArmorPenetrationConfig.explosionFalloff()
            ));
        }
    }

    private record Penetration(float value, boolean fullBypass) {
        private static final Penetration NONE = new Penetration(0.0F, false);
        private static final Penetration FULL = new Penetration(1.0F, true);

        private static Penetration of(float value) {
            float clamped = ArmorPenetrationRules.clampPenetration(value);
            if (clamped <= 0.0F) {
                return NONE;
            }
            if (ArmorPenetrationRules.isFullBypass(clamped)) {
                return FULL;
            }
            return new Penetration(clamped, false);
        }

        private boolean applies() {
            return this.value > 0.0F;
        }
    }
}
