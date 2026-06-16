package com.beipuo.mekenergistics.blockentity.compat.eme.factory;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.compat.shared.MeExternalFactorySupport;
import com.beipuo.mekenergistics.blockentity.support.MeFactoryAeSupport;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.compat.eme.EvolvedMekanismRecipeViewerTypes;
import com.beipuo.mekenergistics.registry.ModBlocks;
import fr.iglee42.evolvedmekanism.recipes.AlloyerRecipe;
import fr.iglee42.evolvedmekanism.tiles.factory.TileEntityAlloyingFactory;
import java.util.List;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MeAlloyingFactoryBlockEntity extends TileEntityAlloyingFactory implements MeFactoryAeMachine, MeExternalFactorySupport.Owner {
    private final MeMekanismMachine machine;
    private final MeFactoryAeSupport aeSupport;

    public MeAlloyingFactoryBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(ModBlocks.getMachineBlock(machine), pos, state);
        this.machine = machine;
        this.aeSupport = new MeFactoryAeSupport(this);
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        builder.addContainer(this.energyContainer = new MeFactoryAeSupport.AeBackedFactoryEnergyContainer<>(this, () -> {
            listener.onContentsChanged();
            for (var cacheLookupMonitor : this.recipeCacheLookupMonitors) {
                cacheLookupMonitor.unpause();
            }
        }));
        return builder.build();
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        return MeExternalFactorySupport.withPatternSlots(super.getInitialInventory(listener), this);
    }

    @Override public MeFactoryAeSupport getAeSupport() { return this.aeSupport; }
    @Override public MeMekanismMachine getMachine() { return this.machine; }
    @Override public Level getOwnerLevel() { return getLevel(); }
    @Override public List<IInventorySlot> meInputSlots() { return this.inputSlots; }
    @Override public List<IInventorySlot> meOutputSlots() { return this.outputSlots; }
    @Override public void unpauseRecipeMonitors() { for (var monitor : this.recipeCacheLookupMonitors) monitor.unpause(); }
    @Override public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) { return getMainNode().isActive() && getAvailablePatterns().contains(patternDetails) && MeExternalFactorySupport.pushThreeItems(this, patternDetails, inputHolder, getExtraSlot(), getSecondExtraSlot()); }
    @Override public boolean isBusy() { return false; }
    @Nullable @Override public IRecipeViewerRecipeType<AlloyerRecipe> recipeViewerType() { return EvolvedMekanismRecipeViewerTypes.ALLOYING; }
    @Override public void addContainerTrackers(MekanismContainer container) { super.addContainerTrackers(container); addAeOutputModeTracker(container); }
    @Override public CachedRecipe<AlloyerRecipe> createNewCachedRecipe(@NotNull AlloyerRecipe recipe, int cacheIndex) { return MeFactoryAeSupport.withAeRecipeEnergy(this, this.energyContainer, super.createNewCachedRecipe(recipe, cacheIndex)); }
    @Override protected boolean onUpdateServer() { boolean sendUpdatePacket = MeExternalFactorySupport.processThreeItemsSmartPatterns(this, getExtraSlot(), getSecondExtraSlot()); sendUpdatePacket |= super.onUpdateServer(); return MeExternalFactorySupport.updateServer(this, sendUpdatePacket, () -> MeExternalFactorySupport.finishThreeItemsSmartPatterns(this, getExtraSlot(), getSecondExtraSlot())); }
    @Override public void clearRemoved() { super.clearRemoved(); MeExternalFactorySupport.createNodeOnFirstTick(this, getAeSupport(), getLevel(), getBlockPos()); }
    @Override public void setRemoved() { this.aeSupport.destroy(); super.setRemoved(); }
    @Override public void onChunkUnloaded() { this.aeSupport.destroy(); super.onChunkUnloaded(); }
    @Override public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); MeExternalFactorySupport.save(getAeSupport(), tag, registries); }
    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.loadAdditional(tag, registries); MeExternalFactorySupport.load(getAeSupport(), tag, registries); }
}
