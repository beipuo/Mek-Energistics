package com.beipuo.mekenergistics.compat.meke;

import com.beipuo.mekenergistics.menu.compat.meke.MePatternExtraMoreMachineFactoryContainer;
import com.jerry.mekextras.common.integration.mekmm.tile.factory.TileEntityExtraMoreMachineFactory;
import mekanism.common.inventory.container.type.MekanismContainerType;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;

public final class MekanismExtrasMoreMachineMenuTypes {
    private MekanismExtrasMoreMachineMenuTypes() {
    }

    public static void register(ContainerTypeDeferredRegister register) {
        register.registerMenu("me_extra_more_machine_factory", () -> MekanismContainerType.tile(TileEntityExtraMoreMachineFactory.class,
                (id, inv, tile) -> new MePatternExtraMoreMachineFactoryContainer(id, inv, (TileEntityExtraMoreMachineFactory<?>) tile)));
    }
}
