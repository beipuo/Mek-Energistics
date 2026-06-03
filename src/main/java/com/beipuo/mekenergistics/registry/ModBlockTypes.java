package com.beipuo.mekenergistics.registry;

import com.beipuo.mekenergistics.block.attribute.MeUpgradeableAttribute;
import com.beipuo.mekenergistics.blockentity.MeMekanismMachineBlockEntity;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.compat.meke.MekanismExtrasCompat;
import com.beipuo.mekenergistics.compat.mekmm.MekanismMoreMachineCompat;
import java.util.EnumMap;
import java.util.Map;
import mekanism.api.Upgrade;
import mekanism.api.text.ILangEntry;
import mekanism.common.block.attribute.AttributeHasBounding;
import mekanism.common.block.attribute.AttributeFactoryType;
import mekanism.common.block.attribute.AttributeStateFacing;
import mekanism.common.block.attribute.AttributeTier;
import mekanism.common.block.attribute.Attributes;
import mekanism.common.content.blocktype.BlockShapes;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.base.TileEntityMekanism;
import net.neoforged.fml.ModList;

public final class ModBlockTypes {
    private static final Map<MeMekanismMachine, BlockTypeTile<? extends TileEntityMekanism>> MACHINES =
            new EnumMap<>(MeMekanismMachine.class);

    static {
        for (MeMekanismMachine machine : MeMekanismMachine.values()) {
            if (machine.isAvailable()) {
                MACHINES.put(machine, createMachineBlockType(machine));
            }
        }
    }

    private ModBlockTypes() {
    }

    public static BlockTypeTile<? extends TileEntityMekanism> getMachineBlockType(MeMekanismMachine machine) {
        return MACHINES.get(machine);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockTypeTile<? extends TileEntityMekanism> createMachineBlockType(MeMekanismMachine machine) {
        return createMachineBlockType(machine, (mekanism.common.registration.impl.TileEntityTypeRegistryObject) ModBlockEntities.getMachineBlockEntity(machine));
    }

    private static <TILE extends TileEntityMekanism> BlockTypeTile<TILE> createMachineBlockType(
            MeMekanismMachine machine,
            mekanism.common.registration.impl.TileEntityTypeRegistryObject<TILE> tileType) {
        if (machine.isMekanismExtrasMekanismFactory()) {
            return MekanismExtrasCompat.createFactoryBlockType(machine, tileType);
        }
        if (machine.isMoreMachineAdvancedFactory()) {
            return MekanismMoreMachineCompat.createAdvancedFactoryBlockType(machine, tileType);
        }
        if (machine.isMoreMachineFactory()) {
            return MekanismMoreMachineCompat.createFactoryBlockType(machine, tileType);
        }
        if (machine.isMoreMachineBaseMachine()) {
            return MekanismMoreMachineCompat.createBaseBlockType(machine, tileType);
        }
        var builder = BlockTypeTile.BlockTileBuilder
                .createBlock(() -> tileType, lang(machine))
                .withGui(() -> ModMenuTypes.getMachineContainer(machine))
                .withEnergyConfig(machine.energyUsage(), machine.energyStorage())
                .with(new AttributeStateFacing(), Attributes.ACTIVE_LIGHT, Attributes.INVENTORY, Attributes.REDSTONE, Attributes.SECURITY, Attributes.COMPARATOR)
                .withSideConfig(sideConfigFor(machine))
                .withSupportedUpgrades(Upgrade.SPEED, Upgrade.ENERGY);
        if (machine.factoryType() != null) {
            builder.with(new AttributeFactoryType(machine.factoryType()));
        }
        if (machine.factoryTier() != null) {
            builder.with(new AttributeTier<>(machine.factoryTier()));
        }
        if (machine == MeMekanismMachine.ISOTOPIC_CENTRIFUGE) {
            builder.withCustomShape(BlockShapes.ISOTOPIC_CENTRIFUGE)
                    .with(AttributeHasBounding.ABOVE_ONLY);
        }
        MeMekanismMachine upgradeTarget = machine.isFactory() ? machine.getNextFactory() : machine.getBasicFactory();
        if (upgradeTarget != null) {
            builder.with(new MeUpgradeableAttribute(() -> ModBlocks.getMachineBlock(upgradeTarget).get()));
            if (upgradeTarget.extraFactoryTierName() != null && ModList.get().isLoaded("mekanism_extras")) {
                MekanismExtrasCompat.withExtraUpgradeable(builder, () -> ModBlocks.getMachineBlock(upgradeTarget).get());
            }
        }
        return builder.build();
    }

    private static ILangEntry lang(MeMekanismMachine machine) {
        return machine::translationKey;
    }

    private static TransmissionType[] sideConfigFor(MeMekanismMachine machine) {
        return switch (machine) {
            case PRESSURIZED_REACTION_CHAMBER ->
                    new TransmissionType[] {TransmissionType.ITEM, TransmissionType.CHEMICAL, TransmissionType.FLUID, TransmissionType.ENERGY};
            case CHEMICAL_INFUSER, ISOTOPIC_CENTRIFUGE, PIGMENT_MIXER ->
                    new TransmissionType[] {TransmissionType.CHEMICAL, TransmissionType.ITEM, TransmissionType.ENERGY};
            case CHEMICAL_WASHER, ROTARY_CONDENSENTRATOR ->
                    new TransmissionType[] {TransmissionType.CHEMICAL, TransmissionType.FLUID, TransmissionType.ITEM, TransmissionType.ENERGY};
            case ELECTROLYTIC_SEPARATOR ->
                    new TransmissionType[] {TransmissionType.FLUID, TransmissionType.CHEMICAL, TransmissionType.ITEM, TransmissionType.ENERGY};
            case NUTRITIONAL_LIQUIFIER ->
                    new TransmissionType[] {TransmissionType.ITEM, TransmissionType.FLUID, TransmissionType.ENERGY};
            case SOLAR_NEUTRON_ACTIVATOR ->
                    new TransmissionType[] {TransmissionType.CHEMICAL, TransmissionType.ITEM};
            default -> machine.hasChemicalInput()
                    ? new TransmissionType[] {TransmissionType.ITEM, TransmissionType.ENERGY, TransmissionType.CHEMICAL}
                    : new TransmissionType[] {TransmissionType.ITEM, TransmissionType.ENERGY};
        };
    }
}
