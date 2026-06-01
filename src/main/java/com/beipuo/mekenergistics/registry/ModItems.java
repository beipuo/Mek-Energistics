package com.beipuo.mekenergistics.registry;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.common.MeMekanismMachine;
import com.beipuo.mekenergistics.item.MeTierInstallerItem;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import mekanism.api.tier.BaseTier;
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
    private static final List<DeferredItem<MeTierInstallerItem>> INSTALLERS = List.of(
            ITEMS.register("me_basic_tier_installer", () -> new MeTierInstallerItem(null, BaseTier.BASIC, new Item.Properties())),
            ITEMS.register("me_advanced_tier_installer", () -> new MeTierInstallerItem(BaseTier.BASIC, BaseTier.ADVANCED, new Item.Properties())),
            ITEMS.register("me_elite_tier_installer", () -> new MeTierInstallerItem(BaseTier.ADVANCED, BaseTier.ELITE, new Item.Properties())),
            ITEMS.register("me_ultimate_tier_installer", () -> new MeTierInstallerItem(BaseTier.ELITE, BaseTier.ULTIMATE, new Item.Properties()))
    );

    static {
        for (MeMekanismMachine machine : MeMekanismMachine.values()) {
            MACHINES.put(machine, ITEMS.register(
                    machine.registryName(),
                    () -> new BlockItem(ModBlocks.getMachineBlock(machine).get(), machineProperties(machine))
            ));
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

    public static Iterable<DeferredItem<MeTierInstallerItem>> getInstallerItems() {
        return INSTALLERS;
    }

    private static Item.Properties machineProperties(MeMekanismMachine machine) {
        return new Item.Properties()
                .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)
                .component(MekanismDataComponents.SIDE_CONFIG, defaultSideConfig(machine));
    }

    private static AttachedSideConfig defaultSideConfig(MeMekanismMachine machine) {
        return switch (machine) {
            case ENRICHMENT_CHAMBER, CRUSHER, ENERGIZED_SMELTER, PRECISION_SAWMILL ->
                    AttachedSideConfig.ELECTRIC_MACHINE;
            case OSMIUM_COMPRESSOR, METALLURGIC_INFUSER ->
                    AttachedSideConfig.ADVANCED_MACHINE;
            case COMBINER, FORMULAIC_ASSEMBLICATOR ->
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
