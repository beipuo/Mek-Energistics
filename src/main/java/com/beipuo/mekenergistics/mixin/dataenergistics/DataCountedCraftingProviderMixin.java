package com.beipuo.mekenergistics.mixin.dataenergistics;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.support.AbstractMeAeSupport;
import com.beipuo.mekenergistics.blockentity.support.MeFactoryAeSupport;
import com.beipuo.mekenergistics.compat.dataenergistics.DataCraftingAdmission;
import com.fish_dan_.data_energistics.api.crafting.dispatch.CountedCraftingAdmission;
import com.fish_dan_.data_energistics.api.crafting.dispatch.CountedCraftingCapacity;
import com.fish_dan_.data_energistics.common.crafting.trinity.dispatch.commit.CountedCraftingPreparation;
import com.fish_dan_.data_energistics.common.crafting.trinity.dispatch.model.CraftingDispatchTargetAvailability;
import com.fish_dan_.data_energistics.common.crafting.trinity.dispatch.provider.CountedCraftingProvider;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MeAeMachine.class)
public interface DataCountedCraftingProviderMixin extends CountedCraftingProvider {

    @Shadow
    AbstractMeAeSupport<?> getRecipeAeSupport();

    @Override
    default ObjectList<CountedCraftingCapacity> captureCapacityFast(
            IPatternDetails patternDetails, KeyCounter[] prototype, long requestedCount) {
        return DataCraftingAdmission.captureCapacity(
                getRecipeAeSupport(), patternDetails, prototype, requestedCount);
    }

    @Override
    @Nullable
    default CountedCraftingAdmission prepareBatch(IPatternDetails patternDetails,
            KeyCounter[] prototype, long requestedCount) {
        return DataCraftingAdmission.prepare(
                getRecipeAeSupport(), patternDetails, prototype, requestedCount);
    }

    @Override
    default CountedCraftingPreparation prepareBatch(IPatternDetails patternDetails,
            KeyCounter[] prototype, long requestedCount, CraftingDispatchTargetAvailability availability) {
        return DataCraftingAdmission.prepareTargetAware(getRecipeAeSupport(), patternDetails,
                prototype, requestedCount, availability);
    }
}

@Mixin(MeFactoryAeMachine.class)
interface DataCountedFactoryCraftingProviderMixin extends CountedCraftingProvider {

    @Shadow
    MeFactoryAeSupport getAeSupport();

    @Override
    default ObjectList<CountedCraftingCapacity> captureCapacityFast(
            IPatternDetails patternDetails, KeyCounter[] prototype, long requestedCount) {
        return DataCraftingAdmission.captureCapacity(
                getAeSupport(), patternDetails, prototype, requestedCount);
    }

    @Override
    @Nullable
    default CountedCraftingAdmission prepareBatch(IPatternDetails patternDetails,
            KeyCounter[] prototype, long requestedCount) {
        return DataCraftingAdmission.prepare(
                getAeSupport(), patternDetails, prototype, requestedCount);
    }

    @Override
    default CountedCraftingPreparation prepareBatch(IPatternDetails patternDetails,
            KeyCounter[] prototype, long requestedCount, CraftingDispatchTargetAvailability availability) {
        return DataCraftingAdmission.prepareTargetAware(getAeSupport(), patternDetails,
                prototype, requestedCount, availability);
    }
}
