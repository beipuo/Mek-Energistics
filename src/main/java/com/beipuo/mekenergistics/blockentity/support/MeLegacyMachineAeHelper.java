package com.beipuo.mekenergistics.blockentity.support;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.KeyCounter;
import java.util.List;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class MeLegacyMachineAeHelper {
    private MeLegacyMachineAeHelper() {
    }

    public static void updatePatterns(List<IPatternDetails> patterns, IntFunction<ItemStack> stackGetter, int firstSlot,
            int lastSlot, @Nullable Level level, BlockPos pos, String ownerName, IManagedGridNode mainNode) {
        patterns.clear();
        for (int i = firstSlot; i <= lastSlot; i++) {
            ItemStack stack = stackGetter.apply(i);
            if (!stack.isEmpty()) {
                IPatternDetails pattern = MePatternDecodeHelper.safeDecode(stack, level, pos, ownerName);
                if (pattern != null) {
                    patterns.add(pattern);
                }
            }
        }
        if (mainNode.getNode() != null) {
            ICraftingProvider.requestUpdate(mainNode);
        }
    }

    public static boolean enqueueSmartPattern(MeSmartPatternMultiplication smartPatternMultiplication,
            IPatternDetails patternDetails, KeyCounter[] inputHolder, Runnable changedCallback, Runnable alertTicker) {
        boolean enqueued = smartPatternMultiplication.enqueue(patternDetails, inputHolder);
        if (enqueued) {
            alertTicker.run();
            changedCallback.run();
        }
        return enqueued;
    }

    public static boolean processSmartPatternWork(MeSmartPatternMultiplication smartPatternMultiplication,
            List<IPatternDetails> patterns, MeSmartPatternMultiplication.PatternFeeder feeder, Runnable changedCallback,
            Runnable alertTicker) {
        boolean wasEnabled = smartPatternMultiplication.isEnabled();
        smartPatternMultiplication.setEnabled(false);
        try {
            boolean changed = smartPatternMultiplication.processNext(patterns, feeder);
            if (changed) {
                changedCallback.run();
                if (smartPatternMultiplication.hasPendingWork()) {
                    alertTicker.run();
                }
            }
            return changed;
        } finally {
            smartPatternMultiplication.setEnabled(wasEnabled);
        }
    }

    public static void saveAeState(CompoundTag tag, HolderLookup.Provider registries,
            MeSmartPatternMultiplication smartPatternMultiplication, IManagedGridNode mainNode) {
        smartPatternMultiplication.saveConfig(tag);
        smartPatternMultiplication.savePending(tag, registries);
        mainNode.saveToNBT(tag);
    }

    public static void loadAeState(CompoundTag tag, HolderLookup.Provider registries,
            MeSmartPatternMultiplication smartPatternMultiplication, IManagedGridNode mainNode) {
        smartPatternMultiplication.loadConfig(tag);
        smartPatternMultiplication.loadPending(tag, registries);
        mainNode.loadFromNBT(tag);
    }
}
