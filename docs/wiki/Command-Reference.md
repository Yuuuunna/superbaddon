# Command Reference

[简体中文](命令参考) | [Home](Home)

All commands below belong to `/superbaddon` and require **permission level 2**. Run them as an operator or in the server console. Omit the leading slash in the console. Single-player also needs sufficient command permission.

---

## Registered command tree

<pre>
/superbaddon
├─ shell_ejection
│  ├─ reload_external  <a href="#external">[1]</a>
│  └─ scan             <a href="#scan">[2]</a>
│     └─ active        <a href="#scan">[2]</a>
├─ content_control
│  └─ reload           <a href="#content">[3]</a>
└─ armor_penetration
   ├─ reload           <a href="#armor">[4]</a>
   ├─ status           <a href="#armor">[4]</a>
   └─ autofill         <a href="#autofill">[5]</a>
</pre>

These are the seven registered command leaves in the documented source. There is no addon-wide `/superbaddon reload`, no standalone knockback reload, and no dedicated MTS or vehicle-recipe reload command.

<a name="external"></a>
## [1] Reload external shell rules

```text
/superbaddon shell_ejection reload_external
```

Reads JSON files recursively under `config/superbaddon/shell_ejection/`, replaces the external part of the combined rule table, applies active ammunition-definition patches, and refreshes content-control state. Datapack rules already in memory remain part of the combined table.

Use it for external ejection, ammunition and knockback rules. The response reports loaded external rules and applied ammunition changes, along with content-control target counts. A count proves that entries were processed, not that your particular shot matched.

It does not reload all datapacks, rebuild recipes, reload client resources or reread the independent MTS/penetration files. Removing an ammo override is not guaranteed to undo earlier definition mutations through this command alone; see [Ammunition Overrides](Ammunition-Overrides).

<a name="scan"></a>
## [2] Generate scan candidates

```text
/superbaddon shell_ejection scan
/superbaddon shell_ejection scan active
```

The first emits disabled candidates; the second emits active candidates. Both scan registered and loaded content, write generated files, reload the external table, apply active ammo patches, and refresh content control.

> [!WARNING]
> **Neither is a read-only dry run.** Both delete and rebuild `generated_scan/` and remove legacy `generated_scan.json`. Even the default scan can deactivate behavior that depended on previously active generated rules. Back up and move maintained rules into `custom/` first.

Output reports candidate rules/files, registered guns/vehicle types, temporary vehicle instances inspected, loaded vehicle weapon owners, namespaces and applied ammo changes. These are not compatibility-test results. For interpretation and a safe procedure, read [Shell Ejection Scan](Shell-Ejection-Scan).

<a name="content"></a>
## [3] Reload content-control state

```text
/superbaddon content_control reload
```

Rereads `config/superbaddon/content_control.json` and rebuilds the query tables and derived resource filters. It does not retroactively rebuild the recipe table or reload client assets. Use server `/reload` or restart for recipes, and restart affected client/server instances for a verified resource-configuration refresh. Ore changes require testing new terrain, not existing chunks.

See [Content Control](Content-Control) for the ordinary item/block `recipes_only` limitation and the risks of blocking existing vehicles.

<a name="armor"></a>
## [4] Penetration reload and status

```text
/superbaddon armor_penetration reload
/superbaddon armor_penetration status
```

`reload` rereads `armor_penetration.json`. `status` reports enabled state, configured partial/full counts and file path. Status is read-oriented and does not measure effective penetration for an individual damage event.

A configured zero or disabled lookup can still allow native projectile fallback. Use [Armor Penetration](Armor-Penetration) to interpret the result instead of assuming the status flag suppresses every bypass mechanic.

<a name="autofill"></a>
## [5] Add missing penetration candidates

```text
/superbaddon armor_penetration autofill
```

Scans loaded vehicle weapon definitions, appends missing heuristic penetration entries to the JSON, then reloads the configuration. Existing configured IDs are retained. It is not a destructive reset of all balance settings and is not a precise ballistic calculation.

Back up a valid file before running it. Inspect newly added IDs, skipped generic weapons and warnings, then test the actual damage paths. A malformed input file should be repaired before using a command that writes it.

## Server `/reload` is separate

Minecraft's server `/reload` reloads datapacks and invokes the addon's registered reload listeners. Use it after datapack shell/recipe changes and vehicle recipe overrides. It is not one of the seven addon command leaves and is not a substitute for restarting when client-local cached resource settings must change.

## Command failure checklist

If the root is missing, check that the server loaded the addon and you have permission. If parsing fails, preserve the full error and the named file. If a command succeeds but behavior is unchanged, check the correct instance directory, exact target IDs, active rule fields and required reload layer. Do not repeatedly run `scan active` as a generic repair command.

## Source

[Registered command implementation](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/command/ShellEjectionCommands.java) · [Datapack reload listener](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/shell/ShellRuleReloadListener.java) · [Recipe rewrite listener](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/event/RecipeRewriteEvents.java)
