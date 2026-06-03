package com.beipuo.mekenergistics.registry;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.machine.process.MeAdvancedElectricMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeAntiprotonicNucleosynthesizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeChemicalCrystallizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeChemicalDissolutionChamberBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeChemicalInfuserBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeChemicalOxidizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeChemicalWasherBlockEntity;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.machine.process.MeCombinerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeDigitalMinerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeDimensionalStabilizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MeElectricMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeElectricPumpBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeElectrolyticSeparatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeFluidicPlenisherBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MeFormulaicAssemblicatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.factory.MeItemStackToItemStackFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeIsotopicCentrifugeBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeLogisticalSorterBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeMekanismMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MeMetallurgicInfuserBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeModificationStationBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeNutritionalLiquifierBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeOredictionificatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MePaintingMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MePigmentExtractorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MePigmentMixerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MePrecisionSawmillBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MePressurizedReactionChamberBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeResistiveHeaterBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeRotaryCondensentratorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeSeismicVibratorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeSolarNeutronActivatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeTeleporterBlockEntity;
import com.beipuo.mekenergistics.menu.MePatternMachineContainer;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import mekanism.common.inventory.container.tile.FactoryContainer;
import mekanism.common.inventory.container.tile.FormulaicAssemblicatorContainer;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.container.type.MekanismContainerType;
import mekanism.common.tile.factory.TileEntityFactory;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;

public final class ModMenuTypes {
    private static final ContainerTypeDeferredRegister MENU_TYPES = new ContainerTypeDeferredRegister(MekEnergistics.MODID);

    public static final ContainerTypeRegistryObject<MePatternMachineContainer<MeElectricMachineBlockEntity>> ME_ELECTRIC_MACHINE =
            registerPatternContainer("me_electric_machine", MeElectricMachineBlockEntity.class);
    public static final ContainerTypeRegistryObject<MePatternMachineContainer<MeMekanismMachineBlockEntity>> ME_GENERIC_MACHINE =
            registerPatternContainer("me_generic_machine", MeMekanismMachineBlockEntity.class);
    public static final ContainerTypeRegistryObject<MePatternMachineContainer<MeAdvancedElectricMachineBlockEntity>> ME_ADVANCED_ELECTRIC_MACHINE =
            registerPatternContainer("me_advanced_electric_machine", MeAdvancedElectricMachineBlockEntity.class);
    public static final ContainerTypeRegistryObject<MePatternMachineContainer<MeMetallurgicInfuserBlockEntity>> ME_METALLURGIC_INFUSER =
            registerPatternContainer("me_metallurgic_infuser", MeMetallurgicInfuserBlockEntity.class);
    public static final ContainerTypeRegistryObject<MePatternMachineContainer<MeCombinerBlockEntity>> ME_COMBINER =
            registerPatternContainer("me_combiner", MeCombinerBlockEntity.class);
    public static final ContainerTypeRegistryObject<MePatternMachineContainer<MePrecisionSawmillBlockEntity>> ME_PRECISION_SAWMILL =
            registerPatternContainer("me_precision_sawmill", MePrecisionSawmillBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeElectricPumpBlockEntity>> ME_ELECTRIC_PUMP =
            MENU_TYPES.register("me_electric_pump", MeElectricPumpBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeFluidicPlenisherBlockEntity>> ME_FLUIDIC_PLENISHER =
            MENU_TYPES.register("me_fluidic_plenisher", MeFluidicPlenisherBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeResistiveHeaterBlockEntity>> ME_RESISTIVE_HEATER =
            MENU_TYPES.register("me_resistive_heater", MeResistiveHeaterBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeSeismicVibratorBlockEntity>> ME_SEISMIC_VIBRATOR =
            MENU_TYPES.register("me_seismic_vibrator", MeSeismicVibratorBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeTeleporterBlockEntity>> ME_TELEPORTER =
            MENU_TYPES.custom("me_teleporter", MeTeleporterBlockEntity.class).offset(0, 74).build();
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeOredictionificatorBlockEntity>> ME_OREDICTIONIFICATOR =
            MENU_TYPES.custom("me_oredictionificator", MeOredictionificatorBlockEntity.class).offset(30, 64).build();
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeModificationStationBlockEntity>> ME_MODIFICATION_STATION =
            MENU_TYPES.custom("me_modification_station", MeModificationStationBlockEntity.class).offset(0, 64).armorSideBar(8, 8, 8).build();
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeDigitalMinerBlockEntity>> ME_DIGITAL_MINER =
            MENU_TYPES.custom("me_digital_miner", MeDigitalMinerBlockEntity.class).offset(0, 76).build();
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeLogisticalSorterBlockEntity>> ME_LOGISTICAL_SORTER =
            MENU_TYPES.custom("me_logistical_sorter", MeLogisticalSorterBlockEntity.class).offset(50, 88).build();
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeDimensionalStabilizerBlockEntity>> ME_DIMENSIONAL_STABILIZER =
            MENU_TYPES.register("me_dimensional_stabilizer", MeDimensionalStabilizerBlockEntity.class);
    public static final ContainerTypeRegistryObject<FormulaicAssemblicatorContainer> ME_FORMULAIC_ASSEMBLICATOR =
            registerFormulaicAssemblicatorContainer();
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MePressurizedReactionChamberBlockEntity>> ME_PRESSURIZED_REACTION_CHAMBER =
            MENU_TYPES.custom("me_pressurized_reaction_chamber", MePressurizedReactionChamberBlockEntity.class).offset(0, 5).build();
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeChemicalCrystallizerBlockEntity>> ME_CHEMICAL_CRYSTALLIZER =
            MENU_TYPES.register("me_chemical_crystallizer", MeChemicalCrystallizerBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeChemicalDissolutionChamberBlockEntity>> ME_CHEMICAL_DISSOLUTION_CHAMBER =
            MENU_TYPES.register("me_chemical_dissolution_chamber", MeChemicalDissolutionChamberBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeChemicalInfuserBlockEntity>> ME_CHEMICAL_INFUSER =
            MENU_TYPES.register("me_chemical_infuser", MeChemicalInfuserBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeChemicalOxidizerBlockEntity>> ME_CHEMICAL_OXIDIZER =
            MENU_TYPES.register("me_chemical_oxidizer", MeChemicalOxidizerBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeChemicalWasherBlockEntity>> ME_CHEMICAL_WASHER =
            MENU_TYPES.register("me_chemical_washer", MeChemicalWasherBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeRotaryCondensentratorBlockEntity>> ME_ROTARY_CONDENSENTRATOR =
            MENU_TYPES.register("me_rotary_condensentrator", MeRotaryCondensentratorBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeElectrolyticSeparatorBlockEntity>> ME_ELECTROLYTIC_SEPARATOR =
            MENU_TYPES.register("me_electrolytic_separator", MeElectrolyticSeparatorBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeSolarNeutronActivatorBlockEntity>> ME_SOLAR_NEUTRON_ACTIVATOR =
            MENU_TYPES.register("me_solar_neutron_activator", MeSolarNeutronActivatorBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeIsotopicCentrifugeBlockEntity>> ME_ISOTOPIC_CENTRIFUGE =
            MENU_TYPES.register("me_isotopic_centrifuge", MeIsotopicCentrifugeBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeNutritionalLiquifierBlockEntity>> ME_NUTRITIONAL_LIQUIFIER =
            MENU_TYPES.register("me_nutritional_liquifier", MeNutritionalLiquifierBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeAntiprotonicNucleosynthesizerBlockEntity>> ME_ANTIPROTONIC_NUCLEOSYNTHESIZER =
            MENU_TYPES.custom("me_antiprotonic_nucleosynthesizer", MeAntiprotonicNucleosynthesizerBlockEntity.class).offset(10, 27).build();
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MePigmentExtractorBlockEntity>> ME_PIGMENT_EXTRACTOR =
            MENU_TYPES.register("me_pigment_extractor", MePigmentExtractorBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MePigmentMixerBlockEntity>> ME_PIGMENT_MIXER =
            MENU_TYPES.register("me_pigment_mixer", MePigmentMixerBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MePaintingMachineBlockEntity>> ME_PAINTING_MACHINE =
            MENU_TYPES.register("me_painting_machine", MePaintingMachineBlockEntity.class);
    public static final ContainerTypeRegistryObject<FactoryContainer> ME_FACTORY =
            registerFactoryContainer();

    private ModMenuTypes() {
    }

    private static ContainerTypeRegistryObject<FactoryContainer> registerFactoryContainer() {
        ContainerTypeRegistryObject<FactoryContainer> holder = new ContainerTypeRegistryObject<>(
                ResourceLocation.fromNamespaceAndPath(MekEnergistics.MODID, "me_factory"));
        MENU_TYPES.registerMenu("me_factory", () -> MekanismContainerType.tile(TileEntityFactory.class,
                (id, inv, tile) -> new FactoryContainer(id, inv, (TileEntityFactory<?>) tile)));
        return holder;
    }

    private static ContainerTypeRegistryObject<FormulaicAssemblicatorContainer> registerFormulaicAssemblicatorContainer() {
        ContainerTypeRegistryObject<FormulaicAssemblicatorContainer> holder = new ContainerTypeRegistryObject<>(
                ResourceLocation.fromNamespaceAndPath(MekEnergistics.MODID, "me_formulaic_assemblicator"));
        MENU_TYPES.registerMenu("me_formulaic_assemblicator", () -> MekanismContainerType.tile(MeFormulaicAssemblicatorBlockEntity.class,
                (id, inv, tile) -> new FormulaicAssemblicatorContainer(id, inv, tile)));
        return holder;
    }

    private static <TILE extends TileEntityMekanism & MeAeMachine> ContainerTypeRegistryObject<MePatternMachineContainer<TILE>> registerPatternContainer(
            String name, Class<TILE> tileClass) {
        ContainerTypeRegistryObject<MePatternMachineContainer<TILE>> holder = new ContainerTypeRegistryObject<>(
                ResourceLocation.fromNamespaceAndPath(MekEnergistics.MODID, name));
        MENU_TYPES.registerMenu(name, () -> MekanismContainerType.tile(tileClass,
                (id, inv, tile) -> new MePatternMachineContainer<>(holder, id, inv, tile)));
        return holder;
    }

    public static ContainerTypeRegistryObject<? extends MekanismTileContainer<? extends TileEntityMekanism>> getMachineContainer(
            MeMekanismMachine machine) {
        if (machine.isFactory()) {
            return ME_FACTORY;
        } else if (machine.slotLayout() == MeMekanismMachine.SlotLayout.SINGLE_ITEM && machine.hasRecipeLogic()) {
            return ME_ELECTRIC_MACHINE;
        } else if (machine.hasAdvancedChemicalInput()) {
            return ME_ADVANCED_ELECTRIC_MACHINE;
        } else if (machine == MeMekanismMachine.METALLURGIC_INFUSER) {
            return ME_METALLURGIC_INFUSER;
        } else if (machine == MeMekanismMachine.COMBINER) {
            return ME_COMBINER;
        } else if (machine == MeMekanismMachine.PRECISION_SAWMILL) {
            return ME_PRECISION_SAWMILL;
        } else if (machine == MeMekanismMachine.ELECTRIC_PUMP) {
            return ME_ELECTRIC_PUMP;
        } else if (machine == MeMekanismMachine.FLUIDIC_PLENISHER) {
            return ME_FLUIDIC_PLENISHER;
        } else if (machine == MeMekanismMachine.RESISTIVE_HEATER) {
            return ME_RESISTIVE_HEATER;
        } else if (machine == MeMekanismMachine.SEISMIC_VIBRATOR) {
            return ME_SEISMIC_VIBRATOR;
        } else if (machine == MeMekanismMachine.TELEPORTER) {
            return ME_TELEPORTER;
        } else if (machine == MeMekanismMachine.OREDICTIONIFICATOR) {
            return ME_OREDICTIONIFICATOR;
        } else if (machine == MeMekanismMachine.MODIFICATION_STATION) {
            return ME_MODIFICATION_STATION;
        } else if (machine == MeMekanismMachine.DIGITAL_MINER) {
            return ME_DIGITAL_MINER;
        } else if (machine == MeMekanismMachine.LOGISTICAL_SORTER) {
            return ME_LOGISTICAL_SORTER;
        } else if (machine == MeMekanismMachine.DIMENSIONAL_STABILIZER) {
            return ME_DIMENSIONAL_STABILIZER;
        } else if (machine == MeMekanismMachine.FORMULAIC_ASSEMBLICATOR) {
            return ME_FORMULAIC_ASSEMBLICATOR;
        } else if (machine == MeMekanismMachine.PRESSURIZED_REACTION_CHAMBER) {
            return ME_PRESSURIZED_REACTION_CHAMBER;
        } else if (machine == MeMekanismMachine.CHEMICAL_CRYSTALLIZER) {
            return ME_CHEMICAL_CRYSTALLIZER;
        } else if (machine == MeMekanismMachine.CHEMICAL_DISSOLUTION_CHAMBER) {
            return ME_CHEMICAL_DISSOLUTION_CHAMBER;
        } else if (machine == MeMekanismMachine.CHEMICAL_INFUSER) {
            return ME_CHEMICAL_INFUSER;
        } else if (machine == MeMekanismMachine.CHEMICAL_OXIDIZER) {
            return ME_CHEMICAL_OXIDIZER;
        } else if (machine == MeMekanismMachine.CHEMICAL_WASHER) {
            return ME_CHEMICAL_WASHER;
        } else if (machine == MeMekanismMachine.ROTARY_CONDENSENTRATOR) {
            return ME_ROTARY_CONDENSENTRATOR;
        } else if (machine == MeMekanismMachine.ELECTROLYTIC_SEPARATOR) {
            return ME_ELECTROLYTIC_SEPARATOR;
        } else if (machine == MeMekanismMachine.SOLAR_NEUTRON_ACTIVATOR) {
            return ME_SOLAR_NEUTRON_ACTIVATOR;
        } else if (machine == MeMekanismMachine.ISOTOPIC_CENTRIFUGE) {
            return ME_ISOTOPIC_CENTRIFUGE;
        } else if (machine == MeMekanismMachine.NUTRITIONAL_LIQUIFIER) {
            return ME_NUTRITIONAL_LIQUIFIER;
        } else if (machine == MeMekanismMachine.ANTIPROTONIC_NUCLEOSYNTHESIZER) {
            return ME_ANTIPROTONIC_NUCLEOSYNTHESIZER;
        } else if (machine == MeMekanismMachine.PIGMENT_EXTRACTOR) {
            return ME_PIGMENT_EXTRACTOR;
        } else if (machine == MeMekanismMachine.PIGMENT_MIXER) {
            return ME_PIGMENT_MIXER;
        } else if (machine == MeMekanismMachine.PAINTING_MACHINE) {
            return ME_PAINTING_MACHINE;
        }
        return ME_GENERIC_MACHINE;
    }

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
