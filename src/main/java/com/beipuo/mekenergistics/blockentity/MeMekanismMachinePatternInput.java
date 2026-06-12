package com.beipuo.mekenergistics.blockentity;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ItemStackToChemicalRecipe;
import mekanism.common.recipe.MekanismRecipeType;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

final class MeMekanismMachinePatternInput {
    private MeMekanismMachinePatternInput() {
    }

    static boolean push(MeMekanismMachineBlockEntity owner, IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (tryInsertItemChemicalPatternInputs(owner, inputHolder)) {
            return true;
        }

        if (tryInsertPatternInputs(owner, inputHolder)) {
            return true;
        }

        return tryInsertChemicalConversionInput(owner, patternDetails, inputHolder);
    }

    private static ItemStack getSingleItemInput(KeyCounter counter) {
        if (counter == null || counter.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack input = ItemStack.EMPTY;
        for (var entry : counter) {
            AEKey key = entry.getKey();
            long amount = entry.getLongValue();
            if (!(key instanceof AEItemKey itemKey) || amount <= 0 || amount > Integer.MAX_VALUE || !input.isEmpty()) {
                return ItemStack.EMPTY;
            }
            input = itemKey.toStack((int) amount);
        }
        return input;
    }

    private static boolean tryInsertChemicalConversionInput(MeMekanismMachineBlockEntity owner, IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (owner.getMachine().slotLayout() != MeMekanismMachine.SlotLayout.ITEM_CHEMICAL || owner.getLevel() == null || inputHolder == null || inputHolder.length != 1) {
            return false;
        }

        ItemStack input = getSingleItemInput(inputHolder[0]);
        if (input.isEmpty()) {
            return false;
        }

        ItemStack singleInput = input.copyWithCount(1);
        ItemStackToChemicalRecipe recipe = MekanismRecipeType.CHEMICAL_CONVERSION.getInputCache().findTypeBasedRecipe(owner.getLevel(), singleInput);
        if (recipe == null) {
            return false;
        }

        int needed = MeMekanismMachineBlockEntity.clampNeeded(recipe.getInput().getNeededAmount(input));
        if (needed <= 0 || input.getCount() < needed) {
            return false;
        }

        ChemicalStack output = recipe.getOutput(singleInput);
        if (output.isEmpty() || !matchesChemicalOutputs(patternDetails, output, output.getAmount() * needed)) {
            return false;
        }

        ItemStack stack = input.copyWithCount(needed);
        if (!owner.insertItem(MeMekanismMachineBlockEntity.SECONDARY_INPUT_SLOT, stack.copy(), Action.SIMULATE).isEmpty()) {
            return false;
        }
        owner.insertItem(MeMekanismMachineBlockEntity.SECONDARY_INPUT_SLOT, stack, Action.EXECUTE);
        owner.setChanged();
        return true;
    }

    private static boolean tryInsertItemChemicalPatternInputs(MeMekanismMachineBlockEntity owner, KeyCounter[] inputHolder) {
        if (owner.getMachine().slotLayout() != MeMekanismMachine.SlotLayout.ITEM_CHEMICAL || inputHolder == null || inputHolder.length != 2 || owner.getChemicalTank() == null) {
            return false;
        }

        ItemStack itemInput = ItemStack.EMPTY;
        ChemicalStack chemicalInput = ChemicalStack.EMPTY;
        for (KeyCounter counter : inputHolder) {
            PatternInput input = getSinglePatternInput(counter);
            if (input == null) {
                return false;
            }
            if (!input.item().isEmpty()) {
                if (!itemInput.isEmpty()) {
                    return false;
                }
                itemInput = input.item();
            } else if (!input.chemical().isEmpty()) {
                if (!chemicalInput.isEmpty()) {
                    return false;
                }
                chemicalInput = input.chemical();
            }
        }

        if (itemInput.isEmpty() || chemicalInput.isEmpty() || !owner.canAddChemical(chemicalInput)) {
            return false;
        }

        if (!owner.insertItem(MeMekanismMachineBlockEntity.INPUT_SLOT, itemInput.copy(), Action.SIMULATE).isEmpty()
                || !owner.getChemicalTank().insert(chemicalInput.copy(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
            return false;
        }
        owner.insertItem(MeMekanismMachineBlockEntity.INPUT_SLOT, itemInput, Action.EXECUTE);
        owner.getChemicalTank().insert(chemicalInput, Action.EXECUTE, AutomationType.INTERNAL);
        owner.setChanged();
        return true;
    }

    @Nullable
    private static PatternInput getSinglePatternInput(KeyCounter counter) {
        if (counter == null || counter.isEmpty()) {
            return null;
        }

        PatternInput input = null;
        for (var entry : counter) {
            AEKey key = entry.getKey();
            long amount = entry.getLongValue();
            PatternInput next;
            if (key instanceof AEItemKey itemKey && amount > 0 && amount <= Integer.MAX_VALUE) {
                next = new PatternInput(itemKey.toStack((int) amount), ChemicalStack.EMPTY);
            } else if (key instanceof MekanismKey chemicalKey && amount > 0) {
                next = new PatternInput(ItemStack.EMPTY, chemicalKey.getStack().copyWithAmount(amount));
            } else {
                return null;
            }
            if (input != null) {
                return null;
            }
            input = next;
        }
        return input;
    }

    private static boolean matchesChemicalOutputs(IPatternDetails patternDetails, ChemicalStack expectedOutput, long expectedAmount) {
        boolean matched = false;
        for (var output : patternDetails.getOutputs()) {
            if (output.what() instanceof MekanismKey chemicalKey) {
                ChemicalStack chemicalOutput = chemicalKey.getStack();
                if (matched || !chemicalOutput.is(expectedOutput.getChemicalHolder()) || output.amount() != expectedAmount) {
                    return false;
                }
                matched = true;
            } else {
                return false;
            }
        }
        return matched;
    }

    private static boolean tryInsertPatternInputs(MeMekanismMachineBlockEntity owner, KeyCounter[] inputHolder) {
        int[] slots = getPatternInputSlots(owner, inputHolder);
        if (slots == null) {
            return false;
        }

        ItemStack[] simulated = new ItemStack[MeMekanismMachineBlockEntity.totalSlots()];
        for (int i = 0; i < MeMekanismMachineBlockEntity.totalSlots(); i++) {
            simulated[i] = owner.getStack(i).copy();
        }

        for (int i = 0; i < inputHolder.length; i++) {
            KeyCounter counter = inputHolder[i];
            if (counter == null || counter.isEmpty()) {
                return false;
            }

            int slot = slots[i];
            for (var entry : counter) {
                AEKey key = entry.getKey();
                long amount = entry.getLongValue();
                if (!(key instanceof AEItemKey itemKey) || amount <= 0 || amount > Integer.MAX_VALUE) {
                    return false;
                }

                ItemStack stack = itemKey.toStack((int) amount);
                if (!insertIntoSimulatedSlot(owner, simulated, slot, stack)) {
                    return false;
                }
            }
        }

        for (int slot : slots) {
            owner.setStack(slot, simulated[slot]);
        }
        owner.setChanged();
        return true;
    }

    @Nullable
    private static int[] getPatternInputSlots(MeMekanismMachineBlockEntity owner, KeyCounter[] inputHolder) {
        if (inputHolder == null) {
            return null;
        }

        return switch (owner.getMachine().slotLayout()) {
            case SINGLE_ITEM, SAWING -> inputHolder.length == 1 ? new int[] { MeMekanismMachineBlockEntity.INPUT_SLOT } : null;
            case DOUBLE_ITEM -> inputHolder.length == 2 ? new int[] { MeMekanismMachineBlockEntity.INPUT_SLOT, MeMekanismMachineBlockEntity.SECONDARY_INPUT_SLOT } : null;
            case ITEM_CHEMICAL -> inputHolder.length == 2 ? new int[] { MeMekanismMachineBlockEntity.INPUT_SLOT, MeMekanismMachineBlockEntity.SECONDARY_INPUT_SLOT } : null;
        };
    }

    private static boolean insertIntoSimulatedSlot(MeMekanismMachineBlockEntity owner, ItemStack[] simulated, int slot, ItemStack stack) {
        if (stack.isEmpty() || slot < 0 || slot >= simulated.length) {
            return false;
        }

        ItemStack existing = simulated[slot];
        int limit = Math.min(stack.getMaxStackSize(), owner.getSlotLimit(slot, stack));
        if (existing.isEmpty()) {
            if (stack.getCount() > limit) {
                return false;
            }
            simulated[slot] = stack.copy();
            return true;
        }

        if (!ItemStack.isSameItemSameComponents(existing, stack)) {
            return false;
        }
        int existingLimit = Math.min(existing.getMaxStackSize(), owner.getSlotLimit(slot, existing));
        if (existing.getCount() + stack.getCount() > existingLimit) {
            return false;
        }
        existing.grow(stack.getCount());
        simulated[slot] = existing;
        return true;
    }

    private record PatternInput(ItemStack item, ChemicalStack chemical) {
    }
}
