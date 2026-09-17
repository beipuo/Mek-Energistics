package com.beipuo.mekenergistics.mixin;

import com.beipuo.mekenergistics.blockentity.api.MeUpgradeableMachine;
import com.beipuo.mekenergistics.blockentity.support.MeRecipeMachineAeSupport;
import com.jerry.mekextras.common.tile.factory.TileEntityExtraCombiningFactory;
import com.jerry.mekextras.common.tile.factory.TileEntityExtraFactory;
import com.jerry.mekextras.common.tile.factory.TileEntityExtraItemStackChemicalToItemStackFactory;
import com.jerry.mekextras.common.tile.factory.TileEntityExtraItemStackToItemStackFactory;
import com.jerry.mekextras.common.tile.factory.TileEntityExtraSawingFactory;
import mekanism.api.recipes.CombinerRecipe;
import mekanism.api.recipes.ItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.api.recipes.SawmillRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {TileEntityExtraItemStackToItemStackFactory.class,
        TileEntityExtraItemStackChemicalToItemStackFactory.class,
        TileEntityExtraCombiningFactory.class, TileEntityExtraSawingFactory.class}, remap = false)
public abstract class MekExtrasFactoryMeUpgradeEnergyMixin {
    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/ItemStackToItemStackRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapItem(ItemStackToItemStackRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<ItemStackToItemStackRecipe>> cir) { mekenergistics$wrap(cir); }

    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/ItemStackChemicalToItemStackRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapChemical(ItemStackChemicalToItemStackRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<ItemStackChemicalToItemStackRecipe>> cir) { mekenergistics$wrap(cir); }

    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/CombinerRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapCombining(CombinerRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<CombinerRecipe>> cir) { mekenergistics$wrap(cir); }

    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/SawmillRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapSawing(SawmillRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<SawmillRecipe>> cir) { mekenergistics$wrap(cir); }

    @Unique
    private <RECIPE extends mekanism.api.recipes.MekanismRecipe<?>> void mekenergistics$wrap(
            CallbackInfoReturnable<CachedRecipe<RECIPE>> cir) {
        MeUpgradeableMachine machine = (MeUpgradeableMachine) this;
        if (machine.isMeUpgradeTarget()) {
            TileEntityExtraFactory<?> tile = (TileEntityExtraFactory<?>) (Object) this;
            cir.setReturnValue(((MeRecipeMachineAeSupport<?>) machine.getRecipeAeSupport())
                    .wrapRecipeEnergy(tile.getEnergyContainer(), cir.getReturnValue()));
        }
    }
}
