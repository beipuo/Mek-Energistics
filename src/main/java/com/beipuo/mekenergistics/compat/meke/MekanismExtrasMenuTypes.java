package com.beipuo.mekenergistics.compat.meke;

import com.beipuo.mekenergistics.menu.compat.meke.MePatternExtraFactoryContainer;
import com.jerry.mekextras.common.tile.factory.TileEntityExtraFactory;
import mekanism.common.inventory.container.type.MekanismContainerType;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;

public final class MekanismExtrasMenuTypes {
    private MekanismExtrasMenuTypes() {
    }

    public static void register(ContainerTypeDeferredRegister register) {
        register.registerMenu("me_extra_factory", () -> MekanismContainerType.tile(TileEntityExtraFactory.class,
                (id, inv, tile) -> new MePatternExtraFactoryContainer(id, inv, (TileEntityExtraFactory<?>) tile)));
    }
}
