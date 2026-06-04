package com.beipuo.mekenergistics.registry;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.block.MeMekanismMachineBlock;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MekEnergistics.MODID);
    private static final Map<MeMekanismMachine, DeferredBlock<MeMekanismMachineBlock>> MACHINES = new EnumMap<>(MeMekanismMachine.class);

    static {
        for (MeMekanismMachine machine : MeMekanismMachine.values()) {
            if (machine.isAvailable()) {
                MACHINES.put(machine, BLOCKS.register(machine.registryName(), () -> new MeMekanismMachineBlock(machine)));
            }
        }
    }

    public static final DeferredBlock<MeMekanismMachineBlock> ME_METALLURGIC_INFUSER = getMachineBlock(MeMekanismMachine.METALLURGIC_INFUSER);

    private ModBlocks() {
    }

    public static DeferredBlock<MeMekanismMachineBlock> getMachineBlock(MeMekanismMachine machine) {
        return MACHINES.get(machine);
    }

    public static Iterable<DeferredBlock<MeMekanismMachineBlock>> getMachineBlocks() {
        return MACHINES.values();
    }

    public static Block[] getMachineBlockArray() {
        return MACHINES.values().stream().map(DeferredBlock::get).toArray(Block[]::new);
    }

    @Nullable
    public static MeMekanismMachine getMachine(Block block) {
        for (Map.Entry<MeMekanismMachine, DeferredBlock<MeMekanismMachineBlock>> entry : MACHINES.entrySet()) {
            if (entry.getValue().get() == block) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
