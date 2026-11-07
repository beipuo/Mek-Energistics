package com.beipuo.mekenergistics.blockentity;

import com.beipuo.mekenergistics.blockentity.support.MeNetworkEnergyHelper;
import mekanism.api.IContentsListener;

final class MeAeBackedEnergyContainer extends MeNetworkEnergyHelper.NetworkBackedEnergyContainer<MeMekanismMachineBlockEntity> {
    MeAeBackedEnergyContainer(MeMekanismMachineBlockEntity owner, IContentsListener listener) {
        // Mekanism builds energy containers inside the TileEntityMekanism super-constructor, before
        // the owner's final actionSource field is assigned. Read it lazily: passing the value here
        // would capture null forever and make every network/Applied Flux extraction fail.
        super(owner, listener, owner::getGrid, owner::getActionSource);
    }
}
