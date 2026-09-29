# Troubleshooting

[简体中文](故障排查) | [Known Issues](Known-Issues)

Preserve the failing configuration and full relevant logs before changing anything. Repeated scans, deleting configuration or changing several systems at once can destroy the evidence needed to identify the problem.

---

## [1] Identify the failing layer

| Symptom | First check | Next action |
| --- | --- | --- |
| Game/server cannot load the addon | Dependency error, Java/Forge versions, duplicate JARs | Follow [Installation](Installation); preserve the complete exception chain |
| `/superbaddon` missing | Server mod presence and permission level 2 | Test from server console; do not edit JSON to repair command registration |
| External file rejected | JSON syntax, `format: 5`, rules array, target object | Use a strict JSON parser and [Data Format Versions](Data-Format-Versions) |
| Rules load but weapon unchanged | Correct instance, IDs, target intersections, priority, enable flags | Reduce to the [Quick Start](Quick-Start) single-target example |
| New vehicle cost not visible | Actual result.entity ID, recipe already removed, correct reload | Run server `/reload`; inspect matched/removed recipe logs |
| Resources still visible | Client-local config/cache and supported path layout | Align files and restart affected instances |
| Ore still present | Existing versus newly generated chunks | Test new terrain; the switch does not delete old ore |

## [2] Casing failures

For no casing, inspect the selected mode, matching ammo entry, chance, first point's casing item and storage destination. In store mode, only the first point is used; a full handler with fallback_to_drop false produces no fallback drop. Content control can reject a casing item after generation.

For unexpected casing, look for another higher-priority rule, an accidentally broad empty target or a copied backup JSON. Rule enabled false does not suppress lower rules. Count zero is clamped to one. Root enabled_by_default does not switch rules off.

For the wrong position, test one port at several headings with motion disabled first. The offset is in the firing/world-up basis, not an animated bone or full vehicle-roll basis. See [Shell Ejection](Shell-Ejection).

## [3] Ammo and knockback failures

Ammo patches need both enabled flags, a nonempty allowed list and a definition target. Projectile-only targeting does not patch definitions. Equal-priority replacements merge, and append contributions can add options you did not expect. After removing a patch, restart or verify full data restoration; external reload alone is not a baseline reset.

Knockback has separate direct and explosion channels, and a whole profile is selected from one rule. A pure-inherit high-priority profile does not shield against lower active rules. A positive minimum can undo the zero produced by disabled mode. Existing custom strengths and unsupported damage/velocity paths may still determine motion. See [Ammunition Overrides](Ammunition-Overrides) and [Knockback](Knockback).

## [4] Recipe and content failures

An ordinary item/block recipes_only rule not removing recipes is a documented implementation gap, not necessarily invalid JSON. Tags and dynamic recipe outputs also limit generic filtering. For vehicle recipes, check group conflicts, whole-spec individual replacement and invalid input arrays.

Content-control reload is not recipe-table reconstruction. If a recipe was removed by another rule or script, an input override cannot resurrect it. If a block remains placed, remember that block resource filtering does not intercept general placement or unload its BlockEntity. See [Recipe Blocking Guide](Recipe-Blocking-Guide).

## [5] Penetration failures

Identify the supported damage type and IDs carried by the direct projectile. The gun ID, entity type and native bypass lookup order matters. Zero/no configured match may fall through; disabling configured lookup does not erase native bypass.

Test direct hits separately from explosion distances. Default explosion penetration is already falling in the inner segment; a full-bypass source does not stay full across its whole radius. Keep target armor, absorption, team state and game mode fixed when comparing. See [Armor Penetration](Armor-Penetration).

## [6] MTS and crafting failures

For MTS fuel, inspect root/subfeature enable flags, normalized fluid key, replacement mapping, actual vehicle energy capacity and nearby vehicle selection. The effective pump search radius cannot be reduced below 32 through the current accessor. Reconnect after restart; links are not persisted.

For controls, bind the initially unbound keys, enable the correct toggle and use a controller seat. Camera tilt and input replacement are independent. For ammo boxes, inspect actual bulletQty rather than treating item count as round count.

For casing crafting, keep the required casings in one stack, place each other ingredient in its own slot and remove extra items. Test the standard crafting interface before diagnosing an automation mod. See [MTS Compatibility](MTS-Compatibility) and [Shell Reloading](Shell-Reloading).

## [7] A useful issue report

Provide the actual mod/JAR versions and source commit when known, whether this is single-player or dedicated server, the exact failing command/action, expected versus observed behavior, and the smallest reproducing configuration plus relevant target IDs. Include server logs and client logs when the failure is multiplayer/UI-specific.

Keep the first relevant exception and its complete caused-by chain rather than only a screenshot of the final disconnect. Remove account tokens, unrelated personal paths and server credentials before sharing logs. State which reload/restart was performed and whether the issue reproduces with a fresh weapon/vehicle in a test world.

## Source

[Load and command boundaries](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/command/ShellEjectionCommands.java) · [Ejection runtime](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/event/ShellEjectionEvents.java) · [Knockback runtime](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/knockback/KnockbackRuntime.java) · [Recipe filter](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/content/ContentControlManager.java)
