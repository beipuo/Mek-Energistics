package com.beipuo.mekenergistics.blockentity.slot;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.common.inventory.slot.BasicInventorySlot;
import net.minecraft.world.item.ItemStack;

public final class PatternSlotInternalInventory implements InternalInventory {
    private final MeAeMachine machine;

    public PatternSlotInternalInventory(MeAeMachine machine) {
        this.machine = machine;
    }

    @Override
    public int size() {
        return getSlots().size();
    }

    @Override
    public int getSlotLimit(int slot) {
        return getSlot(slot).getLimit(getStackInSlot(slot));
    }

    @Override
    public ItemStack getStackInSlot(int slotIndex) {
        return getSlot(slotIndex).getStack();
    }

    @Override
    public void setItemDirect(int slotIndex, ItemStack stack) {
        getSlot(slotIndex).setStack(stack);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return !stack.isEmpty() && PatternDetailsHelper.isEncodedPattern(stack);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!isItemValid(slot, stack)) {
            return stack;
        }
        return getSlot(slot).insertItem(stack, simulate ? Action.SIMULATE : Action.EXECUTE, AutomationType.MANUAL);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return getSlot(slot).extractItem(amount, simulate ? Action.SIMULATE : Action.EXECUTE, AutomationType.MANUAL);
    }

    private BasicInventorySlot getSlot(int slot) {
        List<BasicInventorySlot> slots = getSlots();
        if (slot < 0 || slot >= slots.size()) {
            throw new IndexOutOfBoundsException("Pattern slot out of range: " + slot);
        }
        return slots.get(slot);
    }

    private List<BasicInventorySlot> getSlots() {
        return this.machine.getPatternSlots();
    }
}
