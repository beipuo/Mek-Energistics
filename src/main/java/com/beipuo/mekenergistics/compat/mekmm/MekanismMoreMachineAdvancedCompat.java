package com.beipuo.mekenergistics.compat.mekmm;

import com.beipuo.mekenergistics.block.attribute.MeExtraUpgradeableAttribute;
import com.beipuo.mekenergistics.block.attribute.MeUpgradeableAttribute;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraAdvancedCentrifugingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraAdvancedChemicalToItemFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraAdvancedDissolvingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraAdvancedItemToChemicalFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraAdvancedItemToItemFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraAdvancedLiquifyingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraAdvancedPressurizedReactingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraAdvancedWashingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeAdvancedCentrifugingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeAdvancedChemicalToItemFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeAdvancedDissolvingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeAdvancedItemToChemicalFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeAdvancedItemToItemFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeAdvancedLiquifyingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeAdvancedPressurizedReactingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.factory.MeAdvancedWashingFactoryBlockEntity;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.registry.ModBlockEntities;
import com.beipuo.mekenergistics.registry.ModBlocks;
import com.beipuo.mekenergistics.registry.ModMenuTypes;
import com.beipuo.mekenergistics.registry.machine.MachineFactoryRegistrar;
import com.jerry.mekaf.common.content.blocktype.AdvancedFactory;
import com.jerry.mekaf.common.content.blocktype.AdvancedFactoryType;
import com.jerry.mekextras.common.integration.mekaf.content.blocktype.ExtraAdvancedFactory;
import com.jerry.mekextras.common.tier.ExtraFactoryTier;
import java.util.Locale;
import mekanism.common.block.attribute.AttributeGui;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class MekanismMoreMachineAdvancedCompat {
    private MekanismMoreMachineAdvancedCompat() {
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

    public static <TILE extends TileEntityMekanism> BlockTypeTile<TILE> createAdvancedFactoryBlockType(
            MeMekanismMachine machine, TileEntityTypeRegistryObject<TILE> tileType) {
        if (machine.extraFactoryTierName() != null) {
            return createExtraAdvancedFactoryBlockType(machine, tileType);
        }
        AdvancedFactory.AdvancedFactoryBuilder<?, ?, ?> builder =
                AdvancedFactory.AdvancedFactoryBuilder.createAdvancedFactory(() -> tileType, advancedFactoryType(machine), machine.factoryTier());
        builder.replace(new AttributeGui(() -> ModMenuTypes.ME_ADVANCED_FACTORY, null));
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
    private static <TILE extends TileEntityMekanism> BlockTypeTile<TILE> createExtraAdvancedFactoryBlockType(
            MeMekanismMachine machine, TileEntityTypeRegistryObject<TILE> tileType) {
        ExtraAdvancedFactory.ExtraAdvancedFactoryBuilder<?, ?, ?> builder =
                ExtraAdvancedFactory.ExtraAdvancedFactoryBuilder.createAdvancedFactory(() -> tileType, advancedFactoryType(machine), extraFactoryTier(machine));
        builder.replace(new AttributeGui(() -> ModMenuTypes.ME_EXTRA_ADVANCED_FACTORY, null));
        MeMekanismMachine upgradeTarget = machine.getNextFactory();
        if (upgradeTarget != null) {
            builder.replace(new MeUpgradeableAttribute(() -> ModBlocks.getMachineBlock(upgradeTarget).get()));
            if (upgradeTarget.extraFactoryTierName() != null) {
                builder.replace(new MeExtraUpgradeableAttribute(() -> ModBlocks.getMachineBlock(upgradeTarget).get()));
            }
        }
        return (BlockTypeTile<TILE>) builder.build();
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

    public static void registerGridNodeHost(
            RegisterCapabilitiesEvent event,
            TileEntityTypeRegistryObject<? extends TileEntityMekanism> holder) {
        ModBlockEntities.registerGridNodeHost(event, holder, MeFactoryAeMachine.class);
    }
}
