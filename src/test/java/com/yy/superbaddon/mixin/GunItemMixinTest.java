package com.yy.superbaddon.mixin;

import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.yy.superbaddon.knockback.KnockbackCarrier;
import com.yy.superbaddon.knockback.KnockbackRuntime;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GunItemMixinTest {
    @Test
    void gunItemMixinDoesNotTargetMinecraftAddFreshEntity() {
        boolean targetsMinecraftSpawn = Arrays.stream(GunItemMixin.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(ModifyArg.class))
                .filter(annotation -> annotation != null)
                .map(annotation -> annotation.at().target())
                .anyMatch(target -> target.contains("addFreshEntity") || target.contains("m_7967_"));

        assertTrue(
                !targetsMinecraftSpawn,
                "GunItemMixin must not inject into ServerLevel.addFreshEntity; that target is remapped in production."
        );
    }

    @Test
    void projectileMixinAttachesProfileWhenGunItemInitializesProjectile() {
        boolean hasAttachHook = Arrays.stream(ProjectileEntityMixin.class.getDeclaredMethods())
                .filter(method -> method.getName().contains("attachKnockbackProfile"))
                .map(method -> method.getAnnotation(Inject.class))
                .filter(annotation -> annotation != null)
                .flatMap(annotation -> Arrays.stream(annotation.method()))
                .anyMatch(method -> method.startsWith("setGunItemId("));

        assertTrue(hasAttachHook);
    }

    @Test
    void fastThrowableMixinCarriesAndAttachesKnockbackProfile() throws Exception {
        Class<?> mixin = Class.forName("com.yy.superbaddon.mixin.FastThrowableProjectileMixin");

        assertTrue(KnockbackCarrier.class.isAssignableFrom(mixin));

        boolean hasAttachHook = Arrays.stream(mixin.getDeclaredMethods())
                .filter(method -> method.getName().contains("attachKnockbackProfile"))
                .map(method -> method.getAnnotation(Inject.class))
                .filter(annotation -> annotation != null)
                .flatMap(annotation -> Arrays.stream(annotation.method()))
                .anyMatch(method -> method.startsWith("setDamage("));

        assertTrue(hasAttachHook);
    }

    @Test
    void mixinConfigLoadsFastThrowableProjectileMixin() throws IOException {
        try (InputStream stream = GunItemMixinTest.class.getResourceAsStream("/superbaddon.mixins.json")) {
            assertTrue(stream != null);
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(json.contains("\"FastThrowableProjectileMixin\""));
        }
    }

    @Test
    void damageHandlerMixinWrapsDirectKnockbackForNonProjectileEntityHits() {
        boolean hasBeginHook = Arrays.stream(DamageHandlerMixin.class.getDeclaredMethods())
                .filter(method -> method.getName().contains("beginDirectKnockback"))
                .map(method -> method.getAnnotation(Inject.class))
                .filter(annotation -> annotation != null)
                .flatMap(annotation -> Arrays.stream(annotation.method()))
                .anyMatch(method -> method.equals("doDamage"));

        boolean hasEndHook = Arrays.stream(DamageHandlerMixin.class.getDeclaredMethods())
                .filter(method -> method.getName().contains("endDirectKnockback"))
                .map(method -> method.getAnnotation(Inject.class))
                .filter(annotation -> annotation != null)
                .flatMap(annotation -> Arrays.stream(annotation.method()))
                .anyMatch(method -> method.equals("doDamage"));

        assertTrue(hasBeginHook);
        assertTrue(hasEndHook);
    }

    @Test
    void damageBypassHasExplicitDirectKnockbackPath() {
        boolean hasBypassKnockback = Arrays.stream(KnockbackRuntime.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals("applyDirectBypass")
                        && method.getReturnType().equals(boolean.class));

        assertTrue(
                hasBypassKnockback,
                "Armor-penetration bypass skips normal hurt(), so direct knockback needs an explicit bypass path."
        );
    }

    @Test
    void explosionKnockbackProfileIsCarriedByExplosionInstance() {
        assertTrue(
                KnockbackCarrier.class.isAssignableFrom(CustomExplosionMixin.class),
                "CustomExplosion must carry its own knockback profile; ThreadLocal-only explosion state is too fragile."
        );

        boolean builderAttachesProfileToExplosion = Arrays.stream(CustomExplosionBuilderMixin.class.getDeclaredMethods())
                .filter(method -> method.getName().contains("attachKnockbackProfile"))
                .filter(method -> method.getReturnType().equals(CustomExplosion.class))
                .map(method -> method.getAnnotation(Redirect.class))
                .filter(annotation -> annotation != null)
                .flatMap(annotation -> Arrays.stream(annotation.method()))
                .anyMatch(method -> method.equals("explode"));

        assertTrue(builderAttachesProfileToExplosion);

        boolean explosionForceHookFailsLoudly = Arrays.stream(CustomExplosionMixin.class.getDeclaredMethods())
                .filter(method -> method.getName().contains("modifyExplosionKnockback"))
                .map(method -> method.getAnnotation(Redirect.class))
                .anyMatch(annotation -> annotation != null && annotation.require() != 0);

        assertTrue(
                explosionForceHookFailsLoudly,
                "Explosion knockback injection must not use require = 0; silent mixin failure is the bug hiding place."
        );

        // 0.8.9.1 moved the final Vec3.scale(force) into a deferred shockwave lambda, so the old
        // @ModifyArg on the scale call no longer resolves.  The stable interception point inside
        // explode() is now the ProtectionEnchantment dampener call — the redirect applies the spec
        // to the dampener's result, which is exactly the force the deferred runnable scales with.
        boolean explosionForceHookTargetsDampenedForce = Arrays.stream(CustomExplosionMixin.class.getDeclaredMethods())
                .filter(method -> method.getName().contains("modifyExplosionKnockback"))
                .map(method -> method.getAnnotation(Redirect.class))
                .anyMatch(annotation -> annotation != null
                        && !annotation.remap()
                        && annotation.at().remap()
                        && annotation.at().target().equals(
                                "Lnet/minecraft/world/item/enchantment/ProtectionEnchantment;getExplosionKnockbackAfterDampener(Lnet/minecraft/world/entity/LivingEntity;D)D")
                        && Arrays.asList(annotation.method()).contains("explode")
                        && Arrays.asList(annotation.method()).contains("m_46061_"));

        assertTrue(
                explosionForceHookTargetsDampenedForce,
                "Explosion knockback must redirect the dampened-force computation inside explode() (dev name + SRG alias); that result feeds the deferred Vec3.scale."
        );
    }
}
