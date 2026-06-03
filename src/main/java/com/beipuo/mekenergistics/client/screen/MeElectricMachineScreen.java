package com.beipuo.mekenergistics.client.screen;

import com.beipuo.mekenergistics.blockentity.machine.process.MeElectricMachineBlockEntity;
import com.beipuo.mekenergistics.client.screen.machine.MeGuiElectricMachine;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MeElectricMachineScreen extends MeGuiElectricMachine<MeElectricMachineBlockEntity, MekanismTileContainer<MeElectricMachineBlockEntity>> {
    public MeElectricMachineScreen(MekanismTileContainer<MeElectricMachineBlockEntity> menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
