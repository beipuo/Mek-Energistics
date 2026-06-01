package com.beipuo.mekenergistics.blockentity;

import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEItemKey;
import appeng.helpers.patternprovider.PatternContainer;
import com.beipuo.mekenergistics.common.MeMekanismMachine;
import java.util.List;
import mekanism.common.inventory.slot.BasicInventorySlot;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public interface MeAeMachine extends PatternContainer {
    MeMekanismMachineBlockEntity.AeOutputMode getAeOutputMode();

    void cycleAeOutputMode();

    void setOwner(ServerPlayer player);

    List<BasicInventorySlot> getPatternSlots();

    MeMekanismMachine getMachine();

    ItemStack getTerminalIconStack();

    @Override
    IGrid getGrid();

    @Override
    default InternalInventory getTerminalPatternInventory() {
        return new PatternSlotInternalInventory(this);
    }

    @Override
    default long getTerminalSortOrder() {
        return 0;
    }

    @Override
    default PatternContainerGroup getTerminalGroup() {
        ItemStack iconStack = getTerminalIconStack();
        AEItemKey icon = iconStack.isEmpty() ? null : AEItemKey.of(iconStack);
        Component name = Component.translatable(getMachine().translationKey());
        return new PatternContainerGroup(icon, name, List.of());
    }
}
