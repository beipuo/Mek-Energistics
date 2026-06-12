# Mek-Energistics 后续重构计划 v3

## Summary

当前 `refactor/factory-compat-helpers` 已完成 PLAN2 的 pattern decode hardening、旧通用机器 helper、factory compat bridge 清理，并已通过 `compileJava`、`build`、`runClient`。下一步不再扩大 backend 架构重构，先收口剩余两个用户可见 `decodePattern` 路径，再做一轮很窄的 factory 默认委托清理。

继续保持：不改方块 ID、菜单 ID、注册名、NBT tag、JSON、世界兼容格式；不抽 `MeAeSupportBase`，不 Builder 化 `MeMekanismMachine`，不合并 optional compat 注册/客户端类。

## Key Changes

- **Phase 1: Checkpoint current branch**
  - 基线使用 `575da13 Simplify factory compat owner bridges`。
  - `docs/PLAN2.md` 继续保持本地未跟踪。
  - 新建分支 `refactor/pattern-ui-decode-hardening`。

- **Phase 2: Harden user-facing pattern decode**
  - 将 `MeMemoryCardSettings` 导入 pattern 时的 `PatternDetailsHelper.decodePattern(...)` 改为安全 decode。
  - memory card 导入遇到坏 pattern 时跳过该 pattern，不中断整次导入；blank pattern 扣除逻辑只针对成功恢复的 pattern。
  - 将 `MePatternWindowOverlay` 的 pattern 输出预览 decode 改为安全 decode；坏 pattern 只显示原 pattern 物品图标或空预览，不让客户端 GUI 崩溃。
  - 不改变 terminal pattern inventory、memory card 数据格式、AE component key 或导入来源校验。

- **Phase 3: External factory default delegates**
  - 新建分支 `refactor/external-factory-defaults`，从 Phase 2 通过后的提交创建。
  - 在 `MeExternalFactorySupport.Owner` 中增加窄默认委托：`getAvailablePatterns()`、`getPatternPriority()`、`getGridNode(...)`，均委托 `getAeSupport()`。
  - 删除 EME / MEKE / MekMM factory 类中完全等价的一行 override；保留所有需要调用 `super` 的 lifecycle/save/load/addContainerTrackers override。
  - 不改 `MeExtraFactoryBridge`、`MeAdvancedFactorySupport` 的 optional 加载边界，不合并 compat 类。

- **Deferred**
  - 暂不处理 UI widget helper；剩余 GUI 能量条和进度条坐标/supplier 差异较多，不适合批量抽取。
  - 暂不处理 `MeMemoryCardSettings` 的功能扩展，例如导入优先级、custom terminal name 或 AE output mode。
  - 暂不拆 `MeMekanismMachineBlockEntity` 的 recipe 执行逻辑。
  - 暂不处理 `ClientSetup` event bus deprecation。

## Test Plan

- 每个 phase 后运行：
  ```powershell
  $env:JAVA_HOME='C:\GRAALVM\graalvm-jdk-21.0.6+8.1'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat compileJava --no-daemon --no-problems-report --no-configuration-cache
  ```

- 完成后运行：
  ```powershell
  $env:JAVA_HOME='C:\GRAALVM\graalvm-jdk-21.0.6+8.1'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat build --no-daemon --no-problems-report --no-configuration-cache
  $env:JAVA_HOME='C:\GRAALVM\graalvm-jdk-21.0.6+8.1'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat runClient --no-daemon --no-problems-report --no-configuration-cache
  ```

- 静态验收：
  - `rg -n "PatternDetailsHelper\.decodePattern" src\main\java\com\beipuo\mekenergistics` 只剩 `MePatternDecodeHelper` 内部，或有明确注释说明的刻意例外。
  - `rg -n "getAvailablePatterns\(\)|getPatternPriority\(\)|getGridNode\(Direction" src\main\java\com\beipuo\mekenergistics\blockentity\compat` 明显减少纯委托 override。
  - `git diff --stat` 只集中在 decode helper、memory card、overlay、factory owner/default delegate 相关文件，不扩散到 registry、JSON、menu、machine enum。

- 游戏内抽测：
  - 打开普通 ME 机器、factory、MekMM/EME factory 的 pattern window，坏 pattern 不导致客户端崩溃。
  - 用 memory card 导入含坏 pattern 的 pattern 设置：有效 pattern 正常恢复，坏 pattern 被跳过，blank pattern 数量只按成功恢复项扣除。
  - pattern terminal 仍显示有效 pattern，priority/custom name/smart multiplication 不受影响。
  - factory GUI 打开、AE output mode、pattern provider、能量消耗和输出导出行为保持不变。

## Assumptions

- 当前未跟踪的 `extendedae_plus.mixins.json`、`libs/` 继续保持本地未提交。
- `docs/PLAN2.md` 是本地计划文件，继续不混入代码提交。
- 本轮目标是收口剩余崩溃入口和删除纯委托重复，不做新功能和大结构重写。
