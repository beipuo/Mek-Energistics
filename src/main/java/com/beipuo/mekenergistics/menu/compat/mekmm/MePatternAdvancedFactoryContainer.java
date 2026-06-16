package com.beipuo.mekenergistics.menu.compat.mekmm;

import com.beipuo.mekenergistics.menu.MePatternContainerQuickMove;
import com.beipuo.mekenergistics.menu.MePatternQuickMoveContainer;
import com.jerry.mekaf.common.inventory.container.tile.AdvancedFactoryContainer;
import com.jerry.mekaf.common.tile.factory.base.TileEntityAdvancedFactoryBase;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MePatternAdvancedFactoryContainer extends AdvancedFactoryContainer implements MePatternQuickMoveContainer {
    public MePatternAdvancedFactoryContainer(int id, Inventory inv, TileEntityAdvancedFactoryBase<?> tile) {
        super(id, inv, tile);
    }

    @NotNull
    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int slotID) {
        return MePatternContainerQuickMove.quickMoveStack(this.slots, this, this.tile, this::transferSuccess, super::quickMoveStack, player, slotID);
    }
}
