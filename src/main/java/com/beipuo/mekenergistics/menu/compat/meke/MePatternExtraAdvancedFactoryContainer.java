package com.beipuo.mekenergistics.menu.compat.meke;

import com.beipuo.mekenergistics.menu.MePatternContainerQuickMove;
import com.beipuo.mekenergistics.menu.MePatternQuickMoveContainer;
import com.jerry.mekextras.common.integration.mekaf.inventory.container.tile.ExtraAdvancedFactoryContainer;
import com.jerry.mekextras.common.integration.mekaf.tile.factory.base.TileEntityExtraAdvancedFactoryBase;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MePatternExtraAdvancedFactoryContainer extends ExtraAdvancedFactoryContainer implements MePatternQuickMoveContainer {
    public MePatternExtraAdvancedFactoryContainer(int id, Inventory inv, TileEntityExtraAdvancedFactoryBase<?> tile) {
        super(id, inv, tile);
    }

    @NotNull
    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int slotID) {
        return MePatternContainerQuickMove.quickMoveStack(this.slots, this, this.tile, this::transferSuccess, super::quickMoveStack, player, slotID);
    }
}
