# Vehicle Recipe Override

[简体中文](载具配方覆写) | [Configuration Overview](Configuration-Overview)

File: `config/superbaddon/vehicle_recipe_override.json`.

This system rewrites existing SuperbWarfare vehicle assembling recipes by **`result.entity` vehicle ID**. It can disable a matching recipe, replace its complete input list, and/or change its category. It does not create missing recipes, change the output entity or unregister vehicles.

---

## Complete example

```json
{
  "groups": {
    "starter_aircraft": {
      "vehicles": ["superbwarfare:ah_6", "superbwarfare:a_10a"],
      "category": "aircraft",
      "inputs": ["8 minecraft:iron_block", "4 minecraft:redstone"]
    }
  },
  "overrides": {
    "superbwarfare:ah_6": {
      "inputs": ["4 minecraft:iron_block"]
    },
    "superbwarfare:tom_6": {
      "disabled": true
    }
  }
}
```

These are example costs, not defaults. Both `groups` and `overrides` default to empty objects.

<pre>
groups / vehicles   <a href="#groups">[1]</a>
overrides           <a href="#overrides">[2]</a>
disabled            <a href="#disabled">[3]</a>
category            <a href="#category">[4]</a>
inputs              <a href="#inputs">[5]</a>
reload and order    <a href="#reload">[6]</a>
</pre>

<a name="groups"></a>
## [1] Groups

A group name is a human-chosen object key. Its `vehicles` array lists entity IDs; the group's `disabled`, `category` and `inputs` form one shared specification.

A vehicle in multiple groups produces a conflict warning and does not receive an arbitrary winning group specification. A separate individual override can still resolve that vehicle explicitly. Avoid relying on group declaration order.

<a name="overrides"></a>
## [2] Individual overrides replace groups wholesale

`overrides` maps vehicle IDs to specifications. An individual specification replaces the **entire** group specification, not just the fields present.

In the example, AH-6 receives the individual input list. Its omitted category is taken from its original recipe, not inherited from the group's `aircraft` field. To retain a group setting in the individual override, repeat it explicitly. An individual empty object can similarly prevent a group specification from being used without changing the original recipe.

All matching assembling recipes for the same output entity can be affected; keys are not recipe IDs and are not container-item IDs.

<a name="disabled"></a>
## [3] `disabled`

Boolean, default false. True removes matching assembling recipes from the final table. It does not prevent existing or administratively spawned vehicles from joining the world.

Recipe removal is evaluated before input/category rewriting. A disabled recipe is not restored by supplying inputs. A recipe removed by content control also cannot be restored by this override.

<a name="category"></a>
## [4] `category`

Optional string. Supported values are `land`, `defense`, `aircraft`, `civilian`, `water` and `misc`. The value is normalized to lowercase. Omit it to keep the original category for the final selected specification.

An invalid category is ignored with a warning rather than inventing a new category. Categories are assembling UI categories, not entity types or tags.

<a name="inputs"></a>
## [5] `inputs`

Optional **nonempty array of strings**, replacing the complete material list. Use the main mod's vehicle-assembling ingredient syntax:

| Example string | Meaning |
| --- | --- |
| `minecraft:iron_ingot` | One item ingredient |
| `8 minecraft:iron_block` | Counted item ingredient |
| `3 #superbwarfare:storage_blocks/steel` | Counted tag ingredient, provided that tag exists |

Use positive counts and actual registered items/tags. Syntax acceptance does not prove that a tag is populated. NBT objects or standard crafting ingredient objects are not the format of this field.

An empty array does **not** mean disabled or free crafting; it is rejected/ignored as an input override. If one entry is invalid, the entire replacement input field is discarded rather than applying a partial cost. Independently valid `disabled` or `category` fields can still apply. Use `disabled: true` to disable a recipe deliberately.

<a name="reload"></a>
## [6] Reload and integration order

Run server `/reload` or restart after editing. The recipe rewrite listener runs after the recipe manager's load and operates on the final deserialized recipe table, including the intended KubeJS-rewritten table. It reloads both content control and vehicle overrides, removes blocked recipes, and then reconstructs surviving matched vehicle recipes with new inputs/category while preserving ID and result.

`/superbaddon content_control reload` alone does not run this recipe rewrite. There is no separate vehicle-recipe reload command. This ordering is an implementation design, not a test certificate for every KubeJS/addon version.

Inspect logs for `Loaded SuperbAddon vehicle recipe overrides`, `[vehicle-recipe-override] applied` or the warning that configured overrides matched zero recipes. Test the assembling UI and actual material consumption with one vehicle first. For rollback, remove the individual/group entry and perform a full recipe reload from the original datapacks.

## Source

[Schema and conflict resolution](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/content/VehicleRecipeOverrideConfig.java) · [Recipe reconstruction](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/content/VehicleRecipeOverrideManager.java) · [Final table rewrite](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/mixin/RecipeManagerMixin.java)
