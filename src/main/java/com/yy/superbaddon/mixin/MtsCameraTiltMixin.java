package com.yy.superbaddon.mixin;

import com.yy.superbaddon.compat.mts.control.MtsControlBridge;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Rolls/pitches the first-person camera to follow the MTS vehicle the player is seated in, so driving a
 * MinecraftTransportSimulator vehicle with the SuperbWarfare control bridge no longer leaves the view
 * stuck horizontal.
 *
 * <p>SuperbWarfare's own {@code GameRendererMixin} only tilts the camera for its own
 * {@code VehicleEntity}; an MTS rider sits on a {@code BuilderEntityLinkedSeat}, never a
 * {@code VehicleEntity}, so that path never fires. This mixin re-applies the <b>same</b> tilt maths at
 * the <b>same</b> injection point ({@code Camera.setup} inside {@code renderLevel}), feeding it the MTS
 * vehicle's roll/pitch/yaw (already converted to Minecraft angle convention by the bridge).</p>
 *
 * <p>References no MTS types — all MTS-typed work is behind the {@link MtsControlBridge} facade, which
 * returns {@code null} (no tilt) unless MTS is present, the rider opted in, and they're seated in an MTS
 * vehicle. So this mixin is safe to load and apply even without MTS installed.</p>
 */
@Mixin(GameRenderer.class)
public class MtsCameraTiltMixin {

    @Shadow
    @Final
    private Camera mainCamera;

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V"))
    public void superbaddon$mtsCameraTilt(float tickDelta, long limitTime, PoseStack matrices, CallbackInfo ci) {
        // First-person only, mirroring SuperbWarfare's `!mainCamera.isDetached()` gate. Third-person keeps
        // the vanilla orbit so the player can still see the tilted vehicle from outside.
        if (mainCamera.isDetached()) {
            return;
        }

        float[] tilt = MtsControlBridge.getRiddenVehicleTilt(tickDelta);
        if (tilt == null) {
            return;
        }
        float roll = tilt[0];
        float pitch = tilt[1];
        float vehicleYaw = tilt[2];

        // How far the look direction has swung off the vehicle's nose. This blends roll into pitch as you
        // turn to look sideways, so a banked turn doesn't throw the horizon on its side. (From SuperbWarfare.)
        float a = Mth.wrapDegrees(mainCamera.getYRot() - vehicleYaw);
        float r = (Mth.abs(a) - 90f) / 90f;
        float r2;
        if (Mth.abs(a) <= 90f) {
            r2 = a / 90f;
        } else if (a < 0) {
            r2 = -(180f + a) / 90f;
        } else {
            r2 = (180f - a) / 90f;
        }

        // Pure roll about the camera origin — MTS already anchors the first-person camera at the rotated
        // rider-eye position (InterfaceEventsEntityRendering.onIVCameraSetup -> invoke_setPosition), so the
        // view tilts in place and stays in the seat. We must NOT re-translate the eye here (SuperbWarfare's
        // own vehicles need that because they leave the camera at feet+straight-up eye; MTS does not) — doing
        // so adds a tilt-proportional offset that throws the camera out of the cabin during manoeuvres.
        matrices.mulPose(Axis.ZP.rotationDegrees(-r * roll - r2 * pitch));
    }
}
