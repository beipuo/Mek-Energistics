package com.beipuo.mekenergistics.blockentity.compat.shared;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.support.MeFactoryAeSupport;
import com.beipuo.mekenergistics.blockentity.support.MeFactoryInventoryInsert;
import com.beipuo.mekenergistics.blockentity.support.MeFactoryPatternInput;
import com.beipuo.mekenergistics.blockentity.support.MeSmartPatternMultiplication;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.interfaces.ISideConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MeExternalFactorySupport {
    private MeExternalFactorySupport() {
    }

    public interface Owner extends MeFactoryAeMachine {
        List<IInventorySlot> meInputSlots();

        List<IInventorySlot> meOutputSlots();

        void unpauseRecipeMonitors();

        @Override
        default List<IPatternDetails> getAvailablePatterns() {
            return getAeSupport().getAvailablePatterns();
        }

        @Override
        default int getPatternPriority() {
            return getAeSupport().getPatternPriority();
        }

        @Nullable
        @Override
        default IGridNode getGridNode(Direction dir) {
            return getMainNode().getNode();
        }
    }

    public static <TILE extends TileEntityMekanism & ISideConfiguration> IEnergyContainerHolder energyContainers(
            TILE tile, IContentsListener listener, Runnable unpauseRecipeMonitors,
            java.util.function.Consumer<MeFactoryAeSupport.AeBackedFactoryEnergyContainer<TILE>> containerSetter) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(tile);
        MeFactoryAeSupport.AeBackedFactoryEnergyContainer<TILE> container = new MeFactoryAeSupport.AeBackedFactoryEnergyContainer<>(tile, () -> {
            listener.onContentsChanged();
            unpauseRecipeMonitors.run();
        });
        containerSetter.accept(container);
        builder.addContainer(container);
        return builder.build();
    }

    public static IInventorySlotHolder withPatternSlots(IInventorySlotHolder original, Owner owner) {
        return side -> {
            List<IInventorySlot> slots = new ArrayList<>(original.getInventorySlots(side));
            slots.addAll(owner.getAeSupport().getPatternSlots());
            return slots;
        };
    }

    public static boolean pushSingleItem(Owner owner, KeyCounter[] inputHolder) {
        return pushSingleItem(owner, inputHolder, false);
    }

    private static boolean pushSingleItem(Owner owner, KeyCounter[] inputHolder, boolean knownFits) {
        if (inputHolder == null || inputHolder.length != 1) {
            return false;
        }
        MeFactoryPatternInput input = MeFactoryPatternInput.single(inputHolder[0]);
        return input != null && input.isItem() && insertItem(owner, input.item(), knownFits);
    }

    public static boolean pushSingleItem(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        return pushSmartPattern(owner, patternDetails, inputHolder, inputs -> pushSingleItem(owner, inputs));
    }

    public static boolean processSingleItemSmartPatterns(Owner owner) {
        return processIfOutputsClear(owner, () -> owner.getAeSupport().processSmartPattern(new SingleItemFeeder(owner)));
    }

    public static boolean finishSingleItemSmartPatterns(Owner owner) {
        return owner.getAeSupport().processSmartPattern(new SingleItemFeeder(owner));
    }

    public static boolean finishSingleItemSmartPatterns(Owner owner, List<IChemicalTank> outputTanks) {
        return finishSingleItemSmartPatterns(owner);
    }

    public static boolean finishSingleItemSmartPatterns(Owner owner, IExtendedFluidTank outputTank) {
        return finishSingleItemSmartPatterns(owner);
    }

    public static boolean processSingleItemSmartPatterns(Owner owner, List<IChemicalTank> outputTanks) {
        return processIfOutputsClear(owner, outputTanks, () -> owner.getAeSupport().processSmartPattern(new SingleItemFeeder(owner)));
    }

    public static boolean processSingleItemSmartPatterns(Owner owner, IExtendedFluidTank outputTank) {
        return processIfOutputsClear(owner, outputTank, () -> owner.getAeSupport().processSmartPattern(new SingleItemFeeder(owner)));
    }

    public static boolean pushSingleItemWithRequiredExtraSlot(Owner owner, KeyCounter[] inputHolder, IInventorySlot extraSlot) {
        return pushSingleItemWithRequiredExtraSlot(owner, inputHolder, extraSlot, false);
    }

    private static boolean pushSingleItemWithRequiredExtraSlot(Owner owner, KeyCounter[] inputHolder, IInventorySlot extraSlot, boolean knownFits) {
        if (extraSlot == null || extraSlot.getStack().isEmpty()) {
            return false;
        }
        return pushSingleItem(owner, inputHolder, knownFits);
    }

    public static boolean pushSingleItemWithRequiredExtraSlot(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, IInventorySlot extraSlot) {
        return pushSmartPattern(owner, patternDetails, inputHolder, inputs -> pushSingleItemWithRequiredExtraSlot(owner, inputs, extraSlot));
    }

    public static boolean processSingleItemWithRequiredExtraSlotSmartPatterns(Owner owner, IInventorySlot extraSlot) {
        return processIfOutputsClear(owner, () -> owner.getAeSupport().processSmartPattern(new SingleItemWithRequiredExtraSlotFeeder(owner, extraSlot)));
    }

    public static boolean finishSingleItemWithRequiredExtraSlotSmartPatterns(Owner owner, IInventorySlot extraSlot) {
        return owner.getAeSupport().processSmartPattern(new SingleItemWithRequiredExtraSlotFeeder(owner, extraSlot));
    }

    public static boolean pushItemChemical(Owner owner, KeyCounter[] inputHolder, IChemicalTank chemicalTank) {
        return pushItemChemical(owner, inputHolder, chemicalTank, false);
    }

    private static boolean pushItemChemical(Owner owner, KeyCounter[] inputHolder, IChemicalTank chemicalTank, boolean knownFits) {
        if (inputHolder == null || inputHolder.length != 2) {
            return false;
        }
        MeFactoryPatternInput input = MeFactoryPatternInput.separate(inputHolder);
        if (input == null || input.item().isEmpty() || input.chemical().isEmpty() || !input.fluid().isEmpty()) {
            return false;
        }
        boolean canInsertItem = knownFits || MeFactoryInventoryInsert.canInsertAcrossSlots(owner.meInputSlots(), input.item());
        if (canInsertItem && chemicalTank.insert(input.chemical().copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
            InputTransaction transaction = InputTransaction.capture(owner).chemical(chemicalTank);
            boolean chemicalInserted = chemicalTank.insert(input.chemical(), Action.EXECUTE, AutomationType.INTERNAL).isEmpty();
            boolean itemInserted = chemicalInserted && insertItemInput(owner, input.item(), knownFits);
            if (!chemicalInserted || !itemInserted) {
                transaction.restore();
                return false;
            }
            owner.saveChanges();
            return true;
        }
        return false;
    }

    public static boolean pushItemChemical(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, IChemicalTank chemicalTank) {
        return pushSmartPattern(owner, patternDetails, inputHolder, inputs -> pushItemChemical(owner, inputs, chemicalTank));
    }

    public static boolean processItemChemicalSmartPatterns(Owner owner, IChemicalTank chemicalTank) {
        return processIfOutputsClear(owner, () -> owner.getAeSupport().processSmartPattern(new ItemChemicalFeeder(owner, chemicalTank)));
    }

    public static boolean finishItemChemicalSmartPatterns(Owner owner, IChemicalTank chemicalTank) {
        return owner.getAeSupport().processSmartPattern(new ItemChemicalFeeder(owner, chemicalTank));
    }

    public static boolean finishItemChemicalSmartPatterns(Owner owner, IChemicalTank chemicalTank, List<IChemicalTank> outputTanks) {
        return finishItemChemicalSmartPatterns(owner, chemicalTank);
    }

    public static boolean processItemChemicalSmartPatterns(Owner owner, IChemicalTank chemicalTank, List<IChemicalTank> outputTanks) {
        return processIfOutputsClear(owner, outputTanks, () -> owner.getAeSupport().processSmartPattern(new ItemChemicalFeeder(owner, chemicalTank)));
    }

    public static boolean pushChemical(Owner owner, KeyCounter[] inputHolder, IChemicalTank chemicalTank) {
        if (inputHolder == null || inputHolder.length != 1) {
            return false;
        }
        MeFactoryPatternInput input = MeFactoryPatternInput.single(inputHolder[0]);
        if (input == null || !input.isChemical()) {
            return false;
        }
        return insertChemical(owner, input.chemical(), chemicalTank);
    }

    public static boolean pushChemical(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, IChemicalTank chemicalTank) {
        return pushSmartPattern(owner, patternDetails, inputHolder, inputs -> pushChemical(owner, inputs, chemicalTank));
    }

    public static boolean processChemicalSmartPatterns(Owner owner, IChemicalTank chemicalTank) {
        return processIfOutputsClear(owner, () -> owner.getAeSupport().processSmartPattern(new ChemicalFeeder(owner, chemicalTank)));
    }

    public static boolean finishChemicalSmartPatterns(Owner owner, IChemicalTank chemicalTank) {
        return owner.getAeSupport().processSmartPattern(new ChemicalFeeder(owner, chemicalTank));
    }

    public static boolean pushChemical(Owner owner, KeyCounter[] inputHolder, List<IChemicalTank> chemicalTanks) {
        if (inputHolder == null || inputHolder.length != 1) {
            return false;
        }
        MeFactoryPatternInput input = MeFactoryPatternInput.single(inputHolder[0]);
        if (input == null || !input.isChemical()) {
            return false;
        }
        for (IChemicalTank tank : chemicalTanks) {
            if (insertChemical(owner, input.chemical(), tank)) {
                return true;
            }
        }
        return false;
    }

    public static boolean pushChemical(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, List<IChemicalTank> chemicalTanks) {
        return pushSmartPattern(owner, patternDetails, inputHolder, inputs -> pushChemical(owner, inputs, chemicalTanks));
    }

    public static boolean processChemicalSmartPatterns(Owner owner, List<IChemicalTank> chemicalTanks) {
        return processIfOutputsClear(owner, () -> owner.getAeSupport().processSmartPattern(new ChemicalTanksFeeder(owner, chemicalTanks)));
    }

    public static boolean finishChemicalSmartPatterns(Owner owner, List<IChemicalTank> chemicalTanks) {
        return owner.getAeSupport().processSmartPattern(new ChemicalTanksFeeder(owner, chemicalTanks));
    }

    public static boolean finishChemicalSmartPatterns(Owner owner, List<IChemicalTank> chemicalTanks, List<IChemicalTank> outputTanks) {
        return finishChemicalSmartPatterns(owner, chemicalTanks);
    }

    public static boolean processChemicalSmartPatterns(Owner owner, List<IChemicalTank> chemicalTanks, List<IChemicalTank> outputTanks) {
        return processIfOutputsClear(owner, outputTanks, () -> owner.getAeSupport().processSmartPattern(new ChemicalTanksFeeder(owner, chemicalTanks)));
    }

    public static boolean pushFluidChemical(Owner owner, KeyCounter[] inputHolder, IExtendedFluidTank fluidTank, IChemicalTank chemicalTank) {
        if (inputHolder == null || inputHolder.length != 2) {
            return false;
        }
        MeFactoryPatternInput input = MeFactoryPatternInput.separate(inputHolder);
        if (input == null || !input.item().isEmpty() || input.fluid().isEmpty() || input.chemical().isEmpty()
                || !fluidTank.insert(input.fluid().copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()
                || !chemicalTank.insert(input.chemical().copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
            return false;
        }
        InputTransaction transaction = InputTransaction.capture(owner).fluid(fluidTank).chemical(chemicalTank);
        boolean fluidInserted = fluidTank.insert(input.fluid(), Action.EXECUTE, AutomationType.INTERNAL).isEmpty();
        boolean chemicalInserted = fluidInserted && chemicalTank.insert(input.chemical(), Action.EXECUTE, AutomationType.INTERNAL).isEmpty();
        if (!fluidInserted || !chemicalInserted) {
            transaction.restore();
            return false;
        }
        owner.saveChanges();
        return true;
    }

    public static boolean pushFluidChemical(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, IExtendedFluidTank fluidTank, IChemicalTank chemicalTank) {
        return pushSmartPattern(owner, patternDetails, inputHolder, inputs -> pushFluidChemical(owner, inputs, fluidTank, chemicalTank));
    }

    public static boolean processFluidChemicalSmartPatterns(Owner owner, IExtendedFluidTank fluidTank, IChemicalTank chemicalTank) {
        return processIfOutputsClear(owner, () -> owner.getAeSupport().processSmartPattern(new FluidChemicalFeeder(owner, fluidTank, chemicalTank)));
    }

    public static boolean finishFluidChemicalSmartPatterns(Owner owner, IExtendedFluidTank fluidTank, IChemicalTank chemicalTank) {
        return owner.getAeSupport().processSmartPattern(new FluidChemicalFeeder(owner, fluidTank, chemicalTank));
    }

    public static boolean pushFluidChemical(Owner owner, KeyCounter[] inputHolder, IExtendedFluidTank fluidTank, List<IChemicalTank> chemicalTanks) {
        for (IChemicalTank chemicalTank : chemicalTanks) {
            if (pushFluidChemical(owner, inputHolder, fluidTank, chemicalTank)) {
                return true;
            }
        }
        return false;
    }

    public static boolean pushFluidChemical(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, IExtendedFluidTank fluidTank, List<IChemicalTank> chemicalTanks) {
        return pushSmartPattern(owner, patternDetails, inputHolder, inputs -> pushFluidChemical(owner, inputs, fluidTank, chemicalTanks));
    }

    public static boolean processFluidChemicalSmartPatterns(Owner owner, IExtendedFluidTank fluidTank, List<IChemicalTank> chemicalTanks) {
        return processIfOutputsClear(owner, () -> owner.getAeSupport().processSmartPattern(new FluidChemicalTanksFeeder(owner, fluidTank, chemicalTanks)));
    }

    public static boolean finishFluidChemicalSmartPatterns(Owner owner, IExtendedFluidTank fluidTank, List<IChemicalTank> chemicalTanks) {
        return owner.getAeSupport().processSmartPattern(new FluidChemicalTanksFeeder(owner, fluidTank, chemicalTanks));
    }

    public static boolean finishFluidChemicalSmartPatterns(Owner owner, IExtendedFluidTank fluidTank, List<IChemicalTank> chemicalTanks,
            List<IChemicalTank> outputTanks) {
        return finishFluidChemicalSmartPatterns(owner, fluidTank, chemicalTanks);
    }

    public static boolean processFluidChemicalSmartPatterns(Owner owner, IExtendedFluidTank fluidTank, List<IChemicalTank> chemicalTanks, List<IChemicalTank> outputTanks) {
        return processIfOutputsClear(owner, outputTanks, () -> owner.getAeSupport().processSmartPattern(new FluidChemicalTanksFeeder(owner, fluidTank, chemicalTanks)));
    }

    public static boolean pushItemFluidChemical(Owner owner, KeyCounter[] inputHolder, IExtendedFluidTank fluidTank, IChemicalTank chemicalTank) {
        return pushItemFluidChemical(owner, inputHolder, fluidTank, chemicalTank, false);
    }

    private static boolean pushItemFluidChemical(Owner owner, KeyCounter[] inputHolder, IExtendedFluidTank fluidTank, IChemicalTank chemicalTank, boolean knownFits) {
        if (inputHolder == null || inputHolder.length != 3) {
            return false;
        }
        MeFactoryPatternInput input = MeFactoryPatternInput.separate(inputHolder);
        if (input == null || input.item().isEmpty() || input.fluid().isEmpty() || input.chemical().isEmpty()
                || !fluidTank.insert(input.fluid().copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()
                || !chemicalTank.insert(input.chemical().copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
            return false;
        }
        if (knownFits || MeFactoryInventoryInsert.canInsertAcrossSlots(owner.meInputSlots(), input.item())) {
            InputTransaction transaction = InputTransaction.capture(owner).fluid(fluidTank).chemical(chemicalTank);
            boolean fluidInserted = fluidTank.insert(input.fluid(), Action.EXECUTE, AutomationType.INTERNAL).isEmpty();
            boolean chemicalInserted = fluidInserted && chemicalTank.insert(input.chemical(), Action.EXECUTE, AutomationType.INTERNAL).isEmpty();
            boolean itemInserted = chemicalInserted && insertItemInput(owner, input.item(), knownFits);
            if (!fluidInserted || !chemicalInserted || !itemInserted) {
                transaction.restore();
                return false;
            }
            owner.saveChanges();
            return true;
        }
        return false;
    }

    public static boolean pushItemFluidChemical(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, IExtendedFluidTank fluidTank, IChemicalTank chemicalTank) {
        return pushSmartPattern(owner, patternDetails, inputHolder, inputs -> pushItemFluidChemical(owner, inputs, fluidTank, chemicalTank));
    }

    public static boolean processItemFluidChemicalSmartPatterns(Owner owner, IExtendedFluidTank fluidTank, IChemicalTank chemicalTank) {
        return processIfOutputsClear(owner, () -> owner.getAeSupport().processSmartPattern(new ItemFluidChemicalFeeder(owner, fluidTank, chemicalTank)));
    }

    public static boolean finishItemFluidChemicalSmartPatterns(Owner owner, IExtendedFluidTank fluidTank, IChemicalTank chemicalTank) {
        return owner.getAeSupport().processSmartPattern(new ItemFluidChemicalFeeder(owner, fluidTank, chemicalTank));
    }

    public static boolean finishItemFluidChemicalSmartPatterns(Owner owner, IExtendedFluidTank fluidTank, IChemicalTank chemicalTank,
            IChemicalTank outputTank) {
        return finishItemFluidChemicalSmartPatterns(owner, fluidTank, chemicalTank);
    }

    public static boolean processItemFluidChemicalSmartPatterns(Owner owner, IExtendedFluidTank fluidTank, IChemicalTank chemicalTank, IChemicalTank outputTank) {
        return processIfOutputsClear(owner, outputTank, () -> owner.getAeSupport().processSmartPattern(new ItemFluidChemicalFeeder(owner, fluidTank, chemicalTank)));
    }

    private static boolean insertChemical(Owner owner, ChemicalStack chemicalInput, IChemicalTank chemicalTank) {
        if (chemicalTank == null || chemicalInput.isEmpty()
                || !chemicalTank.insert(chemicalInput.copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
            return false;
        }
        ChemicalStack snapshot = chemicalTank.getStack().copy();
        if (!chemicalTank.insert(chemicalInput.copy(), Action.EXECUTE, AutomationType.INTERNAL).isEmpty()) {
            chemicalTank.setStack(snapshot);
            return false;
        }
        owner.saveChanges();
        return true;
    }

    public static boolean pushTwoItems(Owner owner, KeyCounter[] inputHolder, IInventorySlot extraSlot) {
        return pushTwoItems(owner, inputHolder, extraSlot, false);
    }

    private static boolean pushTwoItems(Owner owner, KeyCounter[] inputHolder, IInventorySlot extraSlot, boolean knownFits) {
        if (inputHolder == null || inputHolder.length != 2) {
            return false;
        }
        MeFactoryPatternInput first = MeFactoryPatternInput.single(inputHolder[0]);
        MeFactoryPatternInput second = MeFactoryPatternInput.single(inputHolder[1]);
        if (first == null || second == null || !first.isItem() || !second.isItem()) {
            return false;
        }
        boolean canInsertItem = knownFits || MeFactoryInventoryInsert.canInsertAcrossSlots(owner.meInputSlots(), first.item());
        if (canInsertItem && extraSlot.insertItem(second.item().copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
            InputTransaction transaction = InputTransaction.capture(owner).slot(extraSlot);
            boolean extraInserted = extraSlot.insertItem(second.item(), Action.EXECUTE, AutomationType.INTERNAL).isEmpty();
            boolean itemInserted = extraInserted && insertItemInput(owner, first.item(), knownFits);
            if (!extraInserted || !itemInserted) {
                transaction.restore();
                return false;
            }
            owner.saveChanges();
            return true;
        }
        return false;
    }

    public static boolean pushTwoItems(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, IInventorySlot extraSlot) {
        return pushSmartPattern(owner, patternDetails, inputHolder, inputs -> pushTwoItems(owner, inputs, extraSlot));
    }

    public static boolean processTwoItemsSmartPatterns(Owner owner, IInventorySlot extraSlot) {
        return processIfOutputsClear(owner, () -> owner.getAeSupport().processSmartPattern(new TwoItemsFeeder(owner, extraSlot)));
    }

    public static boolean finishTwoItemsSmartPatterns(Owner owner, IInventorySlot extraSlot) {
        return owner.getAeSupport().processSmartPattern(new TwoItemsFeeder(owner, extraSlot));
    }

    public static boolean pushThreeItems(Owner owner, KeyCounter[] inputHolder, IInventorySlot secondSlot, IInventorySlot thirdSlot) {
        return pushThreeItems(owner, inputHolder, secondSlot, thirdSlot, false);
    }

    private static boolean pushThreeItems(Owner owner, KeyCounter[] inputHolder, IInventorySlot secondSlot, IInventorySlot thirdSlot, boolean knownFits) {
        if (inputHolder == null || inputHolder.length != 3) {
            return false;
        }
        ItemStack first = getItem(inputHolder[0]);
        ItemStack second = getItem(inputHolder[1]);
        ItemStack third = getItem(inputHolder[2]);
        if (first.isEmpty() || second.isEmpty() || third.isEmpty()) {
            return false;
        }
        boolean canInsertItem = knownFits || MeFactoryInventoryInsert.canInsertAcrossSlots(owner.meInputSlots(), first);
        if (canInsertItem && secondSlot.insertItem(second.copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()
                && thirdSlot.insertItem(third.copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
            InputTransaction transaction = InputTransaction.capture(owner).slot(secondSlot).slot(thirdSlot);
            boolean secondInserted = secondSlot.insertItem(second, Action.EXECUTE, AutomationType.INTERNAL).isEmpty();
            boolean thirdInserted = secondInserted && thirdSlot.insertItem(third, Action.EXECUTE, AutomationType.INTERNAL).isEmpty();
            boolean itemInserted = thirdInserted && insertItemInput(owner, first, knownFits);
            if (!secondInserted || !thirdInserted || !itemInserted) {
                transaction.restore();
                return false;
            }
            owner.saveChanges();
            return true;
        }
        return false;
    }

    public static boolean pushThreeItems(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, IInventorySlot secondSlot, IInventorySlot thirdSlot) {
        return pushSmartPattern(owner, patternDetails, inputHolder, inputs -> pushThreeItems(owner, inputs, secondSlot, thirdSlot));
    }

    public static boolean processThreeItemsSmartPatterns(Owner owner, IInventorySlot secondSlot, IInventorySlot thirdSlot) {
        return processIfOutputsClear(owner, () -> owner.getAeSupport().processSmartPattern(new ThreeItemsFeeder(owner, secondSlot, thirdSlot)));
    }

    public static boolean finishThreeItemsSmartPatterns(Owner owner, IInventorySlot secondSlot, IInventorySlot thirdSlot) {
        return owner.getAeSupport().processSmartPattern(new ThreeItemsFeeder(owner, secondSlot, thirdSlot));
    }

    public static boolean insertItem(Owner owner, ItemStack input) {
        return insertItem(owner, input, false);
    }

    private static boolean insertItem(Owner owner, ItemStack input, boolean knownFits) {
        if (input.isEmpty()) {
            return false;
        }
        List<ItemStack> snapshot = knownFits ? MeFactoryInventoryInsert.snapshotSlots(owner.meInputSlots()) : null;
        if (insertItemInput(owner, input, knownFits)) {
            owner.saveChanges();
            return true;
        }
        if (knownFits) {
            MeFactoryInventoryInsert.restoreSlots(owner.meInputSlots(), snapshot);
        }
        return false;
    }

    private static boolean insertItemInput(Owner owner, ItemStack input, boolean knownFits) {
        return knownFits
                ? MeFactoryInventoryInsert.insertAcrossSlotsKnownFits(owner.meInputSlots(), input)
                : MeFactoryInventoryInsert.insertAcrossSlots(owner.meInputSlots(), input);
    }

    private static ItemStack getItem(KeyCounter counter) {
        MeFactoryPatternInput input = MeFactoryPatternInput.single(counter);
        return input != null && input.isItem() ? input.item() : ItemStack.EMPTY;
    }

    public static boolean updateServer(Owner owner, boolean sendUpdatePacket) {
        return drainOutputs(owner) || sendUpdatePacket;
    }

    public static boolean updateServer(Owner owner, boolean sendUpdatePacket, BooleanSupplier processor) {
        boolean changed = updateServer(owner, sendUpdatePacket);
        return processAfterOutputDrain(owner, changed, processor);
    }

    public static boolean updateServer(Owner owner, boolean sendUpdatePacket, IExtendedFluidTank outputTank) {
        boolean changed = updateServer(owner, sendUpdatePacket);
        return owner.getAeSupport().insertFluidTankIntoNetwork(outputTank) || changed;
    }

    public static boolean updateServer(Owner owner, boolean sendUpdatePacket, IExtendedFluidTank outputTank, BooleanSupplier processor) {
        boolean changed = updateServer(owner, sendUpdatePacket, outputTank);
        return processAfterOutputDrain(owner, changed, hasFluidOutputBacklog(outputTank), processor);
    }

    public static boolean updateServer(Owner owner, boolean sendUpdatePacket, List<IChemicalTank> outputTanks) {
        boolean changed = updateServer(owner, sendUpdatePacket);
        return owner.getAeSupport().insertChemicalTanksIntoNetwork(outputTanks) || changed;
    }

    public static boolean updateServer(Owner owner, boolean sendUpdatePacket, List<IChemicalTank> outputTanks, BooleanSupplier processor) {
        boolean changed = updateServer(owner, sendUpdatePacket, outputTanks);
        return processAfterOutputDrain(owner, changed, hasChemicalOutputBacklog(outputTanks), processor);
    }

    public static boolean updateServer(Owner owner, boolean sendUpdatePacket, IChemicalTank outputTank) {
        boolean changed = updateServer(owner, sendUpdatePacket);
        return owner.getAeSupport().insertChemicalTankIntoNetwork(outputTank) || changed;
    }

    public static boolean updateServer(Owner owner, boolean sendUpdatePacket, IChemicalTank outputTank, BooleanSupplier processor) {
        boolean changed = updateServer(owner, sendUpdatePacket, outputTank);
        return processAfterOutputDrain(owner, changed, hasChemicalOutputBacklog(outputTank), processor);
    }

    public static boolean drainOutputs(Owner owner) {
        return owner.getAeSupport().insertOutputSlotsIntoNetwork(owner.meOutputSlots());
    }

    private static boolean pushSmartPattern(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder,
            java.util.function.Function<KeyCounter[], Boolean> feeder) {
        if (!owner.isSmartPatternMultiplicationEnabled()) {
            return feeder.apply(inputHolder);
        }
        return owner.getAeSupport().enqueueSmartPattern(patternDetails, inputHolder);
    }

    public static void createNodeOnFirstTick(TileEntityMekanism tile, MeFactoryAeSupport support, Level level, BlockPos pos) {
        GridHelper.onFirstTick(tile, ignored -> support.create(level, pos));
    }

    public static List<IPatternDetails> getAvailablePatterns(MeFactoryAeSupport support) {
        return support.getAvailablePatterns();
    }

    public static <RECIPE extends MekanismRecipe<?>> CachedRecipe<RECIPE> wrapRecipeEnergy(
            Owner owner, MachineEnergyContainer<?> energyContainer, CachedRecipe<RECIPE> cachedRecipe) {
        return MeFactoryAeSupport.withAeRecipeEnergy(owner, energyContainer, cachedRecipe);
    }

    public static void save(MeFactoryAeSupport support, CompoundTag tag, HolderLookup.Provider registries) {
        support.save(tag);
        support.saveSlots(tag, registries);
    }

    public static void load(MeFactoryAeSupport support, CompoundTag tag, HolderLookup.Provider registries) {
        support.load(tag);
        support.loadSlots(tag, registries);
    }

    private static boolean processIfOutputsClear(Owner owner, BooleanSupplier processor) {
        owner.getAeSupport().markOwnerHandlesSmartPatternProcessing();
        boolean changed = drainOutputs(owner);
        return hasItemOutputBacklog(owner) ? changed : processor.getAsBoolean() || changed;
    }

    private static boolean processIfOutputsClear(Owner owner, List<IChemicalTank> outputTanks, BooleanSupplier processor) {
        owner.getAeSupport().markOwnerHandlesSmartPatternProcessing();
        boolean changed = updateServer(owner, false, outputTanks);
        return hasItemOutputBacklog(owner) || hasChemicalOutputBacklog(outputTanks) ? changed : processor.getAsBoolean() || changed;
    }

    private static boolean processIfOutputsClear(Owner owner, IChemicalTank outputTank, BooleanSupplier processor) {
        owner.getAeSupport().markOwnerHandlesSmartPatternProcessing();
        boolean changed = updateServer(owner, false, outputTank);
        return hasItemOutputBacklog(owner) || hasChemicalOutputBacklog(outputTank) ? changed : processor.getAsBoolean() || changed;
    }

    private static boolean processIfOutputsClear(Owner owner, IExtendedFluidTank outputTank, BooleanSupplier processor) {
        owner.getAeSupport().markOwnerHandlesSmartPatternProcessing();
        boolean changed = updateServer(owner, false, outputTank);
        return hasItemOutputBacklog(owner) || hasFluidOutputBacklog(outputTank) ? changed : processor.getAsBoolean() || changed;
    }

    private static boolean processAfterOutputDrain(Owner owner, boolean changed, BooleanSupplier processor) {
        return processAfterOutputDrain(owner, changed, false, processor);
    }

    private static boolean processAfterOutputDrain(Owner owner, boolean changed, boolean nonItemOutputBacklog, BooleanSupplier processor) {
        return hasItemOutputBacklog(owner) || nonItemOutputBacklog ? changed : processor.getAsBoolean() || changed;
    }

    private static boolean hasItemOutputBacklog(Owner owner) {
        for (IInventorySlot outputSlot : owner.meOutputSlots()) {
            if (outputSlot != null && !outputSlot.getStack().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasChemicalOutputBacklog(List<IChemicalTank> outputTanks) {
        if (outputTanks == null) {
            return false;
        }
        for (IChemicalTank outputTank : outputTanks) {
            if (hasChemicalOutputBacklog(outputTank)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasChemicalOutputBacklog(IChemicalTank outputTank) {
        return outputTank != null && !outputTank.isEmpty();
    }

    private static boolean hasFluidOutputBacklog(IExtendedFluidTank outputTank) {
        return outputTank != null && !outputTank.isEmpty();
    }

    private static final class SingleItemFeeder implements MeSmartPatternMultiplication.CapacityAwareFeeder {
        private final Owner owner;

        private SingleItemFeeder(Owner owner) {
            this.owner = owner;
        }

        @Override
        public boolean feed(KeyCounter[] oneCraftInputs) {
            return pushSingleItem(this.owner, oneCraftInputs, true);
        }

        @Override
        public long maxAcceptedCopies(KeyCounter[] oneCraftInputs) {
            ItemStack input = singleItem(oneCraftInputs);
            return input.isEmpty() ? 0 : MeFactoryInventoryInsert.acceptedCopiesAcrossSlots(this.owner.meInputSlots(), input);
        }
    }

    private static final class SingleItemWithRequiredExtraSlotFeeder implements MeSmartPatternMultiplication.CapacityAwareFeeder {
        private final Owner owner;
        private final IInventorySlot extraSlot;

        private SingleItemWithRequiredExtraSlotFeeder(Owner owner, IInventorySlot extraSlot) {
            this.owner = owner;
            this.extraSlot = extraSlot;
        }

        @Override
        public boolean feed(KeyCounter[] oneCraftInputs) {
            return pushSingleItemWithRequiredExtraSlot(this.owner, oneCraftInputs, this.extraSlot, true);
        }

        @Override
        public long maxAcceptedCopies(KeyCounter[] oneCraftInputs) {
            if (this.extraSlot == null || this.extraSlot.getStack().isEmpty()) {
                return 0;
            }
            ItemStack input = singleItem(oneCraftInputs);
            return input.isEmpty() ? 0 : MeFactoryInventoryInsert.acceptedCopiesAcrossSlots(this.owner.meInputSlots(), input);
        }
    }

    private static final class ItemChemicalFeeder implements MeSmartPatternMultiplication.CapacityAwareFeeder {
        private final Owner owner;
        private final IChemicalTank chemicalTank;

        private ItemChemicalFeeder(Owner owner, IChemicalTank chemicalTank) {
            this.owner = owner;
            this.chemicalTank = chemicalTank;
        }

        @Override
        public boolean feed(KeyCounter[] oneCraftInputs) {
            return pushItemChemical(this.owner, oneCraftInputs, this.chemicalTank, true);
        }

        @Override
        public long maxAcceptedCopies(KeyCounter[] oneCraftInputs) {
            MeFactoryPatternInput input = separateItemChemical(oneCraftInputs);
            if (input == null) {
                return 0;
            }
            long itemCopies = MeFactoryInventoryInsert.acceptedCopiesAcrossSlots(this.owner.meInputSlots(), input.item());
            long chemicalCopies = MeFactoryInventoryInsert.acceptedCopiesIntoChemicalTank(this.chemicalTank, input.chemical());
            return Math.min(itemCopies, chemicalCopies);
        }
    }

    private static final class ChemicalFeeder implements MeSmartPatternMultiplication.CapacityAwareFeeder {
        private final Owner owner;
        private final IChemicalTank chemicalTank;

        private ChemicalFeeder(Owner owner, IChemicalTank chemicalTank) {
            this.owner = owner;
            this.chemicalTank = chemicalTank;
        }

        @Override
        public boolean feed(KeyCounter[] oneCraftInputs) {
            return pushChemical(this.owner, oneCraftInputs, this.chemicalTank);
        }

        @Override
        public long maxAcceptedCopies(KeyCounter[] oneCraftInputs) {
            ChemicalStack input = singleChemical(oneCraftInputs);
            return input.isEmpty() ? 0 : MeFactoryInventoryInsert.acceptedCopiesIntoChemicalTank(this.chemicalTank, input);
        }
    }

    private static final class ChemicalTanksFeeder implements MeSmartPatternMultiplication.CapacityAwareFeeder {
        private final Owner owner;
        private final List<IChemicalTank> chemicalTanks;

        private ChemicalTanksFeeder(Owner owner, List<IChemicalTank> chemicalTanks) {
            this.owner = owner;
            this.chemicalTanks = chemicalTanks;
        }

        @Override
        public boolean feed(KeyCounter[] oneCraftInputs) {
            return pushChemical(this.owner, oneCraftInputs, this.chemicalTanks);
        }

        @Override
        public long maxAcceptedCopies(KeyCounter[] oneCraftInputs) {
            ChemicalStack input = singleChemical(oneCraftInputs);
            if (input.isEmpty() || this.chemicalTanks == null) {
                return 0;
            }
            long accepted = 0;
            for (IChemicalTank tank : this.chemicalTanks) {
                accepted = Math.max(accepted, MeFactoryInventoryInsert.acceptedCopiesIntoChemicalTank(tank, input));
            }
            return accepted;
        }
    }

    private static final class FluidChemicalFeeder implements MeSmartPatternMultiplication.CapacityAwareFeeder {
        private final Owner owner;
        private final IExtendedFluidTank fluidTank;
        private final IChemicalTank chemicalTank;

        private FluidChemicalFeeder(Owner owner, IExtendedFluidTank fluidTank, IChemicalTank chemicalTank) {
            this.owner = owner;
            this.fluidTank = fluidTank;
            this.chemicalTank = chemicalTank;
        }

        @Override
        public boolean feed(KeyCounter[] oneCraftInputs) {
            return pushFluidChemical(this.owner, oneCraftInputs, this.fluidTank, this.chemicalTank);
        }

        @Override
        public long maxAcceptedCopies(KeyCounter[] oneCraftInputs) {
            MeFactoryPatternInput input = separateFluidChemical(oneCraftInputs);
            if (input == null) {
                return 0;
            }
            long fluidCopies = MeFactoryInventoryInsert.acceptedCopiesIntoFluidTank(this.fluidTank, input.fluid());
            long chemicalCopies = MeFactoryInventoryInsert.acceptedCopiesIntoChemicalTank(this.chemicalTank, input.chemical());
            return Math.min(fluidCopies, chemicalCopies);
        }
    }

    private static final class FluidChemicalTanksFeeder implements MeSmartPatternMultiplication.CapacityAwareFeeder {
        private final Owner owner;
        private final IExtendedFluidTank fluidTank;
        private final List<IChemicalTank> chemicalTanks;

        private FluidChemicalTanksFeeder(Owner owner, IExtendedFluidTank fluidTank, List<IChemicalTank> chemicalTanks) {
            this.owner = owner;
            this.fluidTank = fluidTank;
            this.chemicalTanks = chemicalTanks;
        }

        @Override
        public boolean feed(KeyCounter[] oneCraftInputs) {
            return pushFluidChemical(this.owner, oneCraftInputs, this.fluidTank, this.chemicalTanks);
        }

        @Override
        public long maxAcceptedCopies(KeyCounter[] oneCraftInputs) {
            MeFactoryPatternInput input = separateFluidChemical(oneCraftInputs);
            if (input == null || this.chemicalTanks == null) {
                return 0;
            }
            long chemicalCopies = 0;
            for (IChemicalTank tank : this.chemicalTanks) {
                chemicalCopies = Math.max(chemicalCopies, MeFactoryInventoryInsert.acceptedCopiesIntoChemicalTank(tank, input.chemical()));
            }
            long fluidCopies = MeFactoryInventoryInsert.acceptedCopiesIntoFluidTank(this.fluidTank, input.fluid());
            return Math.min(fluidCopies, chemicalCopies);
        }
    }

    private static final class ItemFluidChemicalFeeder implements MeSmartPatternMultiplication.CapacityAwareFeeder {
        private final Owner owner;
        private final IExtendedFluidTank fluidTank;
        private final IChemicalTank chemicalTank;

        private ItemFluidChemicalFeeder(Owner owner, IExtendedFluidTank fluidTank, IChemicalTank chemicalTank) {
            this.owner = owner;
            this.fluidTank = fluidTank;
            this.chemicalTank = chemicalTank;
        }

        @Override
        public boolean feed(KeyCounter[] oneCraftInputs) {
            return pushItemFluidChemical(this.owner, oneCraftInputs, this.fluidTank, this.chemicalTank, true);
        }

        @Override
        public long maxAcceptedCopies(KeyCounter[] oneCraftInputs) {
            MeFactoryPatternInput input = separateItemFluidChemical(oneCraftInputs);
            if (input == null) {
                return 0;
            }
            long itemCopies = MeFactoryInventoryInsert.acceptedCopiesAcrossSlots(this.owner.meInputSlots(), input.item());
            long fluidCopies = MeFactoryInventoryInsert.acceptedCopiesIntoFluidTank(this.fluidTank, input.fluid());
            long chemicalCopies = MeFactoryInventoryInsert.acceptedCopiesIntoChemicalTank(this.chemicalTank, input.chemical());
            return Math.min(itemCopies, Math.min(fluidCopies, chemicalCopies));
        }
    }

    private static final class TwoItemsFeeder implements MeSmartPatternMultiplication.CapacityAwareFeeder {
        private final Owner owner;
        private final IInventorySlot extraSlot;

        private TwoItemsFeeder(Owner owner, IInventorySlot extraSlot) {
            this.owner = owner;
            this.extraSlot = extraSlot;
        }

        @Override
        public boolean feed(KeyCounter[] oneCraftInputs) {
            return pushTwoItems(this.owner, oneCraftInputs, this.extraSlot, true);
        }

        @Override
        public long maxAcceptedCopies(KeyCounter[] oneCraftInputs) {
            if (oneCraftInputs == null || oneCraftInputs.length != 2) {
                return 0;
            }
            MeFactoryPatternInput first = MeFactoryPatternInput.single(oneCraftInputs[0]);
            MeFactoryPatternInput second = MeFactoryPatternInput.single(oneCraftInputs[1]);
            if (first == null || second == null || !first.isItem() || !second.isItem()) {
                return 0;
            }
            long mainCopies = MeFactoryInventoryInsert.acceptedCopiesAcrossSlots(this.owner.meInputSlots(), first.item());
            long extraCopies = MeFactoryInventoryInsert.acceptedCopiesIntoSlot(this.extraSlot, second.item());
            return Math.min(mainCopies, extraCopies);
        }
    }

    private static final class ThreeItemsFeeder implements MeSmartPatternMultiplication.CapacityAwareFeeder {
        private final Owner owner;
        private final IInventorySlot secondSlot;
        private final IInventorySlot thirdSlot;

        private ThreeItemsFeeder(Owner owner, IInventorySlot secondSlot, IInventorySlot thirdSlot) {
            this.owner = owner;
            this.secondSlot = secondSlot;
            this.thirdSlot = thirdSlot;
        }

        @Override
        public boolean feed(KeyCounter[] oneCraftInputs) {
            return pushThreeItems(this.owner, oneCraftInputs, this.secondSlot, this.thirdSlot, true);
        }

        @Override
        public long maxAcceptedCopies(KeyCounter[] oneCraftInputs) {
            if (oneCraftInputs == null || oneCraftInputs.length != 3) {
                return 0;
            }
            ItemStack first = getItem(oneCraftInputs[0]);
            ItemStack second = getItem(oneCraftInputs[1]);
            ItemStack third = getItem(oneCraftInputs[2]);
            if (first.isEmpty() || second.isEmpty() || third.isEmpty()) {
                return 0;
            }
            long mainCopies = MeFactoryInventoryInsert.acceptedCopiesAcrossSlots(this.owner.meInputSlots(), first);
            long secondCopies = MeFactoryInventoryInsert.acceptedCopiesIntoSlot(this.secondSlot, second);
            long thirdCopies = MeFactoryInventoryInsert.acceptedCopiesIntoSlot(this.thirdSlot, third);
            return Math.min(mainCopies, Math.min(secondCopies, thirdCopies));
        }
    }

    private static final class InputTransaction {
        private final Owner owner;
        private final List<ItemStack> inputSlots;
        private final List<SlotSnapshot> slots = new ArrayList<>();
        private final List<FluidSnapshot> fluidTanks = new ArrayList<>();
        private final List<ChemicalSnapshot> chemicalTanks = new ArrayList<>();

        private InputTransaction(Owner owner) {
            this.owner = owner;
            this.inputSlots = MeFactoryInventoryInsert.snapshotSlots(owner.meInputSlots());
        }

        private static InputTransaction capture(Owner owner) {
            return new InputTransaction(owner);
        }

        private InputTransaction slot(IInventorySlot slot) {
            this.slots.add(new SlotSnapshot(slot, slot == null ? ItemStack.EMPTY : slot.getStack().copy()));
            return this;
        }

        private InputTransaction fluid(IExtendedFluidTank tank) {
            this.fluidTanks.add(new FluidSnapshot(tank, tank == null ? FluidStack.EMPTY : tank.getFluid().copy()));
            return this;
        }

        private InputTransaction chemical(IChemicalTank tank) {
            this.chemicalTanks.add(new ChemicalSnapshot(tank, tank == null ? ChemicalStack.EMPTY : tank.getStack().copy()));
            return this;
        }

        private void restore() {
            MeFactoryInventoryInsert.restoreSlots(this.owner.meInputSlots(), this.inputSlots);
            for (SlotSnapshot snapshot : this.slots) {
                if (snapshot.slot() != null) {
                    snapshot.slot().setStack(snapshot.stack().copy());
                }
            }
            for (FluidSnapshot snapshot : this.fluidTanks) {
                if (snapshot.tank() != null) {
                    snapshot.tank().setStack(snapshot.stack().copy());
                }
            }
            for (ChemicalSnapshot snapshot : this.chemicalTanks) {
                if (snapshot.tank() != null) {
                    snapshot.tank().setStack(snapshot.stack().copy());
                }
            }
        }
    }

    private record SlotSnapshot(IInventorySlot slot, ItemStack stack) {
    }

    private record FluidSnapshot(IExtendedFluidTank tank, FluidStack stack) {
    }

    private record ChemicalSnapshot(IChemicalTank tank, ChemicalStack stack) {
    }

    private static ItemStack singleItem(KeyCounter[] inputHolder) {
        if (inputHolder == null || inputHolder.length != 1) {
            return ItemStack.EMPTY;
        }
        return getItem(inputHolder[0]);
    }

    private static ChemicalStack singleChemical(KeyCounter[] inputHolder) {
        if (inputHolder == null || inputHolder.length != 1) {
            return ChemicalStack.EMPTY;
        }
        MeFactoryPatternInput input = MeFactoryPatternInput.single(inputHolder[0]);
        return input != null && input.isChemical() ? input.chemical() : ChemicalStack.EMPTY;
    }

    @Nullable
    private static MeFactoryPatternInput separateItemChemical(KeyCounter[] inputHolder) {
        MeFactoryPatternInput input = MeFactoryPatternInput.separate(inputHolder);
        return input != null && !input.item().isEmpty() && !input.chemical().isEmpty() && input.fluid().isEmpty() ? input : null;
    }

    @Nullable
    private static MeFactoryPatternInput separateFluidChemical(KeyCounter[] inputHolder) {
        MeFactoryPatternInput input = MeFactoryPatternInput.separate(inputHolder);
        return input != null && input.item().isEmpty() && !input.fluid().isEmpty() && !input.chemical().isEmpty() ? input : null;
    }

    @Nullable
    private static MeFactoryPatternInput separateItemFluidChemical(KeyCounter[] inputHolder) {
        MeFactoryPatternInput input = MeFactoryPatternInput.separate(inputHolder);
        return input != null && !input.item().isEmpty() && !input.fluid().isEmpty() && !input.chemical().isEmpty() ? input : null;
    }
}
