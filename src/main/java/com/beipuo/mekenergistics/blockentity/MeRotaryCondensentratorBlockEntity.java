package com.beipuo.mekenergistics.blockentity;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.common.MeMekanismMachine;
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
    private final MeRecipeMachineAeSupport<MeRotaryCondensentratorBlockEntity> aeSupport = new MeRecipeMachineAeSupport<>(this);
    private MeMekanismMachineBlockEntity.AeOutputMode aeOutputMode = MeMekanismMachineBlockEntity.AeOutputMode.BOTH;

    public MeRotaryCondensentratorBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener, IContentsListener recipeCacheListener, IContentsListener recipeCacheUnpauseListener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        var energy = new MeRecipeMachineAeSupport.AeBackedEnergyContainer<TileEntityRotaryCondensentrator>(this, this.aeSupport, recipeCacheUnpauseListener);
        ((TileEntityRotaryCondensentratorAccessor) this).mekenergistics$setEnergyContainer(energy);
        builder.addContainer(energy);
        return builder.build();
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener, IContentsListener recipeCacheListener, IContentsListener recipeCacheUnpauseListener) {
        IInventorySlotHolder original = super.getInitialInventory(listener, recipeCacheListener, recipeCacheUnpauseListener);
        return side -> {
            List<IInventorySlot> slots = new ArrayList<>(original.getInventorySlots(side));
            slots.addAll(this.aeSupport.getPatternSlots());
            return slots;
        };
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        boolean changed = getMode()
                ? this.aeSupport.insertChemicalTankIntoNetwork(this.gasTank, this.aeOutputMode)
                : this.aeSupport.insertFluidTankIntoNetwork(this.fluidTank, this.aeOutputMode);
        return changed || sendUpdatePacket;
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!getMainNode().isActive() || !getAvailablePatterns().contains(patternDetails) || inputHolder == null || inputHolder.length != 1) {
            return false;
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
    @Override public List<IPatternDetails> getAvailablePatterns() { return this.aeSupport.getAvailablePatterns(); }
    @Override public int getPatternPriority() { return this.aeSupport.getPatternPriority(); }
    @Override public List<BasicInventorySlot> getPatternSlots() { return this.aeSupport.getPatternSlots(); }
    @Override public MeMekanismMachine getMachine() { return MeMekanismMachine.ROTARY_CONDENSENTRATOR; }
    @Override public ItemStack getTerminalIconStack() { return new ItemStack(ModBlocks.getMachineBlock(getMachine()).get()); }
    @Override public IGrid getGrid() { return this.aeSupport.getGrid(); }
    public appeng.api.networking.IManagedGridNode getMainNode() { return this.aeSupport.getMainNode(); }
    @Override public void setOwner(ServerPlayer player) { MeOwnerHelper.setOwner(this, getMainNode(), player); }
    @Nullable @Override public IGridNode getGridNode(Direction dir) { return getMainNode().getNode(); }
    @Nullable @Override public IGridNode getActionableNode() { return getMainNode().getNode(); }
    @Override public MeMekanismMachineBlockEntity.AeOutputMode getAeOutputMode() { return this.aeOutputMode; }
    @Override public void cycleAeOutputMode() { this.aeOutputMode = this.aeOutputMode.next(); setChanged(); }
    @Override public void clearRemoved() { super.clearRemoved(); GridHelper.onFirstTick(this, be -> be.aeSupport.create(be.getLevel(), be.getBlockPos())); }
    @Override public void setRemoved() { this.aeSupport.destroy(); super.setRemoved(); }
    @Override public void onChunkUnloaded() { this.aeSupport.destroy(); super.onChunkUnloaded(); }
    @Override public void addContainerTrackers(MekanismContainer container) { super.addContainerTrackers(container); container.track(SyncableInt.create(() -> this.aeOutputMode.ordinal(), mode -> this.aeOutputMode = MeMekanismMachineBlockEntity.AeOutputMode.byId(mode))); }
    @Override public void saveAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) { super.saveAdditional(tag, registries); tag.putInt("AeOutputMode", this.aeOutputMode.ordinal()); this.aeSupport.save(tag); this.aeSupport.saveSlots(tag, registries); }
    @Override public void loadAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) { super.loadAdditional(tag, registries); this.aeOutputMode = MeMekanismMachineBlockEntity.AeOutputMode.byId(tag.getInt("AeOutputMode")); this.aeSupport.load(tag); this.aeSupport.loadSlots(tag, registries); }
}

