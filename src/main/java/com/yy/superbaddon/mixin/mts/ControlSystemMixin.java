package com.yy.superbaddon.mixin.mts;

import com.yy.superbaddon.compat.mts.control.MtsControlBridge;
import com.yy.superbaddon.compat.mts.control.MtsControlBridgeImpl;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses MinecraftTransportSimulator's native client-side control handling so the SuperbWarfare
 * input bridge can drive the vehicle instead.
 *
 * <p>{@code ControlSystem.controlMultipart} is the single client-tick entry point (called from
 * {@code PartSeat}) that reads MTS keybinds and pushes control packets. Cancelling it at HEAD — only
 * for the controlling player and only while the bridge is enabled — stops MTS from fighting us over
 * the vehicle's control {@code ComputedVariable}s. Physics is untouched; we simply become the one who
 * sets those variables (done in T2).</p>
 *
 * <p>{@code @Pseudo} + {@code targets} string: MTS is an optional soft dependency. When it is absent
 * the target class does not exist and this mixin is silently skipped — never a crash. The mixin is
 * registered in the separate {@code superbaddon.mts.mixins.json} config ({@code "required": false}) for
 * the same reason. {@code remap = false} because the target is a mod method, not a Minecraft method.</p>
 */
@Pseudo
@Mixin(targets = "minecrafttransportsimulator.systems.ControlSystem", remap = false)
public class ControlSystemMixin {

    @Inject(method = "controlMultipart", at = @At("HEAD"), cancellable = true, remap = false)
    private static void superbaddon$suppressNativeInput(AEntityF_Multipart<?> multipart, boolean isPlayerController, double mouseXDelta, double mouseYDelta, CallbackInfo ci) {
        if (!isPlayerController || !MtsControlBridge.isInputOverrideEnabled()) {
            return;
        }
        // A screen is open (inventory, chat, MTS panel/radio...) — let the player interact with it;
        // don't drive and don't cancel, so MTS's own GUI-aware handling runs normally.
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        // Only take over actual vehicles; anything else (e.g. placed gun emplacements) keeps MTS controls.
        if (multipart instanceof EntityVehicleF_Physics vehicle) {
            MtsControlBridgeImpl.drive(vehicle, mouseXDelta, mouseYDelta);
            ci.cancel();
        }
    }
}
