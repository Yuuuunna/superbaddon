**SuperbWarfre Addon** is an addon and compatibility layer for **SuperbWarfare** on Minecraft Forge 1.20.1. It focuses on practical gameplay integration: visible spent casings, configurable shell ejection rules, MTS fuel/control compatibility, and content control for SuperbWarfare(Unload unused assets to free up memory).

Install it together with SuperbWarfare and its required libraries.

### Features

*   Adds spent casing items for firearms, shotguns, heavy weapons, autocannons, and large shell weapons.
*   Provides data-driven shell ejection rules for SuperbWarfare guns and vehicle weapons.
*   Supports ammo-specific casing behavior, useful for weapons with multiple ammo types.
*   Includes a `/superbaddon shell_ejection scan` workflow to generate candidate rule files for installed content.
*   Adds optional content control for recipes, blocked resources, vehicle recipe overrides, and ore generation removal.
*   Adds optional MTS compatibility:   - MTS jerrycans can charge SuperbWarfare vehicles through Forge Energy.   - MTS fuel pumps can refuel nearby SuperbWarfare vehicles.   - MTS aircraft can opt into a SuperbWarfare-style control bridge.   - Optional MTS camera tilt support lets the camera follow vehicle roll and pitch.
*   Works without MTS installed; MTS support is soft-integrated and only activates when MTS is present.
*   Includes English and Simplified Chinese localization.

### Requirements

*   Minecraft: **1.20.1**
*   Mod loader: **Forge 47.4.0+**
*   Java: **17**
*   Required mods:   - **SuperbWarfare 0.8.9+**   - **GeckoLib 4.4.6+**
*   Optional:   - **Minecraft Transport Simulator / MTS-compatible packs**, only if you want the MTS fuel, ammo, camera, or control bridge features.

### Configuration

The addon is built around configuration and datapack-style rules instead of hardcoded one-off behavior.

*   Built-in datapack shell ejection rules live under `data/superbaddon/shell_ejection/`.
*   External server config rules live under `config/superbaddon/shell_ejection/`.
*   Generated scan output is intended to be edited before enabling.
*   Content control supports modes such as keeping content, removing recipes only, or blocking configured resources where possible.
*   Vehicle recipe overrides can disable or rewrite SuperbWarfare vehicle assembly recipes.
*   MTS compatibility has its own config for fuel conversion, jerrycan behavior, pump behavior, and control bridge behavior.

### How To Use

1.  Install **SuperbWarfare**, **GeckoLib**, and this addon on both client and server.
2.  Start the game or server once so the default config files are created.
3.  Use the mod normally; supported SuperbWarfare weapons and vehicle weapons will drop the configured spent casing items when they fire.
4.  For MTS fuel support, install MTS and use an MTS jerrycan or fuel pump near a SuperbWarfare vehicle that can receive Forge Energy.
5.  For MTS aircraft control support, open Minecraft's keybind menu and bind:    - `Toggle MTS Control Bridge`

### Commands

These commands are intended for pack authors, server operators, and testing worlds.

*   `/superbaddon shell_ejection scan`   - Generates disabled candidate shell ejection rules for installed SuperbWarfare content.   - Use this when you want templates without immediately changing gameplay.
*   `/superbaddon shell_ejection scan active`   - Generates candidate rules with `enabled` set to true.   - Use this only after you are ready to test the generated behavior.
*   `/superbaddon shell_ejection reload_external`   - Reloads external shell ejection rules from `config/superbaddon/shell_ejection/`.
*   `/superbaddon content_control reload`   - Reloads content control config from `config/superbaddon/content_control.json`.

### Shell Ejection Workflow

1.  Run `/superbaddon shell_ejection scan`.
2.  Open the generated files in `config/superbaddon/shell_ejection/generated_scan/`.
3.  Check each generated target. Do not blindly enable everything; scanners create candidates, not perfect design.
4.  Fill or adjust `ammo_override.allowed` when a weapon should only accept specific ammo.
5.  Fill or adjust `ejection.by_ammo` to decide which casing item each ammo type should drop.
6.  Set `enabled` to true for the rules you want.
7.  Run `/superbaddon shell_ejection reload_external` or restart the server.

### Content Control Workflow

Use `config/superbaddon/content_control.json` when you need to control availability of SuperbWarfare content in a modpack.

*   `keep`: keep the content and its recipes.
*   `recipes_only`: remove recipes but do not try to block runtime content.
*   `block_load`: remove recipes, block configured resources, and cancel supported runtime behavior where possible.

Use \`config/superbaddon/vehicle\_recipe\_override.js

on\` when you only need to disable or rewrite vehicle assembly recipes. This is the cleaner option when the item or vehicle should still exist but its progression cost must change.

### MTS Compatibility Usage

*   Jerrycan support consumes MTS jerrycan fuel data and converts it into Forge Energy for SuperbWarfare vehicles.
*   Fuel pump support drains the MTS pump and charges the nearest valid SuperbWarfare vehicle.
*   If a fuel does nothing, check `config/superbaddon/mts_compat.json`; the fuel may need an energy conversion entry.
*   The control bridge is off by default. That is intentional: existing MTS controls should not be changed unless the player opts in.

### Notes For Modpacks

This addon is designed for pack authors who want tighter control over SuperbWarfare progression and compatibility. You can remove unwanted recipes, adjust vehicle assembly inputs, suppress selected ore generation, and add shell ejection behavior for custom or extended weapon sets.

Existing Forge registries cannot be unregistered after SuperbWarfare loads them, so content control is intentionally practical: it removes recipes, blocks configured resources, and cancels supported runtime behavior where possible.
