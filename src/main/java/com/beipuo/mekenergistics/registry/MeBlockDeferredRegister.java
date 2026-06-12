package com.beipuo.mekenergistics.registry;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.block.MeMekanismMachineBlock;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.item.MeMachineBlockItem;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import mekanism.api.security.SecurityMode;
import mekanism.common.attachments.component.AttachedEjector;
import mekanism.common.attachments.component.AttachedSideConfig;
import mekanism.common.attachments.component.UpgradeAware;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeUpgradeSupport;
import mekanism.common.block.attribute.Attributes.AttributeRedstone;
import mekanism.common.block.attribute.Attributes.AttributeSecurity;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.tile.interfaces.IRedstoneControl.RedstoneControl;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MeBlockDeferredRegister {
    private final DeferredRegister.Blocks blocks = DeferredRegister.createBlocks(MekEnergistics.MODID);
    private final MeItemDeferredRegister items = new MeItemDeferredRegister();

    public MeBlockRegistryObject<MeMekanismMachineBlock, MeMachineBlockItem> registerMachine(MeMekanismMachine machine) {
        return register(machine.registryName(), () -> new MeMekanismMachineBlock(machine),
                (block, properties) -> new MeMachineBlockItem(block, machineProperties(machine, block, properties)));
    }

    public <BLOCK extends Block, ITEM extends BlockItem> MeBlockRegistryObject<BLOCK, ITEM> register(
            String name, Supplier<? extends BLOCK> blockSupplier, BiFunction<BLOCK, Item.Properties, ITEM> itemCreator
    ) {
        DeferredBlock<BLOCK> block = blocks.register(name, blockSupplier);
        DeferredItem<ITEM> item = items.register(name, () -> itemCreator.apply(block.get(), blockItemProperties(block.get())));
        return new MeBlockRegistryObject<>(block, item);
    }

    public void register(IEventBus eventBus) {
        blocks.register(eventBus);
        items.register(eventBus);
    }

    private static Item.Properties machineProperties(MeMekanismMachine machine, Block block, Item.Properties properties) {
        return properties
                .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)
                .component(MekanismDataComponents.SIDE_CONFIG, defaultSideConfig(machine));
    }

    private static Item.Properties blockItemProperties(Block block) {
        Item.Properties properties = new Item.Properties();
        if (Attribute.has(block, AttributeSecurity.class)) {
            properties.component(MekanismDataComponents.SECURITY, SecurityMode.PUBLIC);
        }
        if (Attribute.has(block, AttributeRedstone.class)) {
            properties.component(MekanismDataComponents.REDSTONE_CONTROL, RedstoneControl.DISABLED);
        }
        if (Attribute.has(block, AttributeUpgradeSupport.class)) {
            properties.component(MekanismDataComponents.UPGRADES, UpgradeAware.EMPTY);
        }
        return properties;
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
                    new AttachedSideConfig(java.util.Map.of(TransmissionType.ITEM, AttachedSideConfig.LightConfigInfo.OUT_NO_EJECT));
            default ->
                    AttachedSideConfig.ELECTRIC_MACHINE;
        };
    }
}
