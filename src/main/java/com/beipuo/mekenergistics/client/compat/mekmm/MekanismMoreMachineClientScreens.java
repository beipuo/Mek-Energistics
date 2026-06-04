package com.beipuo.mekenergistics.client.compat.mekmm;

import com.beipuo.mekenergistics.client.screen.machine.MeGuiMoreMachineFactory;
import com.beipuo.mekenergistics.registry.ModMenuTypes;
import com.jerry.mekmm.common.tile.factory.TileEntityMoreMachineFactory;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class MekanismMoreMachineClientScreens {
    private MekanismMoreMachineClientScreens() {
    }

    public static void register(RegisterMenuScreensEvent event) {
        event.register((MenuType) ModMenuTypes.ME_MORE_MACHINE_FACTORY.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiMoreMachineFactory((MekanismTileContainer<TileEntityMoreMachineFactory<?>>) (MekanismTileContainer<?>) menu, inv, title));
    }
}
