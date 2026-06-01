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
    private static ModConfigSpec spec;

    private MekEnergisticsConfig() {
    }

    public static void register(ModContainer container) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        patternPages = builder
                .comment("Number of 4x9 encoded pattern pages available in each ME machine.",
                        "Default: 2 pages, 72 pattern slots.",
                        "Reducing this value hides higher slots, but their saved NBT remains on the machine.")
                .defineInRange("patternPages", DEFAULT_PATTERN_PAGES, MIN_PATTERN_PAGES, MAX_PATTERN_PAGES);
        spec = builder.build();
        container.registerConfig(ModConfig.Type.COMMON, spec);
    }

    public static int patternPages() {
        return patternPages == null ? DEFAULT_PATTERN_PAGES : patternPages.get();
    }

    public static int patternSlots() {
        return patternPages() * PATTERN_SLOTS_PER_PAGE;
    }
}
