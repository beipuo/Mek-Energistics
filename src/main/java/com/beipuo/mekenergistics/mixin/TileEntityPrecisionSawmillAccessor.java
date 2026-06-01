package com.beipuo.mekenergistics.mixin;

import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.tile.machine.TileEntityPrecisionSawmill;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TileEntityPrecisionSawmill.class, remap = false)
public interface TileEntityPrecisionSawmillAccessor {
    @Accessor("inputSlot")
    InputInventorySlot mekenergistics$getInputSlot();

    @Accessor("outputSlot")
    OutputInventorySlot mekenergistics$getOutputSlot();

    @Accessor("secondaryOutputSlot")
    OutputInventorySlot mekenergistics$getSecondaryOutputSlot();

    @Accessor("energyContainer")
    void mekenergistics$setEnergyContainer(MachineEnergyContainer<TileEntityPrecisionSawmill> energyContainer);
}
