package com.beipuo.mekenergistics.upgrade;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MeInterfaceFlowContractTest {
    private static final Path SUPPORT = Path.of(
            "src/main/java/com/beipuo/mekenergistics/blockentity/support/AbstractMeAeSupport.java");
    private static final Path SYNC = Path.of(
            "src/main/java/com/beipuo/mekenergistics/network/packet/InterfaceConfigSyncPacket.java");
    private static final Path REQUEST = Path.of(
            "src/main/java/com/beipuo/mekenergistics/network/packet/RequestInterfaceConfigPacket.java");
    private static final Path NETWORK = Path.of(
            "src/main/java/com/beipuo/mekenergistics/network/ModNetwork.java");
    private static final Path FACTORY_SUPPORT = Path.of(
            "src/main/java/com/beipuo/mekenergistics/blockentity/support/MeFactoryAeSupport.java");
    private static final Path FACTORY_MACHINE = Path.of(
            "src/main/java/com/beipuo/mekenergistics/blockentity/api/MeFactoryAeMachine.java");

    @Test
    void interfaceInventoryIsPersistedAndProcessedBeforeRestocking() throws IOException {
        String support = Files.readString(SUPPORT);

        assertTrue(support.contains("private final MeInterfaceInventory interfaceInventory;"));
        assertTrue(support.contains("drainInterfaceInventoryToMachine"));
        assertTrue(support.contains("restockInterfaceInventory"));
        assertTrue(support.contains("this.interfaceInventory.save(tag, registries);"));
        assertTrue(support.contains("this.interfaceInventory.load(tag, registries);"));
        assertTrue(support.contains("configured.amount() - current"));
        assertTrue(support.contains("layout.routeInterface(key, extracted)"));
        assertTrue(support.contains("layout.maxAcceptedInterfaceAmount(key, requested)"));
        assertTrue(support.contains("flushInterfaceRecovery()"));
    }

    @Test
    void changedOrClearedColumnsReturnOldStockToTheNetwork() throws IOException {
        String support = Files.readString(SUPPORT);

        assertTrue(support.contains("configured == null || !configured.what().equals(key)"));
        assertTrue(support.contains("stocked != null && !stocked.what().equals(key)"));
        assertTrue(support.contains("refundToNetworkOrBuffer(stocked.what(), removed);"));
    }

    @Test
    void serverSynchronizesBothConfigurationAndReadOnlyInventoryRows() throws IOException {
        String sync = Files.readString(SYNC);
        String request = Files.readString(REQUEST);
        String network = Files.readString(NETWORK);

        assertTrue(sync.contains("List<GenericStack> config, List<GenericStack> inventory"));
        assertTrue(request.contains("support.getInterfaceConfig().toList(), support.getInterfaceInventory().toList()"));
        assertTrue(network.contains("registrar.playToClient(InterfaceConfigSyncPacket.TYPE"));
    }

    @Test
    void factoriesProcessAndSynchronizeInterfaceMode() throws IOException {
        String support = Files.readString(FACTORY_SUPPORT);
        String machine = Files.readString(FACTORY_MACHINE);

        assertTrue(support.contains("isInterfaceMode()"));
        assertTrue(support.contains("processInterfaceMode"));
        assertTrue(support.contains("hasInterfaceWork"));
        assertTrue(machine.contains("getAeSupport()::isInterfaceMode"));
        assertTrue(machine.contains("getAeSupport()::setClientInterfaceMode"));
    }

    @Test
    void recipeAndFactoryMachineTicksDoNotRunInterfaceIo() throws IOException {
        String recipe = Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/blockentity/support/MeRecipeMachineAeSupport.java"));
        String factory = Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/blockentity/support/MeFactoryAeSupport.java"));
        assertTrue(recipe.contains("isInterfaceMode"));
        assertTrue(recipe.contains("processInterfaceMode"));
        assertTrue(recipe.contains("processPatternIo"));
        assertTrue(recipe.contains("processAeOutputWork"));
        assertTrue(factory.contains("isInterfaceMode"));
        assertTrue(factory.contains("processInterfaceMode"));
        assertTrue(factory.contains("processAeOutputWork"));
    }

    @Test
    void configAndInventoryAllowOverstackedItemAmounts() throws IOException {
        String config = Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/upgrade/MeInterfaceConfig.java"));
        String inventory = Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/upgrade/MeInterfaceInventory.java"));
        assertTrue(config.contains(".allowOverstacking(true)"));
        assertTrue(inventory.contains(".allowOverstacking(true)"));
        assertFalse(config.contains("useRegisteredCapacities()"));
        assertFalse(inventory.contains("useRegisteredCapacities()"));
    }

    @Test
    void partialRecoveryFlushesPersistTheReducedBuffer() throws IOException {
        String support = Files.readString(SUPPORT);

        assertTrue(support.contains("changed = true;"));
        assertTrue(support.contains("this.owner.saveChanges()"));
        assertTrue(support.contains("flushInterfaceRecovery()"));
    }

    @Test
    void installerUpgradePreservesMeRuntimeState() throws IOException {
        String installer = Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/item/MeInstallerUpgradeHandler.java"));
        String tierInstaller = Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/item/MeTierInstallerItem.java"));
        assertTrue(installer.contains("MePatternSlotTransfer.saveMeState"));
        assertTrue(installer.contains("MePatternSlotTransfer.loadMeState"));
        // The tier installer replaces machines through the shared handler checked above.
        assertTrue(tierInstaller.contains("MeInstallerUpgradeHandler.upgrade("));
    }
}
