package com.beipuo.mekenergistics.menu;

import mekanism.common.inventory.container.tile.FormulaicAssemblicatorContainer;
import mekanism.common.tile.machine.TileEntityFormulaicAssemblicator;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MePatternFormulaicAssemblicatorContainer extends FormulaicAssemblicatorContainer implements MePatternQuickMoveContainer {
    public MePatternFormulaicAssemblicatorContainer(int id, Inventory inv, TileEntityFormulaicAssemblicator tile) {
        super(id, inv, tile);
    }

    @NotNull
    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int slotID) {
        return MePatternContainerQuickMove.quickMoveStack(this.slots, this, this.tile, this::transferSuccess, super::quickMoveStack, player, slotID);
    }
}
