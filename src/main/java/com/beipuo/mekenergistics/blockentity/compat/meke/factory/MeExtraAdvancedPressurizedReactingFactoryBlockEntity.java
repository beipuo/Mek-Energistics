package com.beipuo.mekenergistics.blockentity.compat.meke.factory;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.blockentity.support.MeFactoryAeSupport;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.registry.ModBlocks;
import com.jerry.mekextras.common.integration.mekaf.tile.factory.TileEntityExtraPRCFactory;
import java.util.List;
import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
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

public class MeExtraAdvancedPressurizedReactingFactoryBlockEntity extends TileEntityExtraPRCFactory implements MeExtraFactoryBridge.Owner {
    private final MeMekanismMachine machine;
    private MeFactoryAeSupport aeSupport;

    public MeExtraAdvancedPressurizedReactingFactoryBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(ModBlocks.getMachineBlock(machine), pos, state);
        this.machine = machine;
    }

    @NotNull @Override protected IInventorySlotHolder getInitialInventory(IContentsListener listener) { return MeExtraFactoryBridge.withPatternSlots(super.getInitialInventory(listener), this); }
    @Override public List<IInventorySlot> meInputSlots() { return this.inputItemSlots; }
    @Override public List<IInventorySlot> meOutputSlots() { return this.outputItemSlots; }
    @Override public void unpauseRecipeMonitors() { for (var monitor : this.recipeCacheLookupMonitors) monitor.unpause(); }
    @Override public MeFactoryAeSupport getAeSupport() { if (this.aeSupport == null) this.aeSupport = new MeFactoryAeSupport(this); return this.aeSupport; }
    @Override public MeMekanismMachine getMachine() { return this.machine; }
    @Override public Level getOwnerLevel() { return getLevel(); }
    @Override public List<IPatternDetails> getAvailablePatterns() { return MeExtraFactoryBridge.getAvailablePatterns(getAeSupport()); }
    @Override public int getPatternPriority() { return getAeSupport().getPatternPriority(); }
    @Override public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) { return getMainNode().isActive() && getAvailablePatterns().contains(patternDetails) && MeExtraFactoryBridge.pushItemFluidChemical(this, inputHolder, this.inputFluidTank, this.inputChemicalTank); }
    @Override public boolean isBusy() { return false; }
    @Override public void addContainerTrackers(MekanismContainer container) { super.addContainerTrackers(container); addAeOutputModeTracker(container); }
    @Override protected boolean onUpdateServer() { return MeExtraFactoryBridge.updateServer(this, super.onUpdateServer(), this.outputChemicalTank); }
    @Override public void clearRemoved() { super.clearRemoved(); MeExtraFactoryBridge.createNodeOnFirstTick(this, getAeSupport(), getLevel(), getBlockPos()); }
    @Override public void setRemoved() { getAeSupport().destroy(); super.setRemoved(); }
    @Override public void onChunkUnloaded() { getAeSupport().destroy(); super.onChunkUnloaded(); }
    @Nullable @Override public appeng.api.networking.IGridNode getGridNode(Direction dir) { return MeExtraFactoryBridge.gridNode(this, dir); }
    @Override public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); MeExtraFactoryBridge.save(getAeSupport(), tag, registries); }
    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.loadAdditional(tag, registries); MeExtraFactoryBridge.load(getAeSupport(), tag, registries); }
}
