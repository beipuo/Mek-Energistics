# 15. 新增 Mek 机器的推荐步骤

按这个顺序做，出错时定位最快：

1. 在 `MeMekanismMachine` 添加 enum，确认 `registryName()`、`translationKey()`、`isAvailable()`。
2. 判断是否能复用已有 BlockEntity 模板；不能复用就新增最小专用 BlockEntity。
3. 如需访问 Mek 私有字段，新增 mixin accessor 并注册到 `mekenergistics.mixins.json`。
4. 在 `ModBlockEntities` 选择 `ae(...)` 或 `noAe(...)`，并确认 AE capability 会注册。
5. 在 BlockEntity 中接入 pattern slots、AE grid node、AE-backed energy、`pushPattern`、输出回网、NBT 保存和 container tracker。
6. 在 `ModBlockTypes` 补 shape、side config、升级链和特殊 attribute。
7. 在 `ModBlockTypes.sideConfigFor` 确认 `TransmissionType` 数组与机器能力匹配。
8. 在 `ModMenuTypes` 选择或新增 container，并在 `ClientSetup` 注册 screen。
9. 在 JEI 插件或 compat JEI 类补 catalyst。
10. 检查 `MeInstallerTargetResolver` 能否从原机器升级到 ME 机器。
11. 补 blockstate、model、recipe、lang。
12. 运行 `compileJava`，再进游戏验证样板下单、加工、回网和升级。
