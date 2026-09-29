# Configuration Overview

[简体中文](配置总览) | [Home](Home)

SuperbAddon uses independent configuration systems rather than one large configuration file. Choose the system that owns the behavior you want to change.

> [!IMPORTANT]
> Shell rules do not enable or disable registered content. Vehicle recipe overrides do not change vehicle entities. Knockback and armor penetration are different systems. There is no standalone `common.json` or `knockback.json` loader in this source version.

---

## Complete configuration layout

Click a numbered link for its explanation. Paths are relative to the running game or server instance.

<pre>
config/
└─ superbaddon/
   ├─ content_control.json                  <a href="#content">[1]</a>
   ├─ vehicle_recipe_override.json          <a href="#recipes">[2]</a>
   ├─ mts_compat.json                       <a href="#mts">[3]</a>
   ├─ armor_penetration.json                <a href="#armor">[4]</a>
   └─ shell_ejection/                       <a href="#shell">[5]</a>
      ├─ custom/
      │  └─ my_rules.json
      └─ generated_scan/
         └─ &lt;namespace&gt;/
            ├─ items/
            └─ vehicles/

&lt;world&gt;/datapacks/&lt;pack&gt;/data/&lt;namespace&gt;/
├─ shell_ejection/                          <a href="#datapacks">[6]</a>
└─ recipes/                                <a href="#datapacks">[6]</a>
</pre>

Files are created when their respective loaders first need them; an absent optional file at the main menu is not itself an error. `custom/` is an organizational convention, not a separately privileged loader. See [Directory Structure](Config-Directory-Structure).

<a name="content"></a>
## [1] `content_control.json`

Use this for supported item, vehicle, block and ore-generation controls. Its clean default structure is:

```json
{
  "items": {},
  "vehicles": {},
  "blocks": {},
  "worldgen_ores": { "superbwarfare": false }
}
```

Targets use registry IDs and the modes `keep`, `recipes_only` or `block_load`. In `worldgen_ores`, **true means disable** the supported ore features. It does not delete existing ore blocks.

The current recipe filter implements `recipes_only` for vehicle assembling outputs, but ordinary item/block recipe outputs have a known gap. Do not silently replace `recipes_only` with the more destructive `block_load`. Read [Content Control](Content-Control), [Recipe Blocking](Recipe-Blocking-Guide) and [Known Issues](Known-Issues).

<a name="recipes"></a>
## [2] `vehicle_recipe_override.json`

Use this to change existing `superbwarfare:vehicle_assembling` recipes by their output vehicle entity ID. Supported fields are `disabled`, `category` and `inputs`, arranged into `groups` and individual `overrides`.

```json
{
  "groups": {},
  "overrides": {
    "superbwarfare:ah_6": {
      "inputs": ["8 minecraft:iron_block"]
    }
  }
}
```

This is an illustrative cost override, not the default recipe. Individual overrides replace the whole group specification; they do not inherit omitted group fields. The output vehicle is not replaced. See [Vehicle Recipe Override](Vehicle-Recipe-Override).

<a name="mts"></a>
## [3] `mts_compat.json`

Contains optional fuel conversion, jerrycan and fuel-pump settings plus the persisted client input-bridge preference. Unlisted fluids are rejected by default because `default_energy_per_bucket` defaults to `0`.

Fuel configuration and client control preference are separate concerns even though they share a file. The camera-tilt toggle is session-only and is not an additional JSON field. See [MTS Compatibility](MTS-Compatibility) for the complete defaults and controls.

<a name="armor"></a>
## [4] `armor_penetration.json`

Contains `enabled`, `armor_penetration_by_weapon`, `full_armor_bypass_weapons` and the `explosion` falloff settings. It is not stored inside shell rules.

Use `/superbaddon armor_penetration reload` after editing and `/superbaddon armor_penetration status` to inspect loaded counts. `autofill` adds missing heuristic defaults, not guaranteed balance values. See [Armor Penetration](Armor-Penetration).

<a name="shell"></a>
## [5] `shell_ejection/**/*.json`

Every regular JSON file under this directory is scanned recursively. The required wrapper is:

```json
{
  "format": 5,
  "rules": []
}
```

Each rule may contain `enabled`, `priority`, object-valued `target`, `ammo_override`, `ejection` and `knockback`. These sections are independently useful: a knockback-only rule need not generate casings or replace ammunition.

Read [Shell Ejection](Shell-Ejection), [Ammunition Overrides](Ammunition-Overrides) and [Knockback](Knockback). For discovery, read [Shell Ejection Scan](Shell-Ejection-Scan).

> [!WARNING]
> A scan deletes and rebuilds `generated_scan/`, and removes the legacy `generated_scan.json`. Keep maintained rules in `custom/` and backups outside the scanned directory. Root metadata such as `enabled_by_default`, `owner` and embedded `content_control` does not replace explicit rule fields or `content_control.json`.

<a name="datapacks"></a>
## [6] Datapacks

Datapack shell rules use the same format-5 parser. Built-in and external rules are combined by numeric priority and rule ID; external files do not automatically outrank datapacks. Datapack recipes, including [Shell Reloading](Shell-Reloading), belong under `recipes/`, not the external shell directory.

## Apply changes at the correct layer

| Change | Appropriate action |
| --- | --- |
| External shell/ejection/knockback rules | `/superbaddon shell_ejection reload_external` |
| Datapack shell rules or crafting recipes | Server `/reload`; inspect the reload log |
| Vehicle recipe override or content-controlled recipe table | Server `/reload` or restart |
| Armor penetration JSON | `/superbaddon armor_penetration reload` |
| Content-control predicates only | `/superbaddon content_control reload`; this alone does not rebuild recipes/resources |
| Client resource trimming or ore-generation configuration | Restart the affected client/server and test; old chunks are not regenerated |
| MTS file edits | Restart the affected instance; no dedicated MTS reload command is registered |

Changes that patch ammunition definitions require a verified full data reload or restart when rolling back. Never assume removing a file reverses every in-memory mutation. See [Common Configuration](Common-Configuration) and [Recommended Workflow](Recommended-Workflow).

## Source

[Configuration packages](https://github.com/Yuuuunna/superbaddon/tree/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon) · [Registered commands](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/command/ShellEjectionCommands.java)
