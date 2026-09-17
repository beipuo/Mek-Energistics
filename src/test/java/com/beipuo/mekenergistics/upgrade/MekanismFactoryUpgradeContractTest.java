package com.beipuo.mekenergistics.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.compat.catalog.CompatMachineCatalog;
import com.beipuo.mekenergistics.compat.catalog.CompatMachineFamily;
import com.beipuo.mekenergistics.compat.catalog.CompatMachineKind;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mekanism.common.content.blocktype.FactoryType;
import mekanism.common.tier.FactoryTier;
import org.junit.jupiter.api.Test;

class MekanismFactoryUpgradeContractTest {
    private record FactoryMixinCoverage(String runtimeMixin, String energyMixin) {
    }

    private static final Map<CompatMachineFamily, FactoryMixinCoverage> FACTORY_MIXINS = factoryMixins();

    private static final Path FACTORY_MIXIN = Path.of(
            "src/main/java/com/beipuo/mekenergistics/mixin/TileEntityFactoryMeUpgradeMixin.java");
    private static final Path ITEM_ENERGY_MIXIN = Path.of(
            "src/main/java/com/beipuo/mekenergistics/mixin/TileEntityItemFactoryMeUpgradeEnergyMixin.java");
    private static final Path CHEMICAL_ENERGY_MIXIN = Path.of(
            "src/main/java/com/beipuo/mekenergistics/mixin/TileEntityChemicalFactoryMeUpgradeEnergyMixin.java");
    private static final Path FACTORY_AE_MACHINE = Path.of(
            "src/main/java/com/beipuo/mekenergistics/blockentity/api/MeFactoryAeMachine.java");

    @Test
    void everyVanillaTierAndFactoryTypeHasAnUpgradeDescriptor() {
        int covered = 0;
        for (FactoryTier tier : FactoryTier.values()) {
            for (FactoryType type : FactoryType.values()) {
                MeMekanismMachine machine = MeMekanismMachine.getFactory(tier, type);
                assertNotNull(machine, () -> tier + " " + type);
                assertEquals(CompatMachineFamily.MEKANISM_FACTORY, machine.family());
                assertEquals(tier, machine.factoryTier());
                assertEquals(type, machine.factoryType());
                covered++;
            }
        }
        assertEquals(FactoryTier.values().length * FactoryType.values().length, covered);
    }

    @Test
    void everyCatalogFactoryFamilyHasRegisteredRuntimeAndEnergyMixins() throws IOException {
        var factories = CompatMachineCatalog.all()
                .filter(spec -> spec.kind() != CompatMachineKind.MACHINE)
                .toList();
        assertEquals(354, factories.size(), "factory coverage changed; update the runtime matrix deliberately");

        Set<CompatMachineFamily> catalogFamilies = new HashSet<>();
        factories.forEach(spec -> catalogFamilies.add(spec.family()));
        assertEquals(FACTORY_MIXINS.keySet(), catalogFamilies,
                "every catalog factory family needs an explicit runtime route");

        JsonArray configured = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/mekenergistics.mixins.json")))
                .getAsJsonObject().getAsJsonArray("mixins");
        Set<String> configuredMixins = new HashSet<>();
        configured.forEach(entry -> configuredMixins.add(entry.getAsString()));

        for (var entry : FACTORY_MIXINS.entrySet()) {
            CompatMachineFamily family = entry.getKey();
            FactoryMixinCoverage coverage = entry.getValue();
            assertTrue(factories.stream().anyMatch(spec -> spec.family() == family), family.name());
            assertTrue(configuredMixins.contains(coverage.runtimeMixin()),
                    () -> family + " runtime mixin is not registered");
            assertTrue(configuredMixins.contains(coverage.energyMixin()),
                    () -> family + " energy mixin is not registered");

            String runtime = Files.readString(mixinSource(coverage.runtimeMixin()));
            assertTrue(runtime.contains("getOrCreateMeUpgradeRuntime()"),
                    () -> family + " does not attach the shared upgrade runtime");
            assertTrue(runtime.contains("addMePatternSlots"),
                    () -> family + " does not expose pattern slots");
            assertTrue(runtime.contains("processMePatternIo"),
                    () -> family + " does not process pattern I/O");

            String energy = Files.readString(mixinSource(coverage.energyMixin()));
            assertTrue(energy.contains("wrapRecipeEnergy"),
                    () -> family + " does not wrap recipe energy with AE power");
        }
    }

    @Test
    void advancedFactoryFamiliesRegisterAllSharedIoAdapters() throws IOException {
        Set<String> expected = Set.of(
                "AdvancedFactoryChemicalToChemicalPortMixin",
                "AdvancedFactoryChemicalToItemPortMixin",
                "AdvancedFactoryItemToChemicalPortMixin",
                "AdvancedFactoryItemToItemPortMixin",
                "AdvancedFactoryDissolvingPortMixin",
                "MekExtrasAdvancedFactoryDissolvingPortMixin",
                "EMExtrasAdvancedFactoryDissolvingPortMixin",
                "AdvancedFactoryLiquifyingPortMixin",
                "AdvancedFactoryPaintingPortMixin",
                "AdvancedFactoryPressurizedReactingPortMixin",
                "AdvancedFactoryWashingPortMixin");
        String config = Files.readString(Path.of("src/main/resources/mekenergistics.mixins.json"));
        String plugin = Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/mixin/MekEnergisticsMixinPlugin.java"));

        for (String mixin : expected) {
            assertTrue(config.contains("\"" + mixin + "\""), mixin + " is not registered");
            String source = Files.readString(mixinSource(mixin));
            assertTrue(source.contains("implements AdvancedFactoryUpgradeAccess"), mixin);
            assertTrue(plugin.contains("." + mixin), mixin + " is not optional-gated");
        }
    }

    @Test
    void everyEvolvedFactoryIdentityUsesTheCatalogDrivenFactoryRuntime() throws IOException {
        long evolvedFactories = CompatMachineCatalog.all()
                .filter(spec -> spec.family() == CompatMachineFamily.EMEK_FACTORY)
                .count();
        assertEquals(54, evolvedFactories);

        String profiles = Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/upgrade/MekanismFactoryUpgradeProfiles.java"));
        assertTrue(profiles.contains("CompatMachineCatalog.findBySourceBlockId(sourceBlockId)"));
        assertTrue(profiles.contains("spec.machine().isFactory()"));
    }

    @Test
    void evolvedAlloyingFactoriesExposeThreeInputsAndNetworkEnergy() throws IOException {
        String factoryMixin = Files.readString(FACTORY_MIXIN);
        String alloyingMixin = Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/mixin/EvolvedAlloyingFactoryMeUpgradeMixin.java"));
        String mixinPlugin = Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/mixin/MekEnergisticsMixinPlugin.java"));

        assertTrue(factoryMixin.contains("EvolvedAlloyingFactoryUpgradeAccess alloying"));
        assertTrue(factoryMixin.contains("alloying.mekenergistics$getSecondExtraSlot()"));
        assertTrue(alloyingMixin.contains("wrapRecipeEnergy(tile.getEnergyContainer()"));
        assertTrue(mixinPlugin.contains("EvolvedAlloyingFactoryMeUpgradeMixin"));
        assertTrue(mixinPlugin.contains("Gate.mod(\"evolvedmekanism\")"));
    }

    @Test
    void emExtrasAlloyingAccessorUsesTheExactRuntimeFieldDescriptor() throws IOException {
        String accessor = Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/mixin/EmExtrasAlloyingFactoryAccessor.java"));

        assertTrue(accessor.contains("LimitedInputInventorySlot mekenergistics$getSecondExtraSlot()"),
                "Mixin accessors must match the target field descriptor exactly");
    }

    @Test
    void mekExtrasFactoriesReachTheAlloyingSlotWithoutLoadingAMixinAccessor() throws IOException {
        String factoryMixin = Files.readString(mixinSource("MekExtrasFactoryMeUpgradeMixin"));
        String alloyingMixin = Files.readString(mixinSource("MekExtrasAlloyingFactoryMeUpgradeEnergyMixin"));
        String mixinConfig = Files.readString(Path.of("src/main/resources/mekenergistics.mixins.json"));

        assertTrue(factoryMixin.contains("instanceof EvolvedAlloyingFactoryUpgradeAccess alloying"));
        assertFalse(factoryMixin.contains("MekExtrasAlloyingFactoryAccessor"),
                "Mekanism Extras base factory must not load a Mixin accessor class directly");
        assertTrue(alloyingMixin.contains("implements EvolvedAlloyingFactoryUpgradeAccess"));
        assertTrue(alloyingMixin.contains("@Shadow LimitedInputInventorySlot secondExtraSlot"));
        assertFalse(mixinConfig.contains("\"MekExtrasAlloyingFactoryAccessor\""));
    }

    @Test
    void mekExtrasDissolvingAccessorUsesTheExactRuntimeFieldDescriptor() throws IOException {
        String accessor = Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/mixin/TileEntityExtraDissolvingFactoryAccessor.java"));

        assertTrue(accessor.contains("ChemicalInventorySlot mekenergistics$getChemicalInputSlot()"),
                "Mixin accessors must match the target field descriptor exactly");
    }

    @Test
    void factoryUpgradeMixinsKeepLegacyMeFactoryIoConcrete() throws IOException {
        Set<String> roots = Set.of(
                "TileEntityFactoryMeUpgradeMixin",
                "MekExtrasFactoryMeUpgradeMixin",
                "MekmmFactoryMeUpgradeMixin",
                "MekExtrasMoreMachineFactoryMeUpgradeMixin",
                "EmExtrasFactoryMeUpgradeMixin",
                "EmExtrasMoreMachineFactoryMeUpgradeMixin",
                "AdvancedFactoryMeUpgradeMixin",
                "MekExtrasAdvancedFactoryMeUpgradeMixin",
                "EMExtrasAdvancedFactoryMeUpgradeMixin");

        for (String root : roots) {
            String mixin = Files.readString(mixinSource(root));
            assertTrue(mixin.contains("MeFactoryIoOwner.factoryPatternInputLayout(legacy)"), root);
            assertTrue(mixin.contains("MeFactoryIoOwner.factoryPatternOutputPorts(legacy)"), root);
            assertTrue(mixin.contains("legacy.getAeSupport()"), root);
            assertTrue(mixin.contains("getRecipeAeSupport()"), root);
            assertTrue(mixin.contains("getAeOutputMode()"), root);
            assertTrue(mixin.contains("cycleAeOutputMode()"), root);
        }
    }

    @Test
    void factoryMixinRoutesAllParallelInputsOutputsAndSecondaryResources() throws IOException {
        String source = Files.readString(FACTORY_MIXIN);
        assertTrue(source.contains("autoSortedFactoryItemInput(this.inputSlots)"));
        assertTrue(source.contains("this.outputSlots.stream().map(MeMachineIoAdapter::itemOutput)"));
        assertTrue(source.contains("type == FactoryType.COMBINING"));
        assertTrue(source.contains("chemicalInput(chemicalFactory.getChemicalTank())"));
        assertTrue(source.contains("MeMachineIoAdapter.manualItemInput(getExtraSlot())"));
        assertTrue(source.contains("chemicalOutput(chemicalFactory.getChemicalTank())"));
    }

    @Test
    void activationOwnsNodeRefreshesRecipesAndInvalidatesCapability() throws IOException {
        String factoryMixin = Files.readString(FACTORY_MIXIN);
        String adapter = Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/upgrade/MeUpgradeRecipeMachineAdapter.java"));
        String runtime = Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/upgrade/MeUpgradeRecipeMachineRuntime.java"));

        assertTrue(factoryMixin.contains("processMePatternIo(cir.getReturnValue())"));
        assertTrue(adapter.contains("getOrCreateMeUpgradeRuntime().tick(isMeUpgradeActive(), changed)"));
        assertTrue(runtime.contains("transitionTo(active)"));
        assertTrue(runtime.contains("syncOwner()"));
        assertTrue(runtime.contains("refreshRecipeCache()"));
        assertTrue(runtime.contains("invalidateCapabilities()"));
        assertTrue(runtime.contains("this.support.destroyNode()"));
    }

    @Test
    void existingFactoryInterfaceSpecializesTheSharedAeMachineContract() throws IOException {
        String source = Files.readString(FACTORY_AE_MACHINE);

        assertTrue(source.contains("MeFactoryAeMachine extends MeAeMachine"));
        assertTrue(source.contains("getRecipeAeSupport()"));
        assertTrue(source.contains("return getAeSupport();"));
    }

    @Test
    void factoryMixinProvidesConcreteMainNodeBridge() throws IOException {
        String source = Files.readString(FACTORY_AE_MACHINE);

        assertTrue(source.contains("MeFactoryAeMachine extends MeAeMachine"));
        assertTrue(source.contains("return getAeSupport();"));
    }

    @Test
    void bothFactoryRecipeFamiliesBindDynamicEnergyBeforeInstallation() throws IOException {
        for (Path mixin : new Path[]{ITEM_ENERGY_MIXIN, CHEMICAL_ENERGY_MIXIN}) {
            String source = Files.readString(mixin);
            assertTrue(source.contains("machine.isMeUpgradeTarget()"));
            assertTrue(source.contains("wrapRecipeEnergy(tile.getEnergyContainer()"));
        }
    }

    @Test
    void factoryItemsAndBlocksReceivePatternSlotsAndGridCapability() throws IOException {
        String attachment = Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/mixin/EnrichmentChamberItemContainerCreatorMixin.java"));
        String capabilities = Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/registry/ModBlockEntities.java"));
        assertTrue(attachment.contains("type == ContainerType.ITEM && index >= creators.size()"),
                "the item container creator mixin must fabricate the ME pattern slot for any item whose"
                + "attached item containers exceed the vanilla creators, covering every factory family");
        assertFalse(capabilities.contains("filter(spec -> !spec.machine().isFactory()"));
        assertTrue(capabilities.contains("CompatMachineCatalog.available()"));
        assertTrue(capabilities.contains("machine.isMeUpgradeActive() ? machine : null"));
    }

    @Test
    void meUpgradeStateUsesNativeMekanismComponentsAndRemainsEmpoweredFree() throws IOException {
        List<String> removedBackendSymbols = List.of(
                "MePatternProviderUpgrade",
                "MePassiveCraftingUpgrade",
                "StandaloneUpgradePersistence",
                "MePatternUpgradeLang",
                "EmpoweredMePatternUpgradeProvider",
                "EmpoweredMePassiveCraftingUpgradeProvider",
                "IAdditionalUpgrades",
                "dev.lapis256",
                "hasMekanismEmpoweredCore");
        Path main = Path.of("src/main/java");
        try (var stream = Files.walk(main)) {
            List<Path> sources = stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
            for (Path source : sources) {
                String text = Files.readString(source);
                for (String symbol : removedBackendSymbols) {
                    assertFalse(text.contains(symbol),
                            () -> source + " must not reference removed backend symbol " + symbol);
                }
            }
        }

        String mixins = Files.readString(Path.of("src/main/resources/mekenergistics.mixins.json"));
        assertTrue(mixins.contains("UpgradeMixin"));
        assertTrue(mixins.contains("UpgradeUtilsMixin"));
        assertTrue(mixins.contains("TileComponentUpgradeMixin"));

        String item = Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/item/MeUpgradeItem.java"));
        assertTrue(item.contains("extends ItemUpgrade"));
        String support = Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/upgrade/MeUpgradeSupportRegistrar.java"));
        assertTrue(support.contains("AttributeUpgradeSupport.create"));

        assertFalse(Files.exists(Path.of(
                "src/main/resources/META-INF/services/dev.lapis256.mekanism_empowered.core.api.upgrade.IAdditionalUpgrades")),
                "Empowered service file must be gone");

        String build = Files.readString(Path.of("build.gradle"));
        assertFalse(build.contains("mekanism-empowered"),
                "build.gradle must not declare Mekanism Empowered or Empowered Core dependencies");
    }

    @Test
    void legacyMeFactoryReloadWakesPausedRecipeMonitorsAfterNodeRestore() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/beipuo/mekenergistics/blockentity/support/MeFactoryAeSupport.java"));

        assertTrue(source.contains("factory.unpauseRecipeMonitors()"),
                "restoring a factory node must also wake its paused recipe monitors");
    }

    private static Map<CompatMachineFamily, FactoryMixinCoverage> factoryMixins() {
        Map<CompatMachineFamily, FactoryMixinCoverage> coverage =
                new EnumMap<>(CompatMachineFamily.class);
        coverage.put(CompatMachineFamily.MEKANISM_FACTORY, new FactoryMixinCoverage(
                "TileEntityFactoryMeUpgradeMixin", "TileEntityItemFactoryMeUpgradeEnergyMixin"));
        coverage.put(CompatMachineFamily.EMEK_FACTORY, new FactoryMixinCoverage(
                "TileEntityFactoryMeUpgradeMixin", "TileEntityItemFactoryMeUpgradeEnergyMixin"));
        coverage.put(CompatMachineFamily.MEKE_FACTORY, new FactoryMixinCoverage(
                "MekExtrasFactoryMeUpgradeMixin", "MekExtrasFactoryMeUpgradeEnergyMixin"));
        coverage.put(CompatMachineFamily.MEKMM_FACTORY, new FactoryMixinCoverage(
                "MekmmFactoryMeUpgradeMixin", "MekmmFactoryMeUpgradeEnergyMixin"));
        coverage.put(CompatMachineFamily.MEKMM_ADVANCED_FACTORY, new FactoryMixinCoverage(
                "AdvancedFactoryMeUpgradeMixin", "AdvancedFactoryMeUpgradeEnergyMixin"));
        coverage.put(CompatMachineFamily.MEKE_MEKMM_FACTORY, new FactoryMixinCoverage(
                "MekExtrasMoreMachineFactoryMeUpgradeMixin",
                "MekExtrasMoreMachineFactoryMeUpgradeEnergyMixin"));
        coverage.put(CompatMachineFamily.MEKE_MEKMM_ADVANCED_FACTORY, new FactoryMixinCoverage(
                "MekExtrasAdvancedFactoryMeUpgradeMixin", "AdvancedFactoryMeUpgradeEnergyMixin"));
        coverage.put(CompatMachineFamily.EMEKE_FACTORY, new FactoryMixinCoverage(
                "EmExtrasFactoryMeUpgradeMixin", "EmExtrasFactoryMeUpgradeEnergyMixin"));
        coverage.put(CompatMachineFamily.EMEKE_MEKAF_ADVANCED_FACTORY, new FactoryMixinCoverage(
                "EMExtrasAdvancedFactoryMeUpgradeMixin", "AdvancedFactoryMeUpgradeEnergyMixin"));
        coverage.put(CompatMachineFamily.EMEKE_MEKMM_FACTORY, new FactoryMixinCoverage(
                "EmExtrasMoreMachineFactoryMeUpgradeMixin",
                "EmExtrasMoreMachineFactoryMeUpgradeEnergyMixin"));
        return Map.copyOf(coverage);
    }

    private static Path mixinSource(String simpleName) {
        return Path.of("src/main/java/com/beipuo/mekenergistics/mixin", simpleName + ".java");
    }
}
