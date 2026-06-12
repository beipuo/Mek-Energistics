package com.beipuo.mekenergistics.client.screen;

import com.beipuo.mekenergistics.blockentity.MeMekanismMachineBlockEntity;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public abstract class MeMekanismMachineScreen<TILE extends MeMekanismMachineBlockEntity>
        extends MeGuiConfigurableTile<TILE, MekanismTileContainer<TILE>> {
    protected MeMekanismMachineScreen(MekanismTileContainer<TILE> menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.dynamicSlots = true;
    }

}
