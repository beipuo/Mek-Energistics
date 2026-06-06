package com.beipuo.mekenergistics.compat.eme;

import com.beipuo.mekenergistics.menu.compat.eme.MePatternEMExtraFactoryContainer;
import io.github.masyumero.emextras.common.tile.factory.TileEntityEMExtraFactory;
import mekanism.common.inventory.container.type.MekanismContainerType;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;

public final class EvolvedMekanismExtrasMenuTypes {
    private EvolvedMekanismExtrasMenuTypes() {
    }

    public static void register(ContainerTypeDeferredRegister register) {
        register.registerMenu("me_em_extra_factory", () -> MekanismContainerType.tile(TileEntityEMExtraFactory.class,
                (id, inv, tile) -> new MePatternEMExtraFactoryContainer(id, inv, (TileEntityEMExtraFactory<?>) tile)));
    }
}
