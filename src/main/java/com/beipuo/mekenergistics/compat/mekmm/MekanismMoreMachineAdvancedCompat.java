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
import com.jerry.mekaf.common.block.attribute.AttributeAdvancedFactoryType;
import com.jerry.mekaf.common.content.blocktype.AdvancedFactory;
import com.jerry.mekaf.common.content.blocktype.AdvancedFactoryType;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.common.integration.mekaf.content.blocktype.ExtraAdvancedFactory;
import com.jerry.mekextras.common.tier.ExtraFactoryTier;
import java.util.Locale;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeTier;
import mekanism.common.block.attribute.AttributeGui;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

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

    @Nullable
    public static MeMekanismMachine getFactoryTarget(BlockState state) {
        MeMekanismMachine registryTarget = getFactoryTargetByRegistryName(state);
        if (registryTarget != null) {
            return registryTarget;
        }
        AttributeAdvancedFactoryType attribute = Attribute.get(state, AttributeAdvancedFactoryType.class);
        if (attribute == null) {
            return null;
        }
        String typeName = attribute.getAdvancedFactoryType().getRegistryNameComponent();
        if (ModList.get().isLoaded("mekanism_extras")) {
            ExtraFactoryTier extraTier = ExtraAttribute.getAdvancedTier(state.getBlock(), ExtraFactoryTier.class);
            if (extraTier != null) {
                return MeMekanismMachine.getExtraMoreMachineAdvancedFactory(extraTier.name().toLowerCase(Locale.ROOT), typeName);
            }
        }
        AttributeTier<?> tier = Attribute.get(state, AttributeTier.class);
        if (tier != null && tier.tier() instanceof FactoryTier factoryTier) {
            return MeMekanismMachine.getMoreMachineAdvancedFactory(factoryTier, typeName);
        }
        return MeMekanismMachine.getMoreMachineAdvancedFactory(FactoryTier.BASIC, typeName);
    }

    @Nullable
    private static MeMekanismMachine getFactoryTargetByRegistryName(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = id.getPath();
        if (path.startsWith("me_")) {
            return null;
        }
        for (FactoryTier tier : FactoryTier.values()) {
            MeMekanismMachine target = getFactoryTargetByRegistryName(path, tier.name().toLowerCase(Locale.ROOT), tier);
            if (target != null) {
                return target;
            }
        }
        if (ModList.get().isLoaded("mekanism_extras")) {
            for (String tierName : new String[] {"absolute", "supreme", "cosmic", "infinite"}) {
                MeMekanismMachine target = getExtraFactoryTargetByRegistryName(path, tierName);
                if (target != null) {
                    return target;
                }
            }
        }
        return null;
    }

    @Nullable
    private static MeMekanismMachine getFactoryTargetByRegistryName(String path, String tierName, FactoryTier tier) {
        String typeName = factoryTypeName(path, tierName);
        return typeName == null ? null : MeMekanismMachine.getMoreMachineAdvancedFactory(tier, typeName);
    }

    @Nullable
    private static MeMekanismMachine getExtraFactoryTargetByRegistryName(String path, String tierName) {
        String typeName = factoryTypeName(path, tierName);
        return typeName == null ? null : MeMekanismMachine.getExtraMoreMachineAdvancedFactory(tierName, typeName);
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

    public static void registerGridNodeHost(
            RegisterCapabilitiesEvent event,
            TileEntityTypeRegistryObject<? extends TileEntityMekanism> holder) {
        ModBlockEntities.registerGridNodeHost(event, holder, MeFactoryAeMachine.class);
    }
}
