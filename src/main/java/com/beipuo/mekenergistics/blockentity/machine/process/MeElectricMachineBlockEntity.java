package com.beipuo.mekenergistics.blockentity.machine.process;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeSmartCableConnection;
import com.beipuo.mekenergistics.blockentity.support.MeRecipeMachineAeSupport;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.mixin.TileEntityElectricMachineAccessor;
import com.beipuo.mekenergistics.registry.ModBlocks;
import mekanism.api.IContentsListener;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.SingleItem;
import mekanism.common.tile.prefab.TileEntityElectricMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class MeElectricMachineBlockEntity extends TileEntityElectricMachine
        implements ICraftingProvider, MeSmartCableConnection, IActionHost, MeAeMachine {
    private final MeMekanismMachine machine;
    private MeRecipeMachineAeSupport<MeElectricMachineBlockEntity> aeSupport;
    private AeOutputMode aeOutputMode = AeOutputMode.BOTH;

    public MeElectricMachineBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(ModBlocks.getMachineBlock(machine), pos, state, BASE_TICKS_REQUIRED);
        this.machine = machine;
        getRecipeAeSupport();
    }

    @Override
    public MeRecipeMachineAeSupport<MeElectricMachineBlockEntity> getRecipeAeSupport() {
        if (this.aeSupport == null) {
            this.aeSupport = new MeRecipeMachineAeSupport<>(this);
        }
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
        var energy = new MeRecipeMachineAeSupport.AeBackedEnergyContainer<TileEntityElectricMachine>(this, getRecipeAeSupport(), recipeCacheUnpauseListener);
        ((TileEntityElectricMachineAccessor) this).mekenergistics$setEnergyContainer(energy);
        builder.addContainer(energy);
        return builder.build();
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener, IContentsListener recipeCacheListener,
          IContentsListener recipeCacheUnpauseListener) {
        return getRecipeAeSupport().withPatternSlots(super.getInitialInventory(listener, recipeCacheListener, recipeCacheUnpauseListener));
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = getRecipeAeSupport().processSmartPattern(this::pushPatternInputs);
        sendUpdatePacket |= super.onUpdateServer();
        OutputInventorySlot outputSlot = ((TileEntityElectricMachineAccessor) this).mekenergistics$getOutputSlot();
        return getRecipeAeSupport().drainOutputs(this.aeOutputMode, sendUpdatePacket, outputSlot);
    }

    @NotNull
    @Override
    public CachedRecipe<ItemStackToItemStackRecipe> createNewCachedRecipe(@NotNull ItemStackToItemStackRecipe recipe, int cacheIndex) {
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
        return pushPatternInputs(inputHolder);
    }

    private boolean pushPatternInputs(KeyCounter[] inputHolder) {
        return getRecipeAeSupport().pushSingleItem(inputHolder, ((TileEntityElectricMachineAccessor) this).mekenergistics$getInputSlot());
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    @Override
    public MeMekanismMachine getMachine() {
        return this.machine;
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
        getRecipeAeSupport().createOnFirstTick();
    }

    @Override
    public void setRemoved() {
        getRecipeAeSupport().destroyNode();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        getRecipeAeSupport().destroyNode();
        super.onChunkUnloaded();
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        getRecipeAeSupport().addAeTrackers(container, this::getAeOutputMode, mode -> this.aeOutputMode = mode, true);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        getRecipeAeSupport().saveAeState(tag, registries, this.aeOutputMode);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        this.aeOutputMode = getRecipeAeSupport().loadAeState(tag, registries);
    }
}
