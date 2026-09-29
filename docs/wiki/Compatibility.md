# Compatibility

[简体中文](兼容性) | [Installation](Installation)

Compatibility has three different meanings: a dependency is declared loadable, the addon implements a guarded integration path, and a particular installed combination has passed runtime tests. This page documents the first two; it does not invent the third.

---

## [1] Required environment

The source builds against Minecraft 1.20.1, Forge 47.4.0, SuperbWarfare 0.8.9.1 and GeckoLib 4.4.6, using Java 17. Runtime metadata requires the libraries on both sides and declares minimum/range constraints detailed in [Installation](Installation).

Mixin targets, raw weapon-data fields and reflected cache internals can change despite a dependency version still satisfying a broad range. Match client/server gameplay builds and validate upgrades in a test instance. This source is not a Fabric build.

## [2] Optional integration map

| Integration | Implemented boundary | What still needs testing |
| --- | --- | --- |
| MTS / Immersive Vehicles | Guarded fuel bridge, ammo quantities, client control and camera paths | Actual MTS build, content pack, inventory handlers, camera mods and multiplayer behavior |
| Epic Core API (`eca`) | Optional set-health facade; absent/failing bridge returns to the native fallback path, with failures logged once | Defensive mechanics and event behavior of the installed ECA combination |
| KubeJS or other recipe rewriting | Post-load listener is designed to rewrite the final recipe table after the intended script stage | Actual ordering with all installed recipe integrations and caches |
| Datapack shell rules | Same format-5 parser and merged rule ordering as external files | Conflicting IDs/priorities and main-mod definition load ordering |
| Automated crafting | Counted casing recipe plus vanilla ResultSlot consumption hook | Any machine/menu that bypasses that hook |

MTS is not required to make the base addon load. The ECA facade checks for mod ID eca before referencing its typed implementation and catches linkage/runtime failures. These guards reduce optional-dependency coupling; they are not universal support for unrelated APIs.

## [3] Server gameplay versus client-local data

External configuration is local to each instance. There is no general mechanism here that sends all server JSON files into a player's config directory. The server performs server-side drops, damage and recipe management; clients still need corresponding installed content and local resource/control settings where those paths read them.

Distribute agreed content/resource configuration with the client pack. Do not overwrite player-specific input choices just to synchronize server fuel rules. Test joining and reconnecting, not only a local single-player world.

## [4] Damage and physics integrations

Armor penetration is implemented through a specialized health-damage path for supported SuperbWarfare damage types against living entities. It retains explicit invulnerability, game-mode and team checks, but may not reproduce every callback or defensive mechanic expected by another damage mod.

Knockback profiles travel through supported projectile and custom-explosion hooks. They do not automatically intercept another mod's direct velocity assignment, melee knockback or custom damage path. Camera tilt and MTS input also do not replace vehicle physics. See [Armor Penetration](Armor-Penetration) and [Knockback](Knockback).

## [5] Content and recipe integrations

Content filtering follows known resource layouts within the same namespace. Shared models, custom layouts, tag ingredients and dynamic recipe results can escape or broaden the expected effect. `block_load` is not a general registry uninstall operation, and ordinary `recipes_only` output filtering has a known gap.

Vehicle overrides affect the main mod's VehicleAssemblingRecipe type and preserve the output. A different custom recipe type with a similar UI is not automatically supported. See [Resource Trimming Guide](Resource-Trimming-Guide), [Recipe Blocking Guide](Recipe-Blocking-Guide) and [Known Issues](Known-Issues).

## [6] Upgrade acceptance

Before updating a dependency, preserve the old JAR/config/world set. Check startup mixin/linkage errors first, then exercise one example of each integration actually used by the pack. Compare client and server logs. Keep a record of the exact tested versions and expected outcomes using [Recommended Workflow](Recommended-Workflow).

No game runtime, large modpack or dedicated-server acceptance test was executed as part of this documentation completion. The distinction matters when deciding whether to deploy to a valuable world.

## Source

[Dependency metadata](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/resources/META-INF/mods.toml) · [Optional ECA facade](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/compat/eca/EpicCoreApiBridge.java) · [MTS facade](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/compat/mts/control/MtsControlBridge.java) · [Recipe listener](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/event/RecipeRewriteEvents.java)
