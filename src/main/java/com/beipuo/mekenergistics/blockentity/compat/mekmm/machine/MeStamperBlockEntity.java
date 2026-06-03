package com.beipuo.mekenergistics.blockentity.compat.mekmm.machine;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.support.MeFactoryPatternInput;
import com.beipuo.mekenergistics.blockentity.MeMekanismMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.support.MeOwnerHelper;
import com.beipuo.mekenergistics.blockentity.support.MeRecipeMachineAeSupport;
import com.beipuo.mekenergistics.blockentity.api.MeSmartCableConnection;
import com.beipuo.mekenergistics.common.MeMekanismMachine;
import com.beipuo.mekenergistics.registry.ModBlocks;
import com.jerry.mekmm.common.tile.machine.TileEntityStamper;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MeStamperBlockEntity extends TileEntityStamper implements ICraftingProvider, MeSmartCableConnection, IActionHost, MeAeMachine {
    private final MeRecipeMachineAeSupport<MeStamperBlockEntity> aeSupport = new MeRecipeMachineAeSupport<>(this);
    private MeMekanismMachineBlockEntity.AeOutputMode aeOutputMode = MeMekanismMachineBlockEntity.AeOutputMode.BOTH;
    private InputInventorySlot meItemInputSlot;
    private InputInventorySlot meMoldInputSlot;
    private OutputInventorySlot meOutputSlot;

    public MeStamperBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener, IContentsListener recipeCacheListener, IContentsListener recipeCacheUnpauseListener) {
        IInventorySlotHolder original = super.getInitialInventory(listener, recipeCacheListener, recipeCacheUnpauseListener);
        captureSlots(original);
        return side -> {
            List<IInventorySlot> slots = new ArrayList<>(original.getInventorySlots(side));
            slots.addAll(this.aeSupport.getPatternSlots());
            return slots;
        };
    }

    private void captureSlots(IInventorySlotHolder original) {
        for (IInventorySlot slot : original.getInventorySlots(null)) {
            if (slot instanceof InputInventorySlot input) {
                if (this.meItemInputSlot == null) {
                    this.meItemInputSlot = input;
                } else if (this.meMoldInputSlot == null) {
                    this.meMoldInputSlot = input;
                }
            } else if (slot instanceof OutputInventorySlot output && this.meOutputSlot == null) {
                this.meOutputSlot = output;
            }
        }
    }

    @Override
    protected boolean onUpdateServer() {
        return this.aeSupport.insertOutputSlotIntoNetwork(this.meOutputSlot, this.aeOutputMode) || super.onUpdateServer();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!getMainNode().isActive() || !getAvailablePatterns().contains(patternDetails) || inputHolder == null || inputHolder.length != 2) {
            return false;
        }
        MeFactoryPatternInput first = MeFactoryPatternInput.single(inputHolder[0]);
        MeFactoryPatternInput second = MeFactoryPatternInput.single(inputHolder[1]);
        if (first == null || second == null || !first.isItem() || !second.isItem()) {
            return false;
        }
        if (this.meItemInputSlot == null || this.meMoldInputSlot == null
                || !this.meItemInputSlot.insertItem(first.item().copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()
                || !this.meMoldInputSlot.insertItem(second.item().copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
            return false;
        }
        this.meItemInputSlot.insertItem(first.item(), Action.EXECUTE, AutomationType.INTERNAL);
        this.meMoldInputSlot.insertItem(second.item(), Action.EXECUTE, AutomationType.INTERNAL);
        setChanged();
        return true;
    }

    @Override public boolean isBusy() { return false; }
    @Override public List<IPatternDetails> getAvailablePatterns() { return this.aeSupport.getAvailablePatterns(); }
    @Override public int getPatternPriority() { return this.aeSupport.getPatternPriority(); }
    @Override public List<BasicInventorySlot> getPatternSlots() { return this.aeSupport.getPatternSlots(); }
    @Override public MeMekanismMachine getMachine() { return MeMekanismMachine.CNC_STAMPER; }
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
