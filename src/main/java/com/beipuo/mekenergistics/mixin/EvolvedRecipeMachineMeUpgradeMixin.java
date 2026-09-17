package com.beipuo.mekenergistics.mixin;

import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import com.beipuo.mekenergistics.blockentity.api.MeUpgradeableMachine;
import com.beipuo.mekenergistics.blockentity.support.AbstractMeAeSupport;
import com.beipuo.mekenergistics.blockentity.support.io.MeInputLayout;
import com.beipuo.mekenergistics.blockentity.support.io.MeOutputPort;
import com.beipuo.mekenergistics.upgrade.EvolvedRecipeUpgradeProfiles;
import com.beipuo.mekenergistics.upgrade.MeUpgradeContainer;
import com.beipuo.mekenergistics.upgrade.MeUpgradeMachineProfile;
import com.beipuo.mekenergistics.upgrade.MeUpgradeRecipeMachineRuntime;
import com.beipuo.mekenergistics.upgrade.MeUpgradeStateOwner;
import fr.iglee42.evolvedmekanism.recipes.AlloyerRecipe;
import fr.iglee42.evolvedmekanism.recipes.ChemixerRecipe;
import fr.iglee42.evolvedmekanism.recipes.SolidificationRecipe;
import fr.iglee42.evolvedmekanism.tiles.machine.TileEntityAlloyer;
import fr.iglee42.evolvedmekanism.tiles.machine.TileEntityChemixer;
import fr.iglee42.evolvedmekanism.tiles.machine.TileEntityMelter;
import fr.iglee42.evolvedmekanism.tiles.machine.TileEntitySolidifier;
import java.util.List;
import mekanism.api.IContentsListener;
import mekanism.api.recipes.ItemStackToFluidRecipe;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {TileEntityAlloyer.class, TileEntitySolidifier.class, TileEntityMelter.class,
        TileEntityChemixer.class}, remap = false)
public abstract class EvolvedRecipeMachineMeUpgradeMixin implements MeUpgradeableMachine, MeUpgradeStateOwner, IBlockEntityExtension {
    @Unique private MeUpgradeRecipeMachineRuntime mekenergistics$runtime;

    @Unique
    private TileEntityMekanism mekenergistics$tile() {
        return (TileEntityMekanism) (Object) this;
    }

    @Unique
    private MeUpgradeRecipeMachineRuntime mekenergistics$runtime() {
        if (this.mekenergistics$runtime == null) {
            this.mekenergistics$runtime = new MeUpgradeRecipeMachineRuntime(mekenergistics$tile(), AeOutputMode.BOTH);
        }
        return this.mekenergistics$runtime;
    }

    @Override
    public MeUpgradeContainer getMeUpgradeContainer() {
        return mekenergistics$runtime().upgrades();
    }

    @Override
    public boolean supportsNativePatternProvider() {
        return mekenergistics$runtime().supportsNativePatternProvider();
    }

    @Override
    public boolean isPatternInventoryEmpty() {
        return mekenergistics$runtime().isPatternInventoryEmpty();
    }

    @Override
    public void onMeUpgradeStateChanged() {
        mekenergistics$runtime().onMeUpgradeStateChanged();
    }

    @Override
    public MeUpgradeMachineProfile<?> getMeUpgradeProfile() {
        return EvolvedRecipeUpgradeProfiles.forTile(mekenergistics$tile());
    }

    @Override public boolean isMeUpgradeTarget() { return getMeUpgradeProfile() != null; }
    @Override public boolean isMeUpgradeActive() { return mekenergistics$runtime().active(isMeUpgradeTarget()); }
    @Override public AbstractMeAeSupport<?> getRecipeAeSupport() { return mekenergistics$runtime().support(); }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public MeInputLayout getPatternInputLayout() {
        return ((MeUpgradeMachineProfile) getMeUpgradeProfile()).inputLayoutFor(mekenergistics$tile());
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<? extends MeOutputPort> getPatternOutputPorts() {
        return ((MeUpgradeMachineProfile) getMeUpgradeProfile()).outputPortsFor(mekenergistics$tile());
    }

    @Override public AeOutputMode getAeOutputMode() { return mekenergistics$runtime().outputMode(); }
    @Override public void cycleAeOutputMode() { mekenergistics$runtime().cycleOutputMode(); }

    @Inject(method = "getInitialInventory", at = @At("RETURN"), cancellable = true)
    private void mekenergistics$addPatternSlots(IContentsListener listener, IContentsListener recipeCacheListener,
            IContentsListener recipeCacheUnpauseListener,
            CallbackInfoReturnable<@NotNull IInventorySlotHolder> cir) {
        if (isMeUpgradeTarget()) {
            cir.setReturnValue(mekenergistics$runtime().withPatternSlots(cir.getReturnValue(), recipeCacheListener));
        }
    }

    @Inject(method = "createNewCachedRecipe(Lfr/iglee42/evolvedmekanism/recipes/AlloyerRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapAlloyer(AlloyerRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<AlloyerRecipe>> cir) { mekenergistics$wrapEnergy(cir); }

    @Inject(method = "createNewCachedRecipe(Lfr/iglee42/evolvedmekanism/recipes/SolidificationRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapSolidifier(SolidificationRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<SolidificationRecipe>> cir) { mekenergistics$wrapEnergy(cir); }

    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/ItemStackToFluidRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapMelter(ItemStackToFluidRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<ItemStackToFluidRecipe>> cir) { mekenergistics$wrapEnergy(cir); }

    @Inject(method = "createNewCachedRecipe(Lfr/iglee42/evolvedmekanism/recipes/ChemixerRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapChemixer(ChemixerRecipe recipe, int cacheIndex,
            CallbackInfoReturnable<CachedRecipe<ChemixerRecipe>> cir) { mekenergistics$wrapEnergy(cir); }

    @Unique
    private <RECIPE extends MekanismRecipe<?>> void mekenergistics$wrapEnergy(
            CallbackInfoReturnable<CachedRecipe<RECIPE>> cir) {
        cir.setReturnValue(mekenergistics$runtime().wrapEnergy(
                mekenergistics$energyContainer(), cir.getReturnValue()));
    }

    @Unique
    private MachineEnergyContainer<?> mekenergistics$energyContainer() {
        Object tile = this;
        if (tile instanceof TileEntityAlloyer alloyer) return alloyer.getEnergyContainer();
        if (tile instanceof TileEntitySolidifier solidifier) return solidifier.getEnergyContainer();
        if (tile instanceof TileEntityMelter melter) return melter.getEnergyContainer();
        return ((TileEntityChemixer) tile).getEnergyContainer();
    }

    @Inject(method = "onUpdateServer", at = @At("RETURN"), cancellable = true)
    private void mekenergistics$processPatternIo(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(mekenergistics$runtime().tick(isMeUpgradeActive(), cir.getReturnValue()));
    }

    @Override public void createMeNodeIfActive() { mekenergistics$runtime().createNodeIfActive(isMeUpgradeActive()); }
    @Override public void destroyMeNode() { if (this.mekenergistics$runtime != null) this.mekenergistics$runtime.destroyNode(); }
    @Override public void onChunkUnloaded() { destroyMeNode(); }
    @Override public void addMeTrackers(MekanismContainer container) { mekenergistics$runtime().addTrackers(container, isMeUpgradeTarget()); }
    @Override public void saveMeState(CompoundTag tag, HolderLookup.Provider registries) {
        if (this.mekenergistics$runtime != null) this.mekenergistics$runtime.save(tag, registries);
    }
    @Override public void loadMeState(CompoundTag tag, HolderLookup.Provider registries) {
        if (isMeUpgradeTarget()) mekenergistics$runtime().load(tag, registries);
    }
}
