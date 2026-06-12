package com.beipuo.mekenergistics.blockentity.machine.process;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeSmartCableConnection;
import com.beipuo.mekenergistics.blockentity.support.MeOwnerHelper;
import com.beipuo.mekenergistics.blockentity.support.MeRecipeMachineAeSupport;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.mixin.TileEntityElectricMachineAccessor;
import com.beipuo.mekenergistics.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.SingleItem;
import mekanism.common.tile.prefab.TileEntityElectricMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MeElectricMachineBlockEntity extends TileEntityElectricMachine
        implements ICraftingProvider, MeSmartCableConnection, IActionHost, MeAeMachine {
    private final MeMekanismMachine machine;
    private final MeRecipeMachineAeSupport<MeElectricMachineBlockEntity> aeSupport = new MeRecipeMachineAeSupport<>(this);
    private AeOutputMode aeOutputMode = AeOutputMode.BOTH;

    public MeElectricMachineBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(ModBlocks.getMachineBlock(machine), pos, state, BASE_TICKS_REQUIRED);
        this.machine = machine;
    }

    @Override
    public MeRecipeMachineAeSupport<?> getRecipeAeSupport() {
        return this.aeSupport;
    }

    @NotNull
    @Override
    public IMekanismRecipeTypeProvider<SingleRecipeInput, ItemStackToItemStackRecipe, SingleItem<ItemStackToItemStackRecipe>> getRecipeType() {
        return switch (this.machine.factoryType()) {
            case ENRICHING -> MekanismRecipeType.ENRICHING;
            case CRUSHING -> MekanismRecipeType.CRUSHING;
            case SMELTING -> MekanismRecipeType.SMELTING;
            default -> MekanismRecipeType.ENRICHING;
        };
    }

    @Override
    public IRecipeViewerRecipeType<ItemStackToItemStackRecipe> recipeViewerType() {
        return switch (this.machine.factoryType()) {
            case ENRICHING -> RecipeViewerRecipeType.ENRICHING;
            case CRUSHING -> RecipeViewerRecipeType.CRUSHING;
            case SMELTING -> RecipeViewerRecipeType.SMELTING;
            default -> RecipeViewerRecipeType.ENRICHING;
        };
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener, IContentsListener recipeCacheListener,
          IContentsListener recipeCacheUnpauseListener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        var energy = new MeRecipeMachineAeSupport.AeBackedEnergyContainer<TileEntityElectricMachine>(this, this.aeSupport, recipeCacheUnpauseListener);
        ((TileEntityElectricMachineAccessor) this).mekenergistics$setEnergyContainer(energy);
        builder.addContainer(energy);
        return builder.build();
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener, IContentsListener recipeCacheListener,
          IContentsListener recipeCacheUnpauseListener) {
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
        OutputInventorySlot outputSlot = ((TileEntityElectricMachineAccessor) this).mekenergistics$getOutputSlot();
        return this.aeSupport.drainOutputs(this.aeOutputMode, sendUpdatePacket, outputSlot);
    }

    @NotNull
    @Override
    public CachedRecipe<ItemStackToItemStackRecipe> createNewCachedRecipe(@NotNull ItemStackToItemStackRecipe recipe, int cacheIndex) {
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
        return this.aeSupport.pushSingleItem(inputHolder, ((TileEntityElectricMachineAccessor) this).mekenergistics$getInputSlot());
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    @Override
    public MeMekanismMachine getMachine() {
        return this.machine;
    }

    public appeng.api.networking.IManagedGridNode getMainNode() {
        return this.aeSupport.getMainNode();
    }

    @Override
    public void setOwner(ServerPlayer player) {
        MeOwnerHelper.setOwner(this, getMainNode(), player);
    }

    @Nullable
    @Override
    public IGridNode getGridNode(Direction dir) {
        return getMainNode().getNode();
    }

    @Nullable
    @Override
    public IGridNode getActionableNode() {
        return getMainNode().getNode();
    }

    @Override
    public AeOutputMode getAeOutputMode() {
        return this.aeOutputMode;
    }

    @Override
    public void cycleAeOutputMode() {
        this.aeOutputMode = this.aeOutputMode.next();
        setChanged();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        GridHelper.onFirstTick(this, be -> be.aeSupport.create(be.getLevel(), be.getBlockPos()));
    }

    @Override
    public void setRemoved() {
        this.aeSupport.destroy();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        this.aeSupport.destroy();
        super.onChunkUnloaded();
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableInt.create(() -> this.aeOutputMode.ordinal(), mode -> this.aeOutputMode = AeOutputMode.byId(mode)));
        container.track(mekanism.common.inventory.container.sync.SyncableBoolean.create(
              this::isSmartPatternMultiplicationEnabled, this::setSmartPatternMultiplicationEnabled));
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("AeOutputMode", this.aeOutputMode.ordinal());
        this.aeSupport.save(tag);
        this.aeSupport.saveSlots(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        this.aeOutputMode = AeOutputMode.byId(tag.getInt("AeOutputMode"));
        this.aeSupport.load(tag);
        this.aeSupport.loadSlots(tag, registries);
    }
}
