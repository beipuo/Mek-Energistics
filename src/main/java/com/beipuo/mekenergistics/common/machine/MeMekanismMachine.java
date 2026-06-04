package com.beipuo.mekenergistics.common.machine;

import java.util.Locale;
import java.util.function.LongSupplier;
import org.jetbrains.annotations.Nullable;
import com.beipuo.mekenergistics.compat.OptionalCompatClasses;
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
    PRESSURIZED_REACTION_CHAMBER((FactoryType) null, "pressurized_reaction_chamber", "ME Pressurized Reaction Chamber"),
    CHEMICAL_CRYSTALLIZER((FactoryType) null, "chemical_crystallizer", "ME Chemical Crystallizer"),
    CHEMICAL_DISSOLUTION_CHAMBER((FactoryType) null, "chemical_dissolution_chamber", "ME Chemical Dissolution Chamber"),
    CHEMICAL_INFUSER((FactoryType) null, "chemical_infuser", "ME Chemical Infuser"),
    CHEMICAL_OXIDIZER((FactoryType) null, "chemical_oxidizer", "ME Chemical Oxidizer"),
    CHEMICAL_WASHER((FactoryType) null, "chemical_washer", "ME Chemical Washer"),
    ROTARY_CONDENSENTRATOR((FactoryType) null, "rotary_condensentrator", "ME Rotary Condensentrator"),
    ELECTROLYTIC_SEPARATOR((FactoryType) null, "electrolytic_separator", "ME Electrolytic Separator"),
    DIGITAL_MINER((FactoryType) null, "digital_miner", "ME Digital Miner"),
    FORMULAIC_ASSEMBLICATOR((FactoryType) null, "formulaic_assemblicator", "ME Formulaic Assemblicator"),
    ELECTRIC_PUMP((FactoryType) null, "electric_pump", "ME Electric Pump"),
    FLUIDIC_PLENISHER((FactoryType) null, "fluidic_plenisher", "ME Fluidic Plenisher"),
    SOLAR_NEUTRON_ACTIVATOR((FactoryType) null, "solar_neutron_activator", "ME Solar Neutron Activator"),
    TELEPORTER((FactoryType) null, "teleporter", "ME Teleporter"),
    RESISTIVE_HEATER((FactoryType) null, "resistive_heater", "ME Resistive Heater"),
    SEISMIC_VIBRATOR((FactoryType) null, "seismic_vibrator", "ME Seismic Vibrator"),
    LOGISTICAL_SORTER((FactoryType) null, "logistical_sorter", "ME Logistical Sorter"),
    ISOTOPIC_CENTRIFUGE((FactoryType) null, "isotopic_centrifuge", "ME Isotopic Centrifuge"),
    NUTRITIONAL_LIQUIFIER((FactoryType) null, "nutritional_liquifier", "ME Nutritional Liquifier"),
    ANTIPROTONIC_NUCLEOSYNTHESIZER((FactoryType) null, "antiprotonic_nucleosynthesizer", "ME Antiprotonic Nucleosynthesizer"),
    PIGMENT_EXTRACTOR((FactoryType) null, "pigment_extractor", "ME Pigment Extractor"),
    PIGMENT_MIXER((FactoryType) null, "pigment_mixer", "ME Pigment Mixer"),
    PAINTING_MACHINE((FactoryType) null, "painting_machine", "ME Painting Machine"),
    DIMENSIONAL_STABILIZER((FactoryType) null, "dimensional_stabilizer", "ME Dimensional Stabilizer"),
    OREDICTIONIFICATOR((FactoryType) null, "oredictionificator", "ME Oredictionificator"),
    MODIFICATION_STATION((FactoryType) null, "modification_station", "ME Modification Station"),
    RECYCLER("recycler", "recycling", "ME Recycler"),
    CNC_STAMPER("cnc_stamper", "stamping", "ME CNC Stamper"),
    CNC_LATHE("cnc_lathe", "lathing", "ME CNC Lathe"),
    CNC_ROLLING_MILL("cnc_rolling_mill", "rolling_mill", "ME CNC Rolling Mill"),
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
    ULTIMATE_SAWING_FACTORY(FactoryTier.ULTIMATE, FactoryType.SAWING),
    ABSOLUTE_SMELTING_FACTORY("absolute", FactoryType.SMELTING),
    ABSOLUTE_ENRICHING_FACTORY("absolute", FactoryType.ENRICHING),
    ABSOLUTE_CRUSHING_FACTORY("absolute", FactoryType.CRUSHING),
    ABSOLUTE_COMPRESSING_FACTORY("absolute", FactoryType.COMPRESSING),
    ABSOLUTE_COMBINING_FACTORY("absolute", FactoryType.COMBINING),
    ABSOLUTE_PURIFYING_FACTORY("absolute", FactoryType.PURIFYING),
    ABSOLUTE_INJECTING_FACTORY("absolute", FactoryType.INJECTING),
    ABSOLUTE_INFUSING_FACTORY("absolute", FactoryType.INFUSING),
    ABSOLUTE_SAWING_FACTORY("absolute", FactoryType.SAWING),
    SUPREME_SMELTING_FACTORY("supreme", FactoryType.SMELTING),
    SUPREME_ENRICHING_FACTORY("supreme", FactoryType.ENRICHING),
    SUPREME_CRUSHING_FACTORY("supreme", FactoryType.CRUSHING),
    SUPREME_COMPRESSING_FACTORY("supreme", FactoryType.COMPRESSING),
    SUPREME_COMBINING_FACTORY("supreme", FactoryType.COMBINING),
    SUPREME_PURIFYING_FACTORY("supreme", FactoryType.PURIFYING),
    SUPREME_INJECTING_FACTORY("supreme", FactoryType.INJECTING),
    SUPREME_INFUSING_FACTORY("supreme", FactoryType.INFUSING),
    SUPREME_SAWING_FACTORY("supreme", FactoryType.SAWING),
    COSMIC_SMELTING_FACTORY("cosmic", FactoryType.SMELTING),
    COSMIC_ENRICHING_FACTORY("cosmic", FactoryType.ENRICHING),
    COSMIC_CRUSHING_FACTORY("cosmic", FactoryType.CRUSHING),
    COSMIC_COMPRESSING_FACTORY("cosmic", FactoryType.COMPRESSING),
    COSMIC_COMBINING_FACTORY("cosmic", FactoryType.COMBINING),
    COSMIC_PURIFYING_FACTORY("cosmic", FactoryType.PURIFYING),
    COSMIC_INJECTING_FACTORY("cosmic", FactoryType.INJECTING),
    COSMIC_INFUSING_FACTORY("cosmic", FactoryType.INFUSING),
    COSMIC_SAWING_FACTORY("cosmic", FactoryType.SAWING),
    INFINITE_SMELTING_FACTORY("infinite", FactoryType.SMELTING),
    INFINITE_ENRICHING_FACTORY("infinite", FactoryType.ENRICHING),
    INFINITE_CRUSHING_FACTORY("infinite", FactoryType.CRUSHING),
    INFINITE_COMPRESSING_FACTORY("infinite", FactoryType.COMPRESSING),
    INFINITE_COMBINING_FACTORY("infinite", FactoryType.COMBINING),
    INFINITE_PURIFYING_FACTORY("infinite", FactoryType.PURIFYING),
    INFINITE_INJECTING_FACTORY("infinite", FactoryType.INJECTING),
    INFINITE_INFUSING_FACTORY("infinite", FactoryType.INFUSING),
    INFINITE_SAWING_FACTORY("infinite", FactoryType.SAWING),
    BASIC_RECYCLING_FACTORY(FactoryTier.BASIC, "recycling", "Recycling"),
    BASIC_PLANTING_FACTORY(FactoryTier.BASIC, "planting", "Planting"),
    BASIC_STAMPING_FACTORY(FactoryTier.BASIC, "stamping", "Stamping"),
    BASIC_LATHING_FACTORY(FactoryTier.BASIC, "lathing", "Lathing"),
    BASIC_ROLLING_MILL_FACTORY(FactoryTier.BASIC, "rolling_mill", "Rolling Mill"),
    BASIC_REPLICATING_FACTORY(FactoryTier.BASIC, "replicating", "Replicating"),
    ADVANCED_RECYCLING_FACTORY(FactoryTier.ADVANCED, "recycling", "Recycling"),
    ADVANCED_PLANTING_FACTORY(FactoryTier.ADVANCED, "planting", "Planting"),
    ADVANCED_STAMPING_FACTORY(FactoryTier.ADVANCED, "stamping", "Stamping"),
    ADVANCED_LATHING_FACTORY(FactoryTier.ADVANCED, "lathing", "Lathing"),
    ADVANCED_ROLLING_MILL_FACTORY(FactoryTier.ADVANCED, "rolling_mill", "Rolling Mill"),
    ADVANCED_REPLICATING_FACTORY(FactoryTier.ADVANCED, "replicating", "Replicating"),
    ELITE_RECYCLING_FACTORY(FactoryTier.ELITE, "recycling", "Recycling"),
    ELITE_PLANTING_FACTORY(FactoryTier.ELITE, "planting", "Planting"),
    ELITE_STAMPING_FACTORY(FactoryTier.ELITE, "stamping", "Stamping"),
    ELITE_LATHING_FACTORY(FactoryTier.ELITE, "lathing", "Lathing"),
    ELITE_ROLLING_MILL_FACTORY(FactoryTier.ELITE, "rolling_mill", "Rolling Mill"),
    ELITE_REPLICATING_FACTORY(FactoryTier.ELITE, "replicating", "Replicating"),
    ULTIMATE_RECYCLING_FACTORY(FactoryTier.ULTIMATE, "recycling", "Recycling"),
    ULTIMATE_PLANTING_FACTORY(FactoryTier.ULTIMATE, "planting", "Planting"),
    ULTIMATE_STAMPING_FACTORY(FactoryTier.ULTIMATE, "stamping", "Stamping"),
    ULTIMATE_LATHING_FACTORY(FactoryTier.ULTIMATE, "lathing", "Lathing"),
    ULTIMATE_ROLLING_MILL_FACTORY(FactoryTier.ULTIMATE, "rolling_mill", "Rolling Mill"),
    ULTIMATE_REPLICATING_FACTORY(FactoryTier.ULTIMATE, "replicating", "Replicating"),
    ABSOLUTE_RECYCLING_FACTORY("absolute", "recycling", "Recycling", false, false),
    ABSOLUTE_PLANTING_FACTORY("absolute", "planting", "Planting", false, false),
    ABSOLUTE_STAMPING_FACTORY("absolute", "stamping", "Stamping", false, false),
    ABSOLUTE_LATHING_FACTORY("absolute", "lathing", "Lathing", false, false),
    ABSOLUTE_ROLLING_MILL_FACTORY("absolute", "rolling_mill", "Rolling Mill", false, false),
    ABSOLUTE_REPLICATING_FACTORY("absolute", "replicating", "Replicating", false, false),
    SUPREME_RECYCLING_FACTORY("supreme", "recycling", "Recycling", false, false),
    SUPREME_PLANTING_FACTORY("supreme", "planting", "Planting", false, false),
    SUPREME_STAMPING_FACTORY("supreme", "stamping", "Stamping", false, false),
    SUPREME_LATHING_FACTORY("supreme", "lathing", "Lathing", false, false),
    SUPREME_ROLLING_MILL_FACTORY("supreme", "rolling_mill", "Rolling Mill", false, false),
    SUPREME_REPLICATING_FACTORY("supreme", "replicating", "Replicating", false, false),
    COSMIC_RECYCLING_FACTORY("cosmic", "recycling", "Recycling", false, false),
    COSMIC_PLANTING_FACTORY("cosmic", "planting", "Planting", false, false),
    COSMIC_STAMPING_FACTORY("cosmic", "stamping", "Stamping", false, false),
    COSMIC_LATHING_FACTORY("cosmic", "lathing", "Lathing", false, false),
    COSMIC_ROLLING_MILL_FACTORY("cosmic", "rolling_mill", "Rolling Mill", false, false),
    COSMIC_REPLICATING_FACTORY("cosmic", "replicating", "Replicating", false, false),
    INFINITE_RECYCLING_FACTORY("infinite", "recycling", "Recycling", false, false),
    INFINITE_PLANTING_FACTORY("infinite", "planting", "Planting", false, false),
    INFINITE_STAMPING_FACTORY("infinite", "stamping", "Stamping", false, false),
    INFINITE_LATHING_FACTORY("infinite", "lathing", "Lathing", false, false),
    INFINITE_ROLLING_MILL_FACTORY("infinite", "rolling_mill", "Rolling Mill", false, false),
    INFINITE_REPLICATING_FACTORY("infinite", "replicating", "Replicating", false, false),
    BASIC_OXIDIZING_FACTORY(FactoryTier.BASIC, "oxidizing", "Oxidizing", true),
    BASIC_DISSOLVING_FACTORY(FactoryTier.BASIC, "dissolving", "Dissolving", true),
    BASIC_WASHING_FACTORY(FactoryTier.BASIC, "washing", "Washing", true),
    BASIC_CRYSTALLIZING_FACTORY(FactoryTier.BASIC, "crystallizing", "Crystallizing", true),
    BASIC_PRESSURISED_REACTING_FACTORY(FactoryTier.BASIC, "pressurised_reacting", "Pressurised Reacting", true),
    BASIC_CENTRIFUGING_FACTORY(FactoryTier.BASIC, "centrifuging", "Centrifuging", true),
    BASIC_LIQUIFYING_FACTORY(FactoryTier.BASIC, "liquifying", "Liquifying", true),
    BASIC_PIGMENT_EXTRACTING_FACTORY(FactoryTier.BASIC, "pigment_extracting", "Pigment Extracting", true),
    BASIC_PAINTING_FACTORY(FactoryTier.BASIC, "painting", "Painting", true),
    ADVANCED_OXIDIZING_FACTORY(FactoryTier.ADVANCED, "oxidizing", "Oxidizing", true),
    ADVANCED_DISSOLVING_FACTORY(FactoryTier.ADVANCED, "dissolving", "Dissolving", true),
    ADVANCED_WASHING_FACTORY(FactoryTier.ADVANCED, "washing", "Washing", true),
    ADVANCED_CRYSTALLIZING_FACTORY(FactoryTier.ADVANCED, "crystallizing", "Crystallizing", true),
    ADVANCED_PRESSURISED_REACTING_FACTORY(FactoryTier.ADVANCED, "pressurised_reacting", "Pressurised Reacting", true),
    ADVANCED_CENTRIFUGING_FACTORY(FactoryTier.ADVANCED, "centrifuging", "Centrifuging", true),
    ADVANCED_LIQUIFYING_FACTORY(FactoryTier.ADVANCED, "liquifying", "Liquifying", true),
    ADVANCED_PIGMENT_EXTRACTING_FACTORY(FactoryTier.ADVANCED, "pigment_extracting", "Pigment Extracting", true),
    ADVANCED_PAINTING_FACTORY(FactoryTier.ADVANCED, "painting", "Painting", true),
    ELITE_OXIDIZING_FACTORY(FactoryTier.ELITE, "oxidizing", "Oxidizing", true),
    ELITE_DISSOLVING_FACTORY(FactoryTier.ELITE, "dissolving", "Dissolving", true),
    ELITE_WASHING_FACTORY(FactoryTier.ELITE, "washing", "Washing", true),
    ELITE_CRYSTALLIZING_FACTORY(FactoryTier.ELITE, "crystallizing", "Crystallizing", true),
    ELITE_PRESSURISED_REACTING_FACTORY(FactoryTier.ELITE, "pressurised_reacting", "Pressurised Reacting", true),
    ELITE_CENTRIFUGING_FACTORY(FactoryTier.ELITE, "centrifuging", "Centrifuging", true),
    ELITE_LIQUIFYING_FACTORY(FactoryTier.ELITE, "liquifying", "Liquifying", true),
    ELITE_PIGMENT_EXTRACTING_FACTORY(FactoryTier.ELITE, "pigment_extracting", "Pigment Extracting", true),
    ELITE_PAINTING_FACTORY(FactoryTier.ELITE, "painting", "Painting", true),
    ULTIMATE_OXIDIZING_FACTORY(FactoryTier.ULTIMATE, "oxidizing", "Oxidizing", true),
    ULTIMATE_DISSOLVING_FACTORY(FactoryTier.ULTIMATE, "dissolving", "Dissolving", true),
    ULTIMATE_WASHING_FACTORY(FactoryTier.ULTIMATE, "washing", "Washing", true),
    ULTIMATE_CRYSTALLIZING_FACTORY(FactoryTier.ULTIMATE, "crystallizing", "Crystallizing", true),
    ULTIMATE_PRESSURISED_REACTING_FACTORY(FactoryTier.ULTIMATE, "pressurised_reacting", "Pressurised Reacting", true),
    ULTIMATE_CENTRIFUGING_FACTORY(FactoryTier.ULTIMATE, "centrifuging", "Centrifuging", true),
    ULTIMATE_LIQUIFYING_FACTORY(FactoryTier.ULTIMATE, "liquifying", "Liquifying", true),
    ULTIMATE_PIGMENT_EXTRACTING_FACTORY(FactoryTier.ULTIMATE, "pigment_extracting", "Pigment Extracting", true),
    ULTIMATE_PAINTING_FACTORY(FactoryTier.ULTIMATE, "painting", "Painting", true),
    ABSOLUTE_OXIDIZING_FACTORY("absolute", "oxidizing", "Oxidizing", true),
    ABSOLUTE_DISSOLVING_FACTORY("absolute", "dissolving", "Dissolving", true),
    ABSOLUTE_WASHING_FACTORY("absolute", "washing", "Washing", true),
    ABSOLUTE_CRYSTALLIZING_FACTORY("absolute", "crystallizing", "Crystallizing", true),
    ABSOLUTE_PRESSURISED_REACTING_FACTORY("absolute", "pressurised_reacting", "Pressurised Reacting", true),
    ABSOLUTE_CENTRIFUGING_FACTORY("absolute", "centrifuging", "Centrifuging", true),
    ABSOLUTE_LIQUIFYING_FACTORY("absolute", "liquifying", "Liquifying", true),
    ABSOLUTE_PIGMENT_EXTRACTING_FACTORY("absolute", "pigment_extracting", "Pigment Extracting", true),
    ABSOLUTE_PAINTING_FACTORY("absolute", "painting", "Painting", true),
    SUPREME_OXIDIZING_FACTORY("supreme", "oxidizing", "Oxidizing", true),
    SUPREME_DISSOLVING_FACTORY("supreme", "dissolving", "Dissolving", true),
    SUPREME_WASHING_FACTORY("supreme", "washing", "Washing", true),
    SUPREME_CRYSTALLIZING_FACTORY("supreme", "crystallizing", "Crystallizing", true),
    SUPREME_PRESSURISED_REACTING_FACTORY("supreme", "pressurised_reacting", "Pressurised Reacting", true),
    SUPREME_CENTRIFUGING_FACTORY("supreme", "centrifuging", "Centrifuging", true),
    SUPREME_LIQUIFYING_FACTORY("supreme", "liquifying", "Liquifying", true),
    SUPREME_PIGMENT_EXTRACTING_FACTORY("supreme", "pigment_extracting", "Pigment Extracting", true),
    SUPREME_PAINTING_FACTORY("supreme", "painting", "Painting", true),
    COSMIC_OXIDIZING_FACTORY("cosmic", "oxidizing", "Oxidizing", true),
    COSMIC_DISSOLVING_FACTORY("cosmic", "dissolving", "Dissolving", true),
    COSMIC_WASHING_FACTORY("cosmic", "washing", "Washing", true),
    COSMIC_CRYSTALLIZING_FACTORY("cosmic", "crystallizing", "Crystallizing", true),
    COSMIC_PRESSURISED_REACTING_FACTORY("cosmic", "pressurised_reacting", "Pressurised Reacting", true),
    COSMIC_CENTRIFUGING_FACTORY("cosmic", "centrifuging", "Centrifuging", true),
    COSMIC_LIQUIFYING_FACTORY("cosmic", "liquifying", "Liquifying", true),
    COSMIC_PIGMENT_EXTRACTING_FACTORY("cosmic", "pigment_extracting", "Pigment Extracting", true),
    COSMIC_PAINTING_FACTORY("cosmic", "painting", "Painting", true),
    INFINITE_OXIDIZING_FACTORY("infinite", "oxidizing", "Oxidizing", true),
    INFINITE_DISSOLVING_FACTORY("infinite", "dissolving", "Dissolving", true),
    INFINITE_WASHING_FACTORY("infinite", "washing", "Washing", true),
    INFINITE_CRYSTALLIZING_FACTORY("infinite", "crystallizing", "Crystallizing", true),
    INFINITE_PRESSURISED_REACTING_FACTORY("infinite", "pressurised_reacting", "Pressurised Reacting", true),
    INFINITE_CENTRIFUGING_FACTORY("infinite", "centrifuging", "Centrifuging", true),
    INFINITE_LIQUIFYING_FACTORY("infinite", "liquifying", "Liquifying", true),
    INFINITE_PIGMENT_EXTRACTING_FACTORY("infinite", "pigment_extracting", "Pigment Extracting", true),
    INFINITE_PAINTING_FACTORY("infinite", "painting", "Painting", true);

    @Nullable
    private final FactoryType factoryType;
    @Nullable
    private final FactoryTier factoryTier;
    @Nullable
    private final String extraFactoryTierName;
    @Nullable
    private final String moreMachineFactoryTypeName;
    @Nullable
    private final String moreMachineAdvancedFactoryTypeName;
    @Nullable
    private final String moreMachineBaseTypeName;
    @Nullable
    private final String requiredModId;
    private final String baseName;
    private final String englishName;

    MeMekanismMachine(@Nullable FactoryType factoryType, String baseName, String englishName) {
        this.factoryType = factoryType;
        this.factoryTier = null;
        this.extraFactoryTierName = null;
        this.moreMachineFactoryTypeName = null;
        this.moreMachineAdvancedFactoryTypeName = null;
        this.moreMachineBaseTypeName = null;
        this.requiredModId = null;
        this.baseName = baseName;
        this.englishName = englishName;
    }

    MeMekanismMachine(FactoryTier factoryTier, FactoryType factoryType) {
        this.factoryType = factoryType;
        this.factoryTier = factoryTier;
        this.extraFactoryTierName = null;
        this.moreMachineFactoryTypeName = null;
        this.moreMachineAdvancedFactoryTypeName = null;
        this.moreMachineBaseTypeName = null;
        this.requiredModId = null;
        this.baseName = factoryTier.name().toLowerCase(Locale.ROOT) + "_" + factoryType.getRegistryNameComponent() + "_factory";
        this.englishName = "ME " + capitalize(factoryTier.name()) + " " + factoryType.getRegistryNameComponentCapitalized() + " Factory";
    }

    MeMekanismMachine(String extraFactoryTierName, FactoryType factoryType) {
        this.factoryType = factoryType;
        this.factoryTier = null;
        this.extraFactoryTierName = extraFactoryTierName;
        this.moreMachineFactoryTypeName = null;
        this.moreMachineAdvancedFactoryTypeName = null;
        this.moreMachineBaseTypeName = null;
        this.requiredModId = "mekanism_extras";
        this.baseName = extraFactoryTierName + "_" + factoryType.getRegistryNameComponent() + "_factory";
        this.englishName = "ME " + capitalize(extraFactoryTierName) + " " + factoryType.getRegistryNameComponentCapitalized() + " Factory";
    }

    MeMekanismMachine(FactoryTier factoryTier, String moreMachineFactoryTypeName, String factoryEnglishName) {
        this.factoryType = null;
        this.factoryTier = factoryTier;
        this.extraFactoryTierName = null;
        this.moreMachineFactoryTypeName = moreMachineFactoryTypeName;
        this.moreMachineAdvancedFactoryTypeName = null;
        this.moreMachineBaseTypeName = null;
        this.requiredModId = "mekmm";
        this.baseName = factoryTier.name().toLowerCase(Locale.ROOT) + "_" + moreMachineFactoryTypeName + "_factory";
        this.englishName = "ME " + capitalize(factoryTier.name()) + " " + factoryEnglishName + " Factory";
    }

    MeMekanismMachine(String extraFactoryTierName, String moreMachineFactoryTypeName, String factoryEnglishName, boolean ignoredAdvancedFactory, boolean ignoredExtraFactory) {
        this.factoryType = null;
        this.factoryTier = null;
        this.extraFactoryTierName = extraFactoryTierName;
        this.moreMachineFactoryTypeName = moreMachineFactoryTypeName;
        this.moreMachineAdvancedFactoryTypeName = null;
        this.moreMachineBaseTypeName = null;
        this.requiredModId = "mekanism_extras";
        this.baseName = extraFactoryTierName + "_" + moreMachineFactoryTypeName + "_factory";
        this.englishName = "ME " + capitalize(extraFactoryTierName) + " " + factoryEnglishName + " Factory";
    }

    MeMekanismMachine(FactoryTier factoryTier, String moreMachineAdvancedFactoryTypeName, String factoryEnglishName, boolean advancedFactory) {
        this.factoryType = null;
        this.factoryTier = factoryTier;
        this.extraFactoryTierName = null;
        this.moreMachineFactoryTypeName = null;
        this.moreMachineAdvancedFactoryTypeName = moreMachineAdvancedFactoryTypeName;
        this.moreMachineBaseTypeName = null;
        this.requiredModId = "mekmm";
        this.baseName = factoryTier.name().toLowerCase(Locale.ROOT) + "_" + moreMachineAdvancedFactoryTypeName + "_factory";
        this.englishName = "ME " + capitalize(factoryTier.name()) + " " + factoryEnglishName + " Factory";
    }

    MeMekanismMachine(String extraFactoryTierName, String moreMachineAdvancedFactoryTypeName, String factoryEnglishName, boolean advancedFactory) {
        this.factoryType = null;
        this.factoryTier = null;
        this.extraFactoryTierName = extraFactoryTierName;
        this.moreMachineFactoryTypeName = null;
        this.moreMachineAdvancedFactoryTypeName = moreMachineAdvancedFactoryTypeName;
        this.moreMachineBaseTypeName = null;
        this.requiredModId = "mekanism_extras";
        this.baseName = extraFactoryTierName + "_" + moreMachineAdvancedFactoryTypeName + "_factory";
        this.englishName = "ME " + capitalize(extraFactoryTierName) + " " + factoryEnglishName + " Factory";
    }

    MeMekanismMachine(String baseName, String moreMachineBaseTypeName, String englishName) {
        this.factoryType = null;
        this.factoryTier = null;
        this.extraFactoryTierName = null;
        this.moreMachineFactoryTypeName = null;
        this.moreMachineAdvancedFactoryTypeName = null;
        this.moreMachineBaseTypeName = moreMachineBaseTypeName;
        this.requiredModId = "mekmm";
        this.baseName = baseName;
        this.englishName = englishName;
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
        return factoryTier != null || extraFactoryTierName != null || moreMachineFactoryTypeName != null || moreMachineAdvancedFactoryTypeName != null;
    }

    public boolean isMekanismExtrasFactory() {
        return extraFactoryTierName != null;
    }

    public boolean isMekanismExtrasMekanismFactory() {
        return extraFactoryTierName != null && factoryType != null;
    }

    public boolean isMoreMachineFactory() {
        return moreMachineFactoryTypeName != null;
    }

    public boolean isMoreMachineAdvancedFactory() {
        return moreMachineAdvancedFactoryTypeName != null;
    }

    public boolean isMoreMachineBaseMachine() {
        return moreMachineBaseTypeName != null;
    }

    @Nullable
    public String extraFactoryTierName() {
        return extraFactoryTierName;
    }

    @Nullable
    public String moreMachineFactoryTypeName() {
        return moreMachineFactoryTypeName;
    }

    @Nullable
    public String moreMachineAdvancedFactoryTypeName() {
        return moreMachineAdvancedFactoryTypeName;
    }

    @Nullable
    public String moreMachineBaseTypeName() {
        return moreMachineBaseTypeName;
    }

    @Nullable
    public String requiredModId() {
        return requiredModId;
    }

    public boolean isAvailable() {
        if ("mekmm".equals(requiredModId) && !OptionalCompatClasses.hasMekmm()) {
            return false;
        }
        if ("mekanism_extras".equals(requiredModId) && !OptionalCompatClasses.hasMekanismExtras()) {
            return false;
        }
        if (moreMachineAdvancedFactoryTypeName != null) {
            return extraFactoryTierName == null
                    ? OptionalCompatClasses.hasMekmmAdvancedFactories()
                    : OptionalCompatClasses.hasMekanismExtrasAdvancedFactories();
        }
        if (extraFactoryTierName != null && moreMachineFactoryTypeName != null) {
            return OptionalCompatClasses.hasMekanismExtrasMoreMachineFactories();
        }
        return true;
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
        return this.factoryType == FactoryType.COMBINING || this == CNC_STAMPER;
    }

    public boolean hasChemicalInput() {
        return this.factoryType == FactoryType.COMPRESSING
                || this.factoryType == FactoryType.INFUSING
                || this.factoryType == FactoryType.INJECTING
                || this.factoryType == FactoryType.PURIFYING;
    }

    public boolean hasRecipeLogic() {
        return this.factoryType != null || this.moreMachineFactoryTypeName != null || this.moreMachineAdvancedFactoryTypeName != null || this.moreMachineBaseTypeName != null;
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
        if (this == CNC_STAMPER) {
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
        if (isFactory()) {
            return () -> 50L;
        }
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
        if (isFactory()) {
            return () -> 2_000_000L;
        }
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
        if (moreMachineFactoryTypeName != null) {
            return extraFactoryTierName == null
                    ? getMoreMachineFactory(FactoryTier.BASIC, moreMachineFactoryTypeName)
                    : getExtraMoreMachineFactory("absolute", moreMachineFactoryTypeName);
        }
        if (moreMachineAdvancedFactoryTypeName != null) {
            return extraFactoryTierName == null
                    ? getMoreMachineAdvancedFactory(FactoryTier.BASIC, moreMachineAdvancedFactoryTypeName)
                    : getExtraMoreMachineAdvancedFactory("absolute", moreMachineAdvancedFactoryTypeName);
        }
        if (moreMachineBaseTypeName != null) {
            return getMoreMachineFactory(FactoryTier.BASIC, moreMachineBaseTypeName);
        }
        return factoryType == null ? null : getFactory(FactoryTier.BASIC, factoryType);
    }

    @Nullable
    public MeMekanismMachine getNextFactory() {
        if (moreMachineFactoryTypeName != null) {
            if (factoryTier == FactoryTier.ULTIMATE) {
                return getExtraMoreMachineFactory("absolute", moreMachineFactoryTypeName);
            }
            if (extraFactoryTierName != null) {
                String nextTier = getNextExtraFactoryTier(extraFactoryTierName);
                return nextTier == null ? null : getExtraMoreMachineFactory(nextTier, moreMachineFactoryTypeName);
            }
            if (factoryTier == null) {
                return null;
            }
            return getMoreMachineFactory(FactoryTier.values()[factoryTier.ordinal() + 1], moreMachineFactoryTypeName);
        }
        if (moreMachineAdvancedFactoryTypeName != null) {
            if (factoryTier == FactoryTier.ULTIMATE) {
                return getExtraMoreMachineAdvancedFactory("absolute", moreMachineAdvancedFactoryTypeName);
            }
            if (extraFactoryTierName != null) {
                String nextTier = getNextExtraFactoryTier(extraFactoryTierName);
                return nextTier == null ? null : getExtraMoreMachineAdvancedFactory(nextTier, moreMachineAdvancedFactoryTypeName);
            }
            if (factoryTier == null) {
                return null;
            }
            return getMoreMachineAdvancedFactory(FactoryTier.values()[factoryTier.ordinal() + 1], moreMachineAdvancedFactoryTypeName);
        }
        if (factoryType == null) {
            return null;
        }
        if (extraFactoryTierName != null) {
            return getNextExtraFactory(extraFactoryTierName, factoryType);
        }
        if (factoryTier == FactoryTier.ULTIMATE) {
            return getExtraFactory("absolute", factoryType);
        }
        if (factoryTier == null) {
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

    @Nullable
    public static MeMekanismMachine getMoreMachineFactory(FactoryTier tier, String typeName) {
        if (tier == null || typeName == null) {
            return null;
        }
        for (MeMekanismMachine machine : values()) {
            if (machine.factoryTier == tier && typeName.equals(machine.moreMachineFactoryTypeName)) {
                return machine;
            }
        }
        return null;
    }

    @Nullable
    public static MeMekanismMachine getExtraMoreMachineFactory(String tierName, String typeName) {
        if (tierName == null || typeName == null) {
            return null;
        }
        for (MeMekanismMachine machine : values()) {
            if (tierName.equals(machine.extraFactoryTierName) && typeName.equals(machine.moreMachineFactoryTypeName)) {
                return machine;
            }
        }
        return null;
    }

    @Nullable
    public static MeMekanismMachine getMoreMachineAdvancedFactory(FactoryTier tier, String typeName) {
        if (tier == null || typeName == null) {
            return null;
        }
        for (MeMekanismMachine machine : values()) {
            if (machine.factoryTier == tier && typeName.equals(machine.moreMachineAdvancedFactoryTypeName)) {
                return machine;
            }
        }
        return null;
    }

    @Nullable
    public static MeMekanismMachine getExtraMoreMachineAdvancedFactory(String tierName, String typeName) {
        if (tierName == null || typeName == null) {
            return null;
        }
        for (MeMekanismMachine machine : values()) {
            if (tierName.equals(machine.extraFactoryTierName) && typeName.equals(machine.moreMachineAdvancedFactoryTypeName)) {
                return machine;
            }
        }
        return null;
    }

    @Nullable
    public static MeMekanismMachine getExtraFactory(String tierName, FactoryType type) {
        if (tierName == null || type == null) {
            return null;
        }
        for (MeMekanismMachine machine : values()) {
            if (tierName.equals(machine.extraFactoryTierName) && machine.factoryType == type) {
                return machine;
            }
        }
        return null;
    }

    @Nullable
    public static MeMekanismMachine getByRegistryName(String registryName) {
        for (MeMekanismMachine machine : values()) {
            if (machine.registryName().equals(registryName) && machine.isAvailable()) {
                return machine;
            }
        }
        return null;
    }

    @Nullable
    private static MeMekanismMachine getNextExtraFactory(String tierName, FactoryType type) {
        String next = getNextExtraFactoryTier(tierName);
        return next == null ? null : getExtraFactory(next, type);
    }

    @Nullable
    private static String getNextExtraFactoryTier(String tierName) {
        return switch (tierName) {
            case "absolute" -> "supreme";
            case "supreme" -> "cosmic";
            case "cosmic" -> "infinite";
            default -> null;
        };
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
