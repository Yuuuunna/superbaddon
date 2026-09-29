# Changelog and Version Notes

[简体中文](更新日志) | [Home](Home)

This page distinguishes documentation changes from gameplay releases. Source review uses commit `ab9693f` dated 2026-09-29. A capability appearing in that snapshot does not establish the release date on which it was first introduced.

---

## 2026-09-29 — Wiki completion and source corrections

The documentation now covers the complete bilingual sidebar: installation, quick start, common configuration, all implemented configuration systems, commands, pack-author workflows, troubleshooting, compatibility, design and format migration.

Dedicated pages were added for ammunition overrides, knockback, armor penetration and casing reloading, including functionality not previously linked in the sidebar. Feature pages provide complete JSON examples, defaults, matching/merge rules, reload responsibilities and source references.

The original recovered nine-file Wiki tree is preserved byte-for-byte under `docs/wiki-archive/2026-09-29`, from tree `63636aa3d855f7b2fab90505a5dacbfd712293a7`. Historical addresses remain available, and `_Sidebar.md` is the proper active sidebar filename. The double-extension historical sidebar remains as an entry page.

Current pages correct the obsolete SuperbAH6 name/config path, the minimum SuperbWarfare metadata version, object-valued shell targets, inert scanner metadata, store-mode behavior and feature-specific reload rules. They explicitly document implementation gaps instead of describing desired behavior as already implemented.

A documentation validator checks bilingual coverage, internal page/anchor links, local source-reference paths and JSON examples before Wiki synchronization. Synchronization continues to update/add source-managed pages without deleting unrelated live Wiki pages. This is a documentation/tooling update, not a gameplay code change or mod version bump.

## Verified source capabilities at the baseline

| Area | Present in the reviewed source |
| --- | --- |
| Casings | Five registered casing items, format-5 ejection rules, drop/store/none modes and counted crafting recipes |
| Weapon rules | Allowed-ammo replacement/append, direct/explosion knockback and armor penetration with explosion falloff |
| Pack controls | Item/vehicle/block resource and selected runtime restrictions, supported ore-feature removal and vehicle assembly overrides |
| Administration | External reload, disabled/active scans, content reload and penetration reload/status/autofill |
| Optional integrations | MTS fuel/ammunition/controls/camera, ECA health facade and intended post-script recipe rewriting |

See [Known Issues](Known-Issues) for the actual limitations of these capabilities. This table does not claim that all combinations were run in a game or dedicated server during documentation work.

## Version-string discrepancy

At the reviewed commit, `gradle.properties` contains `mod_version=1.0.0`, while `src/main/resources/META-INF/mods.toml` contains a literal `version="1.0.1"`. The older README also describes a broader/older SuperbWarfare minimum than the runtime metadata's 0.8.9.1.

Do not use these discrepancies to infer two independently verified release milestones. For issue reports, identify the actual JAR name/hash and source commit when available. Dependency compatibility should follow the reviewed declarations and an actual runtime test, not a guessed changelog.

## Maintaining future entries

Add a release entry only when it can be tied to an actual tag, release or commit. State whether the change affects code, defaults, data format or documentation. Record migration steps and reload/restart requirements where behavior changed, and update both language pages together.

Keep historical notes separate from the current reference: copying an old example into a current version is not a migration. See [Data Format Versions](Data-Format-Versions).

## Source

[Source baseline](https://github.com/Yuuuunna/superbaddon/tree/ab9693fc133c3b12f61703579a4102b0308554bb) · [Build version properties](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/gradle.properties) · [Runtime version/dependencies](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/resources/META-INF/mods.toml) · [Recovered documentation archive](https://github.com/Yuuuunna/superbaddon/tree/master/docs/wiki-archive/2026-09-29)
