package com.beipuo.mekenergistics.registry;

import appeng.api.AECapabilities;
import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.MeAdvancedElectricMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeCombinerBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeElectricMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeMekanismMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeMetallurgicInfuserBlockEntity;
import com.beipuo.mekenergistics.blockentity.MePrecisionSawmillBlockEntity;
import com.beipuo.mekenergistics.common.MeMekanismMachine;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import mekanism.common.registration.impl.TileEntityTypeDeferredRegister;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.minecraft.world.level.block.state.BlockState;

public final class ModBlockEntities {
    private static final TileEntityTypeDeferredRegister BLOCK_ENTITIES = new TileEntityTypeDeferredRegister(MekEnergistics.MODID);
    private static final Map<MeMekanismMachine, TileEntityTypeRegistryObject<MeMekanismMachineBlockEntity>> MACHINES =
            new EnumMap<>(MeMekanismMachine.class);

    static {
        for (MeMekanismMachine machine : MeMekanismMachine.values()) {
            MACHINES.put(machine, registerMachine(machine));
        }
    }

    private static <TILE extends MeMekanismMachineBlockEntity> TileEntityTypeRegistryObject<TILE> registerMachine(
            MeMekanismMachine machine, MachineFactory<TILE> factory) {
        return BLOCK_ENTITIES.mekBuilder(
                ModBlocks.getMachineBlock(machine),
                (pos, state) -> factory.create(machine, pos, state)
        ).serverTicker((level, pos, state, tile) -> {
                MeMekanismMachineBlockEntity.tickServer(level, pos, state, tile);
            }).clientTicker((level, pos, state, tile) -> {
                MeMekanismMachineBlockEntity.tickClient(level, pos, state, tile);
            }).build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static TileEntityTypeRegistryObject<MeMekanismMachineBlockEntity> registerMachine(MeMekanismMachine machine) {
        TileEntityTypeRegistryObject<?> registered = switch (machine.factoryType()) {
            case COMPRESSING, INJECTING, PURIFYING -> registerMachine(machine, MeAdvancedElectricMachineBlockEntity::new);
            case INFUSING -> registerMachine(machine, MeMetallurgicInfuserBlockEntity::new);
            case COMBINING -> registerMachine(machine, MeCombinerBlockEntity::new);
            case SAWING -> registerMachine(machine, MePrecisionSawmillBlockEntity::new);
            default -> registerMachine(machine, MeElectricMachineBlockEntity::new);
        };
        return (TileEntityTypeRegistryObject) registered;
    }

    @FunctionalInterface
    private interface MachineFactory<TILE extends MeMekanismMachineBlockEntity> {
        TILE create(MeMekanismMachine machine, BlockPos pos, BlockState state);
    }

    public static final TileEntityTypeRegistryObject<MeMekanismMachineBlockEntity> ME_METALLURGIC_INFUSER =
            getMachineBlockEntity(MeMekanismMachine.METALLURGIC_INFUSER);

    private ModBlockEntities() {
    }

    public static TileEntityTypeRegistryObject<MeMekanismMachineBlockEntity> getMachineBlockEntity(MeMekanismMachine machine) {
        return MACHINES.get(machine);
    }

    public static boolean isMachineBlockEntity(BlockEntityType<?> type) {
        for (TileEntityTypeRegistryObject<MeMekanismMachineBlockEntity> holder : MACHINES.values()) {
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
        for (TileEntityTypeRegistryObject<MeMekanismMachineBlockEntity> holder : MACHINES.values()) {
            event.registerBlockEntity(
                    AECapabilities.IN_WORLD_GRID_NODE_HOST,
                    holder.get(),
                    (blockEntity, context) -> blockEntity
            );
        }
    }
}
