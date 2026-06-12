package com.beipuo.mekenergistics.registry;

import com.beipuo.mekenergistics.MekEnergistics;
import java.util.Collection;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MeItemDeferredRegister {
    private final DeferredRegister.Items items = DeferredRegister.createItems(MekEnergistics.MODID);
    private final MeItemCapabilityRegistrar itemCapabilityRegistrar = new MeItemCapabilityRegistrar(this::getEntries);

    public <ITEM extends Item> DeferredItem<ITEM> register(String name, Supplier<? extends ITEM> itemSupplier) {
        return items.register(name, itemSupplier);
    }

    public Collection<DeferredHolder<Item, ? extends Item>> getEntries() {
        return items.getEntries();
    }

    public void register(IEventBus eventBus) {
        items.register(eventBus);
        itemCapabilityRegistrar.register(eventBus);
    }
}
