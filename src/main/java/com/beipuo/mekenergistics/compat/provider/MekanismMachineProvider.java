package com.beipuo.mekenergistics.compat.provider;

import appeng.api.networking.IInWorldGridNodeHost;
import com.beipuo.mekenergistics.block.attribute.MeUpgradeableAttribute;
import com.beipuo.mekenergistics.blockentity.MeMekanismMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.factory.MeCombiningFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.factory.MeItemStackChemicalToItemStackFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.factory.MeItemStackToItemStackFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.factory.MeSawingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeAntiprotonicNucleosynthesizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeChemicalCrystallizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeChemicalDissolutionChamberBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeChemicalInfuserBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeChemicalOxidizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeChemicalWasherBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeElectrolyticSeparatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeIsotopicCentrifugeBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeNutritionalLiquifierBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MePressurizedReactionChamberBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MePigmentExtractorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MePigmentMixerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeRotaryCondensentratorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeSolarNeutronActivatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MeAdvancedElectricMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MeCombinerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MeElectricMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MeFormulaicAssemblicatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MeMetallurgicInfuserBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MePaintingMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MePrecisionSawmillBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeDigitalMinerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeDimensionalStabilizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeElectricPumpBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeFluidicPlenisherBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeLogisticalSorterBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeModificationStationBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeOredictionificatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeResistiveHeaterBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeSeismicVibratorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeTeleporterBlockEntity;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.compat.catalog.CompatMachineCatalog;
import com.beipuo.mekenergistics.compat.catalog.CompatMachineFamily;
import com.beipuo.mekenergistics.compat.catalog.CompatMachineKind;
import com.beipuo.mekenergistics.compat.catalog.CompatMachineSpec;
import com.beipuo.mekenergistics.compat.catalog.CompatMod;
import com.beipuo.mekenergistics.registry.ModBlockEntities;
import com.beipuo.mekenergistics.registry.ModBlocks;
import com.beipuo.mekenergistics.registry.ModMenuTypes;
import com.beipuo.mekenergistics.registry.machine.MachineFactoryRegistrar;
import mekanism.api.Upgrade;
import mekanism.api.tier.BaseTier;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeFactoryType;
import mekanism.common.block.attribute.AttributeHasBounding;
import mekanism.common.block.attribute.AttributeStateFacing;
import mekanism.common.block.attribute.AttributeTier;
import mekanism.common.block.attribute.AttributeUpgradeSupport;
import mekanism.common.block.attribute.Attributes;
import mekanism.common.content.blocktype.BlockShapes;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.item.ItemTierInstaller;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;
import java.util.Map;

public final class MekanismMachineProvider extends AbstractCompatMachineProvider implements CompatMachineProvider {
    public MekanismMachineProvider() {
        super(CompatMod.MEKANISM, familyAdapters());
    }

    private static Map<CompatMachineFamily, CompatMachineFamilyAdapter> familyAdapters() {
        return Map.of(
                CompatMachineFamily.MEKANISM_MACHINE,
                CompatMachineFamilyAdapter.of(
                        spec -> ModMenuTypes.getCoreMachineContainer(spec.machine()),
                        (spec, registrar) -> registerMachine(spec.machine(), registrar),
                        MekanismMachineProvider::createMachineBlockType,
                        MekanismMachineProvider::registerMachineGridNodeHost),
                CompatMachineFamily.MEKANISM_FACTORY,
                CompatMachineFamilyAdapter.of(
                        spec -> ModMenuTypes.ME_FACTORY,
                        (spec, registrar) -> registerFactory(spec.machine(), registrar),
                        MekanismMachineProvider::createMachineBlockType,
                        MekanismMachineProvider::registerFactoryGridNodeHost));
    }

    @Override
    public boolean isInstaller(ItemStack stack) {
        return stack.getItem() instanceof ItemTierInstaller;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static TileEntityTypeRegistryObject<? extends TileEntityMekanism> registerMachine(
            MeMekanismMachine machine, MachineFactoryRegistrar registrar) {
        return switch (machine.identity()) {
            case METALLURGIC_INFUSER -> registrar.register(machine, MeMetallurgicInfuserBlockEntity::new);
            case COMBINER -> registrar.register(machine, MeCombinerBlockEntity::new);
            case PRECISION_SAWMILL -> registrar.register(machine, MePrecisionSawmillBlockEntity::new);
            case FORMULAIC_ASSEMBLICATOR -> registrar.register(machine, MeFormulaicAssemblicatorBlockEntity::new);
            case PRESSURIZED_REACTION_CHAMBER -> registrar.register(machine, MePressurizedReactionChamberBlockEntity::new);
            case CHEMICAL_CRYSTALLIZER -> registrar.register(machine, MeChemicalCrystallizerBlockEntity::new);
            case CHEMICAL_DISSOLUTION_CHAMBER -> registrar.register(machine, MeChemicalDissolutionChamberBlockEntity::new);
            case CHEMICAL_INFUSER -> registrar.register(machine, MeChemicalInfuserBlockEntity::new);
            case CHEMICAL_OXIDIZER -> registrar.register(machine, MeChemicalOxidizerBlockEntity::new);
            case CHEMICAL_WASHER -> registrar.register(machine, MeChemicalWasherBlockEntity::new);
            case ROTARY_CONDENSENTRATOR -> registrar.register(machine, MeRotaryCondensentratorBlockEntity::new);
            case ELECTROLYTIC_SEPARATOR -> registrar.register(machine, MeElectrolyticSeparatorBlockEntity::new);
            case SOLAR_NEUTRON_ACTIVATOR -> registrar.register(machine, MeSolarNeutronActivatorBlockEntity::new);
            case ISOTOPIC_CENTRIFUGE -> registrar.register(machine, MeIsotopicCentrifugeBlockEntity::new);
            case NUTRITIONAL_LIQUIFIER -> registrar.register(machine, MeNutritionalLiquifierBlockEntity::new);
            case ANTIPROTONIC_NUCLEOSYNTHESIZER -> registrar.register(machine, MeAntiprotonicNucleosynthesizerBlockEntity::new);
            case PIGMENT_EXTRACTOR -> registrar.register(machine, MePigmentExtractorBlockEntity::new);
            case PIGMENT_MIXER -> registrar.register(machine, MePigmentMixerBlockEntity::new);
            case PAINTING_MACHINE -> registrar.register(machine, MePaintingMachineBlockEntity::new);
            case ELECTRIC_PUMP -> registrar.register(machine, MeElectricPumpBlockEntity::new);
            case FLUIDIC_PLENISHER -> registrar.register(machine, MeFluidicPlenisherBlockEntity::new);
            case RESISTIVE_HEATER -> registrar.register(machine, MeResistiveHeaterBlockEntity::new);
            case SEISMIC_VIBRATOR -> registrar.register(machine, MeSeismicVibratorBlockEntity::new);
            case TELEPORTER -> registrar.register(machine, MeTeleporterBlockEntity::new);
            case OREDICTIONIFICATOR -> registrar.register(machine, MeOredictionificatorBlockEntity::new);
            case MODIFICATION_STATION -> registrar.register(machine, MeModificationStationBlockEntity::new);
            case DIGITAL_MINER -> registrar.register(machine, MeDigitalMinerBlockEntity::new);
            case LOGISTICAL_SORTER -> registrar.register(machine, MeLogisticalSorterBlockEntity::new);
            case DIMENSIONAL_STABILIZER -> registrar.register(machine, MeDimensionalStabilizerBlockEntity::new);
            default -> {
                if (machine.slotLayout() == MeMekanismMachine.SlotLayout.SINGLE_ITEM && machine.hasRecipeLogic()) {
                    yield registrar.register(machine, MeElectricMachineBlockEntity::new);
                }
                if (machine.hasAdvancedChemicalInput()) {
                    yield registrar.register(machine, MeAdvancedElectricMachineBlockEntity::new);
                }
                yield registrar.register(machine, MeMekanismMachineBlockEntity::new);
            }
        };
    }

    private static TileEntityTypeRegistryObject<? extends TileEntityMekanism> registerFactory(
            MeMekanismMachine machine, MachineFactoryRegistrar registrar) {
        return switch (machine.factoryType()) {
            case SMELTING, ENRICHING, CRUSHING -> registrar.register(machine, MeItemStackToItemStackFactoryBlockEntity::new);
            case COMPRESSING, INJECTING, PURIFYING, INFUSING ->
                    registrar.register(machine, MeItemStackChemicalToItemStackFactoryBlockEntity::new);
            case COMBINING -> registrar.register(machine, MeCombiningFactoryBlockEntity::new);
            case SAWING -> registrar.register(machine, MeSawingFactoryBlockEntity::new);
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockTypeTile<? extends TileEntityMekanism> createMachineBlockType(
            CompatMachineSpec spec, TileEntityTypeRegistryObject<? extends TileEntityMekanism> tileType) {
        return createBlockTypeTyped(spec, (TileEntityTypeRegistryObject) tileType);
    }

    private static <TILE extends TileEntityMekanism> BlockTypeTile<TILE> createBlockTypeTyped(
            CompatMachineSpec spec, TileEntityTypeRegistryObject<TILE> tileType) {
        MeMekanismMachine machine = spec.machine();
        var builder = BlockTypeTile.BlockTileBuilder
                .createBlock(() -> tileType, machine::translationKey)
                .withGui(() -> ModMenuTypes.getMachineContainer(machine))
                .withEnergyConfig(machine.energyUsage(), machine.energyStorage())
                .with(new AttributeStateFacing(), Attributes.ACTIVE_LIGHT, Attributes.INVENTORY, Attributes.REDSTONE,
                        Attributes.SECURITY, Attributes.COMPARATOR)
                .withSideConfig(sideConfigFor(machine))
                .with(AttributeUpgradeSupport.create(Upgrade.SPEED, Upgrade.ENERGY));
        if (machine.factoryType() != null) {
            builder.with(new AttributeFactoryType(machine.factoryType()));
        }
        if (machine.factoryTier() != null) {
            builder.with(new AttributeTier<>(machine.factoryTier()));
        }
        VoxelShape[] customShape = customShapeFor(machine);
        if (customShape != null) {
            builder.withCustomShape(customShape);
        }
        if (machine == MeMekanismMachine.SOLAR_NEUTRON_ACTIVATOR
                || machine == MeMekanismMachine.ISOTOPIC_CENTRIFUGE) {
            builder.with(AttributeHasBounding.ABOVE_ONLY);
        }
        addUpgradeTarget(builder, machine);
        return builder.build();
    }

    private static void addUpgradeTarget(BlockTypeTile.BlockTileBuilder<?, ?, ?> builder, MeMekanismMachine machine) {
        MeMekanismMachine target = machine.isFactory() ? machine.getNextFactory() : machine.getBasicFactory();
        if (target == null) {
            return;
        }
        builder.with(new MeUpgradeableAttribute(() -> ModBlocks.getMachineBlock(target).get()));
        if (target.provider() == CompatMod.MEKE && CompatMachineCatalog.isAvailable(target)) {
            CompatMachineProviders.get(CompatMod.MEKE)
                    .addUpgradeAttribute(builder, () -> ModBlocks.getMachineBlock(target).get());
        }
    }

    @Nullable
    private static VoxelShape[] customShapeFor(MeMekanismMachine machine) {
        if (machine.factoryType() != null && machine.factoryTier() != null) {
            return BlockShapes.getShape(machine.factoryTier(), machine.factoryType());
        }
        return switch (machine.identity()) {
            case METALLURGIC_INFUSER -> BlockShapes.METALLURGIC_INFUSER;
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

    private static TransmissionType[] sideConfigFor(MeMekanismMachine machine) {
        return switch (machine.identity()) {
            case PRESSURIZED_REACTION_CHAMBER -> new TransmissionType[] {
                    TransmissionType.ITEM, TransmissionType.CHEMICAL, TransmissionType.FLUID, TransmissionType.ENERGY};
            case CHEMICAL_INFUSER, ISOTOPIC_CENTRIFUGE, PIGMENT_MIXER -> new TransmissionType[] {
                    TransmissionType.CHEMICAL, TransmissionType.ITEM, TransmissionType.ENERGY};
            case CHEMICAL_WASHER, ROTARY_CONDENSENTRATOR -> new TransmissionType[] {
                    TransmissionType.CHEMICAL, TransmissionType.FLUID, TransmissionType.ITEM, TransmissionType.ENERGY};
            case ELECTROLYTIC_SEPARATOR -> new TransmissionType[] {
                    TransmissionType.FLUID, TransmissionType.CHEMICAL, TransmissionType.ITEM, TransmissionType.ENERGY};
            case NUTRITIONAL_LIQUIFIER -> new TransmissionType[] {
                    TransmissionType.ITEM, TransmissionType.FLUID, TransmissionType.ENERGY};
            case SOLAR_NEUTRON_ACTIVATOR -> new TransmissionType[] {TransmissionType.CHEMICAL, TransmissionType.ITEM};
            default -> machine.hasChemicalInput()
                    ? new TransmissionType[] {TransmissionType.ITEM, TransmissionType.ENERGY, TransmissionType.CHEMICAL}
                    : new TransmissionType[] {TransmissionType.ITEM, TransmissionType.ENERGY};
        };
    }

    private static void registerMachineGridNodeHost(CompatMachineSpec spec, RegisterCapabilitiesEvent event,
            TileEntityTypeRegistryObject<? extends TileEntityMekanism> holder) {
        Class<? extends IInWorldGridNodeHost> host = machineGridHost(spec.machine());
        if (host != null) {
            ModBlockEntities.registerGridNodeHost(event, holder, host);
        }
    }

    private static void registerFactoryGridNodeHost(CompatMachineSpec spec, RegisterCapabilitiesEvent event,
            TileEntityTypeRegistryObject<? extends TileEntityMekanism> holder) {
        ModBlockEntities.registerGridNodeHost(event, holder, factoryGridHost(spec.machine()));
    }

    @Nullable
    private static Class<? extends IInWorldGridNodeHost> machineGridHost(MeMekanismMachine machine) {
        return switch (machine.identity()) {
            case METALLURGIC_INFUSER -> MeMetallurgicInfuserBlockEntity.class;
            case COMBINER -> MeCombinerBlockEntity.class;
            case PRECISION_SAWMILL -> MePrecisionSawmillBlockEntity.class;
            case FORMULAIC_ASSEMBLICATOR -> MeFormulaicAssemblicatorBlockEntity.class;
            case PRESSURIZED_REACTION_CHAMBER -> MePressurizedReactionChamberBlockEntity.class;
            case CHEMICAL_CRYSTALLIZER -> MeChemicalCrystallizerBlockEntity.class;
            case CHEMICAL_DISSOLUTION_CHAMBER -> MeChemicalDissolutionChamberBlockEntity.class;
            case CHEMICAL_INFUSER -> MeChemicalInfuserBlockEntity.class;
            case CHEMICAL_OXIDIZER -> MeChemicalOxidizerBlockEntity.class;
            case CHEMICAL_WASHER -> MeChemicalWasherBlockEntity.class;
            case ROTARY_CONDENSENTRATOR -> MeRotaryCondensentratorBlockEntity.class;
            case ELECTROLYTIC_SEPARATOR -> MeElectrolyticSeparatorBlockEntity.class;
            case SOLAR_NEUTRON_ACTIVATOR -> MeSolarNeutronActivatorBlockEntity.class;
            case ISOTOPIC_CENTRIFUGE -> MeIsotopicCentrifugeBlockEntity.class;
            case NUTRITIONAL_LIQUIFIER -> MeNutritionalLiquifierBlockEntity.class;
            case ANTIPROTONIC_NUCLEOSYNTHESIZER -> MeAntiprotonicNucleosynthesizerBlockEntity.class;
            case PIGMENT_EXTRACTOR -> MePigmentExtractorBlockEntity.class;
            case PIGMENT_MIXER -> MePigmentMixerBlockEntity.class;
            case PAINTING_MACHINE -> MePaintingMachineBlockEntity.class;
            default -> {
                if (machine.slotLayout() == MeMekanismMachine.SlotLayout.SINGLE_ITEM && machine.hasRecipeLogic()) {
                    yield MeElectricMachineBlockEntity.class;
                }
                if (machine.hasAdvancedChemicalInput()) {
                    yield MeAdvancedElectricMachineBlockEntity.class;
                }
                yield null;
            }
        };
    }

    private static Class<? extends IInWorldGridNodeHost> factoryGridHost(MeMekanismMachine machine) {
        return switch (machine.factoryType()) {
            case SMELTING, ENRICHING, CRUSHING -> MeItemStackToItemStackFactoryBlockEntity.class;
            case COMPRESSING, INJECTING, PURIFYING, INFUSING -> MeItemStackChemicalToItemStackFactoryBlockEntity.class;
            case COMBINING -> MeCombiningFactoryBlockEntity.class;
            case SAWING -> MeSawingFactoryBlockEntity.class;
        };
    }

    @Override
    @Nullable
    public MeMekanismMachine resolveOriginalMachine(BlockState state) {
        AttributeFactoryType factoryType = Attribute.get(state, AttributeFactoryType.class);
        if (factoryType == null) {
            return null;
        }
        AttributeTier<?> tier = Attribute.get(state, AttributeTier.class);
        if (tier == null) {
            return MeMekanismMachine.getBaseMachine(factoryType.getFactoryType());
        }
        return tier.tier() instanceof FactoryTier factoryTier
                ? MeMekanismMachine.getFactory(factoryTier, factoryType.getFactoryType()) : null;
    }

    @Override
    @Nullable
    public MeMekanismMachine resolveInstallerUpgrade(MeMekanismMachine current, ItemStack stack) {
        if (!(stack.getItem() instanceof ItemTierInstaller installer)) {
            return null;
        }
        BaseTier currentTier = current.baseTier();
        BaseTier fromTier = installer.getFromTier();
        BaseTier toTier = installer.getToTier();
        if (currentTier != fromTier || currentTier == toTier) {
            return null;
        }
        MeMekanismMachine target = current.machineKind() == CompatMachineKind.MACHINE
                ? current.getBasicFactory() : current.getNextFactory();
        return target != null && target.baseTier() == toTier ? target : null;
    }
}
