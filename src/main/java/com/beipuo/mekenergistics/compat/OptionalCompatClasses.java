package com.beipuo.mekenergistics.compat;

import net.neoforged.fml.ModList;
import mekanism.common.tier.FactoryTier;
import org.jetbrains.annotations.Nullable;

public final class OptionalCompatClasses {
    private static final String MEKAF_ITEM_TO_CHEMICAL_FACTORY =
            "com/jerry/mekaf/common/tile/factory/TileEntityItemStackToChemicalStackFactory.class";
    private static final String MEKE_EXTRA_ADVANCED_FACTORY =
            "com/jerry/mekextras/common/integration/mekaf/tile/factory/base/TileEntityExtraAdvancedFactoryBase.class";
    private static final String MEKE_EXTRA_MORE_MACHINE_FACTORY =
            "com/jerry/mekextras/common/integration/mekmm/tile/factory/TileEntityExtraMoreMachineFactory.class";

    private OptionalCompatClasses() {
    }

    public static boolean hasMekmm() {
        return ModList.get().isLoaded("mekmm");
    }

    public static boolean hasMekanismExtras() {
        return ModList.get().isLoaded("mekanism_extras");
    }

    public static boolean hasEvolvedMekanism() {
        return ModList.get().isLoaded("evolvedmekanism");
    }

    public static boolean hasEvolvedMekanismExtras() {
        return ModList.get().isLoaded("emextras");
    }

    @Nullable
    public static FactoryTier getEvolvedFactoryTier(String tierName) {
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

    @Nullable
    public static Object getEvolvedMekanismExtrasFactoryTier(String tierName) {
        if (!hasEvolvedMekanismExtras()) {
            return null;
        }
        try {
            return Class.forName("io.github.masyumero.emextras.common.tier.EMExtraFactoryTier")
                    .getField(tierName.toUpperCase(java.util.Locale.ROOT))
                    .get(null);
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }

    public static boolean hasMekmmAdvancedFactories() {
        return hasMekmm() && hasClassResource(MEKAF_ITEM_TO_CHEMICAL_FACTORY);
    }

    public static boolean hasMekanismExtrasAdvancedFactories() {
        return hasMekanismExtras() && hasMekmmAdvancedFactories() && hasClassResource(MEKE_EXTRA_ADVANCED_FACTORY);
    }

    public static boolean hasMekanismExtrasMoreMachineFactories() {
        return hasMekanismExtras() && hasMekmm() && hasClassResource(MEKE_EXTRA_MORE_MACHINE_FACTORY);
    }

    private static boolean hasClassResource(String path) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = OptionalCompatClasses.class.getClassLoader();
        }
        return loader.getResource(path) != null;
    }
}
