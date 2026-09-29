# Configuration Overview

[简体中文](内容控制) | [Configuration](Configuration)

SuperbAddon uses several independent JSON configuration systems instead of one large configuration file.

All user-editable configuration is stored under:

```text
config/superbaddon/
```

Each configuration system has a separate responsibility:

- Content availability and resource trimming
- Vehicle assembling recipe overrides
- Optional MTS compatibility
- External shell-ejection and ammunition rules
- Feature-specific rules such as knockback control

> [!IMPORTANT]
> These configuration systems are independent.
>
> Disabling a vehicle recipe does not block the vehicle entity.  
> Adding a shell-ejection rule does not enable or disable the target weapon.  
> Editing generated shell metadata does not modify `content_control.json`.

---

## Complete configuration layout

Click the number at the end of a line to jump to its explanation.

<pre>
config/
└─ superbaddon/
   ├─ content_control.json                                    <a href="#content-control-file">[1]</a>
   ├─ vehicle_recipe_override.json                            <a href="#vehicle-recipe-file">[2]</a>
   ├─ mts_compat.json                                         <a href="#mts-compat-file">[3]</a>
   └─ shell_ejection/                                         <a href="#shell-ejection-directory">[4]</a>
      ├─ custom/
      │  └─ example.json
      └─ generated_scan/                                      <a href="#generated-scan-directory">[5]</a>
         ├─ superbwarfare/
         │  ├─ items/
         │  └─ vehicles/
         └─ &lt;other-addon-namespace&gt;/
</pre>

---

<a name="content-control-file"></a>

## [1] `content_control.json`

```text
config/superbaddon/content_control.json
```

Controls unwanted items, vehicles, blocks, and supported ore generation.

Use this file when the goal is to:

- Remove supported recipes
- Block an item from normal use
- Block a vehicle and its container
- Filter associated models, textures, animations, or sounds
- Filter a supported block and its resources
- Disable the built-in SuperbWarfare ore-generation features

Detailed documentation:

[[Content-Control|Content Control]]

### Example

<pre>
{
  "_note": "SuperbAddon content control example",              <a href="#content-note">[6]</a>
  "items": {                                                   <a href="#content-items">[7]</a>
    "superbwarfare:ak_47": "recipes_only",                     <a href="#content-modes">[10]</a>
    "superbwarfare:taser": "block_load"                        <a href="#content-modes">[10]</a>
  },
  "vehicles": {                                                <a href="#content-vehicles">[8]</a>
    "superbwarfare:ah_6": "block_load"                         <a href="#content-modes">[10]</a>
  },
  "blocks": {                                                  <a href="#content-blocks">[9]</a>
    "superbwarfare:charging_station": "block_load"             <a href="#content-modes">[10]</a>
  },
  "worldgen_ores": {
    "superbwarfare": true
  }
}
</pre>

<a name="content-note"></a>

### [6] `_note`

```json
"_note": "SuperbAddon content control example"
```

Optional descriptive text for the configuration maintainer.

The parser ignores this field.

It may be changed or removed.

---

<a name="content-items"></a>

### [7] `items`

```json
"items": {
  "superbwarfare:ak_47": "recipes_only",
  "superbwarfare:taser": "block_load"
}
```

Controls registered items and guns.

Each key must be an item registry ID:

```text
namespace:item_id
```

For supported guns, the configured mode also applies to the matching `_blueprint` item.

For example:

```text
superbwarfare:ak_47
```

also controls:

```text
superbwarfare:ak_47_blueprint
```

---

<a name="content-vehicles"></a>

### [8] `vehicles`

```json
"vehicles": {
  "superbwarfare:ah_6": "block_load"
}
```

Controls registered vehicle entity types.

The key must be the vehicle entity ID, not the translated vehicle name and not a guessed container item ID.

Correct:

```text
superbwarfare:ah_6
```

Incorrect:

```text
AH-6 Little Bird
superbwarfare:ah_6_container
```

Vehicle container stacks are matched through their stored `EntityType` value.

---

<a name="content-blocks"></a>

### [9] `blocks`

```json
"blocks": {
  "superbwarfare:charging_station": "block_load"
}
```

Controls registered blocks and their same-name BlockItems.

The current implementation assumes:

```text
block registry ID = BlockItem registry ID
```

A blocked block is not unregistered from Forge and is not automatically removed from existing chunks.

---

<a name="content-modes"></a>

### [10] Content Control modes

The valid values are:

```text
keep
recipes_only
block_load
```

`keep` leaves the target unchanged.

`recipes_only` is intended to remove supported recipes while keeping the target available.

`block_load` applies the strongest available filtering, including supported recipe removal, resource filtering, creative-tab filtering, and runtime interception.

Invalid mode names are treated as `keep`.

Incorrect:

```json
"superbwarfare:ak_47": "disabled"
```

Correct:

```json
"superbwarfare:ak_47": "block_load"
```

`block_load` does not unregister content from Forge registries.

It prevents normal loading and use where the current implementation can intercept it.

---

<a name="vehicle-recipe-file"></a>

## [2] `vehicle_recipe_override.json`

```text
config/superbaddon/vehicle_recipe_override.json
```

Controls SuperbWarfare vehicle assembling recipes.

Use this file when the goal is to:

- Disable one vehicle assembling recipe
- Replace a vehicle's complete ingredient list
- Change the assembling-table category
- Apply the same recipe rule to a group of vehicles
- Override one vehicle separately from its group

Detailed documentation:

[[Vehicle-Recipe-Override|Vehicle Recipe Override]]

### Example

<pre>
{
  "_note": "Vehicle recipe override example",                  <a href="#vehicle-note">[11]</a>
  "groups": {                                                  <a href="#vehicle-groups">[12]</a>
    "light_aircraft": {
      "vehicles": [                                            <a href="#group-vehicles">[13]</a>
        "superbwarfare:ah_6",
        "superbwarfare:a_10a"
      ],
      "category": "aircraft",                                  <a href="#vehicle-category">[14]</a>
      "inputs": [                                              <a href="#vehicle-inputs">[15]</a>
        "8 minecraft:iron_block",
        "4 superbwarfare:large_motor"
      ]
    }
  },
  "overrides": {                                               <a href="#vehicle-overrides">[16]</a>
    "superbwarfare:ah_6": {
      "category": "aircraft",
      "inputs": [
        "4 minecraft:diamond_block"
      ]
    },
    "superbwarfare:tom_6": {
      "disabled": true                                         <a href="#vehicle-disabled">[17]</a>
    }
  }
}
</pre>

<a name="vehicle-note"></a>

### [11] `_note`

```json
"_note": "Vehicle recipe override example"
```

Optional descriptive text.

It has no effect on recipe processing.

---

<a name="vehicle-groups"></a>

### [12] `groups`

```json
"groups": {
  "light_aircraft": {
    ...
  }
}
```

Defines shared rules for multiple vehicles.

The group name is only an organizational label.

It does not need to match a mod ID, category, or registry entry.

A vehicle should belong to only one group.

If the same vehicle is assigned to multiple groups, its group rule is rejected instead of selecting one unpredictably.

---

<a name="group-vehicles"></a>

### [13] `vehicles`

```json
"vehicles": [
  "superbwarfare:ah_6",
  "superbwarfare:a_10a"
]
```

Lists the vehicle entity IDs that receive the group rule.

These IDs are matched against the vehicle entity stored in the assembling recipe result.

---

<a name="vehicle-category"></a>

### [14] `category`

```json
"category": "aircraft"
```

Replaces the vehicle's assembling-table category.

Valid categories are:

```text
land
defense
aircraft
civilian
water
misc
```

An invalid category is dropped while other valid fields in the same rule may still apply.

---

<a name="vehicle-inputs"></a>

### [15] `inputs`

```json
"inputs": [
  "8 minecraft:iron_block",
  "4 superbwarfare:large_motor"
]
```

Completely replaces the original ingredient list.

It does not append to the original recipe.

Supported forms include:

```text
minecraft:iron_ingot
8 minecraft:iron_block
8x minecraft:iron_block
2 #forge:storage_blocks/steel
```

An empty array is not a zero-cost recipe.

To remove a recipe, use:

```json
"disabled": true
```

If one ingredient string is invalid, the complete `inputs` field is rejected.

---

<a name="vehicle-overrides"></a>

### [16] `overrides`

```json
"overrides": {
  "superbwarfare:ah_6": {
    ...
  }
}
```

Defines a rule for one specific vehicle.

A single-vehicle override replaces the complete group rule for that vehicle.

It does not merge field by field with the group.

For example, if the group defines:

```json
{
  "category": "aircraft",
  "inputs": [
    "8 minecraft:iron_block"
  ]
}
```

and the individual override defines only:

```json
{
  "inputs": [
    "4 minecraft:diamond_block"
  ]
}
```

the vehicle does not inherit the group category.

To preserve it, repeat the category in the individual override:

```json
{
  "category": "aircraft",
  "inputs": [
    "4 minecraft:diamond_block"
  ]
}
```

---

<a name="vehicle-disabled"></a>

### [17] `disabled`

```json
"disabled": true
```

Removes the matching vehicle assembling recipe.

It does not:

- Block the vehicle entity
- Remove vehicle models or textures
- Remove existing vehicles
- Prevent commands from referencing the entity ID

To block the complete vehicle, also configure it in:

```text
config/superbaddon/content_control.json
```

---

<a name="mts-compat-file"></a>

## [3] `mts_compat.json`

```text
config/superbaddon/mts_compat.json
```

Controls optional Minecraft Transport Simulator / Immersive Vehicles compatibility.

MTS is not a required dependency.

SuperbAddon continues loading when MTS is absent.

Use this file to configure:

- Jerrycan compatibility
- Fuel-pump compatibility
- Fuel-to-Forge-Energy conversion
- Fuel-pump transfer rate
- Fuel-pump vehicle search radius
- The optional SuperbWarfare-style MTS control bridge

### Example

<pre>
{
  "_note": "Optional MTS compatibility",                       <a href="#mts-note">[18]</a>
  "enabled": true,                                             <a href="#mts-enabled">[19]</a>
  "default_energy_per_bucket": 5000000,                        <a href="#default-energy">[20]</a>
  "jerrycan": {                                                <a href="#mts-jerrycan">[21]</a>
    "enabled": true,
    "allow_nbt_only_detection": true,
    "allowed_items": [
      "examplepack:fuel_can"
    ]
  },
  "fuel_pump": {                                               <a href="#mts-fuel-pump">[22]</a>
    "enabled": true,
    "mb_per_tick": 10,
    "search_radius": 32
  },
  "controls": {                                                <a href="#mts-controls">[23]</a>
    "bridge_controls": false
  },
  "fluid_energy_per_bucket": {                                 <a href="#fluid-energy-map">[24]</a>
    "lava": 5000000,
    "minecraft:lava": 5000000,
    "fuel": 7500000,
    "gasoline": 8750000,
    "diesel": 10000000,
    "biodiesel": 7500000
  }
}
</pre>

<a name="mts-note"></a>

### [18] `_note`

```json
"_note": "Optional MTS compatibility"
```

Optional descriptive text.

It is ignored by the configuration parser.

---

<a name="mts-enabled"></a>

### [19] `enabled`

```json
"enabled": true
```

Master switch for MTS fuel compatibility.

It controls:

- Jerrycan integration
- Fuel-pump integration

It does not control:

```json
"controls": {
  "bridge_controls": true
}
```

The control bridge uses its own independent setting.

To disable every MTS-related feature, set both:

```json
{
  "enabled": false,
  "controls": {
    "bridge_controls": false
  }
}
```

---

<a name="default-energy"></a>

### [20] `default_energy_per_bucket`

```json
"default_energy_per_bucket": 5000000
```

Defines the Forge Energy value of one 1000 mB bucket when the fluid has no explicit entry in `fluid_energy_per_bucket`.

The value is clamped to at least `1`.

Setting it to `0` does not disable unknown fuels.

To reject one specific fuel, assign that fuel an explicit value of `0` in the fluid map.

---

<a name="mts-jerrycan"></a>

### [21] `jerrycan`

```json
"jerrycan": {
  "enabled": true,
  "allow_nbt_only_detection": true,
  "allowed_items": [
    "examplepack:fuel_can"
  ]
}
```

Controls MTS jerrycan compatibility.

`enabled` enables or disables jerrycan fueling.

`allow_nbt_only_detection` allows any item with the expected `jerrycanFluid` NBT to be treated as a jerrycan.

`allowed_items` adds extra item registry IDs that should be accepted.

The allowlist is additive.

It does not exclude native MTS items.

A jerrycan is treated as a complete bucket.

If the target vehicle can accept any energy, the jerrycan is emptied even when part of its energy value does not fit in the vehicle.

---

<a name="mts-fuel-pump"></a>

### [22] `fuel_pump`

```json
"fuel_pump": {
  "enabled": true,
  "mb_per_tick": 10,
  "search_radius": 32
}
```

Controls MTS fuel-pump integration.

`enabled` enables or disables the bridge.

`mb_per_tick` defines the maximum fuel drained each game tick.

`search_radius` defines the radius used to find the nearest SuperbWarfare vehicle.

The effective search radius is never lower than `32`.

A player connects a supported pump by interacting with it using an empty main hand.

The connection ends when the pump is empty, the vehicle is full, the vehicle leaves the range, or either side becomes invalid.

---

<a name="mts-controls"></a>

### [23] `controls`

```json
"controls": {
  "bridge_controls": false
}
```

Controls the optional SuperbWarfare-style input bridge for MTS vehicles.

The default is `false` to preserve native MTS controls.

The bridge may also be toggled through the configured key binding:

```text
Toggle MTS Control Bridge
```

Changing the bridge through the key binding writes the new value back to `mts_compat.json`.

---

<a name="fluid-energy-map"></a>

### [24] `fluid_energy_per_bucket`

```json
"fluid_energy_per_bucket": {
  "diesel": 10000000
}
```

Maps a fluid name or fluid registry ID to Forge Energy per 1000 mB.

Lookup order:

1. Full fluid ID
2. Fluid path
3. `default_energy_per_bucket`

For example:

```json
"diesel": 10000000
```

matches fluids whose path is `diesel`.

Use a full ID when two mods provide different fluids with the same path:

```json
"examplemod:diesel": 12000000
```

Setting a fluid value to `0` prevents it from producing energy.

When the `fluid_energy_per_bucket` object is present, the built-in map is replaced by the entries in the file.

Keep every default entry that should remain explicitly configured.

---

<a name="shell-ejection-directory"></a>

## [4] `shell_ejection/`

```text
config/superbaddon/shell_ejection/
```

Contains external shell-ejection rule files.

Every `.json` file below this directory is scanned recursively.

Subdirectories may be used freely for organization:

```text
shell_ejection/
├─ custom/
│  ├─ guns/
│  └─ vehicles/
├─ pack_overrides/
└─ generated_scan/
```

Directory names do not change rule behavior.

Detailed documentation:

[[Shell-Ejection|Shell Ejection]]

### Example

<pre>
{
  "format": 5,                                                 <a href="#shell-format">[25]</a>
  "rules": [                                                   <a href="#shell-rules">[26]</a>
    {
      "enabled": true,                                         <a href="#shell-enabled">[27]</a>
      "priority": 1500,                                        <a href="#shell-priority">[28]</a>
      "target": {                                               <a href="#shell-target">[29]</a>
        "guns": [
          "superbwarfare:ak_47"
        ]
      },
      "ejection": {                                             <a href="#shell-ejection">[30]</a>
        "default": {
          "mode": "drop",
          "casing": "superbaddon:brass_casing",
          "count": 1,
          "chance": 1.0,
          "pickup_delay": 24,
          "offset": [
            0.2,
            -0.04,
            -0.12
          ],
          "velocity": [
            0.1,
            0.07,
            -0.03
          ],
          "random_velocity": 0.03
        }
      }
    }
  ]
}
</pre>

<a name="shell-format"></a>

### [25] `format`

```json
"format": 5
```

Defines the shell-rule schema version.

The current build accepts only:

```json
"format": 5
```

A missing or unsupported format rejects the complete file.

---

<a name="shell-rules"></a>

### [26] `rules`

```json
"rules": [
  ...
]
```

Contains one or more shell-ejection rules.

A single file may contain multiple rules, but separate files are easier to maintain and isolate when one file has a syntax error.

---

<a name="shell-enabled"></a>

### [27] `enabled`

```json
"enabled": true
```

Controls whether the rule is loaded.

When omitted, the rule defaults to enabled.

Use:

```json
"enabled": false
```

to keep a candidate rule in the file without activating it.

---

<a name="shell-priority"></a>

### [28] `priority`

```json
"priority": 1500
```

Controls rule matching order.

Higher values are checked first.

Only the first matching rule is used for one shot.

Use high priorities for specific pack overrides and lower priorities for general fallback rules.

---

<a name="shell-target"></a>

### [29] `target`

```json
"target": {
  "guns": [
    "superbwarfare:ak_47"
  ]
}
```

Defines which shot may match the rule.

Supported target conditions include:

```text
guns
projectiles
vehicles
weapons
seats
requires_vehicle
min_projectiles_per_shot
```

Different populated target fields must all match.

Multiple values inside one field act as alternatives.

---

<a name="shell-ejection"></a>

### [30] `ejection`

```json
"ejection": {
  "default": {
    "mode": "drop",
    "casing": "superbaddon:brass_casing"
  }
}
```

Defines the casing behavior after the rule matches.

`default` is used when the rule does not need different casings for different ammunition.

Use `by_ammo` when each ammunition option requires a separate casing rule.

Supported disposal modes are:

```text
drop
store
none
```

Detailed field explanations are available in:

[[Shell-Ejection|Shell Ejection]]

---

<a name="generated-scan-directory"></a>

## [5] `generated_scan/`

```text
config/superbaddon/shell_ejection/generated_scan/
```

Contains candidate shell rules generated by:

```mcfunction
/superbaddon shell_ejection scan
```

The scan output is organized by:

```text
namespace → items or vehicles → registry path
```

Example:

```text
generated_scan/
└─ superbwarfare/
   ├─ items/
   │  └─ ak_47.json
   └─ vehicles/
      └─ ah_6.json
```

Running the scan command deletes and recreates the complete `generated_scan` directory.

Do not keep permanent hand-written rules inside it.

Move confirmed rules to another directory:

```text
config/superbaddon/shell_ejection/custom/
```

Detailed documentation:

[[Shell-Ejection-Scan|Shell Ejection Scan]]

### Generated metadata is not active configuration

Generated files may contain:

```json
"content_control": {
  "mode": "keep",
  "target_type": "vehicle",
  "target": "superbwarfare:ah_6"
}
```

This object is descriptive scan metadata.

The shell-rule parser does not apply it as Content Control.

Changing:

```json
"mode": "block_load"
```

inside the generated shell file does not block the target.

Active Content Control must be written to:

```text
config/superbaddon/content_control.json
```

---

## Knockback configuration

Knockback control is documented separately because its rule structure and runtime behavior are independent from the files summarized above.

Use the dedicated page for:

- Direct-hit knockback control
- Explosion knockback control
- Vehicle-specific rules
- Weapon-specific rules
- Rule priority and matching behavior
- Current implementation limitations

Detailed documentation:

[[Knockback|Knockback]]

Do not assume that a Shell Ejection rule also modifies knockback.

---

## Which configuration should I edit?

| Goal | Configuration |
|---|---|
| Remove or block an item | `content_control.json` |
| Remove or block a vehicle | `content_control.json` |
| Filter a supported block | `content_control.json` |
| Disable supported ore generation | `content_control.json` |
| Disable one vehicle assembling recipe | `vehicle_recipe_override.json` |
| Replace vehicle assembling ingredients | `vehicle_recipe_override.json` |
| Change a vehicle assembling category | `vehicle_recipe_override.json` |
| Configure MTS jerrycans | `mts_compat.json` |
| Configure MTS fuel pumps | `mts_compat.json` |
| Configure the MTS control bridge | `mts_compat.json` |
| Add or change shell casings | `shell_ejection/**/*.json` |
| Change accepted ammunition | `shell_ejection/**/*.json` |
| Generate candidate shell rules | `/superbaddon shell_ejection scan` |
| Change knockback behavior | See [[Knockback|Knockback]] |

---

## Applying changes

The configuration systems are loaded at different times.

### Content Control

Recommended:

```mcfunction
/reload
```

Available state reload command:

```mcfunction
/superbaddon content_control reload
```

The command refreshes the Content Control state, but it does not guarantee that every already-built recipe or client resource state is rebuilt.

For major `block_load` changes, restart both client and server.

---

### Vehicle recipe overrides

Run:

```mcfunction
/reload
```

or restart the server or single-player world.

There is currently no dedicated vehicle-recipe-override reload command.

---

### External shell-ejection rules

Run:

```mcfunction
/superbaddon shell_ejection reload_external
```

A full datapack reload also reloads shell rules:

```mcfunction
/reload
```

The external reload command also reapplies active ammunition overrides.

---

### MTS compatibility

Restart the client or server after editing:

```text
config/superbaddon/mts_compat.json
```

Most MTS compatibility values are initialized outside the normal datapack reload process.

The control bridge may be toggled immediately through its key binding.

---

### Knockback rules

Use the reload method documented on:

[[Knockback|Knockback]]

Do not assume that `/superbaddon shell_ejection reload_external` reloads unrelated rule systems.

---

## Basic JSON rules

All SuperbAddon configuration files use JSON.

### Strings require quotation marks

Correct:

```json
"mode": "block_load"
```

Incorrect:

```json
"mode": block_load
```

---

### Booleans do not use quotation marks

Correct:

```json
"enabled": true
```

Incorrect:

```json
"enabled": "true"
```

---

### Arrays use square brackets

```json
"vehicles": [
  "superbwarfare:ah_6",
  "superbwarfare:a_10a"
]
```

---

### Objects use braces

```json
"vehicles": {
  "superbwarfare:ah_6": "block_load"
}
```

---

### JSON does not support comments

Invalid:

```json
{
  // Disable the AH-6
  "vehicles": {
    "superbwarfare:ah_6": "block_load"
  }
}
```

Use a normal descriptive field when the schema allows unknown metadata:

```json
{
  "_note": "Disable the AH-6",
  "vehicles": {
    "superbwarfare:ah_6": "block_load"
  }
}
```

---

### Do not leave a trailing comma

Invalid:

```json
{
  "items": {},
}
```

Correct:

```json
{
  "items": {}
}
```

---

## Registry IDs

Most targets use Minecraft resource locations:

```text
namespace:path
```

Examples:

```text
superbwarfare:ak_47
superbwarfare:ah_6
superbwarfare:charging_station
```

Use registry IDs, not translated display names.

Incorrect:

```text
AK-47
AH-6 Little Bird
Charging Station
```

Correct:

```text
superbwarfare:ak_47
superbwarfare:ah_6
superbwarfare:charging_station
```

Registry IDs should be lowercase.

When a target is ignored, verify the ID using JEI, logs, KubeJS registry tools, commands, or another registry-inspection tool.

---

## Client and server configuration

Some settings affect only server logic, while others also affect client resources.

Server-side behavior may include:

- Recipe filtering
- Entity join cancellation
- ItemEntity cancellation
- World-generation conditions
- Ammunition data patching
- Fuel transfer

Client-side behavior may include:

- Model filtering
- Texture filtering
- Animation filtering
- Icon filtering
- Sound filtering
- Creative-tab filtering
- MTS control bridging

A modpack should distribute matching SuperbAddon configuration to both the client and server when a feature affects both sides.

A server cannot automatically remove resources already loaded from a different client configuration.

---

## Existing worlds

Configuration changes do not automatically clean all existing data.

Existing worlds may still contain:

- Items in player inventories
- Items in containers
- Blocks already stored in chunks
- Vehicles saved in chunk data
- Quests referencing disabled registry IDs
- KubeJS scripts referencing disabled content
- Commands or command blocks referencing disabled entities

Back up the world before applying large Content Control changes.

Test major changes in a separate world before deploying them to a production server.

---

## Recommended workflow

1. Start the game or server once and allow SuperbAddon to create its default files.
2. Back up the current configuration and world.
3. Decide whether the goal is recipe control, content blocking, compatibility, shell behavior, or knockback control.
4. Edit only the configuration system responsible for that goal.
5. Validate the JSON syntax.
6. Apply the documented reload or restart procedure.
7. Check `latest.log` for ignored IDs, rejected fields, or parse errors.
8. Verify the final result in a test world.
9. Keep permanent shell rules outside `generated_scan`.
10. Distribute matching client and server configuration when required.

For a pack-author deployment process, see:

[[Recommended-Workflow|Recommended Workflow]]

---

## Related pages

- [[Config-Directory-Structure|Config Directory Structure]]
- [[Common-Configuration|Common Configuration]]
- [[Content-Control|Content Control]]
- [[Shell-Ejection|Shell Ejection]]
- [[Knockback|Knockback]]
- [[Vehicle-Recipe-Override|Vehicle Recipe Override]]
- [[Command-Reference|Command Reference]]
- [[Shell-Ejection-Scan|Shell Ejection Scan]]
- [[Resource-Trimming-Guide|Resource Trimming Guide]]
- [[Recipe-Blocking-Guide|Recipe Blocking Guide]]
- [[Recommended-Workflow|Recommended Workflow]]
- [[Troubleshooting|Troubleshooting]]