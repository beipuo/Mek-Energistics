package com.beipuo.mekenergistics.blockentity;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.capabilities.energy.MachineEnergyContainer;

final class MeAeBackedEnergyContainer extends MachineEnergyContainer<MeMekanismMachineBlockEntity> {
    private final MeMekanismMachineBlockEntity owner;

    MeAeBackedEnergyContainer(MeMekanismMachineBlockEntity owner, IContentsListener listener) {
        super(MachineEnergyContainer.validateBlock(owner).getStorage(), MachineEnergyContainer.validateBlock(owner).getUsage(),
                BasicEnergyContainer.notExternal, ConstantPredicates.alwaysTrue(), owner, listener);
        this.owner = owner;
    }

    @Override
    public long extract(long amount, Action action, AutomationType automationType) {
        long localExtracted = super.extract(amount, action, automationType);
        long remaining = amount - localExtracted;
        if (remaining <= 0 || automationType != AutomationType.INTERNAL) {
            return localExtracted;
        }
        return localExtracted + this.owner.extractAeAsFe(remaining, action);
    }
}
