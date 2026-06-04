package com.beipuo.mekenergistics.compat.mekmm;

import com.beipuo.mekenergistics.block.attribute.MeUpgradeableAttribute;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeMoreMachineItemStackToItemStackFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MePlantingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeRecyclingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeReplicatingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeStampingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.machine.MeLatheBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.machine.MeRecyclerBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.machine.MeRollingMillBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.machine.MeStamperBlockEntity;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.registry.ModBlockEntities;
import com.beipuo.mekenergistics.registry.ModBlocks;
import com.beipuo.mekenergistics.registry.ModMenuTypes;
import com.beipuo.mekenergistics.registry.machine.MachineFactoryRegistrar;
import com.jerry.mekmm.common.block.attribute.MoreMachineAttributeFactoryType;
import com.jerry.mekmm.common.content.blocktype.MoreMachineFactory;
import com.jerry.mekmm.common.content.blocktype.MoreMachineFactoryType;
import com.jerry.mekmm.common.registries.MoreMachineContainerTypes;
import java.util.Locale;
import mekanism.api.Upgrade;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeStateFacing;
import mekanism.common.block.attribute.AttributeTier;
import mekanism.common.block.attribute.Attributes;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

public final class MekanismMoreMachineBaseCompat {
    private MekanismMoreMachineBaseCompat() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static TileEntityTypeRegistryObject<? extends TileEntityMekanism> registerBaseMachine(
            MeMekanismMachine machine, MachineFactoryRegistrar registrar) {
        TileEntityTypeRegistryObject<?> registered = switch (machine) {
            case RECYCLER -> registrar.register(machine, MeRecyclerBlockEntity::new);
            case CNC_STAMPER -> registrar.register(machine, MeStamperBlockEntity::new);
            case CNC_LATHE -> registrar.register(machine, MeLatheBlockEntity::new);
            case CNC_ROLLING_MILL -> registrar.register(machine, MeRollingMillBlockEntity::new);
            default -> throw new IllegalStateException("Unknown MEKMM base machine: " + machine);
        };
        return (TileEntityTypeRegistryObject) registered;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static TileEntityTypeRegistryObject<? extends TileEntityMekanism> registerFactoryMachine(
            MeMekanismMachine machine, MachineFactoryRegistrar registrar) {
        TileEntityTypeRegistryObject<?> registered = switch (moreMachineFactoryType(machine)) {
            case RECYCLING -> registrar.register(machine, MeRecyclingFactoryBlockEntity::new);
            case PLANTING_STATION -> registrar.register(machine, MePlantingFactoryBlockEntity::new);
            case CNC_STAMPING -> registrar.register(machine, MeStampingFactoryBlockEntity::new);
            case CNC_LATHING, CNC_ROLLING_MILL -> registrar.register(machine, MeMoreMachineItemStackToItemStackFactoryBlockEntity::new);
            case REPLICATING -> registrar.register(machine, MeReplicatingFactoryBlockEntity::new);
        };
        return (TileEntityTypeRegistryObject) registered;
    }

    public static <TILE extends TileEntityMekanism> BlockTypeTile<TILE> createFactoryBlockType(
            MeMekanismMachine machine, TileEntityTypeRegistryObject<TILE> tileType) {
        MoreMachineFactory.MoreMachineFactoryBuilder<?, ?, ?> builder =
                MoreMachineFactory.MoreMachineFactoryBuilder.createMoreMachineFactory(() -> tileType, moreMachineFactoryType(machine), machine.factoryTier());
        builder.replace(new mekanism.common.block.attribute.AttributeGui(() -> ModMenuTypes.ME_MORE_MACHINE_FACTORY, null));
        MeMekanismMachine upgradeTarget = machine.getNextFactory();
        if (upgradeTarget != null) {
            builder.replace(new MeUpgradeableAttribute(() -> ModBlocks.getMachineBlock(upgradeTarget).get()));
        }
        @SuppressWarnings("unchecked")
        BlockTypeTile<TILE> built = (BlockTypeTile<TILE>) builder.build();
        return built;
    }

    public static <TILE extends TileEntityMekanism> BlockTypeTile<TILE> createBaseBlockType(
            MeMekanismMachine machine, TileEntityTypeRegistryObject<TILE> tileType) {
        var builder = BlockTypeTile.BlockTileBuilder
                .createBlock(() -> tileType, machine::translationKey)
                .withGui(() -> baseContainer(machine))
                .withEnergyConfig(machine.energyUsage(), machine.energyStorage())
                .with(new AttributeStateFacing(), Attributes.ACTIVE_LIGHT, Attributes.INVENTORY, Attributes.REDSTONE, Attributes.SECURITY, Attributes.COMPARATOR)
                .withSideConfig(new TransmissionType[] {TransmissionType.ITEM, TransmissionType.ENERGY})
                .withSupportedUpgrades(Upgrade.SPEED, Upgrade.ENERGY)
                .with(new MoreMachineAttributeFactoryType(moreMachineFactoryType(machine)));
        MeMekanismMachine upgradeTarget = machine.getBasicFactory();
        if (upgradeTarget != null) {
            builder.with(new MeUpgradeableAttribute(() -> ModBlocks.getMachineBlock(upgradeTarget).get()));
        }
        return builder.build();
    }

    public static MoreMachineFactoryType moreMachineFactoryType(MeMekanismMachine machine) {
        String typeName = machine.moreMachineFactoryTypeName() != null ? machine.moreMachineFactoryTypeName() : machine.moreMachineBaseTypeName();
        String name = typeName.toUpperCase(Locale.ROOT);
        return switch (name) {
            case "RECYCLING" -> MoreMachineFactoryType.RECYCLING;
            case "PLANTING" -> MoreMachineFactoryType.PLANTING_STATION;
            case "STAMPING" -> MoreMachineFactoryType.CNC_STAMPING;
            case "LATHING" -> MoreMachineFactoryType.CNC_LATHING;
            case "ROLLING_MILL" -> MoreMachineFactoryType.CNC_ROLLING_MILL;
            case "REPLICATING" -> MoreMachineFactoryType.REPLICATING;
            default -> throw new IllegalStateException("Unknown MEKMM factory type: " + name);
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ContainerTypeRegistryObject<? extends MekanismTileContainer<?>> baseContainer(MeMekanismMachine machine) {
        return switch (machine) {
            case RECYCLER -> MoreMachineContainerTypes.RECYCLER;
            case CNC_STAMPER -> MoreMachineContainerTypes.CNC_STAMPER;
            case CNC_LATHE -> MoreMachineContainerTypes.CNC_LATHE;
            case CNC_ROLLING_MILL -> MoreMachineContainerTypes.CNC_ROLLING_MILL;
            default -> (ContainerTypeRegistryObject) ModMenuTypes.getMachineContainer(machine);
        };
    }

    @Nullable
    public static MeMekanismMachine getFactoryTarget(BlockState state) {
        MeMekanismMachine registryTarget = getFactoryTargetByRegistryName(state);
        if (registryTarget != null) {
            return registryTarget;
        }
        MoreMachineAttributeFactoryType attribute = Attribute.get(state, MoreMachineAttributeFactoryType.class);
        if (attribute == null) {
            return null;
        }
        String typeName = attribute.getMoreMachineFactoryType().getRegistryNameComponent();
        AttributeTier<?> tier = Attribute.get(state, AttributeTier.class);
        if (tier != null && tier.tier() instanceof FactoryTier factoryTier) {
            return MeMekanismMachine.getMoreMachineFactory(factoryTier, typeName);
        }
        MeMekanismMachine baseTarget = getBaseTarget(typeName);
        return baseTarget == null ? MeMekanismMachine.getMoreMachineFactory(FactoryTier.BASIC, typeName) : baseTarget;
    }

    @Nullable
    private static MeMekanismMachine getFactoryTargetByRegistryName(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = id.getPath();
        if (path.startsWith("me_")) {
            return null;
        }
        MeMekanismMachine baseTarget = MeMekanismMachine.getByRegistryName("me_" + path);
        if (baseTarget != null) {
            return baseTarget;
        }
        MeMekanismMachine factoryTarget = getFactoryTargetByRegistryName(path, FactoryTier.BASIC.name().toLowerCase(Locale.ROOT), FactoryTier.BASIC);
        if (factoryTarget != null) {
            return factoryTarget;
        }
        factoryTarget = getFactoryTargetByRegistryName(path, FactoryTier.ADVANCED.name().toLowerCase(Locale.ROOT), FactoryTier.ADVANCED);
        if (factoryTarget != null) {
            return factoryTarget;
        }
        factoryTarget = getFactoryTargetByRegistryName(path, FactoryTier.ELITE.name().toLowerCase(Locale.ROOT), FactoryTier.ELITE);
        if (factoryTarget != null) {
            return factoryTarget;
        }
        return getFactoryTargetByRegistryName(path, FactoryTier.ULTIMATE.name().toLowerCase(Locale.ROOT), FactoryTier.ULTIMATE);
    }

    @Nullable
    private static MeMekanismMachine getFactoryTargetByRegistryName(String path, String tierName, FactoryTier tier) {
        String typeName = factoryTypeName(path, tierName);
        return typeName == null ? null : MeMekanismMachine.getMoreMachineFactory(tier, typeName);
    }

    @Nullable
    private static String factoryTypeName(String path, String tierName) {
        String prefix = tierName + "_";
        String suffix = "_factory";
        if (!path.startsWith(prefix) || !path.endsWith(suffix)) {
            return null;
        }
        return path.substring(prefix.length(), path.length() - suffix.length());
    }

    @Nullable
    private static MeMekanismMachine getBaseTarget(String typeName) {
        for (MeMekanismMachine machine : MeMekanismMachine.values()) {
            if (typeName.equals(machine.moreMachineBaseTypeName()) && machine.isAvailable()) {
                return machine;
            }
        }
        return null;
    }

    public static void registerGridNodeHost(
            RegisterCapabilitiesEvent event,
            TileEntityTypeRegistryObject<? extends TileEntityMekanism> holder) {
        ModBlockEntities.registerGridNodeHost(event, holder, MeFactoryAeMachine.class);
    }

    public static void registerBaseGridNodeHost(
            RegisterCapabilitiesEvent event,
            MeMekanismMachine machine,
            TileEntityTypeRegistryObject<? extends TileEntityMekanism> holder) {
        switch (machine) {
            case RECYCLER -> ModBlockEntities.registerGridNodeHost(event, holder, MeRecyclerBlockEntity.class);
            case CNC_STAMPER -> ModBlockEntities.registerGridNodeHost(event, holder, MeStamperBlockEntity.class);
            case CNC_LATHE -> ModBlockEntities.registerGridNodeHost(event, holder, MeLatheBlockEntity.class);
            case CNC_ROLLING_MILL -> ModBlockEntities.registerGridNodeHost(event, holder, MeRollingMillBlockEntity.class);
            default -> throw new IllegalStateException("Unknown MEKMM base machine: " + machine);
        }
    }
}
