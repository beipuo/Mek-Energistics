package com.beipuo.mekenergistics.compat.mekmm;

import com.beipuo.mekenergistics.menu.compat.mekmm.MePatternMoreMachineFactoryContainer;
import com.jerry.mekmm.common.tile.factory.TileEntityMoreMachineFactory;
import mekanism.common.inventory.container.type.MekanismContainerType;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;

public final class MekanismMoreMachineMenuTypes {
    private MekanismMoreMachineMenuTypes() {
    }

    public static void register(ContainerTypeDeferredRegister register) {
        register.registerMenu("me_more_machine_factory", () -> MekanismContainerType.tile(TileEntityMoreMachineFactory.class,
                (id, inv, tile) -> new MePatternMoreMachineFactoryContainer(id, inv, (TileEntityMoreMachineFactory<?>) tile)));
    }
}
