package com.beipuo.mekenergistics.registry;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.MeMekanismMachineBlockEntity;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import net.neoforged.bus.api.IEventBus;

public final class ModMenuTypes {
    private static final ContainerTypeDeferredRegister MENU_TYPES = new ContainerTypeDeferredRegister(MekEnergistics.MODID);

    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeMekanismMachineBlockEntity>> ME_MEKANISM_MACHINE =
            MENU_TYPES.register("me_mekanism_machine", MeMekanismMachineBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeMekanismMachineBlockEntity>> ME_METALLURGIC_INFUSER = ME_MEKANISM_MACHINE;

    private ModMenuTypes() {
    }

    public static ContainerTypeRegistryObject<MekanismTileContainer<MeMekanismMachineBlockEntity>> getMachineContainer() {
        return ME_MEKANISM_MACHINE;
    }

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
