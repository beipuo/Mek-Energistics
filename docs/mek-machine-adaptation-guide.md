# Mek-Energistics: Mekanism 机器适配 Guide

这份 guide 用来沉淀 Mek-Energistics 适配 Mekanism 机器的实际流程。项目的核心不是给原机器外挂一个独立适配器，而是注册一套 `mekenergistics:me_*` 机器：它们继承 Mekanism 原机器或工厂 BlockEntity，保留 Mek 的配方、GUI、侧面配置、升级与缓存逻辑，同时接入 AE2 样板供应、AE 网络能量和产物回网。

后续适配 Mekanism More Machine、Mekanism Extras 或其它机器模组时，也应沿用同一套拆分方式：先把机器元数据放进统一枚举，再选择/新增 BlockEntity 模板，最后补注册、菜单、客户端、JEI、安装器和资源。

## 1. 先判断机器类型

新增机器前，先给机器分型。分型决定复用哪个模板，也决定后面要补哪些槽位、tank、mixin accessor 和输出回网逻辑。

常见类型：

- `item -> item`: Enrichment Chamber、Crusher、Energized Smelter。普通机器多复用 `MeElectricMachineBlockEntity`，工厂复用 `MeItemStackToItemStackFactoryBlockEntity`。
- `item + chemical -> item`: Metallurgic Infuser、Osmium Compressor、Purification Chamber、Chemical Injection Chamber。普通机器根据槽位差异复用或新增类，工厂复用 `MeItemStackChemicalToItemStackFactoryBlockEntity`。
- `item + item -> item`: Combiner、CNC Stamper。通常需要双输入槽处理，参考 `MeCombinerBlockEntity` 和 `MeCombiningFactoryBlockEntity`。
- `item -> item + item`: Precision Sawmill。需要处理 secondary output，参考 `MePrecisionSawmillBlockEntity` 和 `MeSawingFactoryBlockEntity`。
- `chemical/fluid -> chemical/fluid`: Chemical Infuser、Washer、Oxidizer、Electrolytic Separator、Rotary Condensentrator。通常需要专用 BlockEntity，因为 tank 数量和输入顺序不同。
- 非配方或特殊机器：Digital Miner、Teleporter、Logistical Sorter 等一般只保留 Mek 功能和 GUI，不作为 AE crafting provider。

第一判断标准不是名字像不像，而是 `pushPattern` 需要接收几个 `KeyCounter`，每个输入是 item、chemical 还是 fluid，以及输出要从哪些 slot/tank 回 AE 网络。

## 2. 机器事实来源: `MeMekanismMachine`

所有 ME 机器先加入 `src/main/java/com/beipuo/mekenergistics/common/machine/MeMekanismMachine.java`。这个 enum 是注册、资源命名、升级链、JEI 隐藏、安装器目标解析的事实来源。

新增普通 Mek 机器时，优先使用：

```java
MACHINE_NAME((FactoryType) null, "mek_machine_name", "ME Display Name")
```

如果机器属于 Mek 原版工厂体系，使用已有 `FactoryType`：

```java
ENRICHMENT_CHAMBER(FactoryType.ENRICHING, "enrichment_chamber", "ME Enrichment Chamber")
BASIC_ENRICHING_FACTORY(FactoryTier.BASIC, FactoryType.ENRICHING)
```

如果机器来自可选模组，必须设置 `requiredModId` 相关构造路径，并让 `isAvailable()` 能挡住未安装模组时的注册。现有例子：

- `requiredModId = "mekmm"`: Mekanism More Machine。
- `requiredModId = "mekanism_extras"`: Mekanism Extras。
- `OptionalCompatClasses` 用类资源探测更细的可选功能，例如高级工厂或 Extras + More Machine 组合。

这里同时要检查这些方法是否需要更新：

- `hasSecondaryItemInput()`: 双 item 输入机器。
- `hasChemicalInput()`: 普通 chemical 输入机器。
- `hasAdvancedChemicalInput()`: 走高级 electric machine 模板的机器。
- `hasSecondaryOutput()`: 有副产物机器。
- `slotLayout()`: 决定默认菜单/模板选择。
- `hasRecipeLogic()`: 决定是否应作为 AE crafting provider。
- `energyUsage()` / `energyStorage()`: 普通机器应尽量引用 MekanismConfig，无法对应时才用默认值。
- `getBasicFactory()` / `getNextFactory()`: 工厂安装器和升级链依赖这里。

## 3. 选择 BlockEntity 模板

BlockEntity 注册入口在 `ModBlockEntities.registerMachine(machine)`，核心分派在：

- `mekanismMachineRegistration(machine)`: 普通机器。
- `defaultMekanismMachineRegistration(machine)`: 普通机器默认模板。
- `factoryRegistration(machine)`: Mek 原版 factory type 对应模板。
- `MekanismMoreMachine*Compat` / `MekanismExtras*Compat`: 可选模组扩展机器。

普通机器有两种路线：

- 有 AE 样板能力：实现 `MeAeMachine`、`ICraftingProvider`、`IActionHost`，并注册 `AECapabilities.IN_WORLD_GRID_NODE_HOST`。
- 无 AE 样板能力：使用 `noAe(...)`，只注册 ME 方块、物品、Mek GUI 和基础功能。

工厂机器通常实现 `MeFactoryAeMachine`，通过 `MeFactoryAeSupport` 接入 AE。普通机器多数通过 `MeRecipeMachineAeSupport` 接入 AE。

新增模板时建议先复制最接近的现有类：

- 单 item 机器：`MeElectricMachineBlockEntity`。
- item + chemical 普通机器：`MeMetallurgicInfuserBlockEntity` 或 `MeAdvancedElectricMachineBlockEntity`。
- chemical + chemical 机器：`MeChemicalInfuserBlockEntity`。
- 多输出/tank 机器：找同类 chemical 机器，例如 Washer、PRC、Electrolytic Separator。
- 工厂 item -> item：`MeItemStackToItemStackFactoryBlockEntity`。
- 工厂 item + chemical -> item：`MeItemStackChemicalToItemStackFactoryBlockEntity`。

## 4. 普通机器接入 AE 的必做项

普通配方机器应使用 `MeRecipeMachineAeSupport<T>`，并完成这些点：

1. 字段：

```java
private final MeRecipeMachineAeSupport<MyBlockEntity> aeSupport = new MeRecipeMachineAeSupport<>(this);
private AeOutputMode aeOutputMode = AeOutputMode.BOTH;
```

2. `getInitialInventory(...)` 中把 `aeSupport.getPatternSlots()` 合并到原 Mek inventory holder。

3. `getInitialEnergyContainers(...)` 中用 `MeRecipeMachineAeSupport.AeBackedEnergyContainer` 替换原 energy container，并通过 accessor 写回 Mek 私有字段。

4. `createNewCachedRecipe(...)` 中调用：

```java
return this.aeSupport.wrapRecipeEnergy(getEnergyContainer(), super.createNewCachedRecipe(recipe, cacheIndex));
```

这样配方缓存看到的是本地 FE + AE 网络能量，不会只看机器本地缓存。

5. 生命周期必须创建和销毁 grid node：

```java
clearRemoved -> GridHelper.onFirstTick(... aeSupport.create(...))
setRemoved -> aeSupport.destroy()
onChunkUnloaded -> aeSupport.destroy()
```

6. `saveAdditional/loadAdditional` 必须保存：

- `AeOutputMode`
- `aeSupport.save/load`
- `aeSupport.saveSlots/loadSlots`

7. `addContainerTrackers` 必须同步 `AeOutputMode`，否则 GUI 上的 AE 输出开关客户端状态会漂。

8. `getGridNode` 和 `getActionableNode` 返回 `aeSupport.getMainNode().getNode()`。

9. `setOwner` 用 `MeOwnerHelper.setOwner(this, getMainNode(), player)`，保持 Mek 安全系统和 AE 节点 owner 一致。

## 5. 工厂机器接入 AE 的必做项

工厂机器走 `MeFactoryAeMachine` + `MeFactoryAeSupport`。

模板结构：

```java
public class MyFactoryBlockEntity extends SomeMekFactory implements MeFactoryAeMachine {
    private final MeMekanismMachine machine;
    private final MeFactoryAeSupport aeSupport;
}
```

必须实现或委托：

- `getAeSupport()`
- `getMachine()`
- `getOwnerLevel()`
- `setOwner(ServerPlayer player)`
- `pushPattern(...)`
- `createNewCachedRecipe(...)`
- `clearRemoved/setRemoved/onChunkUnloaded`
- `addContainerTrackers`
- `saveAdditional/loadAdditional`

能量容器使用：

```java
this.energyContainer = new MeFactoryAeSupport.AeBackedFactoryEnergyContainer(this, listener)
```

配方缓存使用：

```java
return MeFactoryAeSupport.withAeRecipeEnergy(this.energyContainer, super.createNewCachedRecipe(recipe, cacheIndex));
```

工厂输出回网使用：

```java
this.aeSupport.insertOutputSlotsIntoNetwork(this.outputSlots)
this.aeSupport.insertChemicalTankIntoNetwork(tank)
this.aeSupport.insertFluidTankIntoNetwork(tank)
```

`MeFactoryAeSupport` 会记住输出 slot/tank，并在 AE ticker 中继续尝试输出回网。所以新增机器要确保所有可能输出位置都至少被传给 support 一次。

## 6. `pushPattern` 编写规则

`pushPattern` 是 AE 下单把原料推入机器的核心。新增机器时最容易出资源复制或吞资源的问题，必须按下面顺序写：

1. 检查 AE 节点 active。
2. 检查 `patternDetails` 属于 `getAvailablePatterns()`。
3. 检查 `inputHolder` 数量和机器输入数一致。
4. 用 `MeFactoryPatternInput.single(counter)` 解析输入。
5. 严格区分 item、chemical、fluid。
6. 所有输入都 `Action.SIMULATE` 成功后再 `Action.EXECUTE`。
7. 任何一个输入不能完整插入时直接返回 `false`。
8. execute 后 `setChanged()`。

已有工具：

- `MeFactoryPatternInput.single(...)`: 把 AE `KeyCounter` 转成单一 item/chemical/fluid 输入。一个 counter 混入多个 key 会返回 `null`。
- `MeFactoryInventoryInsert.canInsertAcrossSlots(...)`: 跨 factory 输入槽模拟插入。
- `MeFactoryInventoryInsert.insertAcrossSlots(...)`: 先 simulate，再 execute。
- `MeChemicalInputHelper.insertPair(...)`: 双 chemical tank 按左右顺序插入，并在不合法时回滚。

双输入机器要注意输入顺序。Chemical Infuser 这类左右 tank 可互换的机器，应尝试 `(first, second)` 和 `(second, first)`。Combiner 这类主槽/副槽语义不同的机器不能随便交换。

## 7. 输出回网规则

输出回网由 `AeOutputMode` 控制。新增机器时必须确认输出资源类型映射正确：

- item 输出：`insertOutputSlotIntoNetwork(...)` 或 `insertOutputSlotsIntoNetwork(...)`。
- chemical 输出：`insertChemicalTankIntoNetwork(...)`。
- fluid 输出：`insertFluidTankIntoNetwork(...)`。

普通机器 `MeRecipeMachineAeSupport` 中 fluid 输出当前受 `mode.items()` 控制；工厂 `MeFactoryAeSupport` 中 fluid 输出当前受 `aeOutputMode.chemicals()` 控制。新增机器前要确认这是不是目标语义，尤其是 GUI 上的 “items/chemicals/both” 开关是否符合玩家预期。

`onUpdateServer()` 推荐模式：

```java
boolean sendUpdatePacket = super.onUpdateServer();
sendUpdatePacket |= this.aeSupport.insertOutputSlotIntoNetwork(outputSlot, this.aeOutputMode);
return sendUpdatePacket;
```

如果机器可能因为网络满而留下输出，support 的 AE ticker 会在网络变化后继续尝试。但前提是这个 slot/tank 已经被 `remember` 过，也就是至少调用过一次 insert 方法。

## 8. Mixin accessor

很多 Mek slot、tank、energyContainer 是 private/protected，新增机器通常需要 accessor。

流程：

1. 在 `src/main/java/com/beipuo/mekenergistics/mixin` 添加 `TileEntityXxxAccessor`。
2. 暴露必须字段，例如 input slot、output slot、secondary output、chemical tank、energyContainer。
3. 在 `src/main/resources/mekenergistics.mixins.json` 注册 accessor。
4. 只暴露必要字段，避免把 Mek 内部实现过度绑定。

需要 accessor 的典型场景：

- 替换 energy container。
- 在 `pushPattern` 中直接访问目标输入槽。
- 输出回网时访问原机器 output slot/tank。
- 多输入机器需要区分主槽、副槽。

## 9. BlockType、Side Config 和默认物品配置

`ModBlockTypes.createMachineBlockType(machine)` 负责 Mek block type 属性。新增机器时检查：

- 是否有 GUI：`.withGui(() -> ModMenuTypes.getMachineContainer(machine))`
- 是否有能量：`.withEnergyConfig(machine.energyUsage(), machine.energyStorage())`
- 是否支持升级：`.withSupportedUpgrades(Upgrade.SPEED, Upgrade.ENERGY)`
- 是否需要 `AttributeFactoryType`
- 是否需要 `AttributeTier`
- 是否需要 custom shape
- 是否需要特殊 bounding attribute
- 是否需要升级目标 `MeUpgradeableAttribute`
- `sideConfigFor(machine)` 是否包含正确的 `TransmissionType`

`ModItems.defaultSideConfig(machine)` 负责物品上的默认侧面配置。新增机器时要同步这里，否则玩家放下方块后的初始 side config 会和机器能力不匹配。

常见映射：

- item + energy: `AttachedSideConfig.ELECTRIC_MACHINE`
- item + chemical: `AttachedSideConfig.ADVANCED_MACHINE` 或 `ADVANCED_MACHINE_INPUT_ONLY`
- item + item: `AttachedSideConfig.EXTRA_MACHINE`
- reaction: `AttachedSideConfig.REACTION`
- crystallizer / washer / separator / painting 等用 Mek 已有常量。

## 10. 菜单和客户端 GUI

菜单入口是 `ModMenuTypes.getMachineContainer(machine)`。

新增机器时选择：

- 能复用普通样板 GUI：`registerPatternContainer(...)`
- 需要保留 Mek 原 GUI 并加样板窗口：`registerPatternTileContainer(...)`
- Formulaic Assemblicator 这种特殊 GUI：单独 container。
- 工厂：`ME_FACTORY` 或可选 compat 的 factory container。

客户端注册在 `ClientSetup.registerScreens(...)`。如果已有 ME GUI 包装类，注册对应 `MeGuiXxx`；如果只是无 AE 的工具机器，可以直接复用 Mek 原 GUI，但 container 泛型要确认安全。

新增 GUI 时还要检查：

- 样板按钮是否显示。
- AE 输出开关是否显示并同步。
- JEI exclusion area 是否避开样板按钮。
- 玩家背包偏移是否和 Mek 原 GUI 对齐。

## 11. JEI

JEI 入口是 `client/jei/MekEnergisticsJeiPlugin.java`。

新增有配方机器时要补 recipe catalyst：

- Mek 原 factory type 机器：优先走 `registerCatalysts(registration, recipeType, FactoryType.X)`。
- 非 factory type 机器：走 `registerMachines(registration, RecipeViewerRecipeType.X, MeMekanismMachine.X)`。
- More Machine 扩展：优先放进对应 compat JEI 类，避免主 JEI 插件直接硬引用可选模组类。

如果机器是工厂变体，默认会被 `hiddenStacks()` 隐藏，只保留基础 ME 机器作为 JEI catalyst。新增特殊机器时确认它是否应隐藏。

## 12. 安装器和升级链

ME 工厂安装器目标解析在 `MeInstallerTargetResolver`。

它的顺序是：

1. 如果目标已经是 ME 机器，返回 `null`。
2. 尝试按原 block registry path 拼 `me_` 前缀，例如 `mekanism:crusher` -> `mekenergistics:me_crusher`。
3. 尝试 Mekanism More Machine / Extras compat。
4. 读取 Mek `AttributeFactoryType` 和 `AttributeTier`，映射基础机器或 factory tier。

因此新增机器要确保：

- `MeMekanismMachine.registryName()` 与原机器 path 能对应。
- 对 factory，`getBaseMachine`、`getFactory`、`getNextFactory` 能找到目标。
- 对可选模组，compat resolver 能把原机器 block state 映射到 ME machine。
- `ModBlockTypes` 中升级目标正确，尤其 Ultimate -> Extras absolute 的链。

## 13. 资源文件清单

新增一台机器至少补这些资源：

- `src/main/resources/assets/mekenergistics/blockstates/me_xxx.json`
- `src/main/resources/assets/mekenergistics/models/block/me_xxx.json`
- `src/main/resources/assets/mekenergistics/models/item/me_xxx.json`
- `src/main/resources/data/mekenergistics/recipe/me_xxx.json`
- `src/main/resources/assets/mekenergistics/lang/en_us.json`
- `src/main/resources/assets/mekenergistics/lang/zh_cn.json`
- 如已有俄文维护，也补 `ru_ru.json`

如果是工厂等级批量机器，要确认所有 tier 都有 blockstate、active model、item model 和 recipe。

命名规则：

```text
enum baseName: enrichment_chamber
registryName(): me_enrichment_chamber
block id: mekenergistics:me_enrichment_chamber
translation key: block.mekenergistics.me_enrichment_chamber
```

不要让资源名、enum baseName、recipe id 三套命名各走各的。后面 JEI、安装器和语言文件都会被拖着出问题。

## 14. 可选模组适配边界

可选模组代码应放在独立 compat package：

- `compat/mekmm`: Mekanism More Machine。
- `compat/meke`: Mekanism Extras。
- `client/compat/...`: 客户端 screen 注册。
- `menu/compat/...`: 对应 container。
- `blockentity/compat/...`: 对应 BlockEntity。

公共路径可以引用本项目自己的 compat 类，但不要在未检查可用性时直接触发目标模组类加载。现有项目用两层保护：

- `ModList.get().isLoaded(...)` 检查 mod id。
- `OptionalCompatClasses.hasClassResource(...)` 检查目标类是否存在。

新增其它模组时，也应先做一个类似 `OptionalCompatClasses` 的能力探测，再接注册分派。这样能支持“同一个模组不同版本功能不一致”的情况。

## 15. 新增 Mek 机器的推荐步骤

按这个顺序做，出错时定位最快：

1. 在 `MeMekanismMachine` 添加 enum，确认 `registryName()`、`translationKey()`、`isAvailable()`。
2. 判断是否能复用已有 BlockEntity 模板；不能复用就新增最小专用 BlockEntity。
3. 如需访问 Mek 私有字段，新增 mixin accessor 并注册到 `mekenergistics.mixins.json`。
4. 在 `ModBlockEntities` 选择 `ae(...)` 或 `noAe(...)`，并确认 AE capability 会注册。
5. 在 BlockEntity 中接入 pattern slots、AE grid node、AE-backed energy、`pushPattern`、输出回网、NBT 保存和 container tracker。
6. 在 `ModBlockTypes` 补 shape、side config、升级链和特殊 attribute。
7. 在 `ModItems.defaultSideConfig` 补默认侧面配置。
8. 在 `ModMenuTypes` 选择或新增 container，并在 `ClientSetup` 注册 screen。
9. 在 JEI 插件或 compat JEI 类补 catalyst。
10. 检查 `MeInstallerTargetResolver` 能否从原机器升级到 ME 机器。
11. 补 blockstate、model、recipe、lang。
12. 运行 `compileJava`，再进游戏验证样板下单、加工、回网和升级。

## 16. 新增工厂类型的推荐步骤

如果是新增一整套 factory type，先不要一口气补所有 tier。先完成 basic tier 的闭环，再批量扩展。

1. 在 `MeMekanismMachine` 添加 base machine 和 `BASIC_*_FACTORY`。
2. 确认 factory type 是否来自 Mek `FactoryType`，还是来自扩展模组的字符串 type name。
3. 写或复用 factory BlockEntity。
4. 在 `factoryRegistration` 或对应 compat registration 里映射 factory type 到 BlockEntity。
5. 实现跨槽 `pushPattern`，优先使用 `MeFactoryInventoryInsert`。
6. 输出回网时传入全部 `outputSlots`、secondary outputs 和 tank。
7. basic tier 验证通过后，再补 advanced/elite/ultimate 以及 Extras tiers。
8. 更新 `getNextFactory()` 升级链。
9. 补所有 tier 的资源文件和配方。
10. 确认 JEI 隐藏策略只隐藏变体，不隐藏代表性 catalyst。

## 17. 验证清单

每台机器完成前至少验证：

- 游戏能启动，相关 registry 无缺失。
- 机器方块、物品、语言、模型正常。
- GUI 能打开，样板窗口能打开。
- 放入编码样板后，机器出现在 AE pattern provider 列表。
- AE 下单时 `pushPattern` 返回 true，并且原料进入正确槽/tank。
- 输入不足或槽/tank 满时不执行半插入。
- 机器运行时能消耗本地 FE 或 AE 网络能量。
- item 输出能按 AE 输出模式回网。
- chemical/fluid 输出能按预期回网。
- 网络满时输出留在机器内，不丢失。
- 网络恢复空间后，AE ticker 能继续把输出回网。
- 拆除机器时样板掉落或转移逻辑正常。
- 配置卡、侧面配置、自动输出、升级、红石控制不被破坏。
- JEI catalyst 显示正确，重复工厂变体按配置隐藏。
- 安装器能从原机器升级到 ME 机器，工厂升级链正确。

## 18. 常见坑

- 只在 `onUpdateServer` 推输出，但没有让 support 记住输出 tank，网络满后不会持续重试。
- `pushPattern` 先 execute item，再发现 chemical 插不进去，导致 AE 下单原料丢失。
- 忘记 `addContainerTrackers`，AE 输出模式 GUI 看起来切了，服务端没同步。
- 忘记 `saveSlots/loadSlots`，重进世界样板槽清空。
- 忘记 `ICraftingProvider.requestUpdate`，样板变化后 AE 终端不刷新。
- `MeMekanismMachine.registryName()` 和资源文件名不一致。
- 可选模组类在公共静态初始化中被直接引用，未安装该模组时崩溃。
- 默认 side config 没补，玩家放下机器后输入输出方向不符合 Mek 原机习惯。
- recipe energy view 没包 AE 能量，机器连上 AE 网络也被配方缓存判定没电。

## 19. 后续抽象方向

现在项目已经有两套稳定骨架：

- `MeRecipeMachineAeSupport`: 普通机器 AE 支撑。
- `MeFactoryAeSupport`: 工厂机器 AE 支撑。

后续适配更多模组时，不建议再复制整套 AE 节点、样板槽、能量、输出回网逻辑。更好的方向是继续把差异收敛到小接口：

- 输入解析：item/chemical/fluid 的 lane 定义。
- 输出声明：有哪些 slot/tank 需要回网。
- 能量桥：本地 FE + AE 网络能量。
- 安装器映射：原机器 -> ME 机器。
- 可选模组探测：mod id + class resource。

每新增一种机器，只应该新增“机器差异”，而不是再写一套 AE 机器框架。这样 Mek-Energistics 后面接更多 Mek 系扩展模组时，维护成本会随着机器族增长，而不是随着机器数量爆炸。

## 20. 新增其它模组 compat 的模板

适配新模组时，先照 Mekanism More Machine 和 Mekanism Extras 的形状建目录。不要把可选模组类散落到主注册类里。

推荐目录：

```text
src/main/java/com/beipuo/mekenergistics/compat/<modid>/
src/main/java/com/beipuo/mekenergistics/blockentity/compat/<modid>/
src/main/java/com/beipuo/mekenergistics/menu/compat/<modid>/
src/main/java/com/beipuo/mekenergistics/client/compat/<modid>/
src/main/java/com/beipuo/mekenergistics/client/jei/compat/
```

服务端 compat 门面至少提供这些方法：

```java
public final class SomeModCompat {
    public static TileEntityTypeRegistryObject<? extends TileEntityMekanism> registerBaseMachine(
            MeMekanismMachine machine, MachineFactoryRegistrar registrar);

    public static TileEntityTypeRegistryObject<? extends TileEntityMekanism> registerFactoryMachine(
            MeMekanismMachine machine, MachineFactoryRegistrar registrar);

    public static <TILE extends TileEntityMekanism> BlockTypeTile<TILE> createBaseBlockType(
            MeMekanismMachine machine, TileEntityTypeRegistryObject<TILE> tileType);

    public static <TILE extends TileEntityMekanism> BlockTypeTile<TILE> createFactoryBlockType(
            MeMekanismMachine machine, TileEntityTypeRegistryObject<TILE> tileType);

    public static MeMekanismMachine getFactoryTarget(BlockState state);

    public static void registerGridNodeHost(
            RegisterCapabilitiesEvent event, TileEntityTypeRegistryObject<? extends TileEntityMekanism> holder);
}
```

不是每个模组都需要全部方法，但保留这个形状能让 `ModBlockEntities`、`ModBlockTypes`、`MeInstallerTargetResolver` 的分派很干净。

接入主线的位置：

- `OptionalCompatClasses`: 增加 `hasSomeMod()` 和必要的 class resource 探测。
- `MeMekanismMachine`: 增加 base machine、factory machine、`requiredModId`、type name 字段或构造路径。
- `ModBlockEntities`: 在 `registerMachine` 和 `registerCapabilities` 中分派到 compat 门面。
- `ModBlockTypes`: 在 `createMachineBlockType` 中分派到 compat 的 block type builder。
- `ModMenuTypes`: 增加 optional holder 或实际 container 注册。
- `ClientSetup`: 只在目标模组加载时调用 client compat screen 注册。
- `MekEnergisticsJeiPlugin`: 只调用 JEI compat 门面，不直接引用目标模组 recipe viewer class。
- `MeInstallerTargetResolver`: 在目标模组可用时调用 compat 的 target resolver。

## 21. 可选模组探测规则

`ModList.get().isLoaded(modid)` 只能说明模组加载了，不能说明某个集成类或某个版本功能存在。现有代码用 `OptionalCompatClasses.hasClassResource(...)` 继续检查类资源，这是后续 compat 应继续沿用的方式。

推荐探测层级：

```java
public static boolean hasSomeMod() {
    return ModList.get().isLoaded("some_mod");
}

public static boolean hasSomeModFactories() {
    return hasSomeMod() && hasClassResource("com/example/somemod/factory/SomeFactory.class");
}

public static boolean hasSomeModAdvancedMachines() {
    return hasSomeMod() && hasClassResource("com/example/somemod/machine/AdvancedMachine.class");
}
```

在 `MeMekanismMachine.isAvailable()` 中使用最细粒度探测。这样某个扩展模组缺少高级工厂类时，只会跳过对应 ME 机器，不会让整个 Mek-Energistics 启动失败。

## 22. compat 分派检查单

新增一个模组 compat 后，逐项检查：

- enum 机器在目标模组未安装时 `isAvailable()` 返回 false。
- 未安装目标模组时，公共静态初始化不会加载目标模组类。
- `ModBlocks`、`ModItems`、`ModBlockEntities` 只注册 available 机器。
- `registerMachine` 能给每台机器选到正确 BlockEntity。
- `registerCapabilities` 只给实现 AE host 的 BlockEntity 注册 `IN_WORLD_GRID_NODE_HOST`。
- `createBaseBlockType` / `createFactoryBlockType` 保留目标模组原本的 GUI、shape、tier、side config 和升级支持。
- `getFactoryTarget` 能从原模组机器 block state 找到对应 `MeMekanismMachine`。
- 菜单和客户端 screen 注册只在目标模组存在时运行。
- JEI compat 不在主类中直接引用目标模组客户端类。
- 安装器能处理 base -> basic factory、tier -> next tier、扩展 tier 链。
- 资源文件覆盖所有 available 机器，不覆盖 unavailable 机器。

## 23. 适配记录模板

每新增一个机器族，在 PR 或提交说明里记录下面这张表。它会逼着我们把“能注册”和“真能自动合成”分开看，少踩不少坑。

```text
mod:
machine family:
base machines:
factory tiers:
required classes:
enum entries:
block entities:
menu/container:
screen:
jei catalysts:
installer mapping:
pattern inputs:
ae outputs:
energy bridge:
side config:
resource files:
manual tests:
known limits:
```

示例：

```text
mod: mekanism_more_machine
machine family: CNC Lathe / Lathing Factory
base machines: ME CNC Lathe
factory tiers: basic, advanced, elite, ultimate, absolute, supreme, cosmic, infinite
required classes: com/jerry/mekmm/...
enum entries: CNC_LATHE, BASIC_LATHING_FACTORY, ...
block entities: MeLatheBlockEntity, MeMoreMachineItemStackToItemStackFactoryBlockEntity
pattern inputs: item x1
ae outputs: item output slots
energy bridge: AeBackedFactoryEnergyContainer / MeRecipeMachineAeSupport
known limits: ...
```

## 24. 按机器类型的落地 playbook

### 单 item 输入机器

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

### item + chemical 输入机器

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

### 双 item 输入机器

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

### item 输入 + 副产物输出机器

代表：Precision Sawmill。

优先参考：

- 普通机器：`MePrecisionSawmillBlockEntity`
- 工厂：`MeSawingFactoryBlockEntity`

检查点：

- `hasSecondaryOutput()` 要返回 true。
- 输出回网必须同时传主输出和 secondary output。
- secondary output 可能为空，也可能因概率不产生，不能把空输出当失败。
- JEI 和 recipe viewer 要使用 sawmill 对应 recipe type。

### item + fluid + chemical 输入机器

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

### chemical + chemical 输入机器

代表：Chemical Infuser、Pigment Mixer。

优先参考：

- `MeChemicalInfuserBlockEntity`
- `MePigmentMixerBlockEntity`

检查点：

- 输入都是 chemical，通常左右 tank 可互换。
- 先尝试 `(first, second)`，失败再尝试 `(second, first)`。
- `MeChemicalInputHelper.insertPair(...)` 会检查容量、同类 chemical 和 tank validity，并在不合法时回滚。
- 输出是 chemical tank，必须通过 `insertChemicalTankIntoNetwork` 回网。

### fluid 和 chemical 可切换机器

代表：Rotary Condensentrator。

优先参考：`MeRotaryCondensentratorBlockEntity`。

检查点：

- `getMode()` 决定当前方向。
- 当前实现中 `getMode() == true` 时接受 fluid 输入并输出 chemical；`getMode() == false` 时接受 chemical 输入并输出 fluid。
- `pushPattern` 必须按当前模式验证输入类型。
- 输出回网也必须按当前模式选择 tank。
- GUI/样板使用时要提醒玩家：样板是否可执行取决于机器当前模式。

### 纯 chemical / fluid 输出机器

代表：Chemical Oxidizer、Chemical Washer、Electrolytic Separator、Solar Neutron Activator。

优先参考同目录 `blockentity/machine/chemical` 下现有类。

检查点：

- 输出 tank 数量可能不止一个，例如 Electrolytic Separator 有 left/right tank。
- 多输出 tank 都要传给 support，否则网络满后其中一边不会被 AE ticker 继续处理。
- fluid 输出目前要特别检查 `AeOutputMode` 语义，避免玩家打开 chemical 输出却无法回 fluid。
- 有 daylight、biome、water source 或环境依赖的机器，不要只按 recipe 判断可执行。

### 无 AE 样板的工具机器

代表：Digital Miner、Teleporter、Oredictionificator、Modification Station、Logistical Sorter。

优先复用：`MeMekanismMachineBlockEntity` 或对应 utility BlockEntity。

检查点：

- `ModBlockEntities` 走 `noAe(...)`。
- 不注册 `IN_WORLD_GRID_NODE_HOST`。
- 菜单可直接复用 Mek 原 GUI/container。
- 仍要确认方块、物品、side config、安装器、模型和语言正常。
- 不要为了“统一”强行注册 crafting provider。没有明确 pattern 输入输出语义的机器，接入 AE 下单只会制造假自动化。

## 25. 文件改动路线图

新增普通机器时，通常按下面顺序改文件：

```text
MeMekanismMachine.java
-> mixin/TileEntityXxxAccessor.java
-> mekenergistics.mixins.json
-> blockentity/machine/.../MeXxxBlockEntity.java
-> ModBlockEntities.java
-> ModBlockTypes.java
-> ModItems.java
-> ModMenuTypes.java
-> ClientSetup.java
-> MekEnergisticsJeiPlugin.java
-> lang/blockstate/model/item/recipe json
-> guide 更新
```

新增可选模组机器族时，路线变成：

```text
OptionalCompatClasses.java
-> MeMekanismMachine.java
-> blockentity/compat/<modid>/...
-> compat/<modid>/SomeModCompat.java
-> menu/compat/<modid>/...
-> client/compat/<modid>/...
-> client/jei/compat/SomeModJeiCompat.java
-> ModBlockEntities.java
-> ModBlockTypes.java
-> ModMenuTypes.java
-> ClientSetup.java
-> MeInstallerTargetResolver.java
-> resources
-> guide 更新
```

这个顺序的用处是每一层都只依赖上一层已经存在的事实：先有机器定义，再有实现，再接注册，再接 UI 和资源。

## 26. 编译和游戏内验证命令

编译优先用项目当前 JDK 配置：

```powershell
$env:JAVA_HOME='C:\GRAALVM\graalvm-jdk-21.0.6+8.1'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat compileJava --no-daemon --no-problems-report --no-configuration-cache
```

游戏内验证：

```powershell
$env:JAVA_HOME='C:\GRAALVM\graalvm-jdk-21.0.6+8.1'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat runClient --no-problems-report
```

建议每次只验证一个机器族：

1. 新建创造世界。
2. 放置 ME 机器并接 AE 网络。
3. 放入一个最简单编码样板。
4. 从 AE 终端下单 1 次。
5. 再下单批量，确认多次插入不会卡槽。
6. 填满 AE 存储后让机器产物留在输出槽/tank。
7. 清出 AE 存储空间，确认 AE ticker 会继续回网。
8. 拆除机器，确认样板和 owner/security 行为正常。

## 27. 当前实现索引

新增机器前先从这张表找最接近的实现。能复用就复用，不能复用也尽量复制同类型最小差异。

| 机器族 | 代表机器 | 普通机器实现 | 工厂实现 | 备注 |
| --- | --- | --- | --- | --- |
| 单 item 输入 | Enrichment / Crusher / Smelter | `MeElectricMachineBlockEntity` | `MeItemStackToItemStackFactoryBlockEntity` | `FactoryType.SMELTING/ENRICHING/CRUSHING` |
| item + chemical 输入 | Compressing / Purifying / Injecting | `MeAdvancedElectricMachineBlockEntity` | `MeItemStackChemicalToItemStackFactoryBlockEntity` | Purifying/Injecting 关注随机消耗 |
| Metallurgic Infusing | Metallurgic Infuser | `MeMetallurgicInfuserBlockEntity` | `MeItemStackChemicalToItemStackFactoryBlockEntity` | 普通机器有独立 Mek tile |
| 双 item 输入 | Combiner | `MeCombinerBlockEntity` | `MeCombiningFactoryBlockEntity` | 主/副槽不能交换 |
| 副产物输出 | Precision Sawmill | `MePrecisionSawmillBlockEntity` | `MeSawingFactoryBlockEntity` | 同时回主输出和 secondary output |
| 三输入反应 | Pressurized Reaction Chamber | `MePressurizedReactionChamberBlockEntity` | `MeAdvancedPressurizedReactingFactoryBlockEntity` | item + fluid + chemical |
| chemical -> item | Chemical Crystallizer | `MeChemicalCrystallizerBlockEntity` | `MeAdvancedChemicalToItemFactoryBlockEntity` | 工厂可能只有 chemical 输入，无 item input |
| item -> chemical | Chemical Oxidizer / Pigment Extractor | `MeChemicalOxidizerBlockEntity` / `MePigmentExtractorBlockEntity` | `MeAdvancedItemToChemicalFactoryBlockEntity` | 输出是 chemical tank |
| fluid + chemical | Chemical Washer | `MeChemicalWasherBlockEntity` | `MeAdvancedWashingFactoryBlockEntity` | 输入 fluid + chemical |
| chemical + chemical | Chemical Infuser / Pigment Mixer | `MeChemicalInfuserBlockEntity` / `MePigmentMixerBlockEntity` | 无原版 Mek factory，MekAF 类似机器走 advanced factory | 左右 tank 可交换时用 pair 插入 |
| 模式切换 | Rotary Condensentrator | `MeRotaryCondensentratorBlockEntity` | 无 | 样板执行依赖当前模式 |
| 双 chemical 输出 | Electrolytic Separator | `MeElectrolyticSeparatorBlockEntity` | 无 | left/right tank 都要回网 |
| 环境依赖 chemical | Solar Neutron Activator | `MeSolarNeutronActivatorBlockEntity` | 无 | 不只看 recipe，还看环境 |
| MekMM 基础 item 机器 | Recycler / Lathe / Rolling Mill | `MeRecyclerBlockEntity` / `MeLatheBlockEntity` / `MeRollingMillBlockEntity` | `MeMoreMachineItemStackToItemStackFactoryBlockEntity` | 可选模组，走 compat/mekmm |
| MekMM 双 item | CNC Stamper | `MeStamperBlockEntity` | `MeStampingFactoryBlockEntity` | stamping factory 有独立 extra slot |
| MekMM item + chemical | Planting / Replicating | `MePlantingStationBlockEntity` / `MeReplicatorBlockEntity` | `MePlantingFactoryBlockEntity` / `MeReplicatingFactoryBlockEntity` | 复用 `MeMekmmItemChemicalMachineSupport` |
| Mek Extras 工厂 | absolute/supreme/cosmic/infinite | 无普通机器 | `MeExtra*FactoryBlockEntity` | Extras tier + Mek 原 factory |
| Extras + MekMM 工厂 | Extras More Machine factories | 无普通机器 | `MeExtraMoreMachine*FactoryBlockEntity` | 组合 compat，必须 class resource 探测 |
| 无 AE 样板工具 | Digital Miner / Teleporter / Pump | `Me*BlockEntity` utility 类 | 无 | `noAe(...)`，只保留 Mek GUI/功能 |

## 28. 外部工厂共用支撑层

MekMM advanced factories 和 Mek Extras factories 已经抽了一层 `MeExternalFactorySupport`。后续适配其它工厂模组时，优先接这层，而不是重新写一套 factory pattern/energy/output 逻辑。

核心接口：

```java
public interface Owner extends MeFactoryAeMachine {
    List<IInventorySlot> meInputSlots();
    List<IInventorySlot> meOutputSlots();
    void unpauseRecipeMonitors();
}
```

它已经提供：

- `energyContainers(...)`: 创建 AE-backed factory energy container，并在能量变化时 unpause recipe monitors。
- `withPatternSlots(...)`: 把 pattern slots 合并进外部 factory 的 inventory holder。
- `pushSingleItem(...)`
- `pushTwoItems(...)`
- `pushThreeItems(...)`
- `pushItemChemical(...)`
- `pushChemical(...)`
- `pushFluidChemical(...)`
- `pushItemFluidChemical(...)`
- `drainOutputs(...)`
- `updateServer(...)`
- `createNodeOnFirstTick(...)`
- `save(...)` / `load(...)`
- `wrapRecipeEnergy(...)`

MekMM 的 `MeAdvancedFactorySupport` 和 Extras 的 `MeExtraFactoryBridge` 都是薄适配层：它们把各自的 factory owner 转成 `MeExternalFactorySupport.Owner`，再委托公共逻辑。

新增外部工厂时推荐结构：

```java
public class MeSomeExternalFactoryBlockEntity extends TileEntitySomeFactory implements SomeCompatFactorySupport.Owner {
    private final MeMekanismMachine machine;
    private MeFactoryAeSupport aeSupport;

    @Override
    public List<IInventorySlot> meInputSlots() {
        return this.inputItemSlots;
    }

    @Override
    public List<IInventorySlot> meOutputSlots() {
        return this.outputItemSlots;
    }

    @Override
    public boolean pushPattern(IPatternDetails pattern, KeyCounter[] inputs) {
        return getMainNode().isActive()
                && getAvailablePatterns().contains(pattern)
                && SomeCompatFactorySupport.pushItemChemical(this, inputs, this.chemicalTank);
    }
}
```

只在下面情况才写专用逻辑：

- 外部机器输入槽不是简单 list，必须按语义拆主/副/催化剂槽。
- 机器存在模式切换，输入类型随模式变化。
- 输出不仅在 slot/tank，还在内部缓存、概率表或延迟队列。
- recipe monitor unpause 机制和 Mek factory 不同。

## 29. 当前 guide 维护规则

每次完成一个新机器或机器族，至少回填三处：

- `## 24 按机器类型的落地 playbook`: 如果出现新输入/输出组合，补一小节。
- `## 27 当前实现索引`: 把新机器族和代表实现加入表。
- `## 23 适配记录模板`: 在提交说明或 PR 描述里填一份具体记录。

如果新增的是可选模组，还要回填：

- `## 20 新增其它模组 compat 的模板`: 补该模组是否需要额外门面方法。
- `## 21 可选模组探测规则`: 补 mod id 和关键 class resource。
- `## 22 compat 分派检查单`: 补该模组特殊分派点。

文档不要追求把所有源码复述一遍。它应该回答三个问题：

1. 新机器应该复制哪个现有实现。
2. 哪些注册点和资源文件不能漏。
3. 哪些行为必须进游戏验证。

## 30. 症状排查表

适配机器时，优先按现象排查。不要一上来重写 BlockEntity，很多问题只是漏了注册点或同步点。

| 现象 | 优先检查 |
| --- | --- |
| 机器物品不存在 | `MeMekanismMachine.isAvailable()`、`ModBlocks` / `ModItems` 是否注册、资源名是否和 `registryName()` 一致 |
| 放下方块崩溃 | `ModBlockEntities.registerMachine` 是否选错 BlockEntity、mixin accessor 是否注册、可选模组类是否在未加载时被引用 |
| GUI 打不开 | `ModBlockTypes.withGui(...)`、`ModMenuTypes.getMachineContainer(...)`、`ClientSetup.registerScreens(...)` |
| 样板按钮不出现 | container 是否是 pattern container、BlockEntity 是否实现 `MeAeMachine` 或 `MeFactoryAeMachine`、overlay 是否能识别目标 |
| 放入样板后 AE 终端不刷新 | pattern slot listener 是否调用 `updatePatterns()`、`ICraftingProvider.requestUpdate(...)` 是否触发 |
| AE 终端看不到机器 | 是否注册 `AECapabilities.IN_WORLD_GRID_NODE_HOST`、grid node 是否 `create(...)`、机器是否连接 AE 网络并有频道 |
| AE 下单失败 | `pushPattern` 的 `inputHolder.length`、输入 key 类型、simulate 插入、`getAvailablePatterns().contains(patternDetails)` |
| 下单吞原料 | 是否先 execute 了部分输入；必须所有 item/fluid/chemical simulate 成功后再 execute |
| 下单后机器不加工 | Mek recipe type 是否匹配、输入槽/tank 是否正确、recipe cache 是否 unpause、能量 view 是否包了 AE 网络能量 |
| 机器显示没电 | `createNewCachedRecipe` 是否使用 `wrapRecipeEnergy` / `withAeRecipeEnergy`，energy container 是否 AE-backed |
| 产物留在输出槽 | `AeOutputMode` 是否允许该类型、输出 slot/tank 是否传给 support、AE 网络是否有存储空间 |
| 网络恢复后产物仍不回网 | 输出 slot/tank 是否被 support remember；至少要调用过对应 `insert*IntoNetwork` |
| chemical/fluid 不回网 | `AeOutputMode` 的 type 映射是否符合当前 support 逻辑，Applied Mekanistics key 是否可用 |
| 重进世界样板消失 | `saveSlots/loadSlots` 是否调用，pattern slot tag key 是否一致 |
| 自定义样板终端名丢失 | `PatternTerminalName` 是否通过 support save/load，packet 是否调用 `setCustomPatternTerminalName` |
| AE 输出开关客户端不同步 | `addContainerTrackers` 是否 track `AeOutputMode` |
| JEI 没 catalyst | `MekEnergisticsJeiPlugin.registerRecipeCatalysts` 或 compat JEI 类是否补了 recipe type |
| JEI 重复机器太多 | `hiddenStacks()` 是否包含新增 factory 变体，配置 `hideJeiMachineVariants` 是否生效 |
| 安装器不能升级原机器 | `MeInstallerTargetResolver` 是否能通过 registry path、factory attribute 或 compat resolver 找到目标 |
| 安装器升级丢数据 | 升级路径是否保留 Mek data、side config、energy、upgrades、AE patterns、owner/security |
| 拆除机器样板消失 | `MeMekanismMachineBlock` 拆除逻辑是否能 `dropAndClear` pattern slots，BlockEntity 是否暴露 pattern slots |

## 31. 回归检查

这些是项目已经踩过、以后很容易回归的问题。改注册、安装器、BlockEntity 生命周期或能量逻辑时必须重新测。

1. 拆除 ME 机器时，Mekanism 机器数据保留，AE 样板掉落或被正确转移，不能被删除。
2. ME Factory Installer 升级时，升级组件、机器 NBT、side config、energy、owner/security、AE patterns 都要保留。
3. 机器连接 AE 网络时，配方执行能消耗 AE 网络能量，但 GUI 不应把网络总能量显示成机器本地 FE。
4. cached recipe energy view 只包能量读取/抽取，不替换 Mek 原配方逻辑。
5. MekMM base machines、regular factories、advanced factories 都要使用 AE-aware recipe energy。
6. Mek Extras regular factories、MoreMachine-derived factories、advanced factories 也要使用 AE-aware recipe energy。
7. Optional compat 未安装时，主模组仍能启动，客户端类也不能静态加载目标模组类。
8. 工厂变体在 JEI 隐藏时，基础 ME 机器仍保留为 catalyst。
9. 相邻方块渲染、透明面、active model 不因新增模型或 blockstate 回退。
10. Planting / Replicating 这类 base machine 要映射到 ME base machine，而不是误映射到 basic factory。

## 32. 代码评审清单

适配 PR 或提交前，用这份清单扫一遍。

功能层：

- `MeMekanismMachine` enum、availability、factory chain、slot layout 都正确。
- BlockEntity 继承的 Mek 原类正确，没有绕开原配方逻辑。
- `pushPattern` 对输入数量、类型、重复输入和空输入都有防御。
- 所有输入先 simulate，全部成功后再 execute。
- 输出回网覆盖所有 output slot、secondary output、chemical tank、fluid tank。
- recipe energy view 包住 cached recipe，AE 网络能量能参与加工。

注册层：

- block、item、block entity、menu、screen、capability 都有对应注册。
- 可选模组代码只在目标模组可用时进入。
- client-only 类不被 common 初始化路径加载。
- `ModItems.defaultSideConfig` 和 `ModBlockTypes.sideConfigFor` 保持一致。
- `MeInstallerTargetResolver` 能找到原机器到 ME 机器的映射。

资源层：

- blockstate、block model、active model、item model、recipe、lang 都存在。
- 资源文件名和 `registryName()` 完全一致。
- 工厂 tier 批量资源没有漏 tier。
- JEI 隐藏逻辑不会隐藏唯一 catalyst。

验证层：

- `compileJava` 通过。
- 单次 AE 下单通过。
- 批量 AE 下单通过。
- 输出槽/tank 满时不丢资源。
- AE 网络恢复空间后能继续回网。
- 拆除、升级、配置卡、side config、redstone、upgrade 都至少手动扫一遍。
