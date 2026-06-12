package com.beipuo.mekenergistics.menu;

import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.container.slot.InventoryContainerSlot;
import mekanism.common.inventory.container.slot.VirtualInventoryContainerSlot;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MePatternMachineContainer<TILE extends TileEntityMekanism & MeAeMachine> extends MekanismTileContainer<TILE> implements MePatternQuickMoveContainer {
    public MePatternMachineContainer(ContainerTypeRegistryObject<?> type, int id, Inventory inv, @NotNull TILE tile) {
        super(type, id, inv, tile);
    }

    @NotNull
    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int slotID) {
        return MePatternContainerQuickMove.quickMoveStack(this.slots, this, this.tile, this::transferSuccess, super::quickMoveStack, player, slotID);
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
