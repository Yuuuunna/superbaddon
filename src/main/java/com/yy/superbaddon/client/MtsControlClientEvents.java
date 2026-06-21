package com.yy.superbaddon.client;

import com.yy.superbaddon.SuperbAddonMod;
import com.yy.superbaddon.compat.mts.control.MtsControlBridge;
import com.yy.superbaddon.compat.mts.control.MtsControlBridgeImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles the bridge toggle keybind each client tick. Lives on the FORGE bus, client only.
 *
 * <p>References no MTS types, so it is safe to load without MTS. When MTS is absent the toggle still
 * flips the flag harmlessly (nothing reads it); the message notes when MTS isn't present so the user
 * isn't confused.</p>
 */
@Mod.EventBusSubscriber(modid = SuperbAddonMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class MtsControlClientEvents {
    private MtsControlClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        // Maintain aircraft view-lock (mouse = stick). Guarded so MtsControlBridgeImpl — which
        // references MTS types — is only ever class-loaded when MTS is actually present.
        if (MtsControlBridge.isAvailable()) {
            MtsControlBridgeImpl.tickViewLock();
        }

        while (MtsControlKeybinds.BRIDGE_TOGGLE.consumeClick()) {
            boolean enabled = !MtsControlBridge.isInputOverrideEnabled();
            MtsControlBridge.setInputOverrideEnabled(enabled);

            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                String key;
                if (!MtsControlBridge.isAvailable()) {
                    key = "message.superbaddon.mts_bridge_unavailable";
                } else {
                    key = enabled ? "message.superbaddon.mts_bridge_on" : "message.superbaddon.mts_bridge_off";
                }
                mc.player.displayClientMessage(Component.translatable(key), true);
            }
        }

        while (MtsControlKeybinds.CAMERA_TILT_TOGGLE.consumeClick()) {
            boolean enabled = !MtsControlBridge.isCameraTiltEnabled();
            MtsControlBridge.setCameraTiltEnabled(enabled);

            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                String key;
                if (!MtsControlBridge.isAvailable()) {
                    key = "message.superbaddon.mts_bridge_unavailable";
                } else {
                    key = enabled ? "message.superbaddon.mts_camera_tilt_on" : "message.superbaddon.mts_camera_tilt_off";
                }
                mc.player.displayClientMessage(Component.translatable(key), true);
            }
        }
    }
}
