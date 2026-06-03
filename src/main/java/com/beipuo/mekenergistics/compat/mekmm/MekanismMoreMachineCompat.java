package com.beipuo.mekenergistics.compat.mekmm;

import com.beipuo.mekenergistics.block.attribute.MeUpgradeableAttribute;
import com.beipuo.mekenergistics.block.attribute.MeExtraUpgradeableAttribute;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeMoreMachineFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeAdvancedCentrifugingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeAdvancedChemicalToItemFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeAdvancedDissolvingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeAdvancedItemToChemicalFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeAdvancedItemToItemFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeAdvancedLiquifyingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeAdvancedPressurizedReactingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeAdvancedWashingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraAdvancedCentrifugingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraAdvancedChemicalToItemFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraAdvancedDissolvingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraAdvancedItemToChemicalFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraAdvancedItemToItemFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraAdvancedLiquifyingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraAdvancedPressurizedReactingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraAdvancedWashingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraMoreMachineItemStackToItemStackFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraPlantingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraRecyclingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraReplicatingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraStampingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeMoreMachineItemStackToItemStackFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MePlantingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeRecyclingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeReplicatingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeStampingFactoryBlockEntity;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.registry.ModBlockEntities;
import com.beipuo.mekenergistics.registry.machine.MachineFactoryRegistrar;
import com.beipuo.mekenergistics.registry.ModBlocks;
import com.beipuo.mekenergistics.registry.ModMenuTypes;
import com.jerry.mekaf.common.block.attribute.AttributeAdvancedFactoryType;
import com.jerry.mekaf.common.content.blocktype.AdvancedFactory;
import com.jerry.mekaf.common.content.blocktype.AdvancedFactoryType;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.common.tier.ExtraFactoryTier;
import com.jerry.mekextras.common.integration.mekaf.content.blocktype.ExtraAdvancedFactory;
import com.jerry.mekextras.common.integration.mekmm.content.blocktype.ExtraMoreMachineFactory;
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
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

public final class MekanismMoreMachineCompat {
    private MekanismMoreMachineCompat() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static TileEntityTypeRegistryObject<? extends TileEntityMekanism> registerFactoryMachine(
            MeMekanismMachine machine, MachineFactoryRegistrar registrar) {
        if (machine.extraFactoryTierName() != null) {
            return registerExtraFactoryMachine(machine, registrar);
        }
        TileEntityTypeRegistryObject<?> registered = switch (moreMachineFactoryType(machine)) {
            case RECYCLING -> registrar.register(machine, MeRecyclingFactoryBlockEntity::new);
            case PLANTING_STATION -> registrar.register(machine, MePlantingFactoryBlockEntity::new);
            case CNC_STAMPING -> registrar.register(machine, MeStampingFactoryBlockEntity::new);
            case CNC_LATHING, CNC_ROLLING_MILL -> registrar.register(machine, MeMoreMachineItemStackToItemStackFactoryBlockEntity::new);
            case REPLICATING -> registrar.register(machine, MeReplicatingFactoryBlockEntity::new);
        };
        return (TileEntityTypeRegistryObject) registered;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static TileEntityTypeRegistryObject<? extends TileEntityMekanism> registerAdvancedFactoryMachine(
            MeMekanismMachine machine, MachineFactoryRegistrar registrar) {
        if (machine.extraFactoryTierName() != null) {
            return registerExtraAdvancedFactoryMachine(machine, registrar);
        }
        TileEntityTypeRegistryObject<?> registered = switch (advancedFactoryType(machine)) {
            case OXIDIZING, PIGMENT_EXTRACTING -> registrar.register(machine, MeAdvancedItemToChemicalFactoryBlockEntity::new);
            case DISSOLVING -> registrar.register(machine, MeAdvancedDissolvingFactoryBlockEntity::new);
            case WASHING -> registrar.register(machine, MeAdvancedWashingFactoryBlockEntity::new);
            case CRYSTALLIZING -> registrar.register(machine, MeAdvancedChemicalToItemFactoryBlockEntity::new);
            case PRESSURISED_REACTING -> registrar.register(machine, MeAdvancedPressurizedReactingFactoryBlockEntity::new);
            case CENTRIFUGING -> registrar.register(machine, MeAdvancedCentrifugingFactoryBlockEntity::new);
            case LIQUIFYING -> registrar.register(machine, MeAdvancedLiquifyingFactoryBlockEntity::new);
            case PAINTING -> registrar.register(machine, MeAdvancedItemToItemFactoryBlockEntity::new);
        };
        return (TileEntityTypeRegistryObject) registered;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static TileEntityTypeRegistryObject<? extends TileEntityMekanism> registerExtraFactoryMachine(
            MeMekanismMachine machine, MachineFactoryRegistrar registrar) {
        TileEntityTypeRegistryObject<?> registered = switch (moreMachineFactoryType(machine)) {
            case RECYCLING -> registrar.register(machine, MeExtraRecyclingFactoryBlockEntity::new);
            case PLANTING_STATION -> registrar.register(machine, MeExtraPlantingFactoryBlockEntity::new);
            case CNC_STAMPING -> registrar.register(machine, MeExtraStampingFactoryBlockEntity::new);
            case CNC_LATHING, CNC_ROLLING_MILL -> registrar.register(machine, MeExtraMoreMachineItemStackToItemStackFactoryBlockEntity::new);
            case REPLICATING -> registrar.register(machine, MeExtraReplicatingFactoryBlockEntity::new);
        };
        return (TileEntityTypeRegistryObject) registered;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static TileEntityTypeRegistryObject<? extends TileEntityMekanism> registerExtraAdvancedFactoryMachine(
            MeMekanismMachine machine, MachineFactoryRegistrar registrar) {
        TileEntityTypeRegistryObject<?> registered = switch (advancedFactoryType(machine)) {
            case OXIDIZING, PIGMENT_EXTRACTING -> registrar.register(machine, MeExtraAdvancedItemToChemicalFactoryBlockEntity::new);
            case DISSOLVING -> registrar.register(machine, MeExtraAdvancedDissolvingFactoryBlockEntity::new);
            case WASHING -> registrar.register(machine, MeExtraAdvancedWashingFactoryBlockEntity::new);
            case CRYSTALLIZING -> registrar.register(machine, MeExtraAdvancedChemicalToItemFactoryBlockEntity::new);
            case PRESSURISED_REACTING -> registrar.register(machine, MeExtraAdvancedPressurizedReactingFactoryBlockEntity::new);
            case CENTRIFUGING -> registrar.register(machine, MeExtraAdvancedCentrifugingFactoryBlockEntity::new);
            case LIQUIFYING -> registrar.register(machine, MeExtraAdvancedLiquifyingFactoryBlockEntity::new);
            case PAINTING -> registrar.register(machine, MeExtraAdvancedItemToItemFactoryBlockEntity::new);
        };
        return (TileEntityTypeRegistryObject) registered;
    }

    public static <TILE extends TileEntityMekanism> BlockTypeTile<TILE> createFactoryBlockType(
            MeMekanismMachine machine, TileEntityTypeRegistryObject<TILE> tileType) {
        if (machine.extraFactoryTierName() != null) {
            return createExtraFactoryBlockType(machine, tileType);
        }
        MoreMachineFactory.MoreMachineFactoryBuilder<?, ?, ?> builder =
                MoreMachineFactory.MoreMachineFactoryBuilder.createMoreMachineFactory(() -> tileType, moreMachineFactoryType(machine), machine.factoryTier());
        builder.replace(new mekanism.common.block.attribute.AttributeGui(() -> ModMenuTypes.ME_MORE_MACHINE_FACTORY, null));
        MeMekanismMachine upgradeTarget = machine.getNextFactory();
        if (upgradeTarget != null) {
            builder.replace(new MeUpgradeableAttribute(() -> ModBlocks.getMachineBlock(upgradeTarget).get()));
            if (upgradeTarget.extraFactoryTierName() != null) {
                builder.replace(new MeExtraUpgradeableAttribute(() -> ModBlocks.getMachineBlock(upgradeTarget).get()));
            }
        }
        @SuppressWarnings("unchecked")
        BlockTypeTile<TILE> built = (BlockTypeTile<TILE>) builder.build();
        return built;
    }

    public static <TILE extends TileEntityMekanism> BlockTypeTile<TILE> createAdvancedFactoryBlockType(
            MeMekanismMachine machine, TileEntityTypeRegistryObject<TILE> tileType) {
        if (machine.extraFactoryTierName() != null) {
            return createExtraAdvancedFactoryBlockType(machine, tileType);
        }
        AdvancedFactory.AdvancedFactoryBuilder<?, ?, ?> builder =
                AdvancedFactory.AdvancedFactoryBuilder.createAdvancedFactory(() -> tileType, advancedFactoryType(machine), machine.factoryTier());
        builder.replace(new mekanism.common.block.attribute.AttributeGui(() -> ModMenuTypes.ME_ADVANCED_FACTORY, null));
        MeMekanismMachine upgradeTarget = machine.getNextFactory();
        if (upgradeTarget != null) {
            builder.replace(new MeUpgradeableAttribute(() -> ModBlocks.getMachineBlock(upgradeTarget).get()));
            if (upgradeTarget.extraFactoryTierName() != null) {
                builder.replace(new MeExtraUpgradeableAttribute(() -> ModBlocks.getMachineBlock(upgradeTarget).get()));
            }
        }
        @SuppressWarnings("unchecked")
        BlockTypeTile<TILE> built = (BlockTypeTile<TILE>) builder.build();
        return built;
    }

    @SuppressWarnings("unchecked")
    private static <TILE extends TileEntityMekanism> BlockTypeTile<TILE> createExtraFactoryBlockType(
            MeMekanismMachine machine, TileEntityTypeRegistryObject<TILE> tileType) {
        ExtraMoreMachineFactory.ExtraMoreMachineFactoryBuilder<?, ?, ?> builder =
                ExtraMoreMachineFactory.ExtraMoreMachineFactoryBuilder.createMoreMachineFactory(() -> tileType, moreMachineFactoryType(machine), extraFactoryTier(machine));
        builder.replace(new mekanism.common.block.attribute.AttributeGui(() -> ModMenuTypes.ME_EXTRA_MORE_MACHINE_FACTORY, null));
        MeMekanismMachine upgradeTarget = machine.getNextFactory();
        if (upgradeTarget != null) {
            builder.replace(new MeUpgradeableAttribute(() -> ModBlocks.getMachineBlock(upgradeTarget).get()));
            if (upgradeTarget.extraFactoryTierName() != null) {
                builder.replace(new MeExtraUpgradeableAttribute(() -> ModBlocks.getMachineBlock(upgradeTarget).get()));
            }
        }
        return (BlockTypeTile<TILE>) builder.build();
    }

    @SuppressWarnings("unchecked")
    private static <TILE extends TileEntityMekanism> BlockTypeTile<TILE> createExtraAdvancedFactoryBlockType(
            MeMekanismMachine machine, TileEntityTypeRegistryObject<TILE> tileType) {
        ExtraAdvancedFactory.ExtraAdvancedFactoryBuilder<?, ?, ?> builder =
                ExtraAdvancedFactory.ExtraAdvancedFactoryBuilder.createAdvancedFactory(() -> tileType, advancedFactoryType(machine), extraFactoryTier(machine));
        builder.replace(new mekanism.common.block.attribute.AttributeGui(() -> ModMenuTypes.ME_EXTRA_ADVANCED_FACTORY, null));
        MeMekanismMachine upgradeTarget = machine.getNextFactory();
        if (upgradeTarget != null) {
            builder.replace(new MeUpgradeableAttribute(() -> ModBlocks.getMachineBlock(upgradeTarget).get()));
            if (upgradeTarget.extraFactoryTierName() != null) {
                builder.replace(new MeExtraUpgradeableAttribute(() -> ModBlocks.getMachineBlock(upgradeTarget).get()));
            }
        }
        return (BlockTypeTile<TILE>) builder.build();
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

    public static AdvancedFactoryType advancedFactoryType(MeMekanismMachine machine) {
        String name = machine.moreMachineAdvancedFactoryTypeName().toUpperCase(Locale.ROOT);
        return switch (name) {
            case "OXIDIZING" -> AdvancedFactoryType.OXIDIZING;
            case "DISSOLVING" -> AdvancedFactoryType.DISSOLVING;
            case "WASHING" -> AdvancedFactoryType.WASHING;
            case "CRYSTALLIZING" -> AdvancedFactoryType.CRYSTALLIZING;
            case "PRESSURISED_REACTING" -> AdvancedFactoryType.PRESSURISED_REACTING;
            case "CENTRIFUGING" -> AdvancedFactoryType.CENTRIFUGING;
            case "LIQUIFYING" -> AdvancedFactoryType.LIQUIFYING;
            case "PIGMENT_EXTRACTING" -> AdvancedFactoryType.PIGMENT_EXTRACTING;
            case "PAINTING" -> AdvancedFactoryType.PAINTING;
            default -> throw new IllegalStateException("Unknown MEKMM advanced factory type: " + name);
        };
    }

    public static ExtraFactoryTier extraFactoryTier(MeMekanismMachine machine) {
        return ExtraFactoryTier.valueOf(machine.extraFactoryTierName().toUpperCase(Locale.ROOT));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ContainerTypeRegistryObject<? extends MekanismTileContainer<?>> baseContainer(MeMekanismMachine machine) {
        return switch (machine) {
            case RECYCLER -> MoreMachineContainerTypes.RECYCLER;
            case CNC_STAMPER -> MoreMachineContainerTypes.CNC_STAMPER;
            case CNC_LATHE -> MoreMachineContainerTypes.CNC_LATHE;
            case CNC_ROLLING_MILL -> MoreMachineContainerTypes.CNC_ROLLING_MILL;
            default -> (ContainerTypeRegistryObject) com.beipuo.mekenergistics.registry.ModMenuTypes.getMachineContainer(machine);
        };
    }

    @Nullable
    public static MeMekanismMachine getFactoryTarget(BlockState state) {
        MeMekanismMachine registryTarget = getFactoryTargetByRegistryName(state);
        if (registryTarget != null) {
            return registryTarget;
        }
        MoreMachineAttributeFactoryType attribute = Attribute.get(state, MoreMachineAttributeFactoryType.class);
        AttributeAdvancedFactoryType advancedAttribute = Attribute.get(state, AttributeAdvancedFactoryType.class);
        if (attribute == null && advancedAttribute != null) {
            if (ModList.get().isLoaded("mekanism_extras")) {
                ExtraFactoryTier extraTier = ExtraAttribute.getAdvancedTier(state.getBlock(), ExtraFactoryTier.class);
                if (extraTier != null) {
                    return MeMekanismMachine.getExtraMoreMachineAdvancedFactory(
                            extraTier.name().toLowerCase(Locale.ROOT),
                            advancedAttribute.getAdvancedFactoryType().getRegistryNameComponent());
                }
            }
            AttributeTier<?> tier = Attribute.get(state, AttributeTier.class);
            if (tier != null && tier.tier() instanceof FactoryTier factoryTier) {
                return MeMekanismMachine.getMoreMachineAdvancedFactory(factoryTier, advancedAttribute.getAdvancedFactoryType().getRegistryNameComponent());
            }
            return MeMekanismMachine.getMoreMachineAdvancedFactory(FactoryTier.BASIC, advancedAttribute.getAdvancedFactoryType().getRegistryNameComponent());
        }
        if (attribute == null) {
            return null;
        }
        String typeName = attribute.getMoreMachineFactoryType().getRegistryNameComponent();
        if (ModList.get().isLoaded("mekanism_extras")) {
            ExtraFactoryTier extraTier = ExtraAttribute.getAdvancedTier(state.getBlock(), ExtraFactoryTier.class);
            if (extraTier != null) {
                return MeMekanismMachine.getExtraMoreMachineFactory(extraTier.name().toLowerCase(Locale.ROOT), typeName);
            }
        }
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
        factoryTarget = getFactoryTargetByRegistryName(path, FactoryTier.ULTIMATE.name().toLowerCase(Locale.ROOT), FactoryTier.ULTIMATE);
        if (factoryTarget != null) {
            return factoryTarget;
        }
        if (ModList.get().isLoaded("mekanism_extras")) {
            factoryTarget = getExtraFactoryTargetByRegistryName(path, "absolute");
            if (factoryTarget != null) {
                return factoryTarget;
            }
            factoryTarget = getExtraFactoryTargetByRegistryName(path, "supreme");
            if (factoryTarget != null) {
                return factoryTarget;
            }
            factoryTarget = getExtraFactoryTargetByRegistryName(path, "cosmic");
            if (factoryTarget != null) {
                return factoryTarget;
            }
            factoryTarget = getExtraFactoryTargetByRegistryName(path, "infinite");
            if (factoryTarget != null) {
                return factoryTarget;
            }
        }
        return null;
    }

    @Nullable
    private static MeMekanismMachine getFactoryTargetByRegistryName(String path, String tierName, FactoryTier tier) {
        String typeName = factoryTypeName(path, tierName);
        if (typeName == null) {
            return null;
        }
        MeMekanismMachine target = MeMekanismMachine.getMoreMachineFactory(tier, typeName);
        return target == null ? MeMekanismMachine.getMoreMachineAdvancedFactory(tier, typeName) : target;
    }

    @Nullable
    private static MeMekanismMachine getExtraFactoryTargetByRegistryName(String path, String tierName) {
        String typeName = factoryTypeName(path, tierName);
        if (typeName == null) {
            return null;
        }
        MeMekanismMachine target = MeMekanismMachine.getExtraMoreMachineFactory(tierName, typeName);
        return target == null ? MeMekanismMachine.getExtraMoreMachineAdvancedFactory(tierName, typeName) : target;
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

    private static boolean needsChemical(MeMekanismMachine machine) {
        MoreMachineFactoryType type = moreMachineFactoryType(machine);
        return type == MoreMachineFactoryType.PLANTING_STATION || type == MoreMachineFactoryType.REPLICATING;
    }

    public static void registerGridNodeHost(
            RegisterCapabilitiesEvent event,
            TileEntityTypeRegistryObject<? extends TileEntityMekanism> holder) {
        ModBlockEntities.registerGridNodeHost(event, holder, MeMoreMachineFactoryAeMachine.class);
    }
}
