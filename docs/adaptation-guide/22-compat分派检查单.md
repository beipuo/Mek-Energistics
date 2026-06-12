# 22. compat 分派检查单

新增一个模组 compat 后，逐项检查：

- enum 机器在目标模组未安装时 `isAvailable()` 返回 false。
- 未安装目标模组时，公共静态初始化不会加载目标模组类。
- `ModBlocks`、`ModItems`、`ModBlockEntities` 只注册 available 机器。
- `registerMachine` 能给每台机器选到正确 BlockEntity。
- `registerCapabilities` 只给实现 AE host 的 BlockEntity 注册 `IN_WORLD_GRID_NODE_HOST`。
- `createBaseBlockType` / `createFactoryBlockType` 保留目标模组原本的 GUI、shape、tier、side config 和升级支持。
- `getFactoryTarget` 能从原模组机器 block state 找到对应 `MeMekanismMachine`。
- 菜单和客户端 screen 注册只在目标模组存在时运行。
- JEI compat 不在主类中直接引用目标模组客户端类。
- 安装器能处理 base -> basic factory、tier -> next tier、扩展 tier 链。
- 资源文件覆盖所有 available 机器，不覆盖 unavailable 机器。
