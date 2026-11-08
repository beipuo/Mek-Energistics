package com.fish_dan_.data_energistics.api.crafting.dispatch;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;

public interface CountedCraftingProviderAdapter {
    CountedCraftingAdmission prepareBatch(
            IPatternDetails pattern, KeyCounter[] oneCraftInputs, long requestedMaxCrafts);

    default java.util.List<CountedCraftingCapacity> captureCapacity(
            IPatternDetails pattern, KeyCounter[] prototype, long requestedCount) {
        return java.util.List.of();
    }

    default it.unimi.dsi.fastutil.objects.ObjectList<CountedCraftingCapacity> captureCapacityFast(
            IPatternDetails pattern, KeyCounter[] prototype, long requestedCount) {
        return it.unimi.dsi.fastutil.objects.ObjectLists.unmodifiable(
                new it.unimi.dsi.fastutil.objects.ObjectArrayList<>(
                        captureCapacity(pattern, prototype, requestedCount)));
    }

    default CountedCraftingAdmission prepareBatchForTarget(IPatternDetails pattern,
            KeyCounter[] prototype, long requestedCount, CountedCraftingTarget target) {
        return target.providerScoped() ? prepareBatch(pattern, prototype, requestedCount) : null;
    }
}
