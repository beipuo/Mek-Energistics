package com.beipuo.mekenergistics.mixin;

import fr.iglee42.evolvedmekanism.tiles.machine.TileEntityChemixer;
import mekanism.api.chemical.IChemicalTank;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TileEntityChemixer.class, remap = false)
public interface TileEntityChemixerAccessor {
    @Accessor("mainInputSlot")
    InputInventorySlot mekenergistics$getMainInputSlot();

    @Accessor("extraInputSlot")
    InputInventorySlot mekenergistics$getExtraInputSlot();

    @Accessor("outputSlot")
    OutputInventorySlot mekenergistics$getOutputSlot();

    @Accessor("inputChemicalTank")
    IChemicalTank mekenergistics$getInputChemicalTank();

    @Accessor("energyContainer")
    void mekenergistics$setEnergyContainer(MachineEnergyContainer<TileEntityChemixer> energyContainer);
}
