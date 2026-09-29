# Common Configuration

[简体中文](通用配置) | [Configuration Overview](Configuration-Overview)

This page explains conventions shared by the configuration systems. **It is not a reference to a file named `common.json`.** There is no addon-wide master `enabled` switch that disables every feature.

---

## [1] Write explicit, valid JSON

Use UTF-8, quoted property names, JSON booleans `true`/`false`, and numbers where the field expects numbers. Avoid duplicate keys, trailing commas and programming-language comments. Some parsers tolerate or ignore additional input, but that is not a portable configuration contract.

Use full registry IDs such as `superbwarfare:ah_6`, not display names, translated labels, guessed container names or a file basename. Keep IDs lowercase. A syntactically valid ID is not proof the item/entity exists.

Metadata fields such as `_note` or `comment` are explanatory text only where a loader ignores them. They are not an alternate way to set behavior.

## [2] Understand which `enabled` you are editing

| Location | Meaning |
| --- | --- |
| `rules[].enabled` | Loads/skips this particular shell-rule object; omitted means true |
| `rules[].ammo_override.enabled` | Enables that rule's ammunition patch, provided `allowed` is nonempty; omitted means false |
| `armor_penetration.json` → `enabled` | Enables configured penetration lookup |
| `mts_compat.json` → `enabled` | Gates fuel compatibility; not the independent input/camera toggles |
| Shell root `enabled_by_default` | Scanner metadata; not read as the rule default by the current parser |

Content control instead uses per-target mode strings. Vehicle assembling disabling uses `disabled`, which has the opposite wording. Do not copy a switch from one file format to another.

## [3] Keep the units and modes separate

Casing `chance` uses a 0–1 probability, `pickup_delay` is in ticks, and offsets use the firing coordinate frame. Knockback values are nonnegative impulse controls, not damage percentages. Armor penetration accepts a 0–1 fraction or a quoted percent such as `"35%"`. MTS conversion is energy per bucket (1000 mB).

`ejection.mode: "none"` suppresses casing generation for the selected rule. `knockback.direct.mode: "disabled"` requests zero for that impulse channel. They are not interchangeable tokens.

## [4] Reload is not one operation

External rule reload refreshes the combined rule table, applies active ammunition patches and reloads content-control predicates. It does not reload all server datapacks, rebuild the recipe table, refresh client assets or reread every independent configuration.

For recipes use server `/reload` or restart. For penetration use its dedicated command. For client resource changes and MTS file edits, restart the relevant instance. A client resource reload alone is not a promise that a cached local JSON configuration has been reread.

See the exact matrix in [Configuration Overview](Configuration-Overview) and all registered commands in [Command Reference](Command-Reference).

## [5] Precedence is feature-specific

Ejection and knockback select their own first applicable rule from priority-descending, rule-ID-ascending order. Ammo patches instead merge the highest-priority replacement set and append entries. Vehicle recipe individual overrides replace their group specification wholesale. Resource predicates are derived from independent content tables.

Do not assume a universal “last file wins” rule. Give deliberate numeric priorities and avoid tied rules that match the same situation unless you understand their merge behavior.

## [6] Deploy and roll back safely

Keep a clean backup outside the scanned directory. Make one logical change, reload the correct subsystem, read the log and perform a positive and negative test. Record the mod versions and relevant IDs.

When removing an ammunition override, a reload of external files alone has no general baseline-restoration step. Restart, or perform and verify a complete main-mod data reload. A command reporting loaded rules confirms parsing, not that the intended shot or recipe was matched.

## Source

[Shell parsing](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/shell/ShellRuleParser.java) · [Ammunition patching](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/compat/SuperbWarfareDataPatcher.java) · [Commands](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/command/ShellEjectionCommands.java)
