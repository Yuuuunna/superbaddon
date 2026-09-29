# Shell Ejection JSON Format

[简体中文](抛壳配置) | [Configuration Overview](Configuration-Overview)

This page describes the current **format 5** parser and server-side casing behavior. [Ammunition Overrides](Ammunition-Overrides) and [Knockback](Knockback) share the same rule container but have separate effects.

> [!IMPORTANT]
> `target` must be an object when supplied. Historical examples using a target string, root `enabled_by_default` as a behavioral switch, or embedded `content_control` as an active content configuration are not supported by the current parser. The old document is preserved in the source archive linked in the footer.

---

## Complete working structure

Save as `config/superbaddon/shell_ejection/custom/ak47_casing.json`. The example leaves ammunition definitions unchanged and uses the addon's registered brass casing.

```json
{
  "format": 5,
  "rules": [
    {
      "enabled": true,
      "priority": 1000,
      "target": {
        "guns": ["superbwarfare:ak_47"]
      },
      "ejection": {
        "default": {
          "mode": "drop",
          "casing": "superbaddon:brass_casing",
          "count": 1,
          "chance": 1.0,
          "pickup_delay": 20,
          "fallback_to_drop": false,
          "ports": [
            {
              "id": "right_port",
              "offset": [0.2, -0.05, -0.15],
              "velocity": [0.08, 0.08, -0.02],
              "random_velocity": 0.035
            }
          ]
        }
      }
    }
  ]
}
```

<pre>
format / rules / enabled / priority  <a href="#wrapper">[1]</a>
target                              <a href="#target">[2]</a>
ejection.by_ammo / default          <a href="#selection">[3]</a>
mode / fallback_to_drop             <a href="#mode">[4]</a>
casing / count / chance / delay     <a href="#casing">[5]</a>
ports / offset / velocity           <a href="#ports">[6]</a>
matching / reload / testing         <a href="#testing">[7]</a>
</pre>

<a name="wrapper"></a>
## [1] File wrapper and rule controls

The root must be an object containing numeric `format: 5` and a `rules` array. A raw rule object, a root array, missing format or any other format number is rejected. Non-object array entries are skipped. Each object has `enabled: true` by default; use an explicit boolean in maintained files.

`priority` is an integer, default `0`. Rules sort by priority descending and then full generated rule ID ascending. An external ID derives from the relative filename and the rule's array index, so renaming files or reordering tied rules can change selection. External files do not inherently outrank datapacks.

Root `generated_by`, `generated_at`, `owner_type`, `owner`, `note`, `enabled_by_default` and `content_control` are metadata for this parser. Content control belongs in its [own file](Content-Control).

<a name="target"></a>
## [2] `target`

| Field | Accepted form | Match meaning |
| --- | --- | --- |
| `guns` | String or string array | Gun data/item ID represented in the shot context |
| `projectiles` | String or string array | Projectile ID represented in that context |
| `vehicles` | String or string array | Vehicle entity ID |
| `weapons` | String or string array | Vehicle weapon name or `vehicle_id/weapon_name` |
| `seats` | Integer or integer array | Zero-based vehicle seat indices |
| `requires_vehicle` | Boolean, default false | Reject shots without a vehicle context |
| `min_projectiles_per_shot` | Integer, default 0 | Minimum reported projectile count for that shot |

Within one set, any listed value can match. **Different nonempty fields are AND conditions.** An empty or omitted target is broad, not disabled. Never deploy a broad test rule accidentally.

Use [Scan](Shell-Ejection-Scan) to inspect actual vehicle weapon names. A weapon name is not necessarily an item ID or a model bone. Gun and vehicle IDs are not interchangeable. Ammunition patching uses definition targets rather than all per-shot conditions; see [Ammunition Overrides](Ammunition-Overrides).

<a name="selection"></a>
## [3] `ejection.by_ammo` and `ejection.default`

`by_ammo` is an object whose keys are ammunition specifications understood by `AmmoOption`; each value is one ejection specification. Matching checks normalized selected ammo ID **or** specification. It is not strict NBT equality, and variants sharing the same extracted ID can overlap.

Selection within a rule is: first matching `by_ammo` entry in declaration order, then `default`. A narrow legacy fallback also exists when there is exactly one ammo-specific entry and one active allowed-ammo option matching the shot. Keep keys aligned rather than relying on this fallback.

This complete example changes casing behavior for one selected ammunition and suppresses unmatched ammunition:

```json
{
  "format": 5,
  "rules": [
    {
      "enabled": true,
      "priority": 1000,
      "target": { "guns": ["superbwarfare:ak_47"] },
      "ejection": {
        "by_ammo": {
          "superbwarfare:rifle_ammo": {
            "mode": "drop",
            "casing": "superbaddon:brass_casing"
          }
        },
        "default": { "mode": "none" }
      }
    }
  ]
}
```

Use the actual selected ammo ID/spec from your installation; a weapon using a different virtual/tag specification may not match this illustrative key. `by_ammo` alone does not permit that ammunition to load, and an ejection-only rule does not need `ammo_override.allowed`.

<a name="mode"></a>
## [4] Disposal mode and storage fallback

| Canonical mode | Behavior |
| --- | --- |
| `drop` | Attempt an item drop for each configured point that passes its chance check |
| `store` | Use only the first point and insert its casing stack into the context's storage target through the Forge item-handler capability |
| `none` | Generate/store nothing for this selected ejection rule |

The default is `drop`. Legacy aliases for store are `stored`, `collect`, `container`; aliases for none are `ignore`, `discard_no_visual`, `disabled`. Unknown values fall back to drop, so use the canonical tokens.

`fallback_to_drop` defaults to **false**. It only affects uninserted remainders in `store` mode. When true, a full/missing storage capability can fall back to dropping the remainder. When false, that remainder is not spawned. It is **not** a fallback for invalid port coordinates, missing casing items or malformed JSON.

To suppress casing despite lower-priority rules, keep a sufficiently high-priority matching rule enabled and select `mode: "none"`. Disabling the rule itself allows other rules to match.

<a name="casing"></a>
## [5] Casing properties and defaults

| Field | Default | Meaning / validation |
| --- | --- | --- |
| `casing` | `superbaddon:brass_casing` | Registered item ID; absent/air/invalid items do not produce a stack |
| `count` | `1` | Stack size per point; clamped to at least 1 |
| `chance` | `1.0` | Per-point probability, clamped to 0–1 |
| `pickup_delay` | `20` | Nonnegative ticks before pickup; 20 ticks is about one second at normal tick rate |
| `offset` | `[0.2,-0.05,-0.15]` | Position offset in the firing frame |
| `velocity` | `[0.08,0.08,-0.02]` | Initial velocity in that frame |
| `random_velocity` | `0.035` | Nonnegative random disturbance magnitude |

`count: 0` does not disable an entry; it becomes 1. In drop mode, count is the size of a dropped stack, not a request to create that many separate entities. More ports and more firing events still create more dropped entities. See [Shell Reloading](Shell-Reloading) for casing items and crafting.

The runtime validates the first point's casing before processing the drop loop; a bad first casing can prevent later otherwise-valid ports too. Validate every casing ID.

<a name="ports"></a>
## [6] Ports and coordinate system

`ports` is an array of objects. Each point can override `id`, `casing`, `count`, `chance`, `pickup_delay`, `offset`, `velocity` and `random_velocity`; omitted point properties inherit the ejection entry's fallback values. `id` is a label, not a model-bone binding.

Legacy `points` is accepted when a valid `ports` array is not supplied. An empty or invalid-only point array falls back to one point rather than disabling ejection. Write three numeric components for vectors; object-form vectors are not the documented format.

The basis uses firing forward, world up and a horizontal side vector `(-forward.z, 0, forward.x)`. X is the side component, Y is world-up, and Z is along firing direction. It is **not** a full vehicle roll/bone-local transform. The origin comes from the shot position when available. Velocity also receives a quarter of the shooter's movement plus random disturbance.

Start with one port. Test facing several directions and on a moving vehicle before adding more ports. Store mode never processes a second storage port.

<a name="testing"></a>
## [7] Rule selection, reload and diagnosis

Ejection selects the first rule whose target matches and which yields an ejection specification. A matching target with no selected ejection can allow a lower rule to supply one. Knockback is selected independently; ammunition definitions have their own merge logic.

For external files run `/superbaddon shell_ejection reload_external`. For datapack rules use server `/reload`. Check the log for file-level parse errors, loaded counts and conflicting active rules. A malformed rule can reject that file's parse; do not assume every other rule in the same file survives.

Test the gun, vehicle weapon, seat and ammo variant you actually configured. If nothing appears, inspect `enabled`, target intersections, priorities, selected ammunition, `mode`, casing ID, storage capacity and content-control restrictions. See [Troubleshooting](Troubleshooting).

## Source

[Parser and target selection](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/shell/ShellRule.java) · [Ejection defaults](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/shell/ShellEjectionSpec.java) · [Server-side drop/store implementation](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/event/ShellEjectionEvents.java)
