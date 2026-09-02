package com.yy.superbaddon.command;

import com.yy.superbaddon.content.ContentControlConfig;
import com.yy.superbaddon.content.ContentControlManager;
import com.yy.superbaddon.penetration.ArmorPenetrationAutofill;
import com.yy.superbaddon.penetration.ArmorPenetrationConfig;
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
                                .executes(context -> reloadContentControl(context.getSource()))))
                .then(Commands.literal("armor_penetration")
                        .then(Commands.literal("reload")
                                .executes(context -> reloadArmorPenetration(context.getSource())))
                        .then(Commands.literal("status")
                                .executes(context -> armorPenetrationStatus(context.getSource())))
                        .then(Commands.literal("autofill")
                                .executes(context -> autofillArmorPenetration(context.getSource())))));
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

    private static int reloadArmorPenetration(CommandSourceStack source) {
        ArmorPenetrationConfig.reload();
        source.sendSuccess(() -> Component.literal("Reloaded armor penetration from " + ArmorPenetrationConfig.FILE
                + " (enabled=" + ArmorPenetrationConfig.enabled()
                + ", partial=" + ArmorPenetrationConfig.partialCount()
                + ", full_bypass=" + ArmorPenetrationConfig.fullBypassCount() + ")"), true);
        return ArmorPenetrationConfig.partialCount() + ArmorPenetrationConfig.fullBypassCount();
    }

    private static int armorPenetrationStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Armor penetration: enabled=" + ArmorPenetrationConfig.enabled()
                + ", partial=" + ArmorPenetrationConfig.partialCount()
                + ", full_bypass=" + ArmorPenetrationConfig.fullBypassCount()
                + ", config=" + ArmorPenetrationConfig.FILE), false);
        return ArmorPenetrationConfig.partialCount() + ArmorPenetrationConfig.fullBypassCount();
    }

    private static int autofillArmorPenetration(CommandSourceStack source) {
        ArmorPenetrationAutofill.ScanResult scan = ArmorPenetrationAutofill.scanVehicleWeapons();
        ArmorPenetrationConfig.WeaponArmorPenetrationUpdate update =
                ArmorPenetrationConfig.addMissingWeaponArmorPenetration(scan.partial(), scan.fullBypass());
        source.sendSuccess(() -> Component.literal("Autofilled armor penetration defaults in " + ArmorPenetrationConfig.FILE
                + " (vehicles=" + scan.vehicleCount()
                + ", weapons=" + scan.weaponCount()
                + ", candidate_partial=" + scan.partial().size()
                + ", candidate_full_bypass=" + scan.fullBypass().size()
                + ", added_partial=" + update.partialAdded()
                + ", added_full_bypass=" + update.fullBypassAdded()
                + ", skipped_generic_projectile=" + scan.skippedGenericWeaponCount() + ")"), true);
        return update.partialAdded() + update.fullBypassAdded();
    }
}
