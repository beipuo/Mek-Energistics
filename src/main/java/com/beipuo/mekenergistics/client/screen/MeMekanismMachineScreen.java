package com.beipuo.mekenergistics.client.screen;

import com.beipuo.mekenergistics.common.MeMekanismMachine;
import com.beipuo.mekenergistics.menu.MeMekanismMachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class MeMekanismMachineScreen extends AbstractContainerScreen<MeMekanismMachineMenu> {
    private static final ResourceLocation BASE = ResourceLocation.fromNamespaceAndPath("mekanism", "gui/base.png");
    private static final ResourceLocation SLOTS = ResourceLocation.fromNamespaceAndPath("mekanism", "gui/slot/slots.png");
    private static final ResourceLocation POWER = ResourceLocation.fromNamespaceAndPath("mekanism", "gui/slot/power.png");
    private static final ResourceLocation PROGRESS = ResourceLocation.fromNamespaceAndPath("mekanism", "gui/progress/right.png");

    public MeMekanismMachineScreen(MeMekanismMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 46;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        guiGraphics.blit(BASE, left, top, 0, 0, this.imageWidth, this.imageHeight);

        drawSlot(guiGraphics, left + 50, top + 42);
        switch (getMachine().slotLayout()) {
            case ITEM_CHEMICAL -> drawSlot(guiGraphics, left + 16, top + 34);
            case DOUBLE_ITEM -> drawSlot(guiGraphics, left + 16, top + 42);
            case SAWING, SINGLE_ITEM -> {
            }
        }
        drawSlot(guiGraphics, left + 108, top + 42);
        if (getMachine().hasSecondaryOutput()) {
            drawSlot(guiGraphics, left + 132, top + 42);
        }
        guiGraphics.blit(POWER, left + 142, top + 34, 0, 0, 18, 18, 18, 18);
        guiGraphics.blit(PROGRESS, left + 72, top + 47, 0, 0, 28, 8, 28, 8);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                drawSlot(guiGraphics, left - 55 + col * 18, top + 16 + row * 18);
            }
        }
    }

    private static void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(SLOTS, x, y, 0, 0, 18, 18, 18, 18);
    }

    public MeMekanismMachine getMachine() {
        return this.menu.getMachine();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
