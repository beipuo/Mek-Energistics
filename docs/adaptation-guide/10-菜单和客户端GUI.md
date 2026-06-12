# 10. 菜单和客户端 GUI

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
