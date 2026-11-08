package com.beipuo.mekenergistics.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beipuo.mekenergistics.compat.catalog.CompatMod;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Both provider registries name their implementations as strings and resolve them with
 * {@link Class#forName}, so that an implementation linking an absent mod is never loaded. The
 * compiler cannot check those strings; a typo or a move surfaces only as a missing machine at
 * runtime, on the one setup that has the mod installed.
 *
 * <p>The names are read out of the production tables by reflection and checked against compiled
 * bytecode. The implementations themselves are deliberately never loaded here — several of them
 * reference optional mod classes that are absent from the test runtime.
 */
class ProviderTableTest {
    private static final JavaClasses MOD_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.beipuo.mekenergistics");

    @Test
    void everyServerProviderNameResolvesToAClassImplementingTheContract() {
        assertTableEntriesImplement(
                "com.beipuo.mekenergistics.compat.provider.CompatMachineProviders",
                "com.beipuo.mekenergistics.compat.provider.CompatMachineProvider");
    }

    @Test
    void everyClientProviderNameResolvesToAClassImplementingTheContract() {
        assertTableEntriesImplement(
                "com.beipuo.mekenergistics.client.compat.provider.CompatMachineClientProviders",
                "com.beipuo.mekenergistics.client.compat.provider.CompatMachineClientProvider");
    }

    @Test
    void everyModThatSuppliesMachinesHasAServerProvider() {
        Map<CompatMod, String> providers = providerTable(
                "com.beipuo.mekenergistics.compat.provider.CompatMachineProviders");

        for (CompatMod mod : CompatMod.values()) {
            assertTrue(providers.containsKey(mod), () -> mod + " has no registered machine provider");
        }
    }

    /**
     * Every optional mod that contributes machines also contributes menus, and a menu without a
     * bound screen silently opens nothing on the client. Core Mekanism screens are bound directly
     * by {@code ClientSetup}, so only the optional mods need a client provider.
     */
    @Test
    void everyOptionalModThatSuppliesMachinesHasAClientProvider() {
        Map<CompatMod, String> providers = providerTable(
                "com.beipuo.mekenergistics.client.compat.provider.CompatMachineClientProviders");

        for (CompatMod mod : CompatMod.values()) {
            if (mod == CompatMod.MEKANISM) {
                continue;
            }
            assertTrue(providers.containsKey(mod), () -> mod + " has no registered client screen provider");
        }
    }

    private static void assertTableEntriesImplement(String registryClass, String contract) {
        Map<CompatMod, String> providers = providerTable(registryClass);
        assertFalse(providers.isEmpty(), () -> registryClass + " lists no providers");

        providers.forEach((mod, className) -> {
            Optional<JavaClass> implementation = MOD_CLASSES.stream()
                    .filter(candidate -> candidate.getName().equals(className))
                    .findFirst();
            assertTrue(implementation.isPresent(),
                    () -> mod + " names " + className + ", which no longer exists");
            assertTrue(implementation.get().getAllRawInterfaces().stream()
                            .anyMatch(type -> type.getName().equals(contract)),
                    () -> className + " no longer implements " + contract);
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<CompatMod, String> providerTable(String registryClass) {
        try {
            Field table = Class.forName(registryClass).getDeclaredField("PROVIDER_CLASSES");
            table.setAccessible(true);
            return (Map<CompatMod, String>) table.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Cannot read the provider table of " + registryClass, exception);
        }
    }
}
