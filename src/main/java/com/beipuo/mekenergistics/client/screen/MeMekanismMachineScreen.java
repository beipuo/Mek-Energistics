package com.beipuo.mekenergistics.client.screen;

import com.beipuo.mekenergistics.blockentity.MeMekanismMachineBlockEntity;
import com.beipuo.mekenergistics.network.CycleAeOutputModePacket;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

public abstract class MeMekanismMachineScreen<TILE extends MeMekanismMachineBlockEntity>
        extends GuiConfigurableTile<TILE, MekanismTileContainer<TILE>> {
    private MekanismButton aeOutputModeButton;

    protected MeMekanismMachineScreen(MekanismTileContainer<TILE> menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 46;
        this.inventoryLabelY = 72;
        this.dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        this.aeOutputModeButton = addRenderableWidget(new MekanismButton(this, 7, 4, 58, 12,
                Component.literal(tile.getAeOutputMode().label()),
                (element, mouseX, mouseY) -> {
                    PacketDistributor.sendToServer(new CycleAeOutputModePacket(tile.getBlockPos()));
                    return true;
                }));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (this.aeOutputModeButton != null) {
            this.aeOutputModeButton.setMessage(Component.literal(tile.getAeOutputMode().label()));
        }
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        renderInventoryText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }
}
