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
import com.beipuo.mekenergistics.blockentity.support.MeFactoryPatternInput;
import com.beipuo.mekenergistics.blockentity.support.MeOwnerHelper;
import com.beipuo.mekenergistics.blockentity.support.MeRecipeMachineAeSupport;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.mixin.TileEntityAdvancedElectricMachineAccessor;
import com.beipuo.mekenergistics.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.ItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.vanilla_input.SingleItemChemicalRecipeInput;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.config.MekanismConfig;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.ItemChemical;
import mekanism.common.tile.prefab.TileEntityAdvancedElectricMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MeAdvancedElectricMachineBlockEntity extends TileEntityAdvancedElectricMachine
        implements ICraftingProvider, MeSmartCableConnection, IActionHost, MeAeMachine {
    private final MeMekanismMachine machine;
    private final MeRecipeMachineAeSupport<MeAdvancedElectricMachineBlockEntity> aeSupport = new MeRecipeMachineAeSupport<>(this);
    private AeOutputMode aeOutputMode = AeOutputMode.BOTH;

    public MeAdvancedElectricMachineBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(ModBlocks.getMachineBlock(machine), pos, state, BASE_TICKS_REQUIRED);
        this.machine = machine;
    }

    @Override
    public MeRecipeMachineAeSupport<?> getRecipeAeSupport() {
        return this.aeSupport;
    }

    @NotNull
    @Override
    public IMekanismRecipeTypeProvider<SingleItemChemicalRecipeInput, ItemStackChemicalToItemStackRecipe, ItemChemical<ItemStackChemicalToItemStackRecipe>> getRecipeType() {
        return switch (this.machine.factoryType()) {
            case COMPRESSING -> MekanismRecipeType.COMPRESSING;
            case PURIFYING -> MekanismRecipeType.PURIFYING;
            case INJECTING -> MekanismRecipeType.INJECTING;
            default -> MekanismRecipeType.COMPRESSING;
        };
    }

    @Override
    public IRecipeViewerRecipeType<ItemStackChemicalToItemStackRecipe> recipeViewerType() {
        return switch (this.machine.factoryType()) {
            case COMPRESSING -> RecipeViewerRecipeType.COMPRESSING;
            case PURIFYING -> RecipeViewerRecipeType.PURIFYING;
            case INJECTING -> RecipeViewerRecipeType.INJECTING;
            default -> RecipeViewerRecipeType.COMPRESSING;
        };
    }

    @Override
    protected boolean useStatisticalMechanics() {
        MeMekanismMachine machine = getMachineEarly();
        return (machine.factoryType() == mekanism.common.content.blocktype.FactoryType.PURIFYING
                || machine.factoryType() == mekanism.common.content.blocktype.FactoryType.INJECTING)
                && MekanismConfig.usage.randomizedConsumption.get();
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener, IContentsListener recipeCacheListener,
          IContentsListener recipeCacheUnpauseListener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        var energy = new MeRecipeMachineAeSupport.AeBackedEnergyContainer<TileEntityAdvancedElectricMachine>(this, this.aeSupport, recipeCacheUnpauseListener);
        ((TileEntityAdvancedElectricMachineAccessor) this).mekenergistics$setEnergyContainer(energy);
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
        OutputInventorySlot outputSlot = ((TileEntityAdvancedElectricMachineAccessor) this).mekenergistics$getOutputSlot();
        return this.aeSupport.drainOutputs(this.aeOutputMode, sendUpdatePacket, outputSlot);
    }

    @NotNull
    @Override
    public CachedRecipe<ItemStackChemicalToItemStackRecipe> createNewCachedRecipe(@NotNull ItemStackChemicalToItemStackRecipe recipe, int cacheIndex) {
        return this.aeSupport.wrapRecipeEnergy(getEnergyContainer(), super.createNewCachedRecipe(recipe, cacheIndex));
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!getMainNode().isActive() || !getAvailablePatterns().contains(patternDetails) || inputHolder == null || inputHolder.length != 2) {
            return false;
        }
        if (this.aeSupport.isSmartPatternMultiplicationEnabled()) {
            return this.aeSupport.enqueueSmartPattern(patternDetails, inputHolder);
        }
        return pushPatternInputs(inputHolder);
    }

    private boolean pushPatternInputs(KeyCounter[] inputHolder) {
        MeFactoryPatternInput input = MeFactoryPatternInput.separate(inputHolder);
        if (input == null || input.item().isEmpty() || input.chemical().isEmpty() || !input.fluid().isEmpty()) {
            return false;
        }
        InputInventorySlot inputSlot = ((TileEntityAdvancedElectricMachineAccessor) this).mekenergistics$getInputSlot();
        if (!inputSlot.insertItem(input.item().copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
            return false;
        }
        if (!this.chemicalTank.insert(input.chemical().copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
            return false;
        }
        inputSlot.insertItem(input.item(), Action.EXECUTE, AutomationType.INTERNAL);
        this.chemicalTank.insert(input.chemical(), Action.EXECUTE, AutomationType.INTERNAL);
        setChanged();
        return true;
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    @Override
    public MeMekanismMachine getMachine() {
        return this.machine;
    }

    private MeMekanismMachine getMachineEarly() {
        return this.machine == null ? ModBlocks.getMachine(getBlockState().getBlock()) : this.machine;
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
