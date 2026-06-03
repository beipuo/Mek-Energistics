package com.beipuo.mekenergistics.blockentity.support;

import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.inventory.IInventorySlot;
import net.minecraft.world.item.ItemStack;

public final class MeFactoryInventoryInsert {
    private MeFactoryInventoryInsert() {
    }

    public static boolean canInsertAcrossSlots(List<IInventorySlot> slots, ItemStack input) {
        return insertAcrossSlots(slots, input, Action.SIMULATE);
    }

    public static boolean insertAcrossSlots(List<IInventorySlot> slots, ItemStack input) {
        return canInsertAcrossSlots(slots, input) && insertAcrossSlots(slots, input, Action.EXECUTE);
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
