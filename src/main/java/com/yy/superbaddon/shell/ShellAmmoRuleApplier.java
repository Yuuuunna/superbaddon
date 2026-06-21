package com.yy.superbaddon.shell;

import com.yy.superbaddon.compat.SuperbWarfareDataPatcher;

import java.util.Collection;

public final class ShellAmmoRuleApplier {
    private ShellAmmoRuleApplier() {
    }

    public static int apply(Collection<ShellRule> rules) {
        return SuperbWarfareDataPatcher.applyAmmoOverrides(rules);
    }
}
