package com.yy.superbaddon.mixin;

import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.yy.superbaddon.knockback.KnockbackRuntime;
import com.yy.superbaddon.util.CopiedHurtCalculator;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CustomExplosion.Builder.class, remap = false)
public abstract class CustomExplosionBuilderMixin {
    @Shadow
    private Entity directSource;

    @Shadow
    private float radius;

    @Shadow
    private DamageSource damageSource;

    @Shadow
    public Vec3 position;

    @Inject(method = "explode", at = @At("HEAD"), remap = false)
    private void superbaddon$beginExplosionKnockback(CallbackInfo ci) {
        CopiedHurtCalculator.beginExplosion(this.damageSource, this.directSource, this.position, this.radius);
    }

    @Redirect(
            method = "explode",
            at = @At(
                    value = "NEW",
                    target = "Lcom/atsuishio/superbwarfare/tools/CustomExplosion;"
            ),
            remap = false
    )
    private CustomExplosion superbaddon$attachKnockbackProfile(
            Level level,
            Entity directSource,
            DamageSource source,
            float damage,
            double x,
            double y,
            double z,
            float radius,
            Explosion.BlockInteraction blockInteraction
    ) {
        // `source` is the resolved damage source (built here in Builder.explode when the shadowed
        // one is null), i.e. exactly the source the 0.8.9.1 deferred shockwave runnable will carry
        // into forceHurt.  Register the armor-penetration context for those deferred hurt calls,
        // whose damage lands after the synchronous begin/end window has been popped.
        CopiedHurtCalculator.attachDeferredExplosion(source, directSource, this.position, this.radius);
        return KnockbackRuntime.attachToExplosion(
                new CustomExplosion(level, directSource, source, damage, x, y, z, radius, blockInteraction),
                this.directSource
        );
    }

    @Inject(method = "explode", at = @At("RETURN"), remap = false)
    private void superbaddon$endExplosionKnockback(CallbackInfo ci) {
        CopiedHurtCalculator.endExplosion();
    }
}
