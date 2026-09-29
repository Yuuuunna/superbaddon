# Casing Items and Shell Reloading

[简体中文](弹壳复装) | [Shell Ejection](Shell-Ejection)

Casings are registered items, not just particles. The addon also registers the `superbaddon:shell_reloading` crafting serializer to consume a counted stack of casings with ordinary ingredients.

---

## [1] Built-in casing items

| Item ID | Reference name |
| --- | --- |
| `superbaddon:brass_casing` | Brass casing |
| `superbaddon:shotgun_hull` | Shotgun hull |
| `superbaddon:heavy_casing` | Heavy casing |
| `superbaddon:autocannon_casing` | Autocannon casing |
| `superbaddon:large_shell_stub` | Large shell stub |

[Ejection configuration](Shell-Ejection) chooses which item appears. Changing a casing item does not automatically create recipes for it; a modpack's custom casing needs matching recipe data.

## [2] A shipped counted recipe

This is the shipped rifle-ammunition recipe. It requires **48 brass casings in one slot**, plus one steel-ingot ingredient, gunpowder and primer in three other slots, and produces 48 rifle-ammo items.

```json
{
  "type": "superbaddon:shell_reloading",
  "category": "misc",
  "group": "shell_reloading",
  "casing": {
    "ingredient": { "item": "superbaddon:brass_casing" },
    "count": 48
  },
  "ingredients": [
    { "tag": "superbwarfare:ingots/steel" },
    { "item": "minecraft:gunpowder" },
    { "item": "superbwarfare:primer" }
  ],
  "result": { "item": "superbwarfare:rifle_ammo", "count": 48 },
  "show_notification": true
}
```

The recipe ID is determined by its datapack path, for example `data/superbaddon/recipes/rifle_ammo_from_casing.json` becomes `superbaddon:rifle_ammo_from_casing`. Use server `/reload` after changing datapack recipes.

<pre>
casing.ingredient / count  <a href="#casing">[3]</a>
ingredients                <a href="#ingredients">[4]</a>
result / metadata          <a href="#result">[5]</a>
matching and automation    <a href="#matching">[6]</a>
shipped recipe index       <a href="#index">[7]</a>
</pre>

<a name="casing"></a>
## [3] Counted casing ingredient

`casing` is required. It contains an object-valued standard `ingredient` (such as an item or tag object) and a required positive integer `count`.

The count must fit in a single actual stack. Splitting 48 required casings into two stacks of 24 does not satisfy the counted slot. A count larger than that casing item's stack capacity is not practical in a normal crafting grid.

<a name="ingredients"></a>
## [4] Other ingredients

`ingredients` is a required array of standard crafting ingredients, with at least one nonempty entry. Each entry occupies its own nonempty grid slot and follows normal one-item-per-craft consumption. Repeating an ingredient in the array requires another occupied slot; increasing the stack size in one slot does not fill two entries.

Avoid overlapping casing and other ingredient definitions, and avoid ambiguous alternatives. Matching walks the slots and chooses the first matching unmatched ingredient; it is not an exhaustive assignment solver.

<a name="result"></a>
## [5] Result and metadata

`result` is required and uses the ordinary item-stack result format, including item ID and count. `group` defaults to an empty string, crafting-book `category` falls back to misc, and `show_notification` defaults to true.

The recipe marks itself special (`isSpecial()` is true). Lack of ordinary recipe-book discovery is not proof that the recipe is disabled, and this serializer alone is not a dedicated recipe-viewer plugin.

<a name="matching"></a>
## [6] Grid matching and consumption

There is no shaped pattern. The grid needs at least `ingredients.size() + 1` slots, exactly one occupied casing slot plus the required ordinary ingredient slots, and no unrelated extra items. With the rifle example, four slots are sufficient, so the usual 2×2 grid can fit it when the tag/materials resolve correctly.

The server captures the matching custom recipe when the result is taken. Normal crafting consumes one casing item, then the ResultSlot hook consumes `casing.count - 1` more. The complete requested casing count is therefore charged through that supported crafting path.

> [!IMPORTANT]
> Automated crafters and custom menus that bypass `ResultSlot.onTake` need their own compatibility verification. Displaying the recipe is not enough: test actual extra-casing consumption, shift crafting, insufficient materials and output inventory limits. Do not assume every automation mod calls the vanilla result-slot hook.

<a name="index"></a>
## [7] Shipped recipe file index

The source snapshot includes these 14 recipe files under `data/superbaddon/recipes/`. Read the corresponding JSON for its exact current inputs and counts rather than inferring balance from the name.

| Family | Files |
| --- | --- |
| Small-arms ammunition | `handgun_ammo_from_casing.json`, `rifle_ammo_from_casing.json`, `shotgun_ammo_from_casing.json`, `sniper_ammo_from_casing.json`, `heavy_ammo_from_casing.json` |
| Small shells | `small_shell_aa_from_casing.json`, `small_shell_ap_from_casing.json`, `small_shell_gs_from_casing.json`, `small_shell_he_from_casing.json` |
| Large shells | `large_shell_ap_from_casing.json`, `large_shell_cm_from_casing.json`, `large_shell_gs_from_casing.json`, `large_shell_he_from_casing.json`, `large_shell_wp_from_casing.json` |

For a second concrete reference, the shipped large AP recipe uses one `large_shell_stub`, one `superbwarfare:ap_head` and one `superbwarfare:grain`, producing one `superbwarfare:large_shell_ap`.

## Source

[Registered casing items](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/registry/ShellEjectionItems.java) · [Serializer and matcher](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/recipe/ShellReloadingRecipe.java) · [Extra consumption hook](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/mixin/ResultSlotMixin.java) · [All shipped recipe JSON](https://github.com/Yuuuunna/superbaddon/tree/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/resources/data/superbaddon/recipes)
