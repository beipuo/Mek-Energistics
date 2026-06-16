package com.beipuo.mekenergistics.blockentity.compat.mekmm.machine;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeSmartCableConnection;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.blockentity.support.MeRecipeMachineAeSupport;
import com.jerry.mekmm.common.tile.machine.TileEntityPlantingStation;
import mekanism.api.IContentsListener;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MePlantingStationBlockEntity extends TileEntityPlantingStation implements ICraftingProvider, MeSmartCableConnection, IActionHost, MeAeMachine {
    private MeMekmmItemChemicalMachineSupport<MePlantingStationBlockEntity> meSupport;

    public MePlantingStationBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(pos, state);
        meSupport();
    }

    @NotNull
    @Override
    public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener, IContentsListener recipeCacheListener, IContentsListener recipeCacheUnpauseListener) {
        IChemicalTankHolder original = super.getInitialChemicalTanks(listener, recipeCacheListener, recipeCacheUnpauseListener);
        return this.meSupport().captureChemicalTank(original);
    }

    @Nullable
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener, IContentsListener recipeCacheListener, IContentsListener recipeCacheUnpauseListener) {
        IInventorySlotHolder original = super.getInitialInventory(listener, recipeCacheListener, recipeCacheUnpauseListener);
        return this.meSupport().withPatternSlots(original);
    }

    @Override
    protected boolean onUpdateServer() {
        return this.meSupport().drainOutputs(super.onUpdateServer());
    }

    @Override
    public mekanism.api.recipes.cache.CachedRecipe<com.jerry.mekmm.api.recipes.PlantingRecipe> createNewCachedRecipe(
            @NotNull com.jerry.mekmm.api.recipes.PlantingRecipe recipe, int cacheIndex) {
        return this.meSupport().wrapRecipeEnergy(getEnergyContainer(), super.createNewCachedRecipe(recipe, cacheIndex));
    }

    private MeMekmmItemChemicalMachineSupport<MePlantingStationBlockEntity> meSupport() {
        if (this.meSupport == null) {
            this.meSupport = new MeMekmmItemChemicalMachineSupport<>(this, MeMekanismMachine.PLANTING_STATION);
        }
        return this.meSupport;
    }

    @Override public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) { return this.meSupport().pushPattern(patternDetails, inputHolder); }
    @Override public boolean isBusy() { return this.meSupport().isBusy(); }
    @Override public MeMekanismMachine getMachine() { return this.meSupport().getMachine(); }
    @Override public MeRecipeMachineAeSupport<?> getRecipeAeSupport() { return this.meSupport().aeSupport(); }
    @Override public AeOutputMode getAeOutputMode() { return this.meSupport().getAeOutputMode(); }
    @Override public void cycleAeOutputMode() { this.meSupport().cycleAeOutputMode(); }
    @Override public void clearRemoved() { super.clearRemoved(); this.meSupport().clearRemoved(); }
    @Override public void setRemoved() { this.meSupport().setRemoved(); super.setRemoved(); }
    @Override public void onChunkUnloaded() { this.meSupport().onChunkUnloaded(); super.onChunkUnloaded(); }
    @Override public void addContainerTrackers(MekanismContainer container) { super.addContainerTrackers(container); this.meSupport().addContainerTrackers(container); }
    @Override public void saveAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) { super.saveAdditional(tag, registries); this.meSupport().saveAdditional(tag, registries); }
    @Override public void loadAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) { super.loadAdditional(tag, registries); this.meSupport().loadAdditional(tag, registries); }
}
