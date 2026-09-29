# Quick Start

[简体中文](快速开始) | [Home](Home)

This walkthrough changes one observable behavior without changing ammunition or deleting content. Work in a disposable world, not your production save.

---

## [1] Establish a baseline

Complete [Installation](Installation), enter a world, and verify a supported gun before changing configuration. Back up `config/superbaddon/`. Record whether you are editing the client instance or dedicated server.

The configuration systems are independent. Start with one file and one target. Do not run an active scan over the whole pack as your first test.

## [2] Create one external rule

Create this file in the server's configuration directory, or the local instance for single-player:

```text
config/superbaddon/shell_ejection/custom/ak47_no_casing.json
```

Copy this complete JSON:

```json
{
  "format": 5,
  "rules": [
    {
      "enabled": true,
      "priority": 1000,
      "target": { "guns": ["superbwarfare:ak_47"] },
      "ejection": { "default": { "mode": "none" } }
    }
  ]
}
```

This requests no casing for the matching gun. It does not remove the gun, change damage, disable recipes or replace ammunition. `priority` must exceed competing applicable ejection rules; it is not an absolute global override switch.

## [3] Reload and test

```text
/superbaddon shell_ejection reload_external
```

Check the command result and server log. Fire the same weapon again with the same ammunition. The intended result is no casing from this matched rule while the gun still fires normally.

If it still ejects, verify the actual gun ID, active file location and competing priorities. If the command is unavailable, check operator permission and installation rather than changing the JSON at random.

## [4] Undo the change

Remove the test file or set its rule-level `enabled` to `false`, then run `reload_external` again. Lower-priority or built-in rules can now match again. Setting a rule to `false` does not mean “disable this weapon's casing everywhere.”

This rollback is safe for the example because it does not mutate ammunition definitions. For ammunition overrides, disabling a rule alone is not guaranteed to restore previously patched data; use a full data reload or restart and verify the restored ammunition list. See [Ammunition Overrides](Ammunition-Overrides).

## [5] Choose the next task

| Goal | Next step |
| --- | --- |
| Discover custom guns or vehicle weapon names | [Scan guide](Shell-Ejection-Scan); use the non-active scan and move edited rules into `custom/` |
| Choose casing items or ejection ports | [Shell Ejection](Shell-Ejection) |
| Stop high-damage hits launching targets | [Knockback](Knockback) |
| Change vehicle construction cost | [Vehicle Recipe Override](Vehicle-Recipe-Override), then `/reload` |
| Remove resources or supported ore generation | [Content Control](Content-Control), with client/server backups and restart testing |
| Use MTS fuel or controls | [MTS Compatibility](MTS-Compatibility) |

> [!WARNING]
> Both scan variants rebuild `generated_scan/` and remove the old `generated_scan.json`. Back up before scanning. JSON backups inside `shell_ejection/` are still loaded if their names end in `.json`.

## Source

[Rule parser](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/shell/ShellRuleParser.java) · [Rule ordering](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/shell/ShellRuleSet.java)
