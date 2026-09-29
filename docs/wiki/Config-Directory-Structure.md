# Config Directory Structure

[简体中文](配置目录结构) | [Configuration Overview](Configuration-Overview)

A path belongs to a running instance, not to an account. Two launchers, a dedicated server and a development `run/` directory can each have different `config/` folders.

---

## [1] Instance-wide configuration

```text
<instance>/
├─ mods/
├─ config/
│  └─ superbaddon/
│     ├─ content_control.json
│     ├─ vehicle_recipe_override.json
│     ├─ armor_penetration.json
│     ├─ mts_compat.json
│     └─ shell_ejection/
│        ├─ custom/
│        │  ├─ guns.json
│        │  └─ vehicle_rules.json
│        └─ generated_scan/
│           └─ superbwarfare/
│              ├─ items/
│              │  └─ ak_47.json
│              └─ vehicles/
│                 └─ ah_6.json
└─ logs/
   └─ latest.log
```

The four named root JSON files are independent fixed-path configurations. Do not split `content_control.json` into arbitrary subfiles: that loader does not recursively scan a directory.

The shell directory is different: all regular `.json` files below it are loaded recursively. `custom/` is recommended because scanner output is disposable, not because `custom/` receives special priority. Use simple lowercase ASCII filenames to avoid collisions in sanitized external rule IDs.

## [2] World datapacks

```text
<world>/datapacks/<pack>/
├─ pack.mcmeta
└─ data/<namespace>/
   ├─ shell_ejection/
   │  └─ rules.json
   └─ recipes/
      └─ casing_reload.json
```

Datapack `pack.mcmeta` and the shell file's `format: 5` are unrelated version systems. A JSON file under `assets/` is not a server datapack shell rule. A crafting recipe under `config/superbaddon/shell_ejection/` will be rejected because it is not a format-5 rule document.

## [3] Generated versus maintained files

[Scan](Shell-Ejection-Scan) outputs are grouped by namespace, owner type and registry path. The scanner deletes the old generated directory before writing new candidates; it also removes the historical `generated_scan.json` file.

Move useful candidates into `custom/`, simplify them and explicitly review each `rules[].enabled`. Keep only one maintained copy of each intended rule. A backup named `rules.backup.json` anywhere under `shell_ejection/` is still executable configuration; store backups elsewhere or use a non-JSON extension.

## [4] Client and server ownership

The dedicated server's JSON does not automatically replace the client's JSON. Deploy matching resource/content rules with your client pack. Keep player-specific controls local. A world datapack moves with that world; instance `config/` normally applies across worlds using the same instance.

## [5] Repository documentation is not game configuration

`docs/wiki/` is the documentation source synchronized to GitHub Wiki. Do not place runtime configuration there expecting Minecraft to load it. `docs/wiki-archive/` contains historical documentation, not active game rules.

## Source

[Fixed content path](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/content/ContentControlConfig.java) · [Recursive external loader](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/shell/ExternalShellRuleLoader.java) · [Datapack loader](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/shell/ShellRuleReloadListener.java)
