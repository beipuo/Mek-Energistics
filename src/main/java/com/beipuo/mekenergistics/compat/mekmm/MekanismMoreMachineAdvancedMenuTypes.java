package com.beipuo.mekenergistics.compat.mekmm;

import com.beipuo.mekenergistics.menu.compat.mekmm.MePatternAdvancedFactoryContainer;
import com.jerry.mekaf.common.tile.factory.base.TileEntityAdvancedFactoryBase;
import mekanism.common.inventory.container.type.MekanismContainerType;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;

public final class MekanismMoreMachineAdvancedMenuTypes {
    private MekanismMoreMachineAdvancedMenuTypes() {
    }

    public static void register(ContainerTypeDeferredRegister register) {
        register.registerMenu("me_advanced_factory", () -> MekanismContainerType.tile(TileEntityAdvancedFactoryBase.class,
                (id, inv, tile) -> new MePatternAdvancedFactoryContainer(id, inv, (TileEntityAdvancedFactoryBase<?>) tile)));
    }
}
