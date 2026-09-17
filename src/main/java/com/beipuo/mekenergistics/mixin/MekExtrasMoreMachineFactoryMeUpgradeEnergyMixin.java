package com.beipuo.mekenergistics.mixin;

import com.beipuo.mekenergistics.blockentity.api.MeUpgradeableMachine;
import com.beipuo.mekenergistics.blockentity.support.MeRecipeMachineAeSupport;
import com.jerry.mekextras.common.integration.mekmm.tile.factory.TileEntityExtraMoreMachineItemStackToItemStackFactory;
import com.jerry.mekextras.common.integration.mekmm.tile.factory.TileEntityExtraMoreMachineFactory;
import com.jerry.mekextras.common.integration.mekmm.tile.factory.TileEntityExtraPlantingFactory;
import com.jerry.mekextras.common.integration.mekmm.tile.factory.TileEntityExtraPressingFactory;
import com.jerry.mekextras.common.integration.mekmm.tile.factory.TileEntityExtraRecyclingFactory;
import com.jerry.mekextras.common.integration.mekmm.tile.factory.TileEntityExtraReplicatingFactory;
import com.jerry.mekextras.common.integration.mekmm.tile.factory.TileEntityExtraStampingFactory;
import com.jerry.mekmm.api.recipes.PlantingRecipe;
import com.jerry.mekmm.api.recipes.RecyclerRecipe;
import com.jerry.mekmm.api.recipes.StamperRecipe;
import com.jerry.mekmm.api.recipes.TripleItemToItemRecipe;
import com.jerry.mekmm.api.recipes.basic.MMBasicItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {TileEntityExtraMoreMachineItemStackToItemStackFactory.class,
        TileEntityExtraRecyclingFactory.class, TileEntityExtraStampingFactory.class,
        TileEntityExtraPlantingFactory.class, TileEntityExtraReplicatingFactory.class,
        TileEntityExtraPressingFactory.class}, remap = false)
public abstract class MekExtrasMoreMachineFactoryMeUpgradeEnergyMixin {
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

    @Inject(method = "createNewCachedRecipe(Lcom/jerry/mekmm/api/recipes/TripleItemToItemRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapPressing(TripleItemToItemRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<TripleItemToItemRecipe>> cir) {
        mekenergistics$wrap(cir);
    }

    @Unique
    private <RECIPE extends MekanismRecipe<?>> void mekenergistics$wrap(
            CallbackInfoReturnable<CachedRecipe<RECIPE>> cir) {
        MeUpgradeableMachine machine = (MeUpgradeableMachine) this;
        if (machine.isMeUpgradeTarget()) {
            TileEntityExtraMoreMachineFactory<?> tile =
                    (TileEntityExtraMoreMachineFactory<?>) (Object) this;
            cir.setReturnValue(((MeRecipeMachineAeSupport<?>) machine.getRecipeAeSupport())
                    .wrapRecipeEnergy(tile.getEnergyContainer(), cir.getReturnValue()));
        }
    }
}
