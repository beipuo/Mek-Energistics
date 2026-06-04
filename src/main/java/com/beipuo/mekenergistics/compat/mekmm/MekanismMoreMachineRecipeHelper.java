package com.beipuo.mekenergistics.compat.mekmm;

import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.jerry.mekmm.api.recipes.RecyclerRecipe;
import com.jerry.mekmm.api.recipes.StamperRecipe;
import com.jerry.mekmm.common.recipe.MoreMachineRecipeType;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class MekanismMoreMachineRecipeHelper {
    private MekanismMoreMachineRecipeHelper() {
    }

    @Nullable
    public static ItemResult findRecycler(Level level, ItemStack input, boolean maxChanceOutput) {
        RecyclerRecipe recipe = MoreMachineRecipeType.RECYCLING.getInputCache().findFirstRecipe(level, input);
        if (recipe == null) {
            return null;
        }
        int needed = clampNeeded(recipe.getInput().getNeededAmount(input));
        ItemStack output = maxChanceOutput ? recipe.getOutput(input).getMaxChanceOutput() : recipe.getOutput(input).getChanceOutput();
        return new ItemResult(needed, output);
    }

    @Nullable
    public static ItemStackToItemStackRecipe findSingleItemRecipe(Level level, MeMekanismMachine machine, ItemStack input) {
        if (machine == MeMekanismMachine.CNC_LATHE) {
            return MoreMachineRecipeType.LATHING.getInputCache().findFirstRecipe(level, input);
        }
        if (machine == MeMekanismMachine.CNC_ROLLING_MILL) {
            return MoreMachineRecipeType.ROLLING_MILL.getInputCache().findFirstRecipe(level, input);
        }
        return null;
    }

    @Nullable
    public static StamperResult findStamper(Level level, ItemStack input, ItemStack mold) {
        StamperRecipe recipe = MoreMachineRecipeType.STAMPING.getInputCache().findFirstRecipe(level, input, mold);
        if (recipe == null) {
            return null;
        }
        return new StamperResult(
                clampNeeded(recipe.getInput().getNeededAmount(input)),
                clampNeeded(recipe.getMold().getNeededAmount(mold)),
                recipe.getOutput(input, mold)
        );
    }

    private static int clampNeeded(long needed) {
        return needed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) needed;
    }

    public record ItemResult(int needed, ItemStack output) {
    }

    public record StamperResult(int neededInput, int neededMold, ItemStack output) {
    }
}
