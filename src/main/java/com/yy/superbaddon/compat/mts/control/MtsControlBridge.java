package com.yy.superbaddon.compat.mts.control;

import com.yy.superbaddon.compat.mts.MtsCompatConfig;
import net.minecraftforge.fml.ModList;

/**
 * Soft-dependency guard for the MTS control bridge.
 *
 * <p>This class deliberately references <b>no</b> MinecraftTransportSimulator types so it is always
 * safe to load, even when MTS is absent. All MTS-typed logic lives in {@link MtsControlBridgeImpl},
 * which must only be touched after {@link #isAvailable()} returns true. Because the JVM links a class
 * lazily (on first use), keeping the typed code in a separate class means {@code MtsControlBridgeImpl}
 * is never loaded — and thus can never throw {@code NoClassDefFoundError} — when MTS is not installed.
 *
 * <p>MTS is declared as {@code compileOnly} and is intentionally absent from {@code mods.toml}: the
 * addon has no hard runtime dependency on it.
 */
public final class MtsControlBridge {
    private MtsControlBridge() {
    }

    private static Boolean mtsLoaded;

    /**
     * Master gate for the input bridge. While false, MTS keeps its native controls untouched
     * (never-break-userspace default). Lazily initialised from {@code MtsCompatConfig.bridgeControls()}
     * and persisted there on toggle, so the choice survives a restart. {@code null} = not yet loaded.
     * Only ever touched on the client thread (mixin read + keybind toggle), so no synchronisation.
     */
    private static Boolean inputOverrideEnabled;

    /** True when an MTS-family mod is present this runtime. Cached after first query. */
    public static boolean isAvailable() {
        if (mtsLoaded == null) {
            ModList modList = ModList.get();
            mtsLoaded = modList.isLoaded("mts")
                    || modList.isLoaded("immersivevehicles")
                    || modList.isLoaded("minecrafttransportsimulator");
        }
        return mtsLoaded;
    }

    /** Whether the bridge should suppress MTS's native input handling and drive controls itself. */
    public static boolean isInputOverrideEnabled() {
        if (inputOverrideEnabled == null) {
            inputOverrideEnabled = MtsCompatConfig.bridgeControls();
        }
        return inputOverrideEnabled;
    }

    /** Toggles the bridge and persists the choice to config so it is remembered next launch. */
    public static void setInputOverrideEnabled(boolean enabled) {
        inputOverrideEnabled = enabled;
        MtsCompatConfig.setBridgeControls(enabled);
    }

    /**
     * Opt-in flag for the SuperbWarfare-style tilt camera (camera follows the MTS vehicle's roll/pitch).
     * Deliberately independent of {@link #isInputOverrideEnabled()} so a passenger who isn't driving can
     * still enable it. Default off; client-only, not persisted — the rider opts in each session via the
     * (unbound by default) keybind. Only touched on the client thread, so no synchronisation.
     */
    private static boolean cameraTiltEnabled;

    public static boolean isCameraTiltEnabled() {
        return cameraTiltEnabled;
    }

    public static void setCameraTiltEnabled(boolean enabled) {
        cameraTiltEnabled = enabled;
    }

    /**
     * MTS-free facade for the camera-tilt mixin: returns the ridden vehicle's tilt in <b>Minecraft</b>
     * angle convention as {@code [roll, pitch, vehicleYaw]} (degrees, interpolated by {@code partialTicks}),
     * or {@code null} when the tilt should not apply (MTS absent, feature off, or not seated in an MTS
     * vehicle). Keeping the MTS-typed work behind {@link #isAvailable()} means {@link MtsControlBridgeImpl}
     * is never class-loaded — and so can never throw {@code NoClassDefFoundError} — when MTS is absent.
     */
    public static float[] getRiddenVehicleTilt(float partialTicks) {
        if (!cameraTiltEnabled || !isAvailable()) {
            return null;
        }
        return MtsControlBridgeImpl.computeRiddenVehicleTilt(partialTicks);
    }
}
