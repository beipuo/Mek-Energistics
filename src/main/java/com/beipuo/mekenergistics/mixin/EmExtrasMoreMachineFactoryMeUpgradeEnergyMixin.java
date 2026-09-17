package com.beipuo.mekenergistics.mixin;

import com.beipuo.mekenergistics.blockentity.api.MeUpgradeableMachine;
import com.beipuo.mekenergistics.blockentity.support.MeRecipeMachineAeSupport;
import com.jerry.mekmm.api.recipes.PlantingRecipe;
import com.jerry.mekmm.api.recipes.basic.MMBasicItemStackChemicalToItemStackRecipe;
import io.github.masyumero.emextras.common.integration.mekmm.tile.factory.TileEntityEMExtraMoreMachineFactory;
import io.github.masyumero.emextras.common.integration.mekmm.tile.factory.TileEntityEMExtraPlantingFactory;
import io.github.masyumero.emextras.common.integration.mekmm.tile.factory.TileEntityEMExtraReplicatingFactory;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {TileEntityEMExtraPlantingFactory.class,
        TileEntityEMExtraReplicatingFactory.class}, remap = false)
public abstract class EmExtrasMoreMachineFactoryMeUpgradeEnergyMixin {
    @Inject(method = "createNewCachedRecipe(Lcom/jerry/mekmm/api/recipes/PlantingRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapPlanting(PlantingRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<PlantingRecipe>> cir) { mekenergistics$wrap(cir); }

    @Inject(method = "createNewCachedRecipe(Lcom/jerry/mekmm/api/recipes/basic/MMBasicItemStackChemicalToItemStackRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapReplicating(MMBasicItemStackChemicalToItemStackRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<MMBasicItemStackChemicalToItemStackRecipe>> cir) {
        mekenergistics$wrap(cir);
    }

    @Unique
    private <RECIPE extends MekanismRecipe<?>> void mekenergistics$wrap(
            CallbackInfoReturnable<CachedRecipe<RECIPE>> cir) {
        MeUpgradeableMachine machine = (MeUpgradeableMachine) this;
        if (machine.isMeUpgradeTarget()) {
            TileEntityEMExtraMoreMachineFactory<?> tile =
                    (TileEntityEMExtraMoreMachineFactory<?>) (Object) this;
            cir.setReturnValue(((MeRecipeMachineAeSupport<?>) machine.getRecipeAeSupport())
                    .wrapRecipeEnergy(tile.getEnergyContainer(), cir.getReturnValue()));
        }
    }
}
