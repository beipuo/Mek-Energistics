package com.beipuo.mekenergistics.client.compat.meke;

import com.beipuo.mekenergistics.client.screen.machine.MeGuiExtraFactory;
import com.beipuo.mekenergistics.registry.ModMenuTypes;
import com.jerry.mekextras.common.tile.factory.TileEntityExtraFactory;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class MekanismExtrasClientScreens {
    private MekanismExtrasClientScreens() {
    }

    public static void register(RegisterMenuScreensEvent event) {
        event.register((MenuType) ModMenuTypes.ME_EXTRA_FACTORY.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiExtraFactory((MekanismTileContainer<TileEntityExtraFactory<?>>) (MekanismTileContainer<?>) menu, inv, title));
    }
}
