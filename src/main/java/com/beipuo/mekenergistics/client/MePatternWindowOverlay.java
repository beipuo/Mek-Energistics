package com.beipuo.mekenergistics.client;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.MeFactoryAeMachine;
import java.util.List;
import mekanism.client.SpecialColors;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.GuiUtils;
import mekanism.client.gui.element.slot.GuiVirtualSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.inventory.container.slot.InventoryContainerSlot;
import mekanism.common.inventory.container.slot.VirtualInventoryContainerSlot;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = MekEnergistics.MODID, value = Dist.CLIENT)
public final class MePatternWindowOverlay {
    private static final int TAB_X = 0;
    private static final int TAB_Y = 62;
    private static final int TAB_SIZE = 26;
    private static final int INNER_SIZE = 18;
    private static final int INNER_X_OFFSET = 3;
    private static final int INNER_Y_OFFSET = 4;
    private static final float ICON_SCALE = 1.35F;
    private static final ResourceLocation HOLDER_RIGHT = MekanismUtils.getResource(ResourceType.GUI, "holder_right.png");
    private static final ResourceLocation BUTTON = MekanismUtils.getResource(ResourceType.GUI, "button.png");
    private static final int WINDOW_WIDTH = 178;
    private static final int WINDOW_HEIGHT = 98;
    private static final int SLOT_COLUMNS = 9;
    private static final int SLOT_ROWS = 4;

    private MePatternWindowOverlay() {
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        Target target = findTarget(event.getScreen());
        if (target == null) {
            return;
        }
        ButtonBounds bounds = buttonBounds(target.gui());
        GuiGraphics graphics = event.getGuiGraphics();
        MekanismRenderer.color(graphics, SpecialColors.TAB_UPGRADE);
        GuiUtils.blitNineSlicedSized(graphics, HOLDER_RIGHT, bounds.tabX(), bounds.tabY(), TAB_SIZE, TAB_SIZE, 4, 26, 9, 0, 0, 26, 9);
        MekanismRenderer.resetColor(graphics);
        int buttonTextureY = bounds.contains(event.getMouseX(), event.getMouseY()) ? 40 : 20;
        GuiUtils.blitNineSlicedSized(graphics, BUTTON, bounds.buttonX(), bounds.buttonY(), INNER_SIZE, INNER_SIZE, 20, 4, 200, 20, 0, buttonTextureY, 200, 60);
        drawPatternIcon(graphics, target.gui(), bounds);
        if (bounds.contains(event.getMouseX(), event.getMouseY())) {
            graphics.renderTooltip(target.gui().font(), Component.literal("样板"), event.getMouseX(), event.getMouseY());
        }
    }

    @SubscribeEvent
    public static void mouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        Target target = findTarget(event.getScreen());
        if (target != null && buttonBounds(target.gui()).contains(event.getMouseX(), event.getMouseY())) {
            openPatternWindow(target.gui(), target);
            event.setCanceled(true);
        }
    }

    private static Target findTarget(Screen screen) {
        if (!(screen instanceof GuiMekanism<?> gui) || !(gui.getMenu() instanceof MekanismTileContainer<?> container)) {
            return null;
        }
        if (container.getTileEntity() instanceof MeAeMachine machine) {
            return new Target(gui, container, machine.getPatternSlots());
        }
        if (container.getTileEntity() instanceof MeFactoryAeMachine machine) {
            return new Target(gui, container, machine.getPatternSlots());
        }
        return null;
    }

    private static void openPatternWindow(GuiMekanism<?> gui, Target target) {
        if (hasPatternWindow(gui)) {
            return;
        }
        gui.addWindow(new MePatternWindow(gui, (gui.getXSize() - WINDOW_WIDTH) / 2, 8, target));
    }

    private static ButtonBounds buttonBounds(GuiMekanism<?> gui) {
        return new ButtonBounds(gui.getGuiLeft() + gui.getXSize() + TAB_X, gui.getGuiTop() + TAB_Y);
    }

    private static void drawPatternIcon(GuiGraphics graphics, GuiMekanism<?> gui, ButtonBounds bounds) {
        String icon = "P";
        float iconWidth = gui.font().width(icon) * ICON_SCALE;
        float iconHeight = gui.font().lineHeight * ICON_SCALE;
        float x = bounds.buttonX() + (INNER_SIZE - iconWidth) / 2F;
        float y = bounds.buttonY() + (INNER_SIZE - iconHeight) / 2F + 0.5F;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(ICON_SCALE, ICON_SCALE, 1);
        graphics.drawString(gui.font(), icon, 0, 0, 0x222222, false);
        graphics.pose().popPose();
    }

    private static boolean hasPatternWindow(GuiMekanism<?> gui) {
        for (GuiWindow window : gui.getWindows()) {
            if (window instanceof MePatternWindow) {
                return true;
            }
        }
        return false;
    }

    private record Target(GuiMekanism<?> gui, MekanismTileContainer<?> container, List<BasicInventorySlot> patternSlots) {
    }

    private record ButtonBounds(int x, int y) {
        private int tabX() {
            return this.x;
        }

        private int tabY() {
            return this.y;
        }

        private int buttonX() {
            return this.x + INNER_X_OFFSET;
        }

        private int buttonY() {
            return this.y + INNER_Y_OFFSET;
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= buttonX() && mouseX < buttonX() + INNER_SIZE && mouseY >= buttonY() && mouseY < buttonY() + INNER_SIZE;
        }
    }

    private static final class MePatternWindow extends GuiWindow {
        private final Target target;

        private MePatternWindow(IGuiWrapper gui, int x, int y, Target target) {
            super(gui, x, y, WINDOW_WIDTH, WINDOW_HEIGHT, SelectedWindowData.UNSPECIFIED);
            this.target = target;
            interactionStrategy = InteractionStrategy.ALL;
            for (int row = 0; row < SLOT_ROWS; row++) {
                for (int column = 0; column < SLOT_COLUMNS; column++) {
                    int index = row * SLOT_COLUMNS + column;
                    addChild(new GuiVirtualSlot(this, SlotType.NORMAL, gui, relativeX + 8 + column * 18, relativeY + 18 + row * 18,
                            getPatternContainerSlot(index)));
                }
            }
        }

        private VirtualInventoryContainerSlot getPatternContainerSlot(int index) {
            if (index < 0 || index >= this.target.patternSlots().size()) {
                return null;
            }
            BasicInventorySlot patternSlot = this.target.patternSlots().get(index);
            for (InventoryContainerSlot containerSlot : this.target.container().getInventoryContainerSlots()) {
                if (containerSlot.getInventorySlot() == patternSlot && containerSlot instanceof VirtualInventoryContainerSlot virtualSlot) {
                    return virtualSlot;
                }
            }
            return null;
        }

        @Override
        public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            super.renderForeground(guiGraphics, mouseX, mouseY);
            drawTitleText(guiGraphics, Component.literal("ME Patterns"), 6);
        }
    }
}
