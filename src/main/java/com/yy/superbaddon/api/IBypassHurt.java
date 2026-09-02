package com.yy.superbaddon.api;

import com.yy.superbaddon.util.CopiedHurtCalculator;
import net.minecraft.world.damagesource.DamageSource;

public interface IBypassHurt {
    CopiedHurtCalculator.Result superbaddon$calculateCopiedHurt(DamageSource source, float amount, float penetration);

    boolean superbaddon$setHealthHurtBypass(DamageSource source, float amount);
}
