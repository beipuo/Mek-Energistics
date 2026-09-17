package com.beipuo.mekenergistics.mixin;

import com.beipuo.mekenergistics.blockentity.api.MeUpgradeableMachine;
import com.beipuo.mekenergistics.blockentity.support.MeRecipeMachineAeSupport;
import fr.iglee42.evolvedmekanism.recipes.AlloyerRecipe;
import io.github.masyumero.emextras.common.tile.factory.TileEntityEMExtraAlloyingFactory;
import io.github.masyumero.emextras.common.tile.factory.TileEntityEMExtraCombiningFactory;
import io.github.masyumero.emextras.common.tile.factory.TileEntityEMExtraFactory;
import io.github.masyumero.emextras.common.tile.factory.TileEntityEMExtraItemStackChemicalToItemStackFactory;
import io.github.masyumero.emextras.common.tile.factory.TileEntityEMExtraItemStackToItemStackFactory;
import io.github.masyumero.emextras.common.tile.factory.TileEntityEMExtraSawingFactory;
import mekanism.api.recipes.CombinerRecipe;
import mekanism.api.recipes.ItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.SawmillRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {TileEntityEMExtraItemStackToItemStackFactory.class,
        TileEntityEMExtraItemStackChemicalToItemStackFactory.class,
        TileEntityEMExtraCombiningFactory.class, TileEntityEMExtraSawingFactory.class,
        TileEntityEMExtraAlloyingFactory.class}, remap = false)
public abstract class EmExtrasFactoryMeUpgradeEnergyMixin {
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

    @Inject(method = "createNewCachedRecipe(Lfr/iglee42/evolvedmekanism/recipes/AlloyerRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapAlloying(AlloyerRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<AlloyerRecipe>> cir) { mekenergistics$wrap(cir); }

    @Unique
    private <RECIPE extends MekanismRecipe<?>> void mekenergistics$wrap(
            CallbackInfoReturnable<CachedRecipe<RECIPE>> cir) {
        MeUpgradeableMachine machine = (MeUpgradeableMachine) this;
        if (machine.isMeUpgradeTarget()) {
            TileEntityEMExtraFactory<?> tile = (TileEntityEMExtraFactory<?>) (Object) this;
            cir.setReturnValue(((MeRecipeMachineAeSupport<?>) machine.getRecipeAeSupport())
                    .wrapRecipeEnergy(tile.getEnergyContainer(), cir.getReturnValue()));
        }
    }
}
