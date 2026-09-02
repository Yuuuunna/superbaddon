package com.yy.superbaddon.mixin;

import com.atsuishio.superbwarfare.entity.projectile.FastThrowableProjectile;
import com.yy.superbaddon.knockback.KnockbackCarrier;
import com.yy.superbaddon.knockback.KnockbackRuntime;
import com.yy.superbaddon.knockback.KnockbackSpec;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FastThrowableProjectile.class, remap = false)
public abstract class FastThrowableProjectileMixin implements KnockbackCarrier {
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

    @Inject(method = "setDamage(F)V", at = @At("HEAD"), remap = false)
    private void superbaddon$attachKnockbackProfile(float damage, CallbackInfo ci) {
        KnockbackRuntime.attachToProjectile((Entity) (Object) this);
    }
}
