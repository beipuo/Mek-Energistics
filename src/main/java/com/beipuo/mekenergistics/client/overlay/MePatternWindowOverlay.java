package com.beipuo.mekenergistics.client.overlay;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.config.MekEnergisticsConfig;
import java.util.ArrayList;
import java.util.List;
import mekanism.client.SpecialColors;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.GuiUtils;
import mekanism.client.gui.element.button.MekanismImageButton;
import mekanism.client.gui.element.slot.GuiVirtualSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.client.render.IFancyFontRenderer.TextAlignment;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.inventory.container.IGUIWindow;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.inventory.container.slot.InventoryContainerSlot;
import mekanism.common.inventory.container.slot.IVirtualSlot;
import mekanism.common.inventory.container.slot.VirtualInventoryContainerSlot;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.Rect2i;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = MekEnergistics.MODID, value = Dist.CLIENT)
public final class MePatternWindowOverlay {
    private static final Component PATTERN_BUTTON_TOOLTIP = Component.translatable("gui.mekenergistics.me_patterns.button");
    private static final Component PATTERN_WINDOW_TITLE = Component.translatable("gui.mekenergistics.me_patterns.title");
    private static final int TAB_X = 0;
    private static final int TAB_Y = 62;
    private static final int TAB_SIZE = 26;
    private static final int INNER_SIZE = 18;
    private static final int INNER_X_OFFSET = 3;
    private static final int INNER_Y_OFFSET = 4;
    private static final ResourceLocation HOLDER_RIGHT = MekanismUtils.getResource(ResourceType.GUI, "holder_right.png");
    private static final ResourceLocation BUTTON = MekanismUtils.getResource(ResourceType.GUI, "button.png");
    private static final ResourceLocation LEFT_BUTTON = MekanismUtils.getResource(ResourceType.GUI_BUTTON, "left.png");
    private static final ResourceLocation RIGHT_BUTTON = MekanismUtils.getResource(ResourceType.GUI_BUTTON, "right.png");
    private static final ResourceLocation PATTERN_BUTTON_ICON = ResourceLocation.fromNamespaceAndPath(MekEnergistics.MODID, "textures/gui/button/pattern_button.png");
    private static final ResourceLocation EMPTY_PATTERN_ICON = ResourceLocation.fromNamespaceAndPath(MekEnergistics.MODID, "textures/gui/slot/pattern_empty.png");
    private static final int WINDOW_WIDTH = 178;
    private static final int WINDOW_HEIGHT = 116;
    private static final int SLOT_COLUMNS = MekEnergisticsConfig.PATTERN_SLOT_COLUMNS;
    private static final int SLOT_ROWS = MekEnergisticsConfig.PATTERN_SLOT_ROWS;
    private static final int SLOTS_PER_PAGE = MekEnergisticsConfig.PATTERN_SLOTS_PER_PAGE;

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
        boolean patternWindowOpen = hasPatternWindow(target.gui());
        MekanismRenderer.color(graphics, SpecialColors.TAB_UPGRADE);
        GuiUtils.blitNineSlicedSized(graphics, HOLDER_RIGHT, bounds.tabX(), bounds.tabY(), TAB_SIZE, TAB_SIZE, 4, 26, 9, 0, 0, 26, 9);
        MekanismRenderer.resetColor(graphics);
        int buttonTextureY = patternWindowOpen || bounds.contains(event.getMouseX(), event.getMouseY()) ? 40 : 20;
        GuiUtils.blitNineSlicedSized(graphics, BUTTON, bounds.buttonX(), bounds.buttonY(), INNER_SIZE, INNER_SIZE, 20, 4, 200, 20, 0, buttonTextureY, 200, 60);
        drawPatternIcon(graphics, target.gui(), bounds);
        if (bounds.contains(event.getMouseX(), event.getMouseY())) {
            graphics.renderTooltip(target.gui().font(), PATTERN_BUTTON_TOOLTIP, event.getMouseX(), event.getMouseY());
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

    public static boolean hasPatternTarget(Screen screen) {
        return findTarget(screen) != null;
    }

    public static Rect2i jeiButtonArea(GuiMekanism<?> gui) {
        ButtonBounds bounds = buttonBounds(gui);
        return new Rect2i(bounds.tabX(), bounds.tabY(), TAB_SIZE, TAB_SIZE);
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
        graphics.blit(PATTERN_BUTTON_ICON, bounds.buttonX() + 1, bounds.buttonY() + 1, 0, 0, 16, 16, 16, 16);
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
        private final List<PatternGuiVirtualSlot> slots = new ArrayList<>(SLOTS_PER_PAGE);
        private int currentPage;

        private MePatternWindow(IGuiWrapper gui, int x, int y, Target target) {
            super(gui, x, y, WINDOW_WIDTH, WINDOW_HEIGHT, SelectedWindowData.UNSPECIFIED);
            this.target = target;
            interactionStrategy = InteractionStrategy.ALL;
            for (int row = 0; row < SLOT_ROWS; row++) {
                for (int column = 0; column < SLOT_COLUMNS; column++) {
                    int index = row * SLOT_COLUMNS + column;
                    this.slots.add(addChild(new PatternGuiVirtualSlot(this, SlotType.NORMAL, gui, relativeX + 8 + column * 18, relativeY + 18 + row * 18,
                            getPatternContainerSlot(index))));
                }
            }
            addChild(new MekanismImageButton(gui, relativeX + 8, relativeY + 94, 12, LEFT_BUTTON, (element, mouseX, mouseY) -> previousPage()));
            addChild(new MekanismImageButton(gui, relativeX + width - 20, relativeY + 94, 12, RIGHT_BUTTON, (element, mouseX, mouseY) -> nextPage()));
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

        private int pageCount() {
            return Math.max(1, (this.target.patternSlots().size() + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE);
        }

        private boolean previousPage() {
            if (this.currentPage <= 0) {
                return false;
            }
            this.currentPage--;
            updatePageSlots();
            return true;
        }

        private boolean nextPage() {
            if (this.currentPage + 1 >= pageCount()) {
                return false;
            }
            this.currentPage++;
            updatePageSlots();
            return true;
        }

        private void updatePageSlots() {
            int pageOffset = this.currentPage * SLOTS_PER_PAGE;
            for (int i = 0; i < this.slots.size(); i++) {
                this.slots.get(i).updateVirtualSlot(this, getPatternContainerSlot(pageOffset + i));
            }
        }

        @Override
        public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            super.renderForeground(guiGraphics, mouseX, mouseY);
            drawTitleText(guiGraphics, PATTERN_WINDOW_TITLE, 6);
            drawScrollingString(guiGraphics, Component.literal((this.currentPage + 1) + "/" + pageCount()), 20, 99, TextAlignment.CENTER, titleTextColor(), width - 40, 0, false);
        }
    }

    private static final class PatternGuiVirtualSlot extends GuiVirtualSlot {
        private IVirtualSlot virtualSlot;

        private PatternGuiVirtualSlot(IGUIWindow window, SlotType type, IGuiWrapper gui, int x, int y, VirtualInventoryContainerSlot containerSlot) {
            super(window, type, gui, x, y, containerSlot);
        }

        @Override
        public void updateVirtualSlot(IGUIWindow window, IVirtualSlot virtualSlot) {
            super.updateVirtualSlot(window, virtualSlot);
            this.virtualSlot = virtualSlot;
        }

        @Override
        protected void drawContents(GuiGraphics guiGraphics) {
            if (virtualSlot == null || virtualSlot.getStackToRender().isEmpty()) {
                guiGraphics.blit(EMPTY_PATTERN_ICON, relativeX + 1, relativeY + 1, 0, 0, 16, 16, 16, 16);
                return;
            }
            super.drawContents(guiGraphics);
        }
    }
}
