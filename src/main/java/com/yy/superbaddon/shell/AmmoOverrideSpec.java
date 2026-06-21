package com.yy.superbaddon.shell;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

public final class AmmoOverrideSpec {
    public enum Policy {
        REPLACE,
        APPEND;

        public static Policy from(String raw) {
            return "append".equalsIgnoreCase(raw) ? APPEND : REPLACE;
        }
    }

    private final boolean enabled;
    private final Policy policy;
    private final List<AmmoOption> allowed;

    public AmmoOverrideSpec(boolean enabled, Policy policy, List<AmmoOption> allowed) {
        this.enabled = enabled;
        this.policy = policy == null ? Policy.REPLACE : policy;
        this.allowed = List.copyOf(allowed == null ? List.of() : allowed);
    }

    public static AmmoOverrideSpec disabled() {
        return new AmmoOverrideSpec(false, Policy.REPLACE, List.of());
    }

    public static AmmoOverrideSpec fromJson(JsonObject object) {
        if (object == null) return disabled();
        boolean enabled = GsonHelper.getAsBoolean(object, "enabled", false);
        Policy policy = Policy.from(GsonHelper.getAsString(object, "policy", "replace"));
        ArrayList<AmmoOption> allowed = new ArrayList<>();
        readAllowed(allowed, object.get("allowed"));
        return new AmmoOverrideSpec(enabled, policy, allowed);
    }

    private static void readAllowed(List<AmmoOption> output, JsonElement element) {
        if (element == null || element.isJsonNull()) return;
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) addAllowed(output, child);
        } else {
            addAllowed(output, element);
        }
    }

    private static void addAllowed(List<AmmoOption> output, JsonElement element) {
        AmmoOption option = AmmoOption.fromJson(element);
        if (!option.matchKeys().isEmpty()) output.add(option);
    }

    public boolean enabled() {
        return enabled;
    }

    public Policy policy() {
        return policy;
    }

    public boolean replace() {
        return policy == Policy.REPLACE;
    }

    public List<AmmoOption> allowed() {
        return allowed;
    }

    public boolean active() {
        return enabled && !allowed.isEmpty();
    }
}
