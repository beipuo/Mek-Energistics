package com.beipuo.mekenergistics.compat.eme;

import com.beipuo.mekenergistics.block.attribute.MeUpgradeableAttribute;
import com.beipuo.mekenergistics.blockentity.compat.eme.factory.MeEMExtraAlloyingFactoryBlockEntity;
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
import fr.iglee42.evolvedmekanism.registries.EMFactoryType;
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
        if ("alloying".equals(machine.customFactoryTypeName())) {
            return registrar.register(machine, MeEMExtraAlloyingFactoryBlockEntity::new);
        }
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
        EMExtraFactoryType type = emExtraFactoryType(machine.factoryTypeName());
        var builder = BlockTypeTile.BlockTileBuilder
                .createBlock(() -> tileType, machine::translationKey)
                .withGui(() -> ModMenuTypes.ME_EM_EXTRA_FACTORY)
                .withEnergyConfig(machine.energyUsage(), machine.energyStorage())
                .with(new AttributeStateFacing(), Attributes.ACTIVE_LIGHT, Attributes.INVENTORY, Attributes.REDSTONE, Attributes.SECURITY, Attributes.COMPARATOR)
                .withSideConfig(machine.hasChemicalInput()
                        ? new TransmissionType[] {TransmissionType.ITEM, TransmissionType.ENERGY, TransmissionType.CHEMICAL}
                        : new TransmissionType[] {TransmissionType.ITEM, TransmissionType.ENERGY})
                .with(emExtraUpgradeSupport(machine.factoryTypeName()))
                .with(new AttributeFactoryType(attributeFactoryType(machine)))
                .with(new EMExtraAttributeFactoryType(type))
                .with(new EMExtraAttributeTier<>(emExtraTier(machine)))
                .withCustomShape(EMExtraBlockShapes.getShape(type));
        MeMekanismMachine upgradeTarget = machine.getNextFactory();
        if (upgradeTarget != null) {
            builder.with(new MeUpgradeableAttribute(() -> ModBlocks.getMachineBlock(upgradeTarget).get()));
        }
        return builder.build();
    }

    private static AttributeUpgradeSupport emExtraUpgradeSupport(String typeName) {
        if ("alloying".equals(typeName)) {
            return ExtraAttributeUpgradeSupport.EXTRA_MACHINE_UPGRADES;
        }
        return switch (FactoryType.valueOf(typeName.toUpperCase(Locale.ROOT))) {
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

    public static EMExtraFactoryType emExtraFactoryType(String typeName) {
        return EMExtraFactoryType.valueOf(typeName.toUpperCase(Locale.ROOT));
    }

    private static FactoryType attributeFactoryType(MeMekanismMachine machine) {
        return "alloying".equals(machine.customFactoryTypeName()) ? EMFactoryType.ALLOYING : machine.factoryType();
    }

    @Nullable
    public static MeMekanismMachine getFactoryTarget(BlockState state) {
        EMExtraAttributeFactoryType typeAttribute = Attribute.get(state, EMExtraAttributeFactoryType.class);
        EMExtraFactoryTier tier = EMExtraAttribute.getEMExtraTier(state.getBlock(), EMExtraFactoryTier.class);
        if (typeAttribute == null || tier == null) {
            return null;
        }
        return MeMekanismMachine.getEvolvedMekanismExtrasFactory(
                tier.name().toLowerCase(Locale.ROOT),
                typeAttribute.getFactoryType().getRegistryNameComponent());
    }

    @Nullable
    public static MeMekanismMachine getInstallerTarget(MeMekanismMachine current, ItemStack stack) {
        if (!(stack.getItem() instanceof EMExtraItemTierInstaller installer)) {
            return null;
        }
        EMExtraTier currentTier = current.emExtraFactoryTierName() == null
                ? null
                : emExtraTier(current).getEMExtraTier();
        if (currentTier != installer.getFromTier() || currentTier == installer.getToTier()) {
            return null;
        }
        MeMekanismMachine target = currentTier == null
                ? getFirstEmExtraFactoryTarget(current, installer.getToTier())
                : getEmExtraFactoryTarget(current, installer.getToTier());
        if (target == null || target.emExtraFactoryTierName() == null) {
            return null;
        }
        EMExtraTier targetTier = emExtraTier(target).getEMExtraTier();
        return targetTier == installer.getToTier() ? target : null;
    }

    @Nullable
    private static MeMekanismMachine getFirstEmExtraFactoryTarget(MeMekanismMachine current, EMExtraTier toTier) {
        if (!isTerminalEvolvedFactory(current)) {
            return null;
        }
        return getEmExtraFactoryTarget(current, toTier);
    }

    @Nullable
    private static MeMekanismMachine getEmExtraFactoryTarget(MeMekanismMachine current, EMExtraTier toTier) {
        return MeMekanismMachine.getEvolvedMekanismExtrasFactory(
                toTier.name().toLowerCase(Locale.ROOT),
                current.factoryTypeName());
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
