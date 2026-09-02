package com.yy.superbaddon.compat.eca;

import net.eca.api.EcaAPI;
import net.minecraft.world.entity.LivingEntity;

/** ECA-typed implementation.  Load only through {@link EpicCoreApiBridge}. */
final class EpicCoreApiBridgeImpl {
    private EpicCoreApiBridgeImpl() {
    }

    static boolean setHealth(LivingEntity entity, float health) {
        return EcaAPI.setHealth(entity, health);
    }
}
