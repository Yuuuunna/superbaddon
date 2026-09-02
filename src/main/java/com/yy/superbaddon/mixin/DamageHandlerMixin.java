package com.yy.superbaddon.mixin;

import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.yy.superbaddon.knockback.KnockbackRuntime;
import com.yy.superbaddon.util.CopiedHurtCalculator;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DamageHandler.class, remap = false)
public abstract class DamageHandlerMixin {
    @Inject(method = "doDamage", at = @At("HEAD"), cancellable = true, remap = false)
    private static void superbaddon$beginDirectKnockback(
            Entity entity,
            DamageSource source,
            float damage,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Boolean result = CopiedHurtCalculator.tryHurtWithArmorPenetration(source, entity, damage);
        if (result != null) {
            if (result) {
                KnockbackRuntime.applyDirectBypass(source, entity);
            }
            cir.setReturnValue(result);
            return;
        }
        KnockbackRuntime.beginDirect(source, entity);
    }

    @Inject(method = "doDamage", at = @At("RETURN"), remap = false)
    private static void superbaddon$endDirectKnockback(
            Entity entity,
            DamageSource source,
            float damage,
            CallbackInfoReturnable<Boolean> cir
    ) {
        KnockbackRuntime.endDirect();
    }
}
