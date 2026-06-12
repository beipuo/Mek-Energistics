# 11. JEI

JEI 入口是 `client/jei/MekEnergisticsJeiPlugin.java`。

新增有配方机器时要补 recipe catalyst：

- Mek 原 factory type 机器：优先走 `registerCatalysts(registration, recipeType, FactoryType.X)`。
- 非 factory type 机器：走 `registerMachines(registration, RecipeViewerRecipeType.X, MeMekanismMachine.X)`。
- More Machine 扩展：优先放进对应 compat JEI 类，避免主 JEI 插件直接硬引用可选模组类。

如果机器是工厂变体，默认会被 `hiddenStacks()` 隐藏，只保留基础 ME 机器作为 JEI catalyst。新增特殊机器时确认它是否应隐藏。
