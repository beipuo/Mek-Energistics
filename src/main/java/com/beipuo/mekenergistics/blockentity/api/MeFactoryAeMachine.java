package com.beipuo.mekenergistics.blockentity.api;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.me.helpers.IGridConnectedBlockEntity;
import com.beipuo.mekenergistics.blockentity.support.MeFactoryAeSupport;
import com.beipuo.mekenergistics.blockentity.support.MeOwnerHelper;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import java.util.List;
import mekanism.common.inventory.slot.BasicInventorySlot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public interface MeFactoryAeMachine extends ICraftingProvider, IGridConnectedBlockEntity, IActionHost, MeSmartCableConnection, appeng.helpers.patternprovider.PatternContainer {
    MeFactoryAeSupport getAeSupport();

    MeMekanismMachine getMachine();

    Level getOwnerLevel();

    @Override
    default IManagedGridNode getMainNode() {
        return getAeSupport().getMainNode();
    }

    @Override
    default void saveChanges() {
        if (this instanceof net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
            blockEntity.setChanged();
        }
    }

    default List<BasicInventorySlot> getPatternSlots() {
        return getAeSupport().getPatternSlots();
    }

    default void setOwner(ServerPlayer player) {
        if (this instanceof mekanism.common.tile.base.TileEntityMekanism tile) {
            MeOwnerHelper.setOwner(tile, getMainNode(), player);
        } else {
            getMainNode().setOwningPlayer(player);
        }
    }

    @Override
    default IGrid getGrid() {
        return getAeSupport().getGrid();
    }

    @Override
    default IGridNode getActionableNode() {
        return getMainNode().getNode();
    }

    @Override
    default InternalInventory getTerminalPatternInventory() {
        return getAeSupport().getTerminalPatternInventory();
    }

    @Override
    default appeng.api.implementations.blockentities.PatternContainerGroup getTerminalGroup() {
        return getAeSupport().getTerminalGroup();
    }
}
