package com.yy.superbaddon.shell;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShellRuleKnockbackTest {
    @AfterEach
    void clearRules() {
        ShellRuleSet.replace(List.of());
    }

    @Test
    void ruleParsesKnockbackWithoutEjection() {
        ShellRule rule = ShellRule.fromJson(new ResourceLocation("test", "knockback"), knockbackRule(100, 0.25));
        ShellContext context = context("superbwarfare:small_shell_he");

        assertTrue(rule.selectedEjection(context).isEmpty());
        assertEquals(0.5, rule.selectedKnockback(context).orElseThrow().explosion().apply(2.0), 0.000001);
    }

    @Test
    void ejectionMatchingIgnoresKnockbackOnlyRule() {
        ShellRule knockbackOnly = ShellRule.fromJson(new ResourceLocation("test", "knockback"), knockbackRule(100, 0.25));
        ShellRule ejectionOnly = ShellRule.fromJson(new ResourceLocation("test", "ejection"), ejectionRule(50));
        ShellContext context = context("superbwarfare:small_shell_he");

        ShellRuleSet.replace(List.of(knockbackOnly, ejectionOnly));

        assertSame(ejectionOnly, ShellRuleSet.matchEjection(context).orElseThrow());
        assertSame(knockbackOnly, ShellRuleSet.matchKnockback(context).orElseThrow().rule());
    }

    @Test
    void inheritOnlyKnockbackDoesNotMatchKnockbackLookup() {
        JsonObject json = new JsonObject();
        json.addProperty("priority", 100);
        json.add("target", new JsonObject());
        json.add("knockback", new JsonObject());
        ShellRule rule = ShellRule.fromJson(new ResourceLocation("test", "inherit"), json);

        ShellRuleSet.replace(List.of(rule));

        assertEquals(Optional.empty(), ShellRuleSet.matchKnockback(context("superbwarfare:small_shell_he")));
    }

    private static JsonObject knockbackRule(int priority, double explosionScale) {
        JsonObject root = new JsonObject();
        root.addProperty("priority", priority);
        root.add("target", target());

        JsonObject knockback = new JsonObject();
        JsonObject explosion = new JsonObject();
        explosion.addProperty("mode", "scale");
        explosion.addProperty("value", explosionScale);
        knockback.add("explosion", explosion);
        root.add("knockback", knockback);
        return root;
    }

    private static JsonObject ejectionRule(int priority) {
        JsonObject root = new JsonObject();
        root.addProperty("priority", priority);
        root.add("target", target());

        JsonObject ejection = new JsonObject();
        JsonObject defaults = new JsonObject();
        defaults.addProperty("mode", "none");
        ejection.add("default", defaults);
        root.add("ejection", ejection);
        return root;
    }

    private static JsonObject target() {
        JsonObject target = new JsonObject();
        target.addProperty("vehicles", "superbwarfare:ah_6");
        target.addProperty("weapons", "cannon");
        return target;
    }

    private static ShellContext context(String ammoId) {
        return new ShellContext(
                null,
                null,
                null,
                "superbwarfare:test_gun",
                ammoId,
                "",
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
