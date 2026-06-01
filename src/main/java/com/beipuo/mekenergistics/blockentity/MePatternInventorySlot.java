package com.beipuo.mekenergistics.blockentity;

import java.util.function.Predicate;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.inventory.container.slot.VirtualInventoryContainerSlot;
import mekanism.common.inventory.slot.BasicInventorySlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MePatternInventorySlot extends BasicInventorySlot {
    public static MePatternInventorySlot create(Predicate<@NotNull ItemStack> validator, @Nullable IContentsListener listener) {
        return new MePatternInventorySlot(validator, listener);
    }

    private MePatternInventorySlot(Predicate<@NotNull ItemStack> validator, @Nullable IContentsListener listener) {
        super(1, (stack, automationType) -> automationType == AutomationType.MANUAL,
                (stack, automationType) -> true, validator, listener, 0, 0);
    }

    @NotNull
    @Override
    public VirtualInventoryContainerSlot createContainerSlot() {
        return new VirtualInventoryContainerSlot(this, SelectedWindowData.UNSPECIFIED, getSlotOverlay(), this::setStackUnchecked);
    }
}
