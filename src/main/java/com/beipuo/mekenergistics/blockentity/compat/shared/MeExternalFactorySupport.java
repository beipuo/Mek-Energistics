package com.beipuo.mekenergistics.blockentity.compat.shared;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.GridHelper;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.support.MeFactoryAeSupport;
import com.beipuo.mekenergistics.blockentity.support.MeFactoryInventoryInsert;
import com.beipuo.mekenergistics.blockentity.support.MeFactoryPatternInput;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public final class MeExternalFactorySupport {
    private MeExternalFactorySupport() {
    }

    public interface Owner extends MeFactoryAeMachine {
        List<IInventorySlot> meInputSlots();

        List<IInventorySlot> meOutputSlots();

        void unpauseRecipeMonitors();
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
        if (inputHolder == null || inputHolder.length != 1) {
            return false;
        }
        MeFactoryPatternInput input = MeFactoryPatternInput.single(inputHolder[0]);
        return input != null && input.isItem() && insertItem(owner, input.item());
    }

    public static boolean pushSingleItem(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        return pushSmartPattern(owner, patternDetails, inputHolder, inputs -> pushSingleItem(owner, inputs));
    }

    public static boolean pushSingleItemWithRequiredExtraSlot(Owner owner, KeyCounter[] inputHolder, IInventorySlot extraSlot) {
        if (extraSlot == null || extraSlot.getStack().isEmpty()) {
            return false;
        }
        return pushSingleItem(owner, inputHolder);
    }

    public static boolean pushSingleItemWithRequiredExtraSlot(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, IInventorySlot extraSlot) {
        return pushSmartPattern(owner, patternDetails, inputHolder, inputs -> pushSingleItemWithRequiredExtraSlot(owner, inputs, extraSlot));
    }

    public static boolean pushItemChemical(Owner owner, KeyCounter[] inputHolder, IChemicalTank chemicalTank) {
        if (inputHolder == null || inputHolder.length != 2) {
            return false;
        }
        MeFactoryPatternInput input = MeFactoryPatternInput.separate(inputHolder);
        if (input == null || input.item().isEmpty() || input.chemical().isEmpty() || !input.fluid().isEmpty()) {
            return false;
        }
        if (MeFactoryInventoryInsert.canInsertAcrossSlots(owner.meInputSlots(), input.item())
                && chemicalTank.insert(input.chemical().copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
            MeFactoryInventoryInsert.insertAcrossSlots(owner.meInputSlots(), input.item());
            chemicalTank.insert(input.chemical(), Action.EXECUTE, AutomationType.INTERNAL);
            owner.saveChanges();
            return true;
        }
        return false;
    }

    public static boolean pushItemChemical(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, IChemicalTank chemicalTank) {
        return pushSmartPattern(owner, patternDetails, inputHolder, inputs -> pushItemChemical(owner, inputs, chemicalTank));
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
        fluidTank.insert(input.fluid(), Action.EXECUTE, AutomationType.INTERNAL);
        chemicalTank.insert(input.chemical(), Action.EXECUTE, AutomationType.INTERNAL);
        owner.saveChanges();
        return true;
    }

    public static boolean pushFluidChemical(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, IExtendedFluidTank fluidTank, IChemicalTank chemicalTank) {
        return pushSmartPattern(owner, patternDetails, inputHolder, inputs -> pushFluidChemical(owner, inputs, fluidTank, chemicalTank));
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

    public static boolean pushItemFluidChemical(Owner owner, KeyCounter[] inputHolder, IExtendedFluidTank fluidTank, IChemicalTank chemicalTank) {
        if (inputHolder == null || inputHolder.length != 3) {
            return false;
        }
        MeFactoryPatternInput input = MeFactoryPatternInput.separate(inputHolder);
        if (input == null || input.item().isEmpty() || input.fluid().isEmpty() || input.chemical().isEmpty()
                || !fluidTank.insert(input.fluid().copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()
                || !chemicalTank.insert(input.chemical().copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
            return false;
        }
        if (MeFactoryInventoryInsert.canInsertAcrossSlots(owner.meInputSlots(), input.item())) {
            MeFactoryInventoryInsert.insertAcrossSlots(owner.meInputSlots(), input.item());
            fluidTank.insert(input.fluid(), Action.EXECUTE, AutomationType.INTERNAL);
            chemicalTank.insert(input.chemical(), Action.EXECUTE, AutomationType.INTERNAL);
            owner.saveChanges();
            return true;
        }
        return false;
    }

    public static boolean pushItemFluidChemical(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, IExtendedFluidTank fluidTank, IChemicalTank chemicalTank) {
        return pushSmartPattern(owner, patternDetails, inputHolder, inputs -> pushItemFluidChemical(owner, inputs, fluidTank, chemicalTank));
    }

    private static boolean insertChemical(Owner owner, ChemicalStack chemicalInput, IChemicalTank chemicalTank) {
        if (chemicalTank == null || chemicalInput.isEmpty()
                || !chemicalTank.insert(chemicalInput.copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
            return false;
        }
        chemicalTank.insert(chemicalInput.copy(), Action.EXECUTE, AutomationType.INTERNAL);
        owner.saveChanges();
        return true;
    }

    public static boolean pushTwoItems(Owner owner, KeyCounter[] inputHolder, IInventorySlot extraSlot) {
        if (inputHolder == null || inputHolder.length != 2) {
            return false;
        }
        MeFactoryPatternInput first = MeFactoryPatternInput.single(inputHolder[0]);
        MeFactoryPatternInput second = MeFactoryPatternInput.single(inputHolder[1]);
        if (first == null || second == null || !first.isItem() || !second.isItem()) {
            return false;
        }
        if (MeFactoryInventoryInsert.canInsertAcrossSlots(owner.meInputSlots(), first.item())
                && extraSlot.insertItem(second.item().copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
            MeFactoryInventoryInsert.insertAcrossSlots(owner.meInputSlots(), first.item());
            extraSlot.insertItem(second.item(), Action.EXECUTE, AutomationType.INTERNAL);
            owner.saveChanges();
            return true;
        }
        return false;
    }

    public static boolean pushTwoItems(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, IInventorySlot extraSlot) {
        return pushSmartPattern(owner, patternDetails, inputHolder, inputs -> pushTwoItems(owner, inputs, extraSlot));
    }

    public static boolean pushThreeItems(Owner owner, KeyCounter[] inputHolder, IInventorySlot secondSlot, IInventorySlot thirdSlot) {
        if (inputHolder == null || inputHolder.length != 3) {
            return false;
        }
        ItemStack first = getItem(inputHolder[0]);
        ItemStack second = getItem(inputHolder[1]);
        ItemStack third = getItem(inputHolder[2]);
        if (first.isEmpty() || second.isEmpty() || third.isEmpty()) {
            return false;
        }
        if (MeFactoryInventoryInsert.canInsertAcrossSlots(owner.meInputSlots(), first)
                && secondSlot.insertItem(second.copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()
                && thirdSlot.insertItem(third.copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
            MeFactoryInventoryInsert.insertAcrossSlots(owner.meInputSlots(), first);
            secondSlot.insertItem(second, Action.EXECUTE, AutomationType.INTERNAL);
            thirdSlot.insertItem(third, Action.EXECUTE, AutomationType.INTERNAL);
            owner.saveChanges();
            return true;
        }
        return false;
    }

    public static boolean pushThreeItems(Owner owner, IPatternDetails patternDetails, KeyCounter[] inputHolder, IInventorySlot secondSlot, IInventorySlot thirdSlot) {
        return pushSmartPattern(owner, patternDetails, inputHolder, inputs -> pushThreeItems(owner, inputs, secondSlot, thirdSlot));
    }

    public static boolean insertItem(Owner owner, ItemStack input) {
        if (input.isEmpty()) {
            return false;
        }
        if (MeFactoryInventoryInsert.insertAcrossSlots(owner.meInputSlots(), input)) {
            owner.saveChanges();
            return true;
        }
        return false;
    }

    private static ItemStack getItem(KeyCounter counter) {
        MeFactoryPatternInput input = MeFactoryPatternInput.single(counter);
        return input != null && input.isItem() ? input.item() : ItemStack.EMPTY;
    }

    public static boolean updateServer(Owner owner, boolean sendUpdatePacket) {
        return drainOutputs(owner) || sendUpdatePacket;
    }

    public static boolean updateServer(Owner owner, boolean sendUpdatePacket, IExtendedFluidTank outputTank) {
        return updateServer(owner, sendUpdatePacket) || owner.getAeSupport().insertFluidTankIntoNetwork(outputTank);
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
}
