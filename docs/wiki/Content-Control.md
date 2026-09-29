# Content Control

[简体中文](内容控制) | [Configuration Overview](Configuration-Overview)

Content control is loaded only from:

```text
config/superbaddon/content_control.json
```

It controls supported recipes, resource access and selected item/entity entry points. It does **not** remove registry entries or provide a universal ban on every operation involving a target.

---

## Complete example

This example is intentionally restrictive. Test it in a copy of the world before deployment.

```json
{
  "_note": "Example only: review every target before enabling",
  "items": {
    "superbwarfare:taser": "block_load"
  },
  "vehicles": {
    "superbwarfare:ah_6": "recipes_only"
  },
  "blocks": {
    "superbwarfare:charging_station": "block_load"
  },
  "worldgen_ores": {
    "superbwarfare": true
  }
}
```

<pre>
items           <a href="#items">[1]</a>
vehicles        <a href="#vehicles">[2]</a>
blocks          <a href="#blocks">[3]</a>
mode values     <a href="#modes">[4]</a>
worldgen_ores   <a href="#ores">[5]</a>
reload/testing  <a href="#reload">[6]</a>
</pre>

<a name="items"></a>
## [1] `items`

An object mapping item registry IDs to mode strings. Use the ID of the gun/item, not its localized name. Matching `<gun>_blueprint` items inherit the base gun's configured mode unless a more specific retained entry applies.

`block_load` participates in blocked-stack checks, creative-list filtering, dropped-item rejection, recipe filtering and item/gun resource filtering. It does not promise to erase every existing inventory stack or intercept every other mod's custom interaction.

The default `items` object is empty. Removing a target or using `keep` means no independent restriction for that target; shared assets or other configured targets can still affect it.

<a name="vehicles"></a>
## [2] `vehicles`

Keys are **vehicle entity registry IDs**, for example `superbwarfare:ah_6`. Do not guess a separate `ah_6_container` item. Vehicle container stacks are identified through their block-entity NBT `EntityType` value.

`recipes_only` removes matching vehicle assembling outputs without blocking the entity or pruning its assets. `block_load` additionally derives vehicle resource filters, blocks matching container stacks and cancels/discards matching vehicle entities when they join a level.

> [!WARNING]
> Entity-join rejection can also affect entities loaded from an existing save. Do not treat `block_load` as a reversible visibility switch for vehicles already placed in a valuable world. Back up before testing and validate save/reload behavior.

<a name="blocks"></a>
## [3] `blocks`

Keys are block registry IDs. The implementation assumes the corresponding BlockItem uses the same ID. `block_load` adds blockstate/model/texture/GUI and block-loot-table resource filters, plus blocked-stack and related recipe/creative filtering.

It does **not** intercept general block placement and does **not** unload an existing BlockEntity. Existing placed blocks can retain logic while losing assets or normal loot data. It is therefore not a safe universal way to remove arbitrary machines from a world.

<a name="modes"></a>
## [4] Modes and actual boundaries

| Mode | Resource/entity restriction | Recipe behavior in this source |
| --- | --- | --- |
| `keep` | None from that entry | No restriction from that entry |
| `recipes_only` | None | Implemented for vehicle assembling outputs; ordinary item/block output filtering has a known gap |
| `block_load` | Supported resource and stack/entity checks | Filters supported blocked outputs and explicit required blocked inputs; not every custom recipe is inspectable |

Only these tokens are recognized; unrecognized modes fall back to `keep`. Do not use `disabled`, `remove` or `true` as substitutes.

For ordinary recipes, `shouldRemoveRecipe` tests the output with `blocksItemStack`, which checks `block_load`, not `recipes_only`. This is a source-verified limitation, not proof that a user's JSON is malformed. For recipe-only removal of ordinary items, use an appropriate datapack/script integration and verify the final recipe table. See [Recipe Blocking Guide](Recipe-Blocking-Guide).

The paths `vehicle_assembling_table` and `container` are protected infrastructure across namespaces. Resource and entity/stack checks deliberately avoid pruning this infrastructure; this protection is not a general promise about every recipe-filtering branch. Do not target it for broad removal.

<a name="ores"></a>
## [5] `worldgen_ores`

The shipped default is `{"superbwarfare": false}`. Set it to true to activate removal of the supported SuperbWarfare ore-generation features through the packaged biome modifier.

This does not unregister ore blocks/items, remove already generated ore or retroactively regenerate chunks. Although the parser accepts namespace keys, adding an arbitrary namespace does not manufacture a biome modifier for another mod's ores.

<a name="reload"></a>
## [6] Reload and verification

`/superbaddon content_control reload` rereads the file and rebuilds the query state/resource filters. It does not itself rerun the recipe rewrite or reload client resources.

For recipe changes run server `/reload` or restart. For resource trimming, align client and server configuration and restart the relevant instances. For ore-generation changes, restart and inspect newly generated terrain in a test world. Do not judge the ore setting by existing chunks.

Verify both the intended restriction and retained infrastructure: assembling tables, container rendering, unrelated recipes, nearby machines and save/reload behavior. Check the log for loaded target counts and removed recipe IDs; a successful parse alone is not acceptance testing.

## Resource layout and input filtering

Resource paths are derived by code; this file does not accept arbitrary file deletion lists. Filters stay within the same namespace and follow the layouts described in [Resource Trimming Guide](Resource-Trimming-Guide).

The recipe filter can remove a recipe when a required ingredient consists entirely of explicitly blocked item alternatives. If an ingredient includes a tag, it is conservatively retained at this stage rather than resolving incomplete tag bindings. Dynamic recipes may expose incomplete outputs/ingredients. These limits are documented in [Known Issues](Known-Issues).

## Source

[Config parser](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/content/ContentControlConfig.java) · [Queries and recipe filtering](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/content/ContentControlManager.java) · [Resource paths](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/content/ResourcePaths.java)
