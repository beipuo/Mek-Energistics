package com.beipuo.mekenergistics.compat.extendedae;

import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.glodblock.github.extendedae.container.ContainerRenamer;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.entity.player.Player;

public final class ExtendedAeRenamerCompat {
    private ExtendedAeRenamerCompat() {
    }

    /**
     * Opens ExtendedAE's renamer for a machine. The renamer edits the machine's name in pattern
     * access terminals, so Mekanism's security (private / trusted machines) is enforced first.
     */
    public static boolean openRenamer(Player player, TileEntityMekanism tile) {
        if (tile.getLevel() == null
                || !IBlockSecurityUtils.INSTANCE.canAccessOrDisplayError(player, tile.getLevel(), tile.getBlockPos(), tile)) {
            return false;
        }
        return MenuOpener.open(ContainerRenamer.TYPE, player, MenuLocators.forBlockEntity(tile));
    }
}
