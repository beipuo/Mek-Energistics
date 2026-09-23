package com.beipuo.mekenergistics.compat.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import org.junit.jupiter.api.Test;

class CompatFactoryTierGraphTest {
    @Test
    void indexesEveryFactoryByRouteProviderAndExactSourceId() {
        CompatMachineCatalog.all().forEach(spec -> {
            MeMekanismMachine expected = spec.machine();
            assertEquals(expected, CompatMachineCatalog.findBySourceBlockId(spec.sourceBlockId())
                    .map(CompatMachineSpec::machine).orElse(null), spec.machine().name());
            assertEquals(expected, CompatFactoryTierGraph.findDeclaredByRegistryName(
                    spec.machine().registryName()), spec.machine().name());
            if (spec.kind() != CompatMachineKind.MACHINE) {
                assertEquals(expected, CompatFactoryTierGraph.findDeclaredFactory(
                        FactoryTypeKey.of(spec), spec.tierId()), spec.machine().name());
                assertEquals(expected, CompatFactoryTierGraph.findDeclaredFactory(
                        spec.route(), spec.tierId(), spec.machineTypeId()), spec.machine().name());
                assertEquals(expected, CompatFactoryTierGraph.findDeclaredFactory(
                        spec.provider(), spec.tierId(), spec.machineTypeId()), spec.machine().name());
            }
        });
    }

    @Test
    void followsCoreAndCrossProviderUpgradeEdges() {
        assertEquals(MeMekanismMachine.valueOf("BASIC_SMELTING_FACTORY"),
                MeMekanismMachine.ENERGIZED_SMELTER.getBasicFactory());
        assertEquals(MeMekanismMachine.valueOf("ADVANCED_SMELTING_FACTORY"),
                MeMekanismMachine.valueOf("BASIC_SMELTING_FACTORY").getNextFactory());
        assertEquals(MeMekanismMachine.valueOf("ELITE_SMELTING_FACTORY"),
                MeMekanismMachine.valueOf("ADVANCED_SMELTING_FACTORY").getNextFactory());
        assertEquals(MeMekanismMachine.valueOf("ULTIMATE_SMELTING_FACTORY"),
                MeMekanismMachine.valueOf("ELITE_SMELTING_FACTORY").getNextFactory());

        assertEquals(MeMekanismMachine.valueOf("OVERCLOCKED_SMELTING_FACTORY"),
                CompatFactoryTierGraph.declaredNextFactory(MeMekanismMachine.valueOf("ULTIMATE_SMELTING_FACTORY")));
        assertEquals(MeMekanismMachine.valueOf("ABSOLUTE_OVERCLOCKED_SMELTING_FACTORY"),
                CompatFactoryTierGraph.declaredNextFactory(MeMekanismMachine.valueOf("CREATIVE_SMELTING_FACTORY")));
    }

    @Test
    void preservesMoreMachineAndAdvancedFactoryTracks() {
        assertEquals(MeMekanismMachine.valueOf("BASIC_DISSOLVING_FACTORY"),
                CompatFactoryTierGraph.declaredBasicFactory(MeMekanismMachine.CHEMICAL_DISSOLUTION_CHAMBER));
        assertEquals(MeMekanismMachine.valueOf("BASIC_RECYCLING_FACTORY"),
                CompatFactoryTierGraph.declaredBasicFactory(MeMekanismMachine.RECYCLER));
        assertEquals(MeMekanismMachine.valueOf("ABSOLUTE_RECYCLING_FACTORY"),
                CompatFactoryTierGraph.declaredNextFactory(MeMekanismMachine.valueOf("ULTIMATE_RECYCLING_FACTORY")));
        assertEquals(MeMekanismMachine.valueOf("BASIC_CENTRIFUGING_FACTORY"),
                CompatFactoryTierGraph.declaredBasicFactory(MeMekanismMachine.ISOTOPIC_CENTRIFUGE));
        assertEquals(MeMekanismMachine.valueOf("ABSOLUTE_CENTRIFUGING_FACTORY"),
                CompatFactoryTierGraph.declaredNextFactory(MeMekanismMachine.valueOf("ULTIMATE_CENTRIFUGING_FACTORY")));
    }

    @Test
    void resolvesInstallerSelectedTierWithoutFamilyBranches() {
        assertEquals(MeMekanismMachine.valueOf("ABSOLUTE_RECYCLING_FACTORY"),
                CompatFactoryTierGraph.declaredFactoryAtTier(
                        MeMekanismMachine.valueOf("ULTIMATE_RECYCLING_FACTORY"), CompatMod.MEKE, "absolute"));
        assertEquals(MeMekanismMachine.valueOf("ABSOLUTE_CENTRIFUGING_FACTORY"),
                CompatFactoryTierGraph.declaredFactoryAtTier(
                        MeMekanismMachine.valueOf("ULTIMATE_CENTRIFUGING_FACTORY"), CompatMod.MEKE, "absolute"));
        assertEquals(MeMekanismMachine.valueOf("ABSOLUTE_OVERCLOCKED_SMELTING_FACTORY"),
                CompatFactoryTierGraph.declaredFactoryAtTier(
                        MeMekanismMachine.valueOf("CREATIVE_SMELTING_FACTORY"), CompatMod.EMEKE,
                        "absolute_overclocked"));
    }

    @Test
    void resolvesOnlyForwardMaxTierInstallerTargets() {
        assertEquals(MeMekanismMachine.valueOf("MULTIVERSAL_SMELTING_FACTORY"),
                CompatFactoryTierGraph.declaredForwardFactoryAtTier(
                        MeMekanismMachine.ENERGIZED_SMELTER, CompatMod.EMEK, "multiversal"));
        assertEquals(MeMekanismMachine.valueOf("CREATIVE_SMELTING_FACTORY"),
                CompatFactoryTierGraph.declaredForwardFactoryAtTier(
                        MeMekanismMachine.valueOf("OVERCLOCKED_SMELTING_FACTORY"), CompatMod.EMEK, "creative"));
        assertEquals(MeMekanismMachine.valueOf("CREATIVE_ALLOYING_FACTORY"),
                CompatFactoryTierGraph.declaredForwardFactoryAtTier(
                        MeMekanismMachine.ALLOYER, CompatMod.EMEK, "creative"));

        assertNull(CompatFactoryTierGraph.declaredForwardFactoryAtTier(
                MeMekanismMachine.valueOf("CREATIVE_SMELTING_FACTORY"), CompatMod.EMEK, "creative"));
        assertNull(CompatFactoryTierGraph.declaredForwardFactoryAtTier(
                MeMekanismMachine.valueOf("CREATIVE_SMELTING_FACTORY"), CompatMod.EMEK, "multiversal"));
        assertNull(CompatFactoryTierGraph.declaredForwardFactoryAtTier(
                MeMekanismMachine.valueOf("ABSOLUTE_SMELTING_FACTORY"), CompatMod.EMEK, "creative"));
        assertNull(CompatFactoryTierGraph.declaredForwardFactoryAtTier(
                MeMekanismMachine.THERMALIZER, CompatMod.EMEK, "creative"));
    }
}
