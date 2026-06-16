# Mek-Energistics 后续重构计划 v5：Recipe AE Support 构造顺序硬化

## Summary

当前 `refactor/factory-core-helpers` 已完成普通 factory helper 收口，并通过 `compileJava`、`build`、`runClient`。下一步不扩大架构，不抽 `MeAeSupportBase`，优先修掉普通 recipe/chemical 机器中与 factory 同类的潜在构造顺序风险：`getInitialInventory` 在 Mekanism `super` 构造期间可能访问尚未初始化的 `aeSupport` 字段。随后删除这些机器里重复的 owner/grid 纯委托。

基线使用 `8722417 Refactor factory core helpers`，新分支建议：`refactor/recipe-support-init-hardening`。

## Key Changes

- **Checkpoint 与文档**
  - 保留未跟踪的 `docs/PLAN2.md`、`extendedae_plus.mixins.json`、`libs/` 不纳入本轮。
  - 如需落盘本计划，单独新增 `docs/PLAN5.md` 并独立提交，不混入代码提交。

- **Recipe support 构造顺序硬化**
  - 给 `MeRecipeMachineAeSupport` 增加 `withPatternSlots(IInventorySlotHolder original)` helper，行为与现有每类手写 `original + patternSlots` 完全一致。
  - 将直接持有 `MeRecipeMachineAeSupport` 的普通 recipe/chemical/EME/MekMM 普通机器从 `private final ... aeSupport = new ...` 改为 lazy 初始化：
    - 字段改为非 final nullable。
    - `getRecipeAeSupport()` 中若为 `null` 则创建。
    - 构造函数主体末尾调用 `getRecipeAeSupport()` 作为显式初始化点。
    - `getInitialInventory(...)`、energy wrapper、lifecycle/save-load 等调用统一改用 `getRecipeAeSupport()` 或本地 `support` 变量，避免 super 构造期读空字段。
  - 不改 `MeMekmmItemChemicalMachineSupport` 这类独立 helper 的构造策略，除非编译或运行证明存在同类 super-constructor 风险。

- **MeAeMachine 默认 owner/grid 委托**
  - 在 `MeAeMachine` 增加安全默认委托：
    - `setOwner(ServerPlayer)`：若实例是 `TileEntityMekanism`，调用 `MeOwnerHelper.setOwner(tile, getMainNode(), player)`；否则设置 main node owning player。
    - `getActionableNode()` 与 `getGridNode(Direction)`：返回 `getMainNode().getNode()`。
  - 删除 recipe/chemical/EME/MekMM 普通机器中完全等价的 `setOwner`、`getActionableNode`、`getGridNode` override。
  - 保留所有 `getMainNode()` override，因为它们仍是 support 入口；保留 lifecycle/save/load override，因为必须调用对应 `super`。

- **Scope Guard**
  - 不改方块 ID、菜单 ID、注册名、NBT tag、recipe JSON、blockstate JSON、世界兼容格式。
  - 不合并 recipe/factory support，不抽 `MeAeSupportBase`，不拆 `MeMekanismMachineBlockEntity`。
  - 不处理 UI widget、compat MenuTypes/ClientScreens、`ClientSetup` event bus deprecation。

## Test Plan

- 每个小阶段后运行：
  ```powershell
  $env:JAVA_HOME='C:\GRAALVM\graalvm-jdk-21.0.6+8.1'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat compileJava --no-daemon --no-problems-report --no-configuration-cache
  ```

- 完成后运行：
  ```powershell
  $env:JAVA_HOME='C:\GRAALVM\graalvm-jdk-21.0.6+8.1'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat build --no-daemon --no-problems-report --no-configuration-cache
  $env:JAVA_HOME='C:\GRAALVM\graalvm-jdk-21.0.6+8.1'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat runClient --no-daemon --no-problems-report --no-configuration-cache
  ```

- 静态验收：
  - `rg -n "private final MeRecipeMachineAeSupport" src\main\java\com\beipuo\mekenergistics\blockentity\machine src\main\java\com\beipuo\mekenergistics\blockentity\compat\eme\machine src\main\java\com\beipuo\mekenergistics\blockentity\compat\mekmm\machine` 不再列出直接 block entity 字段。
  - `rg -n "this\.aeSupport\.getPatternSlots\(\)" src\main\java\com\beipuo\mekenergistics\blockentity\machine src\main\java\com\beipuo\mekenergistics\blockentity\compat` 不再列出 super 构造期可触发路径。
  - `rg -n "setOwner\(ServerPlayer|getGridNode\(Direction|getActionableNode\(\)" src\main\java\com\beipuo\mekenergistics\blockentity\machine src\main\java\com\beipuo\mekenergistics\blockentity\compat\eme\machine src\main\java\com\beipuo\mekenergistics\blockentity\compat\mekmm\machine` 明显减少纯委托 override。
  - `git diff --stat` 主要集中在 `MeAeMachine`、`MeRecipeMachineAeSupport` 和普通 recipe/chemical machine 类。

- 游戏内抽测：
  - 放置并加载普通 ME 机器、chemical 机器、EME/MekMM 普通机器，确认不再出现 block entity 创建期 NPE。
  - 打开 GUI，确认 pattern slot、priority、custom terminal name、AE output mode、smart multiplication 正常。
  - 保存退出再进世界，pattern slot、AE output mode、terminal name、smart multiplication 状态保持。
  - 插入 pattern 后 provider 刷新；AE 能量不足暂停、恢复后继续；输出网络满时产物保留在输出槽。

## Assumptions

- 当前 `refactor/factory-core-helpers` 是新基线，HEAD 为 `8722417`。
- 本轮目标是 runtime hardening + 纯委托清理，不做新功能。
- 如果某些 recipe machine 因 superclass 构造顺序或泛型签名无法安全 lazy 化，保留该类原实现并在提交说明中列为例外，不强行抽象。
