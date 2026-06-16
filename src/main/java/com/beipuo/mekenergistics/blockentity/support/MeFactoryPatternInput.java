package com.beipuo.mekenergistics.blockentity.support;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.KeyCounter;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public record MeFactoryPatternInput(ItemStack item, ChemicalStack chemical, FluidStack fluid) {
    static final MeFactoryPatternInput EMPTY = new MeFactoryPatternInput(ItemStack.EMPTY, ChemicalStack.EMPTY, FluidStack.EMPTY);

    public boolean isItem() {
        return !this.item.isEmpty() && this.chemical.isEmpty() && this.fluid.isEmpty();
    }

    public boolean isChemical() {
        return this.item.isEmpty() && !this.chemical.isEmpty() && this.fluid.isEmpty();
    }

    public boolean isFluid() {
        return this.item.isEmpty() && this.chemical.isEmpty() && !this.fluid.isEmpty();
    }

    @Nullable
    public static MeFactoryPatternInput single(KeyCounter counter) {
        if (counter == null || counter.isEmpty()) {
            return null;
        }
        MeFactoryPatternInput input = null;
        for (var entry : counter) {
            AEKey key = entry.getKey();
            long amount = entry.getLongValue();
            MeFactoryPatternInput next;
            if (key instanceof AEItemKey itemKey && amount > 0 && amount <= Integer.MAX_VALUE) {
                next = new MeFactoryPatternInput(itemKey.toStack((int) amount), ChemicalStack.EMPTY, FluidStack.EMPTY);
            } else if (key instanceof MekanismKey chemicalKey && amount > 0) {
                next = new MeFactoryPatternInput(ItemStack.EMPTY, chemicalKey.getStack().copyWithAmount(amount), FluidStack.EMPTY);
            } else if (key instanceof AEFluidKey fluidKey && amount > 0 && amount <= Integer.MAX_VALUE) {
                next = new MeFactoryPatternInput(ItemStack.EMPTY, ChemicalStack.EMPTY, fluidKey.toStack((int) amount));
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

    public static ItemStack singleItem(KeyCounter counter) {
        MeFactoryPatternInput input = single(counter);
        return input != null && input.isItem() ? input.item() : ItemStack.EMPTY;
    }

    @Nullable
    public static MeFactoryPatternInput separate(KeyCounter[] counters) {
        if (counters == null || counters.length == 0) {
            return null;
        }
        ItemStack item = ItemStack.EMPTY;
        ChemicalStack chemical = ChemicalStack.EMPTY;
        FluidStack fluid = FluidStack.EMPTY;
        for (KeyCounter counter : counters) {
            MeFactoryPatternInput input = single(counter);
            if (input == null) {
                return null;
            }
            if (input.isItem()) {
                if (!item.isEmpty()) {
                    return null;
                }
                item = input.item();
            } else if (input.isChemical()) {
                if (!chemical.isEmpty()) {
                    return null;
                }
                chemical = input.chemical();
            } else if (input.isFluid()) {
                if (!fluid.isEmpty()) {
                    return null;
                }
                fluid = input.fluid();
            } else {
                return null;
            }
        }
        return new MeFactoryPatternInput(item, chemical, fluid);
    }
}
