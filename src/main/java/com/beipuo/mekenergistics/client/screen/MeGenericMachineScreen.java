package com.beipuo.mekenergistics.client.screen;

import com.beipuo.mekenergistics.blockentity.MeMekanismMachineBlockEntity;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MeGenericMachineScreen extends MeMekanismMachineScreen<MeMekanismMachineBlockEntity> {
    public MeGenericMachineScreen(MekanismTileContainer<MeMekanismMachineBlockEntity> menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
