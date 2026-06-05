package com.beipuo.mekenergistics.blockentity;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.functions.ConstantPredicates;
import com.beipuo.mekenergistics.blockentity.support.MeNetworkEnergyHelper;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.capabilities.energy.MachineEnergyContainer;

final class MeAeBackedEnergyContainer extends MachineEnergyContainer<MeMekanismMachineBlockEntity> implements MeNetworkEnergyHelper.LocalEnergyBuffer {
    private final MeMekanismMachineBlockEntity owner;

    MeAeBackedEnergyContainer(MeMekanismMachineBlockEntity owner, IContentsListener listener) {
        super(MachineEnergyContainer.validateBlock(owner).getStorage(), MachineEnergyContainer.validateBlock(owner).getUsage(),
                BasicEnergyContainer.notExternal, ConstantPredicates.alwaysTrue(), owner, listener);
        this.owner = owner;
    }

    @Override
    public long extract(long amount, Action action, AutomationType automationType) {
        return MeNetworkEnergyHelper.extractWithLocalBuffer(this, this.owner.getGrid(), this.owner.getActionSource(), amount, action, automationType);
    }

    @Override
    public long extractLocal(long amount, Action action, AutomationType automationType) {
        return super.extract(amount, action, automationType);
    }
}
