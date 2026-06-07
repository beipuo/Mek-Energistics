package com.beipuo.mekenergistics.compat.eme;

import fr.iglee42.evolvedmekanism.recipes.AlloyerRecipe;
import fr.iglee42.evolvedmekanism.registries.EMBlocks;
import fr.iglee42.evolvedmekanism.registries.EMRecipeType;
import fr.iglee42.evolvedmekanism.recipes.vanilla_input.TriItemRecipeInput;
import fr.iglee42.evolvedmekanism.interfaces.EMInputRecipeCache;
import mekanism.client.recipe_viewer.type.RVRecipeTypeWrapper;

public final class EvolvedMekanismRecipeViewerTypes {
    public static final RVRecipeTypeWrapper<TriItemRecipeInput, AlloyerRecipe, EMInputRecipeCache.TripleItem<AlloyerRecipe>> ALLOYING =
            new RVRecipeTypeWrapper<>(EMRecipeType.ALLOYING, AlloyerRecipe.class, -28, -16, 144, 54, EMBlocks.ALLOYER);

    private EvolvedMekanismRecipeViewerTypes() {
    }
}
