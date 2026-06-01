package com.beipuo.mekenergistics.mixin;

import mekanism.common.capabilities.energy.PRCEnergyContainer;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.tile.machine.TileEntityPressurizedReactionChamber;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TileEntityPressurizedReactionChamber.class, remap = false)
public interface TileEntityPressurizedReactionChamberAccessor {
    @Accessor("inputSlot")
    InputInventorySlot mekenergistics$getInputSlot();

    @Accessor("outputSlot")
    OutputInventorySlot mekenergistics$getOutputSlot();

    @Accessor("energyContainer")
    void mekenergistics$setEnergyContainer(PRCEnergyContainer energyContainer);
}
