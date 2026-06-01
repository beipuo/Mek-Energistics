package com.beipuo.mekenergistics.mixin;

import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.tile.prefab.TileEntityElectricMachine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TileEntityElectricMachine.class, remap = false)
public interface TileEntityElectricMachineAccessor {
    @Accessor("inputSlot")
    InputInventorySlot mekenergistics$getInputSlot();

    @Accessor("outputSlot")
    OutputInventorySlot mekenergistics$getOutputSlot();

    @Accessor("energySlot")
    EnergyInventorySlot mekenergistics$getEnergySlot();

    @Accessor("energyContainer")
    void mekenergistics$setEnergyContainer(MachineEnergyContainer<TileEntityElectricMachine> energyContainer);
}
