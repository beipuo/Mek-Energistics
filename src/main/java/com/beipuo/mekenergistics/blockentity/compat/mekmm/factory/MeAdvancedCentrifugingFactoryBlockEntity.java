package com.beipuo.mekenergistics.blockentity.compat.mekmm.factory;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.blockentity.support.MeFactoryAeSupport;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.registry.ModBlocks;
import com.jerry.mekaf.common.tile.factory.TileEntityCentrifugingFactory;
import java.util.Collections;
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

public class MeAdvancedCentrifugingFactoryBlockEntity extends TileEntityCentrifugingFactory implements MeAdvancedFactorySupport.Owner {
    private final MeMekanismMachine machine;
    private MeFactoryAeSupport aeSupport;

    public MeAdvancedCentrifugingFactoryBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(ModBlocks.getMachineBlock(machine), pos, state);
        this.machine = machine;
    }

    @NotNull @Override protected IInventorySlotHolder getInitialInventory(IContentsListener listener) { return MeAdvancedFactorySupport.withPatternSlots(super.getInitialInventory(listener), this); }
    @Override public List<IInventorySlot> meInputSlots() { return Collections.emptyList(); }
    @Override public List<IInventorySlot> meOutputSlots() { return MeAdvancedFactorySupport.noItemOutput(); }
    @Override public void unpauseRecipeMonitors() { for (var monitor : this.recipeCacheLookupMonitors) monitor.unpause(); }
    @Override public MeFactoryAeSupport getAeSupport() { if (this.aeSupport == null) this.aeSupport = new MeFactoryAeSupport(this); return this.aeSupport; }
    @Override public MeMekanismMachine getMachine() { return this.machine; }
    @Override public Level getOwnerLevel() { return getLevel(); }
    @Override public List<IPatternDetails> getAvailablePatterns() { return getAeSupport().getAvailablePatterns(); }
    @Override public int getPatternPriority() { return getAeSupport().getPatternPriority(); }
    @Override public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) { return getMainNode().isActive() && getAvailablePatterns().contains(patternDetails) && MeAdvancedFactorySupport.pushChemical(this, patternDetails, inputHolder, this.inputChemicalTanks); }
    @Override public boolean isBusy() { return false; }
    @Override public void addContainerTrackers(MekanismContainer container) { super.addContainerTrackers(container); addAeOutputModeTracker(container); }
    @Override public mekanism.api.recipes.cache.CachedRecipe<mekanism.api.recipes.ChemicalToChemicalRecipe> createNewCachedRecipe(@NotNull mekanism.api.recipes.ChemicalToChemicalRecipe recipe, int cacheIndex) { return MeAdvancedFactorySupport.wrapRecipeEnergy(this, this.energyContainer, super.createNewCachedRecipe(recipe, cacheIndex)); }
    @Override protected boolean onUpdateServer() { boolean sendUpdatePacket = MeAdvancedFactorySupport.drainOutputs(this); sendUpdatePacket |= super.onUpdateServer(); return MeAdvancedFactorySupport.updateServer(this, sendUpdatePacket, this.outputChemicalTanks); }
    @Override public void clearRemoved() { super.clearRemoved(); MeAdvancedFactorySupport.createNodeOnFirstTick(this, getAeSupport(), getLevel(), getBlockPos()); }
    @Override public void setRemoved() { getAeSupport().destroy(); super.setRemoved(); }
    @Override public void onChunkUnloaded() { getAeSupport().destroy(); super.onChunkUnloaded(); }
    @Nullable @Override public appeng.api.networking.IGridNode getGridNode(Direction dir) { return getMainNode().getNode(); }
    @Override public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); MeAdvancedFactorySupport.save(getAeSupport(), tag, registries); }
    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.loadAdditional(tag, registries); MeAdvancedFactorySupport.load(getAeSupport(), tag, registries); }
}
