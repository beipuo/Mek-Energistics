package com.beipuo.mekenergistics.mixin;

import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.tile.machine.TileEntityChemicalDissolutionChamber;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TileEntityChemicalDissolutionChamber.class, remap = false)
public interface TileEntityChemicalDissolutionChamberAccessor {
    @Accessor("inputSlot")
    InputInventorySlot mekenergistics$getInputSlot();

    @Accessor("energyContainer")
    void mekenergistics$setEnergyContainer(MachineEnergyContainer<TileEntityChemicalDissolutionChamber> energyContainer);
}
