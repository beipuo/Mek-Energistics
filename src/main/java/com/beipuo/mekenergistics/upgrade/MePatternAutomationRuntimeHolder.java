package com.beipuo.mekenergistics.upgrade;

import org.jetbrains.annotations.Nullable;

/**
 * Storage slot mixed into every {@code TileEntityMekanism} so a pattern-automation SPI host can
 * keep its ME runtime on the tile itself. This is plain storage: it does not make the tile an ME
 * machine. Keeping the runtime on the tile ties its lifetime to the tile; a side map keyed by the
 * tile cannot do that because the runtime strongly references its own tile.
 */
public interface MePatternAutomationRuntimeHolder {
    @Nullable
    MeUpgradeRecipeMachineRuntime mekenergistics$getPatternAutomationRuntime();

    void mekenergistics$setPatternAutomationRuntime(MeUpgradeRecipeMachineRuntime runtime);
}
