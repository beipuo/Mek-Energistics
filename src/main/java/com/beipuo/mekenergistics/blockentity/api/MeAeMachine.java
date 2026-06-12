package com.beipuo.mekenergistics.blockentity.api;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternContainer;
import com.beipuo.mekenergistics.blockentity.support.MeRecipeMachineAeSupport;
import com.beipuo.mekenergistics.blockentity.support.MeOwnerHelper;
import com.beipuo.mekenergistics.blockentity.slot.PatternSlotInternalInventory;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.registry.ModBlocks;
import java.util.List;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public interface MeAeMachine extends PatternContainer, ICraftingProvider, IActionHost, appeng.me.helpers.IGridConnectedBlockEntity {
    int MAX_PATTERN_TERMINAL_NAME_LENGTH = 64;

    AeOutputMode getAeOutputMode();

    void cycleAeOutputMode();

    default void cycleAeOutputMode(TransmissionType type) {
        AeOutputMode target = getAeOutputMode().toggle(type);
        for (int i = 0; i < AeOutputMode.values().length && getAeOutputMode() != target; i++) {
            cycleAeOutputMode();
        }
    }

    @Override
    default IManagedGridNode getMainNode() {
        MeRecipeMachineAeSupport<?> support = getRecipeAeSupport();
        return support == null ? null : support.getMainNode();
    }

    @Override
    default void saveChanges() {
        if (this instanceof BlockEntity blockEntity) {
            blockEntity.setChanged();
        }
    }

    default void setOwner(ServerPlayer player) {
        IManagedGridNode node = getMainNode();
        if (node == null) {
            return;
        }
        if (this instanceof TileEntityMekanism tile) {
            MeOwnerHelper.setOwner(tile, node, player);
        } else {
            node.setOwningPlayer(player);
        }
    }

    default List<BasicInventorySlot> getPatternSlots() {
        MeRecipeMachineAeSupport<?> support = getRecipeAeSupport();
        return support == null ? List.of() : support.getPatternSlots();
    }

    MeMekanismMachine getMachine();

    default ItemStack getTerminalIconStack() {
        return new ItemStack(ModBlocks.getMachineBlock(getMachine()).get());
    }

    default String getCustomPatternTerminalName() {
        MeRecipeMachineAeSupport<?> support = getRecipeAeSupport();
        return support == null ? "" : support.getPatternTerminalName();
    }

    default void setCustomPatternTerminalName(String name) {
        MeRecipeMachineAeSupport<?> support = getRecipeAeSupport();
        if (support != null) {
            support.setPatternTerminalName(name);
        }
    }

    default boolean isSmartPatternMultiplicationEnabled() {
        MeRecipeMachineAeSupport<?> support = getRecipeAeSupport();
        if (support != null) {
            return support.isSmartPatternMultiplicationEnabled();
        }
        return true;
    }

    default void setSmartPatternMultiplicationEnabled(boolean enabled) {
        MeRecipeMachineAeSupport<?> support = getRecipeAeSupport();
        if (support != null) {
            support.setSmartPatternMultiplicationEnabled(enabled);
        }
    }

    default MeRecipeMachineAeSupport<?> getRecipeAeSupport() {
        return null;
    }

    default List<IPatternDetails> getAvailablePatterns() {
        MeRecipeMachineAeSupport<?> support = getRecipeAeSupport();
        return support == null ? List.of() : support.getAvailablePatterns();
    }

    default int getPatternPriority() {
        MeRecipeMachineAeSupport<?> support = getRecipeAeSupport();
        return support == null ? 0 : support.getPatternPriority();
    }

    @Override
    default boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        return false;
    }

    @Override
    default boolean isBusy() {
        return false;
    }

    default Component getPatternTerminalDisplayName() {
        String customName = getCustomPatternTerminalName();
        return customName.isBlank() ? Component.translatable(getMachine().translationKey()) : Component.literal(customName);
    }

    @Override
    default IGrid getGrid() {
        MeRecipeMachineAeSupport<?> support = getRecipeAeSupport();
        return support == null ? null : support.getGrid();
    }

    @Nullable
    @Override
    default IGridNode getActionableNode() {
        IManagedGridNode node = getMainNode();
        return node == null ? null : node.getNode();
    }

    @Nullable
    @Override
    default IGridNode getGridNode(Direction dir) {
        IManagedGridNode node = getMainNode();
        return node == null ? null : node.getNode();
    }

    @Override
    default InternalInventory getTerminalPatternInventory() {
        return new PatternSlotInternalInventory(this);
    }

    @Override
    default long getTerminalSortOrder() {
        return 0;
    }

    @Override
    default PatternContainerGroup getTerminalGroup() {
        ItemStack iconStack = getTerminalIconStack();
        AEItemKey icon = iconStack.isEmpty() ? null : AEItemKey.of(iconStack);
        return new PatternContainerGroup(icon, getPatternTerminalDisplayName(), List.of());
    }

    static String sanitizePatternTerminalName(String name) {
        if (name == null) {
            return "";
        }
        String sanitized = name.trim();
        return sanitized.length() > MAX_PATTERN_TERMINAL_NAME_LENGTH ? sanitized.substring(0, MAX_PATTERN_TERMINAL_NAME_LENGTH) : sanitized;
    }

}
