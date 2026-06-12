package com.beipuo.mekenergistics.blockentity.machine.process;

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
import com.beipuo.mekenergistics.mixin.TileEntityFormulaicAssemblicatorAccessor;
import com.beipuo.mekenergistics.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.tile.machine.TileEntityFormulaicAssemblicator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MeFormulaicAssemblicatorBlockEntity extends TileEntityFormulaicAssemblicator implements ICraftingProvider, MeSmartCableConnection, IActionHost, MeAeMachine {
    private final MeRecipeMachineAeSupport<MeFormulaicAssemblicatorBlockEntity> aeSupport = new MeRecipeMachineAeSupport<>(this);

    @Override
    public MeRecipeMachineAeSupport<?> getRecipeAeSupport() {
        return this.aeSupport;
    }
    private AeOutputMode aeOutputMode = AeOutputMode.BOTH;

    public MeFormulaicAssemblicatorBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        var energy = new MeRecipeMachineAeSupport.AeBackedEnergyContainer<TileEntityFormulaicAssemblicator>(this, this.aeSupport, listener);
        ((TileEntityFormulaicAssemblicatorAccessor) this).mekenergistics$setEnergyContainer(energy);
        builder.addContainer(energy);
        return builder.build();
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        IInventorySlotHolder original = super.getInitialInventory(listener);
        return side -> {
            List<IInventorySlot> slots = new ArrayList<>(original.getInventorySlots(side));
            slots.addAll(this.aeSupport.getPatternSlots());
            return slots;
        };
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = this.aeSupport.processSmartPattern(this::pushPatternInputs);
        sendUpdatePacket |= super.onUpdateServer();
        return this.aeSupport.drainOutputs(this.aeOutputMode, sendUpdatePacket,
                ((TileEntityFormulaicAssemblicatorAccessor) this).mekenergistics$getOutputSlots());
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!getMainNode().isActive() || !getAvailablePatterns().contains(patternDetails) || inputHolder == null || inputHolder.length == 0) {
            return false;
        }
        if (this.aeSupport.isSmartPatternMultiplicationEnabled()) {
            return this.aeSupport.enqueueSmartPattern(patternDetails, inputHolder);
        }
        return pushPatternInputs(inputHolder);
    }

    private boolean pushPatternInputs(KeyCounter[] inputHolder) {
        List<IInventorySlot> inputSlots = ((TileEntityFormulaicAssemblicatorAccessor) this).mekenergistics$getInputSlots();
        List<ItemStack> inputs = new ArrayList<>(inputHolder.length);
        for (KeyCounter counter : inputHolder) {
            MeFactoryPatternInput input = MeFactoryPatternInput.single(counter);
            if (input == null || !input.isItem()) {
                return false;
            }
            inputs.add(input.item());
        }
        List<ItemStack> simulatedSlots = new ArrayList<>(inputSlots.size());
        for (IInventorySlot slot : inputSlots) {
            simulatedSlots.add(slot.getStack().copy());
        }
        for (ItemStack input : inputs) {
            if (!canInsertIntoSnapshot(input.copy(), inputSlots, simulatedSlots)) {
                return false;
            }
        }
        for (ItemStack input : inputs) {
            ItemStack remainder = input;
            for (IInventorySlot slot : inputSlots) {
                remainder = slot.insertItem(remainder, Action.EXECUTE, AutomationType.INTERNAL);
                if (remainder.isEmpty()) {
                    break;
                }
            }
        }
        setChanged();
        return true;
    }

    private static boolean canInsertIntoSnapshot(ItemStack stack, List<IInventorySlot> slots, List<ItemStack> snapshot) {
        ItemStack remainder = stack;
        for (int i = 0; i < slots.size(); i++) {
            IInventorySlot slot = slots.get(i);
            ItemStack current = snapshot.get(i);
            if (remainder.isEmpty() || !slot.isItemValid(remainder)) {
                continue;
            }
            if (current.isEmpty() || ItemStack.isSameItemSameComponents(current, remainder)) {
                int limit = slot.getLimit(remainder);
                int needed = limit - current.getCount();
                if (needed <= 0) {
                    continue;
                }
                int toAdd = Math.min(needed, remainder.getCount());
                if (current.isEmpty()) {
                    snapshot.set(i, remainder.copyWithCount(toAdd));
                } else {
                    current.grow(toAdd);
                }
                remainder = remainder.copyWithCount(remainder.getCount() - toAdd);
            }
        }
        return remainder.isEmpty();
    }

    @Override public boolean isBusy() { return false; }
    @Override public MeMekanismMachine getMachine() { return MeMekanismMachine.FORMULAIC_ASSEMBLICATOR; }
    public appeng.api.networking.IManagedGridNode getMainNode() { return this.aeSupport.getMainNode(); }
    @Override public void setOwner(ServerPlayer player) { MeOwnerHelper.setOwner(this, getMainNode(), player); }
    @Nullable @Override public IGridNode getGridNode(Direction dir) { return getMainNode().getNode(); }
    @Nullable @Override public IGridNode getActionableNode() { return getMainNode().getNode(); }
    @Override public AeOutputMode getAeOutputMode() { return this.aeOutputMode; }
    @Override public void cycleAeOutputMode() { this.aeOutputMode = this.aeOutputMode.next(); setChanged(); }
    @Override public void clearRemoved() { super.clearRemoved(); this.aeSupport.createOnFirstTick(); }
    @Override public void setRemoved() { this.aeSupport.destroyNode(); super.setRemoved(); }
    @Override public void onChunkUnloaded() { this.aeSupport.destroyNode(); super.onChunkUnloaded(); }
    @Override public void addContainerTrackers(MekanismContainer container) { super.addContainerTrackers(container); this.aeSupport.addAeTrackers(container, this::getAeOutputMode, mode -> this.aeOutputMode = mode, true); }
    @Override public void saveAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) { super.saveAdditional(tag, registries); this.aeSupport.saveAeState(tag, registries, this.aeOutputMode); }
    @Override public void loadAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) { super.loadAdditional(tag, registries); this.aeOutputMode = this.aeSupport.loadAeState(tag, registries); }
}

