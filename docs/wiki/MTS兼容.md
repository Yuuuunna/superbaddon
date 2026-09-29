# MTS 兼容

[English](MTS-Compatibility) | [兼容性](兼容性)

MTS／Immersive Vehicles 是可选依赖。本附属通过 `mts`、`immersivevehicles`、`minecrafttransportsimulator` 这些模组 ID 判断受保护的联动入口是否可用。需要的实际载具或弹药内容包仍须另行安装，本附属不会提供这些内容包。

---

## `mts_compat.json` 完整默认值

文件：`config/superbaddon/mts_compat.json`。

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
燃料转换               <a href="#fuel">[1]</a>
jerrycan               <a href="#jerrycan">[2]</a>
fuel_pump              <a href="#pump">[3]</a>
操控与视角             <a href="#controls">[4]</a>
MTS 弹药               <a href="#ammunition">[5]</a>
重载与排查             <a href="#testing">[6]</a>
</pre>

<a name="fuel"></a>
## [1] 燃料名与转换

数值为每桶转换的载具能量，**一桶等于 1000 mB**。默认 `default_energy_per_bucket: 0` 拒绝未列出的液体；即使默认回退值为正数，显式列为零的液体仍被禁用。将回退值设为正数可能让其他所有未列出液体也成为燃料，应谨慎使用。

名称会去掉首尾空白、转小写并移除命名空间前缀。例如 `somepack:diesel` 使用 `diesel` 键；不同命名空间中路径相同的液体，不能用本表区分。

提供 `fluid_energy_per_bucket` 时会**整体替换默认表**，不是自动补齐未列出的默认项。因此要保留的燃料必须全部写进编辑后的表。按 mB 转换能量时向下取整；油泵根据实际抽取量结算，不会按没有抽到的液体赠送能量。

根级 enabled 控制燃料交互，不是输入桥接、倾斜视角或弹药盒适配的全局开关。

<a name="jerrycan"></a>
## [2] 油桶

使用主手拿着符合条件的已装液体油桶，右键卓越前线载具。桥接检查根 NBT `jerrycanFluid` 和燃料转换配置，还要求 MTS 可用、载具具有能接收能量的能力。

`allow_nbt_only_detection` 默认 true，允许只依据 NBT 识别。`allowed_items` 增加显式允许的物品 ID，但不是排他的严格白名单，因为 MTS 命名空间和类来源识别也可通过。关闭 NBT-only 只是缩小识别范围，不会关闭另外这些受支持路径。

> [!WARNING]
> 油桶表示不可拆分的一整桶。只要载具能够接收任何能量，整桶就会被清空，放不下的多余能量被丢弃。因此给接近满能量的载具补能可能浪费燃料。完全满载或不能接收能量时，这条路径不会消耗油桶；未配置燃料也不会被桥接消耗，而会保留原交互机会。

<a name="pump"></a>
## [3] 油泵

**主手空手**右键可识别的 MTS 油泵，可连接搜索范围内检测到的最近卓越前线载具。手持物品时保留 MTS 原有物品交互。

| 字段 | 默认值 | 实际行为 |
| --- | --- | --- |
| `enabled` | true | 根级 enabled 也为 true 时启用油泵桥接 |
| `mb_per_tick` | 10 | 每服务端 tick 尝试抽取的最大量，最小为 1 |
| `search_radius` | 32 | 有效读取值至少为 **32**，保存更小数值也不会获得更小半径 |

选择比较的是到载具包围盒的距离，不是简单比较模型中心。选中载具仍需具有可接收能量的存储；附近其他已满或不合适载具，可能让连接目标不符合直觉。排查时把测试载具分开放置。

油泵或载具无效、燃料耗尽或被拒绝、载具离开过远、没有能量可以转移、购买流程完成时，连接会被移除。连接保存在内存中，重启后应重新连接。剩余容量不足以接收一个完整 mB 对应能量时，也可能无法继续补能。

<a name="controls"></a>
## [4] 输入桥接与倾斜视角

有**两个独立的客户端按键，默认都没有绑定**。请先在 Minecraft 控制设置的 SuperbAddon 分类中分配按键。

输入开关会持久化到 `controls.bridge_controls`。false 保留 MTS 原生输入；启用后，在控制座位通过 MTS 数据包驱动 MTS 控制变量，不替换 MTS 物理。

| 载具类型 | 默认按键对应行为，实际读取玩家可重绑定的 Minecraft 操作 |
| --- | --- |
| 地面载具 | W 油门，S 刹车，A/D 转向，攻击／鼠标左键开火 |
| 航空器 | W/S 增减油门，A/D 滚转，鼠标纵向俯仰、横向偏航／方向舵，攻击／左键开火 |

视角切换仍使用 Minecraft 普通视角切换操作，默认 F5。航空器桥接中鼠标是操纵杆输入，不是普通自由观察。桥接会临时启用 MTS mouse-yoke 视角锁定，并在退出时还原原值，不把这一临时 MTS 设置写入磁盘。

倾斜视角开关独立于驾驶输入，乘客也可按需要开启，默认关闭且**不跨会话保存**。本 JSON 没有 camera_tilt 字段；与其他视角模组叠加时需要单独验证。

<a name="ammunition"></a>
## [5] MTS 弹药盒

弹药适配处理通过 `bulletQty` NBT 保存数量的 MTS 子弹物品，缺少该 NBT 时回退到内容包定义数量。它按发数统计和消耗，不把一个物品简单当成一发，也支持 `mts:pack.bullet` 这类带点的注册名，前提是对应物品真实存在。

部分消耗或退回的弹药可能表现为一个带剩余发数 NBT 的物品，所以背包物品数量不一定等于发数。实际允许装填的弹药仍通过[弹药覆写](弹药覆写)配置，燃料能量参数不会启用弹药消费者。

该物品解析器面向真实的 MTS 子弹物品，不是任意 `@` 虚拟弹药。不要为了让弹药盒看起来满载而移除数量 NBT。应使用实际内容包和自定义库存实现，验证装填、部分消耗、切换弹药、退弹以及满背包行为。

<a name="testing"></a>
## [6] 重载与排查

当前没有专用 MTS 重载命令。文件数据有缓存，手动修改后应重启对应实例。输入按键切换会立即更新标志并尝试保存；倾斜视角则明确只在当前会话生效。

燃料失败时检查 MTS 是否存在、根开关与子功能开关、燃料映射、油桶识别、载具能量接收能力和附近目标选择。操控失败时先绑定默认未分配的按键、开启桥接并坐到控制座位。视角不记忆时先确认其会话级设计；弹药问题则检查真实物品 ID 和 bulletQty，而非只看堆叠数量。

实现使用反射以及主模组／MTS 内部接口。设计上的支持路径不等于保证兼容所有后续 MTS 版本和内容包。详见[已知问题](已知问题)。

## 源码依据

[燃料默认值与解析](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/compat/mts/MtsCompatConfig.java) · [燃料交互](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/compat/mts/MtsCompatEvents.java) · [油泵连接](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/compat/mts/MtsFuelPumpLinks.java) · [操控实现](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/compat/mts/control/MtsControlBridgeImpl.java) · [弹药数量](https://github.com/Yuuuunna/superbaddon/blob/ab9693fc133c3b12f61703579a4102b0308554bb/src/main/java/com/yy/superbaddon/compat/mts/MtsBulletAmmo.java)
