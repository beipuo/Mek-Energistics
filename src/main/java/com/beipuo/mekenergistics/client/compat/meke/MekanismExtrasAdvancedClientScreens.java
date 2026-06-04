package com.beipuo.mekenergistics.client.compat.meke;

import com.beipuo.mekenergistics.client.screen.machine.MeGuiExtraAdvancedFactory;
import com.beipuo.mekenergistics.registry.ModMenuTypes;
import com.jerry.mekextras.common.integration.mekaf.tile.factory.base.TileEntityExtraAdvancedFactoryBase;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class MekanismExtrasAdvancedClientScreens {
    private MekanismExtrasAdvancedClientScreens() {
    }

    public static void register(RegisterMenuScreensEvent event) {
        event.register((MenuType) ModMenuTypes.ME_EXTRA_ADVANCED_FACTORY.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiExtraAdvancedFactory((MekanismTileContainer<TileEntityExtraAdvancedFactoryBase<?>>) (MekanismTileContainer<?>) menu, inv, title));
    }
}
