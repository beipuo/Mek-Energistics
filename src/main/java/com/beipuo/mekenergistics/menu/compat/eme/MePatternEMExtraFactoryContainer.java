package com.beipuo.mekenergistics.menu.compat.eme;

import com.beipuo.mekenergistics.menu.MePatternContainerQuickMove;
import com.beipuo.mekenergistics.menu.MePatternQuickMoveContainer;
import io.github.masyumero.emextras.common.inventory.container.tile.EMExtraFactoryContainer;
import io.github.masyumero.emextras.common.tile.factory.TileEntityEMExtraFactory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MePatternEMExtraFactoryContainer extends EMExtraFactoryContainer implements MePatternQuickMoveContainer {
    public MePatternEMExtraFactoryContainer(int id, Inventory inv, TileEntityEMExtraFactory<?> tile) {
        super(id, inv, tile);
    }

    @NotNull
    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int slotID) {
        return MePatternContainerQuickMove.quickMoveStack(this.slots, this, this.tile, this::transferSuccess, super::quickMoveStack, player, slotID);
    }
}
