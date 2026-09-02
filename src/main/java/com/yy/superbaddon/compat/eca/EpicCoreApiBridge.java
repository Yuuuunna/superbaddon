package com.yy.superbaddon.compat.eca;

import com.yy.superbaddon.SuperbAddonMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runtime-safe facade for the optional Epic Core API dependency.
 *
 * <p>The ECA-typed implementation lives in a separate class.  It is only referenced after Forge
 * confirms that ECA is loaded, so the addon remains loadable when ECA is absent.</p>
 */
public final class EpicCoreApiBridge {
    private static final String MOD_ID = "eca";
    private static final AtomicBoolean FAILURE_LOGGED = new AtomicBoolean();

    private EpicCoreApiBridge() {
    }

    public static boolean setHealth(LivingEntity entity, float health) {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return false;
        }

        try {
            return EpicCoreApiBridgeImpl.setHealth(entity, health);
        } catch (LinkageError | RuntimeException exception) {
            if (FAILURE_LOGGED.compareAndSet(false, true)) {
                SuperbAddonMod.LOGGER.warn(
                        "Epic Core API setHealth failed; using the native health fallback: {}",
                        exception.toString()
                );
            }
            return false;
        }
    }
}
