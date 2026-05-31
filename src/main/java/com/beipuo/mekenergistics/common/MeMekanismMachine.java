package com.beipuo.mekenergistics.common;

import java.util.Locale;
import mekanism.common.content.blocktype.FactoryType;

public enum MeMekanismMachine {
    ENRICHMENT_CHAMBER(FactoryType.ENRICHING, "enrichment_chamber", "ME Enrichment Chamber"),
    CRUSHER(FactoryType.CRUSHING, "crusher", "ME Crusher"),
    ENERGIZED_SMELTER(FactoryType.SMELTING, "energized_smelter", "ME Energized Smelter"),
    PRECISION_SAWMILL(FactoryType.SAWING, "precision_sawmill", "ME Precision Sawmill"),
    OSMIUM_COMPRESSOR(FactoryType.COMPRESSING, "osmium_compressor", "ME Osmium Compressor"),
    COMBINER(FactoryType.COMBINING, "combiner", "ME Combiner"),
    METALLURGIC_INFUSER(FactoryType.INFUSING, "metallurgic_infuser", "ME Metallurgic Infuser"),
    PURIFICATION_CHAMBER(FactoryType.PURIFYING, "purification_chamber", "ME Purification Chamber"),
    CHEMICAL_INJECTION_CHAMBER(FactoryType.INJECTING, "chemical_injection_chamber", "ME Chemical Injection Chamber");

    private final FactoryType factoryType;
    private final String baseName;
    private final String englishName;

    MeMekanismMachine(FactoryType factoryType, String baseName, String englishName) {
        this.factoryType = factoryType;
        this.baseName = baseName;
        this.englishName = englishName;
    }

    public FactoryType factoryType() {
        return factoryType;
    }

    public String baseName() {
        return baseName;
    }

    public String registryName() {
        return "me_" + baseName;
    }

    public String englishName() {
        return englishName;
    }

    public String translationKey() {
        return "block.mekenergistics." + registryName();
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean hasSecondaryItemInput() {
        return this.factoryType == FactoryType.COMBINING;
    }

    public boolean hasChemicalInput() {
        return switch (this.factoryType) {
            case COMPRESSING, INFUSING, INJECTING, PURIFYING -> true;
            default -> false;
        };
    }

    public boolean hasSecondaryOutput() {
        return this.factoryType == FactoryType.SAWING;
    }

    public SlotLayout slotLayout() {
        if (hasSecondaryItemInput()) {
            return SlotLayout.DOUBLE_ITEM;
        }
        if (hasChemicalInput()) {
            return SlotLayout.ITEM_CHEMICAL;
        }
        if (hasSecondaryOutput()) {
            return SlotLayout.SAWING;
        }
        return SlotLayout.SINGLE_ITEM;
    }

    public enum SlotLayout {
        SINGLE_ITEM,
        ITEM_CHEMICAL,
        DOUBLE_ITEM,
        SAWING
    }
}
