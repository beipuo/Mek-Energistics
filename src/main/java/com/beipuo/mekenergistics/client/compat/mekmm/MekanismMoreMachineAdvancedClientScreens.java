package com.beipuo.mekenergistics.client.compat.mekmm;

import com.beipuo.mekenergistics.client.screen.machine.MeGuiAdvancedFactory;
import com.beipuo.mekenergistics.registry.ModMenuTypes;
import com.jerry.mekaf.common.tile.factory.base.TileEntityAdvancedFactoryBase;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class MekanismMoreMachineAdvancedClientScreens {
    private MekanismMoreMachineAdvancedClientScreens() {
    }

    public static void register(RegisterMenuScreensEvent event) {
        event.register((MenuType) ModMenuTypes.ME_ADVANCED_FACTORY.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiAdvancedFactory((MekanismTileContainer<TileEntityAdvancedFactoryBase<?>>) (MekanismTileContainer<?>) menu, inv, title));
    }
}
