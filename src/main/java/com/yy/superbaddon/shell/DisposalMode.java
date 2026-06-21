package com.yy.superbaddon.shell;

import java.util.Locale;

public enum DisposalMode {
    DROP,
    STORE,
    NONE;

    public static DisposalMode from(String value) {
        if (value == null || value.isBlank()) return DROP;
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "store", "stored", "collect", "container" -> STORE;
            case "none", "ignore", "discard_no_visual", "disabled" -> NONE;
            default -> DROP;
        };
    }
}
