# Known Issues and Implementation Limits

[简体中文](已知问题) | [Troubleshooting](Troubleshooting)

Status is tied to source commit `ab9693f`, reviewed on 2026-09-29. “Source-confirmed” below means a code path or limitation was inspected; it does not claim a separate in-game reproduction or a released bug fix.

---

## [1] Source-confirmed behavior gaps

| Area | Current limitation | Safe response |
| --- | --- | --- |
| Ordinary item/block recipe-only mode | Output filtering uses block_load checks rather than recipes_only; vehicle assembly has its own working mode check | Use a verified recipe-only mechanism; do not silently escalate to resource blocking |
| Ammo override rollback | Definitions are mutated without a universal original snapshot | Remove the patch, fully reload and verify main-mod data or restart |
| Block content control | No general placement interception or existing BlockEntity unload | Treat it as supported resource/stack filtering, not complete world removal |
| MTS search radius | Effective accessor enforces at least 32 despite allowing smaller stored values | Do not rely on a smaller configured radius to isolate vehicles |
| Version metadata | gradle.properties declares mod_version 1.0.0 while mods.toml has version 1.0.1 | Identify the actual artifact/commit; do not infer a release chronology from either string alone |

These are documented, not repaired by this Wiki update. Their relevant behavior is explained in [Content Control](Content-Control), [Ammunition Overrides](Ammunition-Overrides), [MTS Compatibility](MTS-Compatibility) and [Changelog](Changelog).

## [2] Destructive or non-obvious operations

Both scan modes delete and rebuild generated_scan/ and remove legacy generated_scan.json. An interrupted write can leave only part of the new generated set. A normal scan is not read-only even though it emits disabled candidates. Keep manual rules and backups outside that generated tree.

Vehicle block_load cancels/discards matching entities as they join a level, potentially including save-loaded entities. Removing the configuration later cannot recreate persistent entities already lost from the saved world.

Jerrycans transfer an indivisible bucket. If the vehicle accepts any energy, excess beyond remaining capacity can be wasted. Fuel-pump links are not persistent across restart.

## [3] Matching and configuration edge cases

An empty shell target is broad. Different nonempty target fields are AND conditions. External files are not automatically higher priority than datapacks, and JSON backups in the scanned directory are still loaded.

Ammo matching can compare extracted ID or full specification; it is not strict NBT identity. Definition patches do not honor all per-shot guards as conditional runtime changes. Equal-priority replacement ammo rules merge rather than selecting only one.

Store-mode casing uses only the first point. Empty ports fall back to a point rather than disabling ejection. Count zero becomes one. Invalid casing on the first point can prevent the whole drop path. Knockback disabled with a positive minimum is no longer necessarily zero.

Configured penetration zero is not a veto against native fallback. The explosion inner region already falls off rather than remaining flat. See the feature references for complete defaults and safe examples.

## [4] Coverage and integration limits

Recipe ingredient filtering conservatively keeps tag-backed paths during its early inspection. Dynamic recipes and custom outputs may not expose enough information for generic filtering. Vehicle overrides only reconstruct the recognized main-mod assembling recipe type.

Counted shell crafting relies on ResultSlot.onTake to charge extra casings. Automated machines and custom menus bypassing that hook are not automatically validated by the serializer's existence.

Damage, custom knockback, reflection-based MTS helpers, optional ECA and camera hooks depend on actual integration paths. A modpack that bypasses those paths can behave differently. Broad dependency ranges do not prove future internal compatibility.

## [5] Configuration distribution and reload limits

Server commands do not distribute local client JSON. Recipe-table, resource-manager, external-rule and cached MTS configuration reloads are distinct operations. A successful load count is not an effective-shot/recipe diagnostic. Use [Recommended Workflow](Recommended-Workflow) for deployment and verification.

## [6] Documentation and test status

The old Wiki snapshot is preserved unchanged under docs/wiki-archive/2026-09-29; it contains obsolete examples and is not the current schema reference. Historical page addresses remain available, while current pages correct target types, directory names and source behavior.

Documentation validation checks page coverage, local links, referenced source paths and JSON syntax. It does not run Minecraft, resolve an actual item registry, evaluate every generated rule against a shot, certify game balance or test all supported mod combinations.

## Source

[Ordinary recipe filtering](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/content/ContentControlManager.java) · [Mutation-based ammo patches](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/compat/SuperbWarfareDataPatcher.java) · [Scan file replacement](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/shell/ExternalShellRuleLoader.java) · [MTS accessor](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/compat/mts/MtsCompatConfig.java) · [Crafting consumption](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/mixin/ResultSlotMixin.java)
