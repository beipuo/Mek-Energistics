package com.beipuo.mekenergistics.mixin;

import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.tile.machine.TileEntityAntiprotonicNucleosynthesizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TileEntityAntiprotonicNucleosynthesizer.class, remap = false)
public interface TileEntityAntiprotonicNucleosynthesizerAccessor {
    @Accessor("inputSlot")
    InputInventorySlot mekenergistics$getInputSlot();

    @Accessor("outputSlot")
    OutputInventorySlot mekenergistics$getOutputSlot();

    @Accessor("energyContainer")
    void mekenergistics$setEnergyContainer(MachineEnergyContainer<TileEntityAntiprotonicNucleosynthesizer> energyContainer);
}
