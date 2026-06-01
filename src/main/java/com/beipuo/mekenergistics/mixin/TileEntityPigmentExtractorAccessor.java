package com.beipuo.mekenergistics.mixin;

import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.tile.machine.TileEntityPigmentExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TileEntityPigmentExtractor.class, remap = false)
public interface TileEntityPigmentExtractorAccessor {
    @Accessor("energyContainer")
    void mekenergistics$setEnergyContainer(MachineEnergyContainer<TileEntityPigmentExtractor> energyContainer);
}
