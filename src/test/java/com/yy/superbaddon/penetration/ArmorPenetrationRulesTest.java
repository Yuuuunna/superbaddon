package com.yy.superbaddon.penetration;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmorPenetrationRulesTest {
    @Test
    void parsesDecimalAndPercentValues() {
        assertOptionalClose(0.35F, ArmorPenetrationRules.parsePenetrationValue("0.35"));
        assertOptionalClose(0.35F, ArmorPenetrationRules.parsePenetrationValue("35%"));
        assertOptionalClose(1.0F, ArmorPenetrationRules.parsePenetrationValue("100%"));
        assertTrue(ArmorPenetrationRules.parsePenetrationValue("not-a-number").isEmpty());
    }

    @Test
    void clampsAndClassifiesFullBypass() {
        assertEquals(1.0F, ArmorPenetrationRules.clampPenetration(2.0F), 0.0001F);
        assertEquals(0.0F, ArmorPenetrationRules.clampPenetration(-2.0F), 0.0001F);
        assertEquals(0.0F, ArmorPenetrationRules.clampPenetration(Float.NaN), 0.0001F);
        assertTrue(ArmorPenetrationRules.isFullBypass(0.999F));
        assertFalse(ArmorPenetrationRules.isFullBypass(0.998F));
    }

    @Test
    void splitsBypassDamageBeforeArmorMagicAndAbsorption() {
        ArmorPenetrationRules.DamageSplit split = ArmorPenetrationRules.splitDamage(
                100.0F,
                0.50F,
                10.0F,
                amount -> amount * 0.50F,
                amount -> amount
        );

        assertEquals(50.0F, split.bypassDamage(), 0.0001F);
        assertEquals(50.0F, split.defendableDamage(), 0.0001F);
        assertEquals(25.0F, split.amountAfterArmor(), 0.0001F);
        assertEquals(25.0F, split.amountAfterMagic(), 0.0001F);
        assertEquals(10.0F, split.absorptionConsumed(), 0.0001F);
        assertEquals(65.0F, split.healthDamage(), 0.0001F);
    }

    @Test
    void fallsOffExplosionPenetrationByDistance() {
        ArmorPenetrationRules.ExplosionFalloff settings =
                new ArmorPenetrationRules.ExplosionFalloff(0.35F, 0.35F, 2.0F);

        assertEquals(1.0F, ArmorPenetrationRules.explosionPenetrationAt(1.0F, 0.0F, settings), 0.0001F);
        assertEquals(0.9025F, ArmorPenetrationRules.explosionPenetrationAt(1.0F, 0.35F, settings), 0.0001F);
        assertEquals(0.35F, ArmorPenetrationRules.explosionPenetrationAt(1.0F, 1.0F, settings), 0.0001F);
        assertEquals(0.35F, ArmorPenetrationRules.explosionPenetrationAt(0.55F, 1.0F, settings), 0.0001F);
        assertEquals(0.20F, ArmorPenetrationRules.explosionPenetrationAt(0.20F, 1.0F, settings), 0.0001F);
    }

    private static void assertOptionalClose(float expected, Optional<Float> actual) {
        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get(), 0.0001F);
    }
}
