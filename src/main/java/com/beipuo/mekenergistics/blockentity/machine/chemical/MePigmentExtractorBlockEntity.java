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
import com.beipuo.mekenergistics.mixin.TileEntityPigmentExtractorAccessor;
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
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.tile.machine.TileEntityPigmentExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MePigmentExtractorBlockEntity extends TileEntityPigmentExtractor implements ICraftingProvider, MeSmartCableConnection, IActionHost, MeAeMachine {
    private final MeRecipeMachineAeSupport<MePigmentExtractorBlockEntity> aeSupport = new MeRecipeMachineAeSupport<>(this);

    @Override
    public MeRecipeMachineAeSupport<?> getRecipeAeSupport() {
        return this.aeSupport;
    }
    private AeOutputMode aeOutputMode = AeOutputMode.BOTH;

    public MePigmentExtractorBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener, IContentsListener recipeCacheListener, IContentsListener recipeCacheUnpauseListener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        var energy = new MeRecipeMachineAeSupport.AeBackedEnergyContainer<TileEntityPigmentExtractor>(this, this.aeSupport, recipeCacheUnpauseListener);
        ((TileEntityPigmentExtractorAccessor) this).mekenergistics$setEnergyContainer(energy);
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
        boolean sendUpdatePacket = this.aeSupport.processSmartPattern(this::pushPatternInputs);
        sendUpdatePacket |= super.onUpdateServer();
        return this.aeSupport.drainChemicalOutputs(this.aeOutputMode, sendUpdatePacket, this.pigmentTank);
    }

    @NotNull
    @Override
    public mekanism.api.recipes.cache.CachedRecipe<mekanism.api.recipes.ItemStackToChemicalRecipe> createNewCachedRecipe(
            @NotNull mekanism.api.recipes.ItemStackToChemicalRecipe recipe, int cacheIndex) {
        return this.aeSupport.wrapRecipeEnergy(getEnergyContainer(), super.createNewCachedRecipe(recipe, cacheIndex));
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!getMainNode().isActive() || !getAvailablePatterns().contains(patternDetails) || inputHolder == null || inputHolder.length != 1) {
            return false;
        }
        if (this.aeSupport.isSmartPatternMultiplicationEnabled()) {
            return this.aeSupport.enqueueSmartPattern(patternDetails, inputHolder);
        }
        return pushPatternInputs(inputHolder);
    }

    private boolean pushPatternInputs(KeyCounter[] inputHolder) {
        MeFactoryPatternInput input = MeFactoryPatternInput.single(inputHolder[0]);
        if (input == null || !input.isItem()) {
            return false;
        }
        InputInventorySlot inputSlot = (InputInventorySlot) getInputSlot();
        if (!inputSlot.insertItem(input.item().copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
            return false;
        }
        inputSlot.insertItem(input.item(), Action.EXECUTE, AutomationType.INTERNAL);
        setChanged();
        return true;
    }

    @Override public boolean isBusy() { return false; }
    @Override public MeMekanismMachine getMachine() { return MeMekanismMachine.PIGMENT_EXTRACTOR; }
    public appeng.api.networking.IManagedGridNode getMainNode() { return this.aeSupport.getMainNode(); }
    @Override public void setOwner(ServerPlayer player) { MeOwnerHelper.setOwner(this, getMainNode(), player); }
    @Nullable @Override public IGridNode getGridNode(Direction dir) { return getMainNode().getNode(); }
    @Nullable @Override public IGridNode getActionableNode() { return getMainNode().getNode(); }
    @Override public AeOutputMode getAeOutputMode() { return this.aeOutputMode; }
    @Override public void cycleAeOutputMode() { this.aeOutputMode = this.aeOutputMode.next(); setChanged(); }
    @Override public void clearRemoved() { super.clearRemoved(); GridHelper.onFirstTick(this, be -> be.aeSupport.create(be.getLevel(), be.getBlockPos())); }
    @Override public void setRemoved() { this.aeSupport.destroy(); super.setRemoved(); }
    @Override public void onChunkUnloaded() { this.aeSupport.destroy(); super.onChunkUnloaded(); }
    @Override public void addContainerTrackers(MekanismContainer container) { super.addContainerTrackers(container); container.track(SyncableInt.create(() -> this.aeOutputMode.ordinal(), mode -> this.aeOutputMode = AeOutputMode.byId(mode))); }
    @Override public void saveAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) { super.saveAdditional(tag, registries); tag.putInt("AeOutputMode", this.aeOutputMode.ordinal()); this.aeSupport.save(tag); this.aeSupport.saveSlots(tag, registries); }
    @Override public void loadAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) { super.loadAdditional(tag, registries); this.aeOutputMode = AeOutputMode.byId(tag.getInt("AeOutputMode")); this.aeSupport.load(tag); this.aeSupport.loadSlots(tag, registries); }
}

