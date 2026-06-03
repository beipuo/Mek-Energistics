package com.beipuo.mekenergistics.menu;

import appeng.api.crafting.PatternDetailsHelper;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.common.inventory.container.slot.InventoryContainerSlot;
import mekanism.common.inventory.slot.BasicInventorySlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface MePatternQuickMoveContainer {
    @NotNull
    default ItemStack tryQuickMovePattern(@NotNull Slot currentSlot, @NotNull Object tile, @NotNull ItemStack stack) {
        if (currentSlot instanceof InventoryContainerSlot || !PatternDetailsHelper.isEncodedPattern(stack)) {
            return stack;
        }
        List<BasicInventorySlot> patternSlots;
        if (tile instanceof MeAeMachine aeMachine) {
            patternSlots = aeMachine.getPatternSlots();
        } else if (tile instanceof MeFactoryAeMachine factoryAeMachine) {
            patternSlots = factoryAeMachine.getPatternSlots();
        } else {
            return stack;
        }
        ItemStack remaining = stack;
        for (BasicInventorySlot patternSlot : patternSlots) {
            remaining = patternSlot.insertItem(remaining, Action.EXECUTE, AutomationType.MANUAL);
            if (remaining.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return remaining;
    }
}
