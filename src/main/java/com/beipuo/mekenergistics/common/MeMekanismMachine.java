package com.beipuo.mekenergistics.common;

import java.util.Locale;
import java.util.function.LongSupplier;
import org.jetbrains.annotations.Nullable;
import mekanism.api.tier.BaseTier;
import mekanism.common.config.MekanismConfig;
import mekanism.common.content.blocktype.FactoryType;
import mekanism.common.tier.FactoryTier;

public enum MeMekanismMachine {
    ENRICHMENT_CHAMBER(FactoryType.ENRICHING, "enrichment_chamber", "ME Enrichment Chamber"),
    CRUSHER(FactoryType.CRUSHING, "crusher", "ME Crusher"),
    ENERGIZED_SMELTER(FactoryType.SMELTING, "energized_smelter", "ME Energized Smelter"),
    PRECISION_SAWMILL(FactoryType.SAWING, "precision_sawmill", "ME Precision Sawmill"),
    OSMIUM_COMPRESSOR(FactoryType.COMPRESSING, "osmium_compressor", "ME Osmium Compressor"),
    COMBINER(FactoryType.COMBINING, "combiner", "ME Combiner"),
    METALLURGIC_INFUSER(FactoryType.INFUSING, "metallurgic_infuser", "ME Metallurgic Infuser"),
    PURIFICATION_CHAMBER(FactoryType.PURIFYING, "purification_chamber", "ME Purification Chamber"),
    CHEMICAL_INJECTION_CHAMBER(FactoryType.INJECTING, "chemical_injection_chamber", "ME Chemical Injection Chamber"),
    PRESSURIZED_REACTION_CHAMBER(null, "pressurized_reaction_chamber", "ME Pressurized Reaction Chamber"),
    CHEMICAL_CRYSTALLIZER(null, "chemical_crystallizer", "ME Chemical Crystallizer"),
    CHEMICAL_DISSOLUTION_CHAMBER(null, "chemical_dissolution_chamber", "ME Chemical Dissolution Chamber"),
    CHEMICAL_INFUSER(null, "chemical_infuser", "ME Chemical Infuser"),
    CHEMICAL_OXIDIZER(null, "chemical_oxidizer", "ME Chemical Oxidizer"),
    CHEMICAL_WASHER(null, "chemical_washer", "ME Chemical Washer"),
    ROTARY_CONDENSENTRATOR(null, "rotary_condensentrator", "ME Rotary Condensentrator"),
    ELECTROLYTIC_SEPARATOR(null, "electrolytic_separator", "ME Electrolytic Separator"),
    DIGITAL_MINER(null, "digital_miner", "ME Digital Miner"),
    FORMULAIC_ASSEMBLICATOR(null, "formulaic_assemblicator", "ME Formulaic Assemblicator"),
    ELECTRIC_PUMP(null, "electric_pump", "ME Electric Pump"),
    FLUIDIC_PLENISHER(null, "fluidic_plenisher", "ME Fluidic Plenisher"),
    SOLAR_NEUTRON_ACTIVATOR(null, "solar_neutron_activator", "ME Solar Neutron Activator"),
    TELEPORTER(null, "teleporter", "ME Teleporter"),
    RESISTIVE_HEATER(null, "resistive_heater", "ME Resistive Heater"),
    SEISMIC_VIBRATOR(null, "seismic_vibrator", "ME Seismic Vibrator"),
    LOGISTICAL_SORTER(null, "logistical_sorter", "ME Logistical Sorter"),
    ISOTOPIC_CENTRIFUGE(null, "isotopic_centrifuge", "ME Isotopic Centrifuge"),
    NUTRITIONAL_LIQUIFIER(null, "nutritional_liquifier", "ME Nutritional Liquifier"),
    ANTIPROTONIC_NUCLEOSYNTHESIZER(null, "antiprotonic_nucleosynthesizer", "ME Antiprotonic Nucleosynthesizer"),
    PIGMENT_EXTRACTOR(null, "pigment_extractor", "ME Pigment Extractor"),
    PIGMENT_MIXER(null, "pigment_mixer", "ME Pigment Mixer"),
    PAINTING_MACHINE(null, "painting_machine", "ME Painting Machine"),
    DIMENSIONAL_STABILIZER(null, "dimensional_stabilizer", "ME Dimensional Stabilizer"),
    OREDICTIONIFICATOR(null, "oredictionificator", "ME Oredictionificator"),
    MODIFICATION_STATION(null, "modification_station", "ME Modification Station"),
    BASIC_SMELTING_FACTORY(FactoryTier.BASIC, FactoryType.SMELTING),
    BASIC_ENRICHING_FACTORY(FactoryTier.BASIC, FactoryType.ENRICHING),
    BASIC_CRUSHING_FACTORY(FactoryTier.BASIC, FactoryType.CRUSHING),
    BASIC_COMPRESSING_FACTORY(FactoryTier.BASIC, FactoryType.COMPRESSING),
    BASIC_COMBINING_FACTORY(FactoryTier.BASIC, FactoryType.COMBINING),
    BASIC_PURIFYING_FACTORY(FactoryTier.BASIC, FactoryType.PURIFYING),
    BASIC_INJECTING_FACTORY(FactoryTier.BASIC, FactoryType.INJECTING),
    BASIC_INFUSING_FACTORY(FactoryTier.BASIC, FactoryType.INFUSING),
    BASIC_SAWING_FACTORY(FactoryTier.BASIC, FactoryType.SAWING),
    ADVANCED_SMELTING_FACTORY(FactoryTier.ADVANCED, FactoryType.SMELTING),
    ADVANCED_ENRICHING_FACTORY(FactoryTier.ADVANCED, FactoryType.ENRICHING),
    ADVANCED_CRUSHING_FACTORY(FactoryTier.ADVANCED, FactoryType.CRUSHING),
    ADVANCED_COMPRESSING_FACTORY(FactoryTier.ADVANCED, FactoryType.COMPRESSING),
    ADVANCED_COMBINING_FACTORY(FactoryTier.ADVANCED, FactoryType.COMBINING),
    ADVANCED_PURIFYING_FACTORY(FactoryTier.ADVANCED, FactoryType.PURIFYING),
    ADVANCED_INJECTING_FACTORY(FactoryTier.ADVANCED, FactoryType.INJECTING),
    ADVANCED_INFUSING_FACTORY(FactoryTier.ADVANCED, FactoryType.INFUSING),
    ADVANCED_SAWING_FACTORY(FactoryTier.ADVANCED, FactoryType.SAWING),
    ELITE_SMELTING_FACTORY(FactoryTier.ELITE, FactoryType.SMELTING),
    ELITE_ENRICHING_FACTORY(FactoryTier.ELITE, FactoryType.ENRICHING),
    ELITE_CRUSHING_FACTORY(FactoryTier.ELITE, FactoryType.CRUSHING),
    ELITE_COMPRESSING_FACTORY(FactoryTier.ELITE, FactoryType.COMPRESSING),
    ELITE_COMBINING_FACTORY(FactoryTier.ELITE, FactoryType.COMBINING),
    ELITE_PURIFYING_FACTORY(FactoryTier.ELITE, FactoryType.PURIFYING),
    ELITE_INJECTING_FACTORY(FactoryTier.ELITE, FactoryType.INJECTING),
    ELITE_INFUSING_FACTORY(FactoryTier.ELITE, FactoryType.INFUSING),
    ELITE_SAWING_FACTORY(FactoryTier.ELITE, FactoryType.SAWING),
    ULTIMATE_SMELTING_FACTORY(FactoryTier.ULTIMATE, FactoryType.SMELTING),
    ULTIMATE_ENRICHING_FACTORY(FactoryTier.ULTIMATE, FactoryType.ENRICHING),
    ULTIMATE_CRUSHING_FACTORY(FactoryTier.ULTIMATE, FactoryType.CRUSHING),
    ULTIMATE_COMPRESSING_FACTORY(FactoryTier.ULTIMATE, FactoryType.COMPRESSING),
    ULTIMATE_COMBINING_FACTORY(FactoryTier.ULTIMATE, FactoryType.COMBINING),
    ULTIMATE_PURIFYING_FACTORY(FactoryTier.ULTIMATE, FactoryType.PURIFYING),
    ULTIMATE_INJECTING_FACTORY(FactoryTier.ULTIMATE, FactoryType.INJECTING),
    ULTIMATE_INFUSING_FACTORY(FactoryTier.ULTIMATE, FactoryType.INFUSING),
    ULTIMATE_SAWING_FACTORY(FactoryTier.ULTIMATE, FactoryType.SAWING);

    @Nullable
    private final FactoryType factoryType;
    @Nullable
    private final FactoryTier factoryTier;
    private final String baseName;
    private final String englishName;

    MeMekanismMachine(@Nullable FactoryType factoryType, String baseName, String englishName) {
        this.factoryType = factoryType;
        this.factoryTier = null;
        this.baseName = baseName;
        this.englishName = englishName;
    }

    MeMekanismMachine(FactoryTier factoryTier, FactoryType factoryType) {
        this.factoryType = factoryType;
        this.factoryTier = factoryTier;
        this.baseName = factoryTier.name().toLowerCase(Locale.ROOT) + "_" + factoryType.getRegistryNameComponent() + "_factory";
        this.englishName = "ME " + capitalize(factoryTier.name()) + " " + factoryType.getRegistryNameComponentCapitalized() + " Factory";
    }

    @Nullable
    public FactoryType factoryType() {
        return factoryType;
    }

    @Nullable
    public FactoryTier factoryTier() {
        return factoryTier;
    }

    public boolean isFactory() {
        return factoryTier != null;
    }

    @Nullable
    public BaseTier baseTier() {
        return factoryTier == null ? null : factoryTier.getBaseTier();
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
        return this.factoryType == FactoryType.COMPRESSING
                || this.factoryType == FactoryType.INFUSING
                || this.factoryType == FactoryType.INJECTING
                || this.factoryType == FactoryType.PURIFYING;
    }

    public boolean hasRecipeLogic() {
        return this.factoryType != null;
    }

    public boolean hasAdvancedChemicalInput() {
        return this.factoryType == FactoryType.COMPRESSING
                || this.factoryType == FactoryType.INJECTING
                || this.factoryType == FactoryType.PURIFYING;
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

    public LongSupplier energyUsage() {
        return switch (this) {
            case ENRICHMENT_CHAMBER -> MekanismConfig.usage.enrichmentChamber;
            case CRUSHER -> MekanismConfig.usage.crusher;
            case ENERGIZED_SMELTER -> MekanismConfig.usage.energizedSmelter;
            case PRECISION_SAWMILL -> MekanismConfig.usage.precisionSawmill;
            case OSMIUM_COMPRESSOR -> MekanismConfig.usage.osmiumCompressor;
            case COMBINER -> MekanismConfig.usage.combiner;
            case METALLURGIC_INFUSER -> MekanismConfig.usage.metallurgicInfuser;
            case PURIFICATION_CHAMBER -> MekanismConfig.usage.purificationChamber;
            case CHEMICAL_INJECTION_CHAMBER -> MekanismConfig.usage.chemicalInjectionChamber;
            default -> () -> 50L;
        };
    }

    public LongSupplier energyStorage() {
        return switch (this) {
            case ENRICHMENT_CHAMBER -> MekanismConfig.storage.enrichmentChamber;
            case CRUSHER -> MekanismConfig.storage.crusher;
            case ENERGIZED_SMELTER -> MekanismConfig.storage.energizedSmelter;
            case PRECISION_SAWMILL -> MekanismConfig.storage.precisionSawmill;
            case OSMIUM_COMPRESSOR -> MekanismConfig.storage.osmiumCompressor;
            case COMBINER -> MekanismConfig.storage.combiner;
            case METALLURGIC_INFUSER -> MekanismConfig.storage.metallurgicInfuser;
            case PURIFICATION_CHAMBER -> MekanismConfig.storage.purificationChamber;
            case CHEMICAL_INJECTION_CHAMBER -> MekanismConfig.storage.chemicalInjectionChamber;
            default -> () -> 2_000_000L;
        };
    }

    @Nullable
    public MeMekanismMachine getBasicFactory() {
        return factoryType == null ? null : getFactory(FactoryTier.BASIC, factoryType);
    }

    @Nullable
    public MeMekanismMachine getNextFactory() {
        if (factoryTier == null || factoryTier == FactoryTier.ULTIMATE || factoryType == null) {
            return null;
        }
        return getFactory(FactoryTier.values()[factoryTier.ordinal() + 1], factoryType);
    }

    @Nullable
    public static MeMekanismMachine getBaseMachine(FactoryType type) {
        for (MeMekanismMachine machine : values()) {
            if (!machine.isFactory() && machine.factoryType == type) {
                return machine;
            }
        }
        return null;
    }

    @Nullable
    public static MeMekanismMachine getFactory(FactoryTier tier, FactoryType type) {
        for (MeMekanismMachine machine : values()) {
            if (machine.factoryTier == tier && machine.factoryType == type) {
                return machine;
            }
        }
        return null;
    }

    private static String capitalize(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.substring(0, 1).toUpperCase(Locale.ROOT) + lower.substring(1);
    }

    public enum SlotLayout {
        SINGLE_ITEM,
        ITEM_CHEMICAL,
        DOUBLE_ITEM,
        SAWING
    }
}
