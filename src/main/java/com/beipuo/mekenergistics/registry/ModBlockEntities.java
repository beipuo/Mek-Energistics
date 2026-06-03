package com.beipuo.mekenergistics.registry;

import appeng.api.AECapabilities;
import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.MeAdvancedElectricMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeAntiprotonicNucleosynthesizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeChemicalCrystallizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeChemicalDissolutionChamberBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeChemicalInfuserBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeChemicalOxidizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeChemicalWasherBlockEntity;
import com.beipuo.mekenergistics.blockentity.factory.MeCombiningFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeCombinerBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeDigitalMinerBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeDimensionalStabilizerBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeElectricMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeElectricPumpBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeElectrolyticSeparatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeFluidicPlenisherBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeFormulaicAssemblicatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeIsotopicCentrifugeBlockEntity;
import com.beipuo.mekenergistics.blockentity.factory.MeItemStackToItemStackFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.factory.MeItemStackChemicalToItemStackFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeLogisticalSorterBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeMekanismMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeMetallurgicInfuserBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeModificationStationBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeNutritionalLiquifierBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeOredictionificatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.MePaintingMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.MePigmentExtractorBlockEntity;
import com.beipuo.mekenergistics.blockentity.MePigmentMixerBlockEntity;
import com.beipuo.mekenergistics.blockentity.MePrecisionSawmillBlockEntity;
import com.beipuo.mekenergistics.blockentity.MePressurizedReactionChamberBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeResistiveHeaterBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeRotaryCondensentratorBlockEntity;
import com.beipuo.mekenergistics.blockentity.factory.MeSawingFactoryBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeSeismicVibratorBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeSolarNeutronActivatorBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeTeleporterBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.machine.MeLatheBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.machine.MeRecyclerBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.machine.MeRollingMillBlockEntity;
import com.beipuo.mekenergistics.blockentity.compat.mekmm.machine.MeStamperBlockEntity;
import com.beipuo.mekenergistics.common.MeMekanismMachine;
import com.beipuo.mekenergistics.compat.meke.MekanismExtrasCompat;
import com.beipuo.mekenergistics.compat.mekmm.MekanismMoreMachineCompat;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import mekanism.common.registration.impl.TileEntityTypeDeferredRegister;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.content.blocktype.FactoryType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.minecraft.world.level.block.state.BlockState;

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
            }).build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static TileEntityTypeRegistryObject<? extends TileEntityMekanism> registerMachine(MeMekanismMachine machine) {
        TileEntityTypeRegistryObject<?> registered;
        if (machine.isMekanismExtrasMekanismFactory()) {
            registered = MekanismExtrasCompat.registerFactoryMachine(machine, ModBlockEntities::registerMachine);
        } else if (machine.isMoreMachineAdvancedFactory()) {
            registered = MekanismMoreMachineCompat.registerAdvancedFactoryMachine(machine, ModBlockEntities::registerMachine);
        } else if (machine.isMoreMachineFactory()) {
            registered = MekanismMoreMachineCompat.registerFactoryMachine(machine, ModBlockEntities::registerMachine);
        } else if (machine.isFactory()) {
            registered = registerFactoryMachine(machine);
        } else if (machine.isMoreMachineBaseMachine()) {
            registered = registerMoreMachineBaseMachine(machine);
        } else if (machine.slotLayout() == MeMekanismMachine.SlotLayout.SINGLE_ITEM && machine.hasRecipeLogic()) {
            registered = registerMachine(machine, MeElectricMachineBlockEntity::new);
        } else if (machine.hasAdvancedChemicalInput()) {
            registered = registerMachine(machine, MeAdvancedElectricMachineBlockEntity::new);
        } else if (machine == MeMekanismMachine.METALLURGIC_INFUSER) {
            registered = registerMachine(machine, MeMetallurgicInfuserBlockEntity::new);
        } else if (machine == MeMekanismMachine.COMBINER) {
            registered = registerMachine(machine, MeCombinerBlockEntity::new);
        } else if (machine == MeMekanismMachine.PRECISION_SAWMILL) {
            registered = registerMachine(machine, MePrecisionSawmillBlockEntity::new);
        } else if (machine == MeMekanismMachine.ELECTRIC_PUMP) {
            registered = registerMachine(machine, MeElectricPumpBlockEntity::new);
        } else if (machine == MeMekanismMachine.FLUIDIC_PLENISHER) {
            registered = registerMachine(machine, MeFluidicPlenisherBlockEntity::new);
        } else if (machine == MeMekanismMachine.RESISTIVE_HEATER) {
            registered = registerMachine(machine, MeResistiveHeaterBlockEntity::new);
        } else if (machine == MeMekanismMachine.SEISMIC_VIBRATOR) {
            registered = registerMachine(machine, MeSeismicVibratorBlockEntity::new);
        } else if (machine == MeMekanismMachine.TELEPORTER) {
            registered = registerMachine(machine, MeTeleporterBlockEntity::new);
        } else if (machine == MeMekanismMachine.OREDICTIONIFICATOR) {
            registered = registerMachine(machine, MeOredictionificatorBlockEntity::new);
        } else if (machine == MeMekanismMachine.MODIFICATION_STATION) {
            registered = registerMachine(machine, MeModificationStationBlockEntity::new);
        } else if (machine == MeMekanismMachine.DIGITAL_MINER) {
            registered = registerMachine(machine, MeDigitalMinerBlockEntity::new);
        } else if (machine == MeMekanismMachine.LOGISTICAL_SORTER) {
            registered = registerMachine(machine, MeLogisticalSorterBlockEntity::new);
        } else if (machine == MeMekanismMachine.DIMENSIONAL_STABILIZER) {
            registered = registerMachine(machine, MeDimensionalStabilizerBlockEntity::new);
        } else if (machine == MeMekanismMachine.FORMULAIC_ASSEMBLICATOR) {
            registered = registerMachine(machine, MeFormulaicAssemblicatorBlockEntity::new);
        } else if (machine == MeMekanismMachine.PRESSURIZED_REACTION_CHAMBER) {
            registered = registerMachine(machine, MePressurizedReactionChamberBlockEntity::new);
        } else if (machine == MeMekanismMachine.CHEMICAL_CRYSTALLIZER) {
            registered = registerMachine(machine, MeChemicalCrystallizerBlockEntity::new);
        } else if (machine == MeMekanismMachine.CHEMICAL_DISSOLUTION_CHAMBER) {
            registered = registerMachine(machine, MeChemicalDissolutionChamberBlockEntity::new);
        } else if (machine == MeMekanismMachine.CHEMICAL_INFUSER) {
            registered = registerMachine(machine, MeChemicalInfuserBlockEntity::new);
        } else if (machine == MeMekanismMachine.CHEMICAL_OXIDIZER) {
            registered = registerMachine(machine, MeChemicalOxidizerBlockEntity::new);
        } else if (machine == MeMekanismMachine.CHEMICAL_WASHER) {
            registered = registerMachine(machine, MeChemicalWasherBlockEntity::new);
        } else if (machine == MeMekanismMachine.ROTARY_CONDENSENTRATOR) {
            registered = registerMachine(machine, MeRotaryCondensentratorBlockEntity::new);
        } else if (machine == MeMekanismMachine.ELECTROLYTIC_SEPARATOR) {
            registered = registerMachine(machine, MeElectrolyticSeparatorBlockEntity::new);
        } else if (machine == MeMekanismMachine.SOLAR_NEUTRON_ACTIVATOR) {
            registered = registerMachine(machine, MeSolarNeutronActivatorBlockEntity::new);
        } else if (machine == MeMekanismMachine.ISOTOPIC_CENTRIFUGE) {
            registered = registerMachine(machine, MeIsotopicCentrifugeBlockEntity::new);
        } else if (machine == MeMekanismMachine.NUTRITIONAL_LIQUIFIER) {
            registered = registerMachine(machine, MeNutritionalLiquifierBlockEntity::new);
        } else if (machine == MeMekanismMachine.ANTIPROTONIC_NUCLEOSYNTHESIZER) {
            registered = registerMachine(machine, MeAntiprotonicNucleosynthesizerBlockEntity::new);
        } else if (machine == MeMekanismMachine.PIGMENT_EXTRACTOR) {
            registered = registerMachine(machine, MePigmentExtractorBlockEntity::new);
        } else if (machine == MeMekanismMachine.PIGMENT_MIXER) {
            registered = registerMachine(machine, MePigmentMixerBlockEntity::new);
        } else if (machine == MeMekanismMachine.PAINTING_MACHINE) {
            registered = registerMachine(machine, MePaintingMachineBlockEntity::new);
        } else {
            registered = registerMachine(machine, MeMekanismMachineBlockEntity::new);
        }
        return (TileEntityTypeRegistryObject) registered;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static TileEntityTypeRegistryObject<? extends TileEntityMekanism> registerFactoryMachine(MeMekanismMachine machine) {
        TileEntityTypeRegistryObject<?> registered = switch (machine.factoryType()) {
            case SMELTING, ENRICHING, CRUSHING -> registerMachine(machine, MeItemStackToItemStackFactoryBlockEntity::new);
            case COMPRESSING, INJECTING, PURIFYING, INFUSING -> registerMachine(machine, MeItemStackChemicalToItemStackFactoryBlockEntity::new);
            case COMBINING -> registerMachine(machine, MeCombiningFactoryBlockEntity::new);
            case SAWING -> registerMachine(machine, MeSawingFactoryBlockEntity::new);
        };
        return (TileEntityTypeRegistryObject) registered;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static TileEntityTypeRegistryObject<? extends TileEntityMekanism> registerMoreMachineBaseMachine(MeMekanismMachine machine) {
        TileEntityTypeRegistryObject<?> registered = switch (machine) {
            case RECYCLER -> registerMachine(machine, MeRecyclerBlockEntity::new);
            case CNC_STAMPER -> registerMachine(machine, MeStamperBlockEntity::new);
            case CNC_LATHE -> registerMachine(machine, MeLatheBlockEntity::new);
            case CNC_ROLLING_MILL -> registerMachine(machine, MeRollingMillBlockEntity::new);
            default -> registerMachine(machine, MeMekanismMachineBlockEntity::new);
        };
        return (TileEntityTypeRegistryObject) registered;
    }

    private static boolean isItemToItemFactory(MeMekanismMachine machine) {
        return machine.factoryType() == FactoryType.SMELTING
                || machine.factoryType() == FactoryType.ENRICHING
                || machine.factoryType() == FactoryType.CRUSHING;
    }

    @FunctionalInterface
    public interface MachineFactory<TILE extends TileEntityMekanism> {
        TILE create(MeMekanismMachine machine, BlockPos pos, BlockState state);
    }

    @FunctionalInterface
    public interface MachineFactoryRegistrar {
        <TILE extends TileEntityMekanism> TileEntityTypeRegistryObject<TILE> register(MeMekanismMachine machine, MachineFactory<TILE> factory);
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
            } else if (machine.isMoreMachineAdvancedFactory()) {
                MekanismMoreMachineCompat.registerGridNodeHost(event, holder);
            } else if (machine.isMoreMachineFactory()) {
                MekanismMoreMachineCompat.registerGridNodeHost(event, holder);
            } else if (machine.isFactory()) {
                switch (machine.factoryType()) {
                    case SMELTING, ENRICHING, CRUSHING ->
                            registerGridNodeHost(event, holder, MeItemStackToItemStackFactoryBlockEntity.class);
                    case COMPRESSING, INJECTING, PURIFYING, INFUSING ->
                            registerGridNodeHost(event, holder, MeItemStackChemicalToItemStackFactoryBlockEntity.class);
                    case COMBINING ->
                            registerGridNodeHost(event, holder, MeCombiningFactoryBlockEntity.class);
                    case SAWING ->
                            registerGridNodeHost(event, holder, MeSawingFactoryBlockEntity.class);
                }
            } else if (machine.isMoreMachineBaseMachine()) {
                registerMoreMachineBaseGridNodeHost(event, machine, holder);
            } else if (machine.slotLayout() == MeMekanismMachine.SlotLayout.SINGLE_ITEM && machine.hasRecipeLogic()) {
                registerGridNodeHost(event, holder, MeElectricMachineBlockEntity.class);
            } else if (machine.hasAdvancedChemicalInput()) {
                registerGridNodeHost(event, holder, MeAdvancedElectricMachineBlockEntity.class);
            } else if (machine == MeMekanismMachine.CHEMICAL_CRYSTALLIZER) {
                registerGridNodeHost(event, holder, MeChemicalCrystallizerBlockEntity.class);
            } else if (machine == MeMekanismMachine.CHEMICAL_DISSOLUTION_CHAMBER) {
                registerGridNodeHost(event, holder, MeChemicalDissolutionChamberBlockEntity.class);
            } else if (machine == MeMekanismMachine.CHEMICAL_INFUSER) {
                registerGridNodeHost(event, holder, MeChemicalInfuserBlockEntity.class);
            } else if (machine == MeMekanismMachine.CHEMICAL_OXIDIZER) {
                registerGridNodeHost(event, holder, MeChemicalOxidizerBlockEntity.class);
            } else if (machine == MeMekanismMachine.CHEMICAL_WASHER) {
                registerGridNodeHost(event, holder, MeChemicalWasherBlockEntity.class);
            } else if (machine == MeMekanismMachine.ELECTROLYTIC_SEPARATOR) {
                registerGridNodeHost(event, holder, MeElectrolyticSeparatorBlockEntity.class);
            } else if (machine == MeMekanismMachine.ROTARY_CONDENSENTRATOR) {
                registerGridNodeHost(event, holder, MeRotaryCondensentratorBlockEntity.class);
            } else if (machine == MeMekanismMachine.SOLAR_NEUTRON_ACTIVATOR) {
                registerGridNodeHost(event, holder, MeSolarNeutronActivatorBlockEntity.class);
            } else if (machine == MeMekanismMachine.ISOTOPIC_CENTRIFUGE) {
                registerGridNodeHost(event, holder, MeIsotopicCentrifugeBlockEntity.class);
            } else if (machine == MeMekanismMachine.PIGMENT_EXTRACTOR) {
                registerGridNodeHost(event, holder, MePigmentExtractorBlockEntity.class);
            } else if (machine == MeMekanismMachine.PIGMENT_MIXER) {
                registerGridNodeHost(event, holder, MePigmentMixerBlockEntity.class);
            } else if (machine == MeMekanismMachine.PAINTING_MACHINE) {
                registerGridNodeHost(event, holder, MePaintingMachineBlockEntity.class);
            } else if (machine == MeMekanismMachine.NUTRITIONAL_LIQUIFIER) {
                registerGridNodeHost(event, holder, MeNutritionalLiquifierBlockEntity.class);
            } else if (machine == MeMekanismMachine.ANTIPROTONIC_NUCLEOSYNTHESIZER) {
                registerGridNodeHost(event, holder, MeAntiprotonicNucleosynthesizerBlockEntity.class);
            } else if (machine == MeMekanismMachine.FORMULAIC_ASSEMBLICATOR) {
                registerGridNodeHost(event, holder, MeFormulaicAssemblicatorBlockEntity.class);
            } else if (machine == MeMekanismMachine.PRESSURIZED_REACTION_CHAMBER) {
                registerGridNodeHost(event, holder, MePressurizedReactionChamberBlockEntity.class);
            } else {
                registerGridNodeHost(event, holder, MeMekanismMachineBlockEntity.class);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerGridNodeHost(
            RegisterCapabilitiesEvent event,
            TileEntityTypeRegistryObject<? extends TileEntityMekanism> holder,
            Class<? extends appeng.api.networking.IInWorldGridNodeHost> tileClass) {
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                (net.minecraft.world.level.block.entity.BlockEntityType) holder.get(),
                (blockEntity, context) -> tileClass.isInstance(blockEntity) ? (appeng.api.networking.IInWorldGridNodeHost) tileClass.cast(blockEntity) : null
        );
    }

    private static void registerMoreMachineBaseGridNodeHost(
            RegisterCapabilitiesEvent event,
            MeMekanismMachine machine,
            TileEntityTypeRegistryObject<? extends TileEntityMekanism> holder) {
        switch (machine) {
            case RECYCLER -> registerGridNodeHost(event, holder, MeRecyclerBlockEntity.class);
            case CNC_STAMPER -> registerGridNodeHost(event, holder, MeStamperBlockEntity.class);
            case CNC_LATHE -> registerGridNodeHost(event, holder, MeLatheBlockEntity.class);
            case CNC_ROLLING_MILL -> registerGridNodeHost(event, holder, MeRollingMillBlockEntity.class);
            default -> registerGridNodeHost(event, holder, MeMekanismMachineBlockEntity.class);
        }
    }
}
