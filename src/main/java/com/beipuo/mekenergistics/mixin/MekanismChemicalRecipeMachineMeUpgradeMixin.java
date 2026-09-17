package com.beipuo.mekenergistics.mixin;

import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import com.beipuo.mekenergistics.blockentity.api.MeUpgradeableMachine;
import com.beipuo.mekenergistics.blockentity.support.AbstractMeAeSupport;
import com.beipuo.mekenergistics.blockentity.support.io.MeInputLayout;
import com.beipuo.mekenergistics.blockentity.support.io.MeOutputPort;
import com.beipuo.mekenergistics.upgrade.MeUpgradeContainer;
import com.beipuo.mekenergistics.upgrade.MeUpgradeMachineProfile;
import com.beipuo.mekenergistics.upgrade.MeUpgradeRecipeMachineRuntime;
import com.beipuo.mekenergistics.upgrade.MeUpgradeStateOwner;
import com.beipuo.mekenergistics.upgrade.MekanismChemicalUpgradeProfiles;
import java.util.List;
import mekanism.api.IContentsListener;
import mekanism.api.recipes.ChemicalChemicalToChemicalRecipe;
import mekanism.api.recipes.ChemicalCrystallizerRecipe;
import mekanism.api.recipes.ChemicalDissolutionRecipe;
import mekanism.api.recipes.ChemicalToChemicalRecipe;
import mekanism.api.recipes.ElectrolysisRecipe;
import mekanism.api.recipes.FluidChemicalToChemicalRecipe;
import mekanism.api.recipes.ItemStackToChemicalRecipe;
import mekanism.api.recipes.ItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.NucleosynthesizingRecipe;
import mekanism.api.recipes.PressurizedReactionRecipe;
import mekanism.api.recipes.RotaryRecipe;
import mekanism.api.recipes.basic.BasicItemStackToFluidOptionalItemRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.machine.TileEntityAntiprotonicNucleosynthesizer;
import mekanism.common.tile.machine.TileEntityChemicalCrystallizer;
import mekanism.common.tile.machine.TileEntityChemicalDissolutionChamber;
import mekanism.common.tile.machine.TileEntityChemicalInfuser;
import mekanism.common.tile.machine.TileEntityChemicalOxidizer;
import mekanism.common.tile.machine.TileEntityChemicalWasher;
import mekanism.common.tile.machine.TileEntityElectrolyticSeparator;
import mekanism.common.tile.machine.TileEntityIsotopicCentrifuge;
import mekanism.common.tile.machine.TileEntityNutritionalLiquifier;
import mekanism.common.tile.machine.TileEntityPigmentExtractor;
import mekanism.common.tile.machine.TileEntityPigmentMixer;
import mekanism.common.tile.machine.TileEntityPaintingMachine;
import mekanism.common.tile.machine.TileEntityPressurizedReactionChamber;
import mekanism.common.tile.machine.TileEntityRotaryCondensentrator;
import mekanism.common.tile.machine.TileEntitySolarNeutronActivator;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {TileEntityAntiprotonicNucleosynthesizer.class, TileEntityChemicalCrystallizer.class,
        TileEntityChemicalDissolutionChamber.class, TileEntityChemicalInfuser.class,
        TileEntityChemicalOxidizer.class, TileEntityChemicalWasher.class,
        TileEntityElectrolyticSeparator.class, TileEntityIsotopicCentrifuge.class,
        TileEntityNutritionalLiquifier.class, TileEntityPaintingMachine.class, TileEntityPigmentExtractor.class,
        TileEntityPigmentMixer.class, TileEntityPressurizedReactionChamber.class,
        TileEntityRotaryCondensentrator.class, TileEntitySolarNeutronActivator.class}, remap = false)
public abstract class MekanismChemicalRecipeMachineMeUpgradeMixin implements MeUpgradeableMachine, MeUpgradeStateOwner, IBlockEntityExtension {
    @Unique private MeUpgradeRecipeMachineRuntime mekenergistics$runtime;

    @Unique private TileEntityMekanism mekenergistics$tile() { return (TileEntityMekanism) (Object) this; }
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

    @Override public MeUpgradeMachineProfile<?> getMeUpgradeProfile() {
        return MekanismChemicalUpgradeProfiles.forTile(mekenergistics$tile());
    }
    @Override public boolean isMeUpgradeTarget() { return getMeUpgradeProfile() != null; }
    @Override public boolean isMeUpgradeActive() { return mekenergistics$runtime().active(isMeUpgradeTarget()); }
    @Override public AbstractMeAeSupport<?> getRecipeAeSupport() { return mekenergistics$runtime().support(); }
    @Override public AeOutputMode getAeOutputMode() { return mekenergistics$runtime().outputMode(); }
    @Override public void cycleAeOutputMode() { mekenergistics$runtime().cycleOutputMode(); }

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

    @Inject(method = "getInitialInventory", at = @At("RETURN"), cancellable = true)
    private void mekenergistics$addPatternSlots(IContentsListener listener, IContentsListener recipeCacheListener,
            IContentsListener recipeCacheUnpauseListener,
            CallbackInfoReturnable<@NotNull IInventorySlotHolder> cir) {
        if (isMeUpgradeTarget()) cir.setReturnValue(
                mekenergistics$runtime().withPatternSlots(cir.getReturnValue(), recipeCacheListener));
    }

    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/NucleosynthesizingRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;", at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapNucleosynthesizing(NucleosynthesizingRecipe recipe, int index, CallbackInfoReturnable<CachedRecipe<NucleosynthesizingRecipe>> cir) { mekenergistics$wrapEnergy(cir); }
    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/ChemicalCrystallizerRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;", at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapCrystallizing(ChemicalCrystallizerRecipe recipe, int index, CallbackInfoReturnable<CachedRecipe<ChemicalCrystallizerRecipe>> cir) { mekenergistics$wrapEnergy(cir); }
    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/ChemicalDissolutionRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;", at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapDissolving(ChemicalDissolutionRecipe recipe, int index, CallbackInfoReturnable<CachedRecipe<ChemicalDissolutionRecipe>> cir) { mekenergistics$wrapEnergy(cir); }
    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/ChemicalChemicalToChemicalRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;", at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapChemicalChemical(ChemicalChemicalToChemicalRecipe recipe, int index, CallbackInfoReturnable<CachedRecipe<ChemicalChemicalToChemicalRecipe>> cir) { mekenergistics$wrapEnergy(cir); }
    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/ItemStackToChemicalRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;", at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapItemChemical(ItemStackToChemicalRecipe recipe, int index, CallbackInfoReturnable<CachedRecipe<ItemStackToChemicalRecipe>> cir) { mekenergistics$wrapEnergy(cir); }
    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/ItemStackChemicalToItemStackRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;", at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapPainting(ItemStackChemicalToItemStackRecipe recipe, int index, CallbackInfoReturnable<CachedRecipe<ItemStackChemicalToItemStackRecipe>> cir) { mekenergistics$wrapEnergy(cir); }
    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/FluidChemicalToChemicalRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;", at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapWashing(FluidChemicalToChemicalRecipe recipe, int index, CallbackInfoReturnable<CachedRecipe<FluidChemicalToChemicalRecipe>> cir) { mekenergistics$wrapEnergy(cir); }
    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/ElectrolysisRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;", at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapElectrolysis(ElectrolysisRecipe recipe, int index, CallbackInfoReturnable<CachedRecipe<ElectrolysisRecipe>> cir) { mekenergistics$wrapEnergy(cir); }
    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/ChemicalToChemicalRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;", at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapChemical(ChemicalToChemicalRecipe recipe, int index, CallbackInfoReturnable<CachedRecipe<ChemicalToChemicalRecipe>> cir) { mekenergistics$wrapEnergy(cir); }
    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/basic/BasicItemStackToFluidOptionalItemRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;", at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapLiquifying(BasicItemStackToFluidOptionalItemRecipe recipe, int index, CallbackInfoReturnable<CachedRecipe<BasicItemStackToFluidOptionalItemRecipe>> cir) { mekenergistics$wrapEnergy(cir); }
    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/PressurizedReactionRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;", at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapReaction(PressurizedReactionRecipe recipe, int index, CallbackInfoReturnable<CachedRecipe<PressurizedReactionRecipe>> cir) { mekenergistics$wrapEnergy(cir); }
    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/RotaryRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;", at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapRotary(RotaryRecipe recipe, int index, CallbackInfoReturnable<CachedRecipe<RotaryRecipe>> cir) { mekenergistics$wrapEnergy(cir); }

    @Inject(method = "nextMode", at = @At("RETURN"), require = 0)
    private void mekenergistics$refreshRotaryPatternIo(CallbackInfo ci) {
        if (mekenergistics$tile() instanceof TileEntityRotaryCondensentrator && this.mekenergistics$runtime != null) {
            this.mekenergistics$runtime.support().invalidatePatternIoCache();
        }
    }

    @Unique
    private <RECIPE extends MekanismRecipe<?>> void mekenergistics$wrapEnergy(CallbackInfoReturnable<CachedRecipe<RECIPE>> cir) {
        MachineEnergyContainer<?> energyContainer = mekenergistics$energyContainer();
        if (energyContainer != null) {
            cir.setReturnValue(mekenergistics$runtime().wrapEnergy(energyContainer, cir.getReturnValue()));
        }
    }

    @Unique
    private MachineEnergyContainer<?> mekenergistics$energyContainer() {
        for (var energyContainer : mekenergistics$tile().getEnergyContainers(null)) {
            if (energyContainer instanceof MachineEnergyContainer<?> machineEnergyContainer) {
                return machineEnergyContainer;
            }
        }
        return null;
    }

    @Inject(method = "onUpdateServer", at = @At("RETURN"), cancellable = true)
    private void mekenergistics$processPatternIo(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(mekenergistics$runtime().tick(isMeUpgradeActive(), cir.getReturnValue()));
    }

    @Override public void createMeNodeIfActive() { mekenergistics$runtime().createNodeIfActive(isMeUpgradeActive()); }
    @Override public void destroyMeNode() { if (this.mekenergistics$runtime != null) this.mekenergistics$runtime.destroyNode(); }
    @Override public void onChunkUnloaded() { destroyMeNode(); }
    @Override public void addMeTrackers(MekanismContainer container) { mekenergistics$runtime().addTrackers(container, isMeUpgradeTarget()); }
    @Override public void saveMeState(CompoundTag tag, HolderLookup.Provider registries) { if (this.mekenergistics$runtime != null) this.mekenergistics$runtime.save(tag, registries); }
    @Override public void loadMeState(CompoundTag tag, HolderLookup.Provider registries) { if (isMeUpgradeTarget()) mekenergistics$runtime().load(tag, registries); }
}
