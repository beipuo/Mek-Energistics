package com.beipuo.mekenergistics.registry;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.item.MeMachineBlockItem;
import com.beipuo.mekenergistics.item.MeTierInstallerItem;
import java.util.EnumMap;
import java.util.Map;
import mekanism.common.attachments.component.AttachedEjector;
import mekanism.common.attachments.component.AttachedSideConfig;
import mekanism.common.attachments.component.AttachedSideConfig.LightConfigInfo;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registries.MekanismDataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MekEnergistics.MODID);
    private static final Map<MeMekanismMachine, DeferredItem<BlockItem>> MACHINES = new EnumMap<>(MeMekanismMachine.class);
    public static final DeferredItem<MeTierInstallerItem> ME_FACTORY_INSTALLER =
            ITEMS.register("me_factory_installer", () -> new MeTierInstallerItem(new Item.Properties()));

    static {
        for (MeMekanismMachine machine : MeMekanismMachine.values()) {
            if (machine.isAvailable()) {
                MACHINES.put(machine, ITEMS.register(
                        machine.registryName(),
                        () -> new MeMachineBlockItem(ModBlocks.getMachineBlock(machine).get(), machineProperties(machine))
                ));
            }
        }
    }

    public static final DeferredItem<BlockItem> ME_METALLURGIC_INFUSER = getMachineItem(MeMekanismMachine.METALLURGIC_INFUSER);

    private ModItems() {
    }

    public static DeferredItem<BlockItem> getMachineItem(MeMekanismMachine machine) {
        return MACHINES.get(machine);
    }

    public static Iterable<DeferredItem<BlockItem>> getMachineItems() {
        return MACHINES.values();
    }

    private static Item.Properties machineProperties(MeMekanismMachine machine) {
        return new Item.Properties()
                .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)
                .component(MekanismDataComponents.SIDE_CONFIG, defaultSideConfig(machine));
    }

    private static AttachedSideConfig defaultSideConfig(MeMekanismMachine machine) {
        if (machine.factoryType() != null && machine.isFactory()) {
            return switch (machine.factoryType()) {
                case SMELTING, ENRICHING, CRUSHING, SAWING -> AttachedSideConfig.ELECTRIC_MACHINE;
                case COMPRESSING, INFUSING -> AttachedSideConfig.ADVANCED_MACHINE;
                case COMBINING -> AttachedSideConfig.EXTRA_MACHINE;
                case PURIFYING, INJECTING -> AttachedSideConfig.ADVANCED_MACHINE_INPUT_ONLY;
            };
        }
        if (machine.moreMachineFactoryTypeName() != null) {
            return switch (machine.moreMachineFactoryTypeName()) {
                case "stamping" -> AttachedSideConfig.EXTRA_MACHINE;
                case "planting", "replicating" -> AttachedSideConfig.ADVANCED_MACHINE_INPUT_ONLY;
                default -> AttachedSideConfig.ELECTRIC_MACHINE;
            };
        }
        if (machine.moreMachineAdvancedFactoryTypeName() != null) {
            return switch (machine.moreMachineAdvancedFactoryTypeName()) {
                case "oxidizing", "pigment_extracting" -> AttachedSideConfig.CHEMICAL_OUT_MACHINE;
                case "dissolving" -> AttachedSideConfig.DISSOLUTION;
                case "washing" -> AttachedSideConfig.WASHER;
                case "pressurised_reacting" -> AttachedSideConfig.REACTION;
                case "crystallizing" -> AttachedSideConfig.CRYSTALLIZER;
                case "centrifuging" -> AttachedSideConfig.CENTRIFUGE;
                case "liquifying" -> AttachedSideConfig.LIQUIFIER;
                case "painting" -> AttachedSideConfig.PAINTING;
                default -> AttachedSideConfig.ELECTRIC_MACHINE;
            };
        }
        return switch (machine) {
            case PLANTING_STATION, REPLICATOR ->
                    AttachedSideConfig.ADVANCED_MACHINE_INPUT_ONLY;
            case ENRICHMENT_CHAMBER, CRUSHER, ENERGIZED_SMELTER, PRECISION_SAWMILL ->
                    AttachedSideConfig.ELECTRIC_MACHINE;
            case OSMIUM_COMPRESSOR, METALLURGIC_INFUSER ->
                    AttachedSideConfig.ADVANCED_MACHINE;
            case ALLOYER, COMBINER, FORMULAIC_ASSEMBLICATOR ->
                    AttachedSideConfig.EXTRA_MACHINE;
            case PURIFICATION_CHAMBER, CHEMICAL_INJECTION_CHAMBER, ANTIPROTONIC_NUCLEOSYNTHESIZER ->
                    AttachedSideConfig.ADVANCED_MACHINE_INPUT_ONLY;
            case PRESSURIZED_REACTION_CHAMBER ->
                    AttachedSideConfig.REACTION;
            case CHEMICAL_CRYSTALLIZER ->
                    AttachedSideConfig.CRYSTALLIZER;
            case CHEMICAL_DISSOLUTION_CHAMBER ->
                    AttachedSideConfig.DISSOLUTION;
            case CHEMICAL_INFUSER ->
                    AttachedSideConfig.CHEMICAL_INFUSING;
            case CHEMICAL_OXIDIZER, PIGMENT_EXTRACTOR ->
                    AttachedSideConfig.CHEMICAL_OUT_MACHINE;
            case CHEMICAL_WASHER ->
                    AttachedSideConfig.WASHER;
            case ROTARY_CONDENSENTRATOR ->
                    AttachedSideConfig.ROTARY;
            case ELECTROLYTIC_SEPARATOR ->
                    AttachedSideConfig.SEPARATOR;
            case SOLAR_NEUTRON_ACTIVATOR ->
                    AttachedSideConfig.SNA;
            case ISOTOPIC_CENTRIFUGE ->
                    AttachedSideConfig.CENTRIFUGE;
            case NUTRITIONAL_LIQUIFIER ->
                    AttachedSideConfig.LIQUIFIER;
            case PIGMENT_MIXER ->
                    AttachedSideConfig.PIGMENT_MIXER;
            case PAINTING_MACHINE ->
                    AttachedSideConfig.PAINTING;
            case OREDICTIONIFICATOR ->
                    new AttachedSideConfig(Map.of(TransmissionType.ITEM, LightConfigInfo.OUT_NO_EJECT));
            default ->
                    AttachedSideConfig.ELECTRIC_MACHINE;
        };
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
