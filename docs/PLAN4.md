# Mek-Energistics 后续重构计划 v4：Factory Core Helper 收口

## Summary

当前 `refactor/external-factory-defaults` 已完成 PLAN3：用户可见 `decodePattern` 路径硬化、external factory 默认委托清理，并通过 `compileJava`、`build`、`runClient`。下一步不扩大 backend 架构，不抽 `MeAeSupportBase`，不碰注册、JSON、NBT 或世界兼容格式；优先把普通 factory 本体补齐默认委托，并把 factory 生命周期/存档重复方法体收敛到窄 helper。

基线使用 `94d2c93 Refactor external factory default delegates`，新分支建议：`refactor/factory-core-helpers`。

## Key Changes

- **Checkpoint 与文档**
  - 保留当前未跟踪的 `docs/PLAN2.md`、`extendedae_plus.mixins.json`、`libs/` 不纳入本轮。
  - 如需落盘本计划，单独新增 `docs/PLAN4.md` 并独立提交，不混入代码提交。

- **普通 factory 默认委托**
  - 在 `MeFactoryAeMachine` 增加与 external factory 一致的默认委托：
    - `getAvailablePatterns()` -> `getAeSupport().getAvailablePatterns()`
    - `getPatternPriority()` -> `getAeSupport().getPatternPriority()`
    - `getGridNode(Direction)` -> `getMainNode().getNode()`
  - 删除 4 个普通 factory 类中完全等价的 `getAvailablePatterns`、`getPatternPriority`、`getGridNode`、`setOwner` override。
  - 保留 `pushPattern`、`createNewCachedRecipe`、`onUpdateServer` 等机器差异逻辑，不合并配方输入行为。

- **普通 factory lifecycle/save-load helper**
  - 在 `MeFactoryAeSupport` 增加窄 helper：
    - pattern slot inventory 合并 helper
    - first tick create helper
    - save/load support + pattern slots helper
  - 4 个普通 factory 类仍保留 `clearRemoved`、`setRemoved`、`onChunkUnloaded`、`addContainerTrackers`、`saveAdditional`、`loadAdditional` override，因为必须调用各自 `super`；方法体改为调用 helper。
  - 不改 NBT tag 名、pattern slot数量、slot index、AE output mode、smart pattern multiplication 或输出导出语义。

- **Compat bridge 小清理**
  - 只删除 `MeExtraFactoryBridge` 中已无调用的纯转发方法和对应 import，例如旧的 `gridNode` / `getAvailablePatterns` 包装。
  - 不合并 `MeExternalFactorySupport`、`MeExtraFactoryBridge`、`MeAdvancedFactorySupport`，不改变 optional compat package、注册入口或加载条件。
  - 暂不批量重写 compat factory lifecycle override；它们已使用 shared helper，当前收益低于风险。

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
  - `rg -n "public List<IPatternDetails> getAvailablePatterns|public int getPatternPriority|getGridNode\\(Direction|void setOwner\\(" src\main\java\com\beipuo\mekenergistics\blockentity\factory` 不再列出纯委托 override。
  - `rg -n "GridHelper\\.onFirstTick|aeSupport\\.save\\(|aeSupport\\.saveSlots|aeSupport\\.load\\(|aeSupport\\.loadSlots" src\main\java\com\beipuo\mekenergistics\blockentity\factory` 显示普通 factory 已改为 helper 调用或无重复。
  - `rg -n "gridNode\\(|getAvailablePatterns\\(MeFactoryAeSupport" src\main\java\com\beipuo\mekenergistics\blockentity\compat\meke\factory` 不再显示无用桥接方法。
  - `git diff --stat` 主要集中在 `MeFactoryAeMachine`、`MeFactoryAeSupport`、4 个普通 factory 类和 `MeExtraFactoryBridge`。

- 游戏内抽测：
  - 打开普通 factory GUI，确认 pattern terminal、priority、custom terminal name、AE output mode、smart multiplication 正常。
  - 插入 pattern 后 provider 刷新；shift/导入行为不变。
  - AE 能量不足暂停、恢复后继续；输出网络满时产物留在输出槽，网络可插入后继续导出。
  - 保存退出再进世界，pattern slots、AE output mode、terminal name、smart multiplication 状态保持。

## Assumptions

- 当前 `refactor/external-factory-defaults` 是新基线，HEAD 为 `94d2c93`。
- 本轮只清普通 factory 本体和明显无用 bridge，不处理 recipe 机器、legacy 通用机器或 UI widget。
- 不改方块 ID、菜单 ID、注册名、NBT tag、recipe JSON、blockstate JSON、世界兼容格式。
- 暂不抽 `MeAeSupportBase`，不 Builder 化 `MeMekanismMachine`，不合并 optional compat 注册/客户端类，不处理 `ClientSetup` event bus deprecation。
