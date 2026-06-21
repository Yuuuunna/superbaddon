package com.yy.superbaddon.compat.mts.control;

import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.packets.instances.PacketPartGun;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.util.Mth;

/**
 * MTS-typed implementation of the control bridge: reads SuperbWarfare-style input and drives the
 * vehicle's control {@link ComputedVariable}s via the same packet MTS itself uses, leaving physics
 * untouched. Only loaded after {@link MtsControlBridge#isAvailable()} (the {@code ControlSystem}
 * mixin and the {@code isAvailable()}-guarded tick that reach this only exist/run when MTS is
 * present), so referencing MTS types here is safe.
 *
 * <p>Mapping (SuperbWarfare "direct control", confirmed with the user):</p>
 * <ul>
 *   <li><b>Ground</b>: W = throttle, S = brake, A/D = steering (rudder), left-click = fire.</li>
 *   <li><b>Aircraft</b>: W/S = throttle up/down, A/D = roll (aileron), mouse = stick
 *       (mouse-Y -> elevator/pitch, mouse-X -> rudder/yaw), left-click = fire.</li>
 * </ul>
 *
 * <p>First/third-person switching is left to vanilla F5: MTS derives its camera mode from
 * {@code Minecraft.options.getCameraType()} each tick, so the perspective key already drives it even
 * though we cancel MTS's own control handling.</p>
 */
public final class MtsControlBridgeImpl {
    private MtsControlBridgeImpl() {
    }

    /** Degrees of control-surface deflection per unit of mouse delta. Tunable. */
    private static final double MOUSE_PITCH_RATE = 0.5;
    private static final double MOUSE_YAW_RATE = 0.5;
    /** Degrees per tick the aileron ramps up while A/D is held (gives a start-up accel feel). Tunable. */
    private static final double AILERON_RAMP_RATE = 2.5;
    /** Degrees per tick a surface springs back toward neutral when there is no input. Tunable. */
    private static final double SURFACE_RETURN_RATE = 1.5;
    /** Mouse movement below this magnitude counts as "still", so the surface self-centers. */
    private static final double MOUSE_DEADZONE = 1.0e-3;
    /** Throttle ramp per tick while W/S held (fraction of MAX_THROTTLE). Tunable. */
    private static final double THROTTLE_STEP = EntityVehicleF_Physics.MAX_THROTTLE / 50.0;
    /** Don't bother sending a packet for changes smaller than this. */
    private static final double EPSILON = 1.0e-4;

    private static EntityVehicleF_Physics activeVehicle;
    private static double throttleTarget;
    private static double aileronTarget;
    private static double elevatorTarget;
    private static double rudderTarget;
    private static boolean lastFire;

    /** The user's original MTS mouseYoke value while we force it on for view-lock; null = not forcing. */
    private static Boolean savedMouseYoke;

    /**
     * Drives one tick of input for the vehicle the local player is controlling. Called from the
     * {@code ControlSystem.controlMultipart} mixin (MTS's own per-tick control entry point), which
     * hands us the vehicle and the rider's mouse deltas.
     */
    public static void drive(EntityVehicleF_Physics vehicle, double mouseXDelta, double mouseYDelta) {
        syncAccumulators(vehicle);

        Options opt = Minecraft.getInstance().options;
        boolean forward = opt.keyUp.isDown();
        boolean back = opt.keyDown.isDown();
        boolean left = opt.keyLeft.isDown();
        boolean right = opt.keyRight.isDown();

        if (vehicle.definition.motorized.isAircraft) {
            driveAircraft(vehicle, forward, back, left, right, mouseXDelta, mouseYDelta);
        } else {
            driveGround(vehicle, forward, back, left, right);
        }

        fireGuns(vehicle, opt.keyAttack.isDown());
    }

    /**
     * Per-tick view-lock maintenance, called from the client tick (only when MTS is present). While
     * the bridge is active and the player controls an aircraft, MTS's own {@code mouseYoke} setting is
     * forced on so {@code PartSeat.updateRider} anchors the seated view and feeds raw mouse deltas to
     * the stick (mouse = stick, not free-look). The original setting is restored on exit. The value is
     * changed transiently (no {@code saveToDisk}), so the user's on-disk MTS config is left untouched.
     */
    public static void tickViewLock() {
        boolean wantLock = MtsControlBridge.isInputOverrideEnabled()
                && isAircraftController(InterfaceManager.clientInterface.getClientPlayer());
        if (wantLock) {
            if (savedMouseYoke == null) {
                savedMouseYoke = ConfigSystem.client.controlSettings.mouseYoke.value;
                ConfigSystem.client.controlSettings.mouseYoke.value = true;
            }
        } else if (savedMouseYoke != null) {
            ConfigSystem.client.controlSettings.mouseYoke.value = savedMouseYoke;
            savedMouseYoke = null;
        }
    }

    /**
     * Computes the tilt of the MTS vehicle the local player is seated in, expressed in <b>Minecraft</b>
     * angle convention so the camera mixin can use it exactly like SuperbWarfare uses its own vehicle's
     * {@code getRoll()/getViewXRot()/getViewYRot()}. Returns {@code [roll, pitch, vehicleYaw]} (degrees,
     * interpolated by {@code partialTicks}), or {@code null} when not seated in an MTS vehicle.
     *
     * <p>The MTS-&gt;MC mapping is taken from {@code WrapperEntity.setOrientation}: MC yaw is the negated
     * MTS yaw, MC pitch equals the MTS pitch, and (because MTS's X/Z basis matrices match JOML's
     * {@code Axis.XP}/{@code Axis.ZP}) MC roll equals the MTS roll. Angles are read straight off the
     * rotation-matrix elements — never via {@code convertToAngles()} — so this never mutates MTS's cached
     * matrix state on the render thread.</p>
     */
    public static float[] computeRiddenVehicleTilt(float partialTicks) {
        IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
        if (player == null) {
            return null;
        }
        AEntityB_Existing riding = player.getEntityRiding();
        if (!(riding instanceof PartSeat seat) || seat.vehicleOn == null) {
            return null;
        }
        EntityVehicleF_Physics vehicle = seat.vehicleOn;

        // Interpolate each component across the previous/current orientation in MC convention. rotLerp
        // takes the shortest path across the 0/360 wrap (matters for yaw; harmless for roll/pitch).
        float prevYaw = mcYaw(vehicle.prevOrientation);
        float curYaw = mcYaw(vehicle.orientation);
        float prevPitch = mcPitch(vehicle.prevOrientation);
        float curPitch = mcPitch(vehicle.orientation);
        float prevRoll = mcRoll(vehicle.prevOrientation);
        float curRoll = mcRoll(vehicle.orientation);

        return new float[]{
                Mth.rotLerp(partialTicks, prevRoll, curRoll),
                Mth.rotLerp(partialTicks, prevPitch, curPitch),
                Mth.rotLerp(partialTicks, prevYaw, curYaw)
        };
    }

    /** MC yaw = -(MTS yaw); MTS yaw = atan2(m02, m22) per {@code RotationMatrix.convertToAngles}. */
    private static float mcYaw(RotationMatrix m) {
        return (float) -Math.toDegrees(Math.atan2(m.m02, m.m22));
    }

    /** MC pitch = MTS pitch = -asin(m12); clamp guards asin against tiny floating-point overshoot. */
    private static float mcPitch(RotationMatrix m) {
        return (float) Math.toDegrees(-Math.asin(Mth.clamp(m.m12, -1.0, 1.0)));
    }

    /** MC roll = MTS roll = atan2(m10, m11); MTS reports 0 under gimbal lock (pitch at +/-90). */
    private static float mcRoll(RotationMatrix m) {
        if (m.m12 == 1.0 || m.m12 == -1.0) {
            return 0f;
        }
        return (float) Math.toDegrees(Math.atan2(m.m10, m.m11));
    }

    private static boolean isAircraftController(IWrapperPlayer player) {
        if (player == null) {
            return false;
        }
        AEntityB_Existing riding = player.getEntityRiding();
        return riding instanceof PartSeat seat
                && seat.placementDefinition.isController
                && seat.vehicleOn != null
                && seat.vehicleOn.definition.motorized.isAircraft;
    }

    /** Resets time-integrating axes to the vehicle's live state when the controlled vehicle changes. */
    private static void syncAccumulators(EntityVehicleF_Physics vehicle) {
        if (vehicle != activeVehicle) {
            activeVehicle = vehicle;
            throttleTarget = vehicle.throttleVar.currentValue;
            aileronTarget = vehicle.aileronInputVar.currentValue;
            elevatorTarget = vehicle.elevatorInputVar.currentValue;
            rudderTarget = vehicle.rudderInputVar.currentValue;
        }
    }

    private static void driveGround(EntityVehicleF_Physics v, boolean forward, boolean back, boolean left, boolean right) {
        set(v.throttleVar, forward ? EntityVehicleF_Physics.MAX_THROTTLE : 0);
        set(v.brakeVar, back ? EntityVehicleF_Physics.MAX_BRAKE : 0);
        set(v.rudderInputVar, steer(left, right, EntityVehicleF_Physics.MAX_RUDDER_ANGLE));
    }

    private static void driveAircraft(EntityVehicleF_Physics v, boolean forward, boolean back, boolean left, boolean right, double mouseXDelta, double mouseYDelta) {
        if (forward) {
            throttleTarget = Math.min(EntityVehicleF_Physics.MAX_THROTTLE, throttleTarget + THROTTLE_STEP);
        }
        if (back) {
            throttleTarget = Math.max(0, throttleTarget - THROTTLE_STEP);
        }
        set(v.throttleVar, throttleTarget);

        // Roll: A/D ramp the aileron up while held and spring it back to level when released, so it
        // accelerates in instead of snapping hard to full deflection.
        double aileronPush = (right ? AILERON_RAMP_RATE : 0) - (left ? AILERON_RAMP_RATE : 0);
        aileronTarget = pushOrCenter(aileronTarget, aileronPush, EntityVehicleF_Physics.MAX_AILERON_ANGLE);
        set(v.aileronInputVar, aileronTarget);

        // Pitch/yaw: mouse Y -> elevator, mouse X -> rudder (negated to match MTS's sign convention).
        // While the mouse moves it deflects the surface; the instant it stops, the surface self-centers
        // like a released stick — a mouse has no spring of its own.
        double elevatorPush = Math.abs(mouseYDelta) > MOUSE_DEADZONE ? -mouseYDelta * MOUSE_PITCH_RATE : 0;
        double rudderPush = Math.abs(mouseXDelta) > MOUSE_DEADZONE ? -mouseXDelta * MOUSE_YAW_RATE : 0;
        elevatorTarget = pushOrCenter(elevatorTarget, elevatorPush, EntityVehicleF_Physics.MAX_ELEVATOR_ANGLE);
        rudderTarget = pushOrCenter(rudderTarget, rudderPush, EntityVehicleF_Physics.MAX_RUDDER_ANGLE);
        set(v.elevatorInputVar, elevatorTarget);
        set(v.rudderInputVar, rudderTarget);
    }

    /** Left-click fires whatever gun on the vehicle this player controls. Sent only on state change. */
    private static void fireGuns(EntityVehicleF_Physics vehicle, boolean fire) {
        if (fire == lastFire) {
            return;
        }
        lastFire = fire;
        IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
        PacketPartGun.Request request = fire ? PacketPartGun.Request.TRIGGER_ON : PacketPartGun.Request.TRIGGER_OFF;
        for (APart part : vehicle.allParts) {
            if (part instanceof PartGun gun && player.equals(gun.getGunController())) {
                InterfaceManager.packetInterface.sendToServer(new PacketPartGun(gun, request));
            }
        }
    }

    /** Left/right key pair into a symmetric ±bound deflection (no special cases for "both" or "none"). */
    private static double steer(boolean left, boolean right, double bound) {
        return (left ? -bound : 0) + (right ? bound : 0);
    }

    /**
     * Spring-loaded surface integrator: while {@code push} is non-zero it ramps the value (giving an
     * acceleration feel and letting you hold a deflection), and when {@code push} is zero it eases the
     * value back toward neutral by {@link #SURFACE_RETURN_RATE} — so both keys and mouse behave like a
     * self-centering stick rather than a hard on/off.
     */
    private static double pushOrCenter(double value, double push, double bound) {
        if (push != 0) {
            value += push;
        } else if (value > SURFACE_RETURN_RATE) {
            value -= SURFACE_RETURN_RATE;
        } else if (value < -SURFACE_RETURN_RATE) {
            value += SURFACE_RETURN_RATE;
        } else {
            value = 0;
        }
        return clamp(value, bound);
    }

    /** Sends the variable only when it actually differs from what the server last reported. */
    private static void set(ComputedVariable variable, double target) {
        if (Math.abs(variable.currentValue - target) > EPSILON) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(variable, target));
        }
    }

    private static double clamp(double value, double bound) {
        return value < -bound ? -bound : (value > bound ? bound : value);
    }
}
