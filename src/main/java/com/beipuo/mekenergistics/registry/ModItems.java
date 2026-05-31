package com.beipuo.mekenergistics.registry;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.common.MeMekanismMachine;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MekEnergistics.MODID);
    private static final Map<MeMekanismMachine, DeferredItem<BlockItem>> MACHINES = new EnumMap<>(MeMekanismMachine.class);

    static {
        for (MeMekanismMachine machine : MeMekanismMachine.values()) {
            MACHINES.put(machine, ITEMS.register(
                    machine.registryName(),
                    () -> new BlockItem(ModBlocks.getMachineBlock(machine).get(), new Item.Properties())
            ));
        }
    }

    public static final DeferredItem<BlockItem> ME_METALLURGIC_INFUSER = getMachineItem(MeMekanismMachine.METALLURGIC_INFUSER);

    private ModItems() {
    }

    public static DeferredItem<BlockItem> getMachineItem(MeMekanismMachine machine) {
        return MACHINES.get(machine);
    }

    public static Iterable<DeferredItem<BlockItem>> getMachineItems() {
        return MACHINES.values();
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
