package com.beipuo.mekenergistics.blockentity.support;

import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;

public final class MeChemicalInputHelper {
    private MeChemicalInputHelper() {
    }

    public static boolean insertPair(IChemicalTank leftTank, IChemicalTank rightTank, ChemicalStack leftInput, ChemicalStack rightInput) {
        if (!canAdd(leftTank, leftInput) || !canAdd(rightTank, rightInput)) {
            return false;
        }
        ChemicalStack oldLeft = leftTank.getStack();
        ChemicalStack oldRight = rightTank.getStack();
        ChemicalStack newLeft = merged(oldLeft, leftInput);
        ChemicalStack newRight = merged(oldRight, rightInput);
        if (newLeft.isEmpty() || newRight.isEmpty()) {
            return false;
        }
        leftTank.setStack(newLeft);
        rightTank.setStack(newRight);
        boolean valid = leftTank.isValid(newLeft) && rightTank.isValid(newRight);
        if (!valid) {
            leftTank.setStack(oldLeft);
            rightTank.setStack(oldRight);
            return false;
        }
        return true;
    }

    private static boolean canAdd(IChemicalTank tank, ChemicalStack input) {
        if (tank == null || input.isEmpty() || input.getAmount() > tank.getNeeded()) {
            return false;
        }
        ChemicalStack existing = tank.getStack();
        return existing.isEmpty() || existing.is(input.getChemicalHolder());
    }

    private static ChemicalStack merged(ChemicalStack existing, ChemicalStack input) {
        if (existing.isEmpty()) {
            return input.copy();
        }
        ChemicalStack merged = existing.copy();
        merged.grow(input.getAmount());
        return merged;
    }
}
