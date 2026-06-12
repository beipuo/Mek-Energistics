# 20. 新增其它模组 compat 的模板

适配新模组时，先照 Mekanism More Machine 和 Mekanism Extras 的形状建目录。不要把可选模组类散落到主注册类里。

推荐目录：

```text
src/main/java/com/beipuo/mekenergistics/compat/<modid>/
src/main/java/com/beipuo/mekenergistics/blockentity/compat/<modid>/
src/main/java/com/beipuo/mekenergistics/menu/compat/<modid>/
src/main/java/com/beipuo/mekenergistics/client/compat/<modid>/
src/main/java/com/beipuo/mekenergistics/client/jei/compat/
```

服务端 compat 门面至少提供这些方法：

```java
public final class SomeModCompat {
    public static TileEntityTypeRegistryObject<? extends TileEntityMekanism> registerBaseMachine(
            MeMekanismMachine machine, MachineFactoryRegistrar registrar);

    public static TileEntityTypeRegistryObject<? extends TileEntityMekanism> registerFactoryMachine(
            MeMekanismMachine machine, MachineFactoryRegistrar registrar);

    public static <TILE extends TileEntityMekanism> BlockTypeTile<TILE> createBaseBlockType(
            MeMekanismMachine machine, TileEntityTypeRegistryObject<TILE> tileType);

    public static <TILE extends TileEntityMekanism> BlockTypeTile<TILE> createFactoryBlockType(
            MeMekanismMachine machine, TileEntityTypeRegistryObject<TILE> tileType);

    public static MeMekanismMachine getFactoryTarget(BlockState state);

    public static void registerGridNodeHost(
            RegisterCapabilitiesEvent event, TileEntityTypeRegistryObject<? extends TileEntityMekanism> holder);
}
```

不是每个模组都需要全部方法，但保留这个形状能让 `ModBlockEntities`、`ModBlockTypes`、`MeInstallerTargetResolver` 的分派很干净。

接入主线的位置：

- `OptionalCompatClasses`: 增加 `hasSomeMod()` 和必要的 class resource 探测。
- `MeMekanismMachine`: 增加 base machine、factory machine、`requiredModId`、type name 字段或构造路径。
- `ModBlockEntities`: 在 `registerMachine` 和 `registerCapabilities` 中分派到 compat 门面。
- `ModBlockTypes`: 在 `createMachineBlockType` 中分派到 compat 的 block type builder。
- `ModMenuTypes`: 增加 optional holder 或实际 container 注册。
- `ClientSetup`: 只在目标模组加载时调用 client compat screen 注册。
- `MekEnergisticsJeiPlugin`: 只调用 JEI compat 门面，不直接引用目标模组 recipe viewer class。
- `MeInstallerTargetResolver`: 在目标模组可用时调用 compat 的 target resolver。
