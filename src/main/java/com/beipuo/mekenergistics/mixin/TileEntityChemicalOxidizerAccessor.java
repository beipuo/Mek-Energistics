package com.beipuo.mekenergistics.mixin;

import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.tile.machine.TileEntityChemicalOxidizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TileEntityChemicalOxidizer.class, remap = false)
public interface TileEntityChemicalOxidizerAccessor {
    @Accessor("inputSlot")
    InputInventorySlot mekenergistics$getInputSlot();

    @Accessor("energyContainer")
    void mekenergistics$setEnergyContainer(MachineEnergyContainer<TileEntityChemicalOxidizer> energyContainer);
}
