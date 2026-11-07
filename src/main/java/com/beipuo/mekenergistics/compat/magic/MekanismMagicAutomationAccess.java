package com.beipuo.mekenergistics.compat.magic;

import com.beipuo.mekenergistics.compat.OptionalCompatClasses;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.inventory.IInventorySlot;
import org.jetbrains.annotations.Nullable;

/**
 * Soft bridge to Mekanism Magic's {@code IMekanismMagicAutomation} surface.
 *
 * <p>Always-loaded code must not hard-link Magic classes. This accessor resolves the published API
 * by name only after the mod is confirmed present, so a world without Magic stays loadable.</p>
 */
public final class MekanismMagicAutomationAccess {
    private static final String API_CLASS = "com.example.mekanismmagic.api.IMekanismMagicAutomation";
    private static final Object LOCK = new Object();

    private static volatile boolean resolved;
    private static @Nullable Class<?> apiClass;
    private static @Nullable Method supportsPatternAutomation;
    private static @Nullable Method patternInputs;
    private static @Nullable Method patternOutputs;
    private static @Nullable Method persistentInputs;
    private static @Nullable Method manualOnlySlots;
    private static @Nullable Method energyContainer;
    private static @Nullable Method isBusy;

    private MekanismMagicAutomationAccess() {
    }

    public static boolean isMagicAutomation(Object tile) {
        Class<?> api = apiClass();
        return api != null && tile != null && api.isInstance(tile);
    }

    public static boolean supportsPatternAutomation(Object tile) {
        if (!isMagicAutomation(tile)) {
            return false;
        }
        Boolean supported = invoke(supportsPatternAutomation, tile);
        return supported == null || supported;
    }

    public static List<IInventorySlot> patternInputs(Object tile) {
        return inventorySlots(invoke(patternInputs, tile));
    }

    public static List<IInventorySlot> patternOutputs(Object tile) {
        return inventorySlots(invoke(patternOutputs, tile));
    }

    public static List<IInventorySlot> persistentInputs(Object tile) {
        return inventorySlots(invoke(persistentInputs, tile));
    }

    public static List<IInventorySlot> manualOnlySlots(Object tile) {
        return inventorySlots(invoke(manualOnlySlots, tile));
    }

    public static @Nullable IEnergyContainer energyContainer(Object tile) {
        return invoke(energyContainer, tile);
    }

    public static boolean isBusy(Object tile) {
        Boolean busy = invoke(isBusy, tile);
        return busy != null && busy;
    }

    @Nullable
    private static Class<?> apiClass() {
        resolve();
        return apiClass;
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        synchronized (LOCK) {
            if (resolved) {
                return;
            }
            if (!OptionalCompatClasses.hasMekanismMagic()) {
                resolved = true;
                return;
            }
            installApiClass(loadApiClass());
            resolved = true;
        }
    }

    @Nullable
    private static Class<?> loadApiClass() {
        try {
            return Class.forName(API_CLASS);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static void installApiClass(@Nullable Class<?> api) {
        if (api == null) {
            clearApi();
            return;
        }
        try {
            apiClass = api;
            supportsPatternAutomation = api.getMethod("mekanismMagicSupportsPatternAutomation");
            patternInputs = api.getMethod("mekanismMagicPatternInputs");
            patternOutputs = api.getMethod("mekanismMagicPatternOutputs");
            persistentInputs = optionalMethod(api, "mekanismMagicPersistentInputs");
            manualOnlySlots = optionalMethod(api, "mekanismMagicManualOnlySlots");
            energyContainer = api.getMethod("mekanismMagicEnergyContainer");
            isBusy = api.getMethod("mekanismMagicIsBusy");
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            clearApi();
        }
    }

    private static void clearApi() {
        apiClass = null;
        supportsPatternAutomation = null;
        patternInputs = null;
        patternOutputs = null;
        persistentInputs = null;
        manualOnlySlots = null;
        energyContainer = null;
        isBusy = null;
    }

    @Nullable
    private static Method optionalMethod(Class<?> api, String name) {
        try {
            return api.getMethod(name);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    /** Test seam: installs a resolved API class without requiring the Magic mod loader entry. */
    public static void installApiForTest(Class<?> api) {
        synchronized (LOCK) {
            installApiClass(api);
            resolved = true;
        }
    }

    /** Test seam: clears cached API resolution between cases. */
    public static void resetForTest() {
        synchronized (LOCK) {
            clearApi();
            resolved = false;
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <T> T invoke(@Nullable Method method, Object tile) {
        if (method == null || tile == null) {
            return null;
        }
        try {
            return (T) method.invoke(tile);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            // Report each failing API method once: a silent failure here makes a Mekanism Magic
            // machine quietly behave as if it had no pattern I/O, which is very hard to diagnose.
            if (REPORTED_FAILURES.add(method.getName())) {
                com.beipuo.mekenergistics.MekEnergistics.LOGGER.warn(
                        "Mekanism Magic automation call {} failed on {}; treating it as unavailable",
                        method.getName(), tile.getClass().getName(), failure);
            }
            return null;
        }
    }

    private static final java.util.Set<String> REPORTED_FAILURES = java.util.concurrent.ConcurrentHashMap.newKeySet();

    @SuppressWarnings("unchecked")
    private static List<IInventorySlot> inventorySlots(@Nullable Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        for (Object entry : list) {
            if (!(entry instanceof IInventorySlot)) {
                return List.of();
            }
        }
        return Collections.unmodifiableList((List<IInventorySlot>) list);
    }
}
