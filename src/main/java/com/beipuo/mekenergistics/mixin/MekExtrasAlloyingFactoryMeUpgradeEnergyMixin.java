package com.beipuo.mekenergistics.mixin;

import com.beipuo.mekenergistics.blockentity.api.MeUpgradeableMachine;
import com.beipuo.mekenergistics.blockentity.support.MeRecipeMachineAeSupport;
import com.beipuo.mekenergistics.upgrade.EvolvedAlloyingFactoryUpgradeAccess;
import com.jerry.mekextras.common.tile.factory.TileEntityExtraAlloyingFactory;
import fr.iglee42.evolvedmekanism.recipes.AlloyerRecipe;
import fr.iglee42.evolvedmekanism.tiles.LimitedInputInventorySlot;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.cache.CachedRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileEntityExtraAlloyingFactory.class, remap = false)
public abstract class MekExtrasAlloyingFactoryMeUpgradeEnergyMixin
        implements EvolvedAlloyingFactoryUpgradeAccess {
    @Shadow LimitedInputInventorySlot secondExtraSlot;

    @Override
    public IInventorySlot mekenergistics$getSecondExtraSlot() {
        return this.secondExtraSlot;
    }

    @Inject(method = "createNewCachedRecipe(Lfr/iglee42/evolvedmekanism/recipes/AlloyerRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true)
    private void mekenergistics$wrapAlloying(AlloyerRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<AlloyerRecipe>> cir) {
        MeUpgradeableMachine machine = (MeUpgradeableMachine) this;
        if (machine.isMeUpgradeTarget()) {
            TileEntityExtraAlloyingFactory tile = (TileEntityExtraAlloyingFactory) (Object) this;
            cir.setReturnValue(((MeRecipeMachineAeSupport<?>) machine.getRecipeAeSupport())
                    .wrapRecipeEnergy(tile.getEnergyContainer(), cir.getReturnValue()));
        }
    }
}
