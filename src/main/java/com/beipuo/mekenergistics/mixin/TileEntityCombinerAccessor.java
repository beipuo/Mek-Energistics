package com.beipuo.mekenergistics.mixin;

import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.tile.machine.TileEntityCombiner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TileEntityCombiner.class, remap = false)
public interface TileEntityCombinerAccessor {
    @Accessor("mainInputSlot")
    InputInventorySlot mekenergistics$getMainInputSlot();

    @Accessor("extraInputSlot")
    InputInventorySlot mekenergistics$getExtraInputSlot();

    @Accessor("outputSlot")
    OutputInventorySlot mekenergistics$getOutputSlot();

    @Accessor("energyContainer")
    void mekenergistics$setEnergyContainer(MachineEnergyContainer<TileEntityCombiner> energyContainer);
}
