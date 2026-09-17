package com.beipuo.mekenergistics.mixin;

import com.beipuo.mekenergistics.blockentity.api.MeUpgradeableMachine;
import com.beipuo.mekenergistics.blockentity.support.MeRecipeMachineAeSupport;
import com.jerry.mekmm.api.recipes.PlantingRecipe;
import com.jerry.mekmm.api.recipes.RecyclerRecipe;
import com.jerry.mekmm.api.recipes.StamperRecipe;
import com.jerry.mekmm.api.recipes.basic.MMBasicItemStackChemicalToItemStackRecipe;
import com.jerry.mekmm.common.tile.factory.TileEntityMoreMachineFactory;
import com.jerry.mekmm.common.tile.factory.TileEntityMoreMachineItemStackToItemStackFactory;
import com.jerry.mekmm.common.tile.factory.TileEntityPlantingFactory;
import com.jerry.mekmm.common.tile.factory.TileEntityRecyclingFactory;
import com.jerry.mekmm.common.tile.factory.TileEntityReplicatingFactory;
import com.jerry.mekmm.common.tile.factory.TileEntityStampingFactory;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {TileEntityMoreMachineItemStackToItemStackFactory.class,
        TileEntityRecyclingFactory.class, TileEntityStampingFactory.class,
        TileEntityPlantingFactory.class, TileEntityReplicatingFactory.class}, remap = false)
public abstract class MekmmFactoryMeUpgradeEnergyMixin {
    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/ItemStackToItemStackRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapItem(ItemStackToItemStackRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<ItemStackToItemStackRecipe>> cir) { mekenergistics$wrap(cir); }

    @Inject(method = "createNewCachedRecipe(Lcom/jerry/mekmm/api/recipes/RecyclerRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapRecycler(RecyclerRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<RecyclerRecipe>> cir) { mekenergistics$wrap(cir); }

    @Inject(method = "createNewCachedRecipe(Lcom/jerry/mekmm/api/recipes/StamperRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapStamper(StamperRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<StamperRecipe>> cir) { mekenergistics$wrap(cir); }

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
            TileEntityMoreMachineFactory<?> tile = (TileEntityMoreMachineFactory<?>) (Object) this;
            cir.setReturnValue(((MeRecipeMachineAeSupport<?>) machine.getRecipeAeSupport())
                    .wrapRecipeEnergy(tile.getEnergyContainer(), cir.getReturnValue()));
        }
    }
}
