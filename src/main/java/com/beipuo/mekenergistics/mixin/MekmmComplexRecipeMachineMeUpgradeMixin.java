package com.beipuo.mekenergistics.mixin;

import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import com.beipuo.mekenergistics.blockentity.api.MeUpgradeableMachine;
import com.beipuo.mekenergistics.blockentity.support.AbstractMeAeSupport;
import com.beipuo.mekenergistics.blockentity.support.io.MeInputLayout;
import com.beipuo.mekenergistics.blockentity.support.io.MeInputPort;
import com.beipuo.mekenergistics.blockentity.support.io.MeMachineIoAdapter;
import com.beipuo.mekenergistics.blockentity.support.io.MeOutputPort;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.compat.catalog.CompatMachineCatalog;
import com.beipuo.mekenergistics.upgrade.MeUpgradeContainer;
import com.beipuo.mekenergistics.upgrade.MeUpgradeMachineProfile;
import com.beipuo.mekenergistics.upgrade.MeUpgradeRecipeMachineRuntime;
import com.beipuo.mekenergistics.upgrade.MeUpgradeStateOwner;
import com.jerry.mekmm.api.recipes.PlantingRecipe;
import com.jerry.mekmm.api.recipes.basic.BasicFluidChemicalToFluidRecipe;
import com.jerry.mekmm.api.recipes.basic.MMBasicChemicalChemicalToChemicalRecipe;
import com.jerry.mekmm.api.recipes.basic.MMBasicItemStackChemicalToItemStackRecipe;
import com.jerry.mekmm.common.tile.machine.TileEntityChemicalReplicator;
import com.jerry.mekmm.common.tile.machine.TileEntityFluidReplicator;
import com.jerry.mekmm.common.tile.machine.TileEntityPlantingStation;
import com.jerry.mekmm.common.tile.machine.TileEntityReplicator;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.fluid.IFluidTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.inventory.slot.chemical.ChemicalInventorySlot;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {TileEntityPlantingStation.class, TileEntityReplicator.class,
        TileEntityChemicalReplicator.class, TileEntityFluidReplicator.class}, remap = false)
public abstract class MekmmComplexRecipeMachineMeUpgradeMixin implements MeUpgradeableMachine, MeUpgradeStateOwner, IBlockEntityExtension {
    @Unique private MeUpgradeRecipeMachineRuntime mekenergistics$runtime;
    @Unique private List<IChemicalTank> mekenergistics$chemicalTanks = List.of();
    @Unique private List<IExtendedFluidTank> mekenergistics$fluidTanks = List.of();
    @Unique private List<InputInventorySlot> mekenergistics$itemInputs = List.of();
    @Unique private List<ChemicalInventorySlot> mekenergistics$conversionSlots = List.of();
    @Unique private List<OutputInventorySlot> mekenergistics$itemOutputs = List.of();

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
            case PLANTING_STATION, REPLICATOR, CHEMICAL_REPLICATOR, FLUID_REPLICATOR -> spec.machine();
            default -> null;
        }).orElse(null);
    }

    @Override
    public MeUpgradeMachineProfile<?> getMeUpgradeProfile() {
        MeMekanismMachine machine = mekenergistics$machine();
        if (machine == null) return null;
        return new MeUpgradeMachineProfile<>(candidate -> candidate == mekenergistics$tile(),
                candidate -> mekenergistics$inputLayout(machine),
                candidate -> mekenergistics$outputPorts(machine), machine,
                candidate -> new net.minecraft.world.item.ItemStack(candidate.getBlockState().getBlock()),
                candidate -> candidate.getBlockState().getBlock().getName());
    }

    @Unique
    private MeInputLayout mekenergistics$inputLayout(MeMekanismMachine machine) {
        if (machine == MeMekanismMachine.CHEMICAL_REPLICATOR) {
            if (this.mekenergistics$chemicalTanks.size() < 2 || this.mekenergistics$conversionSlots.size() < 2) {
                return MeInputLayout.empty();
            }
            return MeInputLayout.lanes(List.of(
                    List.of(MeMachineIoAdapter.chemicalInput(this.mekenergistics$chemicalTanks.get(0)),
                            MeMachineIoAdapter.itemInput(this.mekenergistics$conversionSlots.get(0))),
                    List.of(MeMachineIoAdapter.chemicalInput(this.mekenergistics$chemicalTanks.get(1)),
                            MeMachineIoAdapter.itemInput(this.mekenergistics$conversionSlots.get(1)))));
        }
        if (machine == MeMekanismMachine.FLUID_REPLICATOR && (Object) this instanceof TileEntityFluidReplicator tile) {
            ChemicalInventorySlot slot = ((TileEntityFluidReplicatorAccessor) tile).mekenergistics$getUuSlot();
            if (tile.inputTank == null || tile.uuTank == null || slot == null) return MeInputLayout.empty();
            return MeInputLayout.lanes(List.of(
                    List.of(MeMachineIoAdapter.fluidInput(tile.inputTank)),
                    List.of(MeMachineIoAdapter.chemicalInput(tile.uuTank), MeMachineIoAdapter.itemInput(slot))));
        }
        if (this.mekenergistics$itemInputs.isEmpty() || this.mekenergistics$chemicalTanks.isEmpty()
                || this.mekenergistics$conversionSlots.isEmpty()) return MeInputLayout.empty();
        return MeInputLayout.unordered(List.of(
                MeMachineIoAdapter.itemInput(this.mekenergistics$itemInputs.getFirst()),
                MeMachineIoAdapter.chemicalInput(this.mekenergistics$chemicalTanks.getFirst()),
                MeMachineIoAdapter.itemInput(this.mekenergistics$conversionSlots.getFirst())));
    }

    @Unique
    private List<? extends MeOutputPort> mekenergistics$outputPorts(MeMekanismMachine machine) {
        if (machine == MeMekanismMachine.CHEMICAL_REPLICATOR) {
            if ((Object) this instanceof TileEntityChemicalReplicator replicator && replicator.outputTank != null) {
                return List.of(MeMachineIoAdapter.chemicalOutput(replicator.outputTank));
            }
            return List.of();
        }
        if (machine == MeMekanismMachine.FLUID_REPLICATOR && (Object) this instanceof TileEntityFluidReplicator tile) {
            return tile.outputTank == null ? List.of() : List.of(MeMachineIoAdapter.fluidOutput(tile.outputTank));
        }
        if (machine == MeMekanismMachine.PLANTING_STATION
                && (Object) this instanceof TileEntityPlantingStation planting) {
            TileEntityPlantingStationAccessor accessor = (TileEntityPlantingStationAccessor) planting;
            return List.of(MeMachineIoAdapter.itemOutput(accessor.mekenergistics$getMainOutputSlot()),
                    MeMachineIoAdapter.itemOutput(accessor.mekenergistics$getSecondaryOutputSlot()));
        }
        if (machine == MeMekanismMachine.REPLICATOR && (Object) this instanceof TileEntityReplicator replicator) {
            return List.of(MeMachineIoAdapter.itemOutput(
                    ((TileEntityReplicatorAccessor) replicator).mekenergistics$getOutputSlot()));
        }
        return this.mekenergistics$itemOutputs.stream().map(MeMachineIoAdapter::itemOutput).toList();
    }

    @Override public boolean isMeUpgradeTarget() { return mekenergistics$machine() != null; }
    @Override public boolean isMeUpgradeActive() { return mekenergistics$runtime().active(isMeUpgradeTarget()); }
    @Override public AbstractMeAeSupport<?> getRecipeAeSupport() { return mekenergistics$runtime().support(); }
    @Override public MeInputLayout getPatternInputLayout() { return mekenergistics$inputLayout(mekenergistics$machine()); }
    @Override public List<? extends MeOutputPort> getPatternOutputPorts() { return mekenergistics$outputPorts(mekenergistics$machine()); }
    @Override public AeOutputMode getAeOutputMode() { return mekenergistics$runtime().outputMode(); }
    @Override public void cycleAeOutputMode() { mekenergistics$runtime().cycleOutputMode(); }

    @Inject(method = "getInitialChemicalTanks", at = @At("RETURN"), require = 0)
    private void mekenergistics$captureChemicals(IContentsListener listener, IContentsListener recipeCacheListener,
            IContentsListener recipeCacheUnpauseListener, CallbackInfoReturnable<IChemicalTankHolder> cir) {
        if (isMeUpgradeTarget()) this.mekenergistics$chemicalTanks = List.copyOf(cir.getReturnValue().getTanks(null));
    }

    @Inject(method = "getInitialFluidTanks", at = @At("RETURN"), require = 0)
    private void mekenergistics$captureFluids(IContentsListener listener, IContentsListener recipeCacheListener,
            IContentsListener recipeCacheUnpauseListener, CallbackInfoReturnable<IFluidTankHolder> cir) {
        if (isMeUpgradeTarget()) this.mekenergistics$fluidTanks = List.copyOf(cir.getReturnValue().getTanks(null));
    }

    @Inject(method = "getInitialInventory", at = @At("RETURN"), cancellable = true)
    private void mekenergistics$addPatternSlots(IContentsListener listener, IContentsListener recipeCacheListener,
            IContentsListener recipeCacheUnpauseListener,
            CallbackInfoReturnable<@NotNull IInventorySlotHolder> cir) {
        if (!isMeUpgradeTarget()) return;
        List<InputInventorySlot> inputs = new ArrayList<>();
        List<ChemicalInventorySlot> conversions = new ArrayList<>();
        List<OutputInventorySlot> outputs = new ArrayList<>();
        for (IInventorySlot slot : cir.getReturnValue().getInventorySlots(null)) {
            if (slot instanceof ChemicalInventorySlot conversion) conversions.add(conversion);
            else if (slot instanceof InputInventorySlot input) inputs.add(input);
            else if (slot instanceof OutputInventorySlot output) outputs.add(output);
        }
        this.mekenergistics$itemInputs = List.copyOf(inputs);
        this.mekenergistics$conversionSlots = List.copyOf(conversions);
        this.mekenergistics$itemOutputs = List.copyOf(outputs);
        cir.setReturnValue(mekenergistics$runtime().withPatternSlots(cir.getReturnValue(), recipeCacheListener));
    }

    @Inject(method = "createNewCachedRecipe(Lcom/jerry/mekmm/api/recipes/PlantingRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;", at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapPlanting(PlantingRecipe recipe, int index, CallbackInfoReturnable<CachedRecipe<PlantingRecipe>> cir) { mekenergistics$wrapEnergy(cir); }
    @Inject(method = "createNewCachedRecipe(Lcom/jerry/mekmm/api/recipes/basic/MMBasicItemStackChemicalToItemStackRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;", at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapReplicator(MMBasicItemStackChemicalToItemStackRecipe recipe, int index, CallbackInfoReturnable<CachedRecipe<MMBasicItemStackChemicalToItemStackRecipe>> cir) { mekenergistics$wrapEnergy(cir); }
    @Inject(method = "createNewCachedRecipe(Lcom/jerry/mekmm/api/recipes/basic/MMBasicChemicalChemicalToChemicalRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;", at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapChemicalReplicator(MMBasicChemicalChemicalToChemicalRecipe recipe, int index, CallbackInfoReturnable<CachedRecipe<MMBasicChemicalChemicalToChemicalRecipe>> cir) { mekenergistics$wrapEnergy(cir); }
    @Inject(method = "createNewCachedRecipe(Lcom/jerry/mekmm/api/recipes/basic/BasicFluidChemicalToFluidRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;", at = @At("RETURN"), cancellable = true, require = 0)
    private void mekenergistics$wrapFluidReplicator(BasicFluidChemicalToFluidRecipe recipe, int index, CallbackInfoReturnable<CachedRecipe<BasicFluidChemicalToFluidRecipe>> cir) { mekenergistics$wrapEnergy(cir); }

    @Unique
    private <RECIPE extends MekanismRecipe<?>> void mekenergistics$wrapEnergy(CallbackInfoReturnable<CachedRecipe<RECIPE>> cir) {
        cir.setReturnValue(mekenergistics$runtime().wrapEnergy(mekenergistics$energyContainer(), cir.getReturnValue()));
    }
    @Unique
    private MachineEnergyContainer<?> mekenergistics$energyContainer() {
        Object tile = this;
        if (tile instanceof TileEntityPlantingStation value) return value.getEnergyContainer();
        if (tile instanceof TileEntityReplicator value) return value.getEnergyContainer();
        if (tile instanceof TileEntityChemicalReplicator value) return value.getEnergyContainer();
        return ((TileEntityFluidReplicator) tile).getEnergyContainer();
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
