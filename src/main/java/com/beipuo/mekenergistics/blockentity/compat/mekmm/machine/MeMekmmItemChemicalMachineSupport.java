package com.beipuo.mekenergistics.blockentity.compat.mekmm.machine;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeSmartCableConnection;
import com.beipuo.mekenergistics.blockentity.support.MeFactoryPatternInput;
import com.beipuo.mekenergistics.blockentity.support.MeRecipeMachineAeSupport;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

final class MeMekmmItemChemicalMachineSupport<TILE extends TileEntityMekanism & MeAeMachine & ICraftingProvider & IActionHost> {
    private final TILE owner;
    private final MeMekanismMachine machine;
    private MeRecipeMachineAeSupport<TILE> aeSupport;
    private AeOutputMode aeOutputMode = AeOutputMode.BOTH;
    private InputInventorySlot inputSlot;
    private OutputInventorySlot outputSlot;
    private OutputInventorySlot secondaryOutputSlot;
    private IChemicalTank chemicalTank;

    MeMekmmItemChemicalMachineSupport(TILE owner, MeMekanismMachine machine) {
        this.owner = owner;
        this.machine = machine;
    }

    IInventorySlotHolder withPatternSlots(IInventorySlotHolder original) {
        captureSlots(original);
        MeRecipeMachineAeSupport<TILE> support = aeSupport();
        return side -> {
            List<IInventorySlot> slots = new ArrayList<>(original.getInventorySlots(side));
            slots.addAll(support.getPatternSlots());
            return slots;
        };
    }

    MeRecipeMachineAeSupport<TILE> aeSupport() {
        if (this.aeSupport == null) {
            this.aeSupport = new MeRecipeMachineAeSupport<>(this.owner);
        }
        return this.aeSupport;
    }

    IChemicalTankHolder captureChemicalTank(IChemicalTankHolder original) {
        List<IChemicalTank> tanks = original.getTanks(null);
        if (!tanks.isEmpty()) {
            this.chemicalTank = tanks.getFirst();
        }
        return original;
    }

    private void captureSlots(IInventorySlotHolder original) {
        for (IInventorySlot slot : original.getInventorySlots(null)) {
            if (slot instanceof InputInventorySlot input && this.inputSlot == null) {
                this.inputSlot = input;
            } else if (slot instanceof OutputInventorySlot output) {
                if (this.outputSlot == null) {
                    this.outputSlot = output;
                } else if (this.secondaryOutputSlot == null) {
                    this.secondaryOutputSlot = output;
                }
            }
        }
    }

    boolean drainOutputs(boolean sendUpdatePacket) {
        MeRecipeMachineAeSupport<TILE> support = aeSupport();
        boolean changed = support.insertOutputSlotIntoNetwork(this.outputSlot, this.aeOutputMode);
        changed |= support.insertOutputSlotIntoNetwork(this.secondaryOutputSlot, this.aeOutputMode);
        return changed || sendUpdatePacket;
    }

    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!getMainNode().isActive() || !getAvailablePatterns().contains(patternDetails) || inputHolder == null || inputHolder.length != 2) {
            return false;
        }
        if (aeSupport().isSmartPatternMultiplicationEnabled()) {
            return aeSupport().enqueueSmartPattern(patternDetails, inputHolder);
        }
        MeFactoryPatternInput input = MeFactoryPatternInput.separate(inputHolder);
        if (input == null || input.item().isEmpty() || input.chemical().isEmpty() || !input.fluid().isEmpty()
                || this.inputSlot == null || this.chemicalTank == null
                || !this.inputSlot.insertItem(input.item().copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()
                || !this.chemicalTank.insert(input.chemical().copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
            return false;
        }
        this.inputSlot.insertItem(input.item(), Action.EXECUTE, AutomationType.INTERNAL);
        this.chemicalTank.insert(input.chemical(), Action.EXECUTE, AutomationType.INTERNAL);
        this.owner.setChanged();
        return true;
    }

    public boolean isBusy() { return false; }
    public List<IPatternDetails> getAvailablePatterns() { return aeSupport().getAvailablePatterns(); }
    public int getPatternPriority() { return aeSupport().getPatternPriority(); }
    public String getCustomPatternTerminalName() { return aeSupport().getPatternTerminalName(); }
    public void setCustomPatternTerminalName(String name) { aeSupport().setPatternTerminalName(name); }
    public List<BasicInventorySlot> getPatternSlots() { return aeSupport().getPatternSlots(); }
    public MeMekanismMachine getMachine() { return this.machine; }
    public ItemStack getTerminalIconStack() { return new ItemStack(ModBlocks.getMachineBlock(getMachine()).get()); }
    public IGrid getGrid() { return aeSupport().getGrid(); }
    public appeng.api.networking.IManagedGridNode getMainNode() { return aeSupport().getMainNode(); }
    public AeOutputMode getAeOutputMode() { return this.aeOutputMode; }
    public void cycleAeOutputMode() { this.aeOutputMode = this.aeOutputMode.next(); this.owner.setChanged(); }
    public <RECIPE extends MekanismRecipe<?>> CachedRecipe<RECIPE> wrapRecipeEnergy(MachineEnergyContainer<?> energyContainer, CachedRecipe<RECIPE> cachedRecipe) { return aeSupport().wrapRecipeEnergy(energyContainer, cachedRecipe); }
    void clearRemoved() { aeSupport().createOnFirstTick(); }
    void setRemoved() { aeSupport().destroyNode(); }
    void onChunkUnloaded() { aeSupport().destroyNode(); }
    void addContainerTrackers(MekanismContainer container) { aeSupport().addAeTrackers(container, this::getAeOutputMode, mode -> this.aeOutputMode = mode, false); }
    void saveAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) { aeSupport().saveAeState(tag, registries, this.aeOutputMode); }
    void loadAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) { this.aeOutputMode = aeSupport().loadAeState(tag, registries); }
}
