package com.beipuo.mekenergistics.blockentity.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MeUpgradeableMachineContractTest {
    private static final Path ELECTRIC_MIXIN = Path.of(
            "src/main/java/com/beipuo/mekenergistics/mixin/TileEntityElectricMachineMeUpgradeMixin.java");
    private static final Path MACHINE_PROFILE = Path.of(
            "src/main/java/com/beipuo/mekenergistics/upgrade/MeUpgradeMachineProfiles.java");
    private static final Path CAPABILITIES = Path.of(
            "src/main/java/com/beipuo/mekenergistics/registry/ModBlockEntities.java");
    private static final Path PACKET_TARGET = Path.of(
            "src/main/java/com/beipuo/mekenergistics/network/packet/ServerPacketTarget.java");
    private static final Path UPGRADE_CONTRACT = Path.of(
            "src/main/java/com/beipuo/mekenergistics/blockentity/api/MeUpgradeableMachine.java");

    @Test
    void attachmentTargetsOnlyTheVanillaEnrichmentChamber() throws IOException {
        String source = Files.readString(MACHINE_PROFILE);

        assertTrue(source.contains("getBlockState().is(MekanismBlocks.ENRICHMENT_CHAMBER.value())"));
        assertFalse(source.contains("MekanismBlocks.CRUSHER"));
        assertFalse(source.contains("MekanismBlocks.ENERGIZED_SMELTER"));
    }

    @Test
    void attachmentRoutesPatternsThroughTheVanillaInputAndOutputSlots() throws IOException {
        String source = Files.readString(MACHINE_PROFILE);

        assertTrue(source.contains("MeMachineIoAdapter.itemInput("));
        assertTrue(source.contains("mekenergistics$getInputSlot()"));
        assertTrue(source.contains("MeMachineIoAdapter.itemOutput("));
        assertTrue(source.contains("mekenergistics$getOutputSlot()"));
    }

    @Test
    void inactiveAttachmentsExposeNeitherCapabilityNorPacketTarget() throws IOException {
        String capabilities = Files.readString(CAPABILITIES);
        String packets = Files.readString(PACKET_TARGET);

        assertTrue(capabilities.contains("machine.isMeUpgradeActive() ? machine : null"));
        assertTrue(packets.contains("instanceof MeMekanismMachineBlock"));
        assertTrue(packets.contains("!machine.isMeUpgradeTarget() || !machine.isMeUpgradeActive()"));
        assertTrue(packets.contains("return Optional.empty();"));
    }

    @Test
    void largeMachineBoundingBlockPreservesRegisteredMeHostWhileRejectingUnsupportedUpgradeHost() throws IOException {
        String capabilities = Files.readString(CAPABILITIES).replaceAll("\\s+", " ");

        assertTrue(capabilities.contains(
                "bounding.getMainTile(pos) instanceof MeAeMachine machine "
                        + "&& (!(machine instanceof MeUpgradeableMachine upgradeable) "
                        + "|| isMachineBlockEntity(machine.getAeOwnerTile().getType()) "
                        + "|| upgradeable.isMeUpgradeTarget()) "
                        + "&& machine.getMachine().isMekmmLargeMachine()"),
                "MEKLM bounding-block capability must preserve registered ME hosts while rejecting unsupported "
                        + "upgrade hosts before getMachine()");
    }

    @Test
    void chemicalRecipeEnergyLookupDoesNotAssumeRotaryCondensentrator() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/mixin/MekanismChemicalRecipeMachineMeUpgradeMixin.java"));

        assertTrue(source.contains("getEnergyContainers(null)"));
        assertTrue(source.contains("instanceof MachineEnergyContainer<?> machineEnergyContainer"));
        assertFalse(source.contains("return ((TileEntityRotaryCondensentrator) tile).getEnergyContainer()"));
    }

    @Test
    void mainContainerSynchronizesInstalledUpgradeStateToTheClient() throws IOException {
        String source = Files.readString(ELECTRIC_MIXIN);

        assertTrue(source.contains("SyncableBoolean.create("));
        assertTrue(source.contains("MeUpgradeRuntimeState"));
        assertTrue(source.contains("acceptClientActive(active)"));
        assertTrue(source.contains("this::mekenergistics$isMeUpgradeInstalled"));
        assertTrue(source.contains("getMeUpgradeContainer().isInstalled(MeUpgradeType.PATTERN_PROVIDER)"));
    }

    @Test
    void upgradedVanillaRecipeUsesNetworkEnergyAndRefreshesOnTransition() throws IOException {
        String source = Files.readString(ELECTRIC_MIXIN);

        assertTrue(source.contains("createNewCachedRecipe(Lmekanism/api/recipes/ItemStackToItemStackRecipe;I)"));
        assertTrue(source.contains("isMeUpgradeTarget()"));
        assertTrue(source.contains("wrapRecipeEnergy("));
        assertTrue(source.contains("tile.getEnergyContainer()"));
        assertTrue(source.contains("setRecipeCacheListener(recipeCacheListener)"));
        assertTrue(source.contains("refreshRecipeCache()"));
        assertTrue(Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/upgrade/MeUpgradeRuntimeState.java"))
                .contains("recipeCacheListener.onContentsChanged()"));
    }

    @Test
    void machineSpecificSurfaceIsDelegatedToAReusableProfile() throws IOException {
        String mixin = Files.readString(ELECTRIC_MIXIN);
        String contract = Files.readString(UPGRADE_CONTRACT);
        String profile = Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/upgrade/MeUpgradeMachineProfile.java"));

        assertTrue(mixin.contains("MeUpgradeMachineProfiles.forTile"));
        assertTrue(mixin.contains("getMeUpgradeProfile()"));
        assertTrue(mixin.contains("mekenergistics$profile().inputLayoutFor"));
        assertTrue(mixin.contains("mekenergistics$profile().outputPortsFor"));
        assertTrue(contract.contains("MeUpgradeMachineProfile<?> getMeUpgradeProfile()"));
        assertTrue(profile.contains("Predicate<TILE> target"));
        assertTrue(profile.contains("Function<TILE, MeInputLayout> inputLayout"));
        assertTrue(profile.contains("Function<TILE, List<? extends MeOutputPort>> outputPorts"));
    }

    @Test
    void unsupportedMixinSubtypeUsesItsOwnBlockIdentityInPatternTerminals() throws IOException {
        String contract = Files.readString(UPGRADE_CONTRACT);

        assertTrue(contract.contains("MeUpgradeMachineProfile<?> profile = getMeUpgradeProfile();"));
        assertTrue(contract.contains("profile == null"));
        assertTrue(contract.contains("new ItemStack(meUpgradeTile().getBlockState().getBlock())"));
        assertTrue(contract.contains("meUpgradeTile().getBlockState().getBlock().getName()"));
        assertTrue(contract.contains("profileTerminalIcon(profile)"));
        assertTrue(contract.contains("profileTerminalName(profile)"));
        assertFalse(contract.contains("MeAeMachine.super.getTerminalIconStack()"));
        assertFalse(contract.contains("MeAeMachine.super.getPatternTerminalDisplayName()"));
    }
}
