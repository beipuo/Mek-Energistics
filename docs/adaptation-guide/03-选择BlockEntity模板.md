# 3. 选择 BlockEntity 模板

BlockEntity 注册入口在 `ModBlockEntities.registerMachine(machine)`，核心分派在：

- `mekanismMachineRegistration(machine)`: 普通机器。
- `defaultMekanismMachineRegistration(machine)`: 普通机器默认模板。
- `factoryRegistration(machine)`: Mek 原版 factory type 对应模板。
- `MekanismMoreMachine*Compat` / `MekanismExtras*Compat`: 可选模组扩展机器。

普通机器有两种路线：

- 有 AE 样板能力：实现 `MeAeMachine`、`ICraftingProvider`、`IActionHost`，并注册 `AECapabilities.IN_WORLD_GRID_NODE_HOST`。
- 无 AE 样板能力：使用 `noAe(...)`，只注册 ME 方块、物品、Mek GUI 和基础功能。

工厂机器通常实现 `MeFactoryAeMachine`，通过 `MeFactoryAeSupport` 接入 AE。外部模组工厂（MekMM advanced factories、Mek Extras factories）走 `MeExternalFactorySupport`。普通机器多数通过 `MeRecipeMachineAeSupport` 接入 AE。

新增模板时建议先复制最接近的现有类：

- 单 item 机器：`MeElectricMachineBlockEntity`。
- item + chemical 普通机器：`MeMetallurgicInfuserBlockEntity` 或 `MeAdvancedElectricMachineBlockEntity`。
- chemical + chemical 机器：`MeChemicalInfuserBlockEntity`。
- 多输出/tank 机器：找同类 chemical 机器，例如 Washer、PRC、Electrolytic Separator。
- 工厂 item -> item：`MeItemStackToItemStackFactoryBlockEntity`。
- 工厂 item + chemical -> item：`MeItemStackChemicalToItemStackFactoryBlockEntity`。
