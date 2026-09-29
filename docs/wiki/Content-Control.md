# Content Control

[简体中文](内容控制) | [Configuration](Configuration)

Content Control lets pack authors control unwanted SuperbWarfare items, vehicles, blocks, and ore generation from one JSON file.

The configuration file is:

```text
config/superbaddon/content_control.json
```

The file is created automatically after SuperbAddon starts for the first time.

> **Important**
>
> `block_load` does not unregister content from Forge registries. Forge content is already registered before this configuration is applied.
>
> Instead, SuperbAddon removes recipes, filters resources, hides blocked stacks, and cancels supported runtime behavior where possible.

---

## Complete configuration example

```json
{
  "_note": "SuperbAddon content control example",
  "items": {
    "superbwarfare:ak_47": "recipes_only",
    "superbwarfare:taser": "block_load"
  },
  "vehicles": {
    "superbwarfare:ah_6": "block_load"
  },
  "blocks": {
    "superbwarfare:charging_station": "block_load"
  },
  "worldgen_ores": {
    "superbwarfare": true
  }
}
```

This example does four things:

1. Removes supported recipes for the AK-47 while keeping the weapon available.
2. Blocks the Taser and its related resources.
3. Blocks the AH-6 vehicle, its container, recipes, entity spawning, and related resources.
4. Blocks the Charging Station item/resources and removes its block loot table.
5. Disables the built-in SuperbWarfare ore-generation features in newly generated chunks.

---

# Line-by-line explanation

## Line 1 — Root object

```json
{
```

Starts the root JSON object.

Every setting in `content_control.json` must be placed inside this object.

---

## Line 2 — `_note`

```json
"_note": "SuperbAddon content control example",
```

`_note` is optional descriptive text.

It is ignored by the configuration parser and has no gameplay effect.

You may change or remove it.

Do not use JavaScript-style comments:

```json
{
  // Invalid JSON comment
  "items": {}
}
```

Standard JSON does not support `//` or `/* ... */` comments.

---

## Lines 3–6 — `items`

```json
"items": {
  "superbwarfare:ak_47": "recipes_only",
  "superbwarfare:taser": "block_load"
},
```

The `items` object controls guns and other registered items.

Each entry uses this format:

```json
"<namespace>:<item_id>": "<mode>"
```

Example:

```json
"superbwarfare:taser": "block_load"
```

The key is the item registry ID:

```text
superbwarfare:taser
```

The value is the Content Control mode:

```text
block_load
```

Use registry IDs, not translated names.

Incorrect:

```json
"AK-47": "block_load"
```

Correct:

```json
"superbwarfare:ak_47": "block_load"
```

### Blueprint inheritance

When a configured item has a matching blueprint item, the blueprint inherits the same mode.

For example:

```json
"superbwarfare:ak_47": "block_load"
```

also applies to:

```text
superbwarfare:ak_47_blueprint
```

You normally do not need to add a second entry for the blueprint.

---

## Lines 7–9 — `vehicles`

```json
"vehicles": {
  "superbwarfare:ah_6": "block_load"
},
```

The `vehicles` object controls registered vehicle entity types.

Each key must be the vehicle entity registry ID:

```json
"superbwarfare:ah_6": "block_load"
```

Do not use the vehicle container item name unless it is also the actual entity ID.

Incorrect:

```json
"superbwarfare:ah_6_container": "block_load"
```

Correct:

```json
"superbwarfare:ah_6": "block_load"
```

SuperbAddon identifies vehicle containers through their `EntityType` NBT and applies the configured vehicle mode to the corresponding container stack.

---

## Lines 10–12 — `blocks`

```json
"blocks": {
  "superbwarfare:charging_station": "block_load"
},
```

The `blocks` object controls registered blocks and their same-name BlockItems.

Each key must be a block registry ID:

```json
"superbwarfare:charging_station": "block_load"
```

The current implementation assumes that the block and its BlockItem use the same registry ID.

For a block configured with `block_load`, SuperbAddon may filter:

- Blockstates
- Block models
- Item models
- Block textures
- Item textures
- Bedrock block models and textures
- BlockEntity textures
- GUI textures associated with the same path
- The block loot table

It does not unregister the block from Forge.

It also does not remove already placed blocks from existing chunks.

---

## Lines 13–15 — `worldgen_ores`

```json
"worldgen_ores": {
  "superbwarfare": true
}
```

`worldgen_ores` controls supported ore-generation removal by namespace.

The key is a mod namespace:

```text
superbwarfare
```

The value must be a JSON boolean:

```json
true
```

Only `true` enables removal.

```json
"superbwarfare": false
```

means the namespace is not disabled.

Do not write the boolean as a string:

```json
"superbwarfare": "true"
```

That value is ignored because it is not a real JSON boolean.

The built-in SuperbAddon biome modifier removes these SuperbWarfare features:

```text
superbwarfare:silver_ore
superbwarfare:deepslate_silver_ore
superbwarfare:galena_ore
superbwarfare:deepslate_galena_ore
superbwarfare:scheelite_ore
superbwarfare:deepslate_scheelite_ore
```

This affects newly generated terrain.

It does not:

- Remove ore blocks from existing chunks
- Remove ore items
- Remove ore blocks from Forge registries
- Automatically scan and remove every ore from another mod

Adding another namespace only works when a matching datapack or biome modifier uses SuperbAddon's world-generation condition for that namespace.

---

## Line 16 — End of root object

```json
}
```

Ends the root JSON object.

Do not place a comma after the final field.

Incorrect:

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

# Content Control modes

Only three mode names are supported:

```text
keep
recipes_only
block_load
```

There are no aliases.

Invalid values are treated as `keep`.

For example:

```json
"superbwarfare:ak_47": "disabled"
```

does not disable the AK-47 because `disabled` is not a valid Content Control mode.

---

## `keep`

```json
"superbwarfare:ak_47": "keep"
```

Keeps the target unchanged.

Effects:

- Recipes remain available
- Resources remain available
- Creative-tab entries remain available
- Item stacks remain available
- Vehicle entities remain available

`keep` entries are discarded after parsing because they require no runtime action.

This means the following configurations are effectively equivalent:

```json
{
  "items": {
    "superbwarfare:ak_47": "keep"
  }
}
```

```json
{
  "items": {}
}
```

Use `keep` when you want to leave an explicit record in the file. Otherwise, remove the entry.

---

## `recipes_only`

```json
"superbwarfare:ah_6": "recipes_only"
```

Removes supported recipes for the target without blocking the target itself.

Effects:

- Keeps models and textures
- Keeps the item or entity registered
- Keeps creative-tab entries
- Keeps existing world content
- Does not block item entities
- Does not block vehicle spawning
- Does not perform resource trimming

This mode is intended for progression control.

Typical use:

```json
{
  "vehicles": {
    "superbwarfare:ah_6": "recipes_only"
  }
}
```

The AH-6 still exists and can still be spawned by commands, scripts, loot, or other systems, but its supported assembling recipe is removed.

> **Current-version limitation**
>
> `recipes_only` is reliably matched for SuperbWarfare vehicle-assembling recipes because those recipes expose their resulting vehicle entity ID.
>
> Generic item and block recipe removal depends on the final deserialized recipe output. In the current build, `recipes_only` is not guaranteed to remove every ordinary item or block recipe. Check the final recipe list after `/reload`.

---

## `block_load`

```json
"superbwarfare:ah_6": "block_load"
```

Applies the strongest supported Content Control behavior.

It includes recipe removal and adds resource/runtime filtering.

The exact behavior depends on whether the target is listed under `items`, `vehicles`, or `blocks`.

---

# `block_load` behavior for items

Example:

```json
{
  "items": {
    "superbwarfare:taser": "block_load"
  }
}
```

SuperbAddon may:

- Remove recipes that output the blocked stack
- Remove recipes whose required ingredient contains only blocked item stacks
- Apply the same mode to `taser_blueprint`
- Remove the item from creative tabs
- Cancel dropped ItemEntities containing the blocked stack
- Filter the gun-data file
- Filter item models
- Filter display-setting models
- Filter item textures
- Filter gun icons
- Filter GeckoLib geometry
- Filter LOD geometry
- Filter animations

The item remains registered in Forge.

Existing NBT references may still contain the registry ID.

---

# `block_load` behavior for vehicles

Example:

```json
{
  "vehicles": {
    "superbwarfare:ah_6": "block_load"
  }
}
```

SuperbAddon may:

- Remove the vehicle-assembling recipe
- Filter vehicle container stacks
- Remove vehicle containers from creative tabs
- Cancel blocked vehicle entities when they join a level
- Filter the vehicle data file
- Filter vehicle icons
- Filter GeckoLib vehicle models
- Filter GeckoLib LOD models
- Filter vehicle animations
- Filter vehicle textures
- Filter Bedrock vehicle models
- Filter Bedrock LOD models
- Filter Bedrock vehicle textures
- Filter vehicle sounds

The vehicle entity type remains registered in Forge.

A command, script, or saved chunk may still reference the registry ID, but the entity is cancelled when the supported join-level interception runs.

---

# `block_load` behavior for blocks

Example:

```json
{
  "blocks": {
    "superbwarfare:charging_station": "block_load"
  }
}
```

SuperbAddon may:

- Remove recipes that output the same-name BlockItem
- Remove the BlockItem from creative tabs
- Cancel dropped ItemEntities containing the BlockItem
- Filter blockstates
- Filter block models
- Filter item models
- Filter block textures
- Filter item textures
- Filter associated GUI textures
- Filter associated entity textures
- Filter Bedrock block resources
- Filter the block loot table

Current limitations:

- Existing placed blocks are not removed
- Block placement is not explicitly cancelled
- BlockEntity creation is not explicitly cancelled
- The block remains registered
- Existing chunks are not rewritten

For this reason, `block_load` should be configured before distributing or generating a production world.

---

# Recipe dependency removal

When a recipe uses a blocked item as a required ingredient, SuperbAddon checks whether that ingredient consists entirely of blocked item stacks.

Example:

```json
{
  "items": {
    "superbwarfare:taser": "block_load"
  }
}
```

A recipe using only this exact item in one ingredient slot may be removed:

```json
{
  "ingredient": {
    "item": "superbwarfare:taser"
  }
}
```

A general tag ingredient is not treated as completely blocked:

```json
{
  "ingredient": {
    "tag": "forge:tools"
  }
}
```

SuperbAddon does not expand general item tags during this rewrite stage.

This avoids deleting unrelated recipes when a large shared tag contains both blocked and allowed items.

---

# Protected infrastructure

The following registry paths are protected:

```text
vehicle_assembling_table
container
```

These are SuperbWarfare infrastructure rather than ordinary removable content.

Attempting to use `block_load` on them is ignored for protected resource trimming and runtime blocking.

For example:

```json
{
  "blocks": {
    "superbwarfare:vehicle_assembling_table": "block_load"
  }
}
```

is not a valid way to disable the complete vehicle system.

Removing the assembling table or shared container infrastructure would break unrelated vehicle recipes, container rendering, and vehicle behavior.

To disable a specific vehicle, configure that vehicle instead:

```json
{
  "vehicles": {
    "superbwarfare:ah_6": "block_load"
  }
}
```

To disable or rewrite only its assembling recipe, use:

```text
config/superbaddon/vehicle_recipe_override.json
```

See [Vehicle Recipe Override](Vehicle-Recipe-Override).

---

# Multiple targets

You may configure any number of targets in the same section.

```json
{
  "items": {
    "superbwarfare:ak_47": "recipes_only",
    "superbwarfare:taser": "block_load",
    "superbwarfare:repair_tool": "keep"
  },
  "vehicles": {
    "superbwarfare:ah_6": "block_load",
    "superbwarfare:a_10a": "recipes_only"
  },
  "blocks": {
    "superbwarfare:charging_station": "block_load"
  },
  "worldgen_ores": {
    "superbwarfare": true
  }
}
```

Each target is evaluated independently.

The order of entries does not define priority.

A target should normally appear in only one matching section.

---

# Registry ID normalization

Target IDs are normalized before use.

The parser:

- Trims surrounding spaces
- Converts IDs to lowercase
- Rejects invalid resource locations

This entry:

```json
"  SUPERBWARFARE:AK_47  ": "block_load"
```

is normalized to:

```text
superbwarfare:ak_47
```

Invalid IDs are ignored and logged:

```json
"this is not an id": "block_load"
```

Always use the exact registry ID shown by the game, logs, JEI, KubeJS, or registry inspection tools.

---

# Minimal examples

## Remove only a vehicle recipe

```json
{
  "items": {},
  "vehicles": {
    "superbwarfare:ah_6": "recipes_only"
  },
  "blocks": {},
  "worldgen_ores": {
    "superbwarfare": false
  }
}
```

The AH-6 remains available, but its supported assembling recipe is removed.

---

## Block one gun

```json
{
  "items": {
    "superbwarfare:ak_47": "block_load"
  },
  "vehicles": {},
  "blocks": {},
  "worldgen_ores": {
    "superbwarfare": false
  }
}
```

The AK-47 and matching blueprint are blocked as far as the current runtime permits.

---

## Block one vehicle

```json
{
  "items": {},
  "vehicles": {
    "superbwarfare:ah_6": "block_load"
  },
  "blocks": {},
  "worldgen_ores": {
    "superbwarfare": false
  }
}
```

The AH-6 recipe, container, entity joins, and associated resources are filtered.

---

## Disable SuperbWarfare ore generation only

```json
{
  "items": {},
  "vehicles": {},
  "blocks": {},
  "worldgen_ores": {
    "superbwarfare": true
  }
}
```

No items, vehicles, or blocks are blocked.

Only the built-in SuperbWarfare ore features are removed from supported world generation.

---

# Applying configuration changes

After editing:

```text
config/superbaddon/content_control.json
```

run:

```mcfunction
/reload
```

A full `/reload` is the recommended method because recipe filtering is applied to the final deserialized recipe table.

You may also run:

```mcfunction
/superbaddon content_control reload
```

This command reloads the Content Control state and reports the number of configured targets and resource filters.

However, it does not rebuild every already-loaded system by itself.

Depending on the changed mode, you may still need:

- `/reload` for recipes
- A client resource reload for models and textures
- A client restart
- A server restart
- A world reload

For `block_load`, restarting both client and server is the safest deployment method.

---

# Client and server distribution

Content Control affects both gameplay and resources.

Server-side effects include:

- Recipe filtering
- Vehicle entity cancellation
- ItemEntity cancellation
- World-generation conditions

Client-side effects include:

- Resource filtering
- Creative-tab filtering
- Models
- Textures
- Animations
- Icons
- Sounds

A modpack should distribute the same `content_control.json` to both client and server.

A server cannot automatically remove models or textures already loaded by a client using a different local configuration.

---

# Existing worlds

Content Control is safest when configured before creating the world.

It does not retroactively clean every form of existing content.

Possible existing references include:

- Items stored in inventories
- Items stored in containers
- Blocks already placed in chunks
- Vehicle data saved in chunks
- Commands or scripts referencing blocked IDs
- Advancements or quests referencing blocked content

Back up the world before changing a large number of targets to `block_load`.

Test the configuration in a separate world before deploying it to a production server.

---

# Troubleshooting

## The file has no effect

Check that the file path is exactly:

```text
config/superbaddon/content_control.json
```

Do not place it inside:

```text
config/superbaddon/shell_ejection/
```

Run:

```mcfunction
/reload
```

Then check `latest.log` for:

```text
Loaded SuperbAddon content control
```

---

## A mode is ignored

Only these values are valid:

```text
keep
recipes_only
block_load
```

Incorrect:

```json
"superbwarfare:ak_47": "disable"
```

Correct:

```json
"superbwarfare:ak_47": "block_load"
```

---

## A target ID is ignored

Use a complete lowercase resource location:

```text
namespace:path
```

Incorrect:

```json
"ak_47": "block_load"
```

Correct:

```json
"superbwarfare:ak_47": "block_load"
```

---

## A blocked block still exists in the world

`block_load` does not remove already placed blocks.

It also does not unregister the block.

The block may remain in existing chunks even when its item, resources, recipes, and loot table are filtered.

---

## A blocked vehicle still appears in old data

The entity type remains registered.

Existing save data may still reference it.

SuperbAddon cancels supported vehicle entity joins, but it does not rewrite every saved chunk or external script.

---

## A recipe remains visible

Run:

```mcfunction
/reload
```

Content Control operates on the final recipe table after other recipe systems, including KubeJS, have completed their changes.

For ordinary item or block targets using `recipes_only`, verify the result in the current build. Generic recipe removal is not guaranteed for every serializer.

For vehicle assembling recipes, consider using:

```text
config/superbaddon/vehicle_recipe_override.json
```

with:

```json
{
  "groups": {},
  "overrides": {
    "superbwarfare:ah_6": {
      "disabled": true
    }
  }
}
```

---

# Related pages

- [Configuration](Configuration)
- [Config Directory Structure](Config-Directory-Structure)
- [Vehicle Recipe Override](Vehicle-Recipe-Override)
- [Resource Trimming Guide](Resource-Trimming-Guide)
- [Recipe Blocking Guide](Recipe-Blocking-Guide)
- [Troubleshooting](Troubleshooting)