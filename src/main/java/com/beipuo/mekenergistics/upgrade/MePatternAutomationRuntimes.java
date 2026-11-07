package com.beipuo.mekenergistics.upgrade;

import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import com.beipuo.mekenergistics.blockentity.support.MePatternSlotInventoryHolder;
import java.util.Map;
import java.util.WeakHashMap;
import mekanism.api.IContentsListener;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.tile.base.TileEntityMekanism;
import org.jetbrains.annotations.Nullable;

/**
 * Per-tile ME-upgrade runtime for SPI hosts. The runtime is stored on the tile through
 * {@link MePatternAutomationRuntimeHolder} so it is collected together with the tile.
 *
 * <p>The previous {@code WeakHashMap<TileEntityMekanism, Runtime>} never released anything: each
 * runtime holds a strong reference to its tile (and through it the whole Level), so the weak key
 * could never be cleared. The map below is only a fallback for tiles that somehow lack the holder
 * mixin, and is cleared when such a tile is removed.
 */
public final class MePatternAutomationRuntimes {
    private static final Map<TileEntityMekanism, MeUpgradeRecipeMachineRuntime> FALLBACK_RUNTIMES =
            new WeakHashMap<>();

    private MePatternAutomationRuntimes() {
    }

    public static MeUpgradeRecipeMachineRuntime getOrCreate(TileEntityMekanism tile) {
        if (tile instanceof MePatternAutomationRuntimeHolder holder) {
            MeUpgradeRecipeMachineRuntime runtime = holder.mekenergistics$getPatternAutomationRuntime();
            if (runtime == null) {
                runtime = new MeUpgradeRecipeMachineRuntime(tile, AeOutputMode.BOTH);
                holder.mekenergistics$setPatternAutomationRuntime(runtime);
            }
            return runtime;
        }
        synchronized (FALLBACK_RUNTIMES) {
            return FALLBACK_RUNTIMES.computeIfAbsent(tile,
                    key -> new MeUpgradeRecipeMachineRuntime(key, AeOutputMode.BOTH));
        }
    }

    public static @Nullable MeUpgradeRecipeMachineRuntime getExisting(TileEntityMekanism tile) {
        if (tile instanceof MePatternAutomationRuntimeHolder holder) {
            return holder.mekenergistics$getPatternAutomationRuntime();
        }
        synchronized (FALLBACK_RUNTIMES) {
            return FALLBACK_RUNTIMES.get(tile);
        }
    }

    /** Drops a fallback entry once its tile is removed; holder-backed runtimes need no cleanup. */
    public static void forget(TileEntityMekanism tile) {
        if (tile instanceof MePatternAutomationRuntimeHolder) {
            return;
        }
        synchronized (FALLBACK_RUNTIMES) {
            FALLBACK_RUNTIMES.remove(tile);
        }
    }

    /**
     * Wraps the holder returned by a virtual {@code getInitialInventory} call. Must run at the
     * constructor call site so subclass overrides still receive pattern slots.
     */
    public static IInventorySlotHolder wrapInventory(Object tile, IInventorySlotHolder original,
            IContentsListener listener) {
        // The instanceof test is free; the target check may allocate a host and call into an addon
        // (reflectively for Mekanism Magic), and this runs for every TileEntityMekanism.
        if (original == null || original instanceof MePatternSlotInventoryHolder
                || !(tile instanceof MeUpgradeRecipeMachineAdapter adapter)
                || !MePatternAutomationProfiles.isUpgradeTarget(tile)) {
            return original;
        }
        return adapter.addMePatternSlots(original, listener);
    }

    /** Runs every server tick for every TileEntityMekanism, so the cheap type test comes first. */
    public static boolean processTick(Object tile, boolean changed) {
        if (!(tile instanceof MeUpgradeRecipeMachineAdapter adapter)
                || !MePatternAutomationProfiles.isUpgradeTarget(tile)) {
            return changed;
        }
        return adapter.processMePatternIo(changed);
    }
}
