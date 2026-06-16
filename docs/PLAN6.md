# Mek-Energistics 总重构收尾计划：稳定优先，一次到底

## Summary

当前基线为 `refactor/recipe-support-init-hardening`，HEAD `db934e5 Harden recipe AE support initialization`。已完成 AE support 默认委托、UI/Menu helper、pattern decode hardening、legacy/factory helper、energy wrapper、recipe/factory 构造顺序硬化。后续总目标不再盲目扩大抽象，而是按“剩余风险清零 -> 旧路径收口 -> 可维护性收尾 -> 最终验证”的顺序结束本轮重构。

全程不改方块 ID、菜单 ID、注册名、NBT tag、recipe JSON、blockstate JSON、世界兼容格式；不合并 optional compat 注册/客户端类；不 Builder 化 `MeMekanismMachine`；不抽大而全的 `MeAeSupportBase`，除非最后一阶段有测试证明收益大于风险。

## Key Changes

- **Phase 0: Baseline checkpoint**
  - 保留未跟踪的 `docs/PLAN2.md`、`extendedae_plus.mixins.json`、`libs/` 不纳入代码提交。
  - 新建 `docs/PLAN6.md` 保存本总计划，单独提交。
  - 从 `db934e5` 创建下一分支：`refactor/final-hardening-cleanup`。

- **Phase 1: Runtime hardening leftovers**
  - 只处理当前仍有构造顺序或崩溃风险的遗留边界：`MeMekmmItemChemicalMachineSupport`、`MePlantingStationBlockEntity`、`MeReplicatorBlockEntity`。
  - 若这些类存在 super 构造期访问 support 风险，改为与 recipe support 一致的 lazy 初始化；若无风险，只删除纯 owner/grid 转发或保留并在提交说明列为例外。
  - 不迁移它们到 `MeRecipeMachineAeSupport` 公共路径，不改 optional MekMM 加载边界。

- **Phase 2: Legacy generic machine final cleanup**
  - 继续保留 `MeMekanismMachineBlockEntity` 类名和数据格式，只提取无行为变化的窄 helper。
  - 目标仅限 AE lifecycle、owner/grid、pattern update、save/load 的重复收口；不拆 recipe 执行、不重排 slot index、不替换 block entity type。
  - 如果 helper 会引入构造顺序风险，停止该小步并保留现状。

- **Phase 3: Factory/compat consistency pass**
  - 检查普通 factory、EME/MEKE/MekMM factory 的 lifecycle、pattern slot 合并、AE output mode 保存是否都通过现有 helper。
  - 只删除仍无调用的 bridge 方法和等价一行委托。
  - 不合并 `MeExternalFactorySupport`、`MeExtraFactoryBridge`、`MeAdvancedFactorySupport`，不折叠 compat package。

- **Phase 4: UI/Menu low-risk polish**
  - 只清理完全重复且坐标一致的 screen widget 组装；坐标、supplier、tooltip、recipe viewer category 不一致的 GUI 保持原样。
  - 不再改 `ModMenuTypes#getMachineContainer()`，除非编译或搜索发现新的重复映射。
  - 不处理 compat MenuTypes / ClientScreens 合并。

- **Phase 5: Registry and enum audit, no rewrite**
  - 审计 `MeMekanismMachine` 枚举构造函数、factory lookup、installer upgrade path，记录可读性问题。
  - 本轮只允许增加静态校验或小型 lookup helper；禁止 Builder 化枚举，禁止改 enum 常量参数顺序或 registry name。
  - 若发现真实 bug，单独开 fix 分支，不混入结构重构。

- **Phase 6: Deprecation and documentation cleanup**
  - 单独分支处理 `ClientSetup` 的 `EventBusSubscriber(bus = Bus.MOD)` deprecation，先核对当前 NeoForge 1.21.1 推荐注册方式。
  - 只迁移事件注册写法，不碰 screen 注册内容。
  - 更新 docs：标记 `plan.md` / `PLAN-Codex.md` 中已完成或废弃的激进项，保留最终收敛路线。

- **Phase 7: Final validation branch**
  - 合并所有小分支后创建 `refactor/final-validation`。
  - 只做编译、运行、静态验收、旧世界加载抽测；不再做新重构。
  - 若发现问题，回到对应最小分支修复，不在 validation 分支堆修补。

## Public/Internal API Impact

- 允许新增 internal-only helper，位置优先放在 `blockentity.support` 或对应 compat support 边界。
- 不新增玩家可见配置、命令、菜单、NBT、数据包格式或 registry entry。
- `MeAeMachine`、`MeFactoryAeMachine` 只能继续承载安全默认委托；不得变成跨 recipe/factory/legacy 的大基类替代品。
- 所有 optional compat 类名、注册入口、加载条件保持不变。

## Test Plan

- 每个 phase 后运行：
  ```powershell
  $env:JAVA_HOME='C:\GRAALVM\graalvm-jdk-21.0.6+8.1'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat compileJava --no-daemon --no-problems-report --no-configuration-cache
  ```

- 每个 backend phase 完成后运行：
  ```powershell
  $env:JAVA_HOME='C:\GRAALVM\graalvm-jdk-21.0.6+8.1'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat build --no-daemon --no-problems-report --no-configuration-cache
  ```

- 每个 runtime-hardening phase 和最终 validation 后运行：
  ```powershell
  $env:JAVA_HOME='C:\GRAALVM\graalvm-jdk-21.0.6+8.1'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat runClient --no-daemon --no-problems-report --no-configuration-cache
  ```

- 静态验收：
  - `PatternDetailsHelper.decodePattern` 只存在于 safe decode helper 内部。
  - `private final MeRecipeMachineAeSupport` 只允许存在于明确无构造风险的独立 helper，且提交说明列出原因。
  - `git diff --stat` 不扩散到 registry、JSON、menu ID、block ID、NBT key。
  - 搜索确认 `PatternPriority`、`PatternTerminalName`、`AeOutputMode`、pattern slot tag 名不变。

- 游戏内抽测：
  - 普通 ME 机器、chemical 机器、普通 factory、EME/MEKE/MekMM factory 均可放置、打开 GUI、插入 pattern。
  - pattern terminal 显示、priority、custom terminal name、smart multiplication 保存读档正常。
  - AE 能量不足暂停，恢复后继续；输出网络满时产物留在输出槽。
  - 插入坏 pattern 不崩溃；memory card 导入坏 pattern 只跳过坏项。
  - 旧世界加载不丢 pattern slots、AE output mode、terminal name 或 pending smart pattern 状态。

## Assumptions

- 当前 `db934e5` 是新的稳定基线。
- 下一步目标是结束本轮重构，而不是开启新架构。
- 任何会改变世界兼容、optional compat 边界、注册名或 NBT 的想法都必须移出本轮。
- `MeAeSupportBase`、`MeMekanismMachine` Builder、完整拆分 `MeMekanismMachineBlockEntity` 只作为未来大版本候选，不进入本次“一次到底”的收尾计划。
