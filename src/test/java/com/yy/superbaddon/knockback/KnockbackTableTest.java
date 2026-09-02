package com.yy.superbaddon.knockback;

import com.google.gson.JsonObject;
import com.yy.superbaddon.shell.ShellContext;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnockbackTableTest {
    @Test
    void missingTableIsPureInherit() {
        KnockbackTable table = KnockbackTable.fromJson(null);

        assertTrue(table.selected(context("superbwarfare:small_shell_he", "")).isPureInherit());
    }

    @Test
    void baseChannelsApplyWhenAmmoDoesNotMatch() {
        JsonObject root = new JsonObject();
        root.add("explosion", channel("scale", 0.25));

        KnockbackSpec selected = KnockbackTable.fromJson(root).selected(context("superbwarfare:small_shell_he", ""));

        assertEquals(2.0, selected.direct().apply(2.0), 0.000001);
        assertEquals(0.5, selected.explosion().apply(2.0), 0.000001);
    }

    @Test
    void ammoOverlayReplacesOnlyDefinedChannels() {
        JsonObject root = new JsonObject();
        root.add("direct", channel("scale", 0.5));
        root.add("explosion", channel("scale", 0.25));

        JsonObject byAmmo = new JsonObject();
        JsonObject ap = new JsonObject();
        ap.add("explosion", disabled());
        byAmmo.add("superbwarfare:small_shell_ap", ap);
        root.add("by_ammo", byAmmo);

        KnockbackSpec selected = KnockbackTable.fromJson(root).selected(context("superbwarfare:small_shell_ap", ""));

        assertEquals(1.0, selected.direct().apply(2.0), 0.000001);
        assertEquals(0.0, selected.explosion().apply(2.0), 0.000001);
    }

    @Test
    void ammoSpecMatchUsesAmmoOptionNormalization() {
        JsonObject root = new JsonObject();
        JsonObject byAmmo = new JsonObject();
        JsonObject specEntry = new JsonObject();
        specEntry.add("direct", channel("fixed", 0.3));
        byAmmo.add("1 #SuperbWarfare:Small_Shell_HE", specEntry);
        root.add("by_ammo", byAmmo);

        KnockbackSpec selected = KnockbackTable.fromJson(root).selected(context("", "1 #superbwarfare:small_shell_he"));

        assertEquals(0.3, selected.direct().apply(4.0), 0.000001);
    }

    private static JsonObject channel(String mode, double value) {
        JsonObject json = new JsonObject();
        json.addProperty("mode", mode);
        json.addProperty("value", value);
        return json;
    }

    private static JsonObject disabled() {
        JsonObject json = new JsonObject();
        json.addProperty("mode", "disabled");
        return json;
    }

    private static ShellContext context(String ammoId, String ammoSpec) {
        return new ShellContext(
                null,
                null,
                null,
                "superbwarfare:test_gun",
                ammoId,
                ammoSpec,
                "superbwarfare:test_projectile",
                "superbwarfare:ah_6",
                "superbwarfare:ah_6/cannon",
                "cannon",
                0,
                new Vec3(0.0, 0.0, 0.0),
                new Vec3(0.0, 0.0, 1.0),
                1
        );
    }
}
