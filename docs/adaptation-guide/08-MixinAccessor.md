# 8. Mixin accessor

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
