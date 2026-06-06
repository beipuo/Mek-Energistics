# GTM 多方块结构功能借鉴实现思路

本文整理的是在本地项目中实现一套精简版多方块结构系统的设计思路。目标功能只覆盖：

- 电量输入
- 物品输入、物品输出
- 流体输入、流体输出
- ME 输入总成
- ME 输出总成
- 样板总成

重点不是完整移植 GregTech Modern 的机器框架，而是借鉴它的核心分层：控制器负责结构和配方调度，部件负责能力，结构匹配负责把部件收集进控制器。

## 一、总体架构

建议拆成三层。

### 1. 多方块控制器

示例命名：

```java
MultiblockControllerBlockEntity
```

控制器负责：

- 检查结构是否成型
- 保存 `formed` 状态
- 收集结构内的部件
- 聚合电量、物品、流体、ME 输入、ME 输出、样板总成
- 驱动配方运行

控制器不应该直接保存所有仓室逻辑。它只负责把各部件暴露出来的 handler 聚合起来。

### 2. 多方块部件接口

示例接口：

```java
public interface IMultiblockPart {
    void addedToController(MultiblockControllerBlockEntity controller);

    void removedFromController(MultiblockControllerBlockEntity controller);

    PartAbility getAbility();
}
```

具体部件可以包括：

- `EnergyInputPart`
- `ItemInputBusPart`
- `ItemOutputBusPart`
- `FluidInputHatchPart`
- `FluidOutputHatchPart`
- `MEInputPart`
- `MEOutputPart`
- `PatternProviderPart`

### 3. 部件能力标识

借鉴 GTM 的 `PartAbility`，建议定义自己的能力枚举：

```java
public enum PartAbility {
    INPUT_ENERGY,
    IMPORT_ITEMS,
    EXPORT_ITEMS,
    IMPORT_FLUIDS,
    EXPORT_FLUIDS,
    ME_IMPORT,
    ME_EXPORT,
    PATTERN_PROVIDER
}
```

这个枚举用于两件事：

- 结构匹配时判断某个位置允许放哪些部件
- 控制器聚合部件时按能力分类

## 二、多方块成型与检测

GTM 的检测流程可以拆成五个阶段：

1. 定义结构模板
2. 根据控制器朝向把模板坐标转换成世界坐标
3. 逐格检查方块或部件是否匹配
4. 把匹配到的部件、IO 类型、错误信息写入检测状态
5. 成型成功后调用 `onStructureFormed`，失败或变化后调用 `onStructureInvalid`

本地项目可以保留这个流程，但实现得更轻量。

### 1. 结构模板

建议用三维字符模板描述结构。每一层是一个二维字符串数组，多层叠起来就是三维结构。

示例：

```java
public final class MultiblockPattern {
    private final String[][] layers;
    private final Map<Character, Predicate<BlockState>> blockPredicates;
    private final Map<Character, Set<PartAbility>> partPredicates;
    private final Vec3i controllerOffset;
}
```

示例结构：

```java
String[][] pattern = {
    {
        "CCC",
        "CIC",
        "CCC"
    },
    {
        "CPC",
        "EOE",
        "CFC"
    },
    {
        "CCC",
        "CMC",
        "CCC"
    }
};
```

字符含义可以是：

```text
C = 外壳
I = 任意物品/流体输入部件
O = 任意物品/流体输出部件
E = 电量输入仓
M = ME 输入或输出总成
P = 样板总成
F = 控制器前方或特定外壳
```

不要把结构判断写成一堆硬编码 `if`。结构模板应该独立成对象，这样后面改形状、加变体、做预览都更容易。

### 2. 控制器锚点和相对坐标

结构检测必须确定一个锚点。最简单的做法是以控制器方块为锚点：

```java
BlockPos controllerPos = this.worldPosition;
Direction facing = getBlockState().getValue(FACING);
```

模板里的每个坐标都是相对坐标：

```text
x = 左右
y = 上下
z = 前后
```

需要通过控制器朝向转换成世界坐标。

推荐写一个单独方法：

```java
private BlockPos toWorldPos(BlockPos controllerPos, Direction facing, int x, int y, int z) {
    Direction front = facing;
    Direction back = front.getOpposite();
    Direction right = front.getClockWise();
    Direction left = front.getCounterClockWise();

    return controllerPos
            .relative(right, x)
            .relative(Direction.UP, y)
            .relative(front, z);
}
```

如果模板规定 `z` 正方向是控制器正面，就用 `front`。如果模板规定机器主体在控制器背后，就用 `back`。关键是全项目保持一致。

建议第一版只支持水平朝向：

```java
NORTH, SOUTH, EAST, WEST
```

不要一开始支持上下朝向、翻转结构、可重复层。这些功能可以后续再加。

### 3. 检测状态对象

不要让检测方法只返回 `boolean`。建议返回一个带上下文的结果对象。

```java
public final class MultiblockCheckResult {
    public final boolean success;
    public final List<IMultiblockPart> parts;
    public final Map<BlockPos, IO> ioMap;
    public final Set<BlockPos> checkedPositions;
    public final String errorMessage;
    public final BlockPos errorPos;

    // constructor / factory methods
}
```

这样做有几个好处：

- 成型成功时可以直接拿到部件列表
- 成型失败时可以显示错误位置
- 方块变化时可以知道哪些坐标需要监听或重检
- 后面做投影预览、扳手提示会更方便

GTM 的 `MultiblockState` 就承担了类似职责：保存当前检测坐标、错误、匹配上下文、缓存坐标和控制器信息。

### 4. 单格匹配

每个模板字符都应该对应一种匹配规则。

建议定义：

```java
public interface PatternPredicate {
    boolean test(Level level, BlockPos pos, MultiblockCheckContext context);
}
```

常用匹配规则：

```java
blocks(ModBlocks.MACHINE_CASING)
controller()
part(PartAbility.INPUT_ENERGY)
part(PartAbility.IMPORT_ITEMS, PartAbility.IMPORT_FLUIDS)
part(PartAbility.EXPORT_ITEMS, PartAbility.EXPORT_FLUIDS)
part(PartAbility.ME_IMPORT, PartAbility.ME_EXPORT)
part(PartAbility.PATTERN_PROVIDER)
any()
air()
```

部件匹配示例：

```java
boolean matchPart(Level level, BlockPos pos, Set<PartAbility> allowed, MultiblockCheckContext ctx) {
    BlockEntity be = level.getBlockEntity(pos);
    if (!(be instanceof IMultiblockPart part)) {
        ctx.fail(pos, "expected multiblock part");
        return false;
    }

    if (!allowed.contains(part.getAbility())) {
        ctx.fail(pos, "wrong part ability: " + part.getAbility());
        return false;
    }

    ctx.parts.add(part);
    ctx.checkedPositions.add(pos);
    return true;
}
```

这里要注意：不要只检查方块类型。仓室、ME 总成、样板总成都需要拿到 block entity，因为后面控制器要调用它们的能力。

### 5. 部件共享和重复占用

GTM 会检查一个部件是否已经属于别的多方块，不能共享的部件不能被多个控制器同时使用。

本地项目也建议加最小检查：

```java
public interface IMultiblockPart {
    boolean isFormed();

    boolean canShare();

    boolean hasController(BlockPos controllerPos);
}
```

匹配时：

```java
if (part.isFormed() && !part.canShare() && !part.hasController(controllerPos)) {
    ctx.fail(pos, "part already belongs to another multiblock");
    return false;
}
```

第一版建议默认所有部件不可共享，除非你明确要做 GTM 那种共享仓室。

### 6. 成型入口

控制器应提供一个统一检测入口：

```java
public boolean checkStructure() {
    MultiblockCheckResult result = pattern.check(level, worldPosition, getFacing());

    if (result.success) {
        lastCheckResult = result;
        return true;
    }

    lastError = result.errorMessage;
    lastErrorPos = result.errorPos;
    return false;
}
```

不要在 `checkStructure()` 里直接启动配方。它只负责判断结构。

结构状态切换由外层控制：

```java
public void refreshStructure() {
    boolean valid = checkStructure();

    if (valid && !formed) {
        onStructureFormed(lastCheckResult);
    } else if (valid) {
        onStructureUpdated(lastCheckResult);
    } else if (formed) {
        onStructureInvalid();
    }
}
```

这样可以区分三种情况：

- 原来没成型，现在成型
- 原来已成型，现在仍成型，但部件可能变化
- 原来已成型，现在失效

### 7. 成型成功后的处理

成型成功时：

```java
void onStructureFormed(MultiblockCheckResult result) {
    formed = true;
    parts.clear();
    parts.addAll(result.parts);
    checkedPositions.clear();
    checkedPositions.addAll(result.checkedPositions);

    for (IMultiblockPart part : parts) {
        part.addedToController(this);
    }

    rebuildCapabilities();
    setChanged();
    syncToClient();
}
```

`rebuildCapabilities()` 负责把部件分组：

```java
energyInputs.clear();
itemInputs.clear();
itemOutputs.clear();
fluidInputs.clear();
fluidOutputs.clear();
meInputs.clear();
meOutputs.clear();
patternProviders.clear();

for (IMultiblockPart part : parts) {
    switch (part.getAbility()) {
        case INPUT_ENERGY -> energyInputs.add((EnergyInputPart) part);
        case IMPORT_ITEMS -> itemInputs.add((ItemInputBusPart) part);
        case EXPORT_ITEMS -> itemOutputs.add((ItemOutputBusPart) part);
        case IMPORT_FLUIDS -> fluidInputs.add((FluidInputHatchPart) part);
        case EXPORT_FLUIDS -> fluidOutputs.add((FluidOutputHatchPart) part);
        case ME_IMPORT -> meInputs.add((MEInputPart) part);
        case ME_EXPORT -> meOutputs.add((MEOutputPart) part);
        case PATTERN_PROVIDER -> patternProviders.add((PatternProviderPart) part);
    }
}
```

如果同一种部件允许多个，就用列表。如果只允许一个，比如样板总成，可以在结构检测阶段加数量限制。

### 8. 结构失效后的处理

结构失效时：

```java
void onStructureInvalid() {
    formed = false;

    stopRecipeLogic();

    for (IMultiblockPart part : parts) {
        part.removedFromController(this);
    }

    parts.clear();
    checkedPositions.clear();
    clearCapabilities();

    setChanged();
    syncToClient();
}
```

如果机器正在运行配方，建议第一版直接暂停或重置，不要尝试保留半成品状态。等基础系统稳定后再做断点恢复。

### 9. 重检触发

不要每 tick 全量扫描结构。建议用以下触发方式：

1. 控制器放置或加载时检查一次
2. 玩家右键/扳手点击控制器时手动检查
3. 结构相关方块变化时检查
4. 已成型机器的缓存坐标内发生变化时检查
5. 未成型机器低频自动重试，例如每 20 tick 或 40 tick

控制器保存上次检测过的位置：

```java
private final Set<BlockPos> checkedPositions = new HashSet<>();
```

邻近方块变化时：

```java
public void onObservedBlockChanged(BlockPos changedPos) {
    if (formed && checkedPositions.contains(changedPos)) {
        refreshStructure();
    }
}
```

如果你没有全局方块变化监听，第一版可以简单一点：

- 控制器每秒检查一次，直到成型
- 成型后每秒检查一次

这样性能差一点，但实现简单。后续再优化为缓存坐标监听。

### 10. 数量限制

结构模板能判断“这里允许放某类部件”，但还需要数量限制。

建议在 pattern 上定义：

```java
Map<PartAbility, IntRange> abilityLimits;
```

示例：

```java
INPUT_ENERGY: min 1, max 4
IMPORT_ITEMS: min 0, max unlimited
EXPORT_ITEMS: min 0, max unlimited
IMPORT_FLUIDS: min 0, max unlimited
EXPORT_FLUIDS: min 0, max unlimited
ME_IMPORT: min 0, max 1
ME_EXPORT: min 0, max 1
PATTERN_PROVIDER: min 0, max 1
```

检测结束后统计：

```java
Map<PartAbility, Integer> counts = countAbilities(result.parts);

for (var entry : abilityLimits.entrySet()) {
    PartAbility ability = entry.getKey();
    IntRange limit = entry.getValue();
    int count = counts.getOrDefault(ability, 0);

    if (count < limit.min() || count > limit.max()) {
        return fail("invalid count for " + ability);
    }
}
```

### 11. IO 类型记录

GTM 会在匹配时记录某个位置对应的 IO 类型。你本地项目可以更简单：由部件自己的 `PartAbility` 和 `IO` 决定。

```java
public enum IO {
    IN,
    OUT,
    BOTH,
    NONE
}
```

部件接口：

```java
public interface IRecipePart extends IMultiblockPart {
    IO getIO();
}
```

成型后控制器按 `ability + io` 聚合。这样结构模板只需要关心“这个位置允许哪些部件”，不必额外维护 `ioMap`。

### 12. 客户端同步

至少同步这些字段：

```java
boolean formed;
String lastError;
BlockPos lastErrorPos;
List<BlockPos> partPositions;
```

这些字段用于：

- GUI 显示机器是否成型
- Jade/Waila 显示结构错误
- 客户端模型切换成型状态
- 调试时高亮错误位置

第一版不需要同步完整结构缓存，只同步状态和错误即可。

### 13. 推荐的最小检测实现

第一版建议只实现：

- 固定三维模板
- 水平四方向旋转
- 控制器作为锚点
- 普通方块匹配
- 部件能力匹配
- 数量限制
- 成功后收集部件
- 失败后保存第一个错误

暂时不要实现：

- 可重复层
- 自动寻找第一层偏移
- 上下朝向
- 镜像翻转
- 多控制器共享部件
- 自动搭建
- 客户端结构预览

这些功能都可以后续添加，但不应该阻塞当前目标。

## 三、普通输入输出部件

普通输入输出部件应尽量做成和配方系统解耦的 handler。

### 物品输入总线

职责：

- 保存一个 `IItemHandler`
- 允许外部插入物品
- 配方运行时由控制器从中抽取输入

### 物品输出总线

职责：

- 保存一个 `IItemHandler`
- 配方完成时由控制器向其中写入产物
- 如果输出空间不足，机器应阻塞或停机

### 流体输入仓

职责：

- 保存一个 `IFluidHandler`
- 配方运行时由控制器抽取流体

### 流体输出仓

职责：

- 保存一个 `IFluidHandler`
- 配方完成时由控制器写入流体产物

建议抽象：

```java
public interface IRecipePart extends IMultiblockPart {
    IO getIO();

    List<Object> getRecipeHandlers();
}
```

后续可以把 `Object` 换成项目自己的 `RecipeHandler` 类型。

## 四、电量输入

电量输入仓第一版可以非常简单，不需要 GUI。

示例：

```java
public class EnergyInputPart implements IMultiblockPart {
    private final EnergyStorage energy;

    public long extractEnergyForRecipe(long amount, boolean simulate) {
        return energy.extractEnergy(amount, simulate);
    }
}
```

控制器聚合所有电量输入仓：

```java
long consumeEnergy(long amount, boolean simulate) {
    long remaining = amount;

    for (EnergyInputPart hatch : energyInputs) {
        long extracted = hatch.extractEnergyForRecipe(remaining, simulate);
        remaining -= extracted;

        if (remaining <= 0) {
            return amount;
        }
    }

    return amount - remaining;
}
```

配方开始前先模拟消耗，确认电量足够后再真实消耗。

## 五、ME 输入总成

ME 输入总成不要一开始就直接改配方系统。推荐第一版采用“本地缓存”模式。

职责：

- 连接 AE 网络
- 提供配置槽，记录需要补货的物品或流体
- 周期性从 AE 网络抽取配置内容
- 抽取到本地缓存
- 控制器把它当普通输入仓使用

基本流程：

```text
AE 网络 -> ME 输入总成本地缓存 -> 控制器配方输入
```

## 六、ME 输出总成

ME 输出总成建议采用“内部等待列表”模式。

职责：

- 配方完成时接收产物
- 产物先进入内部 buffer
- tick 时尝试写入 AE 网络
- 如果 AE 网络离线或存储已满，buffer 保留产物

基本流程：

```text
控制器配方产物 -> ME 输出总成 buffer -> AE 网络
```

建议第一版：buffer 满或 AE 离线时阻塞后续输出。

## 七、样板总成

样板总成是这个系统里最关键的部分。它不应该只是一个普通样板槽，而应该作为 AE 自动合成提供者。

职责：

- 保存编码样板
- 解码 AE 样板
- 向 AE 网络注册可用样板
- 接收 AE 自动合成请求
- 把请求转成多方块内部任务
- 将产物送回 ME 输出总成或 AE 网络

样板库存：

```java
ItemStackHandler patternInventory = new ItemStackHandler(27);
```

只允许编码样板：

```java
stack.getItem() instanceof EncodedPatternItem
```

样板变化时解码：

```java
IPatternDetails details = PatternDetailsHelper.decodePattern(stack, level);
```

样板总成应实现 AE2 的 `ICraftingProvider`。

关键方法：

- `getAvailablePatterns()`
- `pushPattern(...)`

样板变化后需要通知 AE：

```java
ICraftingProvider.requestUpdate(mainNode);
```

## 八、并行与跨配方并行

这里建议把“并行”和“跨配方并行”当成两个不同层级的功能处理。它们都能提高吞吐，但调度模型不一样。

### 1. 普通并行

普通并行指的是：同一个配方在同一次运行中执行多份。

例如一个配方是：

```text
1 铁粉 + 100 mB 酸 -> 1 处理后铁粉
耗电 30 EU/t
耗时 200 tick
```

如果机器支持 4 并行，那么一次运行变成：

```text
4 铁粉 + 400 mB 酸 -> 4 处理后铁粉
耗电 120 EU/t
耗时 200 tick
```

也就是说：

- 输入数量乘以并行数
- 输出数量乘以并行数
- 每 tick 耗电通常乘以并行数
- 配方耗时通常不变

这是最适合第一版实现的并行方式。

### 2. 普通并行的计算方式

控制器找到一个可运行配方后，不要立刻只跑 1 份，而是计算当前最多能跑几份。

最大并行数由几类限制共同决定：

```text
最终并行数 = min(
    机器最大并行,
    输入物品可支持并行,
    输入流体可支持并行,
    输出物品空间可支持并行,
    输出流体空间可支持并行,
    电压或功率可支持并行
)
```

示例：

```java
int calculateParallel(Recipe recipe, int machineLimit) {
    int limit = machineLimit;

    limit = Math.min(limit, getMaxParallelByItemInputs(recipe, limit));
    limit = Math.min(limit, getMaxParallelByFluidInputs(recipe, limit));
    limit = Math.min(limit, getMaxParallelByItemOutputs(recipe, limit));
    limit = Math.min(limit, getMaxParallelByFluidOutputs(recipe, limit));
    limit = Math.min(limit, getMaxParallelByEnergy(recipe, limit));

    return limit;
}
```

如果结果是 0，说明当前不能运行该配方。

运行前先模拟抽取和插入：

```java
int parallel = calculateParallel(recipe, maxParallel);
if (parallel <= 0) {
    return false;
}

RecipeRun run = recipe.scaled(parallel);

if (!canConsumeInputs(run)) return false;
if (!canInsertOutputs(run)) return false;
if (!canConsumeEnergy(run)) return false;

consumeInputs(run);
consumeEnergy(run);
startRecipe(run);
```

### 3. 并行控制部件

如果想借鉴 GTM 的“并行控制仓”，可以新增一个部件：

```java
ParallelControlPart
```

能力：

```java
PARALLEL_CONTROL
```

它只负责提供最大并行数：

```java
public class ParallelControlPart implements IMultiblockPart {
    private int maxParallel;
    private int currentParallel;

    public int getCurrentParallel() {
        return currentParallel;
    }
}
```

控制器成型时收集它：

```java
private int getMachineParallelLimit() {
    if (parallelControl != null) {
        return parallelControl.getCurrentParallel();
    }
    return 1;
}
```

第一版也可以不做并行控制仓，直接给控制器一个固定并行数，例如：

```java
maxParallel = 4;
```

### 4. 批处理并行

GTM 还有一种接近“批处理”的做法：对很短的配方，把多次运行合并成一个更长的任务。

例如：

```text
原配方耗时 5 tick
批处理目标时间 100 tick
批处理并行 = 100 / 5 = 20
```

合并后：

```text
输入乘 20
输出乘 20
耗时变成 100 tick
每 tick 耗电不一定乘 20，取决于你的设计
```

这个功能不是必须。建议等普通并行稳定后再加。

### 5. 跨配方并行

跨配方并行指的是：同一个多方块在同一时间运行多个不同配方。

例如机器同时运行：

```text
配方 A: 铁粉 + 酸 -> 处理后铁粉
配方 B: 铜粉 + 酸 -> 处理后铜粉
配方 C: 锡粉 + 水 -> 处理后锡粉
```

这和普通并行不同。

普通并行是：

```text
A x 4
```

跨配方并行是：

```text
A x 2 + B x 1 + C x 1
```

它需要一个真正的任务队列，而不是单个 `currentRecipe`。

### 6. 跨配方并行的数据结构

控制器需要从单任务模型改成多任务模型。

普通单任务模型：

```java
private RecipeRun currentRun;
```

跨配方并行模型：

```java
private final List<RecipeRun> activeRuns = new ArrayList<>();
private final Queue<RecipeRequest> pendingRequests = new ArrayDeque<>();
```

`RecipeRun` 至少包含：

```java
public class RecipeRun {
    public Recipe recipe;
    public int parallel;
    public int progress;
    public int duration;
    public long eut;
    public List<ItemStack> reservedItemInputs;
    public List<FluidStack> reservedFluidInputs;
    public List<ItemStack> expectedItemOutputs;
    public List<FluidStack> expectedFluidOutputs;
}
```

### 7. 跨配方并行的关键问题

跨配方并行不能只写一个循环找配方。它必须处理资源预留，否则会出现多个任务同时认为自己能使用同一批输入。

必须解决：

- 输入预留：任务 A 预留的物品，任务 B 不能再拿
- 流体预留：同一种流体要扣除已预留量
- 输出预留：输出仓或 ME 输出 buffer 要提前确认空间
- 电量预算：所有 active run 的 EU/t 总和不能超过输入能力
- 完成顺序：不同配方耗时不同，可能先后完成
- AE 样板请求：多个样板请求可能同时进入队列

因此跨配方并行应该晚于普通并行实现。

### 8. 跨配方并行调度流程

推荐流程：

```text
tick
-> 更新 activeRuns 进度
-> 完成的 run 写入输出
-> 清理完成任务
-> 如果还有并行槽位，尝试从 pendingRequests 或可用配方中创建新 run
-> 创建 run 前模拟输入、输出、电力
-> 创建成功后立即预留或扣除输入
```

伪代码：

```java
void serverTick() {
    if (!formed) return;

    tickActiveRuns();
    finishCompletedRuns();

    while (activeRuns.size() < maxRecipeSlots) {
        Recipe candidate = findNextRecipe();
        if (candidate == null) break;

        int parallel = calculateParallel(candidate, getPerRecipeParallelLimit());
        if (parallel <= 0) break;

        RecipeRun run = tryCreateRun(candidate, parallel);
        if (run == null) break;

        activeRuns.add(run);
    }
}
```

### 9. 样板总成与跨配方并行

如果机器接 AE 样板总成，跨配方并行最好以“样板请求队列”为入口。

AE 调用 `pushPattern` 时不要直接强行启动配方，而是生成请求：

```java
public class RecipeRequest {
    public IPatternDetails pattern;
    public KeyCounter[] inputs;
    public int requestedAmount;
}
```

然后加入控制器队列：

```java
controller.enqueuePatternRequest(request);
```

控制器 tick 时从队列取请求，检查当前是否有空闲跨配方槽位，再创建 `RecipeRun`。

这样比在 `pushPattern` 里直接运行更安全，因为 AE 可能连续推送多个样板。

### 10. 推荐取舍

第一版建议只做普通并行：

```text
一个 currentRun
一个配方
输入/输出/耗电按 parallel 倍数缩放
```

第二版再做样板请求队列：

```text
pendingRequests
但仍然一次只跑一个 currentRun
```

第三版再做跨配方并行：

```text
activeRuns
多个不同 RecipeRun 同时推进
资源预留
输出预留
总 EU/t 限制
```

不要在第一版同时做普通并行和跨配方并行。跨配方并行会显著提高配方调度复杂度，尤其是接入 ME 输入、ME 输出和样板总成后。

## 九、总成隔离

总成隔离指的是：多个输入、输出、ME、样板总成同时存在时，不应该随意互相混用资源。

典型问题包括：

- 输入总成 A 的物品被配方 B 拿走
- 样板总成 A 推送的任务使用了样板总成 B 的缓存
- ME 输出总成 A 的产物被塞到普通输出仓或另一个 ME 输出总成
- 跨配方并行时，多个任务同时抢同一批输入
- 多个总成服务不同频道或不同生产线，却被控制器当成一个大库存

GTM 里的对应概念是 `distinct bus`。它的核心不是新增一种库存，而是给配方 handler 分组：普通组可以合并，隔离组必须单独尝试。

### 1. 隔离的基本模型

建议给每个可参与配方的总成一个隔离标识：

```java
public record AssemblyGroupKey(
        String channel,
        boolean distinct,
        String id
) {}
```

更简单的第一版可以只做：

```java
public enum IsolationMode {
    SHARED,
    DISTINCT
}
```

含义：

```text
SHARED   = 可以和其他 shared 总成合并成一个大输入/输出池
DISTINCT = 这个总成单独作为一个配方候选池，不和其他 distinct 总成混用
```

部件接口：

```java
public interface IIsolatedPart extends IMultiblockPart {
    IsolationMode getIsolationMode();

    String getChannel();
}
```

`channel` 可以先返回空字符串。后续如果要做颜色频道、频率频道、样板频道，再扩展。

### 2. 为什么需要 distinct

假设有两个输入总线：

```text
输入总线 A: 1 铁粉
输入总线 B: 1 铜粉
```

如果两个输入总线是 shared，控制器看到的是：

```text
总输入池: 铁粉 + 铜粉
```

这适合普通机器。

但如果两个输入总线分别代表两条生产线，就应该 distinct：

```text
候选池 A: 只看输入总线 A
候选池 B: 只看输入总线 B
```

控制器应该分别尝试每个候选池，而不是把 A 和 B 混成一个库存。

### 3. handler 分组

控制器成型后，不要只维护：

```java
List<ItemInputPart> itemInputs;
```

建议额外维护分组：

```java
Map<AssemblyGroupKey, RecipeIOGroup> groups;
```

其中：

```java
public class RecipeIOGroup {
    public List<ItemInputPart> itemInputs = new ArrayList<>();
    public List<FluidInputHatchPart> fluidInputs = new ArrayList<>();
    public List<ItemOutputPart> itemOutputs = new ArrayList<>();
    public List<FluidOutputHatchPart> fluidOutputs = new ArrayList<>();
    public List<MEInputPart> meInputs = new ArrayList<>();
    public List<MEOutputPart> meOutputs = new ArrayList<>();
    public List<PatternProviderPart> patternProviders = new ArrayList<>();
}
```

第一版分组规则可以很简单：

```java
AssemblyGroupKey keyFor(IMultiblockPart part) {
    if (part instanceof IIsolatedPart isolated &&
            isolated.getIsolationMode() == IsolationMode.DISTINCT) {
        return new AssemblyGroupKey(isolated.getChannel(), true, part.getBlockPos().toShortString());
    }
    return new AssemblyGroupKey("", false, "shared");
}
```

### 4. 配方搜索时如何隔离

不要在全局输入池里直接找配方。应该按组尝试。

流程：

```text
先尝试每个 distinct 输入组
再尝试 shared 输入组
```

伪代码：

```java
RecipeSearchResult findRecipe() {
    for (RecipeIOGroup group : distinctGroups) {
        RecipeSearchResult result = tryFindRecipeInGroup(group);
        if (result.success()) {
            return result;
        }
    }

    return tryFindRecipeInGroup(sharedGroup);
}
```

`tryFindRecipeInGroup` 只允许读取该组里的输入。

输出也应该优先写回同组输出：

```text
输入来自 group A
-> 输出进入 group A 的输出总成
```

建议第一版不要 fallback 到 shared 输出。也就是 distinct 输入组必须有自己的可用输出。

### 5. ME 输入/输出总成隔离

ME 总成更需要隔离，因为它们可能连接同一个 AE 网络，但在多方块内部代表不同生产通道。

建议：

- `MEInputPart` 可以设为 shared 或 distinct
- `MEOutputPart` 可以设为 shared 或 distinct
- 如果 ME 输入是 distinct，它拉取到的缓存只给同组配方使用
- 如果 ME 输出是 distinct，只有同组配方能把产物塞进去

示例：

```text
ME 输入总成 A: channel = "line_a", distinct
ME 输出总成 A: channel = "line_a", distinct
样板总成 A: channel = "line_a", distinct

ME 输入总成 B: channel = "line_b", distinct
ME 输出总成 B: channel = "line_b", distinct
样板总成 B: channel = "line_b", distinct
```

这样 A 的样板请求不会使用 B 的输入，也不会把产物送到 B 的输出。

### 6. 样板总成隔离

样板总成建议默认 distinct。

原因：

- 每个样板总成保存自己的样板库存
- AE 的 `pushPattern` 是推给具体 provider 的
- 请求应该回到同一个 provider 所属的控制器组
- 多个样板总成混用会导致任务来源不清晰

推荐：

```java
public class PatternProviderPart implements IIsolatedPart {
    @Override
    public IsolationMode getIsolationMode() {
        return IsolationMode.DISTINCT;
    }
}
```

当 `pushPattern` 进入时，创建请求时带上来源组：

```java
public class RecipeRequest {
    public PatternProviderPart sourceProvider;
    public AssemblyGroupKey groupKey;
    public IPatternDetails pattern;
    public KeyCounter[] inputs;
}
```

控制器处理请求时只使用同组 IO：

```java
RecipeIOGroup group = groups.get(request.groupKey);
tryCreateRunFromPattern(request, group);
```

### 7. 普通并行下的隔离

普通并行仍然只跑一个配方，但需要限定它从哪个组取输入。

流程：

```text
选定 group
-> 在 group 内找配方
-> 在 group 内计算并行数
-> 从 group 内扣输入
-> 向 group 内写输出
```

并行计算不能跨组统计：

```java
int calculateParallel(Recipe recipe, RecipeIOGroup group, int machineLimit) {
    int limit = machineLimit;
    limit = Math.min(limit, group.getMaxParallelByItemInputs(recipe, limit));
    limit = Math.min(limit, group.getMaxParallelByFluidInputs(recipe, limit));
    limit = Math.min(limit, group.getMaxParallelByItemOutputs(recipe, limit));
    limit = Math.min(limit, group.getMaxParallelByFluidOutputs(recipe, limit));
    return limit;
}
```

### 8. 跨配方并行下的隔离

跨配方并行时，隔离必须升级成资源预留。

每个 `RecipeRun` 都要记录来源组：

```java
public class RecipeRun {
    public AssemblyGroupKey groupKey;
    public Recipe recipe;
    public int parallel;
    public List<ItemStack> reservedItems;
    public List<FluidStack> reservedFluids;
}
```

创建新任务时：

```java
RecipeIOGroup group = groups.get(groupKey);
ReservedResources reserved = reservationManager.tryReserve(group, recipe, parallel);
if (reserved == null) {
    return null;
}
```

预留必须按组隔离：

```text
group A 已预留的输入，只影响 group A
group B 不能使用 group A 的输入
shared group 只影响 shared group
```

如果允许 shared 输出作为 fallback，就必须额外处理 shared 输出的并发预留。第一版不建议允许 fallback。

### 9. 隔离与颜色频道

如果想做类似 GTM 染色仓室的体验，可以把颜色作为 channel。

规则可以是：

```text
未染色总成互通
红色总成只和红色总成互通
蓝色总成只和蓝色总成互通
样板总成默认独立
```

示例：

```java
String getChannel() {
    return color == -1 ? "default" : "color:" + color;
}
```

### 10. 推荐第一版实现

第一版建议实现三种隔离：

1. shared 组
2. distinct 组
3. 样板总成强制 distinct

暂时不要做：

- 颜色频道
- fallback 输出
- 跨组共享 ME 输出
- 一个样板总成同时驱动多个组

最小规则：

```text
普通输入/输出默认 shared
ME 输入/输出可配置 shared 或 distinct
样板总成默认 distinct
distinct 组内必须输入、输出都完整
配方从哪个组启动，产物就回哪个组
```

这样可以保证后续接入普通并行、样板请求队列、跨配方并行时不会出现资源串线。

## 十、催化剂槽位

催化剂槽位指的是：配方运行时需要某个物品、流体或模块作为条件，但它不按普通输入消耗，或者只在特定情况下损耗耐久。

它不应该直接混进普通输入总线。否则会出现两个问题：

- 普通配方输入可能误消耗催化剂
- 自动补货和样板请求会把催化剂当成普通材料

建议把催化剂作为单独能力处理。

### 1. 催化剂能力

在 `PartAbility` 里新增：

```java
CATALYST
```

对应部件：

```java
CatalystHatchPart
```

如果只需要控制器自带槽位，也可以不做独立仓室，而是在控制器里保存：

```java
ItemStackHandler catalystInventory = new ItemStackHandler(1);
```

但如果希望多方块结构上能插不同催化剂仓，推荐做成 part。

### 2. 催化剂和普通输入的区别

普通输入：

```text
配方需要 4 铁粉
-> 开始配方时扣除 4 铁粉
```

催化剂：

```text
配方需要 编程电路/模具/催化剂板
-> 开始配方时检查存在
-> 默认不消耗
-> 可选扣耐久或按概率消耗
```

因此催化剂 handler 应该和普通输入 handler 分开：

```java
public class RecipeIOGroup {
    public List<ItemInputPart> itemInputs = new ArrayList<>();
    public List<FluidInputHatchPart> fluidInputs = new ArrayList<>();
    public List<CatalystHatchPart> catalysts = new ArrayList<>();
}
```

配方检查流程：

```text
检查催化剂是否满足
-> 检查普通输入
-> 检查输出空间
-> 检查电量
-> 扣普通输入
-> 处理催化剂损耗
-> 启动配方
```

### 3. 配方定义

建议在配方数据里把催化剂和输入分开。

示例：

```java
public class Recipe {
    public List<ItemIngredient> itemInputs;
    public List<FluidIngredient> fluidInputs;
    public List<ItemIngredient> catalystItems;
    public List<FluidIngredient> catalystFluids;
    public List<ItemStack> itemOutputs;
    public List<FluidStack> fluidOutputs;
}
```

如果只需要物品催化剂，第一版只做：

```java
List<ItemIngredient> catalystItems;
```

### 4. 催化剂匹配

催化剂匹配应该只检查，不扣除。

```java
boolean hasCatalysts(Recipe recipe, RecipeIOGroup group) {
    for (ItemIngredient catalyst : recipe.catalystItems) {
        if (!group.hasCatalyst(catalyst)) {
            return false;
        }
    }
    return true;
}
```

如果催化剂有耐久或概率消耗：

```java
void applyCatalystDamage(Recipe recipe, RecipeIOGroup group) {
    for (CatalystUse use : recipe.catalystUses) {
        group.damageCatalyst(use.ingredient(), use.damage());
    }
}
```

第一版建议只做“不消耗催化剂”，等配方跑通后再做损耗。

### 5. 催化剂与并行

普通并行时，催化剂通常不乘并行数。

例如：

```text
催化剂: 1 模具
输入: 1 铁锭
输出: 1 铁板
并行: 16
```

运行时应是：

```text
检查 1 个模具
消耗 16 铁锭
输出 16 铁板
```

不要变成需要 16 个模具，除非这个配方明确要求催化剂按并行消耗。

建议给催化剂使用方式加枚举：

```java
public enum CatalystUseMode {
    NOT_CONSUMED,
    DAMAGE_ON_RUN,
    CHANCE_CONSUME,
    CONSUME_PER_RUN,
    CONSUME_PER_PARALLEL
}
```

第一版只实现：

```java
NOT_CONSUMED
```

### 6. 催化剂与样板总成

AE 样板通常描述“输入 -> 输出”。催化剂不应该由样板请求自动消耗，尤其是模具、电路、升级卡这类长期存在的物品。

推荐规则：

```text
样板只负责普通输入和输出
催化剂由机器本地槽位或催化剂仓提供
AE pushPattern 时不携带催化剂
控制器创建 RecipeRun 时检查催化剂
```

如果某个催化剂确实要由 AE 自动供应，那它就不应该建模为催化剂，而应该是普通输入。

### 7. 催化剂与总成隔离

催化剂也要进入隔离组。

规则：

```text
shared 催化剂仓 -> shared 组配方可用
distinct 催化剂仓 -> 只给同组配方可用
样板总成 distinct -> 只检查同组催化剂
```

示例：

```text
样板总成 A: channel = line_a
催化剂仓 A: channel = line_a
输入总成 A: channel = line_a
输出总成 A: channel = line_a
```

这样 A 的样板不会使用 B 线的催化剂。

### 8. GUI 与交互

催化剂槽位应该在 UI 上和普通输入槽明显分开。

建议：

- 控制器 UI 显示当前识别到的催化剂
- 催化剂仓 UI 只显示催化剂槽位
- 槽位过滤只允许合法催化剂
- 不允许管道或 ME 输出把普通产物塞进催化剂槽

槽位过滤：

```java
boolean mayPlace(ItemStack stack) {
    return CatalystRegistry.isCatalyst(stack);
}
```

如果催化剂是“任意编程电路”或“任意模具”，可以用 tag 或接口判断。

### 9. 推荐第一版实现

第一版建议：

- 只做物品催化剂
- 只做不消耗催化剂
- 催化剂槽位不参与 AE 样板输入
- 催化剂参与总成隔离
- 普通并行时催化剂不乘并行数

暂时不要做：

- 流体催化剂
- 催化剂耐久
- 概率消耗
- AE 自动补催化剂
- 多催化剂优先级

最小流程：

```text
找配方
-> 在同组 catalyst slots 中检查催化剂
-> 在同组普通输入中检查输入
-> 检查输出和电力
-> 扣普通输入
-> 启动配方
-> 完成后输出
```

## 十一、推荐实现顺序

建议按下面顺序落地：

1. 实现控制器、结构检测、`IMultiblockPart`
2. 实现电量输入
3. 实现普通物品输入输出
4. 实现普通流体输入输出
5. 实现催化剂槽位
6. 让控制器跑通一个测试配方
7. 实现 ME 输出总成，让产物能进入 AE
8. 实现 ME 输入总成，从 AE 拉取配置内容
9. 实现样板总成和 `ICraftingProvider`
10. 把样板请求接入控制器配方队列
11. 实现总成隔离
12. 实现普通并行
13. 最后再考虑跨配方并行

不要一开始就写样板总成。样板总成依赖结构、输入、输出、电量、配方调度全部可用后才好调试。

## 十二、关键取舍

不要直接移植 GTM 的完整系统。建议只借鉴下面几个边界：

- `PartAbility` 管结构允许哪些部件
- `IMultiblockPart` 管部件生命周期
- 控制器成型后统一聚合 handler
- ME 输入总成本质是输入部件
- ME 输出总成本质是输出部件
- 样板总成单独实现 AE 的 `ICraftingProvider`

这样可以避免把本地项目拖进 GTM 的完整注册、GUI、配方和同步体系里，同时保留需要的核心功能。
