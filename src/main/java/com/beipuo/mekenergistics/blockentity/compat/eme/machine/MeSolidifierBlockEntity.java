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
import com.beipuo.mekenergistics.mixin.TileEntitySolidifierAccessor;
import fr.iglee42.evolvedmekanism.recipes.SolidificationRecipe;
import fr.iglee42.evolvedmekanism.tiles.machine.TileEntitySolidifier;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.NotNull;

public class MeSolidifierBlockEntity extends TileEntitySolidifier implements ICraftingProvider, MeSmartCableConnection, IActionHost, MeAeMachine {
    private MeRecipeMachineAeSupport<MeSolidifierBlockEntity> aeSupport;
    private AeOutputMode aeOutputMode = AeOutputMode.BOTH;

    public MeSolidifierBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(pos, state);
        getRecipeAeSupport();
    }

    @Override
    public MeRecipeMachineAeSupport<MeSolidifierBlockEntity> getRecipeAeSupport() {
        if (this.aeSupport == null) {
            this.aeSupport = new MeRecipeMachineAeSupport<>(this);
        }
        return this.aeSupport;
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener, IContentsListener recipeCacheListener, IContentsListener recipeCacheUnpauseListener) {
        return getRecipeAeSupport().withPatternSlots(super.getInitialInventory(listener, recipeCacheListener, recipeCacheUnpauseListener));
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        OutputInventorySlot output = ((TileEntitySolidifierAccessor) this).mekenergistics$getOutputSlot();
        return getRecipeAeSupport().drainOutputs(this.aeOutputMode, sendUpdatePacket, output);
    }

    @NotNull
    @Override
    public CachedRecipe<SolidificationRecipe> createNewCachedRecipe(@NotNull SolidificationRecipe recipe, int cacheIndex) {
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
        ItemStack item = ItemStack.EMPTY;
        FluidStack firstFluid = FluidStack.EMPTY;
        FluidStack secondFluid = FluidStack.EMPTY;
        for (KeyCounter counter : inputHolder) {
            MeFactoryPatternInput input = MeFactoryPatternInput.single(counter);
            if (input == null) {
                return false;
            }
            if (input.isItem() && item.isEmpty()) {
                item = input.item();
            } else if (input.isFluid() && firstFluid.isEmpty()) {
                firstFluid = input.fluid();
            } else if (input.isFluid() && secondFluid.isEmpty()) {
                secondFluid = input.fluid();
            } else {
                return false;
            }
        }
        if (item.isEmpty() || firstFluid.isEmpty() || secondFluid.isEmpty()) {
            return false;
        }
        return tryInsert(item, firstFluid, secondFluid) || tryInsert(item, secondFluid, firstFluid);
    }

    private boolean tryInsert(ItemStack item, FluidStack fluid, FluidStack extraFluid) {
        TileEntitySolidifierAccessor accessor = (TileEntitySolidifierAccessor) this;
        InputInventorySlot inputSlot = accessor.mekenergistics$getInputSlot();
        BasicFluidTank fluidTank = accessor.mekenergistics$getInputFluidTank();
        BasicFluidTank extraTank = accessor.mekenergistics$getInputFluidExtraTank();
        if (!inputSlot.insertItem(item.copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()
                || fluidTank.fill(fluid.copy(), FluidAction.SIMULATE) != fluid.getAmount()
                || extraTank.fill(extraFluid.copy(), FluidAction.SIMULATE) != extraFluid.getAmount()) {
            return false;
        }
        inputSlot.insertItem(item.copy(), Action.EXECUTE, AutomationType.INTERNAL);
        fluidTank.fill(fluid.copy(), FluidAction.EXECUTE);
        extraTank.fill(extraFluid.copy(), FluidAction.EXECUTE);
        setChanged();
        return true;
    }

    @Override public boolean isBusy() { return false; }
    @Override public MeMekanismMachine getMachine() { return MeMekanismMachine.SOLIDIFICATION_CHAMBER; }
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
