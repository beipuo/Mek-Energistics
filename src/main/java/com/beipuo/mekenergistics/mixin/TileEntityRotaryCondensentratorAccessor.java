package com.beipuo.mekenergistics.mixin;

import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.tile.machine.TileEntityRotaryCondensentrator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TileEntityRotaryCondensentrator.class, remap = false)
public interface TileEntityRotaryCondensentratorAccessor {
    @Accessor("energyContainer")
    void mekenergistics$setEnergyContainer(MachineEnergyContainer<TileEntityRotaryCondensentrator> energyContainer);
}
