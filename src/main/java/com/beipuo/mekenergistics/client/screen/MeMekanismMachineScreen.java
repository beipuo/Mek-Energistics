package com.beipuo.mekenergistics.client.screen;

import com.beipuo.mekenergistics.blockentity.MeMekanismMachineBlockEntity;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public abstract class MeMekanismMachineScreen<TILE extends MeMekanismMachineBlockEntity>
        extends GuiConfigurableTile<TILE, MekanismTileContainer<TILE>> {
    protected MeMekanismMachineScreen(MekanismTileContainer<TILE> menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.dynamicSlots = true;
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        renderInventoryText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }
}
