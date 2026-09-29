# Shell Ejection Scan

[简体中文](抛壳扫描命令) | [Command Reference](Command-Reference)

The scanner is a **discovery and candidate-generation tool**. It helps identify guns, vehicle weapons, seats and ammunition specifications present in the running installation. It is not an automatic certification that every generated rule works or is desirable.

---

## [1] Choose the mode deliberately

```text
/superbaddon shell_ejection scan
```

Generates disabled candidate rules. This is the recommended starting mode, but it still rewrites generated files and reloads external rules.

```text
/superbaddon shell_ejection scan active
```

Generates active candidates and applies the resulting active rules during the command. It does not scan a broader registry or provide a more accurate classification. Use it only after understanding the generated schema and consequences in a disposable environment.

Both require permission level 2. Neither accepts a documented per-item or per-namespace argument in this source version.

## [2] What is scanned

The scanner collects registered gun candidates, registered vehicle types and weapon definitions obtainable by inspecting temporary vehicle instances. It also merges weapons found on already loaded vehicles. It recognizes the main `superbwarfare` namespace and namespaces of mods declaring a dependency on it, while using additional runtime/name heuristics where appropriate.

Temporary entity construction is used for inspection and the instances are discarded; this is not intended to spawn a fleet into the world. Constructors and other mods' entity implementations can still be expensive or have side effects. Run the scan deliberately, not repeatedly as a periodic maintenance task.

Seat inspection is bounded at 32 seats. Unknown ammo or inaccessible weapon data may produce placeholders or incomplete candidates. Loaded instances can help reveal data absent from a registry-only inspection, but missing scan output is not conclusive proof that a weapon cannot be configured manually.

## [3] Output ownership and destructive boundary

```text
config/superbaddon/shell_ejection/
├─ custom/                       # Maintained rules: not deleted by scan
└─ generated_scan/                # Rebuilt by every scan
   └─ <namespace>/
      ├─ items/<registry_path>.json
      └─ vehicles/<registry_path>.json
```

The writer removes the complete previous `generated_scan/` tree and the legacy single `generated_scan.json` before writing new files. Files in `custom/` are not removed by that writer, but they still participate in the reloaded rule set.

> [!WARNING]
> A disabled scan can replace previously enabled scan files with disabled candidates. Back up before both variants. A `.json` backup left inside the recursively loaded shell directory remains an active input, so move backups outside it.

## [4] Review candidate fields

Inspect each generated file's `rules` array, not only the descriptive root metadata. Review the actual `enabled`, `priority`, object-valued target, allowed ammunition, ejection entries and any optional knockback fields.

Root owner fields and `enabled_by_default` describe generation. Embedded `content_control` fields are not read as active content control by the current format-5 parser. Put content restrictions in [Content Control](Content-Control), not into scan metadata.

A candidate may include one rule for each known ammo variant. Empty ammo placeholders are not usable ammunition definitions. Check registry IDs, seat indices and the weapon names against the actual vehicle. Keep ammo consumers unchanged unless changing reload behavior is part of the intended task.

## [5] Safe promotion workflow

1. Back up the configuration and use a test world with the intended mods and datapacks loaded. Run the ordinary, non-active scan.
2. Select one useful owner's candidates and **move** them into a maintained file under `custom/`. Remove unrelated variants and descriptive clutter only after understanding them.
3. Set `rules[].enabled` explicitly, choose a deliberate priority, and retain only the features needed. For example, a casing-only rule should not accidentally replace allowed ammunition.
4. Run `/superbaddon shell_ejection reload_external`, read the log, and test the matching and nonmatching gun/seat/ammo combinations.
5. Keep the verified maintained file under version control. Future scans are disposable discovery output, not the source of your hand-tuned rules.

Move rather than blindly copying active duplicates. Tied rule IDs and priorities can change behavior after a rename or rescan; see [Shell Ejection](Shell-Ejection).

## [6] Interpreting output and failures

Gun/vehicle/file/rule counts describe discovery and emitted candidates. The temporary-instance count is not a count of entities intentionally added to the world. Loaded-vehicle counts describe inspected owners, not a proof of complete modpack coverage.

If writing fails, preserve the log and check server-directory write access and storage. Since old generated output is removed before writing, an interrupted scan can leave partial output; restore a backup rather than assuming the previous set survived. If scanning succeeds but a rule does nothing, check its rule-level enable flag and actual target/selected ammo before rescanning.

## Source

[Scanner implementation](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/command/ShellEjectionScanner.java) · [Generated-file replacement](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/shell/ExternalShellRuleLoader.java) · [Command side effects](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/command/ShellEjectionCommands.java)
