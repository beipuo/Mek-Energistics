package com.beipuo.mekenergistics.blockentity;

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
import com.beipuo.mekenergistics.common.MeMekanismMachine;
import com.beipuo.mekenergistics.mixin.TileEntityPressurizedReactionChamberAccessor;
import com.beipuo.mekenergistics.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.tile.machine.TileEntityPressurizedReactionChamber;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MePressurizedReactionChamberBlockEntity extends TileEntityPressurizedReactionChamber implements ICraftingProvider, MeSmartCableConnection, IActionHost, MeAeMachine {
    private final MeRecipeMachineAeSupport<MePressurizedReactionChamberBlockEntity> aeSupport = new MeRecipeMachineAeSupport<>(this);
    private MeMekanismMachineBlockEntity.AeOutputMode aeOutputMode = MeMekanismMachineBlockEntity.AeOutputMode.BOTH;

    public MePressurizedReactionChamberBlockEntity(MeMekanismMachine machine, BlockPos pos, BlockState state) {
        super(pos, state);
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
        OutputInventorySlot output = ((TileEntityPressurizedReactionChamberAccessor) this).mekenergistics$getOutputSlot();
        boolean changed = this.aeSupport.insertOutputSlotIntoNetwork(output, this.aeOutputMode);
        changed |= this.aeSupport.insertChemicalTankIntoNetwork(this.outputGasTank, this.aeOutputMode);
        return changed || sendUpdatePacket;
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!getMainNode().isActive() || !getAvailablePatterns().contains(patternDetails) || inputHolder == null || inputHolder.length != 3) {
            return false;
        }
        ItemStack itemInput = ItemStack.EMPTY;
        ChemicalStack chemicalInput = ChemicalStack.EMPTY;
        FluidStack fluidInput = FluidStack.EMPTY;
        for (KeyCounter counter : inputHolder) {
            MeFactoryPatternInput input = MeFactoryPatternInput.single(counter);
            if (input == null) {
                return false;
            }
            if (input.isItem()) {
                if (!itemInput.isEmpty()) {
                    return false;
                }
                itemInput = input.item();
            } else if (input.isChemical()) {
                if (!chemicalInput.isEmpty()) {
                    return false;
                }
                chemicalInput = input.chemical();
            } else if (input.isFluid()) {
                if (!fluidInput.isEmpty()) {
                    return false;
                }
                fluidInput = input.fluid();
            }
        }
        if (itemInput.isEmpty() || chemicalInput.isEmpty() || fluidInput.isEmpty()) {
            return false;
        }
        InputInventorySlot inputSlot = ((TileEntityPressurizedReactionChamberAccessor) this).mekenergistics$getInputSlot();
        if (!inputSlot.insertItem(itemInput.copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()
                || this.inputGasTank.insert(chemicalInput.copy(), Action.SIMULATE, AutomationType.INTERNAL).getAmount() != 0
                || this.inputFluidTank.fill(fluidInput.copy(), FluidAction.SIMULATE) != fluidInput.getAmount()) {
            return false;
        }
        inputSlot.insertItem(itemInput, Action.EXECUTE, AutomationType.INTERNAL);
        this.inputGasTank.insert(chemicalInput, Action.EXECUTE, AutomationType.INTERNAL);
        this.inputFluidTank.fill(fluidInput, FluidAction.EXECUTE);
        setChanged();
        return true;
    }

    @Override public boolean isBusy() { return false; }
    @Override public List<IPatternDetails> getAvailablePatterns() { return this.aeSupport.getAvailablePatterns(); }
    @Override public int getPatternPriority() { return this.aeSupport.getPatternPriority(); }
    @Override public List<BasicInventorySlot> getPatternSlots() { return this.aeSupport.getPatternSlots(); }
    @Override public MeMekanismMachine getMachine() { return MeMekanismMachine.PRESSURIZED_REACTION_CHAMBER; }
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

