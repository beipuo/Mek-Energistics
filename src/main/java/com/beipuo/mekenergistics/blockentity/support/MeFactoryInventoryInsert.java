package com.beipuo.mekenergistics.blockentity.support;

import java.util.ArrayList;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.inventory.IInventorySlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public final class MeFactoryInventoryInsert {
    private MeFactoryInventoryInsert() {
    }

    public static boolean canInsertAcrossSlots(List<IInventorySlot> slots, ItemStack input) {
        return insertAcrossSlots(slots, input, Action.SIMULATE);
    }

    public static boolean insertAcrossSlots(List<IInventorySlot> slots, ItemStack input) {
        return canInsertAcrossSlots(slots, input) && insertAcrossSlots(slots, input, Action.EXECUTE);
    }

    public static boolean insertAcrossSlotsKnownFits(List<IInventorySlot> slots, ItemStack input) {
        return insertAcrossSlots(slots, input, Action.EXECUTE);
    }

    public static List<ItemStack> snapshotSlots(List<IInventorySlot> slots) {
        List<ItemStack> snapshot = new ArrayList<>(slots.size());
        for (IInventorySlot slot : slots) {
            snapshot.add(slot == null ? ItemStack.EMPTY : slot.getStack().copy());
        }
        return snapshot;
    }

    public static void restoreSlots(List<IInventorySlot> slots, List<ItemStack> snapshot) {
        int size = Math.min(slots.size(), snapshot.size());
        for (int i = 0; i < size; i++) {
            IInventorySlot slot = slots.get(i);
            if (slot != null) {
                slot.setStack(snapshot.get(i).copy());
            }
        }
    }

    public static long acceptedCopiesAcrossSlots(List<IInventorySlot> slots, ItemStack oneCopyInput) {
        if (slots.isEmpty() || oneCopyInput.isEmpty()) {
            return 0;
        }
        long accepted = 0;
        int perCopy = oneCopyInput.getCount();
        for (IInventorySlot slot : slots) {
            if (slot == null) {
                continue;
            }
            ItemStack current = slot.getStack();
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, oneCopyInput)) {
                continue;
            }
            int room = slot.getLimit(oneCopyInput) - slot.getCount();
            if (room <= 0) {
                continue;
            }
            ItemStack probe = oneCopyInput.copyWithCount(room);
            ItemStack remainder = slot.insertItem(probe, Action.SIMULATE, AutomationType.INTERNAL);
            long inserted = (long) probe.getCount() - remainder.getCount();
            if (inserted > 0) {
                accepted += inserted;
            }
        }
        return accepted / perCopy;
    }

    public static long acceptedCopiesIntoSlot(IInventorySlot slot, ItemStack oneCopyInput) {
        if (slot == null || oneCopyInput.isEmpty()) {
            return 0;
        }
        int room = slot.getLimit(oneCopyInput) - slot.getCount();
        if (room <= 0) {
            return 0;
        }
        ItemStack probe = oneCopyInput.copyWithCount(room);
        ItemStack remainder = slot.insertItem(probe, Action.SIMULATE, AutomationType.INTERNAL);
        long inserted = (long) probe.getCount() - remainder.getCount();
        return inserted <= 0 ? 0 : inserted / oneCopyInput.getCount();
    }

    public static long acceptedCopiesIntoChemicalTank(IChemicalTank tank, ChemicalStack oneCopyInput) {
        if (tank == null || oneCopyInput.isEmpty()) {
            return 0;
        }
        long room = tank.getNeeded();
        if (room <= 0) {
            return 0;
        }
        ChemicalStack probe = oneCopyInput.copyWithAmount(room);
        ChemicalStack remainder = tank.insert(probe, Action.SIMULATE, AutomationType.INTERNAL);
        long inserted = probe.getAmount() - remainder.getAmount();
        return inserted <= 0 ? 0 : inserted / oneCopyInput.getAmount();
    }

    public static long acceptedCopiesIntoFluidTank(IExtendedFluidTank tank, FluidStack oneCopyInput) {
        if (tank == null || oneCopyInput.isEmpty()) {
            return 0;
        }
        int room = tank.getNeeded();
        if (room <= 0) {
            return 0;
        }
        FluidStack probe = oneCopyInput.copyWithAmount(room);
        FluidStack remainder = tank.insert(probe, Action.SIMULATE, AutomationType.INTERNAL);
        long inserted = (long) probe.getAmount() - remainder.getAmount();
        return inserted <= 0 ? 0 : inserted / oneCopyInput.getAmount();
    }

    private static boolean insertAcrossSlots(List<IInventorySlot> slots, ItemStack input, Action action) {
        if (slots.isEmpty() || input.isEmpty()) {
            return false;
        }
        ItemStack remaining = input.copy();
        for (IInventorySlot slot : slots) {
            if (remaining.isEmpty()) {
                break;
            }
            remaining = slot.insertItem(remaining, action, AutomationType.INTERNAL);
        }
        return remaining.isEmpty();
    }
}
