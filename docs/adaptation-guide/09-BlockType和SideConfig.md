# 9. BlockType、Side Config 和默认物品配置

`ModBlockTypes.createMachineBlockType(machine)` 负责 Mek block type 属性。新增机器时检查：

- 是否有 GUI：`.withGui(() -> ModMenuTypes.getMachineContainer(machine))`
- 是否有能量：`.withEnergyConfig(machine.energyUsage(), machine.energyStorage())`
- 是否支持升级：`.withUpgradeSupport(upgradeSupportFor(machine))`，Mek 原版 factory 走 `Upgrade.SPEED + Upgrade.ENERGY`，EvolvedMekanism factory 走 `DEFAULT_MACHINE_UPGRADES`
- 是否需要 `AttributeFactoryType`
- 是否需要 `AttributeTier`
- 是否需要 custom shape
- 是否需要特殊 bounding attribute（如 `ISOTOPIC_CENTRIFUGE` 有 `AttributeHasBounding.ABOVE_ONLY`）
- 是否需要升级目标 `MeUpgradeableAttribute`
- `sideConfigFor(machine)` 是否包含正确的 `TransmissionType`

`sideConfigFor(machine)` 的实际映射：

- PRC：`ITEM, CHEMICAL, FLUID, ENERGY`
- Chemical Infuser / Isotopic Centrifuge / Pigment Mixer：`CHEMICAL, ITEM, ENERGY`
- Chemical Washer / Rotary Condensentrator：`CHEMICAL, FLUID, ITEM, ENERGY`
- Electrolytic Separator：`FLUID, CHEMICAL, ITEM, ENERGY`
- Nutritional Liquifier：`ITEM, FLUID, ENERGY`
- Solar Neutron Activator：`CHEMICAL, ITEM`
- 有 chemical 输入的机器：`ITEM, ENERGY, CHEMICAL`
- 其它机器：`ITEM, ENERGY`

新增机器时要确认 `sideConfigFor` 的 `TransmissionType` 数组与机器实际能力匹配，否则玩家放下方块后的初始 side config 会和机器能力不对应。
