package com.beipuo.mekenergistics.compat;

import com.beipuo.mekenergistics.block.MeUpgradeableAttribute;
import com.beipuo.mekenergistics.blockentity.compat.MeMoreMachineFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.compat.MeMoreMachineItemStackToItemStackFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.MePlantingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.MeRecyclingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.MeReplicatingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.MeStampingFactoryBlockEntity;
import com.beipuo.mekenergistics.common.MeMekanismMachine;
import com.beipuo.mekenergistics.registry.ModBlockEntities;
import com.beipuo.mekenergistics.registry.ModBlocks;
import com.jerry.mekmm.common.block.attribute.MoreMachineAttributeFactoryType;
import com.jerry.mekmm.common.content.blocktype.MoreMachineFactoryType;
import com.jerry.mekmm.common.registries.MoreMachineContainerTypes;
import java.util.Locale;
import mekanism.api.Upgrade;
import mekanism.common.block.attribute.AttributeStateFacing;
import mekanism.common.block.attribute.AttributeTier;
import mekanism.common.block.attribute.Attributes;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class MekanismMoreMachineCompat {
    private MekanismMoreMachineCompat() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static TileEntityTypeRegistryObject<? extends TileEntityMekanism> registerFactoryMachine(
            MeMekanismMachine machine, ModBlockEntities.MachineFactoryRegistrar registrar) {
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
        var builder = BlockTypeTile.BlockTileBuilder
                .createBlock(() -> tileType, machine::translationKey)
                .withGui(() -> MoreMachineContainerTypes.MM_FACTORY)
                .withEnergyConfig(machine.energyUsage(), machine.energyStorage())
                .with(new AttributeStateFacing(), Attributes.ACTIVE, Attributes.INVENTORY, Attributes.REDSTONE, Attributes.SECURITY)
                .withSideConfig(needsChemical(machine)
                        ? new TransmissionType[] {TransmissionType.ITEM, TransmissionType.ENERGY, TransmissionType.CHEMICAL}
                        : new TransmissionType[] {TransmissionType.ITEM, TransmissionType.ENERGY})
                .withSupportedUpgrades(Upgrade.SPEED, Upgrade.ENERGY)
                .with(new MoreMachineAttributeFactoryType(moreMachineFactoryType(machine)));
        if (machine.factoryTier() != null) {
            builder.with(new AttributeTier<>(machine.factoryTier()));
        }
        MeMekanismMachine upgradeTarget = machine.getNextFactory();
        if (upgradeTarget != null) {
            builder.with(new MeUpgradeableAttribute(() -> ModBlocks.getMachineBlock(upgradeTarget).get()));
        }
        return builder.build();
    }

    public static MoreMachineFactoryType moreMachineFactoryType(MeMekanismMachine machine) {
        String name = machine.moreMachineFactoryTypeName().toUpperCase(Locale.ROOT);
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
