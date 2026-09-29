# Data Format Versions and Migration

[简体中文](数据格式版本) | [Configuration Overview](Configuration-Overview)

The source has several unrelated JSON schemas. A version number or field name from one schema cannot be copied into another as a universal compatibility setting.

---

## [1] Format map

| Data | Version/discriminator | Accepted location |
| --- | --- | --- |
| Shell, ammo override and knockback rules | Root numeric `format: 5`, `rules` array | External shell directory or datapack `data/<namespace>/shell_ejection/` |
| Content control | Fixed schema, no required format number | config/superbaddon/content_control.json |
| Vehicle recipe overrides | Fixed schema, groups/overrides | config/superbaddon/vehicle_recipe_override.json |
| Penetration | Fixed schema with selected legacy aliases | config/superbaddon/armor_penetration.json |
| MTS settings | Fixed schema, separate client/session concerns | config/superbaddon/mts_compat.json |
| Counted casing recipe | `type: "superbaddon:shell_reloading"` | Datapack recipes directory |

Datapack pack.mcmeta has its own format/version system and is not governed by the shell rule's format 5. Adding format 5 to an arbitrary crafting recipe does not turn it into a shell rule.

## [2] Current shell envelope

A complete no-casing rule is:

```json
{
  "format": 5,
  "rules": [
    {
      "enabled": true,
      "priority": 1000,
      "target": { "guns": ["superbwarfare:ak_47"] },
      "ejection": { "default": { "mode": "none" } }
    }
  ]
}
```

The parser rejects a missing or different format number, a root array, or a missing rules array. When target is supplied, it must be an object. Merely changing an old document's number to 5 does not migrate its field structure.

## [3] Correct obsolete assumptions

| Historical pattern | Current interpretation / migration |
| --- | --- |
| A string-valued target | Replace with an object containing the correct guns/projectiles/vehicles/weapons fields |
| Root enabled_by_default controls all rules | It is metadata; explicitly set each rules[].enabled |
| Embedded content_control inside scan output | Move intended restrictions into the independent content_control.json |
| External directory automatically overrides data | Give explicit priority and account for rule-ID ordering |
| Disabling one rule globally stops ejection | Use an active matching mode-none rule when suppressing lower matches is intended |
| generated_scan.json as a permanent hand-edited file | Move maintained content into custom/ before running the scanner |
| Reload external files undoes all ammo mutations | Restore from original data through a verified full reload/restart |

Do not convert a gun ID into a vehicle ID or guess weapon names during migration. Use current scan output as a reference, not a blanket replacement for hand-maintained behavior.

## [4] Retained compatibility forms

The shell parser accepts scalar or array target sets, and supports the legacy points array when ports is not supplied as the preferred array. Canonical ejection modes are drop/store/none; retained aliases are listed in [Shell Ejection](Shell-Ejection). Unknown mode strings can fall back to drop, so relying on an unrecognized spelling is unsafe.

Ammo options accept strings or object consumer forms and selected case variants of identity keys. This is not a promise that arbitrary old main-mod consumer internals remain compatible with a newer dependency.

Penetration supports canonical snake_case map/set keys plus `armorPenetrationByWeapon` and `fullArmorBypassWeapons`, a legacy array form such as `"namespace:id=35%"`, and older root explosion keys. Prefer the canonical nested form in [Armor Penetration](Armor-Penetration); avoid supplying both alias sets, because the legacy partial map is read after the canonical map and the full-bypass sets combine.

The old root explosion keys are `explosionMinArmorPenetration`, `explosionInnerRadiusFactor` and `explosionOuterFalloffPower`. Nested explosion fields take precedence. None of these aliases is a substitute for the independent file or an addon-wide format version.

## [5] Safe migration procedure

Archive the original configuration outside every active scan directory. Identify its actual schema and source/dependency version. Rewrite one target into a complete current document, keeping IDs and intended behavior explicit rather than copying obsolete metadata switches.

Validate JSON, reload the correct subsystem and test a positive and negative match. Check actual ammo selection and consumption, casing placement, knockback, recipe output and restart behavior where those features changed. Only then migrate the next group.

For shell files, store maintained rules in custom/ and keep generated output disposable. For resource restrictions, preserve a world backup because removing a setting does not restore already discarded world entities. For recipe data, rebuild the recipe table rather than only the content predicate cache.

## [6] Documentation compatibility

Old Wiki addresses Configuration, Home‐ZH and the double-extension sidebar remain entry pages. The unchanged recovered Wiki is archived separately and may contain invalid current examples. Current bilingual feature pages are the schema reference.

The documentation validator checks strict JSON syntax, local links and known page coverage. It does not replace the mod's parser, main-mod consumer deserializer or an actual runtime registry test. Keep schema validation and gameplay validation distinct.

## Source

[Strict shell version check](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/shell/ShellRuleParser.java) · [Target and rule forms](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/shell/ShellRule.java) · [Retained port form](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/shell/ShellEjectionSpec.java) · [Penetration aliases](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/penetration/ArmorPenetrationConfig.java)
