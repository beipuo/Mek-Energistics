package com.beipuo.mekenergistics.registry;

import com.beipuo.mekenergistics.blockentity.MeMekanismMachineBlockEntity;
import com.beipuo.mekenergistics.common.MeMekanismMachine;
import java.util.EnumMap;
import java.util.Map;
import mekanism.api.Upgrade;
import mekanism.api.text.ILangEntry;
import mekanism.common.block.attribute.AttributeStateFacing;
import mekanism.common.block.attribute.Attributes;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.lib.transmitter.TransmissionType;

public final class ModBlockTypes {
    private static final Map<MeMekanismMachine, BlockTypeTile<MeMekanismMachineBlockEntity>> MACHINES =
            new EnumMap<>(MeMekanismMachine.class);

    static {
        for (MeMekanismMachine machine : MeMekanismMachine.values()) {
            MACHINES.put(machine, BlockTypeTile.BlockTileBuilder
                    .createBlock(() -> ModBlockEntities.getMachineBlockEntity(machine), lang(machine))
                    .withGui(() -> ModMenuTypes.getMachineContainer(machine))
                    .withEnergyConfig(() -> 2_000_000L)
                    .with(new AttributeStateFacing(), Attributes.INVENTORY, Attributes.REDSTONE, Attributes.SECURITY)
                    .withSideConfig(machine.hasChemicalInput()
                            ? new TransmissionType[] {TransmissionType.ITEM, TransmissionType.ENERGY, TransmissionType.CHEMICAL}
                            : new TransmissionType[] {TransmissionType.ITEM, TransmissionType.ENERGY})
                    .withSupportedUpgrades(Upgrade.SPEED, Upgrade.ENERGY)
                    .build());
        }
    }

    private ModBlockTypes() {
    }

    public static BlockTypeTile<MeMekanismMachineBlockEntity> getMachineBlockType(MeMekanismMachine machine) {
        return MACHINES.get(machine);
    }

    private static ILangEntry lang(MeMekanismMachine machine) {
        return machine::translationKey;
    }
}
