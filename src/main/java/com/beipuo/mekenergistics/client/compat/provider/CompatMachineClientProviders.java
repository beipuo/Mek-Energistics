package com.beipuo.mekenergistics.client.compat.provider;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.compat.catalog.CompatMachineCatalog;
import com.beipuo.mekenergistics.compat.catalog.CompatMod;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

public final class CompatMachineClientProviders {
    private static final Map<CompatMod, String> PROVIDER_CLASSES = Map.of(
            CompatMod.MEKMM, "com.beipuo.mekenergistics.client.compat.provider.MekmmMachineClientProvider",
            CompatMod.MEKE, "com.beipuo.mekenergistics.client.compat.provider.MekeMachineClientProvider",
            CompatMod.EMEK, "com.beipuo.mekenergistics.client.compat.provider.EmekMachineClientProvider",
            CompatMod.EMEKE, "com.beipuo.mekenergistics.client.compat.provider.EmekeMachineClientProvider");
    private static final Map<CompatMod, CompatMachineClientProvider> PROVIDERS = new EnumMap<>(CompatMod.class);

    private CompatMachineClientProviders() {
    }

    public static Stream<CompatMachineClientProvider> available() {
        return CompatMachineCatalog.available()
                .map(spec -> spec.provider())
                .filter(PROVIDER_CLASSES::containsKey)
                .distinct()
                .map(CompatMachineClientProviders::get);
    }

    private static CompatMachineClientProvider get(CompatMod mod) {
        synchronized (PROVIDERS) {
            return PROVIDERS.computeIfAbsent(mod, CompatMachineClientProviders::load);
        }
    }

    private static CompatMachineClientProvider load(CompatMod mod) {
        String className = PROVIDER_CLASSES.get(mod);
        try {
            return (CompatMachineClientProvider) Class.forName(
                            className, true, CompatMachineClientProviders.class.getClassLoader())
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException
                | InvocationTargetException | LinkageError exception) {
            MekEnergistics.LOGGER.error("Unable to load client machine compatibility provider {} for {}",
                    className, mod, exception);
            throw new IllegalStateException("Unable to load client machine compatibility provider for " + mod,
                    exception);
        }
    }
}
