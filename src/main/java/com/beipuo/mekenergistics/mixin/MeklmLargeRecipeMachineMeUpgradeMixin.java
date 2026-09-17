package com.beipuo.mekenergistics.mixin;

import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import com.beipuo.mekenergistics.blockentity.api.MeUpgradeableMachine;
import com.beipuo.mekenergistics.blockentity.support.AbstractMeAeSupport;
import com.beipuo.mekenergistics.blockentity.support.io.MeInputLayout;
import com.beipuo.mekenergistics.blockentity.support.io.MeMachineIoAdapter;
import com.beipuo.mekenergistics.blockentity.support.io.MeOutputPort;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.compat.catalog.CompatMachineCatalog;
import com.beipuo.mekenergistics.upgrade.MeUpgradeContainer;
import com.beipuo.mekenergistics.upgrade.MeUpgradeMachineProfile;
import com.beipuo.mekenergistics.upgrade.MeUpgradeRecipeMachineRuntime;
import com.beipuo.mekenergistics.upgrade.MeUpgradeStateOwner;
import com.jerry.meklm.common.tile.machine.TileEntityLargeAntiprotonicNucleosynthesizer;
import com.jerry.meklm.common.tile.machine.TileEntityLargeChemicalInfuser;
import com.jerry.meklm.common.tile.machine.TileEntityLargeElectrolyticSeparator;
import com.jerry.meklm.common.tile.machine.TileEntityLargeRotaryCondensentrator;
import com.jerry.meklm.common.tile.machine.TileEntityLargeSolarNeutronActivator;
import java.util.List;
import mekanism.api.IContentsListener;
import mekanism.api.recipes.ChemicalChemicalToChemicalRecipe;
import mekanism.api.recipes.ElectrolysisRecipe;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.NucleosynthesizingRecipe;
import mekanism.api.recipes.RotaryRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {TileEntityLargeRotaryCondensentrator.class, TileEntityLargeSolarNeutronActivator.class,
        TileEntityLargeElectrolyticSeparator.class, TileEntityLargeChemicalInfuser.class,
        TileEntityLargeAntiprotonicNucleosynthesizer.class}, remap = false)
public abstract class MeklmLargeRecipeMachineMeUpgradeMixin implements MeUpgradeableMachine, MeUpgradeStateOwner, IBlockEntityExtension {
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

    @Unique
    private MeMekanismMachine mekenergistics$machine() {
        var id = BuiltInRegistries.BLOCK.getKey(mekenergistics$tile().getBlockState().getBlock());
        return CompatMachineCatalog.findBySourceBlockId(id).map(spec -> switch (spec.machine().identity()) {
            case LARGE_ROTARY_CONDENSENTRATOR, LARGE_SOLAR_NEUTRON_ACTIVATOR,
                    LARGE_ELECTROLYTIC_SEPARATOR, LARGE_CHEMICAL_INFUSER,
                    LARGE_ANTIPROTONIC_NUCLEOSYNTHESIZER -> spec.machine();
            default -> null;
        }).orElse(null);
    }

    @Override
    public MeUpgradeMachineProfile<?> getMeUpgradeProfile() {
        MeMekanismMachine machine = mekenergistics$machine();
        if (machine == null) return null;
        return new MeUpgradeMachineProfile<>(candidate -> candidate == mekenergistics$tile(),
                candidate -> mekenergistics$inputs(machine), candidate -> mekenergistics$outputs(machine), machine,
                candidate -> new net.minecraft.world.item.ItemStack(candidate.getBlockState().getBlock()),
                candidate -> candidate.getBlockState().getBlock().getName());
    }

    @Unique
    private MeInputLayout mekenergistics$inputs(MeMekanismMachine machine) {
        return switch (machine.identity()) {
            case LARGE_ROTARY_CONDENSENTRATOR -> {
                TileEntityLargeRotaryCondensentrator tile = (TileEntityLargeRotaryCondensentrator) (Object) this;
                yield MeInputLayout.unordered(List.of(tile.getMode()
                        ? MeMachineIoAdapter.fluidInput(tile.fluidTank)
                        : MeMachineIoAdapter.chemicalInput(tile.chemicalTank)));
            }
            case LARGE_SOLAR_NEUTRON_ACTIVATOR -> MeInputLayout.unordered(List.of(
                    MeMachineIoAdapter.chemicalInput(((TileEntityLargeSolarNeutronActivator) (Object) this).inputTank)));
            case LARGE_ELECTROLYTIC_SEPARATOR -> MeInputLayout.unordered(List.of(
                    MeMachineIoAdapter.fluidInput(((TileEntityLargeElectrolyticSeparator) (Object) this).fluidTank)));
            case LARGE_CHEMICAL_INFUSER -> {
                TileEntityLargeChemicalInfuser tile = (TileEntityLargeChemicalInfuser) (Object) this;
                yield MeInputLayout.unordered(List.of(MeMachineIoAdapter.chemicalInput(tile.leftTank),
                        MeMachineIoAdapter.chemicalInput(tile.rightTank)));
            }
            case LARGE_ANTIPROTONIC_NUCLEOSYNTHESIZER -> {
                TileEntityLargeAntiprotonicNucleosynthesizer tile =
                        (TileEntityLargeAntiprotonicNucleosynthesizer) (Object) this;
                TileEntityLargeAntiprotonicNucleosynthesizerAccessor accessor =
                        (TileEntityLargeAntiprotonicNucleosynthesizerAccessor) tile;
                yield MeInputLayout.unordered(List.of(
                        MeMachineIoAdapter.itemInput(accessor.mekenergistics$getInputSlot()),
                        MeMachineIoAdapter.chemicalInput(tile.gasTank),
                        MeMachineIoAdapter.itemInput(accessor.mekenergistics$getGasInputSlot())));
            }
            default -> MeInputLayout.empty();
        };
    }

    @Unique
    private List<? extends MeOutputPort> mekenergistics$outputs(MeMekanismMachine machine) {
        return switch (machine.identity()) {
            case LARGE_ROTARY_CONDENSENTRATOR -> {
                TileEntityLargeRotaryCondensentrator tile = (TileEntityLargeRotaryCondensentrator) (Object) this;
                yield List.of(tile.getMode() ? MeMachineIoAdapter.chemicalOutput(tile.chemicalTank)
                        : MeMachineIoAdapter.fluidOutput(tile.fluidTank));
            }
            case LARGE_SOLAR_NEUTRON_ACTIVATOR -> List.of(MeMachineIoAdapter.chemicalOutput(
                    ((TileEntityLargeSolarNeutronActivator) (Object) this).outputTank));
            case LARGE_ELECTROLYTIC_SEPARATOR -> {
                TileEntityLargeElectrolyticSeparator tile = (TileEntityLargeElectrolyticSeparator) (Object) this;
                yield List.of(MeMachineIoAdapter.chemicalOutput(tile.leftTank),
                        MeMachineIoAdapter.chemicalOutput(tile.rightTank));
            }
            case LARGE_CHEMICAL_INFUSER -> List.of(MeMachineIoAdapter.chemicalOutput(
                    ((TileEntityLargeChemicalInfuser) (Object) this).centerTank));
            case LARGE_ANTIPROTONIC_NUCLEOSYNTHESIZER -> List.of(MeMachineIoAdapter.itemOutput(
                    ((TileEntityLargeAntiprotonicNucleosynthesizerAccessor) (Object) this)
                            .mekenergistics$getOutputSlot()));
            default -> List.of();
        };
    }

    @Override public boolean isMeUpgradeTarget() { return mekenergistics$machine() != null; }
    @Override public boolean isMeUpgradeActive() { return mekenergistics$runtime().active(isMeUpgradeTarget()); }
    @Override public AbstractMeAeSupport<?> getRecipeAeSupport() { return mekenergistics$runtime().support(); }
    @Override public MeInputLayout getPatternInputLayout() { return mekenergistics$inputs(mekenergistics$machine()); }
    @Override public List<? extends MeOutputPort> getPatternOutputPorts() { return mekenergistics$outputs(mekenergistics$machine()); }
    @Override public AeOutputMode getAeOutputMode() { return mekenergistics$runtime().outputMode(); }
    @Override public void cycleAeOutputMode() { mekenergistics$runtime().cycleOutputMode(); }

    @Inject(method = "getInitialInventory", at = @At("RETURN"), cancellable = true)
    private void mekenergistics$addPatternSlots(IContentsListener listener, IContentsListener recipeCacheListener,
            IContentsListener recipeCacheUnpauseListener,
            CallbackInfoReturnable<@NotNull IInventorySlotHolder> cir) {
        if (isMeUpgradeTarget()) cir.setReturnValue(
                mekenergistics$runtime().withPatternSlots(cir.getReturnValue(), recipeCacheListener));
    }

    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/RotaryRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;", at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapRotary(RotaryRecipe recipe, int index, CallbackInfoReturnable<CachedRecipe<RotaryRecipe>> cir) { mekenergistics$wrapEnergy(cir); }
    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/ElectrolysisRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;", at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapElectrolysis(ElectrolysisRecipe recipe, int index, CallbackInfoReturnable<CachedRecipe<ElectrolysisRecipe>> cir) { mekenergistics$wrapEnergy(cir); }
    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/ChemicalChemicalToChemicalRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;", at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapInfuser(ChemicalChemicalToChemicalRecipe recipe, int index, CallbackInfoReturnable<CachedRecipe<ChemicalChemicalToChemicalRecipe>> cir) { mekenergistics$wrapEnergy(cir); }
    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/NucleosynthesizingRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;", at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapNucleosynthesizer(NucleosynthesizingRecipe recipe, int index, CallbackInfoReturnable<CachedRecipe<NucleosynthesizingRecipe>> cir) { mekenergistics$wrapEnergy(cir); }

    @Inject(method = "nextMode", at = @At("RETURN"), require = 0)
    private void mekenergistics$refreshRotaryPatternIo(CallbackInfo ci) {
        if (mekenergistics$tile() instanceof TileEntityLargeRotaryCondensentrator
                && this.mekenergistics$runtime != null) {
            this.mekenergistics$runtime.support().invalidatePatternIoCache();
        }
    }

    @Unique
    private <RECIPE extends MekanismRecipe<?>> void mekenergistics$wrapEnergy(CallbackInfoReturnable<CachedRecipe<RECIPE>> cir) {
        cir.setReturnValue(mekenergistics$runtime().wrapEnergy(mekenergistics$energyContainer(), cir.getReturnValue()));
    }
    @Unique
    private MachineEnergyContainer<?> mekenergistics$energyContainer() {
        Object tile = this;
        if (tile instanceof TileEntityLargeRotaryCondensentrator value) return value.getEnergyContainer();
        if (tile instanceof TileEntityLargeElectrolyticSeparator value) return value.getEnergyContainer();
        if (tile instanceof TileEntityLargeChemicalInfuser value) return value.getEnergyContainer();
        return ((TileEntityLargeAntiprotonicNucleosynthesizer) tile).getEnergyContainer();
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
