package com.beipuo.mekenergistics.registry;

import java.util.function.Supplier;
import mekanism.common.attachments.IAttachmentAware;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.ICapabilityAware;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.NotNull;

final class MeItemCapabilityRegistrar {
    private final Supplier<? extends Iterable<? extends DeferredHolder<Item, ? extends Item>>> items;

    MeItemCapabilityRegistrar(Supplier<? extends Iterable<? extends DeferredHolder<Item, ? extends Item>>> items) {
        this.items = items;
    }

    void register(@NotNull IEventBus eventBus) {
        eventBus.addListener(RegisterCapabilitiesEvent.class, this::registerCapabilities);
        eventBus.addListener(EventPriority.LOWEST, RegisterEvent.class, event -> {
            if (event.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) {
                attachDefaultContainers(eventBus);
            }
        });
        eventBus.addListener(EventPriority.LOWEST, ModifyDefaultComponentsEvent.class, this::modifyDefaultComponents);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        forItems(item -> {
            if (item instanceof ICapabilityAware capabilityAware) {
                capabilityAware.attachCapabilities(event);
            }
        });
    }

    private void attachDefaultContainers(IEventBus eventBus) {
        forItems(item -> {
            if (item instanceof IAttachmentAware attachmentAware) {
                attachmentAware.attachAttachments(eventBus);
            }
        });
    }

    private void modifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        forItems(item -> {
            Holder<Item> holder = item.builtInRegistryHolder();
            if (ContainerType.anySupports(holder)) {
                event.modify(item, builder -> addDefaultContainers(holder, builder));
            }
        });
    }

    private static void addDefaultContainers(Holder<Item> item, DataComponentPatch.Builder builder) {
        for (ContainerType<?, ?, ?> type : ContainerType.TYPES) {
            type.addDefault(item, builder);
        }
    }

    private void forItems(ItemConsumer consumer) {
        for (DeferredHolder<Item, ? extends Item> itemHolder : items.get()) {
            consumer.accept(itemHolder.get());
        }
    }

    @FunctionalInterface
    private interface ItemConsumer {
        void accept(Item item);
    }
}
