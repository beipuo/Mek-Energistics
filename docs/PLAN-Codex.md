# Mek-Energistics 重构计划

> Status: superseded by `docs/PLAN6.md`. The low-risk phases in this plan have largely landed; remaining aggressive ideas such as `MeAeSupportBase`, `MeMekanismMachine` Builder conversion, full `MeMekanismMachineBlockEntity` splitting, and optional compat class merging remain deferred.

## Summary

这份计划建议替换当前 `docs/plan.md`。现有计划找到了不少真实重复，但优先级偏激进：不应一开始就抽 `MeAeSupportBase` 大基类，也不应把 `MeMekanismMachine` 枚举 Builder 化放进核心阶段。重构目标应先降低 AE 支持路径的不一致性，再清理低风险重复。

核心方向：

- 先让普通配方机器、电动机器、高级电动机器使用同一套显式 support 接口。
- 先消除反射、重复能量包装、重复 pattern 生命周期，再考虑是否需要抽共享基类。
- 将客户端、菜单、compat 注册重复作为后续清理，不影响核心行为。
- 不改配方 JSON、blockstate JSON、已有注册名、NBT tag 名和世界存档兼容格式。

## Key Changes

### Phase 1: Stabilize AE Support API

- 在 `MeAeMachine` 中新增显式 `getRecipeAeSupport()` 默认方法，默认返回 `null`。
- 所有使用 `MeRecipeMachineAeSupport` 的机器显式 override 该方法。
- 删除 `MeAeMachine` 里通过反射查找 `aeSupport` 字段的逻辑。
- 保持 `MeFactoryAeMachine#getAeSupport()` 不变，不把 factory support 强行塞进同一个接口。
- 移除空接口 `MeExtraFactoryAeMachine`，所有引用直接改为 `MeFactoryAeMachine`。

Public interface impact:

```java
default MeRecipeMachineAeSupport<?> getRecipeAeSupport() {
    return null;
}
```

### Phase 2: Move Electric Machines Onto Existing Support

- 将 `MeElectricMachineBlockEntity` 和 `MeAdvancedElectricMachineBlockEntity` 改为持有 `MeRecipeMachineAeSupport<?> aeSupport`。
- 复用 support 的：
  - grid node lifecycle
  - pattern slots
  - pattern decode/update
  - smart pattern multiplication
  - output insertion wake/tick handling
  - NBT save/load for AE state and pattern slots
- 两个类只保留各自特有逻辑：
  - Mekanism 父类选择
  - recipe type / recipe viewer type
  - accessor 获取 input/output/chemical tank
  - `pushPatternInputs(...)`
  - cached recipe 创建
- 不在此阶段抽 `MeAeSupportBase`。先让旧式机器靠近现有 support 模式，减少一次性风险。

### Phase 3: Extract Shared Energy Wrappers

- 新增一个小型 energy helper 或 nested-free helper 类型，统一当前重复的：
  - AE-backed `MachineEnergyContainer`
  - recipe energy view
  - `withAeRecipeEnergy(...)`
- 目标是让 recipe machine 和 factory machine 都通过同一个能量读写实现调用 `MeNetworkEnergyHelper`。
- 保留 factory 的 `AeOutputMode`、terminal pattern inventory、terminal group 等特有状态在 `MeFactoryAeSupport` 内，不与 recipe support 合并。

### Phase 4: Pattern Input Cleanup

- 在 `MeFactoryPatternInput` 中新增：
  - `singleItem(KeyCounter)`：统一单物品解析。
  - `separate(KeyCounter[])`：统一 item / chemical / fluid 分离。
- 替换电动机器、工厂机器、化学机器中局部重复的 `getSingleItemInput` 和多输入拆分逻辑。
- 保持现有校验语义：多余输入、重复同类输入、空输入、超过 int 数量的 item 都失败。

### Phase 5: Defer Larger Structural Refactors

- 暂不拆 `MeMekanismMachineBlockEntity`。它是旧通用机器路径，应该等 electric/support 收敛后再单独评估。
- 暂不把 `MeMekanismMachine` 全量 Builder 化。枚举构造函数确实难读，但改动范围覆盖注册、语言键、compat 分类，风险高于当前收益。
- 暂不合并 compat MenuTypes / ClientScreens 类。它们重复但稳定，优先级低。
- 可做低风险清理：
  - `quickMoveStack` 提取公共 helper。
  - `MeGuiConfigurableTile` 增加默认 `drawForegroundText`。
  - 提取标准 energy/progress GUI widget helper。
  - 简化 `ModMenuTypes#getMachineContainer()` 为分类 helper 或 map，但不改变注册对象。

## Test Plan

每个 phase 后至少运行：

```powershell
.\gradlew.bat compileJava --no-daemon --no-problems-report --no-configuration-cache
```

Phase 2 和 Phase 3 后额外运行：

```powershell
.\gradlew.bat build --no-daemon --no-problems-report --no-configuration-cache
```

游戏内验证场景：

- ME 电动机器：富集、粉碎、冶炼 encoded pattern 能进入输入槽，产物返回 AE。
- ME 高级电动机器：压缩、净化、注入 item + chemical pattern 能正确消耗两类输入。
- AE 能量耗尽时机器暂停，网络恢复能量后继续。
- 输出网络满时产物留在机器输出槽，网络可插入后 tick 唤醒并继续导出。
- pattern 终端能看到机器 pattern，插拔 pattern 后刷新。
- smart pattern multiplication 开关、pending pattern 保存/读档后行为一致。
- 旧世界已有机器不会丢 pattern slot、priority、AE output mode、custom terminal name。

## Assumptions

- 保持当前 NeoForge / Mekanism / AE2 版本不变。
- 不改方块 ID、菜单 ID、block entity type、recipe type、NBT tag 名。
- 重构以行为等价为第一目标，减少行数是副目标。
- `MeRecipeMachineAeSupport` 与 `MeFactoryAeSupport` 现在先保持两个独立类；只有当 Phase 1-4 后仍有清晰重复，再考虑小范围共享组件，而不是直接抽大基类。
