package com.beipuo.mekenergistics.menu.compat.mekmm;

import com.beipuo.mekenergistics.menu.MePatternContainerQuickMove;
import com.beipuo.mekenergistics.menu.MePatternQuickMoveContainer;
import com.jerry.mekmm.common.inventory.container.tile.MoreMachineFactoryContainer;
import com.jerry.mekmm.common.tile.factory.TileEntityMoreMachineFactory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MePatternMoreMachineFactoryContainer extends MoreMachineFactoryContainer implements MePatternQuickMoveContainer {
    public MePatternMoreMachineFactoryContainer(int id, Inventory inv, TileEntityMoreMachineFactory<?> tile) {
        super(id, inv, tile);
    }

    @NotNull
    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int slotID) {
        return MePatternContainerQuickMove.quickMoveStack(this.slots, this, this.tile, this::transferSuccess, super::quickMoveStack, player, slotID);
    }
}
