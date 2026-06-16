package com.beipuo.mekenergistics.blockentity.compat.eme.machine;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeSmartCableConnection;
import com.beipuo.mekenergistics.blockentity.support.MeFactoryPatternInput;
import com.beipuo.mekenergistics.blockentity.support.MeRecipeMachineAeSupport;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.mixin.TileEntityChemixerAccessor;
import fr.iglee42.evolvedmekanism.recipes.ChemixerRecipe;
import fr.iglee42.evolvedmekanism.tiles.machine.TileEntityChemixer;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class MeChemixerBlockEntity extends TileEntityChemixer implements ICraftingProvider, MeSmartCableConnection, IActionHost, MeAeMachine {
    private MeRecipeMachineAeSupport<MeChemixerBlockEntity> aeSupport;
    private AeOutputMode aeOutputMode = AeOutputMode.BOTH;

    public MeChemixerBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(pos, state);
        getRecipeAeSupport();
    }

    @Override
    public MeRecipeMachineAeSupport<MeChemixerBlockEntity> getRecipeAeSupport() {
        if (this.aeSupport == null) {
            this.aeSupport = new MeRecipeMachineAeSupport<>(this);
        }
        return this.aeSupport;
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener, IContentsListener recipeCacheListener, IContentsListener recipeCacheUnpauseListener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        var energy = new MeRecipeMachineAeSupport.AeBackedEnergyContainer<TileEntityChemixer>(this, getRecipeAeSupport(), recipeCacheUnpauseListener);
        ((TileEntityChemixerAccessor) this).mekenergistics$setEnergyContainer(energy);
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
        OutputInventorySlot output = ((TileEntityChemixerAccessor) this).mekenergistics$getOutputSlot();
        return getRecipeAeSupport().drainOutputs(this.aeOutputMode, sendUpdatePacket, output);
    }

    @NotNull
    @Override
    public CachedRecipe<ChemixerRecipe> createNewCachedRecipe(@NotNull ChemixerRecipe recipe, int cacheIndex) {
        return getRecipeAeSupport().wrapRecipeEnergy(getEnergyContainer(), super.createNewCachedRecipe(recipe, cacheIndex));
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!getMainNode().isActive() || !getAvailablePatterns().contains(patternDetails) || inputHolder == null || inputHolder.length != 3) {
            return false;
        }
        if (getRecipeAeSupport().isSmartPatternMultiplicationEnabled()) {
            return getRecipeAeSupport().enqueueSmartPattern(patternDetails, inputHolder);
        }
        ItemStack first = itemInput(inputHolder[0]);
        ItemStack second = itemInput(inputHolder[1]);
        ChemicalStack chemical = chemicalInput(inputHolder[2]);
        if (first.isEmpty() || second.isEmpty() || chemical.isEmpty()) {
            return false;
        }
        TileEntityChemixerAccessor accessor = (TileEntityChemixerAccessor) this;
        InputInventorySlot mainSlot = accessor.mekenergistics$getMainInputSlot();
        InputInventorySlot extraSlot = accessor.mekenergistics$getExtraInputSlot();
        IChemicalTank chemicalTank = accessor.mekenergistics$getInputChemicalTank();
        if (!mainSlot.insertItem(first.copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()
                || !extraSlot.insertItem(second.copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()
                || chemicalTank.insert(chemical.copy(), Action.SIMULATE, AutomationType.INTERNAL).getAmount() != 0) {
            return false;
        }
        mainSlot.insertItem(first, Action.EXECUTE, AutomationType.INTERNAL);
        extraSlot.insertItem(second, Action.EXECUTE, AutomationType.INTERNAL);
        chemicalTank.insert(chemical, Action.EXECUTE, AutomationType.INTERNAL);
        setChanged();
        return true;
    }

    private static ItemStack itemInput(KeyCounter counter) {
        MeFactoryPatternInput input = MeFactoryPatternInput.single(counter);
        return input != null && input.isItem() ? input.item() : ItemStack.EMPTY;
    }

    private static ChemicalStack chemicalInput(KeyCounter counter) {
        MeFactoryPatternInput input = MeFactoryPatternInput.single(counter);
        return input != null && input.isChemical() ? input.chemical() : ChemicalStack.EMPTY;
    }

    @Override public boolean isBusy() { return false; }
    @Override public MeMekanismMachine getMachine() { return MeMekanismMachine.CHEMIXER; }
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
