package com.beipuo.mekenergistics.client.compat.meke;

import com.beipuo.mekenergistics.client.screen.machine.MeGuiExtraMoreMachineFactory;
import com.beipuo.mekenergistics.registry.ModMenuTypes;
import com.jerry.mekextras.common.integration.mekmm.tile.factory.TileEntityExtraMoreMachineFactory;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class MekanismExtrasMoreMachineClientScreens {
    private MekanismExtrasMoreMachineClientScreens() {
    }

    public static void register(RegisterMenuScreensEvent event) {
        event.register((MenuType) ModMenuTypes.ME_EXTRA_MORE_MACHINE_FACTORY.get(),
                (MenuScreens.ScreenConstructor) (menu, inv, title) ->
                        new MeGuiExtraMoreMachineFactory((MekanismTileContainer<TileEntityExtraMoreMachineFactory<?>>) (MekanismTileContainer<?>) menu, inv, title));
    }
}
