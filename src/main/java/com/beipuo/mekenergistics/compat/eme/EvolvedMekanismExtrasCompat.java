package com.beipuo.mekenergistics.compat.eme;

import com.beipuo.mekenergistics.block.attribute.MeUpgradeableAttribute;
import com.beipuo.mekenergistics.blockentity.compat.eme.factory.MeEMExtraCombiningFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.eme.factory.MeEMExtraItemStackChemicalToItemStackFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.eme.factory.MeEMExtraItemStackToItemStackFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.eme.factory.MeEMExtraSawingFactoryBlockEntity;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.registry.ModBlockEntities;
import com.beipuo.mekenergistics.registry.ModBlocks;
import com.beipuo.mekenergistics.registry.ModMenuTypes;
import com.beipuo.mekenergistics.registry.machine.MachineFactoryRegistrar;
import com.jerry.mekextras.common.block.attribute.ExtraAttributeUpgradeSupport;
import io.github.masyumero.emextras.common.block.attribute.EMExtraAttribute;
import io.github.masyumero.emextras.common.block.attribute.EMExtraAttributeFactoryType;
import io.github.masyumero.emextras.common.block.attribute.EMExtraAttributeTier;
import io.github.masyumero.emextras.common.content.blocktype.EMExtraBlockShapes;
import io.github.masyumero.emextras.common.content.blocktype.EMExtraFactoryType;
import io.github.masyumero.emextras.api.tier.EMExtraTier;
import io.github.masyumero.emextras.common.item.EMExtraItemTierInstaller;
import io.github.masyumero.emextras.common.tier.EMExtraFactoryTier;
import java.util.Locale;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeFactoryType;
import mekanism.common.block.attribute.AttributeStateFacing;
import mekanism.common.block.attribute.AttributeUpgradeSupport;
import mekanism.common.block.attribute.Attributes;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.content.blocktype.FactoryType;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

public final class EvolvedMekanismExtrasCompat {
    private EvolvedMekanismExtrasCompat() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static TileEntityTypeRegistryObject<? extends TileEntityMekanism> registerFactoryMachine(
            MeMekanismMachine machine, MachineFactoryRegistrar registrar) {
        TileEntityTypeRegistryObject<?> registered = switch (machine.factoryType()) {
            case SMELTING, ENRICHING, CRUSHING -> registrar.register(machine, MeEMExtraItemStackToItemStackFactoryBlockEntity::new);
            case COMPRESSING, INJECTING, PURIFYING, INFUSING -> registrar.register(machine, MeEMExtraItemStackChemicalToItemStackFactoryBlockEntity::new);
            case COMBINING -> registrar.register(machine, MeEMExtraCombiningFactoryBlockEntity::new);
            case SAWING -> registrar.register(machine, MeEMExtraSawingFactoryBlockEntity::new);
        };
        return (TileEntityTypeRegistryObject) registered;
    }

    public static <TILE extends TileEntityMekanism> BlockTypeTile<TILE> createFactoryBlockType(
            MeMekanismMachine machine, TileEntityTypeRegistryObject<TILE> tileType) {
        EMExtraFactoryType type = emExtraFactoryType(machine.factoryType());
        var builder = BlockTypeTile.BlockTileBuilder
                .createBlock(() -> tileType, machine::translationKey)
                .withGui(() -> ModMenuTypes.ME_EM_EXTRA_FACTORY)
                .withEnergyConfig(machine.energyUsage(), machine.energyStorage())
                .with(new AttributeStateFacing(), Attributes.ACTIVE_LIGHT, Attributes.INVENTORY, Attributes.REDSTONE, Attributes.SECURITY, Attributes.COMPARATOR)
                .withSideConfig(machine.hasChemicalInput()
                        ? new TransmissionType[] {TransmissionType.ITEM, TransmissionType.ENERGY, TransmissionType.CHEMICAL}
                        : new TransmissionType[] {TransmissionType.ITEM, TransmissionType.ENERGY})
                .with(emExtraUpgradeSupport(machine.factoryType()))
                .with(new AttributeFactoryType(machine.factoryType()))
                .with(new EMExtraAttributeFactoryType(type))
                .with(new EMExtraAttributeTier<>(emExtraTier(machine)))
                .withCustomShape(EMExtraBlockShapes.getShape(type));
        MeMekanismMachine upgradeTarget = machine.getNextFactory();
        if (upgradeTarget != null) {
            builder.with(new MeUpgradeableAttribute(() -> ModBlocks.getMachineBlock(upgradeTarget).get()));
        }
        return builder.build();
    }

    private static AttributeUpgradeSupport emExtraUpgradeSupport(FactoryType type) {
        return switch (type) {
            case PURIFYING, INJECTING -> ExtraAttributeUpgradeSupport.EXTRA_ADVANCED_MACHINE_UPGRADES;
            case SMELTING, ENRICHING, CRUSHING, COMPRESSING, COMBINING, INFUSING, SAWING -> ExtraAttributeUpgradeSupport.EXTRA_MACHINE_UPGRADES;
        };
    }

    public static EMExtraFactoryTier emExtraTier(MeMekanismMachine machine) {
        return EMExtraFactoryTier.valueOf(machine.emExtraFactoryTierName().toUpperCase(Locale.ROOT));
    }

    public static EMExtraFactoryType emExtraFactoryType(FactoryType type) {
        return EMExtraFactoryType.valueOf(type.name());
    }

    @Nullable
    public static MeMekanismMachine getFactoryTarget(BlockState state) {
        EMExtraAttributeFactoryType typeAttribute = Attribute.get(state, EMExtraAttributeFactoryType.class);
        EMExtraFactoryTier tier = EMExtraAttribute.getEMExtraTier(state.getBlock(), EMExtraFactoryTier.class);
        if (typeAttribute == null || tier == null || typeAttribute.getFactoryType() == EMExtraFactoryType.ALLOYING) {
            return null;
        }
        FactoryType factoryType = FactoryType.valueOf(typeAttribute.getFactoryType().name());
        return MeMekanismMachine.getEvolvedMekanismExtrasFactory(tier.name().toLowerCase(Locale.ROOT), factoryType);
    }

    @Nullable
    public static MeMekanismMachine getInstallerTarget(MeMekanismMachine current, ItemStack stack) {
        if (!(stack.getItem() instanceof EMExtraItemTierInstaller installer)) {
            return null;
        }
        MeMekanismMachine target = current.emExtraFactoryTierName() == null
                && installer.getFromTier() == null
                && isTerminalEvolvedFactory(current)
                ? MeMekanismMachine.getEvolvedMekanismExtrasFactory(installer.getToTier().name().toLowerCase(Locale.ROOT), current.factoryType())
                : current.getNextFactory();
        if (target == null || target.emExtraFactoryTierName() == null) {
            return null;
        }
        EMExtraTier currentTier = current.emExtraFactoryTierName() == null
                ? null
                : emExtraTier(current).getEMExtraTier();
        EMExtraTier targetTier = emExtraTier(target).getEMExtraTier();
        return currentTier == installer.getFromTier() && targetTier == installer.getToTier() ? target : null;
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
        ModBlockEntities.registerGridNodeHost(event, holder, com.beipuo.mekenergistics.blockentity.compat.eme.factory.MeEvolvedMekanismExtrasFactoryAeMachine.class);
    }
}
