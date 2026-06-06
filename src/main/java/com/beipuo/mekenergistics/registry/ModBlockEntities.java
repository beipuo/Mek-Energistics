package com.beipuo.mekenergistics.registry;

import appeng.api.AECapabilities;
import appeng.api.networking.IInWorldGridNodeHost;
import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.machine.process.MeAdvancedElectricMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeAntiprotonicNucleosynthesizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeChemicalCrystallizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeChemicalDissolutionChamberBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeChemicalInfuserBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeChemicalOxidizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeChemicalWasherBlockEntity;
import com.beipuo.mekenergistics.blockentity.factory.MeCombiningFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MeCombinerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeDigitalMinerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeDimensionalStabilizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MeElectricMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeElectricPumpBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeElectrolyticSeparatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeFluidicPlenisherBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.process.MeFormulaicAssemblicatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeIsotopicCentrifugeBlockEntity;
import com.beipuo.mekenergistics.blockentity.factory.MeItemStackToItemStackFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.factory.MeItemStackChemicalToItemStackFactoryBlockEntity;
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
import com.beipuo.mekenergistics.blockentity.factory.MeSawingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeSeismicVibratorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.chemical.MeSolarNeutronActivatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.machine.utility.MeTeleporterBlockEntity;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.compat.OptionalCompatClasses;
import com.beipuo.mekenergistics.compat.eme.EvolvedMekanismExtrasCompat;
import com.beipuo.mekenergistics.compat.meke.MekanismExtrasCompat;
import com.beipuo.mekenergistics.compat.meke.MekanismExtrasMoreMachineCompat;
import com.beipuo.mekenergistics.compat.mekmm.MekanismMoreMachineAdvancedCompat;
import com.beipuo.mekenergistics.compat.mekmm.MekanismMoreMachineBaseCompat;
import com.beipuo.mekenergistics.registry.machine.MachineFactory;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.level.block.entity.BlockEntityType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registration.impl.TileEntityTypeDeferredRegister;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

public final class ModBlockEntities {
    private static final TileEntityTypeDeferredRegister BLOCK_ENTITIES = new TileEntityTypeDeferredRegister(MekEnergistics.MODID);
    private static final Map<MeMekanismMachine, TileEntityTypeRegistryObject<? extends TileEntityMekanism>> MACHINES =
            new EnumMap<>(MeMekanismMachine.class);

    static {
        for (MeMekanismMachine machine : MeMekanismMachine.values()) {
            if (machine.isAvailable()) {
                MACHINES.put(machine, registerMachine(machine));
            }
        }
    }

    public static <TILE extends TileEntityMekanism> TileEntityTypeRegistryObject<TILE> registerMachine(
            MeMekanismMachine machine, MachineFactory<TILE> factory) {
        return BLOCK_ENTITIES.mekBuilder(
                ModBlocks.getMachineBlock(machine),
                (pos, state) -> factory.create(machine, pos, state)
        ).serverTicker((level, pos, state, tile) -> {
                TileEntityMekanism.tickServer(level, pos, state, tile);
            }).clientTicker((level, pos, state, tile) -> {
                TileEntityMekanism.tickClient(level, pos, state, tile);
            }).withSimple(Capabilities.CONFIG_CARD).build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static TileEntityTypeRegistryObject<? extends TileEntityMekanism> registerMachine(MeMekanismMachine machine) {
        TileEntityTypeRegistryObject<?> registered;
        if (machine.isMekanismExtrasMekanismFactory()) {
            registered = MekanismExtrasCompat.registerFactoryMachine(machine, ModBlockEntities::registerMachine);
        } else if (machine.isEvolvedMekanismExtrasFactory()) {
            registered = EvolvedMekanismExtrasCompat.registerFactoryMachine(machine, ModBlockEntities::registerMachine);
        } else if (machine.isMoreMachineAdvancedFactory()) {
            registered = MekanismMoreMachineAdvancedCompat.registerAdvancedFactoryMachine(machine, ModBlockEntities::registerMachine);
        } else if (machine.isMoreMachineFactory()) {
            registered = machine.extraFactoryTierName() == null
                    ? MekanismMoreMachineBaseCompat.registerFactoryMachine(machine, ModBlockEntities::registerMachine)
                    : MekanismExtrasMoreMachineCompat.registerFactoryMachine(machine, ModBlockEntities::registerMachine);
        } else if (machine.isFactory()) {
            registered = registerFactoryMachine(machine);
        } else if (machine.isMoreMachineBaseMachine()) {
            registered = MekanismMoreMachineBaseCompat.registerBaseMachine(machine, ModBlockEntities::registerMachine);
        } else {
            registered = registerMekanismMachine(machine);
        }
        return (TileEntityTypeRegistryObject) registered;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static TileEntityTypeRegistryObject<? extends TileEntityMekanism> registerFactoryMachine(MeMekanismMachine machine) {
        TileEntityTypeRegistryObject<?> registered = registerMachine(machine, (MachineFactory) factoryRegistration(machine).factory());
        return (TileEntityTypeRegistryObject) registered;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static TileEntityTypeRegistryObject<? extends TileEntityMekanism> registerMekanismMachine(MeMekanismMachine machine) {
        TileEntityTypeRegistryObject<?> registered = registerMachine(machine, (MachineFactory) mekanismMachineRegistration(machine).factory());
        return (TileEntityTypeRegistryObject) registered;
    }

    public static final TileEntityTypeRegistryObject<? extends TileEntityMekanism> ME_METALLURGIC_INFUSER =
            getMachineBlockEntity(MeMekanismMachine.METALLURGIC_INFUSER);

    private ModBlockEntities() {
    }

    public static TileEntityTypeRegistryObject<? extends TileEntityMekanism> getMachineBlockEntity(MeMekanismMachine machine) {
        return MACHINES.get(machine);
    }

    public static boolean isMachineBlockEntity(BlockEntityType<?> type) {
        for (TileEntityTypeRegistryObject<? extends TileEntityMekanism> holder : MACHINES.values()) {
            if (holder.get() == type) {
                return true;
            }
        }
        return false;
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
        eventBus.addListener(ModBlockEntities::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (MeMekanismMachine machine : MeMekanismMachine.values()) {
            if (!machine.isAvailable()) {
                continue;
            }
            TileEntityTypeRegistryObject<? extends TileEntityMekanism> holder = MACHINES.get(machine);
            if (machine.isMekanismExtrasMekanismFactory()) {
                MekanismExtrasCompat.registerGridNodeHost(event, holder);
            } else if (machine.isEvolvedMekanismExtrasFactory()) {
                EvolvedMekanismExtrasCompat.registerGridNodeHost(event, holder);
            } else if (machine.isMoreMachineAdvancedFactory()) {
                MekanismMoreMachineAdvancedCompat.registerGridNodeHost(event, holder);
            } else if (machine.isMoreMachineFactory()) {
                if (machine.extraFactoryTierName() == null || !OptionalCompatClasses.hasMekanismExtrasMoreMachineFactories()) {
                    MekanismMoreMachineBaseCompat.registerGridNodeHost(event, holder);
                } else {
                    MekanismExtrasMoreMachineCompat.registerGridNodeHost(event, holder);
                }
            } else if (machine.isFactory()) {
                registerFactoryGridNodeHost(event, machine, holder);
            } else if (machine.isMoreMachineBaseMachine()) {
                MekanismMoreMachineBaseCompat.registerBaseGridNodeHost(event, machine, holder);
            } else {
                registerMekanismMachineGridNodeHost(event, machine, holder);
            }
        }
    }

    private static void registerFactoryGridNodeHost(
            RegisterCapabilitiesEvent event,
            MeMekanismMachine machine,
            TileEntityTypeRegistryObject<? extends TileEntityMekanism> holder) {
        Class<? extends IInWorldGridNodeHost> gridHost = factoryRegistration(machine).gridHost();
        if (gridHost != null) {
            registerGridNodeHost(event, holder, gridHost);
        }
    }

    private static void registerMekanismMachineGridNodeHost(
            RegisterCapabilitiesEvent event,
            MeMekanismMachine machine,
            TileEntityTypeRegistryObject<? extends TileEntityMekanism> holder) {
        Class<? extends IInWorldGridNodeHost> gridHost = mekanismMachineRegistration(machine).gridHost();
        if (gridHost != null) {
            registerGridNodeHost(event, holder, gridHost);
        }
    }

    private static MekanismMachineRegistration mekanismMachineRegistration(MeMekanismMachine machine) {
        return switch (machine) {
            case METALLURGIC_INFUSER -> ae(MeMetallurgicInfuserBlockEntity::new, MeMetallurgicInfuserBlockEntity.class);
            case COMBINER -> ae(MeCombinerBlockEntity::new, MeCombinerBlockEntity.class);
            case PRECISION_SAWMILL -> ae(MePrecisionSawmillBlockEntity::new, MePrecisionSawmillBlockEntity.class);
            case ELECTRIC_PUMP -> noAe(MeElectricPumpBlockEntity::new);
            case FLUIDIC_PLENISHER -> noAe(MeFluidicPlenisherBlockEntity::new);
            case RESISTIVE_HEATER -> noAe(MeResistiveHeaterBlockEntity::new);
            case SEISMIC_VIBRATOR -> noAe(MeSeismicVibratorBlockEntity::new);
            case TELEPORTER -> noAe(MeTeleporterBlockEntity::new);
            case OREDICTIONIFICATOR -> noAe(MeOredictionificatorBlockEntity::new);
            case MODIFICATION_STATION -> noAe(MeModificationStationBlockEntity::new);
            case DIGITAL_MINER -> noAe(MeDigitalMinerBlockEntity::new);
            case LOGISTICAL_SORTER -> noAe(MeLogisticalSorterBlockEntity::new);
            case DIMENSIONAL_STABILIZER -> noAe(MeDimensionalStabilizerBlockEntity::new);
            case FORMULAIC_ASSEMBLICATOR -> ae(MeFormulaicAssemblicatorBlockEntity::new, MeFormulaicAssemblicatorBlockEntity.class);
            case PRESSURIZED_REACTION_CHAMBER -> ae(MePressurizedReactionChamberBlockEntity::new, MePressurizedReactionChamberBlockEntity.class);
            case CHEMICAL_CRYSTALLIZER -> ae(MeChemicalCrystallizerBlockEntity::new, MeChemicalCrystallizerBlockEntity.class);
            case CHEMICAL_DISSOLUTION_CHAMBER -> ae(MeChemicalDissolutionChamberBlockEntity::new, MeChemicalDissolutionChamberBlockEntity.class);
            case CHEMICAL_INFUSER -> ae(MeChemicalInfuserBlockEntity::new, MeChemicalInfuserBlockEntity.class);
            case CHEMICAL_OXIDIZER -> ae(MeChemicalOxidizerBlockEntity::new, MeChemicalOxidizerBlockEntity.class);
            case CHEMICAL_WASHER -> ae(MeChemicalWasherBlockEntity::new, MeChemicalWasherBlockEntity.class);
            case ROTARY_CONDENSENTRATOR -> ae(MeRotaryCondensentratorBlockEntity::new, MeRotaryCondensentratorBlockEntity.class);
            case ELECTROLYTIC_SEPARATOR -> ae(MeElectrolyticSeparatorBlockEntity::new, MeElectrolyticSeparatorBlockEntity.class);
            case SOLAR_NEUTRON_ACTIVATOR -> ae(MeSolarNeutronActivatorBlockEntity::new, MeSolarNeutronActivatorBlockEntity.class);
            case ISOTOPIC_CENTRIFUGE -> ae(MeIsotopicCentrifugeBlockEntity::new, MeIsotopicCentrifugeBlockEntity.class);
            case NUTRITIONAL_LIQUIFIER -> ae(MeNutritionalLiquifierBlockEntity::new, MeNutritionalLiquifierBlockEntity.class);
            case ANTIPROTONIC_NUCLEOSYNTHESIZER -> ae(MeAntiprotonicNucleosynthesizerBlockEntity::new, MeAntiprotonicNucleosynthesizerBlockEntity.class);
            case PIGMENT_EXTRACTOR -> ae(MePigmentExtractorBlockEntity::new, MePigmentExtractorBlockEntity.class);
            case PIGMENT_MIXER -> ae(MePigmentMixerBlockEntity::new, MePigmentMixerBlockEntity.class);
            case PAINTING_MACHINE -> ae(MePaintingMachineBlockEntity::new, MePaintingMachineBlockEntity.class);
            default -> defaultMekanismMachineRegistration(machine);
        };
    }

    private static MekanismMachineRegistration defaultMekanismMachineRegistration(MeMekanismMachine machine) {
        if (machine.slotLayout() == MeMekanismMachine.SlotLayout.SINGLE_ITEM && machine.hasRecipeLogic()) {
            return ae(MeElectricMachineBlockEntity::new, MeElectricMachineBlockEntity.class);
        }
        if (machine.hasAdvancedChemicalInput()) {
            return ae(MeAdvancedElectricMachineBlockEntity::new, MeAdvancedElectricMachineBlockEntity.class);
        }
        return noAe(MeMekanismMachineBlockEntity::new);
    }

    private static <TILE extends TileEntityMekanism & IInWorldGridNodeHost> MekanismMachineRegistration ae(
            MachineFactory<TILE> factory,
            Class<TILE> gridHost) {
        return new MekanismMachineRegistration(factory, gridHost);
    }

    private static <TILE extends TileEntityMekanism> MekanismMachineRegistration noAe(MachineFactory<TILE> factory) {
        return new MekanismMachineRegistration(factory, null);
    }

    private static FactoryRegistration factoryRegistration(MeMekanismMachine machine) {
        return switch (machine.factoryType()) {
            case SMELTING, ENRICHING, CRUSHING -> factory(MeItemStackToItemStackFactoryBlockEntity::new, MeItemStackToItemStackFactoryBlockEntity.class);
            case COMPRESSING, INJECTING, PURIFYING, INFUSING -> factory(MeItemStackChemicalToItemStackFactoryBlockEntity::new, MeItemStackChemicalToItemStackFactoryBlockEntity.class);
            case COMBINING -> factory(MeCombiningFactoryBlockEntity::new, MeCombiningFactoryBlockEntity.class);
            case SAWING -> factory(MeSawingFactoryBlockEntity::new, MeSawingFactoryBlockEntity.class);
        };
    }

    private static <TILE extends TileEntityMekanism & IInWorldGridNodeHost> FactoryRegistration factory(
            MachineFactory<TILE> factory,
            Class<TILE> gridHost) {
        return new FactoryRegistration(factory, gridHost);
    }

    private record MekanismMachineRegistration(
            MachineFactory<? extends TileEntityMekanism> factory,
            @Nullable Class<? extends IInWorldGridNodeHost> gridHost) {
    }

    private record FactoryRegistration(
            MachineFactory<? extends TileEntityMekanism> factory,
            Class<? extends IInWorldGridNodeHost> gridHost) {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerGridNodeHost(
            RegisterCapabilitiesEvent event,
            TileEntityTypeRegistryObject<? extends TileEntityMekanism> holder,
            Class<? extends IInWorldGridNodeHost> tileClass) {
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                (net.minecraft.world.level.block.entity.BlockEntityType) holder.get(),
                (blockEntity, context) -> tileClass.isInstance(blockEntity) ? (IInWorldGridNodeHost) tileClass.cast(blockEntity) : null
        );
    }

}
