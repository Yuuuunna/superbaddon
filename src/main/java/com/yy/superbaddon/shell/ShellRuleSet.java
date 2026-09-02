package com.yy.superbaddon.shell;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ShellRuleSet {
    private static volatile List<ShellRule> dataPackRules = List.of();
    private static volatile List<ShellRule> externalRules = List.of();
    private static volatile List<ShellRule> rules = List.of();

    private ShellRuleSet() {
    }

    public static synchronized void replace(List<ShellRule> nextRules) {
        replaceDataPack(nextRules);
    }

    public static synchronized void replaceDataPack(List<ShellRule> nextRules) {
        dataPackRules = List.copyOf(nextRules);
        rebuild();
    }

    public static synchronized void replaceExternal(List<ShellRule> nextRules) {
        externalRules = List.copyOf(nextRules);
        rebuild();
    }

    public static Optional<ShellRule> match(ShellContext context) {
        return matchEjection(context);
    }

    public static Optional<ShellRule> matchEjection(ShellContext context) {
        for (ShellRule rule : rules) {
            if (rule.matchesEjection(context)) return Optional.of(rule);
        }
        return Optional.empty();
    }

    public static Optional<KnockbackMatch> matchKnockback(ShellContext context) {
        for (ShellRule rule : rules) {
            if (!rule.targetMatches(context)) continue;
            Optional<com.yy.superbaddon.knockback.KnockbackSpec> selected = rule.selectedKnockback(context);
            if (selected.isPresent()) return Optional.of(new KnockbackMatch(rule, selected.get()));
        }
        return Optional.empty();
    }

    public record KnockbackMatch(ShellRule rule, com.yy.superbaddon.knockback.KnockbackSpec spec) {
    }

    public static List<ShellRule> snapshot() {
        return rules;
    }

    public static int size() {
        return rules.size();
    }

    public static int dataPackSize() {
        return dataPackRules.size();
    }

    public static int externalSize() {
        return externalRules.size();
    }

    private static void rebuild() {
        ArrayList<ShellRule> sorted = new ArrayList<>(dataPackRules.size() + externalRules.size());
        sorted.addAll(dataPackRules);
        sorted.addAll(externalRules);
        sorted.sort(Comparator.comparingInt(ShellRule::priority).reversed().thenComparing(rule -> rule.id().toString()));
        rules = List.copyOf(sorted);
    }
}
