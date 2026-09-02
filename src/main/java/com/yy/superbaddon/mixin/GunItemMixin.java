package com.yy.superbaddon.mixin;

import com.atsuishio.superbwarfare.data.gun.ShootParameters;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.yy.superbaddon.knockback.KnockbackRuntime;
import com.yy.superbaddon.shell.ShellContext;
import com.yy.superbaddon.shell.ShellContextFactory;
import com.yy.superbaddon.shell.ShellRuleSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = GunItem.class, remap = false)
public abstract class GunItemMixin {
    @Inject(method = "shootBullet", at = @At("HEAD"), remap = false)
    private void superbaddon$beginKnockbackProfile(ShootParameters parameters, CallbackInfoReturnable<Boolean> cir) {
        Optional<ShellContext> context = ShellContextFactory.fromShootParameters(parameters);
        if (context.isEmpty()) {
            KnockbackRuntime.beginShoot(null);
            return;
        }

        ShellRuleSet.matchKnockback(context.get())
                .ifPresentOrElse(
                        match -> KnockbackRuntime.beginShoot(match.spec()),
                        () -> KnockbackRuntime.beginShoot(null)
                );
    }

    @Inject(method = "shootBullet", at = @At("RETURN"), remap = false)
    private void superbaddon$endKnockbackProfile(ShootParameters parameters, CallbackInfoReturnable<Boolean> cir) {
        KnockbackRuntime.endShoot();
    }
}
