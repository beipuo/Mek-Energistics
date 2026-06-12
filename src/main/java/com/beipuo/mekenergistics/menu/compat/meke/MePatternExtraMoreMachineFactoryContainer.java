package com.beipuo.mekenergistics.menu.compat.meke;

import com.beipuo.mekenergistics.menu.MePatternContainerQuickMove;
import com.beipuo.mekenergistics.menu.MePatternQuickMoveContainer;
import com.jerry.mekextras.common.integration.mekmm.inventory.container.tile.ExtraMoreMachineFactoryContainer;
import com.jerry.mekextras.common.integration.mekmm.tile.factory.TileEntityExtraMoreMachineFactory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MePatternExtraMoreMachineFactoryContainer extends ExtraMoreMachineFactoryContainer implements MePatternQuickMoveContainer {
    public MePatternExtraMoreMachineFactoryContainer(int id, Inventory inv, TileEntityExtraMoreMachineFactory<?> tile) {
        super(id, inv, tile);
    }

    @NotNull
    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int slotID) {
        return MePatternContainerQuickMove.quickMoveStack(this.slots, this, this.tile, this::transferSuccess, super::quickMoveStack, player, slotID);
    }
}
