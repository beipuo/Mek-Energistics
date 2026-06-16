package com.beipuo.mekenergistics.blockentity;

import com.beipuo.mekenergistics.blockentity.support.MeNetworkEnergyHelper;
import mekanism.api.IContentsListener;

final class MeAeBackedEnergyContainer extends MeNetworkEnergyHelper.NetworkBackedEnergyContainer<MeMekanismMachineBlockEntity> {
    MeAeBackedEnergyContainer(MeMekanismMachineBlockEntity owner, IContentsListener listener) {
        super(owner, listener, owner::getGrid, owner.getActionSource());
    }
}
