package com.yy.superbaddon.shell;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.phys.Vec3;

public final class ShellEjectionPoint {
    private final String id;
    private final String casingItem;
    private final int count;
    private final float chance;
    private final int pickupDelay;
    private final Vec3 offset;
    private final Vec3 velocity;
    private final double randomVelocity;

    public ShellEjectionPoint(
            String id,
            String casingItem,
            int count,
            float chance,
            int pickupDelay,
            Vec3 offset,
            Vec3 velocity,
            double randomVelocity
    ) {
        this.id = id == null || id.isBlank() ? "default" : ShellRule.normalizeId(id);
        this.casingItem = ShellRule.normalizeId(casingItem);
        this.count = Math.max(1, count);
        this.chance = Math.max(0.0f, Math.min(1.0f, chance));
        this.pickupDelay = Math.max(0, pickupDelay);
        this.offset = offset;
        this.velocity = velocity;
        this.randomVelocity = Math.max(0.0, randomVelocity);
    }

    public String id() {
        return id;
    }

    public String casingItem() {
        return casingItem;
    }

    public int count() {
        return count;
    }

    public float chance() {
        return chance;
    }

    public int pickupDelay() {
        return pickupDelay;
    }

    public Vec3 offset() {
        return offset;
    }

    public Vec3 velocity() {
        return velocity;
    }

    public double randomVelocity() {
        return randomVelocity;
    }

    public static ShellEjectionPoint fromJson(JsonObject object, ShellEjectionPoint fallback) {
        String id = GsonHelper.getAsString(object, "id", fallback.id());
        String casing = GsonHelper.getAsString(object, "casing", fallback.casingItem());
        int count = GsonHelper.getAsInt(object, "count", fallback.count());
        float chance = GsonHelper.getAsFloat(object, "chance", fallback.chance());
        int pickupDelay = GsonHelper.getAsInt(object, "pickup_delay", fallback.pickupDelay());
        Vec3 offset = readVec3(object, "offset", fallback.offset());
        Vec3 velocity = readVec3(object, "velocity", fallback.velocity());
        double randomVelocity = GsonHelper.getAsDouble(object, "random_velocity", fallback.randomVelocity());
        return new ShellEjectionPoint(id, casing, count, chance, pickupDelay, offset, velocity, randomVelocity);
    }

    public static Vec3 readVec3(JsonObject object, String key, Vec3 fallback) {
        if (!object.has(key) || !object.get(key).isJsonArray()) return fallback;
        JsonArray array = object.getAsJsonArray(key);
        if (array.size() < 3) return fallback;
        return new Vec3(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
    }
}
