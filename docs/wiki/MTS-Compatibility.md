# MTS Compatibility

[简体中文](MTS兼容) | [Compatibility](Compatibility)

MTS / Immersive Vehicles is optional. The addon recognizes the mod IDs `mts`, `immersivevehicles` and `minecrafttransportsimulator` for its guarded integration. Install the actual MTS content packs required by the items/vehicles you intend to use; the addon does not provide those packs.

---

## Complete `mts_compat.json` defaults

File: `config/superbaddon/mts_compat.json`.

```json
{
  "enabled": true,
  "default_energy_per_bucket": 0,
  "jerrycan": {
    "enabled": true,
    "allow_nbt_only_detection": true,
    "allowed_items": []
  },
  "fuel_pump": {
    "enabled": true,
    "mb_per_tick": 10,
    "search_radius": 32
  },
  "controls": {
    "bridge_controls": false
  },
  "fluid_energy_per_bucket": {
    "lava": 5000000,
    "fuel": 7500000,
    "gasoline": 8750000,
    "diesel": 10000000,
    "biodiesel": 7500000
  }
}
```

<pre>
fuel conversion        <a href="#fuel">[1]</a>
jerrycan               <a href="#jerrycan">[2]</a>
fuel_pump              <a href="#pump">[3]</a>
controls and camera    <a href="#controls">[4]</a>
MTS ammunition         <a href="#ammunition">[5]</a>
reload and diagnosis   <a href="#testing">[6]</a>
</pre>

<a name="fuel"></a>
## [1] Fuel names and conversion

Values are vehicle energy per bucket, with **1000 mB per bucket**. `default_energy_per_bucket: 0` rejects unlisted fluids. A listed value of zero disables that fluid even when the default fallback is positive. A positive fallback can admit every otherwise-unlisted fluid, so change it deliberately.

Names are trimmed, lowercased and stripped of a namespace prefix. `somepack:diesel` therefore uses the `diesel` key; fluids with the same path from different namespaces cannot be distinguished by this map.

Supplying `fluid_energy_per_bucket` **replaces the default map**, rather than merging missing keys from it. Keep every desired fuel in your edited map. Energy for a measured mB quantity is rounded down; the pump accounts for actual drained volume rather than awarding energy for fuel it did not receive.

Root `enabled` gates the fuel interactions. It is not a global switch for input bridging, camera tilt or the ammo-box helper.

<a name="jerrycan"></a>
## [2] Jerrycans

Use the main hand to right-click a SuperbWarfare vehicle with an eligible filled jerrycan. The bridge checks its root NBT `jerrycanFluid` and the configured fluid conversion. It also requires the MTS integration to be available and a vehicle energy capability that can receive energy.

`allow_nbt_only_detection` defaults to true, permitting NBT-based detection. `allowed_items` adds explicit item IDs; it is not a strict exclusive whitelist because MTS namespace/class detection can also qualify. Setting NBT-only detection false narrows detection but does not remove those other supported paths.

> [!WARNING]
> A jerrycan represents one indivisible bucket. If the vehicle can accept any energy, the whole can is emptied and excess energy that does not fit is discarded. A nearly full vehicle can therefore waste fuel. A full/nonreceiving vehicle does not consume the can through this path. An unconfigured fluid is not consumed by the bridge and the original interaction is left available.

<a name="pump"></a>
## [3] Fuel pumps

With an **empty main hand**, right-click a recognized MTS fuel pump to connect it to the nearest detected SuperbWarfare vehicle in its search region. Holding an item leaves MTS's normal item interactions alone.

| Field | Default | Actual behavior |
| --- | --- | --- |
| `enabled` | true | Enables pump bridging when root enabled is also true |
| `mb_per_tick` | 10 | Maximum attempted drain per server tick, clamped to at least 1 |
| `search_radius` | 32 | Effective accessor enforces at least **32**, even if a smaller value is saved |

Selection compares distance to vehicle bounding boxes, not simply model centers. The selected vehicle still needs receivable energy storage; another nearby/full/ineligible vehicle can make a connection appear unexpected. Move test vehicles apart when diagnosing selection.

A connection is removed when the pump/vehicle becomes invalid, fuel is empty or rejected, the vehicle moves too far away, no energy can be transferred, or the pump purchase is complete. Links are in-memory, so reconnect after restarting. Small remaining capacity can prevent transfer when it cannot pay for even one whole mB.

<a name="controls"></a>
## [4] Input bridge and camera tilt

There are **two separate client key mappings, both unbound by default**. Assign them in Minecraft's controls menu under the SuperbAddon category.

The input toggle persists `controls.bridge_controls`. False preserves native MTS input. When enabled for the controlling seat, it drives MTS control variables using MTS packets; it does not replace MTS physics.

| Vehicle | Default-key mapping (uses the player's remappable Minecraft actions) |
| --- | --- |
| Ground | W throttle; S brake; A/D steer; attack/left click fire |
| Aircraft | W/S increase/decrease throttle; A/D roll; mouse Y pitch; mouse X yaw/rudder; attack/left click fire |

Perspective switching remains Minecraft's normal perspective action (default F5). In aircraft bridge control, mouse motion is a stick input rather than ordinary free-look. The bridge temporarily forces MTS mouse-yoke view locking and restores the prior setting when leaving; it does not write that temporary MTS setting to disk.

The camera-tilt toggle is independent of driving input, can be useful to passengers, defaults off and is **not persisted** between sessions. There is no `camera_tilt` property in this JSON schema. Camera-mod interactions need separate testing.

<a name="ammunition"></a>
## [5] MTS ammunition boxes

The ammo adapter handles MTS bullet items whose quantities are stored in `bulletQty` NBT, falling back to the pack definition's quantity. It counts/consumes rounds rather than treating one item as one bullet, and supports dotted registration names such as `mts:pack.bullet` when the referenced item really exists.

Partially consumed or returned rounds can be represented by one tagged item carrying the remaining quantity. Item count in the inventory is therefore not necessarily round count. Configure actual allowed ammunition through [Ammunition Overrides](Ammunition-Overrides); fuel-energy settings do not enable an ammo consumer.

The item resolver is for real MTS bullet items, not arbitrary virtual `@` ammunition. Do not remove quantity NBT to make a box look full. Test loading, partial consumption, switching ammo, unloading and full-inventory handling with the actual MTS pack and any custom inventory implementation.

<a name="testing"></a>
## [6] Reload and troubleshooting

There is no dedicated MTS reload command. File data is cached; restart the affected instance after manual edits. Input toggling updates its live flag and attempts to persist it, while camera tilt is deliberately session-only.

For fuel failures, inspect MTS presence, root and subfeature enabled flags, fluid mapping, jerrycan detection, vehicle energy acceptance and nearby vehicle selection. For controls, first bind the unbound toggle, enable the bridge and occupy a controller seat. For camera persistence, remember the session-only design. For ammunition, inspect the actual registered bullet item and `bulletQty`, not only stack size.

The integration uses reflection and main-mod/internal MTS interfaces. Supported design paths are not a guarantee of compatibility with every future MTS version or pack. See [Known Issues](Known-Issues).

## Source

[Fuel defaults and parsing](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/compat/mts/MtsCompatConfig.java) · [Fuel interactions](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/compat/mts/MtsCompatEvents.java) · [Pump links](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/compat/mts/MtsFuelPumpLinks.java) · [Controls](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/compat/mts/control/MtsControlBridgeImpl.java) · [Ammo quantities](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/compat/mts/MtsBulletAmmo.java)
