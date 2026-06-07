package com.beipuo.mekenergistics.compat.meke;

import com.beipuo.mekenergistics.block.attribute.MeUpgradeableAttribute;
import com.beipuo.mekenergistics.block.attribute.MeExtraUpgradeableAttribute;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraAlloyingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraCombiningFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraItemStackChemicalToItemStackFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraItemStackToItemStackFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraSawingFactoryBlockEntity;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.compat.eme.EvolvedMekanismCompat;
import com.beipuo.mekenergistics.registry.ModBlockEntities;
import com.beipuo.mekenergistics.registry.machine.MachineFactoryRegistrar;
import com.beipuo.mekenergistics.registry.ModBlocks;
import com.beipuo.mekenergistics.registry.ModMenuTypes;
import com.jerry.mekextras.api.tier.AdvancedTier;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.common.block.attribute.ExtraAttributeTier;
import com.jerry.mekextras.common.block.attribute.ExtraAttributeUpgradeSupport;
import com.jerry.mekextras.common.item.ItemExtraTierInstaller;
import com.jerry.mekextras.common.tier.ExtraFactoryTier;
import java.util.Locale;
import mekanism.common.block.attribute.AttributeFactoryType;
import mekanism.common.block.attribute.AttributeUpgradeSupport;
import mekanism.common.block.attribute.AttributeStateFacing;
import mekanism.common.block.attribute.Attributes;
import mekanism.common.content.blocktype.BlockShapes;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.content.blocktype.FactoryType;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;
import java.util.function.Supplier;

public final class MekanismExtrasCompat {
    private MekanismExtrasCompat() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static TileEntityTypeRegistryObject<? extends TileEntityMekanism> registerFactoryMachine(
            MeMekanismMachine machine, MachineFactoryRegistrar registrar) {
        if ("alloying".equals(machine.customFactoryTypeName())) {
            return registrar.register(machine, MeExtraAlloyingFactoryBlockEntity::new);
        }
        TileEntityTypeRegistryObject<?> registered = switch (machine.factoryType()) {
            case SMELTING, ENRICHING, CRUSHING -> registrar.register(machine, MeExtraItemStackToItemStackFactoryBlockEntity::new);
            case COMPRESSING, INJECTING, PURIFYING, INFUSING -> registrar.register(machine, MeExtraItemStackChemicalToItemStackFactoryBlockEntity::new);
            case COMBINING -> registrar.register(machine, MeExtraCombiningFactoryBlockEntity::new);
            case SAWING -> registrar.register(machine, MeExtraSawingFactoryBlockEntity::new);
        };
        return (TileEntityTypeRegistryObject) registered;
    }

    public static <TILE extends TileEntityMekanism> BlockTypeTile<TILE> createFactoryBlockType(
            MeMekanismMachine machine, TileEntityTypeRegistryObject<TILE> tileType) {
        var builder = BlockTypeTile.BlockTileBuilder
                .createBlock(() -> tileType, machine::translationKey)
                .withGui(() -> ModMenuTypes.ME_EXTRA_FACTORY)
                .withEnergyConfig(machine.energyUsage(), machine.energyStorage())
                .with(new AttributeStateFacing(), Attributes.ACTIVE_LIGHT, Attributes.INVENTORY, Attributes.REDSTONE, Attributes.SECURITY, Attributes.COMPARATOR)
                .withSideConfig(machine.hasChemicalInput()
                        ? new TransmissionType[] {TransmissionType.ITEM, TransmissionType.ENERGY, TransmissionType.CHEMICAL}
                        : new TransmissionType[] {TransmissionType.ITEM, TransmissionType.ENERGY})
                .with(extraUpgradeSupport(machine.factoryTypeName()));
        if (machine.factoryType() != null) {
            builder.with(new AttributeFactoryType(machine.factoryType()));
            builder.withCustomShape(BlockShapes.getShape(null, machine.factoryType()));
        } else if ("alloying".equals(machine.customFactoryTypeName())) {
            EvolvedMekanismCompat.withAlloyingFactoryType(builder);
            builder.withCustomShape(EvolvedMekanismCompat.alloyingFactoryShape(null));
        }
        builder.with(new ExtraAttributeTier<>(extraTier(machine)));
        MeMekanismMachine upgradeTarget = machine.getNextFactory();
        if (upgradeTarget != null) {
            builder.with(new MeUpgradeableAttribute(() -> ModBlocks.getMachineBlock(upgradeTarget).get()));
            builder.with(new MeExtraUpgradeableAttribute(() -> ModBlocks.getMachineBlock(upgradeTarget).get()));
        }
        return builder.build();
    }

    private static AttributeUpgradeSupport extraUpgradeSupport(String typeName) {
        if ("alloying".equals(typeName)) {
            return ExtraAttributeUpgradeSupport.EXTRA_ADVANCED_MACHINE_UPGRADES;
        }
        return switch (FactoryType.valueOf(typeName.toUpperCase(Locale.ROOT))) {
            case PURIFYING, INJECTING -> ExtraAttributeUpgradeSupport.EXTRA_ADVANCED_MACHINE_UPGRADES;
            case SMELTING, ENRICHING, CRUSHING, COMPRESSING, COMBINING, INFUSING, SAWING -> ExtraAttributeUpgradeSupport.EXTRA_MACHINE_UPGRADES;
        };
    }

    public static ExtraFactoryTier extraTier(MeMekanismMachine machine) {
        return ExtraFactoryTier.valueOf(machine.extraFactoryTierName().toUpperCase(Locale.ROOT));
    }

    public static <TILE extends TileEntityMekanism> void withExtraUpgradeable(
            BlockTypeTile.BlockTileBuilder<?, TILE, ?> builder, Supplier<? extends Block> upgradeBlock) {
        builder.with(new MeExtraUpgradeableAttribute(upgradeBlock));
    }

    @Nullable
    public static MeMekanismMachine getFactoryTarget(BlockState state, FactoryType factoryType) {
        ExtraFactoryTier tier = ExtraAttribute.getAdvancedTier(state.getBlock(), ExtraFactoryTier.class);
        return tier == null ? null : MeMekanismMachine.getExtraFactory(tier.name().toLowerCase(Locale.ROOT), factoryType);
    }

    @Nullable
    public static MeMekanismMachine getInstallerTarget(MeMekanismMachine current, ItemStack stack) {
        if (!(stack.getItem() instanceof ItemExtraTierInstaller installer)) {
            return null;
        }
        AdvancedTier currentTier = current.extraFactoryTierName() == null
                ? null
                : AdvancedTier.valueOf(current.extraFactoryTierName().toUpperCase(Locale.ROOT));
        AdvancedTier fromTier = installer.getFromTier();
        AdvancedTier toTier = installer.getToTier();
        if (currentTier != fromTier || currentTier == toTier) {
            return null;
        }
        MeMekanismMachine target = currentTier == null && fromTier == null && isTerminalEvolvedFactory(current)
                ? MeMekanismMachine.getExtraFactory(toTier.getLowerName(), current.factoryTypeName())
                : current.getNextFactory();
        if (target == null || target.extraFactoryTierName() == null) {
            return null;
        }
        AdvancedTier targetTier = AdvancedTier.valueOf(target.extraFactoryTierName().toUpperCase(Locale.ROOT));
        return targetTier == toTier ? target : null;
    }

    private static boolean isTerminalEvolvedFactory(MeMekanismMachine machine) {
        if (!machine.isEvolvedMekanismFactory()) {
            return false;
        }
        MeMekanismMachine next = machine.getNextFactory();
        return next == null || next.isEvolvedMekanismExtrasFactory();
    }

    public static void registerGridNodeHost(
            RegisterCapabilitiesEvent event,
            TileEntityTypeRegistryObject<? extends TileEntityMekanism> holder) {
        ModBlockEntities.registerGridNodeHost(event, holder, com.beipuo.mekenergistics.blockentity.compat.meke.factory.MeExtraFactoryAeMachine.class);
    }
}
