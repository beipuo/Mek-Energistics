package com.beipuo.mekenergistics.mixin;

import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.tile.machine.TileEntityChemicalCrystallizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TileEntityChemicalCrystallizer.class, remap = false)
public interface TileEntityChemicalCrystallizerAccessor {
    @Accessor("outputSlot")
    OutputInventorySlot mekenergistics$getOutputSlot();

    @Accessor("energyContainer")
    void mekenergistics$setEnergyContainer(MachineEnergyContainer<TileEntityChemicalCrystallizer> energyContainer);
}
