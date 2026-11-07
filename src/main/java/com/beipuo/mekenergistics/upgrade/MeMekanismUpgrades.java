package com.beipuo.mekenergistics.upgrade;

import mekanism.api.Upgrade;

/** Access to the three Mekanism upgrade enum entries owned by this mod. */
public final class MeMekanismUpgrades {
    public static final String PATTERN_PROVIDER_NAME = "me_pattern_provider";
    public static final String PASSIVE_CRAFTING_NAME = "me_passive_crafting";
    public static final String OUTPUT_INTERFACE_NAME = "me_output_interface";

    private MeMekanismUpgrades() {
    }

    /*
     * The three entries are appended to Mekanism's Upgrade enum by a mixin during Upgrade's static
     * initializer, so they exist as soon as Upgrade is usable. Resolving them by name on every call
     * was a hash lookup on hot paths (upgrade counts are read several times per machine per tick);
     * resolve once and keep the constants.
     */
    private static volatile Upgrade patternProvider;
    private static volatile Upgrade passiveCrafting;
    private static volatile Upgrade outputInterface;

    public static Upgrade patternProvider() {
        Upgrade upgrade = patternProvider;
        if (upgrade == null) {
            patternProvider = upgrade = Upgrade.valueOf("ME_PATTERN_PROVIDER");
        }
        return upgrade;
    }

    public static Upgrade passiveCrafting() {
        Upgrade upgrade = passiveCrafting;
        if (upgrade == null) {
            passiveCrafting = upgrade = Upgrade.valueOf("ME_PASSIVE_CRAFTING");
        }
        return upgrade;
    }

    public static Upgrade outputInterface() {
        Upgrade upgrade = outputInterface;
        if (upgrade == null) {
            outputInterface = upgrade = Upgrade.valueOf("ME_OUTPUT_INTERFACE");
        }
        return upgrade;
    }

    public static Upgrade forType(MeUpgradeType type) {
        return switch (type) {
            case PATTERN_PROVIDER -> patternProvider();
            case PASSIVE_CRAFTING -> passiveCrafting();
            case OUTPUT_INTERFACE -> outputInterface();
        };
    }

    public static MeUpgradeType toType(Upgrade upgrade) {
        if (upgrade == patternProvider()) {
            return MeUpgradeType.PATTERN_PROVIDER;
        }
        if (upgrade == passiveCrafting()) {
            return MeUpgradeType.PASSIVE_CRAFTING;
        }
        if (upgrade == outputInterface()) {
            return MeUpgradeType.OUTPUT_INTERFACE;
        }
        return null;
    }
}
