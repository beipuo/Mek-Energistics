# 24. 按机器类型的落地 playbook

## 单 item 输入机器

代表：Enrichment Chamber、Crusher、Energized Smelter。

优先复用：

- 普通机器：`MeElectricMachineBlockEntity`
- 工厂：`MeItemStackToItemStackFactoryBlockEntity`

检查点：

- `MeMekanismMachine.slotLayout()` 应返回 `SINGLE_ITEM`。
- `hasRecipeLogic()` 应为 true。
- `pushPattern` 只接受 `inputHolder.length == 1`。
- 输入必须是 `AEItemKey`，数量不能超过 `Integer.MAX_VALUE`。
- 输出只需要 item slot 回网。
- JEI catalyst 可通过 `FactoryType` 统一注册。

## item + chemical 输入机器

代表：Osmium Compressor、Purification Chamber、Chemical Injection Chamber、Metallurgic Infuser。

优先复用：

- 普通 advanced electric 机器：`MeAdvancedElectricMachineBlockEntity`
- Metallurgic Infuser：`MeMetallurgicInfuserBlockEntity`
- 工厂：`MeItemStackChemicalToItemStackFactoryBlockEntity`

检查点：

- `pushPattern` 接受两个输入，一个 item，一个 chemical。
- 不能依赖输入顺序，除非机器槽位语义真的有顺序。
- item slot 和 chemical tank 都 simulate 成功后才能 execute。
- `chemicalTank.insert(...).getAmount() == 0` 才代表完整插入。
- Purifying / Injecting 注意 `useStatisticalMechanics()`，不要把随机消耗逻辑绕开。

## 双 item 输入机器

代表：Combiner、CNC Stamper。

优先参考：

- 普通机器：`MeCombinerBlockEntity`
- 工厂：`MeCombiningFactoryBlockEntity`
- MekMM stamping factory：`MeStampingFactoryBlockEntity`

检查点：

- `hasSecondaryItemInput()` 要返回 true。
- 主输入槽和副输入槽语义不同，不能随便交换两个输入。
- 先 simulate 主输入跨槽/主槽，再 simulate 副输入槽。
- execute 顺序应和 simulate 顺序一致。
- side config 通常是 `EXTRA_MACHINE`。

## item 输入 + 副产物输出机器

代表：Precision Sawmill。

优先参考：

- 普通机器：`MePrecisionSawmillBlockEntity`
- 工厂：`MeSawingFactoryBlockEntity`

检查点：

- `hasSecondaryOutput()` 要返回 true。
- 输出回网必须同时传主输出和 secondary output。
- secondary output 可能为空，也可能因概率不产生，不能把空输出当失败。
- JEI 和 recipe viewer 要使用 sawmill 对应 recipe type。

## item + fluid + chemical 输入机器

代表：Pressurized Reaction Chamber。

优先参考：`MePressurizedReactionChamberBlockEntity`。

检查点：

- `pushPattern` 接受三个输入：item、fluid、chemical。
- 三种输入都必须存在，且每种只能出现一次。
- item 用 `InputInventorySlot.insertItem(..., Action.SIMULATE, AutomationType.INTERNAL)`。
- chemical 用 `inputGasTank.insert(..., Action.SIMULATE, AutomationType.INTERNAL)`。
- fluid 用 `inputFluidTank.fill(..., FluidAction.SIMULATE)`，返回量必须等于输入量。
- 输出通常同时包含 item output slot 和 chemical output tank。
- `ModBlockTypes.sideConfigFor` 必须包含 `ITEM`、`CHEMICAL`、`FLUID`、`ENERGY`。
- 默认 side config 用 `AttachedSideConfig.REACTION`。

## chemical + chemical 输入机器

代表：Chemical Infuser、Pigment Mixer。

优先参考：

- `MeChemicalInfuserBlockEntity`
- `MePigmentMixerBlockEntity`

检查点：

- 输入都是 chemical，通常左右 tank 可互换。
- 先尝试 `(first, second)`，失败再尝试 `(second, first)`。
- `MeChemicalInputHelper.insertPair(...)` 会检查容量、同类 chemical 和 tank validity，并在不合法时回滚。
- 输出是 chemical tank，必须通过 `insertChemicalTankIntoNetwork` 回网。

## fluid 和 chemical 可切换机器

代表：Rotary Condensentrator。

优先参考：`MeRotaryCondensentratorBlockEntity`。

检查点：

- `getMode()` 决定当前方向。
- 当前实现中 `getMode() == true` 时接受 fluid 输入并输出 chemical；`getMode() == false` 时接受 chemical 输入并输出 fluid。
- `pushPattern` 必须按当前模式验证输入类型。
- 输出回网也必须按当前模式选择 tank。
- GUI/样板使用时要提醒玩家：样板是否可执行取决于机器当前模式。

## 纯 chemical / fluid 输出机器

代表：Chemical Oxidizer、Chemical Washer、Electrolytic Separator、Solar Neutron Activator。

优先参考同目录 `blockentity/machine/chemical` 下现有类。

检查点：

- 输出 tank 数量可能不止一个，例如 Electrolytic Separator 有 left/right tank。
- 多输出 tank 都要传给 support，否则网络满后其中一边不会被 AE ticker 继续处理。
- fluid 输出目前要特别检查 `AeOutputMode` 语义，避免玩家打开 chemical 输出却无法回 fluid。
- 有 daylight、biome、water source 或环境依赖的机器，不要只按 recipe 判断可执行。

## item 输入 + fluid 输出机器

代表：Nutritional Liquifier。

优先参考：`MeNutritionalLiquifierBlockEntity`。

检查点：

- `pushPattern` 接受一个 item 输入，输出是 fluid tank。
- 输出 fluid 必须通过 `insertFluidTankIntoNetwork` 回网。
- `sideConfigFor` 应包含 `ITEM, FLUID, ENERGY`。
- `AeOutputMode` 的 `items()` 控制 fluid 输出（`MeRecipeMachineAeSupport` 中 fluid 输出受 `mode.items()` 控制）。

## chemical -> chemical 带模式切换机器

代表：Antiprotonic Nucleosynthesizer。

优先参考：`MeAntiprotonicNucleosynthesizerBlockEntity`。

检查点：

- 输入输出都是 chemical，但机器有模式切换。
- `pushPattern` 必须按当前模式验证输入/输出类型。
- 输出回网也必须按当前模式选择 tank。
- `sideConfigFor` 应包含 `CHEMICAL, ITEM, ENERGY`。

## 无 AE 样板的工具机器

代表：Digital Miner、Teleporter、Oredictionificator、Modification Station、Logistical Sorter。

优先复用：`MeMekanismMachineBlockEntity` 或对应 utility BlockEntity。

检查点：

- `ModBlockEntities` 走 `noAe(...)`。
- 不注册 `IN_WORLD_GRID_NODE_HOST`。
- 菜单可直接复用 Mek 原 GUI/container。
- 仍要确认方块、物品、side config、安装器、模型和语言正常。
- 不要为了"统一"强行注册 crafting provider。没有明确 pattern 输入输出语义的机器，接入 AE 下单只会制造假自动化。
