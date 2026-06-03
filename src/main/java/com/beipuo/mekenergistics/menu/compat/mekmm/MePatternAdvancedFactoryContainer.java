package com.beipuo.mekenergistics.menu.compat.mekmm;

import com.beipuo.mekenergistics.menu.MePatternQuickMoveContainer;
import com.jerry.mekaf.common.inventory.container.tile.AdvancedFactoryContainer;
import com.jerry.mekaf.common.tile.factory.base.TileEntityAdvancedFactoryBase;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MePatternAdvancedFactoryContainer extends AdvancedFactoryContainer implements MePatternQuickMoveContainer {
    public MePatternAdvancedFactoryContainer(int id, Inventory inv, TileEntityAdvancedFactoryBase<?> tile) {
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
