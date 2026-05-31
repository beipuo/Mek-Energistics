package com.beipuo.mekenergistics.registry;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.MeAdvancedElectricMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeCombinerBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeElectricMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeMekanismMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.MeMetallurgicInfuserBlockEntity;
import com.beipuo.mekenergistics.blockentity.MePrecisionSawmillBlockEntity;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import net.neoforged.bus.api.IEventBus;

public final class ModMenuTypes {
    private static final ContainerTypeDeferredRegister MENU_TYPES = new ContainerTypeDeferredRegister(MekEnergistics.MODID);

    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeElectricMachineBlockEntity>> ME_ELECTRIC_MACHINE =
            MENU_TYPES.register("me_electric_machine", MeElectricMachineBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeAdvancedElectricMachineBlockEntity>> ME_ADVANCED_ELECTRIC_MACHINE =
            MENU_TYPES.register("me_advanced_electric_machine", MeAdvancedElectricMachineBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeMetallurgicInfuserBlockEntity>> ME_METALLURGIC_INFUSER =
            MENU_TYPES.register("me_metallurgic_infuser", MeMetallurgicInfuserBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MeCombinerBlockEntity>> ME_COMBINER =
            MENU_TYPES.register("me_combiner", MeCombinerBlockEntity.class);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<MePrecisionSawmillBlockEntity>> ME_PRECISION_SAWMILL =
            MENU_TYPES.register("me_precision_sawmill", MePrecisionSawmillBlockEntity.class);

    private ModMenuTypes() {
    }

    public static ContainerTypeRegistryObject<? extends MekanismTileContainer<? extends MeMekanismMachineBlockEntity>> getMachineContainer(
            com.beipuo.mekenergistics.common.MeMekanismMachine machine) {
        return switch (machine.factoryType()) {
            case COMPRESSING, INJECTING, PURIFYING -> ME_ADVANCED_ELECTRIC_MACHINE;
            case INFUSING -> ME_METALLURGIC_INFUSER;
            case COMBINING -> ME_COMBINER;
            case SAWING -> ME_PRECISION_SAWMILL;
            default -> ME_ELECTRIC_MACHINE;
        };
    }

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
