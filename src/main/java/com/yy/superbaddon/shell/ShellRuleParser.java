package com.yy.superbaddon.shell;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

public final class ShellRuleParser {
    private ShellRuleParser() {
    }

    public static List<ShellRule> parse(ResourceLocation id, JsonElement element) {
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("root is not an object");
        }

        JsonObject root = element.getAsJsonObject();
        int format = GsonHelper.getAsInt(root, "format", -1);
        if (format != 5) {
            throw new IllegalArgumentException("unsupported shell_ejection config format " + format + "; this build only accepts format 5");
        }
        if (!root.has("rules") || !root.get("rules").isJsonArray()) {
            throw new IllegalArgumentException("format 5 config must contain a rules array");
        }

        ArrayList<ShellRule> loaded = new ArrayList<>();
        int index = 0;
        for (JsonElement child : root.getAsJsonArray("rules")) {
            if (!child.isJsonObject()) {
                index++;
                continue;
            }
            JsonObject childObject = child.getAsJsonObject();
            if (GsonHelper.getAsBoolean(childObject, "enabled", true)) {
                loaded.add(ShellRule.fromJson(new ResourceLocation(id.getNamespace(), id.getPath() + "/" + index), childObject));
            }
            index++;
        }
        return loaded;
    }
}
