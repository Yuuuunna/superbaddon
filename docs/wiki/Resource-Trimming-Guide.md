# Resource Trimming Guide

[简体中文](资源裁剪指南) | [Content Control](Content-Control)

Resource trimming is for pack authors who deliberately exclude supported content and its associated data/assets. It is not a disk-cleaning command, a registry-removal feature or a guarantee of a particular memory saving.

---

## [1] Choose the least disruptive operation

To change crafting access, prefer [recipe controls](Recipe-Blocking-Guide). To change weapon behavior, use shell/ammo/knockback rules. Use `block_load` only when you actually want the supported resource and runtime restrictions together.

Work on a world copy and keep an unmodified client/server configuration backup. Existing vehicle entities can be rejected on load, and existing blocks can retain logic while their resources are filtered. A visual disappearance is not evidence of safe world migration.

## [2] Configure registry targets, not arbitrary paths

A complete minimal example is:

```json
{
  "items": { "superbwarfare:taser": "block_load" },
  "vehicles": {},
  "blocks": {},
  "worldgen_ores": { "superbwarfare": false }
}
```

Save this in `config/superbaddon/content_control.json`. Confirm the target exists in your installation. The code derives its resource filters from the registry ID; no JSON field accepts an arbitrary filesystem path or directory deletion command.

## [3] Derived resource layout

For a target `namespace:path`, filters stay within that same namespace. The following are resource-manager paths under the relevant namespace, not files removed from the JAR.

| Target | Exact match | Prefix families, each followed by `/path` |
| --- | --- | --- |
| Item/gun | `sbw/guns/path.json` | `models/item`, `models/displaysettings`, `textures/item`, `textures/gun_icon`, `geo`, `geo/lod`, `animations` |
| Vehicle | `sbw/vehicles/path.json` | `textures/vehicle_icon`, `geo`, `geo/lod`, `animations`, `textures/entity`, `textures/bedrock/vehicle`, `textures/bedrock/vehicle_lod`, `models/bedrock/vehicle`, `models/bedrock/vehicle_lod`, `animations/bedrock/vehicle`, `sounds/vehicle` |
| Block | `loot_tables/blocks/path.json` | `blockstates`, `models/block`, `models/item`, `models/bedrock/block`, `textures/block`, `textures/bedrock/block`, `textures/item`, `textures/entity`, `textures/gui` |

A prefix matches the exact prefix or a continuation beginning with `/`, `.` or `_`. Thus `foo_bar` can be part of a `foo` resource family, while an unrelated `foobar` is not matched merely by sharing its first characters. Shared asset naming can still make a narrow-looking target affect another model.

There is no blanket namespace deletion and no automatic removal of every asset another mod happens to associate with that ID. The hard-coded layouts support known main-mod conventions, including old GeckoLib and newer bedrock vehicle directories. Other content layouts need separate verification.

## [4] Keep infrastructure intact

Paths named `vehicle_assembling_table` and `container` are protected from the core resource/stack/entity pruning checks across namespaces. Do not attempt to circumvent this protection by guessing alternate IDs. Assembly infrastructure, containers and retained vehicle rendering should be tested after every content batch.

Item `_blueprint` handling can follow its base gun's content mode. Vehicle container identity comes from `EntityType` NBT, not a separate item ID for each vehicle. Keep these derived associations in mind when validating inventory and creative-list results.

## [5] Ore-generation scope

The packaged biome modifier activates when `worldgen_ores.superbwarfare` is true. It targets the overworld biome tag and the underground-ores step, removing these six SuperbWarfare features: silver ore, deepslate silver ore, galena ore, deepslate galena ore, scheelite ore and deepslate scheelite ore.

This controls those generation features, not all ores in every dimension. It does not remove existing blocks, unregister ore items or create support for an arbitrary added namespace. Restart and test new terrain in a disposable world rather than deleting production chunks to check the setting.

## [6] Deployment and acceptance tests

Ship consistent content/resource configuration to the server and affected clients; server commands do not distribute those local files. Restart affected instances so cached configuration and resource managers are rebuilt together.

Check the target's supported use/render/drop/recipe paths, then test retained infrastructure and assets that may share a prefix. Save, exit and reopen the test world to expose entity-load effects. For blocks, explicitly check that this is not being mistaken for placement prevention or BlockEntity removal.

Measure startup time, memory or frame time only with a repeatable before/after test on the same pack and hardware. The existence of filters alone does not establish a quantified performance improvement.

## [7] Rollback

Restore the original content configuration and restart. This can restore access to resources, but cannot reconstruct an entity already discarded and saved out of a world, consumed items or other persisted changes. Restore the world backup when the test altered persistent world state.

## Source

[Exact resource mappings and prefix semantics](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/content/ResourcePaths.java) · [Content queries](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/content/ContentControlManager.java) · [Vehicle join rejection](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/event/ContentControlRuntimeEvents.java) · [Ore feature list](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/resources/data/superbaddon/forge/biome_modifier/remove_superbwarfare_ores.json)
