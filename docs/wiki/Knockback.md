# Knockback Configuration

[简体中文](击退配置) | [Configuration Overview](Configuration-Overview)

Knockback is configured inside format-5 shell-rule files. It controls supported **direct-hit** and **explosion** impulse channels independently; it is not a damage multiplier or a universal physics override.

---

## Complete knockback-only example

Save in `config/superbaddon/shell_ejection/custom/controlled_knockback.json`:

```json
{
  "format": 5,
  "rules": [
    {
      "enabled": true,
      "priority": 1000,
      "target": { "guns": ["superbwarfare:ak_47"] },
      "knockback": {
        "direct": { "mode": "cap", "max": 0.2 },
        "explosion": { "mode": "cap", "max": 0.3 }
      }
    }
  ]
}
```

The numbers are tuning examples, not default balance values. This rule does not change ammo or casing behavior, and it does not make a non-explosive projectile explosive.

<pre>
direct / explosion    <a href="#channels">[1]</a>
mode / value          <a href="#modes">[2]</a>
min / max             <a href="#bounds">[3]</a>
by_ammo               <a href="#ammo">[4]</a>
priority and scope    <a href="#scope">[5]</a>
reload and tests      <a href="#tests">[6]</a>
</pre>

<a name="channels"></a>
## [1] Channels

`knockback.direct` handles supported non-explosion hurt knockback. `knockback.explosion` handles the supported custom-explosion path. Either channel can be omitted, in which case it inherits the original behavior within that profile.

The direct runtime uses the vanilla hurt baseline `0.4` when it needs to supply a missing custom strength. It respects SuperbWarfare's `NO_HURT_EFFECT` suppression and does not blindly replace an already supplied custom strength. Target resistance, other hooks and the actual damage path can still affect the observed motion.

<a name="modes"></a>
## [2] Modes and `value`

| Mode | Calculation before optional bounds | Required field |
| --- | --- | --- |
| `inherit` | Keep original value | None |
| `scale` | Original × value | `value` |
| `fixed` | Use value | `value` |
| `cap` | Keep original, then apply upper bound | `max` |
| `disabled` | Start from zero | None |

The default is pure `inherit`. Unknown modes, missing required values, or invalid required numbers cause a warning and fallback to inherit. `value`, `min` and `max` must be finite nonnegative JSON numbers; do not use percentages or numeric strings.

A scale of 0.5 halves the supplied value, not the damage. A fixed value sets an impulse parameter, not a guaranteed travel distance.

<a name="bounds"></a>
## [3] Bounds and evaluation order

The implementation calculates the mode result, then applies `min`, then `max`, and finally clips negative results to zero. Keep `min <= max`; the parser does not enforce a meaningful interval for you.

> [!WARNING]
> `disabled` is not an unconditional last step: a positive `min` can raise its zero result again. To request no knockback, use only `{"mode":"disabled"}` without a positive minimum. Similarly, `inherit` with bounds is an active constrained channel, not pure inheritance.

## Complete zero-impulse example

```json
{
  "format": 5,
  "rules": [
    {
      "enabled": true,
      "priority": 1000,
      "target": { "guns": ["superbwarfare:ak_47"] },
      "knockback": {
        "direct": { "mode": "disabled" },
        "explosion": { "mode": "disabled" }
      }
    }
  ]
}
```

<a name="ammo"></a>
## [4] `by_ammo`

An optional `knockback.by_ammo` object uses the same normalized ammo matching as ejection. The first matching entry overlays the rule's base profile **one whole channel at a time**.

```json
{
  "format": 5,
  "rules": [
    {
      "enabled": true,
      "priority": 1000,
      "target": { "guns": ["superbwarfare:ak_47"] },
      "knockback": {
        "direct": { "mode": "cap", "max": 0.3 },
        "explosion": { "mode": "cap", "max": 0.4 },
        "by_ammo": {
          "superbwarfare:rifle_ammo": {
            "direct": { "mode": "fixed", "value": 0.1 }
          }
        }
      }
    }
  ]
}
```

For that ammo, the direct channel becomes fixed 0.1 and the explosion channel keeps its base cap. The base direct `max` is not merged into the replacement channel. A pure-inherit ammo channel keeps the base channel; it cannot reset a constrained base to vanilla by itself. Verify the actual selected ammo identity in your installation.

<a name="scope"></a>
## [5] Rule selection and supported scope

Knockback independently chooses the first matching rule whose selected profile is not entirely pure inherit. It does not require a matching casing rule. Once selected, the **whole profile** is used; unspecified channels are not separately filled from lower-priority rules.

A high-priority rule containing only pure-inherit channels does not block a lower-priority active profile. Use explicit active channel values when you intend to change behavior.

Profiles are attached through the addon's supported SuperbWarfare projectile/explosion hooks. Other mods' projectiles, direct velocity assignments, melee systems or damage paths that bypass these hooks are not automatically controlled. Do not infer complete global knockback suppression from a successful configuration load.

<a name="tests"></a>
## [6] Reload and verify

For external files run `/superbaddon shell_ejection reload_external`; for datapack rules run server `/reload`. There is no separate `knockback reload` command.

Test direct hits and explosion effects separately on the same target setup. Keep weapon damage, distance, ammunition and target resistance fixed while comparing. Then check an untargeted weapon to confirm the rule is narrow enough. If damage is correct but motion differs, inspect the runtime damage path and other supplied knockback strengths rather than treating the cap as a damage setting.

## Source

[Channel parsing and calculation](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/knockback/KnockbackChannelSpec.java) · [Ammo profile selection](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/knockback/KnockbackTable.java) · [Runtime hooks](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/knockback/KnockbackRuntime.java)
