package com.beipuo.mekenergistics.mixin.neoecoae;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;
import cn.dancingsnow.neoecoae.api.me.provider.ECOParallelCraftingProvider;
import com.beipuo.mekenergistics.blockentity.api.MeAeSupportOwner;
import com.beipuo.mekenergistics.compat.neoecoae.NeoEcoBatchCompat;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MeAeSupportOwner.class)
public interface NeoEcoParallelCraftingProviderMixin extends ECOParallelCraftingProvider {
    @Override
    default int eco$getAvailableParallelSlots() {
        return Math.max(0, ((MeAeSupportOwner) this).getAvailableParallelSlots());
    }

    @Override
    default boolean eco$pushPatternBatch(IPatternDetails patternDetails, KeyCounter[] inputTotal,
            long craftCount, UUID jobId) {
        return NeoEcoBatchCompat.pushPatternBatch((MeAeSupportOwner) this,
                patternDetails, inputTotal, craftCount);
    }
}
