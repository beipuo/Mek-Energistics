package com.beipuo.mekenergistics.compat.extendedae;

import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.glodblock.github.extendedae.container.ContainerRenamer;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.entity.player.Player;

public final class ExtendedAeRenamerCompat {
    private ExtendedAeRenamerCompat() {
    }

    public static boolean openRenamer(Player player, TileEntityMekanism tile) {
        return MenuOpener.open(ContainerRenamer.TYPE, player, MenuLocators.forBlockEntity(tile));
    }
}
