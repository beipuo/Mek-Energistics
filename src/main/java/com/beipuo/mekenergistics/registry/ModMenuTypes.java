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
import com.beipuo.mekenergistics.blockentity.machine.process.MeElectricMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeElectrolyticSeparatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MeFormulaicAssemblicatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.factory.MeItemStackToItemStackFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeIsotopicCentrifugeBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeMekanismMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MeMetallurgicInfuserBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeNutritionalLiquifierBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MePaintingMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MePigmentExtractorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MePigmentMixerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MePrecisionSawmillBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MePressurizedReactionChamberBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeRotaryCondensentratorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeSeismicVibratorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeSolarNeutronActivatorBlockEntity;
import com.beipuo.mekenergistics.menu.MePatternFormulaicAssemblicatorContainer;
import com.beipuo.mekenergistics.menu.MePatternMachineContainer;
import com.beipuo.mekenergistics.menu.MePatternMekanismTileContainer;
import com.beipuo.mekenergistics.compat.OptionalCompatClasses;
import com.beipuo.mekenergistics.compat.eme.EvolvedMekanismExtrasMenuTypes;
import com.beipuo.mekenergistics.compat.meke.MekanismExtrasAdvancedMenuTypes;
import com.beipuo.mekenergistics.compat.meke.MekanismExtrasMenuTypes;
import com.beipuo.mekenergistics.compat.meke.MekanismExtrasMoreMachineMenuTypes;
import com.beipuo.mekenergistics.compat.mekmm.MekanismMoreMachineAdvancedMenuTypes;
import com.beipuo.mekenergistics.compat.mekmm.MekanismMoreMachineMenuTypes;
import com.beipuo.mekenergistics.menu.factory.MePatternFactoryContainer;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import java.util.EnumMap;
import java.util.Map;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.container.type.MekanismContainerType;
import mekanism.common.tile.factory.TileEntityFactory;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

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
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeSeismicVibratorBlockEntity>> ME_SEISMIC_VIBRATOR =
            registerPatternTileContainer("me_seismic_vibrator", MeSeismicVibratorBlockEntity.class);
    public static final ContainerTypeRegistryObject<MePatternFormulaicAssemblicatorContainer> ME_FORMULAIC_ASSEMBLICATOR =
            registerFormulaicAssemblicatorContainer();
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MePressurizedReactionChamberBlockEntity>> ME_PRESSURIZED_REACTION_CHAMBER =
            registerPatternTileContainer("me_pressurized_reaction_chamber", MePressurizedReactionChamberBlockEntity.class, 0, 5);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeChemicalCrystallizerBlockEntity>> ME_CHEMICAL_CRYSTALLIZER =
            registerPatternTileContainer("me_chemical_crystallizer", MeChemicalCrystallizerBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeChemicalDissolutionChamberBlockEntity>> ME_CHEMICAL_DISSOLUTION_CHAMBER =
            registerPatternTileContainer("me_chemical_dissolution_chamber", MeChemicalDissolutionChamberBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeChemicalInfuserBlockEntity>> ME_CHEMICAL_INFUSER =
            registerPatternTileContainer("me_chemical_infuser", MeChemicalInfuserBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeChemicalOxidizerBlockEntity>> ME_CHEMICAL_OXIDIZER =
            registerPatternTileContainer("me_chemical_oxidizer", MeChemicalOxidizerBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeChemicalWasherBlockEntity>> ME_CHEMICAL_WASHER =
            registerPatternTileContainer("me_chemical_washer", MeChemicalWasherBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeRotaryCondensentratorBlockEntity>> ME_ROTARY_CONDENSENTRATOR =
            registerPatternTileContainer("me_rotary_condensentrator", MeRotaryCondensentratorBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeElectrolyticSeparatorBlockEntity>> ME_ELECTROLYTIC_SEPARATOR =
            registerPatternTileContainer("me_electrolytic_separator", MeElectrolyticSeparatorBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeSolarNeutronActivatorBlockEntity>> ME_SOLAR_NEUTRON_ACTIVATOR =
            registerPatternTileContainer("me_solar_neutron_activator", MeSolarNeutronActivatorBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeIsotopicCentrifugeBlockEntity>> ME_ISOTOPIC_CENTRIFUGE =
            registerPatternTileContainer("me_isotopic_centrifuge", MeIsotopicCentrifugeBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeNutritionalLiquifierBlockEntity>> ME_NUTRITIONAL_LIQUIFIER =
            registerPatternTileContainer("me_nutritional_liquifier", MeNutritionalLiquifierBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeAntiprotonicNucleosynthesizerBlockEntity>> ME_ANTIPROTONIC_NUCLEOSYNTHESIZER =
            registerPatternTileContainer("me_antiprotonic_nucleosynthesizer", MeAntiprotonicNucleosynthesizerBlockEntity.class, 10, 27);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MePigmentExtractorBlockEntity>> ME_PIGMENT_EXTRACTOR =
            registerPatternTileContainer("me_pigment_extractor", MePigmentExtractorBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MePigmentMixerBlockEntity>> ME_PIGMENT_MIXER =
            registerPatternTileContainer("me_pigment_mixer", MePigmentMixerBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MePaintingMachineBlockEntity>> ME_PAINTING_MACHINE =
            registerPatternTileContainer("me_painting_machine", MePaintingMachineBlockEntity.class);
    public static final ContainerTypeRegistryObject<MePatternFactoryContainer> ME_FACTORY =
            registerFactoryContainer();
    public static final ContainerTypeRegistryObject<? extends MekanismTileContainer<?>> ME_MORE_MACHINE_FACTORY =
            optionalHolder("me_more_machine_factory");
    public static final ContainerTypeRegistryObject<? extends MekanismTileContainer<?>> ME_ADVANCED_FACTORY =
            optionalHolder("me_advanced_factory");
    public static final ContainerTypeRegistryObject<? extends MekanismTileContainer<?>> ME_EXTRA_FACTORY =
            optionalHolder("me_extra_factory");
    public static final ContainerTypeRegistryObject<? extends MekanismTileContainer<?>> ME_EM_EXTRA_FACTORY =
            optionalHolder("me_em_extra_factory");
    public static final ContainerTypeRegistryObject<? extends MekanismTileContainer<?>> ME_EXTRA_MORE_MACHINE_FACTORY =
            optionalHolder("me_extra_more_machine_factory");
    public static final ContainerTypeRegistryObject<? extends MekanismTileContainer<?>> ME_EXTRA_ADVANCED_FACTORY =
            optionalHolder("me_extra_advanced_factory");
    private static final Map<MeMekanismMachine, ContainerTypeRegistryObject<?>> EXPLICIT_MACHINE_CONTAINERS = createExplicitMachineContainers();

    static {
        if (ModList.get().isLoaded("mekmm")) {
            MekanismMoreMachineMenuTypes.register(MENU_TYPES);
            if (OptionalCompatClasses.hasMekmmAdvancedFactories()) {
                MekanismMoreMachineAdvancedMenuTypes.register(MENU_TYPES);
            }
        }
        if (ModList.get().isLoaded("mekanism_extras")) {
            MekanismExtrasMenuTypes.register(MENU_TYPES);
            if (OptionalCompatClasses.hasMekanismExtrasMoreMachineFactories()) {
                MekanismExtrasMoreMachineMenuTypes.register(MENU_TYPES);
            }
            if (OptionalCompatClasses.hasMekanismExtrasAdvancedFactories()) {
                MekanismExtrasAdvancedMenuTypes.register(MENU_TYPES);
            }
        }
        if (ModList.get().isLoaded("emextras")) {
            EvolvedMekanismExtrasMenuTypes.register(MENU_TYPES);
        }
    }

    private ModMenuTypes() {
    }

    private static ContainerTypeRegistryObject<MePatternFactoryContainer> registerFactoryContainer() {
        ContainerTypeRegistryObject<MePatternFactoryContainer> holder = new ContainerTypeRegistryObject<>(
                ResourceLocation.fromNamespaceAndPath(MekEnergistics.MODID, "me_factory"));
        MENU_TYPES.registerMenu("me_factory", () -> MekanismContainerType.tile(TileEntityFactory.class,
                (id, inv, tile) -> new MePatternFactoryContainer(id, inv, (TileEntityFactory<?>) tile)));
        return holder;
    }

    private static ContainerTypeRegistryObject<MePatternFormulaicAssemblicatorContainer> registerFormulaicAssemblicatorContainer() {
        ContainerTypeRegistryObject<MePatternFormulaicAssemblicatorContainer> holder = new ContainerTypeRegistryObject<>(
                ResourceLocation.fromNamespaceAndPath(MekEnergistics.MODID, "me_formulaic_assemblicator"));
        MENU_TYPES.registerMenu("me_formulaic_assemblicator", () -> MekanismContainerType.tile(MeFormulaicAssemblicatorBlockEntity.class,
                (id, inv, tile) -> new MePatternFormulaicAssemblicatorContainer(id, inv, tile)));
        return holder;
    }

    private static <TILE extends TileEntityMekanism> ContainerTypeRegistryObject<MekanismTileContainer<TILE>> registerPatternTileContainer(
            String name, Class<TILE> tileClass) {
        return registerPatternTileContainer(name, tileClass, 0, 0);
    }

    private static <TILE extends TileEntityMekanism> ContainerTypeRegistryObject<MekanismTileContainer<TILE>> registerPatternTileContainer(
            String name, Class<TILE> tileClass, int offsetX, int offsetY) {
        return registerPatternTileContainer(name, tileClass, offsetX, offsetY, -1, -1, -1);
    }

    private static <TILE extends TileEntityMekanism> ContainerTypeRegistryObject<MekanismTileContainer<TILE>> registerPatternTileContainer(
            String name, Class<TILE> tileClass, int offsetX, int offsetY, int armorSlotsX, int armorSlotsY, int offhandOffset) {
        ContainerTypeRegistryObject<MekanismTileContainer<TILE>> holder = new ContainerTypeRegistryObject<>(
                ResourceLocation.fromNamespaceAndPath(MekEnergistics.MODID, name));
        MENU_TYPES.registerMenu(name, () -> MekanismContainerType.tile(tileClass,
                (id, inv, tile) -> new MePatternMekanismTileContainer<>(holder, id, inv, tile) {
                    @Override
                    protected int getInventoryXOffset() {
                        return super.getInventoryXOffset() + offsetX;
                    }

                    @Override
                    protected int getInventoryYOffset() {
                        return super.getInventoryYOffset() + offsetY;
                    }

                    @Override
                    protected void addInventorySlots(net.minecraft.world.entity.player.Inventory inv) {
                        super.addInventorySlots(inv);
                        if (armorSlotsX != -1 && armorSlotsY != -1) {
                            addArmorSlots(inv, armorSlotsX, armorSlotsY, offhandOffset);
                        }
                    }
                }));
        return holder;
    }

    private static ContainerTypeRegistryObject<? extends MekanismTileContainer<?>> optionalHolder(String name) {
        return new ContainerTypeRegistryObject<>(ResourceLocation.fromNamespaceAndPath(MekEnergistics.MODID, name));
    }

    private static <TILE extends TileEntityMekanism & MeAeMachine> ContainerTypeRegistryObject<MePatternMachineContainer<TILE>> registerPatternContainer(
            String name, Class<TILE> tileClass) {
        ContainerTypeRegistryObject<MePatternMachineContainer<TILE>> holder = new ContainerTypeRegistryObject<>(
                ResourceLocation.fromNamespaceAndPath(MekEnergistics.MODID, name));
        MENU_TYPES.registerMenu(name, () -> MekanismContainerType.tile(tileClass,
                (id, inv, tile) -> new MePatternMachineContainer<>(holder, id, inv, tile)));
        return holder;
    }

    private static Map<MeMekanismMachine, ContainerTypeRegistryObject<?>> createExplicitMachineContainers() {
        Map<MeMekanismMachine, ContainerTypeRegistryObject<?>> containers = new EnumMap<>(MeMekanismMachine.class);
        containers.put(MeMekanismMachine.METALLURGIC_INFUSER, ME_METALLURGIC_INFUSER);
        containers.put(MeMekanismMachine.COMBINER, ME_COMBINER);
        containers.put(MeMekanismMachine.PRECISION_SAWMILL, ME_PRECISION_SAWMILL);
        containers.put(MeMekanismMachine.SEISMIC_VIBRATOR, ME_SEISMIC_VIBRATOR);
        containers.put(MeMekanismMachine.FORMULAIC_ASSEMBLICATOR, ME_FORMULAIC_ASSEMBLICATOR);
        containers.put(MeMekanismMachine.PRESSURIZED_REACTION_CHAMBER, ME_PRESSURIZED_REACTION_CHAMBER);
        containers.put(MeMekanismMachine.CHEMICAL_CRYSTALLIZER, ME_CHEMICAL_CRYSTALLIZER);
        containers.put(MeMekanismMachine.CHEMICAL_DISSOLUTION_CHAMBER, ME_CHEMICAL_DISSOLUTION_CHAMBER);
        containers.put(MeMekanismMachine.CHEMICAL_INFUSER, ME_CHEMICAL_INFUSER);
        containers.put(MeMekanismMachine.CHEMICAL_OXIDIZER, ME_CHEMICAL_OXIDIZER);
        containers.put(MeMekanismMachine.CHEMICAL_WASHER, ME_CHEMICAL_WASHER);
        containers.put(MeMekanismMachine.ROTARY_CONDENSENTRATOR, ME_ROTARY_CONDENSENTRATOR);
        containers.put(MeMekanismMachine.ELECTROLYTIC_SEPARATOR, ME_ELECTROLYTIC_SEPARATOR);
        containers.put(MeMekanismMachine.SOLAR_NEUTRON_ACTIVATOR, ME_SOLAR_NEUTRON_ACTIVATOR);
        containers.put(MeMekanismMachine.ISOTOPIC_CENTRIFUGE, ME_ISOTOPIC_CENTRIFUGE);
        containers.put(MeMekanismMachine.NUTRITIONAL_LIQUIFIER, ME_NUTRITIONAL_LIQUIFIER);
        containers.put(MeMekanismMachine.ANTIPROTONIC_NUCLEOSYNTHESIZER, ME_ANTIPROTONIC_NUCLEOSYNTHESIZER);
        containers.put(MeMekanismMachine.PIGMENT_EXTRACTOR, ME_PIGMENT_EXTRACTOR);
        containers.put(MeMekanismMachine.PIGMENT_MIXER, ME_PIGMENT_MIXER);
        containers.put(MeMekanismMachine.PAINTING_MACHINE, ME_PAINTING_MACHINE);
        return containers;
    }

    public static ContainerTypeRegistryObject<? extends MekanismTileContainer<? extends TileEntityMekanism>> getMachineContainer(
            MeMekanismMachine machine) {
        if (machine.isMoreMachineAdvancedFactory()) {
            return machine.extraFactoryTierName() == null ? ME_ADVANCED_FACTORY : ME_EXTRA_ADVANCED_FACTORY;
        } else if (machine.isMoreMachineFactory()) {
            return machine.extraFactoryTierName() == null ? ME_MORE_MACHINE_FACTORY : ME_EXTRA_MORE_MACHINE_FACTORY;
        } else if (machine.extraFactoryTierName() != null && (machine.factoryType() != null || "alloying".equals(machine.customFactoryTypeName()))) {
            return ME_EXTRA_FACTORY;
        } else if (machine.isEvolvedMekanismExtrasFactory()) {
            return ME_EM_EXTRA_FACTORY;
        } else if (machine.isFactory()) {
            return ME_FACTORY;
        } else if (machine.slotLayout() == MeMekanismMachine.SlotLayout.SINGLE_ITEM && machine.hasRecipeLogic()) {
            return ME_ELECTRIC_MACHINE;
        } else if (machine.hasAdvancedChemicalInput()) {
            return ME_ADVANCED_ELECTRIC_MACHINE;
        }
        ContainerTypeRegistryObject<?> explicitContainer = EXPLICIT_MACHINE_CONTAINERS.get(machine);
        return explicitContainer == null ? ME_GENERIC_MACHINE : asMachineContainer(explicitContainer);
    }

    @SuppressWarnings("unchecked")
    private static ContainerTypeRegistryObject<? extends MekanismTileContainer<? extends TileEntityMekanism>> asMachineContainer(
            ContainerTypeRegistryObject<?> container) {
        return (ContainerTypeRegistryObject<? extends MekanismTileContainer<? extends TileEntityMekanism>>) container;
    }

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
