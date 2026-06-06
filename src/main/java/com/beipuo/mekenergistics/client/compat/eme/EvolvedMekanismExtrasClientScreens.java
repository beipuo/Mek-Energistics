package com.beipuo.mekenergistics.client.compat.eme;

import com.beipuo.mekenergistics.client.screen.machine.MeGuiEMExtraFactory;
import com.beipuo.mekenergistics.registry.ModMenuTypes;
import io.github.masyumero.emextras.common.tile.factory.TileEntityEMExtraFactory;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class EvolvedMekanismExtrasClientScreens {
    private EvolvedMekanismExtrasClientScreens() {
    }

    public static void register(RegisterMenuScreensEvent event) {
        event.register((MenuType) ModMenuTypes.ME_EM_EXTRA_FACTORY.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiEMExtraFactory((MekanismTileContainer<TileEntityEMExtraFactory<?>>) (MekanismTileContainer<?>) menu, inv, title));
    }
}
