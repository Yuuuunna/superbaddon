# Recommended Pack-Author Workflow

[简体中文](推荐工作流) | [Quick Start](Quick-Start)

Treat configuration as a small, reviewable release. A file parsing successfully is only the first check; the actual weapon, recipe, resource and multiplayer behavior still need acceptance tests.

---

## [1] Freeze a reproducible baseline

Record Minecraft, Forge, Java, SuperbAddon, SuperbWarfare, GeckoLib and optional compatibility-mod versions. Preserve the mod list, datapacks and current `config/superbaddon/` alongside a world backup. Use a separate client/server test instance rather than editing a live shared world first.

The build and runtime version strings in this source are not fully aligned; see [Changelog](Changelog). Record the actual JAR/commit rather than identifying a test only as “the newest version.”

## [2] Write an observable requirement

Examples of a useful requirement are “this vehicle's assembly uses this material list,” “this ammo creates one casing stack at this port,” or “the listed weapon caps direct knockback while an unlisted weapon remains unchanged.”

Avoid combining content removal, ammo replacement, penetration and fuel changes in one initial test. Assign each requirement to its owner in [Configuration Overview](Configuration-Overview). Keep player control preferences separate from server gameplay settings.

## [3] Discover and promote only what is needed

Use the non-active [scan](Shell-Ejection-Scan) for IDs and weapon/ammo structure. Before scanning, move maintained generated rules into `custom/` and back up outside the scanned tree. Keep the default scan's rewriting side effects in mind.

Promote one reviewed candidate at a time. Choose explicit priorities and narrow target sets. Prefer separate files or rules for separate intentions; filenames help organization, while numeric priority and feature-specific selection determine behavior.

## [4] Validate syntax, then apply the correct reload

Parse edited JSON with a strict JSON-aware editor or validator. Check target IDs, units, required wrapper format and the placement of booleans. Never copy numbered documentation annotations into JSON.

Use the reload matrix in [Configuration Overview](Configuration-Overview). External rule reload is not recipe reload, and neither command distributes client-local files. For ammunition rollback or cached resource/MTS settings, restart and verify the intended baseline when a narrower reload cannot restore it.

## [5] Use a compact acceptance matrix

| Change | Positive test | Negative / edge test |
| --- | --- | --- |
| Ejection | Correct gun, seat and selected ammo | Unlisted gun; different ammo; full storage; first port item valid |
| Ammo override | Load, consume, fire, unload selected ammo | Removed ammo; fresh weapon; multiplayer client selection |
| Knockback | Direct hit and explosion tested separately | Unlisted weapon; target resistance; `disabled` without positive `min` |
| Penetration | Known armor setup and several explosion distances | Zero/native fallback; creative/team restrictions; unsupported damage path |
| Vehicle recipe | UI and actual ingredient cost | Group conflict; individual override; missing recipe; competing restriction |
| Resource trimming | Intended target access filtered | Shared models, container/table infrastructure, save/reload, existing blocks |
| MTS | Valid fuel and receivable capacity | Unlisted fluid, nearly full tank, neighboring vehicles, reconnect after restart |
| Casing crafting | Correct counted consumption | Split casing stacks, insufficient count, shift-craft, custom automation |

These are recommended tests, not a statement that this documentation update executed them. Keep expected and observed results separate in your change record.

## [6] Verify a real dedicated-server pair

Install the intended client and server builds, not merely two local single-player worlds. Deploy relevant local content/resource settings consistently. Test joining, firing, reload selection, recipe use and reconnecting after restart. Check both client and server logs when only one side shows the error.

Do not assume a working server calculation means its client UI or resources match. Optional MTS and camera behavior have client-local concerns; automated crafting and damage-event integrations need their own paths tested.

## [7] Publish and preserve rollback

Version the maintained configuration, list required dependency changes and record which files are client-local versus server-owned. Keep generated scan output separate from approved rules. Include a rollback instruction that restores both configuration and persistent world state when necessary.

For Wiki maintenance, edit `docs/wiki/` and the bilingual counterpart together. Preserve the historical archive and existing page addresses. Run `python3 docs/validate_wiki.py`; the synchronization workflow also validates before non-destructive copying. Check the actual Actions run before claiming that a repository edit is live in GitHub Wiki.

## Source

[Reload responsibilities](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/command/ShellEjectionCommands.java) · [Definition mutations](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/compat/SuperbWarfareDataPatcher.java) · [Recipe consumption boundary](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/mixin/ResultSlotMixin.java)
