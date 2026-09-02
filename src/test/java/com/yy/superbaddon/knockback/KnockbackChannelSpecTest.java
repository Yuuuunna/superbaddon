package com.yy.superbaddon.knockback;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnockbackChannelSpecTest {
    @Test
    void missingConfigInheritsOriginalValue() {
        KnockbackChannelSpec spec = KnockbackChannelSpec.fromJson(null, "test");

        assertSame(KnockbackChannelSpec.INHERIT, spec);
        assertEquals(2.5, spec.apply(2.5), 0.000001);
        assertTrue(spec.isPureInherit());
    }

    @Test
    void scaleModeMultipliesAndClamps() {
        JsonObject json = new JsonObject();
        json.addProperty("mode", "scale");
        json.addProperty("value", 0.25);
        json.addProperty("max", 0.8);

        KnockbackChannelSpec spec = KnockbackChannelSpec.fromJson(json, "test");

        assertEquals(0.5, spec.apply(2.0), 0.000001);
        assertEquals(0.8, spec.apply(10.0), 0.000001);
    }

    @Test
    void fixedModeReplacesOriginalValue() {
        JsonObject json = new JsonObject();
        json.addProperty("mode", "fixed");
        json.addProperty("value", 0.35);

        KnockbackChannelSpec spec = KnockbackChannelSpec.fromJson(json, "test");

        assertEquals(0.35, spec.apply(7.0), 0.000001);
    }

    @Test
    void capModeOnlyLimitsUpperBound() {
        JsonObject json = new JsonObject();
        json.addProperty("mode", "cap");
        json.addProperty("max", 0.75);

        KnockbackChannelSpec spec = KnockbackChannelSpec.fromJson(json, "test");

        assertEquals(0.4, spec.apply(0.4), 0.000001);
        assertEquals(0.75, spec.apply(2.0), 0.000001);
    }

    @Test
    void disabledModeReturnsZero() {
        JsonObject json = new JsonObject();
        json.addProperty("mode", "disabled");

        KnockbackChannelSpec spec = KnockbackChannelSpec.fromJson(json, "test");

        assertEquals(0.0, spec.apply(4.0), 0.000001);
    }

    @Test
    void unknownModeFallsBackToInherit() {
        JsonObject json = new JsonObject();
        json.addProperty("mode", "nonsense");
        json.addProperty("value", 99.0);

        KnockbackChannelSpec spec = KnockbackChannelSpec.fromJson(json, "test");

        assertSame(KnockbackChannelSpec.INHERIT, spec);
        assertEquals(1.2, spec.apply(1.2), 0.000001);
    }

    @Test
    void missingRequiredValueFallsBackToInherit() {
        JsonObject json = new JsonObject();
        json.addProperty("mode", "scale");

        KnockbackChannelSpec spec = KnockbackChannelSpec.fromJson(json, "test");

        assertSame(KnockbackChannelSpec.INHERIT, spec);
    }

    @Test
    void negativeOptionalClampIsIgnored() {
        JsonObject json = new JsonObject();
        json.addProperty("mode", "inherit");
        json.addProperty("max", -1.0);

        KnockbackChannelSpec spec = KnockbackChannelSpec.fromJson(json, "test");

        assertSame(KnockbackChannelSpec.INHERIT, spec);
    }
}
