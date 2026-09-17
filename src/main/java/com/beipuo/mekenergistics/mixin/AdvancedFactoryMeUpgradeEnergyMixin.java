package com.beipuo.mekenergistics.mixin;

import com.beipuo.mekenergistics.blockentity.api.MeUpgradeableMachine;
import com.beipuo.mekenergistics.blockentity.support.MeRecipeMachineAeSupport;
import com.beipuo.mekenergistics.upgrade.AdvancedFactoryUpgradeAccess;
import mekanism.api.recipes.ChemicalCrystallizerRecipe;
import mekanism.api.recipes.ChemicalDissolutionRecipe;
import mekanism.api.recipes.ChemicalToChemicalRecipe;
import mekanism.api.recipes.FluidChemicalToChemicalRecipe;
import mekanism.api.recipes.ItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.ItemStackToChemicalRecipe;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.PressurizedReactionRecipe;
import mekanism.api.recipes.basic.BasicItemStackToFluidOptionalItemRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = {
        "com.jerry.mekaf.common.tile.factory.TileEntityPaintingFactory",
        "com.jerry.mekaf.common.tile.factory.TileEntityDissolvingFactory",
        "com.jerry.mekaf.common.tile.factory.TileEntityItemStackToChemicalStackFactory",
        "com.jerry.mekaf.common.tile.factory.TileEntityCrystallizingFactory",
        "com.jerry.mekaf.common.tile.factory.TileEntityCentrifugingFactory",
        "com.jerry.mekaf.common.tile.factory.TileEntityWashingFactory",
        "com.jerry.mekaf.common.tile.factory.TileEntityLiquifyingFactory",
        "com.jerry.mekaf.common.tile.factory.TileEntityPressurizedReactingFactory",
        "com.jerry.mekextras.common.integration.mekaf.tile.factory.TileEntityExtraPaintingFactory",
        "com.jerry.mekextras.common.integration.mekaf.tile.factory.TileEntityExtraDissolvingFactory",
        "com.jerry.mekextras.common.integration.mekaf.tile.factory.TileEntityExtraItemStackToChemicalStackFactory",
        "com.jerry.mekextras.common.integration.mekaf.tile.factory.TileEntityExtraCrystallizingFactory",
        "com.jerry.mekextras.common.integration.mekaf.tile.factory.TileEntityExtraCentrifugingFactory",
        "com.jerry.mekextras.common.integration.mekaf.tile.factory.TileEntityExtraWashingFactory",
        "com.jerry.mekextras.common.integration.mekaf.tile.factory.TileEntityExtraLiquifyingFactory",
        "com.jerry.mekextras.common.integration.mekaf.tile.factory.TileEntityExtraPRCFactory",
        "io.github.masyumero.emextras.common.integration.mekaf.tile.factory.TileEntityEMExtraPaintingFactory",
        "io.github.masyumero.emextras.common.integration.mekaf.tile.factory.TileEntityEMExtraDissolvingFactory",
        "io.github.masyumero.emextras.common.integration.mekaf.tile.factory.TileEntityEMExtraItemStackToChemicalStackFactory",
        "io.github.masyumero.emextras.common.integration.mekaf.tile.factory.TileEntityEMExtraCrystallizingFactory",
        "io.github.masyumero.emextras.common.integration.mekaf.tile.factory.TileEntityEMExtraCentrifugingFactory",
        "io.github.masyumero.emextras.common.integration.mekaf.tile.factory.TileEntityEMExtraWashingFactory",
        "io.github.masyumero.emextras.common.integration.mekaf.tile.factory.TileEntityEMExtraLiquifyingFactory",
        "io.github.masyumero.emextras.common.integration.mekaf.tile.factory.TileEntityEMExtraPRCFactory"
}, remap = false)
public abstract class AdvancedFactoryMeUpgradeEnergyMixin {
    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/ItemStackChemicalToItemStackRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapItemChemical(ItemStackChemicalToItemStackRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<ItemStackChemicalToItemStackRecipe>> cir) { mekenergistics$wrap(cir); }

    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/ChemicalDissolutionRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapDissolving(ChemicalDissolutionRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<ChemicalDissolutionRecipe>> cir) { mekenergistics$wrap(cir); }

    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/ItemStackToChemicalRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapItemToChemical(ItemStackToChemicalRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<ItemStackToChemicalRecipe>> cir) { mekenergistics$wrap(cir); }

    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/ChemicalCrystallizerRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapCrystallizing(ChemicalCrystallizerRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<ChemicalCrystallizerRecipe>> cir) { mekenergistics$wrap(cir); }

    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/ChemicalToChemicalRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapChemical(ChemicalToChemicalRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<ChemicalToChemicalRecipe>> cir) { mekenergistics$wrap(cir); }

    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/FluidChemicalToChemicalRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapWashing(FluidChemicalToChemicalRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<FluidChemicalToChemicalRecipe>> cir) { mekenergistics$wrap(cir); }

    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/basic/BasicItemStackToFluidOptionalItemRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapLiquifying(BasicItemStackToFluidOptionalItemRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<BasicItemStackToFluidOptionalItemRecipe>> cir) { mekenergistics$wrap(cir); }

    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/PressurizedReactionRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapPressurized(PressurizedReactionRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<PressurizedReactionRecipe>> cir) { mekenergistics$wrap(cir); }

    @Unique
    private <RECIPE extends MekanismRecipe<?>> void mekenergistics$wrap(
            CallbackInfoReturnable<CachedRecipe<RECIPE>> cir) {
        MeUpgradeableMachine machine = (MeUpgradeableMachine) this;
        if (machine.isMeUpgradeTarget()) {
            AdvancedFactoryUpgradeAccess factory = (AdvancedFactoryUpgradeAccess) this;
            cir.setReturnValue(((MeRecipeMachineAeSupport<?>) machine.getRecipeAeSupport())
                    .wrapRecipeEnergy(factory.meUpgradeEnergyContainer(), cir.getReturnValue()));
        }
    }
}
