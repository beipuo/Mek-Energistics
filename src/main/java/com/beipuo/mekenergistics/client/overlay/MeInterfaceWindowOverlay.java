package com.beipuo.mekenergistics.client.overlay;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.me.common.StackSizeRenderer;
import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.support.AbstractMeAeSupport;
import com.beipuo.mekenergistics.config.MekEnergisticsConfig;
import com.beipuo.mekenergistics.network.packet.InterfaceConfigSyncPacket;
import com.beipuo.mekenergistics.network.packet.RequestInterfaceConfigPacket;
import com.beipuo.mekenergistics.network.packet.SetInterfaceConfigPacket;
import com.beipuo.mekenergistics.upgrade.MeInterfaceConfig;
import java.util.ArrayList;
import java.util.List;
import mekanism.client.SpecialColors;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.GuiUtils;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.button.MekanismImageButton;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.gui.element.text.GuiTextField;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.client.render.IFancyFontRenderer.TextAlignment;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Client overlay for the ME output interface upgrade. When a machine runs in interface mode the
 * pattern tab is replaced by a nine-column interface window. Each column contains an amount
 * button, a virtual configuration slot, and a read-only stocked inventory slot.
 */
@EventBusSubscriber(modid = MekEnergistics.MODID, value = Dist.CLIENT)
public final class MeInterfaceWindowOverlay {
    private static final Component INTERFACE_BUTTON_TOOLTIP = Component.translatable("gui.mekenergistics.me_interface.button");
    private static final Component INTERFACE_WINDOW_TITLE = Component.translatable("gui.mekenergistics.me_interface.title");
    private static final Component INTERFACE_SLOT_HINT = Component.translatable("gui.mekenergistics.me_interface.slot_hint");
    private static final Component INTERFACE_AMOUNT_TOOLTIP = Component.translatable("gui.mekenergistics.me_interface.amount");
    private static final int TAB_X = 0;
    private static final int TAB_Y = 62;
    private static final int TAB_SIZE = 26;
    private static final int INNER_SIZE = 18;
    private static final int INNER_X_OFFSET = 3;
    private static final int INNER_Y_OFFSET = 4;
    private static final int WINDOW_WIDTH = 178;
    private static final int WINDOW_HEIGHT = 118;
    private static final int SLOT_COLUMNS = MekEnergisticsConfig.PATTERN_SLOT_COLUMNS;
    private static final int CONFIG_SLOTS = MeInterfaceConfig.SLOT_COUNT;
    private static final int NAME_FIELD_HEIGHT = 12;
    private static final int ROW_Y = 18;
    private static final int CONFIG_ROW_Y = ROW_Y + 18;
    private static final int INVENTORY_ROW_Y = CONFIG_ROW_Y + 18;
    private static final int AMOUNT_FIELD_Y = 96;
    private static final ResourceLocation HOLDER_RIGHT = MekanismUtils.getResource(ResourceType.GUI, "holder_right.png");
    private static final ResourceLocation BUTTON = MekanismUtils.getResource(ResourceType.GUI, "button.png");
    private static final ResourceLocation LEFT_BUTTON = MekanismUtils.getResource(ResourceType.GUI_BUTTON, "left.png");
    private static final ResourceLocation RIGHT_BUTTON = MekanismUtils.getResource(ResourceType.GUI_BUTTON, "right.png");
    private static final ResourceLocation INTERFACE_BUTTON_ICON = ResourceLocation.fromNamespaceAndPath(
            MekEnergistics.MODID, "textures/item/upgrade_me_output_interface.png");
    private static final ResourceLocation EMPTY_INTERFACE_ICON = ResourceLocation.fromNamespaceAndPath(
            MekEnergistics.MODID, "textures/gui/slot/pattern_empty.png");
    private static final ResourceLocation AE2_STATES = ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/states.png");

    private MeInterfaceWindowOverlay() {
    }

    /** Applies a server config snapshot to the matching open interface window. */
    public static void handleConfigSync(InterfaceConfigSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (!(minecraft.screen instanceof GuiMekanism<?> gui)) {
                return;
            }
            for (GuiWindow window : gui.getWindows()) {
                if (window instanceof MeInterfaceWindow interfaceWindow && interfaceWindow.matches(packet.pos())) {
                    interfaceWindow.applyState(packet.config(), packet.inventory());
                    return;
                }
            }
        });
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        Target target = findTarget(event.getScreen());
        if (target == null) {
            if (event.getScreen() instanceof GuiMekanism<?> gui) {
                closeOpenInterfaceWindows(gui);
            }
            return;
        }
        ButtonBounds bounds = buttonBounds(target.gui());
        GuiGraphics graphics = event.getGuiGraphics();
        boolean windowOpen = hasInterfaceWindow(target.gui());
        MekanismRenderer.color(graphics, SpecialColors.TAB_UPGRADE);
        GuiUtils.blitNineSlicedSized(graphics, HOLDER_RIGHT, bounds.tabX(), bounds.tabY(), TAB_SIZE, TAB_SIZE, 4, 26, 9, 0, 0, 26, 9);
        MekanismRenderer.resetColor(graphics);
        int buttonTextureY = windowOpen || bounds.contains(event.getMouseX(), event.getMouseY()) ? 40 : 20;
        GuiUtils.blitNineSlicedSized(graphics, BUTTON, bounds.buttonX(), bounds.buttonY(), INNER_SIZE, INNER_SIZE, 20, 4, 200, 20, 0, buttonTextureY, 200, 60);
        graphics.blit(INTERFACE_BUTTON_ICON, bounds.buttonX() + 1, bounds.buttonY() + 1, 0, 0, 16, 16, 16, 16);
        if (bounds.contains(event.getMouseX(), event.getMouseY())) {
            graphics.renderTooltip(target.gui().font(), INTERFACE_BUTTON_TOOLTIP, event.getMouseX(), event.getMouseY());
        }
    }

    @SubscribeEvent
    public static void mouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        Target target = findTarget(event.getScreen());
        if (target != null && buttonBounds(target.gui()).contains(event.getMouseX(), event.getMouseY())) {
            openInterfaceWindow(target.gui(), target);
            event.setCanceled(true);
        }
    }

    public static boolean isInterfaceMachine(Screen screen) {
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
        if (!(container.getTileEntity() instanceof MeAeMachine machine)) {
            return null;
        }
        AbstractMeAeSupport<?> support = machine.getRecipeAeSupport();
        if (!Boolean.TRUE.equals(support.getClientInterfaceMode())) {
            return null;
        }
        return new Target(gui, container, machine);
    }

    private static void openInterfaceWindow(GuiMekanism<?> gui, Target target) {
        if (hasInterfaceWindow(gui)) {
            return;
        }
        gui.addWindow(new MeInterfaceWindow(gui, (gui.getXSize() - WINDOW_WIDTH) / 2, 8, target));
    }

    private static boolean hasInterfaceWindow(GuiMekanism<?> gui) {
        for (GuiWindow window : gui.getWindows()) {
            if (window instanceof MeInterfaceWindow) {
                return true;
            }
        }
        return false;
    }

    private static void closeOpenInterfaceWindows(GuiMekanism<?> gui) {
        if (!hasInterfaceWindow(gui)) {
            return; // the common case, every frame: avoid copying the window list
        }
        for (GuiWindow window : new ArrayList<>(gui.getWindows())) {
            if (window instanceof MeInterfaceWindow interfaceWindow) {
                interfaceWindow.close();
            }
        }
    }

    private static ButtonBounds buttonBounds(GuiMekanism<?> gui) {
        return new ButtonBounds(gui.getGuiLeft() + gui.getXSize() + TAB_X, gui.getGuiTop() + TAB_Y);
    }

    private record Target(GuiMekanism<?> gui, MekanismTileContainer<?> container, MeAeMachine machine) {
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

    private static final class MeInterfaceWindow extends GuiWindow {
        private final Target target;
        private final List<GenericStack> config = new ArrayList<>();
        private final List<GenericStack> inventory = new ArrayList<>();
        private int editingSlot = -1;
        private int refreshTicks;
        private final GuiTextField amountField;

        private MeInterfaceWindow(IGuiWrapper gui, int x, int y, Target target) {
            super(gui, x, y, WINDOW_WIDTH, WINDOW_HEIGHT, SelectedWindowData.UNSPECIFIED);
            this.target = target;
            interactionStrategy = InteractionStrategy.ALL;
            for (int i = 0; i < CONFIG_SLOTS; i++) {
                this.config.add(null);
                this.inventory.add(null);
            }
            this.amountField = addChild(new GuiTextField(gui, this, relativeX + 60,
                    relativeY + AMOUNT_FIELD_Y, 60, NAME_FIELD_HEIGHT));
            this.amountField.setMaxLength(12);
            this.amountField.setInputValidator(value -> value >= '0' && value <= '9');
            this.amountField.setEnterHandler(this::commitAmount);
            this.amountField.setVisible(false);
            requestState();
        }

        private boolean matches(net.minecraft.core.BlockPos pos) {
            return this.target.machine().getAeOwnerTile().getBlockPos().equals(pos);
        }

        private void applyState(List<GenericStack> configuredStacks, List<GenericStack> inventoryStacks) {
            this.config.clear();
            this.inventory.clear();
            for (int i = 0; i < CONFIG_SLOTS; i++) {
                this.config.add(i < configuredStacks.size() ? configuredStacks.get(i) : null);
                this.inventory.add(i < inventoryStacks.size() ? inventoryStacks.get(i) : null);
            }
        }

        private void requestState() {
            PacketDistributor.sendToServer(new RequestInterfaceConfigPacket(
                    this.target.machine().getAeOwnerTile().getBlockPos()));
        }

        @Override
        public void tick() {
            super.tick();
            if (++this.refreshTicks >= 10) {
                this.refreshTicks = 0;
                requestState();
            }
        }

        private void setSlot(int slot, @Nullable GenericStack stack) {
            if (slot < 0 || slot >= this.config.size()) {
                return;
            }
            this.config.set(slot, stack);
            PacketDistributor.sendToServer(new SetInterfaceConfigPacket(
                    this.target.machine().getAeOwnerTile().getBlockPos(), slot,
                    stack == null ? null : stack.what(), stack == null ? 0 : stack.amount()));
        }

        private boolean markSlot(int slot) {
            if (Minecraft.getInstance().player == null) {
                return false;
            }
            ItemStack carried = Minecraft.getInstance().player.containerMenu.getCarried();
            if (carried.isEmpty()) {
                return false;
            }
            AEItemKey key = AEItemKey.of(carried);
            setSlot(slot, new GenericStack(key, key.getMaxStackSize()));
            return true;
        }

        private boolean clearSlot(int slot) {
            setSlot(slot, null);
            return true;
        }

        private boolean startEditing(int slot) {
            GenericStack current = this.config.get(slot);
            if (current == null || current.what() == null) {
                return false;
            }
            this.editingSlot = slot;
            this.amountField.setText(Long.toString(current.amount()));
            this.amountField.setVisible(true);
            setFocused(this.amountField);
            return true;
        }

        private boolean cancelEditing() {
            this.editingSlot = -1;
            this.amountField.setVisible(false);
            if (getFocused() == this.amountField) {
                setFocused(null);
            }
            return true;
        }

        private boolean commitAmount() {
            if (this.editingSlot < 0) {
                return true;
            }
            int slot = this.editingSlot;
            GenericStack current = this.config.get(slot);
            cancelEditing();
            if (current == null || current.what() == null) {
                return true;
            }
            long amount = parseLong(this.amountField.getText(), -1);
            if (amount <= 0) {
                setSlot(slot, null);
            } else {
                setSlot(slot, new GenericStack(current.what(), Math.min(amount, Long.MAX_VALUE)));
            }
            return true;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int configSlot = configSlotAt(mouseX, mouseY);
            int amountColumn = columnAt(mouseX, mouseY, ROW_Y, 18);
            int amountSlot = amountButtonAt(mouseX, mouseY);
            int inventorySlot = columnAt(mouseX, mouseY, INVENTORY_ROW_Y, 18);
            if (this.editingSlot >= 0 && configSlot != this.editingSlot && amountSlot != this.editingSlot) {
                commitAmount();
            }
            if (amountSlot >= 0 && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                return startEditing(amountSlot);
            }
            if (configSlot >= 0) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    markSlot(configSlot);
                    return true;
                }
                if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    return clearSlot(configSlot);
                }
            }
            if (this.editingSlot >= 0 && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                return cancelEditing();
            }
            if (amountColumn >= 0 || configSlot >= 0 || inventorySlot >= 0) {
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (this.editingSlot >= 0 && keyCode == GLFW.GLFW_KEY_ESCAPE) {
                return cancelEditing();
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            super.renderForeground(guiGraphics, mouseX, mouseY);
            int titleWidth = font().width(INTERFACE_WINDOW_TITLE);
            int titleX = (width - titleWidth) / 2;
            drawScrollingString(guiGraphics, INTERFACE_WINDOW_TITLE, titleX, 6, TextAlignment.LEFT,
                    titleTextColor(), titleWidth, 0, false);
            Minecraft minecraft = Minecraft.getInstance();
            int slotWidth = SlotType.NORMAL.getWidth();
            int slotHeight = SlotType.NORMAL.getHeight();
            for (int column = 0; column < SLOT_COLUMNS; column++) {
                int slotX = relativeX + 8 + column * 18;
                GenericStack configured = this.config.get(column);
                boolean buttonHovered = amountButtonAt(mouseX, mouseY) == column;
                if (configured != null && configured.what() != null) {
                    guiGraphics.blit(AE2_STATES, slotX + 1, relativeY + ROW_Y + 1,
                            buttonHovered ? 32 : 48, 64, 16, 16, 256, 256);
                }
                drawSlot(guiGraphics, minecraft, slotX, relativeY + CONFIG_ROW_Y,
                        slotWidth, slotHeight, configured, true);
                drawSlot(guiGraphics, minecraft, slotX, relativeY + INVENTORY_ROW_Y,
                        slotWidth, slotHeight, this.inventory.get(column), false);
            }
            if (amountButtonAt(mouseX, mouseY) >= 0) {
                guiGraphics.renderTooltip(font(), INTERFACE_AMOUNT_TOOLTIP, mouseX - getGuiLeft(), mouseY - getGuiTop());
            }
            drawScrollingString(guiGraphics, INTERFACE_SLOT_HINT, 8, 110, TextAlignment.LEFT,
                    titleTextColor(), width - 16, 0, false);
        }

        private void drawSlot(GuiGraphics guiGraphics, Minecraft minecraft, int slotX, int slotY,
                int slotWidth, int slotHeight, @Nullable GenericStack stack, boolean showConfiguredAmount) {
            guiGraphics.blit(SlotType.NORMAL.getTexture(), slotX, slotY, 0, 0,
                    slotWidth, slotHeight, slotWidth, slotHeight);
            if (stack == null || stack.what() == null) {
                return;
            }
            AEKeyRendering.drawInGui(minecraft, guiGraphics, slotX + 1, slotY + 1, stack.what());
            if (stack.amount() > 0 && (showConfiguredAmount || stack.amount() > 1)) {
                String amountText = stack.what().formatAmount(stack.amount(), AmountFormat.SLOT);
                StackSizeRenderer.renderSizeLabel(guiGraphics, minecraft.font,
                        slotX + 1, slotY + 1, amountText, false);
            }
        }

        private int configSlotAt(double mouseX, double mouseY) {
            return columnAt(mouseX, mouseY, CONFIG_ROW_Y, 18);
        }

        private int amountButtonAt(double mouseX, double mouseY) {
            int column = columnAt(mouseX, mouseY, ROW_Y, 18);
            return column >= 0 && this.config.get(column) != null ? column : -1;
        }

        private int columnAt(double mouseX, double mouseY, int rowY, int rowHeight) {
            int slotX = (int) mouseX - (getX() + 8);
            int slotY = (int) mouseY - (getY() + rowY);
            if (slotX < 0 || slotY < 0 || slotX >= SLOT_COLUMNS * 18 || slotY >= rowHeight) {
                return -1;
            }
            int column = slotX / 18;
            return column < CONFIG_SLOTS ? column : -1;
        }

        private static long parseLong(String value, long fallback) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
    }
}
