package com.beipuo.mekenergistics.blockentity.factory;

import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.support.MeFactoryAeSupport;
import com.beipuo.mekenergistics.blockentity.support.MeFactoryPatternInput;
import com.beipuo.mekenergistics.blockentity.support.MeOwnerHelper;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.GridHelper;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.common.MeMekanismMachine;
import com.beipuo.mekenergistics.mixin.TileEntityCombiningFactoryAccessor;
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
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.tile.factory.TileEntityCombiningFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MeCombiningFactoryBlockEntity extends TileEntityCombiningFactory implements MeFactoryAeMachine {
    private final MeMekanismMachine machine;
    private final MeFactoryAeSupport aeSupport;

    public MeCombiningFactoryBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(ModBlocks.getMachineBlock(machine), pos, state);
        this.machine = machine;
        this.aeSupport = new MeFactoryAeSupport(this);
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        builder.addContainer(this.energyContainer = new MeFactoryAeSupport.AeBackedFactoryEnergyContainer(this, () -> {
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
        IInventorySlotHolder original = super.getInitialInventory(listener);
        return side -> {
            List<IInventorySlot> slots = new ArrayList<>(original.getInventorySlots(side));
            slots.addAll(this.aeSupport.getPatternSlots());
            return slots;
        };
    }

    @Override public MeFactoryAeSupport getAeSupport() { return this.aeSupport; }
    @Override public MeMekanismMachine getMachine() { return this.machine; }
    @Override public Level getOwnerLevel() { return getLevel(); }
    public void setOwner(ServerPlayer player) { MeOwnerHelper.setOwner(this, getMainNode(), player); }
    @Override public List<IPatternDetails> getAvailablePatterns() { return this.aeSupport.getAvailablePatterns(); }
    @Override public int getPatternPriority() { return this.aeSupport.getPatternPriority(); }

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
        ItemStack main = first.item();
        ItemStack extra = second.item();
        InputInventorySlot extraSlot = ((TileEntityCombiningFactoryAccessor) this).mekenergistics$getExtraSlot();
        for (var inputSlot : this.inputSlots) {
            if (inputSlot.insertItem(main.copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()
                    && extraSlot.insertItem(extra.copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
                inputSlot.insertItem(main, Action.EXECUTE, AutomationType.INTERNAL);
                extraSlot.insertItem(extra, Action.EXECUTE, AutomationType.INTERNAL);
                setChanged();
                return true;
            }
        }
        return false;
    }

    @Override public boolean isBusy() { return false; }
    @Override protected boolean onUpdateServer() { boolean sendUpdatePacket = super.onUpdateServer(); return this.aeSupport.insertOutputSlotsIntoNetwork(this.outputSlots) || sendUpdatePacket; }
    @Override public void clearRemoved() { super.clearRemoved(); GridHelper.onFirstTick(this, be -> be.aeSupport.create(be.getLevel(), be.getBlockPos())); }
    @Override public void setRemoved() { this.aeSupport.destroy(); super.setRemoved(); }
    @Override public void onChunkUnloaded() { this.aeSupport.destroy(); super.onChunkUnloaded(); }
    @Nullable @Override public appeng.api.networking.IGridNode getGridNode(Direction dir) { return MeFactoryAeMachine.super.getGridNode(dir); }
    @Override public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); this.aeSupport.save(tag); this.aeSupport.saveSlots(tag, registries); }
    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.loadAdditional(tag, registries); this.aeSupport.load(tag); this.aeSupport.loadSlots(tag, registries); }
}
