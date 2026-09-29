# Installation

[简体中文](安装) | [Home](Home)

Install this addon as **SuperbAddon**, whose mod ID and configuration directory are `superbaddon`. Historical references to `SuperbAH6` and `config/superbah6/` do not describe this source version.

---

<a name="requirements"></a>
## [1] Requirements

| Component | Current source requirement / baseline |
| --- | --- |
| Minecraft | Build target **1.20.1**; metadata range `[1.20.1,1.21)` |
| Loader | **Forge 47.4.0 or later** in the metadata; build uses 47.4.0 |
| Java | **17** |
| SuperbWarfare | Required on both sides; metadata minimum **0.8.9.1** |
| GeckoLib | Required on both sides; metadata minimum **4.4.6** |
| MTS / Immersive Vehicles | Optional; needed only for its integration features and associated packs |

> [!IMPORTANT]
> The metadata minimum for SuperbWarfare is 0.8.9.1, not the older 0.8.9 wording in the README. A broad version range is not proof that later releases preserve the internal methods used by the addon. Use matching client/server versions and test the actual combination. Fabric is not the supported loader for this build.

## [2] Client installation

1. Create a Minecraft 1.20.1 instance using Forge 47.4.0 or a deliberately tested compatible Forge version and Java 17.
2. Put SuperbWarfare, GeckoLib and SuperbAddon JARs into that instance's `mods/` directory. Remove duplicate or obsolete JARs for the same mod ID.
3. Add MTS and the packs required by your chosen MTS content only when using that integration.
4. Launch the game, enter a disposable world, and allow the relevant configuration systems to initialize.
5. Edit files under that instance's `config/superbaddon/`, following [Quick Start](Quick-Start).

## [3] Dedicated server installation

Install the same gameplay mods and required libraries in the server's `mods/` directory. Start the server using Java 17. Server-side rules live under the **server working directory**, not in a player's local Minecraft instance.

Server configuration controls server gameplay. Resource filtering and client controls also use local client configuration; the addon does not provide a general JSON-file synchronization protocol. Distribute matching content/resource settings with the client pack and keep player-specific MTS control preferences separate. See [Compatibility](Compatibility).

Use the server console or an operator with permission level 2 for the [addon commands](Command-Reference). In the server console, omit the leading `/`.

## [4] Verify installation

Check that the mod list contains `superbaddon`, that dependency validation succeeds, and that a test world loads. Check `logs/latest.log` and, when produced, `logs/debug.log` for startup and configuration errors. Fire a supported weapon and inspect its expected casing behavior; then run `/superbaddon armor_penetration status` as an operator to verify command registration.

A missing optional MTS installation should not require removing the addon. A missing required library is an installation error, not a shell-rule error.

## [5] Updating and uninstalling

Back up the world and configuration before changing versions. Keep edited rules outside `generated_scan/`. Review [Data Format Versions](Data-Format-Versions) and [Known Issues](Known-Issues); do not delete a customized configuration simply to regenerate defaults.

Do not remove an addon containing registered items from a valuable save without a tested migration and backup. Content control is not an uninstallation or registry-migration tool.

## Source

[Runtime dependency declarations](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/resources/META-INF/mods.toml) · [Build baseline](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/gradle.properties)
