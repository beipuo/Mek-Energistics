# Mek-Energistics 代码结构优化分析

> Status: superseded by `docs/PLAN6.md`. This early analysis includes directions that are now complete or deliberately deferred, including immediate `MeAeSupportBase` extraction, `MeMekanismMachine` Builder conversion, and full `MeMekanismMachineBlockEntity` splitting. Use PLAN6 as the current cleanup route.

> 基于项目 238 个 Java 源文件的全面审查，识别需要合并、拆分和优化的代码。

---

## 1. 项目概览

Mek-Energistics 是一个 NeoForge 1.21.1 Minecraft 模组，桥接 Applied Energistics 2（AE2）和 Mekanism。项目添加了 ME 连接的 Mekanism 机器变体，可处理 AE2 编码模式、消耗 AE 能量并将成品返回 ME 网络。

### 当前代码统计

| 指标 | 数量 |
|------|------|
| Java 源文件 | 238 |
| Blockstate JSON | 323 |
| Recipe JSON | 322 |
| Mixin Accessor | 22 |
| 兼容模组集成 | 5（EME, MEKE, MekMM, ExtendedAE, Jade） |
| `blockentity/` 包文件数 | ~113 |
| `client/` 包文件数 | ~50 |

### 核心架构模式

- **枚举驱动的机器注册**：`MeMekanismMachine` 是所有机器的主枚举
- **基于继承的 BlockEntity**：ME 机器扩展 Mekanism 原始 BlockEntity，添加 AE2 接口
- **Mixin Accessor**：20+ Mixin 暴露 Mekanism 私有字段用于 AE2 集成
- **分层兼容系统**：EME/MEKE/MekMM 兼容分包在 `blockentity/compat/`、`compat/`、`client/compat/`、`menu/compat/`

---

## 2. 需要合并的代码

### 2.1 P0：重构 MeElectricMachineBlockEntity 和 MeAdvancedElectricMachineBlockEntity

**问题**：这两个类手动实现了完整的 AE2 集成（网格生命周期、模式管理、AeBackedEnergyContainer、RecipeEnergyView、AeTicker），与其他机器使用 `MeRecipeMachineAeSupport` 的方式完全不同。

**涉及文件**：
- `blockentity/machine/process/MeElectricMachineBlockEntity.java`（546 行）
- `blockentity/machine/process/MeAdvancedElectricMachineBlockEntity.java`（568 行）

**重复代码**：

两个文件中以下内部类几乎逐行相同，仅泛型参数不同：
- `AeBackedEnergyContainer`（~30 行）
- `RecipeEnergyView`（~43 行）
- `AeTicker`（~18 行）

两个文件中以下方法完全相同：
- `insertOutputSlotIntoNetwork()`（~15 行）
- `hasAeOutputWork()`（~6 行）
- `processAeOutputWork()`（~10 行）
- `alertAeTicker()`（~2 行）
- `processSmartPatternWork()`（~12 行）
- `insertIntoNetwork()`（~4 行）
- `getNetworkStorage()`（~4 行）
- `clearRemoved()` / `setRemoved()` / `onChunkUnloaded()`（~12 行）
- `getGridNode()` / `getActionableNode()`（~6 行）
- `updatePatterns()`（~12 行）
- `saveAdditional()` / `loadAdditional()`（~18 行）
- `addContainerTrackers()`（~5 行）

**总计**：~200 行重复代码 × 2 = ~400 行冗余

**解决方案**：
将这两个类重构为委托模式，使用 `MeRecipeMachineAeSupport` 处理 AE2 集成，仅保留各自特有的配方类型处理和 mixin accessor 调用。重构后每个类应缩减到 ~150 行。

---

### 2.2 P0：统一 MeRecipeMachineAeSupport 和 MeFactoryAeSupport

**问题**：两个 Support 类实现了近乎平行的 AE2 集成基础设施。

**涉及文件**：
- `blockentity/support/MeRecipeMachineAeSupport.java`（591 行）
- `blockentity/support/MeFactoryAeSupport.java`（632 行）

**重复的方法（逐方法对应）**：

| 功能 | MeRecipeMachineAeSupport | MeFactoryAeSupport |
|------|--------------------------|---------------------|
| 构造函数（网格节点创建） | L75-87 | L79-91 |
| getMainNode() | L89-91 | L93-95 |
| getPatternSlots() | L93-95 | L97-99 |
| getAvailablePatterns() | L97-99 | L101-103 |
| getPatternPriority() | L101-103 | L105-107 |
| isSmartPatternMultiplicationEnabled() | L119-121 | L124-126 |
| setSmartPatternMultiplicationEnabled() | L123-130 | L128-135 |
| enqueueSmartPattern() | L132-139 | L137-144 |
| processSmartPattern() | L141-149 | L146-155 |
| processSmartPatternViaOwner() | L152-160 | L157-165 |
| processSmartPatternWithPattern() | L162-171 | L167-176 |
| create() | L173-176 | L396-399 |
| destroy() | L178-180 | L401-403 |
| getGrid() | L182-184 | L210-212 |
| updatePatterns() | L407-422 | L422-438 |
| save() / load() | L424-437 | L405-420 |
| saveSlots() / loadSlots() | L439-454 | L440-455 |
| AeBackedEnergyContainer | L456-486 | L539-571 |
| RecipeEnergyView / FactoryRecipeEnergyView | L488-537 | L573-623 |
| AeTicker | L572-590 | L519-537 |
| NodeListener | L556-570 | L503-517 |
| alertAeTicker() | L361-363 | L356-358 |
| insertIntoNetwork() | L383-396 | L369-374 |
| getNetworkStorage() | L398-405 | L360-367 |

**差异点**：
- `MeRecipeMachineAeSupport` 管理 `OutputInventorySlot` / `IChemicalTank` / `IExtendedFluidTank` 的输出排空
- `MeFactoryAeSupport` 管理 `AeOutputMode` 内部状态和 `PatternContainerGroup` / `InternalInventory terminalPatternInventory`
- `MeFactoryAeSupport` 的 owner 是 `MeFactoryAeMachine` 接口，而 `MeRecipeMachineAeSupport` 的 owner 是泛型 `TILE extends TileEntityMekanism & MeAeMachine`

**解决方案**：
提取共享基类 `MeAeSupportBase`，将共同的网格生命周期、模式管理、智能模式乘法、NBT 保存/加载、AeTicker、NodeListener、alertAeTicker、getNetworkStorage 等移入基类。两个子类仅保留各自的特有逻辑。

**预计节省**：~400 行重复代码

---

### 2.3 P1：统一 AeBackedEnergyContainer 和 RecipeEnergyView

**问题**：存在 4 个几乎相同的 AeBackedEnergyContainer 实现和 4 个 RecipeEnergyView 实现。

**涉及文件**：
- `MeRecipeMachineAeSupport.AeBackedEnergyContainer`（L456-486）
- `MeFactoryAeSupport.AeBackedFactoryEnergyContainer`（L539-571）
- `MeElectricMachineBlockEntity.AeBackedEnergyContainer`（L452-480）
- `MeAdvancedElectricMachineBlockEntity.AeBackedEnergyContainer`（L474-502）

同样的 4 个 RecipeEnergyView 实现也在上述文件中。

**解决方案**：
在 `MeAeSupportBase` 或 `MeNetworkEnergyHelper` 中提供单一的 AeBackedEnergyContainer 实现，通过接口获取 grid 和 actionSource。当前 4 个实现的唯一区别是获取 grid 和 actionSource 的方式。

---

### 2.4 P2：合并 Client 端重复的 drawForegroundText

**问题**：8+ 个 screen 类包含完全相同的 `drawForegroundText` 实现。

**涉及文件**：
- `client/screen/machine/MeGuiCombiner.java`（L36-40）
- `client/screen/machine/MeGuiPrecisionSawmill.java`（L42-46）
- `client/screen/machine/MeGuiMetallurgicInfuser.java`（L39-43）
- `client/screen/machine/MeGuiPRC.java`（L46-50）
- `client/screen/machine/MeGuiSolarNeutronActivator.java`（L37-41）
- `client/screen/machine/MeGuiPaintingMachine.java`（L41-45）
- `client/screen/machine/MeGuiAdvancedElectricMachine.java`（L38-42）
- `client/screen/machine/MeGuiElectricMachine.java`（L37-41）

**解决方案**：
将默认 `drawForegroundText` 实现移入 `MeGuiConfigurableTile` 基类，或让这些类继承已包含此实现的 `MeMekanismMachineScreen`。

---

### 2.5 P2：合并 Client 端重复的能量组件组装

**问题**：8+ 个 screen 类包含相同的能量条 + 能量标签 + 进度条组装代码。

**涉及文件**：
- `client/screen/machine/MeGuiChemicalOxidizer.java`（L32-38）
- `client/screen/machine/MeGuiChemicalDissolutionChamber.java`（L32-41）
- `client/screen/machine/MeGuiChemicalCrystallizer.java`（L39-45）
- `client/screen/machine/MeGuiIsotopicCentrifuge.java`（L33-42）
- `client/screen/machine/MeGuiNutritionalLiquifier.java`（L32-39）
- `client/screen/machine/MeGuiPigmentExtractor.java`（L36-42）
- `client/screen/machine/MeGuiCombiner.java`（L28-32）
- `client/screen/machine/MeGuiMetallurgicInfuser.java`（L28-32）

**解决方案**：
提取 `addStandardEnergyWidgets()` helper 方法到基类。

---

### 2.6 P2：合并 Menu/Container 重复的 quickMoveStack

**问题**：10 个 container 类包含完全相同的 `quickMoveStack` 实现（12 行/个）。

**涉及文件**：
- `menu/MePatternMachineContainer.java`（L23-34）
- `menu/MePatternMekanismTileContainer.java`（L19-30）
- `menu/MePatternFormulaicAssemblicatorContainer.java`（L18-29）
- `menu/factory/MePatternFactoryContainer.java`（L19-30）
- `menu/compat/meke/MePatternExtraFactoryContainer.java`（L19-30）
- `menu/compat/meke/MePatternExtraAdvancedFactoryContainer.java`（L19-30）
- `menu/compat/meke/MePatternExtraMoreMachineFactoryContainer.java`（L19-30）
- `menu/compat/mekmm/MePatternMoreMachineFactoryContainer.java`（L19-30）
- `menu/compat/mekmm/MePatternAdvancedFactoryContainer.java`（L19-30）
- `menu/compat/eme/MePatternEMExtraFactoryContainer.java`（L19-30）

**解决方案**：
提供一个静态工具方法 `quickMovePattern(Player, Container, Object tile, ...)` 供所有容器类调用。

---

### 2.7 P2：合并 Compat MenuTypes 注册类

**问题**：6 个 MenuTypes 注册类结构完全相同（16 行/个），仅类名不同。

**涉及文件**：
- `compat/meke/MekanismExtrasMenuTypes.java`
- `compat/meke/MekanismExtrasAdvancedMenuTypes.java`
- `compat/meke/MekanismExtrasMoreMachineMenuTypes.java`
- `compat/mekmm/MekanismMoreMachineMenuTypes.java`
- `compat/mekmm/MekanismMoreMachineAdvancedMenuTypes.java`
- `compat/eme/EvolvedMekanismExtrasMenuTypes.java`

**解决方案**：
提供一个通用的 `registerTileMenu(ContainerTypeDeferredRegister, String, Class, ...)` 方法，将 6 个类合并为 1 个通用注册工具。

---

### 2.8 P2：合并 Compat ClientScreens 类

**问题**：6 个 ClientScreens 类结构完全相同（20 行/个），仅类名和类型参数不同。

**涉及文件**：
- `client/compat/meke/MekanismExtrasClientScreens.java`
- `client/compat/meke/MekanismExtrasAdvancedClientScreens.java`
- `client/compat/meke/MekanismExtrasMoreMachineClientScreens.java`
- `client/compat/mekmm/MekanismMoreMachineClientScreens.java`
- `client/compat/mekmm/MekanismMoreMachineAdvancedClientScreens.java`
- `client/compat/eme/EvolvedMekanismExtrasClientScreens.java`

**解决方案**：
提供通用的注册 helper 方法，将 6 个类合并为 1 个。

---

## 3. 需要拆分的代码

### 3.1 P0：拆分 MeMekanismMachineBlockEntity（944 行 God class）

**问题**：该类混合了 AE2 网格管理、配方处理（4 种类型）、物品栏管理、化学槽管理、配置数据传输等多种职责。

**涉及文件**：`blockentity/MeMekanismMachineBlockEntity.java`

**当前职责混合**：
- AE2 网格生命周期管理（L661-812）
- 配方处理逻辑（L308-633），包含 4 种配方类型：
  - `processSingleItemRecipe()`（L449-485）
  - `processCombinerRecipe()`（L487-530）
  - `processSawingRecipe()`（L604-633）
  - `processItemChemicalRecipe()`（L532-562）
- 对应的 canProcess 方法（L320-447）
- 化学槽管理（L564-602）
- 物品栏布局（L190-230）
- 手动索引算术（`PATTERN_SLOTS_START`、`energySlot()` 等）

**解决方案**：
1. AE2 网格管理 → 委托给 `MeRecipeMachineAeSupport`（已在 support 类中实现）
2. 配方处理逻辑 → 提取为独立的 `MeRecipeHandler` 或按 SlotLayout 分离
3. 物品栏管理 → 简化索引逻辑，使用命名常量或枚举

---

### 3.2 P1：拆分 MeMekanismMachine 枚举构造函数

**问题**：枚举有 11 个构造函数重载，用 primitive 类型（boolean/byte/short/long/int）来区分，极易混淆。

**涉及文件**：`common/machine/MeMekanismMachine.java`（L370-593）

**11 个构造函数**：
1. `(FactoryType, String, String)` — 标准机器
2. `(String, String, String, char)` — Evolved Mekanism 机器（char 用于类型区分）
3. `(FactoryTier, FactoryType)` — 标准工厂
4. `(String, FactoryType, boolean)` — Evolved 工厂（boolean 标记）
5. `(FactoryTier, String, String, short)` — EM Extras 工厂（short 用于类型区分）
6. `(String, String, String, byte)` — Evolved 自定义工厂（byte 用于类型区分）
7. `(String, FactoryType, int)` — EM Extras 标准工厂
8. `(String, String, String, int)` — EM Extras 自定义工厂
9. `(String, FactoryType)` — Mekanism Extras 工厂
10. `(String, String, String, long)` — Mekanism Extras 自定义工厂
11. `(FactoryTier, String, String)` — MoreMachine 工厂
12. `(String, String, String, boolean, boolean)` — Extra+MoreMachine（boolean 忽略参数）
13. `(FactoryTier, String, String, boolean)` — MoreMachine 高级工厂
14. `(String, String, String, boolean)` — Extra 高级工厂
15. `(String, String, String)` — MoreMachine 基础机器

**解决方案**：
引入 Builder 模式或配置 Record，用命名参数替代 primitive 类型重载：
```java
MeMekanismMachine.mekFactory(FactoryTier.BASIC, FactoryType.SMELTING)
MeMekanismMachine.evolvedFactory("overclocked", FactoryType.SMELTING)
MeMekanismMachine.emExtrasFactory("absolute", FactoryType.SMELTING)
```

---

## 4. 需要优化的代码

### 4.1 P1：提取 getSingleItemInput() 到 support 层

**问题**：`getSingleItemInput(KeyCounter)` 在以下文件中重复出现：
- `MeElectricMachineBlockEntity.java`（L339-353）
- `MeAdvancedElectricMachineBlockEntity.java`（结构相同）
- `MeItemStackToItemStackFactoryBlockEntity.java`（L122-136）
- `MeSawingFactoryBlockEntity.java`（L106-116）

**解决方案**：
在 `MeFactoryPatternInput` 中添加静态方法 `singleItem(KeyCounter)` 统一实现。

---

### 4.2 P1：提取 updatePatterns() 到 support 层

**问题**：`updatePatterns()` 在以下 5 个文件中完全相同：
- `MeElectricMachineBlockEntity.java`（L375-389）
- `MeAdvancedElectricMachineBlockEntity.java`（L393-407）
- `MeMekanismMachineBlockEntity.java`（L798-812）
- `MeRecipeMachineAeSupport.java`（L407-422）
- `MeFactoryAeSupport.java`（L422-438）

**解决方案**：
统一到基类或 support 类中。当重构 2.1 和 2.2 完成后，自然消除此重复。

---

### 4.3 P1：优化 MeFactoryPatternInput 多输入解析

**问题**：多输入解析逻辑（迭代 KeyCounter[] 分离 item/chemical/fluid）在以下位置重复：
- `MeAdvancedElectricMachineBlockEntity.java`（L336-371）
- `MeChemicalDissolutionChamberBlockEntity.java`（L97-128）
- `MeExternalFactorySupport.java`（L92-264，4 个重载方法）

**解决方案**：
在 `MeFactoryPatternInput` 中添加 `separate(KeyCounter[])` 方法返回结构化结果。

---

### 4.4 P2：消除 MeAeMachine 中的反射访问

**问题**：`MeAeMachine.java`（L96-114）使用 Java 反射查找 `aeSupport` 字段：
```java
java.lang.reflect.Field field = type.getDeclaredField("aeSupport");
field.setAccessible(true);
return (MeRecipeMachineAeSupport<?>) field.get(this);
```

**解决方案**：
在接口中提供 `getAeSupport()` 方法，让所有实现类显式实现，消除反射依赖。

---

### 4.5 P2：消除空接口 MeExtraFactoryAeMachine

**问题**：`MeExtraFactoryAeMachine.java` 是一个空接口：
```java
public interface MeExtraFactoryAeMachine extends MeFactoryAeMachine {}
```
未添加任何方法或约束。

**解决方案**：
移除该接口，直接使用 `MeFactoryAeMachine`。

---

### 4.6 P2：简化 ModMenuTypes.getMachineContainer() 的 if-else 链

**问题**：`ModMenuTypes.java`（L205-263）包含 57 行 if-else 链，将每个 `MeMekanismMachine` 映射到对应的 container 类型。

**解决方案**：
在 `MeMekanismMachine` 枚举中携带 container 类型，或使用 `Map<MeMekanismMachine, ContainerTypeRegistryObject<?>>` 替代 if-else 链。

---

### 4.7 P2：消除 Compat 类中重复的 registerFactoryMachine()

**问题**：`MekanismExtrasCompat.java`（L45-57）和 `EvolvedMekanismExtrasCompat.java`（L46-58）的 `registerFactoryMachine` 方法几乎相同。

**解决方案**：
提取参数化的工厂方法，仅传入不同的 block entity 构造函数。

---

### 4.8 P3：翻译中文注释

**问题**：Factory screen 代码中包含中文注释，不利于国际化维护。

**涉及文件**：
- `client/screen/machine/MeGuiMoreMachineFactory.java`（多处）
- `client/screen/machine/MeGuiAdvancedFactory.java`（多处）
- `client/screen/machine/MeGuiExtraFactory.java`（L86）

---

## 5. 推荐的目标代码结构

### 5.1 当前包结构

```
com.beipuo.mekenergistics/
├── blockentity/
│   ├── MeMekanismMachineBlockEntity.java     (944 行 God class)
│   ├── api/                                   (4 个接口)
│   ├── machine/
│   │   ├── chemical/ (14 files)
│   │   ├── process/ (7 files)                 ← 含 2 个不一致的老式类
│   │   └── utility/ (10 files)
│   ├── factory/ (4 files)
│   ├── compat/
│   │   ├── eme/ (8 files)
│   │   ├── meke/ (20 files)
│   │   ├── mekmm/ (22 files)
│   │   └── shared/ (1 file)
│   ├── support/ (12 files)                    ← 含 2 个平行的支持类
│   └── slot/ (2 files)
├── client/
│   ├── screen/                                ← 含大量重复 drawForegroundText
│   ├── overlay/ (2 files)
│   ├── jei/
│   └── compat/                                ← 含 6 个结构相同的类
├── compat/                                    ← 含 6 个结构相同的 MenuTypes
├── menu/                                      ← 含 10 个重复 quickMoveStack
├── mixin/ (24 files)
├── registry/ (11 files)
└── ...
```

### 5.2 推荐的目标结构

```
com.beipuo.mekenergistics/
├── blockentity/
│   ├── MeMekanismMachineBlockEntity.java     (精简至 ~400 行)
│   ├── api/
│   │   ├── AeOutputMode.java
│   │   ├── MeAeMachine.java                  (添加 getAeSupport() 方法)
│   │   ├── MeFactoryAeMachine.java           (移除 MeExtraFactoryAeMachine)
│   │   └── MeSmartCableConnection.java
│   ├── machine/
│   │   ├── chemical/ (14 files，保持不变)
│   │   ├── process/
│   │   │   ├── MeElectricMachineBlockEntity.java      (精简至 ~150 行，委托 support)
│   │   │   ├── MeAdvancedElectricMachineBlockEntity.java (精简至 ~150 行，委托 support)
│   │   │   ├── MeCombinerBlockEntity.java
│   │   │   ├── MePrecisionSawmillBlockEntity.java
│   │   │   └── ...
│   │   └── utility/ (10 files，保持不变)
│   ├── factory/ (4 files，保持不变)
│   ├── compat/
│   │   ├── eme/ (精简至 ~5 个文件)
│   │   ├── meke/ (精简至 ~15 个文件)
│   │   ├── mekmm/ (精简至 ~15 个文件)
│   │   └── shared/ (1 file)
│   ├── support/
│   │   ├── MeAeSupportBase.java               (新：共享基类，~200 行)
│   │   ├── MeRecipeMachineAeSupport.java      (精简至 ~200 行，继承基类)
│   │   ├── MeFactoryAeSupport.java            (精简至 ~250 行，继承基类)
│   │   ├── MeNetworkEnergyHelper.java
│   │   ├── MeFactoryPatternInput.java         (添加 getSingleItemInput/separate)
│   │   └── ... (其余 support 类保持不变)
│   └── slot/ (2 files，保持不变)
├── client/
│   ├── screen/
│   │   ├── MeGuiConfigurableTile.java        (添加默认 drawForegroundText)
│   │   └── machine/
│   │       ├── MeGuiChemicalOxidizer.java    (精简：移除重复代码)
│   │       └── ... (其余 screen 类精简)
│   └── compat/                               (6 个类合并为 ~2 个)
├── compat/                                   (6 个 MenuTypes 合并为 1 个通用工具)
├── menu/                                     (10 个 quickMoveStack 提取为工具方法)
├── common/machine/
│   └── MeMekanismMachine.java               (用 Builder 重构，精简构造函数)
└── ...
```

---

## 6. 实施优先级

### Phase 1：核心合并（影响最大）

| 步骤 | 任务 | 预计节省 | 风险 |
|------|------|----------|------|
| 1.1 | 重构 MeElectricMachineBlockEntity 使用 MeRecipeMachineAeSupport | ~200 行 | 中（需验证配方处理） |
| 1.2 | 重构 MeAdvancedElectricMachineBlockEntity 使用 MeRecipeMachineAeSupport | ~200 行 | 中 |
| 1.3 | 提取 AeBackedEnergyContainer / RecipeEnergyView 为共享实现 | ~120 行 | 低 |
| 1.4 | 重构 MeMekanismMachine 构造函数为 Builder 模式 | 代码可读性提升 | 中（需更新所有枚举值） |

### Phase 2：Support 层统一

| 步骤 | 任务 | 预计节省 | 风险 |
|------|------|----------|------|
| 2.1 | 提取 MeAeSupportBase 共享基类 | ~400 行 | 高（核心架构变更） |
| 2.2 | 统一 getSingleItemInput / updatePatterns 到 support | ~100 行 | 低 |
| 2.3 | 优化 MeFactoryPatternInput 多输入解析 | ~60 行 | 低 |

### Phase 3：Client 和 Menu 优化

| 步骤 | 任务 | 预计节省 | 风险 |
|------|------|----------|------|
| 3.1 | 合并重复的 drawForegroundText | ~40 行 | 低 |
| 3.2 | 提取能量组件组装 helper | ~40 行 | 低 |
| 3.3 | 提取 quickMoveStack 工具方法 | ~80 行 | 低 |

### Phase 4：Compat 层精简

| 步骤 | 任务 | 预计节省 | 风险 |
|------|------|----------|------|
| 4.1 | 合并 Compat MenuTypes 注册类 | ~60 行 | 低 |
| 4.2 | 合并 Compat ClientScreens 类 | ~60 行 | 低 |
| 4.3 | 优化 registerFactoryMachine 重复 | ~25 行 | 低 |

### Phase 5：代码清理

| 步骤 | 任务 | 预计节省 | 风险 |
|------|------|----------|------|
| 5.1 | 消除 MeAeMachine 反射访问 | ~20 行 | 低 |
| 5.2 | 移除空接口 MeExtraFactoryAeMachine | ~6 行 | 低 |
| 5.3 | 简化 ModMenuTypes if-else 链 | 代码可读性提升 | 低 |
| 5.4 | 翻译中文注释 | N/A | 无 |
| 5.5 | 清理未使用 import | 代码整洁 | 无 |

---

## 7. 总计影响

| 指标 | 当前 | 预计优化后 |
|------|------|-----------|
| Java 源文件数 | 238 | ~220（合并 compat 类） |
| 重复代码行数 | ~1200+ | ~200（减少 83%） |
| 最大单文件行数 | 944（MeMekanismMachineBlockEntity） | ~500 |
| 枚举构造函数数 | 11 | 1（Builder） |
| AeBackedEnergyContainer 实现数 | 4 | 1 |
| RecipeEnergyView 实现数 | 4 | 1 |
| quickMoveStack 重复数 | 10 | 1 |

---

## 8. 验证方式

每次重构后执行：

1. `./gradlew build` — 确保编译通过
2. 检查 mixin 配置（`mekenergistics.mixins.json`）中 22 个 mixin 仍正确工作
3. 在游戏内测试 ME 机器的基本功能：
   - 模式编码和处理
   - AE 能量供应
   - 物品/化学品/流体处理
   - 模式终端显示
   - 智能模式乘法
4. 检查 JEI 集成仍正常显示配方
5. 验证所有 323 个 blockstate JSON 和 322 个 recipe JSON 不受影响
