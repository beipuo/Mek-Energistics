package com.beipuo.mekenergistics.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class MekEnergisticsConfig {
    public static final int PATTERN_SLOT_COLUMNS = 9;
    public static final int PATTERN_SLOT_ROWS = 4;
    public static final int PATTERN_SLOTS_PER_PAGE = PATTERN_SLOT_COLUMNS * PATTERN_SLOT_ROWS;

    private static final int DEFAULT_PATTERN_PAGES = 2;
    private static final int MIN_PATTERN_PAGES = 1;
    private static final int MAX_PATTERN_PAGES = 16;

    private static ModConfigSpec.IntValue patternPages;
    private static ModConfigSpec.BooleanValue preferAppliedFluxNetworkFe;
    private static ModConfigSpec.BooleanValue preferNetworkEnergy;
    private static ModConfigSpec.BooleanValue smartPatternMultiplicationDefault;
    private static ModConfigSpec serverSpec;

    private static ModConfigSpec.BooleanValue hideJeiMachineVariants;
    private static ModConfigSpec clientSpec;

    private MekEnergisticsConfig() {
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, buildServerSpec());
        container.registerConfig(ModConfig.Type.CLIENT, buildClientSpec());
    }

    /**
     * Values that decide machine container structure and energy routing. These must be identical on
     * both sides of a connection: {@link #patternSlots()} feeds inventory slot indices, so a client
     * disagreeing with the server would mis-index every slot interaction. SERVER configs are synced
     * to connected clients, COMMON ones are not.
     */
    private static ModConfigSpec buildServerSpec() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        patternPages = builder
                .comment("Number of 4x9 encoded pattern pages available in each ME machine.",
                        "Default: 2 pages, 72 pattern slots.",
                        "Reducing this value hides higher slots, but their saved NBT remains on the machine.")
                .defineInRange("patternPages", DEFAULT_PATTERN_PAGES, MIN_PATTERN_PAGES, MAX_PATTERN_PAGES);
        preferAppliedFluxNetworkFe = builder
                .comment("Prefer FE stored in Applied Flux cells when an ME machine is connected to an AE network.",
                        "When enabled, Applied Flux FE is drained before AE network energy.",
                        "When disabled, AE network energy is drained before Applied Flux FE.")
                .define("preferAppliedFluxNetworkFe", false);
        preferNetworkEnergy = builder
                .comment("Prefer network energy when an ME machine is connected to an AE network.",
                        "When enabled, AE/Applied Flux energy is drained before the machine's local FE buffer.",
                        "When disabled, the local FE buffer is drained before network energy.")
                .define("preferNetworkEnergy", true);
        smartPatternMultiplicationDefault = builder
                .comment("Enable smart pattern multiplication by default for newly created machines.",
                        "A machine's saved setting always takes priority over this default.")
                .define("smartPatternMultiplicationDefault", false);
        serverSpec = builder.build();
        return serverSpec;
    }

    private static ModConfigSpec buildClientSpec() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        hideJeiMachineVariants = builder
                .comment("Hide redundant ME machine variants from JEI.",
                        "When enabled, JEI keeps the basic ME machine as the recipe catalyst and hides ME factory variants from the item list.")
                .define("hideJeiMachineVariants", true);
        clientSpec = builder.build();
        return clientSpec;
    }

    private static boolean serverReady() {
        return serverSpec != null && serverSpec.isLoaded();
    }

    private static boolean clientReady() {
        return clientSpec != null && clientSpec.isLoaded();
    }

    public static int patternPages() {
        return serverReady() ? patternPages.get() : DEFAULT_PATTERN_PAGES;
    }

    public static int patternSlots() {
        return patternPages() * PATTERN_SLOTS_PER_PAGE;
    }

    public static boolean hideJeiMachineVariants() {
        return !clientReady() || hideJeiMachineVariants.get();
    }

    public static boolean preferAppliedFluxNetworkFe() {
        return serverReady() && preferAppliedFluxNetworkFe.get();
    }

    public static boolean preferNetworkEnergy() {
        return !serverReady() || preferNetworkEnergy.get();
    }

    public static boolean smartPatternMultiplicationDefault() {
        return serverReady() && smartPatternMultiplicationDefault.get();
    }
}
