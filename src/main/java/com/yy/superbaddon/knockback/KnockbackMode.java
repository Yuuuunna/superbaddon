package com.yy.superbaddon.knockback;

import java.util.Locale;

public enum KnockbackMode {
    INHERIT,
    SCALE,
    FIXED,
    CAP,
    DISABLED;

    public static KnockbackMode from(String value) {
        if (value == null || value.isBlank()) return INHERIT;
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "inherit", "default", "original" -> INHERIT;
            case "scale", "multiply", "multiplier" -> SCALE;
            case "fixed", "set", "replace" -> FIXED;
            case "cap", "clamp", "limit" -> CAP;
            case "disabled", "disable", "none", "zero" -> DISABLED;
            default -> null;
        };
    }
}
