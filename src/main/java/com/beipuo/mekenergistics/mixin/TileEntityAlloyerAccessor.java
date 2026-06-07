package com.beipuo.mekenergistics.mixin;

import fr.iglee42.evolvedmekanism.tiles.LimitedInputInventorySlot;
import fr.iglee42.evolvedmekanism.tiles.machine.TileEntityAlloyer;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TileEntityAlloyer.class, remap = false)
public interface TileEntityAlloyerAccessor {
    @Accessor("mainInputSlot")
    InputInventorySlot mekenergistics$getMainInputSlot();

    @Accessor("extraInputSlot")
    LimitedInputInventorySlot mekenergistics$getExtraInputSlot();

    @Accessor("secondExtraInputSlot")
    LimitedInputInventorySlot mekenergistics$getSecondExtraInputSlot();

    @Accessor("outputSlot")
    OutputInventorySlot mekenergistics$getOutputSlot();

    @Accessor("energyContainer")
    void mekenergistics$setEnergyContainer(MachineEnergyContainer<TileEntityAlloyer> energyContainer);
}
