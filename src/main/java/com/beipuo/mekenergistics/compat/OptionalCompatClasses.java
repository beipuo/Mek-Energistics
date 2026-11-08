package com.beipuo.mekenergistics.compat;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.neoforged.fml.ModList;
import mekanism.common.tier.FactoryTier;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.Nullable;

public final class OptionalCompatClasses {
    private static final String MEKAF_ITEM_TO_CHEMICAL_FACTORY =
            "com/jerry/mekaf/common/tile/factory/TileEntityItemStackToChemicalStackFactory.class";
    private static final String MEKE_EXTRA_ADVANCED_FACTORY =
            "com/jerry/mekextras/common/integration/mekaf/tile/factory/base/TileEntityExtraAdvancedFactoryBase.class";
    private static final String MEKE_EXTRA_MORE_MACHINE_FACTORY =
            "com/jerry/mekextras/common/integration/mekmm/tile/factory/TileEntityExtraMoreMachineFactory.class";
    private static final String[] MEKMM_LARGE_MACHINE_CLASSES = {
            "com/jerry/meklm/common/tile/machine/TileEntityLargeRotaryCondensentrator.class",
            "com/jerry/meklm/common/tile/machine/TileEntityLargeSolarNeutronActivator.class",
            "com/jerry/meklm/common/tile/machine/TileEntityLargeElectrolyticSeparator.class",
            "com/jerry/meklm/common/tile/machine/TileEntityLargeChemicalInfuser.class",
            "com/jerry/meklm/common/tile/machine/TileEntityLargeAntiprotonicNucleosynthesizer.class"
    };
    private static final String EMEKE_ADVANCED_FACTORY =
            "io/github/masyumero/emextras/common/integration/mekaf/tile/factory/base/TileEntityEMExtraAdvancedFactoryBase.class";
    private static final String EMEKE_MORE_MACHINE_FACTORY =
            "io/github/masyumero/emextras/common/integration/mekmm/tile/factory/TileEntityEMExtraMoreMachineFactory.class";

    private OptionalCompatClasses() {
    }

    /**
     * Loaded-mod answers, which are fixed once the mod list exists. {@link #hasAppliedFlux()} sits on
     * the per-tick energy path, so the lookup is not repeated every call.
     */
    private static final Map<String, Boolean> LOADED_MODS = new ConcurrentHashMap<>();

    private static boolean isLoaded(String modId) {
        ModList modList = ModList.get();
        if (modList == null) {
            // Too early to answer, and too early to remember the answer.
            return false;
        }
        return LOADED_MODS.computeIfAbsent(modId, modList::isLoaded);
    }

    public static boolean hasMekmm() {
        return isLoaded("mekmm");
    }

    public static boolean hasMekanismExtras() {
        return isLoaded("mekanism_extras");
    }

    public static boolean hasEvolvedMekanism() {
        return isLoaded("evolvedmekanism");
    }

    public static boolean hasEvolvedMekanismExtras() {
        return isLoaded("emextras");
    }

    public static boolean hasExtendedAe() {
        return isLoaded("extendedae");
    }

    public static boolean hasAppliedFlux() {
        return isLoaded("appflux");
    }

    public static boolean hasMekanismMagic() {
        return isLoaded("mekanism_magic");
    }

    /**
     * Reflection results keyed by tier id. These resolve to constants, but the lookups sit on
     * tooltip and GUI layout paths that re-ask every frame, so each tier is resolved once.
     * {@link Optional} because a miss is a normal answer that must also be remembered.
     */
    private static final Map<String, Optional<FactoryTier>> EVOLVED_FACTORY_TIERS = new ConcurrentHashMap<>();
    private static final Map<String, Optional<TextColor>> MEKE_TIER_COLORS = new ConcurrentHashMap<>();
    private static final Map<String, Optional<TextColor>> EMEKE_TIER_COLORS = new ConcurrentHashMap<>();

    @Nullable
    public static FactoryTier getEvolvedFactoryTier(String tierName) {
        if (tierName == null) {
            return null;
        }
        return EVOLVED_FACTORY_TIERS
                .computeIfAbsent(tierName, name -> Optional.ofNullable(resolveEvolvedFactoryTier(name)))
                .orElse(null);
    }

    @Nullable
    private static FactoryTier resolveEvolvedFactoryTier(String tierName) {
        if (!hasEvolvedMekanism()) {
            return null;
        }
        try {
            Object value = Class.forName("fr.iglee42.evolvedmekanism.tiers.EMFactoryTier")
                    .getField(tierName.toUpperCase(java.util.Locale.ROOT))
                    .get(null);
            return value instanceof FactoryTier tier ? tier : null;
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }

    /** Resolved EMExtras tiers; only hits are cached, like {@link #hasClassResource}. */
    private static final Map<String, Object> EMEKE_FACTORY_TIERS = new java.util.concurrent.ConcurrentHashMap<>();

    @Nullable
    public static Object getEvolvedMekanismExtrasFactoryTier(String tierName) {
        if (!hasEvolvedMekanismExtras() || tierName == null) {
            return null;
        }
        Object cached = EMEKE_FACTORY_TIERS.get(tierName);
        if (cached != null) {
            return cached;
        }
        try {
            Object tier = Class.forName("io.github.masyumero.emextras.common.tier.EMExtraFactoryTier")
                    .getField(tierName.toUpperCase(java.util.Locale.ROOT))
                    .get(null);
            if (tier != null) {
                EMEKE_FACTORY_TIERS.put(tierName, tier);
            }
            return tier;
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }

    @Nullable
    public static TextColor getMekanismExtrasTierColor(String tierName) {
        if (tierName == null) {
            return null;
        }
        return MEKE_TIER_COLORS
                .computeIfAbsent(tierName, name -> Optional.ofNullable(resolveMekanismExtrasTierColor(name)))
                .orElse(null);
    }

    @Nullable
    private static TextColor resolveMekanismExtrasTierColor(String tierName) {
        if (!hasMekanismExtras()) {
            return null;
        }
        try {
            Object tier = Class.forName("com.jerry.mekextras.common.tier.ExtraFactoryTier")
                    .getField(tierName.toUpperCase(java.util.Locale.ROOT))
                    .get(null);
            return invokeTextColor(invoke(tier, "getAdvanceTier"), "getColor");
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }

    @Nullable
    public static TextColor getEvolvedMekanismExtrasTierColor(String tierName) {
        if (tierName == null) {
            return null;
        }
        return EMEKE_TIER_COLORS
                .computeIfAbsent(tierName, name -> Optional.ofNullable(resolveEvolvedMekanismExtrasTierColor(name)))
                .orElse(null);
    }

    @Nullable
    private static TextColor resolveEvolvedMekanismExtrasTierColor(String tierName) {
        Object tier = getEvolvedMekanismExtrasFactoryTier(tierName);
        return tier == null ? null : invokeTextColor(invoke(tier, "getEMExtraTier"), "getColor");
    }

    /**
     * Parallel operations of a Mekanism Extras factory tier, or 0 when unavailable. Unlike Evolved
     * Mekanism -- whose {@code EMFactoryTier} exposes real {@link FactoryTier} instances -- Extras
     * declares its own enum, so the process count has to be read reflectively.
     */
    public static int getMekanismExtrasFactoryProcesses(String tierName) {
        return hasMekanismExtras()
                ? factoryProcesses("com.jerry.mekextras.common.tier.ExtraFactoryTier", tierName) : 0;
    }

    public static int getEvolvedMekanismExtrasFactoryProcesses(String tierName) {
        return hasEvolvedMekanismExtras()
                ? factoryProcesses("io.github.masyumero.emextras.common.tier.EMExtraFactoryTier", tierName) : 0;
    }

    private static final Map<String, Integer> FACTORY_PROCESSES = new java.util.concurrent.ConcurrentHashMap<>();

    private static int factoryProcesses(String tierClassName, String tierName) {
        if (tierName == null) {
            return 0;
        }
        Integer cached = FACTORY_PROCESSES.get(tierClassName + '#' + tierName);
        if (cached != null) {
            return cached;
        }
        int processes = resolveFactoryProcesses(tierClassName, tierName);
        if (processes > 0) {
            FACTORY_PROCESSES.put(tierClassName + '#' + tierName, processes);
        }
        return processes;
    }

    private static int resolveFactoryProcesses(String tierClassName, String tierName) {
        try {
            Object tier = Class.forName(tierClassName)
                    .getField(tierName.toUpperCase(java.util.Locale.ROOT))
                    .get(null);
            Object processes = tier == null ? null : tier.getClass().getField("processes").get(tier);
            return processes instanceof Integer count ? count : 0;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return 0;
        }
    }

    public static boolean hasMekmmAdvancedFactories() {
        return hasMekmm() && hasClassResource(MEKAF_ITEM_TO_CHEMICAL_FACTORY);
    }

    public static boolean hasMekmmLargeMachines() {
        if (!hasMekmm()) {
            return false;
        }
        for (String classResource : MEKMM_LARGE_MACHINE_CLASSES) {
            if (!hasClassResource(classResource)) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasMekanismExtrasAdvancedFactories() {
        return hasMekanismExtras() && hasMekmmAdvancedFactories() && hasClassResource(MEKE_EXTRA_ADVANCED_FACTORY);
    }

    public static boolean hasMekanismExtrasMoreMachineFactories() {
        return hasMekanismExtras() && hasMekmm() && hasClassResource(MEKE_EXTRA_MORE_MACHINE_FACTORY);
    }

    public static boolean hasEvolvedMekanismExtrasAdvancedFactories() {
        return hasEvolvedMekanismExtras() && hasMekmmAdvancedFactories() && hasClassResource(EMEKE_ADVANCED_FACTORY);
    }

    public static boolean hasEvolvedMekanismExtrasMoreMachineFactories() {
        return hasEvolvedMekanismExtras() && hasMekmm() && hasClassResource(EMEKE_MORE_MACHINE_FACTORY);
    }

    /**
     * Class presence never changes once a class is visible, but a resource lookup walks the whole
     * mod class path. Found classes are remembered; misses are re-probed because very early
     * callers can run before the game class loader sees every mod.
     */
    private static final java.util.Set<String> PRESENT_CLASS_RESOURCES =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static boolean hasClassResource(String path) {
        if (PRESENT_CLASS_RESOURCES.contains(path)) {
            return true;
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = OptionalCompatClasses.class.getClassLoader();
        }
        boolean present = loader.getResource(path) != null;
        if (present) {
            PRESENT_CLASS_RESOURCES.add(path);
        }
        return present;
    }

    @Nullable
    private static Object invoke(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @Nullable
    private static TextColor invokeTextColor(Object target, String methodName) {
        Object value = invoke(target, methodName);
        return value instanceof TextColor color ? color : null;
    }
}
