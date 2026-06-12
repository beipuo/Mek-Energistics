package com.beipuo.mekenergistics.blockentity.api;

import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEItemKey;
import appeng.helpers.patternprovider.PatternContainer;
import com.beipuo.mekenergistics.blockentity.MeMekanismMachineBlockEntity;
import com.beipuo.mekenergistics.blockentity.support.MeRecipeMachineAeSupport;
import com.beipuo.mekenergistics.blockentity.slot.PatternSlotInternalInventory;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import java.util.List;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public interface MeAeMachine extends PatternContainer {
    int MAX_PATTERN_TERMINAL_NAME_LENGTH = 64;

    AeOutputMode getAeOutputMode();

    void cycleAeOutputMode();

    default void cycleAeOutputMode(TransmissionType type) {
        AeOutputMode target = getAeOutputMode().toggle(type);
        for (int i = 0; i < AeOutputMode.values().length && getAeOutputMode() != target; i++) {
            cycleAeOutputMode();
        }
    }

    void setOwner(ServerPlayer player);

    List<BasicInventorySlot> getPatternSlots();

    MeMekanismMachine getMachine();

    ItemStack getTerminalIconStack();

    default String getCustomPatternTerminalName() {
        return "";
    }

    default void setCustomPatternTerminalName(String name) {
    }

    default boolean isSmartPatternMultiplicationEnabled() {
        MeRecipeMachineAeSupport<?> support = getRecipeMachineAeSupport();
        if (support != null) {
            return support.isSmartPatternMultiplicationEnabled();
        }
        return true;
    }

    default void setSmartPatternMultiplicationEnabled(boolean enabled) {
        MeRecipeMachineAeSupport<?> support = getRecipeMachineAeSupport();
        if (support != null) {
            support.setSmartPatternMultiplicationEnabled(enabled);
        }
    }

    default Component getPatternTerminalDisplayName() {
        String customName = getCustomPatternTerminalName();
        return customName.isBlank() ? Component.translatable(getMachine().translationKey()) : Component.literal(customName);
    }

    @Override
    IGrid getGrid();

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

    private MeRecipeMachineAeSupport<?> getRecipeMachineAeSupport() {
        Class<?> type = getClass();
        while (type != null && type != Object.class) {
            try {
                java.lang.reflect.Field field = type.getDeclaredField("aeSupport");
                if (MeRecipeMachineAeSupport.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    return (MeRecipeMachineAeSupport<?>) field.get(this);
                }
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
                continue;
            } catch (IllegalAccessException ignored) {
                return null;
            }
            break;
        }
        return null;
    }
}
