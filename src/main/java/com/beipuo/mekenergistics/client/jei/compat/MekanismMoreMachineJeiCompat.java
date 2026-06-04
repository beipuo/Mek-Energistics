package com.beipuo.mekenergistics.client.jei.compat;

import com.jerry.mekmm.client.recipe_viewer.MMRecipeViewerRecipeType;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;

public final class MekanismMoreMachineJeiCompat {
    private MekanismMoreMachineJeiCompat() {
    }

    public static void registerCatalysts(IRecipeCatalystRegistration registration, CatalystRegistrar registrar) {
        registrar.register(registration, MMRecipeViewerRecipeType.RECYCLER, "recycling");
        registrar.register(registration, MMRecipeViewerRecipeType.PLANTING_STATION, "planting");
        registrar.register(registration, MMRecipeViewerRecipeType.STAMPING, "stamping");
        registrar.register(registration, MMRecipeViewerRecipeType.LATHE, "lathing");
        registrar.register(registration, MMRecipeViewerRecipeType.ROLLING_MILL, "rolling_mill");
        registrar.register(registration, MMRecipeViewerRecipeType.REPLICATOR, "replicating");
    }

    @FunctionalInterface
    public interface CatalystRegistrar {
        void register(IRecipeCatalystRegistration registration, IRecipeViewerRecipeType<?> recipeType, String factoryTypeName);
    }
}
