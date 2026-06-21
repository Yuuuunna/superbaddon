package com.yy.superbaddon.client;

import com.yy.superbaddon.SuperbAddonMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Client keybinds for the MTS control bridge. Registered unconditionally (no MTS reference here, so
 * safe without MTS); the toggle only has any effect while riding an MTS vehicle, where the
 * {@code ControlSystem} mixin reads the bridge flag.
 */
@Mod.EventBusSubscriber(modid = SuperbAddonMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MtsControlKeybinds {
    public static final String CATEGORY = "key.categories.superbaddon";

    /** Toggles the SuperbWarfare-style control bridge on/off. Default unbound to avoid conflicts. */
    public static final KeyMapping BRIDGE_TOGGLE = new KeyMapping(
            "key.superbaddon.mts_bridge_toggle",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY);

    /** Toggles the SuperbWarfare-style tilt camera for MTS vehicles. Default unbound; opt-in. */
    public static final KeyMapping CAMERA_TILT_TOGGLE = new KeyMapping(
            "key.superbaddon.mts_camera_tilt_toggle",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY);

    private MtsControlKeybinds() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(BRIDGE_TOGGLE);
        event.register(CAMERA_TILT_TOGGLE);
    }
}
