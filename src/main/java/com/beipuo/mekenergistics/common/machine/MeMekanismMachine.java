package com.beipuo.mekenergistics.common.machine;

import com.beipuo.mekenergistics.compat.OptionalCompatClasses;
import com.beipuo.mekenergistics.compat.catalog.CompatFactoryTierGraph;
import com.beipuo.mekenergistics.compat.catalog.CompatMachineCatalog;
import com.beipuo.mekenergistics.compat.catalog.CompatMachineFamily;
import com.beipuo.mekenergistics.compat.catalog.CompatMachineKind;
import com.beipuo.mekenergistics.compat.catalog.CompatMod;
import com.beipuo.mekenergistics.compat.catalog.CompatRegistrationRoute;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;
import mekanism.api.tier.BaseTier;
import mekanism.common.content.blocktype.FactoryType;
import mekanism.common.tier.FactoryTier;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.Nullable;

public final class MeMekanismMachine implements Comparable<MeMekanismMachine> {
    private static final List<MeMekanismMachine> DECLARATIONS = new ArrayList<>();
    private static final Map<String, MeMekanismMachine> BY_NAME = new LinkedHashMap<>();

    public static final MeMekanismMachine ENRICHMENT_CHAMBER = register("ENRICHMENT_CHAMBER", FactoryType.ENRICHING, "enrichment_chamber", "ME Enrichment Chamber");
    public static final MeMekanismMachine CRUSHER = register("CRUSHER", FactoryType.CRUSHING, "crusher", "ME Crusher");
    public static final MeMekanismMachine ENERGIZED_SMELTER = register("ENERGIZED_SMELTER", FactoryType.SMELTING, "energized_smelter", "ME Energized Smelter");
    public static final MeMekanismMachine PRECISION_SAWMILL = register("PRECISION_SAWMILL", FactoryType.SAWING, "precision_sawmill", "ME Precision Sawmill");
    public static final MeMekanismMachine OSMIUM_COMPRESSOR = register("OSMIUM_COMPRESSOR", FactoryType.COMPRESSING, "osmium_compressor", "ME Osmium Compressor");
    public static final MeMekanismMachine COMBINER = register("COMBINER", FactoryType.COMBINING, "combiner", "ME Combiner");
    public static final MeMekanismMachine METALLURGIC_INFUSER = register("METALLURGIC_INFUSER", FactoryType.INFUSING, "metallurgic_infuser", "ME Metallurgic Infuser");
    public static final MeMekanismMachine ALLOYER = register("ALLOYER", "alloyer", "alloying", "ME Alloyer", CompatMachineFamily.EMEK_MACHINE);
    public static final MeMekanismMachine SOLIDIFICATION_CHAMBER = register("SOLIDIFICATION_CHAMBER", "solidification_chamber", "solidifying", "ME Solidification Chamber", CompatMachineFamily.EMEK_MACHINE);
    public static final MeMekanismMachine THERMALIZER = register("THERMALIZER", "thermalizer", "melting", "ME Thermalizer", CompatMachineFamily.EMEK_MACHINE);
    public static final MeMekanismMachine CHEMIXER = register("CHEMIXER", "chemixer", "chemixing", "ME Chemical Mixer", CompatMachineFamily.EMEK_MACHINE);
    public static final MeMekanismMachine PURIFICATION_CHAMBER = register("PURIFICATION_CHAMBER", FactoryType.PURIFYING, "purification_chamber", "ME Purification Chamber");
    public static final MeMekanismMachine CHEMICAL_INJECTION_CHAMBER = register("CHEMICAL_INJECTION_CHAMBER", FactoryType.INJECTING, "chemical_injection_chamber", "ME Chemical Injection Chamber");
    public static final MeMekanismMachine PRESSURIZED_REACTION_CHAMBER = register("PRESSURIZED_REACTION_CHAMBER", (FactoryType) null, "pressurized_reaction_chamber", "ME Pressurized Reaction Chamber");
    public static final MeMekanismMachine CHEMICAL_CRYSTALLIZER = register("CHEMICAL_CRYSTALLIZER", (FactoryType) null, "chemical_crystallizer", "ME Chemical Crystallizer");
    public static final MeMekanismMachine CHEMICAL_DISSOLUTION_CHAMBER = register("CHEMICAL_DISSOLUTION_CHAMBER", (FactoryType) null, "chemical_dissolution_chamber", "ME Chemical Dissolution Chamber");
    public static final MeMekanismMachine CHEMICAL_INFUSER = register("CHEMICAL_INFUSER", (FactoryType) null, "chemical_infuser", "ME Chemical Infuser");
    public static final MeMekanismMachine CHEMICAL_OXIDIZER = register("CHEMICAL_OXIDIZER", (FactoryType) null, "chemical_oxidizer", "ME Chemical Oxidizer");
    public static final MeMekanismMachine CHEMICAL_WASHER = register("CHEMICAL_WASHER", (FactoryType) null, "chemical_washer", "ME Chemical Washer");
    public static final MeMekanismMachine ROTARY_CONDENSENTRATOR = register("ROTARY_CONDENSENTRATOR", (FactoryType) null, "rotary_condensentrator", "ME Rotary Condensentrator");
    public static final MeMekanismMachine ELECTROLYTIC_SEPARATOR = register("ELECTROLYTIC_SEPARATOR", (FactoryType) null, "electrolytic_separator", "ME Electrolytic Separator");
    public static final MeMekanismMachine DIGITAL_MINER = register("DIGITAL_MINER", (FactoryType) null, "digital_miner", "ME Digital Miner");
    public static final MeMekanismMachine FORMULAIC_ASSEMBLICATOR = register("FORMULAIC_ASSEMBLICATOR", (FactoryType) null, "formulaic_assemblicator", "ME Formulaic Assemblicator");
    public static final MeMekanismMachine ELECTRIC_PUMP = register("ELECTRIC_PUMP", (FactoryType) null, "electric_pump", "ME Electric Pump");
    public static final MeMekanismMachine FLUIDIC_PLENISHER = register("FLUIDIC_PLENISHER", (FactoryType) null, "fluidic_plenisher", "ME Fluidic Plenisher");
    public static final MeMekanismMachine SOLAR_NEUTRON_ACTIVATOR = register("SOLAR_NEUTRON_ACTIVATOR", (FactoryType) null, "solar_neutron_activator", "ME Solar Neutron Activator");
    public static final MeMekanismMachine TELEPORTER = register("TELEPORTER", (FactoryType) null, "teleporter", "ME Teleporter");
    public static final MeMekanismMachine RESISTIVE_HEATER = register("RESISTIVE_HEATER", (FactoryType) null, "resistive_heater", "ME Resistive Heater");
    public static final MeMekanismMachine SEISMIC_VIBRATOR = register("SEISMIC_VIBRATOR", (FactoryType) null, "seismic_vibrator", "ME Seismic Vibrator");
    public static final MeMekanismMachine LOGISTICAL_SORTER = register("LOGISTICAL_SORTER", (FactoryType) null, "logistical_sorter", "ME Logistical Sorter");
    public static final MeMekanismMachine ISOTOPIC_CENTRIFUGE = register("ISOTOPIC_CENTRIFUGE", (FactoryType) null, "isotopic_centrifuge", "ME Isotopic Centrifuge");
    public static final MeMekanismMachine NUTRITIONAL_LIQUIFIER = register("NUTRITIONAL_LIQUIFIER", (FactoryType) null, "nutritional_liquifier", "ME Nutritional Liquifier");
    public static final MeMekanismMachine ANTIPROTONIC_NUCLEOSYNTHESIZER = register("ANTIPROTONIC_NUCLEOSYNTHESIZER", (FactoryType) null, "antiprotonic_nucleosynthesizer", "ME Antiprotonic Nucleosynthesizer");
    public static final MeMekanismMachine PIGMENT_EXTRACTOR = register("PIGMENT_EXTRACTOR", (FactoryType) null, "pigment_extractor", "ME Pigment Extractor");
    public static final MeMekanismMachine PIGMENT_MIXER = register("PIGMENT_MIXER", (FactoryType) null, "pigment_mixer", "ME Pigment Mixer");
    public static final MeMekanismMachine PAINTING_MACHINE = register("PAINTING_MACHINE", (FactoryType) null, "painting_machine", "ME Painting Machine");
    public static final MeMekanismMachine DIMENSIONAL_STABILIZER = register("DIMENSIONAL_STABILIZER", (FactoryType) null, "dimensional_stabilizer", "ME Dimensional Stabilizer");
    public static final MeMekanismMachine OREDICTIONIFICATOR = register("OREDICTIONIFICATOR", (FactoryType) null, "oredictionificator", "ME Oredictionificator");
    public static final MeMekanismMachine MODIFICATION_STATION = register("MODIFICATION_STATION", (FactoryType) null, "modification_station", "ME Modification Station");
    public static final MeMekanismMachine RECYCLER = register("RECYCLER", "recycler", "recycling", "ME Recycler");
    public static final MeMekanismMachine PLANTING_STATION = register("PLANTING_STATION", "planting_station", "planting", "ME Planting Station");
    public static final MeMekanismMachine CNC_STAMPER = register("CNC_STAMPER", "cnc_stamper", "stamping", "ME CNC Stamper");
    public static final MeMekanismMachine CNC_LATHE = register("CNC_LATHE", "cnc_lathe", "lathing", "ME CNC Lathe");
    public static final MeMekanismMachine CNC_ROLLING_MILL = register("CNC_ROLLING_MILL", "cnc_rolling_mill", "rolling_mill", "ME CNC Rolling Mill");
    public static final MeMekanismMachine REPLICATOR = register("REPLICATOR", "replicator", "replicating", "ME Replicator");
    public static final MeMekanismMachine CHEMICAL_REPLICATOR = register("CHEMICAL_REPLICATOR", "chemical_replicator", "chemical_replicator", "ME Chemical Replicator");
    public static final MeMekanismMachine FLUID_REPLICATOR = register("FLUID_REPLICATOR", "fluid_replicator", "fluid_replicator", "ME Fluid Replicator");
    public static final MeMekanismMachine LARGE_ROTARY_CONDENSENTRATOR = register("LARGE_ROTARY_CONDENSENTRATOR", "large_rotary_condensentrator", "large_rotary_condensentrator", "ME Large Rotary Condensentrator");
    public static final MeMekanismMachine LARGE_SOLAR_NEUTRON_ACTIVATOR = register("LARGE_SOLAR_NEUTRON_ACTIVATOR", "large_solar_neutron_activator", "large_solar_neutron_activator", "ME Large Solar Neutron Activator");
    public static final MeMekanismMachine LARGE_ELECTROLYTIC_SEPARATOR = register("LARGE_ELECTROLYTIC_SEPARATOR", "large_electrolytic_separator", "large_electrolytic_separator", "ME Large Electrolytic Separator");
    public static final MeMekanismMachine LARGE_CHEMICAL_INFUSER = register("LARGE_CHEMICAL_INFUSER", "large_chemical_infuser", "large_chemical_infuser", "ME Large Chemical Infuser");
    public static final MeMekanismMachine LARGE_ANTIPROTONIC_NUCLEOSYNTHESIZER = register("LARGE_ANTIPROTONIC_NUCLEOSYNTHESIZER", "large_antiprotonic_nucleosynthesizer", "large_antiprotonic_nucleosynthesizer", "ME Large Antiprotonic Nucleosynthesizer");

    private static final List<FactoryTypeSpec> CORE_FACTORY_TYPES = List.of(
            coreType(FactoryType.SMELTING),
            coreType(FactoryType.ENRICHING),
            coreType(FactoryType.CRUSHING),
            coreType(FactoryType.COMPRESSING),
            coreType(FactoryType.COMBINING),
            coreType(FactoryType.PURIFYING),
            coreType(FactoryType.INJECTING),
            coreType(FactoryType.INFUSING),
            coreType(FactoryType.SAWING));
    private static final List<FactoryTypeSpec> ALLOYING_TYPE = List.of(
            customType("alloying", "Alloying"));
    private static final List<FactoryTypeSpec> MEKMM_FACTORY_TYPES = List.of(
            customType("recycling", "Recycling"),
            customType("planting", "Planting"),
            customType("stamping", "Stamping"),
            customType("lathing", "Lathing"),
            customType("rolling_mill", "Rolling Mill"),
            customType("replicating", "Replicating"));
    private static final List<FactoryTypeSpec> MEKE_PRESSING_TYPE = List.of(
            customType("pressing", "Pressing"));
    private static final List<FactoryTypeSpec> MEKMM_ADVANCED_TYPES = List.of(
            customType("oxidizing", "Oxidizing"),
            customType("dissolving", "Dissolving"),
            customType("washing", "Washing"),
            customType("crystallizing", "Crystallizing"),
            customType("pressurised_reacting", "Pressurised Reacting"),
            customType("centrifuging", "Centrifuging"),
            customType("liquifying", "Liquifying"),
            customType("pigment_extracting", "Pigment Extracting"),
            customType("painting", "Painting"));
    private static final List<FactoryTypeSpec> COMBINED_ADVANCED_TYPES = List.of(
            customType("recycling", "Recycling", CompatMachineFamily.EMEKE_MEKMM_FACTORY),
            customType("stamping", "Stamping", CompatMachineFamily.EMEKE_MEKMM_FACTORY),
            customType("lathing", "Lathing", CompatMachineFamily.EMEKE_MEKMM_FACTORY),
            customType("rolling_mill", "Rolling Mill", CompatMachineFamily.EMEKE_MEKMM_FACTORY),
            customType("dissolving", "Dissolving", CompatMachineFamily.EMEKE_MEKAF_ADVANCED_FACTORY),
            customType("oxidizing", "Oxidizing", CompatMachineFamily.EMEKE_MEKAF_ADVANCED_FACTORY),
            customType("pigment_extracting", "Pigment Extracting",
                    CompatMachineFamily.EMEKE_MEKAF_ADVANCED_FACTORY),
            customType("painting", "Painting", CompatMachineFamily.EMEKE_MEKAF_ADVANCED_FACTORY),
            customType("planting", "Planting", CompatMachineFamily.EMEKE_MEKMM_FACTORY),
            customType("replicating", "Replicating", CompatMachineFamily.EMEKE_MEKMM_FACTORY),
            customType("washing", "Washing", CompatMachineFamily.EMEKE_MEKAF_ADVANCED_FACTORY),
            customType("crystallizing", "Crystallizing",
                    CompatMachineFamily.EMEKE_MEKAF_ADVANCED_FACTORY),
            customType("pressurised_reacting", "Pressurised Reacting",
                    CompatMachineFamily.EMEKE_MEKAF_ADVANCED_FACTORY),
            customType("centrifuging", "Centrifuging",
                    CompatMachineFamily.EMEKE_MEKAF_ADVANCED_FACTORY),
            customType("liquifying", "Liquifying",
                    CompatMachineFamily.EMEKE_MEKAF_ADVANCED_FACTORY));

    private static final List<TierSpec> MEKANISM_TIERS = List.of(
            tier(FactoryTier.BASIC),
            tier(FactoryTier.ADVANCED),
            tier(FactoryTier.ELITE),
            tier(FactoryTier.ULTIMATE));
    private static final List<TierSpec> EVOLVED_TIERS = tiers(
            "overclocked", "quantum", "dense", "multiversal", "creative");
    private static final List<TierSpec> EXTRA_TIERS = tiers(
            "absolute", "supreme", "cosmic", "infinite");
    private static final List<TierSpec> COMBINED_TIERS = tiers(
            "absolute_overclocked", "supreme_quantum", "cosmic_dense", "infinite_multiversal");

    static {
        generateFactories(MEKANISM_TIERS, CORE_FACTORY_TYPES, CompatMachineFamily.MEKANISM_FACTORY);
        generateFactories(MEKANISM_TIERS, ALLOYING_TYPE, CompatMachineFamily.EMEK_FACTORY);
        generateFactories(EVOLVED_TIERS, CORE_FACTORY_TYPES, CompatMachineFamily.EMEK_FACTORY);
        generateFactories(EVOLVED_TIERS, ALLOYING_TYPE, CompatMachineFamily.EMEK_FACTORY);
        generateFactories(EXTRA_TIERS, CORE_FACTORY_TYPES, CompatMachineFamily.MEKE_FACTORY);
        generateFactories(EXTRA_TIERS, ALLOYING_TYPE, CompatMachineFamily.MEKE_FACTORY);
        generateFactories(COMBINED_TIERS, CORE_FACTORY_TYPES, CompatMachineFamily.EMEKE_FACTORY);
        generateFactories(COMBINED_TIERS, ALLOYING_TYPE, CompatMachineFamily.EMEKE_FACTORY);
        generateFactories(COMBINED_TIERS, COMBINED_ADVANCED_TYPES, null);
        generateFactories(MEKANISM_TIERS, MEKMM_FACTORY_TYPES, CompatMachineFamily.MEKMM_FACTORY);
        generateFactories(EXTRA_TIERS, MEKMM_FACTORY_TYPES, CompatMachineFamily.MEKE_MEKMM_FACTORY);
        generateFactories(MEKANISM_TIERS, MEKMM_ADVANCED_TYPES,
                CompatMachineFamily.MEKMM_ADVANCED_FACTORY);
        generateFactories(EXTRA_TIERS, MEKMM_ADVANCED_TYPES,
                CompatMachineFamily.MEKE_MEKMM_ADVANCED_FACTORY);
        // Append newly introduced Extras factories so existing machine ordinals remain stable.
        generateFactories(EXTRA_TIERS, MEKE_PRESSING_TYPE, CompatMachineFamily.MEKE_MEKMM_FACTORY);
    }

    /**
     * Identity for Mekanism Magic tiles adapted through {@code IMekanismMagicAutomation}.
     * There is no ME block variant; Magic machines stay on their original blocks and receive ME
     * cards through the upgrade path only. Declared after factory generation so legacy ordinals
     * stay stable.
     */
    public static final MeMekanismMachine MEKANISM_MAGIC_AUTOMATION = register(
            "MEKANISM_MAGIC_AUTOMATION", (FactoryType) null, "magic_automation", "ME Mekanism Magic Automation");

    private final String name;
    private final int ordinal;
    private final MeMachineDefinition definition;

    private MeMekanismMachine(String name, MeMachineDefinition definition) {
        this.name = name;
        this.ordinal = DECLARATIONS.size();
        this.definition = definition;
    }

    private static MeMekanismMachine register(
            String name, @Nullable FactoryType factoryType, String baseName, String englishName) {
        return register(name, MeMachineDefinition.mekanismMachine(factoryType, baseName, englishName));
    }

    private static MeMekanismMachine register(
            String name, String nameOrTier, String machineTypeId, String englishName,
            CompatMachineFamily family) {
        return register(name, MeMachineDefinition.familyEntry(
                nameOrTier, machineTypeId, englishName, family));
    }

    private static MeMekanismMachine register(
            String name, String baseName, String machineTypeId, String englishName) {
        return register(name, MeMachineDefinition.mekmmMachine(baseName, machineTypeId, englishName));
    }

    private static MeMekanismMachine register(String name, MeMachineDefinition definition) {
        MeMekanismMachine machine = new MeMekanismMachine(name, definition);
        MeMekanismMachine previous = BY_NAME.put(name, machine);
        if (previous != null) {
            throw new IllegalStateException("Duplicate machine identity " + name);
        }
        DECLARATIONS.add(machine);
        return machine;
    }

    private static void generateFactories(
            List<TierSpec> tiers, List<FactoryTypeSpec> types,
            @Nullable CompatMachineFamily defaultFamily) {
        for (TierSpec tier : tiers) {
            for (FactoryTypeSpec type : types) {
                CompatMachineFamily family = type.family() != null ? type.family() : defaultFamily;
                if (family == null) {
                    throw new IllegalStateException("Factory family is missing for " + type.id());
                }
                String name = tier.id().toUpperCase(Locale.ROOT)
                        + "_" + type.constantName() + "_FACTORY";
                MeMachineDefinition definition;
                if (family == CompatMachineFamily.MEKANISM_FACTORY) {
                    definition = MeMachineDefinition.mekanismFactory(tier.factoryTier(), type.factoryType());
                } else if (type.factoryType() != null) {
                    definition = MeMachineDefinition.factory(tier.id(), type.factoryType(), family);
                } else {
                    definition = MeMachineDefinition.factory(
                            tier.factoryTier(), tier.id(), type.id(), type.englishName(), family);
                }
                register(name, definition);
            }
        }
    }

    private static FactoryTypeSpec coreType(FactoryType type) {
        return new FactoryTypeSpec(
                type.getRegistryNameComponent(),
                type.name(),
                type.getRegistryNameComponentCapitalized(),
                type,
                null);
    }

    private static FactoryTypeSpec customType(String id, String englishName) {
        return customType(id, englishName, null);
    }

    private static FactoryTypeSpec customType(
            String id, String englishName, @Nullable CompatMachineFamily family) {
        return new FactoryTypeSpec(id, id.toUpperCase(Locale.ROOT), englishName, null, family);
    }

    private static TierSpec tier(FactoryTier tier) {
        return new TierSpec(tier.name().toLowerCase(Locale.ROOT), tier);
    }

    private static List<TierSpec> tiers(String... ids) {
        return java.util.Arrays.stream(ids).map(id -> new TierSpec(id, null)).toList();
    }

    public static MeMekanismMachine[] values() {
        return DECLARATIONS.toArray(MeMekanismMachine[]::new);
    }

    public static MeMekanismMachine valueOf(String name) {
        MeMekanismMachine machine = BY_NAME.get(name);
        if (machine == null) {
            throw new IllegalArgumentException(
                    "No enum constant " + MeMekanismMachine.class.getName() + "." + name);
        }
        return machine;
    }

    public String name() {
        return this.name;
    }

    public int ordinal() {
        return this.ordinal;
    }
    /** Resolved lazily (and then reused) because it is queried on per-tick paths. */
    private volatile Identity identity;

    public Identity identity() {
        Identity resolved = this.identity;
        if (resolved == null) {
            resolved = isFactory() ? Identity.FACTORY : Identity.valueOf(this.name);
            this.identity = resolved;
        }
        return resolved;
    }

    public enum Identity {
        ENRICHMENT_CHAMBER,
        CRUSHER,
        ENERGIZED_SMELTER,
        PRECISION_SAWMILL,
        OSMIUM_COMPRESSOR,
        COMBINER,
        METALLURGIC_INFUSER,
        ALLOYER,
        SOLIDIFICATION_CHAMBER,
        THERMALIZER,
        CHEMIXER,
        PURIFICATION_CHAMBER,
        CHEMICAL_INJECTION_CHAMBER,
        PRESSURIZED_REACTION_CHAMBER,
        CHEMICAL_CRYSTALLIZER,
        CHEMICAL_DISSOLUTION_CHAMBER,
        CHEMICAL_INFUSER,
        CHEMICAL_OXIDIZER,
        CHEMICAL_WASHER,
        ROTARY_CONDENSENTRATOR,
        ELECTROLYTIC_SEPARATOR,
        DIGITAL_MINER,
        FORMULAIC_ASSEMBLICATOR,
        ELECTRIC_PUMP,
        FLUIDIC_PLENISHER,
        SOLAR_NEUTRON_ACTIVATOR,
        TELEPORTER,
        RESISTIVE_HEATER,
        SEISMIC_VIBRATOR,
        LOGISTICAL_SORTER,
        ISOTOPIC_CENTRIFUGE,
        NUTRITIONAL_LIQUIFIER,
        ANTIPROTONIC_NUCLEOSYNTHESIZER,
        PIGMENT_EXTRACTOR,
        PIGMENT_MIXER,
        PAINTING_MACHINE,
        DIMENSIONAL_STABILIZER,
        OREDICTIONIFICATOR,
        MODIFICATION_STATION,
        MEKANISM_MAGIC_AUTOMATION,
        RECYCLER,
        PLANTING_STATION,
        CNC_STAMPER,
        CNC_LATHE,
        CNC_ROLLING_MILL,
        REPLICATOR,
        CHEMICAL_REPLICATOR,
        FLUID_REPLICATOR,
        LARGE_ROTARY_CONDENSENTRATOR,
        LARGE_SOLAR_NEUTRON_ACTIVATOR,
        LARGE_ELECTROLYTIC_SEPARATOR,
        LARGE_CHEMICAL_INFUSER,
        LARGE_ANTIPROTONIC_NUCLEOSYNTHESIZER,
        FACTORY
    }


    @Override
    public int compareTo(MeMekanismMachine other) {
        return Integer.compare(this.ordinal, other.ordinal);
    }

    @Override
    public String toString() {
        return this.name;
    }

    private record TierSpec(String id, @Nullable FactoryTier factoryTier) {
    }

    private record FactoryTypeSpec(
            String id,
            String constantName,
            String englishName,
            @Nullable FactoryType factoryType,
            @Nullable CompatMachineFamily family) {
    }

    @Nullable
    public FactoryType factoryType() {
        return definition.factoryType();
    }

    @Nullable
    public FactoryTier factoryTier() {
        return definition.factoryTier() != null ? definition.factoryTier() : evolvedFactoryTier();
    }

    public boolean isFactory() {
        return definition.family().kind() != CompatMachineKind.MACHINE;
    }

    public boolean isEvolvedMekanismFactory() {
        return definition.family().provider() == CompatMod.EMEK && isFactory()
                && definition.factoryTier() == null;
    }

    public CompatMod provider() {
        return definition.family().provider();
    }

    public CompatRegistrationRoute registrationRoute() {
        return definition.family().route();
    }

    public CompatMachineKind machineKind() {
        return definition.family().kind();
    }

    public CompatMachineFamily family() {
        return definition.family();
    }

    public boolean isAvailable() {
        return CompatMachineCatalog.isAvailable(this);
    }

    public boolean hasMeVariant() {
        return CompatMachineCatalog.hasMeVariant(this);
    }

    @Nullable
    public BaseTier baseTier() {
        FactoryTier tier = factoryTier();
        return tier == null ? null : tier.getBaseTier();
    }

    public String baseName() {
        return definition.baseName();
    }

    public String registryName() {
        return "me_" + definition.baseName();
    }

    public boolean isMekmmLargeMachine() {
        return switch (identity()) {
            case LARGE_ROTARY_CONDENSENTRATOR, LARGE_SOLAR_NEUTRON_ACTIVATOR,
                    LARGE_ELECTROLYTIC_SEPARATOR, LARGE_CHEMICAL_INFUSER,
                    LARGE_ANTIPROTONIC_NUCLEOSYNTHESIZER -> true;
            default -> false;
        };
    }

    @Nullable
    public String tierId() {
        return definition.tierId();
    }

    public String machineTypeId() {
        return definition.machineTypeId();
    }

    public String englishName() {
        return definition.englishName();
    }

    public String translationKey() {
        return "block.mekenergistics." + registryName();
    }

    public String descriptionKey() {
        if (isFactory()) {
            return "description.mekanism.factory";
        }
        if (definition.family().provider() == CompatMod.MEKANISM) {
            return "description.mekanism." + definition.baseName();
        }
        return "description.mekenergistics.machine";
    }

    @Nullable
    public TextColor nameColor() {
        if (definition.family().provider() == CompatMod.MEKE) {
            return OptionalCompatClasses.getMekanismExtrasTierColor(definition.tierId());
        }
        if (definition.family().provider() == CompatMod.EMEKE) {
            return OptionalCompatClasses.getEvolvedMekanismExtrasTierColor(definition.tierId());
        }
        BaseTier tier = baseTier();
        return tier == null ? null : tier.getColor();
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean hasSecondaryItemInput() {
        return MeMachineIoProfile.hasSecondaryItemInput(this);
    }

    public boolean hasChemicalInput() {
        return MeMachineIoProfile.hasChemicalInput(this);
    }

    public boolean hasRecipeLogic() {
        return MeMachineIoProfile.hasRecipeLogic(this);
    }

    public boolean hasAdvancedChemicalInput() {
        return MeMachineIoProfile.hasAdvancedChemicalInput(this);
    }

    public boolean hasSecondaryOutput() {
        return MeMachineIoProfile.hasSecondaryOutput(this);
    }

    public SlotLayout slotLayout() {
        return MeMachineIoProfile.slotLayout(this);
    }

    public LongSupplier energyUsage() {
        return MeMachineEnergyProfile.usage(this);
    }

    public LongSupplier energyStorage() {
        return MeMachineEnergyProfile.storage(this);
    }

    @Nullable
    public MeMekanismMachine getBasicFactory() {
        return CompatFactoryTierGraph.basicFactory(this);
    }

    @Nullable
    public MeMekanismMachine getNextFactory() {
        return CompatFactoryTierGraph.nextFactory(this);
    }

    @Nullable
    public static MeMekanismMachine getBaseMachine(FactoryType type) {
        return type == null ? null : CompatFactoryTierGraph.findBaseMachine(
                CompatMod.MEKANISM, type.getRegistryNameComponent());
    }

    @Nullable
    public static MeMekanismMachine getFactory(FactoryTier tier, FactoryType type) {
        return tier == null || type == null ? null : CompatFactoryTierGraph.findFactory(
                CompatMod.MEKANISM, tier.name().toLowerCase(Locale.ROOT), type.getRegistryNameComponent());
    }

    @Nullable
    public static MeMekanismMachine getEvolvedFactory(String tierName, FactoryType type) {
        return type == null ? null : getEvolvedFactory(tierName, type.getRegistryNameComponent());
    }

    @Nullable
    public static MeMekanismMachine getEvolvedFactory(String tierName, String typeName) {
        return CompatFactoryTierGraph.findFactory(CompatMod.EMEK, tierName, typeName);
    }

    @Nullable
    public static MeMekanismMachine getFactory(FactoryTier tier, String typeName) {
        if (tier == null || typeName == null) {
            return null;
        }
        String tierName = tier.name().toLowerCase(Locale.ROOT);
        MeMekanismMachine core = CompatFactoryTierGraph.findFactory(CompatMod.MEKANISM, tierName, typeName);
        return core != null ? core : CompatFactoryTierGraph.findFactory(CompatMod.EMEK, tierName, typeName);
    }

    @Nullable
    public static MeMekanismMachine getEvolvedMekanismExtrasFactory(String tierName, FactoryType type) {
        return type == null ? null : getEvolvedMekanismExtrasFactory(tierName, type.getRegistryNameComponent());
    }

    @Nullable
    public static MeMekanismMachine getEvolvedMekanismExtrasFactory(String tierName, String typeName) {
        return CompatFactoryTierGraph.findFactory(CompatMod.EMEKE, tierName, typeName);
    }

    @Nullable
    public static MeMekanismMachine getMoreMachineFactory(FactoryTier tier, String typeName) {
        return tier == null ? null : CompatFactoryTierGraph.findFactory(
                CompatRegistrationRoute.MEKMM_FACTORY, tier.name().toLowerCase(Locale.ROOT), typeName);
    }

    @Nullable
    public static MeMekanismMachine getExtraMoreMachineFactory(String tierName, String typeName) {
        return CompatFactoryTierGraph.findFactory(
                CompatRegistrationRoute.MEKE_MEKMM_FACTORY, tierName, typeName);
    }

    @Nullable
    public static MeMekanismMachine getMoreMachineAdvancedFactory(FactoryTier tier, String typeName) {
        return tier == null ? null : CompatFactoryTierGraph.findFactory(
                CompatRegistrationRoute.MEKMM_ADVANCED_FACTORY, tier.name().toLowerCase(Locale.ROOT), typeName);
    }

    @Nullable
    public static MeMekanismMachine getExtraMoreMachineAdvancedFactory(String tierName, String typeName) {
        return CompatFactoryTierGraph.findFactory(
                CompatRegistrationRoute.MEKE_MEKMM_ADVANCED_FACTORY, tierName, typeName);
    }

    @Nullable
    public static MeMekanismMachine getExtraFactory(String tierName, FactoryType type) {
        return type == null ? null : getExtraFactory(tierName, type.getRegistryNameComponent());
    }

    @Nullable
    public static MeMekanismMachine getExtraFactory(String tierName, String typeName) {
        return CompatFactoryTierGraph.findFactory(CompatRegistrationRoute.MEKE_FACTORY, tierName, typeName);
    }

    @Nullable
    public static MeMekanismMachine getByRegistryName(String registryName) {
        return CompatFactoryTierGraph.findByRegistryName(registryName);
    }

    @Nullable
    private FactoryTier evolvedFactoryTier() {
        return isEvolvedMekanismFactory()
                ? OptionalCompatClasses.getEvolvedFactoryTier(definition.tierId()) : null;
    }

    public enum SlotLayout {
        SINGLE_ITEM,
        ITEM_CHEMICAL,
        DOUBLE_ITEM,
        SAWING
    }
}
