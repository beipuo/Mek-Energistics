package com.beipuo.mekenergistics.registry;

import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.item.MeMachineBlockItem;
import com.beipuo.mekenergistics.item.MeTierInstallerItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;

public final class ModItems {
    private static final MeItemDeferredRegister ITEMS = new MeItemDeferredRegister();
    public static final DeferredItem<MeTierInstallerItem> ME_FACTORY_INSTALLER =
            ITEMS.register("me_factory_installer", () -> new MeTierInstallerItem(new Item.Properties()));

    public static final DeferredItem<MeMachineBlockItem> ME_METALLURGIC_INFUSER = getMachineItem(MeMekanismMachine.METALLURGIC_INFUSER);

    private ModItems() {
    }

    public static DeferredItem<MeMachineBlockItem> getMachineItem(MeMekanismMachine machine) {
        return ModBlocks.getMachineItem(machine);
    }

    public static Iterable<DeferredItem<MeMachineBlockItem>> getMachineItems() {
        return ModBlocks.getMachineItems();
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
