package com.beipuo.mekenergistics.registry;

import com.beipuo.mekenergistics.block.MeMekanismMachineBlock;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.item.MeMachineBlockItem;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.Nullable;

public final class ModBlocks {
    private static final MeBlockDeferredRegister BLOCKS = new MeBlockDeferredRegister();
    private static final Map<MeMekanismMachine, MeBlockRegistryObject<MeMekanismMachineBlock, MeMachineBlockItem>> MACHINES = new EnumMap<>(MeMekanismMachine.class);

    static {
        for (MeMekanismMachine machine : MeMekanismMachine.values()) {
            if (machine.isAvailable()) {
                MACHINES.put(machine, BLOCKS.registerMachine(machine));
            }
        }
    }

    public static final DeferredBlock<MeMekanismMachineBlock> ME_METALLURGIC_INFUSER = getMachineBlock(MeMekanismMachine.METALLURGIC_INFUSER);

    private ModBlocks() {
    }

    public static DeferredBlock<MeMekanismMachineBlock> getMachineBlock(MeMekanismMachine machine) {
        MeBlockRegistryObject<MeMekanismMachineBlock, MeMachineBlockItem> registryObject = MACHINES.get(machine);
        return registryObject == null ? null : registryObject.blockHolder();
    }

    public static DeferredItem<BlockItem> getMachineItem(MeMekanismMachine machine) {
        MeBlockRegistryObject<MeMekanismMachineBlock, MeMachineBlockItem> registryObject = MACHINES.get(machine);
        return registryObject == null ? null : (DeferredItem<BlockItem>) (DeferredItem<?>) registryObject.itemHolder();
    }

    public static Iterable<DeferredBlock<MeMekanismMachineBlock>> getMachineBlocks() {
        return MACHINES.values().stream().map(MeBlockRegistryObject::blockHolder)::iterator;
    }

    public static Iterable<DeferredItem<BlockItem>> getMachineItems() {
        return MACHINES.values().stream()
                .map(registryObject -> (DeferredItem<BlockItem>) (DeferredItem<?>) registryObject.itemHolder())::iterator;
    }

    public static Block[] getMachineBlockArray() {
        return MACHINES.values().stream().map(MeBlockRegistryObject::get).toArray(Block[]::new);
    }

    @Nullable
    public static MeMekanismMachine getMachine(Block block) {
        for (Map.Entry<MeMekanismMachine, MeBlockRegistryObject<MeMekanismMachineBlock, MeMachineBlockItem>> entry : MACHINES.entrySet()) {
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
