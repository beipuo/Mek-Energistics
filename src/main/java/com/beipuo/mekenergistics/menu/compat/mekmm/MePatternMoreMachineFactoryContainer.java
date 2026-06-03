package com.beipuo.mekenergistics.menu.compat.mekmm;

import com.beipuo.mekenergistics.menu.MePatternQuickMoveContainer;
import com.jerry.mekmm.common.inventory.container.tile.MoreMachineFactoryContainer;
import com.jerry.mekmm.common.tile.factory.TileEntityMoreMachineFactory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MePatternMoreMachineFactoryContainer extends MoreMachineFactoryContainer implements MePatternQuickMoveContainer {
    public MePatternMoreMachineFactoryContainer(int id, Inventory inv, TileEntityMoreMachineFactory<?> tile) {
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
