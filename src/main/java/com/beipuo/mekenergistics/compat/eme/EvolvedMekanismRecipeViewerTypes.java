package com.beipuo.mekenergistics.compat.eme;

import fr.iglee42.evolvedmekanism.recipes.AlloyerRecipe;
import fr.iglee42.evolvedmekanism.recipes.ChemixerRecipe;
import fr.iglee42.evolvedmekanism.recipes.SolidificationRecipe;
import fr.iglee42.evolvedmekanism.registries.EMBlocks;
import fr.iglee42.evolvedmekanism.registries.EMRecipeType;
import fr.iglee42.evolvedmekanism.recipes.vanilla_input.BiItemChemicalRecipeInput;
import fr.iglee42.evolvedmekanism.recipes.vanilla_input.SingleItemBiFluidRecipeInput;
import fr.iglee42.evolvedmekanism.recipes.vanilla_input.TriItemRecipeInput;
import fr.iglee42.evolvedmekanism.interfaces.EMInputRecipeCache;
import mekanism.api.recipes.ItemStackToFluidRecipe;
import mekanism.client.recipe_viewer.type.RVRecipeTypeWrapper;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.SingleItem;
import net.minecraft.world.item.crafting.SingleRecipeInput;

public final class EvolvedMekanismRecipeViewerTypes {
    public static final RVRecipeTypeWrapper<TriItemRecipeInput, AlloyerRecipe, EMInputRecipeCache.TripleItem<AlloyerRecipe>> ALLOYING =
            new RVRecipeTypeWrapper<>(EMRecipeType.ALLOYING, AlloyerRecipe.class, -28, -16, 144, 54, EMBlocks.ALLOYER);
    public static final RVRecipeTypeWrapper<SingleItemBiFluidRecipeInput, SolidificationRecipe, EMInputRecipeCache.ItemFluidFluid<SolidificationRecipe>> SOLIDIFICATION =
            new RVRecipeTypeWrapper<>(EMRecipeType.SOLIDIFICATION, SolidificationRecipe.class, -28, -16, 144, 54, EMBlocks.SOLIDIFIER);
    public static final RVRecipeTypeWrapper<SingleRecipeInput, ItemStackToFluidRecipe, SingleItem<ItemStackToFluidRecipe>> MELTING =
            new RVRecipeTypeWrapper<>(EMRecipeType.MELTING, ItemStackToFluidRecipe.class, -28, -16, 144, 54, EMBlocks.MELTER);
    public static final RVRecipeTypeWrapper<BiItemChemicalRecipeInput, ChemixerRecipe, EMInputRecipeCache.ItemItemChemical<ChemixerRecipe>> CHEMIXING =
            new RVRecipeTypeWrapper<>(EMRecipeType.CHEMIXING, ChemixerRecipe.class, -28, -16, 144, 54, EMBlocks.CHEMIXER);

    private EvolvedMekanismRecipeViewerTypes() {
    }
}
