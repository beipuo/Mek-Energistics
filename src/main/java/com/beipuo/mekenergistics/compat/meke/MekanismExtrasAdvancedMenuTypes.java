package com.beipuo.mekenergistics.compat.meke;

import com.beipuo.mekenergistics.menu.compat.meke.MePatternExtraAdvancedFactoryContainer;
import com.jerry.mekextras.common.integration.mekaf.tile.factory.base.TileEntityExtraAdvancedFactoryBase;
import mekanism.common.inventory.container.type.MekanismContainerType;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;

public final class MekanismExtrasAdvancedMenuTypes {
    private MekanismExtrasAdvancedMenuTypes() {
    }

    public static void register(ContainerTypeDeferredRegister register) {
        register.registerMenu("me_extra_advanced_factory", () -> MekanismContainerType.tile(TileEntityExtraAdvancedFactoryBase.class,
                (id, inv, tile) -> new MePatternExtraAdvancedFactoryContainer(id, inv, (TileEntityExtraAdvancedFactoryBase<?>) tile)));
    }
}
