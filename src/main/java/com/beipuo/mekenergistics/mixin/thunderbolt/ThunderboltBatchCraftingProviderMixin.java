package com.beipuo.mekenergistics.mixin.thunderbolt;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.support.AbstractMeAeSupport;
import com.beipuo.mekenergistics.blockentity.support.MeFactoryAeSupport;
import com.beipuo.mekenergistics.compat.thunderbolt.ThunderboltBatchCompat;
import com.moakiee.thunderbolt.api.crafting.batch.BatchDispatchMode;
import com.moakiee.thunderbolt.api.crafting.batch.IBatchCraftingProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MeAeMachine.class)
public interface ThunderboltBatchCraftingProviderMixin extends IBatchCraftingProvider {
    @Shadow
    AbstractMeAeSupport<?> getRecipeAeSupport();

    @Shadow
    boolean isSmartPatternMultiplicationEnabled();

    @Override
    default long getBatchCapacity(IPatternDetails details) {
        return ThunderboltBatchCompat.getBatchCapacity(getRecipeAeSupport(),
                isSmartPatternMultiplicationEnabled(), details);
    }

    @Override
    default BatchDispatchMode getBatchDispatchMode(IPatternDetails details) {
        return ThunderboltBatchCompat.getBatchDispatchMode(isSmartPatternMultiplicationEnabled());
    }

    @Override
    default long pushBatch(IPatternDetails details, KeyCounter[] oneCopyTemplate, long maxCraft) {
        return ThunderboltBatchCompat.pushBatch(getRecipeAeSupport(),
                isSmartPatternMultiplicationEnabled(), details, oneCopyTemplate, maxCraft);
    }
}

@Mixin(MeFactoryAeMachine.class)
interface ThunderboltBatchFactoryCraftingProviderMixin extends IBatchCraftingProvider {
    @Shadow
    MeFactoryAeSupport getAeSupport();

    @Shadow
    boolean isSmartPatternMultiplicationEnabled();

    @Override
    default long getBatchCapacity(IPatternDetails details) {
        return ThunderboltBatchCompat.getBatchCapacity(getAeSupport(),
                isSmartPatternMultiplicationEnabled(), details);
    }

    @Override
    default BatchDispatchMode getBatchDispatchMode(IPatternDetails details) {
        return ThunderboltBatchCompat.getBatchDispatchMode(isSmartPatternMultiplicationEnabled());
    }

    @Override
    default long pushBatch(IPatternDetails details, KeyCounter[] oneCopyTemplate, long maxCraft) {
        return ThunderboltBatchCompat.pushBatch(getAeSupport(),
                isSmartPatternMultiplicationEnabled(), details, oneCopyTemplate, maxCraft);
    }
}
