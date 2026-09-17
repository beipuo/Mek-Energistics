package com.beipuo.mekenergistics.upgrade;

import com.beipuo.mekenergistics.blockentity.support.io.MeInputLayout;
import com.beipuo.mekenergistics.blockentity.support.io.MeInfusionModePolicy;
import com.beipuo.mekenergistics.blockentity.support.io.MeMachineIoAdapter;
import com.beipuo.mekenergistics.blockentity.support.io.MeOutputPort;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.content.blocktype.FactoryType;

public interface BasicFactoryUpgradeAccess extends CatalogFactoryUpgradeAdapter {
    List<IInventorySlot> meUpgradeInputSlots();

    List<IInventorySlot> meUpgradeOutputSlots();

    IInventorySlot meUpgradeExtraSlot();

    default IInventorySlot meUpgradeSecondExtraSlot() {
        return null;
    }

    default IChemicalTank meUpgradeChemicalTank() {
        return null;
    }

    @Override
    default MeInputLayout mekenergistics$getFactoryInputLayout() {
        var mainInput = MeMachineIoAdapter.autoSortedFactoryItemInput(meUpgradeInputSlots());
        IInventorySlot extraSlot = meUpgradeExtraSlot();
        IInventorySlot secondExtraSlot = meUpgradeSecondExtraSlot();
        if (secondExtraSlot != null) {
            return MeInputLayout.lanes(List.of(
                    List.of(mainInput),
                    List.of(MeMachineIoAdapter.itemInput(extraSlot)),
                    List.of(MeMachineIoAdapter.itemInput(secondExtraSlot))));
        }
        IChemicalTank chemicalTank = meUpgradeChemicalTank();
        if (chemicalTank != null) {
            if (getMachine().factoryType() == FactoryType.INFUSING
                    && MeInfusionModePolicy.isConversionMode(getAeOutputMode())) {
                return MeInputLayout.unordered(List.of(MeMachineIoAdapter.manualItemInput(extraSlot)));
            }
            return MeInputLayout.unordered(List.of(mainInput,
                    MeMachineIoAdapter.chemicalInput(chemicalTank),
                    getMachine().factoryType() == FactoryType.INFUSING
                            ? MeMachineIoAdapter.manualItemInput(extraSlot)
                            : MeMachineIoAdapter.itemInput(extraSlot)));
        }
        if (extraSlot != null && "combining".equals(getMachine().machineTypeId())) {
            return MeInputLayout.lanes(List.of(
                    List.of(mainInput),
                    List.of(MeMachineIoAdapter.itemInput(extraSlot))));
        }
        return MeInputLayout.unordered(List.of(mainInput));
    }

    @Override
    default List<? extends MeOutputPort> mekenergistics$getFactoryOutputPorts() {
        List<MeOutputPort> outputs = new ArrayList<>();
        meUpgradeOutputSlots().stream().map(MeMachineIoAdapter::itemOutput).forEach(outputs::add);
        IChemicalTank chemicalTank = meUpgradeChemicalTank();
        if (chemicalTank != null && getMachine().factoryType() == FactoryType.INFUSING) {
            outputs.add(MeMachineIoAdapter.chemicalOutput(chemicalTank));
        }
        return List.copyOf(outputs);
    }
}
