package com.yy.superbaddon.knockback;

public record KnockbackSpec(KnockbackChannelSpec direct, KnockbackChannelSpec explosion) {
    public static final KnockbackSpec INHERIT =
            new KnockbackSpec(KnockbackChannelSpec.INHERIT, KnockbackChannelSpec.INHERIT);

    public KnockbackSpec {
        direct = direct == null ? KnockbackChannelSpec.INHERIT : direct;
        explosion = explosion == null ? KnockbackChannelSpec.INHERIT : explosion;
    }

    public static KnockbackSpec inherit() {
        return INHERIT;
    }

    public boolean isPureInherit() {
        return direct.isPureInherit() && explosion.isPureInherit();
    }

    public KnockbackSpec overlay(KnockbackSpec override) {
        if (override == null || override.isPureInherit()) return this;
        KnockbackChannelSpec nextDirect = override.direct.isPureInherit() ? this.direct : override.direct;
        KnockbackChannelSpec nextExplosion = override.explosion.isPureInherit() ? this.explosion : override.explosion;
        return new KnockbackSpec(nextDirect, nextExplosion);
    }
}
