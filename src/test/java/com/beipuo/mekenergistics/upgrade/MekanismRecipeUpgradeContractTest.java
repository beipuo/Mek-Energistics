package com.beipuo.mekenergistics.upgrade;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MekanismRecipeUpgradeContractTest {
    private static final Path PROFILES = Path.of(
            "src/main/java/com/beipuo/mekenergistics/upgrade/MekanismRecipeUpgradeProfiles.java");
    private static final Path MIXIN = Path.of(
            "src/main/java/com/beipuo/mekenergistics/mixin/TileEntityRecipeMachineMeUpgradeMixin.java");

    @Test
    void declaresAllFourMekanismRecipeMachineSurfaces() throws IOException {
        String source = Files.readString(PROFILES);
        assertTrue(source.contains("TileEntityAdvancedElectricMachine advanced"));
        assertTrue(source.contains("TileEntityCombiner combiner"));
        assertTrue(source.contains("TileEntityPrecisionSawmill sawmill"));
        assertTrue(source.contains("TileEntityMetallurgicInfuser infuser"));
    }

    @Test
    void routesAdvancedItemChemicalAndContainerInputs() throws IOException {
        String source = Files.readString(PROFILES);
        assertTrue(source.contains("mekenergistics$getInputSlot()"));
        assertTrue(source.contains("chemicalInput(candidate.chemicalTank)"));
        assertTrue(source.contains("mekenergistics$getSecondarySlot()"));
    }

    @Test
    void routesCombinerAndSawmillDistinctSlots() throws IOException {
        String source = Files.readString(PROFILES);
        assertTrue(source.contains("mekenergistics$getMainInputSlot()"));
        assertTrue(source.contains("mekenergistics$getExtraInputSlot()"));
        assertTrue(source.contains("mekenergistics$getSecondaryOutputSlot()"));
    }

    @Test
    void preservesInfuserDualItemChemicalMode() throws IOException {
        String source = Files.readString(PROFILES);
        assertTrue(source.contains("MeInfusionModePolicy.isConversionMode("));
        assertTrue(source.contains("mekenergistics$getInfusionSlot()"));
        assertTrue(source.contains("chemicalInput(candidate.infusionTank)"));
        assertTrue(source.contains("chemicalOutput(candidate.infusionTank)"));
    }

    @Test
    void wrapsEverySupportedRecipeFamilyBeforeInstallation() throws IOException {
        String source = Files.readString(MIXIN);
        assertTrue(source.contains("ItemStackChemicalToItemStackRecipe;I"));
        assertTrue(source.contains("CombinerRecipe;I"));
        assertTrue(source.contains("SawmillRecipe;I"));
        assertTrue(source.contains("isMeUpgradeTarget()"));
        assertTrue(source.contains("wrapRecipeEnergy("));
    }

    @Test
    void transitionsRefreshRecipeCacheAndNodeCapability() throws IOException {
        String source = Files.readString(MIXIN);
        assertTrue(source.contains("transitionTo(active)"));
        assertTrue(source.contains("refreshRecipeCache()"));
        assertTrue(source.contains("invalidateCapabilities"));
        assertTrue(source.contains("destroyNode()"));
    }
}
