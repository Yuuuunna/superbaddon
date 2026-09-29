# Armor Penetration

[简体中文](护甲穿透) | [Configuration Overview](Configuration-Overview)

Armor penetration is configured independently in `config/superbaddon/armor_penetration.json`. It affects supported SuperbWarfare damage paths against living entities; it is not a generic armor-attribute editor.

---

## Complete default configuration

The generated file can also contain explanatory notes and serialize fractions as strings. Both the numeric fractions below and quoted percentages are accepted.

```json
{
  "enabled": true,
  "armor_penetration_by_weapon": {
    "superbwarfare:small_cannon_shell": 0.8,
    "superbwarfare:grapeshot": 0.35
  },
  "full_armor_bypass_weapons": [
    "superbwarfare:cannon_shell",
    "superbwarfare:small_rocket",
    "superbwarfare:medium_rocket",
    "superbwarfare:rpg_rocket_standard",
    "superbwarfare:rpg_rocket_tbg",
    "superbwarfare:wire_guide_missile",
    "superbwarfare:javelin_missile",
    "superbwarfare:agm_65",
    "superbwarfare:kh_39",
    "superbwarfare:igla_9k38_missile",
    "superbwarfare:ru_9m336_missile",
    "superbwarfare:swarm_drone",
    "superbwarfare:mk_82",
    "superbwarfare:sc_50",
    "superbwarfare:sc_250",
    "superbwarfare:mortar_shell"
  ],
  "explosion": {
    "minimum_outer_penetration": 0.35,
    "inner_radius_factor": 0.35,
    "outer_falloff_power": 2.0
  }
}
```

<pre>
enabled / lookup order             <a href="#lookup">[1]</a>
armor_penetration_by_weapon        <a href="#partial">[2]</a>
full_armor_bypass_weapons           <a href="#full">[3]</a>
explosion                          <a href="#explosion">[4]</a>
reload / status / autofill         <a href="#commands">[5]</a>
</pre>

<a name="lookup"></a>
## [1] `enabled`, scope and ID lookup

`enabled` defaults to true and gates **configured lookup**. Supported damage types are SuperbWarfare projectile hit, grapeshot hit, projectile explosion and custom explosion, when they reach the addon's damage hooks and the target is a LivingEntity.

For a supported projectile, lookup first tries its reported gun-item ID, then its entity type ID, then its own built-in bypass rate. A positive configured match is used; zero/no match can fall through. Within a configured ID, membership in the full-bypass set takes precedence over the partial map.

> [!IMPORTANT]
> Setting a map value to zero is not a veto against entity-type or projectile-native fallback. Likewise, `enabled: false` disables this file's lookups but does not remove the projectile's own bypass rate. Do not describe this switch as disabling every source of penetration.

Use projectile/gun IDs actually carried by the damage source, not a display name or an assumed vehicle model name. Do not add a generic projectile ID globally unless broad effects are intentional.

<a name="partial"></a>
## [2] Partial penetration

Values are clamped to 0–1. `0.35` and `"35%"` mean 35%; plain `35` is clamped to 1, not interpreted as 35%.

The helper splits requested damage D into a bypass part `D × p` and a defendable part `D × (1 - p)`. The defendable part passes through armor, magic reduction and absorption; the bypass part is added directly to resulting health damage. This is not merely subtracting p percent of the target's armor attribute, nor a linear interpolation of already-reduced total damage.

The implementation uses a specialized health-damage path. It retains checks for nonpositive damage, client-side execution, dead targets, relevant invulnerability/fire immunity, creative/spectator players and team friendly-fire restrictions. This does not establish compatibility with every other mod's damage events or defensive mechanics.

<a name="full"></a>
## [3] Full bypass

IDs listed in `full_armor_bypass_weapons` receive full configured bypass before distance falloff is considered. A partial value at or above the implementation threshold **0.999** also selects the full path.

Full bypass does not mean every entity can be hurt or that ordinary safety/team checks are removed. An explosion sourced by a full-bypass projectile can still have lower effective penetration away from its center.

<a name="explosion"></a>
## [4] Explosion falloff

| Field | Default | Meaning |
| --- | --- | --- |
| `minimum_outer_penetration` | 0.35 | Outer floor, capped at the source penetration so it never raises a weak source |
| `inner_radius_factor` | 0.35 | Fraction of radius defining the inner segment; clamped to 0–1 |
| `outer_falloff_power` | 2.0 | Shape of the outer segment; minimum 0.1 |

Distance ratio uses distance from explosion center to the target position divided by radius, clipped to 0–1. The inner segment is **not flat**: it already accounts for 15% of the total drop by its outer boundary. The outer segment supplies the remaining 85% with the configured power.

With source penetration 1 and default settings:

| Relative distance | Effective penetration |
| --- | --- |
| Center | 1.0 |
| 35% of radius | 0.9025 |
| 100% of radius | 0.35 |

This changes penetration, not the explosion's separate damage falloff or [knockback](Knockback). The source also carries contexts for supported delayed SuperbWarfare shockwave damage; arbitrary explosions without a resolvable context are not automatically covered.

<a name="commands"></a>
## [5] Reload, status and autofill

```text
/superbaddon armor_penetration reload
/superbaddon armor_penetration status
/superbaddon armor_penetration autofill
```

All require permission level 2. `reload` rereads the JSON; `status` reports loaded counts and path, not an effective-per-hit diagnostic.

`autofill` scans loaded main-mod vehicle weapon definitions and adds only IDs not already configured. It classifies weapons using existing bypass data, text/name hints and damage/explosion thresholds. For example, it proposes partial values for recognized autocannon/machine-gun families and full bypass for recognized heavy/explosive categories. It deliberately avoids automatically adding the generic `superbwarfare:projectile` entity ID, although a valid specific weapon ID can still be added.

These are heuristic candidates, not measured armor performance. Back up valid JSON first, inspect the added entries and log, then test direct hits and several explosion distances. Existing configured IDs are preserved; repeated autofill is not a reset-to-default command.

## Legacy input and failure handling

Legacy camelCase map/set names and a `"namespace:id=35%"` array form are accepted. Prefer the canonical snake_case object/array form above. Avoid supplying both naming styles: the legacy partial map is read after the canonical map, while full-bypass sets are combined. Older root explosion settings also have compatibility reads; nested `explosion` fields take precedence. See [Data Format Versions](Data-Format-Versions).

A malformed file can cause defaults to be used with a log warning. Do not run writing commands over an unreviewed malformed configuration assuming all original text will be preserved.

## Source

[Configuration and defaults](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/penetration/ArmorPenetrationConfig.java) · [Damage and falloff formulas](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/penetration/ArmorPenetrationRules.java) · [Actual lookup and damage scope](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/util/CopiedHurtCalculator.java) · [Autofill heuristics](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/penetration/ArmorPenetrationAutofill.java)
