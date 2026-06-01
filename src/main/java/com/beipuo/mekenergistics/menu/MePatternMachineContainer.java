package com.beipuo.mekenergistics.menu;

import appeng.api.crafting.PatternDetailsHelper;
import com.beipuo.mekenergistics.blockentity.MeAeMachine;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.container.slot.InventoryContainerSlot;
import mekanism.common.inventory.container.slot.VirtualInventoryContainerSlot;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MePatternMachineContainer<TILE extends TileEntityMekanism & MeAeMachine> extends MekanismTileContainer<TILE> {
    public MePatternMachineContainer(ContainerTypeRegistryObject<?> type, int id, Inventory inv, @NotNull TILE tile) {
        super(type, id, inv, tile);
    }

    @NotNull
    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int slotID) {
        Slot currentSlot = this.slots.get(slotID);
        if (currentSlot == null || !currentSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack slotStack = currentSlot.getItem();
        if (!(currentSlot instanceof InventoryContainerSlot)
                && PatternDetailsHelper.isEncodedPattern(slotStack)) {
            ItemStack remaining = insertPattern(slotStack);
            if (remaining.getCount() != slotStack.getCount()) {
                return transferSuccess(currentSlot, player, slotStack, remaining);
            }
        }
        return super.quickMoveStack(player, slotID);
    }

    private ItemStack insertPattern(ItemStack stack) {
        ItemStack remaining = stack;
        for (BasicInventorySlot patternSlot : this.tile.getPatternSlots()) {
            remaining = patternSlot.insertItem(remaining, Action.EXECUTE, AutomationType.MANUAL);
            if (remaining.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return remaining;
    }

    public VirtualInventoryContainerSlot getPatternContainerSlot(int index) {
        if (index < 0 || index >= this.tile.getPatternSlots().size()) {
            return null;
        }
        BasicInventorySlot patternSlot = this.tile.getPatternSlots().get(index);
        for (InventoryContainerSlot containerSlot : getInventoryContainerSlots()) {
            if (containerSlot.getInventorySlot() == patternSlot && containerSlot instanceof VirtualInventoryContainerSlot virtualSlot) {
                return virtualSlot;
            }
        }
        return null;
    }
}
