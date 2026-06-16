package com.beipuo.mekenergistics.client.jei.compat;

import com.beipuo.mekenergistics.client.jei.MekEnergisticsJeiPlugin;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.compat.eme.EvolvedMekanismRecipeViewerTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;

public final class EvolvedMekanismJeiCompat {
    private EvolvedMekanismJeiCompat() {
    }

    public static void registerCatalysts(IRecipeCatalystRegistration registration) {
        MekEnergisticsJeiPlugin.registerMachines(registration, EvolvedMekanismRecipeViewerTypes.ALLOYING, MeMekanismMachine.ALLOYER);
        MekEnergisticsJeiPlugin.registerMachines(registration, EvolvedMekanismRecipeViewerTypes.SOLIDIFICATION, MeMekanismMachine.SOLIDIFICATION_CHAMBER);
        MekEnergisticsJeiPlugin.registerMachines(registration, EvolvedMekanismRecipeViewerTypes.MELTING, MeMekanismMachine.THERMALIZER);
        MekEnergisticsJeiPlugin.registerMachines(registration, EvolvedMekanismRecipeViewerTypes.CHEMIXING, MeMekanismMachine.CHEMIXER);
    }
}
