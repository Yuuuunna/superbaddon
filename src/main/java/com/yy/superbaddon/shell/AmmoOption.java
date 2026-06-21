package com.yy.superbaddon.shell;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AmmoOption {
    private static final Pattern AMMO_PATTERN = Pattern.compile("^(?<count>(\\d+)?)\\s*(?<prefix>[@#]?)(?<id>[\\w.:-]+)\\s*(?<data>(\\{.*})?)$");

    private final String spec;
    private final String ammoId;
    private final JsonObject consumerJson;
    private final Set<String> matchKeys;

    private AmmoOption(String spec, String ammoId, JsonObject consumerJson) {
        this.spec = normalizeAmmoSpec(spec);
        this.ammoId = normalizeAmmoId(ammoId);
        this.consumerJson = consumerJson == null ? null : consumerJson.deepCopy();

        LinkedHashSet<String> keys = new LinkedHashSet<>();
        if (!this.spec.isBlank()) keys.add(this.spec);
        if (!this.ammoId.isBlank()) keys.add(this.ammoId);
        this.matchKeys = Set.copyOf(keys);
    }

    public static AmmoOption fromString(String value) {
        String spec = normalizeAmmoSpec(value);
        return new AmmoOption(spec, extractAmmoId(spec), null);
    }

    public static AmmoOption fromJson(JsonElement element) {
        if (element == null || element.isJsonNull()) return fromString("");
        if (element.isJsonPrimitive()) return fromString(element.getAsString());
        if (!element.isJsonObject()) return fromString("");

        JsonObject object = element.getAsJsonObject();
        JsonObject consumer = null;
        if (object.has("consumer") && object.get("consumer").isJsonObject()) {
            consumer = object.getAsJsonObject("consumer").deepCopy();
        } else {
            consumer = object.deepCopy();
            consumer.remove("casing");
            consumer.remove("Casing");
            consumer.remove("comment");
            consumer.remove("Comment");
        }

        String spec = firstString(object, "ammo", "Ammo", "id", "Id");
        if (spec.isBlank()) spec = firstString(consumer, "Ammo", "ammo");
        String id = extractAmmoId(spec);
        return new AmmoOption(spec, id, consumer);
    }

    public String spec() {
        return spec;
    }

    public String ammoId() {
        return ammoId;
    }

    public JsonObject consumerJson() {
        return consumerJson == null ? null : consumerJson.deepCopy();
    }

    public Set<String> matchKeys() {
        return matchKeys;
    }

    public JsonElement toConsumerElement() {
        if (consumerJson != null) return consumerJson.deepCopy();
        if (!spec.isBlank()) return new JsonPrimitive(spec);
        return new JsonPrimitive(ammoId);
    }

    public boolean matches(String selectedAmmoId, String selectedAmmoSpec) {
        String id = normalizeAmmoId(selectedAmmoId);
        String spec = normalizeAmmoSpec(selectedAmmoSpec);
        return (!id.isBlank() && matchKeys.contains(id)) || (!spec.isBlank() && matchKeys.contains(spec));
    }

    public static String normalizeAmmoSpec(String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    public static String normalizeAmmoId(String raw) {
        if (raw == null) return "";
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) return "";
        ResourceLocation location = ResourceLocation.tryParse(value);
        return location == null ? value : location.toString();
    }

    public static String extractAmmoId(String rawSpec) {
        String spec = normalizeAmmoSpec(rawSpec);
        if (spec.isBlank()) return "";
        Matcher matcher = AMMO_PATTERN.matcher(spec);
        if (!matcher.matches()) return normalizeAmmoId(spec);
        return normalizeAmmoId(matcher.group("id"));
    }

    private static String firstString(JsonObject object, String... keys) {
        if (object == null) return "";
        for (String key : keys) {
            if (object.has(key) && object.get(key).isJsonPrimitive()) {
                return GsonHelper.convertToString(object.get(key), key);
            }
        }
        return "";
    }
}
