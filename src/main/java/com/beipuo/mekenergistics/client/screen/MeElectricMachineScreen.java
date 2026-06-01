package com.beipuo.mekenergistics.client.screen;

import com.beipuo.mekenergistics.blockentity.MeElectricMachineBlockEntity;
import mekanism.client.gui.machine.GuiElectricMachine;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MeElectricMachineScreen extends GuiElectricMachine<MeElectricMachineBlockEntity, MekanismTileContainer<MeElectricMachineBlockEntity>> {
    public MeElectricMachineScreen(MekanismTileContainer<MeElectricMachineBlockEntity> menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
