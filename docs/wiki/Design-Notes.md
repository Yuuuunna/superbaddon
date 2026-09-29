# Design Notes

[简体中文](设计说明) | [Data Format Versions](Data-Format-Versions)

This is a map of the current implementation for maintainers. It explains which component owns each decision so that new documentation or fixes do not accidentally join unrelated configuration systems.

---

## [1] Configuration ownership

| System | Source of truth | Main implementation |
| --- | --- | --- |
| Content restrictions | content_control.json | ContentControlConfig, ContentControlManager, ResourcePaths and runtime/resource hooks |
| Vehicle assembly | vehicle_recipe_override.json plus original recipes | VehicleRecipeOverrideConfig/Manager and the post-load recipe rewrite |
| Shot rules | Datapack and external format-5 documents | ShellRuleParser, ShellRuleSet and selected runtime consumers |
| Penetration | armor_penetration.json plus native projectile fallback | ArmorPenetrationConfig/Rules, CopiedHurtCalculator |
| MTS fuel/input preference | mts_compat.json plus client session state | MtsCompatConfig, fuel events/links, guarded controls |
| Counted casing crafting | Datapack recipes | ShellReloadingRecipe and ResultSlotMixin |

A shared configuration container does not imply one shared merge policy. Ejection, ammo patching and knockback reuse rule targeting/data structures but have different lifetimes and selection semantics.

## [2] Shot-rule data flow

```text
Datapack shell JSON ─┐
                    ├─ format-5 parsing ─ sorted combined rule table
External shell JSON ┘                         │
                     ┌────────────────────────┼───────────────────────┐
                     │                        │                       │
               ammo definition patch    per-shot ejection       per-shot knockback
               merge + cache invalidation select specification  select whole profile
                     │                        │                       │
               gun/vehicle data         server drop/store       projectile/explosion hooks
```

Datapack reload replaces the datapack rule set and refreshes external rules. External reload replaces only that source's rules, then applies active ammo patches. Numeric priority descending and rule ID ascending establish deterministic table ordering, but ammo patches merge rather than using the same first-match rule as ejection.

Ammunition changes mutate definitions and invalidate caches. The current implementation has no universal original-definition snapshot for reversing removed patches. Document this lifetime explicitly; do not promise transactional undo from a rule-table replacement.

## [3] Content and recipe layers

Content configuration produces normalized target modes and known resource-path predicates. Resource hooks filter access/listing, while selected runtime hooks filter creative stacks, dropped items, containers or vehicle joins. This is not registry deletion and does not cover every possible use of an item/block.

The recipe listener runs after recipe loading, reloads the relevant configuration and rewrites the final tables. Removal precedes vehicle reconstruction. Reconstructed vehicle recipes retain ID and output while replacing only selected input/category fields.

A correctness review must inspect call sites, not only enum names: the ordinary recipes_only gap exists because the ordinary output branch checks blocked stacks instead of the recipe-disable predicate. Tag-backed ingredients are conservatively retained during early inspection.

## [4] Damage and impulse layers

Armor penetration resolves supported gun/projectile IDs, divides defendable and bypass damage, and uses a specialized health path. Explosion contexts carry source penetration and distance falloff, including the supported deferred shockwave path. Zero configuration does not necessarily stop native projectile fallback.

Knockback instead captures a profile around shooting and carries it into supported projectile/explosion hooks. Direct and explosion channels are separate, while ammo-specific overlays replace whole non-inherit channels. The context is not a universal physics service for other mods.

These mechanisms can coexist but should not be documented as a single “damage strength” parameter. Test armor, absorption, impulse and explosion distance independently.

## [5] Optional integrations and authority

MTS fuel operations execute on the server against vehicle energy and fuel quantity; client controls drive MTS's own variables/packets rather than replacing physics. MTS camera tilt is session-local and independent of the saved input preference. Ammo quantities adapt MTS bulletQty instead of equating one item with one round.

Optional typed bridges are guarded before use. The ECA facade returns false on absence or linkage/runtime failure so the caller can use its native fallback. None of these facades is a generic configuration-file synchronization mechanism.

## [6] Maintainer change checklist

When adding a field, update its parser, default, range/unit, merge policy, consumer, reload behavior and both language pages. For a new rule effect, decide whether it is a definition-time mutation or a per-shot decision before reusing target fields.

When fixing a limitation, add the relevant regression checks and then change the known-issues text; do not remove a warning merely because the enum or comment describes the desired behavior. Preserve main-mod API boundaries and test the exact dependency version used for compilation and deployment.

For documentation, maintain canonical bilingual page pairs and a reachable sidebar. Use complete valid JSON plus separate numbered annotations. Run `python3 docs/validate_wiki.py` before publication. The validator checks documentation structure, not game correctness or arbitrary remote URL availability.

The synchronization workflow updates source-managed Wiki pages without deleting other live pages. Archive historical content outside docs/wiki/ so it is retained in the repository without republishing obsolete examples as current instructions.

## Source

[Rule loading and selection](https://github.com/Yuuuunna/superbaddon/tree/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/shell) · [Definition patcher](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/compat/SuperbWarfareDataPatcher.java) · [Content and recipe systems](https://github.com/Yuuuunna/superbaddon/tree/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/content) · [Damage helper](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/util/CopiedHurtCalculator.java) · [Optional integrations](https://github.com/Yuuuunna/superbaddon/tree/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/compat)
