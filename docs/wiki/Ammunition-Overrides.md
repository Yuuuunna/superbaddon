# Ammunition Overrides

[简体中文](弹药覆写) | [Shell Ejection](Shell-Ejection)

`ammo_override` edits SuperbWarfare gun or vehicle-weapon ammunition definitions. It does not itself select a casing, change projectile damage or create a new ammunition item.

---

## Complete example

Place this in an external format-5 file. Confirm that the listed item and gun exist in your installation before enabling it.

```json
{
  "format": 5,
  "rules": [
    {
      "enabled": true,
      "priority": 1000,
      "target": { "guns": ["superbwarfare:ak_47"] },
      "ammo_override": {
        "enabled": true,
        "policy": "replace",
        "allowed": ["superbwarfare:rifle_ammo"]
      }
    }
  ]
}
```

<pre>
enabled / allowed       <a href="#activation">[1]</a>
policy and merging      <a href="#policy">[2]</a>
ammunition formats      <a href="#ammo">[3]</a>
target definitions      <a href="#targets">[4]</a>
reload and restoration  <a href="#reload">[5]</a>
</pre>

<a name="activation"></a>
## [1] Activation

The containing rule must be enabled. `ammo_override.enabled` defaults to **false**. An override is active only when that flag is true and at least one valid nonempty `allowed` option exists.

An empty `allowed` list does not mean “make the gun unable to load anything”; it makes this override inactive. A disabled ammo override can coexist with active ejection or knockback behavior.

<a name="policy"></a>
## [2] `policy` and merging

The canonical policies are `replace` (default) and `append`. Only the string `append` selects append mode; other policy strings fall back to replace, so typos are potentially significant.

For each gun or vehicle-weapon target, the patcher gathers applicable definition patches. It keeps the **highest priority among replacement rules**, combines allowed options from all replacement rules at that priority, and then adds append options. Lower-priority replacements are not combined. Append contributions are gathered independently of replacement priority.

This is not the ejection system's “first matching rule wins.” Two equal-priority replace rules can broaden the allowed list instead of one replacing the other. Deduplication is based on normalized specifications/IDs and differs between gun consumers and raw vehicle weapon data; do not rely on duplicate options to encode distinct variants.

<a name="ammo"></a>
## [3] `allowed` entries

The recommended form is an array of actual ammunition specifications exported by the scanner or known to the main mod. The parser also accepts a single option instead of an array.

String options preserve the main-mod consumer syntax, including an optional count and supported `@`/`#` prefixes. Do not invent tag names or virtual ammunition identifiers. A simple item ID is sufficient only when an item-based consumer is appropriate.

An object form can preserve main-mod consumer data:

```json
{
  "format": 5,
  "rules": [
    {
      "enabled": true,
      "target": { "guns": ["superbwarfare:ak_47"] },
      "ammo_override": {
        "enabled": true,
        "policy": "append",
        "allowed": [
          {
            "ammo": "superbwarfare:rifle_ammo",
            "consumer": { "Ammo": "superbwarfare:rifle_ammo" }
          }
        ]
      }
    }
  ]
}
```

`consumer` is passed through to SuperbWarfare's consumer deserializer; it is not an arbitrary item-stack object. The outer identity can be read from `ammo`, `Ammo`, `id` or `Id`, then from the consumer. Without a nested consumer, the object itself is used after removing casing/comment metadata. Preserve scanner-exported consumer properties unless you understand the main-mod format.

Ammo identity matching normalizes case and whitespace and can match by extracted ID or full spec. This is not a strict NBT discriminator. MTS ammo-box handling is discussed in [MTS Compatibility](MTS-Compatibility).

<a name="targets"></a>
## [4] Definition targets are not per-shot guards

An active override needs explicit `target.guns` or `target.vehicles`. A projectile-only target does not apply ammunition patches.

Gun patches are collected for the listed gun definitions. Vehicle patches resolve listed vehicles, optionally their zero-based `seats`, and optional `weapons` names. A vehicle with no seat/weapon restriction can affect all its weapon definitions.

Per-shot filters such as `projectiles`, `requires_vehicle` and `min_projectiles_per_shot` do not turn definition patching into a conditional change for one shot. Avoid mixing gun and vehicle targets in one rule unless both definition changes are intentional. Use separate rules for separate responsibilities.

<a name="reload"></a>
## [5] Applying and restoring changes

`/superbaddon shell_ejection reload_external` applies active patches and invalidates relevant main-mod caches. A successful count means definitions were processed, not that the weapon was reloaded or a player's currently selected ammo state was corrected. Test a fresh weapon/vehicle and actual reload/fire behavior.

The patcher mutates in-memory definitions; it does not keep a universal original-definition snapshot. Removing or disabling a patch and running only `reload_external` is therefore **not guaranteed to restore the old list**. For rollback, remove the override, perform and verify a full main-mod data reload, or restart from the unchanged datapacks and configuration backup.

The server's patched definitions are not a general client JSON-file distribution mechanism. Validate client UI, reload selection, consumption and firing in multiplayer, not only in single-player.

## Source

[Override parser](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/shell/AmmoOverrideSpec.java) · [Ammo option identity](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/shell/AmmoOption.java) · [Definition patching and merge rules](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/compat/SuperbWarfareDataPatcher.java)
