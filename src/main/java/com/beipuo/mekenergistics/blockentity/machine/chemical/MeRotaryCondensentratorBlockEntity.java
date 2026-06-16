package com.beipuo.mekenergistics.blockentity.machine.chemical;

import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;

import com.beipuo.mekenergistics.blockentity.MeMekanismMachineBlockEntity;

import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeSmartCableConnection;
import com.beipuo.mekenergistics.blockentity.support.MeFactoryPatternInput;
import com.beipuo.mekenergistics.blockentity.support.MeOwnerHelper;
import com.beipuo.mekenergistics.blockentity.support.MeRecipeMachineAeSupport;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.mixin.TileEntityRotaryCondensentratorAccessor;
import com.beipuo.mekenergistics.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.tile.machine.TileEntityRotaryCondensentrator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MeRotaryCondensentratorBlockEntity extends TileEntityRotaryCondensentrator implements ICraftingProvider, MeSmartCableConnection, IActionHost, MeAeMachine {
    private MeRecipeMachineAeSupport<MeRotaryCondensentratorBlockEntity> aeSupport;

    @Override
    public MeRecipeMachineAeSupport<MeRotaryCondensentratorBlockEntity> getRecipeAeSupport() {
        if (this.aeSupport == null) {
            this.aeSupport = new MeRecipeMachineAeSupport<>(this);
        }
        return this.aeSupport;
    }
    private AeOutputMode aeOutputMode = AeOutputMode.BOTH;

    public MeRotaryCondensentratorBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(pos, state);
        getRecipeAeSupport();
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener, IContentsListener recipeCacheListener, IContentsListener recipeCacheUnpauseListener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        var energy = new MeRecipeMachineAeSupport.AeBackedEnergyContainer<TileEntityRotaryCondensentrator>(this, getRecipeAeSupport(), recipeCacheUnpauseListener);
        ((TileEntityRotaryCondensentratorAccessor) this).mekenergistics$setEnergyContainer(energy);
        builder.addContainer(energy);
        return builder.build();
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener, IContentsListener recipeCacheListener, IContentsListener recipeCacheUnpauseListener) {
        return getRecipeAeSupport().withPatternSlots(super.getInitialInventory(listener, recipeCacheListener, recipeCacheUnpauseListener));
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        return getMode()
                ? getRecipeAeSupport().drainChemicalOutputs(this.aeOutputMode, sendUpdatePacket, this.gasTank)
                : getRecipeAeSupport().drainFluidOutputs(this.aeOutputMode, sendUpdatePacket, this.fluidTank);
    }

    @NotNull
    @Override
    public mekanism.api.recipes.cache.CachedRecipe<mekanism.api.recipes.RotaryRecipe> createNewCachedRecipe(
            @NotNull mekanism.api.recipes.RotaryRecipe recipe, int cacheIndex) {
        return getRecipeAeSupport().wrapRecipeEnergy(getEnergyContainer(), super.createNewCachedRecipe(recipe, cacheIndex));
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!getMainNode().isActive() || !getAvailablePatterns().contains(patternDetails) || inputHolder == null || inputHolder.length != 1) {
            return false;
        }
        if (getRecipeAeSupport().isSmartPatternMultiplicationEnabled()) {
            return getRecipeAeSupport().enqueueSmartPattern(patternDetails, inputHolder);
        }
        MeFactoryPatternInput input = MeFactoryPatternInput.single(inputHolder[0]);
        if (input == null) {
            return false;
        }
        if (getMode()) {
            return insertFluidInput(input);
        }
        return insertChemicalInput(input);
    }

    private boolean insertFluidInput(MeFactoryPatternInput input) {
        if (!input.isFluid()) {
            return false;
        }
        FluidStack fluidInput = input.fluid();
        if (fluidInput.isEmpty() || !this.fluidTank.insert(fluidInput.copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
            return false;
        }
        this.fluidTank.insert(fluidInput, Action.EXECUTE, AutomationType.INTERNAL);
        setChanged();
        return true;
    }

    private boolean insertChemicalInput(MeFactoryPatternInput input) {
        if (!input.isChemical()) {
            return false;
        }
        ChemicalStack chemicalInput = input.chemical();
        if (chemicalInput.isEmpty() || this.gasTank.insert(chemicalInput.copy(), Action.SIMULATE, AutomationType.INTERNAL).getAmount() != 0) {
            return false;
        }
        this.gasTank.insert(chemicalInput, Action.EXECUTE, AutomationType.INTERNAL);
        setChanged();
        return true;
    }

    @Override public boolean isBusy() { return false; }
    @Override public MeMekanismMachine getMachine() { return MeMekanismMachine.ROTARY_CONDENSENTRATOR; }
    public appeng.api.networking.IManagedGridNode getMainNode() { return getRecipeAeSupport().getMainNode(); }
    @Override public AeOutputMode getAeOutputMode() { return this.aeOutputMode; }
    @Override public void cycleAeOutputMode() { this.aeOutputMode = this.aeOutputMode.next(); setChanged(); }
    @Override public void clearRemoved() { super.clearRemoved(); getRecipeAeSupport().createOnFirstTick(); }
    @Override public void setRemoved() { getRecipeAeSupport().destroyNode(); super.setRemoved(); }
    @Override public void onChunkUnloaded() { getRecipeAeSupport().destroyNode(); super.onChunkUnloaded(); }
    @Override public void addContainerTrackers(MekanismContainer container) { super.addContainerTrackers(container); getRecipeAeSupport().addAeTrackers(container, this::getAeOutputMode, mode -> this.aeOutputMode = mode, false); }
    @Override public void saveAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) { super.saveAdditional(tag, registries); getRecipeAeSupport().saveAeState(tag, registries, this.aeOutputMode); }
    @Override public void loadAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) { super.loadAdditional(tag, registries); this.aeOutputMode = getRecipeAeSupport().loadAeState(tag, registries); }
}

