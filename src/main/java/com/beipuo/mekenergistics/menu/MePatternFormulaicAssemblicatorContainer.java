package com.beipuo.mekenergistics.menu;

import mekanism.common.inventory.container.tile.FormulaicAssemblicatorContainer;
import mekanism.common.tile.machine.TileEntityFormulaicAssemblicator;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MePatternFormulaicAssemblicatorContainer extends FormulaicAssemblicatorContainer implements MePatternQuickMoveContainer {
    public MePatternFormulaicAssemblicatorContainer(int id, Inventory inv, TileEntityFormulaicAssemblicator tile) {
        super(id, inv, tile);
    }

    @NotNull
    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int slotID) {
        Slot currentSlot = this.slots.get(slotID);
        if (currentSlot == null || !currentSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack slotStack = currentSlot.getItem();
        ItemStack remaining = tryQuickMovePattern(currentSlot, tile, slotStack);
        if (remaining.getCount() != slotStack.getCount()) {
            return transferSuccess(currentSlot, player, slotStack, remaining);
        }
        return super.quickMoveStack(player, slotID);
    }
}
