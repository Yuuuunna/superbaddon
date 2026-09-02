package com.yy.superbaddon.mixin;

import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.yy.superbaddon.knockback.KnockbackCarrier;
import com.yy.superbaddon.knockback.KnockbackRuntime;
import com.yy.superbaddon.knockback.KnockbackSpec;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CustomExplosion.class, remap = false)
public abstract class CustomExplosionMixin implements KnockbackCarrier {
    @Unique
    private KnockbackSpec superbaddon$knockbackSpec = KnockbackSpec.inherit();

    @Override
    public void superbaddon$setKnockbackSpec(KnockbackSpec spec) {
        this.superbaddon$knockbackSpec = spec == null ? KnockbackSpec.inherit() : spec;
    }

    @Override
    public KnockbackSpec superbaddon$getKnockbackSpec() {
        return this.superbaddon$knockbackSpec;
    }

    /**
     * 0.8.9.1 defers knockback to a shockwave runnable (lambda$explode$N), so the old
     * {@code @ModifyArg} on {@code Vec3.scale(D)} no longer finds any call inside {@code explode()}.
     * The dampener call <em>is</em> still inside {@code explode()} and its result is exactly the
     * force the deferred runnable scales with, so intercept it instead: apply the spec to the
     * dampened force, keeping the pre-0.8.9.1 semantics (custom knockback replaces the vanilla
     * post-dampener value, Blast Protection still applies to the vanilla force).
     */
    @Redirect(
            method = {"explode", "m_46061_"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/ProtectionEnchantment;getExplosionKnockbackAfterDampener(Lnet/minecraft/world/entity/LivingEntity;D)D",
                    remap = true
            ),
            require = 1,
            remap = false
    )
    private double superbaddon$modifyExplosionKnockbackForce(LivingEntity entity, double force) {
        double dampened = ProtectionEnchantment.getExplosionKnockbackAfterDampener(entity, force);
        return KnockbackRuntime.currentExplosion((CustomExplosion) (Object) this).explosion().apply(dampened);
    }
}
