# Frequently Asked Questions

[简体中文](常见问题) | [Home](Home)

For a symptom-by-symptom investigation, use [Troubleshooting](Troubleshooting). This page answers the most common configuration and workflow questions directly.

---

## Do I need MTS or Epic Core API to start the addon?

No. MTS and Epic Core API have guarded optional integration paths. The required dependencies in this source are SuperbWarfare and GeckoLib on the supported Forge/Minecraft environment. Missing a required dependency is different from an optional feature being unavailable. See [Installation](Installation) and [Compatibility](Compatibility).

## Which configuration folder is correct?

Use `config/superbaddon/` under the running instance or dedicated server's working directory. The old `superbah6` name is not the active configuration namespace. A client instance and a server can each have a different copy; changing one does not distribute files to the other. See [Directory Structure](Config-Directory-Structure).

## Why does my JSON load but not change anything?

Parsing only establishes that the loader accepted data. The rule may be disabled, target the wrong ID, lose on priority, require a different seat/ammo combination, or belong to a subsystem that has not been reloaded. Root scan metadata is not a substitute for `rules[].enabled`. Follow [Quick Start](Quick-Start) with one target before debugging a large file.

## Does an external rule always beat a datapack rule?

No. Both participate in priority-descending, rule-ID-ascending ordering. Ejection and knockback independently select applicable rules; ammo overrides use their own replacement/append merge. There is no general “last file wins” convention. See [Shell Ejection](Shell-Ejection) and [Ammunition Overrides](Ammunition-Overrides).

## How do I stop casing generation without disabling a gun?

Use an enabled, sufficiently high-priority matching ejection rule with `mode: "none"`. Setting the rule's enabled flag false allows other rules to match; setting count to zero is clamped to one. This does not remove the gun or its ammunition. The complete reversible example is in [Quick Start](Quick-Start).

## Is ordinary `scan` safe to run over edited generated files?

No. Both scan modes rebuild generated_scan/ and remove the old generated_scan.json. The ordinary mode generates disabled candidates but still writes and reloads them. Move hand-maintained rules to custom/ and keep backups outside the scanned tree first. See [Shell Ejection Scan](Shell-Ejection-Scan).

## Why did `recipes_only` not remove an ordinary item's recipe?

The current filter implements that mode for vehicle assembly outputs, but ordinary item/block output filtering checks block_load and has a source-verified gap. Do not switch to resource/entity blocking simply to conceal this limitation. Use a verified recipe-only mechanism for your pack. See [Recipe Blocking Guide](Recipe-Blocking-Guide).

## Why did a vehicle's group settings disappear after adding an override?

A single-vehicle override replaces the whole group specification. Missing fields then keep their original-recipe behavior rather than inheriting the group. Repeat every group setting you intentionally retain. Empty inputs do not disable crafting. See [Vehicle Recipe Override](Vehicle-Recipe-Override).

## Why is there still penetration with a zero value or disabled configuration?

Configured lookup can fall through to a projectile entity-type entry or the projectile's native bypass rate. A zero entry is not an explicit veto, and the enabled flag does not erase native bypass. See the lookup order and supported damage paths in [Armor Penetration](Armor-Penetration).

## Why does MTS reject a fluid, consume a whole can, or forget camera tilt?

Unlisted fluids are rejected by the default zero fallback; an explicit zero also disables a listed fluid. Jerrycans are indivisible one-bucket transfers and can waste excess when a vehicle has only partial capacity. Camera tilt is deliberately session-only, while the separate input bridge is persisted. Both client toggle keys start unbound. See [MTS Compatibility](MTS-Compatibility).

## Why will 48 casings split across two slots not craft?

The counted casing requirement must be met by one stack in one slot. Other ingredients each occupy their own slot. Automated crafters that bypass the result-slot consumption hook need separate testing. See [Shell Reloading](Shell-Reloading).

## Can I remove a rule and simply reload to undo everything?

Not always. Ejection/knockback rule tables are replaceable, but ammo patches mutate definitions and do not retain a universal restoration snapshot. Resource restrictions may already have caused persistent world changes. Use full data reload/restart for verified definition restoration and restore a world backup when persistent state changed. See [Recommended Workflow](Recommended-Workflow).

## Is this a list of tested compatible mod versions?

No. The Wiki records a pinned source review and explicit implementation boundaries. Declared version ranges and guarded integrations are not a test matrix. Report the actual JAR versions, configuration and reproduction steps for the combination you use.

## Source

[Commands](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/command/ShellEjectionCommands.java) · [Recipe filtering](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/content/ContentControlManager.java) · [Penetration fallback](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/util/CopiedHurtCalculator.java) · [Optional ECA guard](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/compat/eca/EpicCoreApiBridge.java)
