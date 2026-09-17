package com.beipuo.mekenergistics.mixin;

import com.beipuo.mekenergistics.blockentity.api.AeOutputMode;
import com.beipuo.mekenergistics.blockentity.support.io.MeInputLayout;
import com.beipuo.mekenergistics.blockentity.support.io.MeInfusionModePolicy;
import com.beipuo.mekenergistics.blockentity.support.io.MeMachineIoAdapter;
import com.beipuo.mekenergistics.blockentity.support.io.MeOutputPort;
import com.beipuo.mekenergistics.upgrade.EvolvedAlloyingFactoryUpgradeAccess;
import com.beipuo.mekenergistics.upgrade.MeUpgradeMachineProfile;
import com.beipuo.mekenergistics.upgrade.MeUpgradeRecipeMachineAdapter;
import com.beipuo.mekenergistics.upgrade.MeUpgradeRecipeMachineRuntime;
import com.beipuo.mekenergistics.upgrade.MekanismFactoryUpgradeProfiles;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.content.blocktype.FactoryType;
import mekanism.common.recipe.lookup.monitor.FactoryRecipeCacheLookupMonitor;
import mekanism.common.tile.factory.TileEntityFactory;
import mekanism.common.tile.factory.TileEntityItemStackChemicalToItemStackFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileEntityFactory.class, remap = false)
public abstract class TileEntityFactoryMeUpgradeMixin implements MeUpgradeRecipeMachineAdapter,
        MekanismFactoryUpgradeProfiles.FactoryIoAccess {
    @Shadow protected List<IInventorySlot> inputSlots;
    @Shadow protected List<IInventorySlot> outputSlots;
    @Shadow protected FactoryRecipeCacheLookupMonitor<?>[] recipeCacheLookupMonitors;
    @Shadow protected abstract IInventorySlot getExtraSlot();

    @Unique private MeUpgradeRecipeMachineRuntime mekenergistics$factoryRuntime;

    @Unique
    private TileEntityFactory<?> mekenergistics$factory() {
        return (TileEntityFactory<?>) (Object) this;
    }

    @Override
    public MeUpgradeRecipeMachineRuntime getOrCreateMeUpgradeRuntime() {
        if (this.mekenergistics$factoryRuntime == null) {
            this.mekenergistics$factoryRuntime = new MeUpgradeRecipeMachineRuntime(
                    mekenergistics$factory(), AeOutputMode.BOTH);
        }
        return this.mekenergistics$factoryRuntime;
    }

    @Override
    public MeUpgradeRecipeMachineRuntime getExistingMeUpgradeRuntime() {
        return this.mekenergistics$factoryRuntime;
    }

    @Override
    public com.beipuo.mekenergistics.blockentity.support.AbstractMeAeSupport<?> getRecipeAeSupport() {
        return (Object) this instanceof com.beipuo.mekenergistics.blockentity.api.MeFactoryIoOwner legacy
                ? legacy.getAeSupport() : MeUpgradeRecipeMachineAdapter.super.getRecipeAeSupport();
    }

    @Override
    public AeOutputMode getAeOutputMode() {
        return (Object) this instanceof com.beipuo.mekenergistics.blockentity.api.MeFactoryIoOwner legacy
                ? legacy.getAeSupport().getAeOutputMode() : MeUpgradeRecipeMachineAdapter.super.getAeOutputMode();
    }

    @Override
    public void cycleAeOutputMode() {
        if ((Object) this instanceof com.beipuo.mekenergistics.blockentity.api.MeFactoryIoOwner legacy) {
            legacy.getAeSupport().cycleAeOutputMode();
        } else {
            MeUpgradeRecipeMachineAdapter.super.cycleAeOutputMode();
        }
    }

    @Unique
    private MeUpgradeMachineProfile<TileEntityFactory<?>> mekenergistics$factoryProfile() {
        return MekanismFactoryUpgradeProfiles.forTile(mekenergistics$factory(), this);
    }

    @Override
    public MeUpgradeMachineProfile<?> getMeUpgradeProfile() {
        return mekenergistics$factoryProfile();
    }

    @Override
    public MeInputLayout mekenergistics$getFactoryInputLayout() {
        if ((Object) this instanceof EvolvedAlloyingFactoryUpgradeAccess alloying) {
            return MeInputLayout.lanes(List.of(
                    List.of(MeMachineIoAdapter.autoSortedFactoryItemInput(this.inputSlots)),
                    List.of(MeMachineIoAdapter.itemInput(getExtraSlot())),
                    List.of(MeMachineIoAdapter.itemInput(alloying.mekenergistics$getSecondExtraSlot()))));
        }
        List<com.beipuo.mekenergistics.blockentity.support.io.MeInputPort> inputs = new ArrayList<>();
        TileEntityFactory<?> tile = mekenergistics$factory();
        FactoryType type = tile.getFactoryType();
        if (type == FactoryType.INFUSING && MeInfusionModePolicy.isConversionMode(getAeOutputMode())) {
            inputs.add(MeMachineIoAdapter.manualItemInput(getExtraSlot()));
        } else {
            inputs.add(MeMachineIoAdapter.autoSortedFactoryItemInput(this.inputSlots));
            if (tile instanceof TileEntityItemStackChemicalToItemStackFactory chemicalFactory) {
                inputs.add(MeMachineIoAdapter.chemicalInput(chemicalFactory.getChemicalTank()));
                inputs.add(type == FactoryType.INFUSING
                        ? MeMachineIoAdapter.manualItemInput(getExtraSlot())
                        : MeMachineIoAdapter.itemInput(getExtraSlot()));
            } else if (type == FactoryType.COMBINING) {
                inputs.add(MeMachineIoAdapter.itemInput(getExtraSlot()));
            }
        }
        return MeInputLayout.unordered(inputs);
    }

    @Override
    public List<? extends MeOutputPort> mekenergistics$getFactoryOutputPorts() {
        List<MeOutputPort> outputs = new ArrayList<>();
        this.outputSlots.stream().map(MeMachineIoAdapter::itemOutput).forEach(outputs::add);
        TileEntityFactory<?> tile = mekenergistics$factory();
        if (tile.getFactoryType() == FactoryType.INFUSING
                && tile instanceof TileEntityItemStackChemicalToItemStackFactory chemicalFactory) {
            outputs.add(MeMachineIoAdapter.chemicalOutput(chemicalFactory.getChemicalTank()));
        }
        return List.copyOf(outputs);
    }

    @Override
    public MeInputLayout getPatternInputLayout() {
        return (Object) this instanceof com.beipuo.mekenergistics.blockentity.api.MeFactoryIoOwner legacy
                ? com.beipuo.mekenergistics.blockentity.api.MeFactoryIoOwner.factoryPatternInputLayout(legacy)
                : MeUpgradeRecipeMachineAdapter.super.getPatternInputLayout();
    }

    @Override
    public List<? extends MeOutputPort> getPatternOutputPorts() {
        return (Object) this instanceof com.beipuo.mekenergistics.blockentity.api.MeFactoryIoOwner legacy
                ? com.beipuo.mekenergistics.blockentity.api.MeFactoryIoOwner.factoryPatternOutputPorts(legacy)
                : MeUpgradeRecipeMachineAdapter.super.getPatternOutputPorts();
    }

    @Inject(method = "getInitialInventory", at = @At("RETURN"), cancellable = true)
    private void mekenergistics$addFactoryPatternSlots(IContentsListener listener,
            CallbackInfoReturnable<IInventorySlotHolder> cir) {
        cir.setReturnValue(addMePatternSlots(cir.getReturnValue(), this::mekenergistics$unpauseFactoryRecipes));
    }

    @Unique
    private void mekenergistics$unpauseFactoryRecipes() {
        if (this.recipeCacheLookupMonitors != null) {
            for (FactoryRecipeCacheLookupMonitor<?> monitor : this.recipeCacheLookupMonitors) {
                if (monitor != null) {
                    monitor.unpause();
                }
            }
        }
    }

    @Inject(method = "onUpdateServer", at = @At("RETURN"), cancellable = true)
    private void mekenergistics$processFactoryPatternIo(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(processMePatternIo(cir.getReturnValue()));
    }
}
