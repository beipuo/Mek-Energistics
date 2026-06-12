package com.beipuo.mekenergistics.menu;

import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class MePatternContainerQuickMove {
    private MePatternContainerQuickMove() {
    }

    @NotNull
    public static ItemStack quickMoveStack(@NotNull List<Slot> slots, @NotNull MePatternQuickMoveContainer patternContainer, @NotNull Object tile,
            @NotNull TransferSuccess transferSuccess, @NotNull FallbackQuickMove fallback, @NotNull Player player, int slotID) {
        Slot currentSlot = slots.get(slotID);
        if (currentSlot == null || !currentSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack slotStack = currentSlot.getItem();
        ItemStack remaining = patternContainer.tryQuickMovePattern(currentSlot, tile, slotStack);
        if (remaining.getCount() != slotStack.getCount()) {
            return transferSuccess.transfer(currentSlot, player, slotStack, remaining);
        }
        return fallback.quickMove(player, slotID);
    }

    @FunctionalInterface
    public interface TransferSuccess {
        ItemStack transfer(@NotNull Slot currentSlot, @NotNull Player player, @NotNull ItemStack slotStack, @NotNull ItemStack stackToInsert);
    }

    @FunctionalInterface
    public interface FallbackQuickMove {
        ItemStack quickMove(@NotNull Player player, int slotID);
    }
}
