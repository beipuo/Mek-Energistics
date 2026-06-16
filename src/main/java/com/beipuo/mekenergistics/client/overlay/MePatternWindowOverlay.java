package com.beipuo.mekenergistics.client.overlay;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.me.common.StackSizeRenderer;
import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.support.MePatternDecodeHelper;
import com.beipuo.mekenergistics.config.MekEnergisticsConfig;
import com.beipuo.mekenergistics.network.packet.SetPatternTerminalNamePacket;
import com.beipuo.mekenergistics.network.packet.SetSmartPatternMultiplicationPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import mekanism.client.SpecialColors;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.GuiUtils;
import mekanism.client.gui.element.button.MekanismImageButton;
import mekanism.client.gui.element.slot.GuiVirtualSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.gui.element.text.GuiTextField;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = MekEnergistics.MODID, value = Dist.CLIENT)
public final class MePatternWindowOverlay {
    private static final Component PATTERN_BUTTON_TOOLTIP = Component.translatable("gui.mekenergistics.me_patterns.button");
    private static final Component PATTERN_WINDOW_TITLE = Component.translatable("gui.mekenergistics.me_patterns.title");
    private static final Component RENAME_BUTTON_TOOLTIP = Component.translatable("gui.mekenergistics.me_patterns.rename");
    private static final Component SMART_MULTIPLICATION_ON_TOOLTIP = Component.translatable("gui.mekenergistics.me_patterns.smart_multiplication.on");
    private static final Component SMART_MULTIPLICATION_OFF_TOOLTIP = Component.translatable("gui.mekenergistics.me_patterns.smart_multiplication.off");
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
    private static final ResourceLocation CHECK_BUTTON = MekanismUtils.getResource(ResourceType.GUI_BUTTON, "checkmark.png");
    private static final ResourceLocation PATTERN_BUTTON_ICON = ResourceLocation.fromNamespaceAndPath(MekEnergistics.MODID, "textures/gui/button/pattern_button.png");
    private static final ResourceLocation EMPTY_PATTERN_ICON = ResourceLocation.fromNamespaceAndPath(MekEnergistics.MODID, "textures/gui/slot/pattern_empty.png");
    private static final int WINDOW_WIDTH = 178;
    private static final int WINDOW_HEIGHT = 116;
    private static final int NAME_FIELD_WIDTH = 24;
    private static final int NAME_FIELD_HEIGHT = 12;
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

    @SubscribeEvent
    public static void screenOpening(ScreenEvent.Opening event) {
        if (event.getCurrentScreen() instanceof GuiMekanism<?> gui) {
            saveOpenPatternWindows(gui);
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
            return new Target(gui, container, machine.getPatternSlots(), new NameAccess(machine::getCustomPatternTerminalName, machine::setCustomPatternTerminalName),
                    new SmartMultiplicationAccess(machine::isSmartPatternMultiplicationEnabled, machine::setSmartPatternMultiplicationEnabled));
        }
        if (container.getTileEntity() instanceof MeFactoryAeMachine machine) {
            return new Target(gui, container, machine.getPatternSlots(), new NameAccess(machine::getCustomPatternTerminalName, machine::setCustomPatternTerminalName),
                    new SmartMultiplicationAccess(machine::isSmartPatternMultiplicationEnabled, machine::setSmartPatternMultiplicationEnabled));
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

    private static void saveOpenPatternWindows(GuiMekanism<?> gui) {
        for (GuiWindow window : gui.getWindows()) {
            if (window instanceof MePatternWindow patternWindow) {
                patternWindow.saveNameIfDirty();
            }
        }
    }

    private record Target(GuiMekanism<?> gui, MekanismTileContainer<?> container, List<BasicInventorySlot> patternSlots, NameAccess nameAccess,
                          SmartMultiplicationAccess smartMultiplicationAccess) {
    }

    private record NameAccess(Supplier<String> getter, Consumer<String> setter) {
        private String get() {
            return this.getter.get();
        }

        private void set(String name) {
            this.setter.accept(name);
        }
    }

    private record SmartMultiplicationAccess(Supplier<Boolean> getter, Consumer<Boolean> setter) {
        private boolean get() {
            return Boolean.TRUE.equals(this.getter.get());
        }

        private void set(boolean enabled) {
            this.setter.accept(enabled);
        }
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
        private final GuiTextField nameField;
        private final MekanismImageButton smartMultiplicationButton;
        private int currentPage;
        private boolean nameEditorOpen;
        private String lastSavedName;
        private boolean smartMultiplicationEnabled;

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
            this.smartMultiplicationEnabled = this.target.smartMultiplicationAccess().get();
            this.smartMultiplicationButton = addChild(new MekanismImageButton(gui, relativeX + 18, relativeY + 4, 12, CHECK_BUTTON,
                    (element, mouseX, mouseY) -> toggleSmartPatternMultiplication()));
            updateSmartMultiplicationTooltip();
            addChild(new MekanismImageButton(gui, relativeX + 158, relativeY + 4, 12, CHECK_BUTTON, (element, mouseX, mouseY) -> toggleNameEditor()))
                    .setTooltip(Tooltip.create(RENAME_BUTTON_TOOLTIP));
            this.lastSavedName = this.target.nameAccess().get();
            this.nameField = addChild(new GuiTextField(gui, this, relativeX + 132, relativeY + 4, NAME_FIELD_WIDTH, NAME_FIELD_HEIGHT));
            this.nameField.setMaxLength(MeAeMachine.MAX_PATTERN_TERMINAL_NAME_LENGTH);
            this.nameField.setEnterHandler(this::saveName);
            this.nameField.setText(this.lastSavedName);
            setNameFieldVisible(false);
            addChild(new MekanismImageButton(gui, relativeX + 8, relativeY + 94, 12, LEFT_BUTTON, (element, mouseX, mouseY) -> previousPage()));
            addChild(new MekanismImageButton(gui, relativeX + width - 20, relativeY + 94, 12, RIGHT_BUTTON, (element, mouseX, mouseY) -> nextPage()));
        }

        private boolean toggleSmartPatternMultiplication() {
            this.smartMultiplicationEnabled = !this.smartMultiplicationEnabled;
            this.target.smartMultiplicationAccess().set(this.smartMultiplicationEnabled);
            PacketDistributor.sendToServer(new SetSmartPatternMultiplicationPacket(this.target.container().getTileEntity().getBlockPos(), this.smartMultiplicationEnabled));
            updateSmartMultiplicationTooltip();
            return true;
        }

        private void updateSmartMultiplicationTooltip() {
            this.smartMultiplicationButton.setTooltip(Tooltip.create(this.smartMultiplicationEnabled ? SMART_MULTIPLICATION_ON_TOOLTIP : SMART_MULTIPLICATION_OFF_TOOLTIP));
        }

        private boolean toggleNameEditor() {
            this.nameEditorOpen = !this.nameEditorOpen;
            setNameFieldVisible(this.nameEditorOpen);
            if (this.nameEditorOpen) {
                setFocused(this.nameField);
            } else {
                saveNameIfDirty();
            }
            return true;
        }

        private boolean saveName() {
            saveNameIfDirty();
            this.nameEditorOpen = false;
            setNameFieldVisible(false);
            return true;
        }

        private void setNameFieldVisible(boolean visible) {
            this.nameField.visible = visible;
            this.nameField.setVisible(visible);
            if (!visible && getFocused() == this.nameField) {
                setFocused(null);
            }
        }

        private void saveNameIfDirty() {
            String name = MeAeMachine.sanitizePatternTerminalName(this.nameField.getText());
            if (name.equals(this.lastSavedName)) {
                return;
            }
            this.target.nameAccess().set(name);
            PacketDistributor.sendToServer(new SetPatternTerminalNamePacket(this.target.container().getTileEntity().getBlockPos(), name));
            this.lastSavedName = name;
        }

        @Override
        public void close() {
            saveNameIfDirty();
            super.close();
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
            boolean syncedSmartMultiplication = this.target.smartMultiplicationAccess().get();
            if (syncedSmartMultiplication != this.smartMultiplicationEnabled) {
                this.smartMultiplicationEnabled = syncedSmartMultiplication;
                updateSmartMultiplicationTooltip();
            }
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
            ItemStack patternStack = virtualSlot.getStackToRender();
            GenericStack output = getPatternOutput(patternStack);
            if (output != null) {
                int xPos = relativeX + 1;
                int yPos = relativeY + 1;
                if (virtualSlot.shouldDrawOverlay()) {
                    guiGraphics.fill(RenderType.guiOverlay(), xPos, yPos, xPos + 16, yPos + 16, DEFAULT_HOVER_COLOR);
                }
                renderPatternOutput(guiGraphics, output, xPos, yPos);
                return;
            }
            super.drawContents(guiGraphics);
        }

        private static GenericStack getPatternOutput(ItemStack patternStack) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return null;
            }
            var pattern = MePatternDecodeHelper.safeDecode(patternStack, minecraft.level, "pattern window preview");
            if (pattern == null || pattern.getOutputs().isEmpty()) {
                return null;
            }
            return pattern.getPrimaryOutput();
        }

        private static void renderPatternOutput(GuiGraphics guiGraphics, GenericStack output, int xPos, int yPos) {
            Minecraft minecraft = Minecraft.getInstance();
            AEKeyRendering.drawInGui(minecraft, guiGraphics, xPos, yPos, output.what());
            if (output.amount() > 0) {
                String amountText = output.what().formatAmount(output.amount(), AmountFormat.SLOT);
                StackSizeRenderer.renderSizeLabel(guiGraphics, minecraft.font, xPos, yPos, amountText, false);
            }
        }
    }
}
