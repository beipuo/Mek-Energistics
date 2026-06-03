package com.beipuo.mekenergistics.client.overlay;

import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.client.screen.element.MeGuiSideConfiguration;
import com.beipuo.mekenergistics.network.packet.CycleAeOutputTypePacket;
import java.util.Map;
import java.util.WeakHashMap;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.element.GuiElement;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.tab.GuiConfigTypeTab;
import mekanism.client.gui.element.window.GuiSideConfiguration;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.client.render.IFancyFontRenderer.TextAlignment;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.interfaces.ISideConfiguration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = MekEnergistics.MODID, value = Dist.CLIENT)
public final class AeOutputConfigOverlay {
    private static final int BUTTON_X_OFFSET = 120;
    private static final int BUTTON_Y_OFFSET = 6;
    private static final int BUTTON_SIZE = 14;
    private static final int AE_TEXT_X_OFFSET = 78;
    private static final int AE_TEXT_Y_OFFSET = 27;
    private static final int AE_TEXT_WIDTH = 38;
    private static final Map<GuiSideConfiguration<?>, MekanismButton> BUTTONS = new WeakHashMap<>();
    private static final Map<GuiSideConfiguration<?>, AeOutputText> TEXTS = new WeakHashMap<>();

    private AeOutputConfigOverlay() {
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        OverlayTarget target = findTarget(event.getScreen());
        ensureButton(target);
    }

    @SubscribeEvent
    public static void mouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        OverlayTarget target = findTarget(event.getScreen());
        if (target == null || !canToggle(target.type())) {
            return;
        }
        ButtonBounds bounds = bounds(target.gui(), target.sideConfig());
        if (bounds.contains(event.getMouseX(), event.getMouseY())) {
            sendToggle(target);
            event.setCanceled(true);
        }
    }

    private static void ensureButton(OverlayTarget target) {
        if (target == null) {
            return;
        }
        MekanismButton button = BUTTONS.computeIfAbsent(target.sideConfig(), sideConfig -> {
            AeOutputButton newButton = new AeOutputButton(target.gui(), sideConfig.getRelativeX() + BUTTON_X_OFFSET,
                    sideConfig.getRelativeY() + BUTTON_Y_OFFSET);
            sideConfig.children().add(newButton);
            return newButton;
        });
        if (button instanceof AeOutputButton aeButton) {
            aeButton.target = target;
        }
        button.visible = shouldRender(target.type());
        button.active = canToggle(target.type());
        button.setMessage(Component.empty());
        AeOutputText text = TEXTS.computeIfAbsent(target.sideConfig(), sideConfig -> {
            AeOutputText newText = new AeOutputText(target.gui(), sideConfig.getRelativeX() + AE_TEXT_X_OFFSET,
                    sideConfig.getRelativeY() + AE_TEXT_Y_OFFSET);
            sideConfig.children().add(newText);
            return newText;
        });
        text.target = target;
        text.visible = canToggle(target.type());
    }

    private static OverlayTarget findTarget(Screen screen) {
        ScreenTarget screenTarget = findScreenTarget(screen);
        if (screenTarget == null) {
            return null;
        }
        for (GuiWindow window : screenTarget.gui().getWindows()) {
            if (window instanceof GuiSideConfiguration<?> sideConfig) {
                if (sideConfig instanceof MeGuiSideConfiguration<?>) {
                    return null;
                }
                TransmissionType type = getCurrentType(sideConfig);
                MekEnergistics.LOGGER.debug("Using fallback AE output overlay for {} at {}",
                        screenTarget.container().getTileEntity().getClass().getName(),
                        screenTarget.container().getTileEntity().getBlockPos());
                return new OverlayTarget(screenTarget.gui(), sideConfig, screenTarget.container().getTileEntity(), screenTarget.output(), type);
            }
        }
        return null;
    }

    private static ScreenTarget findScreenTarget(Screen screen) {
        if (!(screen instanceof GuiMekanism<?> gui) || !(gui.getMenu() instanceof MekanismTileContainer<?> container) ||
                !(container.getTileEntity() instanceof ISideConfiguration sideConfig)) {
            return null;
        }
        Object output = container.getTileEntity();
        if (!(output instanceof MeAeMachine) && !(output instanceof MeFactoryAeMachine)) {
            return null;
        }
        return new ScreenTarget(gui, container, output, sideConfig);
    }

    private static TransmissionType getCurrentType(GuiSideConfiguration<?> sideConfig) {
        for (GuiElement child : sideConfig.children()) {
            if (child instanceof GuiConfigTypeTab tab && !tab.visible) {
                return tab.getTransmissionType();
            }
        }
        for (GuiElement child : sideConfig.children()) {
            if (child instanceof GuiConfigTypeTab tab) {
                return tab.getTransmissionType();
            }
        }
        return TransmissionType.ITEM;
    }

    private static boolean shouldRender(TransmissionType type) {
        return canToggle(type) || type == TransmissionType.FLUID;
    }

    private static boolean canToggle(TransmissionType type) {
        return type == TransmissionType.ITEM || type == TransmissionType.CHEMICAL;
    }

    private static boolean isTypeEnabled(AeOutputMode mode, TransmissionType type) {
        return switch (type) {
            case ITEM -> mode.items();
            case CHEMICAL -> mode.chemicals();
            default -> false;
        };
    }

    private static void sendToggle(OverlayTarget target) {
        PacketDistributor.sendToServer(new CycleAeOutputTypePacket(target.tile().getBlockPos(), target.type()));
    }

    private static AeOutputMode getAeOutputMode(Object output) {
        if (output instanceof MeAeMachine machine) {
            return machine.getAeOutputMode();
        }
        if (output instanceof MeFactoryAeMachine machine) {
            return machine.getAeOutputMode();
        }
        return AeOutputMode.NONE;
    }

    private static ButtonBounds bounds(GuiMekanism<?> gui, GuiSideConfiguration<?> sideConfig) {
        return new ButtonBounds(gui.getGuiLeft() + sideConfig.getRelativeX() + BUTTON_X_OFFSET,
                gui.getGuiTop() + sideConfig.getRelativeY() + BUTTON_Y_OFFSET, BUTTON_SIZE);
    }

    private record ScreenTarget(GuiMekanism<?> gui, MekanismTileContainer<?> container, Object output, ISideConfiguration sideConfig) {
    }

    private record OverlayTarget(GuiMekanism<?> gui, GuiSideConfiguration<?> sideConfig,
                                 net.minecraft.world.level.block.entity.BlockEntity tile, Object output,
                                 TransmissionType type) {
    }

    private record ButtonBounds(int x, int y, int size) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.size && mouseY >= this.y && mouseY < this.y + this.size;
        }
    }

    private static final class AeOutputText extends GuiElement {
        private OverlayTarget target;

        private AeOutputText(GuiMekanism<?> gui, int x, int y) {
            super(gui, x, y, AE_TEXT_WIDTH, 8);
        }

        @Override
        public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            super.renderForeground(guiGraphics, mouseX, mouseY);
            if (this.target == null || !canToggle(this.target.type())) {
                return;
            }
            Component text = Component.literal(isTypeEnabled(getAeOutputMode(this.target.output()), this.target.type()) ? "AE: 开" : "AE: 关");
            drawScaledScrollingString(guiGraphics, text, 0, 0, TextAlignment.RIGHT, screenTextColor(), getWidth(), 1, false, 0.8F);
        }
    }

    private static final class AeOutputButton extends MekanismButton {
        private OverlayTarget target;

        private AeOutputButton(GuiMekanism<?> gui, int x, int y) {
            super(gui, x, y, BUTTON_SIZE, BUTTON_SIZE, Component.empty(), (element, mouseX, mouseY) -> {
                if (element instanceof AeOutputButton button && button.target != null && canToggle(button.target.type())) {
                    sendToggle(button.target);
                    return true;
                }
                return false;
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
