package com.beipuo.mekenergistics.compat.catalog;

import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Indexed factory upgrade graph. The graph only contains catalog data so loading it never links
 * classes from optional compatibility mods.
 */
public final class CompatFactoryTierGraph {
    private static final String BASIC = "basic";
    private static final String ABSOLUTE = "absolute";
    private static final String OVERCLOCKED = "overclocked";
    private static final String ABSOLUTE_OVERCLOCKED = "absolute_overclocked";

    private static final List<String> MEKANISM_TIERS = List.of(BASIC, "advanced", "elite", "ultimate");
    private static final List<String> EVOLVED_TIERS = List.of(
            BASIC, "advanced", "elite", "ultimate", OVERCLOCKED, "quantum", "dense", "multiversal", "creative");
    private static final List<String> EXTRA_TIERS = List.of(ABSOLUTE, "supreme", "cosmic", "infinite");
    private static final List<String> COMBINED_TIERS = List.of(
            ABSOLUTE_OVERCLOCKED, "supreme_quantum", "cosmic_dense", "infinite_multiversal");

    private static final Map<FactoryCoordinate, MeMekanismMachine> FACTORIES = buildFactories();
    private static final Map<ProviderCoordinate, FactoryTypeKey> FACTORY_KEYS_BY_PROVIDER =
            buildFactoryKeysByProvider();
    private static final Map<MachineCoordinate, MeMekanismMachine> BASE_MACHINES = buildBaseMachines();
    private static final Map<String, MeMekanismMachine> MACHINES_BY_REGISTRY_NAME = buildMachinesByRegistryName();

    private CompatFactoryTierGraph() {
    }

    @Nullable
    public static MeMekanismMachine basicFactory(MeMekanismMachine machine) {
        return basicFactory(machine, true);
    }

    @Nullable
    static MeMekanismMachine declaredBasicFactory(MeMekanismMachine machine) {
        return basicFactory(machine, false);
    }

    @Nullable
    private static MeMekanismMachine basicFactory(MeMekanismMachine machine, boolean requireAvailable) {
        CompatMachineSpec spec = CompatMachineCatalog.get(machine);
        String typeId = spec.machineTypeId();
        if (machine == MeMekanismMachine.ISOTOPIC_CENTRIFUGE) {
            return findFactory(
                    CompatRegistrationRoute.MEKMM_ADVANCED_FACTORY, BASIC, "centrifuging", requireAvailable);
        }
        if (machine == MeMekanismMachine.CHEMICAL_DISSOLUTION_CHAMBER) {
            return findFactory(
                    CompatRegistrationRoute.MEKMM_ADVANCED_FACTORY, BASIC, "dissolving", requireAvailable);
        }
        MeMekanismMachine advancedFactory = findFactory(
                CompatRegistrationRoute.MEKMM_ADVANCED_FACTORY, BASIC, typeId, requireAvailable);
        if (advancedFactory != null) {
            return advancedFactory;
        }
        return switch (spec.provider()) {
            case MEKANISM -> findFactory(CompatMod.MEKANISM, BASIC, typeId, requireAvailable);
            case MEKMM -> findFactory(CompatMod.MEKMM, BASIC, typeId, requireAvailable);
            case MEKE -> findFactory(CompatMod.MEKE, ABSOLUTE, typeId, requireAvailable);
            case EMEK, EMEKE -> {
                MeMekanismMachine evolved = findFactory(CompatMod.EMEK, BASIC, typeId, requireAvailable);
                yield evolved != null
                        ? evolved : findFactory(CompatMod.MEKANISM, BASIC, typeId, requireAvailable);
            }
        };
    }

    @Nullable
    public static MeMekanismMachine nextFactory(MeMekanismMachine machine) {
        return nextFactory(machine, true);
    }

    @Nullable
    static MeMekanismMachine declaredNextFactory(MeMekanismMachine machine) {
        return nextFactory(machine, false);
    }

    @Nullable
    private static MeMekanismMachine nextFactory(MeMekanismMachine machine, boolean requireAvailable) {
        CompatMachineSpec spec = CompatMachineCatalog.get(machine);
        if (spec.kind() == CompatMachineKind.MACHINE || spec.tierId() == null) {
            return null;
        }
        String tierId = spec.tierId();
        String typeId = spec.machineTypeId();
        return switch (spec.provider()) {
            case MEKANISM -> {
                String nextTier = nextTier(MEKANISM_TIERS, tierId);
                if (nextTier != null) {
                    yield findFactory(CompatMod.MEKANISM, nextTier, typeId, requireAvailable);
                }
                MeMekanismMachine evolved = findFactory(
                        CompatMod.EMEK, OVERCLOCKED, typeId, requireAvailable);
                yield evolved != null
                        ? evolved : findFactory(CompatMod.MEKE, ABSOLUTE, typeId, requireAvailable);
            }
            case EMEK -> {
                String nextTier = nextTier(EVOLVED_TIERS, tierId);
                yield nextTier == null
                        ? findFactory(CompatMod.EMEKE, ABSOLUTE_OVERCLOCKED, typeId, requireAvailable)
                        : findFactory(CompatMod.EMEK, nextTier, typeId, requireAvailable);
            }
            case MEKMM -> {
                String nextTier = nextTier(MEKANISM_TIERS, tierId);
                yield nextTier == null
                        ? findFactory(CompatMod.MEKE, ABSOLUTE, typeId, requireAvailable)
                        : findFactory(CompatMod.MEKMM, nextTier, typeId, requireAvailable);
            }
            case MEKE -> {
                String nextTier = nextTier(EXTRA_TIERS, tierId);
                yield nextTier == null
                        ? null : findFactory(CompatMod.MEKE, nextTier, typeId, requireAvailable);
            }
            case EMEKE -> {
                String nextTier = nextTier(COMBINED_TIERS, tierId);
                yield nextTier == null
                        ? null : findFactory(CompatMod.EMEKE, nextTier, typeId, requireAvailable);
            }
        };
    }

    /** Resolves an installer-selected tier while preserving the current physical factory type. */
    @Nullable
    public static MeMekanismMachine factoryAtTier(
            MeMekanismMachine current, CompatMod targetProvider, String targetTierId) {
        return factoryAtTier(current, targetProvider, targetTierId, true);
    }

    @Nullable
    static MeMekanismMachine declaredFactoryAtTier(
            MeMekanismMachine current, CompatMod targetProvider, String targetTierId) {
        return factoryAtTier(current, targetProvider, targetTierId, false);
    }

    @Nullable
    private static MeMekanismMachine factoryAtTier(
            MeMekanismMachine current, CompatMod targetProvider, String targetTierId,
            boolean requireAvailable) {
        if (targetTierId == null) {
            return null;
        }
        CompatMachineSpec currentSpec = CompatMachineCatalog.get(current);
        return findFactory(targetProvider, targetTierId, currentSpec.machineTypeId(), requireAvailable);
    }

    /**
     * Resolves a direct installer target only when it is ahead of the current machine on the
     * declared upgrade chain. This lets max-tier installers skip intermediate blocks without
     * permitting downgrades or jumps between unrelated compatibility tracks.
     */
    @Nullable
    public static MeMekanismMachine forwardFactoryAtTier(
            MeMekanismMachine current, CompatMod targetProvider, String targetTierId) {
        return forwardFactoryAtTier(current, targetProvider, targetTierId, true);
    }

    @Nullable
    static MeMekanismMachine declaredForwardFactoryAtTier(
            MeMekanismMachine current, CompatMod targetProvider, String targetTierId) {
        return forwardFactoryAtTier(current, targetProvider, targetTierId, false);
    }

    @Nullable
    private static MeMekanismMachine forwardFactoryAtTier(
            MeMekanismMachine current, CompatMod targetProvider, String targetTierId,
            boolean requireAvailable) {
        MeMekanismMachine target = factoryAtTier(current, targetProvider, targetTierId, requireAvailable);
        if (target == null || target == current) {
            return null;
        }
        java.util.Set<MeMekanismMachine> visited = new HashSet<>();
        MeMekanismMachine next = CompatMachineCatalog.get(current).kind() == CompatMachineKind.MACHINE
                ? basicFactory(current, requireAvailable)
                : nextFactory(current, requireAvailable);
        while (next != null && visited.add(next)) {
            if (next == target) {
                return target;
            }
            next = nextFactory(next, requireAvailable);
        }
        return null;
    }

    @Nullable
    public static MeMekanismMachine findFactory(
            CompatRegistrationRoute route, String tierId, String machineTypeId) {
        if (route == null || machineTypeId == null) {
            return null;
        }
        return findFactory(new FactoryTypeKey(route, machineTypeId), tierId);
    }

    @Nullable
    static MeMekanismMachine findDeclaredFactory(
            CompatRegistrationRoute route, String tierId, String machineTypeId) {
        if (route == null || machineTypeId == null) {
            return null;
        }
        return findFactory(new FactoryTypeKey(route, machineTypeId), tierId, false);
    }

    @Nullable
    public static MeMekanismMachine findFactory(FactoryTypeKey key, String tierId) {
        return findFactory(key, tierId, true);
    }

    @Nullable
    static MeMekanismMachine findDeclaredFactory(FactoryTypeKey key, String tierId) {
        return findFactory(key, tierId, false);
    }

    @Nullable
    private static MeMekanismMachine findFactory(
            FactoryTypeKey key, String tierId, boolean requireAvailable) {
        if (key == null || tierId == null) {
            return null;
        }
        return resolve(FACTORIES.get(new FactoryCoordinate(key, tierId)), requireAvailable);
    }

    @Nullable
    private static MeMekanismMachine findFactory(
            CompatRegistrationRoute route, String tierId, String machineTypeId,
            boolean requireAvailable) {
        if (route == null || machineTypeId == null) {
            return null;
        }
        return findFactory(new FactoryTypeKey(route, machineTypeId), tierId, requireAvailable);
    }

    @Nullable
    public static MeMekanismMachine findFactory(CompatMod provider, String tierId, String machineTypeId) {
        return findFactory(provider, tierId, machineTypeId, true);
    }

    @Nullable
    static MeMekanismMachine findDeclaredFactory(
            CompatMod provider, String tierId, String machineTypeId) {
        return findFactory(provider, tierId, machineTypeId, false);
    }

    @Nullable
    private static MeMekanismMachine findFactory(
            CompatMod provider, String tierId, String machineTypeId, boolean requireAvailable) {
        if (provider == null || tierId == null || machineTypeId == null) {
            return null;
        }
        FactoryTypeKey key = FACTORY_KEYS_BY_PROVIDER.get(
                new ProviderCoordinate(provider, tierId, machineTypeId));
        return key == null ? null : findFactory(key, tierId, requireAvailable);
    }

    @Nullable
    public static MeMekanismMachine findBaseMachine(CompatMod provider, String machineTypeId) {
        if (provider == null || machineTypeId == null) {
            return null;
        }
        return available(BASE_MACHINES.get(new MachineCoordinate(provider, machineTypeId)));
    }

    @Nullable
    public static MeMekanismMachine findByRegistryName(String registryName) {
        return registryName == null ? null : available(MACHINES_BY_REGISTRY_NAME.get(registryName));
    }

    @Nullable
    static MeMekanismMachine findDeclaredByRegistryName(String registryName) {
        return registryName == null ? null : MACHINES_BY_REGISTRY_NAME.get(registryName);
    }

    private static Map<FactoryCoordinate, MeMekanismMachine> buildFactories() {
        Map<FactoryCoordinate, MeMekanismMachine> factories = new HashMap<>();
        CompatMachineCatalog.all().filter(spec -> spec.kind() != CompatMachineKind.MACHINE).forEach(spec -> putUnique(
                factories,
                new FactoryCoordinate(FactoryTypeKey.of(spec), spec.tierId()),
                spec.machine(),
                "factory route coordinate"));
        return Map.copyOf(factories);
    }

    private static Map<ProviderCoordinate, FactoryTypeKey> buildFactoryKeysByProvider() {
        Map<ProviderCoordinate, FactoryTypeKey> keys = new HashMap<>();
        CompatMachineCatalog.all().filter(spec -> spec.kind() != CompatMachineKind.MACHINE).forEach(spec -> {
            ProviderCoordinate coordinate =
                    new ProviderCoordinate(spec.provider(), spec.tierId(), spec.machineTypeId());
            FactoryTypeKey previous = keys.put(coordinate, FactoryTypeKey.of(spec));
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate factory provider coordinate " + coordinate
                                + " for " + previous + " and " + FactoryTypeKey.of(spec));
            }
        });
        return Map.copyOf(keys);
    }

    private static Map<MachineCoordinate, MeMekanismMachine> buildBaseMachines() {
        Map<MachineCoordinate, MeMekanismMachine> machines = new HashMap<>();
        CompatMachineCatalog.all().filter(spec -> spec.kind() == CompatMachineKind.MACHINE).forEach(spec -> putUnique(
                machines,
                new MachineCoordinate(spec.provider(), spec.machineTypeId()),
                spec.machine(),
                "base machine coordinate"));
        return Map.copyOf(machines);
    }

    private static Map<String, MeMekanismMachine> buildMachinesByRegistryName() {
        Map<String, MeMekanismMachine> machines = new HashMap<>();
        CompatMachineCatalog.all().forEach(spec ->
                putUnique(machines, spec.machine().registryName(), spec.machine(), "ME registry name"));
        return Map.copyOf(machines);
    }

    private static <K> void putUnique(
            Map<K, MeMekanismMachine> index, K key, MeMekanismMachine machine, String description) {
        MeMekanismMachine previous = index.put(key, machine);
        if (previous != null) {
            throw new IllegalStateException(
                    "Duplicate " + description + " " + key + " for " + previous + " and " + machine);
        }
    }

    @Nullable
    private static MeMekanismMachine available(@Nullable MeMekanismMachine machine) {
        return machine != null && CompatMachineCatalog.isAvailable(machine) ? machine : null;
    }

    @Nullable
    private static MeMekanismMachine resolve(
            @Nullable MeMekanismMachine machine, boolean requireAvailable) {
        return requireAvailable ? available(machine) : machine;
    }

    @Nullable
    private static String nextTier(List<String> tiers, String currentTier) {
        int index = tiers.indexOf(currentTier);
        return index >= 0 && index + 1 < tiers.size() ? tiers.get(index + 1) : null;
    }

    private record FactoryCoordinate(FactoryTypeKey key, String tierId) {
    }

    private record ProviderCoordinate(CompatMod provider, String tierId, String machineTypeId) {
    }

    private record MachineCoordinate(CompatMod provider, String machineTypeId) {
    }

}
