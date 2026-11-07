package com.beipuo.mekenergistics.client.overlay;

import com.beipuo.mekenergistics.upgrade.MePassiveCraftingSettings;
import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.me.common.StackSizeRenderer;
import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.block.MeMekanismMachineBlock;
import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeUpgradeableMachine;
import com.beipuo.mekenergistics.blockentity.support.MePatternDecodeHelper;
import com.beipuo.mekenergistics.config.MekEnergisticsConfig;
import com.beipuo.mekenergistics.network.packet.SetAeOutputModePacket;
import com.beipuo.mekenergistics.network.packet.SetPatternTerminalNamePacket;
import com.beipuo.mekenergistics.network.packet.SetPassiveCraftingSettingsPacket;
import com.beipuo.mekenergistics.network.packet.SetSmartPatternMultiplicationPacket;
import com.beipuo.mekenergistics.network.packet.SetTerminalVisibilityPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import mekanism.client.SpecialColors;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.GuiUtils;
import mekanism.client.gui.element.button.MekanismButton;
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
import mekanism.common.lib.transmitter.TransmissionType;
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
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = MekEnergistics.MODID, value = Dist.CLIENT)
public final class MePatternWindowOverlay {
    private static final Component PATTERN_BUTTON_TOOLTIP = Component.translatable("gui.mekenergistics.me_patterns.button");
    private static final Component PATTERN_WINDOW_TITLE = Component.translatable("gui.mekenergistics.me_patterns.title");
    private static final Component RENAME_BUTTON_TOOLTIP = Component.translatable("gui.mekenergistics.me_patterns.rename");
    private static final Component SMART_MULTIPLICATION_ON_TOOLTIP = Component.translatable("gui.mekenergistics.me_patterns.smart_multiplication.on");
    private static final Component SMART_MULTIPLICATION_OFF_TOOLTIP = Component.translatable("gui.mekenergistics.me_patterns.smart_multiplication.off");
    private static final Component TERMINAL_VISIBLE_TOOLTIP = Component.translatable("gui.mekenergistics.me_patterns.terminal_visibility.shown");
    private static final Component TERMINAL_HIDDEN_TOOLTIP = Component.translatable("gui.mekenergistics.me_patterns.terminal_visibility.hidden");
    private static final Component PASSIVE_INTERVAL_TOOLTIP = Component.translatable("gui.mekenergistics.me_patterns.passive.interval",
            MePassiveCraftingSettings.MIN_INTERVAL_TICKS, MePassiveCraftingSettings.MAX_INTERVAL_TICKS);
    private static final Component PASSIVE_MULTIPLIER_TOOLTIP = Component.translatable("gui.mekenergistics.me_patterns.passive.multiplier",
            MePassiveCraftingSettings.MAX_COPIES_PER_TICK);
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
    private static final ResourceLocation PUBLIC_BUTTON = MekanismUtils.getResource(ResourceType.GUI_BUTTON, "public.png");
    private static final ResourceLocation PRIVATE_BUTTON = MekanismUtils.getResource(ResourceType.GUI_BUTTON, "private.png");
    private static final ResourceLocation PATTERN_BUTTON_ICON = ResourceLocation.fromNamespaceAndPath(MekEnergistics.MODID, "textures/gui/button/pattern_button.png");
    private static final ResourceLocation EMPTY_PATTERN_ICON = ResourceLocation.fromNamespaceAndPath(MekEnergistics.MODID, "textures/gui/slot/pattern_empty.png");
    private static final int WINDOW_WIDTH = 178;
    private static final int WINDOW_HEIGHT = 118;
    private static final int NAME_FIELD_WIDTH = 24;
    private static final int NAME_FIELD_HEIGHT = 12;
    private static final int SLOT_COLUMNS = MekEnergisticsConfig.PATTERN_SLOT_COLUMNS;
    private static final int SLOT_ROWS = MekEnergisticsConfig.PATTERN_SLOT_ROWS;
    private static final int SLOTS_PER_PAGE = MekEnergisticsConfig.PATTERN_SLOTS_PER_PAGE;

    private MePatternWindowOverlay() {
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        // Runs every frame on every screen: only test whether this is a target here. Building the
        // full Target (records + a dozen lambdas) is deferred until the window is actually opened.
        GuiMekanism<?> targetGui = targetGui(event.getScreen());
        if (targetGui == null) {
            if (event.getScreen() instanceof GuiMekanism<?> gui) {
                closeOpenPatternWindows(gui);
            }
            return;
        }
        ButtonBounds bounds = buttonBounds(targetGui);
        GuiGraphics graphics = event.getGuiGraphics();
        boolean patternWindowOpen = hasPatternWindow(targetGui);
        MekanismRenderer.color(graphics, SpecialColors.TAB_UPGRADE);
        GuiUtils.blitNineSlicedSized(graphics, HOLDER_RIGHT, bounds.tabX(), bounds.tabY(), TAB_SIZE, TAB_SIZE, 4, 26, 9, 0, 0, 26, 9);
        MekanismRenderer.resetColor(graphics);
        int buttonTextureY = patternWindowOpen || bounds.contains(event.getMouseX(), event.getMouseY()) ? 40 : 20;
        GuiUtils.blitNineSlicedSized(graphics, BUTTON, bounds.buttonX(), bounds.buttonY(), INNER_SIZE, INNER_SIZE, 20, 4, 200, 20, 0, buttonTextureY, 200, 60);
        drawPatternIcon(graphics, targetGui, bounds);
        if (bounds.contains(event.getMouseX(), event.getMouseY())) {
            graphics.renderTooltip(targetGui.font(), PATTERN_BUTTON_TOOLTIP, event.getMouseX(), event.getMouseY());
        }
    }

    @SubscribeEvent
    public static void mouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        GuiMekanism<?> targetGui = targetGui(event.getScreen());
        if (targetGui == null || !buttonBounds(targetGui).contains(event.getMouseX(), event.getMouseY())) {
            return;
        }
        Target target = findTarget(event.getScreen());
        if (target != null) {
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
        return targetGui(screen) != null;
    }

    /**
     * Allocation-free version of the {@link #findTarget} checks: returns the screen when it shows
     * the pattern button. Called every frame (render hook and JEI exclusion areas).
     */
    @org.jetbrains.annotations.Nullable
    private static GuiMekanism<?> targetGui(Screen screen) {
        if (!(screen instanceof GuiMekanism<?> gui) || !(gui.getMenu() instanceof MekanismTileContainer<?> container)) {
            return null;
        }
        if (MeInterfaceWindowOverlay.isInterfaceMachine(screen)) {
            return null;
        }
        if (container.getTileEntity() instanceof MeFactoryAeMachine machine) {
            return gui; // factory targets are resolved before the generic ME upgrade gate
        }
        if (container.getTileEntity() instanceof MeAeMachine machine) {
            if (machine instanceof MeUpgradeableMachine upgradeable) {
                boolean nativeMeMachine = container.getTileEntity().getBlockState().getBlock()
                        instanceof MeMekanismMachineBlock;
                boolean upgradeTarget = upgradeable.isMeUpgradeTarget();
                boolean upgradeActive = upgradeTarget && upgradeable.isMeUpgradeActive();
                if (!PatternWindowTargetPolicy.shouldShow(nativeMeMachine, upgradeTarget, upgradeActive)) {
                    return null;
                }
            }
            return gui;
        }
        return null;
    }

    public static Rect2i jeiButtonArea(GuiMekanism<?> gui) {
        ButtonBounds bounds = buttonBounds(gui);
        return new Rect2i(bounds.tabX(), bounds.tabY(), TAB_SIZE, TAB_SIZE);
    }

    private static Target findTarget(Screen screen) {
        if (MeInterfaceWindowOverlay.isInterfaceMachine(screen)) {
            return null;
        }
        if (!(screen instanceof GuiMekanism<?> gui) || !(gui.getMenu() instanceof MekanismTileContainer<?> container)) {
            return null;
        }
        if (container.getTileEntity() instanceof MeFactoryAeMachine machine) {
            return new Target(gui, container, machine.getPatternSlots(), new NameAccess(machine::getCustomPatternTerminalName, machine::setCustomPatternTerminalName),
                    new SmartMultiplicationAccess(machine::isSmartPatternMultiplicationEnabled, machine::setSmartPatternMultiplicationEnabled),
                    new PassiveCraftingAccess(machine::hasPassiveCraftingUpgrade,
                            () -> machine.getPassiveCraftingSettings().intervalTicks(),
                            () -> machine.getPassiveCraftingSettings().multiplier(), machine::setPassiveCraftingSettings),
                    new OutputAccess(machine::getAeOutputMode, machine::setAeOutputMode),
                    new TerminalVisibilityAccess(machine::isVisibleInTerminal, machine::setVisibleInPatternAccessTerminal));
        }
        if (container.getTileEntity() instanceof MeAeMachine machine) {
            if (machine instanceof MeUpgradeableMachine upgradeable) {
                boolean nativeMeMachine = container.getTileEntity().getBlockState().getBlock()
                        instanceof MeMekanismMachineBlock;
                boolean upgradeTarget = upgradeable.isMeUpgradeTarget();
                boolean upgradeActive = upgradeTarget && upgradeable.isMeUpgradeActive();
                if (!PatternWindowTargetPolicy.shouldShow(nativeMeMachine, upgradeTarget, upgradeActive)) {
                    return null;
                }
            }
            return new Target(gui, container, machine.getPatternSlots(), new NameAccess(machine::getCustomPatternTerminalName, machine::setCustomPatternTerminalName),
                    new SmartMultiplicationAccess(machine::isSmartPatternMultiplicationEnabled, machine::setSmartPatternMultiplicationEnabled),
                    new PassiveCraftingAccess(machine::hasPassiveCraftingUpgrade,
                            () -> machine.getPassiveCraftingSettings().intervalTicks(),
                            () -> machine.getPassiveCraftingSettings().multiplier(), machine::setPassiveCraftingSettings),
                    new OutputAccess(machine::getAeOutputMode, machine::setAeOutputMode),
                    new TerminalVisibilityAccess(machine::isVisibleInTerminal, machine::setVisibleInPatternAccessTerminal));
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

    private static void closeOpenPatternWindows(GuiMekanism<?> gui) {
        if (!hasPatternWindow(gui)) {
            return; // the common case, every frame: avoid copying the window list
        }
        for (GuiWindow window : new ArrayList<>(gui.getWindows())) {
            if (window instanceof MePatternWindow patternWindow) {
                patternWindow.close();
            }
        }
    }

    private record Target(GuiMekanism<?> gui, MekanismTileContainer<?> container,
                          List<BasicInventorySlot> patternSlots, NameAccess nameAccess,
                          SmartMultiplicationAccess smartMultiplicationAccess, PassiveCraftingAccess passiveCraftingAccess, OutputAccess outputAccess,
                          TerminalVisibilityAccess terminalVisibilityAccess) {
    }

    private record TerminalVisibilityAccess(Supplier<Boolean> getter, Consumer<Boolean> setter) {
        private boolean get() {
            return Boolean.TRUE.equals(this.getter.get());
        }

        private void set(boolean visible) {
            this.setter.accept(visible);
        }
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

    private record PassiveCraftingAccess(Supplier<Boolean> enabled, Supplier<Integer> intervalSupplier,
                                        Supplier<Long> multiplierSupplier, PassiveCraftingSetter setter) {
        private boolean isEnabled() { return Boolean.TRUE.equals(enabled.get()); }
        private int interval() { return intervalSupplier.get(); }
        private long multiplier() { return multiplierSupplier.get(); }
        private void set(int intervalTicks, long value) { setter.set(intervalTicks, value); }
    }

    @FunctionalInterface
    private interface PassiveCraftingSetter {
        void set(int intervalTicks, long multiplier);
    }

    private record OutputAccess(Supplier<AeOutputMode> getter, Consumer<AeOutputMode> setter) {
        private AeOutputMode get() {
            return this.getter.get();
        }

        private void set(AeOutputMode mode) {
            this.setter.accept(mode);
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
        private final GuiTextField passiveIntervalField;
        private final GuiTextField passiveMultiplierField;
        private final MekanismImageButton smartMultiplicationButton;
        private final MekanismImageButton terminalVisibilityButton;
        private int currentPage;
        private boolean nameEditorOpen;
        private String lastSavedName;
        private boolean smartMultiplicationEnabled;
        private boolean visibleInTerminal;
        private boolean passiveCraftingEnabled;
        /** Last server values copied into the passive fields; MIN_VALUE forces the first sync. */
        private int syncedPassiveInterval = Integer.MIN_VALUE;
        private long syncedPassiveMultiplier = Long.MIN_VALUE;
        private boolean passiveIntervalFocused;
        private boolean passiveMultiplierFocused;

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
            this.passiveCraftingEnabled = this.target.passiveCraftingAccess().isEnabled();
            this.visibleInTerminal = this.target.terminalVisibilityAccess().get();
            this.terminalVisibilityButton = addChild(new MekanismImageButton(gui, relativeX + 32, relativeY + 4, 12, PUBLIC_BUTTON,
                    (element, mouseX, mouseY) -> toggleTerminalVisibility()) {
                @Override
                protected ResourceLocation getResource() {
                    return MePatternWindow.this.visibleInTerminal ? PUBLIC_BUTTON : PRIVATE_BUTTON;
                }
            });
            updateTerminalVisibilityTooltip();
            addChild(new MekanismImageButton(gui, relativeX + 158, relativeY + 4, 12, CHECK_BUTTON, (element, mouseX, mouseY) -> toggleNameEditor()))
                    .setTooltip(Tooltip.create(RENAME_BUTTON_TOOLTIP));
            this.lastSavedName = this.target.nameAccess().get();
            this.nameField = addChild(new GuiTextField(gui, this, relativeX + 132, relativeY + 4, NAME_FIELD_WIDTH, NAME_FIELD_HEIGHT));
            this.nameField.setMaxLength(MeAeMachine.MAX_PATTERN_TERMINAL_NAME_LENGTH);
            this.nameField.setEnterHandler(this::saveName);
            this.nameField.setText(this.lastSavedName);
            setNameFieldVisible(false);
            this.passiveIntervalField = addChild(new GuiTextField(gui, this, relativeX + 22, relativeY + 96, 21, NAME_FIELD_HEIGHT));
            this.passiveMultiplierField = addChild(new GuiTextField(gui, this, relativeX + 45, relativeY + 96, 21, NAME_FIELD_HEIGHT));
            this.passiveIntervalField.setTooltip(Tooltip.create(PASSIVE_INTERVAL_TOOLTIP));
            this.passiveMultiplierField.setTooltip(Tooltip.create(PASSIVE_MULTIPLIER_TOOLTIP));
            this.passiveIntervalField.setMaxLength(Integer.toString(MePassiveCraftingSettings.MAX_INTERVAL_TICKS).length());
            this.passiveMultiplierField.setMaxLength(Long.toString(MePassiveCraftingSettings.MAX_COPIES_PER_TICK).length());
            this.passiveIntervalField.setEnterHandler(this::savePassiveCraftingSettings);
            this.passiveMultiplierField.setEnterHandler(this::savePassiveCraftingSettings);
            syncPassiveCraftingFields();
            addChild(new MekanismImageButton(gui, relativeX + 8, relativeY + 96, 12, LEFT_BUTTON, (element, mouseX, mouseY) -> previousPage()));
            addChild(new AeOutputButton(gui, relativeX + 116, relativeY + 96,
                    Component.translatable("gui.mekenergistics.me_patterns.ae_output.item.short"), TransmissionType.ITEM, target));
            addChild(new AeOutputButton(gui, relativeX + 130, relativeY + 96,
                    Component.translatable("gui.mekenergistics.me_patterns.ae_output.chemical.short"), TransmissionType.CHEMICAL, target));
            addChild(new AeOutputButton(gui, relativeX + 144, relativeY + 96,
                    Component.translatable("gui.mekenergistics.me_patterns.ae_output.fluid.short"), TransmissionType.FLUID, target));
            addChild(new MekanismImageButton(gui, relativeX + width - 20, relativeY + 96, 12, RIGHT_BUTTON, (element, mouseX, mouseY) -> nextPage()));
        }

        private boolean savePassiveCraftingSettings() {
            if (!this.passiveCraftingEnabled) return true;
            int interval = parseInt(this.passiveIntervalField.getText(), this.target.passiveCraftingAccess().interval());
            long multiplier = parseMultiplier(this.passiveMultiplierField.getText(), this.target.passiveCraftingAccess().multiplier());
            // Mirror the server's limits so the field shows the value that will actually be used
            // instead of silently accepting e.g. 1000 and running at the per-tick cap.
            interval = Math.clamp(interval, MePassiveCraftingSettings.MIN_INTERVAL_TICKS, MePassiveCraftingSettings.MAX_INTERVAL_TICKS);
            multiplier = Math.clamp(multiplier, 1, MePassiveCraftingSettings.MAX_COPIES_PER_TICK);
            this.passiveIntervalField.setText(Integer.toString(interval));
            this.passiveMultiplierField.setText(multiplier == 1 ? "" : Long.toString(multiplier));
            this.syncedPassiveInterval = interval;
            this.syncedPassiveMultiplier = multiplier;
            this.target.passiveCraftingAccess().set(interval, multiplier);
            PacketDistributor.sendToServer(new SetPassiveCraftingSettingsPacket(
                    this.target.container().getTileEntity().getBlockPos(), interval, multiplier));
            return true;
        }

        /**
         * Shows/hides the passive crafting fields and pulls server values into them. Server values
         * are only copied when they actually change and the field is not being edited, so a value
         * the player is typing is never overwritten by the next frame.
         */
        private void syncPassiveCraftingFields() {
            this.passiveIntervalField.visible = this.passiveCraftingEnabled;
            this.passiveMultiplierField.visible = this.passiveCraftingEnabled;
            this.passiveIntervalField.setVisible(this.passiveCraftingEnabled);
            this.passiveMultiplierField.setVisible(this.passiveCraftingEnabled);
            if (this.passiveCraftingEnabled) {
                int interval = this.target.passiveCraftingAccess().interval();
                long multiplier = this.target.passiveCraftingAccess().multiplier();
                if (!this.passiveIntervalField.isFocused() && interval != this.syncedPassiveInterval) {
                    this.passiveIntervalField.setText(Integer.toString(interval));
                    this.syncedPassiveInterval = interval;
                }
                if (!this.passiveMultiplierField.isFocused() && multiplier != this.syncedPassiveMultiplier) {
                    this.passiveMultiplierField.setText(multiplier == 1 ? "" : Long.toString(multiplier));
                    this.syncedPassiveMultiplier = multiplier;
                }
            }
            this.smartMultiplicationButton.active = !this.passiveCraftingEnabled;
        }

        /** Commits passive crafting edits when a field loses focus, not only on Enter. */
        private void commitPassiveFieldsOnFocusLoss() {
            boolean intervalFocused = this.passiveIntervalField.isFocused();
            boolean multiplierFocused = this.passiveMultiplierField.isFocused();
            boolean lostFocus = this.passiveIntervalFocused && !intervalFocused
                    || this.passiveMultiplierFocused && !multiplierFocused;
            this.passiveIntervalFocused = intervalFocused;
            this.passiveMultiplierFocused = multiplierFocused;
            if (lostFocus && passiveFieldsDirty()) {
                savePassiveCraftingSettings();
            }
        }

        private boolean passiveFieldsDirty() {
            if (!this.passiveCraftingEnabled) {
                return false;
            }
            int interval = this.target.passiveCraftingAccess().interval();
            long multiplier = this.target.passiveCraftingAccess().multiplier();
            return parseInt(this.passiveIntervalField.getText(), interval) != interval
                    || parseMultiplier(this.passiveMultiplierField.getText(), multiplier) != multiplier;
        }

        private static int parseInt(String value, int fallback) {
            try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return fallback; }
        }

        /** The multiplier field shows 1 as an empty field, so an empty field means 1, not "unchanged". */
        private static long parseMultiplier(String value, long fallback) {
            return value == null || value.isBlank() ? 1 : parseLong(value.trim(), fallback);
        }

        private static long parseLong(String value, long fallback) {
            try { return Long.parseLong(value); } catch (NumberFormatException ignored) { return fallback; }
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

        private boolean toggleTerminalVisibility() {
            this.visibleInTerminal = !this.visibleInTerminal;
            this.target.terminalVisibilityAccess().set(this.visibleInTerminal);
            PacketDistributor.sendToServer(new SetTerminalVisibilityPacket(this.target.container().getTileEntity().getBlockPos(), this.visibleInTerminal));
            updateTerminalVisibilityTooltip();
            return true;
        }

        private void updateTerminalVisibilityTooltip() {
            this.terminalVisibilityButton.setTooltip(Tooltip.create(this.visibleInTerminal ? TERMINAL_VISIBLE_TOOLTIP : TERMINAL_HIDDEN_TOOLTIP));
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
            if (passiveFieldsDirty()) {
                savePassiveCraftingSettings();
            }
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
            boolean syncedPassiveCrafting = this.target.passiveCraftingAccess().isEnabled();
            if (syncedPassiveCrafting != this.passiveCraftingEnabled) {
                this.passiveCraftingEnabled = syncedPassiveCrafting;
                syncPassiveCraftingFields();
            } else if (this.passiveCraftingEnabled) {
                commitPassiveFieldsOnFocusLoss();
                syncPassiveCraftingFields();
            }
            boolean syncedTerminalVisibility = this.target.terminalVisibilityAccess().get();
            if (syncedTerminalVisibility != this.visibleInTerminal) {
                this.visibleInTerminal = syncedTerminalVisibility;
                updateTerminalVisibilityTooltip();
            }
            int titleWidth = font().width(PATTERN_WINDOW_TITLE);
            int titleX = (width - titleWidth) / 2;
            drawScrollingString(guiGraphics, PATTERN_WINDOW_TITLE, titleX, 6, TextAlignment.LEFT,
                    titleTextColor(), titleWidth, 0, false);
            int pageX = titleX + titleWidth + 4;
            drawScrollingString(guiGraphics, Component.literal((this.currentPage + 1) + "/" + pageCount()),
                    pageX, 6, TextAlignment.LEFT, titleTextColor(), width - pageX - 4, 0, false);
        }
    }

    private static final class AeOutputButton extends MekanismButton {
        private final TransmissionType type;
        private final Target target;

        private AeOutputButton(IGuiWrapper gui, int x, int y, Component label,
                TransmissionType type, Target target) {
            super(gui, x, y, 12, 12, label,
                    (element, mouseX, mouseY) -> toggleOutput(target, type));
            this.type = type;
            this.target = target;
        }

        private static boolean toggleOutput(Target target, TransmissionType type) {
            AeOutputMode mode = target.outputAccess().get().toggle(type);
            target.outputAccess().set(mode);
            PacketDistributor.sendToServer(new SetAeOutputModePacket(
                    target.container().getTileEntity().getBlockPos(), mode));
            return true;
        }

        @Override
        public void updateTooltip(int mouseX, int mouseY) {
            AeOutputMode mode = this.target.outputAccess().get();
            Component state = Component.translatable(outputEnabled(mode, this.type)
                    ? "gui.mekenergistics.me_patterns.ae_output.on"
                    : "gui.mekenergistics.me_patterns.ae_output.off");
            setTooltip(Tooltip.create(Component.translatable(outputTooltipKey(this.type), state)));
            super.updateTooltip(mouseX, mouseY);
        }

        @Override
        protected int getButtonTextColor(int mouseX, int mouseY) {
            return outputEnabled(this.target.outputAccess().get(), this.type) ? 0x159515 : 0x555555;
        }

        @Override
        protected boolean displayButtonTextShadow() {
            return false;
        }
    }

    private static boolean outputEnabled(AeOutputMode mode, TransmissionType type) {
        return switch (type) {
            case ITEM -> mode.items();
            case CHEMICAL -> mode.chemicals();
            case FLUID -> mode.fluids();
            default -> false;
        };
    }

    private static String outputTooltipKey(TransmissionType type) {
        return switch (type) {
            case ITEM -> "gui.mekenergistics.me_patterns.ae_output.item";
            case CHEMICAL -> "gui.mekenergistics.me_patterns.ae_output.chemical";
            case FLUID -> "gui.mekenergistics.me_patterns.ae_output.fluid";
            default -> "tooltip.mekenergistics.ae_output.button";
        };
    }

    private static final class PatternGuiVirtualSlot extends GuiVirtualSlot {
        private IVirtualSlot virtualSlot;
        /*
         * Decoding a pattern (for crafting patterns: a recipe lookup and match) used to run for
         * every visible slot on every frame. The decoded output is memoized per slot and only
         * recomputed when the pattern stack or the client level changes.
         */
        private ItemStack decodedPattern = ItemStack.EMPTY;
        @Nullable
        private net.minecraft.world.level.Level decodedLevel;
        @Nullable
        private GenericStack decodedOutput;

        private PatternGuiVirtualSlot(IGUIWindow window, SlotType type, IGuiWrapper gui, int x, int y, VirtualInventoryContainerSlot containerSlot) {
            super(window, type, gui, x, y, containerSlot);
        }

        @Override
        public void updateVirtualSlot(@Nullable IGUIWindow window, @Nullable IVirtualSlot virtualSlot) {
            this.virtualSlot = virtualSlot;
            if (virtualSlot != null) {
                super.updateVirtualSlot(window, virtualSlot);
            }
        }

        @Override
        public boolean isElementForSlot(IVirtualSlot virtualSlot) {
            return this.virtualSlot == virtualSlot;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.virtualSlot != null && super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public Optional<?> getIngredient(double mouseX, double mouseY) {
            return this.virtualSlot == null ? Optional.empty() : Optional.of(this.virtualSlot.getStackToRender());
        }

        @Override
        protected void drawContents(GuiGraphics guiGraphics) {
            if (virtualSlot == null || virtualSlot.getStackToRender().isEmpty()) {
                guiGraphics.blit(EMPTY_PATTERN_ICON, relativeX + 1, relativeY + 1, 0, 0, 16, 16, 16, 16);
                return;
            }
            ItemStack patternStack = virtualSlot.getStackToRender();
            GenericStack output = cachedPatternOutput(patternStack);
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

        @Nullable
        private GenericStack cachedPatternOutput(ItemStack patternStack) {
            net.minecraft.world.level.Level level = Minecraft.getInstance().level;
            if (level == null) {
                return null;
            }
            boolean samePattern = patternStack == this.decodedPattern
                    || ItemStack.isSameItemSameComponents(patternStack, this.decodedPattern);
            if (level != this.decodedLevel || !samePattern) {
                this.decodedOutput = getPatternOutput(patternStack);
                this.decodedPattern = patternStack.copy();
                this.decodedLevel = level;
            }
            return this.decodedOutput;
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
