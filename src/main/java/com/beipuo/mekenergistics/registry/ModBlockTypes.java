package com.beipuo.mekenergistics.registry;

import com.beipuo.mekenergistics.block.attribute.MeUpgradeableAttribute;
import com.beipuo.mekenergistics.blockentity.MeMekanismMachineBlockEntity;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.compat.eme.EvolvedMekanismCompat;
import com.beipuo.mekenergistics.compat.eme.EvolvedMekanismExtrasCompat;
import com.beipuo.mekenergistics.compat.meke.MekanismExtrasCompat;
import com.beipuo.mekenergistics.compat.meke.MekanismExtrasMoreMachineCompat;
import com.beipuo.mekenergistics.compat.mekmm.MekanismMoreMachineAdvancedCompat;
import com.beipuo.mekenergistics.compat.mekmm.MekanismMoreMachineBaseCompat;
import java.util.EnumMap;
import java.util.Map;
import mekanism.api.Upgrade;
import mekanism.api.text.ILangEntry;
import mekanism.common.block.attribute.AttributeHasBounding;
import mekanism.common.block.attribute.AttributeFactoryType;
import mekanism.common.block.attribute.AttributeStateFacing;
import mekanism.common.block.attribute.AttributeUpgradeSupport;
import mekanism.common.block.attribute.AttributeTier;
import mekanism.common.block.attribute.Attributes;
import mekanism.common.content.blocktype.BlockShapes;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

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
        if (machine.isEvolvedMekanismExtrasFactory()) {
            return EvolvedMekanismExtrasCompat.createFactoryBlockType(machine, tileType);
        }
        if (machine.isMoreMachineAdvancedFactory()) {
            return MekanismMoreMachineAdvancedCompat.createAdvancedFactoryBlockType(machine, tileType);
        }
        if (machine.isMoreMachineFactory()) {
            return machine.extraFactoryTierName() == null
                    ? MekanismMoreMachineBaseCompat.createFactoryBlockType(machine, tileType)
                    : MekanismExtrasMoreMachineCompat.createFactoryBlockType(machine, tileType);
        }
        if (machine.isMoreMachineBaseMachine()) {
            return MekanismMoreMachineBaseCompat.createBaseBlockType(machine, tileType);
        }
        var builder = BlockTypeTile.BlockTileBuilder
                .createBlock(() -> tileType, lang(machine))
                .withGui(() -> ModMenuTypes.getMachineContainer(machine))
                .withEnergyConfig(machine.energyUsage(), machine.energyStorage())
                .with(new AttributeStateFacing(), Attributes.ACTIVE_LIGHT, Attributes.INVENTORY, Attributes.REDSTONE, Attributes.SECURITY, Attributes.COMPARATOR)
                .withSideConfig(sideConfigFor(machine))
                .with(upgradeSupportFor(machine));
        if (machine.factoryType() != null) {
            builder.with(new AttributeFactoryType(machine.factoryType()));
        } else if ("alloying".equals(machine.customFactoryTypeName())) {
            EvolvedMekanismCompat.withAlloyingFactoryType(builder);
        }
        if (machine.factoryTier() != null) {
            builder.with(new AttributeTier<>(machine.factoryTier()));
        }
        VoxelShape[] customShape = customShapeFor(machine);
        if (customShape != null) {
            builder.withCustomShape(customShape);
        }
        if (machine == MeMekanismMachine.SOLAR_NEUTRON_ACTIVATOR || machine == MeMekanismMachine.ISOTOPIC_CENTRIFUGE) {
            builder.with(AttributeHasBounding.ABOVE_ONLY);
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

    @Nullable
    private static VoxelShape[] customShapeFor(MeMekanismMachine machine) {
        if (machine.factoryType() != null && machine.factoryTier() != null) {
            return BlockShapes.getShape(machine.factoryTier(), machine.factoryType());
        }
        if ("alloying".equals(machine.customFactoryTypeName()) && machine.factoryTier() != null) {
            return EvolvedMekanismCompat.alloyingFactoryShape(machine.factoryTier());
        }
        return switch (machine) {
            case METALLURGIC_INFUSER -> BlockShapes.METALLURGIC_INFUSER;
            case SOLIDIFICATION_CHAMBER, THERMALIZER, CHEMIXER -> EvolvedMekanismCompat.shapeFor(machine);
            case PRESSURIZED_REACTION_CHAMBER -> BlockShapes.PRESSURIZED_REACTION_CHAMBER;
            case CHEMICAL_CRYSTALLIZER -> BlockShapes.CHEMICAL_CRYSTALLIZER;
            case CHEMICAL_DISSOLUTION_CHAMBER -> BlockShapes.CHEMICAL_DISSOLUTION_CHAMBER;
            case CHEMICAL_INFUSER -> BlockShapes.CHEMICAL_INFUSER;
            case CHEMICAL_OXIDIZER -> BlockShapes.CHEMICAL_OXIDIZER;
            case CHEMICAL_WASHER -> BlockShapes.CHEMICAL_WASHER;
            case ROTARY_CONDENSENTRATOR -> BlockShapes.ROTARY_CONDENSENTRATOR;
            case ELECTROLYTIC_SEPARATOR -> BlockShapes.ELECTROLYTIC_SEPARATOR;
            case DIGITAL_MINER -> BlockShapes.DIGITAL_MINER;
            case ELECTRIC_PUMP -> BlockShapes.ELECTRIC_PUMP;
            case FLUIDIC_PLENISHER -> BlockShapes.FLUIDIC_PLENISHER;
            case SOLAR_NEUTRON_ACTIVATOR -> BlockShapes.SOLAR_NEUTRON_ACTIVATOR;
            case RESISTIVE_HEATER -> BlockShapes.RESISTIVE_HEATER;
            case SEISMIC_VIBRATOR -> BlockShapes.SEISMIC_VIBRATOR;
            case LOGISTICAL_SORTER -> BlockShapes.LOGISTICAL_SORTER;
            case ISOTOPIC_CENTRIFUGE -> BlockShapes.ISOTOPIC_CENTRIFUGE;
            case ANTIPROTONIC_NUCLEOSYNTHESIZER -> BlockShapes.ANTIPROTONIC_NUCLEOSYNTHESIZER;
            case PIGMENT_MIXER -> BlockShapes.PIGMENT_MIXER;
            case MODIFICATION_STATION -> BlockShapes.MODIFICATION_STATION;
            default -> null;
        };
    }

    private static ILangEntry lang(MeMekanismMachine machine) {
        return machine::translationKey;
    }

    private static AttributeUpgradeSupport upgradeSupportFor(MeMekanismMachine machine) {
        return machine.isEvolvedMekanismFactory()
                ? AttributeUpgradeSupport.DEFAULT_MACHINE_UPGRADES
                : AttributeUpgradeSupport.create(Upgrade.SPEED, Upgrade.ENERGY);
    }

    private static TransmissionType[] sideConfigFor(MeMekanismMachine machine) {
        return switch (machine) {
            case PRESSURIZED_REACTION_CHAMBER ->
                    new TransmissionType[] {TransmissionType.ITEM, TransmissionType.CHEMICAL, TransmissionType.FLUID, TransmissionType.ENERGY};
            case SOLIDIFICATION_CHAMBER ->
                    new TransmissionType[] {TransmissionType.FLUID, TransmissionType.ITEM, TransmissionType.ENERGY};
            case THERMALIZER ->
                    new TransmissionType[] {TransmissionType.ITEM, TransmissionType.FLUID, TransmissionType.HEAT};
            case CHEMIXER ->
                    new TransmissionType[] {TransmissionType.ITEM, TransmissionType.ENERGY, TransmissionType.CHEMICAL};
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
