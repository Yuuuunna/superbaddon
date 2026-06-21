package com.yy.superbaddon.shell;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class ShellEjectionSpec {
    private static final ShellEjectionSpec NONE = new ShellEjectionSpec(DisposalMode.NONE, false, List.of(defaultPoint()));

    private final DisposalMode mode;
    private final boolean fallbackToDrop;
    private final List<ShellEjectionPoint> points;

    public ShellEjectionSpec(DisposalMode mode, boolean fallbackToDrop, List<ShellEjectionPoint> points) {
        this.mode = mode == null ? DisposalMode.DROP : mode;
        this.fallbackToDrop = fallbackToDrop;
        this.points = List.copyOf(points == null || points.isEmpty() ? List.of(defaultPoint()) : points);
    }

    public static ShellEjectionSpec none() {
        return NONE;
    }

    public static ShellEjectionSpec fromJson(JsonObject object) {
        if (object == null) return none();
        DisposalMode mode = DisposalMode.from(GsonHelper.getAsString(object, "mode", "drop"));
        boolean fallbackToDrop = GsonHelper.getAsBoolean(object, "fallback_to_drop", false);
        ShellEjectionPoint fallbackPoint = new ShellEjectionPoint(
                "default",
                GsonHelper.getAsString(object, "casing", "superbaddon:brass_casing"),
                GsonHelper.getAsInt(object, "count", 1),
                GsonHelper.getAsFloat(object, "chance", 1.0f),
                GsonHelper.getAsInt(object, "pickup_delay", 20),
                ShellEjectionPoint.readVec3(object, "offset", new Vec3(0.2, -0.05, -0.15)),
                ShellEjectionPoint.readVec3(object, "velocity", new Vec3(0.08, 0.08, -0.02)),
                GsonHelper.getAsDouble(object, "random_velocity", 0.035)
        );
        return new ShellEjectionSpec(mode, fallbackToDrop, readPoints(object, fallbackPoint));
    }

    private static List<ShellEjectionPoint> readPoints(JsonObject ejection, ShellEjectionPoint fallbackPoint) {
        JsonArray array = null;
        if (ejection.has("ports") && ejection.get("ports").isJsonArray()) {
            array = ejection.getAsJsonArray("ports");
        } else if (ejection.has("points") && ejection.get("points").isJsonArray()) {
            array = ejection.getAsJsonArray("points");
        }

        if (array == null) return List.of(fallbackPoint);

        ArrayList<ShellEjectionPoint> points = new ArrayList<>();
        for (JsonElement element : array) {
            if (element.isJsonObject()) {
                points.add(ShellEjectionPoint.fromJson(element.getAsJsonObject(), fallbackPoint));
            }
        }
        return points.isEmpty() ? List.of(fallbackPoint) : List.copyOf(points);
    }

    private static ShellEjectionPoint defaultPoint() {
        return new ShellEjectionPoint(
                "default",
                "superbaddon:brass_casing",
                1,
                1.0f,
                20,
                new Vec3(0.2, -0.05, -0.15),
                new Vec3(0.08, 0.08, -0.02),
                0.035
        );
    }

    public DisposalMode mode() {
        return mode;
    }

    public boolean fallbackToDrop() {
        return fallbackToDrop;
    }

    public List<ShellEjectionPoint> points() {
        return points;
    }
}
