package com.yy.superbaddon.knockback;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yy.superbaddon.shell.AmmoOption;
import com.yy.superbaddon.shell.ShellContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class KnockbackTable {
    public static final KnockbackTable INHERIT = new KnockbackTable(KnockbackSpec.inherit(), List.of());

    private final KnockbackSpec base;
    private final List<AmmoKnockback> byAmmo;

    private KnockbackTable(KnockbackSpec base, List<AmmoKnockback> byAmmo) {
        this.base = base == null ? KnockbackSpec.inherit() : base;
        this.byAmmo = List.copyOf(byAmmo == null ? List.of() : byAmmo);
    }

    public static KnockbackTable fromJson(JsonObject object) {
        if (object == null || object.size() == 0) return INHERIT;

        KnockbackSpec base = readSpec(object, "knockback");
        ArrayList<AmmoKnockback> byAmmo = new ArrayList<>();
        if (object.has("by_ammo") && object.get("by_ammo").isJsonObject()) {
            JsonObject table = object.getAsJsonObject("by_ammo");
            for (Map.Entry<String, JsonElement> entry : table.entrySet()) {
                AmmoOption ammo = AmmoOption.fromString(entry.getKey());
                if (ammo.matchKeys().isEmpty()) continue;
                if (!entry.getValue().isJsonObject()) continue;
                KnockbackSpec spec = readSpec(entry.getValue().getAsJsonObject(), "knockback.by_ammo." + entry.getKey());
                byAmmo.add(new AmmoKnockback(ammo, spec));
            }
        }

        KnockbackTable result = new KnockbackTable(base, byAmmo);
        return result.isPureInherit() ? INHERIT : result;
    }

    public KnockbackSpec selected(ShellContext context) {
        if (context != null) {
            for (AmmoKnockback entry : byAmmo) {
                if (entry.ammo().matches(context.ammoId(), context.ammoSpec())) {
                    return base.overlay(entry.spec());
                }
            }
        }
        return base;
    }

    public boolean isPureInherit() {
        return base.isPureInherit() && byAmmo.stream().allMatch(entry -> entry.spec().isPureInherit());
    }

    private static KnockbackSpec readSpec(JsonObject object, String path) {
        KnockbackChannelSpec direct = KnockbackChannelSpec.INHERIT;
        KnockbackChannelSpec explosion = KnockbackChannelSpec.INHERIT;

        if (object.has("direct") && object.get("direct").isJsonObject()) {
            direct = KnockbackChannelSpec.fromJson(object.getAsJsonObject("direct"), path + ".direct");
        }
        if (object.has("explosion") && object.get("explosion").isJsonObject()) {
            explosion = KnockbackChannelSpec.fromJson(object.getAsJsonObject("explosion"), path + ".explosion");
        }
        return new KnockbackSpec(direct, explosion);
    }

    private record AmmoKnockback(AmmoOption ammo, KnockbackSpec spec) {
    }
}
