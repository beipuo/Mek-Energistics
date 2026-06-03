package com.beipuo.mekenergistics.client.screen.element;

import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.network.packet.CycleAeOutputTypePacket;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiElement;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.tab.GuiConfigTypeTab;
import mekanism.client.gui.element.window.GuiSideConfiguration;
import mekanism.client.render.IFancyFontRenderer.TextAlignment;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.interfaces.ISideConfiguration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class MeGuiSideConfiguration<TILE extends TileEntityMekanism & ISideConfiguration> extends GuiSideConfiguration<TILE> {
    private static final int BUTTON_X_OFFSET = 120;
    private static final int BUTTON_Y_OFFSET = 6;
    private static final int BUTTON_SIZE = 14;
    private static final int AE_TEXT_X_OFFSET = 78;
    private static final int AE_TEXT_Y_OFFSET = 27;
    private static final int AE_TEXT_WIDTH = 38;
    private final TILE tile;
    private final AeOutputButton aeButton;

    public MeGuiSideConfiguration(IGuiWrapper gui, int x, int y, TILE tile, SelectedWindowData windowData) {
        super(gui, x, y, tile, windowData);
        this.tile = tile;
        MekEnergistics.LOGGER.debug("Created native ME side configuration for {} at {}", tile.getClass().getName(), tile.getBlockPos());
        this.aeButton = addChild(new AeOutputButton((GuiMekanism<?>) gui, relativeX + BUTTON_X_OFFSET, relativeY + BUTTON_Y_OFFSET));
        addChild(new AeOutputText((GuiMekanism<?>) gui, relativeX + AE_TEXT_X_OFFSET, relativeY + AE_TEXT_Y_OFFSET));
        updateAeElements();
    }

    @Override
    public void updateTabs() {
        super.updateTabs();
        updateAeElements();
    }

    private void updateAeElements() {
        if (this.aeButton == null) {
            return;
        }
        TransmissionType type = currentType();
        this.aeButton.visible = shouldRender(type);
        this.aeButton.active = canToggle(type);
    }

    private TransmissionType currentType() {
        for (GuiElement child : children()) {
            if (child instanceof GuiConfigTypeTab tab && !tab.visible) {
                return tab.getTransmissionType();
            }
        }
        for (GuiElement child : children()) {
            if (child instanceof GuiConfigTypeTab tab) {
                return tab.getTransmissionType();
            }
        }
        return TransmissionType.ITEM;
    }

    private boolean shouldRender(TransmissionType type) {
        return canToggle(type) || type == TransmissionType.FLUID;
    }

    private boolean canToggle(TransmissionType type) {
        return type == TransmissionType.ITEM || type == TransmissionType.CHEMICAL;
    }

    private boolean isTypeEnabled(TransmissionType type) {
        AeOutputMode mode = getAeOutputMode();
        return switch (type) {
            case ITEM -> mode.items();
            case CHEMICAL -> mode.chemicals();
            default -> false;
        };
    }

    private AeOutputMode getAeOutputMode() {
        if (this.tile instanceof MeAeMachine machine) {
            return machine.getAeOutputMode();
        }
        if (this.tile instanceof MeFactoryAeMachine machine) {
            return machine.getAeOutputMode();
        }
        return AeOutputMode.NONE;
    }

    private void sendToggle() {
        TransmissionType type = currentType();
        if (canToggle(type)) {
            PacketDistributor.sendToServer(new CycleAeOutputTypePacket(this.tile.getBlockPos(), type));
        }
    }

    private final class AeOutputText extends GuiElement {
        private AeOutputText(GuiMekanism<?> gui, int x, int y) {
            super(gui, x, y, AE_TEXT_WIDTH, 8);
        }

        @Override
        public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            super.renderForeground(guiGraphics, mouseX, mouseY);
            TransmissionType type = currentType();
            if (!canToggle(type)) {
                return;
            }
            Component text = Component.literal(isTypeEnabled(type) ? "AE: 开" : "AE: 关");
            drawScaledScrollingString(guiGraphics, text, 0, 0, TextAlignment.RIGHT, screenTextColor(), getWidth(), 1, false, 0.8F);
        }
    }

    private final class AeOutputButton extends MekanismButton {
        private AeOutputButton(GuiMekanism<?> gui, int x, int y) {
            super(gui, x, y, BUTTON_SIZE, BUTTON_SIZE, Component.empty(), (element, mouseX, mouseY) -> {
                sendToggle();
                return true;
            });
            setTooltip(Tooltip.create(Component.literal("输出到AE")));
        }

        @Override
        public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            super.renderForeground(guiGraphics, mouseX, mouseY);
            var font = Minecraft.getInstance().font;
            String text = "A";
            guiGraphics.drawString(font, text, (getWidth() - font.width(text)) / 2, (getHeight() - 8) / 2, 0x232323, false);
        }
    }
}
