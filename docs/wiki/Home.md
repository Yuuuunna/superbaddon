# SuperbAddon Wiki

[简体中文](首页)

SuperbAddon is a data-driven addon and compatibility layer for **SuperbWarfare on Minecraft Forge 1.20.1**. It provides spent casings and reloading recipes, configurable ammunition and knockback, armor penetration, content/resource control, vehicle assembling recipe overrides, and optional MTS integration.

> [!IMPORTANT]
> This documentation describes source commit `ab9693f` (2026-09-29), not a claim that every dependency version or modpack has been tested. Start with [Installation](Installation) and consult [Known Issues](Known-Issues) before editing an existing world.

---

## Start here

| Goal | Read |
| --- | --- |
| Install the addon on a client or server | [Installation](Installation) |
| Make and verify your first change | [Quick Start](Quick-Start) |
| Find the correct configuration file | [Configuration Overview](Configuration-Overview), [Directory Structure](Config-Directory-Structure) |
| Understand shared editing and reload rules | [Common Configuration](Common-Configuration) |
| Get a direct answer to a common question | [FAQ](FAQ) |

## Feature reference

| Feature | Documentation |
| --- | --- |
| Items, vehicles, blocks, supported ore generation | [Content Control](Content-Control) |
| Casing selection, ports, drop/store/none modes | [Shell Ejection](Shell-Ejection) |
| Allowed ammunition and merge policies | [Ammunition Overrides](Ammunition-Overrides) |
| Direct-hit and explosion impulse | [Knockback](Knockback) |
| Damage penetration and explosion falloff | [Armor Penetration](Armor-Penetration) |
| Vehicle assembly inputs, categories and disabling | [Vehicle Recipe Override](Vehicle-Recipe-Override) |
| Jerrycans, fuel pumps, controls and camera | [MTS Compatibility](MTS-Compatibility) |
| Casing items and counted crafting ingredients | [Shell Reloading](Shell-Reloading) |

## Administration and pack authoring

Read [Command Reference](Command-Reference) and [Shell Ejection Scan](Shell-Ejection-Scan) for command permissions, output and side effects. For a complete pack workflow, use [Resource Trimming](Resource-Trimming-Guide), [Recipe Blocking](Recipe-Blocking-Guide) and [Recommended Workflow](Recommended-Workflow).

Problems and integration boundaries are covered by [Troubleshooting](Troubleshooting), [Compatibility](Compatibility) and [Known Issues](Known-Issues). Maintainers should read [Design Notes](Design-Notes), [Data Format Versions](Data-Format-Versions) and [Changelog](Changelog).

## Keep configuration responsibilities separate

Removing an assembling recipe does not unregister its vehicle. Disabling an individual shell rule does not suppress lower-priority rules. A server command does not distribute local client JSON files. Resource trimming is not a guarantee that an existing save can safely lose arbitrary content.

The recovered Wiki has been preserved unchanged in the [source archive](https://github.com/Yuuuunna/superbaddon/tree/master/docs/wiki-archive/2026-09-29). Existing page addresses remain available; corrected examples in the current pages take precedence over historical examples.
