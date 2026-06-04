package com.beipuo.mekenergistics.compat.meke;

import com.beipuo.mekenergistics.block.attribute.MeExtraUpgradeableAttribute;
import com.beipuo.mekenergistics.block.attribute.MeUpgradeableAttribute;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraMoreMachineItemStackToItemStackFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraPlantingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraRecyclingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraReplicatingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraStampingFactoryBlockEntity;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.compat.mekmm.MekanismMoreMachineBaseCompat;
import com.beipuo.mekenergistics.registry.ModBlockEntities;
import com.beipuo.mekenergistics.registry.ModBlocks;
import com.beipuo.mekenergistics.registry.ModMenuTypes;
import com.beipuo.mekenergistics.registry.machine.MachineFactoryRegistrar;
import com.jerry.mekextras.common.integration.mekmm.content.blocktype.ExtraMoreMachineFactory;
import com.jerry.mekextras.common.tier.ExtraFactoryTier;
import java.util.Locale;
import mekanism.common.block.attribute.AttributeGui;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class MekanismExtrasMoreMachineCompat {
    private MekanismExtrasMoreMachineCompat() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static TileEntityTypeRegistryObject<? extends TileEntityMekanism> registerFactoryMachine(
            MeMekanismMachine machine, MachineFactoryRegistrar registrar) {
        TileEntityTypeRegistryObject<?> registered = switch (MekanismMoreMachineBaseCompat.moreMachineFactoryType(machine)) {
            case RECYCLING -> registrar.register(machine, MeExtraRecyclingFactoryBlockEntity::new);
            case PLANTING_STATION -> registrar.register(machine, MeExtraPlantingFactoryBlockEntity::new);
            case CNC_STAMPING -> registrar.register(machine, MeExtraStampingFactoryBlockEntity::new);
            case CNC_LATHING, CNC_ROLLING_MILL -> registrar.register(machine, MeExtraMoreMachineItemStackToItemStackFactoryBlockEntity::new);
            case REPLICATING -> registrar.register(machine, MeExtraReplicatingFactoryBlockEntity::new);
        };
        return (TileEntityTypeRegistryObject) registered;
    }

    @SuppressWarnings("unchecked")
    public static <TILE extends TileEntityMekanism> BlockTypeTile<TILE> createFactoryBlockType(
            MeMekanismMachine machine, TileEntityTypeRegistryObject<TILE> tileType) {
        ExtraMoreMachineFactory.ExtraMoreMachineFactoryBuilder<?, ?, ?> builder =
                ExtraMoreMachineFactory.ExtraMoreMachineFactoryBuilder.createMoreMachineFactory(
                        () -> tileType,
                        MekanismMoreMachineBaseCompat.moreMachineFactoryType(machine),
                        extraFactoryTier(machine));
        builder.replace(new AttributeGui(() -> ModMenuTypes.ME_EXTRA_MORE_MACHINE_FACTORY, null));
        MeMekanismMachine upgradeTarget = machine.getNextFactory();
        if (upgradeTarget != null) {
            builder.replace(new MeUpgradeableAttribute(() -> ModBlocks.getMachineBlock(upgradeTarget).get()));
            if (upgradeTarget.extraFactoryTierName() != null) {
                builder.replace(new MeExtraUpgradeableAttribute(() -> ModBlocks.getMachineBlock(upgradeTarget).get()));
            }
        }
        return (BlockTypeTile<TILE>) builder.build();
    }

    public static ExtraFactoryTier extraFactoryTier(MeMekanismMachine machine) {
        return ExtraFactoryTier.valueOf(machine.extraFactoryTierName().toUpperCase(Locale.ROOT));
    }

    public static void registerGridNodeHost(
            RegisterCapabilitiesEvent event,
            TileEntityTypeRegistryObject<? extends TileEntityMekanism> holder) {
        ModBlockEntities.registerGridNodeHost(event, holder, MeFactoryAeMachine.class);
    }
}
