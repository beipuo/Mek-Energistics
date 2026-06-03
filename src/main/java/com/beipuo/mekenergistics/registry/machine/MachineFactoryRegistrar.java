package com.beipuo.mekenergistics.registry.machine;

import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;

@FunctionalInterface
public interface MachineFactoryRegistrar {
    <TILE extends TileEntityMekanism> TileEntityTypeRegistryObject<TILE> register(MeMekanismMachine machine, MachineFactory<TILE> factory);
}
