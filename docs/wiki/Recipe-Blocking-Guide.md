# Recipe Blocking Guide

[简体中文](配方禁用指南) | [Content Control](Content-Control)

First distinguish three identifiers: **recipe ID**, **output item ID**, and **output vehicle entity ID**. The addon's content and vehicle-recipe settings primarily target outputs, not arbitrary recipe IDs.

---

## [1] Choose the correct system

| Intended change | Recommended route |
| --- | --- |
| Disable all matching assembly recipes for one vehicle, without blocking its entity | `vehicle_recipe_override.json` with `disabled: true`, or vehicle content mode `recipes_only` |
| Change assembly cost/category | [Vehicle Recipe Override](Vehicle-Recipe-Override) |
| Disable an ordinary item recipe while retaining the item | Use the pack's verified recipe/datapack or scripting mechanism; the addon's ordinary `recipes_only` branch currently has a gap |
| Exclude supported content and related resources as well | [Content Control](Content-Control) `block_load`, after save-safety testing |
| Change a counted casing recipe | Override its actual datapack recipe JSON using [Shell Reloading](Shell-Reloading) |

Do not use resource pruning merely to hide a recipe. Do not assume an empty `inputs` array disables vehicle crafting.

## [2] Vehicle recipe-only removal

Put this complete object in `config/superbaddon/vehicle_recipe_override.json`:

```json
{
  "groups": {},
  "overrides": {
    "superbwarfare:ah_6": { "disabled": true }
  }
}
```

Run server `/reload`. This removes existing matching vehicle assembling recipes while leaving AH-6 entity/resource access alone, unless another configuration blocks it.

Alternatively, the complete content-control form is:

```json
{
  "items": {},
  "vehicles": { "superbwarfare:ah_6": "recipes_only" },
  "blocks": {},
  "worldgen_ores": { "superbwarfare": false }
}
```

Choose one maintained source of intent rather than duplicating restrictions in several files. To restore crafting, every independent restriction must be removed and the recipe table rebuilt.

## [3] Ordinary item/block recipe limitation

The enum describes `recipes_only` as a recipe-removal mode, but the current ordinary-output filter calls `blocksItemStack`, which checks `block_load`. Consequently, placing an ordinary output under `items` or `blocks` with `recipes_only` is not a reliable recipe-only removal method in this source snapshot.

This documentation does not silently “fix” the problem by recommending the more destructive block mode. Use a recipe-ID/output removal facility already verified for your pack, or a deliberate datapack recipe override appropriate to that recipe serializer. Exact script APIs depend on the installed tooling and are not provided as guessed universal commands here.

Keep the actual recipe ID, serializer type and output in your pack change record. An item can have multiple recipes; removing one ID does not necessarily remove every way to obtain it.

## [4] Input filtering boundaries

For supported ordinary recipes, the addon may remove a recipe whose required ingredient is composed entirely of explicitly blocked item alternatives. If an alternative remains usable, the recipe can remain. If a tag-backed ingredient appears, the filter conservatively avoids resolving incomplete tag bindings during that stage and keeps that ingredient path.

Dynamic/custom recipes can expose incomplete result or ingredient information. Loot, trades, commands, creative access and scripts that directly grant items are not ordinary recipes. Recipe removal is therefore not a universal acquisition ban.

## [5] Reload order and competing authors

The post-load recipe listener reloads content control and vehicle overrides, then removes disabled recipes before rewriting surviving vehicle inputs/categories. A removed recipe cannot be resurrected merely by setting an input override.

This listener is designed to operate after recipe loading and the intended KubeJS rewriting stage. Additional reload-time integrations can still affect the final result and require runtime tests. Do not interpret a recipe-viewer cache as the authoritative server recipe table.

`/superbaddon content_control reload` changes predicates only; use server `/reload` or restart to actually rebuild the recipe table.

## [6] Acceptance test and rollback

Test the relevant crafting/assembling UI and actual material consumption. Also test one unaffected output and an alternative recipe for the same item. For counted shell reloading, test insufficient casing quantity and multiple crafts, not just the displayed output.

Record removed IDs and override counts from the log. A warning that zero vehicle recipes matched usually points to the wrong output ID, a recipe already removed elsewhere, or a missing assembling recipe.

Rollback by removing the responsible restriction/override and reloading from the original datapack/script inputs. Reloading only a query table cannot recreate an already filtered recipe table without rebuilding it.

## Source

[Actual removal predicates](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/content/ContentControlManager.java) · [Mode declarations](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/content/ContentControlMode.java) · [Final recipe rewrite](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/mixin/RecipeManagerMixin.java)
