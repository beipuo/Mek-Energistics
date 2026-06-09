package com.beipuo.mekenergistics.blockentity.api;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.me.helpers.IGridConnectedBlockEntity;
import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import com.beipuo.mekenergistics.blockentity.support.MeFactoryAeSupport;
import com.beipuo.mekenergistics.blockentity.support.MePatternMirrorSupport;
import com.beipuo.mekenergistics.blockentity.support.MeOwnerHelper;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import java.util.List;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
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

    default MePatternMirrorSupport getPatternMirrorSupport() {
        return getAeSupport().getPatternMirrorSupport();
    }

    default AeOutputMode getAeOutputMode() {
        return getAeSupport().getAeOutputMode();
    }

    default String getCustomPatternTerminalName() {
        return getAeSupport().getPatternTerminalName();
    }

    default void setCustomPatternTerminalName(String name) {
        getAeSupport().setPatternTerminalName(name);
    }

    default void cycleAeOutputMode() {
        getAeSupport().cycleAeOutputMode();
    }

    default void cycleAeOutputMode(TransmissionType type) {
        getAeSupport().cycleAeOutputMode(type);
    }

    default void addAeOutputModeTracker(MekanismContainer container) {
        container.track(SyncableInt.create(() -> getAeOutputMode().ordinal(),
                mode -> getAeSupport().setAeOutputMode(AeOutputMode.byId(mode))));
        getPatternMirrorSupport().addContainerTrackers(container);
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
