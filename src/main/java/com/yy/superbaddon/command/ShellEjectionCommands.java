package com.yy.superbaddon.command;

import com.yy.superbaddon.content.ContentControlConfig;
import com.yy.superbaddon.content.ContentControlManager;
import com.yy.superbaddon.shell.ExternalShellRuleLoader;
import com.yy.superbaddon.shell.ShellRule;
import com.yy.superbaddon.shell.ShellRuleSet;
import com.yy.superbaddon.shell.ShellAmmoRuleApplier;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class ShellEjectionCommands {
    private ShellEjectionCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("superbaddon")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("shell_ejection")
                        .then(Commands.literal("reload_external")
                                .executes(context -> reloadExternal(context.getSource())))
                        .then(Commands.literal("scan")
                                .executes(context -> scan(context.getSource(), false))
                                .then(Commands.literal("active")
                                        .executes(context -> scan(context.getSource(), true)))))
                .then(Commands.literal("content_control")
                        .then(Commands.literal("reload")
                                .executes(context -> reloadContentControl(context.getSource())))));
    }

    private static int reloadExternal(CommandSourceStack source) {
        List<ShellRule> external = ExternalShellRuleLoader.loadExternalRules();
        ShellRuleSet.replaceExternal(external);
        int ammoConsumers = ShellAmmoRuleApplier.apply(ShellRuleSet.snapshot());
        ContentControlManager.State contentState = ContentControlManager.reload();
        source.sendSuccess(() -> Component.literal("Reloaded " + ShellRuleSet.externalSize()
                + " external shell ejection rules from " + ExternalShellRuleLoader.DIRECTORY
                + " (ammo consumers applied=" + ammoConsumers + ")"
                + "; content control: items=" + contentState.items().size()
                + ", vehicles=" + contentState.vehicles().size()
                + ", worldgen ore namespaces=" + contentState.worldgenOreNamespaces().size()), true);
        return ShellRuleSet.externalSize();
    }

    private static int reloadContentControl(CommandSourceStack source) {
        ContentControlManager.State contentState = ContentControlManager.reload();
        source.sendSuccess(() -> Component.literal("Reloaded content control from " + ContentControlConfig.FILE
                + " (items=" + contentState.items().size()
                + ", vehicles=" + contentState.vehicles().size()
                + ", resource filters=" + contentState.resourceFilters().size()
                + ", worldgen ore namespaces=" + contentState.worldgenOreNamespaces().size() + ")"), true);
        return contentState.items().size() + contentState.vehicles().size()
                + contentState.worldgenOreNamespaces().size();
    }

    private static int scan(CommandSourceStack source, boolean active) {
        ShellEjectionScanner.ScanResult result = ShellEjectionScanner.scan(source.getServer(), active);
        try {
            Path output = ExternalShellRuleLoader.writeGeneratedScan(result.files());
            ShellRuleSet.replaceExternal(ExternalShellRuleLoader.loadExternalRules());
            int ammoConsumers = ShellAmmoRuleApplier.apply(ShellRuleSet.snapshot());
            ContentControlManager.reload();
            source.sendSuccess(() -> Component.literal("Generated " + result.ruleCount()
                    + " shell ejection candidate rules in " + output
                    + " (files=" + result.generatedFileCount()
                    + ", guns=" + result.gunCount()
                    + ", vehicles=" + result.vehicleCount()
                    + ", created vehicle instances=" + result.createdVehicleCount()
                    + ", loaded weapon owners=" + result.loadedVehicleWeaponOwnerCount()
                    + ", scanned namespaces=" + result.scannedNamespaceCount()
                    + ", ammo consumers applied=" + ammoConsumers
                    + ", active=" + active + ")"), true);
            return result.ruleCount();
        } catch (IOException exception) {
            source.sendFailure(Component.literal("Failed to write shell ejection scan: " + exception.getMessage()));
            return 0;
        }
    }
}
