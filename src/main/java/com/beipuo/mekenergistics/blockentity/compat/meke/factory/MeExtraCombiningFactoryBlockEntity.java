package com.beipuo.mekenergistics.blockentity.compat.meke.factory;

import com.beipuo.mekenergistics.blockentity.compat.shared.MeExternalFactorySupport;
import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.blockentity.support.MeFactoryAeSupport;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.registry.ModBlocks;
import com.jerry.mekextras.common.tile.factory.TileEntityExtraCombiningFactory;
import java.util.List;
import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MeExtraCombiningFactoryBlockEntity extends TileEntityExtraCombiningFactory implements MeExtraFactoryAeMachine, MeExternalFactorySupport.Owner {
    private final MeMekanismMachine machine;
    private MeFactoryAeSupport aeSupport;

    public MeExtraCombiningFactoryBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(ModBlocks.getMachineBlock(machine), pos, state);
        this.machine = machine;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @NotNull @Override protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) { return MeExternalFactorySupport.energyContainers((com.jerry.mekextras.common.tile.factory.TileEntityExtraFactory<?>) this, listener, this::unpauseRecipeMonitors, container -> this.energyContainer = (mekanism.common.capabilities.energy.MachineEnergyContainer) container); }
    @NotNull @Override protected IInventorySlotHolder getInitialInventory(IContentsListener listener) { return MeExternalFactorySupport.withPatternSlots(super.getInitialInventory(listener), this); }
    @Override public List<IInventorySlot> meInputSlots() { return this.inputSlots; }
    @Override public List<IInventorySlot> meOutputSlots() { return this.outputSlots; }
    @Override public void unpauseRecipeMonitors() { for (var monitor : this.recipeCacheLookupMonitors) monitor.unpause(); }
    @Override public MeFactoryAeSupport getAeSupport() { if (this.aeSupport == null) this.aeSupport = new MeFactoryAeSupport(this); return this.aeSupport; }
    @Override public MeMekanismMachine getMachine() { return this.machine; }
    @Override public Level getOwnerLevel() { return getLevel(); }
    @Override public List<IPatternDetails> getAvailablePatterns() { return MeExternalFactorySupport.getAvailablePatterns(getAeSupport()); }
    @Override public int getPatternPriority() { return getAeSupport().getPatternPriority(); }
    @Override public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) { return getMainNode().isActive() && getAvailablePatterns().contains(patternDetails) && MeExternalFactorySupport.pushTwoItems(this, inputHolder, getExtraSlot()); }
    @Override public boolean isBusy() { return false; }
    @Override public void addContainerTrackers(MekanismContainer container) { super.addContainerTrackers(container); addAeOutputModeTracker(container); }
    @Override protected boolean onUpdateServer() { boolean sendUpdatePacket = MeExternalFactorySupport.drainOutputs(this); sendUpdatePacket |= super.onUpdateServer(); return MeExternalFactorySupport.updateServer(this, sendUpdatePacket); }
    @Override public void clearRemoved() { super.clearRemoved(); MeExternalFactorySupport.createNodeOnFirstTick(this, getAeSupport(), getLevel(), getBlockPos()); }
    @Override public void setRemoved() { getAeSupport().destroy(); super.setRemoved(); }
    @Override public void onChunkUnloaded() { getAeSupport().destroy(); super.onChunkUnloaded(); }
    @Nullable @Override public appeng.api.networking.IGridNode getGridNode(Direction dir) { return MeExtraFactoryAeMachine.super.getGridNode(dir); }
    @Override public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); MeExternalFactorySupport.save(getAeSupport(), tag, registries); }
    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.loadAdditional(tag, registries); MeExternalFactorySupport.load(getAeSupport(), tag, registries); }
}
